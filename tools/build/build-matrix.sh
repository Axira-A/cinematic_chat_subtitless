#!/usr/bin/env bash
set -uo pipefail
cd "$(dirname "$0")/../.."
mkdir -p dist
smoke=false
if [[ "${1:-}" == "--smoke" ]]; then smoke=true; fi
failed=0
while IFS=$'\t' read -r mc loader java artifact; do
  started=$(date -u +'%Y-%m-%dT%H:%M:%SZ')
  entry=legacy
  [[ "$java" == 25 ]] && entry=modern
  args=(-p "$entry" "-PmcVersion=$mc" "-Ploader=$loader" --no-parallel)
  build_status=Fail
  launch_status='Not run'
  client_command=''
  printf -v build_command '%q ' bash "$entry/gradlew" "${args[@]}" ":$loader:build" check
  if bash "$entry/gradlew" "${args[@]}" ":$loader:build" check 2>&1 | tee "dist/mc$mc-$loader-build.log"; then
    if cp "targets/$mc/$loader/build/libs/$artifact" dist/; then build_status=Pass; else failed=1; fi
    if [[ "$build_status" == Pass && "$smoke" == true ]]; then
      printf -v client_command '%q ' bash "$entry/gradlew" "${args[@]}" ":$loader:runSmoke"
      if bash "$entry/gradlew" "${args[@]}" ":$loader:runSmoke" 2>&1 | tee "dist/mc$mc-$loader-client.log"; then launch_status=Pass; else launch_status='Fail or incomplete'; failed=1; fi
      if [[ -f "targets/$mc/$loader/run-smoke/smoke-report.json" ]]; then
        if ! cp "targets/$mc/$loader/run-smoke/smoke-report.json" "dist/mc$mc-$loader-scenario.json"; then launch_status='Fail or incomplete'; failed=1; fi
      else
        if [[ "$launch_status" == Pass ]]; then launch_status='Fail or incomplete'; failed=1; fi
      fi
    fi
  else
    failed=1
  fi
  finished=$(date -u +'%Y-%m-%dT%H:%M:%SZ')
  python3 tools/reports/write-matrix-report.py --record-target "$mc" "$loader" --started-at "$started" --finished-at "$finished" \
    --build-command "$build_command" --client-command "$client_command" --build-status "$build_status" --launch-status "$launch_status" || failed=1
done < <(python3 -c 'import json; d=json.load(open("gradle/targets.json")); [print(t["minecraft"],t["loader"],t["java"],t["artifact"],sep="\t") for t in d["targets"]]')
python3 tools/reports/write-matrix-report.py
exit "$failed"
