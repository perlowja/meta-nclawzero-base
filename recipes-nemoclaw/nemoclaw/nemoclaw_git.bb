# SPDX-FileCopyrightText: Copyright (c) 2026 Jason Perlow
# SPDX-License-Identifier: Apache-2.0
#
# NemoClaw — Node.js sandbox framework for AI agents.
# Upstream: https://github.com/NVIDIA/NemoClaw (public Apache-2.0).
#
# This recipe fetches upstream source and builds it as-is. No local
# patches or overlays — any fixes or integrations need to land in
# the upstream project first, then get picked up here via SRCREV.

SUMMARY = "NemoClaw sandbox framework (upstream: github.com/NVIDIA/NemoClaw)"
DESCRIPTION = "Node.js sandbox framework that provides isolated \
    execution environments for AI agent runtimes. Fetches the \
    upstream source directly from GitHub at build time."
HOMEPAGE = "https://github.com/NVIDIA/NemoClaw"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=86d3f3a95c324c9479bd8986968f4327"

SRC_URI = " \
    git://github.com/NVIDIA/NemoClaw.git;protocol=https;branch=main \
    file://nemoclaw.service \
    file://nemoclaw.conf \
"

# Pin to a known-good upstream commit for reproducible builds.
# Update this when rebasing patches against new upstream.
SRCREV = "${AUTOREV}"
# Production: pin to specific commit
# SRCREV = "c333d96..."

PV = "1.0+git"
S = "${WORKDIR}/git"

DEPENDS = "nodejs-native"
RDEPENDS:${PN} = "nodejs zeroclaw-bin"

inherit systemd

SYSTEMD_SERVICE:${PN} = "nemoclaw.service"
SYSTEMD_AUTO_ENABLE = "enable"

do_compile() {
    cd ${S}
    # Production npm install — no dev deps, no optional native addons
    npm install --production --no-optional --ignore-scripts \
        --target_arch=${TARGET_ARCH} \
        --target_platform=linux

    # Build the nemoclaw plugin
    if [ -d "nemoclaw" ]; then
        cd nemoclaw
        npm install --production --no-optional --ignore-scripts
        cd ..
    fi
}

do_install() {
    # Install the NemoClaw application
    install -d ${D}/opt/nemoclaw
    cp -R ${S}/bin ${D}/opt/nemoclaw/
    cp -R ${S}/dist ${D}/opt/nemoclaw/ 2>/dev/null || true
    cp -R ${S}/src ${D}/opt/nemoclaw/
    cp -R ${S}/nemoclaw ${D}/opt/nemoclaw/
    cp -R ${S}/nemoclaw-blueprint ${D}/opt/nemoclaw/
    cp -R ${S}/agents ${D}/opt/nemoclaw/
    cp -R ${S}/node_modules ${D}/opt/nemoclaw/
    cp ${S}/package.json ${D}/opt/nemoclaw/

    # Remove dev artifacts
    rm -rf ${D}/opt/nemoclaw/.git
    rm -rf ${D}/opt/nemoclaw/test
    rm -rf ${D}/opt/nemoclaw/.github
    rm -rf ${D}/opt/nemoclaw/docs

    # Configuration
    install -d ${D}${sysconfdir}/nemoclaw
    install -m 0600 ${WORKDIR}/nemoclaw.conf ${D}${sysconfdir}/nemoclaw/

    # Data directories
    install -d ${D}/var/lib/nemoclaw
    install -d ${D}/var/lib/nemoclaw/snapshots
    install -d ${D}/var/lib/nemoclaw/sandboxes

    # Systemd service
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/nemoclaw.service ${D}${systemd_system_unitdir}/
}

FILES:${PN} = " \
    /opt/nemoclaw \
    ${sysconfdir}/nemoclaw \
    /var/lib/nemoclaw \
    ${systemd_system_unitdir}/nemoclaw.service \
"

CONFFILES:${PN} = "${sysconfdir}/nemoclaw/nemoclaw.conf"

# Node.js modules contain pre-built native addons
INSANE_SKIP:${PN} = "already-stripped ldflags file-rdeps"
