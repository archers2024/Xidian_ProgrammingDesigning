#!/usr/bin/env bash
set -euo pipefail
ORIGIN="${1:?Usage: smoke-test.sh https://domain}"
[[ "$ORIGIN" == https://* ]] || { echo 'HTTPS origin required'; exit 1; }
curl -fsS "$ORIGIN/" >/dev/null
curl -fsS "$ORIGIN/api/v1/actuator/health/readiness" | python3 -c 'import json,sys;sys.exit(json.load(sys.stdin).get("status")!="UP")'
STATUS="$(curl -s -o /dev/null -w '%{http_code}' "$ORIGIN/api/v1/__smoke_missing")"
[[ "$STATUS" == 404 ]]
[[ "$(curl -s -o /dev/null -w '%{http_code}' "$ORIGIN/api/v1/actuator/prometheus")" == 401 ]]
echo 'PASS: HTTPS, UI, readiness, HTTP errors and protected metrics'
