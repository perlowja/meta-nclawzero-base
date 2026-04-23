# SPDX-FileCopyrightText: Copyright (c) 2026 Jason Perlow
# SPDX-License-Identifier: Apache-2.0
#
# nclawzero-demo — "try before you flash" bench image.
#
# Layers the zterm TUI client on top of nclawzero-agent and provides
# a multi-process entrypoint: zeroclaw daemon backgrounded, zterm
# foregrounded in the user's terminal. One container, one command,
# live chat with the agent in ~5 seconds (first pull excluded).
#
# Usage:
#   docker buildx build --platform=linux/amd64,linux/arm64 \
#       --push -t ghcr.io/perlowja/nclawzero-demo:latest \
#       -f docker/demo/Dockerfile .
#
#   docker run -it --rm ghcr.io/perlowja/nclawzero-demo
#
# zterm is cargo-built from pinned-tag source. Rebuilds under
# qemu arm64 emulation are slow (~15-20 min multi-arch first build).
# Planned follow-up: perlowja/zterm v0.2.1 will add a GH Actions
# release workflow producing prebuilt tarballs, at which point
# this Dockerfile swaps to fetch-instead-of-compile.

ARG DEBIAN_VERSION=bookworm
# Base on the agent image — shares all layers, reduces storage
# footprint and guarantees runtime parity between the two.
ARG NCLAWZERO_AGENT_IMAGE=ghcr.io/perlowja/nclawzero-agent:latest
ARG ZTERM_VERSION=v0.2.0
ARG RUST_VERSION=1.82

# ──────────────────────────────────────────────────────────────
# Stage 1 — cargo build zterm from pinned tag
# ──────────────────────────────────────────────────────────────
FROM rust:${RUST_VERSION}-${DEBIAN_VERSION}-slim AS zterm-build
ARG ZTERM_VERSION

RUN apt-get update && apt-get install -y --no-install-recommends \
        git ca-certificates pkg-config \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /build
RUN git clone --depth 1 --branch ${ZTERM_VERSION} \
        https://github.com/perlowja/zterm.git .

# Release build. Rust's strip is enabled in zterm's Cargo.toml
# already (profile.release.strip = true).
RUN cargo build --release --locked

# ──────────────────────────────────────────────────────────────
# Stage 2 — final demo image
# ──────────────────────────────────────────────────────────────
FROM ${NCLAWZERO_AGENT_IMAGE}

LABEL org.opencontainers.image.title="nclawzero-demo"
LABEL org.opencontainers.image.description="Interactive demo: ZeroClaw + NemoClaw + zterm TUI"
LABEL org.opencontainers.image.source="https://github.com/perlowja/meta-nclawzero-base"
LABEL org.opencontainers.image.licenses="Apache-2.0"
LABEL org.opencontainers.image.authors="Jason Perlow <jperlow@gmail.com>"

USER root
COPY --from=zterm-build /build/target/release/zterm /usr/local/bin/zterm
COPY docker/demo/entrypoint.sh /usr/local/bin/demo-entrypoint.sh
RUN chmod 755 /usr/local/bin/zterm /usr/local/bin/demo-entrypoint.sh

USER zeroclaw

# Override the agent image's CMD so `docker run -it <demo>` lands
# the user in zterm + background zeroclaw, not the agent's
# headless CMD ["agent"].
CMD []

# tini comes from the base agent image; override the entrypoint
# to the demo-specific supervisor.
ENTRYPOINT ["/usr/bin/tini", "--", "/usr/local/bin/demo-entrypoint.sh"]
