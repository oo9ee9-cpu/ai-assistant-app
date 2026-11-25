# Aster Engine
Native Android editor shell backed by a C++20 engine core.

## Current milestone
- Previous AI Assistant / Anthropic chat UI and API code removed from the repository.
- New Aster Engine editor interface with viewport preview and scene hierarchy.
- C++20 shared library compiled by Android NDK + CMake and connected through JNI.
- Basic play/stop controls connected to the native core.

## Honest implementation status
The viewport is currently a 2D editor preview, not a real 3D renderer. Vulkan or another graphics backend is a later milestone. This repository is the foundation, not a finished game engine.

## Build
Open in Android Studio or run `gradle assembleDebug`.
APK: `app/build/outputs/apk/debug/app-debug.apk`.
