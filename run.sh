#!/bin/sh
# Age of Empires II 桌面版启动脚本。
# 修正环境里可能失效的 JAVA_HOME 后启动游戏。
# 用法: ./run.sh [放大倍数]（默认 3，即 720x960 窗口）
#
# 每次运行的完整输出（含调试状态行、按键、异常栈）都会留一份在
# ~/Library/Application Support/AoeJ2ME/logs/ 下（保留最近 10 份），
# 出问题时把对应 run-*.log 交给开发者即可定位。

cd "$(dirname "$0")"

JDK=/opt/homebrew/opt/openjdk@17
if [ ! -x "$JDK/bin/java" ]; then
    echo "找不到 JDK 17: $JDK" >&2
    echo "请先安装: brew install openjdk@17" >&2

    exit  1
fi
export JAVA_HOME="$JDK"
export PATH="$JDK/bin:$PATH"

LOG_DIR="$HOME/Library/Application Support/AoeJ2ME/logs"
mkdir -p "$LOG_DIR" 2>/dev/null
LOG="$LOG_DIR/run-$(date +%Y%m%d-%H%M%S).log"
ls -t "$LOG_DIR"/run-*.log 2>/dev/null | tail -n +11 | xargs rm -f 2>/dev/null
echo "本次运行日志: $LOG"

# 优先用仓库内的 wrapper（只需 JDK）；没有 wrapper 时退回系统 gradle
if [ -x ./gradlew ]; then
    ./gradlew run "$@" 2>&1 | tee -a "$LOG"
    exit $?
fi
gradle run "$@" 2>&1 | tee -a "$LOG"
exit $?
