#Requires -Version 5.1
<#
  校园活动管理系统 - 一键启动
  依次启动：MySQL(3306) -> 后端 Spring Boot(8080) -> 前端 Vite(5173)
  已在运行的组件会自动跳过，可重复执行。
#>

$ErrorActionPreference = 'Continue'

# ==================== 配置区（换机器优先改这里） ====================
# 留空 = 自动探测（PATH、JAVA_HOME、Program Files\MySQL、常见安装目录）；
# 探测失败时再填绝对路径。
$MySQLBin = ''          # 例：C:\Program Files\MySQL\MySQL Server 8.0\bin
$JavaExe  = ''          # 例：C:\Program Files\Java\jdk-17\bin\java.exe
$DbUser   = 'root'
$DbPass   = '123456'    # 改成你自己 MySQL 的 root 密码
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

# 自动探测 mysql.exe / mysqld.exe 所在目录
function Resolve-MySqlBin([string]$Hint) {
    if ($Hint -and (Test-Path (Join-Path $Hint 'mysqld.exe'))) { return $Hint }
    $c = Get-Command mysqld.exe -ErrorAction SilentlyContinue
    if ($c) { return (Split-Path -Parent $c.Source) }
    foreach ($r in @("$env:ProgramFiles\MySQL", "${env:ProgramFiles(x86)}\MySQL", 'D:\MySQL', 'D:\mysql', 'C:\MySQL', 'C:\mysql')) {
        if (!(Test-Path $r)) { continue }
        # $r 可能是 MySQL 安装根（其下有 bin），也可能是各版本目录的父级
        if (Test-Path (Join-Path $r 'bin\mysqld.exe')) { return (Join-Path $r 'bin') }
        $hit = Get-ChildItem $r -Directory -ErrorAction SilentlyContinue |
               Where-Object { Test-Path (Join-Path $_.FullName 'bin\mysqld.exe') } |
               Sort-Object Name -Descending | Select-Object -First 1
        if ($hit) { return (Join-Path $hit.FullName 'bin') }
    }
    return $null
}

# 自动探测 java.exe：优先完整 JDK（带 javac）且版本 17+，其次 JAVA_HOME，最后 PATH
function Resolve-Java([string]$Hint) {
    if ($Hint -and (Test-Path $Hint)) { return $Hint }
    $cands = @()
    foreach ($r in @("$env:ProgramFiles\Java", "$env:ProgramFiles\Eclipse Adoptium", 'C:\Program Files\Java', 'F:\JDK', 'D:\JDK', 'C:\JDK', 'F:\Java', 'D:\Java')) {
        if (!(Test-Path $r)) { continue }
        if (Test-Path (Join-Path $r 'bin\java.exe')) { $cands += (Get-Item $r) }
        $cands += Get-ChildItem $r -Directory -ErrorAction SilentlyContinue |
                  Where-Object { Test-Path (Join-Path $_.FullName 'bin\java.exe') }
    }
    $jdkOnly = @($cands | Where-Object { Test-Path (Join-Path $_.FullName 'bin\javac.exe') })
    if ($jdkOnly.Count -gt 0) { $cands = $jdkOnly }
    $preferred = $cands | Where-Object { $_.Name -match 'jdk[-_ ]?(17|18|19|2[0-9])|jdk1[7-9]' } |
                 Sort-Object Name -Descending | Select-Object -First 1
    if (!$preferred) { $preferred = $cands | Sort-Object Name -Descending | Select-Object -First 1 }
    if ($preferred) { return (Join-Path $preferred.FullName 'bin\java.exe') }
    if ($env:JAVA_HOME) {
        $p = Join-Path $env:JAVA_HOME 'bin\java.exe'
        if (Test-Path $p) { return $p }
    }
    $c = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($c) { return $c.Source }
    return $null
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
    Write-Host '[1/3] MySQL 已在运行 (3306)，直接使用' -ForegroundColor Green
    $MySQLBin = Resolve-MySqlBin $MySQLBin
} else {
    Write-Host '[1/3] 正在启动 MySQL ...' -ForegroundColor Yellow
    $MySQLBin = Resolve-MySqlBin $MySQLBin
    if (!$MySQLBin) {
        Write-Host '      找不到 MySQL（mysqld.exe）。' -ForegroundColor Red
        Write-Host '      两条路，任选其一：' -ForegroundColor Yellow
        Write-Host '      A. 装 MySQL 8 后启动系统服务（net start MySQL80），再重新运行本脚本' -ForegroundColor Yellow
        Write-Host '      B. 装好 MySQL 8 后双击「初始化数据库.cmd」，用工程自带数据目录' -ForegroundColor Yellow
        Pause-Exit 1
    }
    $mysqld = Join-Path $MySQLBin 'mysqld.exe'
    if (!(Test-Path (Join-Path $DataDir 'ibdata1'))) {
        Write-Host "      工程内数据目录未初始化：$DataDir" -ForegroundColor Red
        Write-Host '      请先双击「初始化数据库.cmd」跑一次（约 30 秒），或改用系统 MySQL 服务。' -ForegroundColor Yellow
        Pause-Exit 1
    }
    # sql-mode 随实例启动生效：统计接口的 GROUP BY 依赖它，避免每次重启后再手工 SET GLOBAL
    $argLine = '--basedir="{0}" --datadir="{1}" --port=3306 --bind-address=127.0.0.1 --mysqlx=0 --skip-log-bin --log-error="{1}\mysql-error.log" --sql-mode="STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION"' -f $MySQLBin, $DataDir
    Start-Hidden -File $mysqld -Arguments $argLine -WorkingDir $DataDir
    if (Wait-Port 3306 45) {
        Write-Host '      MySQL 已就绪' -ForegroundColor Green
    } else {
        Write-Host "      MySQL 启动超时，请查看 $DataDir\mysql-error.log" -ForegroundColor Red
        Pause-Exit 1
    }
}

