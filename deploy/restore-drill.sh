#!/usr/bin/env bash
set -euo pipefail
umask 077
ARCHIVE="${1:?Usage: restore-drill.sh archive target-schema}"
SCHEMA="${2:?Set an isolated restore schema}"
[[ "$SCHEMA" =~ ^campus_restore_[A-Za-z0-9_]+$ ]] || { echo 'Refusing non-isolated restore schema'; exit 1; }
: "${CAMPUS_BACKUP_PASSPHRASE:?}"
: "${CAMPUS_BACKUP_MYSQL_DEFAULTS:?}"
sha256sum --check "$ARCHIVE.sha256"
python3 - "$ARCHIVE" <<'PY'
import hashlib,hmac,os,sys
key=hashlib.pbkdf2_hmac('sha256',os.environ['CAMPUS_BACKUP_PASSPHRASE'].encode(),b'campus-backup-auth-v1',200000)
auth=hmac.new(key,digestmod=hashlib.sha256)
with open(sys.argv[1],'rb') as source:
    for chunk in iter(lambda:source.read(1024*1024),b''): auth.update(chunk)
with open(sys.argv[1]+'.hmac') as source: expected=source.read().strip()
if not hmac.compare_digest(auth.hexdigest(),expected): raise SystemExit('Backup authentication failed')
PY
WORK="$(mktemp -d /tmp/campus-restore.XXXXXX)"
openssl enc -d -aes-256-cbc -pbkdf2 -iter 200000 -pass env:CAMPUS_BACKUP_PASSPHRASE -in "$ARCHIVE" -out "$WORK/backup.tar"
python3 - "$WORK/backup.tar" "$WORK" <<'PY'
import pathlib,sys,tarfile
root=pathlib.Path(sys.argv[2]).resolve()
with tarfile.open(sys.argv[1]) as archive:
    for member in archive.getmembers():
        target=(root/member.name).resolve()
        if not target.is_relative_to(root) or not (member.isfile() or member.isdir()): raise SystemExit('Unsafe archive member')
    archive.extractall(root)
PY
(cd "$WORK" && sha256sum --check MANIFEST.sha256)
mysql --defaults-extra-file="$CAMPUS_BACKUP_MYSQL_DEFAULTS" -e "CREATE DATABASE \`$SCHEMA\` CHARACTER SET utf8mb4"
mysql --defaults-extra-file="$CAMPUS_BACKUP_MYSQL_DEFAULTS" "$SCHEMA" < "$WORK/database.sql"
mysql --defaults-extra-file="$CAMPUS_BACKUP_MYSQL_DEFAULTS" "$SCHEMA" -e 'SELECT COUNT(*) AS activities FROM t_activity; SELECT COUNT(*) AS registrations FROM t_registration; SELECT COUNT(*) AS reviews FROM t_activity_review;'
echo "Restored isolated schema=$SCHEMA and uploads=$WORK/uploads. Validate business flows before recording a recovery PASS. Production files were not overwritten."
