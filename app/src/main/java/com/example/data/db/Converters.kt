package com.example.data.db

import androidx.room.TypeConverter
import com.example.data.model.MaintenanceType

class Converters {
    @TypeConverter
    fun maintenanceTypeToString(type: MaintenanceType): String = type.name

    @TypeConverter
    fun maintenanceTypeFromString(value: String): MaintenanceType = MaintenanceType.fromStorage(value)
}
