# 游戏机制知识地图

反编译代码（`AgeOfEmpires/c.java`）的领域知识地图。定位：

- 这里只放**跨文件的系统性知识**和索引，帮助你在读代码前建立全局图景；
  单点机制细节写在**代码现场注释**里，两处不重复。以代码注释为最新、本文为导览。
- 标注【已验证】的条目都经动态运行确认；标【推断】的仅由静态阅读得出，改动前先验证。
- 2026-08-31 起已混淆名已批量改为可读名（完全搞懂的符号），本文同步更新；
  新旧对照表见 `docs/symbols.md`，改名的工具与流程见 `DEVELOPMENT.md`。

## 主循环与时序

- 【已验证】80ms 一帧（12.5fps）：`void_b()` 里
  `var_com_ulysseo_mad_b_a.a(Integer.getInteger("aoe.tickms", 80), 1)`（`c.java:295`）启动
  框架定时器（`mad/b.java`，`java.util.Timer` 固定周期）。`aoe.tickms` 是 dev 旋钮，
  但**改它 = 整个游戏等比变速**（逻辑/动画/音乐全按比例加快），不是"更顺滑"，见下。
- 【已验证】每帧流程：`mad/e.java` 的 `run()` → `c.w()`（仅检查退出标志）→
  `repaint()+serviceRepaints()` → `mad/b.java` 的 `paint()` → `c.onPaint(Graphics)`。
  **`onPaint` 是真正的主循环体（paint-driven）**：`++tickCount` 后先推进游戏逻辑再渲染；
  世界模拟集中在 onPaint 的 default 分支（`c.java:1397-1426`：建造/移动/战斗/投射物/AI/任务脚本）。
  `tickCount` = 帧计数（`-Daoe.debug=1` 日志里的 `ar=` 标签就是它——日志字面量未随改名更新，每帧 +1）。
- 【已验证】**帧率与逻辑深度耦合**——"提帧率"不是改一个数字的事（2026-09-01 为
  "要不要上 30/60fps"做的考证）：
  - 帧 = 逻辑 tick = 渲染，三者同频。所有游戏常数以 tick 计：移速累加器
    （`tickMoveTimer` 每 tick 减 256~1536）、远程攻击冷却 15 tick（其余 8）、AI 三周期
    （`aiBuildInterval` 等）、采集节奏、任务脚本帧计数、对话按键门（`aQ > 10`）、
    底部跑马灯滚动（`R++`）。
  - 渲染路径里嵌着逻辑：任务主视图 `a(Graphics)` 在画世界前跑 `mouseTick()` /
    `tickCursor()` / `updateCamera()`（`c.java:2932-2942`）——镜头与光标按"帧"推进，
    单独提高渲染帧率，镜头/光标速度会跟着翻倍。
  - 帧率硬编码进内容时序：背景音乐换曲 `c()`（`c.java:329`）按 `曲长毫秒 / 80`
    折成帧数倒计时——改 tickms 音乐换曲会按比例 skew。
  - 按键每帧才被逻辑采样一次：80ms 输入延迟是 12.5Hz 逻辑的固有属性。
  - 改造路线论证（变速 / 解耦渲染 / 解耦+插值 / 逻辑提速+常数对半）与结论见
    `docs/research/deep-dive-1-frame-decoupling.md`（用户拍板：记录，先不做）。
- 【已验证】**确定性模型（2026-09-01 起）**：模拟 = 纯"任务 + 输入序列 + tick"决定。
  全局 `nextRandomInt`（LCG，种子来自任务资源字节）**只许模拟消费**（现存唯一消费点
  `tickConstruction` 的 +1 掷骰；地图生成器 `AgeOfEmpires.d` 不用全局 RNG）；BGM 选曲
  走独立化妆品流 `nextBgmRandomInt`（此前选曲混用全局流，回放轨迹随"听了几首曲子"
  发散）。快照 v2 把 tickCount 一并钉住（模拟含 `tickCount&8` 回血、投射物旋转起点等
  tick 奇偶逻辑）。工具链：`[input] ar=<tick>` 输入 trace、FIFO `replaytrace`、
  双跑对拍自检 `tools/replaycheck.sh`——落地记录见 WORKLOG.md「确定性回放落地」。
