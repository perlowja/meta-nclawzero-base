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

ZEROCLAW_VERSION = "0.6.9"

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
ZEROCLAW_SHA ?= "INVALID-override-for-this-TARGET_ARCH"
ZEROCLAW_SHA:aarch64 = "25e5a50a2870cfab14a2767d66650b188ca0ccbb38d9e895dd09b6d7399d73f6"
ZEROCLAW_SHA:arm = "8555973fd8a5023647738264fee25092b3f06fdfe6eb193fad230d8deea973b7"
SRC_URI[bin.sha256sum] = "${ZEROCLAW_SHA}"

COMPATIBLE_HOST = "(aarch64|arm).*-linux"

inherit systemd

SYSTEMD_SERVICE:${PN} = "zeroclaw.service"
SYSTEMD_AUTO_ENABLE = "enable"

do_install() {
    # Binary
    install -d ${D}${bindir}
    install -m 0755 ${WORKDIR}/zeroclaw ${D}${bindir}/zeroclaw

    # Configuration
    install -d ${D}${sysconfdir}/zeroclaw
    install -m 0600 ${WORKDIR}/zeroclaw.toml ${D}${sysconfdir}/zeroclaw/

    # Data directories
    install -d ${D}/var/lib/zeroclaw
    install -d ${D}/var/lib/zeroclaw/skills
    install -d ${D}/var/lib/zeroclaw/workspace

    # Systemd service
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/zeroclaw.service ${D}${systemd_system_unitdir}/
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
