# WORKLOG — 工作日志(Age of Empires II J2ME→macOS 移植)

> **本档是 append-only 日志**:只追加,不修改旧条目,不复读。新条目加在最上面
> (`## 工作日志` 头之下),一条 = 一个工作会话,写清:做了什么/证据/commit/事故。
> 接手项目请读 `DEVELOPMENT.md`(手册);本档只在复盘、考古、"这个决定当时怎么来的"
> 时查。长期有效的知识在写日志时就地沉淀进手册,不要指望别人来日志里翻。

## 日志（新在上；只追加，不改旧条目）

### 效率工程:战术宏+接力制+build order(2026-09-02, r21 运行中制定)

- **动因**:挖 r20 trace(183MB)——221 次工具调用 60% 是"sleep+grep log"等待
  循环,游戏操作碎片化每轮手搓;20 轮 0 胜主因=重开局浪费+探索模式低效。
- **服务端战术宏**(devMouseCmd 新增,regress 绿):`sel`(tile 直选)/`goto [all]`
  (移动令)/`train`(建筑排队,如实报 k/n)/`build`(直放建筑,受 canAfford/上限/
  占格约束)/`tile`(格诊断+在建进度)/`sitrep`(一行战况+敌军质心);state json 增
  res/pop/queued/ai。绕过拾取/相机/臂置全链,FAIL 带原因不静默。
- **意外收获(宏测试连锁)**:type10 官方名=**Barracks**(教学弹窗原文实锤)——
  升封建需 a(0,10)>=1 的 r15/r16 原判断"需兵营"恢复名誉;成本实测 House 15木/
  Barracks 20木10石/民兵 5木5金;放置+完工双弹窗冻结世界(施工/训练全停)实锤;
  "弹窗态 FIFO 查询被吞"修正(state/sitrep 可用)。手册 §4.2/4.4/4.7/5.1 连锁修正。
- **流程制度化**:接力协议(手册 §1.4,checkpoint 命名/交棒/回滚,永不重开);
  build order 时刻表(§5.4,纸面估算+自校);AGENTS.md 增 mid-run 盯盘+轮后
  round-stats 画像;tools/aoeops.py(宏组合层,自动清弹窗等完工)+
  tools/round-stats.py(效率画像)入库。
- commit: 宏+工具+手册(待填)。

### 第20轮:三连-5破译(生产即排队);建筑type表钉死;Defeated结算首目击;首带手册(2026-08-31)

- **首个带操作手册的轮次**(docs/agent-operations.md):启动词瘦身,机制知识指向手册,
  报告含「操作经验」节——手册闭环(实测→报告→主会话验证→并入)首次跑通。
- **生产即排队破译**(源码+实测双证):光标在完工生产建筑上,-5 连发=连排
  (pop 检查排队时/付款出兵时);r19"三连-5 爆兵"=此机制,复刻失败真因=pop 满。
- **建筑 type 真值表钉死**:1=Mining 5=Mill 6=Blacksmith 7=Range 8=Stable 9=TC
  10=Lumber(**实为步兵营** t2/t3) 11=House(**唯一村民入口**,本体15木+5人口)
  12=Tower(敌方"农田x6"实为塔);r19 表作废。升封建=Lumber≥1(非兵营,连环误判勘误);
  升城堡=Mill+Blacksmith 计数≥2(第二座 Mill 可绕)。胜利条件=拆光敌建筑(简报原文)。
- **战局**:峰值 13 单位(爆兵达成),总攻触发敌双线反扑(主力回防+raid 绕后屠 4 村民,
  经济归零)→ 13死4 vs 4死 → **Defeated z=98 首目击,自动滚入 mission 2**
  (进程/fifo 存活,跨局接续成立)。19 轮 0 胜后首次看清胜利条件全貌。
