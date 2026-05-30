#!/usr/bin/env bash
# Create or sync application-local.yml from .example in each module (gitignored).
# Usage:
#   bash scripts/setup-local-config.sh          # create only if missing
#   bash scripts/setup-local-config.sh --sync   # overwrite from .example
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SYNC=false
if [[ "${1:-}" == "--sync" ]]; then
  SYNC=true
fi

copy_or_sync() {
  local dir="$1"
  local example="$dir/src/main/resources/application-local.yml.example"
  local target="$dir/src/main/resources/application-local.yml"
  if [[ ! -f "$example" ]]; then
    return 0
  fi
  if [[ -f "$target" && "$SYNC" == false ]]; then
    echo "skip (exists): $target"
  else
    cp "$example" "$target"
    echo "$([[ "$SYNC" == true ]] && echo synced || echo created): $target"
  fi
}

for module in apigateway user-service donation-service matching-service ai-service; do
  copy_or_sync "$ROOT/$module"
done

echo "Done. Eureka uses application.yml only."
echo "MySQL schemas: mealbridge_user | mealbridge_donation | mealbridge_matching | mealbridge_ai"
