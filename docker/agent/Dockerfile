# SPDX-FileCopyrightText: Copyright (c) 2026 Jason Perlow
# SPDX-License-Identifier: Apache-2.0
#
# nclawzero-agent — headless runtime for ZeroClaw + NemoClaw.
#
# The canonical base image for the Docker portfolio. The demo image
# (nclawzero-demo) layers zterm + an interactive entrypoint on top
# of this.
#
# Built for linux/amd64 and linux/arm64 via buildx. Both platforms
# pull prebuilt ZeroClaw binaries (upstream ships aarch64 + x86_64
# tarballs). Node.js 22 LTS is the glibc-linked prebuilt from
# nodejs.org, same architectures.
#
# NemoClaw is git-cloned and npm-installed during build. The commit
# SHA is pinned explicitly (not main HEAD) so every `docker pull`
# of a given tag gets identical bits — registry artifacts need
# reproducibility, even while the sibling Yocto recipe tracks
# upstream HEAD. See feedback_head_default_upstream.md for the
# policy carve-out.
#
# Usage:
#   docker buildx build --platform=linux/amd64,linux/arm64 \
#       --push -t ghcr.io/perlowja/nclawzero-agent:latest \
#       -f docker/agent/Dockerfile .
#
#   docker run -d -p 42617:42617 ghcr.io/perlowja/nclawzero-agent
#
# Exposes:
#   42617 — ZeroClaw gateway (webhook + WebSocket; pairing required
#           by default — read the code from `docker logs` or override
#           config at /etc/zeroclaw/zeroclaw.toml).

ARG DEBIAN_VERSION=bookworm
ARG ZEROCLAW_VERSION=0.7.3
ARG NODEJS_VERSION=22.22.2
# NemoClaw main branch commit as of 2026-04-23; update by rerunning
# `gh api repos/NVIDIA/NemoClaw/commits/main --jq .sha` and bumping
# this value when you want to pick up upstream changes.
ARG NEMOCLAW_REV=885c75b73d14f223c9cd5cd45e5274d46472b280

# ──────────────────────────────────────────────────────────────
# Stage 1 — fetch prebuilt binaries keyed on $TARGETARCH
# ──────────────────────────────────────────────────────────────
FROM --platform=$BUILDPLATFORM debian:${DEBIAN_VERSION}-slim AS fetch
ARG ZEROCLAW_VERSION
ARG NODEJS_VERSION
ARG TARGETARCH

