#!/bin/bash
# m4v boot（r57 视频录制版）：m4q-boot.sh（r55 竞态修复版）+ 视频三旗标
#   -Daoe.reveal=1        迷雾全开（纯 paint 层，不碰模拟）
#   -Daoe.videoDir=$W/frames  逐帧导出 frame_%08d.png（每 10 tick 一帧）
#   -Daoe.resultHold=600  [result] 后多渲染 600t 拍 win 弹窗再退出
# 继承：campaign:5 + 新鲜 rmsDir + mapSeed=8224 + bfsPath=1 + tickms=10 +
#   base 落盘竞态修复（aA==6 才 save + >12KB 校验，aA!=6 才 -6）+
#   同一命令链拉起驱动 v7.1-p + 两支热修脚本（bs-hotfix 自门控：age>=1+mills+
#   25W20S 才放 BS；reseat-miners 连续复位 idle@矿区）。
# pkill 纪律：全部带 m4v 窄 pattern（java 经 devMouse=.../m4v/fifo 命中），
#   绝不全局 pkill aoe.Main（有并行会话）。
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
W=/tmp/aoe-camp/m4v
REPO=/Users/zhangzhuo/repos/personal/age-of-empires-j2me-remastered
N=${1:-1}   # boot 序号（drv 日志后缀）
pkill -f "m4v/m4pdrv.py" 2>/dev/null
pkill -f "m4v/bs-hotfix.py" 2>/dev/null
pkill -f "m4v/reseat-miners.py" 2>/dev/null
pkill -f "m4v/fifo" 2>/dev/null
sleep 1
rm -rf "$W/rms" "$W/saves" "$W/frames"; rm -f "$W/fifo" "$W/fifo.json" "$W/fifo.aistate.json" \
  "$W/play.log" "$W/base.aoesave" "$W/bs-hotfix.log" "$W/reseat-miners.log"
mkdir -p "$W/saves" "$W/rms" "$W/frames"
mkfifo "$W/fifo"
nohup /opt/homebrew/opt/openjdk@17/bin/java \
  -Dapple.awt.UIElement=true -Daoe.headless=1 -Daoe.dev=campaign:5 -Daoe.tickms=10 -Daoe.debug=1 \
  -Daoe.harnessQuiet=1 -Daoe.exitOnResult=1 \
  -Daoe.reveal=1 -Daoe.videoDir=$W/frames -Daoe.resultHold=600 \
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
# ---- base 落盘（r53 教训版 2）：单循环 aA=6 才发 save，save 后校验 >12KB；
# aA!=6（弹窗期）发 -6；不满足就再来。----
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
