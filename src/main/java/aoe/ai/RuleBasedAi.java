package aoe.ai;

import AgeOfEmpires.c;

/**
 * 规则式玩家 AI v1（-Daoe.playerAi=aoe.ai.RuleBasedAi）。
 *
 * 目标：随机图（gameMode=0）打 Easy 敌方 AI 稳定取胜。分四个模块，每 8 tick
 * 决策一次（节流），全部决策只依赖游戏状态 + tickCount，不用墙钟、不用随机数
 * （因此天然确定：同一对局重放结果一致；也绝不消耗游戏的 nextRandomInt）。
 *
 * 读面：双方 playerUnitHeaders / playerUnitSlots / var_int_arr_arr_b(建筑记录) /
 * mapTiles / techFlags。本里程碑允许读对面状态（hdr[1] 全字段）。
 * 写面：军事群令走 selectUnits(0,-1)+orderMove+clearSelection 原语；村民单体
 * 改派与敌方 AI 同做法——直接写 slot[2] 目标（引擎自身在 tickAi 里就是这么写的）。
 *
 * 关键机制依据（均已考证，见 docs/game-mechanics.md / docs/unit-stats.md）：
 * - 村民被命令到资源格后，采集→满载→回交存点→返矿全循环由引擎自动完成
 *   （抵达钩子 c() case 768 开工，case 256 交存后 slot[2]=slot[5] 自动返矿；
 *   资源耗尽会自动换同 kind 邻格）。玩家侧唯一缺的是"首次派工"——这是本 AI 的活。
 * - 建筑放下后自动成型（j() 每帧 +8 进度，~32 tick 建成），无需村民施工；
 *   a(0,type,tx,ty,0x40000000,true) 落建筑并在放置时扣款。
 * - 村民在"房屋(11)"训练（不是 TC！queueUnitTraining case 0→建筑类型 11，
 *   出产时按 tick 奇偶出 0/1 两形态）；训练上限 hdr[75+max(0,type-1)]，
 *   人口判定 hdr[2]+hdr[49] < hdr[3]（+26 单位硬上限），产兵扣款在产出时。
 * - 攻击 = orderMove 到敌格：抵达钩子 case 256(敌建筑)/512(敌单位) 自动转战斗。
 * - 军队价值 = Σ(攻+甲)，引擎自己也是这么算 hdr[55] 的（这里剔除村民另算）。
 */
public final class RuleBasedAi implements PlayerAi {

    // —— 节奏 ——
    private static final int DECIDE_EVERY = 8;       // 决策周期（tick）
    private static final int ATTACK_REISSUE = 150;   // 进攻中群令重发周期（战斗结束单位会原地待命，靠重发继续推进）
    private static final int DEFEND_REISSUE = 48;    // 回防群令重发周期
    private static final int LOG_EVERY = 500;        // 状态摘要日志周期

    // —— 经济参数 ——
    private static final int VILL_TARGET = 6;        // 村民目标数（实际被训练上限 hdr[75] 卡住，设高点无害）
    private static final int WOOD_WORKERS_EARLY = 3; // 封建前的木/金分配（开局 4 村民封顶）
    private static final int GOLD_WORKERS_EARLY = 1;
    private static final int WOOD_WORKERS_LATE = 2;  // 封建后（金兵变贵，加金减木）
    private static final int GOLD_WORKERS_LATE = 2;

    // —— 军事参数 ——
    private static final int MIN_ATTACK_UNITS = 10;  // 出门最少兵力（个）
    private static final int ABS_ATTACK_VALUE = 120; // 军队价值绝对阈值（≈满编混成，含投石机）
    private static final int FORCE_ATTACK_TICK = 12000; // 僵持兜底：超过此 tick 降低出门门槛
    private static final int FORCE_ATTACK_UNITS = 8; // 兜底出门的最少兵力
    private static final int DEFEND_D2 = 100;        // 回防半径²（10 格）
    private static final int RETREAT_LEFT = 3;       // 进攻中兵力 ≤ 此值 → 撤军