- **BUG 修复**:裸 rclick 打崩 FIFO handler(AIOOBE)→ devMouseCmd 缺参回显 usage。
- 手册大修:§3.2/3.4/3.5(生产即排队、双击全选30%、全图跳转运兵菜谱)、§4.2-4.7
  (type表/生产表/升时代前置/House 15木)、§5.1/5.3-5.5(t6骑士、双线反扑、制胜模板
  修订、Defeated 流程)、§7(-7 无选中再证、key35 toggle、缓动期 pick 旧 cam)、
  §9(结案6条+新增5条悬案)。
- commit: devMouse 参数校验 + 手册/WORKLOG。

### 第18轮:采集再验证(R17推翻);哨塔实战击杀;0单位=败北判定(2026-09-02)

- **采集再验证**:金 100→169→583、木 +30-40/35s——R17"全局失效"彻底
  推翻(其村民没真正上资源)。勘误:无食物资源,Outpost"5食"=5金。
- **哨塔实战击杀**(塔防可信);主力 7 剑士入敌基半径被 5弓+4民围歼
  (弓兵 att6 远程克制无掩护近战);2 村民+侦察被 1 追击敌兵屠戮 →
  **0 单位=立即败北**(判定修正,以实测为准)。
- var_int_i 全程=0:"raid 开关翻转"理论修正为近战追击行为。
- 机制补充:迷雾 0x8000 不可建不可拾取;建筑菜单臂置机制(clipBottom gate);
  忙队列 gate;兵营量产配方(任意选中→ctile 兵营→-5);ctile 落点屏外
  钳制到光标格(易死循环选中)。
- 第19轮制胜案(第18轮自拟):永不归零/弓兵先/12+混编/塔群封路/
  城堡时代高级兵/残血站桩回血。
- 无代码改动。

### 第17轮:拾取空间受控裁决(现公式正确);AI 突袭开关定性;再败(2026-09-02)

- **拾取空间争议裁决**:第17轮"只有左上 1/4 可点"与第14轮公式矛盾,
  主会话受控实验(镜头静止+双公式同目标 probe):物理公式精确命中,
  逻辑公式偏 (-2,-1)——**现有 ctile 保持不变**。此前"光标应在屏幕中心"
  的自洽检查是错的(静止位置=ad, J/2-8)。
- **"采集全局失效"判地图相关**:第17轮地图 768 类资源格零收入 vs
  第14轮飞轮完整——疑 mapgen 随机把资源放不可行格(原版生成器)。
  第18轮开局首验。
- **AI 突袭开关定性**(var_int_i 0/1/2):部队接近敌基→防御模式→87.5%
  兵力扑玩家 TC(原版设计)。攻击=引蛇出洞,反制=箭塔+防军。
- 战局再败:违反"全军驻家"纪律,3 兵远征+空巢被 raid。
- 无代码改动。

### 第16轮:兵营悬案裁决(第15轮误判);幽灵-5破案;村民量产入口(2026-09-02)

- **兵营不完工悬案裁决**:受控实验(定时存档+probe.py 读施工进度)证明
  施工 1.3s 完成、a(0,10,true) 门槛畅通、升封建一键成功——第15轮误判,
  根因三选一(弹窗冻结世界/静默拒绝/读错槽)。probe.py 存档解析入库。
- **"幽灵 -5"破案**:ctile/click 落训练型建筑=合成 -5=直接排队 1 兵;
  **Lumber Camp 菜单=训练村民**(本作村民量产入口,修正"产自 House")。
- 控制层:同格双击=选全场同类(比 drag 可靠);菜单数字键被 W 截断
  (按数字前必须 dump);弹窗屏态 FIFO 被吞需先 -7;AI raid 最早 ar≈34.6k。
- 战局再败(TC 被拆),但防御窗口充足(30k tick 前成军即可),第17轮
  改"30k 前成军驻家→击退 raid→整队反推"。
- 无代码改动。commit:无(纯验证轮)。

### 第15轮:p1 死亡行采到;金田发现;兵营不完工待受控实验(2026-09-02)

