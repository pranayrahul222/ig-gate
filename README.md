# IG Gate

An Android app that opens Instagram and YouTube inside itself and blocks the endless scroll.

A Reels page goes back to Instagram Home. Search and Explore open your messages. A Shorts page goes back to YouTube Home. Home, messages, stories, and a normal YouTube video stay open.

It runs on Android. It does not run on iPhone.

Instagram and YouTube stay inside this app. It does not read other apps, and it does not tap buttons on the screen. The only permission is internet, used to load those two sites.

## Security

No API keys, passwords, or signing keys belong in this repo. Cloud backup and device transfer of app data are off. Cleartext network traffic is blocked.

## Use

1. Install the app.
2. Tap **Open Instagram** or **Open YouTube** and sign in once.

The normal Instagram app can stay installed. Alerts from the website are weak. Open Instagram from IG Gate when you want Reels blocked.

## Build

Install a JDK 17 and the Android SDK, then:

```bash
./gradlew assembleDebug
```

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`.
