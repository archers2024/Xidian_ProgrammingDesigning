#!/usr/bin/env bash
set -euo pipefail
umask 077
[[ "$EUID" == 0 ]] || { echo 'Run through sudo on the target server'; exit 1; }
PACKAGE="${1:?Usage: install.sh verified-package-directory}"
(cd "$PACKAGE" && sha256sum --check SHA256SUMS)
APP_ROOT="${CAMPUS_APP_ROOT:-/opt/activity-system}"
[[ "$APP_ROOT" == /opt/activity-system ]] || { echo 'This unit template uses /opt/activity-system'; exit 1; }
[[ ! -e "$APP_ROOT/current" ]] || { echo 'Existing installation: use release.sh'; exit 1; }
: "${CAMPUS_DOMAIN:?Set the HTTPS hostname}"
: "${CAMPUS_TLS_CERT:?Set an existing trusted certificate chain path}"
: "${CAMPUS_TLS_KEY:?Set the private key path}"
: "${CAMPUS_DB_PASSWORD:?Set a random runtime password}"
: "${CAMPUS_MIGRATION_PASSWORD:?Set a separate migration password}"
: "${CAMPUS_JWT_SECRET:?Set at least 32 random characters}"
: "${CAMPUS_BOOTSTRAP_PASSWORD:?Set the initial administrator password}"
[[ "$CAMPUS_DOMAIN" =~ ^[A-Za-z0-9.-]+$ && "$CAMPUS_DOMAIN" != *..* ]] || exit 1
[[ "$CAMPUS_DB_PASSWORD" =~ ^[A-Za-z0-9_@.-]{24,128}$ && "$CAMPUS_MIGRATION_PASSWORD" =~ ^[A-Za-z0-9_@.-]{24,128}$ ]] || { echo 'Database passwords must be 24-128 safe characters'; exit 1; }
[[ "${#CAMPUS_JWT_SECRET}" -ge 32 && "${#CAMPUS_BOOTSTRAP_PASSWORD}" -ge 12 ]] || exit 1
[[ -f "$CAMPUS_TLS_CERT" && -f "$CAMPUS_TLS_KEY" ]] || exit 1
[[ "$CAMPUS_TLS_CERT" =~ ^[/A-Za-z0-9_.-]+$ && "$CAMPUS_TLS_KEY" =~ ^[/A-Za-z0-9_.-]+$ ]] || exit 1
command -v java >/dev/null; command -v mysql >/dev/null; command -v nginx >/dev/null
mysql <<SQL
CREATE DATABASE IF NOT EXISTS campus_activity CHARACTER SET utf8mb4;
CREATE USER IF NOT EXISTS 'activity_runtime'@'127.0.0.1' IDENTIFIED BY '$CAMPUS_DB_PASSWORD';
ALTER USER 'activity_runtime'@'127.0.0.1' IDENTIFIED BY '$CAMPUS_DB_PASSWORD';
CREATE USER IF NOT EXISTS 'activity_migrator'@'127.0.0.1' IDENTIFIED BY '$CAMPUS_MIGRATION_PASSWORD';
ALTER USER 'activity_migrator'@'127.0.0.1' IDENTIFIED BY '$CAMPUS_MIGRATION_PASSWORD';
GRANT ALL PRIVILEGES ON campus_activity.* TO 'activity_migrator'@'127.0.0.1';
SQL
export CAMPUS_MIGRATION_USER=activity_migrator
java -Dloader.main=com.campus.activity.tools.MigrationTool -cp "$PACKAGE/app/activity-system-1.0.0.jar" org.springframework.boot.loader.launch.PropertiesLauncher ${CAMPUS_BASELINE_ARGUMENT:+"$CAMPUS_BASELINE_ARGUMENT"}
mysql -NBe "SELECT table_name FROM information_schema.tables WHERE table_schema='campus_activity' AND table_name<>'t_schema_history'" | while read -r TABLE; do
  [[ "$TABLE" =~ ^[A-Za-z0-9_]+$ ]] || exit 1
  mysql -e "GRANT SELECT,INSERT,UPDATE,DELETE ON campus_activity.\`$TABLE\` TO 'activity_runtime'@'127.0.0.1'"
done
id activity >/dev/null 2>&1 || useradd --system --no-create-home --shell /usr/sbin/nologin activity
install -d -m 750 -o root -g activity "$APP_ROOT"
install -d -m 750 /etc/activity-system "$APP_ROOT/releases"
chgrp activity "$APP_ROOT/releases"
install -d -m 750 -o activity -g activity "$APP_ROOT/state" "$APP_ROOT/state/uploads" "$APP_ROOT/state/logs"
export CAMPUS_DB_USER=activity_runtime CAMPUS_DB_URL='jdbc:mysql://127.0.0.1:3306/campus_activity?characterEncoding=UTF-8&serverTimezone=Asia/Shanghai'
export CAMPUS_CORS_ORIGINS="https://$CAMPUS_DOMAIN"
export CAMPUS_PUBLIC_ORIGIN="https://$CAMPUS_DOMAIN"
export CAMPUS_LOG_DIR="$APP_ROOT/state/logs"
python3 - /etc/activity-system/runtime.env <<'PY'
import os,sys
keys=['CAMPUS_DB_USER','CAMPUS_DB_PASSWORD','CAMPUS_DB_URL','CAMPUS_JWT_SECRET','CAMPUS_BOOTSTRAP_PASSWORD','CAMPUS_CORS_ORIGINS','CAMPUS_PUBLIC_ORIGIN','CAMPUS_LOG_DIR']
with open(sys.argv[1],'w') as output:
    for key in keys:
        value=os.environ[key]
        if '\n' in value or '\r' in value: raise SystemExit('Invalid environment value')
        output.write(key+'="'+value.replace('\\','\\\\').replace('"','\\"')+'"\n')
PY
chmod 600 /etc/activity-system/runtime.env
sed -e "s|__DOMAIN__|$CAMPUS_DOMAIN|g" -e "s|__CERT__|$CAMPUS_TLS_CERT|g" -e "s|__KEY__|$CAMPUS_TLS_KEY|g" "$PACKAGE/deploy/nginx.conf" > /etc/nginx/conf.d/activity-system.conf
install -m 644 "$PACKAGE/deploy/activity-system.service" /etc/systemd/system/activity-system.service
install -m 644 "$PACKAGE/deploy/logrotate.conf" /etc/logrotate.d/activity-system
nginx -t
systemctl daemon-reload
systemctl enable activity-system
bash "$PACKAGE/deploy/release.sh" "$PACKAGE"
systemctl reload nginx
echo 'Installed. Complete HTTPS/mobile checks and remove the one-time bootstrap value from the protected environment after first login.'
