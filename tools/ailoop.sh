#!/bin/bash
# ailoop.sh — 批量 headless 随机图对局 + 胜率统计(玩家 AI 迭代用)。
#
# JVM 属性契约(游戏侧实现,本脚本只消费):
#   -Daoe.dev=random:<1|2|3>  难度 1=Easy 2=Medium 3=Expert
#   -Daoe.turbo=1             tight-loop 全速模拟(不等 80ms)
#   -Daoe.noRender=1          跳过渲染
#   -Daoe.mute=1              静音
#   -Daoe.exitOnResult=1      对局结束(一方 TC 毁/玩家全灭)时 stdout 打一行
#                             "[result] WIN ticks=<N>" 或 "[result] LOSS ticks=<N>"
#                             然后进程自退;N = 游戏 tick 数(80ms/tick 游戏时间)
#   -Daoe.mapSeed=N           随机图种子(属性名若变,改 SEED_PROP 或 env AOE_SEED_PROP 覆盖)
#   -Daoe.playerAi=<类名>     玩家 AI 全限定类名(缺省=玩家站桩,最终必出 LOSS)
#
# 磁盘纪律:每局独立 /tmp 子目录做 saveDir/rmsDir/user.home(隔离 ~/.aoe-desktop
# 的 RMS),绝不碰用户真实存档。
#
# 用法:
#   tools/ailoop.sh [-n 局数] [-d 难度] [-a AI类名] [-s 起始种子] [-t 每局超时秒] [-k] [-b] [-f] [-S N] [-x 种子表]
#                   -k 保留每局日志(默认跑完只留 summary.csv)
#                   -b 开 BFS 寻路(-Daoe.bfsPath=1,部队机动明显改善;默认关=原版行为)
#                   -f 关迷雾诚实模式(-Daoe.aiFog=0,回退全图;默认开=只读已探索格敌情)
#                   -S N 周期快照(-Daoe.snapshotEvery=N,每 N tick 存 snap-<tick>.aoesave,
#                        滚动留最新 8 份;败局尸检用,默认关)
#                   -x 逗号分隔的跳过种子表(叠加在 tools/ailoop-skip.txt 之上);
#                        跳过表=已知退化图(如 1004:无可达金矿+敌TC被围死,必 STALL
#                        白烧超时),被跳过的种子不占局数、不进 CSV
#   PHASE_STEP=N(默认 7)|off:第 i 局传 -Daoe.devPhase=(i-1)*N——进关相位 pin
#                        (tickCount 不随任务重置,菜单导航墙钟漂移会让同种子局
#                        走向不同;pin 后同种子同相位必同结果,A/B 逐对对比)。
#                        off 恢复旧的墙钟漂移行为
#   tools/ailoop.sh --selftest  假日志自检解析/统计逻辑(不跑游戏,无需构建产物)
# 默认:n=10 d=1 种子1000起每局+1 超时300s。-a 必填(无 AI=站桩必败,防空跑)。
# 示例:
#   tools/ailoop.sh -n 20 -d 2 -a aoe.ai.SimpleAi -k
#   AOE_SEED_PROP=aoe.mapSeed tools/ailoop.sh -n 5 -t 120
set -euo pipefail
cd "$(dirname "$0")/.."

JAVA=/opt/homebrew/opt/openjdk@17/bin/java
[ -x "$JAVA" ] || JAVA=java
CP=build/classes/java/main:build/resources/main
SEED_PROP=${AOE_SEED_PROP:-aoe.mapSeed}   # 种子属性名开关(契约未定时便于调整)

# --selftest 在 getopts 之前拦截(getopts 不认识长选项)
SELFTEST=0
[ "${1:-}" = "--selftest" ] && SELFTEST=1

