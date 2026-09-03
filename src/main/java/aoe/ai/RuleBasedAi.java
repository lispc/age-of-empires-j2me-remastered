package aoe.ai;

import AgeOfEmpires.c;

/**
 * 规则式玩家 AI（-Daoe.playerAi=aoe.ai.RuleBasedAi）。随机图（gameMode=0）
 * Easy/Medium 通用。Medium 成绩：三区间 n=10 合计 25/29 决胜（2026-09-03，
 * seeds 1000+/1010+/2000+，-b 开 BFS）；Easy 4/4 + 1 僵持（退化图）。
 *
 * 战略（针对 Medium：敌方 3.07× 采集 + aiAttackThreshold=60；我方 4 村民 +
 * pop 25 硬顶，拼经济必输）：**塔防吸收 all-in → 反击拆敌 TC**（随机图拆敌
 * TC 即胜，i() case 9；败北 = 我方 TC 毁 或 0 单位+0 建筑）。
 *
 * 关键机制依据（docs/game-mechanics.md / docs/unit-stats.md / 源码复核）：
 * - 塔(12)完工注册 projectileTable 成为射击建筑；塔攻 = hdr[46]<<4/甲
 *   （基值 2 → 32/甲），塔甲 = hdr[45]（基值 10）；敌军近战/远程索敌时
 *   **优先打塔**（void_b 遇 type12 立即锁定）——塔是天然肉盾。
 * - 敌 all-in 触发：敌军值 ≥60 且 > 我军值(hdr[55]，**塔完工也加值**)×1.25
 *   时 75% 兵力扑我 TC；我兵近敌 TC 6 格（警戒半径² 36）触发防御模式，
 *   87.5% 反扑我 TC（tickAi，agent-operations §5.3）。
 * - **投石机（t8）产自铁匠铺（type 6）不是攻城工坊**（queueUnitTraining case 8
 *   → 建筑 6，Vineflower/CFR 一致；v1 在攻城工坊排队永远静默失败）。冲车(t7)
 *   才产自攻城工坊(2)。征服者(t9) 产自城堡(3)。
 * - 铁匠铺攻防升级：封建 Forging(id4)/Scale Mail(id8) 各 +1，城堡再 +1，
 *   帝国再 +1（不占人口，人口硬顶 25 下的免费战力）。
 * - 塔升级在塔上研究：Watch Tower(id13，封建) 塔甲 10→15、索敌 16→25；
 *   Guard Tower(id17，城堡) 攻 3/甲 20/索敌 36。
 * - **信息弹窗（z=62 升时代/z=70 首建）冻结世界模拟但 tickCount 照走**——
 *   headless 无人关窗 = 永久冻结。AI 在 ss==2 时自按 -7 关窗（hook 在 onPaint
 *   帧首、不受 screenState 门控，已核实）。
 * - 军事群令 selectUnits(0,-1) 只选军事（type≥2)，村民不受影响；村民单体
 *   改派直接写 slot（引擎 tickAi 同做法）。采集→交存→返矿全自动（§10 定论，
 *   闲置只在资源耗尽时），只需处理真闲置。
 *
 * 确定性：只按 tickCount 节流，无墙钟、无随机数（也不碰游戏 nextRandomInt）。
 */
public final class RuleBasedAi implements PlayerAi {

    // —— 节奏 ——
    private static final int DECIDE_EVERY = 8;       // 决策周期（tick）
    private static final int ATTACK_REISSUE = 150;   // 进攻群令重发周期
    private static final int DEFEND_REISSUE = 48;    // 回防群令重发周期
    private static final int STANCE_REISSUE = 240;   // 驻防集结重发周期
    private static final int LOG_EVERY = 500;

    // —— 军事参数 ——
    private static final int DEFEND_D2 = 100;        // 回防交战半径²（10 格内才出击——v2 用 12 格
                                                     // 被敌放风筝拉出塔圈磨死，seed 1010/1013/1015 教训）
    private static final int RAID_SCAN_D2 = 144;     // 反扑侦测半径²（进攻中止判定用）
    private static final int FLEE_D2 = 49;           // 村民逃跑触发半径²（7 格）
    private static final int MIN_ATTACK_UNITS = 8;   // 反击最少兵力
    private static final int OVERWHELM_VAL = 130;    // 碾压出门的军队价值（Easy 不主动来攻，靠这条收尾）
    private static final int DESPERATE_TICK = 25000; // 僵持兜底
    private static final int RETREAT_LEFT = 4;       // 进攻中兵力 ≤ 此值 → 撤军
    private static final int RAIDERS_IGNORE = 3;     // 进攻中小股（≤此数）骚扰靠塔挡，不撤军
    private static final int BAIT_MIN_TICK = 15000;  // 互瞪僵局诱敌：最早此时刻
    private static final int BAIT_QUIET = 4000;      // 无接触静默期门槛
    private static final int BAIT_RETRY = 3000;      // 诱饵阵亡/无响应后重投间隔

    // 塔位：我方 TC → 敌 TC 走廊上的阶梯（距我 TC 的切比雪夫格数，r26 验证的最优汇率
    // 防术——塔修敌行军线上而非只堆家门口；敌 all-in 目标是我 TC，必走这条走廊）
    private static final int[] TOWER_DIST = {4, 6, 9, 12, 16};

