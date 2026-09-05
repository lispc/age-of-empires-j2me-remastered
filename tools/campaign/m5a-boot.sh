#!/bin/bash
# m5a boot（守城关 missionIndex 5）：campaign:6 + 新鲜 rmsDir → idx5。
# base 落盘防呆（v4 档）：必须先观测到简报弹窗(aA=2)→ -6 推掉 → aA=6 才 save
# （防「简报迟到 ~100t、aA 先读 6」把 base 录在弹窗前 → 回放冻死在弹窗）；
# save 后复查 aA 仍 6 且文件 >12KB 才算过。同命令链立刻启动 m5adrv.py。
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
W=/tmp/aoe-camp/m5a
REPO=/Users/zhangzhuo/repos/personal/age-of-empires-j2me-remastered
N=${1:-1}   # boot 序号（drv 日志后缀）
pkill -f "aoe-camp/m5a/m5adrv.py" 2>/dev/null
pkill -f aoe.Main 2>/dev/null
sleep 1
[ -f "$W/play.log" ] && cp "$W/play.log" "$W/play-b$((N-1)).log"
rm -rf "$W/rms" "$W/saves"; rm -f "$W/fifo" "$W/fifo.json" "$W/fifo.aistate.json" \
  "$W/base.aoesave"
mkdir -p "$W/saves" "$W/rms"
mkfifo "$W/fifo"
nohup /opt/homebrew/opt/openjdk@17/bin/java \
  -Daoe.headless=1 -Daoe.dev=campaign:6 -Daoe.tickms=10 -Daoe.debug=1 \
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
# ---- base 落盘：先见到 aA=2（简报在）→ -6 → aA=6 → save+双校验 ----
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
# ---- 同一命令链：立刻启动驱动 ----
nohup python3 $W/m5adrv.py $W > "$W/drv$N.log" 2>&1 &
DPID=$!
disown
echo "driver pid=$DPID (drv$N.log)"
BASE_AR=$(grep -o 'base\.aoesave.*ar=[0-9]*' "$W/play.log" | grep -o 'ar=[0-9]*' | tail -1 | cut -d= -f2)
echo "BASE_AR=$BASE_AR BOOT_OK"
