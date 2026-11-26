# Repository Guidelines

## Project Structure

The Android app lives in the single `app` module. Start at `app/src/main/java/com/example/MainActivity.kt` for the Compose entry point. Within that package, `ui/` holds screens, dialogs, theme code, and the vehicle view model. Under `data/`, use `db/` for Room queries and migrations, `model/` for entities, `maintenance/` for mileage calculations, and `csv/` for fill-up import and export. Android resources live in `app/src/main/res/`. When changing the database schema, review the migration and the exported schema in `app/schemas/` together.

## Build, Run, and Check

Use JDK 21 for the Gradle wrapper and install Android SDK Platform 36.1. Run commands from the repository root.

- `./gradlew assembleDebug` builds the debug APK.
- `./gradlew lintDebug` runs Android Lint on the debug variant.
- `./gradlew testDebugUnitTest` runs host tests, including screenshot comparisons.
- `./gradlew connectedDebugAndroidTest` runs instrumentation tests on a connected device or emulator.

To run locally, open the repository in Android Studio, sync Gradle, select an API 24 or newer device, and run the app configuration.

## Kotlin Style

Follow the configured official Kotlin style. Use four spaces in Kotlin source and match the surrounding indentation in Gradle scripts. Use PascalCase for types and UI composables, and lowerCamelCase for properties and ordinary functions.

## Testing and Screenshots

Put JUnit tests under `app/src/test/java/`. Name test classes for the behavior they cover, such as `FillUpCsvParserTest`. Use Robolectric for host tests that need Android APIs. Device tests belong under `app/src/androidTest/java/`.

For intentional visual changes, run `./gradlew :app:recordRoborazziDebug`, inspect the changed PNGs in `app/src/test/screenshots/`, and copy the updated images to `docs/screenshots/` for the README.

## Commits and Pull Requests

Recent commits use short imperative subjects such as “Move vehicle delete into the garage card menu.” In the pull request, describe the behavior change, link the relevant issue when available, and list the checks you ran. Include a screenshot when the change affects a visible screen.