    private int nextDecide;
    private int lastAttackOrder = -100000;
    private int lastDefendOrder = -100000;
    private int lastStanceOrder = -100000;
    private int lastLog;
    private boolean attackMode;
    private int attackBestD2 = Integer.MAX_VALUE;
    private int attackBestTick;
    private int attackCooldownUntil;                 // 撤军/停滞后的进攻冷却
    private int enemyMilPeak;                        // 敌军峰值（"主力被歼"判定基准）
    private int lastContactTick;                     // 最后一次敌兵进我警戒圈（诱敌判定用）
    private int lastBaitTick = -100000;              // 上次投诱饵
    // 残血撤下站桩回血（0.5 HP/tick，站桩即回——agent-operations §5.1；敌 3.07× 产量
    // 的消耗战里，保一个老兵=省 10-20 金的替换费）。群令会把回血单位召回战线，
    // 靠每决策重泊覆盖。槽位死亡压缩会错位，最多误泊一次，无害。
    private final int[] healUntil = new int[26];
    // 村民卡死检测（行军中 300 tick 没挪窝 → 目标拉黑 3000 tick 重派）
    private final int[] villLastPos = new int[26];
    private final int[] villLastTick = new int[26];
    private final int[] resBlacklistUntil = new int[4096];
    private boolean noWoodRes;
    private boolean noGoldRes;
    private boolean noStoneRes;

    @Override
    public void tick(c game) {
        if (game.tickCount < this.nextDecide) {
            return;
        }
        this.nextDecide = game.tickCount + DECIDE_EVERY;
        // 模拟只在 onPaint default 分支跑（screenState 2/4/5/7/9..14 暂停），AI 同步休眠。
        // 例外：ss==2 是弹窗态——headless 无人按键时信息弹窗（升时代 z=62/新建筑 z=70）
        // 会永久冻结模拟（tickCount 照走但世界停摆，seed 1010 实测冻 27 万 tick=STALL）。
        // AI 侧绕行：替玩家按 -7 关窗（手册 §4.3 弹窗关闭键；终局 z=98 由 exitOnResult
        // 先退出，轮不到这里）。
        int ss = game.screenState;
        if (ss == 2) {
            game.onKeyPress(-7);
            return;
        }
        if (ss == 4 || ss == 5 || ss == 7 || (ss >= 9 && ss <= 14)) {
            return;
        }
        int[] hdr = game.playerUnitHeaders[0];
        if (hdr[2] <= 0 && hdr[4] <= 0) {
            return;     // 未进任务或已全灭
        }

        // ===== 态势扫描 =====
        short[] slots = game.playerUnitSlots[0];
        int vills = 0, milCount = 0, milVal = 0;
        int woodW = 0, goldW = 0, stoneW = 0;
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
                kind = (slots[o + 7] & 0xF0) >> 4;
            } else {
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
            } else if (kind == 3) {
                ++stoneW;
            }
            int pos = slots[o + 0] & 0xFFFF;
            int tgt = slots[o + 2] & 0xFFFF;
            if (action == 0 && pos != tgt) {
                if (pos == this.villLastPos[i] && game.tickCount - this.villLastTick[i] > 300) {
                    this.resBlacklistUntil[(tgt >>> 8) + ((tgt & 0xFF) << 6)] = game.tickCount + 3000;
                    this.villLastTick[i] = game.tickCount;
                    if (idleN < 26) {
                        idleVill[idleN++] = i;
                    }
                    System.out.println("[ai] villager " + i + " STUCK at " + (pos >>> 8) + ","
                        + (pos & 0xFF) + " blacklisted, t=" + game.tickCount);
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
        if (enemyMilCount > this.enemyMilPeak) {
            this.enemyMilPeak = enemyMilCount;
        }
        // 敌塔计入"防御军值"（v3：seed 1010 总攻 val 142 vs 72 仍败——没算敌 5 座塔的
        // 火力；塔完工加 hdr[55] 的值 = 塔甲 hdr[45] + 塔攻 hdr[46]）
        int[] erecs = game.var_int_arr_arr_b[1];
        int enemyTowerVal = 0;
        for (int i = 0; i < ehdr[4]; ++i) {
            int o = i << 2;
            int bt = erecs[o + 3] & 0xFF;
            if (bt >= 12 && bt <= 15 && (erecs[o + 2] & 0x40000000) == 0) {
                enemyTowerVal += ehdr[45] + ehdr[46];
            }
        }
        int enemyDefVal = enemyMilVal + enemyTowerVal;
        // 建筑扫描
        int[] recs = game.var_int_arr_arr_b[0];
        int houseN = 0, barracksDone = 0, archeryDone = 0, smithDone = 0;
        int lumberN = 0, miningN = 0, millN = 0, towerN = 0, stableDone = 0, siegeDone = 0;
        int tcSlot = -1, houseSlot = -1, barracksSlot = -1, archerySlot = -1, smithSlot = -1;
        int lumberSlot = -1, miningSlot = -1, towerSlot = -1, stableSlot = -1, siegeSlot = -1;
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
                case 6:  if (!uc) { ++smithDone; if (smithSlot < 0) smithSlot = o; } break;
                case 0:  ++lumberN; if (!uc && lumberSlot < 0) lumberSlot = o; break;
                case 1:  ++miningN; if (!uc && miningSlot < 0) miningSlot = o; break;
                case 5:  ++millN; break;
                case 12: ++towerN; if (!uc && towerSlot < 0) towerSlot = o; break;
                case 8:  if (!uc) { ++stableDone; if (stableSlot < 0) stableSlot = o; } break;
                case 2:  if (!uc) { ++siegeDone; if (siegeSlot < 0) siegeSlot = o; } break;
                default: break;
            }
        }
        int myTc = hdr[8];
        int enemyTc = ehdr[8];

