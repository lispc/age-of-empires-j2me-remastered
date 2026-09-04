#!/bin/bash
# 被动死亡监视器：只读文件，零 fifo 写。检测到 [result] 立即封存证据。
WORK="${1:-/tmp/aoe-camp/m1b}"  # 用法: watch-death.sh [workdir]
i=0
while true; do
  if grep -q '\[result\]' $WORK/play.log 2>/dev/null; then
    cp -p $WORK/play.log $WORK/play-loss2.log
    cp -p $WORK/t.aoesave $WORK/death-t.aoesave 2>/dev/null
    cp -p $WORK/full.log $WORK/full-loss2.log 2>/dev/null
    echo "$(date +%T) SEALED" >> $WORK/watch.log
    break
  fi
  i=$((i+1))
  if [ $((i % 24)) -eq 1 ]; then
    cp -p $WORK/t.aoesave $WORK/trail-$(date +%H%M).aoesave 2>/dev/null
  fi
  sleep 5
done
