#!/bin/bash

# 校验必需的输入参数
if [[ -z "$1" || -z "$2" || -z "$3" ]]; then
  echo "错误：缺少必要的参数！"
  echo "请传入以下三个参数："
  echo "1. filePath：目标文件路径"
  echo "2. fileName：文件名"
  echo "3. envList：环境名称（例如：dev、test、pre、prod）"
  exit 1
fi

# 获取输入的参数
filePath=$1
fileName=$2
envList=$3

# 定义不同环境的端口映射
declare -A ports=([dev]=48080 [test]=9003 [pre]=9004 [prod]=9005)
target_port=${ports[$envList]}

# 检查指定环境是否存在映射的端口
if [ -z "$target_port" ]; then
  echo "错误：未知的环境 '$envList'，请检查环境名称是否正确"
  exit 1
fi

# 清理待执行的 `at` 任务队列
echo "清理待执行的任务队列..."
atq | awk '{print $1}' | xargs -r atrm

# 查找并停止占用目标端口的进程
echo "检查并停止占用端口 $target_port 的进程..."
pid=$(lsof -t -i :$target_port)
if [ -n "$pid" ]; then
  echo "进程 $pid 正在使用端口 $target_port，正在停止该进程..."
  kill -15 $pid
  sleep 5
else
  echo "未找到占用端口 $target_port 的进程"
fi

echo "启动服务：${filePath}/${fileName} (${envList} 环境)..."
echo "/opt/jdk1.8.0_131/bin/java -jar ${filePath}/${fileName} --spring.profiles.active=${envList}" | at now
