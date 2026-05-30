#!/usr/bin/env bash
# One-time scaffold helper — modules are committed; script kept for reference.
set -euo pipefail
REF="../be16-fin-3team-NylonMusk-BE/apigateway"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MODULES=(eureka apigateway user-service donation-service matching-service ai-service)

copy_wrapper() {
  local dir="$1"
  cp "$ROOT/$REF/gradlew" "$ROOT/$REF/gradlew.bat" "$dir/"
  mkdir -p "$dir/gradle/wrapper"
  cp "$ROOT/$REF/gradle/wrapper/"* "$dir/gradle/wrapper/"
  chmod +x "$dir/gradlew"
}

for m in "${MODULES[@]}"; do
  copy_wrapper "$ROOT/$m"
done

echo "Gradle wrappers copied."
