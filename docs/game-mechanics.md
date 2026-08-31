# 游戏机制知识地图

反编译代码（`AgeOfEmpires/c.java`）的领域知识地图。定位：

- 这里只放**跨文件的系统性知识**和索引，帮助你在读代码前建立全局图景；
  单点机制细节写在**代码现场注释**里，两处不重复。以代码注释为最新、本文为导览。
- 标注【已验证】的条目都经动态运行确认；标【推断】的仅由静态阅读得出，改动前先验证。
- 2026-08-31 起已混淆名已批量改为可读名（完全搞懂的符号），本文同步更新；
  新旧对照表见 `docs/symbols.md`，改名的工具与流程见 `DEVELOPMENT.md`。

## 主循环与时序

- 【已验证】80ms 一帧（12.5fps），游戏本体写死：`c.java` 构造流程里
  `var_com_ulysseo_mad_b_a.a(80, 1)`（`c.java:266` 附近）启动框架定时器
  （`mad/b.java`，`java.util.Timer`）。
- 【已验证】每帧流程：`mad/e.java` 的 `run()` → `c.w()`（仅检查退出标志）→
  `repaint()+serviceRepaints()` → `mad/b.java` 的 `paint()` → `c.onPaint(Graphics)`。
  **`p()` 是真正的主循环体（paint-driven）**：先推进游戏逻辑再渲染。
  `tickCount` = 帧计数（`-Daoe.debug=1` 日志里的 `ar=` 标签就是它——日志字面量未随改名更新，每帧 +1）。

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
    `var_int_arr_a[玩家][2]`。群移指令 = `orderMove(0, tx, ty)`；按类型多选 = `selectUnits(0, 类型)`；
    全清 = `clearSelection()`。键位表（data.res #129）：-5→22（FIRE/确认），±方向→19/21/23/25
    （平移位掩码），-7→47（右软键/菜单）。

## 状态机速查（未完全考证）

- `screenState` = 顶层画面状态，`p()` 内按它分发渲染/更新；`beginStateTransition()` 在 `screenState` 变化时调
  对应构建器（`t()`/`d()`/`A()`/`r()`/`n()`/`C()`/`u()`…）。
  `pendingScreenState` = 配套"当前"值（`requestStateSwitch(n)` 设置），`pendingScreenState == screenState` 表示画面稳定。
- 【已验证】`pendingScreenState = 4` 出现在所有列表菜单屏（主菜单 / Game Mode / 选关 / 设置），
  但任务进行中 debug 也显示 `pendingScreenState = 4`——`pendingScreenState` 并非画面的唯一标识，**改动状态机
  前先对照 `p()` 的分发逻辑**。
- `H` = 子状态/对话框选择器，`activateMenuScreen` 按它加载对应菜单模板并打补丁：
  【已验证】11 = 随机地图选关，12 = 战役选关，1 = 回主菜单（过关/打完流程设置）。

## 菜单 / 对话框模板系统

- 【已验证】`menuTree` = 当前菜单模板字节，`a(n, true)` 从 `data.res` 加载
  （**131 = 选关对话框**）。结构是树状节点，`menuItemNode(n)`/`int_e(n)`/`int_k`/`int_i`
  遍历；节点参数在偏移 +9 起：**循环器控件**（"◄ Mission 1 ►"这种左右切换项）
  `+9+1` = 选项总数，`+9+2` = 当前选中项。左右键循环逻辑在 `c.java:982/993` 附近
  （到达总数即回绕到 0）。
- 【已验证】item 激活时执行一段内嵌脚本，操作码 switch 在 `c.java:1225` 附近：
  - 操作码 3 = 模式/设置项激活，内部分发动作码：**65 = 教程**（并按参数把教程
    选关上限补成 11/12/10）、**71 = 随机地图**、**73 = 战役**、67/72 = 设置开关。
  - 操作码 5 = 改写模板某字节；8 = 打开高分屏（拷贝 `nfoHighScores` →
    `var_int_arr_e`）。
- 【已验证】选关上限只有一处生效点：`activateMenuScreen` 的 menuScreenId-case 11/12 模板补丁
  （原版写 `campaignUnlock+1`/`randomUnlock+1`，移植已改为固定 3/7 全解锁）。启动任务时无二次校验。

## 游戏模式与关卡

- 【已验证】Game Mode 三项（Game Mode 屏是循环器）：Tutorial → Campaign →
  Random Map。`gameMode`：0 = 教程，32 = 战役，16 = 随机地图。
- 【已验证】战役 7 关（`missionIndex` 0..6），可选上限 = `campaignUnlock+1`；随机地图 3 档，上限 = `randomUnlock+1`。
  `campaignUnlock`/`randomUnlock` 只在胜利结算时推进：`startMissionBriefing(0, 98, n3)`（`n3 == 0` 为胜）→ 推进计数、
  写高分、`h(28, 1, randomUnlock<<4|campaignUnlock)` 持久化。战役最后一关（`campaignUnlock` 已到 6）胜利后回主菜单。
- 【已验证】进入关卡流程：选关 → 战役简报（灰色对话框，"Next" 在左下角 =
  左软键，桌面按 **F1**，回车通常也可推进）→ 地图。
- 【推断】教程（gameMode=0）的进度存 `.nfo` 字节 31 起两字节（`startMissionBriefing()` 的 case 0），
  具体含义未考证。

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

## 存档（.nfo）

- 【已验证】`nfoData`（314 字节），`loadNfo()` 读写 RecordStore `.nfo`
  （桌面 shim 落盘为 `~/.aoe-desktop/` 下的文件）。布局：
  - `[0..27)`：7 个 4 字节大端整数 = 战役每关高分（`nfoHighScores`，按 `missionIndex` 索引）
  - `[28]`：高半字节 `randomUnlock`，低半字节 `campaignUnlock`
  - `[29]`：音效开关（0 = 开，对应 `var_boolean_d`）
  - `[30]`：另一开关（`AgeOfEmpires.b.c`，含义未考证）
- 【已验证】读写助手：`nfoReadInt(offset, len)` 读大端、`h(offset, len, value)` 写大端。

## data.res 资源索引

- 【已验证】**#129** 键位表（键码→动作码，`loadKeymap(129)`）。
- 【已验证】**#131** 选关对话框模板（菜单树）。
- 【推断】字符串表按资源号加载（`AgeOfEmpires/a.java` 读取 UTF-8 文本），
  常见调用 `new a(65)`（说明文字）、`new a(97)`、`new a(99)`、`new a(H + 83)`
  （对话框相关，随 H 变化）。全包约 134 个资源，图片为自定义位打包格式，
  运行时由 `AgeOfEmpires/b.java` 拼成 PNG 解码（详见 README）。

## 调试速查

- `-Daoe.debug=1`：每 25 帧打印 `tickCount/pendingScreenState/screenState/aH/l/j/fullRedraw`（`mad/e.java`）。
- `-Daoe.dumpFrames=/path.png`：每 ~5 秒导出当前帧缓冲（`Canvas.java`）。
- 自动化验证（FIFO 注入 + 帧缓冲 dump + 黄金回归）：见 `DEVELOPMENT.md`。
