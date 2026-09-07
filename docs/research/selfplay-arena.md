# 自对弈竞技场（Self-play Arena）规格与协议

2026-09-07 立项（用户拍板）。目标：在对称、公平、不作弊、不开全图的自对弈
竞技场里持续迭代提升 AI 强度。本文档是竞技场规格 + 评估协议 + 联赛表的
唯一权威；每轮迭代的结果登记在末节联赛表。

## 第 0 条铁律（用户拍板，凌驾一切）

**默认游戏行为永远不变**。所有研究性改动都是可选分支（旋钮默认关/新类/
新 hook），不设旋钮时默认路径逐字节等于原版。敌 AI 默认永远是引擎
tickAi；enemyAi、fair 旋钮、arena 模式全部是显式挂载才生效的实验开关。

## 竞技场规格（`-Daoe.arena=1` 主开关捆绑以下全部）

| 项 | 规格 | 实现 |
|---|---|---|
| 参赛双方 | 两侧都是 RuleBasedAi（`-Daoe.playerAi` + `-Daoe.enemyAi`） | 已有 |
| 起始资源 | 双方拉平 200/100/100 | `fairStart`（arena 隐含） |
| 采集乘数 | 双方 ×1（无作弊） | `fairGather`（arena 隐含） |
| 资源滴 | 无 | tickAi 抑制 + 不开 enemyDrip |
| **迷雾** | **两侧都诚实**：side 0 用引擎迷雾层（mapTiles 0x8000）；side 1 用 AI 自维护的私有探索 bitmap（按自己单位/建筑每 tick 揭 3×3/半径 3/塔 6，镜像引擎语义） | side1 私有雾（第 0 轮已落地） |
| 调用顺序 | 帧首两侧 AI 的调用序按 tickCount&1 交替（消固定先手） | c.java onPaint（第 0 轮已落地） |
| 模拟段顺序 | tickUnits/tickAutoEngage/aimProjectiles/tickProjectiles/tickBuildings 的玩家处理序按 tickCount&1 交替（与帧首 AI 序同奇偶），中和"恒 P0 先行动"的占位竞争/先开火/先索敌 | `arenaSimAlt`（arena 隐含开，=0 回退；第 1 轮落地） |
| 出生对称 | 原版 Easy/Medium（randomMapDifficulty<2）只给 P0 白送侦察骑兵（type 5）；arena 下给 P1 镜像位补一只 | `arenaSymSpawn`（arena 隐含开，=0 回退；第 1 轮落地） |
| 遥测 | arena 下每 500t 打 `[arena] t=… s0[u/d/mv/r/exp] s1[…]` 对称指标行（side0 exp 排海：(tile&0xFFF)==768 从不置雾不计入） | c.java arenaTelemetry（第 1 轮落地） |
| side 偏差控制 | **镜像配对**：同种子 candidate 在 side 0 / side 1 各赛一局，合并计胜率 | ailoop `-m`（第 0 轮已落地） |
| 认输 | 竞技场模式下 side 1 的僵尸投降门开启（打 `[ai] concede side=1`；exitOnResult 批测路径再打 `[result] WIN`+exit=player 0 胜，非批测只打日志、终局仍走引擎） | 第 0 轮已落地 |
| 和局 | tickCount > 50000 判和（≈当前对局均值 2.7 倍），`[result] DRAW` + exit（仅 arena+exitOnResult 批测路径；隐藏覆盖旋钮 `aoe.arenaDrawTick`，默认 50000 勿改） | 第 0 轮已落地 |
| 旋钮按侧 | RuleBasedAi 全部自消费旋钮（aiK.*/exm.*/exp*/spNear/aiFog）支持 `aoe.<name>.p0/.p1` 按侧覆盖，不设则与 base 名/默认值逐字节一致 | 第 0 轮已落地 |

## 候选评估协议（防双生过拟合是命门）

1. **候选 = 旋钮门内的新机制**。旋钮必须支持**按侧覆盖**：`aoe.<name>` 为
   两侧默认，`aoe.<name>.p0`/`aoe.<name>.p1` 覆盖对应侧（镜像配对靠它——
   同一 JVM 内 candidate 与 champion 各居一侧）。
2. **镜像批**：ailoop 镜像模式，n=20（10 种子 × 双侧各一），同形状
   phase pin。candidate 胜 ≥14/20（二项 p≈0.08）才算赢。
3. **引擎锚**：candidate（两侧同开）对引擎敌 AI 跑 Easy/Medium/Expert 各
   一批 n=10，相对当前 champion 基线显著回退（>2 胜场差）则不采纳——
   防止"只克自己镜像"的策略。
