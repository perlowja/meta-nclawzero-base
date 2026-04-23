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

For Jetson builds, also:

```bash
git clone -b scarthgap-l4t-r36.3.0 https://github.com/OE4T/meta-tegra.git
```

### Jetson build-host caveat (GCC14 / glibc ≥ 2.41)

On recent Debian/Ubuntu hosts where the native toolchain is GCC 14
against glibc ≥ 2.41, four recipes in `meta-tegra` fail to compile
the BaseTools with `error: function declaration isn't a prototype`.
Both `-Wno-error=strict-prototypes` AND `-std=gnu17` are required —
either alone fails the other. Add a local bbappend:

```bash
mkdir -p ~/yocto/meta-local/recipes-bsp/edk2/
cat > ~/yocto/meta-local/recipes-bsp/edk2/edk2-firmware-tegra_%.bbappend <<'EOF'
BUILD_CFLAGS:append = " -Wno-error=strict-prototypes -std=gnu17"
EOF
```

Apply the same bbappend to `edk2-basetools-tegra-native`,
`edk2-firmware-tegra-minimal`, and `standalone-mm-optee-tegra`.

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

For Jetson targets also append `${TOPDIR}/../meta-tegra`.

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

**NVIDIA Jetson** (via `meta-tegra`, branch tracks the L4T release):

| MACHINE | Hardware | meta-tegra branch | Status |
|---|---|---|---|
| `jetson-orin-nano-devkit` | Jetson Orin Nano 8 GB Devkit | `scarthgap-l4t-r36.3.0` | ✅ maintainer-tested |
| `jetson-orin-nx-devkit` | Jetson Orin NX 8 / 16 GB | `scarthgap-l4t-r36.3.0` | 🟡 recipe-compatible, untested |
| `jetson-agx-orin-devkit` | Jetson AGX Orin 32 / 64 GB | `scarthgap-l4t-r36.3.0` | 🟡 recipe-compatible, untested |
| `jetson-xavier-nx-devkit` | Jetson Xavier NX | `scarthgap-l4t-r35.x.x` | 🟡 recipe-compatible, needs r35 branch |
| `jetson-agx-xavier-devkit` | Jetson AGX Xavier | `scarthgap-l4t-r35.x.x` | 🟡 recipe-compatible, needs r35 branch |
| `jetson-nano-devkit` | Jetson Nano (legacy 4 GB) | `scarthgap-l4t-r32.x.x` | 🟡 recipe-compatible, needs r32 branch + careful sizing (4 GB RAM is tight) |
| `jetson-agx-thor-devkit` | Jetson AGX Thor (Blackwell) | `scarthgap-l4t-r38.x` *(pending upstream)* | 🔵 drafted on [`jetson-thor`](../../tree/jetson-thor) branch, awaiting external tester — see `JETSON-THOR.md` |

Legend: ✅ = maintainer-booted and validated. 🟡 = architecture-clean, should build, not tested by the maintainer. 🔵 = drafted on a separate branch pending hardware validation.

**Important:** `MACHINE` names have been stable in recent meta-tegra
releases but are not guaranteed. Always verify against the branch
you cloned:

```bash
ls ~/yocto/meta-tegra/conf/machine/ | grep -iE '(orin|xavier|nano|thor)'
```

Same check for Pi:

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

#### Jetson — canonical `local.conf`

Template (swap `MACHINE` and make sure `meta-tegra` is on the
matching L4T branch):

```bitbake
MACHINE ?= "jetson-orin-nano-devkit"
DISTRO ?= "poky"
IMAGE_FSTYPES += "wic wic.xz"

# meta-tegra version pin must match the branch you cloned.
# e.g. for scarthgap-l4t-r36.3.0 (Orin family):
PREFERRED_PROVIDER_virtual/bootloader = "tegra-bootloader"
```

For Xavier (`scarthgap-l4t-r35.x.x`) and legacy Nano
(`scarthgap-l4t-r32.x.x`) the `PREFERRED_PROVIDER` line is the same;
what changes is which branch you cloned for meta-tegra and which
JetPack version NVIDIA's flash tooling targets. Expect bbappends to
land for each branch as those flows get validated.

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

build/tmp/deploy/images/jetson-orin-nano-devkit/
  nclawzero-base-image-jetson-orin-nano-devkit.wic.xz

build/tmp/deploy/images/jetson-agx-orin-devkit/
  nclawzero-base-image-jetson-agx-orin-devkit.wic.xz
```

Pi output is wic.gz + bmap (bmaptool-friendly). Jetson output is
wic.xz consumed by NVIDIA's `flash.sh`. See **`FLASH.md`** for
writing these images — SD card on Pi + Orin Nano, USB-recovery on
AGX / NX / Xavier / Thor.

## Troubleshooting

- **`do_rootfs` fails with `pseudo` / `LD_PRELOAD` error**: you're on
  a host with AppArmor in enforce mode. The `base-passwd_%.bbappend`
  in this layer should handle it; verify the bbappend is being
  picked up via `bitbake -e base-passwd | grep -i bbappend`.
- **Jetson BaseTools fails with `error: function declaration isn't
  a prototype`**: add the GCC14/glibc bbappend described above.
- **NemoClaw first-boot provisioning fails on-device**: the device
  needs outbound HTTPS to `github.com`. Check `journalctl -u
  nemoclaw-firstboot` on the target.
