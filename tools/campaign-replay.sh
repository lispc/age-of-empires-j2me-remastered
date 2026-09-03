#!/bin/bash
# 战役录制回放：devBoot 直启 base 存档 → replaytrace 按 tick 锚定重放全部操作。
# 录制协议（玩家主会话产出）：base.aoesave + trace.txt（tools/mktrace.py 从会话日志提取，
# trace 首行 `# base=<tick>`）。同一 trace 任意 tickms 重放结果一致（事件按 tick 锚定）。
#
# 用法: campaign-replay.sh <missionDir> [tickms] [--headless]
#   missionDir   含 base.aoesave + trace.txt 的录制目录（recordings/campaign/mN）
#   tickms       帧间隔，默认 10 = 4 倍速（原速 40）；只改观看速度不改结果
#   --headless   无窗口验证模式（CI/对拍；窗口模式供人观看）
#
# 终局标志：[result] WIN|LOSS ticks=N（-Daoe.exitOnResult）。
set -u
DIR="${1:?用法: campaign-replay.sh <missionDir> [tickms] [--headless]}"
MS="${2:-10}"
MODE="${3:-}"
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
REPO="$(cd "$(dirname "$0")/.." && pwd)"
CP="$REPO/build/classes/java/main:$REPO/build/resources/main"
BASE=$(awk '/^# base=/ {sub("base=", "", $2); print $2; exit}' "$DIR/trace.txt")
case "$BASE" in ''|*[!0-9]*) echo "FAIL: $DIR/trace.txt base 行解析失败: '$BASE'"; exit 1;; esac

WORK=$(mktemp -d /tmp/aoe-replay-XXXXXX)
FIFO="$WORK/fifo"; mkfifo "$FIFO"
FLAGS="-Daoe.tickms=$MS -Daoe.debug=1 -Daoe.harnessQuiet=1 -Daoe.exitOnResult=1
 -Daoe.saveDir=$WORK/saves -Daoe.mapSeed=8224 -Daoe.devBoot=$DIR/base.aoesave
 -Daoe.devMouse=$FIFO"
[ "$MODE" = "--headless" ] && FLAGS="$FLAGS -Daoe.headless=1"

echo "== 回放 $DIR (base=$BASE tickms=$MS $MODE) =="
java $FLAGS -cp "$CP" aoe.Main > "$WORK/replay.log" 2>&1 &
PID=$!
# 等 devBoot 完成装载（读档→nav→覆写快照状态）
OK=""
for i in $(seq 1 120); do
  grep -q '\[devBoot\] done' "$WORK/replay.log" && { OK=1; break; }
  grep -q '\[devBoot\] failed' "$WORK/replay.log" && break
  sleep 2
done
[ -n "$OK" ] || { echo "FAIL: devBoot 未完成"; tail -5 "$WORK/replay.log"; kill $PID 2>/dev/null; exit 1; }
# devBoot 落地即 base 快照状态（aA=6 无弹窗）——不发任何额外输入，避免污染对拍流
echo "replaytrace $DIR/trace.txt $BASE" > "$FIFO"
# 等回放完成 + 终局（trace 时长/4 = 4 倍速墙钟秒，给足余量）
for i in $(seq 1 600); do
  grep -q 'replaytrace done' "$WORK/replay.log" && break
  kill -0 $PID 2>/dev/null || break     # exitOnResult 可能先退
  sleep 3
done
# 尾局等待: 最后事件到 [result] 之间还有几百 tick 的行军/结算, 给足时间
for i in $(seq 1 30); do
  grep -q '^\[result\]' "$WORK/replay.log" && break
  kill -0 $PID 2>/dev/null || break
  sleep 2
done
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
  if diff -q "$WORK/stream-play.txt" "$WORK/stream-replay.txt" >/dev/null; then
    echo "操作流对拍: 一致 ✓ ($(wc -l < "$WORK/stream-replay.txt" | tr -d ' ') 行)"
  else
    echo "操作流对拍: 不一致 ✗"
    diff "$WORK/stream-play.txt" "$WORK/stream-replay.txt" | head -10
    DIFF=1
  fi
fi
echo "replay log: $WORK/replay.log"
[ -z "$DIFF" ]
