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
#   tools/ailoop.sh [-n 局数] [-d 难度] [-a AI类名] [-s 起始种子] [-t 每局超时秒] [-k] [-b] [-f] [-S N] [-x 种子表] [-m name=value]
#                   -k 保留每局日志(默认跑完只留 summary.csv)
#                   -b 开 BFS 寻路(-Daoe.bfsPath=1,部队机动明显改善;默认关=原版行为)
#                   -f 关迷雾诚实模式(-Daoe.aiFog=0,回退全图;默认开=只读已探索格敌情)
#                   -S N 周期快照(-Daoe.snapshotEvery=N,每 N tick 存 snap-<tick>.aoesave,
#                        滚动留最新 8 份;败局尸检用,默认关)
#                   -x 逗号分隔的跳过种子表(叠加在 tools/ailoop-skip.txt 之上);
#                        跳过表=已知退化图(如 1004:无可达金矿+敌TC被围死,必 STALL
#                        白烧超时),被跳过的种子不占局数、不进 CSV
#                   -T N 覆盖敌方 AI 进攻阈值(-Daoe.aiAttackThreshold,阈值矩阵
#                        批测用;省略=难度默认 Easy50/Medium60/Expert100)
#                   -e <AI类名> 敌 AI 反串(-Daoe.enemyAi=<类名>,抑制引擎 tickAi,
#                        见 aoe/ai/README.md「EnemyAi」节;省略=引擎原版敌 AI)
#                   -m name=value  镜像配对模式(自对弈竞技场,规格
#                        docs/research/selfplay-arena.md):每个种子跑两局——一局
#                        -Daoe.<name>.p0=<value>(candidate 在 side 0)、一局
#                        .p1(candidate 在 side 1),同相位 pin;自动带
#                        -Daoe.arena=1,两侧默认都挂 RuleBasedAi(-a/-e 可覆盖类名,
#                        镜像模式免 -a 必填)。-m "-"=无 candidate 的纯镜像基线
#                        (G0 vs G0 对称性测量)。CSV 加 cand 列(p0/p1),result
#                        列支持 DRAW;summary 增 candidate 合并胜场(DRAW 各计半)
#                        +逐侧拆分+side0 胜场(side 偏差)。
#   PHASE_STEP=N(默认 7)|off:第 i 局传 -Daoe.devPhase=(i-1)*N——进关相位 pin
#                        (tickCount 不随任务重置,菜单导航墙钟漂移会让同种子局
#                        走向不同;pin 后同种子同相位必同结果,A/B 逐对对比)。
#                        off 恢复旧的墙钟漂移行为
#   tools/ailoop.sh --selftest  假日志自检解析/统计逻辑(不跑游戏,无需构建产物)
# 默认:n=10 d=1 种子1000起每局+1 超时300s。-a 必填(无 AI=站桩必败,防空跑;
# 镜像模式 -m 例外,自动双侧挂 RuleBasedAi)。
# 示例:
#   tools/ailoop.sh -n 20 -d 2 -a aoe.ai.SimpleAi -k
#   AOE_SEED_PROP=aoe.mapSeed tools/ailoop.sh -n 5 -t 120
#   tools/ailoop.sh -m aiK.towerW=18 -n 10 -d 2 -k -b   # 镜像批:candidate 双侧各一
#   tools/ailoop.sh -m - -n 10 -d 2 -k -b               # G0 vs G0 纯镜像基线
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
N=10; DIFF=1; AI=""; EAI=""; SEED0=1000; TIMEOUT=300; KEEP=0; BFS=0; SNAP=0; SKIPX=""; FOGOFF=0; ATKTHR=""; MIRROR=""
usage() { sed -n '2,52p' "$0"; exit "${1:-1}"; }
[ $SELFTEST = 0 ] && while getopts "n:d:a:e:s:t:kbS:x:fT:m:h" opt; do
    case $opt in
        n) N=$OPTARG ;; d) DIFF=$OPTARG ;; a) AI=$OPTARG ;; e) EAI=$OPTARG ;;
        s) SEED0=$OPTARG ;; t) TIMEOUT=$OPTARG ;; k) KEEP=1 ;;
        b) BFS=1 ;;
        f) FOGOFF=1 ;;
        S) SNAP=$OPTARG ;;
        x) SKIPX=$OPTARG ;;
        T) ATKTHR=$OPTARG ;;
        m) MIRROR=$OPTARG ;;
        *) usage ;;
    esac
done

# 镜像模式（-m）：自动带 arena 主开关，两侧默认都挂 RuleBasedAi（-a/-e 覆盖）。
if [ -n "$MIRROR" ]; then
    AI=${AI:-aoe.ai.RuleBasedAi}
    EAI=${EAI:-aoe.ai.RuleBasedAi}
fi

