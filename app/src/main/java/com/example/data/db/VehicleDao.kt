package com.example.data.db

import com.example.data.csv.importKey
import com.example.data.csv.ServiceImport
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.FillUp
import com.example.data.model.Maintenance
import com.example.data.model.TireSet
import com.example.data.model.Vehicle
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles ORDER BY id DESC")
    fun getAllVehicles(): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicles ORDER BY id ASC")
    suspend fun getVehicleSnapshot(): List<Vehicle>

    @Query("SELECT * FROM vehicles WHERE id = :vehicleId LIMIT 1")
    suspend fun getVehicleById(vehicleId: Int): Vehicle?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: Vehicle): Long

    @Delete
    suspend fun deleteVehicle(vehicle: Vehicle)

    @Query("SELECT * FROM fill_ups WHERE vehicleId = :vehicleId ORDER BY date DESC, odometer DESC")
    fun getFillUpsForVehicle(vehicleId: Int): Flow<List<FillUp>>

    @Query("SELECT * FROM fill_ups WHERE vehicleId = :vehicleId")
    suspend fun getFillUpSnapshot(vehicleId: Int): List<FillUp>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFillUp(fillUp: FillUp): Long

    @androidx.room.Transaction
    suspend fun commitServiceImport(preview: ServiceImport): Int {
        requireNotNull(getVehicleById(preview.vehicleId)) { "Destination vehicle was deleted" }
        val tires = getTireSetSnapshot(preview.vehicleId).groupBy { it.name }
        val known = getMaintenanceSnapshot(preview.vehicleId).map { it.importKey() }.toMutableSet()
        var count = 0
        for (row in preview.rows) {
            val record = row.record ?: continue
            require(record.vehicleId == preview.vehicleId)
            if (record.tireSetId != null) {
                require(tires[row.tireName]?.singleOrNull()?.id == record.tireSetId) { "Tire sets changed. Preview the file again." }
            }
            if (known.add(record.importKey())) {
                insertMaintenance(record)
                count++
            }
        }
        return count
    }

    @Update
    suspend fun updateFillUp(fillUp: FillUp)

    @Delete
    suspend fun deleteFillUp(fillUp: FillUp)

    @Query("SELECT * FROM maintenance WHERE vehicleId = :vehicleId ORDER BY date DESC, odometer DESC")
    fun getMaintenanceForVehicle(vehicleId: Int): Flow<List<Maintenance>>

    @Query("SELECT * FROM maintenance WHERE vehicleId = :vehicleId ORDER BY date DESC, odometer DESC")
    suspend fun getMaintenanceSnapshot(vehicleId: Int): List<Maintenance>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaintenance(maintenance: Maintenance): Long

    @Update
    suspend fun updateMaintenance(maintenance: Maintenance)

    @Delete
    suspend fun deleteMaintenance(maintenance: Maintenance)

    @Query(
        """
        SELECT COUNT(*) FROM maintenance
        WHERE tireSetId = :tireSetId AND type IN ('TIRE_SWAP', 'TIRE_ROTATION')
        """
    )
    suspend fun countTireSetReferences(tireSetId: Int): Int

    @Query("SELECT * FROM tire_sets WHERE vehicleId = :vehicleId ORDER BY name COLLATE NOCASE ASC, id ASC")
    fun getTireSetsForVehicle(vehicleId: Int): Flow<List<TireSet>>

    @Query("SELECT * FROM tire_sets WHERE vehicleId = :vehicleId ORDER BY name COLLATE NOCASE ASC, id ASC")
    suspend fun getTireSetSnapshot(vehicleId: Int): List<TireSet>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTireSet(tireSet: TireSet): Long

    @Update
    suspend fun updateTireSet(tireSet: TireSet)

    @Delete
    suspend fun deleteTireSet(tireSet: TireSet)
}
