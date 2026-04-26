#!/usr/bin/env bash
# 一键启动脚本：构建镜像并启动 MySQL + Redis + 后端
set -euo pipefail

cd "$(dirname "$0")"

if [ ! -f .env ]; then
  echo "[INFO] 未发现 .env，使用默认 .env.example 复制一份。"
  cp .env.example .env
fi

if command -v docker compose >/dev/null 2>&1; then
  DC="docker compose"
elif command -v docker-compose >/dev/null 2>&1; then
  DC="docker-compose"
else
  echo "[ERROR] 未检测到 docker compose / docker-compose 命令，请先安装 Docker Desktop / docker-ce。"
  exit 1
fi

echo "[INFO] 构建并启动容器..."
$DC --env-file .env up -d --build

echo "[INFO] 等待后端健康检查通过..."
for i in $(seq 1 90); do
  state=$(docker inspect -f '{{.State.Health.Status}}' feedback-backend 2>/dev/null || echo "starting")
  if [ "$state" = "healthy" ]; then
    echo "[OK] 后端已就绪。访问： http://localhost:${BACKEND_PORT_HOST:-9091}"
    exit 0
  fi
  sleep 2
done

echo "[WARN] 后端 180 秒内未就绪。"
echo "       1) 查看日志：docker compose logs -f backend"
echo "       2) 直接探活：curl http://localhost:${BACKEND_PORT_HOST:-9091}/actuator/health/live"
exit 0