# 无 -a = 玩家站桩裸奔,批测结果全是 0%(2026-09-05 实踩:白烧一批 20 局)。
# 除了 --selftest 与镜像模式（自动双侧挂 RB）,显式拒绝缺省,宁可报错不可出假数据。
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

# ---- 解析:从一局日志提取结果,输出 "<WIN|LOSS|DRAW|STALL> <ticks|->" ----
# 取最后一行 [result](防重复打印);无 [result] 行 = STALL(超时被杀/异常退出)。
# DRAW = arena 和局(tickCount>50000,c.java onPaint 帧首判定)。
parse_result() {
    local line
    line=$(grep -oE '\[result\] (WIN|LOSS|DRAW) ticks=[0-9]+' "$1" | tail -1 || true)
    if [ -n "$line" ]; then
        echo "$line" | awk '{print $2}' | tr -d '\n'
        echo -n " "
        echo "$line" | cut -d= -f2
    else
        echo "STALL -"
    fi
}

# ---- 统计:读 summary.csv 打印胜/负/和/僵持、胜率、平均/中位 ticks、平均墙钟 ----
# 镜像模式(CSV 带 cand 列=p0/p1):增打 candidate 合并胜场(DRAW 计半)+逐侧拆分
# +side0 胜场(side 偏差——G0 vs G0 时应≈50%)。cand=p0 局 WIN=candidate 胜;
# cand=p1 局 LOSS(side0 负)=candidate 胜。
print_stats() {  # $1=summary.csv
python3 - "$1" <<'PYEOF'
import csv, statistics, sys
rows = [r for r in csv.DictReader(open(sys.argv[1])) if r.get("result")]
w = sum(1 for r in rows if r["result"] == "WIN")
l = sum(1 for r in rows if r["result"] == "LOSS")
d = sum(1 for r in rows if r["result"] == "DRAW")
s = sum(1 for r in rows if r["result"] == "STALL")
ticks = [int(r["ticks"]) for r in rows if r["ticks"] != "-"]
wall = [int(r["wallsec"]) for r in rows]
print("---- 统计 ----")
print(f"总局数: {len(rows)}  胜: {w}  负: {l}" + (f"  和: {d}" if d else "") + f"  僵持: {s}")
if w + l:
    print(f"胜率(决胜局,side0 视角): {100.0*w/(w+l):.1f}% ({w}/{w+l})   胜率(含和僵): {100.0*w/len(rows):.1f}%")
if ticks:
    print(f"ticks: 平均 {sum(ticks)/len(ticks):.0f}  中位 {statistics.median(ticks):.0f}  (n={len(ticks)})")
if wall:
    print(f"墙钟: 平均 {sum(wall)/len(wall):.1f}s/局  累计 {sum(wall)}s")
mirror = any((r.get("cand") or "") in ("p0", "p1") for r in rows)
if mirror:
    def score(rs, cand_is_p0):
        cw = sum(1 for r in rs if r["result"] == ("WIN" if cand_is_p0 else "LOSS"))
        cl = sum(1 for r in rs if r["result"] == ("LOSS" if cand_is_p0 else "WIN"))
        cd = sum(1 for r in rs if r["result"] == "DRAW")
        cs = sum(1 for r in rs if r["result"] == "STALL")
        return cw, cl, cd, cs
    p0 = [r for r in rows if r.get("cand") == "p0"]
    p1 = [r for r in rows if r.get("cand") == "p1"]
    w0, l0, d0, s0 = score(p0, True)
    w1, l1, d1, s1 = score(p1, False)
    cw, cl, cd, cs = w0 + w1, l0 + l1, d0 + d1, s0 + s1
    pts = cw + 0.5 * cd
    print("---- 镜像统计（candidate 视角，DRAW 各计半）----")
    print(f"candidate 合并: {pts:.1f}/{len(rows)}  (胜 {cw} 负 {cl} 和 {cd} 僵持 {cs})")
    print(f"逐侧拆分: candidate@p0 {w0}-{l0}-{d0}" + (f"+{s0}僵" if s0 else "")
        + f"  candidate@p1 {w1}-{l1}-{d1}" + (f"+{s1}僵" if s1 else "") + "  (胜-负-和)")
    side0w = sum(1 for r in rows if r["result"] == "WIN")
    side0pts = side0w + 0.5 * d
    print(f"side0 合并得分: {side0pts:.1f}/{len(rows)} ({100.0*side0pts/len(rows):.1f}%——对称镜像应≈50%,残差=side 偏差)")
PYEOF
}

