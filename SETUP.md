# Development Environment Setup — Ubuntu 24.04 LTS

This guide walks you through setting up a complete Android/Kotlin development
environment on **Ubuntu 24.04.4 LTS** to build rg-counter (and similar Android
apps) for the Anbernic RG Rotate.

---

## 1. Install Java 17

Android's build toolchain requires JDK 17.

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk
java -version   # should print openjdk 17.x.x
```

Set it as the active JDK if you have multiple versions:

```bash
sudo update-alternatives --config java
# Select the entry for java-17-openjdk-amd64
```

---

## 2. Install Android Studio

Android Studio bundles the Android SDK, Gradle, emulator, and ADB.

1. Download the latest stable build from
   <https://developer.android.com/studio>
   (the `.tar.gz` package for Linux).

2. Extract and install:

   ```bash
   tar -xzf android-studio-*.tar.gz -C ~/
   ~/android-studio/bin/studio.sh
   ```

3. Follow the Setup Wizard:
   - Choose **Standard** installation.
   - Accept all SDK licences.
   - Let it download the Android 12 (API 31) SDK platform.

4. (Optional) Create a desktop shortcut / launcher entry:

   ```bash
   # Add to PATH permanently
   echo 'export PATH="$PATH:$HOME/android-studio/bin"' >> ~/.bashrc
   source ~/.bashrc
   ```

---

## 3. Configure the Android SDK

After Android Studio's first-run wizard, SDK components live at
`~/Android/Sdk` by default.

Add SDK tools to `PATH` so `adb` and related tools work from any terminal:

```bash
echo 'export ANDROID_HOME="$HOME/Android/Sdk"' >> ~/.bashrc
echo 'export PATH="$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools:$ANDROID_HOME/tools/bin"' >> ~/.bashrc
source ~/.bashrc
```

Verify:

```bash
adb version   # Android Debug Bridge version 1.x.x
```

---

## 4. Install SDK Platform and Build Tools via SDK Manager

In Android Studio: **File → Settings → Languages & Frameworks → Android SDK**

Under **SDK Platforms**, install:
- **Android 12 (API 31)** — this is the OS on the RG Rotate.

Under **SDK Build-Tools**, install:
- **34.0.0** (or the latest stable version).

Click **Apply** and let it download.

---

## 5. Clone and open rg-counter

```bash
git clone https://github.com/MittyKittens/rg-counter
cd rg-counter
studio.sh .     # opens the project in Android Studio
```

Or open it manually: **File → Open** → select the `rg-counter` directory.

Wait for the Gradle sync to complete. It will download all dependencies
(Shizuku API, AndroidX, Material Components) automatically.

---

## 6. Connect the RG Rotate via USB

### Enable Developer Options

1. On the device: **Settings → About device** → tap **Build number** 7 times.
2. Go back to Settings → **Developer options** → enable **USB debugging**.

### Connect and authorise

```bash
# Connect USB cable, then:
adb devices
```

The first time you run `adb devices` after plugging in, the device will show
a "Allow USB debugging?" dialog — tap **Allow**. Re-run `adb devices` until
the device appears as `authorized`:

```
List of devices attached
XXXXXXXX    device
```

---

## 7. Install and start Shizuku

rg-counter uses [Shizuku](https://shizuku.rikka.app) to run a shell-uid
service that reads `/dev/input/event2` (the Hall-effect hinge sensor).

### Install Shizuku

```bash
# Option A — sideload from the official release:
adb install shizuku-v13.x.x.apk

# Option B — install from Play Store on the device.
```

### Start Shizuku via ADB (easiest for development)

```bash
adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh
```

Shizuku should now report "Running" in its main screen.

### Auto-start across reboots (optional, no PC needed after setup)

Grant Shizuku the `WRITE_SECURE_SETTINGS` permission once:

```bash
adb shell pm grant moe.shizuku.privileged.api android.permission.WRITE_SECURE_SETTINGS
```

Then in the Shizuku app, enable **Start via Wireless Debugging**. Shizuku
will auto-start on every boot without needing a PC.

---

## 8. Build and install rg-counter

### From Android Studio

1. Select **Run → Run 'app'** (or press **Shift+F10**).
2. Android Studio will build the APK and install it directly on the connected device.

### From the command line

```bash
cd rg-counter
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 9. Grant the POST_NOTIFICATIONS permission (Android 13+)

The RG Rotate runs Android 12 so this permission is auto-granted. If you ever
test on a newer device:

```bash
adb shell pm grant com.mittykittens.rgcounter android.permission.POST_NOTIFICATIONS
```

---

## 10. Verify it works

1. Open the **RG Counter** app on the device.
2. Tap **Start watcher** (grant Shizuku permission when prompted).
3. Flip the hinge open and closed a few times.
4. The counters in the app and in the notification bar should increment.

Watch the logcat stream for events in real time:

```bash
adb logcat -s RgCounter
```

---

## Useful ADB commands for hinge debugging

```bash
# Watch raw hinge events directly from the kernel
adb shell getevent -l /dev/input/event2

# List all input devices (find the gpio-keys device)
adb shell getevent -lp

# Dump recent logs tagged RgCounter
adb logcat -s RgCounter -d
```

---

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| `adb: no devices` | Check USB cable; re-enable USB debugging; run `adb kill-server && adb start-server` |
| Shizuku shows "not running" | Re-run the start script: `adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh` |
| "Shizuku permission denied" in app | Open Shizuku → tap the rg-counter entry → Grant |
| Counter doesn't increment | Check `adb logcat -s RgCounter`; confirm the watcher is active in the app |
| Build fails: "SDK not found" | Set `ANDROID_HOME` correctly (see step 3) or set `sdk.dir` in `local.properties` |
