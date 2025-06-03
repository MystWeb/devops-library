#!/bin/bash

set -euo pipefail

# 检查传参
if [ $# -lt 1 ]; then
    echo "Usage: $0 <tag>"
    exit 1
fi

APP_PORT=8003
APP_NAME=devops-web-fe
DOCKER_IMAGE="$APP_NAME:$1"
HARBOR_PATH="192.168.100.150:8082/devops"

# 删除当前运行的重复容器
if docker ps -a | grep "$APP_NAME" >/dev/null 2>&1; then
    docker stop $(docker ps -a | grep "$APP_NAME" | awk '{print $1}')
    docker rm $(docker ps -a | grep "$APP_NAME"  | awk '{print $1}')
fi

# 删除旧的镜像
#docker image prune -f -a --filter "until=24h" --filter "label=app=$APP_NAME" >/dev/null 2>&1
docker images --format '{{.Repository}} {{.Tag}} {{.ID}}' | awk -v app="$APP_NAME" -v tag="$1" '$1 ~ app && $2 != tag {print $3}' | xargs -r docker rmi

docker run -dit --restart=always -p "$APP_PORT":80 -e TZ=Asia/Shanghai \
-m 256m --memory-swap=-1 \
--name "$APP_NAME" "$HARBOR_PATH/$DOCKER_IMAGE"
