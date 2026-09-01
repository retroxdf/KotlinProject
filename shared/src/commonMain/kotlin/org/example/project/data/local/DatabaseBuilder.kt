package com.abtsplazita.posplazita.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

expect fun getDatabaseBuilder(): RoomDatabase.Builder<PosDatabase>

fun createDatabase(): PosDatabase {
    return getDatabaseBuilder()
        .setDriver(BundledSQLiteDriver())
        .addMigrations(
            PosDatabase.MIGRATION_35_36, 
            PosDatabase.MIGRATION_36_37, 
            PosDatabase.MIGRATION_37_38,
            PosDatabase.MIGRATION_38_40,
            PosDatabase.MIGRATION_40_41, 
            PosDatabase.MIGRATION_41_42,
            PosDatabase.MIGRATION_42_43,
            PosDatabase.MIGRATION_43_44,
            PosDatabase.MIGRATION_44_45,
            PosDatabase.MIGRATION_45_46
        )
        .fallbackToDestructiveMigration(true)
        .build()
}
