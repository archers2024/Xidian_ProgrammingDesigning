#Requires -Version 5.1
<#
  校园活动管理系统 - 首次初始化数据库
  在 <工程根>\db\data 下从零创建 MySQL 数据目录、设置 root 密码、导入数据库快照。

  什么时候需要跑：
    - 全新 clone 的项目，且本机没有可用的 MySQL 服务（3306 空着）
    - 只想用工程自带的独立数据库，不装系统服务

  已经有 MySQL 服务的人不用跑这个，直接导入 db\campus_activity_dump.sql 即可。

  参数（一般用不到，调试时可覆盖）：
    -Port     MySQL 端口，默认 3306
    -DataDir  数据目录，默认 <工程根>\db\data
#>

param(
    [int]$Port = 3306,
    [string]$DataDir = ''
)

$ErrorActionPreference = 'Continue'

# ==================== 配置区 ====================
$MySQLBin = ''                 # 留空自动探测
$RootPwd  = '123456'           # 初始化后 root 的密码，必须与 scripts/start-all.ps1 里的 $DbPass 一致
# ===============================================

$Root    = Split-Path -Parent $PSScriptRoot
if (!$DataDir) { $DataDir = Join-Path $Root 'db\data' }
$SqlFile = Join-Path $Root 'db\campus_activity_dump.sql'
$LogDir  = Join-Path $Root 'logs'
if (!(Test-Path $LogDir)) { New-Item -ItemType Directory -Path $LogDir -Force | Out-Null }

function Pause-Exit([int]$Code = 0) {
    if (!$env:CAMPUS_NO_PAUSE) { Read-Host '按回车退出' | Out-Null }
    exit $Code
}

function Test-Port([int]$P) {
    return [bool](Get-NetTCPConnection -LocalPort $P -State Listen -ErrorAction SilentlyContinue)
}

function Start-Hidden([string]$File, [string]$Arguments, [string]$WorkingDir) {
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $File
    $psi.Arguments = $Arguments
    $psi.WorkingDirectory = $WorkingDir
    $psi.UseShellExecute = $true
    $psi.WindowStyle = 'Hidden'
    [void][System.Diagnostics.Process]::Start($psi)
}

function Resolve-MySqlBin([string]$Hint) {
    if ($Hint -and (Test-Path (Join-Path $Hint 'mysqld.exe'))) { return $Hint }
    $c = Get-Command mysqld.exe -ErrorAction SilentlyContinue
    if ($c) { return (Split-Path -Parent $c.Source) }
    $roots = @("$env:ProgramFiles\MySQL", "${env:ProgramFiles(x86)}\MySQL", 'D:\MySQL', 'D:\mysql', 'C:\MySQL', 'C:\mysql')
    foreach ($r in $roots) {
        if (!(Test-Path $r)) { continue }
        if (Test-Path (Join-Path $r 'bin\mysqld.exe')) { return (Join-Path $r 'bin') }
        $hit = Get-ChildItem $r -Directory -ErrorAction SilentlyContinue |
               Where-Object { Test-Path (Join-Path $_.FullName 'bin\mysqld.exe') } |
               Sort-Object Name -Descending | Select-Object -First 1
        if ($hit) { return (Join-Path $hit.FullName 'bin') }
    }
    return $null
}

Write-Host ''
Write-Host '=========================================' -ForegroundColor Cyan
Write-Host '   首次初始化数据库（只需跑一次）' -ForegroundColor Cyan
Write-Host '=========================================' -ForegroundColor Cyan
Write-Host ''

