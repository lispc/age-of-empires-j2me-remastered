#!/bin/sh
# regress.sh — 黄金回归基线（重构安全网）。
#
# 流程：headless 进关（隔离 RMS，turbo 全速模拟）→
#   A. 静态阶段：选中/全图开关/光标移动（全部确定性动作）→ 取指纹与基线比对
#   B. 运动阶段：框选 + 移动单位（只防崩溃/卡死，不做断言——移动步数依赖
#      tick 数，随时钟抖动，位置天然非确定）
#   C. 存读 roundtrip：fields 前后夹存读档，噪声（regress-noise.txt）外零差异
#      roundtrip 放在进关后立即做：相机静止、无脚本活动，校验最严格
#
# 速度设计（2026-09-04 turbo 化，~5min → ~13s）：
#   - -Daoe.turbo=1：tight-loop 全速模拟，菜单导航/对话框推进不再受 40ms/tick 拖累；
#     场景动作全是事件驱动（until/tapk 自带确认重试），与 tick 速率无关。
#   - FIFO 放进 $WORK（每run唯一），不再占用固定路径 /tmp/aoe-regress.fifo，
#     多实例并行互不撞车。
#   - 收尾不等 DevHarness 的兜底计时器：f_after 落盘（日志回执）即主动 kill。
#     传给 DevHarness 的秒数只是防卡死上限，不是常态耗时。
#   - fifo 里的 "sleep N" 行从来就是 unknown-cmd 空操作（sleep 只在 script
#     指令内有效），真实节拍由 until/tapk/click 内部确认机制提供——保留原样，
#     不做语义清理。
#   - ⚠️ 若某次改动后静态指纹对不上 golden：先怀疑墙钟依赖泄漏进场景
#     （真发现），不许直接 --update 盖掉。
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
FIFO="$WORK/aoe.fifo"     # 每run唯一：多实例并行不撞固定路径
SAVES="$WORK/saves"
SCEN=tools/regress.scn
NOISE=tools/regress-noise.txt
GOLDEN=tools/regress.golden.json
STATE=$WORK/state.json
LOG=$WORK/run.log
trap 'rm -rf "$WORK"' EXIT

mkfifo "$FIFO"
mkdir -p "$WORK/userhome"
CP=build/classes/java/main:build/resources/main
# -Duser.home 指到临时目录：RMS(.nfo) 每次运行都是干净的——避免
# ~/.aoe-desktop 的跨会话污染（模式循环器位置漂移），保证指纹确定性。
# 载体用 tutorial:1：无 AI 噪声（随机地图 AI 走动，字段级确定性不可能）。
# -Daoe.turbo=1 tight-loop 全速模拟：场景全是事件驱动（until/tapk 确认重试），
# 提速只改 tick 墙钟速率，不改 tick 语义；指纹若因此对不上 golden = 墙钟依赖
# 泄漏进场景，属真发现，停下来查，不许 --update。
# DevHarness 尾参 60 只是防卡死上限（正常收尾靠下方 f_after 回执 + kill）。
"$JAVA" -Dapple.awt.UIElement=true -Daoe.debug=1 -Daoe.autoDismiss=1 -Daoe.headless=1 -Daoe.tickms=40 \
    -Daoe.turbo=1 \
    -Duser.home="$WORK/userhome" \
    -Daoe.dev=tutorial:1 -Daoe.devMouse="$FIFO" -Daoe.saveDir="$SAVES" \
    -cp "$CP" aoe.DevHarness "$WORK/final.png" 60 > "$LOG" 2>&1 &
PID=$!

# 等进关（state 命令打印 unit 行 = 任务态）
ready=0
i=0
while [ $i -lt 90 ]; do
    sleep 0.5
    i=$((i+1))
    echo "state" > "$FIFO" 2>/dev/null || true
    if grep -q "devMouse. p0 unit 0" "$LOG" 2>/dev/null; then ready=1; break; fi
    kill -0 $PID 2>/dev/null || break
done
if [ $ready -ne 1 ]; then
    echo "FAIL: 未进入任务态"; tail -5 "$LOG"; exit 1
fi
sleep 2        # autoDismiss 推掉开局对话（turbo 下 4 帧即推，2s 是宽余量）

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
sleep 4        # turbo 下单位几百 tick 即走完路程；4s 覆盖 drag 内部节拍+移动

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

# 收尾：等 f_after 落盘（写完才打回执日志），随后主动结束进程——
# 不傻等 DevHarness 的兜底计时器（turbo 前那里是 300s 的纯等待）。
i=0
while [ $i -lt 60 ]; do
    if grep -q "fields dumped to $WORK/f_after.txt" "$LOG" 2>/dev/null; then break; fi
    kill -0 $PID 2>/dev/null || break
    sleep 0.5
    i=$((i+1))
done
kill $PID 2>/dev/null || true
wait $PID 2>/dev/null || true

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
