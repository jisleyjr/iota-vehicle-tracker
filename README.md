# IOTA Vehicle Tracker

A fully local Android app for tracking vehicles, gas fill-ups, fuel efficiency, expenses, and maintenance. Logs are stored on the device with Room. The UI is Jetpack Compose.

## Prerequisites

- [Android Studio](https://developer.android.com/studio) Panda 4 (2025.3.4) or newer, which supports Android Gradle Plugin 9.2.
- JDK 17 or newer. Android Gradle Plugin 9.2.1 requires JDK 17 to run the build. This project sets the Gradle daemon JVM to JDK 21 in `gradle/gradle-daemon-jvm.properties`. Use JDK 21 for `./gradlew` (`JAVA_HOME`). Android Studio can use its bundled JDK when that JDK is 17 or newer.
- Android SDK Platform 36.1. The app compiles against API 36.1, targets API 36, and supports devices back to API 24.

App bytecode compatibility is Java 11.

## Build and run

### Android Studio

1. Open this directory.
2. Let Gradle sync, and install any SDK components Android Studio asks for.
3. Run the `app` configuration on an emulator or a device.

### Command line

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

`assembleDebug` produces `app/build/outputs/apk/debug/app-debug.apk`. The debug build type uses `signingConfigs.getByName("debug")`, the standard Android debug keystore (`~/.android/debug.keystore`). The Android Gradle Plugin creates that keystore if it is not already there.

`testDebugUnitTest` runs the local unit tests on the host, including Robolectric tests and Roborazzi screenshot comparisons. No emulator is required. `./gradlew test` runs the same unit-test work when that aggregate task is available.

## Release signing

Release builds use the `release` signing config in `app/build.gradle.kts`. The key alias defaults to `upload`. `*.jks` and `*.keystore` files are gitignored. Do not commit the keystore.

### Create the upload keystore once

`keytool` prompts for a keystore password, a key password, and a name. JDK 21 writes a PKCS12 keystore, which has one password: enter the same password both times. Put that password in `STORE_PASSWORD` and in `KEY_PASSWORD`. A different key password is ignored for PKCS12, and the release build then fails when it tries to unlock the key.

```bash
mkdir -p ~/keys && keytool -genkeypair -v -keystore ~/keys/iota-upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

Back up `~/keys/iota-upload.jks` and the password outside this repository. Updates only install over an existing copy when they are signed with this same key. If the file or the password is lost, phones that already have the app cannot take an in-place update.

### Build a signed APK locally

| Variable | Purpose |
| --- | --- |
| `KEYSTORE_PATH` | Path to the upload keystore. If unset, the build uses `my-upload-key.jks` in the project root. |
| `STORE_PASSWORD` | Keystore password. |
| `KEY_PASSWORD` | Key password. |
| `KEY_ALIAS` | Key alias. If unset, the build uses `upload`. |
| `VERSION_NAME` | Version name shown on the release. If unset, the build uses `1.0`. |
| `VERSION_CODE` | Positive integer Android uses to decide whether an APK is an update. If unset, the build uses `1`. |

```bash
export KEYSTORE_PATH="$HOME/keys/iota-upload.jks"
export STORE_PASSWORD=your-store-password
export KEY_PASSWORD=your-key-password
./gradlew assembleRelease
```

The APK is `app/build/outputs/apk/release/app-release.apk`.

### Add the GitHub Actions secrets

The [Release workflow](.github/workflows/release.yml) reads four repository secrets. In GitHub, open **Settings → Secrets and variables → Actions** and add:

| Secret | Value |
| --- | --- |
| `KEYSTORE_BASE64` | Base64 text of `~/keys/iota-upload.jks`. |
| `STORE_PASSWORD` | Keystore password from `keytool`. |
| `KEY_PASSWORD` | Key password from `keytool`. |
| `KEY_ALIAS` | `upload` |

On macOS, copy the keystore into the clipboard, then paste it as `KEYSTORE_BASE64`:

```bash
base64 -i ~/keys/iota-upload.jks | pbcopy
```

On Linux, print one line and copy that into `KEYSTORE_BASE64`:

```bash
base64 -w 0 ~/keys/iota-upload.jks
```

The workflow rejects a run that is missing any of these secrets. It decodes `KEYSTORE_BASE64` to a temp file on the runner and points `KEYSTORE_PATH` at that file. The keystore is not stored in the repository.

### Cut a release

Tag the commit you want to ship and push the tag:

```bash
git tag v1.0.0 && git push origin v1.0.0
```

Pushing a tag that matches `v*` starts the Release workflow. You can also run it by hand: open **Actions → Release → Run workflow** and enter an existing tag such as `v1.0.0`. The workflow checks out that tag, builds a signed release APK, and publishes a GitHub Release with the APK attached and auto-generated release notes.

The published file is named with the tag, for example `iota-vehicle-tracker-v1.0.0.apk`. `versionName` is the tag without the leading `v` (`1.0.0` for `v1.0.0`). `versionCode` is the GitHub Actions run number, so each later run installs over an older release signed with the same keystore. Local builds stay on version name `1.0` and version code `1` unless you set `VERSION_NAME` and `VERSION_CODE`.

### Install the APK on a phone

1. Open the repository **Releases** page after the workflow finishes.
2. Download `iota-vehicle-tracker-<tag>.apk`.
3. Open the file on the phone and confirm the install. Android may ask you to allow installs from the browser or the Files app.
4. To update, download a newer release and open it the same way. Android replaces the installed app when the APK is signed with the same upload key and its version code is higher.

## Screenshots

Captured from the Compose UI with Roborazzi. Descriptions and the regenerate command are in [docs/README.md](docs/README.md).

| Empty garage | Garage, efficiency, and fill-ups |
| --- | --- |
| <img src="docs/screenshots/empty-garage.png" alt="Empty garage" width="240" /> | <img src="docs/screenshots/garage-fill-ups.png" alt="Garage, fuel efficiency, and fill-ups" width="240" /> |

| Maintenance log | Vehicle with no fuel logs |
| --- | --- |
| <img src="docs/screenshots/maintenance-log.png" alt="Maintenance log" width="240" /> | <img src="docs/screenshots/empty-fuel-logs.png" alt="Vehicle with no fuel logs" width="240" /> |

| Add vehicle | Log a fill-up |
| --- | --- |
| <img src="docs/screenshots/add-vehicle.png" alt="Add vehicle dialog" width="240" /> | <img src="docs/screenshots/add-fill-up.png" alt="Add fill-up dialog" width="240" /> |

| Log maintenance | Fuel-efficiency history |
| --- | --- |
| <img src="docs/screenshots/add-maintenance.png" alt="Add maintenance dialog" width="240" /> | <img src="docs/screenshots/mpg-history.png" alt="MPG history chart" width="240" /> |
