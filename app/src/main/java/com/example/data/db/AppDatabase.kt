package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.FillUp
import com.example.data.model.Maintenance
import com.example.data.model.TireSet
import com.example.data.model.Vehicle

@Database(
    entities = [Vehicle::class, FillUp::class, Maintenance::class, TireSet::class],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao

    companion object {
        private const val DATABASE_NAME = "vehicle_tracker_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Version 4 upgrades through [MIGRATION_4_5]. Versions 1–3 still use a destructive
         * fallback; version 4 is not in that list, so a missing 4→5 migration cannot wipe the garage.
         */
        internal fun builder(context: Context, name: String = DATABASE_NAME) =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, name)
                .addMigrations(MIGRATION_4_5)
                .fallbackToDestructiveMigrationFrom(true, 1, 2, 3)

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = builder(context).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
