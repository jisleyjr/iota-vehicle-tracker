package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val make: String,
    val model: String,
    val year: Int = 0,
    val licensePlate: String = ""
) {
    /** Name shown on the garage card and used for alphabetical ordering. */
    fun garageName(): String = "$year $make $model"
}

fun List<Vehicle>.sortedForGarage(): List<Vehicle> =
    sortedWith(
        compareBy<Vehicle, String>(String.CASE_INSENSITIVE_ORDER) { it.garageName() }
            .thenBy { it.id }
    )
