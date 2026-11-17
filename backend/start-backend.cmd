@echo off
REM 数据库密码使用 CAMPUS_DB_PASSWORD 或 config/local.properties，勿放在命令行。
REM 可选环境变量：MOBILE_BASE_URL（手机扫码用的地址）。
cd /d "%~dp0"
if "%MOBILE_BASE_URL%"=="" set MOBILE_BASE_URL=http://localhost:5173/m
java -Xmx384m -jar target\activity-system-1.0.0.jar --campus.mobile.base-url=%MOBILE_BASE_URL%
