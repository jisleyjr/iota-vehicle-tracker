package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.csv.ExistingFillUp
import com.example.data.csv.FillUpCsvParseResult
import com.example.data.csv.FillUpCsvParser
import com.example.data.db.AppDatabase
import com.example.data.model.FillUp
import com.example.data.model.Maintenance
import com.example.data.model.MaintenanceType
import com.example.data.model.TireSet
import com.example.data.model.Vehicle
import com.example.ui.PendingCsvImport
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the app's Compose UI on a Pixel 8 and records PNGs under
 * app/src/test/screenshots. Copy those files to docs/screenshots after recording.
 * See docs/README.md.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class AppScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    Locale.setDefault(Locale.US)
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    runBlocking(Dispatchers.IO) { database().clearAllTables() }
  }

  @Test
  fun emptyGarage() {
    showApp()
    composeTestRule.waitUntil(TIMEOUT_MS) {
      hasText("Your Garage is Empty") || hasText("Prius")
    }
    if (hasText("Prius")) {
      composeTestRule.onNodeWithContentDescription("Vehicle options").performClick()
      composeTestRule.onNodeWithText("Delete vehicle").performClick()
      composeTestRule.onNodeWithText("Delete").performClick()
      composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Your Garage is Empty") }
    }
    captureRoot("empty-garage")
  }

  @Test
  fun garageFillUpsAndEfficiency() {
    seedSampleGarage()
    showApp()
    selectCamry()
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("11/06/2026") }
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("2,399") }
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("MILES") }
    captureRoot("garage-fill-ups")
  }

  @Test
  fun maintenanceLog() {
    seedSampleGarage()
    showApp()
    selectCamry()
    composeTestRule.onNodeWithTag("maintenance_tab").performClick()
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Brake pads") }
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Oil changed 2,399 miles ago") }
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("14,000 mi") }
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("1,399 mi") }
    composeTestRule.onNodeWithTag("maintenance_filter_OIL_CHANGE").performClick()
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("5W-30 synthetic") }
    check(!hasText("Front pads and rotors"))
    composeTestRule.onNodeWithTag("maintenance_filter_all").performClick()
    composeTestRule.onNodeWithTag("maintenance_list").performScrollToNode(
      androidx.compose.ui.test.hasText("Front pads and rotors")
    )
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Front pads and rotors") }
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Repair") }
    composeTestRule.onNodeWithTag("maintenance_list").performScrollToNode(
      androidx.compose.ui.test.hasText("Brake pads")
    )
    captureRoot("maintenance-log")
  }

  @Test
  fun noOilChangeLogged() {
    val garage = seedSampleGarage()
    showApp()
    selectCamry()
    composeTestRule.onNodeWithTag("vehicle_card_${garage.outbackId}").performClick()
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("No Fuel Logs") }
    composeTestRule.onNodeWithTag("maintenance_tab").performClick()
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("No oil change logged") }
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("No tire sets yet.") }
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("No Service Records") }
    captureRoot("no-oil-change")
  }

  @Test
  fun deleteMaintenanceConfirmation() {
    seedSampleGarage()
    showApp()
    selectCamry()
    composeTestRule.onNodeWithTag("maintenance_tab").performClick()
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Brake pads") }
    composeTestRule.onAllNodesWithText("Edit")[0].performClick()
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Edit Maintenance") }
    composeTestRule.onNodeWithTag("delete_maintenance_button").performClick()
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Delete service record?") }
    composeTestRule.waitUntil(TIMEOUT_MS) {
      hasText("Are you sure you want to delete this service record? This cannot be undone.")
    }
    captureDialog("delete-maintenance")
    composeTestRule.onNodeWithText("Cancel").performClick()
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Front pads and rotors") }
  }

  @Test
  fun editMaintenanceDialog() {
    seedSampleGarage()
    showApp()
    selectCamry()
    composeTestRule.onNodeWithTag("maintenance_tab").performClick()
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Brake pads") }
    composeTestRule.onAllNodesWithText("Edit")[0].performClick()
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Edit Maintenance") }
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("1.5") }
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Front pads and rotors") }
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Delete service record") }
    captureDialog("edit-maintenance")
  }

  @Test
  fun emptyFuelLogs() {
    val garage = seedSampleGarage()
    showApp()
    selectCamry()
    composeTestRule.onNodeWithTag("vehicle_card_${garage.outbackId}").performClick()
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("No Fuel Logs") }
    captureRoot("empty-fuel-logs")
  }

  @Test
  fun addVehicleDialog() {
    seedSampleGarage()
    showApp()
    selectCamry()
    composeTestRule.onNodeWithTag("vehicles_row").performScrollToNode(hasTestTag("add_vehicle_card"))
    composeTestRule.onNodeWithTag("add_vehicle_card").performClick()
    composeTestRule.onNodeWithTag("vehicle_make_input").performTextInput("Honda")
    composeTestRule.onNodeWithTag("vehicle_model_input").performTextInput("Civic")
    composeTestRule.onNodeWithTag("vehicle_year_input").performTextInput("2016")
    composeTestRule.onNodeWithTag("vehicle_license_input").performTextInput("4ABC123")
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Add Vehicle Details") }
    captureDialog("add-vehicle")
  }

  @Test
  fun editFillUpDialog() {
    seedSampleGarage()
    showApp()
    selectCamry()
    composeTestRule.onAllNodesWithText("Edit")[0].performClick()
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Edit Fill-up") }
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Date: 11/06/2026") }
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Delete fill-up") }
    captureDialog("edit-fill-up")
  }

  @Test
  fun deleteFillUpConfirmation() {
    seedSampleGarage()
    showApp()
    selectCamry()
    composeTestRule.onAllNodesWithText("Edit")[0].performClick()
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Edit Fill-up") }
    composeTestRule.onNodeWithTag("delete_fill_up_button").performClick()
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Delete Fill-up?") }
    composeTestRule.waitUntil(TIMEOUT_MS) {
      hasText("Are you sure you want to delete this fill-up? This cannot be undone.")
    }
    captureDialog("delete-fill-up-confirm")
  }

  @Test
  fun addFillUpDialog() {
    seedSampleGarage()
    showApp()
    selectCamry()
    composeTestRule.onNodeWithTag("add_log_fab").performClick()
    composeTestRule.onNodeWithTag("trip_miles_input").performTextInput("250")
    composeTestRule.onNodeWithTag("gallons_input").performTextInput("8.5")
    composeTestRule.onNodeWithTag("price_input").performTextInput("3.59")
    composeTestRule.onNodeWithTag("odo_input").performTextInput("86000")
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Log Gas Fill-Up") }
    captureDialog("add-fill-up")
  }

  @Test
  fun addMaintenanceDialog() {
    seedSampleGarage()
    showApp()
    selectCamry()
    composeTestRule.onNodeWithTag("maintenance_tab").performClick()
    composeTestRule.onNodeWithTag("add_log_fab").performClick()
    composeTestRule.onNodeWithTag("service_type_OIL_CHANGE").performClick()
    composeTestRule.onNodeWithTag("service_title_input").performScrollTo().performTextInput("Oil change")
    composeTestRule.onNodeWithTag("service_description_input").performScrollTo().performTextInput("5W-30 synthetic")
    composeTestRule.onNodeWithTag("service_cost_input").performScrollTo().performTextInput("72.40")
    composeTestRule.onNodeWithTag("service_odo_input").performScrollTo().performTextInput("86000")
    composeTestRule.onNodeWithTag("service_hours_input").performScrollTo().performTextInput("1.0")
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Log Maintenance") }
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Operating Hours (Optional)") }
    captureDialog("add-maintenance")
  }

  @Test
  fun addTireSwapDialog() {
    val garage = seedSampleGarage()
    showApp()
    selectCamry()
    composeTestRule.onNodeWithTag("maintenance_tab").performClick()
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Brake pads") }
    composeTestRule.onNodeWithTag("add_log_fab").performClick()
    composeTestRule.onNodeWithTag("service_type_TIRE_SWAP").performClick()
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Choose the set being mounted.") }
    composeTestRule.onNodeWithTag("tire_set_option_${garage.summerSetId}").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("service_title_input").performScrollTo().performTextInput("Mount summer tires")
    composeTestRule.onNodeWithTag("service_description_input").performScrollTo().performTextInput("After winter storage")
    composeTestRule.onNodeWithTag("service_hours_input").performScrollTo().performTextInput("0.5")
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Summer") }
    captureDialog("add-tire-swap")
  }

  @Test
  fun importCsvPreviewDialog() {
    val garage = seedSampleGarage()
    val csv = """
      trip,gallons,price,date
      188.5,11.25,${'$'}3.46,11/01/2026
      298,9,${'$'}3.71,10/22/2026
      188.5,10.5,${'$'}3.52,2026-11-01T16:00:00Z
      142.5,5.1,${'$'}3.65,not-a-date
    """.trimIndent()
    val existing = sampleFillUps(garage.camryId).map { ExistingFillUp(it.tripMiles, it.date) }
    val parsed = FillUpCsvParser.parse(csv, existing)
    check(parsed is FillUpCsvParseResult.Ok)
    assertEquals("1 new · 2 duplicates · 1 skipped", parsed.preview.statusLabel())
    composeTestRule.setContent {
      MyApplicationTheme(darkTheme = false) {
        VehicleTrackerApp(
          nowMillis = utcDate(2026, 11, 13),
          initialPendingImport = PendingCsvImport(
            vehicleId = garage.camryId,
            vehicleLabel = "2021 Toyota Camry",
            fileName = "camry_fillups.csv",
            preview = parsed.preview
          )
        )
      }
    }
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Import Fill-Ups (CSV)") }
    selectCamry()
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Import 1 Row") }
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Duplicate") }
    captureDialog("import-csv-preview")
  }

  @OptIn(com.github.takahirom.roborazzi.ExperimentalRoborazziApi::class)
  @Test
  fun csvMenu() {
    seedSampleGarage()
    showApp()
    selectCamry()
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("11/06/2026") }
    composeTestRule.onNodeWithTag("csv_menu_button").performClick()
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Export CSV") }
    composeTestRule.waitForIdle()
    captureScreenRoboImage(filePath = screenshotPath("csv-menu"))
  }

  @Test
  fun exportCsvSheet() {
    seedSampleGarage()
    showApp()
    selectCamry()
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("11/06/2026") }
    composeTestRule.onNodeWithTag("csv_menu_button").performClick()
    composeTestRule.onNodeWithTag("export_csv_menu_item").performClick()
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Export Fill-Ups (CSV)") }
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("toyota-camry-2021-fillups.csv") }
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("5 rows · headers: trip,gallons,price,date") }
    captureDialog("export-csv-sheet")
  }

  @Test
  fun expandedDashboard() {
    seedSampleGarage()
    showApp()
    selectCamry()
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("11/06/2026") }
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("2,399") }
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("MILES") }
    composeTestRule.onNodeWithTag("efficiency_toggle").performClick()
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("TOTAL EXPENDITURES") }
    composeTestRule.onNodeWithTag("stats_icon_button").assertIsDisplayed()
    captureRoot("expanded-dashboard")
  }

  @Test
  fun mpgHistoryChart() {
    seedSampleGarage()
    showApp()
    selectCamry()
    composeTestRule.onNodeWithTag("efficiency_toggle").performClick()
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("TOTAL EXPENDITURES") }
    composeTestRule.onNodeWithTag("stats_icon_button").performClick()
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("MPG History (Last 30)") }
    captureDialog("mpg-history")
  }

  private fun selectCamry() {
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Camry") }
    composeTestRule.onNodeWithText("Camry").performClick()
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("11/06/2026") }
  }

  private fun showApp() {
    composeTestRule.setContent {
      MyApplicationTheme(darkTheme = false) {
        VehicleTrackerApp(nowMillis = utcDate(2026, 11, 13))
      }
    }
  }

  private fun hasText(text: String, substring: Boolean = false): Boolean {
    return composeTestRule.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()
  }

  private fun captureRoot(name: String) {
    composeTestRule.waitForIdle()
    composeTestRule.onRoot().captureRoboImage(filePath = screenshotPath(name))
  }

  private fun captureDialog(name: String) {
    composeTestRule.waitForIdle()
    composeTestRule.onNode(isDialog()).captureRoboImage(filePath = screenshotPath(name))
  }

  private fun screenshotPath(name: String): String = "src/test/screenshots/$name.png"

  private fun seedSampleGarage(): SampleGarage = runBlocking(Dispatchers.IO) {
    val dao = database().vehicleDao()
    val outbackId = dao.insertVehicle(
      Vehicle(make = "Subaru", model = "Outback", year = 2018, licensePlate = "8FKR392")
    ).toInt()
    val camryId = dao.insertVehicle(
      Vehicle(make = "Toyota", model = "Camry", year = 2021, licensePlate = "CAM4219")
    ).toInt()
    val summerId = dao.insertTireSet(
      TireSet(
        vehicleId = camryId,
        name = "Summer",
        brand = "Michelin",
        size = "225/45R18",
        purchaseDate = utcDate(2025, 4, 2)
      )
    ).toInt()
    val winterId = dao.insertTireSet(
      TireSet(
        vehicleId = camryId,
        name = "Winter",
        brand = "Blizzak",
        size = "225/45R18",
        purchaseDate = utcDate(2025, 10, 1)
      )
    ).toInt()
    sampleFillUps(camryId).forEach { dao.insertFillUp(it) }
    sampleMaintenance(camryId, summerId, winterId).forEach { dao.insertMaintenance(it) }
    SampleGarage(outbackId, camryId, summerId, winterId)
  }

  private fun sampleFillUps(vehicleId: Int): List<FillUp> = listOf(
    FillUp(vehicleId = vehicleId, tripMiles = 312.0, gallons = 9.4, pricePerGallon = 3.49, odometer = 84210.0, date = utcDate(2026, 9, 2)),
    FillUp(vehicleId = vehicleId, tripMiles = 286.0, gallons = 8.7, pricePerGallon = 3.62, odometer = 84496.0, date = utcDate(2026, 9, 18)),
    FillUp(vehicleId = vehicleId, tripMiles = 340.0, gallons = 10.1, pricePerGallon = 3.55, odometer = 84836.0, date = utcDate(2026, 10, 5)),
    FillUp(vehicleId = vehicleId, tripMiles = 298.0, gallons = 9.0, pricePerGallon = 3.71, odometer = 85134.0, date = utcDate(2026, 10, 22)),
    FillUp(vehicleId = vehicleId, tripMiles = 265.0, gallons = 8.2, pricePerGallon = 3.48, odometer = 85399.0, date = utcDate(2026, 11, 6))
  )

  private fun sampleMaintenance(vehicleId: Int, summerId: Int, winterId: Int): List<Maintenance> = listOf(
    Maintenance(
      vehicleId = vehicleId,
      type = MaintenanceType.TIRE_SWAP,
      title = "Mounted summer tires",
      cost = 0.0,
      odometer = 70000.0,
      date = utcDate(2026, 4, 1),
      tireSetId = summerId
    ),
    Maintenance(
      vehicleId = vehicleId,
      type = MaintenanceType.OIL_CHANGE,
      title = "Oil change",
      description = "5W-30 synthetic",
      cost = 64.50,
      odometer = 83000.0,
      date = utcDate(2026, 7, 12)
    ),
    Maintenance(
      vehicleId = vehicleId,
      type = MaintenanceType.TIRE_SWAP,
      title = "Swapped to winter tires",
      cost = 0.0,
      odometer = 84000.0,
      date = utcDate(2026, 8, 15),
      tireSetId = winterId
    ),
    Maintenance(
      vehicleId = vehicleId,
      type = MaintenanceType.TIRE_ROTATION,
      title = "Tire rotation",
      cost = 40.0,
      odometer = 84500.0,
      date = utcDate(2026, 9, 20),
      tireSetId = winterId
    ),
    Maintenance(
      vehicleId = vehicleId,
      type = MaintenanceType.REPAIR,
      title = "Brake pads",
      description = "Front pads and rotors",
      cost = 285.0,
      odometer = 85100.0,
      date = utcDate(2026, 10, 28),
      hours = 1.5
    )
  )

  private fun utcDate(year: Int, month: Int, day: Int): Long {
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
      set(Calendar.YEAR, year)
      set(Calendar.MONTH, month - 1)
      set(Calendar.DAY_OF_MONTH, day)
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }.timeInMillis
  }

  private fun database(): AppDatabase {
    val context = ApplicationProvider.getApplicationContext<Context>()
    return AppDatabase.getDatabase(context)
  }

  private data class SampleGarage(
    val outbackId: Int,
    val camryId: Int,
    val summerSetId: Int,
    val winterSetId: Int
  )

  private companion object {
    const val TIMEOUT_MS = 15_000L
  }
}
