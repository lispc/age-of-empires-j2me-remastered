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

## 已改名：wave2（2026-09-01，AI / 任务脚本 DSL / RNG）

字段（AgeOfEmpires.c）：

| 新名 | 旧名 | 语义 |
|---|---|---|
| aiEnabled | var_boolean_i | AI 开关（默认 true；战役 m0..3,5,6 关；只在 setupMissionEnv 赋值且从不清除） |
| aiBuildInterval | an | AI 建造步进周期：Easy 250 / Medium 150 / Expert 100 |
| aiAttackThreshold | q | AI 进攻所需军队价值：50/60/100 |
| aiTrainInterval | C | AI 训练尝试周期：20/20/1 |
| aiGuardRadiusSq | var_int_l | AI 基地警戒半径²：49/36/25 |
| aiFreeResInterval | aw | "定时白送资源"周期——**死代码**（白送量 hdr[57] 恒 0） |
| aiGatherMultiplier | aM | AI 采集产量倍率（256=1×）：512/786/2048 |
| aiBuildOrder | var_byte_arr_j | AI 硬编码建造顺序表（`类型,数量` 对，-1 分阶段，-2 结束） |
| aiBuildTarget | M | AI 当前建造目标（void_a() 选取，tryPlaceAiBuilding 落位） |
| aiBuildPhase | ai | AI 建造顺序的阶段游标（aiBuildOrder 下标） |
| aiFreeResTimer / aiBuildTimer / aiTrainTimer | w / E / aq | AI 三个节流计时器（白送资源/建造/训练） |
| tutorialProgress | aG | 教学关进度 0..2（曾按反编译注释猜反；nfo[28] 高半字节） |
| campaignProgress | aj | 战役进度 0..6（nfo[28] 低半字节） |
| rngStateHi / rngStateLo | r / var_int_k | 全局随机数状态对（xorshift 变体；生成器当种子，nfo[31,32] 持久化） |
| techFlags | var_byte_arr_c | 科技旗标（res#127 初值；研究完成/DSL 动作 9 写，build 菜单门控读） |
| costTable | var_byte_arr_e | 造价表 res#122（每项 3 字节，canAfford 查、购买时扣） |
| dirTable | var_byte_arr_g | 移动避障方向扇形表 res#123（AI 落位螺旋也用） |

方法（AgeOfEmpires.c）：

| 新名 | 旧名 | 语义 |
|---|---|---|
| tickAi | z | AI 大脑：威胁扫描 → 攻/防模式判定 → 军队指令 + 村民自动采集 |
| setupMissionEnv | o(int) | 任务环境装配/拆除（n=0 拆 1 装）：难度参数、资源装载都在这 |
| aimProjectiles / tickProjectiles | G / J | 投射物目标获取 / 飞行与伤害（箭塔与远程）；G 的扫描边界经原 jar 字节码考证，2026-09-01 修复的卡死系 CFR 伪影（见 game-mechanics 投射物节） |
| tickMissionScript | F | 任务事件脚本解释器（每帧扫块：条件→动作，块头负=已触发） |
| skipScriptBlock | int_g(int) | 跳过一个完整脚本块（条件+动作） |
| skipScriptActions | int_a(int) | 从条件尾跳到块尾 |
| scriptBlockOffset | int_f(int) | 走 N 个块头，返回目标块偏移（ARM/解锁用） |
| evalScriptCondition | int_d(int) | 脚本条件求值（≥0 真 / 负 假） |
| runScriptActions | int_d(int,int) | 脚本动作执行（操作码见 game-mechanics） |
| tryPlaceAiBuilding | boolean_b() | AI 建造一步：买得起就在基地旁落位当前目标 |
| tryTrainAiUnit | boolean_b(int) | AI 训练器：兵种=参数轮换，受人口/上限/价格约束 |
| findAiBuildSpot | int_h(int) | AI 建筑落位：以基地为中心螺旋找空格 |
| findNearbyResource | short_a(int,int) | 村民采集目标：就近资源格 |
| canAfford | boolean_a(int,int,int) | 价格检查（玩家, 类别 0=单位/1=科技/2=建筑, 类型；costTable 行 = 类别×10/26 偏移 + id） |
| queueUnitTraining | int_c(int,int) | 把某类型单位的训练排队到对应生产建筑 |
| nextRandomInt | int_a() 静态 | 全局 RNG 一步，返回低 8 位 |

## 已改名：wave3（2026-09-01，兵种/建筑属性 + 菜单考证）

考证产出见 docs/unit-stats.md。方法（AgeOfEmpires.c）：

| 新名 | 旧名 | 语义 |
|---|---|---|
| payCost | c(int,int,int) | 购买扣款（玩家, 类别 0=单位/1=科技/2=建筑, 类型；负数钳 0） |
| refundCost | b(int,int,int) | 取消退款（对称加回） |
| openBuildingMenu | l(int) | 选中生产/经济建筑后开 训练/研究/建造 菜单（按建筑类型分派 + 科技门控） |
| buildActionMenu | m(int) | 菜单构建：按 var_int_g 组装条目表 var_int_arr_c + 文本对 |
| clearActionMenu | n() | 菜单清理（条目表/文本置空） |
| tickMoveTimer | f(int,int) | 单位移动节流：按兵种装填值递减 slot[6]，到点走一步（兵种移速差全在这） |
| tickConstruction | void_c(int,int) | 村民施工：目标建筑每 tick +1~2 HP 至 255 |
| convertUnitType | e(int,int) | 兵种整体换形（升时代 2↔3、5↔6）：改全员类型并搬移计数 |

