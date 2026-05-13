# MirrorLink Android App

The Android app provides the native sender surface for MirrorLink.

Current alpha:

- Installable Android app shell
- MirrorLink sender UI
- Release APK packaging

Planned sender capabilities:

- Start screen sharing
- Create or join sender room
- Connect to the signaling server
- Stream screen content to browser receivers with WebRTC
- Package APKs for GitHub Releases

## Build APK

From the repository root:

```bash
./gradlew :apps:android:assembleDebug
```

The APK is created at:

```txt
apps/android/build/outputs/apk/debug/android-debug.apk
```
