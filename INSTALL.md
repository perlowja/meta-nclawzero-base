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

For **Raspberry Pi 4** (64-bit):

```bitbake
MACHINE ?= "raspberrypi4-64"

DISTRO ?= "poky"
PACKAGE_CLASSES ?= "package_ipk"
EXTRA_IMAGE_FEATURES ?= "debug-tweaks"

# Emit .wic.gz for bmaptool flashing
IMAGE_FSTYPES += "wic wic.gz wic.bmap"
```

For **Jetson Orin Nano** (SD-boot flow):

```bitbake
MACHINE ?= "jetson-orin-nano-devkit"
DISTRO ?= "poky"
IMAGE_FSTYPES += "wic wic.xz"

# meta-tegra version pin must match the branch you cloned.
# e.g. for scarthgap-l4t-r36.3.0:
PREFERRED_PROVIDER_virtual/bootloader = "tegra-bootloader"
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

```
build/tmp/deploy/images/raspberrypi4-64/
  nclawzero-base-image-raspberrypi4-64.wic.gz
  nclawzero-base-image-raspberrypi4-64.wic.bmap

build/tmp/deploy/images/jetson-orin-nano-devkit/
  nclawzero-base-image-jetson-orin-nano-devkit.wic.xz
```

See **`FLASH.md`** for writing these images to SD cards.

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
