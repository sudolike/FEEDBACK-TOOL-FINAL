@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

rem ============================================================
rem Force rebuild backend image with the latest source code,
rem cleanup orphan wslrelay port bindings, then restart the
rem stack and run AI provider self-checks.
rem Usage: rebuild.bat
rem ============================================================

echo.
echo [1/6] Stopping running containers...
docker compose down
if errorlevel 1 goto :err

echo.
echo [2/6] Cleaning orphan port bindings on 9091 (wslrelay residue)...
rem Docker Desktop on WSL2 sometimes leaves wslrelay.exe holding 127.0.0.1:9091
rem after stack down, hijacking host curl traffic to a dead container.
rem Find any non-docker-proxy listener on 9091 and kill it.
for /f "tokens=5" %%P in ('netstat -ano ^| findstr "127.0.0.1:9091" ^| findstr "LISTENING"') do (
  echo   Found stale listener PID %%P on 127.0.0.1:9091
  for /f "tokens=1" %%N in ('tasklist /FI "PID eq %%P" /FO CSV /NH 2^>nul') do (
    set NAME=%%N
    if /I not "!NAME!"=="\"docker-proxy.exe\"" (
      echo   Killing !NAME! (PID %%P)
      taskkill /PID %%P /F >nul 2>&1
    )
  )
)
for /f "tokens=5" %%P in ('netstat -ano ^| findstr "::1]:9091" ^| findstr "LISTENING"') do (
  for /f "tokens=1" %%N in ('tasklist /FI "PID eq %%P" /FO CSV /NH 2^>nul') do (
    set NAME=%%N
    if /I not "!NAME!"=="\"docker-proxy.exe\"" (
      echo   Killing !NAME! (PID %%P) on [::1]:9091
      taskkill /PID %%P /F >nul 2>&1
    )
  )
)

echo.
echo [3/6] Rebuilding backend image (no-cache)...
docker compose build --no-cache backend
if errorlevel 1 goto :err

echo.
echo [4/6] Starting stack in detached mode...
docker compose up -d
if errorlevel 1 goto :err

echo.
echo [5/6] Waiting for backend to become healthy (up to 180s)...
set /a counter=0
:waitloop
timeout /t 5 /nobreak >nul
set /a counter+=5
curl.exe -s -o nul -w "%%{http_code}" http://localhost:9091/actuator/health/live > tmp_health.txt 2>nul
set /p HEALTH_CODE=<tmp_health.txt
del tmp_health.txt 2>nul
if "%HEALTH_CODE%"=="200" goto :ready
if %counter% geq 180 goto :timeout
echo   waiting... %counter%s / 180s (last status=%HEALTH_CODE%)
goto :waitloop

:ready
echo   Backend is healthy.

echo.
echo [6/6] Running AI provider self-checks...
echo --- /ai/status ---
curl.exe -s http://localhost:9091/ai/status
echo.
echo --- /ai/ping?msg=hi ---
curl.exe -s "http://localhost:9091/ai/ping?msg=hi"
echo.
echo --- backend logs (AI Provider banner) ---
docker compose logs --tail 200 backend | findstr "AI Provider"
echo.
echo Done.
exit /b 0

:timeout
echo [WARN] Backend did not become healthy in 180s.
echo        Possible causes:
echo          1) wslrelay.exe still holding 127.0.0.1:9091 - run rebuild.bat again
echo          2) backend startup error - run: docker compose logs -f backend
exit /b 1

:err
echo [ERROR] Step failed. Aborting.
exit /b 1
