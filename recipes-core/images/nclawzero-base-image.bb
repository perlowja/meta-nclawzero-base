# SPDX-FileCopyrightText: Copyright (c) 2026 Jason Perlow
# SPDX-License-Identifier: Apache-2.0
#
# nclawzero-base-image — minimal Linux + goodies for edge SBCs.
#
# A headless console image: kernel, systemd, SSH, networking, shell
# tools, Python. No GUI, no desktop compositor, no AI agent runtime.
# Intended as a clean starting point you can layer additional
# packagegroups onto (`packagegroup-nclawzero-agent` for the AI
# stack, `packagegroup-nclawzero-desktop` for Weston+VNC).
#
# Targets:
#   - raspberrypi4-64 (Pi 4 Model B, 2GB / 4GB / 8GB)
#   - jetson-orin-nano-devkit (via meta-tegra; SD-boot flow)
#
# Flash (Raspberry Pi):
#   bmaptool copy tmp/deploy/images/raspberrypi4-64/nclawzero-base-image-raspberrypi4-64.wic.gz /dev/sdX
#
# Flash (Jetson Orin Nano SD):
#   xz -d tmp/deploy/images/jetson-orin-nano-devkit/nclawzero-base-image-*.wic.xz
#   dd if=...wic of=/dev/sdX bs=4M status=progress conv=fsync

SUMMARY = "nclawzero — minimal Linux + goodies for edge SBCs (headless)"
DESCRIPTION = "Headless console image for Raspberry Pi and Jetson: \
    systemd + SSH + networking + shell/dev tools. No GUI, no AI agent. \
    Layer packagegroup-nclawzero-agent or -desktop on top if wanted."
LICENSE = "Apache-2.0"

inherit core-image

IMAGE_FEATURES += " \
    ssh-server-openssh \
    debug-tweaks \
"
# NOTE: `debug-tweaks` enables passwordless root. Remove it for
# production images — or add your own hardening packagegroup.

IMAGE_INSTALL = " \
    packagegroup-core-boot \
    packagegroup-core-full-cmdline \
    packagegroup-nclawzero-base \
    nclawzero-network \
    kernel-modules \
"

IMAGE_LINGUAS = ""

DISTRO_FEATURES:append = " systemd"
DISTRO_FEATURES_BACKFILL_CONSIDERED:append = " sysvinit"
VIRTUAL-RUNTIME_init_manager = "systemd"
VIRTUAL-RUNTIME_initscripts = "systemd-compat-units"
