#!/bin/bash
# devBoot 双跑确定性自检：造基准档 → 同一存档 -Daoe.devBoot 直启两次（无输入）
# → stopat 同一 tick → state JSON + fields 全量对拍，必须逐字节一致。
# 背景：旧实现 apply 时机墙钟依赖（主视图稳定 15×200ms 后才 load），live 段
# 模拟残留（AI 计时器等快照外字段）使双跑发散（2026-09-02 取证）；修复 =
# apply 钉到首次 screenState==6 的帧首（c.java devBootPendingRestore）。
# 本脚本是该修复的常驻回归。用法: tools/bootcheck.sh [任务内tick数=3000]
# 注意：默认 random:1 + 固定 mapSeed——AI 激活的局才会暴露 AI 计时器残留
# （教学/战役 aiEnabled=false，盖不住这个 bug）。
set -u
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
REPO="$(cd "$(dirname "$0")/.." && pwd)"
CP="$REPO/build/classes/java/main:$REPO/build/resources/main"
TICKS="${1:-3000}"
SEED=999
WORK=/tmp/bootcheck-$$
mkdir -p "$WORK"

wait_view6() {  # $1=fifo $2=tag：等到主视图(aA=6)，途中推掉简报对话框
  local FIFO=$1 TAG=$2 j=""
  for i in $(seq 1 90); do
    echo state >&9; sleep 0.4
    j=$(grep -o '"aA":[0-9]*' "$FIFO.json" 2>/dev/null | head -1)
    [ "$j" = '"aA":6' ] && return 0
    [ "$j" = '"aA":2' ] && echo "key -6" >&9
    sleep 1
  done
  echo "[$TAG] FAIL 未进任务/未回主视图 ($j)"; return 1
}

# ---- 造基准档（headless random:1，固定种子）----
FIFO="$WORK/fifo-gen"; LOG="$WORK/gen.log"
rm -f "$FIFO"; mkfifo "$FIFO"
java -Daoe.headless=1 -Daoe.dev=random:1 -Daoe.mapSeed=$SEED -Daoe.tickms=40 \
     -Daoe.debug=1 -Daoe.harnessQuiet=1 -Daoe.saveDir="$WORK/saves-gen" \
     -Daoe.rmsDir="$WORK/rms-gen" -Daoe.devMouse="$FIFO" \
     -cp "$CP" aoe.Main > "$LOG" 2>&1 &
PID=$!
sleep 3; exec 9>"$FIFO"
wait_view6 "$FIFO" GEN || { kill $PID 2>/dev/null; exit 1; }
sleep 3
wait_view6 "$FIFO" GEN || { kill $PID 2>/dev/null; exit 1; }
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
  [ "$a" = "2" ] && { echo "key -6" >&9; sleep 1; }
done
[ -n "$line" ] || { echo "[GEN] FAIL 存档始终被拒"; kill $PID 2>/dev/null; exit 1; }
BASE=$(echo "$line" | grep -o 'ar=[0-9]*' | cut -d= -f2)
echo "基准档 ar=$BASE"
echo "exit" >&9; sleep 1; kill $PID 2>/dev/null; wait $PID 2>/dev/null

# ---- 双跑 devBoot（无输入，stopat 同一 tick）----
run_boot() {  # $1=A|B
  local TAG=$1
  local FIFO="$WORK/fifo-$TAG" LOG="$WORK/run-$TAG.log"
  rm -f "$FIFO"; mkfifo "$FIFO"
  java -Daoe.headless=1 -Daoe.devBoot="$WORK/base.aoesave" -Daoe.mapSeed=$SEED \
       -Daoe.tickms=40 -Daoe.debug=1 -Daoe.harnessQuiet=1 \
       -Daoe.saveDir="$WORK/saves-$TAG" -Daoe.rmsDir="$WORK/rms-$TAG" \
       -Daoe.devMouse="$FIFO" -cp "$CP" aoe.Main > "$LOG" 2>&1 &
  local PID=$!
  sleep 3; exec 9>"$FIFO"
  for i in $(seq 1 90); do
    grep -q '\[devBoot\] done' "$LOG" && break
    grep -q '\[devBoot\] failed' "$LOG" && { echo "[$TAG] FAIL devBoot failed"; kill $PID 2>/dev/null; return 1; }
    sleep 2
  done
  grep -q '\[devBoot\] done' "$LOG" || { echo "[$TAG] FAIL devBoot 超时"; kill $PID 2>/dev/null; return 1; }
  local STOP=$(( BASE + TICKS ))
  echo "stopat $STOP" >&9
  for i in $(seq 1 $(( TICKS / 10 + 60 ))); do
    grep -q "stopped at ar=$STOP" "$LOG" && break; sleep 2
  done
  grep -q "stopped at ar=$STOP" "$LOG" || { echo "[$TAG] FAIL 停表超时"; kill $PID 2>/dev/null; return 1; }
  sleep 0.5; echo state >&9; sleep 0.5
  echo "fields $WORK/fields-$TAG.txt" >&9; sleep 1
  cp "$FIFO.json" "$WORK/state-$TAG.json"
  echo "exit" >&9; sleep 1; kill $PID 2>/dev/null; wait $PID 2>/dev/null
  echo "[$TAG] done: stopped at $STOP"
}

run_boot A || exit 1
run_boot B || exit 1

echo '---- 对拍 ----'
RC=0
if diff -q "$WORK/state-A.json" "$WORK/state-B.json" >/dev/null; then
  echo "state JSON: 一致 ✓"
else
  echo "state JSON: 不一致 ✗"; RC=1
  diff <(python3 -m json.tool "$WORK/state-A.json") <(python3 -m json.tool "$WORK/state-B.json") | head -20
fi
# fields 噪声过滤：'@' = 对象身份哈希渲染（Midlet/Font/Image 等，非游戏状态），
# devToastUntil 是墙钟，devFifoPath 是各自 FIFO 路径，Image_arr_a 的 crc 由身份哈希构成。
norm() { grep -vE '@|devToastUntil|devFifoPath|Image_arr_a' "$1"; }
if diff <(norm "$WORK/fields-A.txt") <(norm "$WORK/fields-B.txt") >/dev/null; then
  echo "fields: 一致 ✓"
else
  echo "fields: 不一致 ✗"; RC=1
  diff <(norm "$WORK/fields-A.txt") <(norm "$WORK/fields-B.txt") | head -30
fi
echo "工作目录: $WORK"
[ $RC = 0 ] && echo "bootcheck PASS" || echo "bootcheck FAIL"
exit $RC
