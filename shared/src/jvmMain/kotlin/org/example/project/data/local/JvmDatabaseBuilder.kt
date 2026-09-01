package com.abtsplazita.posplazita.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

actual fun getDatabaseBuilder(): RoomDatabase.Builder<PosDatabase> {
    val dbFile = File(System.getProperty("user.home"), "pos.db")
    return Room.databaseBuilder<PosDatabase>(
        name = dbFile.absolutePath
    ).fallbackToDestructiveMigration(true)
}
