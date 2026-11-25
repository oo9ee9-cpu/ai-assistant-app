# AI Assistant Android

A native Android chat client for the Anthropic Messages API.

## Build
Open the `android` folder in Android Studio, or run:
`gradle -p android assembleDebug`

The APK is generated at `android/app/build/outputs/apk/debug/app-debug.apk`.

## Configure
Install the app, enter your Anthropic API key, save it on-device, then chat.
The key is stored in Android private SharedPreferences. API usage may incur charges.