# 0. 前置检查
if (Test-Port $Port) {
    Write-Host "端口 $Port 已被占用（本机已有 MySQL 在运行）。" -ForegroundColor Red
    Write-Host '这种情况不需要初始化，直接导入 SQL 即可：' -ForegroundColor Yellow
    Write-Host "  mysql --default-character-set=utf8mb4 -uroot -p < `"$SqlFile`"" -ForegroundColor Yellow
    Pause-Exit 1
}
if (Test-Path (Join-Path $DataDir 'ibdata1')) {
    Write-Host "数据目录已初始化过：$DataDir" -ForegroundColor Green
    Write-Host '如需重建，请先删除该目录（会丢失全部数据）。' -ForegroundColor DarkGray
    Pause-Exit 0
}
$MySQLBin = Resolve-MySqlBin $MySQLBin
if (!$MySQLBin) {
    Write-Host '找不到 MySQL 安装目录（mysqld.exe）。' -ForegroundColor Red
    Write-Host '请先安装 MySQL 8，或在本脚本顶部 $MySQLBin 填写 bin 目录。' -ForegroundColor Red
    Pause-Exit 1
}
if (!(Test-Path $SqlFile)) {
    Write-Host "找不到数据库快照：$SqlFile" -ForegroundColor Red
    Pause-Exit 1
}

$mysqld     = Join-Path $MySQLBin 'mysqld.exe'
$mysql      = Join-Path $MySQLBin 'mysql.exe'
$mysqladmin = Join-Path $MySQLBin 'mysqladmin.exe'

Write-Host "MySQL   : $MySQLBin"
Write-Host "数据目录: $DataDir"
Write-Host "端口    : $Port"
Write-Host ''

# 1. 初始化数据目录
Write-Host '[1/4] 初始化数据目录 ...' -ForegroundColor Yellow
New-Item -ItemType Directory -Path $DataDir -Force | Out-Null
$initLog = Join-Path $LogDir 'init-db.log'
& $mysqld --initialize-insecure "--basedir=$MySQLBin" "--datadir=$DataDir" "--log-error=$initLog" 2>&1 | Out-Null
if (!(Test-Path (Join-Path $DataDir 'ibdata1'))) {
    Write-Host '      初始化失败，日志尾部：' -ForegroundColor Red
    if (Test-Path $initLog) { Get-Content $initLog -Tail 15 | ForEach-Object { Write-Host "      $_" } }
    Pause-Exit 1
}
Write-Host '      完成' -ForegroundColor Green

# 2. 临时启动实例并设置 root 密码
Write-Host '[2/4] 启动实例并设置 root 密码 ...' -ForegroundColor Yellow
$argLine = '--basedir="{0}" --datadir="{1}" --port={2} --bind-address=127.0.0.1 --mysqlx=0 --skip-log-bin --log-error="{1}\mysql-error.log"' -f $MySQLBin, $DataDir, $Port
Start-Hidden -File $mysqld -Arguments $argLine -WorkingDir $DataDir
$ok = $false
for ($i = 0; $i -lt 45; $i++) {
    if (Test-Port $Port) { $ok = $true; break }
    Start-Sleep -Seconds 1
}
if (!$ok) {
    Write-Host '      启动超时，请看 db\data\mysql-error.log' -ForegroundColor Red
    Pause-Exit 1
}

$alterSql = "ALTER USER 'root'@'localhost' IDENTIFIED BY '$RootPwd'; CREATE USER IF NOT EXISTS 'root'@'127.0.0.1' IDENTIFIED BY '$RootPwd'; GRANT ALL PRIVILEGES ON *.* TO 'root'@'127.0.0.1' WITH GRANT OPTION; FLUSH PRIVILEGES;"
& $mysql @('-h127.0.0.1', "-P$Port", '-uroot', '--skip-password', '-e', $alterSql) 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host '      设置 root 密码失败' -ForegroundColor Red
    Pause-Exit 1
}
Write-Host '      完成' -ForegroundColor Green

# 3. 导入数据库快照
Write-Host '[3/4] 导入数据库快照 ...' -ForegroundColor Yellow
$sqlForSource = $SqlFile -replace '\\', '/'
& $mysql @('-h127.0.0.1', "-P$Port", '-uroot', "-p$RootPwd", '--default-character-set=utf8mb4', '-e', "source $sqlForSource") 2>&1 | Out-Null
$cntRaw = (& $mysql @('-h127.0.0.1', "-P$Port", '-uroot', "-p$RootPwd", '--default-character-set=utf8mb4', '-N', '-e', 'SELECT COUNT(*) FROM campus_activity.t_activity;') 2>&1)
$cnt = ($cntRaw | Where-Object { "$_".Trim() -match '^\d+$' } | Select-Object -First 1)
if ("$cnt".Trim() -match '^\d+$') {
    Write-Host "      完成（t_activity 现有 $("$cnt".Trim()) 条）" -ForegroundColor Green
} else {
    Write-Host '      导入失败，请手动执行：' -ForegroundColor Red
    Write-Host "      mysql --default-character-set=utf8mb4 -uroot -p < `"$SqlFile`"" -ForegroundColor Red
}

# 4. 关闭临时实例
Write-Host '[4/4] 关闭临时实例 ...' -ForegroundColor Yellow
& $mysqladmin @('-h127.0.0.1', "-P$Port", '-uroot', "-p$RootPwd", 'shutdown') 2>&1 | Out-Null
for ($i = 0; $i -lt 15; $i++) { if (!(Test-Port $Port)) { break }; Start-Sleep -Seconds 1 }
Write-Host '      完成' -ForegroundColor Green

Write-Host ''
Write-Host '初始化完成。现在双击根目录的「启动全部.cmd」即可启动。' -ForegroundColor Cyan
Write-Host ''
Pause-Exit 0
