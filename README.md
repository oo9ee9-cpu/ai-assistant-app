# Aster Engine v1.2 — C++20 Android 3D Engine Prototype

This repository now contains a real native OpenGL ES 3.0 cube renderer, not the earlier Canvas viewport mock-up.

## Included
- Platform-independent C++20 runtime, math primitives and matrix operations.
- Scene entity/transform storage and basic asset registry.
- Fixed-step update accumulator with a 250 ms frame-delta clamp.
- Renderer interface with Android OpenGL ES 3.0 implementation.
- Perspective camera, depth testing, cube mesh and directional diffuse lighting.
- JNI bridge and Android GLSurfaceView lifecycle.
- Editor shell with hierarchy, inspector, add-cube and play/pause controls.
- Host CMake test target.

## Android build
JDK 17, Android SDK 35, NDK 27.0.12077973, CMake 3.22.1.
Run `gradle assembleDebug`. APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Host core tests
`cmake -S app/src/main/cpp -B build/core && cmake --build build/core && ctest --test-dir build/core --output-on-failure`

## Engineering status — no inflated claims
The renderer is real OpenGL ES 3.0 on Android. The C++ runtime and math/scene layer are platform-neutral and host-testable. A Windows window/surface adapter and Windows graphics backend are not shipped yet.
Physics currently has only a minimal integration point and ground clamp, not a full rigid-body solver. The asset registry is metadata-only; no glTF importer, PBR material pipeline, shadows, skeletal animation, Vulkan backend, project serialization, full editor gizmos, or audio mixer are implemented. These need dedicated implementation and device validation before production use.
