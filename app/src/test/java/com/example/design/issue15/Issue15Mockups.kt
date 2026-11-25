package com.example.design.issue15

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.OilBarrel
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.EmptyLogsState
import com.example.FillUpLogCard
import com.example.MiniDashboardGridCard
import com.example.TabSegmentControl
import com.example.VehicleDashboardStats
import com.example.VehicleGarageCard
import com.example.data.maintenance.OilChangeStatus
import com.example.data.maintenance.TireSetDeleteResult
import com.example.data.maintenance.TireSetMileage
import com.example.data.maintenance.formatLastOilChangeMiles
import com.example.data.maintenance.formatOilChangeIndicator
import com.example.data.maintenance.formatTireSetMiles
import com.example.data.maintenance.tireSetDeleteResult
import com.example.data.model.FillUp
import com.example.data.model.Maintenance
import com.example.data.model.MaintenanceType
import com.example.data.model.Vehicle
import com.example.ui.MaintenanceLogCard
import com.example.ui.ServiceLogsContent
import com.example.ui.viewmodel.VehicleStats
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val BoardCanvas = Color(0xFFE7E0DE)

enum class Issue15Vehicle { Camry, Outback }

enum class ServiceLogBody { Current, OptionA, OptionB }

enum class DashboardBody { CollapsedReal, ExpandedPlain, ExpandedTireCards, ExpandedTireRows }

data class Issue15Scenario(
  val vehicle: Issue15Vehicle = Issue15Vehicle.Camry,
  val serviceLog: ServiceLogBody = ServiceLogBody.OptionA,
  val dashboard: DashboardBody = DashboardBody.CollapsedReal,
  val typeFilter: MaintenanceType? = null,
  val menuOpen: Boolean = false,
  val deleteBlocked: Boolean = false
)

private enum class TireBlock { None, Cards, Rows }

private data class Slice(
  val vehicle: Vehicle,
  val maintenance: List<Maintenance>,
  val tireMileage: List<TireSetMileage>,
  val mountedTireSetId: Int?,
  val stats: VehicleStats,
  val oilLogged: Boolean,
  val oilLabel: String,
  val lastOilMiles: String
)

private fun slice(data: Issue15Data, vehicle: Issue15Vehicle): Slice {
  if (vehicle == Issue15Vehicle.Outback) {
    val none = OilChangeStatus.None
    return Slice(
      vehicle = data.outback,
      maintenance = emptyList(),
      tireMileage = emptyList(),
      mountedTireSetId = null,
      stats = VehicleStats(),
      oilLogged = false,
      oilLabel = formatOilChangeIndicator(none),
      lastOilMiles = formatLastOilChangeMiles(none)
    )
  }
  return Slice(
    vehicle = data.camry,
    maintenance = data.maintenance,
    tireMileage = data.tireMileage,
    mountedTireSetId = data.mountedTireSetId,
    stats = data.stats,
    oilLogged = data.oilStatus is OilChangeStatus.MilesAgo,
    oilLabel = data.oilLabel,
    lastOilMiles = data.lastOilMiles
  )
}

