# Opengur GitHub Instructions

## Project overview

- Android app built with Gradle (legacy Android plugin style).
- Main code package: `com.kenny.openimgur`.
- App id: `com.kennyc.open.imgur`.
- Language: Java.
- Min SDK: 17.
- Target SDK: 28.
- Compile SDK: 28.
- Java version: 1.7 source and target.

## Build setup

- Debug build command:
  - `$env:JAVA_HOME="C:\Program Files\Java\jdk1.8.0_481"; .\gradlew.bat --no-daemon assembleDebug`
- Release build command (signed with debug keystore, directly installable):
  - `$env:JAVA_HOME="C:\Program Files\Java\jdk1.8.0_481"; .\gradlew.bat --no-daemon assembleRelease`
- Local environment expects JDK 8.
- Example local setup used in this repo:
  - `JAVA_HOME=C:\Program Files\Java\jdk1.8.0_481`
- No product flavors — single variant only.
- Both debug and release are signed with the debug keystore (`~/.android/debug.keystore`).
- Output APKs:
  - `app/build/outputs/apk/debug/app-debug.apk`
  - `app/build/outputs/apk/release/app-release.apk`

## Network and architecture notes

- API layer uses Retrofit 2 + OkHttp 3.
- OAuth auth is handled through `OAuthInterceptor`.
- Image loading uses Universal Image Loader.
- Video caching/downloading is handled by `VideoCache`.

## Coding rules for this repository

- Keep code simple.
- Keep code minimal.
- No comments in code.
- Avoid overengineering.
- Prefer small, targeted changes.
- Do not refactor unrelated files.
- Preserve existing behavior unless explicitly requested.

## Change safety rules

- Always keep builds passing.
- Prefer backward-compatible changes.
- Respect existing package names and app id.
- Do not introduce new frameworks unless necessary.

Use your tools to be as efficient as possible, to catch every findable string and replace everything you need to replace in order to finalize changes.

# Opengur GitHub Instructions

## Project overview

- Android app built with Gradle (legacy Android plugin style).
- Main code package: `com.kenny.openimgur`.
- App id: `com.kennyc.open.imgur`.
- Language: Java.
- Min SDK: 17.
- Target SDK: 28.
- Compile SDK: 28.
- Java version: 1.7 source and target.

## Build setup

- Debug build command:
  - `$env:JAVA_HOME="C:\Program Files\Java\jdk1.8.0_481"; .\gradlew.bat --no-daemon assembleDebug`
- Release build command (signed with debug keystore, directly installable):
  - `$env:JAVA_HOME="C:\Program Files\Java\jdk1.8.0_481"; .\gradlew.bat --no-daemon assembleRelease`
- Local environment expects JDK 8.
- Example local setup used in this repo:
  - `JAVA_HOME=C:\Program Files\Java\jdk1.8.0_481`
- No product flavors — single variant only.
- Both debug and release are signed with the debug keystore (`~/.android/debug.keystore`).
- Output APKs:
  - `app/build/outputs/apk/debug/app-debug.apk`
  - `app/build/outputs/apk/release/app-release.apk`

## Network and architecture notes

- API layer uses Retrofit 2 + OkHttp 3.
- OAuth auth is handled through `OAuthInterceptor`.
- Image loading uses Universal Image Loader.
- Video caching/downloading is handled by `VideoCache`.

## Coding rules for this repository

- Keep code simple.
- Keep code minimal.
- No comments in code.
- Avoid overengineering.
- Prefer small, targeted changes.
- Do not refactor unrelated files.
- Preserve existing behavior unless explicitly requested.

## Change safety rules

- Always keep builds passing.
- Prefer backward-compatible changes.
- Respect existing package names and app id.
- Do not introduce new frameworks unless necessary.

Use your tools to be as efficient as possible, to catch every findable string and replace everything you need to replace in order to finalize changes.
