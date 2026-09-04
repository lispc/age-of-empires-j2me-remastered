# recordings/ — 战役通关录制（tick 锚定，可精确回放观看）

每关一个目录 `campaign/mN/`：
- `base.aoesave`  开局快照（进任务后、任何操作前的完整模拟状态，含 tickCount）
- `trace.txt`     操作 trace（`t <相对tick> key|move|fifo ...`，tools/mktrace.py 产物）
- `session.log`   原始会话日志（[fifo]/[input] ar= 行 = 录制本体）
- `flags.txt`     录制旗标（回放必须同款；campaign-replay.sh 已默认 bfsPath=1）
- `replay.mp4`    全亮视频（--video 派生产物，30fps≈12 倍原速；可随时由三件套重生成）

回放（任意倍速，事件按 tick 锚定）：
    tools/campaign-replay.sh recordings/campaign/m1 10      # 4 倍速窗口观看
    tools/campaign-replay.sh recordings/campaign/m1 5 --headless   # 无窗口验证
    tools/campaign-replay.sh recordings/campaign/m1 2 --headless --video   # 验证+全亮视频(默认 fps=30≈12倍速, 输出 <dir>/replay.mp4)

视频 = 回放同时逐帧导出（`-Daoe.reveal=1` 迷雾全开）→ ffmpeg 合成；帧按 tick 锚定，
tickms 只影响墙钟不影响视频内容。长 trace 验证+出视频建议 tickms=2（免超时）。

战绩（调度表制录制，均经 campaign-replay 终局复现验证）：
- m1 护送关  **WIN ticks=392912**（2026-09-04，phase0 清西敌→砍隧道 y=57-59→
  护送入堡；村民 3/3 全活；军事死亡合法）。尸检翻案与关脚本解码见
  tools/campaign/NOTES.md 与 docs/agent-operations.md §11。
- m2 经济关  待打（100木/100石/100金）。
- （旧 DDA 时代 WIN ticks=90133 录制因锚点制度升级已退役，被本表 m1 覆盖。）