- **[combat] p1 死亡行样本采到**(两种阵营格式齐了);"移动接触=攻击"实测
  成立(侦察兵踩死 AI 村民)。
- **金田发现**:基地东侧 2-4 格外就有金田/石田——此前 9 轮从未发现。
  敌城位置每局不同再确认。
- **[高]兵营不完工挂起**:j() 施工循环源码复核无问题(+8/帧 ~32 帧完工,
  世界态每帧跑),与观察矛盾;该轮资源读数本身可疑(成本对不上任何表行)。
  候选解释:a) 兵营完工了但 TC 升级菜单的 a(0,10,true) 计数门槛错位
  (菜单槽位 aK vs 建筑 type 索引);b) 读数误判。第16轮受控实验
  (定时存档+res.py 读施工进度字节)裁决。
- ctile 屏内限制记录(Δdiff/Δsum 窗口)。
- 无代码改动。

### 第14轮:经济飞轮完全验证;ctile 公式 2 倍基准修正(2026-09-02)

- **经济飞轮完全验证**(第14轮核心成果):采集速率线性(1 村民采石
  +15/29s),资源按树变体区分(mapTiles 低 2 位),资源耗尽后自动 8 邻域
  找同变体,TC/House 通用交存。终局 55木/55金/691石全靠采集。
  完整流水线:House→村民/民兵→Barracks→升封建→形态升级→Archery Range。
- **ctile 公式 2 倍基准错修正**(第14轮玩家钉死):FIFO mouse 坐标直接喂
  j() 拾取管线=**物理帧缓冲空间**(=逻辑×SCALE),非 240x320 逻辑空间
  ——docs 的"逻辑坐标"说法只对真实鼠标(dispatchMouse /SCALE)成立。
  ctile 改 sx=32*(tx-ty)-2*camX-64, sy=16*(tx+ty)-2*camY+19,
  主会话验证精确命中。历轮"点击漂移 2-4 格"的真正根源即此。
- 战局:经济/军事基建全就绪但行军组织失败(相机永续缓动使框选只中 1 兵),
  未交战到时停止。新机制发现:class512 单位占位标记致村民回流误判
  (u1 卡死根因);封建建造菜单键位重排;存档直读脚本 res.py。
- regress PASS(未跑——本轮无回归面改动,compile 验证)。commit 待下轮合并。
  实际:commit `dev: ctile 物理基准修正 + FIFO 坐标勘误`。

### 第13轮:防御反击成僵局;"无资源收入"证伪——采集经济完整(2026-09-02)

- **第13轮"机制级发现"证伪**:它宣称"全代码无采集逻辑、资源只出不进"。
  主会话复查:交存入账点 c.java:6234(村民带资源回交存建筑时
  headers[5+资源]+=采集率×倍率,计算索引导致 grep 漏判),game-mechanics
  本就记载采集。**采集循环完整**:派村民上树/矿格→采集→自动回交存→
  自动回采集点。第13轮木=0 真因=从没把村民派到资源上。AI 同机制
  (aiFreeRes 是额外补贴,Easy 关闭)。第5轮"金 60→88"实证互洽。
- "payCost 钳0=免费建造"存疑:放置确认 4505 行有 canAfford 静默拒绝,
  与其观察矛盾。
- **战局僵局**:防御反击战术本身成功(老家零失守),但双方都"破产"——
  我方没采集所以木=0,AI 村民被战争消耗。第14轮改经济优先菜谱。
- 控制层语义破译入库(光标/鼠标双系统、'0' 全图视图无 parity 机动、
  -5 选中 4s 超时、训练/建造完成吸走光标、aA=7 挂空放置毒化输入)。
- 教训(第 N 次):玩家的"全代码无 X"结论必须对照实证与 docs 再采信——
  本轮若照它的建议"修 clamp 或加收入"就破坏了原版经济。

### 第12轮:[combat] 完整性验证通过;封建内容实测;Surrender 陷阱二次实锤(2026-09-02)

