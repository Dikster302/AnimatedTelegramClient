# Specialgram Android updates

Specialgram updates are installed over the existing app. Users must not uninstall the previous version.

## Compatibility requirements

- Keep `applicationId` equal to `com.dikster.specialgram`.
- Every distributed update must use the same release signing keystore and alias.
- Increase `versionCode` for every published APK.
- Do not distribute debug-signed APKs as public updates. Debug signing keys generated on hosted CI runners are not stable between runs.
- Android displays its normal install/update confirmation. The app cannot silently replace itself.

When package ID and signing certificate match and the new versionCode is higher, Android performs an in-place update and keeps app-private data.

## One-time release key setup

Generate one private keystore and keep a secure backup. Never commit it to Git:

```bash
keytool -genkeypair -v -keystore specialgram-release.jks -alias specialgram -keyalg RSA -keysize 4096 -validity 10000
```

Encode the keystore as a single-line Base64 string:

- Linux/macOS: `base64 -w 0 specialgram-release.jks` (on macOS, use `base64 < specialgram-release.jks | tr -d '\\n'`)
- Windows PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("specialgram-release.jks"))`

In GitHub repository Settings → Secrets and variables → Actions, add:

- `ANDROID_KEYSTORE_BASE64`: Base64 output of the keystore
- `ANDROID_KEYSTORE_PASSWORD`: keystore password
- `ANDROID_KEY_ALIAS`: `specialgram` (or the alias chosen above)
- `ANDROID_KEY_PASSWORD`: key password

Keep the keystore and passwords private and backed up. Losing this key prevents publishing compatible updates to existing installs.

## Build

The `Build Android APK` workflow is manual-only. Run it on `migration/android-java-native`. It builds a signed release APK and publishes it as the `Specialgram-update` artifact. Download the APK and distribute that exact file.

The first public release must also be signed with this same key. If an earlier APK was installed with a different debug/release key, Android will reject an in-place update; that one-time transition must be resolved before distributing updates to those users.
