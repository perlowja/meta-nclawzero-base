# meta-nclawzero-base

Yocto layer for building minimal Linux images for the **Raspberry Pi**
family, plus optional AI-agent runtime recipes layered on top.

All recipes are ARM-universal (`COMPATIBLE_HOST = "(aarch64|arm).*-linux"`).
The maintainer has booted Pi 4 (2 GB + 8 GB); other family members
(Pi 5, Pi 3 64-bit, Pi Zero 2 W) are recipe-compatible but untested.
See `INSTALL.md` for the full `MACHINE` value matrix with
tested/untested flags.

nclawzero currently supports:

- x86_64 + macOS via Docker/Podman containers
  (`ghcr.io/perlowja/nclawzero-demo`, `ghcr.io/perlowja/nclawzero-agent`)
- ARM Raspberry Pi family (Pi 4, Pi 5, Pi Zero 2 W, Pi 3 64-bit) via
  Yocto-built flashable images from this layer or pre-built SD images
  from `pi-gen-nclawzero`

Jetson family support is deferred pending hardware validation.

No pre-built images are distributed from this repository. Everything
is built locally from upstream source and this layer's recipes.

## What's in here

### Base
- `nclawzero-base-image` — headless console image: systemd, SSH,
  networking, shell/dev tools, Python. This is the "just Linux +
  goodies" image. No compositor, no agent, nothing else.
- `nclawzero-desktop-image` — base + the Weston Wayland compositor
  with its built-in VNC backend. Connect from your workstation via
  `vncviewer <device-ip>:5901`.

### Optional AI agent stack (opt-in)
- `nclawzero-agent-image` — base + ZeroClaw (Rust AI agent runtime)
  + NemoClaw (Node.js sandbox framework, provisioned from upstream
  `github.com/NVIDIA/NemoClaw` on first boot).
- `packagegroup-nclawzero-agent` — same dependencies, for inclusion
  in your own image recipes.

## Recipes

| Recipe | Role |
|---|---|
| `recipes-core/images/nclawzero-base-image.bb` | Headless minimal image |
| `recipes-core/images/nclawzero-desktop-image.bb` | Base + Weston/VNC |
| `recipes-core/images/nclawzero-agent-image.bb` | Base + agent stack |
| `recipes-core/packagegroups/packagegroup-nclawzero-base.bb` | Shell/dev tools |
| `recipes-core/packagegroups/packagegroup-nclawzero-agent.bb` | AI agent runtime |
| `recipes-core/packagegroups/packagegroup-nclawzero-desktop.bb` | Weston, mesa, fonts |
| `recipes-connectivity/nclawzero-network/` | Wired networking defaults |
| `recipes-devtools/nodejs/` | Pre-built Node.js aarch64 binary |
| `recipes-zeroclaw/` | ZeroClaw agent (pulls public GitHub release) |
| `recipes-nemoclaw/` | NemoClaw (builds from upstream + local patches) |

## Licensing

This layer is Apache-2.0 licensed. Upstream projects retain their
own licenses:

- **ZeroClaw** (`zeroclaw-labs/zeroclaw`) — MIT
- **NemoClaw** (`NVIDIA/NemoClaw`) — Apache-2.0

The `recipes-nemoclaw/` tree contains three local patches authored
by Jason Perlow. Those patches are themselves Apache-2.0 derivative
work against upstream NemoClaw:

- `0001-fix-snapshot-symlink-protection.patch` — security: guard
  `cpSync` and directory walker against symlink traversal.
- `0002-fix-config-file-permissions.patch` — security: enforce
  mode `0600` on credential config files.
- `0003-feat-agent-defs-zeroclaw.patch` — integration: register
  ZeroClaw as an agent runtime alongside Hermes.

No binaries from any vendor are redistributed through this layer.
All fetches happen at build time from public upstream sources.

## Layer dependencies

- `openembedded-core` (poky)
- `meta-openembedded/meta-oe`, `meta-openembedded/meta-python`,
  `meta-openembedded/meta-networking`
- `meta-raspberrypi` (for Pi targets)

## Getting started

- **`QUICKSTART.md`** — one-page TL;DR: hardware requirements
  (host + target), a six-step build recipe, pointers into the
  upstream Yocto Project documentation. Start here if you've
  never used Yocto before.
- **`INSTALL.md`** — full build-host setup, `bblayers.conf` /
  `local.conf` templates.
- **`FLASH.md`** — writing images to SD cards on Raspberry Pi
  (`bmaptool`, `dd`, macOS-specific notes).
- **`docker/README.md`** — Docker deliverables: `nclawzero-demo`
  (one-command interactive bench with zterm TUI + live agent) and
  `nclawzero-agent` (headless runtime for compose/k8s). Published
  to `ghcr.io/perlowja`. If you just want to feel the stack
  before committing to hardware, this is the fastest path.

New to Yocto? The [Yocto Project Quick Build](https://docs.yoctoproject.org/brief-yoctoprojectqs/)
is a 20-minute walkthrough that builds `core-image-minimal` for
qemu — everything `meta-nclawzero-base` does is plain Yocto on top
of that foundation.

### Try the stack in 30 seconds (Docker)

Before touching Yocto or flashing hardware, you can kick the tires
on any x86_64 / arm64 machine with Docker installed:

```bash
docker run -it --rm ghcr.io/perlowja/nclawzero-demo
```

That's it — zterm TUI opens, already connected to a live ZeroClaw
agent inside the container. See `docker/README.md` for the
`nclawzero-agent` headless variant.

## Layer compatibility

- Yocto release: **scarthgap** (5.0 LTS).
- Tested build hosts: Debian 12, Ubuntu 24.04.

## Contributing

Issues and PRs welcome at this repository. Upstream fixes to
NemoClaw or ZeroClaw should be sent to their respective
repositories directly:

- `github.com/NVIDIA/NemoClaw`
- `github.com/zeroclaw-labs/zeroclaw`

## Author

Jason Perlow — `<jperlow@gmail.com>`
