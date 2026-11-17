#!/usr/bin/env bash
set -euo pipefail
PACKAGE="${1:?Usage: release.sh package-directory}"
APP_ROOT="${CAMPUS_APP_ROOT:-/opt/activity-system}"
[[ -f "$PACKAGE/SHA256SUMS" && -f "$PACKAGE/app/activity-system-1.0.0.jar" ]] || exit 1
(cd "$PACKAGE" && sha256sum --check SHA256SUMS)
ID="$(date -u +%Y%m%dT%H%M%SZ)-$(sha256sum "$PACKAGE/app/activity-system-1.0.0.jar" | cut -c1-12)"
DEST="$APP_ROOT/releases/$ID"
[[ ! -e "$DEST" ]] || { echo 'Release already exists'; exit 1; }
mkdir -p "$DEST" "$APP_ROOT/state/uploads" "$APP_ROOT/state/logs"
cp -a "$PACKAGE/." "$DEST/"
chown -R root:activity "$DEST"
find "$DEST" -type d -exec chmod 550 {} +
find "$DEST" -type f -exec chmod 440 {} +
chmod -R a-w "$DEST"
PREVIOUS="$(readlink -f "$APP_ROOT/current" || true)"
# Migration credentials belong to this process, never to the runtime service.
java -Dloader.main=com.campus.activity.tools.MigrationTool -cp "$DEST/app/activity-system-1.0.0.jar" org.springframework.boot.loader.launch.PropertiesLauncher ${CAMPUS_BASELINE_ARGUMENT:+"$CAMPUS_BASELINE_ARGUMENT"}
ln -s "$DEST" "$APP_ROOT/current.next"
mv -Tf "$APP_ROOT/current.next" "$APP_ROOT/current"
systemctl restart activity-system || true
READY=0
for _ in $(seq 1 30); do
  if curl -fsS http://127.0.0.1:8080/actuator/health/readiness | python3 -c 'import json,sys;sys.exit(json.load(sys.stdin).get("status")!="UP")'; then READY=1; break; fi
  sleep 2
done
if [[ "$READY" != 1 ]]; then
  if [[ -n "$PREVIOUS" ]]; then ln -s "$PREVIOUS" "$APP_ROOT/current.rollback"; mv -Tf "$APP_ROOT/current.rollback" "$APP_ROOT/current"; systemctl restart activity-system; fi
  echo 'Candidate failed readiness; previous release restored when available. Database migrations are not automatically reversed.' >&2
  exit 1
fi
nginx -t
systemctl reload nginx
printf '%s\n' "$PREVIOUS" > "$APP_ROOT/state/previous-release"
echo "Release ready: $ID"
