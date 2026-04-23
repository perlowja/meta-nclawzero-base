# SPDX-FileCopyrightText: Copyright (c) 2026 Jason Perlow
# SPDX-License-Identifier: Apache-2.0
#
# Base + goodies packagegroup — the console tools you'd expect on an
# edge Linux box. No AI agent runtime; no desktop. See
# `packagegroup-nclawzero-agent` for the ZeroClaw/NemoClaw layer and
# `packagegroup-nclawzero-desktop` for Weston+VNC.

SUMMARY = "nclawzero base system — shell, editors, network tools, Python"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"

inherit packagegroup

RDEPENDS:${PN} = " \
    ca-certificates \
    curl \
    git \
    openssh-sftp-server \
    openssh-sshd \
    openssh-ssh \
    openssh-scp \
    htop \
    nano \
    less \
    vim \
    tmux \
    rsync \
    iproute2 \
    iputils-ping \
    bash-completion \
    python3 \
    python3-pip \
"
