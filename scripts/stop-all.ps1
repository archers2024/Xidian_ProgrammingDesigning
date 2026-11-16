#Requires -Version 5.1
<#
  校园活动管理系统 - 一键停止
  顺序：后端(8080) -> 前端(5173) -> MySQL(3306，优雅关闭)
  只停止占用这些端口的进程，不会影响其他程序。
#>

$ErrorActionPreference = 'Continue'

# ==================== 配置区（与 start-all.ps1 保持一致） ====================
$MySQLBin = 'C:\Program Files\MySQL\MySQL Server 8.0\bin'
$DbUser   = 'root'
$DbPass   = '123456'
# ==========================================================================

function Stop-ByPort([int]$Port, [string]$Name) {
    $conns = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    if (!$conns) {
        Write-Host "      $Name 未在运行 ($Port)" -ForegroundColor DarkGray
        return
    }
    $pids = $conns | Select-Object -ExpandProperty OwningProcess -Unique
    foreach ($p in $pids) {
        $proc = Get-Process -Id $p -ErrorAction SilentlyContinue
        if ($proc) {
            Write-Host "      停止 $Name : $($proc.ProcessName) (PID $p)" -ForegroundColor Yellow
            Stop-Process -Id $p -Force -ErrorAction SilentlyContinue
        }
    }
}

Write-Host ''
Write-Host '=========================================' -ForegroundColor Cyan
Write-Host '   校园活动管理系统 - 一键停止' -ForegroundColor Cyan
Write-Host '=========================================' -ForegroundColor Cyan
Write-Host ''

Write-Host '[1/3] 停止后端 (8080)' -ForegroundColor Yellow
Stop-ByPort 8080 '后端'

Write-Host '[2/3] 停止前端 (5173)' -ForegroundColor Yellow
Stop-ByPort 5173 '前端'

Write-Host '[3/3] 停止 MySQL (3306)' -ForegroundColor Yellow
$mysqladmin = Join-Path $MySQLBin 'mysqladmin.exe'
if (Test-Path $mysqladmin) {
    & $mysqladmin -h127.0.0.1 -P3306 -u$DbUser -p$DbPass shutdown 2>$null | Out-Null
    Start-Sleep -Seconds 2
}
if (Get-NetTCPConnection -LocalPort 3306 -State Listen -ErrorAction SilentlyContinue) {
    Stop-ByPort 3306 'MySQL'
} else {
    Write-Host '      MySQL 已优雅关闭' -ForegroundColor Green
}

Write-Host ''
Write-Host '全部服务已停止。数据保存在工程内 db\data 目录，不会丢失。' -ForegroundColor Green
Write-Host ''
if (!$env:CAMPUS_NO_PAUSE) { Read-Host '按回车关闭此窗口' | Out-Null }
