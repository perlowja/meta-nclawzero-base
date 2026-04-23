# Docker images

Companion Docker deliverables for [`meta-nclawzero-base`](..).
Published to `ghcr.io/perlowja`.

The Yocto layer is for building flashable images for edge ARM
SBCs; these Docker images are for **trying the stack on a dev
machine before you commit to hardware**, or for embedding
ZeroClaw + NemoClaw in container-native deployments (compose, k8s).

## Images

### `ghcr.io/perlowja/nclawzero-demo`

One-command interactive bench. `docker run -it` drops you into
the `zterm` TUI client talking to a live ZeroClaw inside the
container.

```bash
docker run -it --rm ghcr.io/perlowja/nclawzero-demo
# Type a message, press Enter. Exit with Alt-X.
```

Multi-arch: `linux/amd64` + `linux/arm64` (runs natively on Intel
Macs, Apple Silicon, Linux x86_64, Raspberry Pi 4/5 64-bit, etc.).

### `ghcr.io/perlowja/nclawzero-agent`

Headless runtime. Starts ZeroClaw as pid 1, exposes the
OpenAI-compatible API on port 42617. No UI.

```bash
docker run -d -p 42617:42617 ghcr.io/perlowja/nclawzero-agent
# Then point any OpenAI-compatible client at http://localhost:42617
```

Same multi-arch coverage. Designed as the canonical base image —
the demo image is `FROM nclawzero-agent + zterm + an interactive
entrypoint`, so runtime parity between the two is guaranteed.

## Building locally

Requires Docker 23+ with `buildx` (bundled since Docker 23.0).

```bash
# Build + push both images
./docker/build.sh

# Local-only build (amd64 loaded into your docker engine)
./docker/build.sh --no-push --platforms=linux/amd64

# Custom tag
./docker/build.sh --tag v0.1.0
```

The script creates a buildx builder named `nclawzero-builder` on
first run (idempotent) and handles the multi-stage build for you.

### Build-time caveats

- The **demo image cargo-builds `zterm` from source**. Under
  `buildx` + qemu emulation for `linux/arm64`, this takes 15-20
  min. The agent image is all binary-fetch (~2-3 min per arch).
- Push requires a GitHub PAT with `write:packages` scope for the
  `perlowja` user. `docker login ghcr.io -u perlowja -p <PAT>`
  once; credentials live in `~/.docker/config.json`.
- Planned: once `perlowja/zterm` v0.2.1 ships prebuilt release
  tarballs, this Dockerfile swaps to fetch-instead-of-compile
  and the demo image drops to ~3 min per arch.

## Upstream pins

Each image bakes in specific upstream versions for reproducibility
— every `docker pull <tag>` gets identical bits even if upstream
moves:

| Component | Pin | Source |
|---|---|---|
| ZeroClaw | `v0.6.9` | [GitHub release SHA256](https://github.com/zeroclaw-labs/zeroclaw/releases/tag/v0.6.9) |
| NemoClaw | commit `885c75b73d14f223c9cd5cd45e5274d46472b280` | [NVIDIA/NemoClaw](https://github.com/NVIDIA/NemoClaw) main @ 2026-04-23 |
| Node.js | `22.22.2` LTS | [nodejs.org](https://nodejs.org/dist/v22.22.2/) |
| zterm | `v0.2.0` tag | [perlowja/zterm](https://github.com/perlowja/zterm/tree/v0.2.0) |

Bumping any pin is a one-line edit in the relevant `Dockerfile`
plus a rebuild. NemoClaw commit is stored as the `NEMOCLAW_REV`
build arg; bump by re-running
`gh api repos/NVIDIA/NemoClaw/commits/main --jq .sha` and pasting
the new SHA.

Note that this is a different policy from the sibling Yocto
layer, which tracks upstream NemoClaw `main` HEAD via
`SRCREV = "${AUTOREV}"`. The Yocto layer's users build from
source at their own clock; Docker-image users pull pre-built bits
and expect per-tag reproducibility. Same upstream, two fetch
policies, each correct for its medium.

## License

Apache-2.0 — same as the Yocto layer. Each upstream component
retains its own license: ZeroClaw MIT, NemoClaw Apache-2.0,
Node.js MIT, zterm Apache-2.0.
