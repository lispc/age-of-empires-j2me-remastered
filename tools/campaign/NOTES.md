# 战役攻略会话笔记（BFS 时代，2026-09-03 第 2 夜）

## 启动配方（录制标准，每关同款）
```
java -Daoe.headless=1 -Daoe.dev=campaign:N -Daoe.tickms=10 -Daoe.debug=1 \
  -Daoe.harnessQuiet=1 -Daoe.exitOnResult=1 -Daoe.saveDir=<work>/saves \
  -Daoe.mapSeed=8224 -Daoe.bfsPath=1 -Daoe.devMouse=<work>/fifo \
  -cp build/classes/java/main:build/resources/main aoe.Main > <work>/play.log 2>&1 &
```
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
### m2 经济关 100木/100石/100金
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
