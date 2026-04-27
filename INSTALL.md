# Building `meta-nclawzero-base` images

## Build host

Any recent Debian- or Ubuntu-based x86_64 Linux will work. The
build takes roughly 30 GB of disk (`tmp/` + sstate + downloads) and
completes in about an hour on 8 cores with a warm sstate cache.

Recent Ubuntu 25.10 (or any host with AppArmor in enforce mode
that blocks `LD_PRELOAD` inside `pseudo`) needs the
`base-passwd_%.bbappend` shipped in this layer — it skips the
`chown /etc` postinst when running in a sysroot context and
avoids a bitbake failure at image assembly time.

Install Yocto's host prerequisites:

```bash
sudo apt install -y gawk wget git diffstat unzip texinfo gcc \
    build-essential chrpath socat cpio python3 python3-pip \
    python3-pexpect xz-utils debianutils iputils-ping \
    python3-git python3-jinja2 python3-subunit zstd liblz4-tool \
    file locales libacl1-dev bmap-tools
```

## Workspace layout

Pick a parent directory (below, `~/yocto`) and clone the five
layers plus poky:

```bash
mkdir ~/yocto && cd ~/yocto

# Core
git clone -b scarthgap https://git.yoctoproject.org/git/poky
git clone -b scarthgap https://git.openembedded.org/meta-openembedded
git clone -b scarthgap https://git.yoctoproject.org/git/meta-raspberrypi

# This layer
git clone https://github.com/perlowja/meta-nclawzero-base.git
```

> **Note:** Jetson family BSP integration (via `meta-tegra`) is on
> the `wip/jetson-pending-validation` branch pending hardware
> validation. `main` is Raspberry-Pi-only.

## Initialize the build

```bash
cd ~/yocto
source poky/oe-init-build-env build
```

This drops you into `~/yocto/build/` with `conf/local.conf` and
`conf/bblayers.conf` stubs generated.

### `conf/bblayers.conf`

Append the layer paths:

```bitbake
BBLAYERS ?= " \
  ${TOPDIR}/../poky/meta \
  ${TOPDIR}/../poky/meta-poky \
  ${TOPDIR}/../poky/meta-yocto-bsp \
  ${TOPDIR}/../meta-openembedded/meta-oe \
  ${TOPDIR}/../meta-openembedded/meta-python \
  ${TOPDIR}/../meta-openembedded/meta-networking \
  ${TOPDIR}/../meta-openembedded/meta-multimedia \
  ${TOPDIR}/../meta-raspberrypi \
  ${TOPDIR}/../meta-nclawzero-base \
"
```

### `conf/local.conf`

#### Supported machines

All recipes in this layer are ARM-universal (`COMPATIBLE_HOST =
"(aarch64|arm).*-linux"`) — they compile for any ARM target the
BSP layer supports. The matrix below summarizes the known-good
`MACHINE` values by family.

**Raspberry Pi** (via `meta-raspberrypi` scarthgap):

| MACHINE | Hardware | Status |
|---|---|---|
| `raspberrypi4-64` | Pi 4 / CM4, 64-bit | ✅ maintainer-tested (Pi 4 2 GB + 8 GB) |
| `raspberrypi5` | Pi 5 | 🟡 recipe-compatible, maintainer has not booted |
| `raspberrypi3-64` | Pi 3 B/B+, 64-bit | 🟡 recipe-compatible, untested |
| `raspberrypi0-2w-64` | Pi Zero 2 W, 64-bit | 🟡 recipe-compatible, untested — may run out of RAM during agent startup (512 MB) |
| `raspberrypi4` | Pi 4, 32-bit | 🟡 recipe-compatible, not recommended (agent stack benefits from 64-bit) |
| `raspberrypi3` | Pi 3, 32-bit | 🟡 recipe-compatible, not recommended |
| Pi 1 / Pi 2 / Pi Zero (original) | ARMv6 / older ARMv7 | ❌ out of scope — too underpowered for the agent runtime |

Legend: ✅ = maintainer-booted and validated. 🟡 = architecture-clean, should build, not tested by the maintainer.

Verify the MACHINE you want exists in the BSP checkout:

```bash
ls ~/yocto/meta-raspberrypi/conf/machine/
```

If a name below doesn't exist in your BSP checkout, the BSP has
renamed or dropped it — the layer has nothing to do with that.

#### Raspberry Pi — canonical `local.conf`

Template (swap `MACHINE` for your Pi generation from the table
above):

```bitbake
MACHINE ?= "raspberrypi4-64"

DISTRO ?= "poky"
PACKAGE_CLASSES ?= "package_ipk"
EXTRA_IMAGE_FEATURES ?= "debug-tweaks"

# Emit .wic.gz for bmaptool flashing
IMAGE_FSTYPES += "wic wic.gz wic.bmap"
```

## Build

Pick an image recipe:

```bash
# Minimal headless console (base + goodies)
bitbake nclawzero-base-image

# Lightweight desktop via Weston+VNC (no agent)
bitbake nclawzero-desktop-image

# Agent-runtime image (ZeroClaw + NemoClaw provisioned on first boot)
bitbake nclawzero-agent-image
```

## Where the output lands

Path pattern: `build/tmp/deploy/images/${MACHINE}/nclawzero-*-image-${MACHINE}.wic.<ext>`.

Concrete examples:

```
build/tmp/deploy/images/raspberrypi4-64/
  nclawzero-base-image-raspberrypi4-64.wic.gz
  nclawzero-base-image-raspberrypi4-64.wic.bmap

build/tmp/deploy/images/raspberrypi5/
  nclawzero-base-image-raspberrypi5.wic.gz
  nclawzero-base-image-raspberrypi5.wic.bmap
```

Pi output is wic.gz + bmap (bmaptool-friendly). See **`FLASH.md`**
for writing these images to SD card.

## Troubleshooting

- **`do_rootfs` fails with `pseudo` / `LD_PRELOAD` error**: you're on
  a host with AppArmor in enforce mode. The `base-passwd_%.bbappend`
  in this layer should handle it; verify the bbappend is being
  picked up via `bitbake -e base-passwd | grep -i bbappend`.
- **NemoClaw first-boot provisioning fails on-device**: the device
  needs outbound HTTPS to `github.com`. Check `journalctl -u
  nemoclaw-firstboot` on the target.
