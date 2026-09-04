# tools/campaign/ — 战役攻略驱动库（BFS 时代）

来自 2026-09-03/04 战役攻略会话的长期资产。机制考据与坑清单见
`docs/agent-operations.md` §11（录制/回放协议）与 §11.1（战役地图种子/BFS 采集约束）。

## lib.py — Camp 驱动库

`Camp(work)` 按 work 目录（fifo/fifo.json/play.log 所在地）参数化：

- `cmd / cmdq` — FIFO 指令（写宏已队列化，应用在帧首，回放位精确）
- `state / res / aA / result` — 状态与终局（`[result] WIN|LOSS ticks=N`）
- `slots(p) / units / villagers / military` — 槽位表（retask 按槽位寻址前先核对 type——
  单位死亡触发槽位压缩）
- `save_probe / trips` — 存档探针 + 树趟数读取（趟数=(raw>>2)&0x1F，砍完一载就扣，
  与交存无关）
- `gather_hammer(secs, {slot:(rx,ry,ax,ay)})` — 采集锤：位置 3 轮无进展才重发 retask
  （频繁重发触发 BFS 离队重算）；资源格/approach 格乒乓制造"踏入"
- `chop_rows(rows, x_target, secs, fence, home)` — 砍隧道机：多行并行推进；
  fence=(xmin,ymin,ymax[,xmax[,ygate]]) 围栏（出界拉回 home；xmax+ygate 组合成
  塔区禁入门）；p0 掉员即中止（护送关死村民=判负）
- `rally_seq(targets)` — 军事逐站清场

## m1run.py — m1（护送关）全程配方

用法：boot 游戏（配方见下）+ base 存档后 `python3 tools/campaign/m1run.py <work>`。
流程：清西部固定敌 (15,47)/(16,54) → 勘察树墙前排（必须前排树，墙内深处树 BFS
无种子）→ `hdr9 33 58` 伪造交存点（根治载满回送 orbit）→ 三行砍隧道（塔区围栏
防敌塔 (49,52) 射杀）→ 破墙后护送全体村民进堡 (51,60) → 等 `[result] WIN`。

## boot 配方（录制标准）

```
java -Daoe.headless=1 -Daoe.dev=campaign:N -Daoe.tickms=10 -Daoe.debug=1 \
  -Daoe.harnessQuiet=1 -Daoe.exitOnResult=1 -Daoe.saveDir=<work>/saves \
  -Daoe.mapSeed=8224 -Daoe.bfsPath=1 -Daoe.devMouse=<work>/fifo \
  -cp build/classes/java/main:build/resources/main aoe.Main > <work>/play.log 2>&1 &
```
必须 **nohup+disown**（后台任务清理会 SIGHUP 误杀游戏，macOS 无 setsid）。
地图种子：战役地图随种子派生且 z=98 结算重掷——同图重打必须 devBoot 读 base 存档。
长跑机器日志直写文件（nohup python3 -u ... > run.log），别接 tail 管道（缓冲致盲）。
