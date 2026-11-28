package com.example.design.payments

import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the payments design mockups. PNGs are written to docs/design/payments/.
 * These tests do not exercise production payment storage; that feature is not built yet.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class PaymentsMockupScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    Locale.setDefault(Locale.US)
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
  }

  @Test
  fun ownershipFiguresMatchTheCamryDashboard() {
    assertEquals(162.08, CamryOwnership.fuel, 0.0)
    assertEquals(389.50, CamryOwnership.maintenance, 0.0)
    assertEquals(551.58, camryDashboardStats.overallTotalSpent, 0.001)
    assertEquals(33.1, camryDashboardStats.averageMpg, 0.05)
    assertEquals(4992.0, CamryOwnership.cashPaid, 0.001)
    assertEquals(26904.0, CamryOwnership.scheduleTotal, 0.001)
    assertEquals(21912.0, CamryOwnership.remaining, 0.001)
    assertEquals(5543.58, CamryOwnership.totalCost, 0.001)
    assertEquals(17399.0, CamryOwnership.milesDriven, 0.001)
    assertEquals("31.9¢", CamryOwnership.centsLabel())
    assertEquals("$5,543.58", money(CamryOwnership.totalCost))
    assertEquals("$21,912.00", money(CamryOwnership.remaining))
  }

  @Test
  fun optionAPaymentsTab() {
    show(DesignOption.TabRow)
    assertShown("Payments")
    assertShown("31.9¢")
    assertShown("$5,543.58")
    assertShown("Down payment")
    capture("option-a-payments-tab")
  }

  @Test
  fun optionAAddPayment() {
    show(DesignOption.TabRow, overlay = DesignOverlay.AddPayment)
    assertShown("Log Payment")
    assertShown("December installment")
    captureDialog("option-a-add-payment")
  }

  @Test
  fun optionAHidePaymentsMenu() {
    show(DesignOption.TabRow, overlay = DesignOverlay.HideMenu)
    composeTestRule.onNodeWithTag("hide_payments_menu").assertExists()
    assertShown("Mark paid off and hide tab")
    capture("option-a-hide-payments-menu")
  }

  @Test
  fun optionAHidePaymentsConfirm() {
    show(DesignOption.TabRow, overlay = DesignOverlay.HideConfirm)
    assertShown("Hide the Payments tab?")
    assertShown("Hide tab")
    captureDialog("option-a-hide-payments-confirm")
  }

  @Test
  fun optionAPaymentsHidden() {
    show(DesignOption.TabRow, paymentsHidden = true)
    assertShown("Fuel Logs")
    assertShown("31.9¢")
    assertShown("$5,543.58")
    assertShown("11/06/2026")
    capture("option-a-payments-hidden")
  }

  @Test
  fun optionBPaymentsTab() {
    show(DesignOption.Segmented)
    assertShown("Payments")
    assertShown("31.9¢")
    assertShown("Down payment")
    capture("option-b-payments-tab")
  }

  @Test
  fun optionBEditPayment() {
    show(DesignOption.Segmented, overlay = DesignOverlay.EditPayment)
    assertShown("Edit Payment")
    assertShown("Delete payment")
    assertShown("November installment")
    captureDialog("option-b-edit-payment")
  }

  @Test
  fun optionBHidePaymentsMenu() {
    show(DesignOption.Segmented, overlay = DesignOverlay.HideMenu)
    composeTestRule.onNodeWithTag("hide_payments_menu").assertExists()
    assertShown("Mark paid off and hide Payments")
    assertShown("Delete vehicle")
    capture("option-b-hide-payments-menu")
  }

  @Test
  fun optionBHidePaymentsConfirm() {
    show(DesignOption.Segmented, overlay = DesignOverlay.HideConfirm)
    assertShown("Hide the Payments tab?")
    captureDialog("option-b-hide-payments-confirm")
  }

  @Test
  fun optionBPaymentsHidden() {
    show(DesignOption.Segmented, paymentsHidden = true)
    assertShown("Paid off")
    assertShown("31.9¢ / mi")
    assertShown("Fuel Logs")
    capture("option-b-payments-hidden")
  }

  @Test
  fun optionCPaymentsTab() {
    show(DesignOption.DashboardTiles)
    composeTestRule.onNodeWithTag("ownership_tiles").assertExists()
    assertShown("Payments")
    assertShown("$21,912.00")
    assertShown("Down payment")
    capture("option-c-payments-tab")
  }

  @Test
  fun optionCAddPayment() {
    show(DesignOption.DashboardTiles, overlay = DesignOverlay.AddPayment)
    assertShown("Log Payment")
    assertShown("Due at signing")
    assertShown("Purchase price")
    captureDialog("option-c-add-payment")
  }

  @Test
  fun optionCVehicleDetails() {
    show(DesignOption.DashboardTiles, overlay = DesignOverlay.VehicleDetails)
    assertShown("Vehicle details")
    assertShown("Show Payments tab")
    assertShown("Mark paid off and hide tab")
    captureDialog("option-c-vehicle-details")
  }

  @Test
  fun optionCHidePaymentsConfirm() {
    show(DesignOption.DashboardTiles, overlay = DesignOverlay.HideConfirm)
    assertShown("vehicle details")
    captureDialog("option-c-hide-payments-confirm")
  }

  @Test
  fun optionCPaymentsHidden() {
    show(DesignOption.DashboardTiles, paymentsHidden = true)
    assertShown("Paid off")
    assertShown("Gas Fill-ups")
    assertShown("Service Logs")
    assertShown("31.9¢")
    assertShown("Fuel Logs")
    capture("option-c-payments-hidden")
  }

  @Test
  fun totalCostBreakdown() {
    composeTestRule.setContent {
      MyApplicationTheme(darkTheme = false) {
        CostOfOwnershipBreakdownScreen()
      }
    }
    assertShown("Cost of ownership")
    assertShown("31.9¢")
    assertShown("Payments")
    assertShown("Fuel")
    assertShown("Maintenance")
    assertShown("$5,543.58")
    assertAbsent("Agreed price")
    assertAbsent("Remaining")
    capture("total-cost-breakdown")
  }

  @Test
  fun optionDCollapsed() {
    show(DesignOption.ExpandedDashboard)
    assertShown("Payments")
    assertShown("Down payment")
    assertShown("Fill-ups")
    assertShown("Service Logs")
    assertAbsent("31.9¢")
    assertAbsent("$5,543.58")
    assertAbsent("$21,912.00")
    assertAbsent("Loan")
    assertAbsent("Remaining")
    capture("option-d-collapsed")
  }

  @Test
  fun optionDExpanded() {
    show(DesignOption.ExpandedDashboard, dashboardExpanded = true)
    assertShown("TOTAL EXPENDITURES")
    assertShown("GAS COSTS")
    assertShown("SERVICE REPAIRS")
    assertShown("TOTAL COST")
    assertShown("$5,543.58")
    assertShown("31.9¢")
    assertShown("PAID TO DATE")
    assertShown("$4,992.00")
    assertAbsent("REMAINING")
    assertAbsent("$21,912.00")
    assertAbsent("Paid off")
    assertShown("Tire sets")
    assertShown("Summer")
    assertShown("Winter")
    assertShown("Payments")
    capture("option-d-expanded")
  }

  @Test
  fun optionDPaymentsHiddenExpanded() {
    show(DesignOption.ExpandedDashboard, paymentsHidden = true, dashboardExpanded = true)
    assertShown("TOTAL COST")
    assertShown("$5,543.58")
    assertShown("31.9¢")
    assertShown("PAID TO DATE")
    assertShown("$4,992.00")
    assertAbsent("Paid off")
    assertAbsent("REMAINING")
    assertShown("Gas Fill-ups")
    assertShown("Service Logs")
    assertAbsent("$21,912.00")
    assertShown("Fuel Logs")
    capture("option-d-payments-hidden-expanded")
  }

  @Test
  fun optionDPaymentsHiddenCollapsed() {
    show(DesignOption.ExpandedDashboard, paymentsHidden = true)
    assertShown("Gas Fill-ups")
    assertShown("Service Logs")
    assertShown("Fuel Logs")
    assertShown("11/06/2026")
    assertAbsent("31.9¢")
    assertAbsent("$5,543.58")
    assertAbsent("Paid off")
    assertAbsent("Payments")
    assertAbsent("Paid off")
    assertAbsent("PAID TO DATE")
    capture("option-d-payments-hidden-collapsed")
  }

  @Test
  fun optionDAddPayment() {
    show(DesignOption.ExpandedDashboard, overlay = DesignOverlay.AddPayment)
    assertShown("Log Payment")
    assertShown("Purchase")
    assertShown("Down payment")
    assertShown("Payment")
    assertShown("Other")
    assertShown("Amount ($)")
    assertShown("Note (optional)")
    assertShown("Date: 11/13/2026")
    assertAbsent("Odometer")
    assertAbsent("Loan")
    assertAbsent("Lease")
    captureDialog("option-d-add-payment")
  }

  @Test
  fun optionDHideConfirm() {
    show(DesignOption.ExpandedDashboard, overlay = DesignOverlay.HideConfirm)
    assertShown("Hide Payments tab?")
    assertShown("Your logged payments stay in total cost.")
    assertAbsent("paid off")
    assertAbsent("Paid off")
    captureDialog("option-d-hide-confirm")
  }

  private fun show(
    option: DesignOption,
    paymentsHidden: Boolean = false,
    overlay: DesignOverlay = DesignOverlay.None,
    dashboardExpanded: Boolean = false
  ) {
    composeTestRule.setContent {
      MyApplicationTheme(darkTheme = false) {
        PaymentsDesignScreen(
          option = option,
          paymentsHidden = paymentsHidden,
          overlay = overlay,
          dashboardExpanded = dashboardExpanded
        )
      }
    }
    composeTestRule.waitForIdle()
  }

  private fun assertShown(text: String) {
    check(composeTestRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()) {
      "Missing text: $text"
    }
  }

  private fun assertAbsent(text: String) {
    check(composeTestRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isEmpty()) {
      "Unexpected text: $text"
    }
  }

  private fun capture(name: String) {
    composeTestRule.waitForIdle()
    composeTestRule.onRoot().captureRoboImage(filePath = designPng(name))
  }

  private fun captureDialog(name: String) {
    composeTestRule.waitForIdle()
    composeTestRule.onNode(isDialog()).captureRoboImage(filePath = designPng(name))
  }

  private fun designPng(name: String): String {
    val cwd = File(System.getProperty("user.dir") ?: ".")
    val root = if (File(cwd, "docs").isDirectory) cwd else cwd.parentFile
    val dir = File(root, "docs/design/payments")
    dir.mkdirs()
    return File(dir, "$name.png").absolutePath
  }
}
