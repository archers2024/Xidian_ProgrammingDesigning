#Requires -Version 5.1
<#
  校园活动管理系统 - 一键启动
  依次启动：MySQL(3306) -> 后端 Spring Boot(8080) -> 前端 Vite(5173)
  已在运行的组件会自动跳过，可重复执行。
#>

$ErrorActionPreference = 'Continue'

# ==================== 配置区（换机器只改这里） ====================
$MySQLBin = 'C:\Program Files\MySQL\MySQL Server 8.0\bin'
$JavaExe  = 'F:\JDK\bin\java.exe'
$DbUser   = 'root'
$DbPass   = '123456'
$DbName   = 'campus_activity'
# ================================================================

$Root     = Split-Path -Parent $PSScriptRoot
# 数据库跟着工程走：<工程>\db\data。工程路径必须是纯英文，mysqld 不认中文 datadir。
$DataDir  = Join-Path $Root 'db\data'
$Jar      = Join-Path $Root 'backend\target\activity-system-1.0.0.jar'
$FrontDir = Join-Path $Root 'frontend'
$LogDir   = Join-Path $Root 'logs'
$DbUrl    = 'jdbc:mysql://127.0.0.1:3306/' + $DbName + '?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false'

if (!(Test-Path $LogDir)) { New-Item -ItemType Directory -Path $LogDir -Force | Out-Null }

# 设置 CAMPUS_NO_PAUSE=1 可免交互（供自动化调用）
function Pause-Exit([int]$Code = 0) {
    if (!$env:CAMPUS_NO_PAUSE) { Read-Host '按回车退出' | Out-Null }
    exit $Code
}

# 用 .NET 直接启动，规避 PowerShell 5.1 在环境变量大小写重名（HTTP_PROXY/https_proxy）时
# Start-Process 抛“字典中的关键字重复”的问题；同时让子进程完全独立于当前窗口。
function Start-Hidden([string]$File, [string]$Arguments, [string]$WorkingDir) {
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $File
    $psi.Arguments = $Arguments
    $psi.WorkingDirectory = $WorkingDir
    $psi.UseShellExecute = $true
    $psi.WindowStyle = 'Hidden'
    [void][System.Diagnostics.Process]::Start($psi)
}

function Test-Port([int]$Port) {
    $c = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    return [bool]$c
}

function Wait-Port([int]$Port, [int]$Seconds) {
    for ($i = 0; $i -lt $Seconds; $i++) {
        if (Test-Port $Port) { return $true }
        Start-Sleep -Seconds 1
    }
    return $false
}

# 取默认路由网卡的局域网 IP（避开 VMware 虚拟网卡）
function Get-LanIp {
    try {
        $route = Get-NetRoute -DestinationPrefix '0.0.0.0/0' -ErrorAction Stop |
                 Sort-Object RouteMetric | Select-Object -First 1
        $ip = Get-NetIPAddress -AddressFamily IPv4 -InterfaceIndex $route.InterfaceIndex -ErrorAction Stop |
              Select-Object -First 1
        if ($ip.IPAddress) { return $ip.IPAddress }
    } catch { }
    return '127.0.0.1'
}

Write-Host ''
Write-Host '=========================================' -ForegroundColor Cyan
Write-Host '   校园活动管理系统 - 一键启动' -ForegroundColor Cyan
Write-Host '=========================================' -ForegroundColor Cyan
Write-Host ''

# ---------- 1. MySQL ----------
if (Test-Port 3306) {
    Write-Host '[1/3] MySQL 已在运行 (3306)，跳过' -ForegroundColor Green
} else {
    Write-Host '[1/3] 正在启动 MySQL ...' -ForegroundColor Yellow
    $mysqld = Join-Path $MySQLBin 'mysqld.exe'
    if (!(Test-Path $mysqld)) {
        Write-Host "      找不到 mysqld.exe：$mysqld" -ForegroundColor Red
        Pause-Exit 1
    }
    if (!(Test-Path (Join-Path $DataDir 'ibdata1'))) {
        Write-Host "      数据目录不存在或未初始化：$DataDir" -ForegroundColor Red
        Pause-Exit 1
    }
    $argLine = '--basedir="{0}" --datadir="{1}" --port=3306 --bind-address=127.0.0.1 --mysqlx=0 --skip-log-bin --log-error="{1}\mysql-error.log"' -f $MySQLBin, $DataDir
    Start-Hidden -File $mysqld -Arguments $argLine -WorkingDir $DataDir
    if (Wait-Port 3306 45) {
        Write-Host '      MySQL 已就绪' -ForegroundColor Green
    } else {
        Write-Host "      MySQL 启动超时，请查看 $DataDir\mysql-error.log" -ForegroundColor Red
        Pause-Exit 1
    }
}

