package com.abtsplazita.posplazita.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.abtsplazita.posplazita.data.local.PreCutDao
import com.abtsplazita.posplazita.data.toDomain
import com.abtsplazita.posplazita.data.toEntity
import com.abtsplazita.posplazita.domain.PreCut
import com.abtsplazita.posplazita.data.remote.FirebaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PreCutRepository(
    private val preCutDao: PreCutDao,
    private val firebaseManager: FirebaseManager? = null
) {
    private val repositoryScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun getPreCuts(branchId: String): Flow<List<PreCut>> {
        return preCutDao.getPreCutsByBranch(branchId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun savePreCut(preCut: PreCut) {
        preCutDao.insertPreCut(preCut.toEntity())
        firebaseManager?.syncPreCut(preCut)
    }

    suspend fun getPreCutCountForUserToday(userId: String): Int {
        val now = com.abtsplazita.posplazita.currentTimeMillis()
        // Obtener el inicio del día local (aproximado usando milisegundos de un día)
        // O mejor, calcular el inicio del día real si es posible.
        // Por simplicidad, tomamos las últimas 24 horas o usamos una utilidad de fecha.
        val todayStart = com.abtsplazita.posplazita.getStartOfDay() 
        return preCutDao.getPreCutCountForUserSince(userId, todayStart)
    }

    suspend fun refreshPreCuts(branchId: String) {
        println("PRECUT_REPO: Trayendo precortes de la nube para $branchId...")
        val cloudItems = firebaseManager?.fetchPreCuts(branchId) ?: emptyList()
        if (cloudItems.isNotEmpty()) {
            cloudItems.forEach { preCut ->
                preCutDao.insertPreCut(preCut.toEntity().copy(isSynced = true))
            }
            println("PRECUT_REPO: Sincronización completada (${cloudItems.size} precortes).")
        }
    }

    fun startCloudSync(branchId: String) {
        // Modo tiempo real deshabilitado para ahorrar datos.
    }
}
