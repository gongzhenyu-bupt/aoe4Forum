#!/bin/bash

# 记录初始目录
BASE_DIR=$(pwd)

# 启动 Spring Boot
cd "$BASE_DIR/aoe4Forum/aoe4Forum-web" || exit
nohup mvn spring-boot:run > spring.log 2>&1 &

# 回到原目录并启动前端
cd "$BASE_DIR/webapp" || exit
nohup npm run dev -- --host 0.0.0.0 > webapp.log 2>&1 &

# 回到原目录
cd "$BASE_DIR" || exit

echo "Spring Boot 和前端已在后台启动。"
echo "日志文件：spring.log 和 webapp.log"

