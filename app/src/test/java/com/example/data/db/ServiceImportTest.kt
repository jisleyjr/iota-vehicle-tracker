package com.example.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.csv.ServiceLogCsvWriter
import com.example.data.model.TireSet
import com.example.data.model.Vehicle
import com.example.data.repository.VehicleRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ServiceImportTest {
    @Test
    fun roundTripRollbackAndConcurrentRetry() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val dao = db.vehicleDao()
            val repo = VehicleRepository(dao)
            val id = dao.insertVehicle(Vehicle(make = "Toyota", model = "Camry")).toInt()
            val csv = requireNotNull(javaClass.getResource("/service-logs.csv")).readText()
            val name = "Neige, été"
            val tire = TireSet(vehicleId = id, name = name)
            val tireId = dao.insertTireSet(tire).toInt()
            val preview = repo.previewServiceImport(id, csv)
            assertEquals(5, preview.newCount)
            dao.insertTireSet(tire.copy(id = tireId, name = "Renamed"))
            assertTrue(runCatching { repo.commitServiceImport(preview) }.isFailure)
            assertTrue(dao.getMaintenanceSnapshot(id).isEmpty())
            dao.insertTireSet(tire.copy(id = tireId))
            val counts = List(2) { async { repo.commitServiceImport(preview) } }.awaitAll()
            assertEquals(listOf(0, 5), counts.sorted())
            assertEquals(csv, ServiceLogCsvWriter.write(dao.getMaintenanceSnapshot(id), mapOf(tireId to name)))
            assertEquals(5, repo.previewServiceImport(id, csv).duplicates)
        } finally {
            db.close()
        }
    }
}
