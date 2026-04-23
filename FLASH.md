# Flashing images to SD cards

No prebuilt images are distributed — you build them yourself per
`INSTALL.md`, then flash the resulting `.wic` file onto an SD card.

⚠️ `dd` and `bmaptool` both write raw bytes to the device.
**Double-check the target device path** (`/dev/sdX`, `/dev/mmcblk0`,
`/dev/disk4` on macOS) before running any of these commands. The
wrong path will overwrite your hard drive without warning.

On Linux, confirm with `lsblk` or `sudo fdisk -l` immediately after
inserting the SD card — the new device usually shows up at the
bottom of the list.

On macOS, use `diskutil list` and note the `/dev/diskN` for the SD
card. Always `diskutil unmountDisk /dev/diskN` before `dd`ing to
it, and use `/dev/rdiskN` (raw device, much faster) rather than
`/dev/diskN`.

## Raspberry Pi (any 64-bit generation)

The same flash flow works for **Pi 5**, **Pi 4 / CM4**, **Pi 3 64-bit**,
and **Pi Zero 2 W 64-bit** — just swap the `MACHINE` in the path.
Substitute `raspberrypi5`, `raspberrypi4-64`, `raspberrypi3-64`, or
`raspberrypi0-2w-64` for the directory and filename stem. 32-bit
Pi targets (`raspberrypi4`, `raspberrypi3`, `raspberrypi2`) follow
the identical `dd` / `bmaptool` procedure with their own MACHINE-stem
paths.

Examples below use `raspberrypi4-64` (the maintainer-tested target).

### Fastest path — `bmaptool` (Linux)

`bmaptool` only writes the parts of the image that contain data,
using the `.bmap` sidecar file bitbake produces next to the image.
~10× faster than `dd` for Yocto images because most of a 2 GB
rootfs is zeros.

```bash
sudo bmaptool copy \
  tmp/deploy/images/raspberrypi4-64/nclawzero-base-image-raspberrypi4-64.wic.gz \
  /dev/sdX
```

### Portable path — `dd` (Linux and macOS)

```bash
# Linux
gunzip -k nclawzero-base-image-raspberrypi4-64.wic.gz
sudo dd if=nclawzero-base-image-raspberrypi4-64.wic of=/dev/sdX \
        bs=4M status=progress conv=fsync
sync

# macOS (use the raw device /dev/rdiskN for 4-5× speed)
diskutil unmountDisk /dev/diskN
gunzip -k nclawzero-base-image-raspberrypi4-64.wic.gz
sudo dd if=nclawzero-base-image-raspberrypi4-64.wic of=/dev/rdiskN \
        bs=4m status=progress
sync
```

### GUI path — balenaEtcher or Raspberry Pi Imager

Both accept `.wic` files directly. Gunzip first, then pick "Use
custom" in Raspberry Pi Imager or "Flash from file" in Etcher.

### First boot

Pop the card into the Pi, hook up Ethernet + power. The default
image has `debug-tweaks` (passwordless root) enabled — log in at
the console or via SSH as `root` with no password. **Remove
`debug-tweaks` from the image recipe before shipping this to
anything that touches the open internet.**

## Jetson Orin Nano (SD-boot flow)

The Jetson Orin Nano Developer Kit has two boot paths:

1. **SD card boot** (simpler, what this layer targets). The UEFI
   boots straight from the SD card — no `flash.sh` / USB-recovery
   dance required.
2. **NVMe/eMMC boot via `flash.sh`** (more involved, out of scope
   here).

### Flashing the SD card

The Jetson Yocto build produces `.wic.xz` — decompress first.

```bash
# Linux
xz -d -k nclawzero-base-image-jetson-orin-nano-devkit.wic.xz
sudo bmaptool copy nclawzero-base-image-jetson-orin-nano-devkit.wic \
                   /dev/sdX
# or without bmap
sudo dd if=nclawzero-base-image-jetson-orin-nano-devkit.wic \
        of=/dev/sdX bs=4M status=progress conv=fsync
sync
```

### Boot-order gotcha

Some Orin Nano UEFI `extlinux` loaders boot the first `LABEL` entry
in `extlinux.conf` regardless of the `DEFAULT` directive. If your
image doesn't boot to the expected kernel, check `extlinux.conf` on
the boot partition and ensure the intended entry comes first in
file order, not just pointed at by `DEFAULT`.

Note that `TIMEOUT` in extlinux is in **deciseconds**, not seconds
— `TIMEOUT 30` is 3 seconds. Use `TIMEOUT 300` for a 30-second
menu.

### First boot

The Jetson takes noticeably longer than the Pi to come up on the
first boot — the initial `systemd-journal-flush` + initial resize
fills up the rest of the card, typically 60-90 seconds before SSH
is reachable. Give it time before assuming it's hung.

## Other Jetson family members (USB-recovery / `flash.sh`) — untested

The maintainer has only booted the Orin Nano Devkit (SD-card path).
The Jetson family members below are recipe-compatible but use
different flash flows; they need NVIDIA's USB-recovery tooling
because they boot from eMMC or NVMe, not SD.

| Module | Primary storage | Flash tooling |
|---|---|---|
| Jetson Orin NX (8 / 16 GB) | NVMe | JetPack 6 `flash.sh` |
| Jetson AGX Orin (32 / 64 GB) | eMMC + NVMe | JetPack 6 `flash.sh` |
| Jetson Xavier NX | eMMC | JetPack 5 `flash.sh` |
| Jetson AGX Xavier | eMMC | JetPack 5 `flash.sh` |
| Jetson Nano (legacy 4 GB) | SD or eMMC | JetPack 4 flashing tools |
| Jetson AGX Thor | NVMe | JetPack 7 `flash.sh` (on the [`jetson-thor`](../../tree/jetson-thor) branch) |

### General shape of the `flash.sh` flow

1. Put the module in recovery mode (force-recovery button / jumper —
   see the Devkit's guide).
2. Connect via USB-C (or the Devkit's USB-micro "recovery" port).
3. Confirm the host sees it: `lsusb | grep -i nvidia` — expect an
   "NVIDIA Corp." entry in APX mode.
4. From the Yocto build directory (or NVIDIA's BSP tarball), invoke
   `flash.sh <MACHINE> mmcblk0p1` (exact syntax depends on JetPack
   version). meta-tegra integrates this via `bitbake -c do_image_complete` +
   a generated flashing script per board.
5. Reboot the target. First boot takes 2–3 minutes while the Jetson
   expands the partition and runs NVIDIA's OEM-config.

### Authoritative references

Don't follow this section as gospel — for eMMC / NVMe flash the NVIDIA
Jetson Linux Developer Guide for the specific L4T release is
authoritative:

- Orin family (L4T r36): https://docs.nvidia.com/jetson/archives/r36.3/DeveloperGuide/
- Xavier family (L4T r35): https://docs.nvidia.com/jetson/archives/r35.5/DeveloperGuide/
- Legacy Nano (L4T r32): https://docs.nvidia.com/jetson/l4t/
- Thor (L4T r38+): check NVIDIA's current JetPack 7 release notes

Open an issue on `github.com/perlowja/meta-nclawzero-base` if you
validate one of these flows — notes welcome so a future reader has
real commands instead of shape-of-the-flow prose.

## After flashing

On any image:

```bash
# Check the agent runtime (agent-image only)
systemctl status zeroclaw
systemctl status nemoclaw-firstboot

# Tail the first-boot log if NemoClaw provisioning is slow
journalctl -u nemoclaw-firstboot -f
```
