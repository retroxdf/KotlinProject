package com.abtsplazita.posplazita.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.abtsplazita.posplazita.data.local.PurchaseDao
import com.abtsplazita.posplazita.data.local.PurchaseEntity
import com.abtsplazita.posplazita.data.toDomain
import com.abtsplazita.posplazita.data.toEntity
import com.abtsplazita.posplazita.domain.Purchase
import com.abtsplazita.posplazita.domain.PurchaseItem
import com.abtsplazita.posplazita.domain.PurchaseStatus
import com.abtsplazita.posplazita.domain.MovementType
import com.abtsplazita.posplazita.domain.StockMovement

class PurchaseRepository(
    private val purchaseDao: PurchaseDao,
    private val productRepository: ProductRepository,
    private val firebaseManager: com.abtsplazita.posplazita.data.remote.FirebaseManager? = null
) {
    fun getPurchases(branchId: String): Flow<List<Purchase>> {
        return purchaseDao.getPurchasesByBranch(branchId).map { entities ->
            entities.map { it.toDomain(emptyList()) }
        }
    }

    fun getPurchasesBySupplier(supplierId: String): Flow<List<Purchase>> {
        return purchaseDao.getPurchasesBySupplier(supplierId).map { entities ->
            entities.map { it.toDomain(emptyList()) }
        }
    }

    suspend fun getPurchaseItems(purchaseId: String): List<PurchaseItem> {
        return purchaseDao.getItemsByPurchase(purchaseId).map { it.toDomain() }
    }

    suspend fun savePurchase(purchase: Purchase) {
        val updated = purchase.copy(lastUpdated = com.abtsplazita.posplazita.currentTimeMillis())
        val entity = updated.toEntity()
        val items = updated.items.map { it.toEntity(updated.id) }
        purchaseDao.insertPurchaseWithItems(entity, items)
        
        firebaseManager?.syncPurchase(updated)

        // Actualizar stock de cada producto
        updated.items.forEach { item ->
            val currentStock = productRepository.getStock(item.productId, updated.branchId)
            productRepository.updateStock(
                productId = item.productId,
                branchId = updated.branchId,
                newStock = currentStock + item.quantity,
                userId = updated.userId,
                type = MovementType.IN_PURCHASE,
                reason = "Compra #${updated.id}"
            )
        }
    }

    suspend fun refreshPurchases(branchId: String) {
        println("PURCHASE_REPO: Sincronizando compras desde la nube...")
        val cloudPurchases = firebaseManager?.fetchPurchases(branchId) ?: emptyList()
        if (cloudPurchases.isNotEmpty()) {
            cloudPurchases.forEach { purchase ->
                val entity = purchase.toEntity()
                val items = purchase.items.map { it.toEntity(purchase.id) }
                purchaseDao.insertPurchaseWithItems(entity, items)
            }
            println("PURCHASE_REPO: Compras actualizadas (${cloudPurchases.size}).")
        }
    }

    suspend fun updatePurchaseStatus(purchase: Purchase, newStatus: PurchaseStatus) {
        val updated = purchase.copy(status = newStatus, lastUpdated = com.abtsplazita.posplazita.currentTimeMillis())
        purchaseDao.updatePurchase(updated.toEntity())
        firebaseManager?.syncPurchase(updated)
    }

    suspend fun getNextPurchaseId(): String {
        val count = purchaseDao.getPurchasesCount()
        return (count + 1).toString()
    }
}
