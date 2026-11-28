@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.design.payments

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.example.CsvActionsButton
import com.example.CustomDialogEnvelope
import com.example.FillUpLogCard
import com.example.MiniDashboardGridCard
import com.example.TabSegmentControl
import com.example.VehicleDashboardStats
import com.example.VehicleGarageCard
import com.example.ui.theme.EmeraldSuccess
import java.util.Locale

internal enum class DesignOption { TabRow, Segmented, DashboardTiles }

internal enum class DesignOverlay { None, AddPayment, EditPayment, HideMenu, HideConfirm, VehicleDetails }

private data class DesignMenuItem(
  val label: String,
  val icon: ImageVector,
  val destructive: Boolean = false,
  val dividerBefore: Boolean = false
)

private data class PillTab(
  val label: String,
  val icon: ImageVector,
  val selectedIcon: ImageVector
)

/**
 * Design-only screens for a Payments tab and cost per mile. Nothing here is wired to Room.
 * The garage, efficiency card, fill-up rows, and dialog shell are the app's own composables.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PaymentsDesignScreen(
  option: DesignOption,
  paymentsHidden: Boolean = false,
  overlay: DesignOverlay = DesignOverlay.None
) {
  val showPayments = !paymentsHidden
  val fabLabel = if (showPayments && overlay != DesignOverlay.None) {
    "Add Payment"
  } else if (showPayments) {
    "Add Payment"
  } else {
    "Add Fill-Up"
  }
  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = { TrackerTopBar() },
    floatingActionButton = {
      FloatingActionButton(
        onClick = {},
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.navigationBarsPadding()
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(imageVector = Icons.Default.Add, contentDescription = null)
          Text(text = fabLabel, fontWeight = FontWeight.SemiBold)
        }
      }
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp)
    ) {
      Text(
        text = "Your Garage",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
      )
      GarageRow(menuOpen = option == DesignOption.Segmented && overlay == DesignOverlay.HideMenu)
      Spacer(modifier = Modifier.height(16.dp))
      VehicleDashboardStats(
        stats = camryDashboardStats,
        lastOilChangeMiles = "2399",
        tireSetMileage = emptyList(),
        mountedTireSetId = null,
        onStatsClick = {},
        onAddTireSet = {},
        onEditTireSet = {}
      )
      if (option == DesignOption.TabRow) {
        Spacer(modifier = Modifier.height(12.dp))
        CostPerMileStrip()
      }
      if (option == DesignOption.DashboardTiles) {
        Spacer(modifier = Modifier.height(12.dp))
        OwnershipTileRow(paidOff = paymentsHidden)
      }
      Spacer(modifier = Modifier.height(16.dp))
      OptionTabs(option = option, paymentsHidden = paymentsHidden)
      if (option == DesignOption.Segmented && paymentsHidden) {
        Spacer(modifier = Modifier.height(8.dp))
        PaidOffChipRow()
      }
      Spacer(modifier = Modifier.height(12.dp))
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
      ) {
        if (showPayments) {
          PaymentsBody(
            option = option,
            menuOpen = option == DesignOption.TabRow && overlay == DesignOverlay.HideMenu
          )
        } else {
          FuelBody()
        }
      }
    }
  }

  if (overlay == DesignOverlay.AddPayment || overlay == DesignOverlay.EditPayment) {
    PaymentFormDialog(editing = overlay == DesignOverlay.EditPayment, emphasizeDownPayment = option == DesignOption.DashboardTiles && overlay == DesignOverlay.AddPayment)
  }
  if (overlay == DesignOverlay.HideConfirm) {
    HidePaymentsConfirm(option = option)
  }
  if (overlay == DesignOverlay.VehicleDetails) {
    VehicleDetailsDialog()
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CostOfOwnershipBreakdownScreen() {
  val paymentsShare = CamryOwnership.cashPaid / CamryOwnership.totalCost
  val fuelShare = CamryOwnership.fuel / CamryOwnership.totalCost
  val serviceShare = CamryOwnership.maintenance / CamryOwnership.totalCost
  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "Cost of ownership",
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp
          )
        },
        navigationIcon = {
          IconButton(onClick = {}) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp)
    ) {
      Text(
        text = "2021 Toyota Camry",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = "CAM4219 · loan in progress",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 13.sp
      )
      Spacer(modifier = Modifier.height(12.dp))
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          SectionLabel("Cost per mile")
          Text(
            text = CamryOwnership.centsLabel(),
            fontSize = 42.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "${money(CamryOwnership.totalCost)} total  ·  ${wholeMiles(CamryOwnership.milesDriven)} miles",
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = "Counted in the total",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
      )
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(14.dp)
          .clip(RoundedCornerShape(7.dp))
      ) {
        Box(modifier = Modifier.weight(paymentsShare.toFloat()).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
        Box(modifier = Modifier.weight(fuelShare.toFloat()).fillMaxHeight().background(MaterialTheme.colorScheme.tertiary))
        Box(modifier = Modifier.weight(serviceShare.toFloat()).fillMaxHeight().background(EmeraldSuccess))
      }
      Spacer(modifier = Modifier.height(10.dp))
      BreakdownRow(
        color = MaterialTheme.colorScheme.primary,
        title = "Purchase payments",
        detail = "Down payment ${money(CamryOwnership.downPayment.amount)} · ${CamryOwnership.paymentsLogged} loan payments ${money(CamryOwnership.monthlyAmount * CamryOwnership.paymentsLogged)}",
        amount = money(CamryOwnership.cashPaid),
        share = paymentsShare
      )
      BreakdownRow(
        color = MaterialTheme.colorScheme.tertiary,
        title = "Fuel",
        detail = "Same figure as Gas Costs on the efficiency card",
        amount = money(CamryOwnership.fuel),
        share = fuelShare
      )
      BreakdownRow(
        color = EmeraldSuccess,
        title = "Maintenance",
        detail = "Same figure as Service Repairs on the efficiency card",
        amount = money(CamryOwnership.maintenance),
        share = serviceShare
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "Shown for context",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
      )
      Spacer(modifier = Modifier.height(4.dp))
      ContextLine("Agreed price", money(CamryOwnership.agreedPrice), "Not added, because this Camry is tracked as a loan.")
      ContextLine("Remaining on the schedule", money(CamryOwnership.remaining), "Unpaid installments are not in the total yet.")
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "Miles",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
      )
      Spacer(modifier = Modifier.height(4.dp))
      ContextLine("Latest odometer", wholeMiles(CamryOwnership.latestOdometer), "Highest reading on a fill-up, service, or payment.")
      ContextLine("Odometer at purchase", wholeMiles(CamryOwnership.odometerAtPurchase), "Taken from the down payment.")
      ContextLine("Miles driven", wholeMiles(CamryOwnership.milesDriven), "Latest minus purchase. With no purchase odometer, use the sum of trip miles.")
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = "(${money(CamryOwnership.cashPaid)} + ${money(CamryOwnership.fuel)} + ${money(CamryOwnership.maintenance)}) ÷ ${wholeMiles(CamryOwnership.milesDriven)} mi = ${CamryOwnership.centsLabel()} per mile",
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
private fun TrackerTopBar() {
  TopAppBar(
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(
          imageVector = Icons.Filled.DirectionsCar,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(30.dp)
        )
        Text(
          text = "Vehicle Tracker",
          fontWeight = FontWeight.Bold,
          fontSize = 22.sp,
          color = MaterialTheme.colorScheme.onBackground
        )
      }
    },
    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
  )
}

@Composable
private fun GarageRow(menuOpen: Boolean) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    modifier = Modifier
      .fillMaxWidth()
      .zIndex(if (menuOpen) 2f else 0f)
  ) {
    VehicleGarageCard(
      vehicle = designOutback,
      isSelected = false,
      onClick = {},
      onDelete = {}
    )
    Box {
      VehicleGarageCard(
        vehicle = designCamry,
        isSelected = true,
        onClick = {},
        onDelete = {}
      )
      if (menuOpen) {
          DesignMenu(
            items = listOf(
              DesignMenuItem("Cost breakdown", Icons.Filled.Insights),
              DesignMenuItem("Mark paid off and hide Payments", Icons.Outlined.VisibilityOff),
              DesignMenuItem("Delete vehicle", Icons.Filled.Delete, destructive = true, dividerBefore = true)
            ),
            modifier = Modifier
              .overlayAt(x = (-123).dp, y = 72.dp)
              .testTag("hide_payments_menu")
          )
      }
    }
  }
}

@Composable
private fun CostPerMileStrip() {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        SectionLabel("Total cost")
        Text(
          text = money(CamryOwnership.totalCost),
          fontSize = 22.sp,
          fontWeight = FontWeight.Black,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      Column(horizontalAlignment = Alignment.End) {
        SectionLabel("Cost per mile")
        Text(
          text = CamryOwnership.centsLabel(),
          fontSize = 22.sp,
          fontWeight = FontWeight.Black,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
private fun OwnershipTileRow(paidOff: Boolean) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("ownership_tiles"),
    shape = RoundedCornerShape(28.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      MiniDashboardGridCard(
        title = "Total cost",
        value = money(CamryOwnership.totalCost),
        modifier = Modifier.weight(1f)
      )
      MiniDashboardGridCard(
        title = "Per mile",
        value = CamryOwnership.centsLabel(),
        modifier = Modifier.weight(1f)
      )
      MiniDashboardGridCard(
        title = if (paidOff) "Status" else "Remaining",
        value = if (paidOff) "Paid off" else money(CamryOwnership.remaining),
        modifier = Modifier.weight(1f)
      )
    }
  }
}

@Composable
private fun PaidOffChipRow() {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = EmeraldSuccess.copy(alpha = 0.12f)),
    border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.35f))
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = Icons.Filled.Check,
        contentDescription = null,
        tint = EmeraldSuccess,
        modifier = Modifier.size(18.dp)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(text = "Total cost", fontWeight = FontWeight.Bold, color = EmeraldSuccess)
        Text(
          text = "Payments stay in the total",
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = money(CamryOwnership.totalCost),
          fontWeight = FontWeight.Black,
          fontSize = 14.sp
        )
        Text(
          text = "${CamryOwnership.centsLabel()} / mi",
          fontWeight = FontWeight.Bold,
          fontSize = 12.sp,
          color = EmeraldSuccess
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionTabs(option: DesignOption, paymentsHidden: Boolean) {
  when (option) {
    DesignOption.TabRow -> {
      val labels = if (paymentsHidden) listOf("Fill-ups", "Service Logs") else listOf("Fill-ups", "Service Logs", "Payments")
      val selected = if (paymentsHidden) 0 else 2
      PrimaryTabRow(
        selectedTabIndex = selected,
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        contentColor = MaterialTheme.colorScheme.primary
      ) {
        labels.forEachIndexed { index, label ->
          Tab(
            selected = index == selected,
            onClick = {},
            selectedContentColor = MaterialTheme.colorScheme.primary,
            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            text = {
              Text(
                text = label,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1
              )
            }
          )
        }
      }
    }
    DesignOption.Segmented -> {
      val labels = if (paymentsHidden) listOf("Fill-ups", "Service Logs") else listOf("Fill-ups", "Service Logs", "Payments")
      val selected = if (paymentsHidden) 0 else 2
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
          labels.forEachIndexed { index, label ->
            SegmentedButton(
              selected = index == selected,
              onClick = {},
              shape = SegmentedButtonDefaults.itemShape(index = index, count = labels.size),
              colors = auburnSegments(),
              icon = {}
            ) {
              Text(
                text = label,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
        if (paymentsHidden) {
          FilterChip(
            selected = true,
            onClick = {},
            label = { Text("Paid off", fontWeight = FontWeight.Bold) },
            leadingIcon = {
              Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = EmeraldSuccess.copy(alpha = 0.15f),
              selectedLabelColor = EmeraldSuccess,
              selectedLeadingIconColor = EmeraldSuccess
            )
          )
        }
      }
    }
    DesignOption.DashboardTiles -> {
      if (paymentsHidden) {
        TabSegmentControl(selectedTab = 0, onTabSelected = {})
      } else {
        PillTabs(
          tabs = listOf(
            PillTab("Fill-ups", Icons.Outlined.LocalGasStation, Icons.Filled.LocalGasStation),
            PillTab("Service Logs", Icons.Outlined.Build, Icons.Filled.Build),
            PillTab("Payments", Icons.Outlined.Payments, Icons.Filled.Payments)
          ),
          selected = 2
        )
      }
    }
  }
}

@Composable
private fun PillTabs(tabs: List<PillTab>, selected: Int) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .height(52.dp),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
  ) {
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(4.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      tabs.forEachIndexed { index, tab ->
        val on = index == selected
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(if (on) MaterialTheme.colorScheme.primary else Color.Transparent),
          contentAlignment = Alignment.Center
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = if (on) tab.selectedIcon else tab.icon,
              contentDescription = null,
              tint = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = tab.label,
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }
  }
}

@Composable
private fun PaymentsBody(option: DesignOption, menuOpen: Boolean) {
  Column(modifier = Modifier.fillMaxSize()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .zIndex(1f)
        .padding(bottom = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Payments",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
      )
      if (option == DesignOption.TabRow) {
        Box {
          IconButton(
            onClick = {},
            modifier = Modifier
              .size(32.dp)
              .testTag("payments_overflow")
          ) {
            Icon(
              imageVector = Icons.Filled.MoreVert,
              contentDescription = "Payment options",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          if (menuOpen) {
            DesignMenu(
              items = listOf(
                DesignMenuItem("Cost breakdown", Icons.Filled.Insights),
                DesignMenuItem("Mark paid off and hide tab", Icons.Outlined.VisibilityOff)
              ),
              modifier = Modifier
                .overlayAt(x = (-276).dp, y = 28.dp)
                .testTag("hide_payments_menu")
            )
          }
        }
      }
    }
    LazyColumn(
      verticalArrangement = Arrangement.spacedBy(8.dp),
      contentPadding = PaddingValues(bottom = 88.dp),
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
    ) {
      if (option == DesignOption.TabRow) {
        item { LoanProgressCard() }
      }
      if (option == DesignOption.Segmented) {
        item { SegmentedBalanceBanner() }
      }
      item {
        Text(
          text = "At purchase",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 4.dp)
        )
      }
      item { PaymentRow(CamryOwnership.downPayment) }
      item {
        Text(
          text = "Installments",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 4.dp)
        )
      }
      items(CamryOwnership.installments) { payment ->
        PaymentRow(payment)
      }
    }
  }
}

@Composable
private fun FuelBody() {
  Column(modifier = Modifier.fillMaxSize()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Fuel Logs",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
      )
      CsvActionsButton(
        exportEnabled = true,
        onImportClick = {},
        onExportClick = {}
      )
    }
    LazyColumn(
      verticalArrangement = Arrangement.spacedBy(8.dp),
      contentPadding = PaddingValues(bottom = 88.dp),
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
    ) {
      items(designFillUps()) { fillUp ->
        FillUpLogCard(fillUp = fillUp, onEditClick = {})
      }
    }
  }
}

@Composable
private fun LoanProgressCard() {
  val fraction = (CamryOwnership.cashPaid / CamryOwnership.scheduleTotal).toFloat()
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Column(modifier = Modifier.weight(1f)) {
          SectionLabel("Auto loan")
          Text(
            text = "${money(CamryOwnership.monthlyAmount)} a month · ${CamryOwnership.termMonths} months",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Column(horizontalAlignment = Alignment.End) {
          SectionLabel("Remaining")
          Text(
            text = money(CamryOwnership.remaining),
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }
        LinearProgressIndicator(
        progress = { fraction },
        modifier = Modifier
          .fillMaxWidth()
          .height(8.dp)
          .clip(RoundedCornerShape(4.dp)),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surface,
        gapSize = 0.dp,
        drawStopIndicator = {}
      )
      Text(
        text = "${CamryOwnership.paymentsLogged} of ${CamryOwnership.termMonths} payments · ${money(CamryOwnership.cashPaid)} paid of ${money(CamryOwnership.scheduleTotal)}",
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Text(
        text = "Next due ${CamryOwnership.nextDue}. Agreed price ${money(CamryOwnership.agreedPrice)} is not added on top of payments.",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.outline
      )
    }
  }
}

@Composable
private fun SegmentedBalanceBanner() {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    MiniDashboardGridCard("Total cost", money(CamryOwnership.totalCost), Modifier.weight(1.15f))
    MiniDashboardGridCard("Per mile", CamryOwnership.centsLabel(), Modifier.weight(0.8f))
    MiniDashboardGridCard("Remaining", money(CamryOwnership.remaining), Modifier.weight(1.15f))
  }
}

@Composable
private fun PaymentRow(payment: MockPayment) {
  val accent = if (payment.kind == PaymentKind.LOAN || payment.kind == PaymentKind.LEASE) {
    MaterialTheme.colorScheme.primary
  } else {
    EmeraldSuccess
  }
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
    shape = RoundedCornerShape(12.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(54.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(accent.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Outlined.Payments,
          contentDescription = null,
          tint = accent,
          modifier = Modifier.size(26.dp)
        )
      }
      Spacer(modifier = Modifier.width(14.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = paymentTitle(payment.kind),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = payment.kind.chip,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = accent,
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(accent.copy(alpha = 0.12f))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = money(payment.amount),
            fontWeight = FontWeight.Black,
            fontSize = 15.sp,
            color = accent,
            maxLines = 1
          )
          TextButton(onClick = {}, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
            Text(text = "Edit", fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }
        }
        Spacer(modifier = Modifier.height(4.dp))
        val odo = payment.odometer?.let { "Odo: ${wholeMiles(it)}" }
        Text(
          text = listOfNotNull(payment.dateLabel, odo).joinToString(" · "),
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.outline,
          maxLines = 1
        )
      }
    }
  }
}

@Composable
private fun DesignMenu(items: List<DesignMenuItem>, modifier: Modifier = Modifier) {
  Card(
    modifier = modifier.width(308.dp),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
  ) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
      items.forEach { item ->
        if (item.dividerBefore) {
          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        }
        val color = if (item.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(imageVector = item.icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(12.dp))
          Text(text = item.label, fontWeight = FontWeight.SemiBold, color = color, fontSize = 14.sp)
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaymentFormDialog(editing: Boolean, emphasizeDownPayment: Boolean) {
  val kind = when {
    editing -> PaymentKind.LOAN
    emphasizeDownPayment -> PaymentKind.DOWN_PAYMENT
    else -> PaymentKind.LOAN
  }
  val amount = when (kind) {
    PaymentKind.DOWN_PAYMENT -> "3000.00"
    else -> if (editing) "498.00" else "498.00"
  }
  val odometer = when (kind) {
    PaymentKind.DOWN_PAYMENT -> "68000"
    else -> if (editing) "85050" else "85420"
  }
  val note = when {
    kind == PaymentKind.DOWN_PAYMENT -> "Due at signing"
    editing -> "November installment"
    else -> "December installment"
  }
  val date = when {
    kind == PaymentKind.DOWN_PAYMENT -> "03/12/2024"
    editing -> "11/01/2026"
    else -> "11/13/2026"
  }
  CustomDialogEnvelope(
    title = if (editing) "Edit Payment" else "Log Payment",
    subtitle = "2021 Toyota Camry",
    onDismiss = {},
    onSave = {}
  ) {
    Text(text = "Type", fontWeight = FontWeight.Bold, fontSize = 13.sp)
    Spacer(modifier = Modifier.height(6.dp))
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      listOf(
        PaymentKind.LOAN to "Loan payment",
        PaymentKind.LEASE to "Lease payment",
        PaymentKind.DOWN_PAYMENT to "Down payment",
        PaymentKind.PURCHASE_PRICE to "Purchase price"
      ).forEach { (value, label) ->
        FilterChip(
          selected = kind == value,
          onClick = {},
          label = { Text(label) }
        )
      }
    }
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = kindHelp(kind),
      fontSize = 12.sp,
      color = MaterialTheme.colorScheme.outline
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
      value = amount,
      onValueChange = {},
      label = { Text("Amount ($)") },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
      singleLine = true,
      modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
      value = odometer,
      onValueChange = {},
      label = { Text("Odometer (optional)") },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
      singleLine = true,
      modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
      value = note,
      onValueChange = {},
      label = { Text("Note (optional)") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(12.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(text = "Date: $date", fontWeight = FontWeight.Bold)
      TextButton(onClick = {}) {
        Icon(imageVector = Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Change Date")
      }
    }
    if (editing) {
      Spacer(modifier = Modifier.height(12.dp))
      OutlinedButton(
        onClick = {},
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
      ) {
        Icon(imageVector = Icons.Outlined.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Delete payment", fontWeight = FontWeight.SemiBold)
      }
    }
  }
}

@Composable
private fun HidePaymentsConfirm(option: DesignOption) {
  val where = when (option) {
    DesignOption.TabRow -> "You can show the tab again from this menu."
    DesignOption.Segmented -> "You can show the tab again from the vehicle menu."
    DesignOption.DashboardTiles -> "You can show the tab again in vehicle details."
  }
  AlertDialog(
    onDismissRequest = {},
    modifier = Modifier.testTag("hide_payments_confirm"),
    title = { Text("Hide the Payments tab?") },
    text = {
      Text(
        "Mark the 2021 Camry paid off. The ${money(CamryOwnership.cashPaid)} already logged still counts toward the ${money(CamryOwnership.totalCost)} total cost and ${CamryOwnership.centsLabel()} per mile. $where"
      )
    },
    confirmButton = {
      Button(onClick = {}) { Text("Hide tab", fontWeight = FontWeight.Bold) }
    },
    dismissButton = {
      TextButton(onClick = {}) { Text("Cancel") }
    }
  )
}

@Composable
private fun VehicleDetailsDialog() {
  Dialog(
    onDismissRequest = {},
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .padding(16.dp)
        .testTag("vehicle_details"),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
    ) {
      Column(modifier = Modifier.padding(20.dp)) {
        Text(
          text = "Vehicle details",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "2021 Toyota Camry · CAM4219",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(16.dp))
        DetailLine("Make", "Toyota")
        DetailLine("Model", "Camry")
        DetailLine("Year", "2021")
        DetailLine("Plate", "CAM4219")
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Payments", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(text = "Show Payments tab", fontWeight = FontWeight.SemiBold)
            Text(
              text = "On while the loan is active",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.outline
            )
          }
          Switch(checked = true, onCheckedChange = {})
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Hiding the tab keeps every logged payment in the total cost and the cost per mile.",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
          onClick = {},
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("Mark paid off and hide tab", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          TextButton(onClick = {}) { Text("Close", fontWeight = FontWeight.SemiBold) }
        }
      }
    }
  }
}

@Composable
private fun BreakdownRow(color: Color, title: String, detail: String, amount: String, share: Double) {
  Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(10.dp)
          .clip(RoundedCornerShape(3.dp))
          .background(color)
      )
      Spacer(modifier = Modifier.width(10.dp))
      Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        modifier = Modifier.weight(1f)
      )
      Text(text = amount, fontWeight = FontWeight.Black, fontSize = 14.sp)
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = String.format(Locale.US, "%.0f%%", share * 100.0),
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.width(32.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.End
      )
    }
    Text(
      text = detail,
      fontSize = 11.sp,
      color = MaterialTheme.colorScheme.outline,
      modifier = Modifier.padding(start = 20.dp, top = 2.dp)
    )
  }
}

@Composable
private fun ContextLine(label: String, value: String, detail: String) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(text = label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
      Text(text = value, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
    Text(text = detail, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
  }
}

@Composable
private fun DetailLine(label: String, value: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 3.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(text = value, fontWeight = FontWeight.SemiBold)
  }
}

@Composable
private fun SectionLabel(text: String) {
  Text(
    text = text.uppercase(Locale.US),
    fontSize = 11.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 1.2.sp,
    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
  )
}

@Composable
private fun auburnSegments() = SegmentedButtonDefaults.colors(
  activeContainerColor = MaterialTheme.colorScheme.primary,
  activeContentColor = MaterialTheme.colorScheme.onPrimary,
  activeBorderColor = MaterialTheme.colorScheme.primary,
  inactiveContainerColor = MaterialTheme.colorScheme.surface,
  inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
  inactiveBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
)

private fun Modifier.overlayAt(x: Dp, y: Dp): Modifier = layout { measurable, constraints ->
  val placeable = measurable.measure(
    constraints.copy(
      minWidth = 0,
      minHeight = 0,
      maxWidth = Int.MAX_VALUE,
      maxHeight = Int.MAX_VALUE
    )
  )
  layout(0, 0) {
    placeable.placeRelative(x.roundToPx(), y.roundToPx())
  }
}

private fun paymentTitle(kind: PaymentKind): String = when (kind) {
  PaymentKind.LOAN -> "Loan payment"
  PaymentKind.LEASE -> "Lease payment"
  PaymentKind.DOWN_PAYMENT -> "Down payment"
  PaymentKind.PURCHASE_PRICE -> "Purchase price"
}

private fun kindHelp(kind: PaymentKind): String = when (kind) {
  PaymentKind.LOAN -> "One installment. It counts toward the payoff and toward total cost."
  PaymentKind.LEASE -> "One lease installment. It counts toward total cost while the lease is open."
  PaymentKind.DOWN_PAYMENT -> "Cash paid up front. It counts toward total cost and toward the payoff."
  PaymentKind.PURCHASE_PRICE -> "Agreed price. Used as the ownership cost only when there is no loan or lease."
}
