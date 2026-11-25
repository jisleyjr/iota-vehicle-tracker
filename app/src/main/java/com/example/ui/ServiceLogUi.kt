package com.example.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.OilBarrel
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.CustomDialogEnvelope
import com.example.EmptyLogsState
import com.example.data.maintenance.TireSetMileage
import com.example.data.maintenance.formatTireSetMiles
import com.example.data.model.Maintenance
import com.example.data.model.MaintenanceType
import com.example.data.model.TireSet
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class MaintenanceFormResult(
    val type: MaintenanceType,
    val title: String,
    val description: String?,
    val cost: Double,
    val odometer: Double,
    val date: Long,
    val hours: Double?,
    val tireSetId: Int?
)

data class TireSetFormResult(
    val name: String,
    val brand: String?,
    val size: String?,
    val purchaseDate: Long?
)

@Composable
fun ServiceLogsContent(
    vehicleId: Int?,
    maintenance: List<Maintenance>,
    tireSetMileage: List<TireSetMileage>,
    onEditMaintenance: (Maintenance) -> Unit,
    modifier: Modifier = Modifier
) {
    var typeFilter by remember(vehicleId) { mutableStateOf<MaintenanceType?>(null) }
    val listState = rememberLazyListState()
    val visible = if (typeFilter == null) {
        maintenance
    } else {
        maintenance.filter { it.type == typeFilter }
    }
    val tireSetNames = remember(tireSetMileage) {
        tireSetMileage.associate { it.tireSet.id to it.tireSet.name }
    }
    LaunchedEffect(typeFilter, vehicleId) {
        if (visible.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
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
            MaintenanceTypeMenu(
                selected = typeFilter,
                onSelect = { typeFilter = it }
            )
        }

        if (visible.isEmpty()) {
            EmptyLogsState(
                title = if (maintenance.isEmpty()) "No Service Records" else "No matching records",
                subtitle = if (maintenance.isEmpty()) {
                    "Tap '+ Add Maintenance' below to record an oil change, repair, tire rotation, or tire swap."
                } else {
                    "No service records match this type."
                },
                icon = Icons.Outlined.Build
            )
        } else {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("maintenance_list")
            ) {
                items(visible, key = { it.id }) { record ->
                    MaintenanceLogCard(
                        maintenance = record,
                        tireSetName = record.tireSetId?.let { tireSetNames[it] },
                        onEditClick = { onEditMaintenance(record) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MaintenanceTypeMenu(
    selected: MaintenanceType?,
    onSelect: (MaintenanceType?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    androidx.compose.foundation.layout.Box {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
            modifier = Modifier
                .height(34.dp)
                .testTag("maintenance_type_menu")
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
            onDismissRequest = { expanded = false }
        ) {
            MaintenanceTypeMenuItem(
                label = "All",
                selected = selected == null,
                onClick = {
                    onSelect(null)
                    expanded = false
                },
                modifier = Modifier.testTag("maintenance_filter_all")
            )
            MaintenanceType.entries.forEach { type ->
                MaintenanceTypeMenuItem(
                    label = type.label,
                    selected = selected == type,
                    onClick = {
                        onSelect(type)
                        expanded = false
                    },
                    modifier = Modifier.testTag("maintenance_filter_${type.name}")
                )
            }
        }
    }
}

@Composable
private fun MaintenanceTypeMenuItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        },
        onClick = onClick,
        leadingIcon = {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        modifier = modifier
    )
}

@Composable
fun TireSetsDashboardSection(
    mileage: List<TireSetMileage>,
    mountedTireSetId: Int?,
    onAdd: () -> Unit,
    onEdit: (TireSet) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tire_sets_section")
    ) {
        Text(
            text = "Tire sets",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (mileage.isEmpty()) {
            Text(
                text = "No tire sets yet.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                mileage.forEach { item ->
                    TireSetRow(
                        item = item,
                        mounted = item.tireSet.id == mountedTireSetId,
                        onEdit = { onEdit(item.tireSet) }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onAdd,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .testTag("add_tire_set_button"),
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

@Composable
private fun TireSetRow(
    item: TireSetMileage,
    mounted: Boolean,
    onEdit: () -> Unit
) {
    val detail = listOfNotNull(item.tireSet.brand, item.tireSet.size).joinToString(" · ")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onEdit)
            .testTag("tire_set_row_${item.tireSet.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        ),
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
                    .clickable(onClick = onEdit)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("edit_tire_set_${item.tireSet.id}"),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun MaintenanceLogCard(
    maintenance: Maintenance,
    tireSetName: String?,
    onEditClick: () -> Unit
) {
    val formattedDate = remember(maintenance.date) { formatServiceDate(maintenance.date) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("maintenance_card_${maintenance.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
            androidx.compose.foundation.layout.Box(
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
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = maintenance.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = maintenance.type.label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier
                                .testTag("maintenance_badge_${maintenance.id}")
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatServiceCurrency(maintenance.cost),
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        softWrap = false
                    )
                    TextButton(
                        onClick = onEditClick,
                        modifier = Modifier.testTag("edit_maintenance_${maintenance.id}"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "Edit",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                val description = maintenance.description?.takeIf { it.isNotBlank() }
                val odoText = "Odo: ${String.format(Locale.getDefault(), "%,.0f", maintenance.odometer)}"
                val hoursText = maintenance.hours?.let {
                    String.format(Locale.getDefault(), "%.1f hrs", it)
                }
                val detail = listOfNotNull(formattedDate, odoText, hoursText, tireSetName)
                    .joinToString(" · ")
                Text(
                    text = if (description != null) "$description · $detail" else detail,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MaintenanceFormDialog(
    initial: Maintenance?,
    tireSets: List<TireSet>,
    mountedTireSetId: Int?,
    selectTireSetId: Int?,
    onDismiss: () -> Unit,
    onSave: (MaintenanceFormResult) -> Unit,
    onDelete: (() -> Unit)? = null,
    onAddTireSet: () -> Unit,
    initialDateMillis: Long = System.currentTimeMillis()
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var type by remember(initial?.id) { mutableStateOf(initial?.type ?: MaintenanceType.OIL_CHANGE) }
    var title by remember(initial?.id) { mutableStateOf(initial?.title.orEmpty()) }
    var description by remember(initial?.id) { mutableStateOf(initial?.description.orEmpty()) }
    var cost by remember(initial?.id) { mutableStateOf(initial?.cost?.let(::formatEditableNumber).orEmpty()) }
    var odometer by remember(initial?.id) { mutableStateOf(initial?.odometer?.let(::formatEditableNumber).orEmpty()) }
    var hours by remember(initial?.id) { mutableStateOf(initial?.hours?.let(::formatEditableNumber).orEmpty()) }
    var selectedTireSetId by remember(initial?.id) { mutableStateOf(initial?.tireSetId) }
    var dateTimestamp by remember(initial?.id) {
        mutableStateOf(initial?.date ?: startOfLocalDay(initialDateMillis))
    }

    var titleError by remember { mutableStateOf(false) }
    var costError by remember { mutableStateOf(false) }
    var odoError by remember { mutableStateOf(false) }
    var hoursError by remember { mutableStateOf(false) }
    var tireSetError by remember { mutableStateOf(false) }

    LaunchedEffect(selectTireSetId) {
        if (selectTireSetId != null) {
            selectedTireSetId = selectTireSetId
            tireSetError = false
        }
    }

    val needsTireSet = type == MaintenanceType.TIRE_SWAP || type == MaintenanceType.TIRE_ROTATION
    val editing = initial != null
    val context = LocalContext.current

    if (showDeleteConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            modifier = Modifier.testTag("delete_maintenance_confirm"),
            title = { Text("Delete service record?") },
            text = {
                Text("Are you sure you want to delete this service record? This cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = onDelete,
                    modifier = Modifier.testTag("confirm_delete_maintenance"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false },
                    modifier = Modifier.testTag("cancel_delete_maintenance")
                ) {
                    Text("Cancel")
                }
            }
        )
        return
    }

    fun chooseType(newType: MaintenanceType) {
        type = newType
        tireSetError = false
        if (newType != MaintenanceType.TIRE_SWAP && newType != MaintenanceType.TIRE_ROTATION) {
            selectedTireSetId = null
        } else if (selectedTireSetId == null && newType == MaintenanceType.TIRE_ROTATION) {
            selectedTireSetId = mountedTireSetId
        }
    }

    CustomDialogEnvelope(
        title = if (initial == null) "Log Maintenance" else "Edit Maintenance",
        onDismiss = onDismiss,
        onSave = {
            val costVal = cost.toDoubleOrNull()
            val odoVal = odometer.toDoubleOrNull()
            val hoursVal = if (hours.isNotBlank()) hours.toDoubleOrNull() else null
            val trimmedTitle = title.trim()
            titleError = trimmedTitle.isBlank()
            costError = costVal == null || costVal < 0
            odoError = odoVal == null || odoVal < 0
            hoursError = hours.isNotBlank() && (hoursVal == null || hoursVal < 0)
            val knownSet = selectedTireSetId != null && tireSets.any { it.id == selectedTireSetId }
            tireSetError = type == MaintenanceType.TIRE_SWAP && !knownSet
            if (!titleError && !costError && !odoError && !hoursError && !tireSetError) {
                val tireSetId = if (needsTireSet && knownSet) selectedTireSetId else null
                onSave(
                    MaintenanceFormResult(
                        type = type,
                        title = trimmedTitle,
                        description = description.trim().ifBlank { null },
                        cost = costVal!!,
                        odometer = odoVal!!,
                        date = dateTimestamp,
                        hours = hoursVal,
                        tireSetId = tireSetId
                    )
                )
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = if (editing) 440.dp else 560.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Type",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MaintenanceType.entries.forEach { option ->
                    FilterChip(
                        selected = type == option,
                        onClick = { chooseType(option) },
                        label = { Text(option.label) },
                        modifier = Modifier.testTag("service_type_${option.name}")
                    )
                }
            }

            if (needsTireSet) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Tire set", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    text = if (type == MaintenanceType.TIRE_SWAP) {
                        "Choose the set being mounted."
                    } else {
                        "Records the set on the vehicle. Rotating does not change which set is mounted."
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                if (tireSets.isEmpty()) {
                    Text(
                        text = "This vehicle has no tire sets yet.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else {
                    tireSets.forEach { set ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedTireSetId = set.id
                                    tireSetError = false
                                }
                                .testTag("tire_set_option_${set.id}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedTireSetId == set.id,
                                onClick = {
                                    selectedTireSetId = set.id
                                    tireSetError = false
                                }
                            )
                            Column {
                                val mountedLabel = if (set.id == mountedTireSetId) " (mounted)" else ""
                                Text(
                                    text = set.name + mountedLabel,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                val detail = listOfNotNull(set.brand, set.size).joinToString(" · ")
                                if (detail.isNotBlank()) {
                                    Text(
                                        text = detail,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }
                if (tireSetError) {
                    Text(
                        text = "Select the tire set being mounted",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
                TextButton(
                    onClick = onAddTireSet,
                    modifier = Modifier.testTag("maintenance_form_add_tire_set")
                ) {
                    Text("New tire set", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    titleError = false
                },
                label = { Text("Title") },
                isError = titleError,
                supportingText = if (titleError) { { Text("Title is required") } } else null,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("service_title_input")
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optional)") },
                singleLine = false,
                minLines = 2,
                maxLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("service_description_input")
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
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("service_cost_input")
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
                supportingText = if (odoError) { { Text("Enter an odometer of 0 or more") } } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("service_odo_input")
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
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("service_hours_input")
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Date: ${formatServiceDate(dateTimestamp)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = {
                        val calendar = Calendar.getInstance().apply { timeInMillis = dateTimestamp }
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                dateTimestamp = localMidnight(year, month, dayOfMonth)
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Change Date")
                }
            }
        }

            if (editing && onDelete != null) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("delete_maintenance_button"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete service record", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun TireSetFormDialog(
    initial: TireSet?,
    onDismiss: () -> Unit,
    onSave: (TireSetFormResult) -> Unit,
    onDelete: (() -> Unit)?,
    initialDateMillis: Long = System.currentTimeMillis()
) {
    var name by remember(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var brand by remember(initial?.id) { mutableStateOf(initial?.brand.orEmpty()) }
    var size by remember(initial?.id) { mutableStateOf(initial?.size.orEmpty()) }
    var purchaseDate by remember(initial?.id) { mutableStateOf(initial?.purchaseDate) }
    var nameError by remember { mutableStateOf(false) }
    val context = LocalContext.current

    CustomDialogEnvelope(
        title = if (initial == null) "Add Tire Set" else "Edit Tire Set",
        onDismiss = onDismiss,
        onSave = {
            val trimmed = name.trim()
            nameError = trimmed.isBlank()
            if (!nameError) {
                onSave(
                    TireSetFormResult(
                        name = trimmed,
                        brand = brand.trim().ifBlank { null },
                        size = size.trim().ifBlank { null },
                        purchaseDate = purchaseDate
                    )
                )
            }
        },
        saveButtonText = "Save set"
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                nameError = false
            },
            label = { Text("Name (e.g., Summer)") },
            isError = nameError,
            supportingText = if (nameError) { { Text("Name is required") } } else null,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tire_set_name_input")
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = brand,
            onValueChange = { brand = it },
            label = { Text("Brand (optional)") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tire_set_brand_input")
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = size,
            onValueChange = { size = it },
            label = { Text("Size (optional)") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tire_set_size_input")
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = purchaseDate?.let { "Purchased: ${formatServiceDate(it)}" } ?: "Purchase date (optional)",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            if (purchaseDate != null) {
                TextButton(onClick = { purchaseDate = null }) { Text("Clear") }
            }
            TextButton(
                onClick = {
                    val calendar = Calendar.getInstance().apply {
                        timeInMillis = purchaseDate ?: startOfLocalDay(initialDateMillis)
                    }
                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            purchaseDate = localMidnight(year, month, dayOfMonth)
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }
            ) {
                Text(if (purchaseDate == null) "Add date" else "Change")
            }
        }
        if (onDelete != null) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_tire_set_button")
            ) {
                Text(
                    text = "Delete set",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

private fun MaintenanceType.icon(): ImageVector = when (this) {
    MaintenanceType.OIL_CHANGE -> Icons.Outlined.OilBarrel
    MaintenanceType.REPAIR -> Icons.Outlined.Build
    MaintenanceType.TIRE_ROTATION -> Icons.Outlined.Autorenew
    MaintenanceType.TIRE_SWAP -> Icons.Outlined.SwapHoriz
    MaintenanceType.OTHER -> Icons.Outlined.MoreHoriz
}

private fun formatEditableNumber(value: Double): String {
    val asLong = value.toLong()
    return if (value == asLong.toDouble()) asLong.toString() else value.toString()
}

private fun startOfLocalDay(timestamp: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun localMidnight(year: Int, month: Int, dayOfMonth: Int): Long {
    return Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, dayOfMonth)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun formatServiceDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatServiceCurrency(amount: Double): String {
    return NumberFormat.getCurrencyInstance(Locale.getDefault()).format(amount)
}