## 已改名：wave4（2026-09-01，主循环/逻辑分辨率/相机/全图视图/音乐考证）

宽视野落地会话的产出（证据与语义见 DEVELOPMENT.md「宽屏视野落地」+ 现场注释）。
注意 `j(int,int)` 是框架基类 `com.ulysseo.mad.a` 抽象方法的 override，两侧同名一起改。
字段（AgeOfEmpires.c）：

| 新名 | 旧名 | 语义 |
|---|---|---|
| viewTileRows | aB | 可视格行数（`(screenH>>4)+5`，iso 16px/行步长） |
| bottomBarY | aL | 底栏（消息/跑马灯条）上缘 y |
| camTargetX / camTargetY | az / al | 相机缓动目标 = 光标格 iso 坐标 - 半屏偏移（ad/J） |
| mediaRequestId | o | 当前请求的媒体资源 id（-1 无；requestMedia 置，onPaint 异步装） |

方法（AgeOfEmpires.c）：

| 新名 | 旧名 | 语义 |
|---|---|---|
| setupScreenMetrics | m(int,int) | 由逻辑分辨率派生全部屏幕度量（宽视野 -Daoe.width 经此生效） |
| onScreenSizeChanged | j(int,int) | 框架 sizeChanged 回调（= setupScreenMetrics） |
| startGameCanvas | void_b() | 显示 Canvas + 启动 80ms 主循环定时器（aoe.tickms） |
| loadKeymap | void_b(int) | 键位表加载（data.res #129 → keymap） |
| playNextBgm | c() | 随机选下一首 BGM（曲长 ms/80 折帧倒计时，硬编码原版帧率） |
| requestMedia | a(int,boolean) | 请求播放媒体资源（同 id 幂等） |
| stampThumbTile | b(Graphics,int,int) | 全图缩略图上钉一格（1px/格，迷雾格画深色） |

## 已改名：wave5（2026-09-01 晚，投射物池考证 + 卡死修复）

考证产出见 docs/game-mechanics.md 投射物节。字段（AgeOfEmpires.c）：

| 新名 | 旧名 | 语义 |
|---|---|---|
| projectileTable | var_short_arr_arr_b | 投射物记录池 [2][20]：每玩家 5 条×4 short 紧凑存放，布局 +1 状态字(1000=待瞄准)/+2 目标格/+3 飞行计时，活跃数在 headers[48] |
| missionScript | var_byte_arr_a | 任务脚本字节码缓冲（res 装载；tickMissionScript 解释器读，写"已执行"标记；127=区结束） |

注意：AgeOfEmpires.b 里另有同名旧静态 var_byte_arr_a（媒体缓冲，未读懂未改名），
与 c.missionScript 无关。

## 半懂（保留旧名，先补注释）

aH（转场计时/激活参数双职责）、ap（返回节点）、var_int_arr_e（当局资源拷贝，
与 nfoHighScores 的关系待证）、var_int_arr_a[4]（脚本帧计数器，DSL 条件 2 用，
每帧 +1、动作 3 清零——已并入 AI/DSL 考证）、var_boolean_k、
aI/aB 中的 aB、E/G/x/T/aD 等零散 int、
ad/J（相机半屏居中偏移，2:1 菱形投影精确推导未考证——只当"半屏修正"用）、
m（int 字段：BGM 曲长帧倒计时，但菜单/世界两态都有写入点，双职责未全证）、
l(int,int)（在单位周围 3×3 置/清迷雾位 0x4000，重标时机与用途未证）。

迷雾位考证（2026-09-01，曾被字面 grep 误导后由 regress golden 纠正）：
mapTiles 的 0x8000 = 未探索（装载大面积置位、随探索清除；short 置位后变负，
stampThumbTile 据此走深色分支 = 黑迷雾）；0x4000 = 二级暗化位（l(int,int) 每帧
在单位周围置位；世界地块渲染走暗化变体——疑为"当前可见/已探明"两级迷雾，
细节未证）。0x8000 的置位走 `a(int,int,int,int,int)` 矩形填充的参数间接传入，
字面 grep "0x8000" 找不到 setter。

## 未考证（保留旧名）

 AgeOfEmpires.b 的多数静态位、
`j(int,int)`（框架抽象，无调用方可观察行为）、`w()`（仅转发退出标志）。

## 单字母名说明

字段 `t`、`u`、`K` 等与一批单字母方法（含 `a`/`b`/`c`/`int_b`/`int_e` 的重载）
尚未改名：要么语义未考证，要么重载族需要逐签名分析。wave2 已验证改名器可
以"名字+参数表"精确定位重载（`boolean_b` 三兄弟改二留一、`int_a`/`int_d` 各
按签名拆分），后续 wave 只需在 map.tsv 里补行——前提是先把语义搞懂。
