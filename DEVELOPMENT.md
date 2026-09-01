# 开发手册 — Age of Empires II (J2ME) macOS 桌面移植

> 本档是**持续活跃的手册**：现状 / 环境 / 工具用法 / 路线图 / 坑，只写现在时态。
> 带时间序的会话日志在 `WORKLOG.md`（append-only）；一次性考证报告在 `docs/research/`；
> 游戏机制知识 `docs/game-mechanics.md`；符号对照 `docs/symbols.md`；玩家文档 `USER-GUIDE.md`。

## 一句话现状

反编译移植完成度很高：渲染管线（v3 设备分辨率持久帧缓冲）、键鼠双全（悬停高亮/
单击选中/拖框选/右键群移）、宽视野（run.sh 默认 720x320 逻辑宽，原版 240 可退回）、
快照存档 v3（F5/F9 + 自动 checkpoint + devBoot 直启；v3=techFlags 解锁位持久化，
v2 旧档可读）、**确定性回放**（RNG 分流 +
tick 戳输入 trace + tools/replaycheck.sh 双跑对拍）、**卡死看门狗**（Timer 线程停跳
自动打栈进日志）。反编译血统是 CFR，已采信 Vineflower 为对照 oracle（见「不变量与坑」）。

## 环境与构建

- JDK：`/opt/homebrew/opt/openjdk@17`（brew）。**仓库已带 Gradle wrapper**（9.7.1），
  构建只依赖 JDK：`./gradlew classes`；`run.sh` 优先用 wrapper。`gradle.properties`
  固定了 `org.gradle.java.home`。
- 直接跑 java（可传 JVM 参数，gradle run 不透传 -D）：
  `java -Daoe.debug=1 -cp "build/classes/java/main:build/resources/main" aoe.Main`
- 日志：`run.sh` 每次运行 tee 到 `~/Library/Application Support/AoeJ2ME/logs/run-*.log`
  （保留 10 份）；`build.gradle` 给 run 默认加 `-Daoe.debug=1`（安静窗口
  `AOE_QUIET=1 ./run.sh`）。
- **本机 TCC 授权坑**：宿主终端没有辅助功能/屏幕录制授权 → CGEvent 注入、
  `screencapture -l` 不可用。视觉验证走 `-Daoe.dumpFrames` / FIFO `dump`；
  输入注入走 `-Daoe.devMouse`。要恢复真实注入：系统设置给宿主 App 授权。
- 屏幕休眠会锁死键盘注入路径；长时间操作前注意。

## 代码地图

- `USER-GUIDE.md` — 玩家手册（键鼠/小地图/存读档），开发者内容别写进来。
- `docs/game-mechanics.md` — 机制知识地图（主循环/按键模型/状态机/菜单模板树/
  投射物/移动寻路/确定性模型/存档布局/data.res 索引）。读 `c.java` 前先看。
- `docs/unit-stats.md` — 兵种/建筑/科技属性总表（res#121/122 逐字段考证）。
- `docs/symbols.md` — 符号字典（混淆名↔可读名 + 语义 + 考证分级；半懂清单在此）。
- `docs/research/` — 深调研报告（一次性考证，结论速览在「路线图」）。
- `WORKLOG.md` — 工作日志（append-only，新在上）。
- `src/main/java/AgeOfEmpires/` — 游戏本体（CFR 反编译 + 人工修正）。`c.java`
  （~7000 行）是状态机+渲染+逻辑全集；`AoeMidlet` 入口（有 `game()` 访问器）。
- `src/main/java/com/ulysseo/mad/` — In-Fusio J2ME 框架（定时器主循环、菜单资源读取）。
- `src/main/java/javax/microedition/` — 手写 Swing 适配层（lcdui/rms/media/midlet）。
- `src/main/java/aoe/` — `Main`（启动器）+ `DevHarness`（headless 测试驱动）+
  `SaveState`（快照存档）+ `DevFields`（反射字段 dump）。
- `decompiled/` — CFR 原始输出（历史参考）；`decompiled-vf/` — **Vineflower 基准
  oracle**（控制流可疑时先查这里，用法见「不变量与坑」）。
