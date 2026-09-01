# WORKLOG — 工作日志(Age of Empires II J2ME→macOS 移植)

> **本档是 append-only 日志**:只追加,不修改旧条目,不复读。新条目加在最上面
> (`## 工作日志` 头之下),一条 = 一个工作会话,写清:做了什么/证据/commit/事故。
> 接手项目请读 `DEVELOPMENT.md`(手册);本档只在复盘、考古、"这个决定当时怎么来的"
> 时查。长期有效的知识在写日志时就地沉淀进手册,不要指望别人来日志里翻。

## 日志（新在上；只追加，不改旧条目）

### BUG-002 修正版结论 + Easy 平衡修正;BUG-003 checkpoint 节流(2026-09-01 深夜,玩家会话第二轮)

- 玩家代理首局(误入教学关那局改为正常随机图后续局)报**致命**:Easy AI ~30s
  rush、2 分钟推平,并给出双根因假说(模板 byte54=0 首轮扫描中毒 + 防御目标语义反)。
  **复核结论**:① [54] 的写入点只有 setup 武装(0xFFFFFF)与 tickAi 运行时两处,
  不存在模板拷贝,首轮中毒假说不成立;② 防御目标=玩家基地[0][8] 经原版字节码
  仲裁(case 1 @684: 先验 [1][8]!=-1 再取 [0][8]+抖动)是**原版设计**。真正的
  rush 驱动是 **Easy 原版参数**:aiAttackThreshold=50 + aiTrainInterval=20
  (1.6s/兵),军队价值 ~2 分钟到线即 75% 兵力 all-in。
- **平衡修正(有意偏离,已标注)**:Easy aiAttackThreshold 50→200,给新手发育
  窗口;中/高难维持原值。game-mechanics AI 节已注明原值与理由。
- **BUG-003 修复**:auto checkpoint 加 600 tick 节流(devLastCheckpointTick)——
  原实现每次离开主视图(开地图/弹窗)回来都重存一次。
- 附注(未改):Under Attack 弹窗为模态不暂停模拟且会拒存(aA=2 时 devSaveTo 拒绝),
  属原版语义,先观察。
- 分工:玩家代理已通知重启第三局(正式随机图,阈值修正后)。

### 反编译器全量对拍(Phase 2):CFR 再挖出 B() 伪影并修复;random:N 导航修复(2026-09-01 深夜,玩家子代理会话)

- **Phase 2 字节码对拍**(子代理执行,报告 `docs/research/decompiler-fidelity.md`,
  工具 `tools/decomp-study/`):反编译→重编译(--release 8 + shim)→ javap 归一化
  四级对拍(226 方法)。**CFR 0.152 确认 3 处静默语义伪影**:已知 G() 循环出口、
  **B() 出口+极性反转(新)**、继承静态字段误解析(10 点,已在 src 修复);
  **Vineflower 零静默伪影**(问题全是响亮的编译阻断)。G() 锚点按预期判定,流水线可信。
- **B() 修复**:原版字节码 `bl 置位即 break`(每玩家每次调用只处理一个单位,179:
  iinc 1,1 进下一玩家),CFR 渲染成 `if (!bl) continue;` 两分支都续循环 → repo 行为
  变成"每 tick 处理全部闲置单位"。已按原版语义改 `if (bl) break;`。回归 PASS
  (golden 不含该路径的指纹差)。
- **BUG-001(玩家代理报)修复**:`-Daoe.dev=random:1` 落进教学关——开屏闪屏页
  (menuNode=187/254/333,定时脚本自动翻页)同样满足 aA=4"稳定",Play 被闪屏吞掉,
  后续 Game Mode 右切全落空。修复:devNavToMission 按 Play 前先等真主菜单
  (menuNode==0,20s 超时)。
- 分工纪律:玩家子代理继续游玩找 bug(BUGS.md),本会话负责修复+回归+推送。
### 文档拆分：DEVELOPMENT.md → 手册 + 本日志 + docs/research/；wave6 改名（2026-09-01 深夜，用户提议"日志归档与活跃手册分开"）
### 文档拆分：DEVELOPMENT.md → 手册 + 本日志 + docs/research/；wave6 改名（2026-09-01 深夜，用户提议"日志归档与活跃手册分开"）

