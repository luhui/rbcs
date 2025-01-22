#!/bin/bash

# 设置目标URL
URL="http://localhost:8080"

# 设置wrk的参数
THREADS=30
CONNECTIONS=300
DURATION=10s
LUA_SCRIPT="transaction.lua"
SCENE="deposit"  # 默认场景为deposit

# 检查是否通过命令行参数指定了场景
if [ -n "$1" ]; then
    SCENE="$1"
fi

# 执行wrk性能测试，并传递场景参数
echo "开始执行场景：$SCENE 的性能测试..."
wrk -s $LUA_SCRIPT -t$THREADS -c$CONNECTIONS -d$DURATION --timeout 10s -- $URL $SCENE

echo "场景：$SCENE 的性能测试完成。"