# 交接文档 — Age of Empires II (J2ME) macOS 桌面移植

> 2026-08-31 更新。项目说明见 `README.md`，游戏机制见 `GAME_NOTES.md`，本文档是
> 开发交接总纲：现状 / 目标 / 进行中工作 / 调试方法 / 坑。

## 一句话现状

反编译移植完成度很高：渲染管线（v3 设备分辨率持久帧缓冲）、键盘映射（含 WASD/
QEZC/X 别名）、鼠标支持（悬停高亮/单击选中/拖动框选/右键群移）均已完成并验证。
**本轮进行中（WIP）**：悬停 UX 改造（点击直达 + 边缘滚动，代码完成待验证）、
Dev 模式（headless ✓ 已验证、直进关卡 ✓ 可用、窗口模式下的自动导航有已知缺陷）。
细节见"当前进行中的工作"。

## 环境与构建

- JDK：`/opt/homebrew/opt/openjdk@17`（brew）。**没有 Gradle wrapper**，用系统
  gradle（9.x）。`gradle.properties` 固定了 `org.gradle.java.home`。
- 构建：`gradle classes` / 运行：`./run.sh`（内部 `gradle run`）。
- 直接跑 java（可传 JVM 参数，gradle run 不透传 -D）：
  `java -Daoe.debug=1 -cp "build/classes/java/main:build/resources/main" aoe.Main`
- 调试截图（只截游戏窗口，被遮挡也能截）：`tools/shot.sh /tmp/shot.png`
  （内部用 `tools/winid.swift` 枚举窗口 ID + `screencapture -l`）。
- 鼠标注入（无 pyobjc，用 Swift CGEvent）：`/tmp/mouse.swift` 的写法见 git 历史或
  重写（move/click/rclick/drag 四个命令，`CGEvent.post(tap: .cghidEventTap)`）。
  注意：osascript 打键盘、CGEvent 打鼠标都需要宿主终端的辅助功能/屏幕录制授权。
- 屏幕休眠会锁死键盘注入路径；长时间操作前注意。

## 代码地图

- `GAME_NOTES.md` — **游戏机制知识地图**（主循环/按键模型/状态机/菜单模板树/
  移动寻路/存档布局/data.res 资源索引）。读 `c.java` 前先看。
- `src/main/java/AgeOfEmpires/` — 游戏本体（CFR 反编译 + 人工修正清单见 README）。
  `c.java`（~7000 行）是状态机+渲染+逻辑全集；`AoeMidlet` 入口（有 `game()` 访问器）。
- `src/main/java/com/ulysseo/mad/` — In-Fusio J2ME 框架（定时器主循环、菜单资源读取）。
- `src/main/java/javax/microedition/` — 手写 Swing 适配层（lcdui/rms/media/midlet）。
- `src/main/java/aoe/` — `Main`（启动器）+ `DevHarness`（headless 测试驱动）。
- `decompiled/` — CFR 原始输出，仅参考；**以 src/ 为准**。
- `tools/` — 调试辅助（shot.sh / winid.swift / cfr.jar）。

## 当前进行中的工作（接手请先读）

### WIP-1 悬停 UX 改造（代码完成，**未验证**）

需求：桌面惯例——悬停不平移镜头（只高亮），点击 = 光标/镜头直达该格并就地确认，
镜头平移靠"鼠标贴窗口边缘滚动"或键盘。已实现于 `c.java` "桌面鼠标增强"段 +
`a(Graphics)` 尾部的高亮绘制 + `j()` 里的高亮格坐标记录 + `Canvas` 的 kind=4
（mouseExited）。**验证清单**（窗口模式跑起来试）：
1. 鼠标在画面内移动：镜头不动，鼠标所指格出现黄色菱形高亮。
2. 左键点村民：光标（红框）跳到该格并选中（教程目标应推进）。
3. 鼠标贴窗口四边：镜头持续向该方向平移（半速，每 2 tick 1 格）；鼠标离窗停止。
4. 拖动框选、右键群移回归正常。

### WIP-2 Dev 模式（L2 已验证，L1 部分工作）

- **L2 headless ✓ 已验证**：`-Daoe.headless=1` 时 `Display.setCurrent` 不创建窗口，
  游戏照常 tick、渲染进内存帧缓冲。配合 `-Daoe.mute=1`（MIDI 静音）、
  `-Daoe.tickms=N`（tick 周期，默认 80，dev 建议 40 加速）。
- **DevHarness**：`java -Daoe.headless=1 -Daoe.dev=tutorial:1 [-Daoe.tickms=40] -cp
  build/classes/java/main:build/resources/main aoe.DevHarness out.png [额外等待秒]`
  → 启动、自动导航进关卡、导出 PNG、打印状态、退出。
