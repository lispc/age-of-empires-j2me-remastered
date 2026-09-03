# aoe/ai — 玩家 AI

规则式玩家 AI 包。接口 `PlayerAi.tick(c game)` 每帧首在 paint/Timer 线程被调
（c.java 帧首 hook，`-Daoe.playerAi=<全限定类名>` 反射装载；装载失败/tick 异常
打 `[ai]` 日志并禁用，不影响游戏）。hook 在 onPaint 帧首，**不受 screenState
门控**——弹窗态（ss==2）下模拟停走但 AI 照样被调，AI 须自按 -7 关窗。

## RuleBasedAi

随机图（gameMode=0）Easy/Medium 通用。成绩（2026-09-03，ailoop `-b` 开 BFS）：
Medium 三区间 n=10 合计 **25/29 决胜（86.2%）**——seeds 1000+ 9胜1僵持 /
1010+ 9胜1负 / 2000+ 7胜3负；Easy n=5 4胜1僵持（僵持均为 seed 1004 退化图：
全图无可达金矿 + 敌 TC 被 5 塔围死）。

战略一句话：**塔防吸收敌方 all-in → 反击拆敌 TC**（拆 TC 即胜）。拼经济必输
（敌 3.07× 采集 vs 我方 4 村民硬上限 + pop 25），塔不占人口且完工计入 hdr[55]
军值（威慑敌 all-in 判定），是唯一的免费战力。

每 8 tick 决策一次，模块顺序：

1. **态势扫描**：双方单位/建筑全量读。我方村民按 action/目标格归工种（木/金/石），
   军事计数量与价值（Σ(攻+甲)）；敌方塔（完工 type≥12）按 hdr[45]+hdr[46] 折算
   进"防御军值"（总攻门槛用，v4 起——不算是送死）。
2. **弹窗自关**：ss==2 时 `onKeyPress(-7)` 后返回（z=62/z=70 信息弹窗冻结世界，
   headless 无人关窗 = 永久 STALL，seed 1010 实测冻 27 万 tick）。
3. **军事**：
   - 敌兵进我方 TC 10 格（DEFEND_D2=100）→ 近战群令压上；弓兵/投石机停在
     入侵者朝我 TC 3 格处开火（远程不送近战圈）；敌 t8 在 20 格内时远程优先点杀
     （投石机远程拆塔，近战无解——agent-operations §5.1/r29）。
   - 残血（HP<100）回撤 TC 背敌侧站桩回血（0.5 HP/tick），HP>220 归队——
     消耗战里保老兵 = 省替换金，对 3.07× 产量的敌人是刚需（v11 关键胜率来源）。
   - 反击触发（需家门清净）：敌主力被歼（峰值 ≥5 且现存量 ≤1/3）/ 碾压
     （军值 ≥130 且 ≥ 敌防御军值 4/3）/ 僵持兜底（t25000 后）/ **贴脸图闪击**
     （双方 TC ≤14 格：消耗战必输，兵力不劣直接上）。进攻中小股（≤3）反扑不撤，
     大股撤；1500 tick 无进展判定停滞撤回重整。
   - 互瞪僵局诱敌：t15000 后久无接触且敌蹲家 ≥10 兵 → 派最便宜的兵直扑敌 TC
     触发其防御模式反扑，喂给我方塔阵（§5.3 调虎离山；驻防群令会召回诱饵，
     靠每决策重投续上）。
4. **村民**：闲置（action=0 且已到目标格；采集→交存→返矿全自动，§10 定论）
   与卡死（行军 300 tick 未动 → 目标拉黑 3000 tick）重派；配额 boot 期 2木1金，
   封建后 1木2金（塔 <5 座时压 1 人采石），开战后（敌亮过 6+ 兵）1木3金。
   在岗配额漂移由"每次决策最多换 1 人"的主动再平衡修正。敌兵贴脸 7 格 →
   村民撤到 TC 另一侧。资源点用自扫 findResource（引擎 findNearbyResource
   缓存双方共享，会把敌村民引到我方资源点互殴）。
5. **科技**：封建（兵营前置）→ Forging / Watch Tower / Gold Mining /
   Double-Bit Axe / Scale Mail（金 ≥25 才研）/ Stone Mining（塔未满 5 座时）；
   城堡时代（磨坊+铁匠 ≥2 且资源富余）→ Iron Casting / Chain Mail / Guard Tower。
   科技必须尽早就位——GoldMining/WatchTower 直接决定首波窗口的金收入与塔生存
   （v5 推迟到 8k 的实验证明是灾难）。
6. **建筑**（一次一座，放下自动成型 ~32 tick）：房屋1 → 伐木场 → 兵营 →
   走廊塔1 → 采矿场 → 走廊塔2 → 铁匠铺 → 射箭场 → 走廊塔3-5 → 房屋2-4 →
   磨坊 → 马厩 → 攻城工坊。塔位 = 我方 TC → 敌 TC 走廊上 4/6/9/12/16 格阶梯
   （r26 验证的最优汇率防术；近距图钳到走廊 60% 内，别蹭进敌警戒圈）。
   **交战中（敌兵 12 格内）只补塔**——seed 1019 兵临城下连放 4 座铁匠铺被秒拆
   白烧 100 木 80 石的教训。
7. **生产**：兵营（剑士/民兵）/射箭场（弓兵）各维持 ≤2 在训，铁匠铺 ≤1 台
   投石机（金 ≥40 才排；**t8 产自铁匠铺不是攻城工坊**——queueUnitTraining
   case 8 → 建筑 6），马厩 ≤2（封建侦察/城堡骑兵自动换形），攻城工坊 ≤1 冲车
   （金 ≥60）。产兵扣款在产出时，排多不亏但占人口名额。

确定性：只按 tickCount 节流，无墙钟、无随机数（也不许碰游戏 nextRandomInt）。

## 怎么跑

```bash
# 批量胜率（10 局 Medium，种子 1000 起，-b 开 BFS 寻路，-k 留每局日志）
tools/ailoop.sh -n 10 -d 2 -a aoe.ai.RuleBasedAi -s 1000 -t 240 -k -b
# Easy 复测
tools/ailoop.sh -n 5 -d 1 -a aoe.ai.RuleBasedAi -s 1000 -t 180 -k -b
# 窗口观战
./run.sh    # 或任意方式启动后加 -Daoe.playerAi=aoe.ai.RuleBasedAi（-Daoe.tickms=40 加速）
```

日志：`[ai]` 前缀（assign/rebalance/build/research/DEFEND/ATTACK（带触发原因
CRUSHED/OVERWHELM/DESPERATE/CLOSERUSH）/RETREAT/ABORTED/STALLED/BAIT/STUCK +
每 500 tick 态势摘要含敌峰值与塔数）；诊断配合 FIFO `aistate`（`tools/aoectl aistate`）。

## 已知边界

- seed 1004 型退化图（无可达金矿 + 敌 TC 被塔围死）：僵持，可接受。
- 高方差种子（敌贴脸 + 3.5k 早 rush）：2004/2007 仍会败——金收入上限是硬约束。
- 同一种子跨跑结果会翻（菜单导航墙钟相位）：只信批量胜率。
