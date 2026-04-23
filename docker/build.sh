#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
#
# Build + push the nclawzero-agent and nclawzero-demo Docker images.
#
# Run from the repository root. Requires:
#   - docker 23+ with buildx
#   - a buildx builder with docker-container driver (one-time setup)
#   - `docker login ghcr.io` as perlowja with a token that has
#     write:packages scope
#
# Usage:
#   ./docker/build.sh              # build + push latest tags
#   ./docker/build.sh --no-push    # local build only (amd64 cached,
#                                    arm64 loaded via emulation)
#   ./docker/build.sh --tag v0.1.0 # override the tag (both images)
#
# Multi-arch caveat: the demo image cargo-builds zterm, which
# under linux/arm64 buildx emulation takes ~15-20 min. The agent
# image is pure binary-fetch, much faster (~2-3 min per arch).

set -euo pipefail

REGISTRY="ghcr.io/perlowja"
TAG="latest"
PUSH=true
PLATFORMS="linux/amd64,linux/arm64"
BUILDER_NAME="nclawzero-builder"

while [ $# -gt 0 ]; do
  case "$1" in
    --tag) TAG="$2"; shift 2 ;;
    --no-push) PUSH=false; shift ;;
    --platforms) PLATFORMS="$2"; shift 2 ;;
    -h|--help)
      grep '^#' "$0" | sed 's/^#\s\{0,1\}//'
      exit 0
      ;;
    *)
      echo "unknown arg: $1" >&2; exit 1 ;;
  esac
done

cd "$(dirname "$0")/.."

# One-time: create a docker-container builder with multi-arch
# emulation. Idempotent — `create --use` errors if the name
# already exists, so we check first.
if ! docker buildx inspect "${BUILDER_NAME}" >/dev/null 2>&1; then
  echo "[build.sh] creating buildx builder: ${BUILDER_NAME}"
  docker buildx create --name "${BUILDER_NAME}" \
                       --driver docker-container \
                       --use \
                       --bootstrap
else
  docker buildx use "${BUILDER_NAME}"
fi

push_flag="--load"
if [ "${PUSH}" = true ]; then
  push_flag="--push"
fi

# --------------------------------------------------------------
# Agent image — built first so the demo image's FROM can resolve
# against it on the registry (or locally if --no-push).
# --------------------------------------------------------------
echo "[build.sh] building ${REGISTRY}/nclawzero-agent:${TAG}"
docker buildx build \
  --platform="${PLATFORMS}" \
  ${push_flag} \
  -t "${REGISTRY}/nclawzero-agent:${TAG}" \
  -f docker/agent/Dockerfile \
  .

# --------------------------------------------------------------
# Demo image — FROM nclawzero-agent. Only reachable after the
# agent image is either --pushed or --loaded into the local
# docker engine.
# --------------------------------------------------------------
echo "[build.sh] building ${REGISTRY}/nclawzero-demo:${TAG}"
docker buildx build \
  --platform="${PLATFORMS}" \
  ${push_flag} \
  --build-arg "NCLAWZERO_AGENT_IMAGE=${REGISTRY}/nclawzero-agent:${TAG}" \
  -t "${REGISTRY}/nclawzero-demo:${TAG}" \
  -f docker/demo/Dockerfile \
  .

if [ "${PUSH}" = true ]; then
  echo "[build.sh] done. Pushed to:"
  echo "  ${REGISTRY}/nclawzero-agent:${TAG}"
  echo "  ${REGISTRY}/nclawzero-demo:${TAG}"
else
  echo "[build.sh] done (local load, no push)."
fi
