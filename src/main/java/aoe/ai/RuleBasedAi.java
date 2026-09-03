package aoe.ai;

import AgeOfEmpires.c;

/**
 * 规则式玩家 AI（-Daoe.playerAi=aoe.ai.RuleBasedAi）。随机图（gameMode=0）
 * Easy/Medium/Expert 通用。最终成绩（2026-09-03 第四批，v35 定型 = v30 +
 * Expert 波 1 接战微操四件套）：Medium 1000+ 9胜1僵持（1004 退化图）/ 1010+
 * 9胜1负 / 2000+ 8胜2负（决胜 26/29，与 v30 一致）；Easy 4胜1僵持；
 * Expert 1000+ 3/10 + 复测 1/10（合并 20%，与 v30 的 22-33% 同噪声带）。
 * 第四批结论：微操交换比收益是实的（[combat] 统计 2.46→3.0-3.5，崩盘推迟
 * ~50%）但决胜胜率不动——约束在围城经济/产能总量而非交战质量，规则式
 * Expert 天花板 ≈20-33% 定性成立，详见迭代笔记「波 1 接战微操（第四批）」。
 *
 * 战略（针对 Medium/Expert：敌方 3.07×/8× 采集 + all-in 阈值 60/100；我方 4 村民 +
 * pop 25 硬顶，拼经济必输）：**塔防吸收 all-in → 反击拆敌 TC**（随机图拆敌
 * TC 即胜，i() case 9；败北 = 我方 TC 毁 或 0 单位+0 建筑）。
 *
 * 读面：双方 playerUnitHeaders / playerUnitSlots / buildingTable(建筑记录) /
 * mapTiles / techFlags。本里程碑允许读对面状态（hdr[1] 全字段）。
 * 写面：军事群令走 selectUnits(0,-1)+orderMove+clearSelection 原语；村民单体
 * 改派与敌方 AI 同做法——直接写 slot[2] 目标（引擎自身在 tickAi 里就是这么写的）。
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
    // v17 Expert 紧凑塔环：×8 经济的波次规模下，外圈孤塔（12/16 格）援护不及=白送，
    // 压缩到 3-11 格让塔火互相覆盖、军队援护路程最短（Expert 连败全是塔被逐个拔掉）
    // （v33 试过扩到 7 座：石料贫乏图塔 6/7 卡死 smith/射箭场链（弓兵绝育、金囤 860
    // 花不掉，seed 1001 由胜转负实锤），回滚——塔链长度就是 smith 前置，动不得）
    private static final int[] TOWER_DIST_EXPERT = {3, 5, 7, 9, 11};

    private int nextDecide;
    private int lastAttackOrder = -100000;
    private int lastDefendOrder = -100000;
    private int lastStanceOrder = -100000;
    private int lastLog;
    private boolean attackMode;
    private boolean attackMuster = true;             // 进攻两阶段：true=集结中 false=已开打
    private int musterStart;                         // 集结超时起点
    private int musterTile = -1;                     // 集结点（敌 TC 朝我 7 格）
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
    // 集火闪避计数（v31 Expert 微操遥测，摘要行 dodges=）
    private int dodgeCount;
    // 投石机猎手标记/计数（v32：本决策被派往 t8 的单位不参与闪避；摘要行 hunts=）
    private final boolean[] huntingM = new boolean[26];
    private int huntCount;
    // 村民卡死检测（行军中 300 tick 没挪窝 → 目标拉黑 3000 tick 重派）
    private final int[] villLastPos = new int[26];
    private final int[] villLastTick = new int[26];
    private final int[] resBlacklistUntil = new int[4096];
    private boolean noWoodRes;
    private boolean noGoldRes;
    private boolean noStoneRes;
    private int resEnemyTc = -1;                     // 资源勘察危险区圆心（敌 TC），每决策刷新
    private final int[] towerTiles = new int[8];     // 完工塔位（v16 塔援护用）
    private int towerCnt;

    // ===== 波次建模（2026-09-03 第三批：预测器+遥测落库，行为与 v21 完全一致）=====
    // 预测器本身已验证（首发波预测误差 ~120-168t≈行军项系统偏差），但架在其上的
    // 五个行为变体全部被批量数据证伪并回滚（v25 发波即冲 1/9、v26 波扎进塔群再
    // 出门 2/9、v27 预测抢先手 2/9、v28 村民守家参战 1/9、v29 村民随军抢攻 1/9，
    // 基线 3/9）——约束不在时机信息而在波 1 接战的兵力比（产能硬顶）。详见
    // docs/research/rulebased-ai-medium-iteration.md「波次建模（第三批）」。
    // 敌 all-in 判定（tickAi 源码复核）：hdr[1][55]（敌军值=Σ活兵攻甲+村民2/个+
    // 历史建成塔的幻影值——完工 +hdr[45]+hdr[46]，被拆不减）≥ aiAttackThreshold
    // 且 hdr[0][55]（我军值，同口径）< 敌军值×1.25 时，75% 兵力 attack-move 我 TC
    // （目标 = hdr[0][8] + tickCount 抖动的 ±1-2 格；扫描每"我方单位数" tick 完成
    // 一轮，判定同频）。预测器：滚动窗口最小二乘拟合 hdr[1][55] 斜率 → 外推跨越
    // need=max(aiAttackThreshold, 4×hdr[0][55]/5+1) 的时刻 → 加行军时间（走廊长
    // ×8t/格：移速计时器初值 0xF00 每 tick 减装填值，剑士 1024≈4t/格、投石机
    // 256≈16t/格的混合估值）= 下一波到家时刻。发波侦测：敌军事单位 slot[2] 目标
    // 落在我 TC 4 格内的 ≥4 个 = 波已在路上（直接镜像 tickAi 的目标写法）。
    private static final int WAVE_WIN = 32;           // 斜率窗口样本数（采样间隔=DECIDE_EVERY）
    private static final int WAVE_MARCH_PER_TILE = 8; // 敌波行军估值（tick/格）
    private final int[] waveSampT = new int[WAVE_WIN];
    private final int[] waveSampV = new int[WAVE_WIN];
    private int waveSampN;
    private int waveEta = -1;                         // 预测下一波到家 tick（-1=不可预测）
    private int waveNeed;                             // 当前发波军值门槛
    private int waveSlopeMilli;                       // 敌军值斜率（千分比 val/tick）
    private boolean waveInFlight;
    private int waveLaunchTick = -1;

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
        int[] erecs = game.buildingTable[1];
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
        int[] recs = game.buildingTable[0];
        int houseN = 0, barracksDone = 0, archeryDone = 0, smithDone = 0;
        int lumberN = 0, miningN = 0, millN = 0, towerN = 0, stableDone = 0, siegeDone = 0;
        int tcSlot = -1, houseSlot = -1, barracksSlot = -1, archerySlot = -1, smithSlot = -1;
        int lumberSlot = -1, miningSlot = -1, towerSlot = -1, stableSlot = -1, siegeSlot = -1;
        int barracks2Slot = -1;                      // 第二兵营（Expert 专建）
        boolean anyUC = false;
        this.towerCnt = 0;
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
                case 10: if (!uc) { ++barracksDone; if (barracksSlot < 0) barracksSlot = o;
                    else if (barracks2Slot < 0) barracks2Slot = o; } break;
                case 7:  if (!uc) { ++archeryDone; if (archerySlot < 0) archerySlot = o; } break;
                case 6:  if (!uc) { ++smithDone; if (smithSlot < 0) smithSlot = o; } break;
                case 0:  ++lumberN; if (!uc && lumberSlot < 0) lumberSlot = o; break;
                case 1:  ++miningN; if (!uc && miningSlot < 0) miningSlot = o; break;
                case 5:  ++millN; break;
                case 12: ++towerN; if (!uc && towerSlot < 0) towerSlot = o;
                    if (!uc && this.towerCnt < 8) {
                        this.towerTiles[this.towerCnt++] = recs[o + 0];
                    }
                    break;
                case 8:  if (!uc) { ++stableDone; if (stableSlot < 0) stableSlot = o; } break;
                case 2:  if (!uc) { ++siegeDone; if (siegeSlot < 0) siegeSlot = o; } break;
                default: break;
            }
        }
        int myTc = hdr[8];
        int enemyTc = ehdr[8];
        this.resEnemyTc = enemyTc;
        // 波次预测器（v24）：只读+打点，行为不变
        this.trackWaves(game, hdr, ehdr, eslots, eunits, myTc, enemyTc);
        // 难度感知（v15）：Expert（采集 ×8 + 每 tick 出兵尝试 + 免费资源滴）下
        // 敌兵是磨不完的，v13/v14 的 7 兵 CRUSHED 反击等于把仅有的家底送进
        // 敌塔环（Expert 基线 4 连败同一样态：反击送军→下一波破家）。Expert 上
        // 反击门槛抬高到 10 兵且军值 ≥ 敌塔计入后的防御军值。
        boolean expert = game.aiGatherMultiplier >= 1024;

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
        int defendTile = threat ? invaderTile : -1;
        int defendAnchor = myTc;
        // v16 塔援护（仅 Expert）：敌兵贴近任一完工塔（6 格内）同样触发 DEFEND——
        // v15 Expert 复盘：敌波次沿走廊逐个拔掉 9/12/16 格外圈塔，我军因敌未进 TC
        // 10 格全程 eco 旁观，5 塔被白拆 4 座。塔+军协同后 Expert 0/4→3/4。
        // Medium 上同机制反而把军队拉离 TC 圈吃败仗（v16 2000+ 掉到 6/10），
        // 故收到 expert 门内——Medium 敌 3.07× 的波次规模，TC 圈+塔射程已够。
        if (!threat && expert && this.towerCnt > 0) {
            int twBest = Integer.MAX_VALUE, twTile = -1, twAnchor = -1;
            for (int i = 0; i < eunits; ++i) {
                int o = i << 3;
                if ((eslots[o + 3] & 0xFF) < 2) {
                    continue;
                }
                int t = eslots[o + 0] & 0xFFFF;
                for (int k = 0; k < this.towerCnt; ++k) {
                    int dx = (t >>> 8) - (this.towerTiles[k] >>> 8);
                    int dy = (t & 0xFF) - (this.towerTiles[k] & 0xFF);
                    int d2 = dx * dx + dy * dy;
                    if (d2 < twBest) {
                        twBest = d2;
                        twTile = t;
                        twAnchor = this.towerTiles[k];
                    }
                }
            }
            if (twTile >= 0 && twBest <= 36) {
                threat = true;
                defendTile = twTile;
                defendAnchor = twAnchor;
            }
        }
        if (invaderN > 0) {
            this.lastContactTick = game.tickCount;
        }
        if (threat && defendTile >= 0 && game.tickCount - this.lastDefendOrder >= DEFEND_REISSUE) {
            this.lastDefendOrder = game.tickCount;
            // 近战压向最近入侵者；远程（弓/投石机）停在入侵者朝防御锚点 3 格处开火——
            // v7 前远程被群令送进近战圈白死（seed 1019 弓兵 3 连阵亡实锤）。
            // 敌 t8 投石机在 20 格内时远程改点杀 t8（远程拆塔克星，§5.1/r29）。
            // 顺序：先 -1 群令，再按兵种覆盖（selectUnits(type) 重选后 orderMove 只动该兵种）。
            // v31（仅 Expert）环形拦截：近战不再冲入侵者本身（冲锋=走出塔火圈被围殴，
            // v2 十二格冲锋被放风筝的同类问题），钉在锚点朝敌 3 格塔火重叠区等接敌。
            // 敌 t8 由 v32 猎手小队处理（见下），全军冲 t8 = 离圈送死。
            int meleeTile = defendTile;
            if (expert && defendAnchor >= 0 && corridorLen(defendAnchor, defendTile) > 3) {
                meleeTile = stanceTile(defendAnchor, defendTile, 3);
            }
            int rangedTile;
            if (mangonelTile >= 0 && mangonelD2 <= 400) {
                rangedTile = mangonelTile;
            } else {
                rangedTile = defendAnchor >= 0 ? stanceTile(defendTile, defendAnchor, 3) : defendTile;
            }
            if (expert) {
                // v34 逐单位下令：攻击态（action==1）单位不动——群令 orderMove 会清
                // slot[7] 攻击计数器并把缠斗中的兵拉开，48t 一次的重发等于周期性
                // 抹掉全军装填 + 来回踱步（持续 DPS 暗坑）。回撤/猎手单位也不动。
                for (int i = 0; i < units; ++i) {
                    int o = i << 3;
                    int type = slots[o + 3] & 0xFF;
                    if (type < 2 || (slots[o + 7] & 0xF) == 1
                            || this.healUntil[i] > game.tickCount || this.huntingM[i]) {
                        continue;
                    }
                    int tgt = (type == 4 || type == 8) ? rangedTile : meleeTile;
                    if ((slots[o + 2] & 0xFFFF) != tgt) {
                        slots[o + 1] = slots[o + 0];
                        slots[o + 2] = (short) tgt;
                        slots[o + 7] = 0;
                        slots[o + 3] = (short) (slots[o + 3] & 0xFF);
                    }
                }
            } else {
            game.selectUnits(0, -1);
            game.orderMove(0, meleeTile >>> 8, meleeTile & 0xFF);
            game.selectUnits(0, 4);
            game.orderMove(0, rangedTile >>> 8, rangedTile & 0xFF);
            game.selectUnits(0, 8);
            game.orderMove(0, rangedTile >>> 8, rangedTile & 0xFF);
            game.clearSelection();
            }
            if (invaderD2 <= 64 || defendAnchor != myTc) {
                System.out.println("[ai] DEFEND invader " + invaderN + " at " + (defendTile >>> 8) + ","
                    + (defendTile & 0xFF) + (defendAnchor != myTc ? " (tower)" : "")
                    + " t=" + game.tickCount);
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
            boolean goCrushed = (crushed || closeRush) && milCount >= MIN_ATTACK_UNITS - 1
                && milVal >= enemyMilVal;
            if (expert) {
                // Expert：只打有把握的反击——10 兵以上且军值压过敌塔计入后的防御军值
                goCrushed = (crushed || closeRush) && milCount >= 10 && milVal >= enemyDefVal;
            }
            if (!this.attackMode && !threat && game.tickCount >= this.attackCooldownUntil
                    && (goCrushed || overwhelm || desperate)) {
                this.attackMode = true;
                this.attackMuster = true;                    // v14：先集结后开打
                this.musterStart = game.tickCount;
                this.musterTile = stanceTile(enemyTc, myTc, 7); // 敌 TC 朝我 7 格（警戒圈 6 格外沿）
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
            // v14 集结阶段：全军先压到敌 TC 7 格外集合，到齐（或超时 700 tick）再一起上。
            // 解决兵种移速差（冲车/投石机 256 vs 剑士 1024）导致的添油式送死——
            // v13 败局复盘（seed 2001/2006）：7-9 兵分批走进敌塔环，200 tick 折损过半。
            if (this.attackMode && this.attackMuster) {
                int mx = this.musterTile >>> 8, my = this.musterTile & 0xFF;
                int arrived = 0;
                for (int i = 0; i < units; ++i) {
                    int o = i << 3;
                    if ((slots[o + 3] & 0xFF) < 2) {
                        continue;
                    }
                    int t = slots[o + 0] & 0xFFFF;
                    int dx = (t >>> 8) - mx, dy = (t & 0xFF) - my;
                    if (dx * dx + dy * dy <= 9) {
                        ++arrived;
                    }
                }
                if (arrived >= Math.max(5, milCount * 2 / 3)
                        || game.tickCount - this.musterStart > 700) {
                    this.attackMuster = false;
                    this.lastAttackOrder = -100000;
                    this.attackBestD2 = Integer.MAX_VALUE;
                    this.attackBestTick = game.tickCount;
                    System.out.println("[ai] MUSTER done, " + arrived + "/" + milCount
                        + " assault t=" + game.tickCount);
                } else if (game.tickCount - this.lastAttackOrder >= ATTACK_REISSUE) {
                    this.lastAttackOrder = game.tickCount;
                    game.selectUnits(0, -1);
                    game.orderMove(0, mx, my);
                    game.clearSelection();
                }
            }
            if (this.attackMode && !this.attackMuster && game.tickCount - this.lastAttackOrder >= ATTACK_REISSUE) {
                this.lastAttackOrder = game.tickCount;
                game.selectUnits(0, -1);
                game.orderMove(0, enemyTc >>> 8, enemyTc & 0xFF);
                game.clearSelection();
            }
            if (this.attackMode && !this.attackMuster) {
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

        // ===== Expert 波 1 接战微操：投石机猎手（v32，每决策重投） =====
        // 敌 t8 是塔群棺材钉：拆塔 14 伤/15t（塔对射 8 伤/17t 射不过），且移速 256
        // （全场最慢）逃不掉。塔援护 DEFEND 要敌兵进塔 6 格才触发，t8 在 4-8 格
        // 外慢慢磨塔时全军旁观（v31 seed 1000 复盘：外圈 3 塔被 2 台 t8 白拆）。
        // 全军冲=离圈送死，派 ≤2 个最近的快腿近战（t2/3/5/6）直取；敌命中会改写
        // 我方 slot[2]（d() 反扑语义），靠每决策重投续上。猎手不参与闪避。
        java.util.Arrays.fill(this.huntingM, false);
        if (expert && !this.attackMode && myTc >= 0) {
            int huntersLeft = Math.min(4, Math.max(1, milCount / 2));
            for (int j = 0; j < eunits && huntersLeft > 0; ++j) {
                int eo = j << 3;
                if ((eslots[eo + 3] & 0xFF) != 8) {
                    continue;
                }
                int mt = eslots[eo + 0] & 0xFFFF;
                int mx = mt >>> 8, my = mt & 0xFF;
                boolean menace = false;
                {
                    int dx = mx - (myTc >>> 8), dy = my - (myTc & 0xFF);
                    menace = dx * dx + dy * dy <= 100;
                }
                for (int k = 0; !menace && k < this.towerCnt; ++k) {
                    int dx = mx - (this.towerTiles[k] >>> 8), dy = my - (this.towerTiles[k] & 0xFF);
                    menace = dx * dx + dy * dy <= 100;
                }
                if (!menace) {
                    continue;
                }
                for (int pick = 0; pick < 2 && huntersLeft > 0; ++pick) {
                    int bestI = -1, bestD2 = Integer.MAX_VALUE;
                    for (int i = 0; i < units; ++i) {
                        int o = i << 3;
                        int type = slots[o + 3] & 0xFF;
                        if ((type != 2 && type != 3 && type != 5 && type != 6)
                                || this.huntingM[i] || this.healUntil[i] > game.tickCount) {
                            continue;
                        }
                        int up = slots[o + 0] & 0xFFFF;
                        int dx = (up >>> 8) - mx, dy = (up & 0xFF) - my;
                        int d2 = dx * dx + dy * dy;
                        if (d2 < bestD2) {
                            bestD2 = d2;
                            bestI = i;
                        }
                    }
                    if (bestI < 0) {
                        break;
                    }
                    this.huntingM[bestI] = true;
                    --huntersLeft;
                    ++this.huntCount;
                    int o = bestI << 3;
                    if ((slots[o + 2] & 0xFFFF) != mt) {
                        slots[o + 1] = slots[o + 0];
                        slots[o + 2] = (short) mt;
                        slots[o + 7] = 0;
                        slots[o + 3] = (short) (slots[o + 3] & 0xFF);
                    }
                }
            }
        }

        // ===== Expert 波 1 接战微操：集火闪避（v31，必须在群令/回撤之后跑） =====
        // 引擎攻击按"格"结算：攻击态 slot[5]=目标格，格上无人则每 tick 校验时
        // slot[7] 清零重来（g() case 1），敌近战出手需攒 8 tick 装填。被锁定单位
        // 每决策（8t）横移一步 = 敌装填永远攒不满还要重新走位，我方其余单位原地
        // 继续输出——把 1:1 换血变成 0:N。落点强制留在塔火圈（hdr[12]，封建后 25）
        // 内，防 v2 式被拉出塔圈风筝；自己装填将满（再 1-2t 出手）时让这一击打完。
        // 闪避人数上限 milCount/2，防全员后撤零输出。
        if (expert && !this.attackMode && myTc >= 0) {
            int coverR2 = hdr[12];
            int maxDancers = Math.max(1, milCount / 2);
            int dancers = 0;
            for (int i = 0; i < units && dancers < maxDancers; ++i) {
                int o = i << 3;
                int myType = slots[o + 3] & 0xFF;
                if (myType < 2 || this.healUntil[i] > game.tickCount || this.huntingM[i]) {
                    continue;
                }
                int pos = slots[o + 0] & 0xFFFF;
                int px = pos >>> 8, py = pos & 0xFF;
                boolean focused = false;
                for (int j = 0; j < eunits; ++j) {
                    int eo = j << 3;
                    int eType = eslots[eo + 3] & 0xFF;
                    if (eType < 2) {
                        continue;
                    }
                    if ((eslots[eo + 7] & 0xF) == 1 && (eslots[eo + 5] & 0xFFFF) == pos) {
                        focused = true;            // 敌攻击态正锁定我这格
                        break;
                    }
                    // 远程脆皮加一条：敌近战以我为目标贴到 2 格内时提前闪
                    if ((myType == 4 || myType == 8) && eType != 4 && eType != 8 && eType != 9
                            && (eslots[eo + 2] & 0xFFFF) == pos) {
                        int et = eslots[eo + 0] & 0xFFFF;
                        int edx = (et >>> 8) - px, edy = (et & 0xFF) - py;
                        if (edx * edx + edy * edy <= 4) {
                            focused = true;
                            break;
                        }
                    }
                }
                if (!focused) {
                    continue;
                }
                // 我方攻击装填将满（近战 ≥6/8）：让这一击打完再走
                if ((slots[o + 7] & 0xF) == 1 && ((slots[o + 7] & 0x7F00) >> 8) >= 6) {
                    continue;
                }
                // 8 邻格选退步点：可站立，塔火圈内优先，其次离敌近战最远
                int curScore = dodgeScore(eslots, eunits, px, py, coverR2);
                int best = -1, bestScore = curScore;
                for (int ddy = -1; ddy <= 1; ++ddy) {
                    for (int ddx = -1; ddx <= 1; ++ddx) {
                        if (ddx == 0 && ddy == 0) {
                            continue;
                        }
                        int nx = px + ddx, ny = py + ddy;
                        if (nx < 1 || ny < 1 || nx > 62 || ny > 62
                                || (game.mapTiles[nx + (ny << 6)] & 0x300) != 0) {
                            continue;
                        }
                        int sc = dodgeScore(eslots, eunits, nx, ny, coverR2);
                        if (sc > bestScore) {
                            bestScore = sc;
                            best = nx << 8 | ny;
                        }
                    }
                }
                if (best >= 0 && best != (slots[o + 2] & 0xFFFF)) {
                    slots[o + 1] = slots[o + 0];
                    slots[o + 2] = (short) best;
                    slots[o + 7] = 0;
                    slots[o + 3] = (short) (slots[o + 3] & 0xFF);
                    ++dancers;
                    ++this.dodgeCount;
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
                // OVERWHELM 门槛（Easy seed 1002 七百万 tick 对峙实证）。
                // v18/v19 教训：Expert 上砍金工保 2 木 + 兵营木门槛降 8 的组合 =
                // 暴兵断粮/塔补不起，两连版 1/9（v17 4/9），全量回滚。
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
                // v15 石断供补救：需要石而石工为 0 时抽调门槛放宽到 >target（否则
                // 2木2金的满配状态永远抽不出人——Expert 基线 4 败局 towers 恒 ≤2、
                // 塔被拆后 stone<18 永远补不了的实锤）。
                if (overKind == 0 && stoneTarget > 0 && stoneW == 0 && miningN > 0) {
                    overKind = woodW > woodTarget ? 1 : (goldW > goldTarget ? 2 : 0);
                }
                int wantKind = goldW < goldTarget ? 2
                    : (stoneW < stoneTarget && miningN > 0 ? 3 : (woodW < woodTarget ? 1 : 0));
                // 补救路径要直接补石，别让 wantKind 的金优先把它盖掉
                if (stoneTarget > 0 && stoneW == 0 && miningN > 0
                        && (overKind == 1 || overKind == 2) && goldW >= 1) {
                    wantKind = 3;
                }
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
                // 城堡时代（磨坊+铁匠≥2）：再 +1 攻/甲 + Guard Tower，富余才升。
                // v23 试过 Expert 放宽到 30/25/25 + 磨坊提到射箭场前：2/9 不升反降
                // （射箭场推迟丢了投石机反制），回滚。
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
                int[] tdist = expert ? TOWER_DIST_EXPERT : TOWER_DIST;
                if (threat) {
                    // 交战中只补塔（敌 12 格内）：seed 1019 兵临城下连放 4 座铁匠铺全被
                    // 秒拆白烧 100 木 80 石；塔例外——战中补塔=战力，且敌军索敌优先打塔，
                    // 在建塔也是 255 HP 的仇恨海绵。v14：门槛压到塔成本(20/5/15)附近，
                    // 塔被拆能第一时间原地补（原 30/8/18 在金石枯竭的消耗战里永远不够）。
                    if (towerN < tdist.length && hdr[5] >= 22 && hdr[6] >= 6 && hdr[7] >= 16) {
                        need = 12;
                        anchor = corridorAnchor(myTc, enemyTc, tdist[towerN]);
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
                    anchor = corridorAnchor(myTc, enemyTc, tdist[0]);
                } else if (miningN == 0 && !this.noGoldRes && hdr[5] >= 20) {
                    need = 1;
                    anchor = findResource(game, myTc, 2, game.tickCount);
                    if (anchor < 0) {
                        this.noGoldRes = true;
                    }
                } else if (towerN + ucCount(recs, hdr[4], 12) < 2 && hdr[5] >= 28 && hdr[6] >= 8 && hdr[7] >= 20) {
                    need = 12;                                   // 走廊塔 2
                    anchor = corridorAnchor(myTc, enemyTc, tdist[1]);
                } else if (expert && houseN < 4 && hdr[5] >= 5) {
                    need = 11;                                   // Expert：塔 2 后立刻补满 4 房——×8 消耗战
                } else if (expert && feudal && barracksDone < 2 && !hasUC(recs, hdr[4], 10)  // 里金囤着花不出去全是
                        && hdr[5] >= 25 && hdr[7] >= 12) {                                 // pop 上限卡的（v16b 败局
                    need = 10;                                   // v21：第二兵营提到塔 3-5 前——v17 败局复盘：
                } else if (expert && towerN < tdist.length && !hasUC(recs, hdr[4], 12)   // 塔 4-5 座俱在、兵线 4-6 个
                        && hdr[5] >= 22 && hdr[6] >= 6 && hdr[7] >= 16) {                // 被 13+ 波次碾死=产能不足
                    need = 12;                                   // 不是塔不足
                    anchor = corridorAnchor(myTc, enemyTc, tdist[towerN]);
                } else if (feudal && smithDone == 0 && !hasUC(recs, hdr[4], 6) && hdr[5] >= 35 && hdr[7] >= 25) {
                    need = 6;                                    // 铁匠铺：攻防升级 + 投石机（产 t8 的建筑）
                } else if (feudal && archeryDone == 0 && !hasUC(recs, hdr[4], 7) && hdr[5] >= 35 && hdr[7] >= 15) {
                    need = 7;                                    // 射箭场：弓兵反制敌投石机
                } else if (towerN < tdist.length && !hasUC(recs, hdr[4], 12)
                        && hdr[5] >= 22 && hdr[6] >= 6 && hdr[7] >= 16) {
                    need = 12;                                   // 走廊塔 3-5：阶梯前推（v3 提到房屋/磨坊前，
                    anchor = corridorAnchor(myTc, enemyTc, tdist[towerN]); // 塔是抗消耗主力；v6/v15 降门槛保战中补塔）
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
                if (barracks2Slot >= 0 && canTrain(hdr, meleeType) && queueLen(recs, barracks2Slot) < 2
                        && hdr[5] >= 15 && feudal && hdr[6] >= 15
                        && game.canAfford(0, 0, meleeType)) {
                    // 第二兵营（Expert）要自己写队列位——queueUnitTraining 只会排到同类型
                    // 第一座建筑（c.java:6519 按记录序扫），语义照抄该原语：排队不付款，
                    // 产出时 spawn 路径 canAfford 硬检后才扣。
                    recs[barracks2Slot + 2] += 65536;
                    hdr[49] += 1;
                    hdr[66 + meleeType - 1] += 1;
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
                + " e55=" + ehdr[55] + "/" + this.waveNeed
                + " slope=" + this.waveSlopeMilli
                + " eta=" + (this.waveEta > 0 ? this.waveEta - game.tickCount : -1)
                + (this.waveInFlight ? " INFLIGHT" : "")
                + (expert ? " dodges=" + this.dodgeCount + " hunts=" + this.huntCount : "")
                + " mode=" + (threat ? "DEFEND" : this.attackMode ? "ATTACK" : "eco"));
        }
    }

    /** 波次预测器（v24）：每决策 tick 采样敌军值，拟合斜率外推发波时刻 + 侦测在途波。
     *  只读游戏状态+打日志，不改任何行为。全确定性（只用 tickCount 与读面数据）。 */
    private void trackWaves(c game, int[] hdr, int[] ehdr, short[] eslots, int eunits,
                            int myTc, int enemyTc) {
        // 采样入窗（满了挤掉最旧样本）
        if (this.waveSampN < WAVE_WIN) {
            this.waveSampT[this.waveSampN] = game.tickCount;
            this.waveSampV[this.waveSampN] = ehdr[55];
            ++this.waveSampN;
        } else {
            for (int i = 1; i < WAVE_WIN; ++i) {
                this.waveSampT[i - 1] = this.waveSampT[i];
                this.waveSampV[i - 1] = this.waveSampV[i];
            }
            this.waveSampT[WAVE_WIN - 1] = game.tickCount;
            this.waveSampV[WAVE_WIN - 1] = ehdr[55];
        }
        // 发波侦测：敌军事单位的目标格（slot[2]）落在我 TC 4 格内的计数
        int dispatched = 0, leadD2 = Integer.MAX_VALUE;
        if (myTc >= 0) {
            int tcx = myTc >>> 8, tcy = myTc & 0xFF;
            for (int i = 0; i < eunits; ++i) {
                int o = i << 3;
                if ((eslots[o + 3] & 0xFF) < 2) {
                    continue;
                }
                int tgt = eslots[o + 2] & 0xFFFF;
                if (Math.max(Math.abs((tgt >>> 8) - tcx), Math.abs((tgt & 0xFF) - tcy)) > 4) {
                    continue;
                }
                ++dispatched;
                int pos = eslots[o + 0] & 0xFFFF;
                int px = (pos >>> 8) - tcx, py = (pos & 0xFF) - tcy;
                int d2 = px * px + py * py;
                if (d2 < leadD2) {
                    leadD2 = d2;
                }
            }
        }
        int corridor = corridorLen(myTc, enemyTc);
        if (!this.waveInFlight && dispatched >= 4) {
            this.waveInFlight = true;
            this.waveLaunchTick = game.tickCount;
            System.out.println("[ai] WAVE launched n=" + dispatched
                + " e55=" + ehdr[55] + " m55=" + hdr[55]
                + (this.waveEta > 0 ? " predictedEta=" + this.waveEta
                    + " err=" + (game.tickCount - this.waveEta) : " (no prediction)")
                + " t=" + game.tickCount);
        } else if (this.waveInFlight && dispatched <= 1) {
            this.waveInFlight = false;
            this.waveSampN = 0;      // 窗内混入波次折损骤降，重置斜率拟合
            this.waveEta = -1;
            System.out.println("[ai] WAVE cleared, e55=" + ehdr[55]
                + " span=" + (game.tickCount - this.waveLaunchTick) + " t=" + game.tickCount);
        }
        if (this.waveInFlight) {
            // 波已在路上：ETA = 先头兵距我 TC 格数 × 4t/格（按最快兵种估）
            this.waveEta = leadD2 < Integer.MAX_VALUE
                ? game.tickCount + isqrt(leadD2) * 4 : game.tickCount;
            return;
        }
        // 积累期：斜率外推跨越门槛的时刻 + 行军时间
        this.waveNeed = Math.max(game.aiAttackThreshold, (hdr[55] << 2) / 5 + 1);
        if (this.waveSampN < 8 || corridor == Integer.MAX_VALUE) {
            this.waveEta = -1;
            this.waveSlopeMilli = 0;
            return;
        }
        int n = this.waveSampN;
        int t0 = this.waveSampT[0];
        long sx = 0, sv = 0, sxv = 0, sxx = 0;
        for (int i = 0; i < n; ++i) {
            long x = this.waveSampT[i] - t0;
            long v = this.waveSampV[i];
            sx += x;
            sv += v;
            sxv += x * v;
            sxx += x * x;
        }
        long denom = (long) n * sxx - sx * sx;
        if (denom <= 0) {
            this.waveEta = -1;
            this.waveSlopeMilli = 0;
            return;
        }
        this.waveSlopeMilli = (int) (1000 * ((long) n * sxv - sx * sv) / denom);
        if (this.waveSlopeMilli <= 0) {
            this.waveEta = -1;
            return;
        }
        int gap = this.waveNeed - ehdr[55];
        long toCross = gap <= 0 ? 0 : (long) gap * 1000 / this.waveSlopeMilli;
        this.waveEta = (int) (game.tickCount + toCross + (long) corridor * WAVE_MARCH_PER_TILE);
    }

    /** 集火闪避落点评分（v31）：在任一完工塔的火圈（hdr[12]）内 +100000，
     *  外加与敌近战兵种（t2/3/5/6）最近距离²（截断 999，无敌人时=999）。 */
    private int dodgeScore(short[] eslots, int eunits, int x, int y, int coverR2) {
        int minD2 = 999;
        for (int j = 0; j < eunits; ++j) {
            int eo = j << 3;
            int eType = eslots[eo + 3] & 0xFF;
            if (eType < 2 || eType == 4 || eType == 8 || eType == 9) {
                continue;
            }
            int et = eslots[eo + 0] & 0xFFFF;
            int dx = (et >>> 8) - x, dy = (et & 0xFF) - y;
            int d2 = dx * dx + dy * dy;
            if (d2 < minD2) {
                minD2 = d2;
            }
        }
        for (int k = 0; k < this.towerCnt; ++k) {
            int dx = (this.towerTiles[k] >>> 8) - x, dy = (this.towerTiles[k] & 0xFF) - y;
            if (dx * dx + dy * dy <= coverR2) {
                return 100000 + minD2;
            }
        }
        return minD2;
    }

    /** 整数平方根（math 不进模拟路径的确定性实现）。 */
    private static int isqrt(int v) {
        int r = 0;
        while ((r + 1) * (r + 1) <= v) {
            ++r;
        }
        return r;
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

    /** 距敌 TC 切比雪夫 < 此格数的资源格视为敌境，不派村民（v14：seed 2004 复盘——
     *  最近金矿恰在敌 TC 3 格处，矿工走进敌基被守军追杀→逃命→重派死循环，金收入
     *  归零，采矿场原地被拆 6 次白烧 90 木）。过滤后无候选则回退不过滤（防死锁）。 */
    private static final int RES_ENEMY_SAFE = 10;

    /** 全图扫描找最近的指定 kind 资源格（打包 tx<<8|ty；找不到返回 -1）。
     *  固定扫描序保确定性；过滤卡死拉黑格与无可走邻格的死角资源。 */
    private int findResource(c game, int fromPacked, int kind, int tick) {
        int r = this.findResource(game, fromPacked, kind, tick, true);
        if (r < 0) {
            r = this.findResource(game, fromPacked, kind, tick, false);
        }
        return r;
    }

    private int findResource(c game, int fromPacked, int kind, int tick, boolean avoidEnemy) {
        int fx = fromPacked >>> 8, fy = fromPacked & 0xFF;
        int ex = this.resEnemyTc >>> 8, ey = this.resEnemyTc & 0xFF;
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
                if (avoidEnemy && this.resEnemyTc >= 0
                        && Math.max(Math.abs(tx - ex), Math.abs(ty - ey)) < RES_ENEMY_SAFE) {
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
