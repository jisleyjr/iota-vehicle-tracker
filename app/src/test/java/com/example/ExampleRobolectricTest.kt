package com.example

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.FillUp
import com.example.data.model.Maintenance
import com.example.data.model.Vehicle
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("IOTA Vehicle Tracker", appName)
  }

  @Test
  fun `test activity launch`() {
    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
      scenario.onActivity { activity ->
        assertNotNull(activity)
      }
    }
  }

  @Test
  fun `test maintenance log modeling and hours field`() {
    val maintenance = Maintenance(
      id = 1,
      vehicleId = 1,
      whatWasDone = "Oil Change",
      cost = 45.0,
      odometer = 12000.0,
      date = System.currentTimeMillis(),
      hours = 12.5
    )
    assertEquals(12.5, maintenance.hours!!, 0.001)
  }

  @Test
  fun `test CSV import parsing`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    
    val dao = db.vehicleDao()
    val repository = com.example.data.repository.VehicleRepository(dao)
    val viewModel = com.example.ui.viewmodel.VehicleViewModel(repository)
    
    // Add vehicle to satisfy foreign key constraints and selected vehicle requirement
    val vehicleId = dao.insertVehicle(
      com.example.data.model.Vehicle(make = "Toyota", model = "Corolla", year = 2020, licensePlate = "ABC-123")
    ).toInt()
    
    viewModel.selectVehicle(vehicleId)
    
    val csvContent = """
      miles, gallons, price per gallon, date
      300.5, 10.2, 3.49, 2026-05-29
      150.0, 5.0, 3.60, 1716940800000
    """.trimIndent()
    
    val result = viewModel.importFillUpsFromCsv(csvContent)
    
    assertEquals(true, result.isSuccess)
    assertEquals(2, result.getOrNull())
    
    db.close()
  }

  @Test
  fun `csv import skips saved fill-ups and earlier rows in the file`() = kotlinx.coroutines.runBlocking {
    val previousZone = TimeZone.getDefault()
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    try {
      val dao = db.vehicleDao()
      val repository = com.example.data.repository.VehicleRepository(dao)
      val viewModel = com.example.ui.viewmodel.VehicleViewModel(repository)
      val vehicleId = dao.insertVehicle(
        Vehicle(make = "Toyota", model = "Camry", year = 2021, licensePlate = "CAM4219")
      ).toInt()
      viewModel.selectVehicle(vehicleId)
      dao.insertFillUp(
        FillUp(
          vehicleId = vehicleId,
          tripMiles = 100.0,
          gallons = 4.0,
          pricePerGallon = 3.0,
          odometer = null,
          date = utcMidnight(2026, 1, 1)
        )
      )

      val csv = """
        miles, gallons, price per gallon, date
        100,5.5,3.25,2026-01-01
        100.0,6,3.40,2026-01-01T10:00:00Z
        80,4.5,${'$'}3.10,01/03/2026
      """.trimIndent()

      val result = viewModel.importFillUpsFromCsv(csv)
      assertTrue(result.isSuccess)
      assertEquals(1, result.getOrNull())

      val stored = dao.getFillUpSnapshot(vehicleId)
      assertEquals(2, stored.size)
      val imported = stored.single { it.tripMiles == 80.0 }
      assertEquals(4.5, imported.gallons, 0.001)
      assertEquals(3.10, imported.pricePerGallon, 0.001)
      assertEquals(null, imported.odometer)
      assertEquals(utcMidnight(2026, 1, 3), imported.date)
    } finally {
      TimeZone.setDefault(previousZone)
      db.close()
    }
  }

  @Test
  fun `csv import fails when a required header is missing`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    try {
      val dao = db.vehicleDao()
      val repository = com.example.data.repository.VehicleRepository(dao)
      val viewModel = com.example.ui.viewmodel.VehicleViewModel(repository)
      val vehicleId = dao.insertVehicle(
        Vehicle(make = "Toyota", model = "Camry", year = 2021, licensePlate = "CAM4219")
      ).toInt()
      viewModel.selectVehicle(vehicleId)

      val result = viewModel.importFillUpsFromCsv(
        """
        gallons,price,date
        2,3.5,2026-01-02
        """.trimIndent()
      )
      assertTrue(result.isFailure)
      val message = result.exceptionOrNull()?.message.orEmpty()
      assertTrue(message.contains("Missing required"))
      assertTrue(message.contains("trip") || message.contains("miles"))
    } finally {
      db.close()
    }
  }

  private fun utcMidnight(year: Int, month: Int, day: Int): Long {
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
}

