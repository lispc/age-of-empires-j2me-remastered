# 开发文档 — Age of Empires II (J2ME) macOS 桌面移植

> 2026-08-31（三次更新）。项目说明见 `README.md`，游戏机制见 `docs/game-mechanics.md`，
> 本文档是开发总纲：现状 / 目标 / 进行中工作 / 调试方法 / 重构工作流 / 坑。

## 一句话现状

反编译移植完成度很高：渲染管线（v3 设备分辨率持久帧缓冲）、键盘映射（含 WASD/
QEZC/X 别名）、鼠标支持（悬停高亮/单击选中/拖动框选/右键群移）均已完成。
**悬停 UX 改造（WIP-1）已于 2026-08-31 全项验证通过**（含一个 UX 发现：点击
选中认"占位格"不认精灵，见下）；**Dev 模式自动导航（WIP-2）已修复并稳定**
（headless 冒烟 5/5 进关）。本轮机器重建过环境（JDK/wrapper/授权坑见下节）。

## 环境与构建

- JDK：`/opt/homebrew/opt/openjdk@17`（brew，2026-08-31 重装）。**仓库已带
  Gradle wrapper**（9.7.1，2026-08-31 提交），构建只依赖 JDK：`./gradlew classes`；
  `run.sh` 优先用 wrapper，退回系统 gradle。`gradle.properties` 固定了
  `org.gradle.java.home`。
- 直接跑 java（可传 JVM 参数，gradle run 不透传 -D）：
  `java -Daoe.debug=1 -cp "build/classes/java/main:build/resources/main" aoe.Main`
- **本机 TCC 授权坑（2026-08-31）**：宿主终端/ZCode CUA 助手**没有**辅助功能与
  屏幕录制授权 → `screencapture -l`、CGEvent 鼠标/键盘注入、computer-use 的
  截图通道全部不可用。调试注入改走游戏内通道：
  - 视觉验证：`-Daoe.dumpFrames=/tmp/frame.png`（每 ~5s）或 devMouse 的 `dump`
    指令（同步、即时）。
  - 鼠标/键盘注入：`-Daoe.devMouse=<fifo>`（见下"dev 鼠标驱动"）。
  - 若要恢复真实 CGEvent 注入：系统设置 → 隐私与安全性 → 辅助功能/屏幕录制
    给宿主 App 授权。
- 屏幕休眠会锁死键盘注入路径；长时间操作前注意。

## 代码地图

- `USER-GUIDE.md` — **面向玩家的使用手册**（键鼠操作/小地图/存读档），纯用户视角，
  开发者内容不要写进这里。
- `docs/game-mechanics.md` — **游戏机制知识地图**（主循环/按键模型/状态机/菜单
  模板树/移动寻路/存档布局/data.res 资源索引）。读 `c.java` 前先看。
- `docs/unit-stats.md` — **兵种/建筑/科技属性总表**（成本/攻防/移速/采集/伤害公式，
  res#121/122 逐字段考证）。
- `docs/symbols.md` — **符号字典**：混淆名→可读名的新旧对照 + 语义 + 考证分级；
  未改名符号（半懂/未考证）也在其中列明。
- `src/main/java/AgeOfEmpires/` — 游戏本体（CFR 反编译 + 人工修正清单见 README）。
  `c.java`（~7000 行）是状态机+渲染+逻辑全集；`AoeMidlet` 入口（有 `game()` 访问器）。