- 【已验证】**逻辑分辨率是自适配的**（2026-09-01 宽视野落地时的考证）：
  240x320 只是设计基准，本体没有任何定宽假设——`setupScreenMetrics(int,int)` 从
  screenW/H 现算可视格数 `viewTileCols = (screenW>>6)+3`、`viewTileRows = (screenH>>4)+5`、
  镜头居中偏移 `ad`/`J`、底栏边界 `bottomBarY`；`renderWorld` 按 viewTileCols 遍历；菜单
  中心锚定（`screenW>>1` + menuTree 数据偏移）；全图视图的缩略图定位/视野框/镜头
  全部屏幕相对；顶栏对 240 宽素材做居中（`screenW-240>>1`）。桌面层尺寸单点在
  `lcdui/Screen`（`-Daoe.width/height` 可覆盖，run.sh 默认传 720 宽）。
  **注意**：回归测试与 golden 基线必须走默认 240x320（state JSON 里的镜头坐标
  `cam` 依赖 screenW 派生偏移，改宽度基线全变）；菜单/剧情背景图是 240 宽素材，
  宽屏下右侧留黑边（已知，暂不适配）。世界/全图画面中的黑色区域 = 未探索迷雾
  （或图外虚空），不是渲染缺陷。

## 按键输入模型

- 【已验证】路径：AWT 键事件 → `Canvas.mapKeyCode`（桌面键→J2ME 键码，对照表见
  README"键位"一节）→ `mad/b` → `c.onKeyPress(按下)` / `c.onKeyRelease(松开)`。
- 【已验证】键位表是 `data.res` 资源 **#129**（键码→动作码映射，`loadKeymap(129)` 加载）。
- 【已验证】核心状态：`keyActionHeld != 0` 即"有键按住"（`tickCursor()` 等处开头 `if (keyActionHeld == 0) return`）。
  游戏自身从不清 `keyActionHeld`，全靠松开事件清除。`L`/`s` 实现先延迟后重复的自动重复
  （同真机手感）。
- 【已验证】桌面时序保证：快速点按的按下+松开会落进同一个 80ms 帧间隙，所以
  `Canvas` 把松开事件**延迟到本次 paint 之后**才投递（`pendingKeyReleases`）——
  按下保证被至少一帧完整消费（不吞点按），松开照原版全清（不粘滞）。
  真机上物理键行程 + 平台事件延迟天然保证按下跨帧，无需此机制。
- 【已验证】桌面鼠标增强（`c.java` "桌面鼠标增强"段，事件链
  Canvas→mad.b→mad.a→c）：仅在任务主视图（`screenState` ∈ {1, 6}）生效。
  - 屏幕→格子的映射复用 `renderWorld` 的世界渲染遍历做**像素拾取**
    （`mousePick*`，EDT 置请求、渲染线程拾取、下一帧 `mouseTick` 应用——
    注意 pending 的清理在 j() 侧完成，mouseTick 只做序号去重后应用）。
  - 主视图（screenState=6）= `a(Graphics)`：`tickCursor()` 光标输入 + `updateCamera()` 镜头缓动 + `renderWorld` 世界
    渲染；光标格恒被镜头居中，教程对话框弹出期间 screenState=2（鼠标被守卫拒绝）。
  - 选中标志：`playerUnitSlots[玩家][i*8+4] & 0x8000`；单位表 8 short/单位，
    `[i*8+0]` = 当前格（tx<<8|ty），`[i*8+3]&0xFF` = 类型；数量
    `playerUnitHeaders[玩家][2]`。群移指令 = `orderMove(0, tx, ty)`；按类型多选 = `selectUnits(0, 类型)`；
    全清 = `clearSelection()`。键位表（data.res #129）：-5→22（FIRE/确认），±方向→19/21/23/25
    （平移位掩码），-7→47（右软键/菜单）。

## 状态机速查（未完全考证）

- `screenState` = 顶层画面状态，`onPaint` 内按它分发渲染/更新；`beginStateTransition()` 在 `screenState` 变化时调
  对应构建器（`t()`/`d()`/`A()`/`r()`/`n()`/`C()`/`u()`…）。
  `pendingScreenState` = 配套"当前"值（`requestStateSwitch(n)` 设置），`pendingScreenState == screenState` 表示画面稳定。
- 【已验证】`pendingScreenState = 4` 出现在所有列表菜单屏（主菜单 / Game Mode / 选关 / 设置），
  但任务进行中 debug 也显示 `pendingScreenState = 4`——`pendingScreenState` 并非画面的唯一标识，**改动状态机
  前先对照 `p()` 的分发逻辑**。
