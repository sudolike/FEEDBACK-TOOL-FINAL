#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

if command -v docker compose >/dev/null 2>&1; then
  DC="docker compose"
else
  DC="docker-compose"
fi

$DC --env-file .env down
echo "[OK] 服务已停止。"
