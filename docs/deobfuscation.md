# 反混淆符号表（32 轮玩家代理逆向考据汇编）

> 2026-09-03 整理。主战场 `src/main/java/AgeOfEmpires/c.java`（~8000 行 CFR 反编译）。
> **改名纪律**：大改名会污染与反编译原版的对照考古、且 diff 噪声大——以本表 + c.java
> 类头符号表注释 + 关键方法就地注释代替改名。符号表同文已内嵌 c.java 类头。
> 证据链详见 docs/agent-operations.md 各勘误节与 /tmp/aoe-play*/BUGS*.md 各轮战报。

## 1. 状态机与流程

| 符号 | 含义 | 关键证据 |
|---|---|---|
| `screenState` (aA) | 当前屏状态。2=对话框/弹窗（**期间 j() 世界循环冻结**：施工/训练/移动全停）；4=菜单；6=世界视图；8=建造菜单；12=胜利/结算屏 | r21 弹窗纪律；r26 胜利屏 |
| `pendingScreenState` (am) | 待切换屏状态（`startMissionBriefing` 链） | BUG-005 链式弹窗修复 |
| `tickCount` (ar) | 全局 tick。`-Daoe.tickms=40` → 25 tick/s | 各轮时间线 |
| `gameMode` | 0=dev/headless 随机图；16=tutorial；32=战役链。**32 链且 missionIndex≠0 时敌 TC 毁不即胜**（走 missionScript 路径） | r31 源码定案 i() case 9 |
| `startMissionBriefing(as,z,V)` | 切简报/对话框。z=对话框脚本 id：62=任务简报 70=难度/链页 74=失败简报 **98=胜负结算**；V=变体（98: **0=胜 1=败**） | r23-r26 结算流程；r30 源码 |

## 2. 胜负判定（r30/r31 定案，双路径）

1. **敌 TC 毁 → 即胜**（`i()` case 9 → `startMissionBriefing(0,98,0)`）——gameMode 32 链
   missionIndex≠0 除外；headless dev（gameMode=0）适用。**工人/村民不计入**（r30 实测
   敌 4 工人存活即胜）。
2. **我方 TC 毁 → 即败**（同函数 case 9 n==0 → 98,1）——与"0 单位=败北"是两条独立判定。
3. `missionScript` 脚本条件（opcode 4=胜 5=败）为另一路径（r23"敌 0 单位即胜"疑此路径）。

## 3. 玩家数据结构

| 符号 | 结构 | 字段语义 |
|---|---|---|
| `playerUnitHeaders[p]` | int[] 标量区 | [0]=age（黑暗0/封建1）[2]=单位数(=pop占用) [3]=pop上限 [4]=建筑数 [5][6][7]=**木/金/石** [48]=投射物活跃数 [49]=在训队列长 [66+]=按产品计数 |
| `playerUnitSlots[p]` | **short[]** 单位表，stride=8，`i < [2]` 全是活单位 | [+0] tile 打包(tx=`>>>8`, ty=`&0xFF`)；[+3] `&0xFF`=type；[+4] `&0x8000`=选中位；[+7] **任务字**：低 nibble 0=闲置 1=行军 2=采集中（高字节 0x66=满载计时，bit4-5=资源种）3=回送中 |
| `var_int_arr_arr_b[p]` | **int[] 建筑表**，stride=4，`i < [4]` | [+0] tile 打包(tx=`>>8&0x3F`, ty=`&0x3F`)；[+2] 状态（0x40000000=施工中，`&0xFF`=进度，255=完工）；[+3] `&0xFF`=type。⚠️ **别拿它数单位**——rally 宏因此零移动+回显失真（r31 事故，已修 dd96c2f） |
| `mapTiles[64*64]` | short[] 地图 | 见 §4 |

单位 type 表：0/1=村民（两种；**House 产的是 t1，t0 是任务初始赠品**——aK=0 产物 fifo
显示 type=1，r30 实测 4/4）2=民兵 3=剑士 4=弓兵 5/6=骑兵（封建/城堡）7=近战未知种
8=投石机（攻14 甲4 远程，纯近战无解）。

建筑 type 表：1=Mining Camp 5=Mill 6=Blacksmith 7=Archery Range 8=Stable 9=TC
10=Barracks 11=House（唯一村民入口，15木）12=Tower/Outpost（上限5）。
2/3 型建筑未定性（疑 Market/研究所，勿引用）。

`aK` = 生产建筑默认产品（**由建筑唯一决定不可选**）：0村民→House 2/3→Barracks
（黑暗出 2 封建出 3）4→Range 5/6→Stable。映射表硬编码在 `queueUnitTraining`。

## 4. 地图格编码（mapTiles short）

| 码 | 含义 |
|---|---|
| `0x8000` | 未探索雾（gather/build 的雾检位） |
| `0x300` 类 | 地表对象：**0x300=资源**（低 2 位 1木 2金 3石；高字节≈剩余量非种类）`0x200`=单位占位（低字节=槽位）`0x100`=建筑占位（低字节=序号，bit10-11=owner） |
| `0x83xx` | **雾下资源**——`&3` 判矿种（r27 判别式）；低字节 0x7d/e/f 是雾占位符 |
| `0x85xx` | **雾下建筑**（低字节=建筑 type）——雾中信息可读，免侦察地图术（r31） |
| `0x0` | 虚空/毁灭后清零（`i()` 清占位） |
| `0x1604` | rubble 建筑废墟 |
| `-32768` | 未初始化（任务环境装载前） |

## 5. 关键方法速查（均已就地注释）

| 方法 | 语义 |
|---|---|
| `i(p, slot)` | 建筑/单位摧毁处理器：清占位、techFlags 置位、pop 回收、**胜负判定**（§2） |
| `startMissionBriefing(as,z,V)` | 简报/对话框切换（§1） |
| `setupMissionEnv(n)` | 任务环境装载（n=0 卸载）；`devCheckpointedThisMission` 复位在这里 |
| `selectUnits(p, type)` | 组选：0x8000 选中位；type=-1 全军事；**type<2 只选闲置村民**（word7 nibble==0——游戏自己的闲置判定，assign 宏同款）。不清旧组，调用方先 clearSelection |
| `queueUnitTraining(p, aK)` | 排队产品；aK→建筑映射硬编码（§3）；占 pop 当量，付款在出兵时 |
| `a(p, type, bl)` | ⚠️ **数建筑表**的（bl=true 排除施工中）——勿当单位计数用 |
| `devCountUnits(p, type)` | r32 新增：真单位计数基元（playerUnitSlots，type<0=全军事） |
| `openBuildingMenu(n)` | n<<=2 内部换算；宏调用传 `type & 0xFF` 不预乘；设置 aK |
| `orderMove(p, tx, ty)` | 移动令；命令落点=资源格即触发采集（r22 读码 case768） |
| `devMouseCmd` | FIFO 战术宏入口（参数校验表+FAIL 带原因回显）；宏清单见 DEVELOPMENT.md |

## 6. FIFO 战术宏（服务端直连，详见 DEVELOPMENT.md / 手册 §6.1b）

`sel`（tile 直选；FAIL 也清选中）`goto [all]` `rally`（r32 修复：真单位计数）
`count`（r32 新增）`assign`（r32 新增：闲置村民绑资源）`train` `build` `gather`
`tile` `sitrep` `ping`。**生效以进程重启为准**——live 进程加载的是启动时的类。
