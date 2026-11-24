package com.example.data.maintenance

import com.example.data.model.Maintenance
import com.example.data.model.MaintenanceType
import com.example.data.model.TireSet
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class TireSetMilesTest {

    @Before
    fun setUp() {
        Locale.setDefault(Locale.US)
    }

    @Test
    fun alternatingSetsSumCompletedIntervalsAndExtendTheCurrentSet() {
        val summer = set(1, "Summer")
        val winter = set(2, "Winter")
        val maintenance = listOf(
            swap(id = 1, setId = summer.id, odometer = 10000.0, date = 1),
            swap(id = 2, setId = winter.id, odometer = 15000.0, date = 2),
            swap(id = 3, setId = summer.id, odometer = 22000.0, date = 3)
        )

        val miles = tireSetMileages(listOf(summer, winter), maintenance, latestOdometer = 25000.0)

        assertEquals(8000.0, miles.milesFor(summer.id)!!, 0.001)
        assertEquals(7000.0, miles.milesFor(winter.id)!!, 0.001)
        assertEquals(summer.id, currentlyMountedTireSetId(maintenance))
    }

    @Test
    fun rotationsDoNotStartOrEndIntervalsOrChangeTheMountedSet() {
        val summer = set(1, "Summer")
        val winter = set(2, "Winter")
        val beforeRotation = listOf(
            swap(id = 1, setId = summer.id, odometer = 10000.0, date = 1)
        )
        val withRotation = beforeRotation + rotation(
            id = 2,
            setId = winter.id,
            odometer = 12000.0,
            date = 2
        )
        val afterNextSwap = withRotation + swap(
            id = 3,
            setId = winter.id,
            odometer = 15000.0,
            date = 3
        )

        assertEquals(summer.id, currentlyMountedTireSetId(withRotation))
        val miles = tireSetMileages(listOf(summer, winter), afterNextSwap, latestOdometer = 18000.0)
        assertEquals(5000.0, miles.milesFor(summer.id)!!, 0.001)
        assertEquals(3000.0, miles.milesFor(winter.id)!!, 0.001)
        assertEquals(winter.id, currentlyMountedTireSetId(afterNextSwap))
    }

    @Test
    fun aSetThatWasNeverMountedShowsNoMiles() {
        val summer = set(1, "Summer")
        val spare = set(2, "Spare", purchaseDate = 50)
        val maintenance = listOf(
            swap(id = 1, setId = summer.id, odometer = 1000.0, date = 1, cost = 0.0)
        )

        val miles = tireSetMileages(listOf(summer, spare), maintenance, latestOdometer = 1500.0)

        assertEquals(500.0, miles.milesFor(summer.id)!!, 0.001)
        assertNull(miles.milesFor(spare.id))
        assertEquals("\u2014", formatTireSetMiles(null))
        assertEquals("500 mi", formatTireSetMiles(miles.milesFor(summer.id)))
    }

    @Test
    fun zeroCostInitialSwapCountsMilesFromThatOdometer() {
        val summer = set(1, "Summer", purchaseDate = 10)
        val maintenance = listOf(
            swap(id = 1, setId = summer.id, odometer = 5000.0, date = 1, cost = 0.0)
        )

        val miles = tireSetMileages(listOf(summer), maintenance, latestOdometer = 8000.0)

        assertEquals(3000.0, miles.milesFor(summer.id)!!, 0.001)
        assertEquals("3,000 mi", formatTireSetMiles(3000.0))
    }

    @Test
    fun negativeIntervalsAreClampedAndTheRestOfTheSumStaysUsable() {
        val summer = set(1, "Summer")
        val winter = set(2, "Winter")
        val maintenance = listOf(
            swap(id = 1, setId = summer.id, odometer = 10000.0, date = 1),
            swap(id = 2, setId = winter.id, odometer = 8000.0, date = 2),
            swap(id = 3, setId = summer.id, odometer = 9000.0, date = 3)
        )

        val miles = tireSetMileages(listOf(summer, winter), maintenance, latestOdometer = 12000.0)

        assertEquals(3000.0, miles.milesFor(summer.id)!!, 0.001)
        assertEquals(1000.0, miles.milesFor(winter.id)!!, 0.001)
        assertEquals("0 mi", formatTireSetMiles(0.0))
    }

    private fun List<TireSetMileage>.milesFor(id: Int): Double? = first { it.tireSet.id == id }.miles

    private fun set(id: Int, name: String, purchaseDate: Long? = null) = TireSet(
        id = id,
        vehicleId = 1,
        name = name,
        purchaseDate = purchaseDate
    )

    private fun swap(
        id: Int,
        setId: Int,
        odometer: Double,
        date: Long,
        cost: Double = 25.0
    ) = Maintenance(
        id = id,
        vehicleId = 1,
        type = MaintenanceType.TIRE_SWAP,
        title = "Swap",
        cost = cost,
        odometer = odometer,
        date = date,
        tireSetId = setId
    )

    private fun rotation(id: Int, setId: Int, odometer: Double, date: Long) = Maintenance(
        id = id,
        vehicleId = 1,
        type = MaintenanceType.TIRE_ROTATION,
        title = "Rotation",
        cost = 0.0,
        odometer = odometer,
        date = date,
        tireSetId = setId
    )
}
