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

This layer is architecture-aware and BSP-agnostic — the base image
builds for any `MACHINE` a Yocto BSP layer supports, and the agent
image builds wherever ZeroClaw and Node.js ship prebuilt binaries
(aarch64 / armv7 / armv6 via bbappend / x86_64). See the coverage
matrix below for specifics.

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

| Module | Arch | MACHINE (meta-tegra) | `base` | `desktop` | `agent` |
|---|---|---|---|---|---|
| Jetson Orin Nano 8GB Dev Kit | aarch64 | `jetson-orin-nano-devkit` | ✅ | ✅ | ✅ |
| Jetson Orin NX / AGX Orin | aarch64 | (per board) | ✅ | ✅ | ✅ |

SD-boot targets are the primary path. `flash.sh` / USB-recovery
flow is out of scope here — use it for eMMC/NVMe targets if you
know what you're doing.

#### Other ARM SBCs

Anything with a Yocto BSP layer and a 64-bit or hard-float 32-bit
Cortex-A core should work end-to-end. Add the BSP layer to
`bblayers.conf`, set the right `MACHINE`, and build — no recipe
changes required.

| Family | BSP layer | Example boards |
|---|---|---|
| Rockchip | `meta-rockchip` | Radxa Rock 5 / 4 / 3, OrangePi 5, Pine RockPro64 |
| Allwinner | `meta-sunxi` | OrangePi H-series / R-series, Cubieboard, BananaPi M1/M2 |
| NXP i.MX | `meta-freescale` + `meta-freescale-3rdparty` | Toradex Verdin, Phytec phyBOARD, Kobol Helios64 |
| Amlogic | `meta-meson` | ODROID-C2 / C4 / N2, Khadas VIM |
| Mediatek | `meta-mediatek` | Genio 350/500/700 dev kits |
| Xilinx Zynq | `meta-xilinx` | ZCU102, Kria KV260, Ultra96 |
| TI | `meta-ti` | BeagleBone AI-64, BeaglePlay |
| Samsung Exynos | `meta-samsung` | ODROID-XU4 (with caveats on kernel support) |

#### x86 / x86_64 SBCs and mini-PCs

| Target | BSP layer | MACHINE | Notes |
|---|---|---|---|
| Intel NUC, any x86_64 PC | `meta-intel` | `intel-corei7-64` | Full coverage of all three image variants |
| UP Board, UP² | `meta-intel` + `meta-aaeon-bsp` | `up-*` | Same |
| LattePanda 3 Delta | `meta-intel` | `intel-corei7-64` | Same |
| Generic qemu x86-64 | poky default | `qemux86-64` | Useful for build validation without hardware |

#### RISC-V

The base image builds for any RISC-V 64-bit board via
`meta-riscv`. The **agent image does not** — upstream ZeroClaw and
Node.js don't ship prebuilt `riscv64` binaries today. A source
recipe would be needed (ZeroClaw via `rust-bin`-style build,
Node.js via the upstream `meta-nodejs` source recipe).

| Board | BSP layer | MACHINE | `base` | `desktop` | `agent` |
|---|---|---|---|---|---|
| StarFive VisionFive 2 | `meta-riscv` | `visionfive2` | ✅ | ⚠️ kernel dep | ❌ |
| LicheePi 4A | `meta-riscv` | `licheepi4a` | ✅ | ⚠️ | ❌ |
| BeagleV-Ahead | `meta-riscv` | `beaglev-ahead` | ✅ | ⚠️ | ❌ |
| SiFive HiFive Unmatched | `meta-riscv` | `unmatched` | ✅ | ❌ (no GPU) | ❌ |

If you need the agent stack on RISC-V, open an issue — the fix is
a small nodejs-source recipe plus a ZeroClaw bbappend that builds
from the upstream crate instead of pulling a prebuilt release.

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
