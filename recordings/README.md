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
- m4 大学关  **WIN ticks=93642**（2026-09-05，16 轮 51 boot 首胜：木银行→
  raid 灭尽敌村民波断流→封建 15/15/15→城堡 20/20/20→University(46,56)→50t）。
  **⚠️ 本关 trace 只保证录制本体可考，回放不能位精确复现**：m4 是唯一
  aiEnabled=true 的战役关，敌方 AI 波时刻存在非模拟随机源（r51 升格实证：
  trace 逐拍重放、ar 检查点吻合但首战提前 650t）——回放会分叉，见
  tools/campaign/BUGS-m4p/m4pdrv.md 与 WORKLOG 第 22 夜。录制侧含 2 支
  FIFO 热修脚本（bs-hotfix/reseat-miners，已入库 tools/campaign/）。
- m5 守城关  **WIN ticks=2936**（2026-09-05，首轮 4 boot 即胜：扛完 5 波
  （剑士西/弓兵北/骑士南/冲车北/投石机南，间隔 500-700t）再全歼——LOSS=城堡
  被毁、WIN=p1 单位数==0 且 c0≥20；制胜配方=「单点集火+簇驻+接战门」，
  96 条写宏流回放逐 tick 一致）。回放验证 ✓（probe 模式）。
- （旧 DDA 时代 WIN ticks=90133 录制因锚点制度升级已退役，被本表 m1 覆盖。）