@Composable
fun Issue15Screen(scenario: Issue15Scenario, data: Issue15Data = remember { buildIssue15Data() }) {
  val slice = slice(data, scenario.vehicle)
  Scaffold(
    topBar = { TrackerTopBar() },
    floatingActionButton = { AddMaintenanceFab() },
    containerColor = MaterialTheme.colorScheme.background
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp)
    ) {
      GarageRow(vehicles = listOf(data.outback, data.camry), selectedId = slice.vehicle.id)
      when (scenario.dashboard) {
        DashboardBody.CollapsedReal -> VehicleDashboardStats(
          stats = slice.stats,
          lastOilChangeMiles = slice.lastOilMiles,
          onStatsClick = {}
        )
        DashboardBody.ExpandedPlain -> ProposedDashboard(slice, TireBlock.None)
        DashboardBody.ExpandedTireCards -> ProposedDashboard(slice, TireBlock.Cards)
        DashboardBody.ExpandedTireRows -> ProposedDashboard(slice, TireBlock.Rows)
      }
      Spacer(modifier = Modifier.height(16.dp))
      TabSegmentControl(selectedTab = 1, onTabSelected = {})
      Spacer(modifier = Modifier.height(12.dp))
      Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        when (scenario.serviceLog) {
          ServiceLogBody.Current -> ServiceLogsContent(
            vehicleId = slice.vehicle.id,
            maintenance = slice.maintenance,
            tireSetMileage = slice.tireMileage,
            mountedTireSetId = slice.mountedTireSetId,
            oilChangeLabel = slice.oilLabel,
            oilChangeLogged = slice.oilLogged,
            onEditMaintenance = {},
            onAddTireSet = {},
            onEditTireSet = {}
          )
          ServiceLogBody.OptionA -> ProposedServiceLogs(
            slice = slice,
            typeFilter = scenario.typeFilter,
            menuOpen = scenario.menuOpen,
            compactMenu = false,
            keepOilLineWhenLogged = true
          )
          ServiceLogBody.OptionB -> ProposedServiceLogs(
            slice = slice,
            typeFilter = scenario.typeFilter,
            menuOpen = scenario.menuOpen,
            compactMenu = true,
            keepOilLineWhenLogged = false
          )
        }
      }
    }
  }
  if (scenario.deleteBlocked) {
    val blocked = tireSetDeleteResult(referenceCount = 1, name = "Winter") as TireSetDeleteResult.Blocked
    AlertDialog(
      onDismissRequest = {},
      modifier = Modifier.testTag("tire_delete_blocked"),
      title = { Text("Can't delete this tire set") },
      text = { Text(blocked.message) },
      confirmButton = {
        TextButton(onClick = {}) { Text("OK") }
      }
    )
  }
}

@Composable
fun ServiceLogComparisonBoard() {
  val data = remember { buildIssue15Data() }
  Issue15ThreeUp(
    title = "Service Logs — shorter cards and a type menu",
    subtitle = "Tire sets leave this list. Cards match a fill-up. The oil line is the open choice.",
    panes = listOf(
      Issue15Pane("BEFORE\nChips, tire sets, and tall cards") {
        Issue15Screen(Issue15Scenario(serviceLog = ServiceLogBody.Current), data)
      },
      Issue15Pane("OPTION A\nFull-width type menu. Keep the miles line") {
        Issue15Screen(Issue15Scenario(serviceLog = ServiceLogBody.OptionA), data)
      },
      Issue15Pane("OPTION B\nCompact menu, like CSV. Drop the oil line") {
        Issue15Screen(Issue15Scenario(serviceLog = ServiceLogBody.OptionB), data)
      }
    )
  )
}

@Composable
fun EmptyServiceLogComparisonBoard() {
  val data = remember { buildIssue15Data() }
  Issue15ThreeUp(
    title = "No oil change and no service records",
    subtitle = "Remove the \"No oil change logged\" line. The type menu stays either way.",
    panes = listOf(
      Issue15Pane("BEFORE\n\"No oil change logged\" under the title") {
        Issue15Screen(
          Issue15Scenario(vehicle = Issue15Vehicle.Outback, serviceLog = ServiceLogBody.Current),
          data
        )
      },
      Issue15Pane("OPTION A\nThat line is gone. Full-width type menu") {
        Issue15Screen(
          Issue15Scenario(vehicle = Issue15Vehicle.Outback, serviceLog = ServiceLogBody.OptionA),
          data
        )
      },
      Issue15Pane("OPTION B\nThat line is gone. Compact type menu") {
        Issue15Screen(
          Issue15Scenario(vehicle = Issue15Vehicle.Outback, serviceLog = ServiceLogBody.OptionB),
          data
        )
      }
    )
  )
}

@Composable
fun TireSetComparisonBoard() {
  val data = remember { buildIssue15Data() }
  Issue15ThreeUp(
    title = "Tire sets move under Service Repairs",
    subtitle = "On the expanded vehicle card, below Gas Costs and Service Repairs.",
    panes = listOf(
      Issue15Pane("BEFORE\nThe card ends at Service Repairs") {
        Issue15Screen(
          Issue15Scenario(serviceLog = ServiceLogBody.Current, dashboard = DashboardBody.ExpandedPlain),
          data
        )
      },
      Issue15Pane("OPTION A\nToday's tire cards, in the new spot") {
        Issue15Screen(
          Issue15Scenario(serviceLog = ServiceLogBody.OptionA, dashboard = DashboardBody.ExpandedTireCards),
          data
        )
      },
      Issue15Pane("OPTION B\nFull-width rows with Edit and Add set") {
        Issue15Screen(
          Issue15Scenario(serviceLog = ServiceLogBody.OptionA, dashboard = DashboardBody.ExpandedTireRows),
          data
        )
      }
    )
  )
}

