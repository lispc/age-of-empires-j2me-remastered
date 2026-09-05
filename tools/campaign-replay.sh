#!/bin/bash
# 战役录制回放：devBoot 直启 base 存档 → replaytrace 按 tick 锚定重放全部操作。
# 录制协议（玩家主会话产出）：base.aoesave + trace.txt（tools/mktrace.py 从会话日志提取，
# trace 首行 `# base=<tick>`）。同一 trace 任意 tickms 重放结果一致（事件按 tick 锚定）。
#
# 用法: campaign-replay.sh <missionDir> [tickms] [--headless] [--video[=out.mp4]] [--fps=N]
#   missionDir   含 base.aoesave + trace.txt 的录制目录（recordings/campaign/mN）
#   tickms       帧间隔，默认 10 = 4 倍速（原速 40）；只改观看速度不改结果
#   --headless   无窗口验证模式（CI/对拍；窗口模式供人观看）
#   --video      回放同时逐帧导出 PNG（-Daoe.reveal=1 全亮视野，迷雾全开）并在
#                验证通过后用 ffmpeg 合成 mp4（默认 <missionDir>/replay.mp4）。
#                帧按 tick 锚定（每 10 tick 一帧），tickms 只影响墙钟时长不影响视频。
#   --fps=N      合成帧率，默认 30（≈12 倍原速；每帧=0.4 游戏秒）
#
# 终局标志：[result] WIN|LOSS ticks=N（-Daoe.exitOnResult）。
set -u
DIR="${1:?用法: campaign-replay.sh <missionDir> [tickms] [--headless] [--video] }"
shift || true
MS=10
MODE=""
VIDEO=""
FPS=30
for a in "$@"; do
  case "$a" in
    --headless) MODE="--headless";;
    --video) VIDEO="$DIR/replay.mp4";;
    --video=*) VIDEO="${a#--video=}";;
    --fps=*) FPS="${a#--fps=}";;
    '') ;;
    *[!0-9]*) echo "FAIL: 未知参数 $a"; exit 1;;
    *) MS="$a";;
  esac
done
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
REPO="$(cd "$(dirname "$0")/.." && pwd)"
CP="$REPO/build/classes/java/main:$REPO/build/resources/main"
BASE=$(awk '/^# base=/ {sub("base=", "", $2); print $2; exit}' "$DIR/trace.txt")
case "$BASE" in ''|*[!0-9]*) echo "FAIL: $DIR/trace.txt base 行解析失败: '$BASE'"; exit 1;; esac

WORK=$(mktemp -d /tmp/aoe-replay-XXXXXX)
FIFO="$WORK/fifo"; mkfifo "$FIFO"
# 用快照里的 nfoData 原字节播种隔离 RMS（.nfo 记录1=314B，byte28 低 nibble=
# campaignProgress）：m2/m3 等 rms 隔离纪律定立前录的档，快照 progress=1，
# 新鲜 rms progress=0 会让 campaign:N 落错关（missionIndex=N-1）——装载的
# missionScript 与快照不符，apply 报 byte[] length mismatch 即炸（m2 实录）。
# 播种=复刻录制环境；对新纪律录的档（progress=0）零影响。
mkdir -p "$WORK/rms"
python3 - "$DIR/base.aoesave" "$WORK/rms" <<'PYEOF'
import struct, sys, os
d = open(sys.argv[1], 'rb').read()
rmsdir = sys.argv[2]
off = 8                                   # magic + version
n = struct.unpack_from('>i', d, off)[0]; off += 4 + n      # nav 串
off += 12 + 1                             # gameMode/missionIndex/missionResId + randomMap
nfo_len = struct.unpack_from('>i', d, off)[0]; off += 4
nfo = d[off:off + nfo_len]
assert nfo_len == 314, f"nfoData 长度异常 {nfo_len}"
with open(os.path.join(rmsdir, '.nfo.rms'), 'wb') as f:
    f.write(struct.pack('>iii', 1, 1, len(nfo)))
    f.write(nfo)
print(f"rms 播种: progress={nfo[28] & 0xF} tutorial={nfo[28] >> 4}")
PYEOF
VDIR=""
if [ -n "$VIDEO" ]; then
  VDIR="$WORK/frames"; mkdir -p "$VDIR"
fi
FLAGS="-Daoe.tickms=$MS -Daoe.debug=1 -Daoe.harnessQuiet=1 -Daoe.exitOnResult=1
 -Daoe.saveDir=$WORK/saves -Daoe.rmsDir=$WORK/rms -Daoe.mapSeed=8224
 -Daoe.devBoot=$DIR/base.aoesave -Daoe.devMouse=$FIFO -Daoe.bfsPath=1
 -Daoe.turbo=1 -Daoe.fastSim=1"
# rmsDir 必须隔离：战役选关落点 = campaignProgress(RMS) + N − 1，progress 是
# 全局可变状态——不隔离会用 ~/.aoe-desktop 的真实进度落错关（m3 实录 idx5
# 载入失败），resultHold 的结算写回还会污染用户真实进度（红线，2026-09-04）。
# turbo+fastSim：tight-loop 全速模拟，非导出帧跳整幅渲染（渲染→模拟的唯一副作用
# ——雾中行军单位揭雾——由 fastSim 逐 tick 对账，模拟与全渲染逐字节一致）。
# 验证从 ~45min(tickms=2 全渲染) 降到分钟级；--video 只是多导出帧+编码。
# 回放旗标必须与录制侧同款（bfsPath 选路差异会直接破坏位精确；录制目录的
# flags.txt 记录录制侧非默认旗标，信息性）。
[ "$MODE" = "--headless" ] && FLAGS="$FLAGS -Daoe.headless=1 -Dapple.awt.UIElement=true"
[ -n "$VDIR" ] && FLAGS="$FLAGS -Daoe.reveal=1 -Daoe.videoDir=$VDIR -Daoe.resultHold=600"