        // ===== 军事模块 =====
        // 1) 找离我方 TC 最近的敌军事单位（回防目标）+ 最近的敌投石机（t8 优先点杀，
        //    远程拆塔克星，agent-operations §5.1/r29）
        int tcx = myTc >= 0 ? myTc >>> 8 : 0, tcy = myTc >= 0 ? myTc & 0xFF : 0;
        int invaderTile = -1, invaderD2 = Integer.MAX_VALUE, invaderN = 0;
        int mangonelTile = -1, mangonelD2 = Integer.MAX_VALUE;
        if (myTc >= 0) {
            for (int i = 0; i < eunits; ++i) {
                int o = i << 3;
                int type = eslots[o + 3] & 0xFF;
                if (type < 2) {
                    continue;
                }
                int t = eslots[o + 0] & 0xFFFF;
                int dx = (t >>> 8) - tcx, dy = (t & 0xFF) - tcy;
                int d2 = dx * dx + dy * dy;
                if (d2 <= RAID_SCAN_D2) {
                    ++invaderN;
                }
                if (d2 < invaderD2) {
                    invaderD2 = d2;
                    invaderTile = t;
                }
                if (type == 8 && d2 < mangonelD2) {
                    mangonelD2 = d2;
                    mangonelTile = t;
                }
            }
        }
        boolean threat = invaderTile >= 0 && invaderD2 <= DEFEND_D2;
        if (invaderN > 0) {
            this.lastContactTick = game.tickCount;
        }
        if (threat && game.tickCount - this.lastDefendOrder >= DEFEND_REISSUE) {
            this.lastDefendOrder = game.tickCount;
            // 近战压向最近入侵者；远程（弓/投石机）停在入侵者朝我 TC 3 格处开火——
            // v7 前远程被群令送进近战圈白死（seed 1019 弓兵 3 连阵亡实锤）。
            // 敌 t8 投石机在 20 格内时远程改点杀 t8（远程拆塔克星，§5.1/r29）。
            // 顺序：先 -1 群令，再按兵种覆盖（selectUnits(type) 重选后 orderMove 只动该兵种）。
            game.selectUnits(0, -1);
            game.orderMove(0, invaderTile >>> 8, invaderTile & 0xFF);
            int rangedTile;
            if (mangonelTile >= 0 && mangonelD2 <= 400) {
                rangedTile = mangonelTile;
            } else {
                rangedTile = myTc >= 0 ? stanceTile(invaderTile, myTc, 3) : invaderTile;
            }
            game.selectUnits(0, 4);
            game.orderMove(0, rangedTile >>> 8, rangedTile & 0xFF);
            game.selectUnits(0, 8);
            game.orderMove(0, rangedTile >>> 8, rangedTile & 0xFF);
            game.clearSelection();
            if (invaderD2 <= 64) {
                System.out.println("[ai] DEFEND invader " + invaderN + " at " + (invaderTile >>> 8) + ","
                    + (invaderTile & 0xFF) + " t=" + game.tickCount);
            }
        }
        // 进攻中遇到小股反扑不回家（塔顶着），大股才撤（敌防御模式 87.5% 反扑我 TC 的应对）
        if (this.attackMode && threat && invaderN > RAIDERS_IGNORE) {
            this.attackMode = false;
            this.attackCooldownUntil = game.tickCount + 800;
            System.out.println("[ai] attack ABORTED, " + invaderN + " raiders home t=" + game.tickCount);
        }
        // 2) 反击/总攻判定：敌主力被歼（从峰值跌到 1/3）/ 碾压 / 僵持兜底。
        //    v1 教训（M1 报告）：没碾平敌主力就逼近敌基 = 替对面开 87.5% 反扑开关。
        //    v9：贴脸图（走廊 ≤14 格）敌兵常驻我警戒圈 → !threat 永远假 → 永不能反击
        //    （seed 1019 实锤）；closeRush 因此豁免 threat 门。进攻态下小股骚扰不回防
        //    （DEFEND 块已被 attackMode 抑制，塔顶着），大股才中止进攻。
        if (enemyTc >= 0 && myTc >= 0) {
            boolean crushed = this.enemyMilPeak >= 5 && enemyMilCount <= Math.max(2, this.enemyMilPeak / 3);
            boolean overwhelm = milVal >= OVERWHELM_VAL && milVal * 3 >= enemyDefVal * 4;
            boolean desperate = game.tickCount > DESPERATE_TICK && milCount >= MIN_ATTACK_UNITS
                && milVal >= enemyDefVal;
            // 贴脸图闪击（v8：敌我 TC ≤14 格时消耗战必输——敌 3.07× 经济 + 8 格补给线，
            // 拖=被三倍产量磨死，seed 1013/1015/1019 连续实证）：兵力不劣就走，
            // 家里靠塔接敌防御模式的反扑残兵。
            boolean closeRush = corridorLen(myTc, enemyTc) <= 14 && milCount >= 7
                && milVal >= enemyMilVal;
            if (!this.attackMode && !threat && game.tickCount >= this.attackCooldownUntil
                    && ((crushed || closeRush) && milCount >= MIN_ATTACK_UNITS - 1 && milVal >= enemyMilVal
                        || overwhelm || desperate)) {
                this.attackMode = true;
                this.lastAttackOrder = -100000;
                this.attackBestD2 = Integer.MAX_VALUE;
                System.out.println("[ai] ATTACK enemy TC " + (enemyTc >>> 8) + "," + (enemyTc & 0xFF)
                    + " mil=" + milCount + "(val " + milVal + ") vs enemy " + enemyMilCount
                    + "(val " + enemyMilVal + ", peak " + this.enemyMilPeak + ")"
                    + (crushed ? " CRUSHED" : "") + (overwhelm ? " OVERWHELM" : "")
                    + (desperate ? " DESPERATE" : "") + (closeRush ? " CLOSERUSH" : "") + " t=" + game.tickCount);
            }
            if (this.attackMode && milCount <= RETREAT_LEFT) {
                this.attackMode = false;
                this.attackCooldownUntil = game.tickCount + 1500;
                game.selectUnits(0, -1);
                game.orderMove(0, tcx, tcy);
                game.clearSelection();
                System.out.println("[ai] RETREAT, mil left " + milCount + " t=" + game.tickCount);
            }
            if (this.attackMode && game.tickCount - this.lastAttackOrder >= ATTACK_REISSUE) {
                this.lastAttackOrder = game.tickCount;
                game.selectUnits(0, -1);
                game.orderMove(0, enemyTc >>> 8, enemyTc & 0xFF);
                game.clearSelection();
            }
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
                } else if (game.tickCount - this.attackBestTick > 1500) {
                    // 进攻停滞（敌塔群/重建兵挡住）：撤回重整，攒下一波
                    this.attackMode = false;
                    this.attackCooldownUntil = game.tickCount + 2000;
                    game.selectUnits(0, -1);
                    game.orderMove(0, tcx, tcy);
                    game.clearSelection();
                    System.out.println("[ai] attack STALLED bestD2=" + this.attackBestD2
                        + ", regroup t=" + game.tickCount);
                }
            }
            // 3) 平时驻防：全军集结在 TC 朝敌方向（塔火力圈内，新兵自动归队）；
            //    近距图钳到走廊 1/3 内，别蹭进敌警戒圈（半径² 36 = 6 格）
            if (!this.attackMode && !threat && milCount > 0
                    && game.tickCount - this.lastStanceOrder >= STANCE_REISSUE) {
                this.lastStanceOrder = game.tickCount;
                int stanceDist = 3;
                if (enemyTc >= 0) {
                    int dx = (enemyTc >>> 8) - tcx, dy = (enemyTc & 0xFF) - tcy;
                    stanceDist = Math.max(2, Math.min(3, Math.max(Math.abs(dx), Math.abs(dy)) / 3));
                }
                int stance = stanceTile(myTc, enemyTc >= 0 ? enemyTc : myTc, stanceDist);
                game.selectUnits(0, -1);
                game.orderMove(0, stance >>> 8, stance & 0xFF);
                game.clearSelection();
            }
            // 4) 互瞪僵局诱敌（agent-operations §5.3 调虎离山）：敌大军蹲家不出门、
            //    我方久无接触 → 派最便宜的兵直扑敌 TC，触发敌防御模式（警戒圈 6 格）
            //    87.5% 反扑我 TC——把敌主力喂给我方塔阵后再反击。seed 1006 双方满编
            //    对峙 5.5M tick 的破解。驻防群令会把诱饵召回，靠每决策重投续上。
            if (!this.attackMode && !threat && enemyTc >= 0 && milCount >= 12
                    && enemyMilCount >= 10 && game.tickCount > BAIT_MIN_TICK
                    && game.tickCount - this.lastContactTick > BAIT_QUIET
                    && game.tickCount - this.lastBaitTick > 240) {
                int baitO = -1, baitVal = Integer.MAX_VALUE;
                for (int i = 0; i < units; ++i) {
                    int o = i << 3;
                    int type = slots[o + 3] & 0xFF;
                    if (type < 2) {
                        continue;
                    }
                    if ((slots[o + 2] & 0xFFFF) == enemyTc) {
                        baitO = -2;      // 已有诱饵在路上
                        break;
                    }
                    int v = hdr[13 + type] + hdr[23 + type];
                    if (v < baitVal) {
                        baitVal = v;
                        baitO = o;
                    }
                }
                if (baitO >= 0) {
                    slots[baitO + 1] = slots[baitO + 0];
                    slots[baitO + 2] = (short) enemyTc;
                    slots[baitO + 7] = 0;
                    slots[baitO + 3] = (short) (slots[baitO + 3] & 0xFF);
                    this.lastBaitTick = game.tickCount;
                    System.out.println("[ai] BAIT sent (val " + baitVal + ") toward enemy TC, t="
                        + game.tickCount);
                }
            }
        }

        // ===== 残血回撤回血（必须在群令之后跑，覆盖被召回的回血单位） =====
        if (myTc >= 0 && !this.attackMode) {
            int healTile = enemyTc >= 0 ? stanceTile(myTc, enemyTc, -3) : myTc;
            for (int i = 0; i < units; ++i) {
                int o = i << 3;
                if ((slots[o + 3] & 0xFF) < 2) {
                    continue;
                }
                int hp = slots[o + 4] & 0xFF;
                if (this.healUntil[i] > game.tickCount) {
                    if (hp > 220) {
                        this.healUntil[i] = 0;      // 回满归队（下个群令自然收编）
                        continue;
                    }
                    if ((slots[o + 2] & 0xFFFF) != healTile) {
                        slots[o + 1] = slots[o + 0];
                        slots[o + 2] = (short) healTile;
                        slots[o + 7] = 0;
                        slots[o + 3] = (short) (slots[o + 3] & 0xFF);
                    }
                } else if (threat && hp < 100 && milCount > 4) {
                    this.healUntil[i] = game.tickCount + 600;
                    slots[o + 1] = slots[o + 0];
                    slots[o + 2] = (short) healTile;
                    slots[o + 7] = 0;
                    slots[o + 3] = (short) (slots[o + 3] & 0xFF);
                }
            }
        }

        // ===== 村民逃命（敌兵贴脸 7 格 → 撤到 TC 另一侧；塔/军队顶前面） =====
        if (myTc >= 0) {
            for (int i = 0; i < units; ++i) {
                int o = i << 3;
                if ((slots[o + 3] & 0xFF) >= 2) {
                    continue;
                }
                int pos = slots[o + 0] & 0xFFFF;
                int px = pos >>> 8, py = pos & 0xFF;
                int near = -1, nd2 = Integer.MAX_VALUE;
                for (int j = 0; j < eunits; ++j) {
                    int eo = j << 3;
                    if ((eslots[eo + 3] & 0xFF) < 2) {
                        continue;
                    }
                    int t = eslots[eo + 0] & 0xFFFF;
                    int dx = (t >>> 8) - px, dy = (t & 0xFF) - py;
                    int d2 = dx * dx + dy * dy;
                    if (d2 < nd2) {
                        nd2 = d2;
                        near = t;
                    }
                }
                if (near < 0 || nd2 > FLEE_D2) {
                    continue;
                }
                int flee = fleeTile(game, px, py, near >>> 8, near & 0xFF, tcx, tcy);
                if (flee >= 0 && flee != pos) {
                    slots[o + 1] = slots[o + 0];
                    slots[o + 2] = (short) flee;
                    slots[o + 7] = 0;
                    slots[o + 3] = (short) (slots[o + 3] & 0xFF);
                }
            }
        }

        // ===== 经济模块 =====
        if (myTc >= 0) {
            boolean feudal = hdr[0] >= 1;
            // 村民配额（v3 起：金优先——军事单位全吃金，木头永远过剩；三败全是金=0
            // 僵尸队列饿死的）。boot 期 2木1金；封建后 1木2金，塔未满 5 座压 1 人采石；
            // 开战后（敌亮过 6+ 兵）且塔≥3 → 1木3金 全力暴兵。
            int woodTarget = 1, goldTarget = 2, stoneTarget = 0;
            if (!feudal) {
                woodTarget = 2;
                goldTarget = 1;
            } else if (miningN > 0 && towerN < 5) {
                stoneTarget = 1;
            }
            if (this.enemyMilPeak >= 6 && towerN >= 3 && miningN > 0) {
                // 开战状态（敌亮过 6+ 兵）：石料 buffered/塔满才 3 金全力暴兵；
                // 否则保 1 石工——石断供会卡死塔 4/5→磨坊→马厩链条，军值摸不到
                // OVERWHELM 门槛（Easy seed 1002 七百万 tick 对峙实证）
                stoneTarget = towerN < 5 && hdr[7] < 25 ? 1 : 0;
                goldTarget = stoneTarget == 1 ? 2 : 3;
            }
            for (int k = 0; k < idleN; ++k) {
                int o = idleVill[k] << 3;
                int kind = woodW < woodTarget ? 1
                    : (goldW < goldTarget ? 2 : (stoneW < stoneTarget ? 3 : 1));
                // 石只能交存采矿场，无矿场不派石
                if (kind == 3 && miningN == 0) {
                    kind = 1;
                }
                // 用自扫 findResource：引擎 findNearbyResource 的缓存双方共享，
                // 玩家侧调用会把敌村民引到我方资源点互殴（2026-09-02 实测）
                int r = findResource(game, slots[o + 0] & 0xFFFF, kind, game.tickCount);
                if (r < 0) {
                    kind = kind == 1 ? 2 : 1;
                    r = findResource(game, slots[o + 0] & 0xFFFF, kind, game.tickCount);
                }
                if (r >= 0 && r != (slots[o + 0] & 0xFFFF)) {
                    slots[o + 1] = slots[o + 0];
                    slots[o + 2] = (short) r;
                    slots[o + 7] = 0;
                    slots[o + 3] = (short) (slots[o + 3] & 0xFF);
                    if (kind == 1) {
                        ++woodW;
                    } else if (kind == 2) {
                        ++goldW;
                    } else {
                        ++stoneW;
                    }
                    System.out.println("[ai] assign villager " + idleVill[k] + " -> kind" + kind
                        + " " + (r >>> 8) + "," + (r & 0xFF));
                }
            }
            // 存量村民主动再平衡：配额变了在岗的不会自己换（v3——否则采石人永远上不了岗，
            // 塔 3-5 被石饿死）。每次决策最多换 1 人，打断采集一趟的代价可接受。
            if (idleN == 0) {
                int overKind = woodW > woodTarget + 1 ? 1 : (goldW > goldTarget + 1 ? 2 : 0);
                int wantKind = goldW < goldTarget ? 2
                    : (stoneW < stoneTarget && miningN > 0 ? 3 : (woodW < woodTarget ? 1 : 0));
                if (overKind != 0 && wantKind != 0 && overKind != wantKind) {
                    for (int i = 0; i < units; ++i) {
                        int o = i << 3;
                        if ((slots[o + 3] & 0xFF) >= 2 || (slots[o + 7] & 0xF) == 1) {
                            continue;
                        }
                        int action = slots[o + 7] & 0xF;
                        int kind = 0;
                        if (action == 2 || action == 3) {
                            kind = (slots[o + 7] & 0xF0) >> 4;
                        } else {
                            int tgt = slots[o + 2] & 0xFFFF;
                            int tt = game.mapTiles[(tgt >>> 8) + ((tgt & 0xFF) << 6)] & 0xFFF;
                            if ((tt & 0x300) == 0x300) {
                                kind = tt & 3;
                            }
                        }
                        if (kind != overKind) {
                            continue;
                        }
                        int r = findResource(game, slots[o + 0] & 0xFFFF, wantKind, game.tickCount);
                        if (r >= 0) {
                            slots[o + 1] = slots[o + 0];
                            slots[o + 2] = (short) r;
                            slots[o + 7] = 0;
                            slots[o + 3] = (short) (slots[o + 3] & 0xFF);
                            System.out.println("[ai] rebalance villager " + i + " kind" + overKind
                                + "->kind" + wantKind + " t=" + game.tickCount);
                        }
                        break;
                    }
                }
            }
            boolean popRoom = hdr[2] + hdr[49] < hdr[3] && hdr[2] + hdr[49] < 26;
            // 补村民（房屋训练，上限 hdr[75]=4 含开局赠送）
            if (popRoom && vills + hdr[66] < 4 && hdr[57] + hdr[66] < hdr[75]
                    && houseSlot >= 0 && queueLen(recs, houseSlot) < 1 && game.canAfford(0, 0, 0)) {
                game.queueUnitTraining(0, 0);
            }
            // 科技（tryResearch 内部已校验时代/前置/已研究；失败静默下个周期再试）。
            // v6 教训（v5 退步实锤）：GoldMining/WatchTower 必须尽早就位——它们直接
            // 决定 3-8k 首波窗口的金收入与塔生存（v5 推迟到 8k，金价 4-11 饿死暴兵，
            // 三颗 v4 胜种全翻负）。科技不是军费黑洞，持续收入才是。
            if (!feudal && barracksDone > 0 && tcSlot >= 0 && game.canAfford(0, 2, 21)) {
                if (game.tryResearch(0, tcSlot, 21)) {
                    System.out.println("[ai] research FEUDAL t=" + game.tickCount);
                }
            }
            if (feudal) {
                if (smithSlot >= 0 && game.canAfford(0, 2, 4) && game.tryResearch(0, smithSlot, 4)) {
                    System.out.println("[ai] research Forging t=" + game.tickCount);
                }
                if (towerSlot >= 0 && game.canAfford(0, 2, 13) && game.tryResearch(0, towerSlot, 13)) {
                    System.out.println("[ai] research WatchTower t=" + game.tickCount);
                }
                if (miningSlot >= 0 && game.canAfford(0, 2, 5) && game.tryResearch(0, miningSlot, 5)) {
                    System.out.println("[ai] research GoldMining t=" + game.tickCount);
                }
                if (miningSlot >= 0 && towerN < 5 && game.canAfford(0, 2, 9)
                        && game.tryResearch(0, miningSlot, 9)) {
                    System.out.println("[ai] research StoneMining t=" + game.tickCount);
                }
                if (lumberSlot >= 0 && game.canAfford(0, 2, 3) && game.tryResearch(0, lumberSlot, 3)) {
                    System.out.println("[ai] research DoubleBitAxe t=" + game.tickCount);
                }
                if (smithSlot >= 0 && hdr[6] >= 25 && game.canAfford(0, 2, 8) && game.tryResearch(0, smithSlot, 8)) {
                    System.out.println("[ai] research ScaleMail t=" + game.tickCount);
                }
                // 城堡时代（磨坊+铁匠≥2）：再 +1 攻/甲 + Guard Tower，富余才升
                if (hdr[0] == 1 && millN > 0 && smithDone > 0 && tcSlot >= 0
                        && hdr[5] >= 40 && hdr[6] >= 40 && hdr[7] >= 40
                        && game.tryResearch(0, tcSlot, 22)) {
                    System.out.println("[ai] research CASTLE t=" + game.tickCount);
                }
            }
            if (hdr[0] >= 2) {
                if (smithSlot >= 0 && game.canAfford(0, 2, 7) && game.tryResearch(0, smithSlot, 7)) {
                    System.out.println("[ai] research IronCasting t=" + game.tickCount);
                }
                if (smithSlot >= 0 && game.canAfford(0, 2, 2) && game.tryResearch(0, smithSlot, 2)) {
                    System.out.println("[ai] research ChainMail t=" + game.tickCount);
                }
                if (towerSlot >= 0 && game.canAfford(0, 2, 17) && game.tryResearch(0, towerSlot, 17)) {
                    System.out.println("[ai] research GuardTower t=" + game.tickCount);
                }
            }
            // 建筑（一次一座，自动成型；找位失败/被占下个决策再试）
            // v5/v6：交战中（敌兵 12 格内）禁放非塔建筑——seed 1019 兵临城下时连放
            // 4 座铁匠铺全被秒拆，白烧 100 木 80 石；塔例外（战中补塔=战力，且敌军
            // 索敌优先打塔，在建塔也是 255 HP 的仇恨海绵）。
            if (!anyUC) {
                int need = -1, anchor = myTc;
                if (threat) {
                    // 交战中只补塔（敌 12 格内）：seed 1019 兵临城下连放 4 座铁匠铺全被
                    // 秒拆白烧 100 木 80 石；塔例外——战中补塔=战力，且敌军索敌优先打塔，
                    // 在建塔也是 255 HP 的仇恨海绵。
                    if (towerN < TOWER_DIST.length && hdr[5] >= 30 && hdr[6] >= 8 && hdr[7] >= 18) {
                        need = 12;
                        anchor = corridorAnchor(myTc, enemyTc, TOWER_DIST[towerN]);
                    }
                } else if (houseN == 0 && hdr[5] >= 5) {
                    need = 11;                                   // 房屋 1：村民训练前置 + 人口
                } else if (lumberN == 0 && !this.noWoodRes && hdr[5] >= 20) {
                    need = 0;
                    anchor = findResource(game, myTc, 1, game.tickCount);
                    if (anchor < 0) {
                        this.noWoodRes = true;
                    }
                } else if (barracksDone == 0 && !hasUC(recs, hdr[4], 10) && hdr[5] >= 30 && hdr[7] >= 15) {
                    need = 10;                                   // 兵营：出兵 + 封建前置
                } else if (towerN + ucCount(recs, hdr[4], 12) < 1 && hdr[5] >= 28 && hdr[6] >= 8 && hdr[7] >= 20) {
                    need = 12;                                   // 走廊塔 1：敌 rush 最早 ~3.5k（近距图），塔必须先就位
                    anchor = corridorAnchor(myTc, enemyTc, TOWER_DIST[0]);
                } else if (miningN == 0 && !this.noGoldRes && hdr[5] >= 20) {
                    need = 1;
                    anchor = findResource(game, myTc, 2, game.tickCount);
                    if (anchor < 0) {
                        this.noGoldRes = true;
                    }
                } else if (towerN + ucCount(recs, hdr[4], 12) < 2 && hdr[5] >= 28 && hdr[6] >= 8 && hdr[7] >= 20) {
                    need = 12;                                   // 走廊塔 2
                    anchor = corridorAnchor(myTc, enemyTc, TOWER_DIST[1]);
                } else if (feudal && smithDone == 0 && !hasUC(recs, hdr[4], 6) && hdr[5] >= 35 && hdr[7] >= 25) {
                    need = 6;                                    // 铁匠铺：攻防升级 + 投石机（产 t8 的建筑）
                } else if (feudal && archeryDone == 0 && !hasUC(recs, hdr[4], 7) && hdr[5] >= 35 && hdr[7] >= 15) {
                    need = 7;                                    // 射箭场：弓兵反制敌投石机
                } else if (towerN < TOWER_DIST.length && !hasUC(recs, hdr[4], 12)
                        && hdr[5] >= 30 && hdr[6] >= 8 && hdr[7] >= 18) {
                    need = 12;                                   // 走廊塔 3-5：阶梯前推（v3 提到房屋/磨坊前，
                    anchor = corridorAnchor(myTc, enemyTc, TOWER_DIST[towerN]); // 塔是抗消耗主力；v6 降门槛保战中补塔）
                } else if (houseN < 4 && (hdr[2] + hdr[49] >= hdr[3] - 1 || hdr[5] >= 80)) {
                    need = 11;                                   // 补人口到 25 硬顶（上限 4 座）
                } else if (feudal && millN == 0 && hdr[5] >= 30 && hdr[7] >= 15) {
                    need = 5;                                    // 磨坊：全员训练 +50%
                } else if (feudal && stableDone == 0 && !hasUC(recs, hdr[4], 8) && hdr[5] >= 40 && hdr[7] >= 20) {
                    need = 8;                                    // 马厩：侦察/骑兵，兵种容量 +4（破 val 天花板）
                } else if (feudal && siegeDone == 0 && !hasUC(recs, hdr[4], 2) && hdr[5] >= 40 && hdr[7] >= 30) {
                    need = 2;                                    // 攻城工坊：冲车，兵种容量 +3
                }
                if (need >= 0 && anchor >= 0) {
                    int spot = game.findAiBuildSpot(anchor);
                    int tx = spot >>> 8, ty = spot & 0xFF;
                    // findAiBuildSpot 找不到会原样返回锚点；只认空格
                    if (tx < 64 && ty < 64 && (game.mapTiles[tx + (ty << 6)] & 0xFFF) == 0) {
                        int rc = game.a(0, need, tx, ty, 0x40000000, true);
                        System.out.println("[ai] build type=" + need + " at " + tx + "," + ty
                            + " rc=" + rc + " res=" + hdr[5] + "/" + hdr[6] + "/" + hdr[7]
                            + " t=" + game.tickCount);
                    }
                }
            }
            // 军事生产：各建筑并行排队 ≤2（产兵扣款在产出时，排多不亏只占人口名额）。
            // 投石机 20 金/台，金 <40 时让位给剑士/弓兵（队列不清，产出时 canAfford 卡）。
            if (popRoom && barracksDone > 0 && vills >= 3) {
                int meleeType = feudal ? 3 : 2;
                if (canTrain(hdr, meleeType) && queueLen(recs, barracksSlot) < 2
                        && hdr[5] >= 15 && (feudal || hdr[6] >= 25)
                        && game.canAfford(0, 0, meleeType)) {
                    game.queueUnitTraining(0, meleeType);
                }
                if (archeryDone > 0 && archerySlot >= 0 && canTrain(hdr, 4)
                        && queueLen(recs, archerySlot) < 2 && hdr[5] >= 25
                        && game.canAfford(0, 0, 4)) {
                    game.queueUnitTraining(0, 4);
                }
                if (smithDone > 0 && smithSlot >= 0 && canTrain(hdr, 8)
                        && queueLen(recs, smithSlot) < 1 && hdr[5] >= 40 && hdr[6] >= 40
                        && game.canAfford(0, 0, 8)) {
                    game.queueUnitTraining(0, 8);    // 投石机产自铁匠铺（case 8 → 建筑 6）
                }
                if (stableDone > 0 && stableSlot >= 0 && canTrain(hdr, 5)
                        && queueLen(recs, stableSlot) < 2 && hdr[5] >= 25 && hdr[6] >= 30
                        && game.canAfford(0, 0, 5)) {
                    game.queueUnitTraining(0, 5);    // 封建产侦察，城堡自动转骑兵（j() case 8）
                }
                if (siegeDone > 0 && siegeSlot >= 0 && canTrain(hdr, 7)
                        && queueLen(recs, siegeSlot) < 1 && hdr[5] >= 40 && hdr[6] >= 60
                        && game.canAfford(0, 0, 7)) {
                    game.queueUnitTraining(0, 7);    // 冲车产自攻城工坊（case 7 → 建筑 2）
                }
            }
        }

        // ===== 摘要日志 =====
        if (game.tickCount - this.lastLog >= LOG_EVERY) {
            this.lastLog = game.tickCount;
            System.out.println("[ai] t=" + game.tickCount + " res=" + hdr[5] + "/" + hdr[6] + "/" + hdr[7]
                + " pop=" + hdr[2] + "+" + hdr[49] + "/" + hdr[3]
                + " vills=" + vills + "(w" + woodW + " g" + goldW + " s" + stoneW + ")"
                + " mil=" + milCount + "(val " + milVal + ")"
                + " enemy=" + ehdr[2] + "u " + ehdr[4] + "b mil=" + enemyMilCount
                + "(val " + enemyMilVal + ", peak " + this.enemyMilPeak + ")"
                + " towers=" + towerN
                + " mode=" + (threat ? "DEFEND" : this.attackMode ? "ATTACK" : "eco"));
        }
    }

    /** TC 朝敌 TC 方向 dist 格（切比雪夫）的驻防/塔位锚点。 */
    private static int stanceTile(int fromPacked, int toPacked, int dist) {
        int fx = fromPacked >>> 8, fy = fromPacked & 0xFF;
        int dx = (toPacked >>> 8) - fx, dy = (toPacked & 0xFF) - fy;
        int m = Math.max(Math.abs(dx), Math.abs(dy));
        if (m == 0) {
            return fromPacked;
        }
        int ax = fx + dx * dist / m, ay = fy + dy * dist / m;
        ax = Math.max(1, Math.min(62, ax));
        ay = Math.max(1, Math.min(62, ay));
        return ax << 8 | ay;
    }

    private static int corridorAnchor(int myTc, int enemyTc, int dist) {
        if (enemyTc < 0) {
            return myTc;
        }
        // 近距图（贴脸出生）钳制：塔/驻防点不越过走廊 60%，避免建筑压进敌警戒圈
        // （警戒半径² 36，触发 87.5% 反扑）和白送在建塔。seed 1019 敌我相距 ~10 格。
        return stanceTile(myTc, enemyTc, Math.min(dist, corridorLen(myTc, enemyTc) * 3 / 5));
    }

    /** 双方 TC 的切比雪夫距离（-1 = 敌 TC 不明）。 */
    private static int corridorLen(int myTc, int enemyTc) {
        if (enemyTc < 0) {
            return Integer.MAX_VALUE;
        }
        int dx = (enemyTc >>> 8) - (myTc >>> 8), dy = (enemyTc & 0xFF) - (myTc & 0xFF);
        return Math.max(Math.abs(dx), Math.abs(dy));
    }

    /** 村民逃跑点：远离威胁 5 格， candidate 不可走则依次试 TC 四周。 */
    private static int fleeTile(c game, int px, int py, int ex, int ey, int tcx, int tcy) {
        int dx = px - ex, dy = py - ey;
        int m = Math.max(Math.abs(dx), Math.abs(dy));
        if (m == 0) {
            dx = px - tcx;
            dy = py - tcy;
            m = Math.max(1, Math.max(Math.abs(dx), Math.abs(dy)));
        }
        int fx = px + dx * 5 / m, fy = py + dy * 5 / m;
        for (int r = 0; r < 4; ++r) {
            int nx = Math.max(1, Math.min(62, fx + (r == 1 ? 1 : r == 2 ? -1 : 0)));
            int ny = Math.max(1, Math.min(62, fy + (r == 3 ? 1 : r == 2 ? 1 : 0)));
            if ((game.mapTiles[nx + (ny << 6)] & 0x300) == 0) {
                return nx << 8 | ny;
            }
        }
        return -1;
    }

    /** 全图扫描找最近的指定 kind 资源格（打包 tx<<8|ty；找不到返回 -1）。
     *  固定扫描序保确定性；过滤卡死拉黑格与无可走邻格的死角资源。 */
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

    /** 该格 8 邻格里是否有可站立格（建筑 0x100 与资源/虚空 0x300 当墙）。 */
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

    /** 建筑在训队列长度（rec[+2] 位 16..23；研究中的 0x10000 也算 1）。 */
    private static int queueLen(int[] recs, int slot) {
        return (recs[slot + 2] >> 16) & 0xFF;
    }

    private static boolean hasUC(int[] recs, int bcount, int type) {
        return ucCount(recs, bcount, type) > 0;
    }

    private static int ucCount(int[] recs, int bcount, int type) {
        int n = 0;
        for (int i = 0; i < bcount; ++i) {
            int o = i << 2;
            if ((recs[o + 3] & 0xFF) == type && (recs[o + 2] & 0x40000000) != 0) {
                ++n;
            }
        }
        return n;
    }

    /** 兵种训练上限检查（与 tryTrainAiUnit 同源）。 */
    private static boolean canTrain(int[] hdr, int type) {
        int t = type - 1;
        if (t < 0) {
            t = 0;
        }
        return hdr[57 + t] + hdr[66 + t] < hdr[75 + t];
    }
}
