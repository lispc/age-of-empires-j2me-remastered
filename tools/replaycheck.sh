#!/bin/bash
# 确定性回放自检：同一份 tick 锚定输入 trace 跑两遍（A: 现场存基准档后回放；
# B: 读基准档后回放），最终 state JSON 与 [input] 轨迹必须逐字节一致。
# 用法: tools/replaycheck.sh [campaign|tutorial] [mission]
# 依赖: -Daoe.debug=1（[input] 行）、SaveState v2（快照钉 tickCount）、
#       FIFO replaytrace/save/load 指令。详见 DEVELOPMENT.md 深调研三。
set -u
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
REPO="$(cd "$(dirname "$0")/.." && pwd)"
CP="$REPO/build/classes/java/main:$REPO/build/resources/main"
MODE="${1:-campaign}"; MISSION="${2:-1}"
WORK=/tmp/replaycheck-$$
mkdir -p "$WORK"
TRACE="$WORK/trace.txt"

# ---- 生成合成 trace:2500 tick 内 ~130 个事件(镜头/FIRE/开关地图/光标移动) ----
python3 - "$TRACE" <<'EOF'
import sys, random
rnd = random.Random(42)  # trace 本身固定
out = []
# 确定性前奏:load 可能弹出任务对话框,固定在 300/340/380 三发 -6 关掉
# (aoe.harnessQuiet 已禁掉看门狗的墙钟式乱按)
for t in (300, 340, 380):
    out.append(f"t {t} key -6")
t = 420
while t < 2500:
    r = rnd.random()
    if r < 0.40:
        k = rnd.choice([-1,-2,-3,-4,-5,-5])
        out.append(f"t {t} key {k}"); t += rnd.choice([2,3,5,8])
    elif r < 0.62:
        out.append(f"t {t} move {rnd.randint(4,236)} {rnd.randint(4,300)}"); t += rnd.choice([2,4,7])
    elif r < 0.68:
        out.append(f"t {t} key 48"); t += rnd.choice([40,120,300])
    else:
        t += rnd.choice([5,10,20])
open(sys.argv[1],"w").write("\n".join(out)+"\n")
print(f"trace: {len(out)} events, span {t} ticks")
EOF

