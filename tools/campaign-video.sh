#!/bin/bash
# 战役录制视频渲染（紧竞态回放版）：replay-verify 内核 + reveal 全亮帧导出 +
# ffmpeg 合成。campaign-replay.sh 的视频路径对短局不可用（宽竞态注入偏晚,
# m0 实录偏 ~4500t）——本脚本是 m0/m1/m2/m3 四轮手搓 render-video.py 的
# 统一收编,位精确口径与 replay-verify 一致（含 TOL）。
#
# 用法: campaign-video.sh <missionDir> <baseTick> '<预期result>' [TOL] [out.mp4] [fps]
#   missionDir   含 base.aoesave + trace.txt + session.log 的录制目录
#   baseTick     trace 锚点（trace.txt 首行 # base=）
#   预期result   如 '[result] WIN ticks=5172'（TOL>0 时按 WIN+ticks 差对拍）
#   TOL          终局 tick 容差（默认 0=严格全等;±1=devBoot 首帧批粒度残差）
#   out.mp4      默认 <missionDir>/replay.mp4
#   fps          默认 30（≈12 倍原速）
set -eu
DIR="${1:?用法: campaign-video.sh <missionDir> <baseTick> '<预期result>' [TOL] [out.mp4] [fps]}"
BASE="${2:?baseTick}"
EXPECT="${3:?预期 result}"
TOL="${4:-0}"
VIDEO="${5:-$DIR/replay.mp4}"
FPS="${6:-30}"
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
REPO="$(cd "$(dirname "$0")/.." && pwd)"
CP="$REPO/build/classes/java/main:$REPO/build/resources/main"

WORK=$(mktemp -d /tmp/aoe-video-XXXXXX)
FIFO="$WORK/fifo"; mkfifo "$FIFO"
VDIR="$WORK/frames"; mkdir -p "$VDIR" "$WORK/rms" "$WORK/saves"
# 快照 nfoData 播种隔离 RMS（录制环境复刻,见 campaign-replay.sh 同款注释）
python3 - "$DIR/base.aoesave" "$WORK/rms" <<'PYEOF'
import struct, sys, os
d = open(sys.argv[1], 'rb').read()
off = 8
n = struct.unpack_from('>i', d, off)[0]; off += 4 + n
off += 13
nfo_len = struct.unpack_from('>i', d, off)[0]; off += 4
nfo = d[off:off + nfo_len]
with open(os.path.join(sys.argv[2], '.nfo.rms'), 'wb') as f:
    f.write(struct.pack('>iii', 1, 1, len(nfo))); f.write(nfo)
PYEOF

echo "== 紧竞态回放+渲染 $DIR (base=$BASE TOL=$TOL) =="
java -Daoe.tickms=10 -Daoe.debug=1 -Daoe.harnessQuiet=1 -Daoe.exitOnResult=1 \
     -Daoe.saveDir=$WORK/saves -Daoe.rmsDir=$WORK/rms -Daoe.mapSeed=8224 \
     -Daoe.devBoot=$DIR/base.aoesave -Daoe.devMouse=$FIFO -Daoe.bfsPath=1 \
     -Daoe.turbo=1 -Daoe.fastSim=1 -Daoe.headless=1 -Dapple.awt.UIElement=true \
     -Daoe.reveal=1 -Daoe.videoDir=$VDIR -Daoe.resultHold=600 \
     -cp "$CP" aoe.Main > "$WORK/replay.log" 2>&1 &
PID=$!
# 提前握 fifo 写端 + 10ms 轮询 devBoot done 即发 + 等 [result] 收尾
python3 - "$FIFO" "$WORK/replay.log" "$DIR/trace.txt" "$BASE" "$EXPECT" "$TOL" <<'PYEOF'
import re, sys, time
fifo, logp, trace, base, expect, tol = sys.argv[1:7]
tol = int(tol)
fw = open(fifo, 'w')
armed = sent = False
t0 = time.time()
res = ''
while time.time() - t0 < 900:
    if res:
        break
    try:
        txt = open(logp, errors='replace').read()
    except FileNotFoundError:
        time.sleep(0.01); continue
    if not armed and '[devBoot] done' in txt:
        armed = True
        fw.write(f'replaytrace {trace} {base}\n'); fw.flush()
        print('replaytrace 已发送（紧竞态）', flush=True)
    if armed and '[result]' in txt:
        res = [ln.strip() for ln in txt.splitlines() if '[result]' in ln][-1]
        break
    time.sleep(0.01)
fw.close()
ok = bool(res) and res == expect
if not ok and tol > 0 and 'WIN' in res and 'WIN' in expect:
    a = re.search(r'ticks=(\d+)', res); b = re.search(r'ticks=(\d+)', expect)
    ok = a is not None and b is not None and abs(int(a.group(1)) - int(b.group(1))) <= tol
print(f'终局对拍: replay=[{res}] 预期=[{expect}] TOL={tol} → {"一致 ✓" if ok else "不一致 ✗"}', flush=True)
sys.exit(0 if ok else 1)
PYEOF
RC=$?
# 等帧落盘（resultHold 600t + 余量）
for i in $(seq 1 20); do kill -0 $PID 2>/dev/null || break; sleep 1; done
kill $PID 2>/dev/null || true
if [ $RC -ne 0 ]; then
  echo "FAIL: 终局对拍未过（不合成伪证视频）"; echo "replay log: $WORK/replay.log"; exit 1
fi
command -v ffmpeg >/dev/null || { echo "FAIL: ffmpeg 未安装"; exit 1; }
NFRAMES=$(ls "$VDIR" 2>/dev/null | wc -l | tr -d ' ')
[ "$NFRAMES" -gt 0 ] || { echo "FAIL: 视频帧为 0（videoDir 未生效?）"; exit 1; }
echo "== 合成视频 $VIDEO (fps=$FPS, $NFRAMES 帧) =="
ffmpeg -y -loglevel error -framerate "$FPS" -i "$VDIR/frame_%08d.png" \
  -c:v libx264 -pix_fmt yuv420p -crf 20 -movflags +faststart "$VIDEO"
echo "视频: $VIDEO"
echo "replay log: $WORK/replay.log"
