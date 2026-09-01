package com.abtsplazita.posplazita.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import platform.Foundation.NSHomeDirectory

actual fun getDatabaseBuilder(): RoomDatabase.Builder<PosDatabase> {
    val dbFile = NSHomeDirectory() + "/Documents/pos.db"
    return Room.databaseBuilder<PosDatabase>(
        name = dbFile
    ).fallbackToDestructiveMigration(true)
}
