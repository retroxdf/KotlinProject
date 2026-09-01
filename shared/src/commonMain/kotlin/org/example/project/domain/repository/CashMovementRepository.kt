package com.abtsplazita.posplazita.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.abtsplazita.posplazita.data.local.CashMovementDao
import com.abtsplazita.posplazita.data.toDomain
import com.abtsplazita.posplazita.data.toEntity
import com.abtsplazita.posplazita.domain.CashMovement
import com.abtsplazita.posplazita.data.remote.FirebaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CashMovementRepository(
    private val cashMovementDao: CashMovementDao,
    private val firebaseManager: FirebaseManager? = null
) {
    private val repositoryScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun getMovements(branchId: String): Flow<List<CashMovement>> {
        return cashMovementDao.getMovementsByBranch(branchId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getMovementsByTerminal(branchId: String, terminalId: String): Flow<List<CashMovement>> {
        return cashMovementDao.getMovementsByTerminal(branchId, terminalId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun saveMovement(movement: CashMovement) {
        cashMovementDao.insertMovement(movement.toEntity().copy(isSynced = false))
        // No sincronizamos de inmediato para ahorrar datos
    }

    suspend fun syncPendingMovementsWithCloud() {
        val unsynced = cashMovementDao.getUnsyncedMovements()
        unsynced.forEach { entity ->
            try {
                firebaseManager?.syncCashMovement(entity.toDomain())
                cashMovementDao.markAsSynced(entity.id)
                println("CLOUD_SYNC: Movimiento ${entity.id} sincronizado.")
            } catch (e: Exception) {
                println("CLOUD_SYNC_ERROR: ${e.message}")
            }
        }
    }

    suspend fun refreshMovements(branchId: String) {
        println("MOV_REPO: Trayendo movimientos de la nube para $branchId...")
        val cloudItems = firebaseManager?.fetchCashMovements(branchId) ?: emptyList()
        if (cloudItems.isNotEmpty()) {
            cloudItems.forEach { movement ->
                cashMovementDao.insertMovement(movement.toEntity().copy(isSynced = true))
            }
            println("MOV_REPO: Sincronización completada (${cloudItems.size} movimientos).")
        }
    }

    fun startCloudSync(branchId: String) {
        // Modo tiempo real deshabilitado para ahorrar datos.
    }
}
