# recordings/ — 战役通关录制（tick 锚定，可精确回放观看）

每关一个目录 `campaign/mN/`：
- `base.aoesave`  开局快照（进任务后、任何操作前的完整模拟状态，含 tickCount）
- `trace.txt`     操作 trace（`t <相对tick> key|move|fifo ...`，tools/mktrace.py 产物）
- `session.log`   原始会话日志（[fifo]/[input] ar= 行 = 录制本体）
- `flags.txt`     录制旗标（回放必须同款；campaign-replay.sh 已默认 bfsPath=1）
- `replay.mp4`    全亮视频（本地派生产物，**不入库**——`recordings/**/*.mp4` 在
    .gitignore；用 campaign-replay `--video` 随时再生，30fps≈12 倍原速）

回放（任意倍速，事件按 tick 锚定）：
    tools/campaign-replay.sh recordings/campaign/m1 10      # 4 倍速窗口观看
    tools/campaign-replay.sh recordings/campaign/m1 5 --headless   # 无窗口验证
    tools/campaign-replay.sh recordings/campaign/m1 2 --headless --video   # 验证+全亮视频(默认 fps=30≈12倍速, 输出 <dir>/replay.mp4)

视频 = 回放同时逐帧导出（`-Daoe.reveal=1` 迷雾全开）→ ffmpeg 合成；帧按 tick 锚定，
tickms 只影响墙钟不影响视频内容。长 trace 验证+出视频建议 tickms=2（免超时）。

战绩（调度表制录制，均经 campaign-replay 终局复现验证；验证+出视频全程 turbo+fastSim ≈1-5 分钟/关）：
- m1 护送关  **WIN ticks=392912**（2026-09-04，phase0 清西敌→砍隧道 y=57-59→
  护送入堡；村民 3/3 全活；军事死亡合法）。尸检翻案与关脚本解码见
  tools/campaign/NOTES.md 与 docs/agent-operations.md §11。片尾拍到
  "You are victorious"（-Daoe.resultHold）。
- m2 经济关  **WIN ticks=64077**（2026-09-04，三桶链式锁存 >101：木→金→石；
  无判负块；BFS 长途交付 34 格自动循环零 stall）。trace 仅 49 事件——
  尾段为三村民全自动采集循环。
- m4 大学关  **WIN ticks=19112**（2026-09-05 r58 重录，v4 档+直播三旗标；
  本局 16 波全 n1/n2 小波+塔@5118+封建@14312+城堡@18595+University@18981；
  **回放位精确 ✓ drift=0**——replay-probe 221 事件复现 WIN@19112。首胜局
  WIN@93642 为 v3 档不可回放，已被本录制覆盖；本地 replay.mp4=全亮胜利视频
  65s/2.97MB 含 win 弹窗。波抽签是本关唯一难度（r53-r58 见 NOTES m4 档案）。
  录制侧含 2 支 FIFO 热修脚本（bs-hotfix/reseat-miners，已入库）+ 带视频
  旗标的 m4v-boot.sh。
- m5 守城关  **WIN ticks=2936**（2026-09-05，首轮 4 boot 即胜：扛完 5 波
  （剑士西/弓兵北/骑士南/冲车北/投石机南，间隔 500-700t）再全歼——LOSS=城堡
  被毁、WIN=p1 单位数==0 且 c0≥20；制胜配方=「单点集火+簇驻+接战门」，
  96 条写宏流回放逐 tick 一致）。回放验证 ✓（probe 模式）。
- m6 总攻关  **WIN ticks=30067**（2026-09-06 r66 攻克，十轮 27 boot；制胜
  配方=「帖位双闸帖扫」——y0 行驻军+帖、洪泛连通+帖 d²>25 严格 aggro 外，
  白拿 12 座后守军被引动、reprisal 相邻战清袋、BAD refresh 逐波解锁，my=3
  生还；冲车啃塔探针定案 0.16hp/t/车 作兜底未上主线）。717 事件
  replay-verify 位精确 PASS + campaign-replay 终局对拍一致；本地
  replay.mp4=全亮胜利视频 3031 帧。十轮配方演进（standoff→决斗梯→帖诱
  反杀→自投罗网→帖扫+攻城器械）见 tools/campaign/NOTES.md m6 档案。
- m0 拆堡关  **WIN ticks=8576**（2026-09-06 r67 首战即捷，宏线收官战；
  探针测绘→离线 73-hop 环线推演→单 boot 通关，8 兵损 4 拆平 TC；本关产出
  近战零 reprisal/威胁模型按型半径两条机制修正）。336 事件
  replay-verify 位精确 PASS + mktrace 事件流逐 tick 一致；本地 replay.mp4
  874 帧紧竞态自渲染。

**宏线七关全清（2026-09-06）**：m0 8576 / m1 392912 / m2 64077 / m3 89191 /
m4 19112 / m5 2936 / m6 30067——全部位精确可回放+全亮视频。

- （旧 DDA 时代 WIN ticks=90133 录制因锚点制度升级已退役，被本表 m1 覆盖。）

**2026-09-06 全量复验**（player-ai 合并后构建，campaign-replay 紧竞态版）：
m1/m2/m3/m4/m5 五套三件套**全部位精确复现 WIN**（操作流逐 tick 一致）。
两处工具修复由本次复验暴露：①campaign-replay 发令改紧竞态（提前握 fifo 写端
+10ms 轮询 devBoot done 即发——旧版 2-3s 轮询在 turbo 下迟到数百 tick，短局
m5 直接打成另一结局）；②回放前用快照 nfoData 原字节播种隔离 RMS（m2/m3 是
rms 隔离纪律定立前录的档，快照 progress=1 而新鲜 rms progress=0 会让
campaign:N 落错关，apply 即 byte[] length mismatch）。m2/m3 旧视频先于现行
base，已用修后脚本重渲染；m5 全亮视频首次生成（314 帧）。
