#Requires -Version 5.1
<#
  校园活动管理系统 - 首次安装
  编译后端 jar、安装前端依赖。只需跑一次，之后直接用「启动全部.cmd」。

  为什么需要这一步：
    仓库不提交 backend/target（编译产物）和 frontend/node_modules（约 210MB），
    clone 下来必须先构建一次。
#>

$ErrorActionPreference = 'Continue'

$Root     = Split-Path -Parent $PSScriptRoot
$BackDir  = Join-Path $Root 'backend'
$FrontDir = Join-Path $Root 'frontend'
$Jar      = Join-Path $BackDir 'target\activity-system-1.0.0.jar'

function Pause-Exit([int]$Code = 0) {
    if (!$env:CAMPUS_NO_PAUSE) { Read-Host '按回车退出' | Out-Null }
    exit $Code
}

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

function Resolve-Node {
    $c = Get-Command node.exe -ErrorAction SilentlyContinue
    if ($c) { return $c.Source }
    foreach ($p in @("$env:ProgramFiles\nodejs\node.exe", "${env:ProgramFiles(x86)}\nodejs\node.exe", "$env:LOCALAPPDATA\Programs\nodejs\node.exe")) {
        if (Test-Path $p) { return $p }
    }
    return $null
}

Write-Host ''
Write-Host '=========================================' -ForegroundColor Cyan
Write-Host '   首次安装（只需跑一次）' -ForegroundColor Cyan
Write-Host '=========================================' -ForegroundColor Cyan
Write-Host ''

# ---------- 1. 环境检查 ----------
Write-Host '[1/4] 检查环境 ...' -ForegroundColor Yellow
$java = Resolve-Java ''
if (!$java) {
    Write-Host '      没有找到 JDK。请先安装 JDK 17 或更高版本。' -ForegroundColor Red
    Write-Host '      下载：https://adoptium.net/temurin/releases/?version=17' -ForegroundColor Yellow
    Pause-Exit 1
}
$env:JAVA_HOME = Split-Path -Parent (Split-Path -Parent $java)
$verLine = (& $java -version 2>&1 | Select-Object -First 1)
if ("$verLine" -match '"(\d+)' -and [int]$Matches[1] -lt 17) {
    Write-Host "      JDK 版本过低（主版本 $($Matches[1])），需要 17+。" -ForegroundColor Red
    Write-Host "      找到的是：$java" -ForegroundColor Red
    Pause-Exit 1
}
Write-Host "      JDK  : $java" -ForegroundColor Green

$node = Resolve-Node
if (!$node) {
    Write-Host '      没有找到 Node.js。请先安装 Node.js 18 或更高版本。' -ForegroundColor Red
    Write-Host '      下载：https://nodejs.org/zh-cn/download' -ForegroundColor Yellow
    Pause-Exit 1
}
$env:PATH = (Split-Path -Parent $node) + ';' + $env:PATH
Write-Host "      Node : $node" -ForegroundColor Green

# ---------- 2. 后端构建 ----------
Write-Host '[2/4] 编译后端 ...' -ForegroundColor Yellow
if (Test-Path $Jar) {
    Write-Host '      已有 jar，跳过（要重新编译请先删除 backend\target）' -ForegroundColor Green
} else {
    $mvnw = Join-Path $BackDir 'mvnw.cmd'
    $mvnCmd = $null
    if (Test-Path $mvnw) {
        $mvnCmd = $mvnw
        Write-Host '      使用仓库自带 mvnw（首次会下载 Maven，约 10MB）' -ForegroundColor DarkGray
    } else {
        $m = Get-Command mvn.cmd -ErrorAction SilentlyContinue
        if ($m) { $mvnCmd = $m.Source }
    }
    if (!$mvnCmd) {
        Write-Host '      既没有 mvnw 也没有 mvn，无法编译。' -ForegroundColor Red
        Write-Host '      请安装 Maven 3.8+：https://maven.apache.org/download.cgi' -ForegroundColor Yellow
        Pause-Exit 1
    }
    Push-Location $BackDir
    & $mvnCmd -DskipTests package
    $rc = $LASTEXITCODE
    Pop-Location
    if ($rc -ne 0 -or !(Test-Path $Jar)) {
        Write-Host '      编译失败，看上面的 Maven 输出。' -ForegroundColor Red
        Pause-Exit 1
    }
    Write-Host '      编译完成' -ForegroundColor Green
}

# ---------- 3. 前端依赖 ----------
Write-Host '[3/4] 安装前端依赖 ...' -ForegroundColor Yellow
if (Test-Path (Join-Path $FrontDir 'node_modules')) {
    Write-Host '      已有 node_modules，跳过（要重装请先删除该目录）' -ForegroundColor Green
} else {
    Push-Location $FrontDir
    & npm ci
    $rc = $LASTEXITCODE
    Pop-Location
    if ($rc -ne 0) {
        Write-Host '      npm ci 失败。常见原因：网络慢或被墙。' -ForegroundColor Red
        Write-Host '      换国内镜像后重试：' -ForegroundColor Yellow
        Write-Host '        npm config set registry https://registry.npmmirror.com' -ForegroundColor Yellow
        Write-Host '        cd frontend && npm install' -ForegroundColor Yellow
        Pause-Exit 1
    }
    Write-Host '      安装完成' -ForegroundColor Green
}

# ---------- 4. 汇总 ----------
Write-Host '[4/4] 完成' -ForegroundColor Yellow
Write-Host ''
Write-Host '安装完成。下一步：' -ForegroundColor Cyan
Write-Host '  1) 如果本机没有 MySQL 服务，双击「初始化数据库.cmd」（约 30 秒）' -ForegroundColor White
Write-Host '  2) 双击「启动全部.cmd」' -ForegroundColor White
Write-Host '  3) 浏览器打开 http://localhost:5173 ，账号 admin / admin123' -ForegroundColor White
Write-Host ''
Pause-Exit 0
