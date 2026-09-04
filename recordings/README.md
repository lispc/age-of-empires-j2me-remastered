# recordings/ — 战役通关录制（tick 锚定，可精确回放观看）

每关一个目录 `campaign/mN/`：
- `base.aoesave`  开局快照（进任务后、任何操作前的完整模拟状态，含 tickCount）
- `trace.txt`     操作 trace（`t <相对tick> key|move|fifo ...`，tools/mktrace.py 产物）
- `session.log`   原始会话日志（[fifo]/[input] ar= 行 = 录制本体）

回放（任意倍速，事件按 tick 锚定）：
    tools/campaign-replay.sh recordings/campaign/m1 10      # 4 倍速窗口观看
    tools/campaign-replay.sh recordings/campaign/m1 5 --headless   # 无窗口验证
    tools/campaign-replay.sh recordings/campaign/m1 2 --headless --video   # 验证+全亮视频(默认 fps=30≈12倍速, 输出 <dir>/replay.mp4)

视频 = 回放同时逐帧导出（`-Daoe.reveal=1` 迷雾全开）→ ffmpeg 合成；帧按 tick 锚定，
tickms 只影响墙钟不影响视频内容。长 trace 验证+出视频建议 tickms=2（免超时）。

战绩：
- m1 护送关  WIN ticks=90133（砍隧道穿森林护村民入堡）
- m2 经济关  未完（采集引擎长途交付 stall，见 agent-operations §11）
