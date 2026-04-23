# SPDX-FileCopyrightText: Copyright (c) 2026 Jason Perlow
# SPDX-License-Identifier: Apache-2.0
#
# zterm CLI packagegroup — the terminal REPL for the claw-family
# agent stack. Connects to a running zeroclaw (or any OpenAI-
# compatible endpoint) and gives the user a keyboard-driven
# agent interaction loop. No backend dependency on this
# packagegroup — zterm is a client, not a server.
#
# Included in both `nclawzero-zeroclaw-image` (zeroclaw-only) and
# `nclawzero-agent-image` (zeroclaw + NemoClaw). Left out of
# `nclawzero-base-image` (headless baseline, no agent stack).

SUMMARY = "nclawzero — zterm terminal REPL for claw-family agents"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"

inherit packagegroup

RDEPENDS:${PN} = " \
    zterm-bin \
"
