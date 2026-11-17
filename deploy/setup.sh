#!/usr/bin/env bash
# Compatibility entry; dependencies and certificates must be prepared explicitly.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec bash "$SCRIPT_DIR/install.sh" "${1:?Pass a verified package directory; see deploy/README.md}"
