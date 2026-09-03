# aoe/ai — 玩家 AI

规则式玩家 AI 包。接口 `PlayerAi.tick(c game)` 每帧首在 paint/Timer 线程被调
（c.java 帧首 hook，`-Daoe.playerAi=<全限定类名>` 反射装载；装载失败/tick 异常
打 `[ai]` 日志并禁用，不影响游戏）。

## RuleBasedAi

目标：随机图（`-Daoe.dev=random:1` Easy）稳定取胜。每 8 tick 决策一次，顺序：

1. **态势扫描**：双方单位/建筑全量读（本里程碑允许读 hdr[1]）。我方村民按
   action/目标格归类（木/金），军事单位计数量与价值（Σ(攻+甲)，同引擎 hdr[55]
   算法但剔除村民）。
2. **军事**：敌方军事单位进入我方 TC 10 格内 → 全军回防（优先于一切）；
   否则兵力 ≥10 且（价值 ≥ 敌 1.3× 或 ≥ 110）→ 全军攻敌 TC，每 150 tick 重发
   群令（战斗结束单位原地待命，靠重发推进）；t12000 后僵局降门槛（≥6 兵即出门，
   打消耗战）——Easy 敌方 army<200 永不进攻，我们不主动就是无限平局。
3. **经济**：空闲/卡死村民派工（木金配额，封建后 2:2）；资源点用自扫的
   `findResource`（**不用引擎 findNearbyResource**——其结果缓存双方共享，
   会把敌方村民引到我们采的森林引发斗殴）；资源格要求有可走邻格（边缘死角树
   会让村民永远走不到位）；行军 300 tick 未移动 → 目标拉黑 3000 tick 重派。
4. **生产**：房屋里补村民（训练上限 hdr[75]，约 4 封顶）；兵营/射箭场/攻城工坊
   各维持 ≤2 在训（queueUnitTraining 只认首座同类型建筑，多建同型不提速）；
   产兵扣款发生在产出时（j() 里 canAfford 门槛），排多不亏。
5. **科技**：兵营建成即升封建（tryResearch）；封建后顺手研究 Double-Bit Axe
   （木 +5/趟）与 Gold Mining（金 +3/趟）。
6. **建筑**：一次一座（放下自动成型 ~32 tick，无需村民施工），顺序：房屋（人口
   紧张/开局/富余补满，上限 4 座）→ 伐木场（贴树林）→ 兵营 → 采矿场（贴金矿）
   → 封建后射箭场 → 磨坊（训练速度 +50%）→ 攻城工坊。资源点不存在时永久跳过
   对应矿场（否则链条卡死）。

确定性：只按 tickCount 节流，无墙钟、无随机数（也不许碰游戏 nextRandomInt）。

## 怎么跑

```bash
# 批量胜率（5 局 Easy，种子 1000 起，-b 开 BFS 寻路）
tools/ailoop.sh -n 5 -d 1 -a aoe.ai.RuleBasedAi -s 1000 -t 120 -k -b
# 窗口观战
./run.sh    # 或任意方式启动后加 -Daoe.playerAi=aoe.ai.RuleBasedAi（-Daoe.tickms=40 加速）
```

日志：`[ai]` 前缀（assign/build/research/queue/ATTACK/DEFEND/RETREAT/STUCK +
每 500 tick 态势摘要）；诊断配合 FIFO `aistate`（`tools/aoectl aistate`）。
