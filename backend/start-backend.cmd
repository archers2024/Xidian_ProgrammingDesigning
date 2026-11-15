@echo off
REM 本机 Windows 启动示例：先 set DB_PASSWORD=你的密码，再运行本脚本。
REM 可选环境变量：MYSQL_BIN（mysql.exe 完整路径）、MOBILE_BASE_URL（手机扫码用的地址）。
cd /d "%~dp0"
if "%DB_PASSWORD%"=="" set DB_PASSWORD=root
if "%MYSQL_BIN%"=="" set MYSQL_BIN=mysql
if "%MOBILE_BASE_URL%"=="" set MOBILE_BASE_URL=http://localhost:5173/m
"%MYSQL_BIN%" -uroot -p%DB_PASSWORD% -e "SET GLOBAL sql_mode='STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';" >nul 2>&1
java -jar target\activity-system-1.0.0.jar --spring.datasource.password=%DB_PASSWORD% "--spring.datasource.url=jdbc:mysql://127.0.0.1:3306/campus_activity?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false" --campus.mobile.base-url=%MOBILE_BASE_URL% > "%~dp0run-backend.log" 2>&1