# ---- 自检:假日志验证 parse_result / print_stats,不碰游戏 ----
selftest() {
    local dir rc=0
    dir=$(mktemp -d /tmp/aoe-ailoop-selftest.XXXXXX)
    trap "rm -rf '$dir'" EXIT
    # game1: 正常 WIN,带干扰行;game2: LOSS;game3: 无 result → STALL;
    # game4: 两行 result(取最后一行 WIN);game5: arena DRAW
    printf '[dbg] heartbeat\nsome [result]ish decoy\n[result] WIN ticks=12345\n' > "$dir/g1.log"
    printf '[trace] g->6\n[result] LOSS ticks=6789\n' > "$dir/g2.log"
    printf '[dbg] still running, killed by timeout\n' > "$dir/g3.log"
    printf '[result] LOSS ticks=50\n[result] WIN ticks=100\n' > "$dir/g4.log"
    printf '[result] DRAW ticks=50001\n' > "$dir/g5.log"
    echo "game,seed,result,ticks,wallsec" > "$dir/summary.csv"
    local i r t
    for i in 1 2 3 4 5; do
        read -r r t <<< "$(parse_result "$dir/g$i.log")"
        echo "$i,$((1000+i-1)),$r,$t,$((i*7))" >> "$dir/summary.csv"
    done
    cat "$dir/summary.csv"
    local out
    out=$(print_stats "$dir/summary.csv")
    echo "$out"
    # 期望: 胜2 负1 和1 僵持1;决胜胜率 66.7%;ticks 平均 17309 中位 9567;墙钟平均 21.0s
    for want in "胜: 2" "负: 1" "和: 1" "僵持: 1" "66.7%" "平均 17309" "中位 9567" "平均 21.0s"; do
        grep -qF "$want" <<< "$out" || { echo "SELFTEST FAIL: 缺 '$want'"; rc=1; }
    done
    [ "$(parse_result "$dir/g4.log")" = "WIN 100" ] || { echo "SELFTEST FAIL: 多 result 行取值错"; rc=1; }
    [ "$(parse_result "$dir/g5.log")" = "DRAW 50001" ] || { echo "SELFTEST FAIL: DRAW 解析错"; rc=1; }
    # 镜像模式统计自检:cand=p0 局 WIN=candidate 胜;cand=p1 局 LOSS(side0 负)=candidate 胜
    echo "game,seed,result,ticks,wallsec,cand" > "$dir/mirror.csv"
    printf '1,1000,WIN,5000,10,p0\n2,1000,LOSS,6000,10,p1\n3,1001,DRAW,50001,20,p0\n4,1001,WIN,7000,10,p1\n' >> "$dir/mirror.csv"
    local mout
    mout=$(print_stats "$dir/mirror.csv")
    echo "$mout"
    # candidate: g1(p0 WIN)=胜、g2(p1 LOSS=side0 负)=胜、g3=半、g4(p1 WIN=side0 胜)=负
    # → 2.5/4;side0: g1+g4 两胜+半和 → 2.5/4=62.5%
    for want in "candidate 合并: 2.5/4" "candidate@p0 1-0-1" "candidate@p1 1-1-0" "side0 合并得分: 2.5/4 (62.5%"; do
        grep -qF "$want" <<< "$mout" || { echo "SELFTEST FAIL: 镜像统计缺 '$want'"; rc=1; }
    done
    [ $rc -eq 0 ] && echo "SELFTEST PASS"
    return $rc
}
[ $SELFTEST = 1 ] && { selftest; exit; }

# ---- 正式跑 ----
[ -d build/classes/java/main ] || { echo "缺少构建产物,先 ./gradlew classes" >&2; exit 1; }
RUNDIR=/tmp/aoe-ai/$(date +%Y%m%d-%H%M%S)-$$
CSV="$RUNDIR/summary.csv"
mkdir -p "$RUNDIR"
# 镜像模式 CSV 加 cand 列(p0/p1=candidate 所在侧);普通模式保持原 5 列契约。
if [ -n "$MIRROR" ]; then
    echo "game,seed,result,ticks,wallsec,cand" > "$CSV"
else
    echo "game,seed,result,ticks,wallsec" > "$CSV"
fi
PID=""
trap '[ -n "$PID" ] && kill "$PID" 2>/dev/null; true' EXIT