- **拆分**（用户拍板的"日志/手册"两层 + 研究档）：
  - `WORKLOG.md` = 本档，append-only,原「当前进行中的工作」全部带日期条目迁入（新在上）。
  - `DEVELOPMENT.md` 重写为手册（现状/环境/代码地图/重构工作流/**调试工具箱**/路线图/
    不变量与坑），只写现在时态;新增三条长期规则——反编译伪影防线（VF oracle + javap）、
    确定性纪律（化妆品 RNG/tick 确定/replaycheck）、存档目录纪律。
  - 深调研一/二/三迁 `docs/research/deep-dive-{1,2,3}-*.md`,手册「路线图」留结论速览表。
  - 交叉引用全仓修复（README/USER-GUIDE/game-mechanics/symbols）。
- **wave6 改名**（AST renamer,16+2 处）:var_boolean_k→randomMap（任务种子字节为零
  =随机图）、m→bgmFramesLeft（BGM 换曲倒计时——半懂清单里"菜单/世界双写入点双职责
  未证"经核实为同一职责两处赋值,移出半懂）。**wave4 教训再现**:单字母 m 的注释同步
  把无参方法 m()（.nfo RecordStore 读写）的注释误伤成 bgmFramesLeft(),已人工修复。
- **新注释**:randomMap/bgmFramesLeft 字段声明处;AgeOfEmpires/d（地图生成器）类头
  （相位驱动、影响扩散公式、种子来源;9/20 参数含义为推断,已标注）。
- 反编译器 Phase 2（重编译字节码对拍）由子代理并行进行,报告另入库
  docs/research/decompiler-fidelity.md。

### 反编译器交叉对比：采纳 Vineflower 1.10.1 为基准 oracle（2026-09-01 晚，用户追问"CFR 还有别的问题吗"）

对原 jar 用三家引擎各反编译一遍并逐方法对比控制流关键字计数
（对比脚本思路 + 结果全记录于此）：

| 引擎 | 已知伪影点(aimProjectiles/G) | body 末尾 continue(c.java) | 备注 |
|---|---|---|---|
| CFR 0.152（现用） | **错**——丢失循环出口 | 9 处（1 处致命） | MIT，tools/cfr.jar |
| **Vineflower 1.10.1** | **对**——`if (--n<=0) break;` | **0 处** | GPL+Classpath例外，tools/vineflower-1.10.1.jar |
| Procyon 0.6.0 | 对（带标签 continue） | 24 处 | Apache-2.0，不著.unpack 噪声多 |

方法级控制流计数对比（CFR↔VF）：207 个方法中 70 个有差异，抽样仲裁
（地图生成 d.e 的 6 个 continue = for 计数循环内纯风格；脚本解释器大 switch 的
case 15↔38 = 表渲染方式）均为等价渲染。**结论**：CFR 在本 jar 上至少有一类
"静默丢失循环出口"伪影，Vineflower 渲染最忠实。规程：

- **对照 oracle**：`decompiled-vf/`（Vineflower 全套输出已入库，重生成命令见其
  README）。src 里控制流可疑时，先查本树同方法的 VF 渲染，再 `javap -c` 对照
  原 jar 仲裁（原 jar 路径见"注意事项与坑"）。
- src 工作树不迁移到 VF 输出（改名/注释投入太大）；仅当新考证大块逻辑时可用
  VF 输出做底稿对照。
- 在手的 all-clear：全部 11 处 body 末尾 continue 已逐一审计（见卡死条目），
  无已知未修伪影；残余风险主要在"计数对比法覆盖不到的等价改写"，交给
  看门狗 + replaycheck 兜底。

### 确定性回放落地（2026-09-01 晚，深调研三 P1 实施，用户拍板"做了吧"）

模拟轨迹自此**纯"任务 + 输入序列"决定**，事件回放成为可用调试工具：

- **RNG 分流**：`playNextBgm` 选曲改走独立 LCG `nextBgmRandomInt`（化妆品流），
  全局 `nextRandomInt` 只剩模拟消费（tickConstruction 掷骰）。这是回放的前提：
  换曲时机随墙钟漂移，共用一条流会让战斗掷骰随"听了几首曲子"发散。
  **golden 已重录**（--update）：模拟 RNG 消耗序列变化 → 建造掷骰结果变化。
  新增非模拟随机需求一律走化妆品流，勿动 nextRandomInt。
- **快照 v2**：SaveState 末尾新增 tickCount（旧 v1 档不再可读，版本校验拦截）。
  模拟含 tick 奇偶/取模逻辑（回血 &8、投射物旋转起点、BGM 倒计时），不钉 tick
  的读档走不出可复现轨迹。v1 档均为 dev 临时产物，无迁移价值。
- **输入 trace**：`[void_a]` 行追加 `ar=<tick>`；`mouseA` 新增
  `[input] ar=<tick> move x y`；onKeyPress 新增 `[input] ar=<tick> key <键码>`。
  trace 文件格式（`#` 注释）：
  `t <相对tick> key <键码>` / `t <相对tick> move <x> <y>`。