run_one() {  # $1=A|B  $2=base(仅B用)
  local tag=$1 base=$2
  local FIFO="$WORK/fifo-$tag" LOG="$WORK/run-$tag.log"
  rm -f "$FIFO"; mkfifo "$FIFO"
  mkdir -p "$WORK/rms-$tag"
  java -Daoe.headless=1 "-Daoe.dev=$MODE:$MISSION" -Daoe.devMouse="$FIFO" \
       -Daoe.tickms=40 -Daoe.debug=1 -Daoe.harnessQuiet=1 -Daoe.saveDir="$WORK/saves-$tag" \
       -Daoe.rmsDir="$WORK/rms-$tag" \
       -cp "$CP" aoe.DevHarness "" 420 > "$LOG" 2>&1 &
  local PID=$!
  sleep 3; exec 9>"$FIFO"
  local j=""; for i in $(seq 1 60); do
    echo state >&9; sleep 0.4
    j=$(grep -o '"aA":[0-9]*' "$FIFO.json" 2>/dev/null | head -1)
    [ "$j" = '"aA":6' ] && break
    # 任务简报对话框(aA=2)可能在 nav 线程退出后才弹出:无人关窗时轮询永远卡在
    # aA=2(2026-09-02 实测 baseline 同病)。补一发 -6 关窗续等——这些键都发生在
    # 基准存档(A)或 load(B)之前,与下方 save 被拒重试的既有做法同款,不进对拍。
    [ "$j" = '"aA":2' ] && echo "key -6" >&9
    sleep 2
  done
  [ "$j" = '"aA":6' ] || { echo "[$tag] FAIL 未进任务"; kill $PID; return 1; }
  sleep 3   # 等自动 checkpoint 落盘、画面彻底稳定
  # load/save 前必须回到 aA==6（主视图）：任务脚本对话框（campaign:1 的 z=71 实测）
  # 可能在等待期间弹出——screenState 不在快照里，带着弹窗 load，恢复后弹窗残留，
  # 回放按键全打到弹窗上 → sel 字段分叉而 [input] 轨迹照一致（2026-09-04 排雷
  # 第 9 类）。这些 -6 都在基准存档(A)或 load(B)之前，不进对拍。
  for i in $(seq 1 15); do
    echo state >&9; sleep 0.4
    j=$(grep -o '"aA":[0-9]*' "$FIFO.json" 2>/dev/null | head -1)
    [ "$j" = '"aA":6' ] && break
    [ "$j" = '"aA":2' ] && echo "key -6" >&9
    sleep 1
  done
  [ "$j" = '"aA":6' ] || { echo "[$tag] FAIL load 前未回主视图 ($j)"; kill $PID; return 1; }
  if [ "$tag" = A ]; then
    # 重试直到写入成功。被拒多半是对话框开着(aA=2)——补一发 -6 关掉再试。
    # 这些 -6 都发生在基准存档之前,会被 B 的 load 整个丢弃,不涉及确定性。
    line=""
    for try in $(seq 1 30); do
      echo "save $WORK/base.aoesave" >&9
      for i in $(seq 1 10); do
        line=$(grep -o "\[save\] wrote $WORK/base.aoesave.*ar=[0-9]*" "$LOG" | tail -1)
        [ -n "$line" ] && break; sleep 0.5
      done
      [ -n "$line" ] && break
      echo state >&9; sleep 0.4
      a=$(grep -o '"aA":[0-9]*' "$FIFO.json" 2>/dev/null | head -1 | cut -d: -f2)
      echo "[$tag] save 被拒(aA=$a),试 -6 关对话框"
      [ "$a" = "2" ] && { echo "key -6" >&9; sleep 1; }
    done
    [ -n "$line" ] || { echo "[$tag] FAIL 存档始终被拒"; kill $PID; return 1; }
    base=$(echo "$line" | grep -o 'ar=[0-9]*' | cut -d= -f2)
    echo "$base" > "$WORK/base.txt"
    # A 也读自己的档:让 A/B 从"含 load 副作用(onShown 重建等)完全一致"的状态出发回放。
    # 存档成功到 load 之间弹窗仍可能冒出（同上）——再确认一次 aA==6。
    for i in $(seq 1 15); do
      echo state >&9; sleep 0.4
      j=$(grep -o '"aA":[0-9]*' "$FIFO.json" 2>/dev/null | head -1)
      [ "$j" = '"aA":6' ] && break
      [ "$j" = '"aA":2' ] && echo "key -6" >&9
      sleep 1
    done
    echo "load $WORK/base.aoesave" >&9
    sleep 1
    echo "replaytrace $TRACE $base" >&9
  else
    base=$(cat "$WORK/base.txt")   # A 的存档捕获时刻 tick,不是 A 的最终 ar!
    echo "load $WORK/base.aoesave" >&9
    # 不额外等待:load 指令自身 sleep 300ms 等帧首应用,lead-in 吸收剩余延迟
    echo "[$tag] loaded base, explicit tick=$base"
    echo "replaytrace $TRACE $base" >&9
  fi
  # 回放 2500 tick@40ms ≈ 100s + 余量
  for i in $(seq 1 75); do
    grep -q 'replaytrace done' "$LOG" && break; sleep 4
  done
  grep -q 'replaytrace done' "$LOG" || { echo "[$tag] FAIL 回放超时"; kill $PID; return 1; }
  # 确定性停表:两次运行都冻在 base+3000 这个精确 tick 上再取 state
  STOP=$(( base + 3000 ))
  echo "stopat $STOP" >&9
  for i in $(seq 1 75); do
    grep -q "stopped at ar=$STOP" "$LOG" && break; sleep 2
  done
  grep -q "stopped at ar=$STOP" "$LOG" || { echo "[$tag] FAIL 停表超时"; kill $PID; return 1; }
  sleep 0.5; echo state >&9; sleep 0.5
  cp "$FIFO.json" "$WORK/state-$tag.json"
  # 只取"读档之后"的输入行：load 把 tickCount 倒回 base，load 前现场会话的按键
  # （关对话框的 -6 等）ar 可能已越过 base，单靠 ar>=base 过滤会把它们混进对拍。
  awk '/^\[load\] applied/{seen=1; next} seen && /^\[input\]/' "$LOG" > "$WORK/input-$tag.txt"
  kill $PID 2>/dev/null; wait $PID 2>/dev/null
  echo "[$tag] done: stopped at $STOP, $(wc -l < "$WORK/input-$tag.txt") input 行"
}

run_one A "" || exit 1
run_one B "$(grep -o '"ar":[0-9]*' "$WORK/state-A.json" | head -1 | cut -d: -f2)" || exit 1

echo '---- 对拍 ----'
if diff -q "$WORK/state-A.json" "$WORK/state-B.json" >/dev/null; then
  echo "state JSON: 一致 ✓  $(head -c 120 "$WORK/state-A.json")"
else
  echo "state JSON: 不一致 ✗"; diff <(python3 -m json.tool "$WORK/state-A.json") <(python3 -m json.tool "$WORK/state-B.json") | head -20
fi
if diff -q "$WORK/input-A.txt" "$WORK/input-B.txt" >/dev/null; then
  echo "input 轨迹: 一致 ✓ ($(wc -l < "$WORK/input-A.txt") 行)"
else
  echo "input 轨迹: 不一致 ✗"; diff "$WORK/input-A.txt" "$WORK/input-B.txt" | head -10
fi
echo "工作目录: $WORK"
