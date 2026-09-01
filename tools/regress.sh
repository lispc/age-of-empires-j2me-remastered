#!/bin/sh
# regress.sh — 黄金回归基线（重构安全网）。
#
# 流程：headless 进关（隔离 RMS）→
#   A. 静态阶段：选中/全图开关/光标移动（全部确定性动作）→ 取指纹与基线比对
#   B. 运动阶段：框选 + 移动单位（只防崩溃/卡死，不做断言——移动步数依赖
#      tick 数，随时钟抖动，位置天然非确定）
#   C. 存读 roundtrip：fields 前后夹存读档，噪声（regress-noise.txt）外零差异
#      roundtrip 放在进关后立即做：相机静止、无脚本活动，校验最严格
#
# 用法:
#   tools/regress.sh            # 跑基线比对
#   tools/regress.sh --update   # 重录基线（重构改名后、或刻意变更行为时）
set -e
cd "$(dirname "$0")/.."

# 静态阶段的点击与教程对话窗口存在偶发竞态（实测 ~1/6 概率动作被吞导致指纹
# 抖动）。失败自动整体重跑一次；仍失败才判定 FAIL。--update 不重试。
if [ -z "$REGRESS_RETRY" ] && [ "$1" != "--update" ]; then
    if ! REGRESS_RETRY=1 "$0" "$@"; then
        echo "---- 重试一次 ----"
        REGRESS_RETRY=1 "$0" "$@"
    fi
    exit $?
fi
JAVA=/opt/homebrew/opt/openjdk@17/bin/java
[ -x "$JAVA" ] || JAVA=java

WORK=$(mktemp -d /tmp/aoe-regress.XXXXXX)
FIFO=/tmp/aoe-regress.fifo
SAVES="$WORK/saves"
SCEN=tools/regress.scn
NOISE=tools/regress-noise.txt
GOLDEN=tools/regress.golden.json
STATE=$WORK/state.json
LOG=$WORK/run.log
trap 'rm -f "$FIFO"; rm -rf "$WORK"' EXIT

mkfifo "$FIFO"
mkdir -p "$WORK/userhome"
CP=build/classes/java/main:build/resources/main
# -Duser.home 指到临时目录：RMS(.nfo) 每次运行都是干净的——避免
# ~/.aoe-desktop 的跨会话污染（模式循环器位置漂移），保证指纹确定性。
# 载体用 tutorial:1：无 AI 噪声（随机地图 AI 走动，字段级确定性不可能）。
"$JAVA" -Daoe.debug=1 -Daoe.autoDismiss=1 -Daoe.headless=1 -Daoe.tickms=40 \
    -Duser.home="$WORK/userhome" \
    -Daoe.dev=tutorial:1 -Daoe.devMouse="$FIFO" -Daoe.saveDir="$SAVES" \
    -cp "$CP" aoe.DevHarness "$WORK/final.png" 300 > "$LOG" 2>&1 &
PID=$!

# 等进关（state 命令打印 unit 行 = 任务态）
ready=0
i=0
while [ $i -lt 45 ]; do
    sleep 2
    i=$((i+1))
    echo "state" > "$FIFO" 2>/dev/null || true
    if grep -q "devMouse. p0 unit 0" "$LOG" 2>/dev/null; then ready=1; break; fi
    kill -0 $PID 2>/dev/null || break
done
if [ $ready -ne 1 ]; then
    echo "FAIL: 未进入任务态"; tail -5 "$LOG"; exit 1
fi
sleep 4        # autoDismiss 推掉开局对话

# ---- A. 静态阶段 → 指纹 ----
{
    echo "until 6 5"
    echo "sleep 1000"
    echo "click 116 140"
    echo "sleep 1000"
    echo "until 6 5"
    echo "tapk 48 1"
    echo "until 1 5"
    echo "tapk 48 6"
    echo "until 6 5"
    echo "click 120 200"
    echo "sleep 1000"
    echo "until 6 5"
    echo "state"
    echo "sleep 300"
} > "$FIFO"
rm -f "$FIFO.json"
for i in $(seq 1 50); do
    [ -f "$FIFO.json" ] && break
    sleep 0.2
done
cp "$FIFO.json" "$STATE"

# ---- B. 运动阶段（只防崩溃，不断言）+ ----
{
    echo "drag 60 120 150 210"
    echo "sleep 1000"
    echo "rclick 150 180"
    echo "sleep 5000"
    echo "rclick 75 190"
    echo "sleep 5000"
    echo "until 6 5"
} > "$FIFO"
sleep 13

# ---- C. 存读 roundtrip（静态相机，严格）----
{
    echo "until 6 5"
    echo "sleep 2000"
    echo "fields $WORK/f_before.txt"
    echo "save $WORK/rt.aoesave"
    echo "load $WORK/rt.aoesave"
    echo "sleep 800"
    echo "fields $WORK/f_after.txt"
    echo "exit"
} > "$FIFO"

wait $PID

[ -f "$FIFO.json" ] || { echo "FAIL: state JSON 未生成"; tail -5 "$LOG"; exit 1; }

# ---- 检查 1：静态指纹 vs 基线 ----
python3 - "$STATE" "$GOLDEN" "$1" <<'PYEOF'
import json, sys
state = json.load(open(sys.argv[1]))
mode = sys.argv[3] if len(sys.argv) > 3 else ""
fp = {
    "cursor": state["cursor"],
    "sel": state["sel"],
    "explored": state.get("explored", -1),
    "units": [[u["tile"], u["type"]] for u in state["units"] if u.get("p", 0) == 0],
}
if mode == "--update":
    json.dump(fp, open(sys.argv[2], "w"), indent=1)
    print("baseline updated:", fp)
    sys.exit(0)
golden = json.load(open(sys.argv[2]))
if fp != golden:
    print("FAIL: 静态指纹与基线不一致")
    print(" got:   ", json.dumps(fp))
    print(" golden:", json.dumps(golden))
    sys.exit(1)
print("ok: 静态指纹与基线一致")
PYEOF

# ---- 检查 2：存→读 roundtrip，fields diff 除噪声外必须为零 ----
python3 - "$WORK/f_before.txt" "$WORK/f_after.txt" "$NOISE" <<'PYEOF'
import sys
noise = {l.strip() for l in open(sys.argv[3]) if l.strip() and not l.startswith("#")}
def load(p):
    d = {}
    for line in open(p):
        line = line.rstrip("\n")
        if " = " in line:
            k, v = line.split(" = ", 1)
            d[k] = v
    return d
before, after = load(sys.argv[1]), load(sys.argv[2])
real = []
for k in sorted(set(before) | set(after)):
    if k in noise:
        continue
    if before.get(k) != after.get(k):
        real.append(f"  {k}: {before.get(k)} -> {after.get(k)}")
if real:
    print(f"FAIL: 存读 roundtrip 有 {len(real)} 处非噪声差异:")
    print("\n".join(real[:30]))
    sys.exit(1)
print("ok: 存读 roundtrip 干净（噪声外零差异）")
PYEOF

echo "REGRESS PASS"
