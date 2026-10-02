# CraftWorld 3D

A native Android open-world voxel sandbox prototype, written in Java and rendered directly with **OpenGL ES 3.0**. It targets Android 7.0 (API 24) and later.

## What is playable

- Seeded, deterministic terrain in canonical `16 × 16 × 256` chunks
- Plains, desert, mountain, water, forest and snow terrain
- Infinite coordinate space with asynchronous distance-based chunk streaming
- First-person movement, gravity, jumping and touch camera
- Nine-slot hotbar backed by a 36-slot inventory
- Place blocks using the **BUILD** button; center-screen voxel ray casting
- 60+ stable block types with hardness and colors
- Ten-minute day/night cycle, directional lighting and distance fog
- GLES 3 cube VBO, depth/cull testing, generated sky color, landscape immersive mode
- Health/hunger HUD and English/Amharic Android resources
- Domain models for recipes, cars, aircraft, mobs and atomic player saves

The project also provides extensible models for five car types, four aircraft types, fuel, damage, vehicle physics, passive/hostile mobs, and car/airplane crafting recipes. Large production systems such as online multiplayer, Play Games, billing, ads, authored audio, full redstone simulation, portals and store publishing require external services/content and are intentionally not represented as finished features.

## Controls

| Control | Action |
|---|---|
| Left circular area | Move |
| Drag right side | Look |
| JUMP | Jump |
| BUILD | Place selected block at crosshair |
| Bottom hotbar | Select block |

## Build in Android Studio

1. Install Android Studio Ladybug or newer, JDK 17, and Android SDK Platform 35.
2. Open this repository (the root containing `settings.gradle`).
3. Allow Gradle sync to finish.
4. Select an API 24+ physical device/emulator with OpenGL ES 3 support.
5. Run the `app` configuration.

Command-line build with Gradle 8.9:

```bash
gradle testDebugUnitTest assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## GitHub Actions APK

Every push and pull request runs `.github/workflows/android.yml`. Open **Actions → Build Android APK → latest run → Artifacts**, then download `CraftWorld-3D-debug-apk`. You can also start a run with **Run workflow**.

A release build needs your own signing key. Never commit it; configure signing with encrypted GitHub secrets or Android Studio's **Generate Signed Bundle / APK**.

## Architecture

- `engine/`: GLES lifecycle, shaders, simulation loop and ray casting
- `world/`: block registry, chunks, deterministic noise, generation and streaming
- `player/`: physics, inventory and crafting
- `entity/`: mob AI domain model
- `vehicle/`: common vehicle, car and aircraft physics
- `data/`: persistence boundary
- `ui/`: adaptive Canvas touch HUD

The world generator is Android-independent and unit tested. Chunk generation runs off the render thread; simulation state is kept separate from views so it can evolve toward multiplayer/server authority.

## Roadmap

1. Greedy chunk meshes and persistent compressed chunk deltas
2. Breaking progress, tools, drops, crafting/inventory screens
3. Entity renderer, combat, spawning and pathfinding
4. Vehicle meshes and contextual driving/flight HUDs
5. Weather particles, texture atlas, sound and authored assets
6. LAN/online protocol and opt-in Play Games/cloud integrations

## License and assets

Source code is provided by this repository. The prototype uses generated colors and platform drawing primitives, so it includes no copyrighted Minecraft textures, sounds, branding, or models.
