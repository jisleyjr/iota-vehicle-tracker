package com.example.data.db

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.MaintenanceType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MaintenanceMigrationTest {

    @Test
    fun migrationFrom4To5PreservesGarageFillUpsAndServiceHistory() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-4-to-5"
        context.deleteDatabase(name)

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(4) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        createVersion4Tables(db)
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        val sqlite = helper.writableDatabase
        assertEquals(4, sqlite.version)
        sqlite.execSQL(
            "INSERT INTO vehicles (id, make, model, year, licensePlate) VALUES (?, ?, ?, ?, ?)",
            arrayOf<Any?>(1, "Toyota", "Camry", 2021, "CAM4219")
        )
        sqlite.execSQL(
            "INSERT INTO fill_ups (id, vehicleId, tripMiles, gallons, pricePerGallon, odometer, date) VALUES (?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>(7, 1, 312.0, 9.4, 3.49, 84210.0, 1_700_000_000_000L)
        )
        sqlite.execSQL(
            "INSERT INTO fill_ups (id, vehicleId, tripMiles, gallons, pricePerGallon, odometer, date) VALUES (?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>(8, 1, 265.0, 8.2, 3.48, null, 1_700_100_000_000L)
        )
        sqlite.execSQL(
            "INSERT INTO maintenance (id, vehicleId, whatWasDone, cost, odometer, date, hours) VALUES (?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>(10, 1, "Oil change", 64.5, 83000.0, 1_699_000_000_000L, null)
        )
        sqlite.execSQL(
            "INSERT INTO maintenance (id, vehicleId, whatWasDone, cost, odometer, date, hours) VALUES (?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>(11, 1, "Brake pads", 285.0, 85100.0, 1_699_500_000_000L, 1.5)
        )
        sqlite.close()
        helper.close()

        val database = AppDatabase.builder(context, name).allowMainThreadQueries().build()
        try {
            val dao = database.vehicleDao()
            val vehicles = dao.getVehicleSnapshot()
            assertEquals(1, vehicles.size)
            assertEquals("Toyota", vehicles.single().make)
            assertEquals("Camry", vehicles.single().model)
            assertEquals(2021, vehicles.single().year)
            assertEquals("CAM4219", vehicles.single().licensePlate)

            val fillUps = dao.getFillUpSnapshot(1)
            assertEquals(2, fillUps.size)
            val withOdometer = fillUps.single { it.id == 7 }
            assertEquals(312.0, withOdometer.tripMiles, 0.001)
            assertEquals(9.4, withOdometer.gallons, 0.001)
            assertEquals(3.49, withOdometer.pricePerGallon, 0.001)
            assertEquals(84210.0, withOdometer.odometer!!, 0.001)
            assertEquals(1_700_000_000_000L, withOdometer.date)
            val withoutOdometer = fillUps.single { it.id == 8 }
            assertNull(withoutOdometer.odometer)
            assertEquals(265.0, withoutOdometer.tripMiles, 0.001)

            val maintenance = dao.getMaintenanceSnapshot(1)
            assertEquals(2, maintenance.size)
            val oil = maintenance.single { it.id == 10 }
            assertEquals("Oil change", oil.title)
            assertEquals(MaintenanceType.OTHER, oil.type)
            assertNull(oil.description)
            assertNull(oil.tireSetId)
            assertNull(oil.hours)
            assertEquals(64.5, oil.cost, 0.001)
            assertEquals(83000.0, oil.odometer, 0.001)
            assertEquals(1_699_000_000_000L, oil.date)

            val brakes = maintenance.single { it.id == 11 }
            assertEquals("Brake pads", brakes.title)
            assertEquals(MaintenanceType.OTHER, brakes.type)
            assertNull(brakes.description)
            assertNull(brakes.tireSetId)
            assertEquals(1.5, brakes.hours!!, 0.001)
            assertEquals(285.0, brakes.cost, 0.001)
            assertEquals(85100.0, brakes.odometer, 0.001)
            assertEquals(1_699_500_000_000L, brakes.date)

            assertTrue(dao.getTireSetSnapshot(1).isEmpty())

            val setId = dao.insertTireSet(
                com.example.data.model.TireSet(vehicleId = 1, name = "Summer")
            ).toInt()
            val savedId = dao.insertMaintenance(
                com.example.data.model.Maintenance(
                    vehicleId = 1,
                    type = MaintenanceType.TIRE_SWAP,
                    title = "Mounted summer tires",
                    cost = 0.0,
                    odometer = 86000.0,
                    date = 1_700_200_000_000L,
                    tireSetId = setId
                )
            )
            val reread = dao.getMaintenanceSnapshot(1).single { it.id == savedId.toInt() }
            assertEquals(MaintenanceType.TIRE_SWAP, reread.type)
            assertEquals("Mounted summer tires", reread.title)
            assertEquals(setId, reread.tireSetId)
            assertEquals(0.0, reread.cost, 0.001)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    private fun createVersion4Tables(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `vehicles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `make` TEXT NOT NULL, `model` TEXT NOT NULL, `year` INTEGER NOT NULL, `licensePlate` TEXT NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `fill_ups` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `vehicleId` INTEGER NOT NULL, `tripMiles` REAL NOT NULL, `gallons` REAL NOT NULL, `pricePerGallon` REAL NOT NULL, `odometer` REAL, `date` INTEGER NOT NULL, FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_fill_ups_vehicleId` ON `fill_ups` (`vehicleId`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `maintenance` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `vehicleId` INTEGER NOT NULL, `whatWasDone` TEXT NOT NULL, `cost` REAL NOT NULL, `odometer` REAL NOT NULL, `date` INTEGER NOT NULL, `hours` REAL, FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_maintenance_vehicleId` ON `maintenance` (`vehicleId`)")
    }
}