@Composable
fun EmptyTireSetComparisonBoard() {
  val data = remember { buildIssue15Data() }
  Issue15ThreeUp(
    title = "No tire sets yet",
    subtitle = "The empty tire-set state moves with them, off the service list.",
    panes = listOf(
      Issue15Pane("BEFORE\n\"No tire sets yet\" sits above the list") {
        Issue15Screen(
          Issue15Scenario(
            vehicle = Issue15Vehicle.Outback,
            serviceLog = ServiceLogBody.Current,
            dashboard = DashboardBody.ExpandedPlain
          ),
          data
        )
      },
      Issue15Pane("OPTION A\nSame message, under Service Repairs") {
        Issue15Screen(
          Issue15Scenario(
            vehicle = Issue15Vehicle.Outback,
            serviceLog = ServiceLogBody.OptionA,
            dashboard = DashboardBody.ExpandedTireCards
          ),
          data
        )
      },
      Issue15Pane("OPTION B\nAdd set is a full-width button") {
        Issue15Screen(
          Issue15Scenario(
            vehicle = Issue15Vehicle.Outback,
            serviceLog = ServiceLogBody.OptionA,
            dashboard = DashboardBody.ExpandedTireRows
          ),
          data
        )
      }
    )
  )
}

@Composable
fun CardHeightBoard() {
  val data = remember { buildIssue15Data() }
  val names = remember(data) { data.tireMileage.associate { it.tireSet.id to it.tireSet.name } }
  val samples = listOf(
    data.maintenance.first { it.title == "Brake pads" },
    data.maintenance.first { it.title == "Swapped to winter tires" },
    data.maintenance.first { it.title == "Oil change" }
  )
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(BoardCanvas)
      .padding(10.dp)
  ) {
    BoardHeader(
      title = "Maintenance cards at the same height as a fill-up",
      subtitle = "Green icon, green cost, and green type stay. The numbers are measured heights."
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      CardColumn(
        tag = "current",
        title = "CURRENT",
        explanation = "The badge, description, and footer each add height.",
        fillUp = data.referenceFillUp,
        samples = samples,
        names = names,
        proposed = null
      )
      CardColumn(
        tag = "a",
        title = "OPTION A",
        explanation = "One detail line. The type stays green, without the chip.",
        fillUp = data.referenceFillUp,
        samples = samples,
        names = names,
        proposed = ProposedCardStyle.MetaLine
      )
      CardColumn(
        tag = "b",
        title = "OPTION B",
        explanation = "The chip stays. The description leads the detail line.",
        fillUp = data.referenceFillUp,
        samples = samples,
        names = names,
        proposed = ProposedCardStyle.BadgeAndDescription
      )
    }
  }
}

private enum class ProposedCardStyle { MetaLine, BadgeAndDescription }

@Composable
private fun CardColumn(
  tag: String,
  title: String,
  explanation: String,
  fillUp: FillUp,
  samples: List<Maintenance>,
  names: Map<Int, String>,
  proposed: ProposedCardStyle?
) {
  Column(modifier = Modifier.width(411.dp).padding(horizontal = 16.dp)) {
    Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
    Text(
      text = explanation,
      minLines = 2,
      maxLines = 2,
      fontSize = 12.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
    )
    SectionLabel("FILL-UP")
    Measured(tag = "h_${tag}_fill") { FillUpLogCard(fillUp = fillUp, onEditClick = {}) }
    SectionLabel("MAINTENANCE")
    samples.forEach { record ->
      val slug = when (record.title) {
        "Brake pads" -> "brake"
        "Swapped to winter tires" -> "swap"
        else -> "oil"
      }
      Measured(tag = "h_${tag}_$slug") {
        if (proposed == null) {
          MaintenanceLogCard(
            maintenance = record,
            tireSetName = record.tireSetId?.let { names[it] },
            onEditClick = {}
          )
        } else {
          CompactMaintenanceCard(
            maintenance = record,
            tireSetName = record.tireSetId?.let { names[it] },
            style = proposed
          )
        }
      }
    }
  }
}

