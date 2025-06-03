#!/bin/bash

set -euo pipefail

# 检查传参
if [ $# -lt 1 ]; then
    echo "Usage: $0 <tag>"
    exit 1
fi

HOST_IP=192.168.100.142
APP_PORT=9003
XXL_PORT=9998
PRODUCT=devops
PROFILES=pre

APP_NAME=devops-web-be
DOCKER_IMAGE="$APP_NAME:$1"
DIRECTORY=/opt/docker-app/$APP_NAME
HARBOR_PATH="192.168.100.150:8082/devops"

# 删除当前运行的重复容器
if docker ps -a | grep "$APP_NAME" >/dev/null 2>&1; then
    docker stop $(docker ps -a | grep "$APP_NAME" | awk '{print $1}')
    docker rm $(docker ps -a | grep "$APP_NAME"  | awk '{print $1}')
fi

# 删除不匹配当前 tag 的旧镜像
docker images --format '{{.Repository}} {{.Tag}} {{.ID}}' | awk -v app="$APP_NAME" -v tag="$1" '$1 ~ app && $2 != tag {print $3}' | xargs -r docker rmi

docker run -dit --restart=always \
  --memory 2g --memory-swap=-1 \
  -p ${APP_PORT}:8083 \
  -p ${XXL_PORT}:9998 \
  -e PROAIM_HOST=${HOST_IP} \
  -e TZ=Asia/Shanghai \
  -e PARAMS="--spring.config.additional-location=classpath:/config/${PRODUCT}/ --spring.profiles.active=${PROFILES}" \
  -v $DIRECTORY/HostDataVolume:/data \
  --log-opt max-size=6g --privileged=true \
  --name "$APP_NAME" "$HARBOR_PATH/$DOCKER_IMAGE"
