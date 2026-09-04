# 战役攻略会话笔记（BFS 时代，2026-09-03 第 2 夜）

## 启动配方（录制标准，每关同款）
```
java -Daoe.headless=1 -Daoe.dev=campaign:N -Daoe.tickms=10 -Daoe.debug=1 \
  -Daoe.harnessQuiet=1 -Daoe.exitOnResult=1 -Daoe.saveDir=<work>/saves \
  -Daoe.rmsDir=<work>/rms -Daoe.mapSeed=8224 -Daoe.bfsPath=1 -Daoe.devMouse=<work>/fifo \
  -cp build/classes/java/main:build/resources/main aoe.Main > <work>/play.log 2>&1 &
```
- **rmsDir 必须钉到 work（红线，m3 事故）**：战役选关落点 = campaignProgress(RMS)
  + N − 1，不隔离会用 `~/.aoe-desktop` 的真实进度落错关，resultHold 结算写回还会
  污染用户真实进度。新鲜 rmsDir progress=0 → `campaign:N` 落 idx N−1。
- 进任务后 key -6 推简报 → `save <work>/base.aoesave`（此刻 ar=回放原点）
- 终局 `[result] WIN|LOSS ticks=N`；mktrace --until '[result]'
- 录制=新二进制(写宏队列化+携带 bfsPath 标志)，回放用同款 flags
- 永远 base.aoesave+trace.txt+session.log 三件套入 recordings/campaign/mN/

## 通用机制（已验证，勿重新考据）
- 采集钩子=多步行走踏入资源格；站资源格上 idle 时同格 retask 是 no-op；需 2+ 格 approach
- 趟数=(raw>>2)&0x1F，砍完一载就扣（与交存无关）；载量 hdr[50..52]=木5/金3/石3
- 雾下资源 0x83xx&3：0=浆果不可采集！1木 2金 3石；`aoesave.py <save> res` 全图对账
- 槽位死亡压缩：retask/slots 前核对 type；战役死单位不判负（m2 实测）
- 写宏已队列化(sel/goto/rally/retask/assign/train/build/gather)，帧首统一应用=回放位精确
- res 格式 [木,金,石]；读 state JSON: fifo.json {"aA","ar","res","units":[{p,tile,type}...]}

## 各关档案
（数据 res 号：m1=104 / m2=105 / m3=106 …；脚本=数据+7，解码器 resdec.py 换号即用。
地形=数据内嵌 rng 确定性生成（res 106 头两字节），与 mapSeed/RMS 无关，每 boot 同图，
可离线洪泛侦察——注意 BFS 下虚空可通行、资源格当墙。）
### m4 大学关（⏳ 5 boot 全 LOSS 止损交棒，r38；制胜方案已定，待第 2 轮）
- **胜负条件（res 114）**：WIN = 封建→**升城堡完成**（techFlags[14]==1，c.java:6200）
  → **放置 University/type4**（置回 0，c.java:7590）→ 计时 50t → WIN。res 127 初始
  [14]=0 ⇒ University 在城堡前被"已建成"锁死，**顺序不可颠倒**。Univ 25木/25石。
  **无判负块**（TC 毁/末单位死通用规则）。纯经济竞速关。
- **首个战役 AI 关**（aiEnabled=true，threshold=30）：敌每 tick 重算 stance，
  **一次性派 75% 兵力直扑我 TC±1**，波次随军力增长（wave1=2 军 ar~2400，wave2=4-5
  军 ar~4100，25k tick 时 24 军含投石机×3）。**n7==0 冻结漏洞**：我方 0 单位时
  AI stance 扫描整块跳过（c.java:8450）→ AI 永不再出兵（实证 24 军挂机 20k tick）。
- **反 raid 制胜方案（r38 推荐，待验证）**：wave1 换掉后民兵直扑敌村民集群
  (12-17,38) 断金木 → 敌 resources<30 后 all-in 永久短路 → 安心 boom 城堡+大学。
  次选：爆村民硬扛 3-4 波，赶 t8 前放 University。
- 操作要点：z=70 完工弹窗冻结世界（aA=2 → key -6 清）；build 雾格 FAIL 要候选表
  重试（TC 视野仅 ~2.5 格）；t2=2 pop ⇒ House 先行；**敌行军走廊=村民坟场**（±7
  格 aggro，早避险）；**rally 前沿非 TC**（波次幸存者 idle 在击杀点不啃楼）；硬化
  驱动=tools/campaign/m4drv.py（雾回退 build/同余类光标舞步/combat 去重）。
- 选关注记：移植修改已**解锁全部 7 关**（c.java:2968），选关器初始高亮=campaignProgress；
  公式 idx=progress+N−1 经读码复核成立（nav 恰按 N−1 次 -4；r38"progress+N"勘误
  系其战报笔误，自行撤回）。
### m3 拆楼关（✅ WIN ticks=89191，2026-09-04）
- **胜负条件（res 113，28B）**：WIN = `headers[1][4]==0`（敌建筑表清零=夷平全部
  5 栋敌楼）→ 计时 20t → WIN。**无判负块**——只剩通用规则：我方无建筑常态下
  最后 1 单位死亡瞬间=LOSS（红线：不可全灭）；单兵死亡合法（实战损 2×t3 照常）。
  战役通用"拆 TC 胜"在 campaign 关被关（gameMode 32 && missionIndex≠0）。
- 地形：出生点=3 个虚空口袋（**BFS 下虚空可通行**，源码注释"资源/虚空当墙"误导
  ——bfsWalkable 只墙 0x300/0x100）。敌 8 单位+5 楼。