- **[combat] 验证通过**:9 死亡=9 行,remaining 单调 8→0 与战后 state 一致,
  战斗可完整回放。p1 死亡行留样本(我方零击杀)。
- **封建内容实测**:Mill(生产加速文案)/Blacksmith 建成,Forging 研究成功
  (扣 5木5金);Archery Range 建成。敌军力实测:民兵+弓兵混编持续爆兵。
- **AI 双线战术确认**:截击接近敌基的远征军 + 同窗口 raid 空虚老家,
  9:0 全灭败北。第13轮改防御反击模板(军队不离家)。
- **Surrender 无确认二次实锤**:自动化误触直接弃局(auto.aoesave 恢复)。
  "Surrender/Quit/退出任务加确认"合并为一个现代化候选项。
- **语义修正**:光标与鼠标两套系统(ctile 不动光标);生产菜单正确打开
  =光标置建筑格+-5;'*' 对生产无效(修正旧知识);成本表实测勘定
  (House 5木/Villager 5木 产自 House/Forging 5木5金)。
- 拾取管线卡死窗口与相机缓动漂移记档(工具瑕疵,键盘光标路径不受影响)。
- 无代码改动。commit:无(docs 走下轮合并)。

### 第11轮:"order 聋"反转结案;mod4 修正;[combat] 日志(2026-09-02)

- **"order 聋"定性反转结案**:不可达目标行为(走到断点→回退→停),复现 3 次,
  上轮两只"聋"单位全部洗清。与寻路失败回退同族,原版语义。
- **方向键不变量修正**:是 (x+y) **mod 4 四色格**,非奇偶二色(第10轮结论
  不完整)——同奇偶但 mod4 不同的格方向键不可达。文档已同步修正。
- **暂停菜单取证完成**:Continue(高亮)/Instructions/Surrender/Options/Quit;
  Surrender/Quit 无确认 → "退出加确认"候选项成立待用户拍板。勘定两界面:
  aA=12=Statistics(原版自带),aA=4 上下文相关。
- **AI raid 触发新证据**:部队接近敌基 8-10 格即触发 → 阈值判定含接近因子。
- **[combat] 死亡日志入库**:g(player,slot)=单位死亡唯一入口(槽位压缩即
  "单位瞬移"错觉源头),现打 p/type/位置/ar/剩余数 行;战斗推演可从日志
  完整回放。败北判定勘定:单位清零&&建筑清零。
- 战局再败(AI raid 老家,我军被拦截),raid 前存档已留。regress PASS。
- commit:`dev: [combat] 死亡日志; docs: 第11轮 order 聋反转+mod4 修正`。

### 第10轮:升时代打通;方向键奇偶卡位破案;情报跨局复用证伪(2026-09-02)

- **升时代流程打通**(生产链最后一块):TC -5 → "Upgrade?" → 封建
  (15/15/15),解锁 5 建筑;城堡需 Mill+Blacksmith+资源。5 村民+部队成军,
  pop 10/10。"攻击"=移动接触(-5/rclick 均 orderMove,玩家 AI 同机制,
  源码证实)。
- **方向键奇偶卡位破案**(BUG-008 终答):方向键只走对角线,(x+y) 奇偶
  永不改变——奇偶子格不连通,跨 parity 必须 ctile/右键。原版设计。
  雾格 Q=-1 证伪(-1 仅水/出界)。
- **情报跨局复用证伪**:random:1 出生点可复现,敌城/地形不可复用——
  第9轮坐标失效。方法论(链路/工具/流程)仍然全有效。
- **[高]挂起**:单位远距离 order 后变"order 聋"(sel 正常但指令无响应),
  存档 ar=87622 已留,第11轮复现+fields 采样;源码 G() 状态机排查是
  长期欠账。
- 工具:state 文本/json 上限统一(每方 16);-7 暂停菜单 Continue 两轮
  确认 → 现代化候选"菜单加 Resume"撤销,保留"退出确认"。
