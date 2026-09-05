#!/bin/bash
# m6b 主线 boot（总攻关 missionIndex 6）：campaign:7 + 新鲜 rmsDir → idx6。
# base 落盘 v4 防呆（aA=2→key -6→aA=6→save→复查>12KB）。同命令链启动 m6bdrv.py。
# 红线：pkill 只用窄 pattern "aoe-camp/m6b"。
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
W=/tmp/aoe-camp/m6b
REPO=/Users/zhangzhuo/repos/personal/age-of-empires-j2me-remastered
N=${1:-1}
pkill -f "aoe-camp/m6b" 2>/dev/null
sleep 1
[ -f "$W/play.log" ] && cp "$W/play.log" "$W/play-b$((N-1)).log"
rm -rf "$W/rms" "$W/saves"; rm -f "$W/fifo" "$W/fifo.json" "$W/fifo.aistate.json" \
  "$W/base.aoesave"
mkdir -p "$W/saves" "$W/rms"
mkfifo "$W/fifo"
nohup /opt/homebrew/opt/openjdk@17/bin/java \
  -Dapple.awt.UIElement=true -Daoe.headless=1 -Daoe.dev=campaign:7 -Daoe.tickms=10 -Daoe.debug=1 \
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
if [ "${M6_START_DRV:-1}" = "1" ]; then
  nohup python3 $W/m6bdrv.py $W > "$W/drv$N.log" 2>&1 &
  DPID=$!
  disown
  echo "driver pid=$DPID (drv$N.log)"
fi
BASE_AR=$(grep -o 'base\.aoesave.*ar=[0-9]*' "$W/play.log" | grep -o 'ar=[0-9]*' | tail -1 | cut -d= -f2)
echo "BASE_AR=$BASE_AR BOOT_OK"
