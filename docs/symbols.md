# 符号字典（新旧对照）

2026-08-31 起混淆名分批改为可读名。改名通过 `tools/renamer`（javac Tree API
符号解析）完成——只有解析到目标元素的标识符被改写，局部变量遮蔽不受影响；
改名映射的机器可读版是 `tools/renamer/wave1.tsv`（同目录后续 wave 文件）。

分级约定：

- **已改名**：语义完全搞懂（代码证据 + 动态验证），已通过 AST 改名器改名。
- **半懂（保留旧名）**：有注释但语义没实测闭环——先补注释，不改名。
- **未考证（保留旧名）**：只有名字，改动前必须先考古。

## 已改名：字段（AgeOfEmpires.c）

| 新名 | 旧名 | 语义 |
|---|---|---|
| screenState | aA | 顶层画面状态，onPaint 按它分发（6=任务主视图 2=对话框 1=全图 4=菜单 11=装载） |
| pendingScreenState | am | requestStateSwitch 设置的目标画面；== screenState 表示稳定 |
| overlayPrevState | aN | 对话框/浮层挂起前的画面（恢复渲染用） |
| tickCount | ar | 帧计数，每帧 +1（80ms 一帧） |
| mapThumbStampRow | af | 全图缩略图的逐行盖章进度，onShown 复位 |
| screenW / screenH | aO / var_int_j | 逻辑屏幕尺寸 240×320 |
| clipLeft / clipRight / clipTop / clipBottom | F / B / O / P | 渲染裁剪矩形 |
| gameMode | ac | **0=随机地图(遭遇战) 16=教学 Mission 1..3 32=战役 1..7**（曾长期搞反，2026-09-01 以简报文本+运行时 dump 实证纠正；菜单动作 65/66/71/73 设置） |
| missionIndex | aC | 当前任务序号（教学/随机难度 0..2，战役 0..6） |
| missionResId | aF | 任务资源 id（data.res 资源号） |
| keyActionPulse | ax | 本帧按键动作（volatile，帧末清零） |
| keyActionHeld | ab | 按住中的动作（松开事件清零） |
| keyActionEvent | ae | 键位表映射出的动作（单次事件） |
| keymapCount / keymap | ak / var_byte_arr_k | 键位表（data.res #129）长度与内容 |
| keyRepeatFrames / keyRepeatLast | s / L | 先延迟后重复的自动重复计数 |
| cursorTileIdx | Q | 光标格线性索引 tx+(ty<<6) |
| cursorTileX / cursorTileY | aa / aV | 光标格坐标 |
| cameraPxX / cameraPxY | y / N | 相机像素位置（光标恒被居中，带缓动） |
| mapViewSavedCamX/Y、mapViewSavedCursorX/Y | aU / A / at / n | 进全图视图前保存的相机/光标，退出时恢复 |
| viewTileCols | aI | 世界渲染每行可见格数 |
| cursorScreenPxX / cursorScreenPxY | aP / aS | 光标格的屏幕像素位置（渲染遍历记录） |
| selectionMark | aE | 选中标记：512=有选中，-1=从未选中 |
| selectionPlayer | Y | 选中归属的玩家编号 |
| selectedType / selectedSlot | var_int_h / aJ | 选中单位类型与槽位 |
| selectionMode | var_int_b | 0=无选中 6=有选中/光标就位 |
| menuTree | var_byte_arr_i | 当前菜单/对话框模板树字节 |
| menuNode / menuNodeCount / menuHighlight | aR / ao / Z | 树遍历指针 / 项数 / 高亮项 |
| menuScreenId | H | 子画面选择器（10=随机图难度选择 11=教学选关 12=战役选关 1=Game Mode 屏） |
| pendingPanelSwitch | v | 延迟生效的面板切换 |
| mapTiles | var_short_arr_a | 地图格 short[4096]：地形+迷雾(0x8000)+占位 |
| playerUnitSlots | var_short_arr_arr_a | 每玩家单位槽（8 short/单位：格位/目标/类型/朝向/选中位…） |
| playerUnitHeaders | var_int_arr_arr_a | 每玩家单位头信息（含 [2]=单位数） |
| nfoData | var_byte_arr_f | .nfo 记录镜像（314B 设置/进度） |
| nfoHighScores | var_int_arr_d | 战役每关高分（7×int） |

## 已改名：方法

| 新名 | 旧名 | 语义 |
|---|---|---|
| onKeyPress / onKeyRelease | void_a / void_e | 键按下/松开（mad.a 抽象 + c 覆写） |
| onShown / onHidden | k / q | 显示/隐藏通知（=J2ME showNotify/hideNotify） |
| onPaint | p | 帧入口：先逻辑后渲染（paint-driven 主循环体） |
| tickCursor | o | 任务中光标/相机输入消费 |
| confirmAtCursor | x | 光标处确认（选中/移动/采集分发） |
| updateCamera | f() | 相机缓动 |
| orderMove | d(int,int,int) | 选中单位移动令 |
| selectUnits | h(int,int) | 选单位（-1=全部，<2=村民，else 同类型） |
| clearSelection | H | 清除全部选中 |
| selectUnderCursor | f(int,int,int) | 设置光标下目标的选中标记 |
| dispatchRender | n(Graphics) | 按 screenState 分发渲染 |
| renderWorld | j(Graphics) | 任务主视图世界渲染（含拾取遍历） |
| renderMapView | e(Graphics) | 全图视图（`0` 键） |
| renderDialog | g(Graphics) | 对话框/简报 |
| renderLoadingScreen | h(Graphics) | 装载进度屏 |
| renderMenu | f(Graphics) | 菜单屏 |
| drawTileSprite | a(Graphics,int,int,int,int) | 世界内贴地形/精灵 |
| runMenuScript | k(int,int) | 菜单项激活的内嵌脚本解释器 |
| loadMenuScreen | l | 按 menuScreenId 构建菜单树与字符串表 |
| activateMenuScreen | boolean_d(int) | 菜单屏激活（加载模板+补丁） |
| requestStateSwitch | boolean_g(int) | 设置 pendingScreenState（带守卫） |
| beginStateTransition | E | screenState 变化时执行对应构建器 |
| beginMissionLoad | boolean_c(int) | 任务装载第一阶段（地形生成器启动） |
| spawnMission | void_f(int) | 按任务资源生成单位/建筑/初始探索 |
| startMissionBriefing | g(int,int,int) | 进任务简报转场 |
| loadNfo / saveNfo | m / I | .nfo 记录读/写 |
| nfoReadInt / nfoWriteInt | int_a(int,int) / h(int,int,int) | .nfo 大端读/写助手 |