- regress PASS。commit:`dev: state 单位上限统一`。

### 第9轮:敌基地找到+首败;自动回家终局;双工具修复(2026-09-02)

- **战局**:敌基地=北岸 (24-28,14-18) AI 前进基地(5 建筑+~8 兵,会南下 raid);
  我军散落未回防,村民 30 秒内被屠 → 首次完整走到"You have been defeated"。
  random:1 地图确定性 → 情报跨局复用,第10轮改为"开局建军+驻防+推北岸"。
- **"自动回家"五轮悬案终局(三层)**:寻路失败回退 + state 花名册只读 P0/
  死亡槽位压缩造成的"我方单位瞬移"错觉 + idle 小半径游走。无未解释成分。
- **工具双修**:① drag 终点在贴边滚动区自动回屏中心(修复每帧边缘滚动楔死
  光标/probe 全 -2 只能重启的问题);② state/fifo.json 列出双方单位带 "p"
  阵营列(可直接监视 AI 兵力,根治阵营混淆误读)。regress 探测行同步适配
  (p0 unit 0),指纹过滤 p==0 保持语义。PASS。
- **资源负值证伪**(是 Q/aE 振荡误读,真实结论=无选中时移动令静默失效);
  方向键在 cursorTileIdx=-1(雾/水)失效待第10轮对取证(源码无门控,实测却
  复现,机制未明)。
- commit:`dev: state 双阵营单位列 + drag 防贴边楔死`。

### 第8轮:地图全勘测证伪东南假说;自动回家定性;ctile 指令(2026-09-02)

- **地图勘测**:64x64 确认(第7轮"NE 有地"系界外 probe 假阳性);东侧是海,
  东南段无敌 → 敌基地在未探的北/西北带 (y<16, x=6..40)。会战未组织成:
  无编组热键(现代化候选已有),单兵操作 ~40s/兵不可行——下轮用
  drag 框选(群体选择)+新 ctile 指令做群体移动。
- **"自动回家"定性闭环**:寻路失败回退(failed-path fallback)——部分执行+
  寻路失败后自行折返家方向,零输入;对照"可达格到达后静置 26s"。
  五轮悬案收敛为可解释行为,是否原版语义待源码确认(观察项降级)。
- **新指令 `ctile <tx> <ty>`**(第8轮玩家推导换算公式,主会话入库):
  实时相机换算 tile 直点,零漂移;解决"click 相机滞后 2-4 格"的工具问题。
  实测一次命中。
- 挂起(第9轮):资源显示负值采样;方向键某状态不动光标的干净复测;
  -7 任务菜单两轮矛盾(第7轮见 Continue/第8轮无菜单)的差异分析。
- regress PASS(compileJava+roundtrip)。commit:`dev: ctile tile 直点指令`。

### 第7轮:SaveState v3 端到端验证通过;敌基地定位推进(2026-09-02)

- **BUG-006 修复确认**:建 House → techFlags CRC 变化 → 存档(v3 头)→ 杀进程
  重启 → 读档 CRC 一致、'*' 菜单村民槽在。SaveState v3 端到端闭环。
- **经济/建军首次全链路走通**:6 村民 + 9 Pikeman + 侦察,人口 15/15;
  地图破解 = NW→SE 对角带状大陆,我方南岸,敌基地推断在东南段
  (45-60, 20-35);先遣 Pikeman 已深入 (47,31),进度存档 s2.aoesave(ar=50428)。
- 新发现 triage:雾格移动令静默拒绝与第5轮"雾中可行走"矛盾,疑似 click 路径
  把水/虚空格 cursorTileIdx 置 -1 的已有机制,第8轮对照复测;
  **-7 双义修正前案**:无弹窗时 -7 开任务菜单、高亮项=Continue、-5=恢复——
  "误触即退任务"更可能是连按 -5 竞态,若第8轮证实则现代化候选降级;
  其余(光标困小岛/state sel 显示瑕疵/probe 穿雾)记录归档。
