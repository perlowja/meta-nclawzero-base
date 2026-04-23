# SPDX-FileCopyrightText: Copyright (c) 2026 Jason Perlow
# SPDX-License-Identifier: Apache-2.0
#
# nclawzero-zeroclaw-image — base + ZeroClaw daemon + zterm REPL.
#
# Lightweight agent-capable image that OMITS NemoClaw and Node.js.
# Use this when the target has limited RAM / storage and a plain
# ZeroClaw gateway + interactive terminal REPL is sufficient.
#
# Difference vs `nclawzero-agent-image`:
#   - No NemoClaw runtime
#   - No Node.js
#   - No first-boot provisioner (no outbound HTTPS required at boot)
#
# Primary target: Raspberry Pi 4 2 GB / Pi Zero 2 W / Jetson Orin
# Nano on a modest SD card. Also works on any machine the agent
# image supports.
#
# Users who later want the full agent stack can either reflash with
# `nclawzero-agent-image` or runtime-install NemoClaw themselves.

require nclawzero-base-image.bb

SUMMARY = "nclawzero + ZeroClaw gateway + zterm REPL (no NemoClaw)"

IMAGE_INSTALL:append = " \
    packagegroup-nclawzero-zeroclaw \
    packagegroup-nclawzero-zterm \
"

# Modest headroom — the two binaries fit comfortably in the stock
# rootfs size; this is a small top-up for agent state + user
# workspace. Compare to agent-image's 524288 (for npm install +
# NemoClaw provisioning).
IMAGE_ROOTFS_EXTRA_SPACE = "65536"