- `menuScreenId`（旧名 H）= 子画面选择器，`activateMenuScreen` 按它打补丁：
  【已验证】10 = 随机地图难度选择（Easy/Medium/Expert），11 = 教学 Mission 选关，
  12 = 战役选关，1 = Game Mode 屏。

## 菜单 / 对话框模板系统

- 【已验证】**全部菜单屏来自同一棵静态树** `menuTree = byte_arr_a(117)`（res#117，
  1029B，所有 menuScreenId 的子树顺序拼接）；每屏的文本表 = res **(83 + menuScreenId)**
  （如 83=主菜单 90=设置 93=难度 94/95=教学/战役选关）。`loadMenuScreen()` 按屏根
  构建；屏根序号 = 子树偏移（主菜单 0、Game Mode 116、难度 739、教学选关 810、
  战役选关 881——debug 日志 `[fMenu] aR=` 就是它，可用作导航断言）。
- 【已验证】节点布局：`+8` = 控件类型（2=循环器 0=文本 4=定时脚本 5=FIRE 捕手…），
  参数在 +9 起；循环器 `+9+1` = 选项总数，`+9+2` = 当前选中项。脚本区偏移由
  `int_k(n)`（按类型跳过参数）给出；循环器脚本 = `[3][动作码][右转脚本][左转脚本]`，
  选项脚本由 `int_i` 逐个跳过。
- 【已验证】FIRE 激活 = `runMenuScript(node, 1)`，脚本操作码 switch（~2150 行）：
  - 操作码 3 = 模式启动：第二个字节是动作码——**65 = Game Mode 自改导航**（按
    循环器值改写自身导航目标 11/12/10）、**66 = 随机地图+难度**（gameMode=0，
    `var_byte_a` = 档位）、**71 = 教学 Mission**（gameMode=16）、**73 = 战役**
    （gameMode=32）、67/72 = 设置开关、74 = 空操作。
  - 操作码 2 = 导航到指定屏；5 = 改写模板字节；8 = 高分屏。
- 【已验证】选关上限只有一处生效点：`activateMenuScreen` 的 menuScreenId-case 11/12
  模板补丁（原版按 `tutorialProgress+1`/`campaignProgress+1` 门控，移植已改为固定 3/7 全解锁）。启动任务时无二次校验。
- 【已验证】**对话框（screenState=2）的背后画面**由 `overlayPrevState` 决定
  （`n()` 打开时记录、关闭时 `pendingScreenState = overlayPrevState`；渲染分发
  1→renderMapView / 4→renderMenu / 其余→renderWorld）。任务脚本的 update 链在
  小地图（1）下也跑，所以**对话框可能从小地图里弹出**；原版会把关闭动作弹回
  小地图（连锁对话框=反复弹回蓝屏）。移植版 `n()` 改为记录 1→8（回世界视图，
  2026-09-01）。debug 日志 `[view] dialog open` 打印每次记录值。

## AI（敌方玩家，player 1）

- 【已验证】AI 开关 = `aiEnabled`（旧名 var_boolean_i，字段默认 true）。教学 Mission/
  随机地图为 true；战役除 m4 外 false（静态敌人）。**它只在任务环境装配
  `setupMissionEnv(int)`（旧名 o(int)，~2945 行）里按模式赋值且从不复位**——同一次
  进程里先玩战役再玩随机图，会继承上一个任务的 AI 开关与全部调参（原版引擎的怪癖；
  冷启动无此问题）。
- 【已验证】AI 大脑 = `tickAi()`（旧名 z()；每帧，更新链 `g→p→B→aimProjectiles→
  tickProjectiles→tickAi→j→tickMissionScript` 的一环）：
  - **威胁扫描**：每帧比对一个玩家 0 单位与 AI 城镇中心 `hdr[1][8]` 的距离
    （`hdr[1][54]` 高字节 = 扫描指针，低 3 字节 = 当前最近距离²，最近者存 `hdr[1][53]`）。
  - **模式判定**（扫描完成时）：`var_int_i = 2`（进攻）当 AI 军队价值 `hdr[1][55]` ≥
    `aiAttackThreshold` 且 ≥ 玩家军队价值 ×1.25；`= 1`（防御/驱赶）当最近入侵者距离²
    ≤ `aiGuardRadiusSq`；否则 0（种田）。军队价值 = 单位成本之和（训练时累加）。
  - **军队指令**：进攻（模式 2）= 75% 兵力 attack-move 到玩家城镇中心（带 ±1 格抖动）；
    防御（模式 1）= 87.5% 扑玩家城镇中心、12.5% 拦截最近入侵者。村民（类型 <2）空闲时
    自动画采集目标（`findNearbyResource` 找资源格）。