@Composable
private fun SectionLabel(text: String) {
  Text(
    text = text,
    fontSize = 10.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 1.sp,
    color = MaterialTheme.colorScheme.outline,
    modifier = Modifier.padding(bottom = 4.dp)
  )
}

@Composable
private fun Measured(tag: String, content: @Composable () -> Unit) {
  var px by remember { mutableIntStateOf(0) }
  Column(modifier = Modifier.testTag(tag)) {
    Box(modifier = Modifier.onGloballyPositioned { px = it.size.height }) { content() }
    Text(
      text = "$px px",
      modifier = Modifier
        .testTag("${tag}_label")
        .padding(top = 2.dp, bottom = 8.dp),
      fontSize = 11.sp,
      fontWeight = FontWeight.Medium,
      color = MaterialTheme.colorScheme.outline
    )
  }
}

data class Issue15Pane(val label: String, val content: @Composable () -> Unit)

@Composable
fun Issue15ThreeUp(title: String, subtitle: String, panes: List<Issue15Pane>) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(BoardCanvas)
      .padding(10.dp)
  ) {
    BoardHeader(title = title, subtitle = subtitle)
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      panes.forEach { pane ->
        Column(modifier = Modifier.width(411.dp)) {
          Text(
            text = pane.label,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(modifier = Modifier.height(6.dp))
          Box(
            modifier = Modifier
              .width(411.dp)
              .height(914.dp)
              .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f))
          ) {
            pane.content()
          }
        }
      }
    }
  }
}

@Composable
private fun BoardHeader(title: String, subtitle: String) {
  Column(modifier = Modifier.height(88.dp).fillMaxWidth()) {
    Text(
      text = "IOTA VEHICLE TRACKER  ·  ISSUE 15",
      color = MaterialTheme.colorScheme.primary,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 1.1.sp
    )
    Text(
      text = title,
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onBackground,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(top = 2.dp)
    )
    Text(
      text = subtitle,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(top = 2.dp)
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
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
          contentDescription = "Car Icon",
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
private fun AddMaintenanceFab() {
  FloatingActionButton(
    onClick = {},
    containerColor = MaterialTheme.colorScheme.primary,
    contentColor = MaterialTheme.colorScheme.onPrimary,
    modifier = Modifier.testTag("add_log_fab")
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Icon")
      Text(text = "Add Maintenance", fontWeight = FontWeight.SemiBold)
    }
  }
}

@Composable
private fun GarageRow(vehicles: List<Vehicle>, selectedId: Int) {
  Text(
    text = "Your Garage",
    style = MaterialTheme.typography.titleMedium,
    fontWeight = FontWeight.Bold,
    modifier = Modifier.padding(bottom = 8.dp)
  )
  LazyRow(
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 16.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    items(vehicles, key = { it.id }) { vehicle ->
      VehicleGarageCard(
        vehicle = vehicle,
        isSelected = vehicle.id == selectedId,
        onClick = {},
        onDelete = {}
      )
    }
    item { AddVehicleCard() }
  }
}

@Composable
private fun AddVehicleCard() {
  Card(
    modifier = Modifier
      .height(84.dp)
      .width(130.dp)
      .clip(RoundedCornerShape(16.dp)),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Filled.Add,
          contentDescription = "Add Vehicle Logo",
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp)
        )
      }
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "Add Vehicle",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
      )
    }
  }
}