- **FIFO `replaytrace <file> [baseTick]`**：到点注入（等 tickCount≥目标后在
  dev-mouse 线程直呼 onKeyPress/mouseA，与真实输入同路径）。相对 tick 原点 =
  最近一次 load 落地的 tickCount（快照 v2 钉住）；未 load 则为指令执行瞬间；
  双跑对拍必须显式传同一 baseTick。
- **FIFO `stopat <tick>`**：确定性停表（等 tickCount≥目标后取消主循环 Timer）。
  对拍取态必须停表——回放结束后任由墙钟推进，ar 抖 ±几 tick，tile 级比较假失败。
- **tools/replaycheck.sh**：自检工具——合成固定 trace，A 跑（存基准档→回放→
  stopat 定点）与 B 跑（读基准档→回放→stopat 同一 tick）的最终 state JSON 与
  [input] 轨迹必须逐字节一致。这是"回放可用"本身的回归测试，工具链任何动
  时钟/线程的改动后应重跑。
- **实机卡死→回放复现工作流**（这套工具的最终目的）：
  1. 用户窗口会话的日志里已有带 tick 戳的输入流（`[input] ar=…`）+ 自动
     checkpoint（v2 快照，含 tickCount）。
  2. 提取现场：从卡死前最后一次 `[load]`/开局起，把 `[input]` 行转成 trace
     文件（`[input] ar=482 key 48` → `t 482 key 48`，减去基准 tick 偏移），
     存档取 auto.aoesave。
  3. 复现：`-Daoe.dev=campaign:N`（或 tutorial）headless/窗口 + FIFO
     `load auto.aoesave` → `replaytrace trace.txt <基准tick>` → 复现后
     `stopat <tick>` 冻结现场，`fields`/`state`/`dump` 随意验尸；
     配合 `[watchdog]` 栈直接定位死循环行。
  4. 单调收敛调试：trace 可以截短二分（回放前半段 + 1 个可疑事件），迭代定位
     最小触发输入。

**replaycheck 八轮排雷记录**（每一轮都是一类真实的非确定性源，后人加输入/定时器
相关功能前先读这页）：
1. 回放结束后由墙钟决定多走几 tick → 对拍 ar 抖 ±几 tick，tile 级比较假失败。
   → 加 FIFO `stopat`（到点取消 Timer，冻在精确 tick 再取态）。
2. 基准 tick 取"存档后的 state ar"错——`save` 只是指令排队，真正捕获在帧首，
   两者可差十几 tick → `[save]` 行补 `ar=`（捕获时刻），基准以它为准。
3. load→replaytrace 的启动延迟让首个事件"已被越过"→ trace 留 ≥300 tick lead-in
   （并用确定性 -6 前奏关掉 load 弹的对话框）。
4. **DevHarness 看门狗是墙钟驱动的输入源**：载入后弹对话框时它按 300ms 节奏乱按
   -6（stable 判定被打断导致它一直不退出）→ `-Daoe.harnessQuiet=1` 静音，
   对话框交给 trace 前奏。
5. `save` 被拒（对话框开着 aA=2）→ 脚本重试并在 aA=2 时补 -6——这些按键发生在
   基准存档之前，B 的 load 会整体丢弃，不破坏确定性。
6. 脚本重复行导致 replaytrace 跑两遍（第二轮 blast 到同一 tick）——"回放做了两次"
   表现为 input 数翻倍、全部挤在同一 ar。
7. 快照必须钉 tickCount + RNG 静态（见上）；A 流程也要"读自己的档再回放"，
   让 load 副作用（onShown 强制重建等）在 A/B 两侧同样发生。
8. 终局判定必须双通道：state JSON 一致 + [input] 轨迹一致（后者验证注入时刻，
   前者验证注入效果）。

### 卡死修复：aimProjectiles 待瞄准扫描死旋 + 通用卡死看门狗（2026-09-01，用户报告"玩着玩着卡死"）

**现象**（run-20260901-150628.log，战役任务 in-mission ~6800 tick 处）：`[dbg] ar=7098`
之后再无帧心跳，其后 ~240 行全是 EDT 收键/鼠标的日志，无任何异常栈。判定：模拟+渲染
整体跑在 mad/b 80ms Timer 线程的 `serviceRepaints()` 里（shim 特有，EDT 只做贴图），
EDT 活着 + tickCount 冻结 = **Timer 线程在 paint(模拟/渲染) 内死旋**；Timer 线程若因
异常死亡会往 stderr 打栈（日志无）→ 排除，锁定真死循环。