- 【已验证】AI 经济（同样硬编码，无脚本）：
  - **建造**：写死的建造顺序表 `aiBuildOrder`（旧名 var_byte_arr_j，c.java:208，4 阶段：
    房×2/兵营×1/哨塔×2 → 房×3/哨塔×5/铁匠/射箭场/磨坊/兵营 → 房×5/哨塔×5/马厩/射箭场/
    攻城工坊 → 房×5/马厩/哨塔×5/射箭场/城堡/兵营×2）。每 `aiBuildInterval` tick 跑一步：
    `void_a()` 从表里找第一个未达标建筑设为当前目标 `aiBuildTarget`，`tryPlaceAiBuilding()`
    若买得起（`canAfford`）就在 AI 基地旁 `findAiBuildSpot` 螺旋找空位放下（类型计数含在建）。
  - **训练**：每 `aiTrainInterval` tick 尝试一次 `tryTrainAiUnit`，兵种 = `tickCount % 10`
    轮换（村民/长枪/剑士/弓兵/侦察/骑兵/冲车/投石车/征服者，受人口 26 与每类上限约束，
    入队走 `queueUnitTraining`）。
  - **采集加成**：村民交存资源时 AI 产量 × `aiGatherMultiplier/256`（Easy 2×、
    Medium ~3.07×、Expert 8×）——原版 AI 的资源作弊。
- 【已验证】三档难度参数（`setupMissionEnv`，gameMode=0 分支；玩家起始资源恒 200/100/100，
  AI 起始 `hdr[1][5..7]` = 50/15/15（Easy）、50/50/50（Medium）、20/20/20（Expert））：

  | 参数 | Easy | Medium | Expert | 含义 |
  |---|---|---|---|---|
  | `aiBuildInterval` | 250 | 150 | 100 | 建造步进周期（tick） |
  | `aiAttackThreshold` | 50 | 60 | 100 | 进攻所需军队价值 |
  | `aiTrainInterval` | 20 | 20 | 1 | 训练尝试周期（tick） |
  | `aiGuardRadiusSq` | 49 | 36 | 25 | 基地警戒半径²（7²/6²/5² 格） |
  | `aiFreeResInterval` | MAX | 2500 | 1000 | "定时白送资源"周期——**死代码**：白送量 `hdr[57]` 恒 0 |
  | `aiGatherMultiplier` | 512 | 786 | 2048 | 采集产量倍率（256=1×） |

- 【已验证】2026-09-01 headless 实测（Easy 随机图，`-Daoe.dev=random:1`）：
  fields dump 命中上表全部参数，37 秒内 `aiBuildTarget` 11→12（房→哨塔目标推进）、
  AI 头数组 CRC 变化、`aiTrainTimer` 训练计时循环——AI 确实按上述逻辑运转。

## 任务事件脚本 DSL

- 【已验证】每个任务可带一段**字节码触发器脚本**，装载于 `setupMissionEnv`
  （`var_byte_arr_a` = res 124/125/126 教学、110..116 战役、128 随机图）。解释器 =
  `tickMissionScript()`（旧名 F()；每帧，更新链末位）：顺序扫块，块 =
  `[条件][动作…][126 结束]`，程序尾 = 127；**块头字节为负 = 已触发/禁用，跳过**
  （`skipScriptBlock` 整块跳过）。动作 1 = 解锁某块（负→正），动作 2 = 打已触发标记
  （正→负，常用 `-1` = 标记自己实现 fire-once）。4 个帧计数器 `var_int_arr_a`
  （动作 3 清零）供条件 2 做"经过 N 秒"判定。
- 条件操作码（`evalScriptCondition`）：1=单位在某矩形 2=计数器×10>阈值 3=画面状态
  4=当前选中==特定单位 5=某槽位单位类型（-1=已死）6=科技旗标==值
  7=玩家头字段 ==/</> 值。
- 动作操作码（`runScriptActions`）：0=弹简报页（文本表 = `I`，按模式 71..80）1=解锁块
  2=触发标记 3=清计数器 4=**胜利** 5=**失败** 6=生成单位 7=命令单位/全村民/某类型
  移动 8=写玩家头字段 9=写科技旗标。