@Composable
private fun ProposedServiceLogs(
  slice: Slice,
  typeFilter: MaintenanceType?,
  menuOpen: Boolean,
  compactMenu: Boolean,
  keepOilLineWhenLogged: Boolean
) {
  var selected by remember { mutableStateOf(typeFilter) }
  var expanded by remember { mutableStateOf(menuOpen) }
  val names = remember(slice.tireMileage) { slice.tireMileage.associate { it.tireSet.id to it.tireSet.name } }
  val visible = if (selected == null) slice.maintenance else slice.maintenance.filter { it.type == selected }
  Column(modifier = Modifier.fillMaxSize()) {
    if (compactMenu) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Service Logs",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
        CompactTypeMenu(
          selected = selected,
          expanded = expanded,
          onExpanded = { expanded = it },
          onSelect = { selected = it }
        )
      }
    } else {
      Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Text(
          text = "Service Logs",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
        if (keepOilLineWhenLogged && slice.oilLogged) {
          Text(
            text = slice.oilLabel,
            modifier = Modifier.padding(top = 2.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }
      ExposedTypeMenu(
        selected = selected,
        expanded = expanded,
        onExpanded = { expanded = it },
        onSelect = { selected = it }
      )
      Spacer(modifier = Modifier.height(8.dp))
    }
    if (visible.isEmpty()) {
      EmptyLogsState(
        title = if (slice.maintenance.isEmpty()) "No Service Records" else "No matching records",
        subtitle = if (slice.maintenance.isEmpty()) {
          "Tap '+ Add Maintenance' below to record an oil change, repair, tire rotation, or tire swap."
        } else {
          "No service records match this type."
        },
        icon = Icons.Outlined.Build
      )
    } else {
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 80.dp),
        modifier = Modifier.weight(1f).fillMaxWidth()
      ) {
        items(visible, key = { it.id }) { record ->
          CompactMaintenanceCard(
            maintenance = record,
            tireSetName = record.tireSetId?.let { names[it] },
            style = ProposedCardStyle.MetaLine
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExposedTypeMenu(
  selected: MaintenanceType?,
  expanded: Boolean,
  onExpanded: (Boolean) -> Unit,
  onSelect: (MaintenanceType?) -> Unit
) {
  ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpanded) {
    OutlinedTextField(
      value = selected?.label ?: "All",
      onValueChange = {},
      readOnly = true,
      singleLine = true,
      label = { Text("Type") },
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
      textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
      modifier = Modifier
        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
        .fillMaxWidth()
        .testTag("type_exposed_field")
    )
    ExposedDropdownMenu(
      expanded = expanded,
      onDismissRequest = { onExpanded(false) },
      modifier = Modifier.testTag("type_menu")
    ) {
      TypeMenuItems(selected = selected, onSelect = {
        onSelect(it)
        onExpanded(false)
      })
    }
  }
}

@Composable
private fun CompactTypeMenu(
  selected: MaintenanceType?,
  expanded: Boolean,
  onExpanded: (Boolean) -> Unit,
  onSelect: (MaintenanceType?) -> Unit
) {
  Box {
    OutlinedButton(
      onClick = { onExpanded(true) },
      shape = RoundedCornerShape(8.dp),
      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
      modifier = Modifier
        .height(34.dp)
        .testTag("type_compact_button")
    ) {
      Text(
        text = selected?.label ?: "All types",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold
      )
      Icon(
        imageVector = Icons.Filled.ArrowDropDown,
        contentDescription = "Open type menu",
        modifier = Modifier.size(18.dp)
      )
    }
    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { onExpanded(false) },
      modifier = Modifier.testTag("type_menu")
    ) {
      TypeMenuItems(selected = selected, onSelect = {
        onSelect(it)
        onExpanded(false)
      })
    }
  }
}

@Composable
private fun TypeMenuItems(selected: MaintenanceType?, onSelect: (MaintenanceType?) -> Unit) {
  TypeMenuItem(label = "All", selected = selected == null, onClick = { onSelect(null) })
  MaintenanceType.entries.forEach { type ->
    TypeMenuItem(label = type.label, selected = selected == type, onClick = { onSelect(type) })
  }
}

@Composable
private fun TypeMenuItem(label: String, selected: Boolean, onClick: () -> Unit) {
  DropdownMenuItem(
    text = {
      Text(text = label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    },
    onClick = onClick,
    leadingIcon = {
      if (selected) {
        Icon(
          imageVector = Icons.Filled.Check,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary
        )
      } else {
        Spacer(modifier = Modifier.size(24.dp))
      }
    },
    modifier = Modifier.testTag("type_option_$label")
  )
}

@Composable
private fun CompactMaintenanceCard(
  maintenance: Maintenance,
  tireSetName: String?,
  style: ProposedCardStyle
) {
  val date = formatMdY(maintenance.date)
  val odo = "Odo: ${String.format(Locale.getDefault(), "%,.0f", maintenance.odometer)}"
  val hours = maintenance.hours?.let { String.format(Locale.getDefault(), "%.1f hrs", it) }
  val tail = listOfNotNull(date, odo, hours, tireSetName).joinToString(" · ")
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
          .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = maintenance.type.icon(),
          contentDescription = null,
          tint = MaterialTheme.colorScheme.secondary,
          modifier = Modifier.size(26.dp)
        )
      }
      Spacer(modifier = Modifier.width(14.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (style == ProposedCardStyle.BadgeAndDescription) {
            Row(
              modifier = Modifier.weight(1f),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = maintenance.title,
                modifier = Modifier.weight(1f, fill = false),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
              )
              Spacer(modifier = Modifier.width(6.dp))
              TypeBadge(maintenance.type.label)
            }
          } else {
            Text(
              text = maintenance.title,
              modifier = Modifier.weight(1f),
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = formatCurrency(maintenance.cost),
            fontWeight = FontWeight.Black,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1,
            softWrap = false
          )
          TextButton(
            onClick = {},
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
          ) {
            Text(text = "Edit", fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }
        }
        Spacer(modifier = Modifier.height(4.dp))
        if (style == ProposedCardStyle.MetaLine) {
          val line = buildAnnotatedString {
            withStyle(
              SpanStyle(
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
              )
            ) {
              append(maintenance.type.label)
            }
            withStyle(
              SpanStyle(
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.Medium
              )
            ) {
              append(" · ")
              append(tail)
            }
          }
          Text(text = line, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        } else {
          val description = maintenance.description?.takeIf { it.isNotBlank() }
          Text(
            text = if (description != null) "$description · $tail" else tail,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.outline,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }
  }
}

@Composable
private fun TypeBadge(label: String) {
  Text(
    text = label,
    fontSize = 10.sp,
    fontWeight = FontWeight.Bold,
    color = MaterialTheme.colorScheme.secondary,
    maxLines = 1,
    softWrap = false,
    modifier = Modifier
      .clip(RoundedCornerShape(6.dp))
      .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
      .padding(horizontal = 6.dp, vertical = 2.dp)
  )
}

@Composable
private fun ProposedDashboard(slice: Slice, tireBlock: TireBlock) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
    shape = RoundedCornerShape(28.dp)
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom
      ) {
        Column(
          modifier = Modifier
            .weight(1f)
            .semantics { contentDescription = "Show fewer stats" }
        ) {
          DashboardStatLabel("Efficiency")
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
          ) {
            Icon(
              imageVector = Icons.Filled.ChevronRight,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier
                .size(28.dp)
                .rotate(90f)
            )
            Text(
              text = if (slice.stats.averageMpg > 0) {
                String.format(Locale.getDefault(), "%.1f", slice.stats.averageMpg)
              } else {
                "--"
              },
              fontSize = 38.sp,
              fontWeight = FontWeight.Black,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              softWrap = false
            )
            DashboardStatUnit("MPG")
          }
        }
        Column(modifier = Modifier.weight(1f)) {
          DashboardStatLabel("Last Oil Change")
          Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom
          ) {
            Text(
              text = slice.lastOilMiles,
              fontSize = oilMilesFontSize(slice.lastOilMiles),
              fontWeight = FontWeight.Black,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              softWrap = false
            )
            if (slice.lastOilMiles != "\u2014") {
              DashboardStatUnit("Miles")
            }
          }
        }
      }
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          Box(
            modifier = Modifier
              .size(48.dp)
              .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Filled.Insights,
              contentDescription = "Stats Icon",
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(26.dp)
            )
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          MiniDashboardGridCard(
            title = "Total Expenditures",
            value = formatCurrency(slice.stats.overallTotalSpent),
            modifier = Modifier.weight(1f)
          )
          MiniDashboardGridCard(
            title = "Current Mileage",
            value = if (slice.stats.latestOdometer > 0) {
              String.format(Locale.getDefault(), "%,.0f mi", slice.stats.latestOdometer)
            } else {
              "-- mi"
            },
            modifier = Modifier.weight(1f)
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          MiniDashboardGridCard(
            title = "Gas Costs",
            value = formatCurrency(slice.stats.totalFuelSpent),
            modifier = Modifier.weight(1f)
          )
          MiniDashboardGridCard(
            title = "Service Repairs",
            value = formatCurrency(slice.stats.totalMaintenanceCost),
            modifier = Modifier.weight(1f)
          )
        }
        if (tireBlock != TireBlock.None) {
          TireSetBlock(mileage = slice.tireMileage, mountedId = slice.mountedTireSetId, rows = tireBlock == TireBlock.Rows)
        }
      }
    }
  }
}