# ---------- 2. 修正 sql_mode（统计接口依赖，MySQL 重启后必须重设） ----------
if ($MySQLBin) {
    $mysql = Join-Path $MySQLBin 'mysql.exe'
    & $mysql @('-h127.0.0.1', '-P3306', "-u$DbUser", "-p$DbPass", '-e',
        "SET GLOBAL sql_mode='STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';") 2>$null | Out-Null
}

$LanIp = Get-LanIp

# ---------- 3. 后端 ----------
if (Test-Port 8080) {
    Write-Host '[2/3] 后端已在运行 (8080)，跳过' -ForegroundColor Green
} else {
    Write-Host '[2/3] 正在启动后端 ...' -ForegroundColor Yellow
    # 库不存在时后端会启动失败并刷一屏 SQLException，先在这里拦住
    if ($MySQLBin) {
        $mysqlCli = Join-Path $MySQLBin 'mysql.exe'
        $dbExists = (& $mysqlCli @('-h127.0.0.1', '-P3306', "-u$DbUser", "-p$DbPass", '-N', '-e', "SHOW DATABASES LIKE '$DbName';") 2>$null)
        if (!"$dbExists".Trim()) {
            # 有快照就自动导入，省掉“先导入再启动”这一步
            $dump = Join-Path $Root 'db\campus_activity_dump.sql'
            if (Test-Path $dump) {
                Write-Host "      数据库 $DbName 不存在，正在自动导入 db\campus_activity_dump.sql ..." -ForegroundColor Yellow
                & cmd /c "`"$mysqlCli`" -h127.0.0.1 -P3306 -u$DbUser -p$DbPass --default-character-set=utf8mb4 < `"$dump`"" 2>$null | Out-Null
                $dbExists = (& $mysqlCli @('-h127.0.0.1', '-P3306', "-u$DbUser", "-p$DbPass", '-N', '-e', "SHOW DATABASES LIKE '$DbName';") 2>$null)
            }
            if (!"$dbExists".Trim()) {
                Write-Host "      数据库 $DbName 不存在，且自动导入失败。" -ForegroundColor Red
                Write-Host '      手动导入：mysql --default-character-set=utf8mb4 -uroot -p < db\campus_activity_dump.sql' -ForegroundColor Yellow
                Pause-Exit 1
            }
            Write-Host '      数据库已就绪' -ForegroundColor Green
        }
    }
    if (!(Test-Path $Jar)) {
        # 有 Maven Wrapper 就自动编译，clone 下来不用先跑安装脚本
        $mvnw = Join-Path $Root 'backend\mvnw.cmd'
        if (Test-Path $mvnw) {
            Write-Host '      首次启动：正在编译后端（首次会下载 Maven，约 1-3 分钟）...' -ForegroundColor Yellow
            if (!$env:JAVA_HOME) {
                $javaForBuild = Resolve-Java $JavaExe
                if ($javaForBuild) { $env:JAVA_HOME = Split-Path -Parent (Split-Path -Parent $javaForBuild) }
            }
            Push-Location (Join-Path $Root 'backend')
            & cmd /c "`"$mvnw`" -q -DskipTests package"
            $buildRc = $LASTEXITCODE
            Pop-Location
            if ($buildRc -ne 0 -or !(Test-Path $Jar)) {
                Write-Host '      编译失败，请运行「首次安装.cmd」查看详细日志' -ForegroundColor Red
                Pause-Exit 1
            }
            Write-Host '      编译完成' -ForegroundColor Green
        } else {
            Write-Host "      找不到 jar：$Jar" -ForegroundColor Red
            Write-Host '      请先双击「首次安装.cmd」，或在 backend 目录执行 mvnw.cmd -DskipTests package' -ForegroundColor Yellow
            Pause-Exit 1
        }
    }
    $java = Resolve-Java $JavaExe
    if (!$java) {
        Write-Host '      找不到 java。请安装 JDK 17+，或在 scripts\start-all.ps1 顶部 $JavaExe 填写完整路径。' -ForegroundColor Red
        Pause-Exit 1
    }
    # Spring Boot 3 需要 JDK 17+，版本不对直接说清楚，别让它抛一堆 UnsupportedClassVersionError
    $verLine = (& $java -version 2>&1 | Select-Object -First 1)
    if ("$verLine" -match '"(\d+)') {
        $major = [int]$Matches[1]
        if ($major -lt 17) {
            Write-Host "      当前 Java 主版本 $major，低于 17，无法运行 Spring Boot 3。" -ForegroundColor Red
            Write-Host "      自动找到的是：$java" -ForegroundColor Red
            Write-Host '      请安装 JDK 17+，或在 $JavaExe 里指定正确的 java.exe。' -ForegroundColor Yellow
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
        Write-Host '      首次启动：正在安装前端依赖（约 1-5 分钟）...' -ForegroundColor Yellow
        Push-Location $FrontDir
        if (Test-Path (Join-Path $FrontDir 'package-lock.json')) {
            & cmd /c 'npm ci'
        } else {
            & cmd /c 'npm install'
        }
        $npmRc = $LASTEXITCODE
        Pop-Location
        if ($npmRc -ne 0 -or !(Test-Path $vite)) {
            Write-Host '      前端依赖安装失败，请运行「首次安装.cmd」查看详细日志' -ForegroundColor Red
            Pause-Exit 1
        }
        Write-Host '      依赖安装完成' -ForegroundColor Green
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
