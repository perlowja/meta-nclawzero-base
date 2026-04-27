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

## After flashing

On any image:

```bash
# Check the agent runtime (agent-image only)
systemctl status zeroclaw
systemctl status nemoclaw-firstboot

# Tail the first-boot log if NemoClaw provisioning is slow
journalctl -u nemoclaw-firstboot -f
```
