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
| nextRandomInt | int_a() 静态 | 全局 RNG 一步，返回低 8 位。**只许模拟消费**（2026-09-01 起）：剩余消费点仅 tickConstruction 掷骰；BGM 选曲已分流到 nextBgmRandomInt（同 LCG 独立状态），确定性回放的前提 |
| nextBgmRandomInt | （新增，非原名） | 化妆品 RNG：只服务 playNextBgm 选曲。新增非模拟随机需求一律走这里 |

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

宽视野落地会话的产出（证据与语义见 WORKLOG.md「宽屏视野落地」+ 现场注释）。
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


## 已改名：wave6（2026-09-01 深夜，随机地图/音乐倒计时 + 文档拆分会话）

字段（AgeOfEmpires.c）：

| 新名 | 旧名 | 语义 |
|---|---|---|
| randomMap | var_boolean_k | 本局走随机图生成：任务资源字节[0..1]（地图种子）为零时置位；d.a 据此走生成器，t() 补城镇中心 |
| bgmFramesLeft | m | 距下次 BGM 换曲帧数（playNextBgm 写 曲长ms/80；v() 刷 510；装载清 0；模拟块 -- 触发） |

注意：AgeOfEmpires.c 另有**无参方法 m()**（.nfo RecordStore 读写），与字段无关；
单字母改名后注释同步曾误伤该方法注释，已人工修复（wave4 教训再现）。

## 已改名：wave7（2026-09-03，玩家代理 32 轮考据批次 + 文档归并）

> 原 docs/deobfuscation.md（2026-09-03 玩家代理工程整理轮创建）并入本文件——
> 该文档的符号映射内容在本节与既有表格中，结构语义考据见下方「补充结构语义」节。
> 证据链：/tmp/aoe-play22,25/BUGS*.md（r29-r31 战报）+ 读码。

字段（AgeOfEmpires.c）：

| 新名 | 旧名 | 语义 |
|---|---|---|
| buildingTable | var_int_arr_arr_b | **建筑表** int[p][stride4]，i<headers[p][4]：[+0]tile 打包(tx=`>>8&0x3F`,ty=`&0x3F`) [+2]状态(0x40000000=施工中,&0xFF=进度,255=完工) [+3]&0xFF=type。⚠️ 别拿它数单位——rally 宏曾因此零移动+回显失真（r31 实锤，修于 dd96c2f） |
| selectedTrainProduct | aK | 当前生产菜单选中待训的产品（openBuildingMenu 设默认、菜单光标可选；aK→建筑映射硬编码在 queueUnitTraining）。aK=0 产物 fifo 显示 type=1（村民第二种），t0 是任务初始赠品（r30 实测 4/4） |
| dialogScriptId | z | 当前对话框/简报的脚本 id（startMissionBriefing 唯一写入）：62=任务简报 70=难度/链页 74=失败简报 **98=胜负结算** |
| briefingVariant | V | 对话框变体/正文索引；z=98 结算时 **0=Victorious 1=Defeated**（r23-r31 判读位） |

方法（AgeOfEmpires.c）：

| 新名 | 旧名 | 语义 |
|---|---|---|
| countBuildings(int,int,boolean) | a | ⚠️ 数的是**建筑表**（bl=true 排除施工中）——勿当单位计数用；真单位计数用 devCountUnits（r32 基元） |
| onThingDestroyed(int,int) | i | 建筑/单位摧毁处理器：清 mapTiles 占位、techFlags 置位、pop 回收、**胜负判定**——case 9(TC)：我方 TC 毁→98,1 即败；敌 TC 毁→98,0 即胜（gameMode 32 链 missionIndex≠0 时跳过，走 missionScript 路径）。r30 源码定案+r31 实战双验证（敌 0 单位+TC 毁同轮触发；敌工人存活不影响即胜） |

注意：单字母 a/i/z/V 改名后 renamer 注释同步经全量 diff 复核，无 wave4 式误伤
（英文冠词 "a" 因词边界+上下文未被动；check 模式对单字母名报大量噪声——循环变量
i 与其他类同名符号均非本表目标，已用编译+javac 解析+`this.i(`/`var_int_arr_arr_b`
绝迹 grep 替代验证）。

## 已改名：wave8（2026-09-04，移动/索敌/伤害/世界 tick/迷雾对/AI 攻势批）

证据：c.java 现场读码（含调用点全枚举）+ game-mechanics/unit-stats 既有考证 +
RuleBasedAi 注释交叉印证；迷雾对方向（reveal vs dim）由 tickMoveTimer 的
"离开旧格→dim、到达新格→reveal"调用序 + p() 每 tick 全单位 relight + removeUnit
入口 dim 三处互证。d.java 两字段由类头注释 + c.java 消费点（3612-3625 出生点摆
TC/村民）钉死。

