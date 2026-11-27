# AnimatedTelegramClient

Android client UI scaffold in Kotlin + Jetpack Compose, styled after Telegram.

**Important status:** this archive is a UI/build scaffold, not a finished Telegram client. The current screen uses demo chats. TDLib JNI, phone authorization, real chat loading, typing actions and progressive `EditMessageText` sending are not implemented yet. Do not install it expecting a working messenger.

## Build
Open this folder in Android Studio (Hedgehog or newer), sync Gradle, then build `app` → `assembleDebug`.

Telegram `api_id` and `api_hash` must never be committed. They are read from Gradle properties or environment variables named `TELEGRAM_API_ID` and `TELEGRAM_API_HASH`; they are not required for this UI-only scaffold.

## TDLib
The official TDLib Android build uses the upstream scripts in `tdlib/td/example/android`: `check-environment.sh`, `fetch-sdk.sh`, `build-openssl.sh`, and `build-tdlib.sh`. Its documentation describes the resulting `tdlib/libs` and `tdlib/java` outputs. See [official Android build instructions](https://github.com/tdlib/td/blob/master/example/android/README.md).
