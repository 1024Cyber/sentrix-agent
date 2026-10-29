# Sentrix Agent

Android agent APK for the Sentrix remote device control platform.

## Quick Start

### 1. Build the APK
Push this repo to GitHub — GitHub Actions builds the APK automatically.
Download it from **Actions → latest run → Artifacts → sentrix-debug-N**.

### 2. Deploy the Relay
```
cd sentrix-relay
# Push to Railway — set RELAY_API_KEY env var in Railway dashboard
```

### 3. Install on device
- Enable "Install from unknown sources"
- Install the APK
- Open Sentrix → enter your relay URL and copy your device token
- Toggle agent ON

### 4. Connect your dashboard
Your existing Vercel dashboard calls:
```
POST https://your-relay.railway.app/command
Headers: x-api-key: YOUR_RELAY_API_KEY
Body: { "token": "device-token", "action": "wifi_on", "params": {} }
```

---

## Project Structure

```
sentrix-agent/
├── .github/workflows/build.yml     ← GitHub Actions build
├── app/src/main/
│   ├── AndroidManifest.xml
│   └── java/com/sentrix/agent/
│       ├── SentrixApp.kt           ← Application class
│       ├── AgentService.kt         ← Foreground service (core)
│       ├── CommandRouter.kt        ← Action dispatcher
│       ├── commands/
│       │   ├── ConnectivityCommands.kt
│       │   ├── CameraCommands.kt
│       │   ├── LocationCommands.kt
│       │   ├── AudioCommands.kt
│       │   └── SystemCommands.kt
│       ├── network/
│       │   ├── WebSocketClient.kt  ← Auto-reconnect WS
│       │   └── Uploader.kt         ← Supabase file upload
│       ├── receiver/
│       │   └── BootReceiver.kt     ← Auto-start on boot
│       ├── ui/
│       │   ├── SetupActivity.kt    ← Config screen
│       │   └── PermissionActivity.kt
│       └── util/
│           ├── Prefs.kt            ← SharedPreferences
│           └── RootUtil.kt         ← Root detection + shell exec
```

## Release Signing

Add these GitHub Secrets for signed release builds:
- `KEYSTORE_BASE64` — base64 of your .jks file
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

Generate keystore:
```
keytool -genkey -v -keystore sentrix.jks -alias sentrix -keyalg RSA -keysize 2048 -validity 10000
base64 -w 0 sentrix.jks
```