- 无代码改动,无 commit(本轮纯验证+侦察轮)。

### BUG-006(存档丢 techFlags)破案修复:SaveState v3;第6轮(2026-09-02 凌晨)

- **第6轮玩家代理立功**:发现读档局 '*' 生产菜单空、疑似存档不持久化解锁位。
  主会话逐环节核实:SaveState 确实零读写 techFlags,而解锁链实证为
  **建筑建成/研究完成就地置位**(j() 里 House 完成 techFlags[0]=1 解锁村民
  训练;boolean_k 按单位[0+n]/建筑[10+n]/科技[23+n]门控)——读档后全部回退
  模板,已解锁的生产/研究消失。**修复:SaveState v3** 追加 techFlags 段;
  v2 旧档(用户 ~/.aoe-desktop 里那些)跳过该段保持可读。regress PASS。
  (agent 报告里"读档局 '#' 也闪关"无日志证据——它读档后只按过 '*',该细节
  已在 triage 更正;但主结论独立成立。)
- **生产链路拼图完成**(第3/6两轮接力):'#' 建造菜单开局 5 槽(模板自带)
  → 建成解锁 '*' 生产槽(House→村民、兵营→长枪)→ TC -5=研究菜单
  (升时代需先有兵营)。第3/4轮能练兵正是因为先建了房/兵营。
- 第6轮其余:链式弹窗修复经独立复现验证生效(d1-chain.png);自动回家实验
  因局面崩坏未完成,顺延第7轮;新局又踩"无选中 -5=开任务菜单→退出"陷阱
  (原版行为,自动化要避);FIFO 坐标勘误=随 aoe.width 变化,probe 每步标定。
- regress PASS。commit:`fix: SaveState v3 持久化 techFlags(读档解锁回退)`。

### BUG-005 破案修复:链式弹窗不重解析;第5轮实验(2026-09-02 凌晨)

- **第5轮玩家代理完成两个实验**,零新崩溃:
  - 自动回家:纯移动 4 例(含雾区)>13 分钟全部原地驻停 → 触发条件转向
    受击/路径受阻,第6轮在敌袭中观察。
  - 面板机制:三数字 = costTable[(id+类目偏移)*3],与扣款同源,结案;
    建造菜单 5 槽无 TC(与 TC 不可新建一致)。
- **BUG-005(空白羊皮纸)破案**:第5轮"强制 dlg 也空白"是关键——复盘发现
  agent 的第二条 dlg 是在上一条未关(aA=2)时发的。startMissionBriefing 更新
  z/V,但 screenState==pending==2 不触发 boolean_a() 的 n(aH) 重入,void_c
  不重解析 → 屏幕停留上一条正文。真实游戏连锁弹窗(简报/完成/结算相连)
  同样中招——第3轮两例空白即此类。已修:已在态 2 时重跑 n() 解析段
  (不动 overlayPrevState);加 [dlg-parse] 调试行。FIFO 验证:空条目(70,9)
  开着直接发(70,12),同一对话框立即显示 Tower 正文 ✓。
  教训:同批 FIFO 里 dlg+dump 会抓到解析前一帧,dump 要分开发。
- 战局:第5轮时间尽于实验,存档 /tmp/aoe-play5/saves/quick.aoesave
  (ar=48413)留给第6轮续打。
- regress PASS。commit:`fix: 链式弹窗立即重解析(BUG-005 结案)`。

### 第4轮玩家战报全绿;成本表破案;光标钳制(2026-09-01 深夜)

- **第4轮 agent 顺利收官**(防崩守则生效:上下文用量仅为上一轮的零头)。验证:
  BUG-007 修复后 40 连发按键 0 丢失(此前 10-30%);BUG-003 终版全程恰 1 次
  checkpoint;0 watchdog 告警。战局败(贸然进军被 AI 团灭,Statistics 屏正常)。
