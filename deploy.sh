#!/bin/bash

# 检查是否已构建镜像
if [[ "$(docker images -q 255170271587.dkr.ecr.ap-northeast-1.amazonaws.com/luhui/rbcs:latest 2> /dev/null)" == "" ]]; then
  echo "请先运行build-images.sh构建镜像"
  exit 1
fi

# 部署rbcs
kubectl apply -f k8s/rbcs-deployment.yaml
kubectl apply -f k8s/rbcs-service.yaml

echo "部署完成"