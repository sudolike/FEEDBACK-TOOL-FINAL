@echo off
chcp 65001 >nul
setlocal

rem ============================================================
rem Force rebuild backend image with the latest source code,
rem then restart the stack and run AI provider self-checks.
rem Usage: rebuild.bat
rem ============================================================

echo.
echo [1/5] Stopping running containers...
docker compose down
if errorlevel 1 goto :err

echo.
echo [2/5] Rebuilding backend image (no-cache)...
docker compose build --no-cache backend
if errorlevel 1 goto :err

echo.
echo [3/5] Starting stack in detached mode...
docker compose up -d
if errorlevel 1 goto :err

echo.
echo [4/5] Waiting for backend to become healthy (up to 180s)...
set /a counter=0
:waitloop
timeout /t 5 /nobreak >nul
set /a counter+=5
curl.exe -s -o nul -w "%%{http_code}" http://localhost:9091/actuator/health/live > tmp_health.txt 2>nul
set /p HEALTH_CODE=<tmp_health.txt
del tmp_health.txt 2>nul
if "%HEALTH_CODE%"=="200" goto :ready
if %counter% geq 180 goto :timeout
echo   waiting... %counter%s / 180s
goto :waitloop

:ready
echo   Backend is healthy.

echo.
echo [5/5] Running AI provider self-checks...
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
echo        Run: docker compose logs -f backend
exit /b 1

:err
echo [ERROR] Step failed. Aborting.
exit /b 1
