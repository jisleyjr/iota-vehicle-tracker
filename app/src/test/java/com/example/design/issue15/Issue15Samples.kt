package com.example.design.issue15

import com.example.data.maintenance.OilChangeStatus
import com.example.data.maintenance.TireSetMileage
import com.example.data.maintenance.computeOilChangeStatus
import com.example.data.maintenance.formatLastOilChangeMiles
import com.example.data.maintenance.formatOilChangeIndicator
import com.example.data.maintenance.latestOdometer
import com.example.data.maintenance.tireSetMileages
import com.example.data.model.FillUp
import com.example.data.model.Maintenance
import com.example.data.model.MaintenanceType
import com.example.data.model.TireSet
import com.example.data.model.Vehicle
import com.example.ui.viewmodel.VehicleStats
import java.util.Calendar
import java.util.TimeZone

/**
 * The same Camry and Outback records the app screenshot tests store in Room.
 * Dates are UTC midnight so the mockups match the existing 11/06/2026 fuel log.
 */
data class Issue15Data(
  val outback: Vehicle,
  val camry: Vehicle,
  val fillUps: List<FillUp>,
  val maintenance: List<Maintenance>,
  val tireMileage: List<TireSetMileage>,
  val mountedTireSetId: Int?,
  val stats: VehicleStats,
  val oilStatus: OilChangeStatus,
  val oilLabel: String,
  val lastOilMiles: String
) {
  val referenceFillUp: FillUp get() = fillUps.first()
}

fun buildIssue15Data(): Issue15Data {
  val outback = Vehicle(id = 1, make = "Subaru", model = "Outback", year = 2018, licensePlate = "8FKR392")
  val camry = Vehicle(id = 2, make = "Toyota", model = "Camry", year = 2021, licensePlate = "CAM4219")
  val summer = TireSet(
    id = 1,
    vehicleId = camry.id,
    name = "Summer",
    brand = "Michelin",
    size = "225/45R18",
    purchaseDate = utcDate(2025, 4, 2)
  )
  val winter = TireSet(
    id = 2,
    vehicleId = camry.id,
    name = "Winter",
    brand = "Blizzak",
    size = "225/45R18",
    purchaseDate = utcDate(2025, 10, 1)
  )
  val maintenance = listOf(
    Maintenance(
      id = 5,
      vehicleId = camry.id,
      type = MaintenanceType.REPAIR,
      title = "Brake pads",
      description = "Front pads and rotors",
      cost = 285.0,
      odometer = 85100.0,
      date = utcDate(2026, 10, 28),
      hours = 1.5
    ),
    Maintenance(
      id = 4,
      vehicleId = camry.id,
      type = MaintenanceType.TIRE_ROTATION,
      title = "Tire rotation",
      cost = 40.0,
      odometer = 84500.0,
      date = utcDate(2026, 9, 20),
      tireSetId = winter.id
    ),
    Maintenance(
      id = 3,
      vehicleId = camry.id,
      type = MaintenanceType.TIRE_SWAP,
      title = "Swapped to winter tires",
      cost = 0.0,
      odometer = 84000.0,
      date = utcDate(2026, 8, 15),
      tireSetId = winter.id
    ),
    Maintenance(
      id = 2,
      vehicleId = camry.id,
      type = MaintenanceType.OIL_CHANGE,
      title = "Oil change",
      description = "5W-30 synthetic",
      cost = 64.50,
      odometer = 83000.0,
      date = utcDate(2026, 7, 12)
    ),
    Maintenance(
      id = 1,
      vehicleId = camry.id,
      type = MaintenanceType.TIRE_SWAP,
      title = "Mounted summer tires",
      cost = 0.0,
      odometer = 70000.0,
      date = utcDate(2026, 4, 1),
      tireSetId = summer.id
    )
  )
  val fillUps = listOf(
    FillUp(id = 5, vehicleId = camry.id, tripMiles = 265.0, gallons = 8.2, pricePerGallon = 3.48, odometer = 85399.0, date = utcDate(2026, 11, 6)),
    FillUp(id = 4, vehicleId = camry.id, tripMiles = 298.0, gallons = 9.0, pricePerGallon = 3.71, odometer = 85134.0, date = utcDate(2026, 10, 22)),
    FillUp(id = 3, vehicleId = camry.id, tripMiles = 340.0, gallons = 10.1, pricePerGallon = 3.55, odometer = 84836.0, date = utcDate(2026, 10, 5)),
    FillUp(id = 2, vehicleId = camry.id, tripMiles = 286.0, gallons = 8.7, pricePerGallon = 3.62, odometer = 84496.0, date = utcDate(2026, 9, 18)),
    FillUp(id = 1, vehicleId = camry.id, tripMiles = 312.0, gallons = 9.4, pricePerGallon = 3.49, odometer = 84210.0, date = utcDate(2026, 9, 2))
  )
  val latest = latestOdometer(fillUps, maintenance)
  val fuelSpent = fillUps.sumOf { it.gallons * it.pricePerGallon }
  val maintenanceSpent = maintenance.sumOf { it.cost }
  val gallons = fillUps.sumOf { it.gallons }
  val stats = VehicleStats(
    averageMpg = if (gallons > 0) fillUps.sumOf { it.tripMiles } / gallons else 0.0,
    totalFuelSpent = fuelSpent,
    totalMaintenanceCost = maintenanceSpent,
    overallTotalSpent = fuelSpent + maintenanceSpent,
    latestOdometer = latest
  )
  val oilStatus = computeOilChangeStatus(maintenance, latest)
  return Issue15Data(
    outback = outback,
    camry = camry,
    fillUps = fillUps,
    maintenance = maintenance,
    tireMileage = tireSetMileages(listOf(summer, winter), maintenance, latest),
    mountedTireSetId = winter.id,
    stats = stats,
    oilStatus = oilStatus,
    oilLabel = formatOilChangeIndicator(oilStatus),
    lastOilMiles = formatLastOilChangeMiles(oilStatus)
  )
}

internal fun utcDate(year: Int, month: Int, day: Int): Long {
  return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
    set(Calendar.YEAR, year)
    set(Calendar.MONTH, month - 1)
    set(Calendar.DAY_OF_MONTH, day)
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
  }.timeInMillis
}