- **L1 自动导航**：`c.devStartMission` 守护线程注入按键走真实菜单流。
  **已知缺陷**：进入 Game Mode 之后的"循环器选模式/选关"步骤时序不稳，
  campaign/random 可能卡在菜单（tutorial:1 大多正常，也不保证）。
  **下一步建议**：两条路任选——(a) 继续打磨按键导航（重试 + 用菜单模板节点字节
  `int_c(4)+9+2` 读循环器当前位置做验证，位置读法已写在 devCyclerValue 的历史
  实现里，git 可查）；(b) 状态直跳：任务装配链已摸清（见下），照链直跳。
- **任务装配链（已摸清，直跳用）**：选关界面 FIRE → 菜单脚本动作 73 设
  `ac=32, aC=关卡-1` → 状态机进装载态 aA=11 → p() 的 full-redraw switch
  case 11 调 `boolean_c(0)` 建图 → 敌方初始化 `g(1,71,x)` 顺带把 am 切到简报
  （aA=2）→ F1 推简报 → aA=6 主视图。(ac,aC)→资源映射表在 `o(int)` 内
  （campaign aC0..6 → aF=103..；random aC0..2 → aF=118..120；同时定简报模板/字符串表）。
  直跳法上次尝试停在 aA=4，缺"装配后进入 aA=11/简报"的那几步状态切换，照上面链条补。

### 调试打印清单（全部 `-Daoe.debug=1` 门控，验证完可清）

`[mouseA]/[pick]/[mtick]/[corner1]/[corner2]/[band]`（鼠标链路，c.java/Canvas.java）、
`[fMenu]/[menuGate]/[fHead]`（菜单渲染与激活门，f(Graphics)）、`[void_a]`（键映射）、
`[trace]`（boolean_c/g/boolean_g 状态转换）、`[dev]`（自动导航）。
另有 `mad/e.java` 的 25 帧状态打印（ar/am/aA/aH）。

## 整体目标状态

把 2005 年的 J2ME《帝国时代 II》移植成一个"现代桌面小游戏"：键鼠双全、
可调速度、有 dev 基建（headless 测试、直进关卡）、画质可升级、最终 jpackage 成 .app。
功能层面原版内容全保留（战役 7 关已全解锁、随机地图 3 档全解锁）。

## 现代化候选清单（按价值/成本分档，与用户讨论过）

**第一梯队（小而实用，建议先做）**
- 游戏速度切换：tick 周期参数已就位（`aoe.tickms`），做个 1x/2x 的运行时切换 UI。
- 双击选同类：`h(0, 类型)` 原语现成（教程"全选村民"就是它），接鼠标双击即可。
- Ctrl+数字编组：需要新增编组存储（存选中单位槽位表），召回 = 重放 0x8000 置位。
- MIDI 换 SoundFont 音源 + 音量设置（`javax.sound.midi` 原生支持加载 soundfont）。

**第二梯队（中等成本）**
- 素材超分：`Image.ASSET_SCALE` 管线现成，离线超分后同尺寸替换即生效。
- 战局快照：序列化 c 的关键字段（单位表/资源/地图占位）——中等工作量。
- 鼠标 UX 进一步打磨（滚轮缩放？原版无缩放，谨慎）。

**第三梯队（大工程，想清楚再做）**
- 逻辑与渲染解耦（深调研见下）。
- AI 增强（深调研见下）。
- 更大地图 / 剧情编辑器：涉及 data.res 数据格式，属"重制"而非"移植"。

## 深调研一：逻辑与渲染解耦

**现状**：整个游戏是 paint-driven——每 tick 由定时器触发 `p(Graphics)`，
逻辑推进和渲染在同一个方法里完成（如任务主视图 `a(Graphics)` = `mouseTick()` +
`o()` 光标输入 + `f()` 镜头缓动 + `j()` 世界渲染）。tick 率 = 渲染率 = 12.5fps。

**解耦能换来什么**：纯逻辑 headless（不再需要为跑逻辑渲染 240x320）、快于实时的
仿真（回归测试提速）、渲染率与逻辑率解耦（60fps 平滑渲染 + 12.5fps 逻辑插值）、
确定性回放（配合固定 tick 已满足一半）。

**可行性评估**（好消息）：主视图已经是"逻辑段 + 渲染段"的自然分层——
`a(Graphics)` 的 `o()/f()/mouseTick()` 是纯逻辑（不画），`j()` 是纯渲染；
`e(Graphics)`（另一种任务视图）同样是先逻辑后渲染。菜单态（aA=4）的逻辑极薄
（f(Graphics) 内嵌的脚本解释器在渲染遍历里处理按键，这是最纠缠的部分）。

