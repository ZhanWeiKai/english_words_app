# English Word App - Build Instructions

## Prerequisites
- Android Studio Hedgehog or later
- JDK 17
- Android SDK API 34

## Build Commands

### Clean Build
```bash
cd android-word-app
./gradlew clean
```

### Build Debug APK
```bash
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### Build Release APK
```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

### Install on Connected Device
```bash
./gradlew installDebug
```

## Project Structure
- **Kotlin** with Jetpack Compose UI
- **MVVM Architecture** with Navigation Component
- **Retrofit2** for API calls
- **DataStore** for token storage
- **Material 3** design system

## API Configuration
Backend URL: `http://47.83.126.42:8885/api/`

## Features
1. ✅ User authentication (Login/Register)
2. ✅ Word Vault with filtering
3. ✅ AI Chat interface
4. ✅ Scene Practice
5. ✅ Training Summary
6. ✅ Bottom navigation
7. ✅ Material Design 3 theme

## Notes
- Minimum SDK: 24 (Android 7.0)
- Target SDK: 34 (Android 14)
- Language: Kotlin
- UI Framework: Jetpack Compose
