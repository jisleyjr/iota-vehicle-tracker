package com.example.data.maintenance

import com.example.data.model.FillUp
import com.example.data.model.Maintenance
import com.example.data.model.MaintenanceType
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OilChangeIndicatorTest {

    @Before
    fun setUp() {
        Locale.setDefault(Locale.US)
    }

    @Test
    fun noOilChangeShowsTheDiscoverableHint() {
        val status = computeOilChangeStatus(
            maintenance = listOf(record(MaintenanceType.REPAIR, odometer = 40000.0, date = 2)),
            latestOdometer = 42000.0
        )

        assertEquals(OilChangeStatus.None, status)
        assertEquals("No oil change logged", formatOilChangeIndicator(status))
        assertEquals("\u2014", formatLastOilChangeMiles(status))
    }

    @Test
    fun milesAreLatestOdometerMinusTheMostRecentOilChange() {
        val maintenance = listOf(
            oil(id = 1, odometer = 30000.0, date = 1),
            oil(id = 2, odometer = 42000.0, date = 2),
            record(MaintenanceType.REPAIR, odometer = 45000.0, date = 3)
        )

        val status = computeOilChangeStatus(maintenance, latestOdometer = 48000.0) as OilChangeStatus.MilesAgo

        assertEquals(6000.0, status.miles, 0.001)
        assertEquals("Oil changed 6000 miles ago", formatOilChangeIndicator(status))
        assertEquals("6000", formatLastOilChangeMiles(status))
    }

    @Test
    fun sameDateUsesTheHigherOdometer() {
        val maintenance = listOf(
            oil(id = 1, odometer = 42000.0, date = 5),
            oil(id = 2, odometer = 43000.0, date = 5)
        )

        val status = computeOilChangeStatus(maintenance, latestOdometer = 45000.0) as OilChangeStatus.MilesAgo

        assertEquals(2000.0, status.miles, 0.001)
    }

    @Test
    fun negativeMilesAgoAreShownAsZero() {
        val status = computeOilChangeStatus(
            maintenance = listOf(oil(id = 1, odometer = 50000.0, date = 1)),
            latestOdometer = 48000.0
        ) as OilChangeStatus.MilesAgo

        assertEquals(0.0, status.miles, 0.001)
        assertEquals("Oil changed 0 miles ago", formatOilChangeIndicator(status))
        assertEquals("0", formatLastOilChangeMiles(status))
    }

    @Test
    fun fillUpOdometerCanBeTheSourceOfLatestOdometer() {
        val fillUps = listOf(
            FillUp(
                id = 1,
                vehicleId = 1,
                tripMiles = 200.0,
                gallons = 8.0,
                pricePerGallon = 3.5,
                odometer = 50000.0,
                date = 10
            )
        )
        val maintenance = listOf(
            oil(id = 1, odometer = 42000.0, date = 1),
            record(MaintenanceType.REPAIR, odometer = 43000.0, date = 2)
        )

        val latest = latestOdometer(fillUps, maintenance)
        val status = computeOilChangeStatus(maintenance, latest) as OilChangeStatus.MilesAgo

        assertEquals(50000.0, latest, 0.001)
        assertEquals(8000.0, status.miles, 0.001)
        assertEquals("Oil changed 8000 miles ago", formatOilChangeIndicator(status))
        assertTrue(latest > maintenance.maxOf { it.odometer })
    }

    private fun oil(id: Int, odometer: Double, date: Long) =
        record(MaintenanceType.OIL_CHANGE, odometer, date, id)

    private fun record(
        type: MaintenanceType,
        odometer: Double,
        date: Long,
        id: Int = 1
    ) = Maintenance(
        id = id,
        vehicleId = 1,
        type = type,
        title = type.label,
        cost = 40.0,
        odometer = odometer,
        date = date
    )
}
