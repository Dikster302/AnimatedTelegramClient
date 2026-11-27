# AnimatedTelegramClient
Kotlin/Compose Android Telegram client using the official TDLib Java/JNI API. Implements phone, code and cloud-password authorization, chat list/history, sending and progressive EditMessageText updates.
GitHub Actions builds official TDLib from pinned commit c8828d50f5eac2fc7089ce1089d7ce9095c61b5e and packages ARM64 JNI.
Configure repository Actions secrets TELEGRAM_API_ID and TELEGRAM_API_HASH using credentials from https://my.telegram.org. Never commit secret values.
Run Actions → Build Android APK → Run workflow. Download AnimatedTelegramClient-debug from the completed run's Artifacts. Telegram server rate limits may prevent every intermediate edit from appearing.
