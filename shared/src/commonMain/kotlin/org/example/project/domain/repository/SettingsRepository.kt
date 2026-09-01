package com.abtsplazita.posplazita.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.abtsplazita.posplazita.data.local.AppSettingsDao
import com.abtsplazita.posplazita.data.local.AppSettingsEntity

class SettingsRepository(private val settingsDao: AppSettingsDao) {

    suspend fun saveSetting(key: String, value: String) {
        settingsDao.saveSetting(AppSettingsEntity(key, value))
    }

    suspend fun getSetting(key: String): String? {
        return settingsDao.getSetting(key)
    }

    fun getAllSettings(): Flow<Map<String, String>> {
        return settingsDao.getAllSettings().map { list ->
            list.associate { it.key to it.value }
        }
    }
}
