# Jetson Thor target — **EXTERNAL-TESTER ONLY, UNVALIDATED**

This branch (`jetson-thor`) adds build-system support for the
**NVIDIA Jetson AGX Thor** module (Blackwell, L4T r38.x series).

> ⚠️ **The maintainer does not own Thor hardware and has not built
> or booted this target.** All recipes on `main` are architecture-clean
> and should compile for Thor's aarch64 SoC, but the BSP wiring below
> is documented from meta-tegra upstream guidance — not from a
> successful local bitbake + flash cycle.
>
> A downstream tester with physical Thor hardware is validating;
> this branch will fold back into `main` only after a confirmed boot
> and `nclawzero-agent-image` smoke test.

## Hardware

| Thing | Value |
|---|---|
| SoC | NVIDIA T264 (Blackwell GPU, 14× Arm Cortex-A78AE + 8× Cortex-A78C) |
| AI perf | up to 2,070 FP4 TOPS |
| RAM | 128 GB LPDDR5X (unified) |
| Boot media | NVMe-first (the Devkit ships with a 1 TB NVMe); eMMC fallback |
| Flash protocol | USB recovery (`flash.sh` tooling, same shape as Orin) |
| JetPack | 7.0+ (L4T r38.x) |

## What this branch changes vs `main`

`main` targets Raspberry Pi (raspberrypi4-64) and Jetson Orin Nano
(jetson-orin-nano-devkit) via:
- `meta-raspberrypi` @ scarthgap
- `meta-tegra` @ `scarthgap-l4t-r36.3.0`

Thor needs a **different meta-tegra branch** — `scarthgap-l4t-r38.x`
or whatever OE4T ships for the Thor release. Check upstream first:

```bash
git ls-remote https://github.com/OE4T/meta-tegra.git \
    'refs/heads/scarthgap-l4t-r38*'
```

If no r38-series branch has shipped yet, Thor is genuinely not
buildable with OE-core scarthgap — wait for upstream or work from
the NVIDIA JetPack 7 reference BSP directly.

## Workspace layout

Same as `INSTALL.md` on `main` but swap the meta-tegra clone:

```bash
mkdir ~/yocto && cd ~/yocto

# Core (unchanged)
git clone -b scarthgap https://git.yoctoproject.org/git/poky
git clone -b scarthgap https://git.openembedded.org/meta-openembedded

# Thor BSP — branch name TBD, check upstream (see above)
git clone -b scarthgap-l4t-r38.x https://github.com/OE4T/meta-tegra.git

# This layer — jetson-thor branch
git clone -b jetson-thor https://github.com/perlowja/meta-nclawzero-base.git
```

Note: `meta-raspberrypi` is not needed on Thor-only builds; drop it
from `bblayers.conf` if you don't also build Pi images.

## `conf/local.conf` — Jetson Thor

```bitbake
# MACHINE name is meta-tegra-dependent. Verify against
# meta-tegra/conf/machine/ on the branch you cloned:
#   ls ~/yocto/meta-tegra/conf/machine/ | grep -i thor
# Common candidates:
#   jetson-thor-devkit
#   jetson-agx-thor-devkit
MACHINE ?= "jetson-agx-thor-devkit"

DISTRO ?= "poky"
IMAGE_FSTYPES += "wic wic.xz"

# Match the meta-tegra branch (r38.x for Thor). Pin the bootloader
# provider explicitly:
PREFERRED_PROVIDER_virtual/bootloader = "tegra-bootloader"

# Thor has an NVMe-first boot story vs Orin Nano's SD. Image size
# budget is generous — the Devkit ships with 1 TB NVMe.
# Adjust IMAGE_ROOTFS_SIZE if you want a fatter image than default.
```

## `conf/bblayers.conf`

Identical to the Jetson Orin case on `main`, just point at the
r38.x meta-tegra clone:

```bitbake
BBLAYERS ?= " \
  ${TOPDIR}/../poky/meta \
  ${TOPDIR}/../poky/meta-poky \
  ${TOPDIR}/../poky/meta-yocto-bsp \
  ${TOPDIR}/../meta-openembedded/meta-oe \
  ${TOPDIR}/../meta-openembedded/meta-python \
  ${TOPDIR}/../meta-openembedded/meta-networking \
  ${TOPDIR}/../meta-openembedded/meta-multimedia \
  ${TOPDIR}/../meta-tegra \
  ${TOPDIR}/../meta-nclawzero-base \
"
```

## GCC14 / glibc bbappend

The same `edk2-firmware-tegra` caveat from `INSTALL.md` on `main`
almost certainly applies — meta-tegra's BaseTools build is the
offending piece, and the compiler-flag fix is BSP-independent.
Apply the bbappend as described in `INSTALL.md` unless r38.x has
already upstreamed the fix (check `git log` for
"strict-prototypes" in the meta-tegra r38 branch first).

## Flash

Thor uses USB-recovery flashing like Orin. The wic.xz output lands
at:

```
build/tmp/deploy/images/<MACHINE>/
  nclawzero-base-image-<MACHINE>.wic.xz
```

The NVIDIA `flash.sh` script (shipped with JetPack 7 BSP) consumes
that image. Exact invocation depends on the Devkit revision — refer
to NVIDIA's Jetson Thor flashing guide.

## What the external tester should report back

Minimum acceptance before folding into `main`:

1. `bitbake nclawzero-base-image` completes without error.
2. Flashed image boots to serial/UART console.
3. Network comes up (DHCP on built-in interface).
4. `nclawzero-agent-image` boots and `zeroclaw` process is running
   (can be checked with `systemctl status zeroclaw` or `curl
   http://localhost:42617/v1/models`).

Any of those failing is a legitimate blocker — file an issue on
`github.com/perlowja/meta-nclawzero-base` with the `bitbake` /
`journalctl` output and the exact meta-tegra branch in use.

## Why a branch instead of main

`jetson-thor` lives as a branch — not yet merged to `main` — so
users who pull `main` never accidentally land in the unvalidated
Thor path. Once external validation passes, the BSP wiring squashes
into `main`'s `INSTALL.md` under a third target section alongside Pi
and Orin Nano.