echo "rundir: $RUNDIR  (n=$N diff=$DIFF ai=${AI:-无} enemyAi=${EAI:-引擎原版} seed=$SEED0+ timeout=${TIMEOUT}s${MIRROR:+ mirror=$MIRROR arena=1})"
printf '%-5s %-6s %-7s %-8s %s\n' game seed result ticks wallsec
i=1
pair=0
seed=$SEED0
# N = 种子数（镜像模式每种子两局=candidate 在 p0/p1 各一,同相位 pin）。
while [ $pair -lt "$N" ]; do
    if is_skipped "$seed"; then
        printf '%-5s %-6s %-7s\n' "-" "$seed" "SKIP(退化表)"
        seed=$((seed + 1)); continue
    fi
    pair=$((pair + 1))
    SIDES="x"
    [ -n "$MIRROR" ] && SIDES="p0 p1"
    for cside in $SIDES; do
        gdir="$RUNDIR/game$i"
        mkdir -p "$gdir/saves" "$gdir/rms" "$gdir/userhome"
        log="$gdir/game.log"
        AI_ARG=""
        [ -n "$AI" ] && AI_ARG="-Daoe.playerAi=$AI"
        EAI_ARG=""
        [ -n "$EAI" ] && EAI_ARG="-Daoe.enemyAi=$EAI"
        BFS_ARG=""
        [ "$BFS" = 1 ] && BFS_ARG="-Daoe.bfsPath=1"
        SNAP_ARG=""
        [ "$SNAP" -gt 0 ] 2>/dev/null && SNAP_ARG="-Daoe.snapshotEvery=$SNAP"
        FOG_ARG=""
        [ "$FOGOFF" = 1 ] && FOG_ARG="-Daoe.aiFog=0"
        # 消融诊断：AOE_AIFOG=res = 资源全图+敌情诚实（覆盖 -f）
        [ -n "${AOE_AIFOG:-}" ] && FOG_ARG="-Daoe.aiFog=$AOE_AIFOG"
        # 阈值矩阵：-T N 覆盖敌方 AI 进攻阈值（c.java 测试钩子）
        THR_ARG=""
        [ -n "$ATKTHR" ] && THR_ARG="-Daoe.aiAttackThreshold=$ATKTHR"
        # 镜像模式：arena 主开关 + candidate 旋钮按侧分配（-m "-"=无 candidate
        # 纯镜像基线）。cside=p0 → -Daoe.<name>.p0=<value>;p1 同理。
        ARENA_ARG=""
        CAND_ARG=""
        if [ -n "$MIRROR" ]; then
            ARENA_ARG="-Daoe.arena=1"
            if [ "$MIRROR" != "-" ]; then
                CAND_ARG="-Daoe.${MIRROR%%=*}.$cside=${MIRROR#*=}"
            fi
        fi
        # 旋钮透传：EXTRA_D="-Daoe.xxx=1 -Daoe.yyy=2"（AI 研究用旋钮 A/B）。
        # ⚠️ 必须无引号展开："$EXTRA_ARG" 会把多个 -D 并成一个 argv，JVM 把整串当
        # 单属性值 → Integer.parseInt 炸 → ExceptionInInitializerError → AI 裸奔假局
        # （2026-09-06 实测：双旋钮筛选三屏全废）。-D 值含空格的场景不支持。
        EXTRA_ARG=""
        [ -n "${EXTRA_D:-}" ] && EXTRA_ARG=${EXTRA_D}
        PHASE_ARG=""
        if [ "${PHASE_STEP:-7}" != "off" ]; then
            # 镜像模式相位按种子对 pin（同种子两局同相位=同形状成对对比）
            PHASE_ARG="-Daoe.devPhase=$(( (pair - 1) * ${PHASE_STEP:-7} ))"
        fi
        t0=$SECONDS
        "$JAVA" -Dapple.awt.UIElement=true -Daoe.headless=1 "-Daoe.dev=random:$DIFF" -Daoe.turbo=1 -Daoe.noRender=1 \
            -Daoe.mute=1 -Daoe.debug=1 -Daoe.exitOnResult=1 "-D$SEED_PROP=$seed" \
            ${AI_ARG:+"$AI_ARG"} ${EAI_ARG:+"$EAI_ARG"} ${BFS_ARG:+"$BFS_ARG"} ${SNAP_ARG:+"$SNAP_ARG"} ${FOG_ARG:+"$FOG_ARG"} ${THR_ARG:+"$THR_ARG"} ${ARENA_ARG:+"$ARENA_ARG"} ${CAND_ARG:+"$CAND_ARG"} ${EXTRA_ARG:+$EXTRA_ARG} ${PHASE_ARG:+"$PHASE_ARG"} \
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
        if [ -n "$MIRROR" ]; then
            echo "$i,$seed,$res,$ticks,$wall,$cside" >> "$CSV"
            printf '%-5s %-6s %-7s %-8s %s\n' "$i($cside)" "$seed" "$res" "$ticks" "$wall"
        else
            echo "$i,$seed,$res,$ticks,$wall" >> "$CSV"
            printf '%-5s %-6s %-7s %-8s %s\n' "$i" "$seed" "$res" "$ticks" "$wall"
        fi
        i=$((i + 1))
    done
    seed=$((seed + 1))
done

print_stats "$CSV"
if [ "$KEEP" != 1 ]; then
    rm -rf "$RUNDIR"/game*
    echo "每局日志已清理(-k 可保留);CSV: $CSV"
else
    echo "每局日志保留在 $RUNDIR/game<i>/game.log"
fi
