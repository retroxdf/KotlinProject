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
    private val productRepository: ProductRepository
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
        val entity = purchase.toEntity()
        val items = purchase.items.map { it.toEntity(purchase.id) }
        purchaseDao.insertPurchaseWithItems(entity, items)

        // Actualizar stock de cada producto
        purchase.items.forEach { item ->
            val currentStock = productRepository.getStock(item.productId, purchase.branchId)
            productRepository.updateStock(
                productId = item.productId,
                branchId = purchase.branchId,
                newStock = currentStock + item.quantity,
                userId = purchase.userId,
                type = MovementType.IN_PURCHASE,
                reason = "Compra #${purchase.id}"
            )
        }
    }

    suspend fun updatePurchaseStatus(purchase: Purchase, newStatus: PurchaseStatus) {
        purchaseDao.updatePurchase(purchase.copy(status = newStatus).toEntity())
    }

    suspend fun getNextPurchaseId(): String {
        val count = purchaseDao.getPurchasesCount()
        return (count + 1).toString()
    }
}
