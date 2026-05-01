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

if ! docker info >/dev/null 2>&1; then
  echo "[ERROR] Docker daemon 未运行。请先启动 Docker Desktop，确认 'Engine running' 后再执行。"
  exit 1
fi

# 启用 BuildKit 让 Dockerfile 中的 --mount=type=cache 生效（Maven 依赖缓存复用）
export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1

echo "[INFO] 构建并启动容器..."
$DC --env-file .env up -d --build

echo "[INFO] 等待后端健康检查通过（最长 5 分钟）..."
PORT="${BACKEND_PORT_HOST:-9091}"
for i in $(seq 1 150); do
  state=$(docker inspect -f '{{.State.Health.Status}}' feedback-backend 2>/dev/null || echo "starting")
  if [ "$state" = "healthy" ]; then
    echo "[OK] 后端已就绪。访问： http://localhost:${PORT}"
    exit 0
  fi
  # docker inspect 偶尔慢半拍，直接 curl 兜底
  if curl -fsS "http://localhost:${PORT}/actuator/health/live" >/dev/null 2>&1; then
    echo "[OK] 后端 /actuator/health/live 已通。访问： http://localhost:${PORT}"
    exit 0
  fi
  sleep 2
done

echo "[WARN] 后端 300 秒内未就绪。"
echo "       1) 查看日志：docker compose logs -f backend"
echo "       2) 直接探活：curl http://localhost:${PORT}/actuator/health/live"
exit 0
