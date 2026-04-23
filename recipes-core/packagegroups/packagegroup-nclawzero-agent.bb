# SPDX-FileCopyrightText: Copyright (c) 2026 Jason Perlow
# SPDX-License-Identifier: Apache-2.0
#
# Optional agent stack — pulls in ZeroClaw (Rust AI agent runtime,
# pre-built binary) and the NemoClaw first-boot provisioner. Only
# included in `nclawzero-agent-image`. The base image does NOT
# depend on this packagegroup; users who just want a headless
# edge Linux box can skip it entirely.

SUMMARY = "nclawzero agent stack — ZeroClaw + NemoClaw + Node runtime"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"

inherit packagegroup

RDEPENDS:${PN} = " \
    packagegroup-nclawzero-base \
    zeroclaw-bin \
    zterm-bin \
    nodejs-bin \
    nemoclaw-firstboot \
"
