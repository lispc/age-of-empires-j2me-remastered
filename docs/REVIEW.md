# REVIEW — 战役/AI 攻关白皮书（知识地图,一页版）

> 2026-09-06 AI 探索期收官时的总索引。**只做链接和压缩,不复述**——细节以
> 源文档为准,本页负责"什么问题查哪里"。当前时态,过期即删。

## 战绩与资产

- **宏线（FIFO 驱动录制）七关全清**,全部位精确可回放+全亮视频:
  m0 8576 / m1 5172 / m2 6037 / m3 2874 / m4 19112 / m5 2936 / m6 30067。
  三关重录:m1 快 76×、m2 快 10.6×、m3 快 31×。旧配方录制留档
  `recordings/campaign/m1-chop|m2-longhaul|m3-chew/`。战绩详见
  `recordings/README.md`。
- **代码版 AI（player-ai→已合并）**：#0/#1/#2/#3/#5/#6 全胜(2.3k-11.7k tick),
  #4 科技关 9/20(唯一开放伤口,波抽签结构)。
- **开放 resting points**(非待办,重启攻关再动):Easy 原版阈值 RuleBasedAi
  0/5(方向在 future-tasks B.4)、Expert 3/20、#4 剩余负相位、units=0 判负
  归属考证。

## 知识地图(什么问题查哪里)

| 问题 | 查 |
|---|---|
| 引擎机制真相(战斗/经济/索敌/回放) | `docs/game-mechanics.md` |
| 操作定律与事故教训(驱动怎么写才不死) | `docs/agent-operations.md` §11 |
| 逐关档案(布阵/配方演进/交棒) | `tools/campaign/NOTES.md` |
| 关卡脚本解码(胜负条件/开局写值) | `docs/research/campaign-mission-scripts.md` |
| 当前可信数值口径 | `docs/fact-card.md`(口径卡) |
| 改名登记/字段语义 | `docs/symbols.md`、`docs/unit-stats.md` |
| 做了什么/事故史 | `WORKLOG.md`(append-only) |
| 挂起与 resting points | `docs/future-tasks.md` |

## Top 10 定律(全称与证据见 §11/game-mechanics)

1. **确定性三件套**=快照+tick 锚定 trace+紧竞态发令;任何录制必须双路验证
   (replay-verify 位精确+mktrace 流 diff)。
2. **retask 风暴禁律**:逐拍重发=永不接战;批量同帧+沉淀守卫(同目标跳过)+
   状态变了才发。直攻失败先怀疑指令风暴再怀疑战术几何。
3. **帖位三半径定律**:开火 TRIG(弓16/猫20)<aggro 25(边界=≤)<安全 36;
   攻城帖位按 25 排他。
4. **reprisal=投射物专属**,位移机制(走向着弹源,覆盖 retask);近战击杀零反击。
5. **帖扫=攻城关主输出**:idle 远程只自动索敌建筑(0.383/t),对单位仅 reprisal。
6. **分兵种目标选择**:弓=最近持械,猫=最近单位(冲车照杀);probe 结论跨兵种
   推广必须重测。
7. **交存入账定律**:入账格=己方建筑格;type0 放格即 hdr[9]、type1 完工写
   hdr[10]/[11];前置仓=经济关 10× 提速主源(先仓后采)。
8. **死亡判定唯一真源=[combat]**:aistate 有死亡幽灵;尸检先画死点散布。
9. **磨血无效**(回血 0.5/t+20/40t):只数击杀,战术=制造安全击杀点。
10. **口径带证据等级+适用域**:跨兵种/跨图推广必须重测(四次翻案的教训)。

## 标准工作流

- **新关开工序**:mapdump 真值(修复版)→离线 claims/路线推演→探针局标定→
  dry 端到端→boot。运动微操层面 dry 无信息量,第 3 轮起转实弹 probe。
- **重录/新录验收**:三件套(base+trace+**session.log=play.log 正主**)+
  `replay-verify.py <dir> <base> '<expect>' [TOL]` + mktrace 流 diff;
  视频用 `campaign-video.sh`(紧竞态,campaign-replay 只作旁证)。
- **批测**:`ailoop/camloop`+devPhase 相位 pin+同相位成对 A/B;排除项入库。
- **接力制**:60-90 分钟止损;BUGS-*.md 交棒必须含行号级配方+操作经验+
  反思;主会话先消化再开下一轮。

## 工具清单

| 用途 | 工具 |
|---|---|
| 随机图/战役批测 | `ailoop.sh`(-a 必带)/`camloop.sh` |
| 录制回放验证 | `campaign/replay-verify.py`(紧竞态,含 TOL) |
| 回放旁证+长局视频 | `campaign-replay.sh` |
| 短局/位精确视频 | `campaign-video.sh` |
| trace 提取 | `mktrace.py`(只读指令不进 trace) |
| 驱动公共件 | `campaign/drvkit.py`(DRY 闸/沉淀守卫/[combat] 差分/幽灵过滤) |
| 引擎调试 | FIFO 指令族(`DEVELOPMENT.md` 调试工具箱:mapdump/aistate/…) |
| 存档解析 | `aoesave.py`、`resdec.py` |
| 效率画像 | `round-stats.py`(主会话侧跑) |