字段（AgeOfEmpires.c 除注明外）：

| 新名 | 旧名 | 语义 |
|---|---|---|
| aiStance | var_int_i | AI 攻势模式：0=种田/1=防御（入侵者进警戒半径²）/2=进攻（军值过阈）；tickAi 扫描完成时写、发令后清 0（fields 可读） |
| randomMapDifficulty | var_byte_a | 随机图难度档位 0/1/2（菜单脚本 op 66 写；setupMissionEnv 的 gameMode==0 分支读，决定 AI 参数档） |
| scriptFrameCounters | var_int_arr_a | 任务脚本 4 个帧计数器（每帧 +1、DSL 动作 3 清零、条件 2 做"经过 N×10 tick"判定）；在 regress-noise 里（两 dump 间合法漂移） |
| actionMenuItemIds | var_int_arr_c | 动作/建造菜单槽位 id 表（buildActionMenu 组装，含 techFlags[36] 塔升级偏移；渲染/支付/排队都经它取条目 id；clearActionMenu 置 null） |
| mapTiles（AgeOfEmpires.d） | var_short_arr_a | 生成器持有的地图格缓冲引用（= c.mapTiles 同一数组） |
| seedPoints（AgeOfEmpires.d） | var_int_arr_b | 影响扩散的种源坐标表（x,y 对；g() 随机漫步布源/a(int) 散布，e() 按 var_int_c 个取正影响 +m/距离²） |

方法（AgeOfEmpires.c）：

| 新名 | 旧名 | 语义 |
|---|---|---|
| stepUnitMove | boolean_b(int,int) | 单位移动单步进器：DDA 直线步进 + 落点三重检查 + 7 邻格扇形回退 + 占位表更新；落点==目标且被占时触发抵达钩子；BFS 寻路只换"选落点"一步 |
| acquireTarget | boolean_a(int,int) | 索敌：**槽序第一个**进判定半径者（非最近）；邻接（d²≤1）双方互锁进攻击态，远程（t4/8）圈内直接锁定，其余写 slot[2] 追击 |
| tickAutoEngage | B() | 索敌节流处理器：每玩家每 tick 只处理 1 个单位（tickCount 轮转起点；原版字节码语义，CFR 曾丢循环出口）；我方单位要站在目标格上才自动接敌 |
| nearestDropOff | int_a(int,int,int) | 交存点选择：按任务字高 nibble（资源种）在 TC+伐木场/双采矿场中取距离平方最近者；**不是战斗索敌**（deep-dive-2 曾误记） |
| removeUnit | g(int,int) | 单位死亡/移除唯一入口：[combat] 日志、pop/军值回收、mapTiles 置 0x4000 残迹、槽位尾交换压缩、0 单位+0 建筑判负 |
| tickUnits | g() | 主单位更新链：两玩家全单位按任务字分发——case 0 闲置回血（tickCount&8 门）/直通移动、case 1 战斗装填与出手、case 2 采集计时与耗尽迁格、case 3 回送移动、case 4 施工 |
| tickBuildings | j() | 建筑 tick：施工 +8/帧至 255 完工（人口/科技旗标/塔注册投射物池/完工弹窗）、研究进度、训练队列推进与出兵（含时代变形） |
| onUnitArrived | c(int,int,int,int) | 单位抵达钩子（stepUnitMove 唯一调用点）：资源格→采集、己方建筑→交存/施工修理、敌方建筑/单位→接敌并互锁反击 |
| resolveAttack | d(int,int,int,int) | 伤害结算：攻×16÷目标护甲（整除），对单位/建筑两路；致死分别走 removeUnit/onThingDestroyed；命中时把受害者 slot[2] 改指攻击者（反扑改写） |
| revealFogAroundUnit | void_d(int,int) | 点亮单位周围 3×3（清 0x8000 未探索 + 0x4000 暗化）；p() 每 tick 对我方全单位调用，出生/移动到达/雾下战斗单位被画时也调 |
| dimFogAroundUnit | l(int,int) | 单位离开旧格/死亡时把旧位置 3×3 转"已探明但无人看守"（清 0x8000、置 0x4000 暗化位）；渲染对暗化格上非战斗敌单位跳过绘制 |

注意：①`void_d(int)`（菜单按键处理）与 `l(Graphics)`（渲染）是未改名的同名
重载，勿混。②renamer 的注释同步对单字母旧名会漏扫（commentSpans 的引号配对
被注释里的撇号打乱），本波的注释同步全部由人工逐条补齐/回退——唯一自动误伤
是 tickAutoEngage 体内"原版字节码（方法 B,175…)"的考据注释被改，已人工还原
（引的是原 jar 方法名）。③0x4000 暗化位会隐藏"暗化格上非战斗中的敌单位"
（renderWorld 单位段守卫）——与第六批"只切地表明暗"记录在单位维度上冲突，
以本波读码为准（实测复核留给后续）。

