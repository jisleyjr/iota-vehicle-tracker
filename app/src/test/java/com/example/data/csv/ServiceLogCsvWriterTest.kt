package com.example.data.csv

import com.example.data.model.Maintenance
import com.example.data.model.MaintenanceType
import org.junit.Assert.assertEquals
import org.junit.Test

class ServiceLogCsvWriterTest {
    @Test
    fun writesExamplesForEveryServiceType() {
        val records = listOf(
            Maintenance(
                vehicleId = 1, title = "Oil and filter", type = MaintenanceType.OIL_CHANGE,
                date = 1_795_132_800_000L, odometer = 45200.0, cost = 59.95,
                description = "5W-30 synthetic", hours = 0.5
            ),
            Maintenance(
                vehicleId = 1, title = "Replace front brake pads", type = MaintenanceType.REPAIR,
                date = 1_790_899_200_000L, odometer = 44010.0, cost = 385.75,
                description = "Pads and rotors", hours = 2.25
            ),
            Maintenance(
                vehicleId = 1, title = "Rotate tires", type = MaintenanceType.TIRE_ROTATION,
                date = 1_786_492_800_000L, odometer = 42100.0, cost = 0.0
            ),
            Maintenance(
                vehicleId = 1, title = "Replace cabin air filter", type = MaintenanceType.OTHER,
                date = 1_782_864_000_000L, odometer = 41350.0, cost = 18.5
            ),
            Maintenance(
                vehicleId = 1, title = "Swap, winter", description = "Line \"one\"\nLine two",
                date = 123, odometer = 1200.25, cost = 0.0, type = MaintenanceType.TIRE_SWAP, tireSetId = 7
            )
        )
        val expected = requireNotNull(javaClass.getResource("/service-logs.csv")).readText()
        assertEquals(expected, ServiceLogCsvWriter.write(records, mapOf(7 to "Neige, été")))
    }
}
