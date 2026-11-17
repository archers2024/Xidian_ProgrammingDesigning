#!/usr/bin/env bash
set -euo pipefail
PACKAGE="${1:?Usage: upload.sh verified-package-directory user@host [ssh-port]}"
HOST="${2:?Set the SSH destination}"
PORT="${3:-22}"
[[ "$PORT" =~ ^[0-9]+$ && "$HOST" != -* ]] || exit 1
PACKAGE="$(cd "$PACKAGE" && pwd)"
(cd "$PACKAGE" && sha256sum --check SHA256SUMS)
NAME="$(basename "$PACKAGE")"
[[ "$NAME" =~ ^release-[0-9]+$ ]] || { echo 'Use scripts/package-release.mjs'; exit 1; }
ssh -p "$PORT" "$HOST" 'mkdir -p ~/activity-system-packages'
scp -P "$PORT" -r "$PACKAGE" "$HOST:~/activity-system-packages/"
printf 'Uploaded verified package: ~/activity-system-packages/%s\n' "$NAME"