# ---- 参数 ----
N=10; DIFF=1; AI=""; SEED0=1000; TIMEOUT=300; KEEP=0; BFS=0; SNAP=0; SKIPX=""; FOGOFF=0
usage() { sed -n '2,40p' "$0"; exit "${1:-1}"; }
[ $SELFTEST = 0 ] && while getopts "n:d:a:s:t:kbS:x:fh" opt; do
    case $opt in
        n) N=$OPTARG ;; d) DIFF=$OPTARG ;; a) AI=$OPTARG ;;
        s) SEED0=$OPTARG ;; t) TIMEOUT=$OPTARG ;; k) KEEP=1 ;;
        b) BFS=1 ;;
        f) FOGOFF=1 ;;
        S) SNAP=$OPTARG ;;
        x) SKIPX=$OPTARG ;;
        *) usage ;;
    esac
done

# 无 -a = 玩家站桩裸奔,批测结果全是 0%(2026-09-05 实踩:白烧一批 20 局)。
# 除了 --selftest,显式拒绝缺省,宁可报错不可出假数据。
if [ $SELFTEST = 0 ] && [ -z "$AI" ]; then
    echo "错误: 缺 -a <AI类名>(如 -a aoe.ai.RuleBasedAi);不带 AI 跑批是无效数据" >&2
    exit 2
fi

# ---- 退化种子跳过表:tools/ailoop-skip.txt(一行一个,#注释)+ -x 叠加 ----
SKIP=" "
[ -f tools/ailoop-skip.txt ] && while IFS= read -r line; do
    line=${line%%#*}; line=$(echo "$line" | tr -d '[:space:]')
    [ -n "$line" ] && SKIP="$SKIP$line "
done < tools/ailoop-skip.txt
[ -n "$SKIPX" ] && SKIP="$SKIP$(echo "$SKIPX" | tr ',' ' ') "
is_skipped() { case "$SKIP" in *" $1 "*) return 0;; *) return 1;; esac; }

# ---- 解析:从一局日志提取结果,输出 "<WIN|LOSS|STALL> <ticks|->" ----
# 取最后一行 [result](防重复打印);无 [result] 行 = STALL(超时被杀/异常退出)。
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

