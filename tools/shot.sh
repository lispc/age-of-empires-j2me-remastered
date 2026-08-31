#!/bin/sh
# 只截图 aoe 游戏进程的窗口（不受其他窗口遮挡/前台影响）。
# 用法: tools/shot.sh /tmp/shot.png
PID=$(pgrep -f "aoe.Main" | head -1)
[ -z "$PID" ] && { echo "game not running" >&2; exit 1; }
WID=$(swift "$(dirname "$0")/winid.swift" 2>/dev/null | awk -v p="$PID" '$1==p {print $2; exit}')
[ -z "$WID" ] && { echo "window not found" >&2; exit 1; }
screencapture -x -l"$WID" "$1"
