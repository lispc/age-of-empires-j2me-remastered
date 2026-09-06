#!/bin/bash
# m2v2 主线 boot（经济关 missionIndex 2，r70 重录）：campaign:3 + 新鲜 rmsDir → idx2。
# base 落盘防呆（aA=2→key -6→aA=6→save→复查>12KB）后挂 m2v2drv.py。
# 红线：pkill 只用窄 pattern "aoe-camp/<work basename>"。
# 用法: boot.sh <boot序号> [workdir]   (workdir 缺省 /tmp/aoe-camp/m2v2;
#       彩排/探针传 /tmp/aoe-camp/m2v2-probe)
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
N=${1:-1}
W=${2:-/tmp/aoe-camp/m2v2}
REPO=/Users/zhangzhuo/repos/personal/age-of-empires-j2me-remastered
PAT=$(basename "$W")
pkill -f "aoe-camp/$PAT" 2>/dev/null
pkill -f "m2v2drv.py $W" 2>/dev/null
sleep 1
[ -f "$W/play.log" ] && cp "$W/play.log" "$W/play-b$((N-1)).log"
rm -rf "$W/rms" "$W/saves"; rm -f "$W/fifo" "$W/fifo.json" "$W/fifo.aistate.json" \
  "$W/base.aoesave" "$W/mapdump.txt" "$W/md2.txt" "$W/md3.txt"
mkdir -p "$W/saves" "$W/rms"
mkfifo "$W/fifo"
nohup /opt/homebrew/opt/openjdk@17/bin/java \
  -Dapple.awt.UIElement=true -Daoe.headless=1 -Daoe.dev=campaign:3 -Daoe.tickms=10 -Daoe.debug=1 \
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
grep -o 'g(1,[0-9]*,0)' "$W/play.log" | tail -1
SAVED=""
SAW2=""
AA=''
for i in $(seq 1 90); do
  echo 'state' > "$W/fifo"; sleep 0.5
  AA=$(python3 -c "import json;print(json.load(open('$W/fifo.json'))['aA'])" 2>/dev/null || echo '')
  if [ "$AA" = "2" ]; then
    SAW2=1
    echo "key -6" > "$W/fifo"; sleep 0.4
  elif [ "$AA" = "6" ] && [ -n "$SAW2" ]; then
    echo "save $W/base.aoesave" > "$W/fifo"; sleep 1.2
    if [ -f "$W/base.aoesave" ]; then
      SZ=$(stat -f%z "$W/base.aoesave" 2>/dev/null || echo 0)
      if [ "$SZ" -gt 12288 ]; then
        echo 'state' > "$W/fifo"; sleep 0.5
        AA2=$(python3 -c "import json;print(json.load(open('$W/fifo.json'))['aA'])" 2>/dev/null || echo '')
        if [ "$AA2" = "6" ]; then SAVED=1; break; fi
        echo "post-save aA=$AA2 弹窗复现, base 作废重录"
        rm -f "$W/base.aoesave"
      fi
    fi
    echo "save 校验未过（重试 $i）"
  fi
done
[ -n "$SAVED" ] || { echo BOOT_FAIL_SAVE "aA=$AA SAW2=$SAW2"; tail -8 "$W/play.log"; exit 1; }
echo "base.aoesave OK $(stat -f%z "$W/base.aoesave") bytes"
if [ "${M2_START_DRV:-1}" = "1" ]; then
  nohup python3 $W/m2v2drv.py $W > "$W/drv$N.log" 2>&1 &
  DPID=$!
  disown
  echo "driver pid=$DPID (drv$N.log)"
fi
BASE_AR=$(grep -o 'base\.aoesave.*ar=[0-9]*' "$W/play.log" | grep -o 'ar=[0-9]*' | tail -1 | cut -d= -f2)
echo "BASE_AR=$BASE_AR BOOT_OK"
