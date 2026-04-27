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

This layer targets the **ARM Raspberry Pi family**. nclawzero
currently supports x86_64 + macOS via Docker/Podman containers
(`ghcr.io/perlowja/nclawzero-demo`, `ghcr.io/perlowja/nclawzero-agent`)
and ARM Raspberry Pi family boards (Pi 4, Pi 5, Pi Zero 2 W,
Pi 3 64-bit) via Yocto-built flashable images from this layer or
pre-built SD images from `pi-gen-nclawzero`.

#### Raspberry Pi family

| Model | Arch | MACHINE (meta-raspberrypi) | `base` | `desktop` | `agent` |
|---|---|---|---|---|---|
| Pi 5 / Pi 500 | aarch64 | `raspberrypi5` | ✅ | ✅ | ✅ |
| Pi 4 / 400 / CM4 | aarch64 | `raspberrypi4-64` | ✅ | ✅ (4 GB+) | ✅ (4 GB+) |
| Pi 3 B+ / A+ | aarch64 | `raspberrypi3-64` | ✅ | ⚠️ tight | ⚠️ tight (1 GB) |
| Pi Zero 2 W | aarch64 | `raspberrypi0-2w-64` | ✅ | ❌ (512 MB) | ❌ (512 MB) |
| Pi 4 / CM4 in 32-bit | armv7 | `raspberrypi4` | ✅ | ✅ (4 GB+) | ✅ (4 GB+) |
| Pi 3 B+ / A+ in 32-bit | armv7 | `raspberrypi3` | ✅ | ⚠️ | ⚠️ |
| Pi 2 Model B | armv7 | `raspberrypi2` | ✅ | ❌ | ⚠️ (1 GB) |
| Pi 1 / Pi Zero / Pi Zero W / CM1 | armv6 | `raspberrypi` / `raspberrypi0-wifi` | ✅ | ❌ | ⚠️ see note |

**Pi Zero / Pi 1 (ARMv6) note**: the agent image needs a bbappend
for `zeroclaw-bin` to pick the `arm-unknown-linux-gnueabihf`
(ARMv6+) release tarball, and Node.js has no official armv6 prebuilt
— so `nemoclaw-firstboot` can't complete on those boards today. Use
`base` for Pi Zero / Pi 1.

Flashing, power, and SD-card requirements (the physical side of
the story):

| | |
|---|---|
| SD card | 8 GB minimum for `base`, 16 GB recommended for `agent` (Class 10 / UHS-1 or faster) |
| Power | Pi 4 / Pi 5 need an official 5V/3A PSU; underpowered supplies throttle boot silently |
| First boot | `agent` image needs outbound HTTPS to `github.com` for NemoClaw provisioning (Ethernet DHCP just works) |

#### NVIDIA Jetson

Jetson family support is deferred pending hardware validation.

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

# 3. Initialize the build directory
source poky/oe-init-build-env build

# 4. Edit conf/bblayers.conf and conf/local.conf per INSTALL.md
#    (one-time setup; MACHINE=raspberrypi4-64)

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
- NemoClaw first-boot provisioning fails on-device: the device
  needs outbound HTTPS to `github.com`. Check
  `journalctl -u nemoclaw-firstboot -f` on the target.

For anything else, the Yocto mailing list and the `#yocto` channel
on Libera Chat are extremely responsive for well-formed questions.