- `src/main/java/com/ulysseo/mad/` — In-Fusio J2ME 框架（定时器主循环、菜单资源读取）。
- `src/main/java/javax/microedition/` — 手写 Swing 适配层（lcdui/rms/media/midlet）。
- `src/main/java/aoe/` — `Main`（启动器）+ `DevHarness`（headless 测试驱动）。
- `decompiled/` — CFR 原始输出，仅参考；**以 src/ 为准**。
- `tools/` — 调试与重构辅助：
  - **regress.sh** — 黄金回归网（重构安全网）：headless 教程关 + 固定场景，
    静态指纹比对 + 存读 roundtrip fields diff。任何重构 commit 前必跑；
    `--update` 重录基线（改名/刻意变更行为后）。噪声清单 regress-noise.txt。
  - **renamer/** — AST 改名器（javac Tree API 符号解析，非文本替换）：
    `apply <srcRoot> <map.tsv> [--dry-run]` / `check`。改名映射 wave1.tsv，
    后续 wave 只补 map 行（前提：语义已考证）。
  - aoectl — FIFO 控制器 CLI（见"dev 鼠标驱动"节）。
  - shot.sh / winid.swift / cfr.jar。

## 重构工作流（2026-08-31 起生效）

原则：**零风险操作只有纯注释和编译器兜底的机械改名**；搬代码用回归网压到实际等效。
每步一个 commit，commit 前必跑 `tools/regress.sh`（三连绿后才算过）。

1. 回归网先行（已完成）：regress.sh + 静态指纹 + 存读 roundtrip fields diff。
2. 注释分节 banner：c.java 按状态机/输入/菜单引擎/各画面渲染/任务装载分节（零风险）。
3. 改名：只改"完全搞懂"的符号，走 renamer + wave.tsv；每个符号在 docs/symbols.md
   登记语义。已完成的批次见 wave1.tsv（2026-08-31，~70 符号，1947 处编辑）。
4. 搬代码（待做）：把桌面鼠标增强 + dev 工具搬出 c.java 到 hook 类（低风险，
   只放宽我们自己加的字段可见性）。原版反编译逻辑不动。
5. 不做：单字母 sed（用 renamer 可做但需先考证语义）、逻辑/结构改写、
   渲染器按状态拆类（等改名全部完成后再议）、逻辑渲染解耦（见深调研一）。

## 当前进行中的工作（接手请先读）

### wave4 改名：屏幕度量/相机/全图视图/音乐一族（2026-09-01 深夜，紧接宽视野落地）

宽视野考证中读懂的 12 个符号经 AST renamer 改名（对照表见 docs/symbols.md wave4 节）：
setupScreenMetrics/onScreenSizeChanged/viewTileRows/bottomBarY/camTargetX·Y/mediaRequestId/
startGameCanvas/loadKeymap/playNextBgm/requestMedia/stampThumbTile。
要点与坑：

- `j(int,int)` 是框架基类 `com.ulysseo.mad.a` 的抽象方法 override——**owner 侧改名会
  断 override 契约**，需把 `mad.a` 声明与 `mad.b` 调用点一并改（renamer 的 owner 限定
  不跨类继承，此步手工补的，已记入 wave4.tsv）。
- renamer 会按词边界同步注释里的旧名引用：单字母旧名（m/o/c/b/j）在注释里大量撞名
  （无参 m() 的 nfo 刷写、字段 m、类名 b/c），本次误伤 3 处已人工改回。**单字母符号
  改名后必须逐条过 diff 看注释**。
- 改名器对单字母旧名的 check 模式噪声极大（撞类名/重载/局部变量），验证以
  编译 + regress 为准。
- **regress 再立一功**：顺手"修"state JSON explored 统计位（0x8000→0x4000）被 golden
  当场拦下（got 4096 vs golden 298）——0x8000 才是真迷雾位，其置位走矩形填充助手的
  参数间接传入，字面 grep 找不到 setter；已回退并把正确语义写进现场注释与
  symbols.md 半懂节。教训与"autoDismiss 惨案"同类：**动机制前先找齐间接写入口**。
- 结果：REGRESS PASS，golden 无需重录（纯改名零行为变化）。

### 宽屏视野落地：run.sh 默认 720x320（2026-09-01 深夜，用户拍板"720；Java 默认不改；run.sh 改"）

决策：逻辑宽 720（约 3 倍原版视野）；**Java 层默认仍 240x320**（回归/测试走默认路径，
golden 基线稳定）；`run.sh` 默认传宽屏；菜单/剧情 240 宽素材的黑边**暂不适配**（用户明确）。

实现（三层，各一处改动）：
- `lcdui/Screen.java`：`WIDTH/HEIGHT` 改读 `-Daoe.width/-Daoe.height`（默认 240/320）。
  帧缓冲、窗口、鼠标映射、Graphics 变换全部从它派生，自动跟随。
- `build.gradle` run 块：环境变量 `AOE_WIDTH/AOE_HEIGHT` → jvmArgs（沿用 AOE_QUIET 的
  env 模式）。
- `run.sh`：`export AOE_WIDTH="${AOE_WIDTH:-720}"`，外部可覆盖
  （`AOE_WIDTH=240 ./run.sh` 回原版视野）。

为什么低风险（考证结论，2026-09-01 讨论）：原游戏按多分辨率 J2ME 机型设计——
`m(int,int)`（c.java:3628）从 screenW/H 派生可视格数 `viewTileCols`/`aB`、镜头居中
偏移 `ad`/`J`、底栏边界；renderWorld 按 viewTileCols 循环；菜单中心锚定
（screenW>>1 + 数据偏移）；全图视图（缩略图/视野框/镜头）全部屏幕相对；
顶栏已有 `screenW-240` 居中。唯一硬编码 240 的 UI 是 dev toast（已改 screenW/2 居中）。

**测试顺带抓到并修掉一颗既有暗雷**（与宽度无关，240 下也在）：`renderMapView` 单位
盖章循环（c.java:2651）反编译原样把 `(n3 &= 0xFF)` 写在 `||` 短路第二支上——tx 相等
时被跳过，`mapTiles[n2 + (n3<<6)]` 用全值 `tx<<8|ty` 做索引必越界（实测
Index 641895）。表现：全图视图内每隔单位数帧崩一次 paint（掉帧/`beginStateTransition`
被跳过 → 偶发"按 0 退出迟滞"）。修复：改为用点处掩码（数值与原已掩码路径完全一致）。
720 headless 实测：世界视图/全图视图 framebuffer dump 正常（1440x640）、进出全图
aA=1→6 正常、零 paint 异常；默认参数 dump 仍 480x640（240@2x）；REGRESS PASS。

### 修复：换关后小地图"全蓝退不出"（2026-09-01 晚，用户第二次实测报告）

现象：第一关进出小地图正常，**换一关后**按 0 全蓝且按 0 回不去（有随机性）。
根因（现场日志抓到）：**任务脚本/结算对话框会从小地图视图（aA=1）里弹出**
（脚本的 update 链在所有主视图态都跑），`n()` 构建对话框时把
`overlayPrevState` 如实记成 1；按 F1 关闭时 `pendingScreenState = overlayPrevState`
又把用户弹回全蓝小地图。教程/战役的对话框是**连锁**的（弹→关→回蓝屏→再弹），
玩家侧表现即"全蓝、按 0 无效、退不出去"，且取决于弹出时机——有随机性。
第一关若为随机图（无脚本无对话框）则永远正常。此前 headless 复现全失败，
因为测试都开着 `-Daoe.autoDismiss=1`（每 4 帧自动推框）把这条链路全跳过了。
修复：`n()` 记录返回目标时 1→8（关闭对话框回世界视图，小地图可再按 0 进）。
A/B 验证（驱动直调 `startMissionBriefing` 在 aA=1 时拉框）：修复前
overlayPrevState=1→关闭后 aA=1（弹回蓝屏）；修复后 =8→aA=6（世界视图）✓。
regress PASS。

### 现场日志（2026-09-01 起 run.sh 默认留档，排查此类问题的标准流程）

- `run.sh` 每次运行的完整输出 tee 到
  `~/Library/Application Support/AoeJ2ME/logs/run-<时间戳>.log`（保留最近 10 份），
  启动时打印日志路径；`build.gradle` 给 run 任务默认加 `-Daoe.debug=1`
  （要安静窗口：`AOE_QUIET=1 ./run.sh`）。
- 游戏内打点：`[paint]`（**帧内异常无条件打印 + 状态上下文**——静默吞异常的表现
  就是画面冻在最后一帧/全蓝、按键无响应，以前只在 aoe.debug 下打）；
  `[view] enter/exit map`（小地图进出，含光标/相机/clipTop/盖章行）；
  `[view] dialog open z=… overlayPrevState=…`（对话框打开及其返回目标）；
  `[view] world rebuild clipTop=0`（HUD 重建回路）。加上原有的
  `[dbg]/[trace]/[void_a]/[fMenu]`，一次复现即可从日志读出完整状态时间线。
- 排查口诀：用户报"卡死/蓝屏/按键无效"→ 先要 run-*.log，看最后一段的
  `[paint]`（有没有异常循环）、`[view]`（视图去哪了）、`[trace] g->`（状态机）。

### 修复：按 0 全图视图"回不去"之一（2026-09-01，OS 连发键）

现象：任务里按 `0` 进全图视图后，再按 `0` 大概率出不来（画面一直蓝）。
根因不在游戏逻辑（静态推演 + FIFO 注入都证明"再按一次能退"），而在**端口层
按键映射漏了 OS 自动重复语义**：macOS 对按住的键连发 keyDown，`CanvasPanel`
逐个当新 `keyPressed` 投递；游戏里"全图视图退出→回世界视图"要 2~3 帧
（1→8→6），期间任何一个重复 keyDown（动作码仍是 1）都会被世界视图按键处理
当成"再开全图"，形成 **退出即重进的死循环**。headless 复现：驱动线程直调
`onKeyPress(48)` 并每 50ms 重发（模拟连发），观察 aA 1→8→6→1 循环。
修复（`CanvasPanel`）：记录按住键码，未抬起期间的同码 keyDown 改投
`keyRepeated`（MIDP 语义，游戏未覆写=忽略）；失焦清空按下表。
FIFO `key` 直调 `game.onKeyPress` 不经此路径，dev 工作流不受影响。

### 本轮小结（2026-08-31 下午）

1. **dev 效率工具**落地：autoDismiss / devHud / FIFO 状态 JSON+until/probe/script/
   fields / aoectl / DevFields diff——"调试会话先开 autoDismiss、机制测试用
   random:1（无教程脚本）、快照直启代替菜单爬行"是新工作流。
   ⚠️ 2026-09-01 修正：`random:N` 的导航此前只右切一次循环器，实际进的是**战役**
   （菜单 Game Mode 顺序 Tutorial→Campaign→Random Map）；已补第二次右切，
   现在 `random:N` 才是真正的随机图（gameMode=0 遭遇战，难度 Easy）。
   菜单导航验证技巧：`[fMenu] aR=` 打印屏根节点偏移（Game Mode=116 难度=739
   教学选关=810 战役选关=881），循环器旋转会打 `[k] node=<n> mode=0`——都比
   截图比对可靠。另外 modes 的真实映射是 **gameMode 0=随机 16=教学 32=战役**
   （此前文档写反了），详见 `docs/game-mechanics.md`。
2. **快照存档 v1** 落地并验收（F5/F9 + 自动 checkpoint + devBoot 直启）。
3. **常驻小地图**方案已评审定稿，用户拍板暂缓——见"常驻小地图"节。
4. 玩家手册 `USER-GUIDE.md` 新建（键鼠/小地图迷雾/存读档）。

### WIP-1 悬停 UX 改造 —— ✅ 2026-08-31 全项验证通过

验证方式：窗口模式 + `-Daoe.devMouse` FIFO 注入 + 帧缓冲 dump（本机无 TCC 授权，
无法注入真实 CGEvent；OS→Swing 的 `dispatchMouse` 换算层未变、此前已验证）。
四项结论：
1. **悬停**：黄菱形高亮跟随鼠标，镜头/红框不动 ✓（`[pick]` 锚点与画面吻合）。
2. **点击直达**：红框跳到点击格 + 镜头缓动居中 + 选中（aE=512、单位 0x8000 置位）✓。
   ⚠️ **UX 发现**：选中认**占位格**（单位脚下）不认精灵——点身体会选中身后空格
   （身体精灵向上叠画 ~11px）。改进方向：像素拾取时对点击点附近格做单位占位优先。
3. **边缘滚动**：贴边（14px）持续半速平移（~6 格/秒对角），到 0/63 正确钳制，
   鼠标移开/离窗（kind=4）停止 ✓。
4. **框选 + 群移**：拖框 rect 内 3 兵全选（绿色血条标识）+ 右键群移齐走 ✓。

### WIP-2 Dev 模式 —— ✅ 自动导航已修复（headless 冒烟 5/5）

- **L2 headless** ✓：`-Daoe.headless=1` 不建窗口照常 tick；配 `-Daoe.mute=1`、
  `-Daoe.tickms=40` 加速。
- **DevHarness** ✓：进关 → 自动推完任务内教程对话框（aA==2 时打 F1，直到主视图
  稳定 ~2.4s）→ dump PNG → 打印状态 → 退出。
- **L1 自动导航修复要点**（`devPress`/`devSig`/`devStartMission`）：
  - 每次注入用**菜单指针快照**（aA/ao/Z/aR + 循环器位置字节）验证"被消费"，
    未变化按帧重注——单次注入会被"帧末 ax=0 vs 激活判断"的竞态无痕吞掉。
  - 注入前后等**指针静止**（~3 帧不变）；确认窗口 12 帧——面板切换是延迟生效
    （v=H 由状态机后续帧消费），窗口太短会把"生效中"误判为"被吞"而重注，
    造成一次按键两次消费、流程跳屏。
  - 菜单链长度随模式不同（有的屏高亮项脚本是空操作），用**自适应 FIRE 循环**
    直到 aA≠4，替代定长按键序列；选关循环只在"高亮项是 op=3 脚本的循环器"上切。

### dev 鼠标驱动（-Daoe.devMouse=<fifo>，2026-08-31 新增）

`mkfifo /tmp/aoe-mouse` 后启动游戏，`echo "指令" > /tmp/aoe-mouse` 注入。指令
（**逻辑坐标 240x320**，别超界——x≥226/y≥306 会触发边缘滚动）：
`move x y` / `press` / `release` / `click` / `rclick` / `drag x1 y1 x2 y2` /
`key <J2ME键码>`（单发：注入即挂延迟释放）/ `dump <png>`（同步导帧，最方便的视觉
验证）/ `state`（完整状态：aA/am/光标/相机/选中/迷雾格数/单位表，并同步写
`<fifo>.json` 供工具读回）/ `until <aA> [秒]`（进程内阻塞等状态到位）/
`probe x y`（只拾取不下令，屏幕→格子标定）/ `script <文件>`（批量执行，支持
`sleep 毫秒` 与 #注释）/ `fields <txt>`（反射字段全量 dump，存档 diff 验证）/
`save <路径>` / `load <路径>` / `exit`。配套调试打印：
`[mouseA]`/`[pick]`/`[band]`/`[rcmd]`/`[k]`。

注意：`click`/`rclick` 会各自刷新拾取点并等下一帧拾取生效后执行（rclick 挂起 ≤3 帧，
落点在虚空/地图外则丢弃）；早期版本 rclick 直接用上一帧悬停的 lastTile，冷注入会
指向 -1 或旧格（2026-08-31 修复）。

**tools/aoectl**：上述指令的 CLI 包装（`AOE_FIFO` 环境变量选通道）。
`aoectl state|wait-mission|tap|rclick|key|probe|dump|save|load|script|fields|exit`。
典型流程一条命令搞定：`aoectl wait-mission && aoectl tap 116 140 && aoectl dump`。

### dev 效率工具（2026-08-31 二轮新增）

- `-Daoe.autoDismiss=1`：任务内教程对话框自动推进（进过一次主视图后，aA==2 的
  弹窗每 4 帧补一个左软键）。**调试会话默认开**——对话框吞输入曾是最大干扰源。
- `-Daoe.devHud=1`：画面顶部两行状态叠加（aA/am/ar/菜单指针/光标/相机/pick/选中），
  dump 出的截图自描述；另有存/读档 toast 提示（居中 2s）。
- `-Daoe.autoCheckpoint`（默认开）：进关 ~2s 后自动写 `auto.aoesave`。
- `aoe.DevFields`：反射 dump 全部 AgeOfEmpires.* 字段（数组=长度+CRC，小数组带
  全元素）。**存档完整性验收法**：存→立刻读→前后两次 fields dump diff——
  剩余差异应只有帧计数/动画计数/toast/音频对象等良性噪声（2026-08-31 验收通过）。

### 快照存档 v1（aoe.SaveState，2026-08-31 落地）

- **方案**：定向二进制快照 + "同任务重载+覆写"，不做世界重建。捕获清单：
  任务身份（ac/aC/aF + .nfo 任务号——教程的 aF 恒 0，必须带 nfo[31,32] 才能区分
  教程各关）、设置镜像 nfoData（旧名 var_byte_arr_f）、解锁进度 aj/aG（wave2 已改名
tutorialProgress/campaignProgress）、菜单树 menuTree（旧名 var_byte_arr_i）+
  引擎指针（menuNode/menuNodeCount/menuHighlight/menuScreenId/pendingPanelSwitch/ap）
  + 任务数据镜像 var_byte_arr_a（脚本解释器会就地写执行标记）、地图格 mapTiles、每玩家单位/建筑四个槽位数组、资源/计数
  int 数组、相机/光标/选中、dev 导航 spec（头部，供直启重放）。
- **应用点**：读档请求在 EDT 帧首（p() 的 devFrameHousekeeping）串行化消费，
  避免与 tick/渲染竞态产生撕裂快照；apply 后 `af=0`+`k()` 强制全量重画。
- **入口**：F5/F9（Canvas.desktopCommand 链：Swing→mad.b→mad.a→c，快捷存读
  quick.aoesave）；FIFO save/load；自动 checkpoint；`-Daoe.devBoot=<存档>` 直启
  （按快照里的 nav spec 重放菜单导航，稳定后覆写——实测 14s 含导航，状态与存档
  会话完全一致）。
- **守卫**：读档校验任务身份三元组（ac/aF/nfo号），不匹配拒绝并提示。
- **已知边界（phase 2 候选）**：① 窗口会话（无 nav spec）的档暂不支持 boot 直启
  ——直接触发 aA=11 装载器需要把 aF/装载链语义考古清楚；② 任务内菜单 UI 的
  多存档槽未做（菜单树是 RAM 数据可打补丁，但字符串表/布局要动 l()）。
- **坑**：RecordStore 持久化在 `~/.aoe-desktop`，模式循环器位置跨会话漂移——
  快照里记 nav spec 重放，**不要**试图从 ac 推导模式（运行时语义与反编译注释
  对不上，实测教程会话 ac=16、随机地图 ac=32）。

### 小地图 / 全图视图（2026-08-31 查证，用户问过"蓝屏"）

任务中按 **`0`**（动作码 1，键位表 data.res #129）进全图视图，再按 `0`（或
Enter/Space/X/F1/Esc）退出；方向键/WASD/数字斜向键在全图上平移。**这不是 bug**：
`e(Graphics)` 蓝底 0x3476FF 上画 64×64 地图缩略，`b(Graphics,x,y)`（c.java:2395）
对 0x8000 未探索格画黑色（原版战争迷雾语义，与 PC 帝 2 小地图一致）；绿色=已探索，
白框=当前视口，点=单位/建筑。教程开局几乎全黑，探索后逐格变绿。预热保护 `af>=10`
（c.java:2297）内按键不响应属正常。

### 常驻小地图 —— 方案已评审（2026-08-31），暂不实施

用户需求：小地图常驻任务画面（原版只有 `0` 键全屏切换视图，方案保留不动）。
已定方案：
- **渲染**：新建 240×120 可变图，配色复用 `b(Graphics,x,y)` 的语义（未探索黑/
  地形色/单位点），缩放 blit 到画面角落。地形层缓存 + 节流（~0.5s 或探索变化时
  重画，4096 格全量重画较贵）；视口框与单位点每帧叠加。
- **规格**：右下角 ~96×48 逻辑像素，半透明深色底 + 1px 边框；`-Daoe.minimap=0`
  可关；`M` 键开关（桌面别名，不占原版键位）。
- **点击跳相机**：逆投影公式（由 `b()` 正投影反解）：screen_x=2(tx−ty)+120、
  screen_y=tx+ty ⇒ tx=((x−120)/2+y)/2、ty=(y−(x−120)/2)/2。
- 顺带绕开原版坑：全图视图缩略图只在首次进入时盖章（`af` 计数不复位），之后
  探索推进不更新；常驻版定期重画天然没有此问题。
- 挂载点：`a(Graphics)` 末尾（框选/悬停高亮之后）。

### 调试打印清单（全部 `-Daoe.debug=1` 门控，验证完可清）

`[mouseA]/[pick]/[mtick]/[corner1]/[corner2]/[band]`（鼠标链路，c.java/Canvas.java）、
`[rcmd]`（右键命令分支与 aE/Y 状态）、`[fMenu]/[menuGate]/[fHead]`（菜单渲染与
激活门，f(Graphics)）、`[k]`（菜单项激活：节点/类型/脚本操作码，菜单流调试利器）、
`[void_a]`（键映射）、`[trace]`（boolean_c/g/boolean_g 状态转换）、`[dev]`（自动
导航）、`[devMouse]`（FIFO 驱动）、`[save]/[load]`（快照存读）、`[probe]`（拾取
标定）、`[devBoot]`（快照直启）。另有 `mad/e.java` 的 25 帧状态打印（ar/am/aA/aH）。

## 整体目标状态

把 2005 年的 J2ME《帝国时代 II》移植成一个"现代桌面小游戏"：键鼠双全、
可调速度、有 dev 基建（headless 测试、直进关卡）、画质可升级、最终 jpackage 成 .app。
功能层面原版内容全保留（战役 7 关已全解锁、随机地图 3 档全解锁）。

## 现代化候选清单（按价值/成本分档，与用户讨论过）

**第一梯队（小而实用，建议先做）**
- ~~宽屏视野~~ → **已完成**（2026-09-01，run.sh 默认 720x320，见"当前进行中的工作"首节；
  遗留：菜单/剧情 240 宽素材黑边，用户拍板暂不适配）。
- 游戏速度切换：tick 周期参数已就位（`aoe.tickms`），做个 1x/2x 的运行时切换 UI。
  注意音乐换曲按 80ms/帧硬编码折算（`c.java:332`），变速会 skew，需一并修
  （论证见「深调研一」2026-09-01 补充）。
- 双击选同类：`h(0, 类型)` 原语现成（教程"全选村民"就是它），接鼠标双击即可。
- Ctrl+数字编组：需要新增编组存储（存选中单位槽位表），召回 = 重放 0x8000 置位。
- MIDI 换 SoundFont 音源 + 音量设置（`javax.sound.midi` 原生支持加载 soundfont）。
- ~~战局快照~~ → **已完成**（快照存档 v1，见上）；剩余 phase 2：窗口档 boot 直启
  （直接触发 aA=11 装载链）+ 任务内菜单多槽位 UI。
- **常驻小地图**（方案已定，见上节——用户 2026-08-31 拍板"方案记录、暂缓实施"）。

**第二梯队（中等成本）**
- 素材超分：`Image.ASSET_SCALE` 管线现成，离线超分后同尺寸替换即生效。
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

### 2026-09-01 补充：30/60fps 现代化论证（用户问"值得吗"，结论：记录、先不做）

用户问："现在默认 12.5 帧，对现代体验太低了，容易/值得改成 30 或 60 帧吗？"
逐层考证（证据同步进 `docs/game-mechanics.md`「主循环与时序」）：

**现状证据链**：`mad/e.java run()` 每跳只调 `w()`（仅退出检查，c.java:218）+
repaint/serviceRepaints；**整个模拟跑在 `onPaint` 里**（c.java:1300 `++tickCount`，
1397-1426 是世界模拟块）；镜头/光标/鼠标逻辑嵌在渲染函数 `a(Graphics)` 开头
（c.java:2932-2942）；音乐换曲按 `曲长ms/80` 折帧（c.java:332）。即
**帧 = 逻辑 tick = 渲染，三者同频 12.5Hz**，且渲染路径不纯。

**四条路**：

- **A. 只调 `aoe.tickms`**：整个游戏等比快进（40ms=双倍速、33ms≈2.4 倍速）。
  是"变速"不是"顺滑"——采集/训练/AI/剧本节奏全变，音乐换曲 skew。dev 旋钮保留，不作答案。
- **B. 解耦：逻辑保持 12.5Hz，渲染提到 60fps**。把模拟块挪回 Timer 回调按 80ms 跑、
  paint 只画；镜头/光标挪出 `a(Graphics)` 进逻辑步；跑马灯/闪烁/对话按键门等纯 UI
  计时改按真实毫秒。菜单/转场/光标立刻顺滑，性能毫无压力（240×320 光栅）。
  **收益边界要说清**：无插值时游戏内移动/投射物仍是 12.5Hz 跳变（渲染只是把每个
  逻辑位置多画几遍），战场观感提升有限。工作量中等，回归网可护。
- **C. B + 渲染插值**：单位位置影子缓冲 + alpha 插值、动画帧按真实时间推进——
  战场画面真正顺滑的关键，也是工作量大头（出生/死亡/传送/转换/读档的位置突变边界）。
  做完后"12.5 原味"与"60fps 现代"可做成双模式开关（渲染频率与逻辑频率本就是两个旋钮）。
- **D. 逻辑提到 30Hz + 所有时间常数对半**（保持真实速度）：改变移动粒度 → 碰撞/卡位
  微观行为全变，回归验证成本远超收益。不推荐。

**结论**（用户 2026-09-01 拍板"先不做"）：若将来做，先 B——它独立有价值
（菜单/UI 层现代化），且是本节"逻辑/渲染分离"的地基，不管做不做 C 都要做；
B 完成后上分支试吃战场观感，再决定是否投入 C。注意输入延迟 80ms 是 12.5Hz
逻辑采样的固有属性，B/C 都不解决。

## 深调研二：AI 增强

**现状（已核实）**：
- 敌人（玩家 1）的"AI"是**生产脚本**：`tryTrainAiUnit(兵种)`（旧名 boolean_b(int)，
  wave2 已改名）——定时器（`aiTrainTimer`/`aiTrainInterval` 计数）到期时按
  `tickCount % 10` 伪随机选兵种，检查人口上限
  （`[1][3]`，硬上限 26）与兵种配额（`[1][57+i]/[66+i]/[75+i]`），再按**玩家当前
  时代**（`[0][0]`）切换克制兵种（n==2→3、5→6 之类），最后 `queueUnitTraining(1, 兵种)` 扣资源入队。
- 单位行为 = 移动步进器（贪心直线 + 7 邻格局部避障，见 docs/game-mechanics.md"移动与寻路"）
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
  `javap -c`（原 jar：`~/Downloads/age_of_empires_ii_240x320-9174.jar`）。
- **开发调试的黄金路径**：`-Daoe.headless=1 -Daoe.dev=tutorial:1 -Daoe.tickms=40
  aoe.DevHarness out.png` 一条命令拿到"进关后的画面 PNG + 状态打印"，不用碰 GUI
  （2026-08-31 起稳定 5/5）。窗口模式下的鼠标/键盘交互验证用
  `-Daoe.devMouse=<fifo>`（见上），自动导航不稳时回退窗口模式 + osascript。