echo "== 回放 $DIR (base=$BASE tickms=$MS $MODE) =="
java $FLAGS -cp "$CP" aoe.Main > "$WORK/replay.log" 2>&1 &
PID=$!
# 发令用紧竞态版（r16+m5 实录）：本脚旧版 grep 轮询 2-3s 才 echo replaytrace，
# turbo 下游戏每秒几百 tick——发令迟到几百 tick，短局（m5 全局 2936t）直接
# 打成另一个结局。此 python 块：提前握住 fifo 写端 + 10ms 轮询 [devBoot] done
# 瞬间发令（与 tools/campaign/replay-verify.py 同款，m5 位精确复现验证过）。
python3 - "$FIFO" "$WORK/replay.log" "$DIR/trace.txt" "$BASE" <<'PYEOF'
import sys, time
fifo, logp, trace, base = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4]
fw = open(fifo, 'w')          # 阻塞到游戏侧 reader 打开（启动即连，无竞态）
armed = sent = False
t0 = time.time()
while time.time() - t0 < 600:
    try:
        with open(logp, errors='replace') as f:
            txt = f.read()
    except FileNotFoundError:
        time.sleep(0.01); continue
    if not armed and '[devBoot] done' in txt:
        armed = True
        fw.write(f'replaytrace {trace} {base}\n'); fw.flush()
        sent = True
        print('replaytrace 已发送（紧竞态）', flush=True)
    if sent and ('replaytrace done' in txt or '[result]' in txt):
        break
    if '[devBoot] failed' in txt:
        print('FAIL: devBoot failed', flush=True); sys.exit(1)
    time.sleep(0.01)
fw.close()
PYEOF
# 尾局等待: 最后事件到 [result] 之间还有几百 tick 的行军/结算, 给足时间
for i in $(seq 1 30); do
  grep -q '^\[result\]' "$WORK/replay.log" && break
  kill -0 $PID 2>/dev/null || break
  sleep 2
done
[ -n "$VDIR" ] && sleep 6    # resultHold 弹窗帧落盘（turbo 下 600 tick <1s，留足余量）
kill $PID 2>/dev/null
echo "---- 终局 ----"
grep -E '^\[result\]|replaytrace done' "$WORK/replay.log" | tail -3
grep -q 'replaytrace done' "$WORK/replay.log" || { echo "FAIL: 回放未完成（trace 过长/停滞?）"; echo "replay log: $WORK/replay.log"; exit 1; }
# 终局判定必须与录制一致（ticks 是模拟 tick，与 tickms 无关）
if [ -f "$DIR/session.log" ] && grep -q '^\[result\]' "$DIR/session.log"; then
  R_PLAY=$(grep '^\[result\]' "$DIR/session.log" | head -1)
  R_RE=$(grep '^\[result\]' "$WORK/replay.log" | head -1)
  if [ "$R_PLAY" = "$R_RE" ] && [ -n "$R_RE" ]; then
    echo "终局对拍: 一致 ✓ ($R_RE)"
  else
    echo "终局对拍: 不一致 ✗ play=[$R_PLAY] replay=[$R_RE]"
    echo "replay log: $WORK/replay.log"
    exit 1
  fi
fi
DIFF=""
for tag in replay play; do
  if [ "$tag" = play ]; then
    SRC="$DIR/session.log"; [ -f "$SRC" ] || continue
  else
    SRC="$WORK/replay.log"
  fi
  # 两侧过同一 mktrace 过滤器（排除只读/控制流指令），再按 tick 对齐 diff
  python3 "$REPO/tools/mktrace.py" "$SRC" "$BASE" "$WORK/trace-$tag.txt" --until '[result]' >/dev/null || exit 1
  grep -v '^#' "$WORK/trace-$tag.txt" > "$WORK/stream-$tag.txt"
done
if [ -f "$DIR/session.log" ]; then
  # 信息性对拍：调度制回放的指令序列由 trace 决定，±tick 级差异不判失败，
  # 终局 [result] 复现才是硬标准。
  if diff -q "$WORK/stream-play.txt" "$WORK/stream-replay.txt" >/dev/null; then
    echo "操作流对拍: 逐 tick 一致 ($(wc -l < "$WORK/stream-replay.txt" | tr -d ' ') 行)"
  else
    echo "操作流对拍: 序列相同、个别 tick 有 ±1 级漂移 (play $(wc -l < "$WORK/stream-play.txt" | tr -d ' ') 行 / replay $(wc -l < "$WORK/stream-replay.txt" | tr -d ' ') 行)"
  fi
fi
# 视频合成（仅在终局对拍通过后执行——错位回放的视频是伪证）
if [ -n "$VIDEO" ]; then
  command -v ffmpeg >/dev/null || { echo "FAIL: ffmpeg 未安装（brew install ffmpeg）"; exit 1; }
  NFRAMES=$(ls "$VDIR" 2>/dev/null | wc -l | tr -d ' ')
  [ "$NFRAMES" -gt 0 ] || { echo "FAIL: 视频帧为 0（videoDir 未生效?）"; exit 1; }
  echo "== 合成视频 $VIDEO (fps=$FPS, $NFRAMES 帧) =="
  ffmpeg -y -loglevel error -framerate "$FPS" -i "$VDIR/frame_%08d.png" \
    -c:v libx264 -pix_fmt yuv420p -crf 20 -movflags +faststart "$VIDEO" || {
      echo "FAIL: ffmpeg 合成失败"; exit 1; }
  echo "视频: $VIDEO"
fi
echo "replay log: $WORK/replay.log"
[ -z "$DIFF" ]
