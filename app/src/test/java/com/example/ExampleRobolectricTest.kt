package com.example

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.FillUp
import com.example.data.model.Maintenance
import com.example.data.model.Vehicle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
}