## 补充结构语义（原 deobfuscation.md 考据，2026-09-03）

**胜负判定双路径**（r30/r31 定案）：①敌 TC 毁→即胜（工人/村民不计入，r30 实测敌
4 工人存活即胜；32 链模式例外见上）②我方 TC 毁→即败（与"0 单位=败北"互相独立的
两条判定）。③missionScript 脚本条件（opcode 4=胜 5=败）为另一路径。

**单位任务字**（playerUnitSlots[+7]，r32 读码钉死）：低 nibble=任务态 **0=闲置
1=行军 2=采集中（高字节 0x66=满载计时，bit4-5=资源种）3=回送中**；selectUnits 的
村民分支（type<2 且 nibble==0）=游戏自己的"选闲置村民"判定，assign 宏同款。

**单位表/建筑表速查**：playerUnitSlots stride8（[+0]tile 打包 tx=`>>>8`/ty=`&0xFF`、
[+3]type、[+4]&0x8000 选中位、[+7]任务字），`i<headers[2]` 全是活单位；
buildingTable 见上表。playerUnitHeaders：[0]=age [2]=单位数(pop占用) [3]=pop上限
[4]=建筑数 [5][6][7]=木/金/石 [49]=在训队列长。

**地图格编码**（mapTiles short）：0x8000=未探索雾（gather/build 雾检位）；0x300=资源
（低2位 1木2金3石，高字节≈剩余量非种类）；0x200=单位占位（低字节=槽位）；
0x100=建筑占位（低字节=序号，bit10-11=owner）；雾下 **0x83xx=资源（&3 判种）、
0x85xx=建筑（低字节=type）——雾中信息可读**（r31 免侦察地图术）；0x0=虚空/清零；
0x1604=rubble；-32768=未初始化。

**玩家 AI/工具钩子**：`-Daoe.bfsPath=1` 可选 BFS 寻路（player-ai 分支 2026-09-03
合入，默认关）；`-Daoe.playerAi=<类名>` 帧首 AI hook（aoe/ai/RuleBasedAi 范例）；
FIFO 宏清单见 DEVELOPMENT.md；存档解析 `tools/aoesave.py`（只读，类型化解码表内置）。

## 半懂（保留旧名，先补注释）

aH（转场计时/激活参数双职责）、ap（返回节点）、var_int_arr_e（当局资源拷贝，
与 nfoHighScores 的关系待证）、
aI/aB 中的 aB、E/G/x/T/aD 等零散 int、
ad/J（相机半屏居中偏移，2:1 菱形投影精确推导未考证——只当"半屏修正"用）、
var_boolean_f/var_boolean_b（渲染分发处的左下/右下角图标开关，逐屏置位；图标
本体语义未钉死，且 var_boolean_b 与 AgeOfEmpires.b 同名静态撞注释空间，wave8 跳过）、
d.var_int_arr_a（双出生点 x0,y0,x1,y1——c.java:3612-3625 消费点已读，但与
c.scriptFrameCounters 同 oldName 撞 renamer 注释表，留 wave9）。

（2026-09-01 晚移出：var_boolean_k → randomMap、m → bgmFramesLeft，见 wave6——
后者"菜单/世界双写入点"经核实是同一职责的两处赋值，非双职责。）
（2026-09-04 移出：var_int_arr_a → scriptFrameCounters、l(int,int) →
dimFogAroundUnit，见 wave8。）

迷雾位考证（2026-09-01 立案，2026-09-04 wave8 闭环）：
mapTiles 的 0x8000 = 未探索（装载大面积置位、随探索清除；short 置位后变负，
stampThumbTile 据此走深色分支 = 黑迷雾）；0x4000 = 暗化位（已探明但当前无人
看守）——dimFogAroundUnit 在单位离开旧格/死亡时置位，revealFogAroundUnit 在
p() 每 tick/单位到达/出生时清除；渲染对暗化格走地表暗化变体，且跳过暗化格上
非战斗中的敌单位绘制。0x8000 的置位走 `a(int,int,int,int,int)` 矩形填充的参数
间接传入，字面 grep "0x8000" 找不到 setter。

## 未考证（保留旧名）

 AgeOfEmpires.b 的多数静态位、
`j(int,int)`（框架抽象，无调用方可观察行为）、`w()`（仅转发退出标志）。

## 单字母名说明

字段 `t`、`u`、`K` 等与一批单字母方法（含 `a`/`b`/`c`/`int_b`/`int_e` 的重载）
尚未改名：要么语义未考证，要么重载族需要逐签名分析。wave2 已验证改名器可
以"名字+参数表"精确定位重载（`boolean_b` 三兄弟改二留一、`int_a`/`int_d` 各
按签名拆分），后续 wave 只需在 map.tsv 里补行——前提是先把语义搞懂。
