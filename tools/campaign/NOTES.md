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
### m4 大学关（✅ **WIN ticks=93642**（r53，2026-09-05）——16 轮 51 boot 零的突破！胜利链=木银行→b2 序→僵局盘（m=9/双塔/raid 灭尽敌村民波断流）→封建 15/15/15→BS 热修城堡门→城堡 20/20/20→University(46,56)→50t。三件套入 recordings/campaign/m4/；⚠️ 回放不能位精确复现（AI 波非模拟随机实证））
- **胜负条件（res 114）**：WIN = 封建→**升城堡完成**（techFlags[14]==1，c.java:6200）
  → **放置 University/type4**（置回 0，c.java:7590）→ 计时 50t → WIN。res 127 初始
  [14]=0 ⇒ University 在城堡前被"已建成"锁死，**顺序不可颠倒**。Univ 25木/25石。
  res 122：Mill[15,0,10]/BS[25,0,20]——[15][16]=0 锁 Mill/BS 到封建。
  **无判负块**（TC 毁/末单位死通用规则）。纯经济竞速关。
- **本关生产语义（r44 实证，全游戏通用）**：**村民由 House(type11) 训练，TC 不生产**；
  兵营(type10)产民兵占 2 pop。村民产能=House 数；"House 先行"=生产建筑先行。
- **首个战役 AI 关**（threshold=30）：敌每 tick 重算 stance，**一次性派 75% 单位槽**
  （含村民混排）直扑 TC±1。练兵类型按 `tickCount%10` 抽签；**敌会补村民（raid 须
  驻留压制）**。**n7==0 冻结漏洞**：我方 0 单位时 AI 扫描整块跳过 → 永不再出兵。
- **all-in 门**：`敌 armyValue≥30 且 我<敌×1.25`（c.java:8458）；杀村民不直接减
  armyValue（断收入用）。**reserve 干涸前提（r42 修正）**：「敌 reserve 波
  ~ar5700-6000 干涸」仅当敌村民被清才成立——敌村民存活时 army 滚到 102 波不断
  （r44 boot3 再证 AV 97）。
- **波 1 方差（r44 勘误，r51 续证）**：26+ 样本跨 **[1168, 3838]**；**连续最早档
  是常态不是尾部**（6 连 ≤1364）；**burst 跟波真实存在且愈演愈烈**（间隔下界
  193t，到 n=6；r50 boot3 1150t 内 n1→5）——中晚签也会输，burst 局死于级联；
  但 **raid 灭尽敌村民=波断流**（r51 boot3 实证 20+ 波全防），僵局盘是合法终局路线——战术评估以"1600t 内 n1→4 连击"为默认场景，
  卡时刻一律按 **1168** 设计。同图同种子开局逐 tick 一致而波 1 差 ~2300t
  （疑似 AI 侧非模拟随机，机制未考证）。
- **探索模型三要素（r43/N4 读码定案，全战役通用）**：build 雾判=`mapTiles[t]<0`
  （c.java:1479）；可建区=①全体单位当前格 3×3（每 tick，revealFogAroundUnit
  c.java:5978）+ ②**完工建筑**曼哈顿菱形 r3（塔 r6，每 tick 轮询一栋，void_a
  c.java:5904）。TC 菱形 r3 只有 8 个偶和格——**首建筑要么在这 8 格里，要么先派
  单位扫堂**（3×3 贴一格开一片）。
- **dry 假阳性定律（r43 实锤，五轮悬案结案）**：mocksim 无雾模型时"雾格 FAIL"分支
  永不触发——(45,59) 兵营五轮"时好时坏"全是 dry 假阳性，真实 boot 从未建成过。
  mocksim 必须与真实判定同构（雾/占格/时代门三件套）；mocksim4g 已补雾模型。
  **且 mocksim 波模型结构性偏温和**（真实 all-in AV 滚 97+、250-600t 连发 vs sim
  波距 700-1500t）——dry WIN 只证分支正确，中盘节奏以 boot 为准。
- **七轮战况**：r43 N1-N4 全生效（扫堂后兵营 3/3 建成、WOOD_NEAR 退役零走廊死亡）；
  r44 残局三行+尸检三修落地，**boot3 修复全按设计工作：m#1@1634、House#1@1994、
  v=5@4083、矿区上人@4731，波 1 晚抽时 TC+m 把前两波全歼（p1m 归零）**——史上最接近。