@Composable
private fun TireSetBlock(mileage: List<TireSetMileage>, mountedId: Int?, rows: Boolean) {
  Column(modifier = Modifier.fillMaxWidth()) {
    if (rows) {
      Text(text = "Tire sets", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
      Spacer(modifier = Modifier.height(8.dp))
    } else {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(text = "Tire sets", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        AddSetButton()
      }
    }
    if (mileage.isEmpty()) {
      Text(
        text = "No tire sets yet.",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(bottom = 4.dp)
      )
    } else if (rows) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        mileage.forEach { item ->
          TireSetRow(item = item, mounted = item.tireSet.id == mountedId)
        }
      }
    } else {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        mileage.forEach { item ->
          TireSetCard(item = item, mounted = item.tireSet.id == mountedId)
        }
      }
    }
    if (rows) {
      Spacer(modifier = Modifier.height(8.dp))
      OutlinedButton(
        onClick = {},
        modifier = Modifier.fillMaxWidth().height(40.dp),
        shape = RoundedCornerShape(10.dp)
      ) {
        Icon(
          imageVector = Icons.Outlined.SwapHoriz,
          contentDescription = null,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Add set", fontWeight = FontWeight.SemiBold)
      }
    }
  }
}

@Composable
private fun AddSetButton() {
  TextButton(onClick = {}) {
    Icon(
      imageVector = Icons.Outlined.SwapHoriz,
      contentDescription = null,
      modifier = Modifier.size(16.dp)
    )
    Spacer(modifier = Modifier.width(4.dp))
    Text("Add set", fontWeight = FontWeight.SemiBold)
  }
}

