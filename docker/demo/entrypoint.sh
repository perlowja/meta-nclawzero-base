#!/bin/sh
# SPDX-License-Identifier: Apache-2.0
#
# nclawzero-demo entrypoint
#
# Multi-process supervisor for the "one container, live chat"
# demo. Starts zeroclaw in the background, waits for its API to
# come up, then exec's zterm in the foreground.
#
# When zterm exits (user hits Alt-X / F10 → File → Exit), this
# script signals zeroclaw and returns — making the container
# clean-exit-friendly with `docker run --rm`.
#
# The agent image's entrypoint is single-process by design; the
# demo image overrides it because the demo's value is the live
# REPL, not compose/k8s lifecycle correctness.

set -e

ZEROCLAW_PORT=42617
ZEROCLAW_URL="http://127.0.0.1:${ZEROCLAW_PORT}/health"

cleanup() {
  if [ -n "${ZEROCLAW_PID:-}" ]; then
    kill "${ZEROCLAW_PID}" 2>/dev/null || true
    wait "${ZEROCLAW_PID}" 2>/dev/null || true
  fi
}
trap cleanup EXIT INT TERM

# Start zeroclaw backgrounded. Redirect its stdout to a log file
# the user can tail from another shell if they docker exec in.
mkdir -p /var/lib/zeroclaw/logs
/usr/local/bin/zeroclaw daemon >/var/lib/zeroclaw/logs/zeroclaw.log 2>&1 &
ZEROCLAW_PID=$!

# Wait up to 30s for the health endpoint to respond.
# curl is preferred over wget — wget isn't in the debian-slim base, curl is
# added in the agent Dockerfile's final-stage apt install.
i=0
while [ $i -lt 30 ]; do
  if curl -fsS "${ZEROCLAW_URL}" >/dev/null 2>&1; then
    break
  fi
  sleep 1
  i=$((i + 1))
done

if [ $i -ge 30 ]; then
  echo "zeroclaw did not come up within 30s; check /var/lib/zeroclaw/logs/zeroclaw.log" >&2
  echo "dropping into shell for debugging:" >&2
  exec /bin/sh
fi

# Foreground zterm. Default workspace points at the local zeroclaw.
exec /usr/local/bin/zterm tui --remote "http://127.0.0.1:${ZEROCLAW_PORT}"