- `tools/` — **regress.sh**（黄金回归网；`--update` 重录基线）、**renamer/**（AST
  改名器 + wave1~6.tsv）、**replaycheck.sh**（确定性回放自检）、**vineflower-1.10.1.jar**、
  **cfr.jar**、aoectl（FIFO CLI）、shot.sh/winid.swift。

## 重构工作流（2026-08-31 起生效）

原则：**零风险操作只有纯注释和编译器兜底的机械改名**；搬代码用回归网压到实际等效。
每步一个 commit，commit 前必跑 `tools/regress.sh`（三连绿后才算过）。

1. 回归网先行（已完成）：regress.sh + 静态指纹 + 存读 roundtrip fields diff。
2. 注释分节 banner：c.java 按状态机/输入/菜单引擎/各画面渲染/任务装载分节（零风险）。
3. 改名：只改"完全搞懂"的符号，走 renamer + waveN.tsv（wave1~6 已完成批次见
   各 tsv 头注释）；每个符号在 docs/symbols.md 登记语义。
4. 搬代码（待做）：桌面鼠标增强 + dev 工具搬出 c.java 到 hook 类。原版反编译逻辑不动。
5. 不做：单字母 sed、逻辑/结构改写、渲染器按状态拆类（等改名全部完成后再议）、
   逻辑渲染解耦（见 docs/research/deep-dive-1）。

## 调试工具箱

### 运行模式与常用属性（`-D` 前缀，gradle run 已带 debug）

| 属性 | 作用 |
|---|---|
| `aoe.debug` | 全部调试日志的总闸（build.gradle run 默认开） |
| `aoe.dev=tutorial:N\|campaign:N\|random:N` | 跳菜单直进关卡（守护线程走真实菜单流） |
| `aoe.headless=1` | 无窗口跑（配 DevHarness 做测试） |
| `aoe.tickms=N` | 主循环周期（dev 旋钮，=整个游戏变速，非"顺滑"） |
| `aoe.mute=1` | 静音 |
| `aoe.devMouse=<fifo>` | FIFO 驱动输入注入（见下） |
| `aoe.saveDir=<dir>` | 快照存档目录（**调试会话一律重定向到 /tmp**，防覆盖用户档） |
| `aoe.devBoot=<存档>` | 从快照直启（仅存档带 nav spec 时可用） |
| `aoe.autoDismiss=1` | 教程对话框自动推进（调试会话常用；**回放时禁用**） |
| `aoe.autoCheckpoint=1`（默认开） | 每任务稳定后自动写一次 auto.aoesave（setupMissionEnv 复位标志；弹窗/全图往返不再重触发） |
| `aoe.devHud=1` | 画面顶部状态 HUD（截图自描述） |
| `aoe.harnessQuiet=1` | DevHarness 进任务后不再自动按键（**回放时必开**，墙钟输入会破坏确定性） |
| `aoe.dumpFrames=<png>` | 每 ~5s 导帧 |
| `aoe.width/height/scale` | 逻辑分辨率/窗口倍数（回归/golden 必须走默认 240x320） |

### FIFO 指令（`-Daoe.devMouse=<fifo>`；逻辑坐标 240x320）

`mkfifo /tmp/aoe-mouse` 后 `echo "指令" > /tmp/aoe-mouse`；CLI 包装 `tools/aoectl`。

- 输入：`move x y` / `press` / `release` / `click x y` / `rclick` / `drag x1 y1 x2 y2` /
  `ctile <tx> <ty>`（tile 直点：实时相机换算，无 click 的镜头缓动漂移；240x320 标定）/
  `key <键码>`（单发；合成松开要求"再完整过一帧"防 paint 中段吞键——2026-09-01 修）/
  `tapk <键码> <期望aA> [重试]`（带验证重注）
  ⚠️ 坐标 = 当前屏幕逻辑分辨率（默认 240x320；设 aoe.width=480 则为 480x535）。
  x≥宽-14/y≥高-14 的贴边区域会触发边缘滚动。
- 观测：`state`（写 `<fifo>.json` 快照：aA/am/ar/光标/相机/选中/迷雾/单位表）/
  `until <aA> [秒]` / `probe x y`（只拾取）/ `dump <png>`（同步导帧）/ `fields <txt>`
- 编排：`script <文件>`（批量，支持 `sleep 毫秒`、#注释）
- 考古：`strtbl <表> <条目|all>`（打印 data.res 字符串表条目）/
  `dlg <z> <v>`（强制开结算/简报对话框复现渲染问题）
- 存读：`save [路径]` / `load <路径>`（`[save]` 行带捕获时刻 `ar=`，回放锚）
- **确定性回放**：`replaytrace <trace文件> [baseTick]`（到点注入，行格式
  `t <相对tick> key <键码>` / `t <相对tick> move <x> <y>`）；`stopat <tick>`
  （冻在精确 tick，对拍取态前必用）
- `exit`

### 日志行速查（全部 aoe.debug 门控，`[paint]` 例外=无条件）

`[dbg]` 25-tick 心跳（ar/am/aA…）· `[void_a]` 按键 · `[input]` 带 tick 戳的输入
trace（回放锚）· `[mouse]/[mouseA]/[pick]/[band]` 鼠标链路 · `[trace] g->` 状态切换 ·
`[dlg-parse]` 对话框正文解析（z/v/串长，查空白弹窗）·
`[view]` 地图进出/对话框/世界重建 · `[paint]` 帧内异常（**无条件**打印，画面冻住
先看它）· `[save]/[load]` 快照（save 带 `ar=`）· `[proj]` 投射物扫描护栏触发 ·
**`[watchdog]` Timer 线程疑似卡死 + 完整栈** · `[fMenu]/[k]/[menuGate]` 菜单流 ·
`[dev]/[devBoot]/[devMouse]/[probe]` dev 链路。

排查口诀：用户报"卡死/蓝屏/按键无效" → 先要 run-*.log：`[paint]`（异常循环）→
`[watchdog]`（死循环栈）→ `[view]`（视图去哪了）→ `[trace] g->`（状态机）。

### 黄金回归网

`tools/regress.sh`：headless 教程关 + 固定场景，静态指纹 + 存读 roundtrip
fields diff。任何重构 commit 前必跑；`--update` 重录基线（改名/刻意变更后）。
**回归/golden 必须走默认 240x320**（镜头坐标依赖 screenW 派生偏移）。噪声清单
regress-noise.txt。

### 确定性回放

模拟 = 纯"任务 + 输入序列 + tick"决定（RNG 分流 + 快照 v2 钉 tick/RNG，见
docs/game-mechanics.md「确定性模型」）。自检：`tools/replaycheck.sh [campaign|tutorial] [N]`
——合成 trace 双跑对拍，state JSON 与 `[input]` 轨迹必须逐字节一致；动过输入路径/
定时器/线程后必跑。实机卡死复现工作流（日志 `[input]` 行 + auto.aoesave → trace →
`load` + `replaytrace` → `stopat` 冻结验尸）见 WORKLOG.md 2026-09-01「确定性回放落地」。

### 存读档

F5/F9 快存快读（quick.aoesave）；自动 checkpoint（auto.aoesave）；FIFO save/load；
`-Daoe.devBoot` 直启（需 nav spec）。快照 v2 含任务身份/设置镜像/菜单树与脚本指针/
地图/全部槽位/相机光标选中/**tickCount/RNG 静态**。读档校验任务身份三元组。
已知边界：窗口会话档（无 nav spec）不支持 boot 直启；任务内菜单多存档槽未做。

### 全图视图速查

任务中按 `0` 进全图（蓝底 64×64 缩略；黑=未探索迷雾，绿=已探索，白框=视口），
再按 `0`/Enter/Space/X/F1/Esc 退出。预热保护（进关 10 秒内）按键不响应属正常。

## 路线图

### 整体目标状态

把 2005 年的 J2ME《帝国时代 II》移植成"现代桌面小游戏"：键鼠双全、可调速度、
有 dev 基建（headless 测试、直进关卡、确定性回放）、画质可升级、最终 jpackage
成 .app。功能层面原版内容全保留（战役 7 关全解锁、随机地图 3 档全解锁）。

### 现代化候选清单（按价值/成本分档，与用户讨论过）

**第一梯队（小而实用）**
- 任务内菜单安全化（2026-09-01 玩家实测定性：菜单树=原版资源 res#117 原样，
  "无 Resume、高亮项即退出任务、-6=前进" 全是原版行为；改动属重制需拍板）：
  ① 菜单首项加 Resume；② 退出任务前加确认对话框；③ 菜单取消键与对话框统一
  （原版菜单 -6=前进、-7=取消，与直觉相反，实测一次误触弃掉 ar≈77000 成型局）。
- ~~宽屏视野~~ → 已完成（2026-09-01；遗留：菜单/剧情 240 宽素材黑边，暂不适配）。
- ~~战局快照~~ → 已完成（v2）；剩余：窗口档 boot 直启 + 任务内菜单多槽位 UI。
- ~~确定性回放~~ → 已完成（2026-09-01，见 WORKLOG）；P2 飞行记录仪等见
  docs/research/deep-dive-3。
- 游戏速度切换：tick 周期已就位（`aoe.tickms`），做 1x/2x 运行时切换 UI。
  音乐换曲按 80ms/帧硬编码，变速会 skew，需一并修（见 docs/research/deep-dive-1）。
- 双击选同类：`h(0, 类型)` 原语现成，接鼠标双击即可。
- Ctrl+数字编组：需新增编组存储，召回 = 重放 0x8000 置位。
- MIDI 换 SoundFont 音源 + 音量设置。
- **常驻小地图**（方案已评审，用户拍板暂缓——规格与点击跳相机公式见
  WORKLOG.md 2026-08-31「常驻小地图」）。

**第二梯队**：素材超分（`Image.ASSET_SCALE` 管线现成）；鼠标 UX 进一步打磨。

**第三梯队（大工程）**：逻辑渲染解耦、AI 增强（各见 docs/research/）、
更大地图/剧情编辑器（属"重制"）。

### 深调研结论速览（全文在 docs/research/）

| 报告 | 结论 |
|---|---|
| deep-dive-1 逻辑渲染解耦（含 30/60fps） | 缓做。要做先"解耦渲染"（独立价值），再议插值；调 tickms 只是变速 |
| deep-dive-2 AI 增强 | 先调参；受击/索敌做成 `-Daoe.ai` 可选开关；行为树不建议 |
| deep-dive-3 调试基建 | P1 确定性回放已实施；P2 飞行记录仪/P3 不变式/P4 步进/P5 异常兜底未做 |

## 不变量与已知坑

- **反编译伪影防线**：src 派生自 CFR，全量重编译字节码对拍已证实 CFR 至少
  3 处静默语义伪影（G() 循环出口、B() 出口+极性、继承静态字段误解析——前两者
  已修复，见 WORKLOG 2026-09-01），Vineflower 零静默伪影：报告
  `docs/research/decompiler-fidelity.md`，工具 `tools/decomp-study/`。控制流可疑时：
  ① 查 `decompiled-vf/` 同方法的 Vineflower 渲染；② `javap -c` 对照原 jar
  （`~/Downloads/age_of_empires_ii_240x320-9174.jar`）仲裁；③ 对照 `decompiled/`。
- **确定性纪律**：模拟外随机一律走 `nextBgmRandomInt`（化妆品流），勿动
  `nextRandomInt`；新模拟代码必须保持 tick 决定（别引入墙钟/线程序依赖）；
  动过输入路径/定时器/线程后跑 `tools/replaycheck.sh`。
- **存档目录纪律**：调试/复现会话一律 `-Daoe.saveDir=/tmp/...`，绝不写用户真实
  存档（2026-09-01 覆档事故，见 WORKLOG）；绝不编辑正在运行的脚本文件；动用户
  文件先备份到独立路径。
- **ARM 弱内存模型**：跨线程游戏状态字段（EDT/dev 线程写、tick 线程读）必须
  `volatile`（dev 自动导航曾因无 volatile 长期失灵且现象是"确定性失败"）。
- **菜单消费按键的时序**：`f(Graphics)` 帧末清 `ax`，激活只在 item 类型字节==5
  分支判断——注入必须带重试/状态验证。
- **aA 状态语义（动态观察，未完全考证）**：4=菜单列表，6=任务主视图（对话框期
  也报 2），2=简报/对话框/装载，11=任务装载，9=简报相关，1=另一任务视图，0=构建中。
  状态机大量直写字段，静态分析不可靠，以动态观察为准。
- **线程模型**：主循环 java.util.Timer 线程（模拟+渲染都在其 serviceRepaints 里），
  键鼠在 Swing EDT，dev 导航/dev 鼠标在自起守护线程。游戏按单线程假设写。
- 鼠标事件是屏幕坐标命中：调试注入时游戏窗口不能被盖住。
- 菜单导航验证技巧：`[fMenu] aR=` 打印屏根节点偏移（Game Mode=116 难度=739
  选关=810/881），循环器旋转打 `[k] node=…`——比截图比对可靠。真实模式映射
  **gameMode 0=随机 16=教学 32=战役**。
- RecordStore 持久化在 `~/.aoe-desktop`：模式循环器位置跨会话漂移——快照记
  nav spec 重放，**不要**从 ac 推导模式。

## 工作日志

带时间序的会话日志在 **`WORKLOG.md`**（append-only，新在上）。规则：日志只追加
不修改；写日志时就地把长期有效的知识沉淀进本手册，不要指望别人来日志里翻。