**定位**：静态审计全部 while 循环。`aimProjectiles()` 找"待瞄准"投射物记录（+1==1000）
的扫描循环，反编译体把"扫满一圈放弃"的退出分支弄丢了：`if (--n3 > 0) continue;` 只在
>0 时 continue，≤0 时落回 while 再判，而循环条件不含 n3 → 窗口内全是飞行中记录时
永久自旋。**任何一发投射物在飞行期间就满足条件**（发射后 aim 置为已瞄准，下一帧扫描
窗口即无 1000）——用户的卡死点即战役第一场接战。机制详情见 docs/game-mechanics.md
投射物节。修复：循环体内补回 `if (--n3 <= 0) break;`，aoe.debug 下留
`[proj] aim scan exhausted` 观测线；健康路径逐条等价，REGRESS PASS。
修复版 campaign:1 浸没 12525 in-mission tick 无冻结。

**字节码考证（追问"原版 jar 也有这个 bug 吗"）**：对原 jar
`~/Downloads/age_of_empires_ii_240x320-9174.jar` 的 `AgeOfEmpires/c.class` 方法 G 做
`javap -c`：`82: iinc 7,-1; 85: iload 7; ifgt 48`——**ifgt 的 false 分支（n3≤0）直接
落到循环出口 90，与"找到 1000"（66→goto 90）汇合**，即原版自带"n3 耗尽即退出"边界，
随后 `90-95` 就是 `if (n3<=0) continue`。**原版没有这个 bug；是 CFR 把 ifgt false
分支（退出循环）错渲染成 while 体末尾的裸 continue（= 回头再判条件）而静默丢失**，
pristine CFR 输出 `decompiled/AgeOfEmpires/c.java` 同样带病可证伪影来自 CFR 而非手工
改动。本修复即逐字恢复原版语义。**同类伪影审计**：按"while 体末尾语句是 continue"
扫出 11 处，逐一判定（3 处手写 dev 代码除外）：for 有界 / n5·n3·n4·i 严格推进 /
脚本解释器 n2 由自身赋值推进且有 AIOOBE 兜底——唯一真伪影即本处。审计方法（javap
对照原 jar 逐条读分支）对任何"反编译输出可疑"的控制流可复用；CFR 在简单形状上也会
静默丢出口，**这是比"反编译错"更危险的"反编译漏"**。

**复现未果的教训**：先按日志回放用户输入（691 事件 ar 锚定）+ 5 实例×25k tick 模糊
测试均未命中。原因是 **BGM 随机选曲消耗全局 RNG**（种子虽来自任务资源字节，但任务内
选曲次数/曲目随时长与墙钟漂移），7000 tick 混沌系统必然发散——对这类游戏做逐帧对齐
回放前先查 RNG 消耗源。

**新增通用安全网：paint 看门狗**（mad/b.startPaintWatchdog，随主循环 Timer 启动）：
每 2s 采样 Timer 线程，栈含游戏代码帧（正在执行任务）且栈顶 3 帧签名连续 6s 不变 →
`[watchdog] Timer 线程疑似卡死` + 完整栈打进日志。空闲态（栈=wait←mainLoop←run，无
游戏帧）不报。实弹验证：忙转线程 6s 即报、栈顶直指死循环行；健康运行 40s 零误报。
今后再遇卡死：`grep watchdog ~/Library/Application\ Support/AoeJ2ME/logs/run-*.log`。

**事故记录（存档丢失，我的操作失误）**：复现调试期间，v1/v2 脚本用了同一个
/tmp/freeze-repro.sh 文件且在 v2 shell 仍在运行时原地 Edit 该脚本——bash 按字节偏移
增量读脚本，旧 shell 从错位处读到新内容、额外拉起了一个 campaign:2 会话；多个 headless
会话的 DEV_AUTO_CHECKPOINT 把用户真实 auto.aoesave（卡死前 ~150 tick 的现场，
mission aF=104）覆盖掉，且 /tmp 备份也被二次覆盖。已确认无法恢复（无 TM/快照），
脏档已删除。**用户实际损失有限**：战役进度存 `~/.aoe-desktop/.nfo.rms`（RMS）未受影响，
丢的只是那次任务的中途存档；但这是我的失误，规程改为：① 调试会话一律显式
`-Daoe.saveDir=/tmp/...`（v3 起）；② 绝不编辑正在运行的脚本文件，新任务用新文件名；
③ 动用户目录下的任何文件前先备份到独立路径。

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