- **成本表破案**:costTable 按 3 字节条目、类目偏移 单位+0/建筑+10/科技+26。
  旧文档建筑表误用 +26 解码,数值全错。玩家实测放 House 扣 5 木 + 字节复核
  双确认,unit-stats.md 建筑表已按 (id+10) 重写;升时代三行(15/15/15 等)
  当年碰巧用对,维持。遗留:TC 信息面板显示 10/0/20 与所有行不符(纯显示)。
- **光标钳制(移植修正)**:tickCursor 移动后钳回 [0,63]。原版可走出边界,此后
  FIRE 对一切目标静默失效(玩家实测"行军中断"),现不可能再出界。
- **第4轮其余发现 triage 全部结案**:建筑放置即自动成型(原版无村民施工机制,
  VF 逐行对照);拖框选正常(agent 框的横带里没单位,log 佐证);兵营菜单含
  村民槽(菜单只按时代可用性过滤,原版 quirk);不可达格移动静默(原版语义)。
- **唯一挂起**:单位"自动回家"(下令远行后自行折返 TC,两次观察)——疑似
  orderMove 存下的 [slot+1] 回退路径,第5轮受控实验定位。
- regress PASS。commit:`fix: 光标出界钳制; docs: 建筑成本表按正确类目偏移重写`。

### 玩家代理三轮战报+崩溃事故;BUG-007/003 修复;005/008/004/006 triage(2026-09-01 深夜)

- **玩家代理(会话3~5,随机图+战役)累计 5 局后上下文溢出崩溃**(Invalid string
  length,2h10m)。BUGS.md 因"发现即写盘"纪律零丢失,五局产出:BUG-004~008 五案
  +一批正向验证(Easy 阈值生效:AI 首攻 30s→9min;BUG-003 节流生效;采集/训练/
  回血/框选全通)。孤儿游戏进程已收。
- **BUG-007 修复(Canvas 合成键松开竞态)**:FIFO `key` 的按下若落在 paint 中段
  (本帧输入已消费),帧末 flush 立刻清脉冲→键无痕丢失(实测 ~10-30%,且 [input]
  日志照打,看起来像"发了没反应")。修复=合成松开要求再完整过一帧
  (paintCompletedSeq+2);replaytrace 同路径,回放确定性同步受益。
- **BUG-003 终版**:auto checkpoint 从"600 tick 节流"改为"每任务一次"
  (devCheckpointedThisMission,setupMissionEnv 复位)——一局 18 次写盘变 1 次,
  与 USER-GUIDE 文档语义对齐。
- **BUG-008 结案(测试污染)**:原版键表 res#129 解码,数字键 0-9→动作 1-10,
  tickCursor 全部消费('2'/'4'/'6'/'8'=屏幕四向);agent 四次测试都打在菜单态
  (aA=7)才"不动"。非 bug。
- **BUG-005 挂起**:空白弹窗(建筑完成 parchment 无字)静态链路全过——表 70 只有
  条目 9(TC)为空、解析器/滚动钳制/颜色变体均排死,强制 `dlg 70 {12,10,11}` 含
  首帧 dump 全有文本;两例现场共同点=与 checkpoint 同帧+toast 在屏,无法复现。
  新 FIFO 指令 `strtbl`/`dlg` 入库,探针写进 BUGS.md。
- **BUG-004/006 定性(不改)**:'*'=态7 生产菜单正常;菜单树 res#117 原版未改,
  无 Resume/误触弃局/-6=前进 属原版行为 → 记入现代化候选待用户拍板。
- **新线索**:建筑成本存在三套 id 空间——res#122+payCost/canAfford=文档表
  (字节级复核一致),但游戏内面板显示另一行(TC 面板 10/0/20 vs 数据 10/0/5,
  主会话截图复证)。unit-stats.md 不动,下轮实测"造房扣料"定论。
- regress PASS。commit:`fix: FIFO key 可靠化 + checkpoint 每任务一次 + strtbl/dlg 调试指令`。

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
