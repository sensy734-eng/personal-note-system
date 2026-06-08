@echo off
setlocal
cd /d "%~dp0"

echo Starting backend demo server...
start "note-backend" cmd /k "cd /d %~dp0backend && java ""-Dspring.profiles.active=demo"" -jar target\demo-0.0.1-SNAPSHOT.jar"

echo Starting frontend dev server...
start "note-frontend" cmd /k "cd /d %~dp0frontend && npm.cmd run dev -- --host 0.0.0.0"

echo Waiting for local services...
timeout /t 15 /nobreak >nul

echo Starting public tunnel. Copy the https://*.loca.lt URL shown below.
set npm_config_cache=%~dp0.npm-cache
npx.cmd --yes localtunnel --port 5173
