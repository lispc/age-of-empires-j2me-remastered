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
### m1 护送关（BFS 时代重录目标）
- 口袋被树墙围死；砍隧道 3 行 y=58/59/60 从 x=36 推进到 x≥50；穿林后村民自己走进
  堡垒(TC 53,59 + 4塔)触发胜利。旧时代 WIN ticks=90133。敌塔 (48,50)(50,50)(49,52)
  卡北线豁口——南线安全。敌兵 16 在 (38,32) 北边不动。
### m2 经济关 100木/100石/100金
- 村庄 (16-23,5-12)：TC(21,10) House(19,10) Barracks(20,8)；村民 3 (17,9)(17,11)(19,11)
- 军 4×t3(20-21,5-6) 2×t5(23,9)(23,11)；敌 8: (29,9)(36,14)(51,20)(16,28)(18,30)(17,38)(18,39)(59,42)
- 石(30-34,4-8) NE 近；金(53-57,18-22) E 远；木在西南 (10-15,24-32)+(19-52,29-49 南部大山)
  ——村庄边全是浆果丛(kind0)！
- 上轮教训: 先清(17,38)(18,39)弓手再伐木；BFS 应解长途交付 stall（待验证）

## 工具
- /tmp/aoe-camp/lib.py —— 本轮整合的驱动库（Driver/slots/砍树/采集锤/护航/result）
- /tmp/aoe-camp/drive.py —— 旧版（m1 时代），部分方法仍被引用
- tools/mktrace.py <log> <base> <out> --until '[result]'；tools/campaign-replay.sh <dir> [tickms] [--headless]
- 验证回放: campaign-replay 出"终局对拍 一致 ✓ + 操作流对拍 一致 ✓"才算录制合格
