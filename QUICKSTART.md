# Quickstart

TL;DR build guide. For the fine print, see `INSTALL.md` (build
configuration) and `FLASH.md` (SD card writing), or jump straight
to the upstream Yocto docs linked at the bottom.

## What you need to build

### Build host (the Linux PC that compiles the image)

| | Minimum | Comfortable |
|---|---|---|
| CPU | 4 cores | 8–16 cores |
| RAM | 8 GB | 16 GB |
| Disk | 50 GB free | 100 GB free (SSD) |
| OS | Debian 12 / Ubuntu 22.04+ | Same |
| Network | Outbound HTTPS (for git + wget fetches) | Same |

Ballpark build times with a **cold** sstate cache on 8 cores:

| Image | Build time (first time) | Rebuild (warm sstate) |
|---|---|---|
| `nclawzero-base-image` | 45–90 min | 3–10 min |
| `nclawzero-desktop-image` | 60–120 min | 5–15 min |
| `nclawzero-agent-image` | 50–100 min | 4–12 min |

First build dominates by download time — poky + meta-oe alone pull
several GB of source tarballs. Subsequent builds are much faster.

macOS and Windows are **not** supported build hosts — Yocto
depends on Linux-specific toolchain behavior (pseudo, fakeroot).
If you're on a Mac/Windows workstation, use a Linux VM, a
Debian/Ubuntu container with `--privileged`, or a cloud instance.

### Target hardware (what you flash the image onto)

#### Raspberry Pi 4 Model B

| | |
|---|---|
| Board | Raspberry Pi 4 Model B |
| RAM | 2 GB for `base`, 4 GB+ recommended for `desktop` or `agent`, 8 GB for everything |
| SD card | 8 GB minimum (Class 10 / UHS-1) — 16 GB or larger preferred for agent image |
| Power | Official 5V 3A USB-C PSU — underpowered supplies will throttle boot |
| Other | Ethernet cable (first-boot provisioning needs network if using agent-image) |

The Compute Module 4 (eMMC or SD) works with the same recipes but
isn't regularly smoke-tested in this layer.

#### Jetson Orin Nano Developer Kit

| | |
|---|---|
| Board | Jetson Orin Nano 8GB Developer Kit |
| RAM | 8 GB (fixed, not user-upgradable) |
| SD card | 16 GB minimum (Class 10) — 64 GB recommended so the rootfs has room to grow |
| Power | 19V barrel adapter (not USB-C) |
| Boot mode | SD boot enabled in UEFI (this layer targets the simple SD-boot flow, not `flash.sh` / USB recovery) |

### Network (first-boot only, agent-image only)

If you built `nclawzero-agent-image`, the NemoClaw first-boot
provisioner clones from `github.com/NVIDIA/NemoClaw` and installs
npm dependencies. The device therefore needs outbound HTTPS on
first boot — wired Ethernet via DHCP is the simplest path, and
that's what the default `nclawzero-network` recipe configures.

`nclawzero-base-image` and `nclawzero-desktop-image` do not
require network at boot.

## How to build (6 steps)

```bash
# 1. Install build-host prereqs (Debian/Ubuntu; full list in INSTALL.md)
sudo apt install -y gawk wget git diffstat unzip texinfo gcc \
    build-essential chrpath socat cpio python3 python3-pip xz-utils \
    debianutils bmap-tools

# 2. Clone the layers
mkdir ~/yocto && cd ~/yocto
git clone -b scarthgap https://git.yoctoproject.org/git/poky
git clone -b scarthgap https://git.openembedded.org/meta-openembedded
git clone -b scarthgap https://git.yoctoproject.org/git/meta-raspberrypi
git clone https://github.com/perlowja/meta-nclawzero-base.git
# Jetson only:
#   git clone -b scarthgap-l4t-r36.3.0 https://github.com/OE4T/meta-tegra.git

# 3. Initialize the build directory
source poky/oe-init-build-env build

# 4. Edit conf/bblayers.conf and conf/local.conf per INSTALL.md
#    (one-time setup; MACHINE=raspberrypi4-64 or jetson-orin-nano-devkit)

# 5. Build
bitbake nclawzero-base-image
# or: nclawzero-desktop-image, nclawzero-agent-image

# 6. Flash to SD card — see FLASH.md
sudo bmaptool copy \
  tmp/deploy/images/$MACHINE/nclawzero-base-image-$MACHINE.wic.gz \
  /dev/sdX
```

## Yocto Project docs (canonical upstream)

If you're new to Yocto, start here. Everything this layer does is
plain Yocto — no exotic extensions.

- **[Yocto Project Quick Build](https://docs.yoctoproject.org/brief-yoctoprojectqs/)**
  — 20-minute walk-through that builds `core-image-minimal` for
  qemu. Does what steps 2–5 above do, with more explanation.
- **[Yocto Mega-Manual](https://docs.yoctoproject.org/mega-manual/)**
  — single-HTML compendium of every Yocto manual. Searchable, handy
  as an `Ctrl-F` reference.
- **[Yocto Reference Manual](https://docs.yoctoproject.org/ref-manual/)**
  — authoritative source for variables like `MACHINE`, `DISTRO`,
  `IMAGE_FSTYPES`, etc.
- **[BitBake User Manual](https://docs.yoctoproject.org/bitbake/)**
  — how bitbake parses recipes, when tasks run, how
  `do_compile`/`do_install` fit together.
- **[OpenEmbedded Layer Index](https://layers.openembedded.org/)**
  — search for any recipe across every published layer, with
  upstream/compatibility metadata.

## Board-specific docs

- **Raspberry Pi** — `meta-raspberrypi`:
  <https://git.yoctoproject.org/meta-raspberrypi/about/>
  Per-board variables, supported peripherals, known issues.
- **Jetson Orin Nano** — OE4T `meta-tegra` (upstream community fork;
  this layer builds against it, not NVIDIA-internal tegra bits):
  <https://github.com/OE4T/meta-tegra>
  Includes machine configs, boot artifact layouts, and NVIDIA
  L4T-version pinning tables.

## Upstream project links

- **NemoClaw** (Node.js sandbox framework): <https://github.com/NVIDIA/NemoClaw>
- **ZeroClaw** (Rust AI agent runtime): <https://github.com/zeroclaw-labs/zeroclaw>

Both are Apache-2.0 / MIT and fetched by their respective recipes
at build time. Nothing is redistributed through this layer.

## Troubleshooting starting points

- Yocto build fails early with a `pseudo` / `LD_PRELOAD` error on
  Ubuntu 25.10+: handled by `recipes-core/base-passwd/base-passwd_%.bbappend`
  in this layer. If it still fires, verify the bbappend is being
  picked up with `bitbake -e base-passwd | grep BBFILE`.
- Jetson build fails at `edk2-firmware-tegra` with
  `function declaration isn't a prototype`: add the GCC14/glibc
  bbappend described in `INSTALL.md`.
- NemoClaw first-boot provisioning fails on-device: the device
  needs outbound HTTPS to `github.com`. Check
  `journalctl -u nemoclaw-firstboot -f` on the target.

For anything else, the Yocto mailing list and the `#yocto` channel
on Libera Chat are extremely responsive for well-formed questions.
