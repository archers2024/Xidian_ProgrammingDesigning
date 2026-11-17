#!/usr/bin/env bash
set -euo pipefail
umask 077
: "${CAMPUS_BACKUP_DIR:?Set a protected backup directory}"
: "${CAMPUS_BACKUP_PASSPHRASE:?Provide encryption passphrase through protected environment}"
: "${CAMPUS_BACKUP_MYSQL_DEFAULTS:?Path to a chmod 600 MySQL option file}"
ROOT="${CAMPUS_APP_ROOT:-/opt/activity-system}"
mkdir -p "$CAMPUS_BACKUP_DIR"
WORK="$(mktemp -d /tmp/campus-backup.XXXXXX)"
cleanup(){ [[ "$WORK" == /tmp/campus-backup.* && -d "$WORK" ]] && rm -rf -- "$WORK"; }
trap cleanup EXIT
# Pause writes before invoking this script when file-reference consistency is required.
mysqldump --defaults-extra-file="$CAMPUS_BACKUP_MYSQL_DEFAULTS" --single-transaction --routines --triggers --set-gtid-purged=OFF campus_activity > "$WORK/database.sql"
cp -a "$ROOT/state/uploads" "$WORK/uploads"
(cd "$WORK" && find database.sql uploads -type f -print0 | sort -z | xargs -0 sha256sum > MANIFEST.sha256)
NAME="campus-$(date -u +%Y%m%dT%H%M%SZ).tar.enc"
tar -C "$WORK" -cf - database.sql uploads MANIFEST.sha256 | openssl enc -aes-256-cbc -salt -pbkdf2 -iter 200000 -pass env:CAMPUS_BACKUP_PASSPHRASE -out "$CAMPUS_BACKUP_DIR/$NAME"
sha256sum "$CAMPUS_BACKUP_DIR/$NAME" > "$CAMPUS_BACKUP_DIR/$NAME.sha256"
python3 - "$CAMPUS_BACKUP_DIR/$NAME" <<'PY'
import hashlib,hmac,os,sys
key=hashlib.pbkdf2_hmac('sha256',os.environ['CAMPUS_BACKUP_PASSPHRASE'].encode(),b'campus-backup-auth-v1',200000)
auth=hmac.new(key,digestmod=hashlib.sha256)
with open(sys.argv[1],'rb') as source:
    for chunk in iter(lambda:source.read(1024*1024),b''): auth.update(chunk)
with open(sys.argv[1]+'.hmac','w') as output: output.write(auth.hexdigest())
PY
echo "Encrypted backup: $NAME; plaintext staging is removed on exit. Apply a documented retention policy to this protected directory."