- 【已验证】任务数据（`spawnMission`，res 118..120 教学、103..109 战役）是另一段
  数据流：头 13 字节（RNG 种子对、双方起始资源、光标位、记录数），其后
  记录：op1 = 放建筑（玩家,类型,x,y,血量）op2 = 生成单位 op3 = 写资源/地形格。
  随机图（gameMode 0）无此数据——地图全由生成器 + `t()` 摆双方城镇中心和村民。

## 移动与寻路

- 【已验证】**没有全局寻路**（无 A*/BFS）。单位移动 = `boolean_b(玩家, 单位下标)`
  （`c.java` ~5559 行）的单步贪心：
  1. 目标 `[i*8+2]` 与当前 `[i*8+0]`（都是 tx<<8|ty 打包）相等 → 不动。
  2. 朝目标做 **DDA/Bresenham 直线步进**：主轴每调用走 1 格，副轴按误差累加器
    （`[i*8+3]` 高 8 位，跨步携带，用于渲染级插值）过半才走——即贴直线走。
  3. 步进的落点做三重检查：地图边界（>63 回退）、**占位检查**
    （`mapTiles[tx+(ty<<6)] & 0xFFF` = 该格占据的单位 id，非 0 即堵；
    768 是虚空地形）、不能退回自己上一步的格子。
  4. 被堵时做**局部避障**：按单位朝向 `[i*8+6]` 从方向扇形表 `var_byte_arr_g`
    （data.res 资源 **#123**，每方向 7 个备选偏移）依次试 7 个邻格，第一个空格即走；
    7 个全堵 → 原地等待（清子格累积，返回 false）。
  5. 成功则更新占位表（旧格清 id、新格写 id），`[i*8+1]` = 旧格（渲染插值用），
    `[i*8+0]` = 新格。
- 推论：单位走直线、遇障局部绕行，凹形障碍会卡住（原地等）——典型 J2ME 时代做法，
  64x64 地图 + 少量单位下够用。改进寻路（BFS 流场等）属于逻辑现代化候选。
- 每 tick 由主更新循环对每个单位调用（~5053 行，按单位速度计数器节流）。

## 投射物（弓兵/投石机/箭塔，含一次卡死修复）

- **记录池**：`projectileTable[2][20]`（wave5 前原名 var_short_arr_arr_b），每玩家
  5 条记录 × 4 short，保持紧凑：活跃记录占据 `0 .. headers[48]-1`（`[48]` = 活跃
  投射物数）。发射（攻击 case 12）追加在 `[48]<<2` 槽位并 `[48]++`；移除走
  "尾部交换进空位 + `[48]--`"。
- **记录字段**：`[+0]` 射手单位 id；`[+1]` 状态字——`1000`=待瞄准（发射/落地后的
  静止态），否则 = 已瞄准（值为目标敌单位的槽位下标）；`[+2]` 目标格 tx<<8|ty；
  `[+3]` 飞行计时（0-15，16 tick 命中判定）。
- 每 tick 顺序（`c.java` ~1411）：`aimProjectiles()` → `tickProjectiles()`。
  aim 从 `(tickCount % [48])<<2` 旋转起点扫**前 `[48]` 条**记录找"待瞄准"（+1==1000）
  分配目标；tick 让已瞄准的飞行 16 tick 后按目标格结算伤害并回到 1000。
- **卡死修复（2026-09-01，战役第一场接战即触发）**：CFR 反编译体把"扫满一圈放弃"的
  退出分支弄丢了——`if (--n3 > 0) continue;` 只在 >0 时 continue，≤0 时落回 while
  再判，而 while 条件不含 n3。窗口内全是"已瞄准"记录（任何投射物飞行期间必然如此）
  时整个循环永久自旋。由于模拟+渲染都跑在 mad/b 80ms Timer 线程的 `serviceRepaints`
  里，表现为画面冻结、EDT 键鼠照常收、无任何异常栈。修复：循环体内补回
  `if (--n3 <= 0) break;`（`aoe.debug` 下留 `[proj] aim scan exhausted` 观测线）。
