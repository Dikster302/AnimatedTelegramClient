# Build notes

1. Install Android Studio and Android SDK Platform 35.
2. Open the project root (`settings.gradle.kts`) in Android Studio.
3. Allow Gradle sync to finish.
4. Select Build → Build Bundle(s) / APK(s) → Build APK(s).
5. The debug APK is normally at `app/build/outputs/apk/debug/app-debug.apk`.

This initial scaffold is only a Compose UI demonstration. It does not yet connect to Telegram servers. A working client requires the official TDLib Java/JNI artifacts for Android and implementation of TDLib authorization/update handling. The upstream TDLib Android instructions are linked in the README.
