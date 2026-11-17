#!/usr/bin/env bash
set -euo pipefail
ROOT="${CAMPUS_APP_ROOT:-/opt/activity-system}"
TARGET="$(cat "$ROOT/state/previous-release")"
RESOLVED="$(realpath "$TARGET")"
[[ "$RESOLVED" == "$ROOT/releases/"* && -f "$RESOLVED/app/activity-system-1.0.0.jar" ]] || { echo 'Invalid rollback target'; exit 1; }
# Only application rollback; destructive schema rollback is intentionally excluded.
ln -s "$RESOLVED" "$ROOT/current.rollback"
mv -Tf "$ROOT/current.rollback" "$ROOT/current"
systemctl restart activity-system
curl -fsS http://127.0.0.1:8080/actuator/health/readiness | python3 -c 'import json,sys;sys.exit(json.load(sys.stdin).get("status")!="UP")'
