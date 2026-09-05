#!/bin/bash
# m4q boot（r55 重录版，快照 v4 构建位精确验证局）：campaign:5 + 新鲜 rmsDir
# 继承 m4p boot（r53 胜利配方）：推简报→aA=6→save base.aoesave→同一命令链启动驱动
# r53 教训修正：base 必须在 aA=6 落盘（aA=2 弹窗态的 save 被拒，被迫用
# auto.aoesave@ar581 顶替）——本次改为「循环 -6 直到 aA=6 → save → 校验文件
# 存在且 >12KB，不满足则重发 save」。aA=6 后的 base 是 v4 档，AI 大脑随档。
# 热修双脚本（bs-hotfix/reseat-miners）由本脚本在驱动启动后一并拉起——
# r53 交棒清单「BS 建造支路/Mill 门/idle 跳格复位」未内化进 v7.1-p，且
# 多写者 fifo 已验证安全（PIPE_BUF 原子写）。命令全部 [fifo] 打点入 trace。
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
W=/tmp/aoe-camp/m4q
REPO=/Users/zhangzhuo/repos/personal/age-of-empires-j2me-remastered
N=${1:-1}   # boot 序号（drv 日志后缀）
pkill -f "aoe-camp/m4q/m4pdrv.py" 2>/dev/null
pkill -f "aoe-camp/m4q/bs-hotfix.py" 2>/dev/null
pkill -f "aoe-camp/m4q/reseat-miners.py" 2>/dev/null
pkill -f aoe.Main 2>/dev/null
sleep 1
rm -rf "$W/rms" "$W/saves"; rm -f "$W/fifo" "$W/fifo.json" "$W/fifo.aistate.json" \
  "$W/play.log" "$W/base.aoesave" "$W/bs-hotfix.log" "$W/reseat-miners.log"
mkdir -p "$W/saves" "$W/rms"
mkfifo "$W/fifo"
nohup /opt/homebrew/opt/openjdk@17/bin/java \
  -Dapple.awt.UIElement=true -Daoe.headless=1 -Daoe.dev=campaign:5 -Daoe.tickms=10 -Daoe.debug=1 \
  -Daoe.harnessQuiet=1 -Daoe.exitOnResult=1 \
  -Daoe.saveDir=$W/saves -Daoe.rmsDir=$W/rms -Daoe.mapSeed=8224 -Daoe.bfsPath=1 \
  -Daoe.devMouse=$W/fifo \
  -cp $REPO/build/classes/java/main:$REPO/build/resources/main \
  aoe.Main > "$W/play.log" 2>&1 &
PID=$!
disown
echo "java pid=$PID"
for i in $(seq 1 60); do
  grep -q 'in mission' "$W/play.log" && break
  grep -q 'Exception\|Error' "$W/play.log" && { echo BOOT_FAIL; tail -20 "$W/play.log"; exit 1; }
  sleep 1
done
grep -q 'in mission' "$W/play.log" || { echo BOOT_TIMEOUT; tail -20 "$W/play.log"; exit 1; }
# ---- base 落盘（r53 教训版 2）：简报弹窗进任务 ~100t 后才弹出（aA 6→2），
# 「先推到 6 再 save」有窗口竞态——改为单循环：aA=6 才发 save，save 后校验
# 文件存在且 >12KB；aA!=6（含弹窗期）发 -6；不满足条件就再来。----
SAVED=""
AA=''
for i in $(seq 1 40); do
  echo 'state' > "$W/fifo"; sleep 1
  AA=$(python3 -c "import json;print(json.load(open('$W/fifo.json'))['aA'])" 2>/dev/null || echo '')
  if [ "$AA" = "6" ]; then
    echo "save $W/base.aoesave" > "$W/fifo"; sleep 1.5
    if [ -f "$W/base.aoesave" ]; then
      SZ=$(stat -f%z "$W/base.aoesave" 2>/dev/null || echo 0)
      [ "$SZ" -gt 12288 ] && { SAVED=1; break; }
    fi
    echo "save 校验未过（重试 $i）"
  else
    echo "key -6" > "$W/fifo"; sleep 1
  fi
done
[ -n "$SAVED" ] || { echo BOOT_FAIL_SAVE "aA=$AA"; tail -8 "$W/play.log"; exit 1; }
echo "base.aoesave OK $(stat -f%z "$W/base.aoesave") bytes"
# ---- 同一命令链：立刻启动驱动（v7.1-p）+ 两支热修脚本 ----
M4D_WORK=$W M4F_BOOTN=$N M4D_CAMPFIRST=${M4D_CAMPFIRST:-1} M4D_WFIX=${M4D_WFIX:-b2} \
  nohup python3 $W/m4pdrv.py > "$W/drv$N.log" 2>&1 &
DPID=$!
disown
echo "driver pid=$DPID (drv$N.log)"
nohup python3 $W/bs-hotfix.py > "$W/bs-hotfix.log" 2>&1 &
disown
nohup python3 $W/reseat-miners.py > "$W/reseat-miners.log" 2>&1 &
disown
echo "hotfix pids launched (bs-hotfix.log / reseat-miners.log)"
BASE_AR=$(grep -o 'base\.aoesave.*ar=[0-9]*' "$W/play.log" | grep -o 'ar=[0-9]*' | tail -1 | cut -d= -f2)
echo 'state' > "$W/fifo"; sleep 1
python3 - "$BASE_AR" "$W" <<'PYEOF'
import json, sys
d = json.load(open(sys.argv[2] + '/fifo.json'))
print('BASE_AR', sys.argv[1], 'state_ar', d['ar'], 'aA', d['aA'], 'res', d['res'], 'pop', d['pop'])
print('BOOT_OK')
PYEOF
