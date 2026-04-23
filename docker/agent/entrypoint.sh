#!/bin/sh
# SPDX-License-Identifier: Apache-2.0
#
# nclawzero-agent entrypoint
#
# Modes:
#   agent   — default. Exec zeroclaw in the foreground as pid 1 (via tini).
#   shell   — drop into /bin/sh for debugging. `docker run -it ... shell`.
#   <other> — exec it verbatim (escape hatch: override with any command).
#
# The nclawzero-demo image overrides this entrypoint with its own
# multi-process supervisor that backgrounds zeroclaw and foregrounds
# zterm. The agent image stays single-process so the exit code of
# pid 1 directly reflects zeroclaw's exit, which is what compose /
# k8s liveness probes want.

set -e

# Container bind override. Without --host, zeroclaw's auto-generated
# config at $HOME/.zeroclaw/config.toml binds to 127.0.0.1, which makes
# Docker's -p port publish a no-op. 0.0.0.0 makes the gateway actually
# reachable from outside the container. Override via ZEROCLAW_HOST if
# you're composing a network namespace where that matters.
ZC_HOST="${ZEROCLAW_HOST:-0.0.0.0}"
ZC_PORT="${ZEROCLAW_PORT:-42617}"

case "${1:-agent}" in
  agent)
    exec /usr/local/bin/zeroclaw daemon --host "${ZC_HOST}" --port "${ZC_PORT}"
    ;;
  shell)
    exec /bin/sh
    ;;
  *)
    exec "$@"
    ;;
esac
