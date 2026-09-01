package com.abtsplazita.posplazita.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.MainScope
import com.abtsplazita.posplazita.data.local.StockMovementDao
import com.abtsplazita.posplazita.data.toDomain
import com.abtsplazita.posplazita.data.toEntity
import com.abtsplazita.posplazita.domain.StockMovement
import com.abtsplazita.posplazita.data.remote.FirebaseManager

class StockMovementRepository(
    private val movementDao: StockMovementDao,
    private val firebaseManager: FirebaseManager? = null
) {
    fun getMovements(productId: String): Flow<List<StockMovement>> {
        return movementDao.getMovementsByProduct(productId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getMovementsByBranch(branchId: String): Flow<List<StockMovement>> {
        return movementDao.getMovementsByBranch(branchId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun recordMovement(movement: StockMovement) {
        movementDao.insertMovement(movement.toEntity())
        firebaseManager?.syncStockMovement(movement)
    }

    fun startCloudSync() {
        firebaseManager?.observeStockMovements { cloudMovements ->
            MainScope().launch {
                cloudMovements.forEach { movement ->
                    movementDao.insertMovement(movement.toEntity())
                }
            }
        }
    }
}
