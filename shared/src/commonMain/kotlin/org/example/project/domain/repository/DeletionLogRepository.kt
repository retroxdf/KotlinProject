package com.abtsplazita.posplazita.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.abtsplazita.posplazita.data.local.DeletionLogDao
import com.abtsplazita.posplazita.data.toDomain
import com.abtsplazita.posplazita.data.toEntity
import com.abtsplazita.posplazita.domain.DeletionLog
import com.abtsplazita.posplazita.data.remote.FirebaseManager

class DeletionLogRepository(
    private val deletionLogDao: DeletionLogDao,
    private val firebaseManager: FirebaseManager? = null
) {
    fun getLogs(branchId: String): Flow<List<DeletionLog>> {
        return deletionLogDao.getLogsByBranch(branchId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun saveLog(log: DeletionLog) {
        deletionLogDao.insertLog(log.toEntity())
        firebaseManager?.syncDeletionLog(log)
    }

    suspend fun refreshLogs(branchId: String) {
        println("DELETION_LOG_REPO: Trayendo logs de la nube para $branchId...")
        val cloudItems = firebaseManager?.fetchDeletionLogs(branchId) ?: emptyList()
        if (cloudItems.isNotEmpty()) {
            for (log in cloudItems) {
                deletionLogDao.insertLog(log.toEntity())
            }
            println("DELETION_LOG_REPO: Sincronización completada (${cloudItems.size} logs).")
        }
    }
}
