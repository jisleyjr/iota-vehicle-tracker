package com.example

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.db.AppDatabase
import com.example.data.model.FillUp
import com.example.data.model.Maintenance
import com.example.data.model.Vehicle
import com.example.data.repository.VehicleRepository
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.VehicleViewModel
import com.example.ui.viewmodel.VehicleStats
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          VehicleTrackerApp()
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleTrackerApp(nowMillis: Long = System.currentTimeMillis()) {
  val context = LocalContext.current
  val db = remember { AppDatabase.getDatabase(context) }
  val repository = remember { VehicleRepository(db.vehicleDao()) }
  val viewModel: VehicleViewModel = viewModel(
    factory = VehicleViewModel.Factory(repository)
  )

  val vehicles by viewModel.allVehicles.collectAsStateWithLifecycle()
  val selectedVehicle by viewModel.selectedVehicle.collectAsStateWithLifecycle()
  val fillUps by viewModel.fillUps.collectAsStateWithLifecycle()
  val maintenanceList by viewModel.maintenanceList.collectAsStateWithLifecycle()
  val stats by viewModel.selectedVehicleStats.collectAsStateWithLifecycle()

  // Dynamic Dialog states
  var showAddVehicle by remember { mutableStateOf(false) }
  var showAddFillUp by remember { mutableStateOf(false) }
  var showAddMaintenance by remember { mutableStateOf(false) }
  var showMpgChart by remember { mutableStateOf(false) }
  var showDeleteVehicleConfirmation by remember { mutableStateOf(false) }
  
  // Tab Selection State
  var activeTab by remember { mutableStateOf(0) } // 0 = Fill-ups, 1 = Maintenance

  val coroutineScope = rememberCoroutineScope()
  val contentResolver = context.contentResolver
  val csvLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri ->
    uri?.let {
      try {
        contentResolver.openInputStream(it)?.use { inputStream ->
          val text = inputStream.bufferedReader().use { reader -> reader.readText() }
          coroutineScope.launch {
            val result = viewModel.importFillUpsFromCsv(text)
            result.fold(
              onSuccess = { count ->
                Toast.makeText(context, "Successfully imported $count fill-ups!", Toast.LENGTH_LONG).show()
              },
              onFailure = { error ->
                Toast.makeText(context, "Import failed: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
              }
            )
          }
        }
      } catch (e: Exception) {
        Toast.makeText(context, "Error reading CSV file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
      }
    }
  }

  Scaffold(
    topBar = {
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
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = Color.Transparent
        )
      )
    },
    floatingActionButton = {
      if (selectedVehicle != null) {
        FloatingActionButton(
          onClick = {
            if (activeTab == 0) showAddFillUp = true else showAddMaintenance = true
          },
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
          modifier = Modifier
            .navigationBarsPadding()
            .testTag("add_log_fab")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = "Add Icon"
            )
            Text(
              text = if (activeTab == 0) "Add Fill-Up" else "Add Maintenance",
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }
    },
    modifier = Modifier.fillMaxSize()
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp)
    ) {
      
      // Horizontal Garage / Vehicles selector card
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
          .padding(bottom = 16.dp)
          .testTag("vehicles_row"),
        verticalAlignment = Alignment.CenterVertically
      ) {
        items(vehicles) { vehicle ->
          val isSelected = selectedVehicle?.id == vehicle.id
          VehicleGarageCard(
            vehicle = vehicle,
            isSelected = isSelected,
            onClick = { viewModel.selectVehicle(vehicle.id) },
            onDelete = {
              viewModel.deleteVehicle(vehicle)
              Toast.makeText(context, "${vehicle.make} deleted", Toast.LENGTH_SHORT).show()
            }
          )
        }
        
        // Add Vehicle Button within Garage row
        item {
          Card(
            modifier = Modifier
              .height(84.dp)
              .width(130.dp)
              .clip(RoundedCornerShape(16.dp))
              .clickable { showAddVehicle = true }
              .testTag("add_vehicle_card"),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            border = androidx.compose.foundation.BorderStroke(
              width = 1.dp,
              color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
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
                  imageVector = Icons.Default.Add,
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
      }

      if (vehicles.isEmpty()) {
        // Welcome and Empty garage state
        EmptyGarageState(onAddClick = { showAddVehicle = true })
      } else {
        selectedVehicle?.let { activeVehicle ->
          // Metrics & Dashboard Header
          Text(
            text = "${activeVehicle.year} ${activeVehicle.make} ${activeVehicle.model} Dashboard",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
          )

          // 2x3 Grid/Column layout for high fidelity diagnostics
          VehicleDashboardStats(
            stats = stats,
            onStatsClick = { showMpgChart = true },
            onDeleteClick = { showDeleteVehicleConfirmation = true }
          )

          Spacer(modifier = Modifier.height(16.dp))

          // Tab layout segment selector
          TabSegmentControl(
            selectedTab = activeTab,
            onTabSelected = { activeTab = it }
          )

          Spacer(modifier = Modifier.height(12.dp))

          // Scrolling Log Contents
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
          ) {
            when (activeTab) {
              0 -> {
                Column(modifier = Modifier.fillMaxSize()) {
                  // Action header bar
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
                    OutlinedButton(
                      onClick = { csvLauncher.launch("*/*") },
                      shape = RoundedCornerShape(8.dp),
                      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                      modifier = Modifier.height(34.dp).testTag("import_csv_button")
                    ) {
                      Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Import CSV Icon",
                        modifier = Modifier.size(16.dp)
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = "Import CSV",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                      )
                    }
                  }

                  if (fillUps.isEmpty()) {
                    Box(modifier = Modifier.weight(1f)) {
                      EmptyLogsState(
                        title = "No Fuel Logs",
                        subtitle = "Tap '+ Add Fill-Up' below or choose 'Import CSV' to load fuel logs.",
                        icon = Icons.Outlined.LocalGasStation
                      )
                    }
                  } else {
                    LazyColumn(
                      verticalArrangement = Arrangement.spacedBy(8.dp),
                      contentPadding = PaddingValues(bottom = 80.dp),
                      modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                      items(fillUps) { fillUp ->
                        FillUpLogCard(
                          fillUp = fillUp,
                          onDeleteClick = {
                            viewModel.deleteFillUp(fillUp)
                            Toast.makeText(context, "Log deleted", Toast.LENGTH_SHORT).show()
                          }
                        )
                      }
                    }
                  }
                }
              }
              1 -> {
                if (maintenanceList.isEmpty()) {
                  EmptyLogsState(
                    title = "No Service Records",
                    subtitle = "Tap '+ Add Maintenance' below to record oil changes, inspections, and other service repairs.",
                    icon = Icons.Outlined.Build
                  )
                } else {
                  LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize()
                  ) {
                    items(maintenanceList) { maintenance ->
                      MaintenanceLogCard(
                        maintenance = maintenance,
                        onDeleteClick = {
                          viewModel.deleteMaintenance(maintenance)
                          Toast.makeText(context, "Record deleted", Toast.LENGTH_SHORT).show()
                        }
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // --- POPUP DIALOGS ---

  if (showAddVehicle) {
    AddVehicleDialog(
      onDismiss = { showAddVehicle = false },
      onSave = { make, model, year, license ->
        viewModel.addVehicle(make, model, year, license)
        showAddVehicle = false
        Toast.makeText(context, "$make $model Added to Garage!", Toast.LENGTH_SHORT).show()
      }
    )
  }

  if (showAddFillUp) {
    AddFillUpDialog(
      onDismiss = { showAddFillUp = false },
      onSave = { tripMiles, gallons, pricePerGallon, odometer, date ->
        viewModel.addFillUp(tripMiles, gallons, pricePerGallon, odometer, date)
        showAddFillUp = false
        Toast.makeText(context, "Fill-up Saved!", Toast.LENGTH_SHORT).show()
      },
      initialDateMillis = nowMillis
    )
  }

  if (showAddMaintenance) {
    AddMaintenanceDialog(
      onDismiss = { showAddMaintenance = false },
      onSave = { description, cost, odometer, date, hours ->
        viewModel.addMaintenance(description, cost, odometer, date, hours)
        showAddMaintenance = false
        Toast.makeText(context, "Servicing logged successfully!", Toast.LENGTH_SHORT).show()
      },
      initialDateMillis = nowMillis
    )
  }

  if (showMpgChart) {
    MpgHistoryChartDialog(
      fillUps = fillUps,
      onDismiss = { showMpgChart = false }
    )
  }

  if (showDeleteVehicleConfirmation) {
    selectedVehicle?.let { vehicle ->
      AlertDialog(
        onDismissRequest = { showDeleteVehicleConfirmation = false },
        title = { Text("Delete Vehicle?") },
        text = { Text("This will permanently remove ${vehicle.year} ${vehicle.make} ${vehicle.model} from your garage along with all logged fill-ups and maintenance history.") },
        confirmButton = {
          Button(
            onClick = {
              viewModel.deleteVehicle(vehicle)
              showDeleteVehicleConfirmation = false
              Toast.makeText(context, "${vehicle.make} deleted", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
          ) {
            Text("Delete")
          }
        },
        dismissButton = {
          TextButton(onClick = { showDeleteVehicleConfirmation = false }) {
            Text("Cancel")
          }
        }
      )
    }
  }
}

// --- SUB-COMPONENTS ---

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VehicleGarageCard(
  vehicle: Vehicle,
  isSelected: Boolean,
  onClick: () -> Unit,
  onDelete: () -> Unit
) {
  var showConfirmDelete by remember { mutableStateOf(false) }

  Card(
    modifier = Modifier
      .height(84.dp)
      .width(185.dp)
      .clip(RoundedCornerShape(16.dp))
      .combinedClickable(
        onClick = onClick,
        onLongClick = { showConfirmDelete = true }
      )
      .testTag("vehicle_card_${vehicle.id}"),
    colors = CardDefaults.cardColors(
      containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
      } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
      }
    ),
    border = if (isSelected) {
      androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
      androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    }
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Top
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "${vehicle.year}".uppercase(),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
            Text(
              text = vehicle.make,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          if (isSelected) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = "Active Indicator",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
          } else {
            IconButton(
              onClick = { showConfirmDelete = true },
              modifier = Modifier.size(24.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Vehicle",
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Bottom
        ) {
          Text(
            text = vehicle.model,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          
          if (vehicle.licensePlate.isNotBlank()) {
            Box(
              modifier = Modifier
                .background(
                  color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                  shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
              Text(
                text = vehicle.licensePlate.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }
  }

  if (showConfirmDelete) {
    AlertDialog(
      onDismissRequest = { showConfirmDelete = false },
      title = { Text("Delete Vehicle?") },
      text = { Text("This will permanently remove ${vehicle.year} ${vehicle.make} ${vehicle.model} from your garage along with all logged fill-ups and maintenance history.") },
      confirmButton = {
        Button(
          onClick = {
            onDelete()
            showConfirmDelete = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { showConfirmDelete = false }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
fun EmptyGarageState(onAddClick: () -> Unit) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 40.dp),
    contentAlignment = Alignment.Center
  ) {
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
      ),
      shape = RoundedCornerShape(24.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Box(
          modifier = Modifier
            .size(72.dp)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.DirectionsCar,
            contentDescription = "Car Silhouette",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(38.dp)
          )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = "Your Garage is Empty",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Add your vehicle details (Make and Model) to start tracking gas mileage, fill-ups, and cost breakdown easily.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.outline,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
          onClick = onAddClick,
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("add_first_vehicle_btn")
        ) {
          Icon(imageVector = Icons.Default.Add, contentDescription = "AddIcon")
          Spacer(modifier = Modifier.width(8.dp))
          Text("Add Your First Vehicle", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
fun VehicleDashboardStats(
  stats: VehicleStats,
  onStatsClick: () -> Unit,
  onDeleteClick: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(28.dp))
      .testTag("dashboard_stats_card"),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant
    ),
    border = androidx.compose.foundation.BorderStroke(
      width = 1.dp,
      color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp)
    ) {
      // Top row: Efficiency overview & main badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Efficiency".uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            letterSpacing = 1.5.sp
          )
          Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Text(
              text = if (stats.averageMpg > 0) String.format(Locale.getDefault(), "%.1f", stats.averageMpg) else "--",
              fontSize = 38.sp,
              fontWeight = FontWeight.Black,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = "MPG",
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
          }
        }

        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(48.dp)
              .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
              .clickable { onDeleteClick() }
              .testTag("delete_vehicle_dashboard_button"),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Delete,
              contentDescription = "Delete Vehicle",
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(24.dp)
            )
          }

          Box(
            modifier = Modifier
              .size(48.dp)
              .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
              .clickable { onStatsClick() }
              .testTag("stats_icon_button"),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Insights,
              contentDescription = "Stats Icon",
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(26.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // 2x2 Grid using clean Rows
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Total Spent Detail
          MiniDashboardGridCard(
            title = "Total Expenditures",
            value = formatCurrency(stats.overallTotalSpent),
            modifier = Modifier.weight(1f)
          )

          // Latest Odometer Record
          MiniDashboardGridCard(
            title = "Current Mileage",
            value = if (stats.latestOdometer > 0) String.format(Locale.getDefault(), "%,.0f mi", stats.latestOdometer) else "-- mi",
            modifier = Modifier.weight(1f)
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Gas component Spent
          MiniDashboardGridCard(
            title = "Gas Costs",
            value = formatCurrency(stats.totalFuelSpent),
            modifier = Modifier.weight(1f)
          )

          // Maintenance service spent
          MiniDashboardGridCard(
            title = "Service Repairs",
            value = formatCurrency(stats.totalMaintenanceCost),
            modifier = Modifier.weight(1f)
          )
        }
      }
    }
  }
}

@Composable
fun MiniDashboardGridCard(
  title: String,
  value: String,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .background(
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp)
      )
      .padding(12.dp)
  ) {
    Column(
      verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      Text(
        text = title.uppercase(),
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        letterSpacing = 0.5.sp
      )
      Text(
        text = value,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
      )
    }
  }
}

@Composable
fun TabSegmentControl(
  selectedTab: Int,
  onTabSelected: (Int) -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .height(52.dp),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ),
    border = androidx.compose.foundation.BorderStroke(
      width = 1.dp,
      color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
    )
  ) {
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(4.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      // Tab 0
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight()
          .clip(RoundedCornerShape(8.dp))
          .background(
            if (selectedTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent
          )
          .clickable { onTabSelected(0) }
          .testTag("gas_tab"),
        contentAlignment = Alignment.Center
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          Icon(
            imageVector = if (selectedTab == 0) Icons.Filled.LocalGasStation else Icons.Outlined.LocalGasStation,
            contentDescription = "Gas Tab Icon",
            tint = if (selectedTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Gas Fill-ups",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (selectedTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      // Tab 1
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight()
          .clip(RoundedCornerShape(8.dp))
          .background(
            if (selectedTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent
          )
          .clickable { onTabSelected(1) }
          .testTag("maintenance_tab"),
        contentAlignment = Alignment.Center
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          Icon(
            imageVector = if (selectedTab == 1) Icons.Filled.Build else Icons.Outlined.Build,
            contentDescription = "Service Tab Icon",
            tint = if (selectedTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Service Logs",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (selectedTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}

@Composable
fun EmptyLogsState(
  title: String,
  subtitle: String,
  icon: ImageVector
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 40.dp, horizontal = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
      modifier = Modifier.size(48.dp)
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = subtitle,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.outline,
      textAlign = TextAlign.Center
    )
  }
}

@Composable
fun FillUpLogCard(
  fillUp: FillUp,
  onDeleteClick: () -> Unit
) {
  val formattedDate = remember(fillUp.date) { formatDate(fillUp.date) }
  val mpg = remember(fillUp.tripMiles, fillUp.gallons) {
    if (fillUp.gallons > 0) fillUp.tripMiles / fillUp.gallons else 0.0
  }

  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    border = androidx.compose.foundation.BorderStroke(
      width = 1.dp,
      color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
    ),
    shape = RoundedCornerShape(12.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Metric Highlight (computed MPG)
      Box(
        modifier = Modifier
          .size(54.dp)
          .background(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            shape = RoundedCornerShape(10.dp)
          ),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = String.format(Locale.getDefault(), "%.1f", mpg),
            fontWeight = FontWeight.Black,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.primary
          )
          Text(
            text = "MPG",
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
          )
        }
      }

      Spacer(modifier = Modifier.width(14.dp))

      // Informational parameters
      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = formattedDate,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "${formatCurrency(fillUp.gallons * fillUp.pricePerGallon)} Total",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        
        Spacer(modifier = Modifier.height(4.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "${fillUp.tripMiles} mi · ${fillUp.gallons} gal @ ${formatCurrency(fillUp.pricePerGallon)}/gal",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.outline,
            fontWeight = FontWeight.Medium
          )
          
          Text(
            text = fillUp.odometer?.let { "Odo: ${String.format(Locale.getDefault(), "%,.0f", it)}" } ?: "Odo: --",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.outline,
            fontWeight = FontWeight.Medium
          )
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      IconButton(
        onClick = onDeleteClick,
        modifier = Modifier.size(36.dp)
      ) {
        Icon(
          imageVector = Icons.Default.DeleteOutline,
          contentDescription = "Delete Log Entry",
          tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
          modifier = Modifier.size(20.dp)
        )
      }
    }
  }
}

@Composable
fun MaintenanceLogCard(
  maintenance: Maintenance,
  onDeleteClick: () -> Unit
) {
  val formattedDate = remember(maintenance.date) { formatDate(maintenance.date) }

  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    border = androidx.compose.foundation.BorderStroke(
      width = 1.dp,
      color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
    ),
    shape = RoundedCornerShape(12.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Cost Badge Icon
      Box(
        modifier = Modifier
          .size(54.dp)
          .background(
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
            shape = RoundedCornerShape(10.dp)
          ),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            imageVector = Icons.Default.BuildCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(24.dp)
          )
          Text(
            text = "SERVICE",
            fontWeight = FontWeight.Bold,
            fontSize = 8.sp,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
          )
        }
      }

      Spacer(modifier = Modifier.width(14.dp))

      // Detailed text
      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = maintenance.whatWasDone,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = formatCurrency(maintenance.cost),
            fontWeight = FontWeight.Black,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.secondary
          )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = formattedDate,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.outline,
            fontWeight = FontWeight.Medium
          )
          val odoText = "Odo: ${String.format(Locale.getDefault(), "%,.0f", maintenance.odometer)}"
          val hoursText = maintenance.hours?.let { " · ${String.format(Locale.getDefault(), "%.1f", it)} hrs" } ?: ""
          Text(
            text = "$odoText$hoursText",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.outline,
            fontWeight = FontWeight.Medium
          )
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      IconButton(
        onClick = onDeleteClick,
        modifier = Modifier.size(36.dp)
      ) {
        Icon(
          imageVector = Icons.Default.DeleteOutline,
          contentDescription = "Delete Record Entry",
          tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
          modifier = Modifier.size(20.dp)
        )
      }
    }
  }
}

// --- POPUP DIALOG IMPLEMENTATIONS ---

@Composable
fun AddVehicleDialog(
  onDismiss: () -> Unit,
  onSave: (String, String, Int, String) -> Unit
) {
  var make by remember { mutableStateOf("") }
  var model by remember { mutableStateOf("") }
  var year by remember { mutableStateOf("") }
  var license by remember { mutableStateOf("") }

  var makeError by remember { mutableStateOf(false) }
  var modelError by remember { mutableStateOf(false) }
  var yearError by remember { mutableStateOf(false) }

  CustomDialogEnvelope(
    title = "Add Vehicle Details",
    onDismiss = onDismiss,
    onSave = {
      makeError = make.isBlank()
      modelError = model.isBlank()
      val yearVal = year.toIntOrNull()
      yearError = yearVal == null || yearVal < 1886 || yearVal > 2100

      if (!makeError && !modelError && !yearError) {
        onSave(make.trim(), model.trim(), year.toInt(), license.trim())
      }
    }
  ) {
    OutlinedTextField(
      value = make,
      onValueChange = {
        make = it
        makeError = false
      },
      label = { Text("Make (e.g., Toyota, Ford)") },
      isError = makeError,
      supportingText = if (makeError) { { Text("Make is required") } } else null,
      singleLine = true,
      modifier = Modifier.fillMaxWidth().testTag("vehicle_make_input")
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
      value = model,
      onValueChange = {
        model = it
        modelError = false
      },
      label = { Text("Model (e.g., Prius, Camry)") },
      isError = modelError,
      supportingText = if (modelError) { { Text("Model is required") } } else null,
      singleLine = true,
      modifier = Modifier.fillMaxWidth().testTag("vehicle_model_input")
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
      value = year,
      onValueChange = {
        year = it
        yearError = false
      },
      label = { Text("Model Year (e.g., 2022)") },
      isError = yearError,
      supportingText = if (yearError) { { Text("Enter a valid year (1886 - 2100)") } } else null,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
      singleLine = true,
      modifier = Modifier.fillMaxWidth().testTag("vehicle_year_input")
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
      value = license,
      onValueChange = { license = it },
      label = { Text("License Plate (Optional)") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth().testTag("vehicle_license_input")
    )
  }
}

@Composable
fun AddFillUpDialog(
  onDismiss: () -> Unit,
  onSave: (Double, Double, Double, Double?, Long) -> Unit,
  initialDateMillis: Long = System.currentTimeMillis()
) {
  var tripMiles by remember { mutableStateOf("") }
  var gallons by remember { mutableStateOf("") }
  var pricePerGallon by remember { mutableStateOf("") }
  var odometer by remember { mutableStateOf("") }
  var dateTimestamp by remember {
    mutableStateOf(
      Calendar.getInstance().apply {
        timeInMillis = initialDateMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }.timeInMillis
    )
  }

  var tripError by remember { mutableStateOf(false) }
  var gallonsError by remember { mutableStateOf(false) }
  var priceError by remember { mutableStateOf(false) }
  var odoError by remember { mutableStateOf(false) }

  val context = LocalContext.current

  CustomDialogEnvelope(
    title = "Log Gas Fill-Up",
    onDismiss = onDismiss,
    onSave = {
      val tripVal = tripMiles.toDoubleOrNull()
      val galsVal = gallons.toDoubleOrNull()
      val priceVal = pricePerGallon.toDoubleOrNull()
      val odoVal = if (odometer.isNotBlank()) odometer.toDoubleOrNull() else null

      tripError = tripVal == null || tripVal <= 0
      gallonsError = galsVal == null || galsVal <= 0
      priceError = priceVal == null || priceVal <= 0
      odoError = odometer.isNotBlank() && (odoVal == null || odoVal < 0)

      if (!tripError && !gallonsError && !priceError && !odoError) {
        onSave(tripVal!!, galsVal!!, priceVal!!, odoVal, dateTimestamp)
      }
    }
  ) {
    OutlinedTextField(
      value = tripMiles,
      onValueChange = {
        tripMiles = it
        tripError = false
      },
      label = { Text("Trip Miles") },
      isError = tripError,
      supportingText = if (tripError) { { Text("Enter a positive number") } } else null,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
      singleLine = true,
      modifier = Modifier.fillMaxWidth().testTag("trip_miles_input")
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
      value = gallons,
      onValueChange = {
        gallons = it
        gallonsError = false
      },
      label = { Text("Gallons Purchased") },
      isError = gallonsError,
      supportingText = if (gallonsError) { { Text("Enter a positive number") } } else null,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
      singleLine = true,
      modifier = Modifier.fillMaxWidth().testTag("gallons_input")
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
      value = pricePerGallon,
      onValueChange = {
        pricePerGallon = it
        priceError = false
      },
      label = { Text("Price per Gallon ($)") },
      isError = priceError,
      supportingText = if (priceError) { { Text("Enter positive dollar value") } } else null,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
      singleLine = true,
      modifier = Modifier.fillMaxWidth().testTag("price_input")
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
      value = odometer,
      onValueChange = {
        odometer = it
        odoError = false
      },
      label = { Text("Odometer Reading (Optional)") },
      isError = odoError,
      supportingText = if (odoError) { { Text("Enter a valid odometer reading") } } else null,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
      singleLine = true,
      modifier = Modifier.fillMaxWidth().testTag("odo_input")
    )

    Spacer(modifier = Modifier.height(14.dp))

    // Interactive Date Selector
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Date: ${formatDate(dateTimestamp)}",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold
      )
      TextButton(
        onClick = {
          val calendar = Calendar.getInstance().apply { timeInMillis = dateTimestamp }
          DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
              val updateCalendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
              }
              dateTimestamp = updateCalendar.timeInMillis
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
          ).show()
        }
      ) {
        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Change Date")
      }
    }
  }
}

@Composable
fun AddMaintenanceDialog(
  onDismiss: () -> Unit,
  onSave: (String, Double, Double, Long, Double?) -> Unit,
  initialDateMillis: Long = System.currentTimeMillis()
) {
  var description by remember { mutableStateOf("") }
  var cost by remember { mutableStateOf("") }
  var odometer by remember { mutableStateOf("") }
  var hours by remember { mutableStateOf("") }
  var dateTimestamp by remember {
    mutableStateOf(
      Calendar.getInstance().apply {
        timeInMillis = initialDateMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }.timeInMillis
    )
  }

  var descError by remember { mutableStateOf(false) }
  var costError by remember { mutableStateOf(false) }
  var odoError by remember { mutableStateOf(false) }
  var hoursError by remember { mutableStateOf(false) }

  val context = LocalContext.current

  CustomDialogEnvelope(
    title = "Log Maintenance",
    onDismiss = onDismiss,
    onSave = {
      val costVal = cost.toDoubleOrNull()
      val odoVal = odometer.toDoubleOrNull()
      val hoursVal = if (hours.isNotBlank()) hours.toDoubleOrNull() else null

      descError = description.isBlank()
      costError = costVal == null || costVal < 0
      odoError = odoVal == null || odoVal < 0
      hoursError = hours.isNotBlank() && (hoursVal == null || hoursVal < 0)

      if (!descError && !costError && !odoError && !hoursError) {
        onSave(description.trim(), costVal!!, odoVal!!, dateTimestamp, hoursVal)
      }
    }
  ) {
    OutlinedTextField(
      value = description,
      onValueChange = {
        description = it
        descError = false
      },
      label = { Text("What was done? (e.g., Oil Change)") },
      isError = descError,
      supportingText = if (descError) { { Text("Maintenance description is required") } } else null,
      singleLine = false,
      maxLines = 3,
      modifier = Modifier.fillMaxWidth().testTag("service_desc_input")
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
      value = cost,
      onValueChange = {
        cost = it
        costError = false
      },
      label = { Text("Cost ($)") },
      isError = costError,
      supportingText = if (costError) { { Text("Enter a valid cost (0 or more)") } } else null,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
      singleLine = true,
      modifier = Modifier.fillMaxWidth().testTag("service_cost_input")
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
      value = odometer,
      onValueChange = {
        odometer = it
        odoError = false
      },
      label = { Text("Odometer Reading") },
      isError = odoError,
      supportingText = if (odoError) { { Text("Enter positive odometer") } } else null,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
      singleLine = true,
      modifier = Modifier.fillMaxWidth().testTag("service_odo_input")
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
      value = hours,
      onValueChange = {
        hours = it
        hoursError = false
      },
      label = { Text("Operating Hours (Optional)") },
      isError = hoursError,
      supportingText = if (hoursError) { { Text("Enter a valid hours value") } } else null,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
      singleLine = true,
      modifier = Modifier.fillMaxWidth().testTag("service_hours_input")
    )

    Spacer(modifier = Modifier.height(14.dp))

    // Interactive Date Selector
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Date: ${formatDate(dateTimestamp)}",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold
      )
      TextButton(
        onClick = {
          val calendar = Calendar.getInstance().apply { timeInMillis = dateTimestamp }
          DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
              val updateCalendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
              }
              dateTimestamp = updateCalendar.timeInMillis
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
          ).show()
        }
      ) {
        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Change Date")
      }
    }
  }
}

// Dialog Wrapper layout for consistent material polish and elevation
@Composable
fun CustomDialogEnvelope(
  title: String,
  onDismiss: () -> Unit,
  onSave: () -> Unit,
  content: @Composable ColumnScope.() -> Unit
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .padding(16.dp)
        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
    ) {
      Column(
        modifier = Modifier
          .padding(20.dp)
          .fillMaxWidth()
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
          modifier = Modifier.fillMaxWidth()
        ) {
          content()
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(onClick = onDismiss) {
            Text("Cancel", fontWeight = FontWeight.SemiBold)
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = onSave,
            shape = RoundedCornerShape(10.dp)
          ) {
            Text("Save details", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

// --- CORE UTILITY HELPERS ---

private fun formatDate(timestamp: Long): String {
  val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
  return sdf.format(Date(timestamp))
}

private fun formatCurrency(amount: Double): String {
  val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
  return format.format(amount)
}

@Composable
fun MpgHistoryChartDialog(
  fillUps: List<FillUp>,
  onDismiss: () -> Unit
) {
  val last30FillUps = remember(fillUps) {
    fillUps.filter { it.gallons > 0 }
      .sortedBy { it.date }
      .takeLast(30)
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .height(500.dp)
        .padding(16.dp),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(
        modifier = Modifier
          .padding(20.dp)
          .fillMaxSize()
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "MPG History (Last 30)",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
          )
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close")
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (last30FillUps.size < 2) {
          Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text("Add more fill-ups to view history chart", textAlign = TextAlign.Center)
          }
        } else {
          MpgLineChart(
            data = last30FillUps,
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
          )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
          text = "Horizontal Axis: Date | Vertical Axis: MPG",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.outline,
          modifier = Modifier.fillMaxWidth(),
          textAlign = TextAlign.Center
        )
      }
    }
  }
}

private fun formatShortDate(timestamp: Long): String {
  val sdf = SimpleDateFormat("MM/dd", Locale.getDefault())
  return sdf.format(Date(timestamp))
}

@Composable
fun MpgLineChart(
  data: List<FillUp>,
  modifier: Modifier = Modifier
) {
  val primaryColor = MaterialTheme.colorScheme.primary
  val secondaryColor = MaterialTheme.colorScheme.secondary
  val outline = MaterialTheme.colorScheme.outline
  val textMeasurer = rememberTextMeasurer()
  val labelStyle = MaterialTheme.typography.labelSmall.copy(
    color = MaterialTheme.colorScheme.onSurface,
    fontSize = 9.sp
  )
  
  Canvas(modifier = modifier.padding(bottom = 40.dp, start = 60.dp, end = 20.dp, top = 20.dp)) {
    val width = size.width
    val height = size.height
    
    val mpgs = data.map { it.tripMiles / it.gallons }
    val dates = data.map { it.date.toDouble() }
    
    val minMpg = mpgs.minOrNull() ?: 0.0
    val maxMpg = mpgs.maxOrNull() ?: 1.0
    val minDate = dates.minOrNull() ?: 0.0
    val maxDate = dates.maxOrNull() ?: 1.0
    
    val mpgRange = (maxMpg - minMpg).coerceAtLeast(1.0)
    val dateRange = (maxDate - minDate).coerceAtLeast(1.0)
    
    // Grid and Axes
    drawLine(
      color = outline,
      start = Offset(0f, 0f),
      end = Offset(0f, height),
      strokeWidth = 2f
    )
    drawLine(
      color = outline,
      start = Offset(0f, height),
      end = Offset(width, height),
      strokeWidth = 2f
    )
    
    // Scale points
    val points = data.map { fillUp ->
      val mpg = fillUp.tripMiles / fillUp.gallons
      // Date on X axis: older (minDate) at left (x=0), newer (maxDate) at right (x=width)
      val x = ((fillUp.date - minDate) / dateRange * width).toFloat()
      // MPG on Y axis: higher MPG at top (y=0), lower MPG at bottom (y=height)
      val y = (height - ((mpg - minMpg) / mpgRange * height)).toFloat()
      Offset(x, y)
    }
    
    // Draw connections
    val path = androidx.compose.ui.graphics.Path().apply {
      if (points.isNotEmpty()) {
        moveTo(points.first().x, points.first().y)
        for (i in 1 until points.size) {
          lineTo(points[i].x, points[i].y)
        }
      }
    }
    
    drawPath(
      path = path,
      color = primaryColor,
      style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
    )
    
    // Draw points and labels
    data.zip(points).forEach { (fillUp, point) ->
      drawCircle(
        color = secondaryColor,
        radius = 6f,
        center = point
      )
      
      // MPG Label on Y axis
      val mpg = fillUp.tripMiles / fillUp.gallons
      val mpgText = String.format(Locale.getDefault(), "%.1f", mpg)
      val mpgResult = textMeasurer.measure(mpgText, labelStyle)
      drawText(
        textLayoutResult = mpgResult,
        topLeft = Offset(-mpgResult.size.width - 15f, point.y - mpgResult.size.height / 2f)
      )
      
      // Date Label on X axis
      val dateText = formatShortDate(fillUp.date)
      val dateResult = textMeasurer.measure(dateText, labelStyle)
      drawText(
        textLayoutResult = dateResult,
        topLeft = Offset(point.x - dateResult.size.width / 2f, height + 8f)
      )
    }
  }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun MpgHistoryChartDialogPreview() {
  MyApplicationTheme {
    val sampleFillUps = listOf(
      FillUp(id = 1, vehicleId = 1, tripMiles = 300.0, gallons = 10.0, pricePerGallon = 3.5, odometer = 1000.0, date = 1717142400000L), // June 1
      FillUp(id = 2, vehicleId = 1, tripMiles = 320.0, gallons = 10.5, pricePerGallon = 3.6, odometer = 1320.0, date = 1717228800000L), // June 2
      FillUp(id = 3, vehicleId = 1, tripMiles = 280.0, gallons = 9.0, pricePerGallon = 3.4, odometer = 1600.0, date = 1717315200000L),  // June 3
      FillUp(id = 4, vehicleId = 1, tripMiles = 350.0, gallons = 11.0, pricePerGallon = 3.7, odometer = 1950.0, date = 1717401600000L)  // June 4
    )
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      MpgHistoryChartDialog(
        fillUps = sampleFillUps,
        onDismiss = {}
      )
    }
  }
}
