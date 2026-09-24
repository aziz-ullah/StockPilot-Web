# 🚀 StockPilot Native Android - Production & Play Store Deployment Guide

This document provides complete, step-by-step instructions to test, build, sign, and publish the **StockPilot Enterprise** native Android application to the **Google Play Store**.

---

## 📁 1. Project Architecture Overview

The native Android app is located in the isolated `./android` directory:

```
android/
├── app/
│   ├── build.gradle.kts                # App-level dependencies & signing configs
│   ├── proguard-rules.pro              # ProGuard / R8 code shrinking rules
│   └── src/main/
│       ├── AndroidManifest.xml         # App permissions & FileProvider setup
│       ├── java/com/stockpilot/app/
│       │   ├── StockPilotApp.kt        # Application class
│       │   ├── data/
│       │   │   ├── api/                # Retrofit & OkHttp Network Interceptors
│       │   │   ├── local/              # EncryptedSharedPreferences TokenManager
│       │   │   ├── models/             # Kotlin Data Models matching FastAPI schemas
│       │   │   └── repository/         # Repository layer with Result<T> error handling
│       │   └── ui/
│       │       ├── MainActivity.kt     # Jetpack Compose Navigation & Drawer
│       │       ├── theme/              # Material Design 3 Colors & Typography
│       │       └── screens/            # Jetpack Compose ViewModels & Screens
│       │           ├── login/
│       │           ├── dashboard/
│       │           ├── inventory/
│       │           ├── pos/
│       │           ├── purchases/
│       │           ├── expenses/
│       │           ├── reports/
│       │           └── users/
│       └── res/                        # Drawables, Strings, Network Config, FileProvider XML
├── build.gradle.kts                    # Root build configuration
├── gradle.properties                   # JVM heap and AndroidX flags
└── settings.gradle.kts                 # Project repositories & settings
```

---

## 🌐 2. Backend Server Connection Setup

The Android app includes dynamic Base URL switching built directly into the Login screen.

- **Local Android Emulator**: Use `http://10.0.2.2:8000/api/v1/`
- **Physical Phone on Same Wi-Fi**: Use `http://<YOUR_COMPUTER_LOCAL_IP>:8000/api/v1/`
- **Production Server / Remote Tunnel**: Use `https://<YOUR_DOMAIN_OR_TUNNEL>.trycloudflare.com/api/v1/`

---

## 🔑 3. Generating Production Upload Keystore

Before building a release binary (.aab or .apk), generate a secure release signing keystore using Keytool:

```bash
keytool -genkey -v -keystore stockpilot-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias stockpilot-key
```

> ⚠️ **Important**: Store `stockpilot-release-key.jks` in a secure location and backup the keystore password.

---

## 🔒 4. Configuring Release Signing in `app/build.gradle.kts`

Set system environment variables (or update `app/build.gradle.kts`):

```bash
set KEYSTORE_FILE=C:\path\to\stockpilot-release-key.jks
set KEYSTORE_PASSWORD=YourKeystorePassword
set KEY_ALIAS=stockpilot-key
set KEY_PASSWORD=YourKeyPassword
```

In `android/app/build.gradle.kts`:

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_FILE") ?: "stockpilot-release-key.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "password"
            keyAlias = System.getenv("KEY_ALIAS") ?: "stockpilot-key"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "password"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

---

## 📦 5. Building Production Android App Bundle (.aab)

Open a terminal inside the `./android` folder and execute:

```bash
# Clean project
./gradlew clean

# Build signed Android App Bundle (.aab) for Google Play Console
./gradlew bundleRelease

# Or build standalone APK for direct APK distribution / testing:
./gradlew assembleRelease
```

The generated release bundle will be located at:
- **App Bundle (.aab)**: `./android/app/build/outputs/bundle/release/app-release.aab`
- **Standalone APK**: `./android/app/build/outputs/apk/release/app-release.apk`

---

## 🚀 6. Step-by-Step Google Play Console Publishing

1. **Log in to Google Play Console**:
   Visit [play.google.com/console](https://play.google.com/console) and select your Developer Account.
2. **Create Application**:
   - App Name: `StockPilot Enterprise POS`
   - Default Language: `English (United States)`
   - App or Game: `App`
   - Free or Paid: `Free`
3. **App Content Declaration**:
   - Privacy Policy URL (e.g. `https://yourdomain.com/privacy`)
   - Data Safety Declaration (Declare JWT authentication & HTTPS network usage)
   - Store Listing Details (Short & Full Description, App Screenshots, 512x512 Icon, 1024x500 Feature Graphic)
4. **Internal / Closed Testing**:
   - Navigate to `Testing` -> `Internal testing` or `Closed testing`
   - Create new release and upload `app-release.aab`
   - Add tester emails for pre-release verification
5. **Production Rollout**:
   - Once testing is confirmed stable, promote the release to `Production`
   - Submit for Google Play Store review!
