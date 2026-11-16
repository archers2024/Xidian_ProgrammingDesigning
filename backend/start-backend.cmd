@echo off
chcp 65001 >nul
echo.
echo  This launcher has been merged into the one-click script at the project root.
echo  Calling start-all.cmd  ^(MySQL -^> backend -^> frontend^)
echo.
call "%~dp0..\start-all.cmd"
