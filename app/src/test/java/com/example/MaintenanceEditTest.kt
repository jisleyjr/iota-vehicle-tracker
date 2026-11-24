package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.Maintenance
import com.example.data.model.MaintenanceType
import com.example.data.model.Vehicle
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Editing a service record rewrites the existing row. Deleting from the edit form asks first.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class MaintenanceEditTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    Locale.setDefault(Locale.US)
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    runBlocking(Dispatchers.IO) {
      AppDatabase.getDatabase(ApplicationProvider.getApplicationContext()).clearAllTables()
    }
  }

  @Test
  fun editButtonOpensTheRecordPrefilledAndSavesTheChange() {
    val vehicleId = seedVehicle()
    val maintenanceId = seedMaintenance(vehicleId)
    showApp()
    openServiceLogs()

    composeTestRule.onNodeWithText("Edit").performClick()
    composeTestRule.onNodeWithText("Edit Maintenance").assertIsDisplayed()
    composeTestRule.onNodeWithTag("service_title_input").assertTextContains("Brake pads")
    composeTestRule.onNodeWithTag("service_description_input").assertTextContains("Front pads")
    composeTestRule.onNodeWithTag("service_cost_input").assertTextContains("285")
    composeTestRule.onNodeWithTag("delete_maintenance_button").assertIsDisplayed()

    composeTestRule.onNodeWithTag("service_title_input").performTextReplacement("Rear pads")
    composeTestRule.onNodeWithText("Save details").performClick()

    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Rear pads") }
    composeTestRule.onNodeWithText("Edit Maintenance").assertDoesNotExist()

    val stored = runBlocking(Dispatchers.IO) {
      AppDatabase.getDatabase(ApplicationProvider.getApplicationContext()).vehicleDao()
        .getMaintenanceSnapshot(vehicleId)
        .single()
    }
    assertEquals(maintenanceId, stored.id)
    assertEquals("Rear pads", stored.title)
    assertEquals(MaintenanceType.REPAIR, stored.type)
    assertEquals(285.0, stored.cost, 0.001)
    assertEquals(85100.0, stored.odometer, 0.001)
  }

  @Test
  fun deleteFromTheEditFormAsksForConfirmation() {
    val vehicleId = seedVehicle()
    seedMaintenance(vehicleId)
    showApp()
    openServiceLogs()

    composeTestRule.onNodeWithText("Edit").performClick()
    composeTestRule.onNodeWithTag("delete_maintenance_button").performClick()
    composeTestRule.onNodeWithText("Delete service record?").assertIsDisplayed()
    composeTestRule.onNodeWithText(
      "Are you sure you want to delete this service record? This cannot be undone."
    ).assertIsDisplayed()
    composeTestRule.onNodeWithTag("cancel_delete_maintenance").performClick()
    composeTestRule.onNodeWithText("Delete service record?").assertDoesNotExist()
    composeTestRule.onNodeWithText("Edit Maintenance").assertIsDisplayed()
    val stillThere = runBlocking(Dispatchers.IO) {
      AppDatabase.getDatabase(ApplicationProvider.getApplicationContext()).vehicleDao()
        .getMaintenanceSnapshot(vehicleId)
    }
    assertEquals(1, stillThere.size)

    composeTestRule.onNodeWithTag("delete_maintenance_button").performClick()
    composeTestRule.onNodeWithTag("confirm_delete_maintenance").performClick()

    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("No Service Records") }
    val remaining = runBlocking(Dispatchers.IO) {
      AppDatabase.getDatabase(ApplicationProvider.getApplicationContext()).vehicleDao()
        .getMaintenanceSnapshot(vehicleId)
    }
    assertTrue(remaining.isEmpty())
  }

  private fun openServiceLogs() {
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("EFFICIENCY") }
    composeTestRule.onNodeWithTag("maintenance_tab").performClick()
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Brake pads") }
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

  private fun seedVehicle(): Int = runBlocking(Dispatchers.IO) {
    AppDatabase.getDatabase(ApplicationProvider.getApplicationContext()).vehicleDao().insertVehicle(
      Vehicle(make = "Toyota", model = "Camry", year = 2021, licensePlate = "CAM4219")
    ).toInt()
  }

  private fun seedMaintenance(vehicleId: Int): Int = runBlocking(Dispatchers.IO) {
    AppDatabase.getDatabase(ApplicationProvider.getApplicationContext()).vehicleDao().insertMaintenance(
      Maintenance(
        vehicleId = vehicleId,
        type = MaintenanceType.REPAIR,
        title = "Brake pads",
        description = "Front pads",
        cost = 285.0,
        odometer = 85100.0,
        date = utcDate(2026, 10, 28),
        hours = 1.5
      )
    ).toInt()
  }

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

  private companion object {
    const val TIMEOUT_MS = 15_000L
  }
}
