@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

if not exist .env (
    echo [INFO] No .env found, copy from .env.example
    copy /Y .env.example .env >nul
)

where docker >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Docker is not installed or not in PATH. Please install Docker Desktop first.
    pause
    exit /b 1
)

REM 启用 BuildKit 让 Dockerfile 中的 --mount=type=cache 生效（Maven 依赖缓存复用）
set DOCKER_BUILDKIT=1
set COMPOSE_DOCKER_CLI_BUILD=1

echo [INFO] Building and starting containers...
docker compose --env-file .env up -d --build
if errorlevel 1 (
    echo [ERROR] docker compose failed.
    pause
    exit /b 1
)

echo [INFO] Waiting for backend health check (up to 5 minutes)...
set /a count=0
:wait_loop
set /a count+=1
if !count! gtr 150 goto warn
set state=starting
for /f %%H in ('docker inspect -f "{{.State.Health.Status}}" feedback-backend 2^>nul') do set state=%%H
if "!state!"=="healthy" (
    echo [OK] Backend is ready. Visit: http://localhost:9091
    goto end
)
REM docker inspect 偶尔慢半拍，curl 兜底
curl -fsS http://localhost:9091/actuator/health/live >nul 2>nul
if !errorlevel! == 0 (
    echo [OK] Backend /actuator/health/live ready. Visit: http://localhost:9091
    goto end
)
timeout /t 2 /nobreak >nul
goto wait_loop

:warn
echo [WARN] Backend did not become healthy in 300s.
echo        1) View logs : docker compose logs -f backend
echo        2) Probe live: curl http://localhost:9091/actuator/health/live

:end
endlocal
pause
