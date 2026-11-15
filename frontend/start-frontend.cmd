@echo off
REM 本机 Windows 启动示例：监听 0.0.0.0 以便手机/局域网访问。
cd /d "%~dp0"
npm run dev -- --host 0.0.0.0 --port 5173 > "%~dp0run-frontend.log" 2>&1
