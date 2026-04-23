# SPDX-FileCopyrightText: Copyright (c) 2026 Jason Perlow
# SPDX-License-Identifier: Apache-2.0
#
# Node.js 22 LTS — pre-built binary, per-architecture selection.
# NemoClaw requires node >= 22.16.0.
#
# Upstream nodejs.org ships prebuilt Linux archives for arm64,
# armv7l, and x64. ARMv6 (Pi Zero / Pi 1) has no official prebuilt
# — if you need Node there, use nodejs-source or run the agent on
# a newer board. RISC-V has no official prebuilt either.

SUMMARY = "Node.js 22 LTS JavaScript runtime (pre-built binary)"
HOMEPAGE = "https://nodejs.org"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

# Per-arch archive suffix (matches nodejs.org dist filenames).
NODEJS_ARCH ?= "INVALID-override-for-this-TARGET_ARCH"
NODEJS_ARCH:aarch64 = "arm64"
NODEJS_ARCH:arm = "armv7l"
NODEJS_ARCH:x86_64 = "x64"

SRC_URI = "https://nodejs.org/dist/v${PV}/node-v${PV}-linux-${NODEJS_ARCH}.tar.xz;name=node"

# Per-arch SHA256 — bitbake picks the one matching TARGET_ARCH.
# Verified against upstream SHASUMS256.txt.
SRC_URI[node.sha256sum] = "INVALID-override-for-this-TARGET_ARCH"
SRC_URI[node.sha256sum]:aarch64 = "e9e1930fd321a470e29bb68f30318bf58e3ecb4acb4f1533fb19c58328a091fe"
SRC_URI[node.sha256sum]:arm = "2ebc6746e517f345da340ec76a108203eb6c2365391eb525c0e0dd6135b0b9df"
SRC_URI[node.sha256sum]:x86_64 = "88fd1ce767091fd8d4a99fdb2356e98c819f93f3b1f8663853a2dee9b438068a"

S = "${WORKDIR}/node-v${PV}-linux-${NODEJS_ARCH}"

COMPATIBLE_HOST = "(aarch64|arm|x86_64).*-linux"

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
