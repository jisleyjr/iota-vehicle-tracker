package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Vehicle
import com.example.data.model.FillUp
import com.example.data.model.Maintenance
import com.example.data.repository.VehicleRepository
import com.example.BuildConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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

    suspend fun importFillUpsFromCsv(csvText: String): Result<Int> {
        val vehicleId = selectedVehicleId.value ?: return Result.failure(Exception("No vehicle selected"))
        val lines = csvText.lines()
        var importedCount = 0

        try {
            lines.forEach { line ->
                val trimmedLine = line.trim()
                if (trimmedLine.isBlank()) return@forEach
                
                val separator = if (trimmedLine.contains(";")) ";" else ","
                val parts = trimmedLine.split(separator).map { it.trim().trim('"', '\'') }

                val isHeader = parts.any {
                    it.contains("miles", ignoreCase = true) ||
                    it.contains("gallon", ignoreCase = true) ||
                    it.contains("price", ignoreCase = true) ||
                    it.contains("date", ignoreCase = true)
                }
                if (isHeader) return@forEach

                if (parts.size >= 4) {
                    val tripMiles = parts[0].toDoubleOrNull()
                    val gallons = parts[1].toDoubleOrNull()
                    val pricePerGallon = parts[2].toDoubleOrNull()
                    val dateStr = parts[3]

                    val dateLong = parseDate(dateStr) ?: System.currentTimeMillis()

                    if (tripMiles != null && gallons != null && pricePerGallon != null) {
                        repository.insertFillUp(
                            FillUp(
                                vehicleId = vehicleId,
                                tripMiles = tripMiles,
                                gallons = gallons,
                                pricePerGallon = pricePerGallon,
                                odometer = null,
                                date = dateLong
                            )
                        )
                        importedCount++
                    }
                }
            }
            return Result.success(importedCount)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    private fun parseDate(dateStr: String): Long? {
        val longVal = dateStr.toLongOrNull()
        if (longVal != null) return longVal

        val formats = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd",
            "MM/dd/yyyy HH:mm:ss",
            "MM/dd/yyyy",
            "dd-MM-yyyy",
            "yyyy/MM/dd",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        for (format in formats) {
            try {
                val sdf = java.text.SimpleDateFormat(format, java.util.Locale.US)
                sdf.isLenient = false
                return sdf.parse(dateStr)?.time
            } catch (e: Exception) {
                // keep trying
            }
        }
        return null
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
