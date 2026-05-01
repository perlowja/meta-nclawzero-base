# SPDX-FileCopyrightText: Copyright (c) 2026 Jason Perlow
# SPDX-License-Identifier: Apache-2.0
#
# ZeroClaw — pre-built Rust AI agent runtime binary.
#
# This layer targets ARM edge SBCs. Per-architecture binary is
# selected based on TARGET_ARCH. Upstream ships releases for
# aarch64-linux-gnu and armv7-linux-gnueabihf (hard float) — both
# picked up automatically here. For Pi Zero / Pi 1 / Pi Zero W
# (ARMv6 hard-float), add a bbappend that overrides
# ZEROCLAW_TRIPLE to "arm-unknown-linux-gnueabihf"; Yocto's default
# TARGET_ARCH=arm selection here maps to the ARMv7 tarball, the
# right pick for Pi 2/3 32-bit and most modern ARM SBCs.
#
# Non-ARM targets (x86_64, RISC-V) are deliberately out of scope —
# upstream does ship an x86_64 binary, so widening COMPATIBLE_HOST
# and adding one override is a trivial downstream bbappend for
# users who need it.

SUMMARY = "ZeroClaw AI agent runtime (pre-built binary)"
DESCRIPTION = "Rust-based AI agent runtime with low memory footprint. \
    Runs at ~17MB RSS on Raspberry Pi 4. OpenAI-compatible API on port 42617."
HOMEPAGE = "https://github.com/zeroclaw-labs/zeroclaw"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

ZEROCLAW_VERSION = "0.7.4"

# Per-arch binary tuple. Override in a bbappend for Pi Zero / Pi 1.
ZEROCLAW_TRIPLE ?= "INVALID-override-for-this-TARGET_ARCH"
ZEROCLAW_TRIPLE:aarch64 = "aarch64-unknown-linux-gnu"
ZEROCLAW_TRIPLE:arm = "armv7-unknown-linux-gnueabihf"

SRC_URI = " \
    https://github.com/zeroclaw-labs/zeroclaw/releases/download/v${ZEROCLAW_VERSION}/zeroclaw-${ZEROCLAW_TRIPLE}.tar.gz;name=bin \
    file://zeroclaw.service \
    file://zeroclaw.toml \
"

# Per-arch SHA256 — indirected through a plain variable because
# BitBake varflag syntax (SRC_URI[bin.sha256sum]) does not accept
# :override suffixes. Override ZEROCLAW_SHA; the assignment below
# propagates the arch-correct value to the varflag.
# Verified against upstream SHA256SUMS at
# https://github.com/zeroclaw-labs/zeroclaw/releases/download/v0.7.4/SHA256SUMS
ZEROCLAW_SHA ?= "INVALID-override-for-this-TARGET_ARCH"
ZEROCLAW_SHA:aarch64 = "9d49e89f74e066f85dca2e1f7f96432a7f1ce73f9ee87f64bc6e6929c273b277"
ZEROCLAW_SHA:arm = "16d9fe037f66f53c64f24b47d12d69927b30f76bb1ea27a5228410842d042cd0"
SRC_URI[bin.sha256sum] = "${ZEROCLAW_SHA}"

COMPATIBLE_HOST = "(aarch64|arm).*-linux"

inherit systemd useradd

# Create the `zeroclaw` system user/group referenced by the bundled
# zeroclaw.service (User=zeroclaw, Group=zeroclaw). Without this,
# systemd fails the service with status=217/USER on first boot.
# HOME points at /var/lib/zeroclaw — the daemon's state dir.
USERADD_PACKAGES = "${PN}"
USERADD_PARAM:${PN} = "--system --home-dir /var/lib/zeroclaw --no-create-home --shell /sbin/nologin --user-group zeroclaw"

do_install() {
    # Binary
    install -d ${D}${bindir}
    install -m 0755 ${WORKDIR}/zeroclaw ${D}${bindir}/zeroclaw

    # Configuration
    install -d ${D}${sysconfdir}/zeroclaw
    install -m 0640 ${WORKDIR}/zeroclaw.toml ${D}${sysconfdir}/zeroclaw/

    # Data directories — owned by the zeroclaw user so the daemon
    # can write session/state files without chown on first run.
    install -d -o zeroclaw -g zeroclaw ${D}/var/lib/zeroclaw
    install -d -o zeroclaw -g zeroclaw ${D}/var/lib/zeroclaw/skills
    install -d -o zeroclaw -g zeroclaw ${D}/var/lib/zeroclaw/workspace

    # Systemd service
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/zeroclaw.service ${D}${systemd_system_unitdir}/
}

# Configuration file is readable by the zeroclaw group for the
# daemon to pick up provider/API keys. Not world-readable (contains
# encrypted key material).
pkg_postinst:${PN}() {
    chgrp zeroclaw $D${sysconfdir}/zeroclaw/zeroclaw.toml || true
}

FILES:${PN} = " \
    ${bindir}/zeroclaw \
    ${sysconfdir}/zeroclaw \
    /var/lib/zeroclaw \
    ${systemd_system_unitdir}/zeroclaw.service \
"

CONFFILES:${PN} = "${sysconfdir}/zeroclaw/zeroclaw.toml"

# Pre-built binary is already stripped by upstream release process
INSANE_SKIP:${PN} = "already-stripped"
