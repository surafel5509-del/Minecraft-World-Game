# CraftWorld 3D

An original, **100% offline** 3D voxel sandbox game for Android. Explore
procedurally generated worlds, mine resources, craft tools, build houses,
drive cars, fly planes and discover a second dimension — with **no internet,
no accounts, no ads, no analytics and no downloads**. All assets are bundled
and all saves stay on your device.

- **Package:** `com.craftworld3d.game`
- **Tech:** Java 17 · OpenGL ES 3.0 · Android 8.0+ (minSdk 26) · Gradle
- **Orientation:** landscape, phones and tablets

---

## Project layout

```
CraftWorld3D/
├── core/                          # Pure-Java game engine (no Android!)
│   └── src/main/java/com/craftworld3d/core/
│       ├── block/                 # Block, BlockRegistry, 84 data-driven blocks
│       ├── item/                  # Items, tools (5 types × 5 materials), food
│       ├── inventory/             # 36-slot inventory (9 hotbar), stacking/splitting
│       ├── crafting/              # Shaped + shapeless recipes, smelting, fuels
│       ├── noise/                 # Seeded Perlin noise (2D/3D, fBm, ridges)
│       ├── world/                 # World, 16×16×256 chunks, raycast, weather,
│       │   ├── gen/               #   overworld biomes/caves/ores/trees/villages,
│       │   └── blockentity/       #   "Depths" dimension, chest + furnace entities
│       ├── entity/                # Player, mobs (AI), villagers, arrows, drops
│       │   └── vehicle/           # Cars, planes, helicopter, boat, minecart
│       ├── redstone/              # Power propagation: wire/torch/lever/button/
│       │                          #   plate/repeater/piston/lamp/door
│       ├── progress/              # Achievements + statistics
│       ├── save/                  # Compressed, CRC-checked chunk files, backups
│       └── util/                  # AABB collision, math, minimal JSON
│   └── src/test/java/             # 101 JUnit tests
├── app/                           # Android layer
│   └── src/main/java/com/craftworld3d/game/
│       ├── gl/                    # GLES 3.0 renderer: chunk meshing (worker
│       │                          #   thread), frustum culling, fog, day/night
│       │                          #   sky, precipitation, particles, billboards
│       ├── engine/                # 20 TPS tick thread, chunk streaming,
│       │                          #   mining/placing/interactions, mob spawning,
│       │                          #   portals, vehicles, autosave
│       ├── ui/                    # Touch HUD: joystick, buttons, hotbar,
│       │                          #   inventory/crafting/furnace/chest/trading,
│       │                          #   pause & death screens
│       ├── audio/                 # SoundPool effects + looping music (local WAVs)
│       ├── assets/                # Texture atlas + tile table loader
│       └── *Activity.java         # Menu, create/load world, settings,
│                                  #   achievements, statistics, about
│   └── src/main/assets/           # atlas.png, tiles.json, 30 WAV sounds
└── tools/
    ├── gen_assets.py              # Regenerates all textures/sounds/icons
    └── sandbox/                   # Offline JUnit-compatible test runner
```

### Architecture rules

- `core` has **zero Android dependencies** — every gameplay system is
  unit-testable on a plain JVM.
- All world mutation happens on the **tick thread** (20 TPS). UI actions are
  queued; the GL thread only reads thread-safe snapshots.
- Chunk meshes are built on a **background mesher thread** and uploaded to
  VBOs with a per-frame budget; chunks are streamed in/out around the player
  and saved when modified.
- No allocations in the render loop (pooled particles, reusable buffers).

---

## Building

Open the project in Android Studio (Giraffe or newer) and run the `app`
configuration, or:

```bash
./gradlew :core:test          # run the 101 engine unit tests
./gradlew :app:assembleDebug  # build the APK
```

The engine tests also run without Gradle/network via the bundled runner:

```bash
bash tools/sandbox/run-tests.sh
```

To regenerate the procedural textures, sounds and launcher icons:

```bash
python3 tools/gen_assets.py   # requires Pillow
```

---

## How to play

| Control | Action |
|---|---|
| Left joystick | Move (push fully forward to sprint) |
| Drag right side | Look around |
| Tap right side | Use / interact / place |
| ⛏ (hold) | Break blocks / attack |
| ✛ | Use item: place block, eat food, deploy vehicle |
| ⤒ / ⇩ / » | Jump / sneak (toggle) / sprint |
| … | Inventory & 2×2 crafting |
| II | Pause (resume / save & quit) |
| Hotbar | Tap to select one of 9 slots |

- **Crafting:** inventory gives a 2×2 grid; tap a **crafting table** for 3×3.
- **Furnace:** input + fuel + output slots with live progress.
- **Chests:** 27 extra slots.
- **Villages** generate with houses, roads, farms and villagers that **trade**.
- **Vehicles:** craft an *Engine* (iron + iron + redstone + furnace) and
  *Wheels* (rubber ×2 + iron; smelt rubber-tree logs into rubber), then cars
  (3×2 recipes) or planes/helicopter (3×3). Tap a vehicle to board, tap it
  with coal to refuel. In the air, ▲/▼ control pitch or collective; the HUD
  shows speed, altitude, fuel and damage.
- **Parachute** (wool ×2 + string ×2): keep it selected in the hotbar while
  falling to glide down safely.
- **The Depths:** mine obsidian, tap it while holding a **nether crystal**
  (found as ore in the Depths, or via a first portal built from a crystal
  traded/looted) to ignite a portal. The second dimension has its own
  deterministic terrain, lava seas and tougher mobs.
- **Weather & time:** 20-minute day/night cycle, sun & moon, rain, thunder
  (snowfall in cold biomes), fog.

### Saves

Worlds live under the app's private storage at
`Android/data/com.craftworld3d.game/files/CraftWorld3D/worlds/<world_id>/`:

- `chunks/<dim>/c.X.Z.dat` — RLE + gzip chunk data with magic, version and
  CRC32 header; writes are atomic and keep a `.bak` which is used
  automatically if the primary file is corrupted.
- `level.json` — world meta, player, entities, time, weather.
- `stats.json`, `achievements.json`.

Autosave runs every 60 seconds plus on pause/quit; manual save via the pause
menu.

### Offline guarantee

The app declares **no `INTERNET` permission** — the OS physically prevents
any network access. There are no Firebase/Play Services/ads/IAP/analytics
dependencies (see `app/build.gradle`).

### Localization

English (default) and **Amharic (አማርኛ)** via standard Android resources
(`res/values`, `res/values-am`).

---

## Testing

101 JUnit tests cover: block registry, inventory stacking/splitting, shaped +
shapeless crafting & smelting, chunk generation, **seed determinism**, biomes
& villages, save/load round-trips & corruption recovery, player movement
(walking, jumping, swimming, ladders, fall damage), tools & durability,
mobs (AI activation, chase, drops, creeper fuse, despawn), redstone power
propagation, and vehicles (driving, fuel, boost rails, flight).

---

## Honest limitations (by design, documented per project rules)

- **Entity rendering** uses camera-facing pixel-art billboards, not
  articulated 3D models.
- **Meshing** is per-face hidden-surface culling (not greedy meshing); still
  comfortably fast at the default render distance.
- Doors render as thin panels on a fixed cell side (no facing metadata).
- Mounted players pause hunger/stamina simulation while driving.
- Portal ignition uses a nether crystal rather than flint & steel (no flint
  item exists in this game's item set).
- Graphics-quality presets currently map to render distance + particle
  density rather than separate shader paths.
