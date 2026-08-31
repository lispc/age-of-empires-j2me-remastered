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
| gameMode | ac | 0=教程 16=随机地图 32=战役（菜单脚本动作 65/71/73 设置） |
| missionIndex | aC | 当前任务序号（战役 0..6，随机 0..2） |
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
| menuScreenId | H | 子画面选择器（11=随机选关 12=战役选关 1=主菜单） |
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

## 半懂（保留旧名，先补注释）

aH（转场计时/激活参数双职责）、ap（返回节点）、var_int_arr_e（当局资源拷贝，
与 nfoHighScores 的关系待证）、var_int_arr_a[4]（[1..3] 疑似动画计数、[0] 未知）、
var_byte_arr_a（任务数据镜像，脚本会写执行标记——快照已包含）、var_boolean_k、
aI/aB 中的 aB、E/G/x/T/aD 等零散 int。

## 未考证（保留旧名）

 AgeOfEmpires.b 的多数静态位、var_byte_arr_c/e/g 的部分字段语义、
`j(int,int)`（框架抽象，无调用方可观察行为）、`w()`（仅转发退出标志）。

## 单字母名说明

字段 `t`、`u`、`w`、`z`、`K`、`M`、`q`、`ai`、`an`、`aw` 等与一批单字母方法
（含 `a`/`b`/`c` 的大量重载）尚未改名：要么语义未考证，要么重载族需要逐签名
分析。改名器（tools/renamer）已支持按"名字+参数表"精确定位重载，后续 wave
只需在 map.tsv 里补行——前提是先把语义搞懂。
