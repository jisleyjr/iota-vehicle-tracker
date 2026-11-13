package com.example.data.db

import androidx.room.*
import com.example.data.model.Vehicle
import com.example.data.model.FillUp
import com.example.data.model.Maintenance
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles ORDER BY id DESC")
    fun getAllVehicles(): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicles WHERE id = :vehicleId LIMIT 1")
    suspend fun getVehicleById(vehicleId: Int): Vehicle?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: Vehicle): Long

    @Delete
    suspend fun deleteVehicle(vehicle: Vehicle)

    @Query("SELECT * FROM fill_ups WHERE vehicleId = :vehicleId ORDER BY date DESC, odometer DESC")
    fun getFillUpsForVehicle(vehicleId: Int): Flow<List<FillUp>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFillUp(fillUp: FillUp): Long

    @Delete
    suspend fun deleteFillUp(fillUp: FillUp)

    @Query("SELECT * FROM maintenance WHERE vehicleId = :vehicleId ORDER BY date DESC, odometer DESC")
    fun getMaintenanceForVehicle(vehicleId: Int): Flow<List<Maintenance>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaintenance(maintenance: Maintenance): Long

    @Delete
    suspend fun deleteMaintenance(maintenance: Maintenance)
}
