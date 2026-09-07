package aoe.ai;

import AgeOfEmpires.c;

/**
 * 规则式玩家 AI（-Daoe.playerAi=aoe.ai.RuleBasedAi）。随机图（gameMode=0）
 * Easy/Medium/Expert 通用。2026-09-04 第六批起默认「迷雾诚实模式」
 * （-Daoe.aiFog=0 回退全图；AOE_AIFOG=res|tc 消融档）：只读已探索格上的敌情，
 * 禁读 hdr[1] 统计字段，敌 TC 靠侦察记忆。成绩（诚实模式 v64，种子 1000+
 * n=10，1004 跳过）：Easy 8/10+7/10，Medium 8/10+4/10（两批等价合并 12/20），
 * Expert 1-2/10（四批 5/40；全图对照 -f 实测 4/10 与 v56 同四种子=回退零失真）。
 * 信息价值量化/消融/决策点依赖排名见迭代笔记「信息收紧（第六批）」。
 * ⚠️ 改代码后跑批：编译独立成行确认 BUILD SUCCESSFUL，禁止
 * `gradlew ... | tail && ailoop`（管道吞退出码，五批事故见迭代笔记第五批）。
 * 历史（全图时代 v56 定型 = v35 微操四件套 + 围城经济七件套）：Expert 1000+
 * 4/10、1010+ 1/10；Medium 1000+ 10/10；Easy 5/5。第五批结论：败局定性
 * "石/兵种帽死锁"，胜负完美预测子 = 有没有建成 archery/smith。
 *
 * 战略（针对 Medium/Expert：敌方 3.07×/8× 采集 + all-in 阈值 60/100；我方 4 村民 +
 * pop 25 硬顶，拼经济必输）：**塔防吸收 all-in → 反击拆敌 TC**（随机图拆敌
 * TC 即胜，i() case 9；败北 = 我方 TC 毁 或 0 单位+0 建筑）。
 *
 * 读面：我方 playerUnitHeaders / playerUnitSlots / buildingTable(建筑记录) /
 * mapTiles / techFlags 全量读；对面信息走「迷雾诚实模式」（2026-09-04 第六批，
 * 默认开，-Daoe.aiFog=0 回退全图）：只读已探索格（mapTiles 0x8000 位=唯一
 * 视野层，与渲染/小地图可见性逐格一致）上的敌单位/建筑，hdr[1] 统计字段
 * （军值/资源/人口/TC 位）全部禁止，敌 TC 位置靠侦察记忆。
 * 写面：军事群令走 selectUnits(side,-1)+orderMove+clearSelection 原语；村民单体
 * 改派与敌方 AI 同做法——直接写 slot[2] 目标（引擎自身在 tickAi 里就是这么写的）。
 *
 * side 参数化（2026-09-07，enemyAi 反串 player 1）：无参构造 = side 0
 * （-Daoe.playerAi 挂载，现状）；RuleBasedAi(1) = -Daoe.enemyAi 反串敌方
 * （c.java 帧首 hook 反射优先取 (int) 构造，见该处注释）。引擎两侧同构——
 * playerUnitHeaders/playerUnitSlots/buildingTable 下标与写原语首参都是
 * player 号，原 player-0 硬编码统一走 this.side/this.foe。迷雾是 player-0
 * 单层（mapTiles 0x8000 只被 player 0 单位揭开），player 1 没有自有雾层 →
 * side 1 强制全图（fogHonest=false），侦察/探针/敌 TC 记忆模块全挂在
 * fogHonest 门上自然旁路。[result]/System.exit 是 player-0 视角的批测契约，
 * side 1 的僵尸投降门整个跳过（引擎 z=98 结算会替 player 0 打 [result]）。
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
 * - 军事群令 selectUnits(side,-1) 只选军事（type≥2)，村民不受影响；村民单体
 *   改派直接写 slot（引擎 tickAi 同做法）。采集→交存→返矿全自动（§10 定论，
 *   闲置只在资源耗尽时），只需处理真闲置。
 * - 金/石交存自动取 TC+双采矿场最近者（hdr[10]/[11]，nearestDropOff c.java:8377）——
 *   第二采矿场是引擎原生能力，v40 起用于矿点被蹲转移。
 * - 村民修理免费：走到 HP<255 的自己建筑格自动进 action 4（c.java:7026 不查
 *   UC 位），~0.5 HP/tick/人——v44 起用于修塔（1 人 ≈ 抵消 1 剑士磨塔）。
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
    private int prevTick;               // 回溯检测（devPhase 拨钟/读档）
    private int lastAttackOrder = -100000;
    private int lastDefendOrder = -100000;
    private int lastStanceOrder = -100000;
    private int lastLog;
    private boolean attackMode;
    private boolean attackSpRush;                    // 本次进攻由 SPRUSH 发起（V2 死战/短冷却用）
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
    private int resMyTc = -1;                        // v45 走廊过滤用我方 TC，每决策刷新
    private boolean resRelocateOn;                   // v45 =expert：走廊过滤开关（非 Expert 零行为变化）
    // v44 修塔（第五批，Expert 门控）：村民走到受损完工塔/TC 格，抵达钩子自动进
    // action 4 修理（c.java:7026 对 HP<255 的自己建筑即触发，不查 UC 位），
    // tickConstruction ~0.5 HP/tick/人、零资源消耗。塔 255 HP：1 修理工 ≈ 抵消
    // 1 剑士磨血（4伤/8t），2 个 ≈ 拖住 1 台 t8。塔的替换价 22木5金15石，
    // 修理只花村民闲置时间——围城期塔被逐个磨掉是 v35-v42 败局的统一收尾，
    // 这是唯一的"负熵"来源。标记窗口内逃命/闲置重派都跳过该村民（否则敌兵贴脸
    // 7 格的逃命规则会每 8t 把修理工拽走）。
    private final int[] repairUntil = new int[26];
    private int repairCount;                          // 遥测（摘要行 repairs=）
    // ===== v38 矿点被蹲转移（2026-09-03 第五批·围城经济，Expert 门控）=====
    // 败局定性复盘（v35/v36 基线 7 连败资源时间线）：围城期"金/木归零"的真身不是
    // 配额失衡，而是【交战区=采集区】——敌波压在我方 TC/塔上打，附近资源格全在
    // 逃命半径内，村民永远逃命零产出（哪个桶先见底纯看地形）。调配额救不了
    // （v37 实测 2/10 回滚），必须把采集转移到【交战区外】的备用资源点。
    // 机制：敌军事单位 8 格内的资源格打 camped 标记（滚动窗 1500t=敌走后才解禁，
    // 防两矿来回跳的振荡环）；findResource 跳过 camped 格（无候选逐级回退不过滤，
    // 不死锁）；逃命目的地从"TC 另一侧空地"改为"最近未蹲资源格"（逃命不失业）；
    // 金矿被蹲时在备用矿点旁放第二采矿场（引擎 hdr[10]/[11] 双矿场槽，交存自动取
    // 三者最近者，c.java:8377 实锤）。非 Expert 不打标记=行为与 v35 逐字节一致。
    private static final int CAMP_D2 = 64;          // 蹲守判定半径²（8 格，> 逃命半径 7）
    private static final int CAMP_COOLDOWN = 1500;  // 敌撤离后资源格保持拉黑的 tick（滞后防抖）
    private final int[] resCampedUntil = new int[4096];
    private boolean goldCamped;                      // 本决策有金格被蹲（第二采矿场触发用）
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

    // ===== 迷雾诚实模式（2026-09-04 第六批：摘掉全图挂）=====    // 可见性考证（c.java renderWorld/stampThumbTile 实读）：引擎只有单层迷雾——
    // mapTiles 0x8000=未探索，已探索格上的敌单位/建筑/资源主视图与小地图都照常画
    // （renderWorld 第二遍 `s>0` 门；小地图 stampThumbTile 负值=黑雾、单位点经
    // 3591 行的 0x8000 门刷新）；0x4000 只影响地表贴图明暗，不构成第二层视野。
    // 故诚实规则 = 已探索=可见。视野来源（p()/dimFogAroundUnit/revealFogAroundUnit 实读）：单位揭开
    // 自身 3×3（每 tick + 移动时），建筑轮流揭半径 3、塔半径 6，探索永久累积。
    // 诚实模式下禁读 hdr[1] 全部字段（军值/资源/人口/TC 位=人类看不到的统计）；
    // 敌单位/建筑只认已探索格上的；敌 TC 位置改侦察记忆（见过一次即永久，TC 不动）。
    // 默认开；-Daoe.aiFog=0 回退全图（对照/调试用），回退路径与 v56 逐字节一致。
    private static final String AI_FOG = System.getProperty("aoe.aiFog", "1");
    // 消融档（仅诊断用）：=res → 资源全图（findResource 不过滤迷雾），敌情仍诚实。
    // 用于量化"资源位置信息"vs"敌情信息"各占多少胜率。
    private static final boolean FOG_RES_OMNI = "res".equals(AI_FOG);
    // =tc → 敌 TC 坐标全图（塔走廊/攻击目标直接可用），其余仍诚实。
    private static final boolean FOG_TC_OMNI = "tc".equals(AI_FOG);
    // ===== side 参数化（2026-09-07，-Daoe.enemyAi 反串 player 1）=====
    // 无参构造 = side 0（现状）；带参 side=1 = enemyAi 反串敌方。c.java 装载点
    // 优先取 (int) 构造。side 1 强制全图（player 1 无自有雾层，见类注释）；
    // FOG_RES_OMNI/FOG_TC_OMNI 消融档只挂在 fogHonest 门上，side 1 下天然无效。
    private final int side;
    private final int foe;
    private final boolean fogHonest;

    public RuleBasedAi() {
        this(0);
    }

    public RuleBasedAi(int side) {
        this.side = side;
        this.foe = 1 - side;
        this.fogHonest = side == 0 && !"0".equals(AI_FOG);
    }
    // Expert 全图攻坚旋钮（2026-09-06 第 41 夜，批测 A/B 用；验证后转默认）：
    private static final boolean EXP_WTFIRST =
        System.getProperty("aoe.expWtfirst", "0").equals("1");
    // 敌军 ≥2× 我军时全军收进 TC 塔火重叠区（保反杀部队，塔吃伤害）
    private static final boolean EXP_TURTLE =
        System.getProperty("aoe.expTurtle", "0").equals("1");
    // Expert 下铁匠铺前置（射箭场之前）+ t8 门槛 40/40→25/25——首波窗口的
    // 塔后投石机是集团近战的毁灭性反制（224 溅射/发 vs 塔 1.9/t）。
    // v41 默认采用：全图 Expert 两带 4/20→8/20（2026-09-06，aoe.expMangonel=0 回退）
    private static final boolean EXP_MANGONEL =
        System.getProperty("aoe.expMangonel", "1").equals("1");
    // Expert 围城交存线重建：矿场/伐木场被拆后威胁分支原样只许补塔——石配额
    // 挂 miningN>0，矿场一没采石断供→塔永远补不起→螺旋败（败局尸检：S=2/
    // towers=0/W=0 三连签名）
    private static final boolean EXP_CAMPREBUILD =
        System.getProperty("aoe.expCampRebuild", "0").equals("1");
    // Expert 木瓶颈双闸：Bow Saw 30→15（单伐木工 +50% 出木）+ t8 排队 1→2。
    // 败局经济学：G 反而在囤（78-157），W=0 是唯一卡点——村民替代（5W）/
    // 铁匠铺（25W）/补塔（22W）全部饿死在木上。
    private static final boolean EXP_ECO_KILL =
        System.getProperty("aoe.expEcoKill", "0").equals("1");
    // Horse Collar（磨坊 tech 6，10W/5G）：hdr[56] 训练速度再 +50%（磨坊完工已
    // +50%：1024→1536→2304，剑士 64t→28.3t/个）——波 2/3 消耗战补兵与 CRUSHED
    // 后重建成军的关键提速。unit-stats.md 旧注"木产量[50]+5"是数据表误注
    // （效果=hdr[56] 增量，c.java 磨坊完工/HC 研究 6233/6343 两级各 +50%）。
    private static final boolean EXP_HORSECOLLAR =
        System.getProperty("aoe.expHorsecollar", "0").equals("1");
    // ===== aoe.expStonePoor 石贫检测 + 替代策略分支（2026-09-06 第 45 夜探索）=====
    // 契约同 aiK.*：未设置/=0 → 逐字节当前行为（无检测、无遥测、无分支）。
    // report = 只打检测遥测（[ai] STONEPOOR 行），零行为差——分级/误判率取数用。
    // 1 = 检测 + V1 早闪击分支（Expert 门内）。检测语义见 stoneScan()。
    // 2 = V2 = V1 + 死战回撤（spRush 进攻的撤退阀 4→1、回撤冷却 1500→400）+
    // 触发兵力 5→6——V1 实测：5 民兵接敌掉 1 人即 RETREAT，闪击 256t 自爆。
    // 3 = V3 = 仅塔链优先重排（无闪击）：早期判贫图上 smith/射箭场让位到塔 3
    // 之后——起始 100 石是全部石料，smith 先建=偷走塔 3 的最后一份石
    // （1014 实证：smith@628 先于塔 3，波 1 时 m55=73 vs 1015 三塔 110=胜）。
    private static final String SP_PROP = System.getProperty("aoe.expStonePoor", "0").trim();
    private static final boolean SP_REPORT = SP_PROP.equals("report");
    private static final int SP_MODE = SP_REPORT ? 0 : Integer.parseInt(SP_PROP);
    private static final boolean SP_ON = SP_REPORT || SP_MODE != 0;
    // 近距石界（Chebyshev 格）：界内零可采石 = 塔链长期不可行（补塔/塔 4-5/smith
    // 都靠石收入）。aoe.spNear 覆盖便于 report 模式免重编译扫阈值。
    private static final int SP_NEAR =
        Integer.parseInt(System.getProperty("aoe.spNear", "14").trim());
    // ===== aiK build-order 旋钮（2026-09-06 逐种子离线搜索，B.5）=====
    // 契约：属性 aoe.aiK.<name> 未设置时默认值=改动前行为（逐字节一致，已用
    // 1000-1019 全 19 种子日志 diff 验证）。每个旋钮登记：名字/含义/默认值。
    // 历史禁区（配额类）只为逐种子特解保留——转正默认必须过 Step3 全局验收。
    private static int k(String n, int d) {
        String v = System.getProperty("aoe.aiK." + n);
        return v == null ? d : Integer.parseInt(v.trim());
    }
    // 军事生产总门：vills 下限（默认 3）——调低=军事更早起步，调高=经济先行
    private static final int K_MIL_VILLS = k("milVills", 3);
    // 近战木门（兵营 1/2 共用，默认 15）
    private static final int K_MELEE_W = k("meleeW", 15);
    // 近战金门：兵营 1 封建前（默认 25）/兵营 2 封建后（默认 15）
    private static final int K_MELEE_G1 = k("meleeG1", 25);
    private static final int K_MELEE_G2 = k("meleeG2", 15);
    // 弓兵木门（默认 25）
    private static final int K_ARCH_W = k("archW", 25);
    // 兵营建筑门 W/S（默认 30/15；战中重建与平时建链共用）
    private static final int K_BARR_W = k("barrW", 30);
    private static final int K_BARR_S = k("barrS", 15);
    // 塔门 W/G/S（默认 22/6/16）。塔 1/2 新建门 = 基础门 +6/+2/+4（默认 28/8/20，
    // 随旋钮联动）；战中补塔/塔 3-5 链用基础门
    private static final int K_TOWER_W = k("towerW", 22);
    private static final int K_TOWER_G = k("towerG", 6);
    private static final int K_TOWER_S = k("towerS", 16);
    // 塔数量上限（默认 5=TOWER_DIST[_EXPER] 表长；>5 时复用表末距离）
    private static final int K_TOWER_CAP = k("towerCap", 5);
    // 铁匠铺门（EXP_MANGONEL 前置支默认 25/15；射箭场后备支默认 30/22）
    private static final int K_SMITH_W = k("smithW", 25);
    private static final int K_SMITH_S = k("smithS", 15);
    private static final int K_SMITH2_W = k("smith2W", 30);
    private static final int K_SMITH2_S = k("smith2S", 22);
    // t8 生产门 W/G（默认 25/25）与排队上限（默认 1=现行为；expEcoKill 捆绑里的
    // 排队 2 未做过归因分解，此旋钮单独验证）
    private static final int K_T8_W = k("t8w", 25);
    private static final int K_T8_G = k("t8g", 25);
    private static final int K_T8_Q = k("t8q", 1);
    // Bow Saw 研究木门（默认 30）
    private static final int K_BOWSAW_W = k("bowsawW", 30);
    // GoldMining 最早研究 tick（默认 0=封建立即可研；StoneMining 走 smTowerUnder）
    private static final int K_GM_TICK = k("gmTick", 0);
    // StoneMining 研究的塔数上限门（默认 5；=0 可整体关掉 StoneMining 研究省费用）
    private static final int K_SM_TOWER_UNDER = k("smTowerUnder", 5);
    // 村民配额（历史禁区）：boot 木/金（默认 2/1）、封建 木/金/石（默认 1/2/1）、
    // 开战金配额（默认 3；石 1 时为 qWarG-1=2）、配额"塔满"界（默认 5）
    private static final int K_Q_BOOT_W = k("qBootW", 2);
    private static final int K_Q_BOOT_G = k("qBootG", 1);
    private static final int K_Q_FEUD_W = k("qFeudW", 1);
    private static final int K_Q_FEUD_G = k("qFeudG", 2);
    private static final int K_Q_FEUD_S = k("qFeudS", 1);
    private static final int K_Q_WAR_G = k("qWarG", 3);
    private static final int K_Q_TOWER_FULL = k("qTowerFull", 5);
    // ===== exm 战斗微操旋钮（2026-09-03，波 1 防御期承伤结构/杀伤率搜索）=====
    // 契约同 aiK.*：属性 aoe.exm.<name> 未设置时默认值=改动前行为（逐字节一致）。
    private static int exm(String n, int d) {
        String v = System.getProperty("aoe.exm." + n);
        return v == null ? d : Integer.parseInt(v.trim());
    }
    // 防御集火（1=开,v41 默认）：DEFEND 期未接敌军事单位显式指向同一目标格——可见
    // 敌军中 HP 最低者、平手取最近。先杀掉的敌人不再输出，1:N 时净承伤下降（战役
    // #6 v8 已验证的 doctrine，RuleBasedAi 从未试过）。retask 禁律：目标格未变不重写
    // slot[2]（防装填清零）；攻击态单位不重定向（打断=装填清零，且它已在输出）。
    // 两带 10/20（基线 8/20,8 存量胜全保,样本外 1020+ 带 +1）;aoe.exm.focus=0 回退。
    private static final int EXM_FOCUS = exm("focus", 1);
    // 围城期裸成本应急生产（0=关）：threat（DEFEND 激活）或任一完工塔 HP<220
    // 时军事门按裸成本放行——剑士 W 15→5、t8 25/25→20/20。败局经济学：G 全程
    // 囤积 31→129 而部队恒顶帽 5-6 无补充——卡点是 W≥15 缓冲门认不出"半死
    // 收入"（现有放行只认 woodW==0+无可采木）。排队不付款语义使放行的资源
    // 风险≈0（扣款在产出时，产出失败才退款路径不存在→放行只会多试不会囤积）。
    private static final int EXM_EMGPROD = exm("emgprod", 0);
    // 集火候选距离门（距防御锚点格²）。**64=8 格是几何最优**：恰覆盖紧凑塔环
    // 3/5/7/9/11 主力段;36=6 格候选进出半径导致目标抖动（丢 1012/1013）,
    // 100=10 格追出圈=v2 冲锋教训重演。不要调成可变参数。
    private static final int EXM_FOCUS_D2 = exm("focusD2", 64);
    // 闪避上限自适应（0=关，恒 milCount/2）：1=被围（敌≥2×我）时全员闪避；
    // 2=被围时只 1 人闪避（保输出，赌塔火杀得回来）
    private static final int EXM_DANCE = exm("dance", 0);
    // 修塔人数上限（默认 2=v44/v46 现行为）。塔 255HP、1 修理工≈抵 1 剑士磨塔，
    // 围城期修理容量直接=塔的存活时间。
    private static final int EXM_REPCAP = exm("repcap", 2);
    // 回血阈值（0=关：<100 撤 / >220 归）：1=撤 <130；2=归 >200；3=两者
    private static final int EXM_HEAL = exm("heal", 0);
    // 螺旋侦察路点参数（函数 spiralWaypoint/spiralCount 在文件底部；静态方法无前置
    // 声明顺序问题，但字段初始化器引用这些常量必须文本序在前——JLS 8.3.3）。
    private static final int SCOUT_RINGS = 16;      // 螺旋半径 3,5,…,33（全图覆盖）
    private static final int PROBE_RINGS = 6;       // 村民探路限前 6 环（半径 ≤13）
    private static final int SPIRAL_MAX = spiralCount(SCOUT_RINGS);
    private static final int PROBE_MAX = spiralCount(PROBE_RINGS);
    private static final int RAY_LEN = 16;          // 射线法路点数（步长 3 格，推进 6..51 格）
    private final boolean[] evis = new boolean[26]; // 本决策各敌单位槽可见性（已探索格上）
    private int enemyTcMem = -1;                    // 侦察记忆：敌 TC 格（见过即永久）
    private int enemyHint = -1;                     // 最近可见敌单位/建筑位置（驻防朝向降级用）
    private int enemyHintTick = -100000;
    private final int[] scoutIds = {-1, -1};        // 本决策的侦察兵槽位（[1]=第二侦察兵）
    private final int[] scoutCur = {0, SPIRAL_MAX / 2}; // 螺旋路点游标（第二侦察兵错开半圈）
    private int rayCursor;                          // 射线法路点游标（有首波来向后优先，仅 scout0）
    private int waveOrigin = -1;                    // 首波接触来向（最远可见敌兵格）
    private final int[] scoutLastP = {-1, -1};      // 侦察兵卡死检测
    private final int[] scoutLastT = new int[2];
    private final int[] villProbeCursor = new int[26]; // 村民探路游标（无可派资源时开图）
    private final int[] villProbeTick = new int[26];   // 探路命令发出时刻
    private final int[] villProbeLastWp = new int[26]; // 探针连续重发检测（不可达路点跳过）
    private final int[] villProbeRetry = new int[26];  // 同路点累计重发时长
    private final int[] villPullCursor = new int[26];  // 专职探员（probe-pull）独立游标——
                                                       // 与 idle 探针共写会互相踩（6/16 环
                                                       // 两套路标同索引不同格）
    private final int[] villPullLastWp = new int[26];
    private final int[] villPullRetry = new int[26];
    private final int[] villPullTick = new int[26];
    private boolean resScoutOn;                     // 资源侦察态（敌 TC 已知+木/金盲区时保持 1 侦察兵）
    private int stallTicks;                         // 僵尸局投降计时（无军事+无产能+木经济死）
    private int evisB;                              // 可见敌建筑数（日志用）
    private boolean bootLogged;
    // 石贫检测态（SP_ON 时才维护）：stonePoor 单向 latch（一旦判贫不翻回，防分支
    // 抖动）；未 latch 时每决策重扫（石会采竭——c.java:7393 采竭清资源位自然跌出
    // 统计）。stoneLastScan=上次扫描 tick（report 周期打点用）。
    // stonePoorTick=latch 时刻：V1 分支只认早期 latch（≤600t=波 1 窗口前判定才
    // 谈得上"主动出击"；战中 latch 是围城症状不是地图属性，v25 发波即冲已证伪）。
    private boolean stonePoor;
    private int stonePoorTick = -1;
    private int stoneLastScan = -100000;

    @Override
    public void tick(c game) {
        if (game.tickCount < this.prevTick) {
            // tickCount 回溯（-Daoe.devPhase 拨钟/devBoot 读旧档）：节流器按旧钟面
            // 积攒会冻结 AI（t<nextDecide 恒真）——CampaignAi 同款修复。
            this.nextDecide = 0;
        }
        this.prevTick = game.tickCount;
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
        int[] hdr = game.playerUnitHeaders[this.side];
        if (hdr[2] <= 0 && hdr[4] <= 0) {
            return;     // 未进任务或已全灭
        }

        // ===== 态势扫描 =====
        short[] slots = game.playerUnitSlots[this.side];
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
        int[] ehdr = game.playerUnitHeaders[this.foe];
        short[] eslots = game.playerUnitSlots[this.foe];
        int enemyMilVal = 0, enemyMilCount = 0;
        int eunits = Math.min(ehdr[2], 26);
        for (int i = 0; i < eunits; ++i) {
            int o = i << 3;
            int type = eslots[o + 3] & 0xFF;
            int t = eslots[o + 0] & 0xFFFF;
            // 诚实模式：只认已探索格上的敌单位（与渲染/小地图可见性一致）；
            // 军值按我方同兵种数值估（敌科技不可见），不读 ehdr 攻/甲字段。
            boolean vis = !this.fogHonest
                || (game.mapTiles[(t >>> 8) + ((t & 0xFF) << 6)] & 0x8000) == 0;
            this.evis[i] = vis;
            if (!vis) {
                continue;
            }
            this.enemyHint = t;
            this.enemyHintTick = game.tickCount;
            if (type >= 2) {
                ++enemyMilCount;
                enemyMilVal += this.fogHonest ? hdr[13 + type] + hdr[23 + type]
                    : ehdr[13 + type] + ehdr[23 + type];
            }
        }
        if (enemyMilCount > this.enemyMilPeak) {
            this.enemyMilPeak = enemyMilCount;
        }
        // 敌塔计入"防御军值"（v3：seed 1010 总攻 val 142 vs 72 仍败——没算敌 5 座塔的
        // 火力；塔完工加 hdr[55] 的值 = 塔甲 hdr[45] + 塔攻 hdr[46]）
        int[] erecs = game.buildingTable[this.foe];
        int enemyTowerVal = 0;
        this.evisB = 0;
        for (int i = 0; i < ehdr[4]; ++i) {
            int o = i << 2;
            int bt = erecs[o + 3] & 0xFF;
            if (this.fogHonest
                    && (game.mapTiles[(erecs[o + 0] >>> 8) + ((erecs[o + 0] & 0xFF) << 6)] & 0x8000) != 0) {
                continue;                            // 迷雾中的敌建筑不可见
            }
            ++this.evisB;
            this.enemyHint = erecs[o + 0];
            this.enemyHintTick = game.tickCount;
            if (this.fogHonest && bt == 9 && this.enemyTcMem < 0) {
                this.enemyTcMem = erecs[o + 0];      // 侦察记忆：敌 TC 见过即永久
                if (this.attackMode && !this.attackMuster) {
                    // 猎寻中转总攻：猎寻侧一直在拿 attackBestD2 做路点卡死检测，
                    // 不重置则大军距新 TC 较远时 1500t 无进展误触停滞撤军
                    this.attackBestD2 = Integer.MAX_VALUE;
                    this.attackBestTick = game.tickCount;
                }
                System.out.println("[ai] SCOUT enemy TC found at " + (erecs[o + 0] >>> 8) + ","
                    + (erecs[o + 0] & 0xFF) + " t=" + game.tickCount);
            }
            if (bt >= 12 && bt <= 15 && (erecs[o + 2] & 0x40000000) == 0) {
                enemyTowerVal += this.fogHonest ? hdr[45] + hdr[46] : ehdr[45] + ehdr[46];
            }
        }
        int enemyDefVal = enemyMilVal + enemyTowerVal;
        // 建筑扫描
        int[] recs = game.buildingTable[this.side];
        int houseN = 0, barracksDone = 0, archeryDone = 0, smithDone = 0;
        int lumberN = 0, miningN = 0, millN = 0, towerN = 0, stableDone = 0, siegeDone = 0;
        int tcSlot = -1, houseSlot = -1, barracksSlot = -1, archerySlot = -1, smithSlot = -1;
        int lumberSlot = -1, miningSlot = -1, towerSlot = -1, stableSlot = -1, siegeSlot = -1;
        int millSlot = -1;                            // 磨坊（Horse Collar 研究位）
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
                case 5:  ++millN; if (!uc && millSlot < 0) millSlot = o; break;
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
        // 诚实模式：敌 TC 位置只认侦察记忆（hdr[1][8] 禁读）；找不到时下游全部走
        // 降级姿态（塔环/驻防朝 hint/禁攻击，见各分支注释）。
        int enemyTc = this.fogHonest && !FOG_TC_OMNI ? this.enemyTcMem : ehdr[8];
        // 敌 TC 未知但已有首波接触来向：塔位/驻防朝向先朝来向摆（诚实信息——敌军
        // 从屏幕哪边进来玩家看得见）。v58 前未知敌方向只能摆罗盘塔环，seed 1006 型
        // 图塔落在背敌侧=村民暴露被屠。
        int enemyDir = enemyTc >= 0 ? enemyTc : (this.fogHonest ? this.waveOrigin : -1);
        this.resEnemyTc = enemyTc;
        this.resMyTc = myTc;
        this.resRelocateOn = game.aiGatherMultiplier >= 1024;   // v45：仅 Expert 开走廊过滤
        // 石贫检测（旋钮门内；默认关=零开销零行为差）
        if (SP_ON && myTc >= 0) {
            this.stoneScan(game, myTc, enemyTc);
        }
        if (!this.bootLogged) {
            this.bootLogged = true;
            System.out.println("[ai] RuleBasedAi side=" + this.side + " fogHonest=" + this.fogHonest
                + (this.side != 0 ? " (forced omni: player 1 无自有雾层)" : ""));
        }
        // 波次预测器（v24）：只读+打点，行为不变。诚实模式禁用（ehdr[55] 禁读）。
        if (!this.fogHonest) {
            this.trackWaves(game, hdr, ehdr, eslots, eunits, myTc, enemyTc);
        }
        // 难度感知（v15）：Expert（采集 ×8 + 每 tick 出兵尝试 + 免费资源滴）下
        // 敌兵是磨不完的，v13/v14 的 7 兵 CRUSHED 反击等于把仅有的家底送进
        // 敌塔环（Expert 基线 4 连败同一样态：反击送军→下一波破家）。Expert 上
        // 反击门槛抬高到 10 兵且军值 ≥ 敌塔计入后的防御军值。
        boolean expert = game.aiGatherMultiplier >= 1024;
        // 波次窗口模型（阈值感知，2026-09-06 阈值矩阵批测）：敌 all-in 触发 =
        // 敌军值 ≥ aiAttackThreshold 且 > 我军值×0.8（tickAi:8543 实读），首波到时
        // 随阈值近似线性放大——实测 T=50→~2.3k、T=100→~5.6k、T=150→~5.0k。
        // 取保守下界 waveTick = 1000 + T×22（T=50→2.1k 早于实测，高 T 逐档提前），
        // 只用于排序/窗口，不改任何内部常量；aiAttackThreshold 是难度面板公开
        // 参数（玩家选难度即知），不属 hdr[1] 隐藏信息，诚实模式可读。
        int waveTick = 1000 + game.aiAttackThreshold * 22;

        // v38 蹲守标记（Expert；第六批起诚实模式全难度——标记只由可见敌兵产生，
        // 本身是诚实信息）：每个敌军事单位 8 格窗口内的资源格全部拉黑
        // CAMP_COOLDOWN tick（滚动窗=敌不走标记不消，敌走了也留 1500t 滞后防振荡）。
        // 26 敌兵 × 17×17 窗口 ≈ 7.5k 格/决策，开销可忽略。
        this.goldCamped = false;
        if (expert || this.fogHonest) {
            for (int i = 0; i < eunits; ++i) {
                int o = i << 3;
                if ((eslots[o + 3] & 0xFF) < 2 || !this.evis[i]) {
                    continue;
                }
                int t = eslots[o + 0] & 0xFFFF;
                int ex = t >>> 8, ey = t & 0xFF;
                for (int dy = -8; dy <= 8; ++dy) {
                    int ty = ey + dy;
                    if (ty < 0 || ty >= 64) {
                        continue;
                    }
                    for (int dx = -8; dx <= 8; ++dx) {
                        int tx = ex + dx;
                        if (tx < 0 || tx >= 64 || dx * dx + dy * dy > CAMP_D2) {
                            continue;
                        }
                        int idx = tx + (ty << 6);
                        int tt = game.mapTiles[idx] & 0xFFF;
                        if ((tt & 0x300) != 0x300) {
                            continue;
                        }
                        this.resCampedUntil[idx] = game.tickCount + CAMP_COOLDOWN;
                        if ((tt & 3) == 2) {
                            this.goldCamped = true;
                        }
                    }
                }
            }
        }

        // ===== 军事模块 =====
        // 1) 找离我方 TC 最近的敌军事单位（回防目标）+ 最近的敌投石机（t8 优先点杀，
        //    远程拆塔克星，agent-operations §5.1/r29）
        int tcx = myTc >= 0 ? myTc >>> 8 : 0, tcy = myTc >= 0 ? myTc & 0xFF : 0;
        int invaderTile = -1, invaderD2 = Integer.MAX_VALUE, invaderN = 0;
        int mangonelTile = -1, mangonelD2 = Integer.MAX_VALUE;
        int farTile = -1, farD2 = -1;               // 可见敌兵中离我 TC 最远者（来向估计）
        if (myTc >= 0) {
            for (int i = 0; i < eunits; ++i) {
                int o = i << 3;
                int type = eslots[o + 3] & 0xFF;
                if (type < 2 || !this.evis[i]) {
                    continue;
                }
                int t = eslots[o + 0] & 0xFFFF;
                int dx = (t >>> 8) - tcx, dy = (t & 0xFF) - tcy;
                int d2 = dx * dx + dy * dy;
                if (d2 <= RAID_SCAN_D2) {
                    ++invaderN;
                }
                if (d2 > farD2) {
                    farD2 = d2;
                    farTile = t;
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
                if ((eslots[o + 3] & 0xFF) < 2 || !this.evis[i]) {
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
            // 首波接触来向（诚实模式）：静默 500t 后再次接触时，记录离我 TC 最远的
            // 可见敌兵格（最接近迷雾边界的来向）作为定向侦察射线起点。首波任意规模
            // 即记；射线扫完（RAY_LEN 路点）仍未找到 TC 时，下一波接触重新锚定
            // 射线再扫——人类玩家看敌军从屏幕哪边进来就往哪边找，同理。
            if (this.fogHonest && this.enemyTcMem < 0 && farTile >= 0
                    && game.tickCount - this.lastContactTick > 500
                    && (this.waveOrigin < 0 || (this.rayCursor >= RAY_LEN && invaderN >= 2))) {
                this.waveOrigin = farTile;
                this.rayCursor = 0;
                System.out.println("[ai] CONTACT dir " + (farTile >>> 8) + "," + (farTile & 0xFF)
                    + " t=" + game.tickCount);
            }
            this.lastContactTick = game.tickCount;
        }
        // 侦察兵选拔（诚实模式）：主目标=敌 TC 未知时开图找 TC；辅目标=资源侦察
        // （敌 TC 已找到后军事侦察本会停摆，但木/金整类不可见时经济仍需开图——
        // seed 1000 实锤：TC 附近无森林，200 起始木耗尽后全图无木源，村民探针又限
        // 前 6 环半径 ≤13 够不到远林 → 生产永久断供 21M tick 僵局）。资源侦察保持
        // 1 名（milCount≥3 抽闲，不夺战时人手），找到资源即自动收队。
        this.scoutIds[0] = -1;
        this.scoutIds[1] = -1;
        boolean resScout = this.fogHonest && this.enemyTcMem >= 0 && myTc >= 0 && !this.attackMode
            && milCount >= 3
            && (findResource(game, myTc, 1, game.tickCount) < 0
                || findResource(game, myTc, 2, game.tickCount) < 0);
        if (this.resScoutOn != resScout) {
            this.resScoutOn = resScout;
            if (resScout) {
                System.out.println("[ai] RES-SCOUT on (resource blind spot) t=" + game.tickCount);
            }
        }
        if (this.fogHonest && !FOG_TC_OMNI && (this.enemyTcMem < 0 || resScout) && myTc >= 0 && !this.attackMode
                && (!threat || milCount >= 3)) {
            int firstMil = -1, secondMil = -1, firstT5 = -1;
            for (int i = 0; i < units; ++i) {
                int type = slots[(i << 3) + 3] & 0xFF;
                if (type < 2) {
                    continue;
                }
                if (type == 5) {
                    if (firstT5 < 0) {
                        firstT5 = i;
                    }
                    continue;
                }
                if (firstMil < 0) {
                    firstMil = i;
                } else if (secondMil < 0) {
                    secondMil = i;
                }
            }
            this.scoutIds[0] = firstT5 >= 0 ? firstT5 : firstMil;
            if (milCount >= 6) {
                this.scoutIds[1] = firstT5 >= 0 ? firstMil : secondMil;
            }
        }
        // 僵尸局投降（批测契约，-Daoe.exitOnResult 才生效；CampaignAi 同款）：
        // 无军事单位、无兵营系建筑（含在建）、木经济死（存量<5+无木工+无可采木）
        // ——一切兵种成本含木、军事建筑 20 木起，产能永不可恢复，村民又无力拆
        // 敌 TC；引擎判负式（TC 毁 / 0单位+0建筑）够不着 → 实测空转 21M tick。
        // 持续 500t 按契约认输省批测 240s/局；真实游玩无 exitOnResult 只打日志。
        // side==1（enemyAi 反串）整个跳过——[result]/System.exit 是 player-0 视角
        // 契约，敌方认输由人类玩家自己的胜利结算（z=98）表达。
        if (this.side == 0 && System.getProperty("aoe.exitOnResult") != null && myTc >= 0) {
            boolean noMilBuilding = barracksDone == 0 && archeryDone == 0 && stableDone == 0
                && siegeDone == 0 && !hasUC(recs, hdr[4], 10) && !hasUC(recs, hdr[4], 7)
                && !hasUC(recs, hdr[4], 8) && !hasUC(recs, hdr[4], 2);
            // 收入死断 = 无在岗工人 + 全探索区无可采格。可达性推导（非阈值枚举）：
            // 单位永不可产 = 采集收入死断后存量永远够不到成本（全兵种 ≥5W+5G）；
            // 兵营永不可建 = 木/石同理（20W/10S，无金）。二者合一即产能永不可
            // 恢复，村民无力拆敌 TC，引擎判负式够不着 → 按契约认输（僵尸局
            // 实测空转 21M/10M tick：seed 1000 木=5 金=708 烂在库里）。
            // 村民死锁补洞（2026-09-06，1006 型僵尸）：村民全灭 + 存量木 <5
            // （房屋训练村民 5W，买不起）+ 无在训村民（hdr[66]=0）= 劳动力永不
            // 可恢复——findResource 找得到可采木也再无人去采，原推导把这种局判成
            // "收入未死"（woodW==0 但有可采格 → woodIncomeDead=false →
            // canReachUnit 恒真），投降门永远差一口气（Medium seed 1006 两批连续
            // 空转到墙钟 STALL 实锤：村民 0、木 0、塔在场，敌磨不动塔）。死锁即
            // 视同三系收入全死（没人采 = 没有收入）。保守边界：木 ≥5 可立刻补员
            // 不投；有在训村民不投（含产出时卡 canAfford 的队列——宁可晚投一局
            // 到墙钟，不可误投可翻盘局）。
            boolean villDead = vills == 0 && hdr[5] < 5 && hdr[66] == 0;
            boolean woodIncomeDead = villDead || (woodW == 0
                && findResource(game, myTc, 1, game.tickCount) < 0);
            boolean goldIncomeDead = villDead || (goldW == 0
                && findResource(game, myTc, 2, game.tickCount) < 0);
            boolean stoneIncomeDead = villDead || (stoneW == 0
                && findResource(game, myTc, 3, game.tickCount) < 0);
            boolean canReachUnit = (!woodIncomeDead || hdr[5] >= 5)
                && (!goldIncomeDead || hdr[6] >= 5);
            boolean canReachBarracks = !noMilBuilding
                || ((!woodIncomeDead || hdr[5] >= 20)
                    && (!stoneIncomeDead || hdr[7] >= 10));
            this.stallTicks = milCount <= 1 && !(canReachUnit && canReachBarracks)
                ? this.stallTicks + DECIDE_EVERY : 0;
            // mil≤1（而非 ==0）：单兵无产能支撑永不可胜（1 单位拆不动 TC），
            // 却会让投降门永远差一口气（seed 1006 T=150 实测 mil=1 val=8、
            // 兵营无、金 0 木 165 烂库、僵到超时）。
            if (this.stallTicks >= 500) {
                System.out.println("[ai] concede: no military, no production path ("
                    + (villDead ? "villager deadlock" : "income dead") + "), t="
                    + game.tickCount);
                System.out.println("[result] LOSS ticks=" + game.tickCount);
                System.out.flush();
                System.exit(0);
            }
        } else {
            this.stallTicks = 0;
        }
        // EXP_TURTLE 判定（DEFEND/猎手/闪避共用）：敌军 ≥2× 我军
        boolean expOutnumbered = EXP_TURTLE && expert && enemyMilCount >= milCount * 2;
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
            if (expert && defendAnchor >= 0 && AiKit.corridorLen(defendAnchor, defendTile) > 3) {
                meleeTile = AiKit.stanceTile(defendAnchor, defendTile, 3);
            }
            // EXP_TURTLE（Expert）：敌军 ≥2× 我军时全军收进 TC 塔火重叠区——
            // 敌索敌优先打塔，军队在环内存活等 CRUSHED 反杀窗口；在圈缘被
            // 3 倍兵力集火=全军歼灭（Expert 败局签名：mil 4-7 → 0,村民随后
            // 被猎杀）。1:1 时维持环形拦截。
            boolean outnumbered = expOutnumbered;
            if (outnumbered) {
                meleeTile = myTc >= 0 ? AiKit.stanceTile(myTc, defendTile, 1) : defendTile;
            }
            int rangedTile;
            if (mangonelTile >= 0 && mangonelD2 <= 400) {
                rangedTile = mangonelTile;
            } else {
                rangedTile = defendAnchor >= 0 ? AiKit.stanceTile(defendTile, defendAnchor, 3) : defendTile;
            }
            if (outnumbered) {
                rangedTile = meleeTile;      // 全龟：远程也收进重叠区
            }
            // EXM_FOCUS 集火目标：可见敌军中 HP 最低（平手取最近）、距防御锚点
            // ≤EXM_FOCUS_D2 才入候选（防追出塔火圈）。敌 HP 全图挂可读
            // （eslots[o+4]&0xFF）；无候选回退原驻点行为。expert 门内（含遥测——
            // 非 Expert 不算不打，否则 Medium 的 DEFEND 日志会带 focus 注释）。
            int focusTile = -1, focusHp = -1;
            if (expert && EXM_FOCUS == 1 && defendAnchor >= 0) {
                int fax = defendAnchor >>> 8, fay = defendAnchor & 0xFF;
                int fHp = 0x100, fD2 = Integer.MAX_VALUE;
                for (int j = 0; j < eunits; ++j) {
                    int eo = j << 3;
                    if ((eslots[eo + 3] & 0xFF) < 2 || !this.evis[j]) {
                        continue;
                    }
                    int et = eslots[eo + 0] & 0xFFFF;
                    int fdx = (et >>> 8) - fax, fdy = (et & 0xFF) - fay;
                    int fd2 = fdx * fdx + fdy * fdy;
                    if (fd2 > EXM_FOCUS_D2) {
                        continue;
                    }
                    int ehp = eslots[eo + 4] & 0xFF;
                    if (ehp < fHp || (ehp == fHp && fd2 < fD2)) {
                        fHp = ehp;
                        fD2 = fd2;
                        focusTile = et;
                    }
                }
                if (focusTile >= 0) {
                    focusHp = fHp;
                }
            }
            if (expert) {
                // v34 逐单位下令：攻击态（action==1）单位不动——群令 orderMove 会清
                // slot[7] 攻击计数器并把缠斗中的兵拉开，48t 一次的重发等于周期性
                // 抹掉全军装填 + 来回踱步（持续 DPS 暗坑）。回撤/猎手单位也不动。
                for (int i = 0; i < units; ++i) {
                    int o = i << 3;
                    int type = slots[o + 3] & 0xFF;
                    if (type < 2 || (slots[o + 7] & 0xF) == 1
                            || this.healUntil[i] > game.tickCount || this.huntingM[i]
                            || i == this.scoutIds[0] || i == this.scoutIds[1]) {
                        continue;
                    }
                    int tgt = focusTile >= 0 ? focusTile
                        : (type == 4 || type == 8) ? rangedTile : meleeTile;
                    if ((slots[o + 2] & 0xFFFF) != tgt) {
                        slots[o + 1] = slots[o + 0];
                        slots[o + 2] = (short) tgt;
                        slots[o + 7] = 0;
                        slots[o + 3] = (short) (slots[o + 3] & 0xFF);
                    }
                }
            } else {
            game.selectUnits(this.side, -1);
            game.orderMove(this.side, meleeTile >>> 8, meleeTile & 0xFF);
            game.selectUnits(this.side, 4);
            game.orderMove(this.side, rangedTile >>> 8, rangedTile & 0xFF);
            game.selectUnits(this.side, 8);
            game.orderMove(this.side, rangedTile >>> 8, rangedTile & 0xFF);
            game.clearSelection();
            }
            if (invaderD2 <= 64 || defendAnchor != myTc) {
                // mil=N 注记：残局全军尽没后本行每 48t 照打（指令自然空转），
                // 不带 mil 会把 mil=0 的假 focus 行当真实防御指挥误读（game3
                // 2758-2998 尸检教训）
                System.out.println("[ai] DEFEND invader " + invaderN + " at " + (defendTile >>> 8) + ","
                    + (defendTile & 0xFF) + (defendAnchor != myTc ? " (tower)" : "")
                    + " mil=" + milCount
                    + (focusTile >= 0 ? " focus " + (focusTile >>> 8) + "," + (focusTile & 0xFF)
                        + " hp" + focusHp : "")
                    + " t=" + game.tickCount);
            }
        }
        // 进攻中遇到小股反扑不回家（塔顶着），大股才撤（敌防御模式 87.5% 反扑我 TC 的应对）
        if (this.attackMode && threat && invaderN > RAIDERS_IGNORE) {
            this.attackMode = false;
            this.attackSpRush = false;
            this.attackCooldownUntil = game.tickCount + 800;
            System.out.println("[ai] attack ABORTED, " + invaderN + " raiders home t=" + game.tickCount);
        }
        // 2) 反击/总攻判定：敌主力被歼（从峰值跌到 1/3）/ 碾压 / 僵持兜底。
        //    v1 教训（M1 报告）：没碾平敌主力就逼近敌基 = 替对面开 87.5% 反扑开关。
        //    v9：贴脸图（走廊 ≤14 格）敌兵常驻我警戒圈 → !threat 永远假 → 永不能反击
        //    （seed 1019 实锤）；closeRush 因此豁免 threat 门。进攻态下小股骚扰不回防
        //    （DEFEND 块已被 attackMode 抑制，塔顶着），大股才中止进攻。
        //    诚实模式：敌 TC 未知（enemyTc<0）时不能发起总攻（没有目标），驻防/诱敌
        //    照常；僵持超时后走 HUNT 猎寻兜底（全军螺旋开图找 TC）。
        if (myTc >= 0) {
            boolean crushed = this.enemyMilPeak >= 5 && enemyMilCount <= Math.max(2, this.enemyMilPeak / 3);
            boolean overwhelm = milVal >= OVERWHELM_VAL && milVal * 3 >= enemyDefVal * 4;
            boolean desperate = game.tickCount > DESPERATE_TICK && milCount >= MIN_ATTACK_UNITS
                && milVal >= enemyDefVal;
            // 贴脸图闪击（v8：敌我 TC ≤14 格时消耗战必输——敌 3.07× 经济 + 8 格补给线，
            // 拖=被三倍产量磨死，seed 1013/1015/1019 连续实证）：兵力不劣就走，
            // 家里靠塔接敌防御模式的反扑残兵。
            boolean closeRush = AiKit.corridorLen(myTc, enemyTc) <= 14 && milCount >= 7
                && milVal >= enemyMilVal;
            boolean goCrushed = (crushed || closeRush) && milCount >= MIN_ATTACK_UNITS - 1
                && milVal >= enemyMilVal;
            if (expert) {
                // Expert：只打有把握的反击——10 兵以上且军值压过敌塔计入后的防御军值
                goCrushed = (crushed || closeRush) && milCount >= 10 && milVal >= enemyDefVal;
            }
            // 金竭突击（诚实模式 v64）：金已不可派（采尽/余量全在未探索迷雾）且
            // 存量金不够再训（G<15）——兵力不会再增长，与其坐等被磨死（Easy
            // 1001/1006 金竭站桩败局实锤），不如用现有兵力搏拆 TC。
            boolean goldStarve = this.fogHonest && enemyTc >= 0 && milCount >= 5
                && milVal >= enemyDefVal && hdr[6] < 15 && goldW == 0
                && findResource(game, myTc, 2, game.tickCount) < 0;
            // 木竭突击（seed 1000 型死局兜底）：木<5 且无木工且无可采木=一切兵种
            // （成本全含木）与军事建筑（兵营 20 木起）永不可得，生产链已死——带
            // 现有部队 all-in，赢或死都好过无限僵局。金竭看"存量+军值还够"，
            // 木竭看"生产要素断供"，故不设军值门；静默窗防开图早期误判
            // （敌 TC 刚找到、森林可能还没探到——资源侦察会继续找）。
            boolean woodStarve = this.fogHonest && enemyTc >= 0 && milCount >= 1
                && hdr[5] < 5 && woodW == 0
                && findResource(game, myTc, 1, game.tickCount) < 0
                && game.tickCount - this.lastContactTick > BAIT_QUIET;
            // 时间窗闪击（阈值矩阵批测，T≥150 弱带主修）：高阈值下首波远
            // （敌要攒满 aiAttackThreshold 军值才出发），其军事积累期近乎真空
            // （T=150 seed 1001 实证：peak=0 整整 4.6k tick）——敌 TC 一旦侦察
            // 到位且波尚远，开局部队全部砸向敌基地杀村民断收入（其大波经济永不
            // 成立），其防御模式反扑的小股军事喂我方塔。窗口随阈值连续缩放：
            // T=50 时 2.1k 内 TC 很难探到+波已在途=天然不触发，T≥150 窗口打开。
            boolean timeRush = this.fogHonest && enemyTc >= 0 && milCount >= 5
                && game.tickCount + 1500 < waveTick
                && milVal >= enemyMilVal
                && game.tickCount >= this.attackCooldownUntil;
            // 石贫乏闪击 SPRUSH（V1，aoe.expStonePoor=1）：早期判贫（latch ≤600t）
            // 的石贫图塔链无后继（无石收入补塔/扩塔），塔防吸收 all-in 的胜线物理
            // 上打折——趁敌军事积累期（波 1 阈值未过）把开局部队压到敌基地杀村民
            // 断收入，把 e55 压回阈值下=波永不成立。不设军值门：敌积累期军值≈0-30，
            // 门槛只会空转窗口。不集结（同 TIMERUSH）。波若照常发，现有 ABORTED/
            // DEFEND 链自动退化为标准塔防（闪击只是前置赌注，不破坏退路）。
            boolean spRush = SP_MODE >= 1 && SP_MODE <= 2 && expert && this.stonePoor
                && this.stonePoorTick >= 0 && this.stonePoorTick <= 600
                && enemyTc >= 0 && milCount >= (SP_MODE >= 2 ? 6 : 5)
                && game.tickCount + 1500 < waveTick;
            if (!this.attackMode && !threat && game.tickCount >= this.attackCooldownUntil
                    && enemyTc >= 0 && (goCrushed || overwhelm || desperate || goldStarve || woodStarve
                        || timeRush || spRush)) {
                this.attackMode = true;
                this.attackMuster = !(timeRush || spRush);     // 闪击不集结：抢的就是窗口
                this.attackSpRush = spRush;
                this.musterStart = game.tickCount;
                this.musterTile = AiKit.stanceTile(enemyTc, myTc, 7); // 敌 TC 朝我 7 格（警戒圈 6 格外沿）
                this.lastAttackOrder = -100000;
                this.attackBestD2 = Integer.MAX_VALUE;
                System.out.println("[ai] ATTACK enemy TC " + (enemyTc >>> 8) + "," + (enemyTc & 0xFF)
                    + " mil=" + milCount + "(val " + milVal + ") vs enemy " + enemyMilCount
                    + "(val " + enemyMilVal + ", peak " + this.enemyMilPeak + ")"
                    + (crushed ? " CRUSHED" : "") + (overwhelm ? " OVERWHELM" : "")
                    + (desperate ? " DESPERATE" : "") + (closeRush ? " CLOSERUSH" : "")
                    + (goldStarve ? " GOLDSTARVE" : "") + (woodStarve ? " WOODSTARVE" : "")
                    + (timeRush ? " TIMERUSH(wave@" + waveTick + ")" : "")
                    + (spRush ? " SPRUSH(sp@" + this.stonePoorTick + ")" : "") + " t=" + game.tickCount);
            }
            // 猎寻（诚实模式兜底）：敌 TC 始终未找到且进入僵持期（15k 后）→ 全军
            // 沿侦察路点（有首波来向走射线，否则螺旋）扫荡开图；TC 入侦察记忆后
            // 下个决策攻击目标自动切到敌 TC（下方共用一个块）。
            else if (!this.attackMode && !threat && this.fogHonest && enemyTc < 0
                    && game.tickCount >= this.attackCooldownUntil
                    && game.tickCount > BAIT_MIN_TICK && milCount >= 5) {
                this.attackMode = true;
                this.attackMuster = false;                   // 猎寻无集结，直奔路点
                this.lastAttackOrder = -100000;
                this.attackBestD2 = Integer.MAX_VALUE;
                this.attackBestTick = game.tickCount;
                System.out.println("[ai] ATTACK HUNT (enemy TC unknown) mil=" + milCount
                    + " t=" + game.tickCount);
            }
            if (this.attackMode && milCount <= (this.attackSpRush && SP_MODE >= 2 ? 1 : RETREAT_LEFT)) {
                this.attackMode = false;
                // V2 spRush 死战回撤：撤退阀 4→1（V1 实测 5 民兵接敌掉 1 人即撤=
                // 闪击 256t 自爆）+ 冷却 1500→400（快速重组二波冲，原冷却直接
                // 关上 t<1700 的重冲窗口）
                this.attackCooldownUntil = game.tickCount
                    + (this.attackSpRush && SP_MODE >= 2 ? 400 : 1500);
                this.attackSpRush = false;
                game.selectUnits(this.side, -1);
                game.orderMove(this.side, tcx, tcy);
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
                    game.selectUnits(this.side, -1);
                    game.orderMove(this.side, mx, my);
                    game.clearSelection();
                }
            }
            if (this.attackMode && !this.attackMuster && game.tickCount - this.lastAttackOrder >= ATTACK_REISSUE) {
                this.lastAttackOrder = game.tickCount;
                // 敌 TC 未知（HUNT 猎寻中）：目标=当前侦察路点（射线/螺旋）；
                // TC 一旦入侦察记忆，下个周期这里自动切到敌 TC。
                int tgt = enemyTc >= 0 ? enemyTc : scoutTarget(myTc);
                game.selectUnits(this.side, -1);
                game.orderMove(this.side, tgt >>> 8, tgt & 0xFF);
                game.clearSelection();
            }
            if (this.attackMode && !this.attackMuster) {
                int target = enemyTc >= 0 ? enemyTc : scoutTarget(myTc);
                int etx = target >>> 8, ety = target & 0xFF;
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
                if (enemyTc < 0) {
                    // 猎寻：先头兵到达路点 → 推进下一个；路点不可达（1500t 无逼近
                    // 进展）→ 跳过该路点（无此兜底时 seed 1007 型图全军卡在不可达
                    // 路点上空转 7.8M tick 到超时）；不跑停滞撤军，兵力打光由
                    // RETREAT_LEFT 兜底
                    if (best <= 9) {
                        scoutAdvance();
                    } else if (best < this.attackBestD2) {
                        this.attackBestD2 = best;
                        this.attackBestTick = game.tickCount;
                    } else if (game.tickCount - this.attackBestTick > 1500) {
                        scoutAdvance();
                        this.attackBestD2 = Integer.MAX_VALUE;
                        this.attackBestTick = game.tickCount;
                    }
                } else if (best < this.attackBestD2) {
                    this.attackBestD2 = best;
                    this.attackBestTick = game.tickCount;
                } else if (game.tickCount - this.attackBestTick > 1500) {
                    // 进攻停滞（敌塔群/重建兵挡住）：撤回重整，攒下一波
                    this.attackMode = false;
                    this.attackSpRush = false;
                    this.attackCooldownUntil = game.tickCount + 2000;
                    game.selectUnits(this.side, -1);
                    game.orderMove(this.side, tcx, tcy);
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
                // 敌 TC 未知（诚实模式）：优先朝首波接触来向驻防，其次最近目击方向；
                // 都没有回 TC
                int stanceTgt = enemyDir >= 0 ? enemyDir
                    : (this.enemyHint >= 0 && game.tickCount - this.enemyHintTick < 5000
                        ? this.enemyHint : myTc);
                int stance = AiKit.stanceTile(myTc, stanceTgt, stanceDist);
                game.selectUnits(this.side, -1);
                game.orderMove(this.side, stance >>> 8, stance & 0xFF);
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
            int healTile = enemyDir >= 0 ? AiKit.stanceTile(myTc, enemyDir, -3) : myTc;
            // EXM_HEAL 回血阈值（消耗战老兵存活率）：更早撤（<130）更晚归（>200 的
            // 反向=更早归 >200）。默认 100/220；expert 门内（Easy/Medium 恒原值）。
            int healOut = expert && (EXM_HEAL == 1 || EXM_HEAL == 3) ? 130 : 100;
            int healBack = expert && (EXM_HEAL == 2 || EXM_HEAL == 3) ? 200 : 220;
            for (int i = 0; i < units; ++i) {
                int o = i << 3;
                if ((slots[o + 3] & 0xFF) < 2) {
                    continue;
                }
                int hp = slots[o + 4] & 0xFF;
                if (this.healUntil[i] > game.tickCount) {
                    if (hp > healBack) {
                        this.healUntil[i] = 0;      // 回满归队（下个群令自然收编）
                        continue;
                    }
                    if ((slots[o + 2] & 0xFFFF) != healTile) {
                        slots[o + 1] = slots[o + 0];
                        slots[o + 2] = (short) healTile;
                        slots[o + 7] = 0;
                        slots[o + 3] = (short) (slots[o + 3] & 0xFF);
                    }
                } else if (threat && hp < healOut && milCount > 4) {
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
        // 我方 slot[2]（resolveAttack 反扑语义），靠每决策重投续上。猎手不参与闪避。
        java.util.Arrays.fill(this.huntingM, false);
        if (expert && !expOutnumbered && !this.attackMode && myTc >= 0) {
            int huntersLeft = Math.min(4, Math.max(1, milCount / 2));
            for (int j = 0; j < eunits && huntersLeft > 0; ++j) {
                int eo = j << 3;
                if ((eslots[eo + 3] & 0xFF) != 8 || !this.evis[j]) {
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
                                || this.huntingM[i] || this.healUntil[i] > game.tickCount
                                || i == this.scoutIds[0] || i == this.scoutIds[1]) {
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
        // slot[7] 清零重来（tickUnits() case 1），敌近战出手需攒 8 tick 装填。被锁定单位
        // 每决策（8t）横移一步 = 敌装填永远攒不满还要重新走位，我方其余单位原地
        // 继续输出——把 1:1 换血变成 0:N。落点强制留在塔火圈（hdr[12]，封建后 25）
        // 内，防 v2 式被拉出塔圈风筝；自己装填将满（再 1-2t 出手）时让这一击打完。
        // 闪避人数上限 milCount/2，防全员后撤零输出。
        if (expert && !this.attackMode && myTc >= 0) {
            int coverR2 = hdr[12];
            int maxDancers = Math.max(1, milCount / 2);
            // EXM_DANCE 闪避上限自适应（被围=敌≥2×我）：1=全员走位（承伤结构优先，
            // 敌装填全清）；2=只 1 人走位（保输出密度，赌塔火杀得回来）
            if (EXM_DANCE == 1 && enemyMilCount >= milCount * 2) {
                maxDancers = milCount;
            } else if (EXM_DANCE == 2 && enemyMilCount >= milCount * 2) {
                maxDancers = 1;
            }
            int dancers = 0;
            for (int i = 0; i < units && dancers < maxDancers; ++i) {
                int o = i << 3;
                int myType = slots[o + 3] & 0xFF;
                if (myType < 2 || this.healUntil[i] > game.tickCount || this.huntingM[i]
                        || i == this.scoutIds[0] || i == this.scoutIds[1]) {
                    continue;
                }
                int pos = slots[o + 0] & 0xFFFF;
                int px = pos >>> 8, py = pos & 0xFF;
                boolean focused = false;
                for (int j = 0; j < eunits; ++j) {
                    int eo = j << 3;
                    int eType = eslots[eo + 3] & 0xFF;
                    if (eType < 2 || !this.evis[j]) {
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
                        int sc = dodgeScore(eslots, eunits, nx, ny, coverR2);                        if (sc > bestScore) {
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
                if ((slots[o + 3] & 0xFF) >= 2 || this.repairUntil[i] > game.tickCount) {
                    continue;                        // 军事单位/修塔工（v44）不参与逃命
                }
                int pos = slots[o + 0] & 0xFFFF;
                int px = pos >>> 8, py = pos & 0xFF;
                int near = -1, nd2 = Integer.MAX_VALUE;
                for (int j = 0; j < eunits; ++j) {
                    int eo = j << 3;
                    if ((eslots[eo + 3] & 0xFF) < 2 || !this.evis[j]) {
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
                // v38 逃命不失业（Expert；第六批起诚实模式全难度）：优先撤到"最近
                // 未被蹲守的同种资源格"直接复工——v35/v36 败局复盘的真身：围城期交战区
                // 就是采集区，附近资源全在逃命半径内，撤空地=收入归零。camped 格自带
                // ≥8 格敌距（> 逃命触发 7），落地即可干活；找不到才回退旧 fleeTile。
                // 保持原工种=配额中性，不与再平衡逻辑互激。诚实模式下只派已探索格。
                int flee = -1;
                if (expert || this.fogHonest) {
                    int vk = 0;
                    int va = slots[o + 7] & 0xF;
                    if (va == 2 || va == 3) {
                        vk = (slots[o + 7] & 0xF0) >> 4;
                    } else {
                        int vtgt = slots[o + 2] & 0xFFFF;
                        int vt = game.mapTiles[(vtgt >>> 8) + ((vtgt & 0xFF) << 6)] & 0xFFF;
                        if ((vt & 0x300) == 0x300) {
                            vk = vt & 3;
                        }
                    }
                    if (vk >= 1 && vk <= 3) {
                        flee = findResource(game, pos, vk, game.tickCount);
                    }
                }
                if (flee < 0) {
                    flee = fleeTile(game, px, py, near >>> 8, near & 0xFF, tcx, tcy);
                }
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
            // 科技对称化（2026-09-07）后 side 1 研究/升时代真生效（c.java
            // tickBuildings i==1 分支 + enemyTechFlags + hdr[1][0] 时代），
            // 封建判定两侧同口径：自己的 hdr[0]>=1。
            boolean feudal = hdr[0] >= 1;
            // 村民配额（v3 起：金优先——军事单位全吃金，木头永远过剩；三败全是金=0
            // 僵尸队列饿死的）。boot 期 2木1金；封建后 1木2金，塔未满 5 座压 1 人采石；
            // 开战后（敌亮过 6+ 兵）且塔≥3 → 1木3金 全力暴兵。
            int woodTarget = K_Q_FEUD_W, goldTarget = K_Q_FEUD_G, stoneTarget = 0;
            if (!feudal) {
                woodTarget = K_Q_BOOT_W;
                goldTarget = K_Q_BOOT_G;
                // enemyAi 反串（side 1）：引擎给 player 1 的起始资源远少于 player 0
                // （Easy 50/15/15 vs 200/100/100，c.java 难度分支）——boot 配额 2木1金
                // 无石工，建完兵营 S 剩 5，封建（15S）永远攒不够（Easy 实测 S=5 锁定，
                // 3.2M tick 不升时代、永不进攻）。石存量不够封建且已有矿场可交存时
                // boot 期也保 1 石工。side 0 起始 100 石永远不进此分支（零行为差）。
                if (this.side == 1 && hdr[7] < 15 && miningN > 0) {
                    stoneTarget = 1;
                }
            } else if (miningN > 0 && towerN < K_Q_TOWER_FULL) {
                stoneTarget = K_Q_FEUD_S;
            }
            if (this.enemyMilPeak >= 6 && towerN >= 3 && miningN > 0) {
                // 开战状态（敌亮过 6+ 兵）：石料 buffered/塔满才 3 金全力暴兵；
                // 否则保 1 石工——石断供会卡死塔 4/5→磨坊→马厩链条，军值摸不到
                // OVERWHELM 门槛（Easy seed 1002 七百万 tick 对峙实证）。
                // v18/v19 教训：Expert 上砍金工保 2 木 + 兵营木门槛降 8 的组合 =
                // 暴兵断粮/塔补不起，两连版 1/9（v17 4/9），全量回滚。
                // v41（第五批）：铁匠铺未起也要保 1 石工——v40 seed 1001 实锤：
                // 5 塔完工后 stoneTarget=0 → 石恒 5-17 < smith 门槛 25 → smith/射箭场
                // 永动机位锁死，W/G 囤 165/173 无产能转化，6 剑士被磨到城破。
                stoneTarget = (towerN < K_Q_TOWER_FULL || (feudal && smithDone == 0 && !hasUC(recs, hdr[4], 6)))
                        && hdr[7] < 25 ? 1 : 0;
                goldTarget = stoneTarget == 1 ? K_Q_WAR_G - 1 : K_Q_WAR_G;
            }
            // 诚实模式资源发现兜底（v60-v62）：木或金整类**无可派格**（与派工同一
            // 个 findResource 口径——v60/v61 用"已探索"口径被死水死角林卡死：格已探
            // 但八邻格不可站=永远派不了工，探针却提前收工，Easy 1001 全程 W=5 实锤）时：
            // ① 本决策前两个闲村民改派探路（近环）；② 没有闲村民（全员在岗）且非
            // 威胁期，直接抽序号最小的村民当专职探员（全环）。单项状态翻转（两类资源
            // 都可派后永久停止），不与配额再平衡互激。
            boolean needDiscovery = this.fogHonest
                && (findResource(game, myTc, 1, game.tickCount) < 0
                    || findResource(game, myTc, 2, game.tickCount) < 0);
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
                if (r >= 0 && needDiscovery && k <= 1) {
                    r = -1;                  // 前两个闲村民改去探资源盲区（v61：单探针
                }                            // 开图太慢，Expert 波 1 前塔数被木头卡死）
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
                } else if (r < 0 && this.fogHonest && myTc >= 0) {
                    // 已探索区内无该种资源（诚实模式）：派去近环探路开图（单位视野 3×3
                    // 随走随开），发现资源后下个决策自动回岗。只在零收入态触发，
                    // 不占配额、不参与再平衡。
                    int vi = idleVill[k];
                    int wp = spiralWaypoint(myTc,
                        (this.villProbeCursor[vi] + vi * 11) % PROBE_MAX, PROBE_RINGS);
                    // 不可达路点跳过：引擎对不可达/被占位目标即时取消移动（pos==tgt
                    // → 视作 idle → 下拍重发），slot[2]==wp 与 pos==wp 的时刻都不存在
                    // ——800t 超时门永远够不着（活锁实测 t=1504-5280 每 8t 重发
                    // (25,46) 永不推进）。改按"同一路点累计重发 400t=走不到"跳过：
                    // 正常可行路点行军中不重发（不在 idle 名单），不会误跳。
                    if (wp == this.villProbeLastWp[vi]) {
                        this.villProbeRetry[vi] += DECIDE_EVERY;
                    } else {
                        this.villProbeLastWp[vi] = wp;
                        this.villProbeRetry[vi] = 0;
                    }
                    if ((slots[o + 0] & 0xFFFF) == wp || this.villProbeRetry[vi] > 400) {
                        ++this.villProbeCursor[vi];
                        wp = spiralWaypoint(myTc,
                            (this.villProbeCursor[vi] + vi * 11) % PROBE_MAX, PROBE_RINGS);
                        this.villProbeLastWp[vi] = wp;
                        this.villProbeRetry[vi] = 0;
                    }
                    if ((slots[o + 2] & 0xFFFF) != wp) {
                        slots[o + 1] = slots[o + 0];
                        slots[o + 2] = (short) wp;
                        slots[o + 7] = 0;
                        slots[o + 3] = (short) (slots[o + 3] & 0xFF);
                        this.villProbeTick[vi] = game.tickCount;
                        System.out.println("[ai] probe villager " + vi + " -> "
                            + (wp >>> 8) + "," + (wp & 0xFF) + " t=" + game.tickCount);
                    }
                }
            }
            // 发现兜底②（v62）：全员在岗无闲村民时，抽序号最小的村民当专职探员
            // （全环螺旋；非威胁期——威胁期逃命链优先，且战中开图靠军事侦察兵）。
            // 行走中的探针不重复改写（避免每决策清装填式拉扯）；800t 走不到换路点。
            // v63：Expert 不抽工——波 1 前的经济是以 tick 计的，少 1 个村民 400t 的
            // 代价比晚发现资源更大（Easy/Medium 保留：那里有经济冗余，v62 Easy
            // 5/10→7/10 实锤收益）。
            if (needDiscovery && !threat && !expert) {
                int pv = -1;
                for (int i = 0; i < units; ++i) {
                    if ((slots[(i << 3) + 3] & 0xFF) < 2) {
                        pv = i;
                        break;
                    }
                }
                if (pv >= 0 && this.repairUntil[pv] <= game.tickCount) {
                    int o = pv << 3;
                    int wp = spiralWaypoint(myTc,
                        (this.villPullCursor[pv] + pv * 11) % SPIRAL_MAX, SCOUT_RINGS);
                    // 独立游标/重发检测（同 idle 探针的不可达跳过；此块与 idle 探针
                    // 可能先后作用于同一村民，共用游标会 6/16 环两套路标互相踩）。
                    if (wp == this.villPullLastWp[pv]) {
                        this.villPullRetry[pv] += DECIDE_EVERY;
                    } else {
                        this.villPullLastWp[pv] = wp;
                        this.villPullRetry[pv] = 0;
                    }
                    if ((slots[o + 0] & 0xFFFF) == wp || this.villPullRetry[pv] > 400) {
                        ++this.villPullCursor[pv];
                        wp = spiralWaypoint(myTc,
                            (this.villPullCursor[pv] + pv * 11) % SPIRAL_MAX, SCOUT_RINGS);
                        this.villPullLastWp[pv] = wp;
                        this.villPullRetry[pv] = 0;
                    }
                    int cur = slots[o + 2] & 0xFFFF;
                    if (cur != wp) {
                        slots[o + 1] = slots[o + 0];
                        slots[o + 2] = (short) wp;
                        slots[o + 7] = 0;
                        slots[o + 3] = (short) (slots[o + 3] & 0xFF);
                        this.villPullTick[pv] = game.tickCount;
                        System.out.println("[ai] probe-pull villager " + pv + " -> "
                            + (wp >>> 8) + "," + (wp & 0xFF) + " t=" + game.tickCount);
                    }
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
                    && houseSlot >= 0 && queueLen(recs, houseSlot) < 1 && game.canAfford(this.side, 0, 0)) {
                game.queueUnitTraining(this.side, 0);
            }
            // 科技（tryResearch 内部已校验时代/前置/已研究；失败静默下个周期再试）。
            // v6 教训（v5 退步实锤）：GoldMining/WatchTower 必须尽早就位——它们直接
            // 决定 3-8k 首波窗口的金收入与塔生存（v5 推迟到 8k，金价 4-11 饿死暴兵，
            // 三颗 v4 胜种全翻负）。科技不是军费黑洞，持续收入才是。
            // 2026-09-07 科技对称化：side 1 解禁（研究完成效果已 per-player 化，
            // 形态键 spawnAgeKey 与队列记账同口径）；side 1 的日志带 side=1 后缀。
            String sideTag = this.side == 1 ? " side=1" : "";
            if (!feudal && barracksDone > 0 && tcSlot >= 0 && game.canAfford(this.side, 2, 21)) {
                if (game.tryResearch(this.side, tcSlot, 21)) {
                    System.out.println("[ai] research FEUDAL t=" + game.tickCount + sideTag);
                }
            }
            if (feudal) {
                // Watch Tower 先研（EXP_WTFIRST，仅 Expert）：10/5/10 换塔甲
                // 10→15+索敌² 16→25，是首波窗口最便宜的战略倍增器——Expert 败局
                // 全在 ~2.3-2.9k 首波窗口，Forging(+1 攻) 换不了塔的生存。
                if (EXP_WTFIRST && towerSlot >= 0 && game.canAfford(this.side, 2, 13)
                        && game.tryResearch(this.side, towerSlot, 13)) {
                    System.out.println("[ai] research WatchTower(wtfirst) t=" + game.tickCount + sideTag);
                }
                if (smithSlot >= 0 && game.canAfford(this.side, 2, 4) && game.tryResearch(this.side, smithSlot, 4)) {
                    System.out.println("[ai] research Forging t=" + game.tickCount + sideTag);
                }
                if (!EXP_WTFIRST && towerSlot >= 0 && game.canAfford(this.side, 2, 13) && game.tryResearch(this.side, towerSlot, 13)) {
                    System.out.println("[ai] research WatchTower t=" + game.tickCount + sideTag);
                }
                if (miningSlot >= 0 && game.tickCount >= K_GM_TICK
                        && game.canAfford(this.side, 2, 5) && game.tryResearch(this.side, miningSlot, 5)) {
                    System.out.println("[ai] research GoldMining t=" + game.tickCount + sideTag);
                }
                if (miningSlot >= 0 && towerN < K_SM_TOWER_UNDER && game.canAfford(this.side, 2, 9)
                        && game.tryResearch(this.side, miningSlot, 9)) {
                    System.out.println("[ai] research StoneMining t=" + game.tickCount + sideTag);
                }
                if (lumberSlot >= 0 && game.canAfford(this.side, 2, 3) && game.tryResearch(this.side, lumberSlot, 3)) {
                    System.out.println("[ai] research DoubleBitAxe t=" + game.tickCount + sideTag);
                }
                // v40 Bow Saw（木产量 10→15，第五批）：围城期木桶是唯一硬约束
                // （v38 败局 5/6 木=0 卡死一切，金/石反囤），单木工 +50% 收入=续命。
                // W≥30 门槛防与战中补塔(22 木)抢木料（v43 降到 15 实测 3/10 回滚——
                // 木紧的局连 15 的窗口都踩不中，反而扰动 build 链）。
                if (lumberSlot >= 0 && hdr[5] >= (expert && EXP_ECO_KILL ? 15 : K_BOWSAW_W)
                        && game.canAfford(this.side, 2, 1)
                        && game.tryResearch(this.side, lumberSlot, 1)) {
                    System.out.println("[ai] research BowSaw t=" + game.tickCount + sideTag);
                }
                // Horse Collar（磨坊 tech 6，10W/5G）：训练速度第二级 +50%——
                // 波 2/3 消耗战补兵/CRUSHED 后重建成军提速（64t→28.3t/剑士）。
                if (EXP_HORSECOLLAR && millSlot >= 0 && game.canAfford(this.side, 2, 6)
                        && game.tryResearch(this.side, millSlot, 6)) {
                    System.out.println("[ai] research HorseCollar t=" + game.tickCount + sideTag);
                }
                if (smithSlot >= 0 && hdr[6] >= 25 && game.canAfford(this.side, 2, 8) && game.tryResearch(this.side, smithSlot, 8)) {
                    System.out.println("[ai] research ScaleMail t=" + game.tickCount + sideTag);
                }
                // 城堡时代（磨坊+铁匠≥2）：再 +1 攻/甲 + Guard Tower，富余才升。
                // v23 试过 Expert 放宽到 30/25/25 + 磨坊提到射箭场前：2/9 不升反降
                // （射箭场推迟丢了投石机反制），回滚。
                if (hdr[0] == 1 && millN > 0 && smithDone > 0 && tcSlot >= 0
                        && hdr[5] >= 40 && hdr[6] >= 40 && hdr[7] >= 40
                        && game.tryResearch(this.side, tcSlot, 22)) {
                    System.out.println("[ai] research CASTLE t=" + game.tickCount + sideTag);
                }
            }
            if (hdr[0] >= 2) {
                if (smithSlot >= 0 && game.canAfford(this.side, 2, 7) && game.tryResearch(this.side, smithSlot, 7)) {
                    System.out.println("[ai] research IronCasting t=" + game.tickCount + sideTag);
                }
                if (smithSlot >= 0 && game.canAfford(this.side, 2, 2) && game.tryResearch(this.side, smithSlot, 2)) {
                    System.out.println("[ai] research ChainMail t=" + game.tickCount + sideTag);
                }
                if (towerSlot >= 0 && game.canAfford(this.side, 2, 17) && game.tryResearch(this.side, towerSlot, 17)) {
                    System.out.println("[ai] research GuardTower t=" + game.tickCount + sideTag);
                }
            }
            // 建筑（一次一座，自动成型；找位失败/被占下个决策再试）
            // v5/v6：交战中（敌兵 12 格内）禁放非塔建筑——seed 1019 兵临城下时连放
            // 4 座铁匠铺全被秒拆，白烧 100 木 80 石；塔例外（战中补塔=战力，且敌军
            // 索敌优先打塔，在建塔也是 255 HP 的仇恨海绵）。
            if (!anyUC) {
                int need = -1, anchor = myTc;
                int[] tdist = expert ? TOWER_DIST_EXPERT : TOWER_DIST;
                int tcap = Math.min(K_TOWER_CAP, tdist.length);   // aiK 塔数量上限
                // V3 石贫塔链优先（aoe.expStonePoor=3）：早期判贫图上 smith/射箭场
                // 让位到塔 3 之后（起始 100 石是全部石料，smith 先建=偷走塔 3 的
                // 最后一份石）。塔 1-3 分支恒在 smith 前，deferred 只是解除资源竞争。
                boolean spReSeq = SP_MODE == 3 && expert && this.stonePoor
                    && this.stonePoorTick >= 0 && this.stonePoorTick <= 600
                    && towerN + ucCount(recs, hdr[4], 12) < 3;
                if (threat) {
                    // 交战中只补塔（敌 12 格内）：seed 1019 兵临城下连放 4 座铁匠铺全被
                    // 秒拆白烧 100 木 80 石；塔例外——战中补塔=战力，且敌军索敌优先打塔，
                    // 在建塔也是 255 HP 的仇恨海绵。v14：门槛压到塔成本(20/5/15)附近，
                    // 塔被拆能第一时间原地补（原 30/8/18 在金石枯竭的消耗战里永远不够）。
                    // v55 试过塔环 ≤2 座即停补（囤石给产能）：3/10 回滚——补塔买的
                    // 时间本身就是等到非战窗口的前提，停补=立刻崩。
                    // 围城逃生门：塔补不起（金/石差一口气）时允许战中重建兵营——
                    // TC 旁 255HP 海绵最坏被拆，好过军事断绝被无限磨（T=150
                    // seed 1002 实锤：G=5 永远差 1 金补塔，兵营锁死 mil=0 磨到 8.9k 败）。
                    // 不设兵数门：mil=1 时威胁期既不训练（无兵营）也派不出侦察
                    // （mil<3），敌 TC 永远找不到=全机器锁死（seed 1006 phase35
                    // 实测金 3909/石 2025/木 170 烂库僵 6.9M tick）。
                    if (towerN < tcap && hdr[5] >= K_TOWER_W && hdr[6] >= K_TOWER_G && hdr[7] >= K_TOWER_S) {
                        need = 12;
                        anchor = corridorAnchor(myTc, enemyTc, enemyDir, tdistAt(tdist, towerN), towerN);
                    } else if (barracksDone == 0 && !hasUC(recs, hdr[4], 10)
                            && hdr[5] >= K_BARR_W && hdr[7] >= K_BARR_S) {
                        need = 10;
                    } else if (expert && EXP_CAMPREBUILD
                            && ((miningN == 0 && !hasUC(recs, hdr[4], 1))
                                || (lumberN == 0 && !hasUC(recs, hdr[4], 0)))
                            && hdr[5] >= 15) {
                        // EXP_CAMPREBUILD：交存线重建（矿场→采金/采石、伐木场→木）。
                        // 石配额挂 miningN>0、木/石交存挂营地——营地被拆=收入线
                        // 永久断供（败局尸检：S=2、towers=0、W=0 三连签名）。
                        // 原地重建（15 木，放下自动成型，敌索敌优先打塔）。
                        if (miningN == 0) {
                            need = 1;
                            anchor = findResource(game, myTc, hdr[7] < 20 ? 3 : 2, game.tickCount);
                            if (anchor < 0) {
                                anchor = findResource(game, myTc, hdr[7] < 20 ? 2 : 3, game.tickCount);
                            }
                        } else {
                            need = 0;
                            anchor = findResource(game, myTc, 1, game.tickCount);
                        }
                        if (anchor < 0) {
                            need = -1;
                        }
                    } else if (expert && houseN + ucCount(recs, hdr[4], 11) < 4
                            && hdr[3] < 25 && hdr[5] >= 10) {
                        // v36 围城期补房（第五批）：人口帽被敌磨掉=金囤着变不成兵
                        // （v35 基线败局复盘：seed 1009 pop cap 掉到 15、金囤 119 用不掉；
                        // 1005/1007 cap 20 同理）。房 5 木=最便宜的人口+255HP 仇恨海绵，
                        // 门槛远低于补塔(22/6/16)，木 <22 时也不与塔抢资源。
                        need = 11;
                    }
                } else if (houseN == 0 && hdr[5] >= 5) {
                    need = 11;                                   // 房屋 1：村民训练前置 + 人口
                } else if (lumberN == 0 && !this.noWoodRes && hdr[5] >= 20
                        && findResource(game, myTc, 1, game.tickCount) >= 0) {
                    // 条件里先扫到才进分支（锚点不可达=没探索到木头时**必须让链条
                    // 继续往下走**到兵营——v57 曾在条件里给 anchor 赋值，找不到时
                    // 冲掉 myTc 并吞掉整条链，兵营被卡 1300+t）；体内再扫一次取值，
                    // 同决策内结果确定一致。
                    need = 0;
                    anchor = findResource(game, myTc, 1, game.tickCount);
                } else if (lumberN == 0 && !this.noWoodRes && hdr[5] >= 20 && !this.fogHonest) {
                    this.noWoodRes = true;   // 全图模式：全图无木才锁存（v56 语义）
                } else if (barracksDone == 0 && !hasUC(recs, hdr[4], 10) && hdr[5] >= K_BARR_W && hdr[7] >= K_BARR_S) {
                    need = 10;                                   // 兵营：出兵 + 封建前置
                } else if (towerN + ucCount(recs, hdr[4], 12) < 1
                        && hdr[5] >= K_TOWER_W + 6 && hdr[6] >= K_TOWER_G + 2 && hdr[7] >= K_TOWER_S + 4) {
                    need = 12;                                   // 走廊塔 1：敌 rush 最早 ~3.5k（近距图），塔必须先就位
                    anchor = corridorAnchor(myTc, enemyTc, enemyDir, tdist[0], 0);
                } else if (miningN == 0 && !this.noGoldRes && hdr[5] >= 20
                        && findResource(game, myTc, 2, game.tickCount) >= 0) {
                    need = 1;                                // 同伐木场：先扫到才进分支
                    anchor = findResource(game, myTc, 2, game.tickCount);
                } else if (miningN == 0 && !this.noGoldRes && hdr[5] >= 20 && !this.fogHonest) {
                    this.noGoldRes = true;   // 全图模式：全图无金才锁存（v56 语义）
                } else if (towerN + ucCount(recs, hdr[4], 12) < 2
                        && hdr[5] >= K_TOWER_W + 6 && hdr[6] >= K_TOWER_G + 2 && hdr[7] >= K_TOWER_S + 4) {
                    need = 12;                                   // 走廊塔 2
                    anchor = corridorAnchor(myTc, enemyTc, enemyDir, tdist[1], 1);
                } else if (expert && houseN < 4 && hdr[5] >= 5) {
                    need = 11;                                   // Expert：塔 2 后立刻补满 4 房——×8 消耗战
                } else if (expert && feudal && barracksDone < 2 && !hasUC(recs, hdr[4], 10)  // 里金囤着花不出去全是
                        && hdr[5] >= 25 && hdr[7] >= 12) {                                 // pop 上限卡的（v16b 败局
                    need = 10;                                   // v21：第二兵营提到塔 3-5 前——v17 败局复盘：
                                                                 // （v50 试过射箭场取代兵营2：3/10 回滚——
                                                                 //  兵营1 被拆后的重建路径也要走这格，
                                                                 //  且弓兵 10 木/个在石贫图挤占补塔木）
                } else if (expert && towerN < tcap && (towerN < 3 || this.enemyMilPeak < 6)
                        && !hasUC(recs, hdr[4], 12)
                        && hdr[5] >= K_TOWER_W && hdr[6] >= K_TOWER_G && hdr[7] >= K_TOWER_S) {
                    need = 12;                                   // v47：塔 1-3 恒优先（波 1 最低防线），
                    anchor = corridorAnchor(myTc, enemyTc, enemyDir, tdistAt(tdist, towerN), towerN); // 塔 4-5 战前优先、战后让位给
                }                                                // 射箭场/铁匠铺（v48 全压 3 座实测：
                                                                 // 石贫图 1006 波 1 少 2 塔提前 2500t 崩盘，
                                                                 // 回滚到 peak<6 门；v53 加 S≥40 石富门
                                                                 // 实测 4/10 但 3 败局提前死，回滚）。
                                                                 // 战后塔 4-5 落回通用塔分支（smith 后）。
                else if (expert && miningN + ucCount(recs, hdr[4], 1) < 2 && hdr[5] >= 30
                        && hdr[10] > 0 && this.mineAreaCamped(game, hdr[10], game.tickCount)) {
                    // v40 修正第二采矿场触发（v38 bug 实测：任意金格被蹲就触发，锚点落在
                    // 第一矿场 2 格外=同一片矿白建，seed 1009 白烧 15 木实锤）。改为：
                    // 我方矿场(hdr[10]) 10 格内有金格被蹲才触发，锚点强制距矿场1 ≥8 格。
                    need = 1;
                    anchor = this.findSecondGold(game, myTc, hdr[10], game.tickCount);
                } else if (expert && EXP_MANGONEL && feudal && smithDone == 0 && !spReSeq
                        && !hasUC(recs, hdr[4], 6) && hdr[5] >= K_SMITH_W && hdr[7] >= K_SMITH_S) {
                    need = 6;            // EXP_MANGONEL：铁匠铺提到射箭场前（t8 是波 1 的
                                         // 杀伤率倍增器；门槛 25/15≈成本+缓冲）
                } else if (expert && feudal && archeryDone == 0 && !hasUC(recs, hdr[4], 7) && !spReSeq
                        && hdr[5] >= 30 && hdr[7] >= 12) {
                    // v42 Expert：射箭场提到铁匠铺前。败局复盘（v41）：7/7 败局 smith 未建
                    // ——石被战中补塔(16S/次)持续抽干，S 恒 5-17 够不到 smith 的 S≥25 门槛，
                    // 无 smith 则剑士 5 帽封顶、军值 36 锁死。射箭场只要 S≥15（补塔间隙
                    // 的窗口够得着），+4 弓兵位+反制敌 t8；smith 随后跟上。
                    // v56：门槛 35/15→30/12——v47 败局的石恒在 11-14 振荡，差 1-2 点
                    // 永远够不到 15；成本只要 25W/10S，边际 5W/2S 的保险换成解锁产能。
                    need = 7;
                } else if (expert && feudal && smithDone == 0 && !hasUC(recs, hdr[4], 6) && !spReSeq
                        && hdr[5] >= K_SMITH2_W && hdr[7] >= K_SMITH2_S) {
                    need = 6;                                    // v56 Expert：smith 石门槛 25→22（成本 20S，
                } else if (feudal && smithDone == 0 && !hasUC(recs, hdr[4], 6) && !spReSeq && hdr[5] >= 35 && hdr[7] >= 25) {  // 边际 2S）；Medium 走下一条共享分支不变
                    need = 6;                                    // 铁匠铺：攻防升级 + 投石机（产 t8 的建筑）
                } else if (feudal && archeryDone == 0 && !hasUC(recs, hdr[4], 7) && !spReSeq && hdr[5] >= 35 && hdr[7] >= 15) {
                    need = 7;                                    // 射箭场：弓兵反制敌投石机
                } else if (towerN < tcap && !hasUC(recs, hdr[4], 12)
                        && hdr[5] >= K_TOWER_W && hdr[6] >= K_TOWER_G && hdr[7] >= K_TOWER_S) {
                    need = 12;                                   // 走廊塔 3-5：阶梯前推（v3 提到房屋/磨坊前，
                    anchor = corridorAnchor(myTc, enemyTc, enemyDir, tdistAt(tdist, towerN), towerN); // 塔是抗消耗主力；v6/v15 降门槛保战中补塔）
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
                        int rc = game.a(this.side, need, tx, ty, 0x40000000, true);
                        System.out.println("[ai] build type=" + need + " at " + tx + "," + ty
                            + " rc=" + rc + " res=" + hdr[5] + "/" + hdr[6] + "/" + hdr[7]
                            + " t=" + game.tickCount);
                    }
                }
            }
            // 军事生产：各建筑并行排队 ≤2（产兵扣款在产出时，排多不亏只占人口名额）。
            // 投石机 20 金/台，金 <40 时让位给剑士/弓兵（队列不清，产出时 canAfford 卡）。
            // 金收入已死（无金工+无可采金）时封建门按裸成本放行——家底花光好过锁死
            // （T=150 seed 1002 实锤：G=5、兵营还在、木 240，却因 G≥25 门训不出兵，
            // 塔也差 1 金补不起，mil=0 被磨到 21M 僵局）。
            boolean goldIncomeDead = goldW == 0
                && findResource(game, myTc, 2, game.tickCount) < 0;
            boolean woodIncomeDead = woodW == 0
                && findResource(game, myTc, 1, game.tickCount) < 0;
            // 围城信号（EXM_EMGPROD 用）：DEFEND 已激活或任一完工塔 HP<220。
            // 败局实态是"1 伐木工活着但 W 恒 5-20"——半死收入放不出 15 木缓冲，
            // 兵被歼后补位窗整个错过（G 反而囤到 129 无处可花）。
            boolean siege = threat;
            if (!siege) {
                for (int si = 0; si < hdr[4]; ++si) {
                    int so = si << 2;
                    int sbt = recs[so + 3] & 0xFF;
                    if ((recs[so + 2] & 0x40000000) != 0 || sbt < 12 || sbt > 15) {
                        continue;
                    }
                    if ((recs[so + 2] & 0xFF) < 220) {
                        siege = true;
                        break;
                    }
                }
            }
            boolean emg = EXM_EMGPROD != 0 && siege;
            if (popRoom && barracksDone > 0 && vills >= K_MIL_VILLS) {
                // 形态键 = 自己的时代 hdr[0]（科技对称化后引擎 spawn 形态键
                // spawnAgeKey(side)：enemyAi 局内 side 1 键 hdr[1][0]，与本键
                // 同口径，队产一致；side 0 时 hdr==playerUnitHeaders[0]，与原
                // feudal?3:2 完全等价）。
                int meleeType = hdr[0] >= 1 ? 3 : 2;
                if (canTrain(hdr, meleeType) && queueLen(recs, barracksSlot) < 2
                        && hdr[5] >= (woodIncomeDead ? 5 : emg ? 5 : K_MELEE_W)
                        && (feudal || hdr[6] >= (goldIncomeDead ? 5 : emg ? 5 : K_MELEE_G1))
                        && game.canAfford(this.side, 0, meleeType)) {
                    game.queueUnitTraining(this.side, meleeType);
                }
                if (barracks2Slot >= 0 && canTrain(hdr, meleeType) && queueLen(recs, barracks2Slot) < 2
                        && hdr[5] >= K_MELEE_W && feudal && hdr[6] >= K_MELEE_G2
                        && game.canAfford(this.side, 0, meleeType)) {
                    // 第二兵营（Expert）要自己写队列位——queueUnitTraining 只会排到同类型
                    // 第一座建筑（c.java:6519 按记录序扫），语义照抄该原语：排队不付款，
                    // 产出时 spawn 路径 canAfford 硬检后才扣。
                    recs[barracks2Slot + 2] += 65536;
                    hdr[49] += 1;
                    hdr[66 + meleeType - 1] += 1;
                }
                if (archeryDone > 0 && archerySlot >= 0 && canTrain(hdr, 4)
                        && queueLen(recs, archerySlot) < 2 && hdr[5] >= K_ARCH_W
                        && game.canAfford(this.side, 0, 4)) {
                    game.queueUnitTraining(this.side, 4);
                }
                if (smithDone > 0 && smithSlot >= 0 && canTrain(hdr, 8)
                        && queueLen(recs, smithSlot) < (expert ? (EXP_ECO_KILL ? 2 : K_T8_Q) : 1)
                        && hdr[5] >= (EXP_MANGONEL ? emg ? 20 : K_T8_W : 40)
                        && hdr[6] >= (EXP_MANGONEL ? emg ? 20 : K_T8_G : 40)
                        && game.canAfford(this.side, 0, 8)) {
                    game.queueUnitTraining(this.side, 8);    // 投石机产自铁匠铺（case 8 → 建筑 6）
                }
                if (stableDone > 0 && stableSlot >= 0 && canTrain(hdr, 5)
                        && queueLen(recs, stableSlot) < 2 && hdr[5] >= 25 && hdr[6] >= 30
                        && game.canAfford(this.side, 0, 5)) {
                    game.queueUnitTraining(this.side, 5);    // 封建产侦察，城堡自动转骑兵（tickBuildings() case 8）
                }
                if (siegeDone > 0 && siegeSlot >= 0 && canTrain(hdr, 7)
                        && queueLen(recs, siegeSlot) < 1 && hdr[5] >= 40 && hdr[6] >= 60
                        && game.canAfford(this.side, 0, 7)) {
                    game.queueUnitTraining(this.side, 7);    // 冲车产自攻城工坊（case 7 → 建筑 2）
                }
            }
        }

        // ===== v44 修塔（仅 Expert；必须在经济模块之后跑，覆盖闲置重派的写目标） =====
        if (expert && myTc >= 0) {
            // 残血修理工提前退役：剑士打村民 64 伤/击（甲 1），HP<200=已吃一击
            // 即放（回逃命链），HP<80 才放等于送第二刀（v45 复盘：修理工钉在塔边
            // 被集火，败局村民死亡 12-13 个全在塔环坐标）
            for (int i = 0; i < units; ++i) {
                if (this.repairUntil[i] > game.tickCount && (slots[(i << 3) + 4] & 0xFF) < 200) {
                    this.repairUntil[i] = 0;
                }
            }
            // 最残的完工塔/TC（TC 毁=即败，修 TC 优先级天然最高——同表一起比 HP）
            int repTile = -1, repHp = 245;
            for (int i = 0; i < hdr[4]; ++i) {
                int o = i << 2;
                int bt = recs[o + 3] & 0xFF;
                if ((recs[o + 2] & 0x40000000) != 0) {
                    continue;
                }
                if (!((bt >= 12 && bt <= 15) || bt == 9)) {
                    continue;
                }
                int hp = recs[o + 2] & 0xFF;
                if (hp < repHp) {
                    repHp = hp;
                    repTile = recs[o + 0];
                }
            }
            if (repTile < 0) {
                java.util.Arrays.fill(this.repairUntil, 0);   // 无受损建筑：全员解禁
            } else {
                int repairers = 0;
                for (int i = 0; i < units; ++i) {
                    int o = i << 3;
                    if (this.repairUntil[i] > game.tickCount) {
                        // 已在修这栋：不动（写 slot[2] 会清 action 4 的修理态）；
                        // 在修别栋/走路：重指向最残栋
                        if ((slots[o + 7] & 0xF) == 4 && (slots[o + 5] & 0xFFFF) == repTile) {
                            ++repairers;
                            continue;
                        }
                        this.repairUntil[i] = game.tickCount + 600;
                        ++repairers;
                        if ((slots[o + 2] & 0xFFFF) != repTile) {
                            slots[o + 1] = slots[o + 0];
                            slots[o + 2] = (short) repTile;
                            slots[o + 7] = 0;
                            slots[o + 3] = (short) (slots[o + 3] & 0xFF);
                        }
                    }
                }
                for (int pick = 0; pick < EXM_REPCAP && repairers < EXM_REPCAP; ++pick) {
                    int bestI = -1, bestD2 = Integer.MAX_VALUE;
                    for (int i = 0; i < units; ++i) {
                        int o = i << 3;
                        if ((slots[o + 3] & 0xFF) >= 2 || this.repairUntil[i] > game.tickCount
                                || (slots[o + 4] & 0xFF) < 220) {
                            continue;
                        }
                        int up = slots[o + 0] & 0xFFFF;
                        int dx = (up >>> 8) - (repTile >>> 8), dy = (up & 0xFF) - (repTile & 0xFF);
                        int d2 = dx * dx + dy * dy;
                        if (d2 > 196) {
                            continue;                          // 14 格内才拉（远了赶不上）
                        }
                        if (d2 < bestD2) {
                            bestD2 = d2;
                            bestI = i;
                        }
                    }
                    if (bestI < 0) {
                        break;
                    }
                    int o = bestI << 3;
                    this.repairUntil[bestI] = game.tickCount + 600;
                    ++repairers;
                    ++this.repairCount;
                    slots[o + 1] = slots[o + 0];
                    slots[o + 2] = (short) repTile;
                    slots[o + 7] = 0;
                    slots[o + 3] = (short) (slots[o + 3] & 0xFF);
                }
            }
        }

        // ===== 侦察（诚实模式，敌 TC 未知时）：螺旋/射线（scout0）路点开图 =====
        // 命令放在本 tick 最后写——同 tick 内的驻防群令（STANCE 240t 一次）会被这里
        // 覆盖回去。卡死 300t 未动 → 跳过该路点（凹形障碍死角兜底，与村民 STUCK 同
        // 思路）。scout0 走射线（有首波来向时）+螺旋，scout1 只走螺旋（错开半圈）。
        for (int s = 0; s < 2; ++s) {
            int si = this.scoutIds[s];
            if (si < 0 || this.healUntil[si] > game.tickCount || this.huntingM[si]) {
                continue;
            }
            int o = si << 3;
            int pos = slots[o + 0] & 0xFFFF;
            int wp = s == 0 ? scoutTarget(myTc)
                : spiralWaypoint(myTc, this.scoutCur[1] % SPIRAL_MAX, SCOUT_RINGS);
            int dx = (pos >>> 8) - (wp >>> 8), dy = (pos & 0xFF) - (wp & 0xFF);
            if (dx * dx + dy * dy <= 2
                    || (pos == this.scoutLastP[s] && game.tickCount - this.scoutLastT[s] > 300)) {
                if (s == 0) {
                    scoutAdvance();
                } else {
                    ++this.scoutCur[1];
                }
                wp = s == 0 ? scoutTarget(myTc)
                    : spiralWaypoint(myTc, this.scoutCur[1] % SPIRAL_MAX, SCOUT_RINGS);
            }
            this.scoutLastP[s] = pos;
            this.scoutLastT[s] = game.tickCount;
            if ((slots[o + 2] & 0xFFFF) != wp) {
                slots[o + 1] = slots[o + 0];
                slots[o + 2] = (short) wp;
                slots[o + 7] = 0;
                slots[o + 3] = (short) (slots[o + 3] & 0xFF);
            }
        }

        // ===== 摘要日志 =====
        if (game.tickCount - this.lastLog >= LOG_EVERY) {
            this.lastLog = game.tickCount;
            System.out.println("[ai] t=" + game.tickCount + " res=" + hdr[5] + "/" + hdr[6] + "/" + hdr[7]
                + " pop=" + hdr[2] + "+" + hdr[49] + "/" + hdr[3]
                + " vills=" + vills + "(w" + woodW + " g" + goldW + " s" + stoneW + ")"
                + " mil=" + milCount + "(val " + milVal + ")"
                + (this.fogHonest
                    ? " enemyVis=" + enemyMilCount + "(val~" + enemyMilVal + ", peak "
                        + this.enemyMilPeak + ") eb=" + this.evisB
                        + " etc=" + (this.enemyTcMem >= 0
                            ? (this.enemyTcMem >>> 8) + "," + (this.enemyTcMem & 0xFF) : "?")
                    : " enemy=" + ehdr[2] + "u " + ehdr[4] + "b mil=" + enemyMilCount
                        + "(val " + enemyMilVal + ", peak " + this.enemyMilPeak + ")"
                        + " e55=" + ehdr[55] + "/" + this.waveNeed
                        + " slope=" + this.waveSlopeMilli
                        + " eta=" + (this.waveEta > 0 ? this.waveEta - game.tickCount : -1)
                        + (this.waveInFlight ? " INFLIGHT" : ""))
                + " towers=" + towerN
                + (expert ? " dodges=" + this.dodgeCount + " hunts=" + this.huntCount
                    + " repairs=" + this.repairCount : "")
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
        int corridor = AiKit.corridorLen(myTc, enemyTc);
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
            if (eType < 2 || eType == 4 || eType == 8 || eType == 9 || !this.evis[j]) {
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

    /** 塔位锚点：敌 TC 已知 → 走廊阶梯（近距图钳到走廊 60% 内，避免建筑压进敌
     *  警戒圈——警戒半径² 36 触发 87.5% 反扑——和白送在建塔，seed 1019 相距 ~10 格）；
     *  敌 TC 未知 → 塔环降级，有首波接触来向（enemyDir）则环序整体朝来向旋转
     *  （v58：纯罗盘环会把塔落到背敌侧，seed 1006 村民暴露被屠实锤）。 */
    private static int corridorAnchor(int myTc, int enemyTc, int enemyDir, int dist, int towerIdx) {
        if (enemyTc >= 0) {
            return AiKit.stanceTile(myTc, enemyTc, Math.min(dist, AiKit.corridorLen(myTc, enemyTc) * 3 / 5));
        }
        return ringAnchor(myTc, towerIdx, dist, enemyDir);
    }

    // 未知敌方向的塔环方向表（8 罗盘；findAiBuildSpot 会就近落位）。
    // RING_ROT：有来向时环序相对来向 octant 的偏移——主方向 → 左邻 → 右邻 → 左二 →
    // 右二，塔扇覆盖来向 ±90°。
    private static final int[] RING_DX = {1, 0, -1, 0, 1, -1, 1, -1};
    private static final int[] RING_DY = {0, 1, 0, -1, 1, 1, -1, -1};
    private static final int[] RING_ROT = {0, 7, 1, 2, 6};

    private static int ringAnchor(int myTc, int idx, int dist, int dirPacked) {
        int k;
        if (dirPacked >= 0) {
            k = (octant((dirPacked >>> 8) - (myTc >>> 8), (dirPacked & 0xFF) - (myTc & 0xFF))
                + RING_ROT[idx % RING_ROT.length]) & 7;
        } else {
            k = idx & 7;
        }
        int x = Math.max(1, Math.min(62, (myTc >>> 8) + RING_DX[k] * dist));
        int y = Math.max(1, Math.min(62, (myTc & 0xFF) + RING_DY[k] * dist));
        return x << 8 | y;
    }

    /** 方向 → 8 罗盘 octant（与 RING_DX/DY 对齐：0=E 1=S 2=W 3=N 4=SE 5=NW 6=NE 7=SW）。
     *  免浮点：|dx|>2|dy| 取东西，|dy|>2|dx| 取南北，否则对角。 */
    private static int octant(int dx, int dy) {
        int ax = Math.abs(dx), ay = Math.abs(dy);
        if (ax > 2 * ay) {
            return dx >= 0 ? 0 : 2;
        }
        if (ay > 2 * ax) {
            return dy >= 0 ? 1 : 3;
        }
        if (dx >= 0) {
            return dy >= 0 ? 4 : 6;
        }
        return dy >= 0 ? 7 : 5;
    }

    // ===== 螺旋侦察路点（第六批）=====
    // 以我方 TC 为圆心外扩的正方形环（半径 3,5,…,3+2(rings-1)），环上路点弧距 ≤3 格
    // ——单位视野=自身 3×3（revealFogAroundUnit/dimFogAroundUnit 实读），弧距 3 保证走过即无缝开图。游标全局
    // 递增、取模环绕复用；村民探路限前 6 环（半径 ≤13，不跑丢），军事侦察/猎寻用
    // 全 16 环（半径 ≤33，覆盖全图）。全确定性：固定环序/周长均分，无 RNG。
    // （SCOUT_RINGS/PROBE_RINGS/SPIRAL_MAX/PROBE_MAX 常量在字段区——JLS 8.3.3 前置）
    private static int spiralCount(int rings) {
        int n = 0;
        for (int rr = 0; rr < rings; ++rr) {
            n += (8 * (3 + 2 * rr) + 2) / 3;
        }
        return n;
    }

    /** 第 w 个螺旋路点（打包 tx<<8|ty）：环半径 r=3+2rr，环内 cnt=ceil(8r/3) 个
     *  路点沿正方形周长均分（上边左→右→右边→下边→左边）。 */
    private static int spiralWaypoint(int centerPacked, int w, int maxRings) {
        int cx = centerPacked >>> 8, cy = centerPacked & 0xFF;
        int base = 0, r = 3, cnt = 8;
        for (int rr = 0; rr < maxRings; ++rr) {
            r = 3 + 2 * rr;
            cnt = (8 * r + 2) / 3;
            if (w < base + cnt) {
                break;
            }
            base += cnt;
        }
        int slot = Math.min(w - base, cnt - 1);
        int p = slot * (8 * r) / cnt;            // 正方形周长位置 [0,8r)
        int x, y;
        if (p < 2 * r) { x = cx - r + p; y = cy - r; }
        else if (p < 4 * r) { x = cx + r; y = cy - 3 * r + p; }
        else if (p < 6 * r) { x = cx + 5 * r - p; y = cy + r; }
        else { x = cx - r; y = cy + 7 * r - p; }
        x = Math.max(1, Math.min(62, x));
        y = Math.max(1, Math.min(62, y));
        return x << 8 | y;
    }

    // ===== 射线侦察（第六批）：首波接触来向已知时，沿"我 TC → 来向"延长线反推 =====
    // （RAY_LEN 常量在字段区——JLS 8.3.3 前置）

    /** 射线侦察路点 k：from→to 方向延长线上 dist=6+3k 处，k%4 给横向锯齿，振幅
     *  3+k（远端 ±19 格——来向估计有噪声，远端放宽扫描带不至于擦着敌基走过去）。 */
    private static int rayWaypoint(int fromPacked, int toPacked, int k) {
        int fx = fromPacked >>> 8, fy = fromPacked & 0xFF;
        int dx = (toPacked >>> 8) - fx, dy = (toPacked & 0xFF) - fy;
        int m = Math.max(1, Math.max(Math.abs(dx), Math.abs(dy)));
        int dist = 6 + 3 * k;
        int zig = (k % 4 == 1 ? 1 : (k % 4 == 3 ? -1 : 0)) * (3 + k);
        int x = fx + (dx * dist - dy * zig) / m;
        int y = fy + (dy * dist + dx * zig) / m;
        x = Math.max(1, Math.min(62, x));
        y = Math.max(1, Math.min(62, y));
        return x << 8 | y;
    }

    /** 当前侦察目标路点（scout0 用）：资源侦察态只走螺旋（射线是朝敌 TC 方向找
     * TC 的，资源盲区开图不需要）；否则有首波来向且射线未扫完 → 射线法，兜底螺旋。 */
    private int scoutTarget(int myTc) {
        if (!this.resScoutOn && this.waveOrigin >= 0 && this.rayCursor < RAY_LEN) {
            return rayWaypoint(myTc, this.waveOrigin, this.rayCursor);
        }
        return spiralWaypoint(myTc, this.scoutCur[0] % SPIRAL_MAX, SCOUT_RINGS);
    }

    /** 推进侦察路点（scout0：射线优先，扫完自动落回螺旋；资源侦察态只推螺旋）。 */
    private void scoutAdvance() {
        if (!this.resScoutOn && this.waveOrigin >= 0 && this.rayCursor < RAY_LEN) {
            ++this.rayCursor;
        } else {
            ++this.scoutCur[0];
        }
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
     *  诚实模式限已探索格（无 0x8000 迷雾位——未探索资源人类不可知），且该过滤
     *  不随下面的四级回退放宽（放宽=偷看迷雾）。固定扫描序保确定性；
     *  过滤卡死拉黑格与无可走邻格的死角资源。 */
    private int findResource(c game, int fromPacked, int kind, int tick) {
        // 四级回退：① 避敌 TC 10 格 + 避蹲守（v38）+ 避走廊（v45：只派"比我 TC 离敌
        // 更远"的资源格——敌梯队沿走廊直线进军，中线矿点=排队送村民，v44 败局
        // 村民成片死在 20,20 类中场实锤）② 放掉走廊 ③ 放掉蹲守（=v35 行为）
        // ④ 全不过滤（防死锁）。非 Expert 无 camped 标记且 resRelocateOn=false，
        // ①≡②≡③≡旧行为。
        int r = this.findResource(game, fromPacked, kind, tick, true, true, true);
        if (r < 0) {
            r = this.findResource(game, fromPacked, kind, tick, true, true, false);
        }
        if (r < 0) {
            r = this.findResource(game, fromPacked, kind, tick, true, false, false);
        }
        if (r < 0) {
            r = this.findResource(game, fromPacked, kind, tick, false, false, false);
        }
        return r;
    }

    private int findResource(c game, int fromPacked, int kind, int tick, boolean avoidEnemy,
                             boolean avoidCamped, boolean behindLines) {
        int fx = fromPacked >>> 8, fy = fromPacked & 0xFF;
        int ex = this.resEnemyTc >>> 8, ey = this.resEnemyTc & 0xFF;
        int corridor = this.resEnemyTc >= 0 && this.resMyTc >= 0
            ? AiKit.corridorLen(this.resMyTc, this.resEnemyTc) - 3 : -1;
        int best = -1, bestD2 = Integer.MAX_VALUE;
        for (int ty = 0; ty < 64; ++ty) {
            for (int tx = 0; tx < 64; ++tx) {
                int idx = tx + (ty << 6);
                int raw = game.mapTiles[idx];
                if (this.fogHonest && !FOG_RES_OMNI && raw < 0) {
                    continue;                        // 未探索格的资源不可知
                }
                int t = raw & 0xFFF;
                if ((t & 0x300) != 0x300 || (t & 3) != kind) {
                    continue;
                }
                if (this.resBlacklistUntil[idx] > tick || !hasWalkableNeighbor(game, tx, ty)
                        || (avoidCamped && this.resCampedUntil[idx] > tick)) {
                    continue;
                }
                if (avoidEnemy && this.resEnemyTc >= 0
                        && Math.max(Math.abs(tx - ex), Math.abs(ty - ey)) < RES_ENEMY_SAFE) {
                    continue;
                }
                if (behindLines && this.resRelocateOn && corridor >= 0
                        && Math.max(Math.abs(tx - ex), Math.abs(ty - ey)) < corridor) {
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

    /** v40：矿场1 周边 10 格内是否有被蹲守的金格（第二采矿场触发条件）。 */
    private boolean mineAreaCamped(c game, int campTile, int tick) {
        int cx = campTile >>> 8, cy = campTile & 0xFF;
        for (int dy = -10; dy <= 10; ++dy) {
            int ty = cy + dy;
            if (ty < 0 || ty >= 64) {
                continue;
            }
            for (int dx = -10; dx <= 10; ++dx) {
                int tx = cx + dx;
                if (tx < 0 || tx >= 64) {
                    continue;
                }
                int idx = tx + (ty << 6);
                int t = game.mapTiles[idx] & 0xFFF;
                if (this.resCampedUntil[idx] > tick && (t & 0x300) == 0x300 && (t & 3) == 2) {
                    return true;
                }
            }
        }
        return false;
    }

    /** v40：备用金格 = 未被蹲守、距矿场1 ≥8 格、避敌 TC 危险区（诚实模式另限
     *  已探索格），距 TC 最近；找不到返回 -1（建筑块的 anchor>=0 守卫兜住，下决策再试）。 */
    private int findSecondGold(c game, int myTc, int campTile, int tick) {
        int fx = myTc >>> 8, fy = myTc & 0xFF;
        int cx = campTile >>> 8, cy = campTile & 0xFF;
        int ex = this.resEnemyTc >>> 8, ey = this.resEnemyTc & 0xFF;
        int best = -1, bestD2 = Integer.MAX_VALUE;
        for (int ty = 0; ty < 64; ++ty) {
            for (int tx = 0; tx < 64; ++tx) {
                int idx = tx + (ty << 6);
                int raw = game.mapTiles[idx];
                if (this.fogHonest && !FOG_RES_OMNI && raw < 0) {
                    continue;                        // 未探索格的资源不可知
                }
                int t = raw & 0xFFF;
                if ((t & 0x300) != 0x300 || (t & 3) != 2) {
                    continue;
                }
                if (this.resBlacklistUntil[idx] > tick || this.resCampedUntil[idx] > tick
                        || !hasWalkableNeighbor(game, tx, ty)) {
                    continue;
                }
                if (Math.max(Math.abs(tx - cx), Math.abs(ty - cy)) < 8) {
                    continue;
                }
                if (this.resEnemyTc >= 0
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

    /** 石贫检测（aoe.expStonePoor）：扫可采石格（kind3 + 可站邻格 + 未拉黑/蹲守 +
     *  敌 TC 已知时不在其 RES_ENEMY_SAFE 危险圈），诚实模式限已探索格——与
     *  findResource 同一信息口径，两侧一致。判定：我 TC Chebyshev SP_NEAR 格内
     *  零可采石 → stonePoor 单向 latch（塔链的补塔/塔 4-5/smith 全靠石收入，
     *  近距无石 = 塔链长期不可行）。诚实模式加覆盖率门：SP_NEAR 盒内已探索格
     *  <40% 时本决策不判（开局迷雾不足，判了全是假阳性；覆盖不够 = 维持原状）。
     *  report 模式每 500t 打一行遥测（首扫 + 变化 + 周期），零行为差。 */
    private void stoneScan(c game, int myTc, int enemyTc) {
        int mx = myTc >>> 8, my = myTc & 0xFF;
        int ex = enemyTc >= 0 ? enemyTc >>> 8 : 0, ey = enemyTc >= 0 ? enemyTc & 0xFF : 0;
        int nearest = Integer.MAX_VALUE, nearN = 0, total = 0, boxN = 0, explored = 0;
        for (int ty = 0; ty < 64; ++ty) {
            for (int tx = 0; tx < 64; ++tx) {
                int idx = tx + (ty << 6);
                int raw = game.mapTiles[idx];
                int d = Math.max(Math.abs(tx - mx), Math.abs(ty - my));
                if (this.fogHonest && !FOG_RES_OMNI && d <= SP_NEAR) {
                    ++boxN;
                    if (raw >= 0) {
                        ++explored;
                    }
                }
                if (this.fogHonest && !FOG_RES_OMNI && raw < 0) {
                    continue;                        // 未探索格的石不可知
                }
                int t = raw & 0xFFF;
                if ((t & 0x300) != 0x300 || (t & 3) != 3) {
                    continue;
                }
                if (!hasWalkableNeighbor(game, tx, ty)
                        || this.resBlacklistUntil[idx] > game.tickCount
                        || this.resCampedUntil[idx] > game.tickCount) {
                    continue;
                }
                if (enemyTc >= 0 && Math.max(Math.abs(tx - ex), Math.abs(ty - ey)) < RES_ENEMY_SAFE) {
                    continue;
                }
                ++total;
                if (d < nearest) {
                    nearest = d;
                }
                if (d <= SP_NEAR) {
                    ++nearN;
                }
            }
        }
        boolean canJudge = !this.fogHonest || FOG_RES_OMNI || explored * 5 >= boxN * 2;
        boolean poorNow = canJudge && nearN == 0;
        boolean flipped = poorNow && !this.stonePoor;
        if (flipped) {
            this.stonePoor = true;
            this.stonePoorTick = game.tickCount;
        }
        if (SP_REPORT && (flipped || game.tickCount - this.stoneLastScan >= 500
                || this.stoneLastScan < 0)) {
            this.stoneLastScan = game.tickCount;
            System.out.println("[ai] STONEPOOR scan near=" + nearN + "/" + SP_NEAR
                + " nearest=" + (nearest == Integer.MAX_VALUE ? -1 : nearest)
                + " total=" + total + " poor=" + this.stonePoor
                + (canJudge ? "" : " (coverage " + explored + "/" + boxN + ", deferred)")
                + " t=" + game.tickCount);
        } else if (flipped) {
            System.out.println("[ai] STONEPOOR on (no stone within " + SP_NEAR
                + ") t=" + game.tickCount);
        }
    }

    /** 建筑在训队列长度（rec[+2] 位 16..23；研究中的 0x10000 也算 1）。 */
    private static int queueLen(int[] recs, int slot) {
        return (recs[slot + 2] >> 16) & 0xFF;
    }

    /** aiK towerCap > 表长时塔距复用表末值（防 AIOOBE）。 */
    private static int tdistAt(int[] tdist, int i) {
        return tdist[Math.min(i, tdist.length - 1)];
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
