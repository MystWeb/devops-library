#!/bin/bash

# sh service.sh anyOps-devops-service 1.1.1 8080 start
APP_NAME=$1
VERSION=$2
PORT=$3

start() {
  port_result=$(netstat -anlpt | grep "${PORT}" || echo false)

  if [[ $port_result == "false" ]]; then
    nohup java -jar -Dserver.port="${PORT}" "${APP_NAME}"-"${VERSION}".jar >"${APP_NAME}".log.txt 2>&1 &
  else
    stop
    sleep 5
    nohup java -jar -Dserver.port="${PORT}" "${APP_NAME}"-"${VERSION}".jar >"${APP_NAME}".log.txt 2>&1 &
  fi
}

# TODO 如果多次部署端口号不一致，将无法获取上一个服务监听的端口号
stop() {
  local pid
  pid=$(netstat -anlpt | grep "${PORT}" | awk '{print $NF}' | awk -F '/' '{print $1}')
  kill -15 "$pid"
}

check() {
  local proc_result url_result
  proc_result=$(ps aux | grep java | grep "${APP_NAME}" | grep -v grep || echo false)
  port_result=$(netstat -anlpt | grep "${PORT}" || echo false)
  url_result=$(curl -s http://localhost:"${PORT}" || echo false)

  if [[ $proc_result == "false" || $port_result == "false" || $url_result == "false" ]]; then
    echo "error"
  else
    echo "ok"
  fi
}

case $4 in
start)
  start
  sleep 5
  check
  ;;

stop)
  stop
  sleep 5
  check
  ;;
restart)
  stop
  sleep 5
  start
  sleep 5
  check
  ;;
check)
  check
  ;;
*)
  echo "sh service.sh {start|stop|restart|check}"
  ;;
esac
