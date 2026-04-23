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

case "${1:-agent}" in
  agent)
    exec /usr/local/bin/zeroclaw daemon
    ;;
  shell)
    exec /bin/sh
    ;;
  *)
    exec "$@"
    ;;
esac