@Composable
private fun TireSetCard(item: TireSetMileage, mounted: Boolean) {
  Card(
    modifier = Modifier
      .width(148.dp)
      .clickable {},
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
    shape = RoundedCornerShape(12.dp)
  ) {
    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
      Text(
        text = item.tireSet.name,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = formatTireSetMiles(item.miles),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
      )
      if (mounted) {
        Text(
          text = "Mounted",
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
      }
    }
  }
}

@Composable
private fun TireSetRow(item: TireSetMileage, mounted: Boolean) {
  val detail = listOfNotNull(item.tireSet.brand, item.tireSet.size).joinToString(" · ")
  Card(
    modifier = Modifier.fillMaxWidth().clickable {},
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
    shape = RoundedCornerShape(12.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = item.tireSet.name,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          if (mounted) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Mounted",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = formatTireSetMiles(item.miles),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
          )
          if (detail.isNotBlank()) {
            Text(
              text = "  ·  $detail",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.outline,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }
      Text(
        text = "Edit",
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .clickable {}
          .padding(horizontal = 8.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp
      )
    }
  }
}

@Composable
private fun DashboardStatLabel(text: String) {
  Text(
    text = text.uppercase(),
    style = MaterialTheme.typography.labelSmall,
    fontWeight = FontWeight.Bold,
    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
    letterSpacing = 1.5.sp,
    maxLines = 2,
    overflow = TextOverflow.Ellipsis
  )
}

@Composable
private fun DashboardStatUnit(text: String) {
  Text(
    text = text.uppercase(),
    fontSize = 14.sp,
    fontWeight = FontWeight.Bold,
    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
    maxLines = 1,
    softWrap = false,
    modifier = Modifier.padding(bottom = 6.dp)
  )
}

private fun oilMilesFontSize(value: String) = when {
  value.length >= 7 -> 24.sp
  value.length >= 6 -> 28.sp
  else -> 38.sp
}

private fun MaintenanceType.icon(): ImageVector = when (this) {
  MaintenanceType.OIL_CHANGE -> Icons.Outlined.OilBarrel
  MaintenanceType.REPAIR -> Icons.Outlined.Build
  MaintenanceType.TIRE_ROTATION -> Icons.Outlined.Autorenew
  MaintenanceType.TIRE_SWAP -> Icons.Outlined.SwapHoriz
  MaintenanceType.OTHER -> Icons.Outlined.MoreHoriz
}

private fun formatMdY(timestamp: Long): String {
  return SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(Date(timestamp))
}

private fun formatCurrency(amount: Double): String {
  return NumberFormat.getCurrencyInstance(Locale.getDefault()).format(amount)
}