# ---------- 2. 修正 sql_mode（统计接口依赖，MySQL 重启后必须重设） ----------
$mysql = Join-Path $MySQLBin 'mysql.exe'
& $mysql -h127.0.0.1 -P3306 -u$DbUser -p$DbPass -e "SET GLOBAL sql_mode='STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';" 2>$null | Out-Null

$LanIp = Get-LanIp

# ---------- 3. 后端 ----------
if (Test-Port 8080) {
    Write-Host '[2/3] 后端已在运行 (8080)，跳过' -ForegroundColor Green
} else {
    Write-Host '[2/3] 正在启动后端 ...' -ForegroundColor Yellow
    if (!(Test-Path $Jar)) {
        Write-Host "      找不到 jar：$Jar" -ForegroundColor Red
        Write-Host '      请先在 backend 目录执行：mvn -DskipTests package' -ForegroundColor Red
        Pause-Exit 1
    }
    $java = $JavaExe
    if (!(Test-Path $java)) {
        $cmd = Get-Command java -ErrorAction SilentlyContinue
        if ($cmd) { $java = $cmd.Source } else {
            Write-Host '      找不到 java，请安装 JDK 17 或修改脚本里的 $JavaExe' -ForegroundColor Red
            Pause-Exit 1
        }
    }
    $argLine = '-jar "{0}" --spring.datasource.url="{1}" --spring.datasource.username={2} --spring.datasource.password={3} --campus.mobile.base-url=http://{4}:5173/m --server.servlet.context-path= --logging.file.name="{5}"' -f $Jar, $DbUrl, $DbUser, $DbPass, $LanIp, (Join-Path $LogDir 'backend.log')
    Start-Hidden -File $java -Arguments $argLine -WorkingDir (Join-Path $Root 'backend')
    if (Wait-Port 8080 90) {
        Write-Host '      后端已就绪' -ForegroundColor Green
    } else {
        Write-Host "      后端启动超时，请查看 $LogDir\backend.log" -ForegroundColor Red
        Pause-Exit 1
    }
}

# ---------- 4. 前端 ----------
if (Test-Port 5173) {
    Write-Host '[3/3] 前端已在运行 (5173)，跳过' -ForegroundColor Green
} else {
    Write-Host '[3/3] 正在启动前端 ...' -ForegroundColor Yellow
    $node = (Get-Command node -ErrorAction SilentlyContinue).Source
    if (!$node) {
        Write-Host '      找不到 node，请安装 Node.js 18+' -ForegroundColor Red
        Pause-Exit 1
    }
    $vite = Join-Path $FrontDir 'node_modules\vite\bin\vite.js'
    if (!(Test-Path $vite)) {
        Write-Host "      前端依赖未安装，请先在 frontend 目录执行：npm install" -ForegroundColor Red
        Pause-Exit 1
    }
    Start-Hidden -File $node -Arguments 'node_modules/vite/bin/vite.js --host 0.0.0.0 --port 5173' -WorkingDir $FrontDir
    if (Wait-Port 5173 60) {
        Write-Host '      前端已就绪' -ForegroundColor Green
    } else {
        Write-Host "      前端启动超时，请查看 $LogDir\frontend.log" -ForegroundColor Red
    }
}

# ---------- 汇总 ----------
Write-Host ''
Write-Host '---------------- 访问地址 ----------------' -ForegroundColor Cyan
Write-Host "  电脑端    http://localhost:5173        账号 admin / admin123"
Write-Host "  手机端    http://${LanIp}:5173/m"
Write-Host "  后端 API  http://${LanIp}:8080"
Write-Host "  数据库    $DataDir"
Write-Host '------------------------------------------' -ForegroundColor Cyan
Write-Host '  关闭窗口不影响服务；停止服务请运行「停止全部.cmd」' -ForegroundColor DarkGray
Write-Host ''
Pause-Exit 0