**建议路径**（如果要做）：
1. 先做"渲染到内存的 headless"（已完成，即 L2）——不解耦也能测试。
2. 第一步解耦：任务主视图把 `a(Graphics)` 拆成 `tick6()`（mouseTick+o+f）和
   `draw6(g)`（j+overlay），`p()` 在 state 6 时先 tick6 再 draw6。菜单态不动。
3. 风险点：`f(Graphics)` 这类"渲染遍历里消费输入 + 改模板字节"的方法是重灾区；
   反编译代码可能有未发现的隐式顺序依赖（先画后改 vs 先改后画）。
4. 结论：**建议缓做**——当前 headless 已经覆盖 80% 的测试需求；解耦放在
   "可读化重命名"之后做（先有可读代码再动刀），或永远不做（性价比存疑）。

## 深调研二：AI 增强

**现状（已核实）**：
- 敌人（玩家 1）的"AI"是**生产脚本**：`boolean_b(int 类型)`（c.java ~6341 行）——
  定时器（`aq`/`C` 计数）到期时按 `ar % 10` 伪随机选兵种，检查人口上限
  （`[1][3]`，硬上限 26）与兵种配额（`[1][57+i]/[66+i]/[75+i]`），再按**玩家当前
  时代**（`[0][0]`）切换克制兵种（n==2→3、5→6 之类），最后 `int_c(1, 兵种)` 扣
  资源入队。
- 单位行为 = 移动步进器（贪心直线 + 7 邻格局部避障，见 GAME_NOTES"移动与寻路"）
  + 攻击目标选择（`int_a(int,int,int)` 做距离平方比较选最近目标）。
- 关卡脚本（敌方进攻波次等）在 `d.java`（地图/剧本对象）与菜单脚本里，未深入。

**增强方向与成本**：
1. **低成本·手感向**：进攻波次触发器调参（现波次逻辑在剧本数据里）、难度 =
   生产间隔/人口上限参数化（`C`/`[1][3]` 都是字段，可直接改）。
2. **中成本·行为向**：给敌方单位加"受击响应/主动索敌"（现在大概率先靠脚本送死）：
   在单位 tick 里加一个"视野内敌人检测"（复用 `int_a` 的距离比较，遍历对方单位表
   O(n²)，n<30 无压力），发现即 `d(1, 目标格)` 迎击。
3. **高成本·真 AI**：行为树/效用 AI + 地图连通性分析（BFS 可达 + 预算路径）——
   需要先做逻辑/渲染解耦才值得，且 64x64 地图 + 原版关卡设计下收益存疑。
4. **结论**：先做 1（调参，几乎零风险）；2 做成可选开关（`-Daoe.ai=1`）；3 不建议。

## 注意事项与坑

- **ARM 弱内存模型（本轮实战教训）**：按键状态 `ax/ab` 由 EDT/dev 线程写、
  游戏 tick 线程读，已声明 `volatile`。任何新的跨线程游戏状态字段都要考虑这个
  （dev 自动导航曾因无 volatile 长期失灵，且现象是"确定性失败"，极难排查）。
- **菜单消费按键的时序**：`f(Graphics)` 每帧末尾清 `ax`，激活只在 item 类型字节
  == 5 的分支里判断——注入按键落在"帧末清零之后、下一次激活判断之前"的窗口才有效。
  自动导航的按键步骤必须带重试/状态验证。
- **aA 状态语义（动态观察，未完全考证）**：4=菜单列表屏，6=任务主视图（教程对话框
  弹出期间也报 2），2=简报/对话框/装载，11=任务装载，9=简报相关，1=另一任务视图
  （e()），0=构建中。改动状态机前对照 `p()`/`n()` 的分发。
- **状态机大量直写字段**（`aA`/`am` 直接赋值），boolean_g 只是其中一种入口；
  静态分析状态机不可靠，以动态观察为准。
- 线程模型：主循环 `java.util.Timer` 线程（p() 链），键盘/鼠标在 Swing EDT，
  dev 导航在自起守护线程。游戏按单线程假设写，跨线程写入用 volatile/锁。
- 鼠标事件是屏幕坐标命中：调试注入时游戏窗口不能被别的窗口盖住。
- 改游戏逻辑代码（c.java 等）先怀疑反编译错误，对照 `decompiled/` 与
  `javap -c`（原 jar 在仓库根目录）。
- **开发调试的黄金路径**：`-Daoe.headless=1 -Daoe.dev=tutorial:1 -Daoe.tickms=40
  aoe.DevHarness out.png` 一条命令拿到"进关后的画面 PNG + 状态打印"，不用碰 GUI。
  自动导航不稳时，回退窗口模式 + `tools/shot.sh` + osascript/CGEvent 注入。
