# SPDX-FileCopyrightText: Copyright (c) 2026 Jason Perlow
# SPDX-License-Identifier: Apache-2.0
#
# zterm — pre-built Rust terminal REPL for claw-family agentic
# daemons.
#
# Source: https://github.com/perlowja/zterm (Apache-2.0).
# Upstream release workflow (.github/workflows/release.yml) builds
# four target tarballs on tag push — this recipe fetches the
# aarch64-unknown-linux-gnu tarball for ARM edge SBCs. Binary-fetch
# sidesteps Yocto scarthgap's stock Rust 1.75 vs zterm's MSRV 1.93,
# which would otherwise require meta-rust-bin + a full cargo source
# recipe (offline crate vendoring for ~200 transitive deps).
#
# armv7-unknown-linux-gnueabihf is not currently produced by the
# release workflow (matrix is x86_64-linux, aarch64-linux, and the
# two macOS targets). 32-bit ARM Pis (raspberrypi4, raspberrypi3,
# Pi Zero 2 W when running 32-bit, Pi Zero/1 ARMv6) will need either
# a workflow extension or a source-build bbappend once meta-rust-bin
# is integrated downstream.

SUMMARY = "zterm — light, thin terminal REPL for claw-family agents (pre-built binary)"
DESCRIPTION = "Rust terminal REPL for zeroclaw / openclaw / nemoclaw. \
    Single binary, no daemon. Connects to an OpenAI-compatible agent \
    endpoint (typically zeroclaw on port 42617). v0.2.x is the claw- \
    family backend stable line; v0.3.x on main adds a Paradox 4.5 / \
    dBASE V turbo-vision TUI."
HOMEPAGE = "https://github.com/perlowja/zterm"
BUGTRACKER = "https://github.com/perlowja/zterm/issues"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=3b83ef96387f14655fc854ddc3c6bd57"

ZTERM_VERSION = "0.2.2"

# Per-arch binary tuple. Upstream release.yml ships aarch64-linux-gnu
# and x86_64-linux-gnu (plus two macOS targets). Override in a
# bbappend with a source-build recipe if you need armv7.
ZTERM_TRIPLE ?= "INVALID-override-for-this-TARGET_ARCH"
ZTERM_TRIPLE:aarch64 = "aarch64-unknown-linux-gnu"
ZTERM_TRIPLE:x86-64  = "x86_64-unknown-linux-gnu"

SRC_URI = "https://github.com/perlowja/zterm/releases/download/v${ZTERM_VERSION}/zterm-${ZTERM_VERSION}-${ZTERM_TRIPLE}.tar.gz;name=bin"

# Per-arch SHA256 — indirected through a plain variable because
# BitBake varflag syntax (SRC_URI[bin.sha256sum]) does not accept
# :override suffixes. Override ZTERM_SHA; the assignment below
# propagates the arch-correct value to the varflag.
ZTERM_SHA ?= "INVALID-override-for-this-TARGET_ARCH"
ZTERM_SHA:aarch64 = "8251b441f911d76a01a1104688059ef5b9a08c82d01aaa7a040959495c2b0eb4"
ZTERM_SHA:x86-64  = "0d2fcf4e1793c378881c7b564a224b5c7653802a1c2ae9c02194c6349a9bd975"
SRC_URI[bin.sha256sum] = "${ZTERM_SHA}"

COMPATIBLE_HOST = "(aarch64|x86_64).*-linux"

# zterm's reqwest→openssl-sys chain needs libssl.so.3 + libcrypto.so.3
# at runtime. In scarthgap poky, openssl_3.5.5.bb splits its PACKAGES
# into version-less `libssl` and `libcrypto` (which provide the .so.3
# files). Package names are NOT `libssl3`/`libcrypto3` as in some
# other distros — they're the version-less names here.
RDEPENDS:${PN} = "ca-certificates libssl libcrypto"

# The release tarball layout (per release.yml):
#   zterm-${VERSION}-${TRIPLE}/zterm
#   zterm-${VERSION}-${TRIPLE}/LICENSE
#   zterm-${VERSION}-${TRIPLE}/README.md
#   zterm-${VERSION}-${TRIPLE}/NOTICE       (if present upstream)
S = "${WORKDIR}/zterm-${ZTERM_VERSION}-${ZTERM_TRIPLE}"

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${S}/zterm ${D}${bindir}/zterm
}

FILES:${PN} = "${bindir}/zterm"

# Upstream cargo `profile.release.strip = true` — binary is already
# stripped in the release artifact.
INSANE_SKIP:${PN} = "already-stripped"