    private int nextDecide;
    private boolean attackMode;
    private int lastAttackOrder = -100000;
    private int lastDefendOrder = -100000;
    private int lastLog;
    private int attackBestD2 = Integer.MAX_VALUE;    // 进攻停滞观测：我军距敌 TC 的最近距离²
    private int attackBestTick;
    private boolean feudalTried;                     // 只打一次失败日志，避免刷屏
    // 村民卡死检测（按槽位记上次位置/时间；槽位随死亡压缩会错位，但错位最多导致
    // 一次误判重派，无害）：行军中 300 tick 没挪窝 → 目标拉黑 3000 tick 并重派。
    private final int[] villLastPos = new int[26];
    private final int[] villLastTick = new int[26];
    private final int[] resBlacklistUntil = new int[4096];
    private boolean noWoodRes;      // 全场无可达树/金矿（findResource 空手）→ 跳过对应矿场
    private boolean noGoldRes;

    @Override
    public void tick(c game) {
        if (game.tickCount < this.nextDecide) {
            return;
        }
        this.nextDecide = game.tickCount + DECIDE_EVERY;
        // 模拟只在 onPaint default 分支跑（screenState 2/4/5/7/9..14 暂停），AI 同步休眠
        int ss = game.screenState;
        if (ss == 2 || ss == 4 || ss == 5 || ss == 7 || (ss >= 9 && ss <= 14)) {
            return;
        }
        int[] hdr = game.playerUnitHeaders[0];
        if (hdr[2] <= 0 && hdr[4] <= 0) {
            return;     // 未进任务（菜单态 headers 全零）或已全灭
        }

        // ===== 态势扫描（每决策一次，双方全量） =====
        short[] slots = game.playerUnitSlots[0];
        int vills = 0, milCount = 0, milVal = 0;
        int woodW = 0, goldW = 0;
        int[] idleVill = new int[26];
        int idleN = 0;
        int units = Math.min(hdr[2], 26);
        for (int i = 0; i < units; ++i) {
            int o = i << 3;
            int type = slots[o + 3] & 0xFF;
            if (type >= 2) {
                ++milCount;
                milVal += hdr[13 + type] + hdr[23 + type];
                continue;
            }
            ++vills;
            int action = slots[o + 7] & 0xF;
            int kind = 0;
            if (action == 2 || action == 3) {
                kind = (slots[o + 7] & 0xF0) >> 4;    // 采集/回存：资源 kind 在 action 高半字节
            } else {
                // 行军中等：从目标格反推资源 kind（资源格 = 0x300|kind）
                int tgt = slots[o + 2] & 0xFFFF;
                int tt = game.mapTiles[(tgt >>> 8) + ((tgt & 0xFF) << 6)] & 0xFFF;
                if ((tt & 0x300) == 0x300) {
                    kind = tt & 3;
                }
            }
            if (kind == 1) {
                ++woodW;
            } else if (kind == 2) {
                ++goldW;
            }
            // 空闲 = 动作 0 且已到目标格（还在走路的不动它）；行军中 300 tick
            // 没挪窝 = 卡死（目标不可达/被围），拉黑当前目标格后按空闲重派
            int pos = slots[o + 0] & 0xFFFF;
            int tgt = slots[o + 2] & 0xFFFF;
            if (action == 0 && pos != tgt) {
                if (pos == this.villLastPos[i] && game.tickCount - this.villLastTick[i] > 300) {
                    this.resBlacklistUntil[(tgt >>> 8) + ((tgt & 0xFF) << 6)] = game.tickCount + 3000;
                    this.villLastTick[i] = game.tickCount;   // 重开 300 tick 窗口，避免逐帧刷屏
                    if (idleN < 26) {
                        idleVill[idleN++] = i;
                    }
                    System.out.println("[ai] villager " + i + " STUCK at " + (pos >>> 8) + ","
                        + (pos & 0xFF) + " tgt " + (tgt >>> 8) + "," + (tgt & 0xFF)
                        + " blacklisted, t=" + game.tickCount);
                    continue;
                }
            }
            this.villLastPos[i] = pos;
            this.villLastTick[i] = game.tickCount;
            if (action == 0 && pos == tgt && idleN < 26) {
                idleVill[idleN++] = i;
            }
        }
        int[] ehdr = game.playerUnitHeaders[1];
        short[] eslots = game.playerUnitSlots[1];
        int enemyMilVal = 0, enemyMilCount = 0;
        int eunits = Math.min(ehdr[2], 26);
        for (int i = 0; i < eunits; ++i) {
            int o = i << 3;
            int type = eslots[o + 3] & 0xFF;
            if (type >= 2) {
                ++enemyMilCount;
                enemyMilVal += ehdr[13 + type] + ehdr[23 + type];
            }
        }
        // 建筑扫描
        int[] recs = game.var_int_arr_arr_b[0];
        int houseN = 0, barracksDone = 0, archeryDone = 0, siegeDone = 0;
        int lumberN = 0, miningN = 0, millN = 0, outpostN = 0;
        int tcSlot = -1, houseSlot = -1, barracksSlot = -1, archerySlot = -1, siegeSlot = -1;
        int lumberSlot = -1, miningSlot = -1;
        boolean anyUC = false;
        for (int i = 0; i < hdr[4]; ++i) {
            int o = i << 2;
            int bt = recs[o + 3] & 0xFF;
            boolean uc = (recs[o + 2] & 0x40000000) != 0;
            if (uc) {
                anyUC = true;
            }
            switch (bt) {
                case 9:  tcSlot = o; break;
                case 11: ++houseN; if (!uc && houseSlot < 0) houseSlot = o; break;
                case 10: if (!uc) { ++barracksDone; if (barracksSlot < 0) barracksSlot = o; } break;
                case 7:  if (!uc) { ++archeryDone; if (archerySlot < 0) archerySlot = o; } break;
                case 2:  if (!uc) { ++siegeDone; if (siegeSlot < 0) siegeSlot = o; } break;
                case 0:  ++lumberN; if (!uc && lumberSlot < 0) lumberSlot = o; break;
                case 1:  ++miningN; if (!uc && miningSlot < 0) miningSlot = o; break;
                case 5:  ++millN; break;
                case 12: ++outpostN; break;
                default: break;
            }
        }
        int myTc = hdr[8];                  // 我方 TC 打包格（-1 = 已毁，离输不远）
        int enemyTc = ehdr[8];              // 敌方 TC 打包格（-1 = 已毁，即胜）

        // ===== 军事模块：回防优先于出门 =====
        boolean defending = false;
        if (myTc >= 0) {
            int tcx = myTc >>> 8, tcy = myTc & 0xFF;
            int bestD2 = Integer.MAX_VALUE, bestTile = -1;
            for (int i = 0; i < eunits; ++i) {
                int o = i << 3;
                if ((eslots[o + 3] & 0xFF) < 2) {
                    continue;               // 敌方村民路过不触发回防
                }
                int t = eslots[o + 0] & 0xFFFF;
                int dx = (t >>> 8) - tcx, dy = (t & 0xFF) - tcy;
                int d2 = dx * dx + dy * dy;
                if (d2 < bestD2) {
                    bestD2 = d2;
                    bestTile = t;
                }
            }
            if (bestTile >= 0 && bestD2 <= DEFEND_D2 && milCount > 0) {
                defending = true;
                this.attackMode = false;    // 家门口被打：取消进攻，全军回防
                if (game.tickCount - this.lastDefendOrder >= DEFEND_REISSUE) {
                    this.lastDefendOrder = game.tickCount;
                    game.selectUnits(0, -1);
                    game.orderMove(0, bestTile >>> 8, bestTile & 0xFF);
                    game.clearSelection();
                    System.out.println("[ai] DEFEND invader at " + (bestTile >>> 8) + ","
                        + (bestTile & 0xFF) + " d2=" + bestD2 + " mil=" + milCount);
                }
            }
        }
        if (!defending && enemyTc >= 0 && milCount > 0) {
            // 进攻条件：兵力够数 且（价值 ≥ 120 满编 / 2× 碾压）；僵持太久则降门槛强行
            // 出门（Easy 敌方 army<200 永不进攻，我们不主动就是无限 STALL）。
            // 注意别半吊子出门：我方一逼近敌基，敌方防御模式会把 87.5% 兵力反手
            // 拍向我方 TC——攻击波不够厚就是替对面开进攻开关（seed 1002 教训）。
            boolean force = game.tickCount > FORCE_ATTACK_TICK && milCount >= FORCE_ATTACK_UNITS;
            if (!this.attackMode && (force || (milCount >= MIN_ATTACK_UNITS
                    && (milVal >= ABS_ATTACK_VALUE || milVal >= enemyMilVal * 2)))) {
                this.attackMode = true;
                this.lastAttackOrder = -100000;
                this.attackBestD2 = Integer.MAX_VALUE;
                System.out.println("[ai] ATTACK enemy TC " + (enemyTc >>> 8) + "," + (enemyTc & 0xFF)
                    + " mil=" + milCount + "(val " + milVal + ") vs enemy " + enemyMilCount
                    + "(val " + enemyMilVal + ") t=" + game.tickCount);
            }
            if (this.attackMode && milCount <= RETREAT_LEFT) {
                this.attackMode = false;    // 打光了：撤回 TC 重整
                if (myTc >= 0) {
                    game.selectUnits(0, -1);
                    game.orderMove(0, myTc >>> 8, myTc & 0xFF);
                    game.clearSelection();
                }
                System.out.println("[ai] RETREAT, mil left " + milCount + " t=" + game.tickCount);
            }
            if (this.attackMode && game.tickCount - this.lastAttackOrder >= ATTACK_REISSUE) {
                this.lastAttackOrder = game.tickCount;
                game.selectUnits(0, -1);
                game.orderMove(0, enemyTc >>> 8, enemyTc & 0xFF);
                game.clearSelection();
            }
            // 进攻停滞观测（只打日志，重发已由上面周期覆盖）
            if (this.attackMode) {
                int etx = enemyTc >>> 8, ety = enemyTc & 0xFF;
                int best = Integer.MAX_VALUE;
                for (int i = 0; i < units; ++i) {
                    int o = i << 3;
                    if ((slots[o + 3] & 0xFF) < 2) {
                        continue;
                    }
                    int t = slots[o + 0] & 0xFFFF;
                    int dx = (t >>> 8) - etx, dy = (t & 0xFF) - ety;
                    best = Math.min(best, dx * dx + dy * dy);
                }
                if (best < this.attackBestD2) {
                    this.attackBestD2 = best;
                    this.attackBestTick = game.tickCount;
                } else if (game.tickCount - this.attackBestTick > 1200) {
                    System.out.println("[ai] attack STALLED? bestD2=" + this.attackBestD2
                        + " no progress for 1200t, t=" + game.tickCount);
                    this.attackBestTick = game.tickCount;
                }
            }
        }

        // ===== 经济模块 =====
        if (myTc >= 0) {
            // 1) 空闲村民派工：先补木到目标数，再补金；都够了一律去木
            int woodTarget = hdr[0] >= 1 ? WOOD_WORKERS_LATE : WOOD_WORKERS_EARLY;
            int goldTarget = hdr[0] >= 1 ? GOLD_WORKERS_LATE : GOLD_WORKERS_EARLY;
            for (int k = 0; k < idleN; ++k) {
                int o = idleVill[k] << 3;
                int kind = woodW < woodTarget ? 1 : (goldW < goldTarget ? 2 : 1);
                // 用自扫的 findResource 而非引擎 findNearbyResource：后者的结果缓存
                // （var_short_a/b/c）双方共享，玩家侧调用会把敌方村民引到我们采的
                // 森林，引发村民斗殴（2026-09-02 实测：双方村民在 (43,40) 互殴致死）。
                int r = findResource(game, slots[o + 0] & 0xFFFF, kind, game.tickCount);
                if (r < 0) {                    // 该种资源无可达格，换另一种
                    kind = kind == 1 ? 2 : 1;
                    r = findResource(game, slots[o + 0] & 0xFFFF, kind, game.tickCount);
                }
                if (r >= 0 && r != (slots[o + 0] & 0xFFFF)) {
                    // 与 orderMove 的单单位版逐字同语义（清动作、清步进累加器）
                    slots[o + 1] = slots[o + 0];
                    slots[o + 2] = (short) r;
                    slots[o + 7] = 0;
                    slots[o + 3] = (short) (slots[o + 3] & 0xFF);
                    if (kind == 1) {
                        ++woodW;
                    } else {
                        ++goldW;
                    }
                    System.out.println("[ai] assign villager " + idleVill[k] + " -> "
                        + (kind == 1 ? "wood" : "gold") + " " + (r >>> 8) + "," + (r & 0xFF));
                }
            }
            boolean popRoom = hdr[2] + hdr[49] < hdr[3] && hdr[2] + hdr[49] < 26;
            // 2) 补村民（房屋训练；cap = hdr[75]，已建 hdr[57] + 在训 hdr[66]）
            if (popRoom && vills + hdr[66] < VILL_TARGET && hdr[57] + hdr[66] < hdr[75]
                    && houseSlot >= 0 && queueLen(recs, houseSlot) < 1 && game.canAfford(0, 0, 0)) {
                if (game.queueUnitTraining(0, 0) >= 0) {
                    System.out.println("[ai] queue villager, vills=" + vills + " t=" + game.tickCount);
                }
            }
            // 3) 科技：封建（兵营建成后）；封建后顺手研究采集科技（木材 +5/趟、金 +3/趟）
            if (hdr[0] == 0 && barracksDone > 0 && tcSlot >= 0 && game.canAfford(0, 2, 21)) {
                if (game.tryResearch(0, tcSlot, 21)) {
                    System.out.println("[ai] research FEUDAL t=" + game.tickCount);
                } else if (!this.feudalTried) {
                    this.feudalTried = true;
                    System.out.println("[ai] feudal research refused t=" + game.tickCount);
                }
            }
            if (hdr[0] >= 1) {
                if (lumberSlot >= 0 && game.canAfford(0, 2, 3) && game.tryResearch(0, lumberSlot, 3)) {
                    System.out.println("[ai] research Double-Bit Axe t=" + game.tickCount);
                }
                if (miningSlot >= 0 && game.canAfford(0, 2, 5) && game.tryResearch(0, miningSlot, 5)) {
                    System.out.println("[ai] research Gold Mining t=" + game.tickCount);
                }
            }
            // 4) 建筑（一次一座，自动成型无需施工；找位失败/被占就下个决策再试；
            //    资源点不存在时置 noXxxRes 永久跳过该矿场，否则链条卡死——seed 1004
            //    全场无可达金矿，miningN==0 永远成立，后面的射箭场/磨坊/攻城全被堵死）
            if (!anyUC) {
                int need = -1, anchor = myTc;
                if (((hdr[2] + hdr[49] >= hdr[3] - 1 && hdr[5] >= 5) || (houseN == 0 && hdr[5] >= 40)
                        || (hdr[0] >= 1 && hdr[3] < 25 && hdr[5] >= 100)) && houseN < 4) {
                    need = 11;                                   // 房屋：人口紧张/开局抢节奏/封建后富余补人口（菜单上限 4 座）
                } else if (lumberN == 0 && !this.noWoodRes && hdr[5] >= 25) {
                    need = 0;
                    anchor = this.findResource(game, myTc, 1, game.tickCount);   // 伐木场贴着树林放
                    if (anchor < 0) {
                        this.noWoodRes = true;
                    }
                } else if (barracksDone == 0 && !hasUC(recs, hdr[4], 10) && hdr[5] >= 35 && hdr[7] >= 15) {
                    need = 10;
                } else if (miningN == 0 && !this.noGoldRes && hdr[5] >= 30) {
                    need = 1;
                    anchor = this.findResource(game, myTc, 2, game.tickCount);   // 采矿场贴着金矿放
                    if (anchor < 0) {
                        this.noGoldRes = true;
                    }
                } else if (hdr[0] >= 1 && archeryDone == 0 && !hasUC(recs, hdr[4], 7)
                        && hdr[5] >= 45 && hdr[7] >= 20) {
                    need = 7;
                } else if (hdr[0] >= 1 && millN == 0 && hdr[5] >= 55 && hdr[7] >= 25) {
                    need = 5;                                    // 磨坊：建成即全员训练速度 +50%
                } else if (hdr[0] >= 1 && siegeDone == 0 && !hasUC(recs, hdr[4], 2)
                        && hdr[5] >= 70 && hdr[7] >= 35) {
                    need = 2;
                } else if (siegeDone > 0 && outpostN < 2 && !hasUC(recs, hdr[4], 12)
                        && hdr[5] >= 60 && hdr[6] >= 15 && hdr[7] >= 35) {
                    need = 12;                                   // 哨塔×2：吸收敌方反扑/全压波次
                }
                if (need >= 0 && anchor >= 0) {
                    int spot = game.findAiBuildSpot(anchor);
                    int tx = spot >>> 8, ty = spot & 0xFF;
                    // findAiBuildSpot 找不到会原样返回锚点（被占），放前自查空格
                    if (spot != anchor && tx < 64 && ty < 64
                            && (game.mapTiles[tx + (ty << 6)] & 0xFFF) == 0) {
                        int rc = game.a(0, need, tx, ty, 0x40000000, true);
                        System.out.println("[ai] build type=" + need + " at " + tx + "," + ty
                            + " rc=" + rc + " res=" + hdr[5] + "/" + hdr[6] + "/" + hdr[7]
                            + " t=" + game.tickCount);
                    }
                }
            }
            // 5) 军事生产：每种生产建筑的第一座并行排队（queueUnitTraining 只认首座
            //    同类型建筑），各维持 ≤2 在训。产兵扣款发生在产出时，排多不亏。
            if (popRoom && barracksDone > 0 && vills >= 3) {
                boolean feudalDone = hdr[0] >= 1;
                int meleeType = feudalDone ? 3 : 2;             // 封建后自动剑士
                if (canTrain(hdr, meleeType) && queueLen(recs, barracksSlot) < 2
                        && hdr[5] >= 15 && (feudalDone || hdr[6] >= 25)
                        && game.canAfford(0, 0, meleeType)) {
                    game.queueUnitTraining(0, meleeType);
                }
                if (archeryDone > 0 && archerySlot >= 0 && canTrain(hdr, 4)
                        && queueLen(recs, archerySlot) < 2 && hdr[5] >= 25
                        && game.canAfford(0, 0, 4)) {
                    game.queueUnitTraining(0, 4);
                }
                if (siegeDone > 0 && siegeSlot >= 0 && canTrain(hdr, 8)
                        && queueLen(recs, siegeSlot) < 2 && hdr[5] >= 40
                        && game.canAfford(0, 0, 8)) {
                    game.queueUnitTraining(0, 8);
                }
            }
        }

        // ===== 摘要日志 =====
        if (game.tickCount - this.lastLog >= LOG_EVERY) {
            this.lastLog = game.tickCount;
            System.out.println("[ai] t=" + game.tickCount + " res=" + hdr[5] + "/" + hdr[6] + "/" + hdr[7]
                + " pop=" + hdr[2] + "+" + hdr[49] + "/" + hdr[3]
                + " vills=" + vills + "(w" + woodW + " g" + goldW + ")"
                + " mil=" + milCount + "(val " + milVal + ")"
                + " enemy=" + ehdr[2] + "u " + ehdr[4] + "b mil=" + enemyMilCount + "(val " + enemyMilVal + ")"
                + " mode=" + (defending ? "DEFEND" : this.attackMode ? "ATTACK" : "eco"));
        }
    }

