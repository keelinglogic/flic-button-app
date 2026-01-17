# Flic Bridge

Android app that bridges Flic button presses to broadcast intents, enabling Flic buttons to work with PTT apps like Zello.

## What This Does

Flic buttons use a proprietary Bluetooth protocol and can't natively act as system-level hardware buttons. This app:

1. Connects to your Flic buttons using the official Flic SDK
2. Listens for raw button down/up events
3. Broadcasts Android intents that PTT apps can receive

This turns your Flic button into a true PTT button for apps like Zello, ESChat, etc.

## Setup

### Prerequisites

- Android 8.0 (API 26) or higher
- Flic 2 button(s)
- Zello or other PTT app installed

### Installation

1. Build the APK:
   ```bash
   ./gradlew assembleDebug
   ```

2. Install on your device:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

3. Or open in Android Studio and run directly.

### Pairing a Flic Button

1. Hold your Flic button for 8 seconds until it flashes
2. Open Flic Bridge app
3. Tap "Scan for Buttons"
4. The button will be paired and appear in the list

### Configuration

1. Tap the settings icon
2. Configure the intents to broadcast:
   - **Button Down Intent**: Sent when button is pressed
   - **Button Up Intent**: Sent when button is released

Default intents are for Zello:
- Down: `com.zello.ptt.down`
- Up: `com.zello.ptt.up`

### Other PTT Apps

| App | Down Intent | Up Intent |
|-----|-------------|-----------|
| Zello | `com.zello.ptt.down` | `com.zello.ptt.up` |
| ESChat | `com.speakez.pttbutton.pressed` | `com.speakez.pttbutton.released` |

## How It Works

```
Flic Button (BLE)
      │
      ▼
Flic SDK (onButtonUpOrDown callback)
      │
      ▼
FlicBridgeService (background foreground service)
      │
      ▼
sendBroadcast(Intent("com.zello.ptt.down"))
      │
      ▼
Zello (receives broadcast, activates PTT)
```

The key is the `onButtonUpOrDown` callback in the Flic SDK, which provides raw press/release events rather than click gestures.

## Permissions

- **Bluetooth**: Connect to Flic buttons
- **Location**: Required for BLE scanning on Android 6-11
- **Foreground Service**: Keep running in background
- **Boot Completed**: Auto-start on device boot

## Building

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires signing config)
./gradlew assembleRelease
```

## License

MIT License - do what you want with it.

## Credits

- [Flic SDK for Android](https://github.com/50ButtonsEach/flic2lib-android)
- [Zello PTT Integration](https://zello.com/developers/)
