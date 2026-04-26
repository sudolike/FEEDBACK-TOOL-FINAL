@echo off
cd /d "%~dp0"
docker compose --env-file .env down
echo [OK] Services stopped.
pause
