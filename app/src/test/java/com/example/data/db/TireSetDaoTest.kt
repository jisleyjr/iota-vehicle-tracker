package com.example.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Maintenance
import com.example.data.model.MaintenanceType
import com.example.data.model.TireSet
import com.example.data.model.Vehicle
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TireSetDaoTest {

    @Test
    fun referencesCountSwapsAndRotationsAndRenamingKeepsTheLink() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = db.vehicleDao()
            val vehicleId = dao.insertVehicle(
                Vehicle(make = "Toyota", model = "Camry", year = 2021, licensePlate = "CAM4219")
            ).toInt()
            val setId = dao.insertTireSet(
                TireSet(vehicleId = vehicleId, name = "Summer", brand = "Michelin", size = "225/45R18")
            ).toInt()
            dao.insertMaintenance(
                Maintenance(
                    vehicleId = vehicleId,
                    type = MaintenanceType.TIRE_SWAP,
                    title = "Mounted summer tires",
                    cost = 0.0,
                    odometer = 10000.0,
                    date = 1,
                    tireSetId = setId
                )
            )
            dao.insertMaintenance(
                Maintenance(
                    vehicleId = vehicleId,
                    type = MaintenanceType.TIRE_ROTATION,
                    title = "Tire rotation",
                    cost = 20.0,
                    odometer = 12000.0,
                    date = 2,
                    tireSetId = setId
                )
            )
            dao.insertMaintenance(
                Maintenance(
                    vehicleId = vehicleId,
                    type = MaintenanceType.OIL_CHANGE,
                    title = "Oil change",
                    cost = 40.0,
                    odometer = 13000.0,
                    date = 3
                )
            )

            assertEquals(2, dao.countTireSetReferences(setId))
            dao.updateTireSet(
                TireSet(
                    id = setId,
                    vehicleId = vehicleId,
                    name = "All season",
                    brand = "Michelin",
                    size = "225/45R18"
                )
            )
            val renamed = dao.getTireSetSnapshot(vehicleId).single()
            assertEquals("All season", renamed.name)
            assertEquals(2, dao.countTireSetReferences(setId))

            val spareId = dao.insertTireSet(TireSet(vehicleId = vehicleId, name = "Spare")).toInt()
            assertEquals(0, dao.countTireSetReferences(spareId))
            dao.deleteTireSet(TireSet(id = spareId, vehicleId = vehicleId, name = "Spare"))
            assertTrue(dao.getTireSetSnapshot(vehicleId).none { it.id == spareId })
            assertEquals(2, dao.countTireSetReferences(setId))
        } finally {
            db.close()
        }
    }
}
