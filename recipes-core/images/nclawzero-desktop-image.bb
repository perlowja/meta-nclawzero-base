# SPDX-FileCopyrightText: Copyright (c) 2026 Jason Perlow
# SPDX-License-Identifier: Apache-2.0
#
# nclawzero-desktop-image — base + Weston/VNC lightweight desktop.
#
# Layered on top of `nclawzero-base-image`. Adds the Weston Wayland
# compositor with its built-in VNC backend so you can `vncviewer
# <device-ip>:5901` from your workstation and get a lightweight
# desktop session without installing a full X stack on-device.
#
# Intended for 4GB+ RAM boards; the 2GB Raspberry Pi 4 model should
# stick to `nclawzero-base-image` (headless).
#
# This image does NOT include the AI agent stack. Combine with
# `nclawzero-agent-image`-style inclusion if you want both — or
# add `packagegroup-nclawzero-agent` to `IMAGE_INSTALL` in a local
# `.bbappend`.

require nclawzero-base-image.bb

SUMMARY = "nclawzero + Weston/VNC lightweight desktop (no agent)"

IMAGE_INSTALL:append = " \
    packagegroup-nclawzero-desktop \
"

DISTRO_FEATURES:append = " wayland opengl"
DISTRO_FEATURES:remove = "x11"

IMAGE_ROOTFS_EXTRA_SPACE = "1048576"
