#!/usr/bin/env bash
# Create application-local.yml from .example in each module (gitignored).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

copy_if_missing() {
  local dir="$1"
  local example="$dir/src/main/resources/application-local.yml.example"
  local target="$dir/src/main/resources/application-local.yml"
  if [[ ! -f "$example" ]]; then
    return 0
  fi
  if [[ -f "$target" ]]; then
    echo "skip (exists): $target"
  else
    cp "$example" "$target"
    echo "created: $target"
  fi
}

for module in apigateway user-service donation-service matching-service ai-service; do
  copy_if_missing "$ROOT/$module"
done

echo "Done. Eureka uses application.yml only (no local profile file)."
