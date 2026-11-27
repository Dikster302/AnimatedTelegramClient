# Build instructions
1. Add Actions secrets TELEGRAM_API_ID and TELEGRAM_API_HASH.
2. Open Actions → Build Android APK → Run workflow.
3. Wait for native TDLib and Gradle builds.
4. Download AnimatedTelegramClient-debug from Artifacts, extract app-debug.apk, and install on an ARM64 Android device.
The workflow builds a pinned official TDLib revision using upstream Android scripts.
