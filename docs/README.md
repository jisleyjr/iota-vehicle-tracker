# Screenshots

These images are rendered from the app's Compose UI by Roborazzi and Robolectric (`app/src/test/java/com/example/AppScreenshotTest.kt`) on a Pixel 8 configuration. The copies in this folder are what the project README displays. The same PNGs under `app/src/test/screenshots/` are the comparison goldens.

The populated screens use two vehicles stored through Room: a 2021 Toyota Camry (`CAM4219`) with fill-ups and service records, and a 2018 Subaru Outback (`8FKR392`) with none. Dialog screenshots type into the real form fields. The date on the fill-up and maintenance forms is 11/13/2026 because the screenshot test passes that timestamp; the app defaults to the current time.

## Regenerate

```bash
./gradlew :app:recordRoborazziDebug
cp app/src/test/screenshots/*.png docs/screenshots/
```

`recordRoborazziDebug` runs the screenshot tests in record mode and overwrites `app/src/test/screenshots/`. Copy those PNGs here afterward. `./gradlew testDebugUnitTest` compares the tests against `app/src/test/screenshots/` and does not update the images.

## Images

| File | What it shows |
| --- | --- |
| `empty-garage.png` | Garage with no vehicles: the add-vehicle card and the empty-state prompt. |
| `garage-fill-ups.png` | Both vehicles, the Camry efficiency dashboard (MPG, fuel cost, service cost, mileage), and the gas fill-up list. The Fuel Logs header has a CSV menu. |
| `maintenance-log.png` | The same Camry dashboard on the Service Logs tab, with brake pads, tire rotation, and an oil change. |
| `empty-fuel-logs.png` | The Outback selected, with an empty efficiency summary and the "No Fuel Logs" state. |
| `add-vehicle.png` | The Add Vehicle Details dialog, filled with a 2016 Honda Civic. |
| `add-fill-up.png` | The Log Gas Fill-Up dialog, filled with trip miles, gallons, price, and odometer. |
| `add-maintenance.png` | The Log Maintenance dialog, filled with an oil change, cost, odometer, and hours. |
| `mpg-history.png` | The MPG History chart for the Camry's fill-ups. |
| `import-csv-preview.png` | The import preview for `camry_fillups.csv`: new rows, duplicates of saved and earlier rows, and a row whose date could not be parsed. |
