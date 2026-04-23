# SPDX-FileCopyrightText: Copyright (c) 2026 Jason Perlow
# SPDX-License-Identifier: Apache-2.0
#
# nclawzero-agent-image — base + AI agent runtime (ZeroClaw / NemoClaw).
#
# Layered on top of `nclawzero-base-image`. Adds:
#   - zeroclaw-bin (public GitHub release, aarch64 Linux)
#   - nemoclaw-firstboot — first-boot provisioner that clones
#     NemoClaw from `github.com/NVIDIA/NemoClaw` on device and
#     applies the security/integration patchset shipped in
#     /etc/nemoclaw/patches/.
#   - node.js runtime
#
# Requires network at first boot (the NemoClaw clone fetches from
# GitHub). All sources are public — no internal URLs, no bundled
# binaries from any restricted source.

require nclawzero-base-image.bb

SUMMARY = "nclawzero + ZeroClaw/NemoClaw agent stack (provisions on first boot)"

IMAGE_INSTALL:append = " \
    packagegroup-nclawzero-agent \
"

# Agent workspace + NemoClaw runtime directories need extra rootfs
# headroom for the npm install + venv on first boot.
IMAGE_ROOTFS_EXTRA_SPACE = "524288"