4. **联赛制**：历代 champion 以旋钮组合存档于末节联赛表；candidate 还须
   对上一代 champion 配置不落下风（同协议镜像批）。
5. **退化监控**：报告平均 ticks；胜率涨但 ticks 翻倍的"拖死型"策略不算
   变强（个案裁决）。
6. 判死方向登记进联赛表「别再试」区，附证据。

## 迭代循环（每轮 ≈ 一个 sub-agent 夜）

上轮败局尸检（ailoop `-S` 快照 + `[ai]` 日志）→ 提炼一个假设 → 旋钮门内
实现 → 镜像批 + 引擎锚批 → 采纳（旋钮转正）/判死（登记）→ 联赛表 +
WORKLOG。

## 假设储备池（按预期收益排序，每轮取一个）

1. 探图速率（双侧诚实后的真正胜负手：侦察路径规划/双 scout 时机/开局
   村民探针窗口）
2. 波 1 后产能链韧性（45 夜测绘：胜者 9/10 ≤3.4k 开出射箭场）
3. 承伤结构微操（集火已收；风筝/锤砧/塔下换位）
4. 攻城时机学（敌塔环密度 vs 己方投石机数的出击判据）
5. 旋钮空间自动爬山（aiK/exm/exp 旋钮族=现成基因型，turbo 适应度便宜）

## 基建清单（第 0 轮，2026-09-07 落地）

- [x] RuleBasedAi 旋钮按侧覆盖机制（静态 final → 构造期按 side 解析实例字段）
- [x] side-1 私有迷雾 bitmap + fogHonest 对 side 1 解锁
- [x] ailoop 镜像模式（`-m`：同种子双侧各一，candidate 旋钮按侧分配）
- [x] 帧首 AI 调用序按 tick 交替（arena 门内）
- [x] side-1 arena concede + DRAW 规则
- [x] 公平性确认批：对称竞技场的 side 0/1 胜率基线（镜像合并后应≈50%）

第 1 轮（2026-09-07）追加：

- [x] **side 偏差定位与中和**（见联赛表 G0 行注）：两个偏差源实证——
  ① 出生不对称：原版 Easy/Medium 只给 P0 白送侦察骑兵（type 5），
  遥测 t=0 s0 u=3/mv=9 vs s1 u=2/mv=4；② 模拟段恒 P0 先行动（tickUnits
  占位先到先占/resolveAttack 先杀/tickAutoEngage 先索敌/投射物先落地）。
  中和 = `arenaSymSpawn` + `arenaSimAlt`（均 arena 隐含开）。效果：
  80% → 65%（仅 symSpawn）→ 55%/35%/50%（组合，三种子集各 n=10×2），
  合并 60 局 side0 = 28/60 = **46.7%**（落入 [40%,60%] 噪声带，残差无系统
  方向——seed 1010 同图同配置仅相位不同即翻转胜方）。
- [x] 假设②（渲染期雾泄漏）证伪于批测语境：ailoop 批跑 noRender=1 整跳
  renderWorld，该路径不存在（GUI 独有，且对双方 owner 都揭）。
- [x] `[arena]` 500t 遥测（对称指标分叉时点判据）。

## 联赛表

| 代 | 配置 | 镜像成绩 | 引擎锚 E/M/X | 日期 |
|---|---|---|---|---|
| G0 | 基线 RuleBasedAi（科技对称化后） | **第 0 轮（中和前）**：side0 得分 16.0/20 = 80%（10 种子×2，逐种子 7W-1L-2D；ticks 均 26649）。**第 1 轮中和后（arenaSimAlt+arenaSymSpawn 隐含开）**：side0 = 11.0/20=55%（种子 1000+）、7.0/20=35%（1010+）、10.0/20=50%（1020+），合并 60 局 **28/60=46.7%**——side 偏差已中和进噪声带；后续 candidate 镜像测量直接受益（残差 ±15pp/10种子 级为图运噪声） | 6/10、0/10、0/10（46/47 夜口径=对引擎敌 AI 的 side 0 胜率） | 2026-09-07 |

第 0 轮的 side 偏差假装备查已在第 1 轮闭环：① 坐实（模拟段恒 P0 先，
arenaSimAlt 中和）；② 证伪（批测 noRender 无渲染路径）；③ 真凶补获=
出生侦察兵不对称（arenaSymSpawn 中和）。镜像配对协议仍是 candidate 评估
的统计底座（n 有效=10 的图运噪声靠它摊薄）。

### 别再试（判死登记）

（空——历史判死见 README 各节，本表只收竞技场语境下的判死）
