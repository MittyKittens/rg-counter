# Hinge Event Detection on the Anbernic RG Rotate

**Device:** Anbernic RG Rotate (Unisoc T618, Android 12, non-rooted)
**Source:** Reverse-engineered by [jlgrimes/flipx](https://github.com/jlgrimes/flipx) — see
[hinge-findings.md](https://github.com/jlgrimes/flipx/blob/main/hinge-findings.md) for the
full investigation log.

## How the hinge signal works

The RG Rotate hinge contains a **Hall-effect switch** (a sensor that detects magnetic
fields) wired directly into the Linux kernel's input subsystem as a `gpio-keys` device.
It does **not** use the Android SensorManager API and does **not** fire any standard
broadcast Intent — it is a raw kernel key event.

### Input device identity

```
/dev/input/event2   (kernel driver: gpio-keys)
```

Verify on your device with:

```sh
adb shell getevent -lp
```

Look for a device named `"gpio-keys"` that declares `KEY_F9` and `KEY_F12`.

### Event table

| Hinge transition              | Linux event type | Linux keycode | Scancode (hex) | Value | Android KEYCODE |
|-------------------------------|-----------------|---------------|----------------|-------|-----------------|
| **Close** (hinge folds)       | `EV_KEY` (0x01) | `KEY_F12`     | `0x58`         | `1`   | `KEYCODE_F12`   |
| **Open** (device already awake) | `EV_KEY` (0x01) | `KEY_F12`   | `0x58`         | `0`   | `KEYCODE_F12` release |
| **Open** (waking from sleep)  | `EV_KEY` (0x01) | `KEY_F9`      | `0x43`         | `1`   | `KEYCODE_F8`    |

> **Note on the KEY_F9 → KEYCODE_F8 mapping:** Linux kernel scancode `0x43` is named
> `KEY_F9` in the kernel headers. The device's `.kl` (key layout) file remaps it to
> Android's `KEYCODE_F8`. These are different numbering systems — Linux key name ≠ Android
> key name. rg-counter listens for the Linux scancode (`0x43`) directly via `getevent`,
> so the Android keycode name is informational only.

**State model:** `KEY_F12` is held DOWN for the entire time the hinge is closed, and
released (value `0`) when the hinge opens. When the device is asleep and the hinge opens,
a separate `KEY_F9` (→ Android `KEYCODE_F8`) DOWN event is emitted instead — this is
configured as a wake key to bring the device out of suspend.

### Why KEY_F12 does not trigger the OS

The OEM settings app (`com.anbanic.rgsettings`) registers a `KeyCombinationManager` entry
that **ignores** `KEYCODE_F12` system-wide, so the OS does not react to it. A user-space
listener still receives the event normally.

## How rg-counter reads these events

Because `/dev/input/event2` requires the `input` group or `shell` uid to read, a
standard Android app cannot access it directly. `rg-counter` uses
[Shizuku](https://shizuku.rikka.app) to run a privileged `UserService` as the `shell`
uid:

```
[hinge Hall sensor]
         │
         ▼  EV_KEY on /dev/input/event2
[gpio-keys kernel driver]
         │
         ▼  getevent streaming (shell uid via Shizuku UserService)
HingeUserService.kt
         │  am broadcast -n com.mittykittens.rgcounter/.HingeReceiver
         │                -a rgcounter.HINGE_OPEN | rgcounter.HINGE_CLOSE
         ▼
HingeReceiver.kt  →  Prefs (SharedPreferences)  →  CounterService notification
```

### HingeUserService event parsing

`getevent /dev/input/event2` emits lines of the form:

```
<type_hex> <code_hex> <value_hex>
```

The service filters for `type == 0x01` (EV_KEY) and checks the code:

```kotlin
KEY_F12 (0x58), value 1  →  ACTION_CLOSE  (hinge just closed)
KEY_F12 (0x58), value 0  →  ACTION_OPEN   (hinge just opened, device was awake)
KEY_F9  (0x43), value 1  →  ACTION_OPEN   (hinge just opened, device was asleep)
```

All other EV_KEY codes on event2 (volume up/down, power button) are ignored.

### Why a BroadcastReceiver instead of a background Service

Android 12+ blocks `am broadcast` from starting foreground services when the target
app has been in the background for more than ~1 minute ("Error: app is in background
uid …"). **Explicit broadcasts to manifest-declared receivers are exempt from this
restriction** — the receiver's `onReceive()` runs briefly even in a fully stopped app.
That is enough to update SharedPreferences and trigger a notification update.

## Debugging

Logcat tag for all rg-counter events:

```sh
adb logcat -s RgCounter
```

Expected output on a hinge flip:

```
RgCounter: fired rgcounter.HINGE_OPEN; code=0x58 v=0
RgCounter: hinge OPEN — opens=3 total=6
```

To watch raw kernel events directly:

```sh
adb shell getevent -l /dev/input/event2
```

Expected output on close then open:

```
/dev/input/event2: EV_KEY       KEY_F12              DOWN
/dev/input/event2: EV_SYN       SYN_REPORT           00000000
/dev/input/event2: EV_KEY       KEY_F12              UP
/dev/input/event2: EV_SYN       SYN_REPORT           00000000
```
