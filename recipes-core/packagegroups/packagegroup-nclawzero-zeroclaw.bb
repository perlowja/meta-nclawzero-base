# SPDX-FileCopyrightText: Copyright (c) 2026 Jason Perlow
# SPDX-License-Identifier: Apache-2.0
#
# ZeroClaw-only agent stack — ZeroClaw daemon (Rust) + zterm REPL.
# Unlike `packagegroup-nclawzero-agent`, this one OMITS NemoClaw +
# Node.js. Suitable for lower-RAM / lower-storage targets where the
# full agent framework is overkill but a working single-daemon
# zeroclaw gateway + interactive REPL is all the user needs.
#
# Paired image: `nclawzero-zeroclaw-image.bb`.
#
# Sizing (approximate, verified on Raspberry Pi 4):
#   zeroclaw-bin:   ~35 MB on disk, ~17 MB RSS at rest
#   zterm-bin:       ~8 MB on disk, only live when user runs it
# No Node.js, no npm, no first-boot provisioner.

SUMMARY = "nclawzero zeroclaw-only stack — ZeroClaw daemon + zterm REPL"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"

inherit packagegroup

RDEPENDS:${PN} = " \
    packagegroup-nclawzero-base \
    zeroclaw-bin \
    zterm-bin \
"
