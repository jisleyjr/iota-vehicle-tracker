# Screenshots

These images are rendered from the app's Compose UI by Roborazzi and Robolectric (`app/src/test/java/com/example/AppScreenshotTest.kt`) on a Pixel 8 configuration. The copies in this folder are what the project README displays. The same PNGs under `app/src/test/screenshots/` are the comparison goldens.

The populated screens use two vehicles stored through Room: a 2021 Toyota Camry (`CAM4219`) with fill-ups and service records, and a 2018 Subaru Outback (`8FKR392`) with none. The efficiency dashboard starts collapsed, showing average MPG and a Last Oil Change tile (miles since the last oil change, or an em dash when none is logged). The MPG chart button is inside the expanded section. Each fuel log and each service record has an Edit button that opens the saved row in its form. Delete on that form asks for confirmation before removing the row. The Fuel Logs header has a CSV menu for import and export. Service Logs shows a type filter, a type badge on each record, tire-set miles, and the oil-change indicator. Dialog screenshots type into the real form fields, except the edit forms, which are prefilled. The date on a new fill-up or maintenance form is 11/13/2026 because the screenshot test passes that timestamp; the app defaults to the current time. An edited record keeps the date that was saved.

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
| `garage-fill-ups.png` | Both vehicles, the Camry efficiency row collapsed to average MPG and "2,399 miles" since the last oil change, and the gas fill-up list. Each fill-up has an Edit button. The Fuel Logs header has a CSV menu. |
| `expanded-dashboard.png` | The same Camry screen with efficiency expanded. The MPG chart button is in that area, and the Last Oil Change tile stays in the top row. Fill-up rows have an Edit button. |
| `maintenance-log.png` | The Camry Service Logs tab: type chips, type badges, Summer and Winter miles, "Oil changed 2,399 miles ago", and an Edit button on each record. |
| `empty-fuel-logs.png` | The Outback selected, with a collapsed efficiency row (Last Oil Change shows an em dash) and the "No Fuel Logs" state. |
| `add-vehicle.png` | The Add Vehicle Details dialog, filled with a 2016 Honda Civic. |
| `add-fill-up.png` | The Log Gas Fill-Up dialog, filled with trip miles, gallons, price, and odometer. |
| `edit-fill-up.png` | The Edit Fill-up dialog for the Camry log on 11/06/2026, prefilled, plus Delete fill-up. |
| `delete-fill-up-confirm.png` | The confirmation dialog asking before that fill-up is deleted. |
| `add-maintenance.png` | The Log Maintenance dialog for an oil change, with a type picker, title, description, cost, odometer, and operating hours. |
| `edit-maintenance.png` | The Edit Maintenance form for brake pads, prefilled as a Repair, with Delete service record. |
| `delete-maintenance.png` | The confirmation dialog asking before that service record is deleted. |
| `add-tire-swap.png` | A tire-swap entry with Summer selected, Winter marked as the mounted set, a description, and operating hours. |
| `no-oil-change.png` | The Outback Service Logs tab: "No oil change logged", no tire sets, and no service records. |
| `mpg-history.png` | The MPG History chart for the Camry's fill-ups. |
| `import-csv-preview.png` | The import preview for `camry_fillups.csv`: new rows, duplicates of saved and earlier rows, and a row whose date could not be parsed. |
| `csv-menu.png` | The Fuel Logs `CSV` menu open on the Camry, with Import CSV and Export CSV. |
| `export-csv-sheet.png` | The export sheet for `toyota-camry-2021-fillups.csv`, with Save to Device and Share CSV. |
