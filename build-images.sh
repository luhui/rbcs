#!/bin/bash

# 构建Java项目
./gradlew clean bootJar

# 构建rbcs镜像
docker build -t 255170271587.dkr.ecr.ap-northeast-1.amazonaws.com/luhui/rbcs:latest .
docker push 255170271587.dkr.ecr.ap-northeast-1.amazonaws.com/luhui/rbcs:latest