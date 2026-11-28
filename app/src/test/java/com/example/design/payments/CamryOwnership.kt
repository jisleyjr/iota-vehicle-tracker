package com.example.design.payments

import com.example.data.maintenance.TireSetMileage
import com.example.data.model.FillUp
import com.example.data.model.TireSet
import com.example.data.model.Vehicle
import com.example.ui.viewmodel.VehicleStats
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

internal enum class PaymentKind(val chip: String) {
  LOAN("Loan"),
  LEASE("Lease"),
  DOWN_PAYMENT("Down"),
  PURCHASE_PRICE("Purchase")
}

internal data class MockPayment(
  val kind: PaymentKind,
  val amount: Double,
  val dateLabel: String,
  val odometer: Double?,
  val note: String
)

/**
 * In-memory Camry story for the payments mockups.
 *
 * Cash paid is the down payment plus each logged installment. The schedule total is that down
 * payment plus the monthly amount times the term. Remaining is the schedule minus cash paid.
 * Total cost is cash paid plus fuel plus maintenance. The agreed price is reference only while
 * a loan is being tracked, so it is not added again. Cost per mile is total cost divided by
 * miles driven, shown in cents. Miles driven are the latest odometer minus the odometer at purchase.
 */
internal object CamryOwnership {
  const val agreedPrice = 24_800.0
  const val monthlyAmount = 498.0
  const val termMonths = 48
  const val odometerAtPurchase = 68_000.0
  const val latestOdometer = 85_399.0
  const val fuel = 162.08
  const val maintenance = 389.50
  const val nextDue = "12/01/2026"

  val downPayment = MockPayment(
    kind = PaymentKind.DOWN_PAYMENT,
    amount = 3_000.0,
    dateLabel = "03/12/2024",
    odometer = 68_000.0,
    note = "Due at signing"
  )

  val installments = listOf(
    MockPayment(PaymentKind.LOAN, 498.0, "11/01/2026", 85_050.0, "November installment"),
    MockPayment(PaymentKind.LOAN, 498.0, "10/01/2026", 84_300.0, "October installment"),
    MockPayment(PaymentKind.LOAN, 498.0, "09/01/2026", 83_550.0, "September installment"),
    MockPayment(PaymentKind.LOAN, 498.0, "08/01/2026", 82_400.0, "August installment")
  )

  val paymentsListed: List<MockPayment> = listOf(downPayment) + installments

  val cashPaid: Double = paymentsListed.sumOf { it.amount }
  val scheduleTotal: Double = downPayment.amount + monthlyAmount * termMonths
  val remaining: Double = scheduleTotal - cashPaid
  val totalCost: Double = cashPaid + fuel + maintenance
  val milesDriven: Double = latestOdometer - odometerAtPurchase
  val centsPerMile: Double = totalCost / milesDriven * 100.0
  val paymentsLogged: Int = installments.size
  val paymentsLeft: Int = termMonths - paymentsLogged

  fun centsLabel(): String = String.format(Locale.US, "%.1f¢", centsPerMile)
}

internal fun money(amount: Double): String {
  val format = NumberFormat.getCurrencyInstance(Locale.US)
  format.minimumFractionDigits = 2
  format.maximumFractionDigits = 2
  return format.format(amount)
}

internal fun wholeMiles(miles: Double): String =
  String.format(Locale.US, "%,.0f", miles)

internal val designOutback = Vehicle(
  id = 1,
  make = "Subaru",
  model = "Outback",
  year = 2018,
  licensePlate = "8FKR392"
)

internal val designCamry = Vehicle(
  id = 2,
  make = "Toyota",
  model = "Camry",
  year = 2021,
  licensePlate = "CAM4219"
)

internal const val camryMountedTireSetId = 2

internal val camryTireMileage: List<TireSetMileage> = listOf(
  TireSetMileage(
    tireSet = TireSet(id = 1, vehicleId = designCamry.id, name = "Summer", brand = "Michelin", size = "225/45R18"),
    miles = 14_000.0
  ),
  TireSetMileage(
    tireSet = TireSet(id = camryMountedTireSetId, vehicleId = designCamry.id, name = "Winter", brand = "Blizzak", size = "225/45R18"),
    miles = 1_399.0
  )
)

internal val camryDashboardStats = VehicleStats(
  averageMpg = (312.0 + 286.0 + 340.0 + 298.0 + 265.0) / (9.4 + 8.7 + 10.1 + 9.0 + 8.2),
  totalFuelSpent = CamryOwnership.fuel,
  totalMaintenanceCost = CamryOwnership.maintenance,
  overallTotalSpent = CamryOwnership.fuel + CamryOwnership.maintenance,
  latestOdometer = CamryOwnership.latestOdometer
)

internal fun designFillUps(): List<FillUp> = listOf(
  FillUp(id = 5, vehicleId = designCamry.id, tripMiles = 265.0, gallons = 8.2, pricePerGallon = 3.48, odometer = 85399.0, date = utcMidnight(2026, 11, 6)),
  FillUp(id = 4, vehicleId = designCamry.id, tripMiles = 298.0, gallons = 9.0, pricePerGallon = 3.71, odometer = 85134.0, date = utcMidnight(2026, 10, 22)),
  FillUp(id = 3, vehicleId = designCamry.id, tripMiles = 340.0, gallons = 10.1, pricePerGallon = 3.55, odometer = 84836.0, date = utcMidnight(2026, 10, 5)),
  FillUp(id = 2, vehicleId = designCamry.id, tripMiles = 286.0, gallons = 8.7, pricePerGallon = 3.62, odometer = 84496.0, date = utcMidnight(2026, 9, 18)),
  FillUp(id = 1, vehicleId = designCamry.id, tripMiles = 312.0, gallons = 9.4, pricePerGallon = 3.49, odometer = 84210.0, date = utcMidnight(2026, 9, 2))
)

internal fun utcMidnight(year: Int, month: Int, day: Int): Long {
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
