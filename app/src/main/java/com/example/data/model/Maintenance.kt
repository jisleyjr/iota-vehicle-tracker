package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MaintenanceType {
    OIL_CHANGE,
    REPAIR,
    TIRE_ROTATION,
    TIRE_SWAP,
    OTHER;

    val label: String
        get() = when (this) {
            OIL_CHANGE -> "Oil change"
            REPAIR -> "Repair"
            TIRE_ROTATION -> "Tire rotation"
            TIRE_SWAP -> "Tire swap"
            OTHER -> "Other"
        }

    companion object {
        fun fromStorage(value: String?): MaintenanceType {
            if (value.isNullOrBlank()) return OTHER
            return entries.find { it.name == value } ?: OTHER
        }
    }
}

@Entity(
    tableName = "maintenance",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["vehicleId"]), Index(value = ["tireSetId"])]
)
data class Maintenance(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val vehicleId: Int,
    val title: String,
    val cost: Double,
    val odometer: Double,
    val date: Long,
    val hours: Double? = null,
    val type: MaintenanceType = MaintenanceType.OTHER,
    val description: String? = null,
    val tireSetId: Int? = null
)
