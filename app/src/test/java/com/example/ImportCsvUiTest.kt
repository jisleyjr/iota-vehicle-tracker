package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.csv.FillUpCsvParseResult
import com.example.data.csv.FillUpCsvParser
import com.example.data.db.AppDatabase
import com.example.data.model.Vehicle
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
  fun csvMenuOffersImportWithoutExport() {
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
    composeTestRule.onNodeWithTag("csv_menu_button").performClick()
    composeTestRule.onNodeWithTag("import_csv_menu_item").assertIsDisplayed()
    composeTestRule.onNodeWithText("Export CSV").assertDoesNotExist()
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