RUN apt-get update && apt-get install -y --no-install-recommends \
        curl ca-certificates xz-utils \
    && rm -rf /var/lib/apt/lists/*

# ZeroClaw per-arch: upstream ships x86_64-unknown-linux-gnu and
# aarch64-unknown-linux-gnu. Hardcode SHA256 per arch so a compromised
# GitHub mirror can't silently swap bits.
RUN set -eux; \
    case "${TARGETARCH}" in \
      amd64) \
        ZC_TRIPLE=x86_64-unknown-linux-gnu; \
        ZC_SHA256=1036ab4c01b9ed9e397795454c03d376dbb6288ee722eef5d2bc6b807a3b0a01 ;; \
      arm64) \
        ZC_TRIPLE=aarch64-unknown-linux-gnu; \
        ZC_SHA256=5dbe0991914fdf29b6f3f29364aad5de20eca4bb92dd9c1491b89aef0676bcf5 ;; \
      *) \
        echo "unsupported TARGETARCH: ${TARGETARCH}" >&2; exit 1 ;; \
    esac; \
    curl -fsSL -o /tmp/zeroclaw.tgz \
        "https://github.com/zeroclaw-labs/zeroclaw/releases/download/v${ZEROCLAW_VERSION}/zeroclaw-${ZC_TRIPLE}.tar.gz"; \
    echo "${ZC_SHA256}  /tmp/zeroclaw.tgz" | sha256sum -c -; \
    mkdir -p /opt/zeroclaw; \
    tar -xzf /tmp/zeroclaw.tgz -C /opt/zeroclaw --strip-components=0; \
    rm /tmp/zeroclaw.tgz

# Node.js per-arch: official nodejs.org prebuilts.
RUN set -eux; \
    case "${TARGETARCH}" in \
      amd64) \
        NODE_ARCH=x64; \
        NODE_SHA256=88fd1ce767091fd8d4a99fdb2356e98c819f93f3b1f8663853a2dee9b438068a ;; \
      arm64) \
        NODE_ARCH=arm64; \
        NODE_SHA256=e9e1930fd321a470e29bb68f30318bf58e3ecb4acb4f1533fb19c58328a091fe ;; \
      *) \
        echo "unsupported TARGETARCH: ${TARGETARCH}" >&2; exit 1 ;; \
    esac; \
    curl -fsSL -o /tmp/node.txz \
        "https://nodejs.org/dist/v${NODEJS_VERSION}/node-v${NODEJS_VERSION}-linux-${NODE_ARCH}.tar.xz"; \
    echo "${NODE_SHA256}  /tmp/node.txz" | sha256sum -c -; \
    mkdir -p /opt/node; \
    tar -xJf /tmp/node.txz -C /opt/node --strip-components=1; \
    rm /tmp/node.txz

# ──────────────────────────────────────────────────────────────
# Stage 2 — clone + npm-install NemoClaw at a pinned SHA
# ──────────────────────────────────────────────────────────────
FROM debian:${DEBIAN_VERSION}-slim AS nemoclaw-build
ARG NEMOCLAW_REV

RUN apt-get update && apt-get install -y --no-install-recommends \
        git ca-certificates \
    && rm -rf /var/lib/apt/lists/*

COPY --from=fetch /opt/node /opt/node
ENV PATH=/opt/node/bin:$PATH

# Pinned-commit clone. `git -c advice.detachedHead=false` quiets
# the detached-HEAD warning — intentional, not a misconfiguration.
RUN git clone --no-checkout https://github.com/NVIDIA/NemoClaw.git /opt/nemoclaw \
    && cd /opt/nemoclaw \
    && git -c advice.detachedHead=false checkout ${NEMOCLAW_REV} \
    && rm -rf .git .github docs test \
    && npm install --production --no-optional --ignore-scripts

# ──────────────────────────────────────────────────────────────
# Stage 3 — final runtime image
# ──────────────────────────────────────────────────────────────
FROM debian:${DEBIAN_VERSION}-slim

LABEL org.opencontainers.image.title="nclawzero-agent"
LABEL org.opencontainers.image.description="Headless ZeroClaw + NemoClaw AI agent runtime"
LABEL org.opencontainers.image.source="https://github.com/perlowja/meta-nclawzero-base"
LABEL org.opencontainers.image.licenses="Apache-2.0"
LABEL org.opencontainers.image.authors="Jason Perlow <jperlow@gmail.com>"

RUN apt-get update && apt-get install -y --no-install-recommends \
        ca-certificates \
        tini \
        libstdc++6 \
    && rm -rf /var/lib/apt/lists/* /tmp/*

# Binaries + runtime
COPY --from=fetch /opt/zeroclaw/zeroclaw /usr/local/bin/zeroclaw
COPY --from=fetch /opt/node /opt/node
COPY --from=nemoclaw-build /opt/nemoclaw /opt/nemoclaw
ENV PATH=/opt/node/bin:$PATH

# ZeroClaw expects writable config + state.
RUN useradd --system --create-home --home-dir /var/lib/zeroclaw \
        --shell /usr/sbin/nologin zeroclaw \
    && mkdir -p /var/lib/zeroclaw/workspace /var/lib/zeroclaw/skills \
                /var/lib/nemoclaw /etc/zeroclaw \
    && chown -R zeroclaw:zeroclaw /var/lib/zeroclaw /var/lib/nemoclaw /etc/zeroclaw

COPY docker/agent/entrypoint.sh /usr/local/bin/entrypoint.sh
COPY recipes-zeroclaw/zeroclaw/files/zeroclaw.toml /etc/zeroclaw/zeroclaw.toml

RUN chmod 755 /usr/local/bin/entrypoint.sh \
    && chmod 644 /etc/zeroclaw/zeroclaw.toml

USER zeroclaw
WORKDIR /var/lib/zeroclaw

EXPOSE 42617

ENTRYPOINT ["/usr/bin/tini", "--", "/usr/local/bin/entrypoint.sh"]
CMD ["agent"]
