# 未来任务建议（2026-09-05 起）

> 院里晾着的活，按预期收益排序。开工前先看 `src/main/java/aoe/ai/README.md`
> 的排除项（别再试清单）和 `docs/game-mechanics.md` 的机制档案。
> 完成一项就从本档划掉一项（或标 done+日期），别让它烂尾成考古现场。

## A. 战役 AI（camloop + devPhase 成对批测）

1. **#4 剩余 11 个负相位**（当前 9/20）：
   - 中盘拉锯桶（相位 28/49/56/77/84/98/119 等，~6-8k 耗尽亡）：候选 =
     WatchTower 提前（v28 科研储备后 WT 可能终于研究得起了；塔甲 10→15 +
     索敌 4→5 格是拉锯战倍增器）、塔 2 时机、敌 raid 走廊中段拦截。
   - 早死桶（相位 0/91/112，首波 ~1.2k local 到 TC，塔 ~2k 才立）：候选 =
     首波预警提前攒塔料（敌兵营落成即切配额）、献祭一个便宜建筑拖时间。
   - 尸检入口：`C4V=xxx tools/camloop.sh -m 5 -n 20 -k`，日志里
     `[cai]` 行（build/research/500t 摘要含 emil）。
2. **#2 经济关残留考证**：units=0 不判负到底是引擎原样还是战役脚本缺路径
   （读 onThingDestroyed / tickMissionScript 判负链；若是原版洞，记录即可，
   别"修"——批测侧已有僵尸局投降兜底）。
3. **#6 钓鱼 Volley 借鉴**：主线宏线在 m6 用"冲车饵+溅射触发方阵报复"钓
   守军；我们 #6 已 5/5，但若想压缩 tick 数可以抄。

## B. 随机图 AI（ailoop）

4. **Expert 攻坚（旧账 5/40）**：动手前先读主线 r45-r46 的防线经验
   （WORKLOG 2026-09-04/05 条目：v6.4-i 最早档首杀波/村民零死亡/残墙=
   波期木银行）避免撞车；devPhase 已接入 ailoop（PHASE_STEP 默认 7），
   A/B 变体逐种子×相位成对对比。
5. **假僵局判据移植 ailoop/camloop**（主线 m4 经验）：res 三值冻结 >3k tick
   且军力 ≤2 → 早杀记 LOSS，省批测墙钟（比傻等 -t 超时快得多）。
   实锤需求：seed 1002（Easy，devPhase 接入后**确定性** STALL，两遍复现）
   每批白烧 300s——先查它是不是随机图版僵尸局（敌 stance 链断），
   顺带决定进跳过表还是修引擎。

## C. 基建/卫生

6. **改名债务**：renamer（AST）+ waveN.tsv + 逐条过 diff 看注释；
   语义登记 docs/symbols.md。候选 = 近期考证过的符号（僵尸局链
   tickAi 的 hdr[53]/[54] 威胁扫描字段、aiStance 三态等）。
7. **coreaudiod 防范**：连续 kill 音频初始化中的 java 进程会病态化
   macOS 音频服务（headless 已不碰 MIDI，但 GUI 调试会话注意）。
8. **批测卫生**：同一时刻只跑一批 camloop/ailoop（turbo 吃满 CPU，
   并行会扭曲墙钟敏感的环节）；批测跑着的时候禁止编译（每局起新 JVM
   读 build/classes）。
