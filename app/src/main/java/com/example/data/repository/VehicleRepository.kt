package com.example.data.repository

import com.example.data.db.VehicleDao
import com.example.data.model.Vehicle
import com.example.data.model.FillUp
import com.example.data.model.Maintenance
import kotlinx.coroutines.flow.Flow

class VehicleRepository(private val vehicleDao: VehicleDao) {
    val allVehicles: Flow<List<Vehicle>> = vehicleDao.getAllVehicles()

    suspend fun getVehicleById(vehicleId: Int): Vehicle? {
        return vehicleDao.getVehicleById(vehicleId)
    }

    suspend fun insertVehicle(vehicle: Vehicle): Long {
        return vehicleDao.insertVehicle(vehicle)
    }

    suspend fun deleteVehicle(vehicle: Vehicle) {
        vehicleDao.deleteVehicle(vehicle)
    }

    fun getFillUpsForVehicle(vehicleId: Int): Flow<List<FillUp>> {
        return vehicleDao.getFillUpsForVehicle(vehicleId)
    }

    suspend fun insertFillUp(fillUp: FillUp): Long {
        return vehicleDao.insertFillUp(fillUp)
    }

    suspend fun deleteFillUp(fillUp: FillUp) {
        vehicleDao.deleteFillUp(fillUp)
    }

    fun getMaintenanceForVehicle(vehicleId: Int): Flow<List<Maintenance>> {
        return vehicleDao.getMaintenanceForVehicle(vehicleId)
    }

    suspend fun insertMaintenance(maintenance: Maintenance): Long {
        return vehicleDao.insertMaintenance(maintenance)
    }

    suspend fun deleteMaintenance(maintenance: Maintenance) {
        vehicleDao.deleteMaintenance(maintenance)
    }
}
