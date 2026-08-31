#!/bin/sh
# Age of Empires II 桌面版启动脚本。
# 修正环境里可能失效的 JAVA_HOME 后启动游戏。
# 用法: ./run.sh [放大倍数]（默认 3，即 720x960 窗口）

cd "$(dirname "$0")"

JDK=/opt/homebrew/opt/openjdk@17
if [ ! -x "$JDK/bin/java" ]; then
    echo "找不到 JDK 17: $JDK" >&2
    echo "请先安装: brew install openjdk@17" >&2

    exit  1
fi
export JAVA_HOME="$JDK"
export PATH="$JDK/bin:$PATH"

# 优先用仓库内的 wrapper（只需 JDK）；没有 wrapper 时退回系统 gradle
if [ -x ./gradlew ]; then
    exec ./gradlew run "$@"
fi
exec gradle run "$@"