- **八轮战况（r45）**：储蓄门+house_emg 双豁免落地——「落地即花」墙正式拆除
  （boot1 W=10 稳持 700t → 矿仓 3956 落地）；**宵禁规则达成村民首次全程零死亡**
  （boot3 波 1@3088 前完成 barracks/m2/H1/v=4 零死亡，历代最佳开局）。3 boot
  LOSS 7159/无结果(僵尸局)/5108。
- **新三墙（r45 定性）**：①**TC 无攻击力**（已入 game-mechanics；mocksim 的 TC
  防御分支是虚构=dry 假阳性源，「TC+m 守庭院」实为 m 独守）；②金管道——军事
  死亡→槽位压缩→**jobs 表枚举序重建错位**（boot3 实锤：满载金矿工被改派木工，
  金蒸发）+静默窗不够交一趟金；③**跨 boot 幽灵驱动**（2400s 超时>单局时长，旧
  驱动活进新局发令+误抄 play.log，boot2 真日志被覆写丢失）。附带情报：满载
  （word7 低 nibble=3 回送态）retask 不可靠且**改派吞货**；僵尸局（0 单位但 TC
  立=不判负、TC 掉血极慢）白烧 25 分钟——驱动要自带提前弃局判定。
- **九轮战况（r46）**：交棒残局 0-3 全落地（卫生双保险零污染/风筝 live 正确/
  按槽锚定/半程矿仓/宵禁出坑 bool 短路）+新资产 **m=0 紧急豁免**（无兵时几何
  避险继续经济）。3 boot 全抽最早档（波 1=1364/1322/1168 全 ≤1364）仍 LOSS
  2826/3490/3652，但 **m#1 首次在最早档出膛并单兵全歼波 1（m4 战史首杀）**、
  **最早档局首次村民零死亡**（boot3 2/2 存活）。dry3 起 WIN@12390 稳定复现，
  金管道真实流动（G 14→27、m 冲 8）。mocksim 又修 4 处假阳性（TC 防御/探雾
  多点判定/雾 reveal 时机/action 语义）。
- **残墙定性=「波期木银行」单点（r46）**：最早档连发时满载送货在线上触发点被
  改派吞货 → W 波期恒 0-5 → 民兵只能出生 1 个挡不住 all-in 级联。**几何避险
  保命不保货，破局只能靠离线交存点**。第 10 轮候选：**首选 hdr9 伪造交存点**
  （指 (30,62)：偶和✓/d2_line≈164✓/距西树 4-6 manh；**m1 实战引擎级实证**——
  hdr9+录制+回放全通过，trace 契约支持；注意先让单位扫格探明）；次选开局首建筑
  H#1@(30,62) 作木仓房（**需先验证 House 是否 wood 交存点**——nearestDropOff
  读 hdr[9] 指针，与建筑类型的关系未考证）。配套：mocksim 加 M4D_WAVE0=<tick>
  最早抽签压力注入 + hdr9 交存模拟 + 头部「已知分叉点清单」。
- **首胜驱动**=`tools/campaign/m4pdrv.py`+`mocksim4p.py`（v7.1-p：north_walkers
  target 判据/fleeing 每拍求交/tw_max age 门/pinned 豁免+热修脚本 bs-hotfix.py
  与 reseat-miners.py；**待内化**：BS 建造支路/Mill 门 len(mills)<1/idle 跳格
  复位；boot 用 **campaign:5**+新鲜 rmsDir→idx4）；历代 m4odrv/m4ndrv/m4mdrv/
  m4ldrv/m4kdrv/m4jdrv/m4idrv/m4hdrv/m4gdrv/m4fdrv/m4edrv/m4ddrv/m4cdrv/
  m4bdrv/m4drv（consolidation 候选：r38-r53 十六代 m4 驱动可择机归并）。
- 操作要点：z=70 完工弹窗 -6 清；t2=2 pop ⇒ House 先行；slots 幽灵槽判活用 state
  JSON；`echo > fifo` 阻塞=进程已退；boot.sh 应保留每次 play.log 副本（r42 boot1/2
  play.log 被覆盖丢失）；aistate 缺敌 pool/波出生时间戳字段（波预测靠 p1mil 计数沿）。
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
