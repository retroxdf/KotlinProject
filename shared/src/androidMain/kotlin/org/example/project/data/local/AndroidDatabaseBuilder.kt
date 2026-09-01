package com.abtsplazita.posplazita.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

lateinit var databaseContext: android.content.Context
var currentActivity: android.app.Activity? = null

actual fun getDatabaseBuilder(): RoomDatabase.Builder<PosDatabase> {
    val appContext = databaseContext.applicationContext
    val dbFile = appContext.getDatabasePath("pos.db")
    return Room.databaseBuilder<PosDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    ).fallbackToDestructiveMigration(true)
}
