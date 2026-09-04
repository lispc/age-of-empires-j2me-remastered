#!/bin/bash
# camloop.sh — 批量 headless 战役对局 + 胜率统计(战役 AI 迭代用)。ailoop.sh 的战役版。
#
# 与 ailoop 的差异:
#   - -Daoe.dev=campaign:<N> 进战役第 N 关(1..7,对应 missionIndex 0..6);
#   - 战役地图种子每局重掷(z=98 结算重掷,mapSeed pin 无效)——无需种子参数,
#     重复跑即方差采样;同图重打请用 devBoot 读档(tools/campaign/README.md);
#   - 默认 AI = aoe.ai.CampaignAi。
#
# 用法:
#   tools/camloop.sh [-n 局数] [-m 关卡1..7] [-a AI类名] [-t 每局超时秒] [-k] [-b]
#                    -k 保留每局日志(默认只留 summary.csv)  -b 开 BFS 寻路(默认开)
# 示例:
#   tools/camloop.sh -m 1 -n 5 -k
set -euo pipefail
cd "$(dirname "$0")/.."

JAVA=/opt/homebrew/opt/openjdk@17/bin/java
[ -x "$JAVA" ] || JAVA=java
CP=build/classes/java/main:build/resources/main

N=5; MISSION=1; AI=aoe.ai.CampaignAi; TIMEOUT=300; KEEP=0; BFS=1
usage() { sed -n '2,17p' "$0"; exit "${1:-1}"; }
while getopts "n:m:a:t:kbh" opt; do
    case $opt in
        n) N=$OPTARG ;; m) MISSION=$OPTARG ;; a) AI=$OPTARG ;;
        t) TIMEOUT=$OPTARG ;; k) KEEP=1 ;; b) BFS=0 ;;
        *) usage ;;
    esac
done

parse_result() {
    local line
    line=$(grep -oE '\[result\] (WIN|LOSS) ticks=[0-9]+' "$1" | tail -1 || true)
    if [ -n "$line" ]; then
        echo "$line" | awk '{print $2}' | tr -d '\n'
        echo -n " "
        echo "$line" | cut -d= -f2
    else
        echo "STALL -"
    fi
}

print_stats() {
python3 - "$1" <<'PYEOF'
import csv, statistics, sys
rows = [r for r in csv.DictReader(open(sys.argv[1])) if r.get("result")]
w = sum(1 for r in rows if r["result"] == "WIN")
l = sum(1 for r in rows if r["result"] == "LOSS")
s = sum(1 for r in rows if r["result"] == "STALL")
ticks = [int(r["ticks"]) for r in rows if r["ticks"] != "-"]
wall = [int(r["wallsec"]) for r in rows]
print("---- 统计 ----")
print(f"总局数: {len(rows)}  胜: {w}  负: {l}  僵持: {s}")
if w + l:
    print(f"胜率(决胜局): {100.0*w/(w+l):.1f}% ({w}/{w+l})   胜率(含僵持): {100.0*w/len(rows):.1f}%")
if ticks:
    print(f"ticks: 平均 {sum(ticks)/len(ticks):.0f}  中位 {statistics.median(ticks):.0f}  (n={len(ticks)})")
if wall:
    print(f"墙钟: 平均 {sum(wall)/len(wall):.1f}s/局  累计 {sum(wall)}s")
PYEOF
}

[ -d build/classes/java/main ] || { echo "缺少构建产物,先 ./gradlew classes" >&2; exit 1; }
RUNDIR=/tmp/aoe-camp/$(date +%Y%m%d-%H%M%S)-$$
CSV="$RUNDIR/summary.csv"
mkdir -p "$RUNDIR"
echo "game,mission,result,ticks,wallsec" > "$CSV"
PID=""
trap '[ -n "$PID" ] && kill "$PID" 2>/dev/null; true' EXIT

echo "rundir: $RUNDIR  (n=$N mission=$MISSION ai=${AI} timeout=${TIMEOUT}s bfs=$BFS)"
printf '%-5s %-7s %-8s %s\n' game result ticks wallsec
i=1
while [ $i -le "$N" ]; do
    gdir="$RUNDIR/game$i"
    mkdir -p "$gdir/saves" "$gdir/rms" "$gdir/userhome"
    log="$gdir/game.log"
    BFS_ARG=""
    [ "$BFS" = 1 ] && BFS_ARG="-Daoe.bfsPath=1"
    t0=$SECONDS
    "$JAVA" -Daoe.headless=1 "-Daoe.dev=campaign:$MISSION" -Daoe.turbo=1 -Daoe.noRender=1 \
        -Daoe.mute=1 -Daoe.debug=1 -Daoe.exitOnResult=1 \
        -Daoe.playerAi="$AI" ${BFS_ARG:+"$BFS_ARG"} \
        -Daoe.saveDir="$gdir/saves" -Daoe.rmsDir="$gdir/rms" \
        -Duser.home="$gdir/userhome" \
        -cp "$CP" aoe.Main > "$log" 2>&1 &
    PID=$!
    while kill -0 "$PID" 2>/dev/null; do
        if [ $((SECONDS - t0)) -ge "$TIMEOUT" ]; then
            kill "$PID" 2>/dev/null || true
            sleep 1; kill -9 "$PID" 2>/dev/null || true
            break
        fi
        sleep 1
    done
    wait "$PID" 2>/dev/null || true
    PID=""
    wall=$((SECONDS - t0))
    read -r res ticks <<< "$(parse_result "$log")"
    echo "$i,$MISSION,$res,$ticks,$wall" >> "$CSV"
    printf '%-5s %-7s %-8s %s\n' "$i" "$res" "$ticks" "$wall"
    i=$((i + 1))
done

print_stats "$CSV"
if [ "$KEEP" != 1 ]; then
    rm -rf "$RUNDIR"/game*
    echo "每局日志已清理(-k 可保留);CSV: $CSV"
else
    echo "每局日志保留在 $RUNDIR/game<i>/game.log"
fi
