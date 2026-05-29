# rg-counter

> A simple Android app for the **Anbernic RG Rotate** that counts how many times you flip the hinge open and closed, and shows the running total in a persistent notification.

## How it works

The RG Rotate has a swivel hinge with a built-in Hall-effect switch wired directly into the
Linux kernel's `gpio-keys` input driver (`/dev/input/event2`). rg-counter uses
[Shizuku](https://shizuku.rikka.app) to run a shell-uid service that streams from that
device and fires a broadcast into the app on every hinge transition.

A foreground `CounterService` keeps a notification in the status bar showing the
live count — so you can see your flip count at a glance even when the app is in the
background.

See [`skills.md`](./skills.md) for a detailed explanation of how hinge event detection works.

## Requirements

- **Anbernic RG Rotate** (Android 12, Unisoc T618). The hinge detection is hard-coded to
  `/dev/input/event2` on this device.
- **No root needed.** The app runs its hinge watcher as the `shell` user via Shizuku.
- **[Shizuku](https://shizuku.rikka.app)** installed and running on the device.

## Install

Grab the APK from the [Releases](../../releases) page and sideload it:

```bash
adb install rg-counter-v1.0.apk
```

## Usage

1. Open the **RG Counter** app.
2. Tap **Start watcher** and grant Shizuku permission when prompted.
3. Flip the hinge. The counts update live in the app and in the notification bar.
4. Tap **Reset** to zero the counts.

## Build from source

Requires **JDK 17** and the **Android SDK** (API 34 build tools).

```bash
git clone https://github.com/MittyKittens/rg-counter
cd rg-counter
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

See [`SETUP.md`](./SETUP.md) for a complete step-by-step guide to setting up the
development environment on Ubuntu 24.04 LTS.

## Documentation

| File | Contents |
|------|----------|
| [`skills.md`](./skills.md) | How hinge event detection works (Hall sensor → kernel input → Shizuku → broadcast) |
| [`SETUP.md`](./SETUP.md) | Dev environment setup guide for Ubuntu 24.04 LTS |