- **字节码考证（原 jar `~/Downloads/age_of_empires_ii_240x320-9174.jar`，
  `javap -c AgeOfEmpires/c` 方法 G）**：原版**没有**这个 bug——字节码 `82: iinc 7,-1;
  85: ifgt 48` 的 false 分支直接落到循环出口（与"找到 1000"同一入口 90），即原版
  语义就是"n3 耗尽即退出、随后 `if (n3<=0) continue` 跳过"。是 CFR 把该出口错误
  渲染成 while 体末尾的裸 continue（Java 语义 = 回头再判条件）而静默丢失。
  修复即逐字恢复原版语义。**审计**：按"while 体末尾语句是 continue"扫描 c.java 全部
  11 处，逐一判定——3 处是我们手写的 dev 代码，其余 8 处循环变量均自进展
  （for 有界 / n5、n3、n4、i 严格推进 / 脚本解释器 n2 由自身赋值推进且有越界即抛
  AIOOBE 兜底），唯一真伪影就是本次咬人的这一处。审计方法（javap 对照原 jar）值得
  对任何"反编译输出可疑"的循环复用。

## 存档（.nfo）

- 【已验证】`nfoData`（314 字节），`loadNfo()` 读写 RecordStore `.nfo`
  （桌面 shim 落盘为 `~/.aoe-desktop/` 下的文件）。布局：
  - `[0..27)`：7 个 4 字节大端整数 = 战役每关高分（`nfoHighScores`，按 `missionIndex` 索引）
  - `[28]`：高半字节 `tutorialProgress`（教学关进度 0..2），低半字节 `campaignProgress`（战役进度 0..6）
  - `[29]`：音效开关（0 = 开，对应 `var_boolean_d`）
  - `[30]`：另一开关（`AgeOfEmpires.b.c`，含义未考证）
  - `[31..32]`：随机图 RNG 种子（高字节 `rngStateHi`，低字节 `rngStateLo`；默认
    8224 = 32,32。随机图胜利时写回 → 下局默认复用同图）
- 【已验证】读写助手：`nfoReadInt(offset, len)` 读大端、`nfoWriteInt(offset, len, value)` 写大端。

## data.res 资源索引

134 个资源，取址方式 = 头部 134 个 int32 **绝对偏移**表，每条记录前 4 字节是长度。
字符串表（`AgeOfEmpires/a.java`）= `(u16 长度, UTF-8)` 序列。已考证：

- **#83+H** 各菜单屏文本表（83=主菜单 90=设置 93=难度 94=教学选关 95=战役选关）
- **#62..64** 升时代公告 / 建筑名+描述对 / 科技名+描述对（条目 id 即内部科技 id）
  **#65** 片头/制作名单、**#66** 单位名(10 兵种)、**#67** 单位训练菜单名/描述对、
  **#68** 科技名、**#69** 建筑名(16)、**#70** 建筑说明；**#81** 科技说明、
  **#82** 操作说明/关于/成绩对话框、**#96/97** 成绩屏、**#98** 胜负文案
- **#71..77** 战役简报（每关 1..2 页） **#78..80** 教学 Mission 简报（每关 7..14 页）
- **#99** 对话框按钮与装载屏文案（"Generate Map"/"Next"…）
- **#100..102** UI 精灵/调色板表（`e()` 启动加载）
- **#103..109** 战役任务数据（spawnMission 格式） **#110..116** 战役触发脚本
- **#117** 全部菜单模板树 **#118..120** 教学任务数据 **#121** 玩家头模板（91B×2 共用）
- **#122** 成本表（150B=50 行×3 字节：单位 0..9 / 科技 10..25 / 建筑 26..41 /
  升时代 47..49，列序 木/金/石——资源只有这三种无食物；兵种/建筑属性全表见
  **unit-stats.md**） **#123** 移动避障方向扇形表 **#124..126** 教学触发脚本
- **#127** 科技旗标初值 **#128** 随机图脚本（= 127 单字节，即无脚本）
- **#129** 键位表（键码→动作码：-1..-4→19/25/21/23 方向，-5→22 FIRE，-6→38，
  -7→47） **#131** 对话框背景模板

## 调试速查

- `-Daoe.debug=1`：每 25 帧打印 `tickCount/pendingScreenState/screenState/aH/l/j/fullRedraw`（`mad/e.java`）。
- `-Daoe.dumpFrames=/path.png`：每 ~5 秒导出当前帧缓冲（`Canvas.java`）。
- 自动化验证（FIFO 注入 + 帧缓冲 dump + 黄金回归）：见 `DEVELOPMENT.md`。
