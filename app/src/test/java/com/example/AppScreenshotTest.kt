package com.example

import android.content.Context
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.FillUp
import com.example.data.model.Maintenance
import com.example.data.model.Vehicle
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
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
      composeTestRule.onNodeWithTag("delete_vehicle_dashboard_button").performClick()
      composeTestRule.onNodeWithText("Delete").performClick()
      composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Your Garage is Empty") }
    }
    captureRoot("empty-garage")
  }

  @Test
  fun garageFillUpsAndEfficiency() {
    seedSampleGarage()
    showApp()
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("2021 Toyota Camry Dashboard") }
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("11/06/2026") }
    captureRoot("garage-fill-ups")
  }

  @Test
  fun maintenanceLog() {
    seedSampleGarage()
    showApp()
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("2021 Toyota Camry Dashboard") }
    composeTestRule.onNodeWithTag("maintenance_tab").performClick()
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Brake pads") }
    captureRoot("maintenance-log")
  }

  @Test
  fun emptyFuelLogs() {
    val garage = seedSampleGarage()
    showApp()
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("2021 Toyota Camry Dashboard") }
    composeTestRule.onNodeWithTag("vehicle_card_${garage.outbackId}").performClick()
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("No Fuel Logs") }
    captureRoot("empty-fuel-logs")
  }

  @Test
  fun addVehicleDialog() {
    seedSampleGarage()
    showApp()
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("2021 Toyota Camry Dashboard") }
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
  fun addFillUpDialog() {
    seedSampleGarage()
    showApp()
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("2021 Toyota Camry Dashboard") }
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
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("2021 Toyota Camry Dashboard") }
    composeTestRule.onNodeWithTag("maintenance_tab").performClick()
    composeTestRule.onNodeWithTag("add_log_fab").performClick()
    composeTestRule.onNodeWithTag("service_desc_input").performTextInput("Oil change")
    composeTestRule.onNodeWithTag("service_cost_input").performTextInput("72.40")
    composeTestRule.onNodeWithTag("service_odo_input").performTextInput("86000")
    composeTestRule.onNodeWithTag("service_hours_input").performTextInput("1.0")
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Log Maintenance") }
    captureDialog("add-maintenance")
  }

  @Test
  fun mpgHistoryChart() {
    seedSampleGarage()
    showApp()
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("2021 Toyota Camry Dashboard") }
    composeTestRule.onNodeWithTag("stats_icon_button").performClick()
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("MPG History (Last 30)") }
    captureDialog("mpg-history")
  }

  private fun showApp() {
    composeTestRule.setContent {
      MyApplicationTheme(darkTheme = false) {
        VehicleTrackerApp(nowMillis = utcDate(2026, 11, 13))
      }
    }
  }

  private fun hasText(text: String): Boolean {
    return composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
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
    sampleFillUps(camryId).forEach { dao.insertFillUp(it) }
    sampleMaintenance(camryId).forEach { dao.insertMaintenance(it) }
    SampleGarage(outbackId, camryId)
  }

  private fun sampleFillUps(vehicleId: Int): List<FillUp> = listOf(
    FillUp(vehicleId = vehicleId, tripMiles = 312.0, gallons = 9.4, pricePerGallon = 3.49, odometer = 84210.0, date = utcDate(2026, 9, 2)),
    FillUp(vehicleId = vehicleId, tripMiles = 286.0, gallons = 8.7, pricePerGallon = 3.62, odometer = 84496.0, date = utcDate(2026, 9, 18)),
    FillUp(vehicleId = vehicleId, tripMiles = 340.0, gallons = 10.1, pricePerGallon = 3.55, odometer = 84836.0, date = utcDate(2026, 10, 5)),
    FillUp(vehicleId = vehicleId, tripMiles = 298.0, gallons = 9.0, pricePerGallon = 3.71, odometer = 85134.0, date = utcDate(2026, 10, 22)),
    FillUp(vehicleId = vehicleId, tripMiles = 265.0, gallons = 8.2, pricePerGallon = 3.48, odometer = 85399.0, date = utcDate(2026, 11, 6))
  )

  private fun sampleMaintenance(vehicleId: Int): List<Maintenance> = listOf(
    Maintenance(vehicleId = vehicleId, whatWasDone = "Oil change", cost = 64.50, odometer = 83000.0, date = utcDate(2026, 7, 12), hours = null),
    Maintenance(vehicleId = vehicleId, whatWasDone = "Tire rotation", cost = 40.0, odometer = 84500.0, date = utcDate(2026, 9, 20), hours = null),
    Maintenance(vehicleId = vehicleId, whatWasDone = "Brake pads", cost = 285.0, odometer = 85100.0, date = utcDate(2026, 10, 28), hours = 1.5)
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

  private data class SampleGarage(val outbackId: Int, val camryId: Int)

  private companion object {
    const val TIMEOUT_MS = 15_000L
  }
}
