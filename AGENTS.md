# AGENTS.md — 本仓库代理工作守则

Age of Empires II (2005 J2ME) → macOS Swing 桌面移植。游戏本体是反编译代码
(`AgeOfEmpires/c.java` ~7000 行,CFR 血统),适配层手写。**改任何逻辑前先读
`DEVELOPMENT.md`(手册)与 `docs/game-mechanics.md`。**

## 玩家代理(自动化试玩)附加守则

- **先通读 `docs/agent-operations.md`(操作手册)**——20+ 轮实测沉淀的操作
  菜谱/战术宏/坐标系统/陷阱清单,不要重新试错。游戏操作**首选服务端宏**
  (sel/goto/train/build/sitrep,手册 §6.1b),像素链路退为后备。
- 最终报告必须含**「操作经验」一节**:本轮新发现的操作技巧/坑(标注证据:
  实测/读码/推测),主会话验证后并入操作手册。
- 最终报告必须含**「反思与给主会话的建议」一节**:自查本轮效率瓶颈(哪些
  等待/反复试错/找不到信息浪费了时间)、工具缺口(还想要什么宏/指令)、
  文档缺陷(手册哪里没写清/写错/翻不到)、环境问题(fifo/存档/脚本路径)——
  主会话据此持续改进工具与流程。
- **接力制**:60-90 分钟或上下文过半即止损交棒——save checkpoint + BUGS.md
  写清"当前局面/下一步";禁止无脑重开一整局(手册 §1.4)。
- **主会话 mid-run 盯盘**:agent 运行期间主会话周期读 BUGS-rN.md 增量,
  发现征兆(经济崩/卡死)可提前 SendMessage 干预,不等验收尸。
- **轮后效率画像**:每轮结束跑 `tools/round-stats.py <transcript>` 附战报,
  持续发现时间黑洞。
- 其余纪律(日志/存档目录/上下文卫生)见各轮启动指令与手册 §8。

## 构建与验证

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
./gradlew classes          # 编译
tools/regress.sh           # 黄金回归网——任何 commit 前必跑,三连绿才算过
                            # (~13s/跑,2026-09-04 turbo 化提速 24x)
tools/regress.sh --update  # 仅在刻意变更行为/改名后重录基线,并在 commit message 说明
tools/replaycheck.sh       # 确定性回放自检——动过输入路径/定时器/线程后必跑
tools/bootcheck.sh         # devBoot 双跑对拍自检——动过存档装载/devBoot 时机后必跑
```

- **回归分工:谁改谁跑,不重复跑**。子代理做的批次由子代理跑完验证并在报告里
  给证据,主会话不再机械复跑(2026-09-04 用户拍板)。
- 回归/golden **必须走默认 240x320**(镜头坐标依赖 screenW;宽视野只经 run.sh 的
  `AOE_WIDTH` 生效)。
- **编译与跑批之间禁止管道**:`./gradlew classes | tail && tools/ailoop.sh` 的管道
  会把 gradle 退出码吞成 tail 的 0,编译挂了照样拿旧类文件空跑(2026-09-04 八批
  空转事故,见迭代笔记第五批)。编译独立成行,亲眼确认 BUILD SUCCESSFUL;
  Edit 的 old_string 必须落在完整行边界(以 `}` 结尾而文件里该 `}` 是
  `} else if` 前缀 = 子串匹配吃掉换行)。
- 改名会改变 fields-diff 的字段名:**同步更新 `tools/regress-noise.txt`**。

## 硬性纪律

1. **反编译代码先疑伪影**。CFR 已证实会静默丢循环出口(case: aimProjectiles 卡死)。
   控制流可疑时:① 查 `decompiled-vf/` 同方法的 Vineflower 渲染;② `javap -c`
   对照原 jar(`~/Downloads/age_of_empires_ii_240x320-9174.jar`);③ 别凭猜改。
2. **改名**:只改完全考证的符号;用 `tools/renamer`(AST,非文本替换)+ 新建
   `waveN.tsv`;单字母符号的注释同步有误伤史,**apply 后逐条过 diff 看注释**;
   语义登记进 `docs/symbols.md`。
3. **确定性纪律**:模拟随机只许 `nextRandomInt`;非模拟随机(BGM 选曲等)一律走
   `nextBgmRandomInt`(化妆品流);模拟代码禁止墙钟/线程序依赖;改 tickCount 语义或
   快照格式要 bump `SaveState.VERSION` 并在 WORKLOG 说明。
4. **存档目录纪律**:调试/复现会话一律 `-Daoe.saveDir=/tmp/...`,**绝不写用户真实
   存档**(`~/Library/Application Support/AoeJ2ME/saves`);绝不编辑正在运行的脚本
   文件;动用户文件前先备份到独立路径(2026-09-01 覆档事故)。
5. **跨线程字段必须 `volatile`**(EDT/dev 线程写、Timer 线程读;ARM 弱内存模型下
   失灵现象是"确定性失败",极难排查)。线程模型:模拟+渲染整体跑在 Timer 线程的
   `serviceRepaints` 里,键鼠在 Swing EDT。
6. **日志是排障契约**:`run.sh` 必须始终留日志;新增打点用现有格式
   (`[input]`/`[watchdog]`/`[paint]`/`[view]`/`[trace]`…);卡死先看 `[watchdog]`。

## 文档纪律

- `WORKLOG.md`:工作日志,**append-only**,新条目在最上,一条=一个会话(做了什么/
  证据/commit/事故);写日志时就地把长期知识沉淀进手册。
- `DEVELOPMENT.md`:手册,只写现在时态,不记历史;超 ~300 行就往外拆文件。
- 一次性考证报告放 `docs/research/`;机制知识放 `docs/game-mechanics.md`;
  改名登记放 `docs/symbols.md`。
- 一次工作一个 commit,信息写清 what/why;push 前回归绿。

## 常用调试入口

```bash
# headless 进关 + 截图(不碰 GUI)
java -Daoe.headless=1 -Daoe.dev=tutorial:1 -Daoe.tickms=40 -Daoe.debug=1 \
  -Daoe.saveDir=/tmp/saves -cp build/classes/java/main:build/resources/main \
  aoe.DevHarness /tmp/out.png 5
# FIFO 驱动(玩家/长会话用 aoe.Main,不自动退出)
mkfifo /tmp/aoe-fifo
java -Daoe.headless=1 -Daoe.dev=random:1 -Daoe.tickms=40 -Daoe.debug=1 \
  -Daoe.saveDir=/tmp/saves -Daoe.devMouse=/tmp/aoe-fifo \
  -cp build/classes/java/main:build/resources/main aoe.Main
echo 'state' > /tmp/aoe-fifo && cat /tmp/aoe-fifo.json   # aoectl 是它的 CLI 包装
```

FIFO 全指令与日志行速查见 `DEVELOPMENT.md`「调试工具箱」。卡死排查顺序:
`[watchdog]` → `[paint]` → `[view]` → `[trace] g->` → `[input]`。
