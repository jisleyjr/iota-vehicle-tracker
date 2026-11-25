package com.example.design.issue15

import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.VehicleTrackerApp
import com.example.data.db.AppDatabase
import com.example.data.model.FillUp
import com.example.data.model.Maintenance
import com.example.data.model.MaintenanceType
import com.example.data.model.TireSet
import com.example.data.model.Vehicle
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import java.io.File
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
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
 * Design mockups for issue 15. These composables are test-only: they render the
 * proposed Service Logs and tire-set placement with the current theme and the
 * current garage, dashboard, and list components. They do not change the app.
 *
 * Recorded PNGs are the review files under docs/design/issue-15/.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class Issue15DesignMockupTest {

  @get:Rule val composeTestRule = createComposeRule()

  private var seeded = false

  @Before
  fun setUp() {
    Locale.setDefault(Locale.US)
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
  }

  @After
  fun tearDown() {
    if (!seeded) return
    runBlocking(Dispatchers.IO) { database().clearAllTables() }
    seeded = false
  }

  @Test
  fun beforeServiceLogsPopulated() {
    seedSampleGarage()
    showApp()
    selectCamry()
    composeTestRule.onNodeWithTag("maintenance_tab").performClick()
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Brake pads") }
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Oil changed 2399 miles ago") }
    captureRoot("before-service-logs-populated")
  }

  @Test
  fun beforeServiceLogsEmpty() {
    seedSampleGarage()
    showApp()
    composeTestRule.onNodeWithTag("maintenance_tab").performClick()
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("No oil change logged") }
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("No tire sets yet.") }
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("No Service Records") }
    captureRoot("before-service-logs-empty")
  }

  @Test
  fun beforeExpandedDashboard() {
    seedSampleGarage()
    showApp()
    selectCamry()
    composeTestRule.onNodeWithTag("efficiency_toggle").performClick()
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("TOTAL EXPENDITURES") }
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("SERVICE REPAIRS") }
    captureRoot("before-expanded-dashboard")
  }

  @Test
  @Config(sdk = [36], qualifiers = "w1273dp-h1100dp-420dpi")
  fun serviceLogsPopulatedOptions() {
    captureBoard("service-logs-populated-before-option-a-option-b") { ServiceLogComparisonBoard() }
  }

  @Test
  @Config(sdk = [36], qualifiers = "w1273dp-h1100dp-420dpi")
  fun serviceLogsEmptyOptions() {
    captureBoard("service-logs-empty-before-option-a-option-b") { EmptyServiceLogComparisonBoard() }
  }

  @Test
  @Config(sdk = [36], qualifiers = "w1273dp-h1100dp-420dpi")
  fun tireSetsOnExpandedCard() {
    captureBoard("dashboard-tire-sets-before-option-a-option-b") { TireSetComparisonBoard() }
  }

  @Test
  @Config(sdk = [36], qualifiers = "w1273dp-h1100dp-420dpi")
  fun tireSetsEmptyOnExpandedCard() {
    captureBoard("dashboard-tire-sets-empty-before-option-a-option-b") { EmptyTireSetComparisonBoard() }
  }

  @Test
  @Config(sdk = [36], qualifiers = "w1273dp-h920dp-420dpi")
  fun maintenanceCardHeightOptions() {
    composeTestRule.setContent {
      MyApplicationTheme(darkTheme = false) { CardHeightBoard() }
    }
    composeTestRule.waitUntil(TIMEOUT_MS) {
      composeTestRule.onAllNodesWithText("0 px").fetchSemanticsNodes().isEmpty() &&
        composeTestRule.onAllNodesWithText("px", substring = true).fetchSemanticsNodes().size >= 12
    }
    val fillUp = px("h_a_fill")
    assertEquals(fillUp, px("h_current_fill"))
    assertEquals(fillUp, px("h_b_fill"))
    assertEquals("Option A brake card", fillUp, px("h_a_brake"))
    assertEquals("Option A tire swap card", fillUp, px("h_a_swap"))
    assertEquals("Option A oil card", fillUp, px("h_a_oil"))
    assertEquals("Option B brake card", fillUp, px("h_b_brake"))
    assertEquals("Option B tire swap card", fillUp, px("h_b_swap"))
    assertEquals("Option B oil card", fillUp, px("h_b_oil"))
    assertTrue(px("h_current_brake") > fillUp)
    assertTrue(px("h_current_swap") > fillUp)
    assertTrue(px("h_current_oil") > fillUp)
    captureRoot("maintenance-card-height-options")
  }

  @Test
  @OptIn(ExperimentalRoborazziApi::class)
  fun optionATypeMenuOpen() {
    showScenario(Issue15Scenario(serviceLog = ServiceLogBody.OptionA, menuOpen = true))
    composeTestRule.waitUntil(TIMEOUT_MS) { hasTag("type_menu") }
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Tire rotation") }
    captureScreen("service-logs-option-a-type-menu-open")
  }

  @Test
  @OptIn(ExperimentalRoborazziApi::class)
  fun optionBTypeMenuOpen() {
    showScenario(Issue15Scenario(serviceLog = ServiceLogBody.OptionB, menuOpen = true))
    composeTestRule.waitUntil(TIMEOUT_MS) { hasTag("type_menu") }
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Tire rotation") }
    captureScreen("service-logs-option-b-type-menu-open")
  }

  @Test
  fun optionAFilteredToOilChange() {
    showScenario(
      Issue15Scenario(serviceLog = ServiceLogBody.OptionA, typeFilter = MaintenanceType.OIL_CHANGE)
    )
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Oil change") }
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Odo: 83,000", substring = true) }
    composeTestRule.waitUntil(TIMEOUT_MS) { !hasText("Brake pads") }
    captureRoot("service-logs-option-a-filtered-oil-change")
  }

  @Test
  fun optionBFilteredToOilChange() {
    showScenario(
      Issue15Scenario(serviceLog = ServiceLogBody.OptionB, typeFilter = MaintenanceType.OIL_CHANGE)
    )
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Oil change") }
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Odo: 83,000", substring = true) }
    composeTestRule.waitUntil(TIMEOUT_MS) { !hasText("Brake pads") }
    composeTestRule.waitUntil(TIMEOUT_MS) { !hasText("Oil changed 2399 miles ago") }
    captureRoot("service-logs-option-b-filtered-oil-change")
  }

  @Test
  fun optionAFilterNoMatch() {
    showScenario(
      Issue15Scenario(serviceLog = ServiceLogBody.OptionA, typeFilter = MaintenanceType.OTHER)
    )
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("No matching records") }
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("No service records match this type.") }
    captureRoot("service-logs-option-a-filter-no-match")
  }

  @Test
  @OptIn(ExperimentalRoborazziApi::class)
  fun tireSetDeleteBlocked() {
    showScenario(
      Issue15Scenario(
        serviceLog = ServiceLogBody.OptionA,
        dashboard = DashboardBody.ExpandedTireRows,
        deleteBlocked = true
      )
    )
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Can't delete this tire set") }
    composeTestRule.waitUntil(TIMEOUT_MS) {
      hasText("Winter is used by a tire swap or rotation. Rename the set instead of deleting it so service history stays intact.")
    }
    captureScreen("tire-set-delete-blocked")
  }

  private fun showScenario(scenario: Issue15Scenario) {
    composeTestRule.setContent {
      MyApplicationTheme(darkTheme = false) { Issue15Screen(scenario) }
    }
  }

  private fun captureBoard(name: String, content: @androidx.compose.runtime.Composable () -> Unit) {
    composeTestRule.setContent {
      MyApplicationTheme(darkTheme = false) { content() }
    }
    composeTestRule.waitForIdle()
    captureRoot(name)
  }

  private fun showApp() {
    composeTestRule.setContent {
      MyApplicationTheme(darkTheme = false) { VehicleTrackerApp(nowMillis = utcDate(2026, 11, 13)) }
    }
  }

  private fun selectCamry() {
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("Camry") }
    composeTestRule.onNodeWithText("Camry").performClick()
    composeTestRule.waitUntil(TIMEOUT_MS) { hasText("11/06/2026") }
  }

  private fun hasText(text: String, substring: Boolean = false): Boolean {
    return composeTestRule.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()
  }

  private fun hasTag(tag: String): Boolean {
    return composeTestRule.onAllNodes(androidx.compose.ui.test.hasTestTag(tag), useUnmergedTree = true)
      .fetchSemanticsNodes().isNotEmpty()
  }

  private fun px(tag: String): Int {
    val text = composeTestRule.onNodeWithTag("${tag}_label").fetchSemanticsNode()
      .config[SemanticsProperties.Text].first().text
    return text.removeSuffix(" px").trim().toInt()
  }

  private fun captureRoot(name: String) {
    composeTestRule.waitForIdle()
    composeTestRule.onRoot().captureRoboImage(filePath = designPng(name))
  }

  @OptIn(ExperimentalRoborazziApi::class)
  private fun captureScreen(name: String) {
    composeTestRule.waitForIdle()
    captureScreenRoboImage(filePath = designPng(name))
  }

  private fun designPng(name: String): String {
    val cwd = File(System.getProperty("user.dir") ?: ".")
    val root = if (File(cwd, "docs").isDirectory) cwd else cwd.parentFile
    val dir = File(root, "docs/design/issue-15")
    dir.mkdirs()
    return File(dir, "$name.png").absolutePath
  }

  private fun seedSampleGarage() = runBlocking(Dispatchers.IO) {
    val db = database()
    db.clearAllTables()
    val dao = db.vehicleDao()
    val camryId = dao.insertVehicle(
      Vehicle(make = "Toyota", model = "Camry", year = 2021, licensePlate = "CAM4219")
    ).toInt()
    val outbackId = dao.insertVehicle(
      Vehicle(make = "Subaru", model = "Outback", year = 2018, licensePlate = "8FKR392")
    ).toInt()
    check(outbackId != camryId)
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
    listOf(
      FillUp(vehicleId = camryId, tripMiles = 312.0, gallons = 9.4, pricePerGallon = 3.49, odometer = 84210.0, date = utcDate(2026, 9, 2)),
      FillUp(vehicleId = camryId, tripMiles = 286.0, gallons = 8.7, pricePerGallon = 3.62, odometer = 84496.0, date = utcDate(2026, 9, 18)),
      FillUp(vehicleId = camryId, tripMiles = 340.0, gallons = 10.1, pricePerGallon = 3.55, odometer = 84836.0, date = utcDate(2026, 10, 5)),
      FillUp(vehicleId = camryId, tripMiles = 298.0, gallons = 9.0, pricePerGallon = 3.71, odometer = 85134.0, date = utcDate(2026, 10, 22)),
      FillUp(vehicleId = camryId, tripMiles = 265.0, gallons = 8.2, pricePerGallon = 3.48, odometer = 85399.0, date = utcDate(2026, 11, 6))
    ).forEach { dao.insertFillUp(it) }
    listOf(
      Maintenance(
        vehicleId = camryId,
        type = MaintenanceType.TIRE_SWAP,
        title = "Mounted summer tires",
        cost = 0.0,
        odometer = 70000.0,
        date = utcDate(2026, 4, 1),
        tireSetId = summerId
      ),
      Maintenance(
        vehicleId = camryId,
        type = MaintenanceType.OIL_CHANGE,
        title = "Oil change",
        description = "5W-30 synthetic",
        cost = 64.50,
        odometer = 83000.0,
        date = utcDate(2026, 7, 12)
      ),
      Maintenance(
        vehicleId = camryId,
        type = MaintenanceType.TIRE_SWAP,
        title = "Swapped to winter tires",
        cost = 0.0,
        odometer = 84000.0,
        date = utcDate(2026, 8, 15),
        tireSetId = winterId
      ),
      Maintenance(
        vehicleId = camryId,
        type = MaintenanceType.TIRE_ROTATION,
        title = "Tire rotation",
        cost = 40.0,
        odometer = 84500.0,
        date = utcDate(2026, 9, 20),
        tireSetId = winterId
      ),
      Maintenance(
        vehicleId = camryId,
        type = MaintenanceType.REPAIR,
        title = "Brake pads",
        description = "Front pads and rotors",
        cost = 285.0,
        odometer = 85100.0,
        date = utcDate(2026, 10, 28),
        hours = 1.5
      )
    ).forEach { dao.insertMaintenance(it) }
    seeded = true
  }

  private fun database(): AppDatabase {
    val context = ApplicationProvider.getApplicationContext<Context>()
    return AppDatabase.getDatabase(context)
  }

  private companion object {
    const val TIMEOUT_MS = 15_000L
  }
}
