#!/usr/bin/env bash
# 在本机（Windows Git Bash）执行，把部署所需文件上传到服务器。
# 用法：bash upload.sh root@<公网IP>
# 依赖：Windows 10/11 自带 OpenSSH 客户端；没有 scp 时改用 WinSCP / FinalShell 手动传。
set -euo pipefail

HOST="${1:-}"
if [[ -z "$HOST" ]]; then
  echo "用法: bash upload.sh root@<公网IP>"
  echo "示例: bash upload.sh root@47.98.123.45"
  exit 1
fi

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "==> 在服务器创建目录"
ssh "$HOST" 'mkdir -p ~/activity-system-pkg/dist ~/activity-system-pkg/deploy'

echo "==> 上传后端 jar（约 51MB）与 init.sql"
scp "$ROOT/backend/target/activity-system-1.0.0.jar" "$HOST:~/activity-system-pkg/"
scp "$ROOT/db/init.sql" "$HOST:~/activity-system-pkg/"

echo "==> 上传前端 dist（约 2.6MB）"
scp -r "$ROOT/frontend/dist/." "$HOST:~/activity-system-pkg/dist/"

echo "==> 上传部署包"
scp "$ROOT"/deploy/* "$HOST:~/activity-system-pkg/deploy/"

echo
echo "上传完成。接下来 SSH 登录服务器执行："
echo "  cd ~/activity-system-pkg/deploy"
echo "  sudo bash setup.sh <公网IP>"
