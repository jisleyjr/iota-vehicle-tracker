package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.csv.ExistingFillUp
import com.example.data.csv.FillUpCsvParseResult
import com.example.data.csv.FillUpCsvPreview
import com.example.data.csv.FillUpCsvParser
import com.example.data.csv.importResultMessage
import com.example.data.model.Vehicle
import com.example.data.model.FillUp
import com.example.data.model.Maintenance
import com.example.data.repository.VehicleRepository
import com.example.BuildConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ReadyFillUpImport(
    val vehicleId: Int,
    val vehicleLabel: String,
    val preview: FillUpCsvPreview
)

data class ImportCommitResult(
    val imported: Int,
    val duplicatesSkipped: Int
) {
    fun message(): String = importResultMessage(imported, duplicatesSkipped)
}

data class VehicleStats(
    val averageMpg: Double = 0.0,
    val totalFuelSpent: Double = 0.0,
    val totalMaintenanceCost: Double = 0.0,
    val overallTotalSpent: Double = 0.0,
    val latestOdometer: Double = 0.0
)

@OptIn(ExperimentalCoroutinesApi::class)
class VehicleViewModel(private val repository: VehicleRepository) : ViewModel() {

    val allVehicles: StateFlow<List<Vehicle>> = repository.allVehicles
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val selectedVehicleId = MutableStateFlow<Int?>(null)

    val selectedVehicle: StateFlow<Vehicle?> = combine(allVehicles, selectedVehicleId) { vehicles, id ->
        if (id != null) {
            vehicles.find { it.id == id }
        } else {
            vehicles.firstOrNull()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Automatically synchronize selectedVehicleId if selectedVehicle gets mutated or initialized
    init {
        viewModelScope.launch {
            // Wait for the first actual emission from the database to avoid race condition with StateFlow initialValue
            val currentVehicles = repository.allVehicles.first()
            if (currentVehicles.isEmpty() && BuildConfig.DEBUG) {
                addVehicle("Toyota", "Prius", 2022, "DEV-MODE")
            }
        }

        viewModelScope.launch {
            allVehicles.collect { vehicles ->
                if (vehicles.isNotEmpty() && selectedVehicleId.value == null) {
                    selectedVehicleId.value = vehicles.first().id
                }
            }
        }
    }

    val fillUps: StateFlow<List<FillUp>> = selectedVehicleId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getFillUpsForVehicle(id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val maintenanceList: StateFlow<List<Maintenance>> = selectedVehicleId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getMaintenanceForVehicle(id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Deriving high-fidelity statistics in real-time
    val selectedVehicleStats: StateFlow<VehicleStats> = combine(fillUps, maintenanceList) { fills, maints ->
        val totalMiles = fills.sumOf { it.tripMiles }
        val totalGallons = fills.sumOf { it.gallons }
        val avgMpg = if (totalGallons > 0) totalMiles / totalGallons else 0.0

        val fuelSpent = fills.sumOf { it.gallons * it.pricePerGallon }
        val maintenanceSpent = maints.sumOf { it.cost }

        val maxOdometerFromFills = fills.mapNotNull { it.odometer }.maxOrNull() ?: 0.0
        val maxOdometerFromMaint = maints.maxOfOrNull { it.odometer } ?: 0.0
        val latestOdo = maxOf(maxOdometerFromFills, maxOdometerFromMaint)

        VehicleStats(
            averageMpg = avgMpg,
            totalFuelSpent = fuelSpent,
            totalMaintenanceCost = maintenanceSpent,
            overallTotalSpent = fuelSpent + maintenanceSpent,
            latestOdometer = latestOdo
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VehicleStats())

    fun selectVehicle(vehicleId: Int) {
        selectedVehicleId.value = vehicleId
    }

    fun addVehicle(make: String, model: String, year: Int, licensePlate: String, onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val newlyCreatedId = repository.insertVehicle(
                Vehicle(make = make, model = model, year = year, licensePlate = licensePlate)
            )
            selectedVehicleId.value = newlyCreatedId.toInt()
            onComplete(newlyCreatedId.toInt())
        }
    }

    fun deleteVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            repository.deleteVehicle(vehicle)
            if (selectedVehicleId.value == vehicle.id) {
                selectedVehicleId.value = allVehicles.value.firstOrNull { it.id != vehicle.id }?.id
            }
        }
    }

    fun addFillUp(tripMiles: Double, gallons: Double, pricePerGallon: Double, odometer: Double?, date: Long) {
        val vehicleId = selectedVehicleId.value ?: return
        viewModelScope.launch {
            repository.insertFillUp(
                FillUp(
                    vehicleId = vehicleId,
                    tripMiles = tripMiles,
                    gallons = gallons,
                    pricePerGallon = pricePerGallon,
                    odometer = odometer,
                    date = date
                )
            )
        }
    }

    fun deleteFillUp(fillUp: FillUp) {
        viewModelScope.launch {
            repository.deleteFillUp(fillUp)
        }
    }

    fun addMaintenance(whatWasDone: String, cost: Double, odometer: Double, date: Long, hours: Double?) {
        val vehicleId = selectedVehicleId.value ?: return
        viewModelScope.launch {
            repository.insertMaintenance(
                Maintenance(
                    vehicleId = vehicleId,
                    whatWasDone = whatWasDone,
                    cost = cost,
                    odometer = odometer,
                    date = date,
                    hours = hours
                )
            )
        }
    }

    fun deleteMaintenance(maintenance: Maintenance) {
        viewModelScope.launch {
            repository.deleteMaintenance(maintenance)
        }
    }

    suspend fun previewFillUpImport(csvText: String): Result<ReadyFillUpImport> {
        val vehicleId = selectedVehicleId.value
            ?: return Result.failure(IllegalStateException("No vehicle selected"))
        val vehicle = allVehicles.value.find { it.id == vehicleId }
            ?: repository.getVehicleById(vehicleId)
            ?: return Result.failure(IllegalStateException("No vehicle selected"))
        val existing = repository.getFillUpSnapshot(vehicleId).map {
            ExistingFillUp(tripMiles = it.tripMiles, dateMillis = it.date)
        }
        return when (val parsed = FillUpCsvParser.parse(csvText, existing)) {
            is FillUpCsvParseResult.Error -> Result.failure(IllegalArgumentException(parsed.message))
            is FillUpCsvParseResult.Ok -> Result.success(
                ReadyFillUpImport(
                    vehicleId = vehicle.id,
                    vehicleLabel = "${vehicle.year} ${vehicle.make} ${vehicle.model}",
                    preview = parsed.preview
                )
            )
        }
    }

    suspend fun commitFillUpImport(vehicleId: Int, preview: FillUpCsvPreview): Result<ImportCommitResult> {
        return try {
            var imported = 0
            for (draft in preview.importableDrafts()) {
                repository.insertFillUp(
                    FillUp(
                        vehicleId = vehicleId,
                        tripMiles = draft.tripMiles,
                        gallons = draft.gallons,
                        pricePerGallon = draft.pricePerGallon,
                        odometer = null,
                        date = draft.dateMillis
                    )
                )
                imported++
            }
            Result.success(
                ImportCommitResult(
                    imported = imported,
                    duplicatesSkipped = preview.duplicateCount
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importFillUpsFromCsv(csvText: String): Result<Int> {
        val ready = previewFillUpImport(csvText).getOrElse { return Result.failure(it) }
        val committed = commitFillUpImport(ready.vehicleId, ready.preview).getOrElse { return Result.failure(it) }
        return Result.success(committed.imported)
    }

    companion object {
        fun Factory(repository: VehicleRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return VehicleViewModel(repository) as T
            }
        }
    }
}
