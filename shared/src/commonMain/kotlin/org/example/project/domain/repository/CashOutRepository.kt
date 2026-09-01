package com.abtsplazita.posplazita.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.abtsplazita.posplazita.data.local.CashOutDao
import com.abtsplazita.posplazita.data.toDomain
import com.abtsplazita.posplazita.data.toEntity
import com.abtsplazita.posplazita.domain.CashOut
import com.abtsplazita.posplazita.data.remote.FirebaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CashOutRepository(
    private val cashOutDao: CashOutDao,
    private val firebaseManager: FirebaseManager? = null
) {
    private val repositoryScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun getCashOuts(branchId: String): Flow<List<CashOut>> {
        return cashOutDao.getCashOutsByBranch(branchId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun saveCashOut(cashOut: CashOut) {
        cashOutDao.insertCashOut(cashOut.toEntity())
        firebaseManager?.syncCashOut(cashOut)
    }

    suspend fun refreshCashOuts(branchId: String) {
        println("CASHOUT_REPO: Trayendo cortes de la nube para $branchId...")
        val cloudItems = firebaseManager?.fetchCashOuts(branchId) ?: emptyList()
        if (cloudItems.isNotEmpty()) {
            cloudItems.forEach { cashOut ->
                cashOutDao.insertCashOut(cashOut.toEntity().copy(isSynced = true))
            }
            println("CASHOUT_REPO: Sincronización completada (${cloudItems.size} cortes).")
        }
    }

    fun startCloudSync(branchId: String) {
        // Modo tiempo real deshabilitado para ahorrar datos.
    }
}
