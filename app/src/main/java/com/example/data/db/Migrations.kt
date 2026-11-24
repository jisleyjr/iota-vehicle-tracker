package com.example.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Copies service history forward. Existing rows keep their cost, odometer, date, and hours.
 * `whatWasDone` becomes `title`, and `type` is `OTHER` so the upgrade does not guess a category.
 * `tireSetId` is a plain nullable column (no foreign key) so deleting a vehicle can still cascade
 * service rows and tire sets without a restrict cycle.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `tire_sets` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `vehicleId` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `brand` TEXT,
                `size` TEXT,
                `purchaseDate` INTEGER,
                FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_tire_sets_vehicleId` ON `tire_sets` (`vehicleId`)"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `maintenance_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `vehicleId` INTEGER NOT NULL,
                `title` TEXT NOT NULL,
                `cost` REAL NOT NULL,
                `odometer` REAL NOT NULL,
                `date` INTEGER NOT NULL,
                `hours` REAL,
                `type` TEXT NOT NULL,
                `description` TEXT,
                `tireSetId` INTEGER,
                FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `maintenance_new` (
                `id`, `vehicleId`, `title`, `cost`, `odometer`, `date`, `hours`, `type`, `description`, `tireSetId`
            )
            SELECT `id`, `vehicleId`, `whatWasDone`, `cost`, `odometer`, `date`, `hours`, 'OTHER', NULL, NULL
            FROM `maintenance`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `maintenance`")
        db.execSQL("ALTER TABLE `maintenance_new` RENAME TO `maintenance`")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_maintenance_vehicleId` ON `maintenance` (`vehicleId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_maintenance_tireSetId` ON `maintenance` (`tireSetId`)"
        )
    }
}
