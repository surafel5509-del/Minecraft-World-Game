# CraftWorld 3D

A clean, Java-only Android 8+ voxel sandbox foundation. It runs without external assets: the GLES 3 renderer uses a compact procedural palette, chunks are generated on a worker thread, and the HUD is drawn with Android Canvas.

## Build and run
1. Open this folder in Android Studio Hedgehog or newer (JDK 17).
2. Sync Gradle, select an Android 8.0+ emulator/device, and run `app`.
3. Or run `./gradlew assembleDebug`; install `app/build/outputs/apk/debug/app-debug.apk`.

**Controls:** drag the left pad to walk, drag the right side to look, tap JUMP, and tap the crosshair to break/place. The hotbar selects the active block. Gamepad left stick/right stick/A are supported by the same input surface.

## Architecture
`world` owns deterministic chunk generation and block rules; `render` owns GLES resources and camera projection; `player` owns movement/inventory; `entity` and `vehicle` contain extensible simulation models; `ui` owns touch HUD. `WorldRepository` persists compressed chunk data and player JSON in app storage. See `docs/ROADMAP.md` for production milestones (network play, audio, Firebase/Play services and authored assets are intentionally adapters, not bundled credentials).