## 已考证（wave2 改名候选，2026-09-01 AI/DSL 考古产出）

本次查 random 模式 AI 时闭环考证的一批符号，语义均有代码+动态证据
（详见 `docs/game-mechanics.md` "AI"与"任务事件脚本 DSL"两节），
下一波改名直接补 map 行即可：

| 现名 | 语义 |
|---|---|
| `z()` | AI 大脑：威胁扫描 → 攻/防模式判定 → 军队指令 + 村民自动采集 |
| `var_boolean_i` | AI 开关（字段默认 true；战役 m0..3,5,6 关；只在 o(int) 赋值且从不清除） |
| `an` | AI 建造步进周期（tick）：Easy 250 / Medium 150 / Expert 100 |
| `q` | AI 进攻所需军队价值：50/60/100（随机图默认 0 = 常态侵略） |
| `C` | AI 训练尝试周期（tick）：20/20/1 |
| `var_int_l` | AI 基地警戒半径²：49/36/25 |
| `aw` | AI "定时白送资源"周期——**死代码**（白送量 hdr[57] 恒 0） |
| `aM` | AI 采集产量倍率（256=1×）：512/786/2048 |
| `var_byte_arr_j` | AI 硬编码建造顺序表（`类型,数量` 对，-1 分阶段，-2 结束） |
| `M` | AI 当前建造目标（建筑类型；void_a() 选取，boolean_b() 落位） |
| `ai` | AI 建造顺序的阶段游标（var_byte_arr_j 下标） |
| `w` / `E` / `aq` | AI 三个节流计时器（白送资源/建造/训练；aq 随 C 清零） |
| `o(int)` | 任务环境装配/拆除（n=0 拆 1 装）：难度参数、资源装载都在这 |
| `F()` | 任务事件脚本解释器（每帧扫块：条件→动作，块头负=已触发） |
| `int_g(int)` / `int_a(int)` / `int_f(int)` | 脚本"跳过一个条件 / 跳到块尾 / 走 N 个块头" |
| `int_d(int)` / `int_d(int,int)` | 脚本条件求值 / 动作执行（操作码见 game-mechanics） |
| `boolean_b(int)` | AI 训练器：兵种=参数轮换，受人口/上限/价格约束，int_c(1,n) 入队 |
| `int_c(int,int)` | 把某类型单位的训练排队到对应生产建筑 |
| `boolean_a(int,int,int)` | 价格检查（玩家,类别,类型） |
| `int_h(int)` | AI 建筑落位：以基地为中心螺旋找空格 |
| `short_a(int,int)` | 村民采集目标：就近资源格 |
| `var_int_k` / `r` | 随机地图生成参数（教学/战役取任务资源头 2 字节；随机图取 nfo[31,32]） |
| `aG` / `aj` | 教学关进度(0..2) / 战役进度(0..6)（曾按反编译注释猜反；nfo[28]=aG<<4\|aj） |
| `hdr[53]/[54]` | AI 记录的最近入侵者格 / 扫描指针+最近距离²（每帧扫一个玩家单位） |
| `hdr[55]` | 军队价值（训练时累加单位成本，攻防判定用） |
| `hdr[56]` | 生产速度（定点数，<<8；研究/模板可改） |
| `var_byte_arr_e/c/g` | res#122 单位属性 / res#127 科技旗标 / res#123 避障方向表 |
| `G()` / `J()` | 投射物目标获取 / 投射物飞行与伤害（箭塔与远程） |

## 半懂（保留旧名，先补注释）

aH（转场计时/激活参数双职责）、ap（返回节点）、var_int_arr_e（当局资源拷贝，
与 nfoHighScores 的关系待证）、var_int_arr_a[4]（脚本帧计数器，DSL 条件 2 用，
每帧 +1、动作 3 清零——已并入 AI/DSL 考证）、var_byte_arr_a（任务数据镜像，脚本会写执行标记——快照已包含）、var_boolean_k、
aI/aB 中的 aB、E/G/x/T/aD 等零散 int。

## 未考证（保留旧名）

 AgeOfEmpires.b 的多数静态位、var_byte_arr_c/e/g 的部分字段语义、
`j(int,int)`（框架抽象，无调用方可观察行为）、`w()`（仅转发退出标志）。

## 单字母名说明

字段 `t`、`u`、`w`、`z`、`K`、`M`、`q`、`ai`、`an`、`aw` 等与一批单字母方法
（含 `a`/`b`/`c` 的大量重载）尚未改名：要么语义未考证，要么重载族需要逐签名
分析。改名器（tools/renamer）已支持按"名字+参数表"精确定位重载，后续 wave
只需在 map.tsv 里补行——前提是先把语义搞懂。
