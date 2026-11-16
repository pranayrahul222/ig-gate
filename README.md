# IG Gate

An Android app that keeps Instagram and YouTube usable and cuts the endless scroll.

Instagram Search opens your messages. Five Reels in one go stay open, and the next one opens Instagram Home. Five YouTube Shorts in one go stay open, and the next one opens YouTube Home. A reel sent inside a chat still plays. Leave the reel screen and the count starts over.

It runs on Android. It does not run on iPhone.

The app watches on-screen buttons through an Accessibility Service. It does not read or save messages, and it has no internet permission.

## One-time setup

1. Install the app.
2. Open IG Gate and tap **Allow access**.
3. In the Accessibility list, turn IG Gate on.
4. If Android blocks the switch, open Settings, Apps, IG Gate, the corner menu, and tap **Allow restricted settings**. Then turn IG Gate on.

## Build

Install a JDK 17 and the Android SDK, then:

```bash
./gradlew assembleDebug
```

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`.
