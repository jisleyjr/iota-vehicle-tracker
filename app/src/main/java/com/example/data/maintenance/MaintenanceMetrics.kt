package com.example.data.maintenance

import com.example.data.model.FillUp
import com.example.data.model.Maintenance
import com.example.data.model.MaintenanceType
import com.example.data.model.TireSet
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

data class TireSetMileage(
    val tireSet: TireSet,
    val miles: Double?
)

sealed class OilChangeStatus {
    data object None : OilChangeStatus()
    data class MilesAgo(val miles: Double) : OilChangeStatus()
}

sealed class TireSetDeleteResult {
    data object Deleted : TireSetDeleteResult()
    data class Blocked(val message: String) : TireSetDeleteResult()
}

private val maintenanceChronology = compareBy<Maintenance>({ it.date }, { it.odometer }, { it.id })

fun latestOdometer(fillUps: List<FillUp>, maintenance: List<Maintenance>): Double {
    val fromFills = fillUps.mapNotNull { it.odometer }.maxOrNull() ?: 0.0
    val fromMaintenance = maintenance.maxOfOrNull { it.odometer } ?: 0.0
    return maxOf(fromFills, fromMaintenance)
}

/**
 * Miles are the sum of mounted intervals from tire-swap entries only.
 * A set that has never been mounted returns null so the UI can show an em dash.
 * Negative intervals (out-of-order odometers) contribute 0 and do not discard the rest of the sum.
 */
fun tireSetMileages(
    tireSets: List<TireSet>,
    maintenance: List<Maintenance>,
    latestOdometer: Double
): List<TireSetMileage> {
    val swaps = maintenance
        .filter { it.type == MaintenanceType.TIRE_SWAP && it.tireSetId != null }
        .sortedWith(maintenanceChronology)
    val accumulated = mutableMapOf<Int, Double>()
    swaps.forEachIndexed { index, swap ->
        val setId = swap.tireSetId ?: return@forEachIndexed
        val endOdometer = if (index == swaps.lastIndex) latestOdometer else swaps[index + 1].odometer
        val contribution = (endOdometer - swap.odometer).coerceAtLeast(0.0)
        accumulated[setId] = (accumulated[setId] ?: 0.0) + contribution
    }
    return tireSets.map { set ->
        TireSetMileage(tireSet = set, miles = accumulated[set.id])
    }
}

fun currentlyMountedTireSetId(maintenance: List<Maintenance>): Int? {
    return maintenance
        .filter { it.type == MaintenanceType.TIRE_SWAP && it.tireSetId != null }
        .maxWithOrNull(maintenanceChronology)
        ?.tireSetId
}

fun computeOilChangeStatus(maintenance: List<Maintenance>, latestOdometer: Double): OilChangeStatus {
    val latestOilChange = maintenance
        .filter { it.type == MaintenanceType.OIL_CHANGE }
        .maxWithOrNull(maintenanceChronology)
        ?: return OilChangeStatus.None
    val milesAgo = (latestOdometer - latestOilChange.odometer).coerceAtLeast(0.0)
    return OilChangeStatus.MilesAgo(milesAgo)
}

fun formatMiles(miles: Double, locale: Locale = Locale.getDefault()): String {
    val format = NumberFormat.getIntegerInstance(locale)
    format.roundingMode = RoundingMode.HALF_UP
    format.maximumFractionDigits = 0
    return format.format(miles)
}

fun formatOilChangeIndicator(status: OilChangeStatus, locale: Locale = Locale.getDefault()): String {
    return when (status) {
        OilChangeStatus.None -> "No oil change logged"
        is OilChangeStatus.MilesAgo -> "Oil changed ${formatMiles(status.miles, locale)} miles ago"
    }
}

fun formatLastOilChangeMiles(status: OilChangeStatus, locale: Locale = Locale.getDefault()): String {
    return when (status) {
        OilChangeStatus.None -> "\u2014"
        is OilChangeStatus.MilesAgo -> formatMiles(status.miles, locale)
    }
}

fun formatTireSetMiles(miles: Double?, locale: Locale = Locale.getDefault()): String {
    if (miles == null) return "\u2014"
    return "${formatMiles(miles, locale)} mi"
}

fun tireSetDeleteResult(referenceCount: Int, name: String): TireSetDeleteResult {
    if (referenceCount > 0) {
        return TireSetDeleteResult.Blocked(
            "$name is used by a tire swap or rotation. Rename the set instead of deleting it so service history stays intact."
        )
    }
    return TireSetDeleteResult.Deleted
}
