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
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.FillUp
import com.example.data.model.Vehicle
import com.example.data.repository.VehicleRepository
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.VehicleViewModel
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.robolectric.shadows.ShadowLooper
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
 * Editing a fill-up rewrites the existing row and the stats flow.
 * Deleting from the edit form asks for confirmation first.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class FillUpEditTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var database: AppDatabase

  @Before
  fun setUp() {
    Locale.setDefault(Locale.US)
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    val context = ApplicationProvider.getApplicationContext<Context>()
    database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    runBlocking(Dispatchers.IO) { AppDatabase.getDatabase(context).clearAllTables() }
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun updateFillUpRewritesTheRowAndRecomputesStats() {
    val dao = database.vehicleDao()
    val repository = VehicleRepository(dao)
    val vehicleId = runBlocking {
      dao.insertVehicle(
        Vehicle(make = "Toyota", model = "Camry", year = 2021, licensePlate = "CAM4219")
      ).toInt()
    }
    val insertedId = runBlocking {
      dao.insertFillUp(
        FillUp(
          vehicleId = vehicleId,
          tripMiles = 200.0,
          gallons = 10.0,
          pricePerGallon = 3.0,
          odometer = 10000.0,
          date = utcDate(2026, 11, 6)
        )
      ).toInt()
    }
    val saved = runBlocking { dao.getFillUpSnapshot(vehicleId).single() }
    assertEquals(insertedId, saved.id)

    val viewModel = VehicleViewModel(repository)
    viewModel.selectVehicle(vehicleId)
    val statsJob = CoroutineScope(Dispatchers.Main).launch {
      viewModel.selectedVehicleStats.collect { }
    }
    try {
      awaitIdle {
        val stats = viewModel.selectedVehicleStats.value
        abs(stats.averageMpg - 20.0) < 0.01 && abs(stats.totalFuelSpent - 30.0) < 0.01
      }

      viewModel.updateFillUp(
        fillUp = saved,
        tripMiles = 400.0,
        gallons = 10.0,
        pricePerGallon = 4.0,
        odometer = 12000.0,
        date = utcDate(2026, 11, 8)
      )

      awaitIdle {
        val row = runBlocking { dao.getFillUpSnapshot(vehicleId).single() }
        val stats = viewModel.selectedVehicleStats.value
        row.id == saved.id &&
          row.tripMiles == 400.0 &&
          abs(stats.averageMpg - 40.0) < 0.01 &&
          abs(stats.totalFuelSpent - 40.0) < 0.01 &&
          abs(stats.latestOdometer - 12000.0) < 0.01
      }

      val updated = runBlocking { dao.getFillUpSnapshot(vehicleId).single() }
      assertEquals(saved.id, updated.id)
      assertEquals(vehicleId, updated.vehicleId)
      assertEquals(400.0, updated.tripMiles, 0.001)
      assertEquals(10.0, updated.gallons, 0.001)
      assertEquals(4.0, updated.pricePerGallon, 0.001)
      assertEquals(12000.0, updated.odometer!!, 0.001)
      assertEquals(utcDate(2026, 11, 8), updated.date)
      assertEquals(1, runBlocking { dao.getFillUpSnapshot(vehicleId).size })
    } finally {
      statsJob.cancel()
      ShadowLooper.idleMainLooper()
    }
  }

  @Test
  fun editButtonOpensTheFillUpPrefilledAndSavesTheChange() {
    val vehicleId = seedVehicle()
    val fillUpId = seedFillUp(vehicleId)
    showApp()

    composeTestRule.onNodeWithText("Edit").performClick()
    composeTestRule.onNodeWithText("Edit Fill-up").assertIsDisplayed()
    composeTestRule.onNodeWithTag("trip_miles_input").assertTextContains("265")
    composeTestRule.onNodeWithTag("gallons_input").assertTextContains("8.2")
    composeTestRule.onNodeWithTag("price_input").assertTextContains("3.48")
    composeTestRule.onNodeWithTag("odo_input").assertTextContains("85399")
    composeTestRule.onNodeWithText("Date: 11/06/2026").assertIsDisplayed()
    composeTestRule.onNodeWithTag("delete_fill_up_button").assertIsDisplayed()

    composeTestRule.onNodeWithTag("trip_miles_input").performTextReplacement("300")
    composeTestRule.onNodeWithText("Save details").performClick()

    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("300.0 mi", substring = true) }
    composeTestRule.onNodeWithText("Edit Fill-up").assertDoesNotExist()

    val stored = runBlocking(Dispatchers.IO) {
      AppDatabase.getDatabase(ApplicationProvider.getApplicationContext()).vehicleDao()
        .getFillUpSnapshot(vehicleId)
        .single()
    }
    assertEquals(fillUpId, stored.id)
    assertEquals(300.0, stored.tripMiles, 0.001)
    assertEquals(8.2, stored.gallons, 0.001)
    assertEquals(3.48, stored.pricePerGallon, 0.001)
    assertEquals(85399.0, stored.odometer)
  }

  @Test
  fun deleteFromTheEditFormAsksForConfirmation() {
    val vehicleId = seedVehicle()
    seedFillUp(vehicleId)
    showApp()

    composeTestRule.onNodeWithText("Edit").performClick()
    composeTestRule.onNodeWithTag("delete_fill_up_button").performClick()
    composeTestRule.onNodeWithText("Delete Fill-up?").assertIsDisplayed()
    composeTestRule.onNodeWithText(
      "Are you sure you want to delete this fill-up? This cannot be undone."
    ).assertIsDisplayed()
    composeTestRule.onNodeWithTag("cancel_delete_fill_up").performClick()
    composeTestRule.onNodeWithText("Delete Fill-up?").assertDoesNotExist()
    composeTestRule.onNodeWithText("Edit Fill-up").assertIsDisplayed()
    val stillThere = runBlocking(Dispatchers.IO) {
      AppDatabase.getDatabase(ApplicationProvider.getApplicationContext()).vehicleDao()
        .getFillUpSnapshot(vehicleId)
    }
    assertEquals(1, stillThere.size)

    composeTestRule.onNodeWithTag("delete_fill_up_button").performClick()
    composeTestRule.onNodeWithTag("confirm_delete_fill_up").performClick()

    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("No Fuel Logs") }
    composeTestRule.onNodeWithText("Edit Fill-up").assertDoesNotExist()
    val remaining = runBlocking(Dispatchers.IO) {
      AppDatabase.getDatabase(ApplicationProvider.getApplicationContext()).vehicleDao()
        .getFillUpSnapshot(vehicleId)
    }
    assertTrue(remaining.isEmpty())
  }

  private fun showApp() {
    composeTestRule.setContent {
      MyApplicationTheme(darkTheme = false) {
        VehicleTrackerApp(nowMillis = utcDate(2026, 11, 13))
      }
    }
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("11/06/2026") }
  }

  private fun seedVehicle(): Int = runBlocking(Dispatchers.IO) {
    AppDatabase.getDatabase(ApplicationProvider.getApplicationContext()).vehicleDao()
      .insertVehicle(
        Vehicle(make = "Toyota", model = "Camry", year = 2021, licensePlate = "CAM4219")
      ).toInt()
  }

  private fun seedFillUp(vehicleId: Int): Int = runBlocking(Dispatchers.IO) {
    AppDatabase.getDatabase(ApplicationProvider.getApplicationContext()).vehicleDao()
      .insertFillUp(
        FillUp(
          vehicleId = vehicleId,
          tripMiles = 265.0,
          gallons = 8.2,
          pricePerGallon = 3.48,
          odometer = 85399.0,
          date = utcDate(2026, 11, 6)
        )
      ).toInt()
  }

  private fun hasText(text: String, substring: Boolean = false): Boolean {
    return composeTestRule.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()
  }

  private fun awaitIdle(condition: () -> Boolean) {
    val deadline = System.currentTimeMillis() + TIMEOUT_MS
    while (System.currentTimeMillis() < deadline) {
      ShadowLooper.idleMainLooper()
      if (condition()) return
      Thread.sleep(25)
    }
    throw AssertionError("Timed out waiting for the fill-up update to reach stats")
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
