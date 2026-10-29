#requires -Version 7.0
[CmdletBinding()]
param(
    [ValidateSet('before', 'after', 'manual')]
    [string]$Phase = 'manual',
    [string]$Message = 'project checkpoint',
    [string]$Repository = (Split-Path -Parent $PSScriptRoot),
    [switch]$Push
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false
$repoRoot = (Resolve-Path -LiteralPath $Repository).Path
$backupRef = 'refs/heads/backup/ai-checkpoints'

function Invoke-ProjectGit {
    param([string[]]$GitArgs, [switch]$AllowFailure)
    $output = @(& git -C $repoRoot @GitArgs 2>&1)
    $code = $LASTEXITCODE
    $result = [pscustomobject]@{ Code = $code; Text = ($output -join "`n") }
    if ($code -ne 0 -and -not $AllowFailure) {
        throw "Git command failed: $($result.Text)"
    }
    return $result
}

# Git 在中文路径下可能按系统代码页输出目录，和 PowerShell 的路径字符串对不上。
# 仓库根目录直接用脚本位置，并用 .git/HEAD 确认，避免误读路径后写到别处。
$gitDir = Join-Path $repoRoot '.git'
if (-not (Test-Path -LiteralPath (Join-Path $gitDir 'HEAD'))) {
    throw 'Repository must be the Git project root.'
}
$indexPath = Join-Path $gitDir ('ai-backup-index-' + [guid]::NewGuid().ToString('N'))
$lockStream = $null
$previousIndex = $env:GIT_INDEX_FILE
$previousPrompt = $env:GIT_TERMINAL_PROMPT
$previousInteractive = $env:GCM_INTERACTIVE

try {
    $lockStream = [IO.File]::Open(
        (Join-Path $gitDir 'ai-backup.lock'), [IO.FileMode]::OpenOrCreate,
        [IO.FileAccess]::ReadWrite, [IO.FileShare]::None)
    $head = (Invoke-ProjectGit -GitArgs @('rev-parse', '--verify', 'HEAD')).Text.Trim()
    $previous = Invoke-ProjectGit -GitArgs @('rev-parse', '--verify', '--quiet', $backupRef) -AllowFailure
    $parent = $head
    if ($previous.Code -eq 0) {
        $parent = $previous.Text.Trim()
    } else {
        $remotePrevious = Invoke-ProjectGit -GitArgs @(
            'rev-parse', '--verify', '--quiet', 'refs/remotes/origin/backup/ai-checkpoints'
        ) -AllowFailure
        if ($remotePrevious.Code -eq 0) { $parent = $remotePrevious.Text.Trim() }
    }

    # An independent index leaves the user's staging area and current branch intact.
    $env:GIT_INDEX_FILE = $indexPath
    $null = Invoke-ProjectGit -GitArgs @('read-tree', $head)
    $null = Invoke-ProjectGit -GitArgs @('add', '-A', '--', '.')
    $tree = (Invoke-ProjectGit -GitArgs @('write-tree')).Text.Trim()
    $parentTree = (Invoke-ProjectGit -GitArgs @('rev-parse', ($parent + '^{tree}'))).Text.Trim()
    $checkpoint = $parent
    if ($tree -ne $parentTree) {
        $stamp = [DateTimeOffset]::Now.ToString('yyyy-MM-ddTHH:mm:sszzz')
        $checkpoint = (Invoke-ProjectGit -GitArgs @(
            '-c', 'user.name=Project Backup', '-c', 'user.email=project-backup@local.invalid',
            'commit-tree', $tree, '-p', $parent, '-m', "checkpoint($Phase): $stamp $Message"
        )).Text.Trim()
    }
    if ($previous.Code -ne 0 -or $checkpoint -ne $parent) {
        $expected = if ($previous.Code -eq 0) { $previous.Text.Trim() } else { '0' * $tree.Length }
        $null = Invoke-ProjectGit -GitArgs @('update-ref', $backupRef, $checkpoint, $expected)
    }
    Write-Output "LOCAL_OK $checkpoint $backupRef"

    if ($Push) {
        $env:GIT_TERMINAL_PROMPT = '0'
        $env:GCM_INTERACTIVE = 'Never'
        $remote = Invoke-ProjectGit -GitArgs @('remote', 'get-url', 'origin') -AllowFailure
        if ($remote.Code -ne 0) {
            Write-Warning "REMOTE_FAILED: origin is not configured. Local checkpoint: $checkpoint"
            exit 2
        }
        $pushed = Invoke-ProjectGit -GitArgs @('push', 'origin', "${backupRef}:${backupRef}") -AllowFailure
        if ($pushed.Code -ne 0) {
            Write-Warning "REMOTE_FAILED: local checkpoint $checkpoint is preserved. $($pushed.Text)"
            exit 2
        }
        Write-Output "REMOTE_OK origin $backupRef"
    }
} finally {
    $env:GIT_INDEX_FILE = $previousIndex
    $env:GIT_TERMINAL_PROMPT = $previousPrompt
    $env:GCM_INTERACTIVE = $previousInteractive
    foreach ($temporaryFile in @($indexPath, ($indexPath + '.lock'))) {
        if (Test-Path -LiteralPath $temporaryFile) {
            Remove-Item -LiteralPath $temporaryFile -Force
        }
    }
    if ($null -ne $lockStream) { $lockStream.Dispose() }
}