# ---- 统计:读 summary.csv 打印胜/负/僵持、胜率、平均/中位 ticks、平均墙钟 ----
print_stats() {  # $1=summary.csv
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

# ---- 自检:假日志验证 parse_result / print_stats,不碰游戏 ----
selftest() {
    local dir rc=0
    dir=$(mktemp -d /tmp/aoe-ailoop-selftest.XXXXXX)
    trap "rm -rf '$dir'" EXIT
    # game1: 正常 WIN,带干扰行;game2: LOSS;game3: 无 result → STALL;
    # game4: 两行 result(取最后一行 WIN)
    printf '[dbg] heartbeat\nsome [result]ish decoy\n[result] WIN ticks=12345\n' > "$dir/g1.log"
    printf '[trace] g->6\n[result] LOSS ticks=6789\n' > "$dir/g2.log"
    printf '[dbg] still running, killed by timeout\n' > "$dir/g3.log"
    printf '[result] LOSS ticks=50\n[result] WIN ticks=100\n' > "$dir/g4.log"
    echo "game,seed,result,ticks,wallsec" > "$dir/summary.csv"
    local i r t
    for i in 1 2 3 4; do
        read -r r t <<< "$(parse_result "$dir/g$i.log")"
        echo "$i,$((1000+i-1)),$r,$t,$((i*7))" >> "$dir/summary.csv"
    done
    cat "$dir/summary.csv"
    local out
    out=$(print_stats "$dir/summary.csv")
    echo "$out"
    # 期望: 胜2 负1 僵持1;决胜胜率 66.7%;ticks 平均 6411 中位 6789;墙钟平均 17.5s
    for want in "胜: 2" "负: 1" "僵持: 1" "66.7%" "平均 6411" "中位 6789" "平均 17.5s"; do
        grep -qF "$want" <<< "$out" || { echo "SELFTEST FAIL: 缺 '$want'"; rc=1; }
    done
    [ "$(parse_result "$dir/g4.log")" = "WIN 100" ] || { echo "SELFTEST FAIL: 多 result 行取值错"; rc=1; }
    [ $rc -eq 0 ] && echo "SELFTEST PASS"
    return $rc
}
[ $SELFTEST = 1 ] && { selftest; exit; }

# ---- 正式跑 ----
[ -d build/classes/java/main ] || { echo "缺少构建产物,先 ./gradlew classes" >&2; exit 1; }
RUNDIR=/tmp/aoe-ai/$(date +%Y%m%d-%H%M%S)-$$
CSV="$RUNDIR/summary.csv"
mkdir -p "$RUNDIR"
echo "game,seed,result,ticks,wallsec" > "$CSV"
PID=""
trap '[ -n "$PID" ] && kill "$PID" 2>/dev/null; true' EXIT

echo "rundir: $RUNDIR  (n=$N diff=$DIFF ai=${AI:-无} seed=$SEED0+ timeout=${TIMEOUT}s)"
printf '%-5s %-6s %-7s %-8s %s\n' game seed result ticks wallsec
i=1
seed=$SEED0
while [ $i -le "$N" ]; do
    if is_skipped "$seed"; then
        printf '%-5s %-6s %-7s\n' "-" "$seed" "SKIP(退化表)"
        seed=$((seed + 1)); continue
    fi
    gdir="$RUNDIR/game$i"
    mkdir -p "$gdir/saves" "$gdir/rms" "$gdir/userhome"
    log="$gdir/game.log"
    AI_ARG=""
    [ -n "$AI" ] && AI_ARG="-Daoe.playerAi=$AI"
    BFS_ARG=""
    [ "$BFS" = 1 ] && BFS_ARG="-Daoe.bfsPath=1"
    SNAP_ARG=""
    [ "$SNAP" -gt 0 ] 2>/dev/null && SNAP_ARG="-Daoe.snapshotEvery=$SNAP"
    FOG_ARG=""
    [ "$FOGOFF" = 1 ] && FOG_ARG="-Daoe.aiFog=0"
    # 消融诊断：AOE_AIFOG=res = 资源全图+敌情诚实（覆盖 -f）
    [ -n "${AOE_AIFOG:-}" ] && FOG_ARG="-Daoe.aiFog=$AOE_AIFOG"
    PHASE_ARG=""
    if [ "${PHASE_STEP:-7}" != "off" ]; then
        PHASE_ARG="-Daoe.devPhase=$(( (i - 1) * ${PHASE_STEP:-7} ))"
    fi
    t0=$SECONDS
    "$JAVA" -Dapple.awt.UIElement=true -Daoe.headless=1 "-Daoe.dev=random:$DIFF" -Daoe.turbo=1 -Daoe.noRender=1 \
        -Daoe.mute=1 -Daoe.debug=1 -Daoe.exitOnResult=1 "-D$SEED_PROP=$seed" \
        ${AI_ARG:+"$AI_ARG"} ${BFS_ARG:+"$BFS_ARG"} ${SNAP_ARG:+"$SNAP_ARG"} ${FOG_ARG:+"$FOG_ARG"} ${PHASE_ARG:+"$PHASE_ARG"} \
        -Daoe.saveDir="$gdir/saves" -Daoe.rmsDir="$gdir/rms" \
        -Duser.home="$gdir/userhome" \
        -cp "$CP" aoe.Main > "$log" 2>&1 &
    PID=$!
    # 等进程自退(exitOnResult);超时未出 [result] → 杀,记 STALL
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
    echo "$i,$seed,$res,$ticks,$wall" >> "$CSV"
    printf '%-5s %-6s %-7s %-8s %s\n' "$i" "$seed" "$res" "$ticks" "$wall"
    i=$((i + 1))
    seed=$((seed + 1))
done

print_stats "$CSV"
if [ "$KEEP" != 1 ]; then
    rm -rf "$RUNDIR"/game*
    echo "每局日志已清理(-k 可保留);CSV: $CSV"
else
    echo "每局日志保留在 $RUNDIR/game<i>/game.log"
fi
