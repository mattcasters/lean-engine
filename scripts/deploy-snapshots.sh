#!/usr/bin/env bash
# Deploy org.lean snapshot modules to Nexus "lean" repository.
# Requires ~/.m2/settings.xml server id "lean" with valid credentials.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
LEAN_NEXUS_URL="${LEAN_NEXUS_URL:-https://repository.data-hopper.com/repository/lean/}"

echo "Deploying org.lean snapshots to ${LEAN_NEXUS_URL}"
echo "Maven server id expected in settings.xml: lean"
echo

deploy_module() {
  local dir="$1"
  shift
  echo "======== $(basename "$dir") ========"
  (cd "$dir" && mvn -B clean deploy -Dlean.nexus.url="${LEAN_NEXUS_URL}" "$@")
  echo
}

deploy_module "${ROOT}/lean-engine"
deploy_module "${ROOT}/lean-hop-plugins" -Dmaven.test.skip=false
deploy_module "${ROOT}/hop-lean-plugins" -Dmaven.test.skip=true
deploy_module "${ROOT}/lean-rest" -DskipTests

echo "Done. Browse: https://repository.data-hopper.com/#browse/browse:lean"
