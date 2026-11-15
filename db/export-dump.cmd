@echo off
REM 从运行中的 MySQL 导出真实 dump 到 db/campus_activity_dump.sql
REM 前置：MySQL 服务已启动（管理员权限 net start MySQL80）
REM 用法：set DB_PASSWORD=你的密码 然后运行本脚本
REM 可选：MYSQLDUMP_BIN 指向 mysqldump.exe 的完整路径
cd /d "%~dp0"
if "%DB_PASSWORD%"=="" set DB_PASSWORD=root
if "%MYSQLDUMP_BIN%"=="" set MYSQLDUMP_BIN=mysqldump

echo 正在导出 campus_activity ...
"%MYSQLDUMP_BIN%" --default-character-set=utf8mb4 --single-transaction --no-tablespaces --databases campus_activity -uroot -p%DB_PASSWORD% > campus_activity_dump.sql
if errorlevel 1 (
  echo 导出失败：检查 MySQL 服务是否启动、密码是否正确。
  exit /b 1
)
echo 已生成 campus_activity_dump.sql
