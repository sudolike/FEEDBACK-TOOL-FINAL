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

REM Enable BuildKit so Dockerfile --mount=type=cache (Maven cache reuse) works
set DOCKER_BUILDKIT=1
set COMPOSE_DOCKER_CLI_BUILD=1

REM Clean orphan wslrelay listeners on 9091 (Docker Desktop / WSL2 sometimes
REM leaves wslrelay.exe holding 127.0.0.1:9091 and silently hijacks all
REM localhost traffic, returning 404 even though the container is healthy).
echo [INFO] Cleaning stale listeners on port 9091...
for /f "tokens=5" %%P in ('netstat -ano ^| findstr "127.0.0.1:9091" ^| findstr "LISTENING"') do (
  for /f "tokens=1" %%N in ('tasklist /FI "PID eq %%P" /FO CSV /NH 2^>nul') do (
    set NAME=%%N
    if /I not "!NAME!"=="\"docker-proxy.exe\"" (
      echo   killing stale !NAME! PID %%P
      taskkill /PID %%P /F >nul 2>&1
    )
  )
)
for /f "tokens=5" %%P in ('netstat -ano ^| findstr "::1]:9091" ^| findstr "LISTENING"') do (
  for /f "tokens=1" %%N in ('tasklist /FI "PID eq %%P" /FO CSV /NH 2^>nul') do (
    set NAME=%%N
    if /I not "!NAME!"=="\"docker-proxy.exe\"" (
      echo   killing stale !NAME! PID %%P (ipv6)
      taskkill /PID %%P /F >nul 2>&1
    )
  )
)

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
REM curl fallback in case docker inspect lags
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
