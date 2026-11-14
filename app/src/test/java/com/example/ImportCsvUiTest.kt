package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.csv.FillUpCsvParseResult
import com.example.data.csv.FillUpCsvParser
import com.example.data.db.AppDatabase
import com.example.data.model.FillUp
import com.example.data.model.Vehicle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.ui.ImportCsvPreviewDialog
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class ImportCsvUiTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    runBlocking(Dispatchers.IO) {
      val context = ApplicationProvider.getApplicationContext<Context>()
      AppDatabase.getDatabase(context).clearAllTables()
    }
  }

  @Test
  fun csvMenuDisablesExportWhenTheVehicleHasNoFillUps() {
    runBlocking(Dispatchers.IO) {
      val context = ApplicationProvider.getApplicationContext<Context>()
      AppDatabase.getDatabase(context).vehicleDao().insertVehicle(
        Vehicle(make = "Toyota", model = "Camry", year = 2021, licensePlate = "CAM4219")
      )
    }
    composeTestRule.setContent {
      MyApplicationTheme(darkTheme = false) {
        VehicleTrackerApp()
      }
    }
    composeTestRule.waitUntil(TIMEOUT_MS) {
      composeTestRule.onAllNodesWithTag("csv_menu_button").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("import_csv_menu_item").assertDoesNotExist()
    composeTestRule.onNodeWithTag("export_csv_menu_item").assertDoesNotExist()
    composeTestRule.onNodeWithTag("csv_menu_button").performClick()
    composeTestRule.onNodeWithTag("import_csv_menu_item").assertIsDisplayed()
    composeTestRule.onNodeWithTag("export_csv_menu_item").assertIsDisplayed().assertIsNotEnabled()
  }

  @Test
  fun exportSheetShowsTheSuggestedFileAndSaveOrShareActions() {
    val fillUpDate = 1_762_473_600_000L
    val dateLabel = SimpleDateFormat("MM/dd/yyyy", Locale.US).format(Date(fillUpDate))
    runBlocking(Dispatchers.IO) {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val dao = AppDatabase.getDatabase(context).vehicleDao()
      val vehicleId = dao.insertVehicle(
        Vehicle(make = "Toyota", model = "Camry", year = 2021, licensePlate = "CAM4219")
      ).toInt()
      dao.insertFillUp(
        FillUp(
          vehicleId = vehicleId,
          tripMiles = 265.0,
          gallons = 8.2,
          pricePerGallon = 3.48,
          odometer = 85399.0,
          date = fillUpDate
        )
      )
      dao.insertFillUp(
        FillUp(
          vehicleId = vehicleId,
          tripMiles = 298.0,
          gallons = 9.0,
          pricePerGallon = 3.71,
          odometer = 85134.0,
          date = fillUpDate - 86_400_000L
        )
      )
    }
    composeTestRule.setContent {
      MyApplicationTheme(darkTheme = false) {
        VehicleTrackerApp()
      }
    }
    composeTestRule.waitUntil(TIMEOUT_MS) {
      composeTestRule.onAllNodesWithText(dateLabel).fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("csv_menu_button").performClick()
    composeTestRule.onNodeWithTag("export_csv_menu_item").assertIsDisplayed().assertIsEnabled()
    composeTestRule.onNodeWithTag("export_csv_menu_item").performClick()
    composeTestRule.onNodeWithTag("export_csv_sheet").assertIsDisplayed()
    composeTestRule.onNodeWithText("Export Fill-Ups (CSV)").assertIsDisplayed()
    composeTestRule.onNodeWithText("toyota-camry-fillups.csv").assertIsDisplayed()
    composeTestRule.onNodeWithText("2 rows · headers: trip,gallons,price,date").assertIsDisplayed()
    composeTestRule.onNodeWithText("Save to Device").assertIsDisplayed()
    composeTestRule.onNodeWithText("Share CSV").assertIsDisplayed()
  }

  @Test
  fun importButtonIsDisabledWhenNoNewRows() {
    val parsed = FillUpCsvParser.parse(
      """
      trip,gallons,price,date
      10,2,3.50,not-a-date
      """.trimIndent(),
      emptyList()
    )
    check(parsed is FillUpCsvParseResult.Ok)
    composeTestRule.setContent {
      MyApplicationTheme(darkTheme = false) {
        ImportCsvPreviewDialog(
          vehicleLabel = "2021 Toyota Camry",
          fileName = "camry_fillups.csv",
          preview = parsed.preview,
          onDismiss = {},
          onImport = {}
        )
      }
    }
    composeTestRule.onNodeWithText("Import 0 Rows").assertIsDisplayed().assertIsNotEnabled()
    composeTestRule.onNodeWithText("Row 1: Date could not be parsed").assertIsDisplayed()
  }

  private companion object {
    const val TIMEOUT_MS = 15_000L
  }
}
