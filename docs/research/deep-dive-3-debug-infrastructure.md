# 深调研三:调试基础设施评估与路线图

> P1(确定性回放)已实施——落地记录见 WORKLOG.md 2026-09-01「确定性回放落地」;本文保留评估全文,P2~P5 未做。


**现状盘点**（这次卡死排查实际用到/用到的上限）：
- `aoe.debug=1`（build.gradle 常开）日志族：`[dbg]` 25-tick 心跳（tickCount/画面态）、
  `[void_a]`/`[mouse]` 输入流、`[trace]`/`[view]`/`[pick]` 状态机与拾取、
  `[save]`/`[load]`、`[proj]` 投射物观测线、`[watchdog]` 卡死栈（新增）。
- DevHarness + FIFO 指令：key/tapk/move/press/click/rclick/drag/state(+JSON 快照)/
  until/probe/script/save/load/dump/fields/exit。
- tools/regress.sh 金标准指纹；SaveState 全量快照；run.sh 日志留存 10 份。
- 本次排查证明：这套东西**足够定位"哪类线程在哪种循环里停了"**，但不足以**复现
  特定一局的轨迹**（见下）。

**核心缺口：事件回放(replay-by-events)当前不可用**。逐帧对齐回放 691 个事件 + 
5 实例×25k tick 模糊测试都没命中卡死点（最后靠静态审计找到）。根因三层：
1. **RNG 被选曲消耗**（致命层）：全局 `nextRandomInt`（LCG，种子来自任务资源字节）
   同时服务战斗掷骰与 BGM 选曲；选曲次数随墙钟漂移（曲长 ms/80 折帧的倒计时不受
   tickms 影响，但一首曲子几点开始取决于真实播放时刻链）→ 7000 tick 混沌系统必然
   发散。种子本身是确定性的，问题纯粹在"模拟外的消费源"。
2. **输入无 tick 戳**：`[void_a]`/`[mouse]` 日志只有出现顺序，回放只能按 [dbg] 心跳
   做 ±25 tick 插值。
3. **注入时刻不贴帧**：FIFO 指令在 dev-mouse 线程即时生效，与"用户在第 N tick 按下"
   不是同一语义。

**路线图**（P1 已于 2026-09-01 晚实施，见"确定性回放落地"条目；P2~P5 未做）：
- **P1 确定性回放三件套**（合计 ~2 小时，是"回放成为调试工具"的充分条件）✅：
  a) **RNG 分流**：BGM 选曲改用独立 LCG（或 java.util.Random 固定种子），全局
     nextRandomInt 从此只服务模拟——模拟变纯输入决定。代价：RNG 消耗序列变化 →
     regress golden 需重录一次（regress 有重录模式，一次性成本）。
  b) **输入 trace**：输入日志加 tick 戳（`[input] t=7098 key=-5`）；FIFO 加
     `replaytrace <file>` 按 tick 精确注入（内部 wait-until-tick 循环）。
  c) **确定性自检**：regress 扩展一个模式——同 trace 跑两遍，指纹必须逐字节一致，
     把"回放可用"本身变成被测试守护的性质。
- **P2 飞行记录仪**（~1 小时）：环形缓冲最近 256 tick 的关键状态摘要（资源/双方
  单位数/投射物池概要([48] 与各记录状态字)/RNG 态/脚本游标），O(1) 摊销零 I/O；
  `[watchdog]` 触发时自动随栈打印，FIFO `dumpstate` 手动可取。卡死现场从"只有栈"
  升级为"栈 + 前 256 tick 状态轨迹"。
- **P3 tick 不变式**（~1 小时，aoe.debug 下每帧）：projectileTable 紧凑性
  （[48]≤5、窗口内布局合法）、单位槽 HP≤255、格坐标 <64、占位表与实际格一致（抽样）。
  早把"数据悄悄坏了"变成"当场报哪条不变式"。
- **P4 暂停/步进**（~1 小时）：FIFO `pause` / `step N`（挂起/限步 Timer），配合
  P1b 在精确 tick 停下验尸。
- **P5 全局异常兜底**（~15 分钟）：Thread.setDefaultUncaughtExceptionHandler →
  栈 + 飞行记录仪 dump + 非零退出。EDT 异常目前会静默杀事件线程（表现为"全输入
  失灵但进程活着"），这条能把它变成一条可 grep 的日志。
- **明确不做**：逐帧全量日志（噪声/收益比差，P2 的环形缓冲是它的正确替代）；
  time-travel 调试器（工程量与收益不成比例）；跨版本回放（golden 已覆盖）。
- **一个有利事实**：模拟全程整数运算、无浮点、无 HashMap 迭代——只要 P1a 分流掉
  选曲消费源，跨机器逐 tick 一致是现实可期的，回放调试的地基比一般 Swing 程序好。

