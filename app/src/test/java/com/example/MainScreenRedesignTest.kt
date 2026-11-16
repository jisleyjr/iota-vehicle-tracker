package com.example

import android.content.Context
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.FillUp
import com.example.data.model.Maintenance
import com.example.data.model.Vehicle
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Behavior coverage for the issue 7 main-screen redesign: a collapsed efficiency
 * dashboard, no dashboard trash can, and delete on the selected garage card.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class MainScreenRedesignTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    Locale.setDefault(Locale.US)
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    runBlocking(Dispatchers.IO) { database().clearAllTables() }
  }

  @Test
  fun dashboardIsCollapsedByDefault() {
    showSeededGarage()

    composeTestRule.onNodeWithTag("efficiency_toggle").assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Show more stats").assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Show more stats").assertHeightIsAtLeast(48.dp)
    composeTestRule.onNodeWithText("EFFICIENCY", substring = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("Total Expenditures", ignoreCase = true).assertDoesNotExist()
    composeTestRule.onNodeWithText("Current Mileage", ignoreCase = true).assertDoesNotExist()
    composeTestRule.onNodeWithText("Gas Costs", ignoreCase = true).assertDoesNotExist()
    composeTestRule.onNodeWithText("Service Repairs", ignoreCase = true).assertDoesNotExist()
  }

  @Test
  fun tappingEfficiencyRowExpandsAndCollapses() {
    showSeededGarage()

    composeTestRule.onNodeWithTag("stats_icon_button").performClick()
    composeTestRule.onNodeWithText("MPG History (Last 30)").assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Close").performClick()
    composeTestRule.onNodeWithText("Total Expenditures", ignoreCase = true).assertDoesNotExist()
    composeTestRule.onNodeWithContentDescription("Show more stats").assertIsDisplayed()

    composeTestRule.onNodeWithTag("efficiency_toggle").performClick()
    composeTestRule.onNodeWithContentDescription("Show fewer stats").assertIsDisplayed()
    composeTestRule.onNodeWithText("Total Expenditures", ignoreCase = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("Current Mileage", ignoreCase = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("Gas Costs", ignoreCase = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("Service Repairs", ignoreCase = true).assertIsDisplayed()

    composeTestRule.onNodeWithTag("efficiency_toggle").performClick()
    composeTestRule.onNodeWithContentDescription("Show more stats").assertIsDisplayed()
    composeTestRule.onNodeWithText("Total Expenditures", ignoreCase = true).assertDoesNotExist()
    composeTestRule.onNodeWithText("Current Mileage", ignoreCase = true).assertDoesNotExist()
    composeTestRule.onNodeWithText("Gas Costs", ignoreCase = true).assertDoesNotExist()
    composeTestRule.onNodeWithText("Service Repairs", ignoreCase = true).assertDoesNotExist()
  }

  @Test
  fun dashboardTrashCanIsGone() {
    val garage = showSeededGarage()

    composeTestRule.onNodeWithTag("delete_vehicle_dashboard_button").assertDoesNotExist()
    composeTestRule.onNodeWithTag("dashboard_stats_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("vehicle_overflow_${garage.outbackId}").assertDoesNotExist()
    composeTestRule.onNodeWithContentDescription("Delete Vehicle").assertIsDisplayed()
    assertTrue(composeTestRule.onAllNodesWithContentDescription("Delete Log Entry").fetchSemanticsNodes().isNotEmpty())
  }

  @Test
  fun selectedCardOverflowOpensDeleteDialog() {
    val garage = showSeededGarage()

    composeTestRule.onNodeWithTag("vehicle_overflow_${garage.camryId}").assertIsDisplayed()
    composeTestRule.onNodeWithTag("vehicle_overflow_${garage.camryId}").performClick()
    composeTestRule.onNodeWithText("Delete vehicle").assertIsDisplayed()
    composeTestRule.onNodeWithText("Delete vehicle").performClick()
    composeTestRule.onNodeWithText("Delete Vehicle?").assertIsDisplayed()
    composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    composeTestRule.onNodeWithText("Delete").assertIsDisplayed()
  }

  private fun showSeededGarage(): SampleGarage {
    val garage = seedSampleGarage()
    composeTestRule.setContent {
      MyApplicationTheme(darkTheme = false) {
        VehicleTrackerApp(nowMillis = utcDate(2026, 11, 13))
      }
    }
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("2021 Toyota Camry Dashboard") }
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("11/06/2026") }
    return garage
  }

  private fun hasText(text: String): Boolean {
    return composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
  }

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