    /** 全图扫描找最近的指定 kind 资源格（打包 tx<<8|ty；找不到返回 -1）。
     *  不用引擎的 findNearbyResource：它的结果缓存（var_short_a/b/c）双方共享，
     *  玩家侧调用会污染缓存、把敌方村民引到我们采的资源点。固定扫描序保确定性。
     *  过滤：① 卡死拉黑表（resBlacklistUntil，villager 300 tick 未移动时写入）；
     *  ② 无可走邻格的资源格直接跳过（采集必须站到邻格——seed 1003 边缘树
     *  (63,18) 四邻全是树，村民永远走不到位，经济停摆整局的教训）。 */
    private int findResource(c game, int fromPacked, int kind, int tick) {
        int fx = fromPacked >>> 8, fy = fromPacked & 0xFF;
        int best = -1, bestD2 = Integer.MAX_VALUE;
        for (int ty = 0; ty < 64; ++ty) {
            for (int tx = 0; tx < 64; ++tx) {
                int idx = tx + (ty << 6);
                int t = game.mapTiles[idx] & 0xFFF;
                if ((t & 0x300) != 0x300 || (t & 3) != kind) {
                    continue;
                }
                if (this.resBlacklistUntil[idx] > tick || !hasWalkableNeighbor(game, tx, ty)) {
                    continue;
                }
                int dx = tx - fx, dy = ty - fy;
                int d2 = dx * dx + dy * dy;
                if (d2 < bestD2) {
                    bestD2 = d2;
                    best = tx << 8 | ty;
                }
            }
        }
        return best;
    }

