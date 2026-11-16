# IG Gate

An Android app that keeps Instagram and YouTube usable and cuts the endless scroll.

Instagram Search opens your messages. Five Reels in one go stay open, and the next one opens Instagram Home. Five YouTube Shorts in one go stay open, and the next one opens YouTube Home. A reel sent inside a chat still plays. Leave the reel screen and the count starts over.

It runs on Android. It does not run on iPhone.

The app watches on-screen buttons through an Accessibility Service. It does not read or save messages, and it has no internet permission.

## Why Android shows a risk warning

When the switch is turned on, Android says the app might put financial or personal information at risk. Android shows that sentence for every app that can see the screen and tap buttons. It cannot see that IG Gate only acts inside Instagram and YouTube.

The app has no API key and no internet permission. It does not read banking apps, passwords, or message text. The warning still appears. That is Android's own dialog, and the app cannot hide it.

## Security

No API keys, passwords, or signing keys belong in this repo. Cloud backup and device transfer of app data are off. Cleartext network traffic is blocked. The service is limited to the Instagram and YouTube packages.

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
