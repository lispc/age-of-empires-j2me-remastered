# AGENTS.md — 本仓库代理工作守则

Age of Empires II (2005 J2ME) → macOS Swing 桌面移植。游戏本体是反编译代码
(`AgeOfEmpires/c.java` ~7000 行,CFR 血统),适配层手写。**改任何逻辑前先读
`DEVELOPMENT.md`(手册)与 `docs/game-mechanics.md`。**

## 构建与验证

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
./gradlew classes          # 编译
tools/regress.sh           # 黄金回归网——任何 commit 前必跑,三连绿才算过
tools/regress.sh --update  # 仅在刻意变更行为/改名后重录基线,并在 commit message 说明
tools/replaycheck.sh       # 确定性回放自检——动过输入路径/定时器/线程后必跑
```

- 回归/golden **必须走默认 240x320**(镜头坐标依赖 screenW;宽视野只经 run.sh 的
  `AOE_WIDTH` 生效)。
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
