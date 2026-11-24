# Screenshots

These images are rendered from the app's Compose UI by Roborazzi and Robolectric (`app/src/test/java/com/example/AppScreenshotTest.kt`) on a Pixel 8 configuration. The copies in this folder are what the project README displays. The same PNGs under `app/src/test/screenshots/` are the comparison goldens.

The populated screens use two vehicles stored through Room: a 2021 Toyota Camry (`CAM4219`) with fill-ups and service records, and a 2018 Subaru Outback (`8FKR392`) with none. The efficiency dashboard starts collapsed, showing only average MPG; `expanded-dashboard.png` is the same Camry screen after tapping that row. The selected garage card shows a checkmark and an overflow menu. Unselected cards keep a trash icon. Each fuel log has an Edit button that opens the saved fill-up in the form. Delete fill-up on that form asks for confirmation before removing the row. The Fuel Logs header has a CSV menu for import and export. Dialog screenshots type into the real form fields, except the edit form, which is prefilled from the 11/06/2026 Camry log. The date on the new fill-up and maintenance forms is 11/13/2026 because the screenshot test passes that timestamp; the app defaults to the current time.

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
| `garage-fill-ups.png` | Both vehicles, the Camry efficiency row collapsed to average MPG, and the gas fill-up list. Each fill-up has an Edit button. The Fuel Logs header has a CSV menu. |
| `expanded-dashboard.png` | The same Camry screen with efficiency expanded to expenditures, mileage, gas costs, and service repairs. Fill-up rows have an Edit button. The Fuel Logs header has a CSV menu. |
| `maintenance-log.png` | The Camry screen on the Service Logs tab, efficiency collapsed, with brake pads, tire rotation, and an oil change. |
| `empty-fuel-logs.png` | The Outback selected, with a collapsed efficiency row and the "No Fuel Logs" state. The Fuel Logs header has a CSV menu. |
| `add-vehicle.png` | The Add Vehicle Details dialog, filled with a 2016 Honda Civic. |
| `add-fill-up.png` | The Log Gas Fill-Up dialog, filled with trip miles, gallons, price, and odometer. |
| `edit-fill-up.png` | The Edit Fill-up dialog for the Camry log on 11/06/2026, prefilled with trip miles, gallons, price, and odometer, plus Delete fill-up. |
| `delete-fill-up-confirm.png` | The confirmation dialog asking before that fill-up is deleted. Cancel returns to the edit form. |
| `add-maintenance.png` | The Log Maintenance dialog, filled with an oil change, cost, odometer, and hours. |
| `mpg-history.png` | The MPG History chart for the Camry's fill-ups. |
| `import-csv-preview.png` | The import preview for `camry_fillups.csv`: new rows, duplicates of saved and earlier rows, and a row whose date could not be parsed. |
| `csv-menu.png` | The Fuel Logs `CSV` menu open on the Camry, with Import CSV and Export CSV. |
| `export-csv-sheet.png` | The export sheet for `toyota-camry-2021-fillups.csv`, with Save to Device and Share CSV. |