    /** 该格 8 邻格里是否有可站立格（空格或单位占用格——单位是动态障碍）。
     *  建筑(0x100)与地形/虚空(0x300)当墙，与 BFS 障碍语义一致。 */
    private static boolean hasWalkableNeighbor(c game, int tx, int ty) {
        for (int dy = -1; dy <= 1; ++dy) {
            for (int dx = -1; dx <= 1; ++dx) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                int nx = tx + dx, ny = ty + dy;
                if (nx < 0 || ny < 0 || nx >= 64 || ny >= 64) {
                    continue;
                }
                if ((game.mapTiles[nx + (ny << 6)] & 0x300) == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 建筑在训队列长度（rec[+2] 位 16..23；tryResearch 的 0x10000 只出现在研究建筑）。 */
    private static int queueLen(int[] recs, int slot) {
        return (recs[slot + 2] >> 16) & 0xFF;
    }

    /** 某类型建筑是否有在建的（用于"同类型一次只放一座"）。 */
    private static boolean hasUC(int[] recs, int bcount, int type) {
        for (int i = 0; i < bcount; ++i) {
            int o = i << 2;
            if ((recs[o + 3] & 0xFF) == type && (recs[o + 2] & 0x40000000) != 0) {
                return true;
            }
        }
        return false;
    }

    /** 兵种训练上限检查（与 tryTrainAiUnit 同源：已建 hdr[57+t'] + 在训 hdr[66+t'] < 上限 hdr[75+t']）。 */
    private static boolean canTrain(int[] hdr, int type) {
        int t = type - 1;
        if (t < 0) {
            t = 0;
        }
        return hdr[57 + t] + hdr[66 + t] < hdr[75 + t];
    }
}
