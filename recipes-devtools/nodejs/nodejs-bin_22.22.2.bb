# SPDX-FileCopyrightText: Copyright (c) 2026 Jason Perlow
# SPDX-License-Identifier: Apache-2.0
#
# Node.js 22 LTS — pre-built binary, per-architecture selection.
# NemoClaw requires node >= 22.16.0.
#
# This layer targets ARM edge SBCs. Upstream nodejs.org ships
# prebuilt Linux archives for arm64 and armv7l — both picked up
# automatically here. ARMv6 (Pi Zero / Pi 1) and RISC-V have no
# official prebuilt; agent image is not supported on those
# platforms today. x86_64 is out of scope for this layer.

SUMMARY = "Node.js 22 LTS JavaScript runtime (pre-built binary)"
HOMEPAGE = "https://nodejs.org"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

# Per-arch archive suffix (matches nodejs.org dist filenames).
NODEJS_ARCH ?= "INVALID-override-for-this-TARGET_ARCH"
NODEJS_ARCH:aarch64 = "arm64"
NODEJS_ARCH:arm = "armv7l"

SRC_URI = "https://nodejs.org/dist/v${PV}/node-v${PV}-linux-${NODEJS_ARCH}.tar.xz;name=node"

# Per-arch SHA256 — indirected through a plain variable because
# BitBake varflag syntax (SRC_URI[node.sha256sum]) does not accept
# :override suffixes. Override NODEJS_SHA instead; the assignment
# below propagates the arch-correct value to the varflag.
# Verified against upstream SHASUMS256.txt.
NODEJS_SHA ?= "INVALID-override-for-this-TARGET_ARCH"
NODEJS_SHA:aarch64 = "e9e1930fd321a470e29bb68f30318bf58e3ecb4acb4f1533fb19c58328a091fe"
NODEJS_SHA:arm = "2ebc6746e517f345da340ec76a108203eb6c2365391eb525c0e0dd6135b0b9df"
SRC_URI[node.sha256sum] = "${NODEJS_SHA}"

S = "${WORKDIR}/node-v${PV}-linux-${NODEJS_ARCH}"

COMPATIBLE_HOST = "(aarch64|arm).*-linux"

do_install() {
    install -d ${D}${prefix}
    cp -R ${S}/bin ${D}${prefix}/
    cp -R ${S}/lib ${D}${prefix}/
    cp -R ${S}/include ${D}${prefix}/
    cp -R ${S}/share ${D}${prefix}/ 2>/dev/null || true

    # Symlink node and npm to standard paths
    install -d ${D}${bindir}
    ln -sf ../lib/node_modules/npm/bin/npm-cli.js ${D}${bindir}/npm 2>/dev/null || true
}

FILES:${PN} = " \
    ${prefix}/bin \
    ${prefix}/lib \
    ${prefix}/include \
    ${prefix}/share \
    ${bindir}/npm \
"

RDEPENDS:${PN} = "libstdc++"
INSANE_SKIP:${PN} = "already-stripped file-rdeps ldflags"
INHIBIT_PACKAGE_STRIP = "1"
INHIBIT_SYSROOT_STRIP = "1"
