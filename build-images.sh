#!/bin/bash

# 构建Java项目
./gradlew clean build

# 构建rbcs镜像
docker build -t rbcs:latest .