- 实战：4 波点名清场（敌 idle 守军近圈 4-5 格会主动 aggro 迎击——"idle 不主动"
  只对远征成立）；清完守军闲置单位自动啃楼（近战²≤9），拆楼零专门波次。
- 回放提示：官方脚本已补 rmsDir；驱动判活用 state JSON units/sitrep，
  **slots 宏有死亡幽灵槽**（死后仍报 ~6min，type@坐标 w=0）。
### m1 护送关（✅ WIN ticks=392912，2026-09-04 录制；r35 尸检勘误版）
- **胜负条件（res 111 全量解码 84B；通式 res 111+N，N=missionIndex）**：
  WIN = slot0 在矩形 x[50,57)×y[57,64) **闲置 20 tick**（blk2/3）；
  LOSS = **村民(type<2)死亡 → 20 tick 判负**（blk4/5）。**军事死亡合法**（实战：
  phase0 用 t3 换掉西部双敌，游戏继续）。"任何单位死亡=判负"旧说作废。
- 地形：口袋被树墙围死；砍隧道 3 行 y=57/58/59 从 x=36 推到 ≥50 后村民自流入堡区。
- **堡垒 (53,59) TC + 四塔 (53,57)(53,61)(55,59)(51,59) 全是 p0 我方建筑**——护送
  终点=自家堡垒，"走进敌堡"旧叙事错。塔射程 **√5≈2.24 格**（hdr[12]=5，欧氏距离²
  判定），"塔射程 4 / 塔杀 x=44"旧归因双错。
- 敌兵 16 全程怠机（gameMode 32 → aiEnabled=false）；闲置军事只自动索敌**建筑**
  （近战²≤9/远程²≤16，塔优先），单位间战斗只在相邻触发。**x=44 折损真凶=西部
  固定敌 (15,47)(16,54) t3 追杀滞留村民**——满载 retask 失灵的村民会西北漂 23 格。
- 制胜配方 `m1run2.py`（已入库）：phase0 军事清西敌 → fence y∈[57,63] 全线禁北漂
  → rows 57-59 砍到 50 → 全村民推 (51,60)。
- res 解码法（m2-m7 通用钥匙）：data.res 条目表=偏移数组；条件 opcode 7=headers
  比较（胜负条件都能用它读懂）。
### m2 经济关（✅ WIN ticks=64077，2026-09-04；res 112 解码结案版）
- **胜负条件（res 112 全量解码，tools/campaign/resdec.py 可复用）**：WIN=三桶
  **链式锁存** blk2 木>100 → blk3 金>100 → blk4 石>100 → blk5 计时 20t → WIN；
  **严格 >100（101+）**，每环过线即锁存，不要求三桶同时持有。**无任何判负块**
  ——村民/军事死亡均合法（实战损 2 军事照常），判负只剩通用规则（TC 毁/全灭）。
  开局资源 0/0/0。
- **长途交付无 stall（BFS 实测结案）**：金场距 TC 34 格单程，首趟交付后自动循环
  10+ 趟零干预，整周期 ~500-600 tick——旧"DDA 30+格 stall/接力状态机"证伪废弃。
- 教训：**采集 retask 必须打资源簇前排格**（石场内部孤格 (31,6) 四邻全石 → BFS
  不可达停摆 3 分钟；改前排 (30,6) 立即恢复）。
- 军事点名清场标准流：idle 敌不主动、战斗仅相邻触发 → `sel <位> + goto <敌位>`
  每 6s 一轮重发，单敌 ~10s 击杀。双塔 (16,32)(18,37) + 矿营 (14,40)；敌 8 个全怠机。
- 终局 [290,102,162]（木290/金102/石162——金最后过线触发链）。
- 回放提示：trace t=48577 有一条对在岗金村民的冗余 retask（驱动重入 quirk，无害）；
  尾段 48577→64077 无事件=三村民全自动循环到 WIN，正常。
### m2 经济关 100木/100石/100金（旧档案，被上行取代）
- 村庄 (16-23,5-12)：TC(21,10) House(19,10) Barracks(20,8)；村民 3 (17,9)(17,11)(19,11)
- 军 4×t3(20-21,5-6) 2×t5(23,9)(23,11)；敌 8: (29,9)(36,14)(51,20)(16,28)(18,30)(17,38)(18,39)(59,42)
- 石(30-34,4-8) NE 近；金(53-57,18-22) E 远；木在西南 (10-15,24-32)+(19-52,29-49 南部大山)
  ——村庄边全是浆果丛(kind0)！
- 上轮教训: 先清(17,38)(18,39)弓手再伐木；BFS 应解长途交付 stall（待验证）

## 工具
- `tools/campaign/lib.py` —— 驱动库（slots/retask/chop_rows/gather_hammer/rally_seq/result）
- `tools/campaign/m1run2.py` —— m1 制胜配方（phase0 清敌→fence→砍隧道→护送）
- `tools/campaign/watch-death.sh` —— 被动死讯监视器（poll play.log+封存 t.aoesave，零 fifo 污染）
- tools/mktrace.py <log> <base> <out> --until '[result]'；tools/campaign-replay.sh <dir> [tickms] [--headless] [--video]
- 验证回放: campaign-replay 出"终局对拍 一致 ✓ + 操作流对拍"才算录制合格；
  **回放旗标必须与录制同款（bfsPath/mapSeed）**——脚本已默认 bfsPath=1
