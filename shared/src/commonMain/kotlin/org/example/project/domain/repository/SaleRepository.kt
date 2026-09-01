package com.abtsplazita.posplazita.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.abtsplazita.posplazita.data.toDomain
import com.abtsplazita.posplazita.data.toEntity
import com.abtsplazita.posplazita.data.toHeldEntity
import com.abtsplazita.posplazita.data.local.SaleDao
import com.abtsplazita.posplazita.data.local.HeldSaleDao
import com.abtsplazita.posplazita.domain.Sale
import com.abtsplazita.posplazita.domain.SaleItem
import com.abtsplazita.posplazita.domain.HeldSale
import com.abtsplazita.posplazita.data.remote.FirebaseManager

class SaleRepository(
    private val saleDao: SaleDao,
    private val heldSaleDao: HeldSaleDao? = null,
    private val firebaseManager: FirebaseManager? = null
) {
    private val repositoryScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    suspend fun saveSale(sale: Sale) {
        val saleEntity = sale.toEntity().copy(isSynced = false)
        val itemEntities = sale.items.map { it.toEntity(sale.id) }
        
        saleDao.insertSaleWithItems(saleEntity, itemEntities)
        // No sincronizamos de inmediato para ahorrar datos
    }

    suspend fun syncPendingSalesWithCloud() {
        val unsynced = saleDao.getUnsyncedSales()
        unsynced.forEach { entity ->
            try {
                val items = saleDao.getItemsBySale(entity.id).map { it.toDomain() }
                val sale = entity.toDomain(items)
                firebaseManager?.syncSale(sale)
                saleDao.markAsSynced(entity.id)
                println("CLOUD_SYNC: Venta ${entity.id} sincronizada.")
            } catch (e: Exception) {
                println("CLOUD_SYNC_ERROR: ${e.message}")
            }
        }
    }

    suspend fun getSaleById(id: String): Sale? {
        val entity = saleDao.getSaleById(id) ?: return null
        val items = getSaleItems(id)
        return entity.toDomain(items)
    }

    fun searchSales(branchId: String, query: String): Flow<List<Sale>> {
        return saleDao.searchSales(branchId, query).map { entities ->
            entities.map { it.toDomain(emptyList()) }
        }
    }

    fun getSales(branchId: String): Flow<List<Sale>> {
        return saleDao.getSalesByBranch(branchId).map { entities ->
            // Necesitamos los items para reportes/dashboard
            entities.map { entity ->
                val items = kotlinx.coroutines.runBlocking { getSaleItems(entity.id) }
                entity.toDomain(items)
            }
        }
    }

    fun getSalesByTerminal(branchId: String, terminalId: String): Flow<List<Sale>> {
        return saleDao.getSalesByTerminal(branchId, terminalId).map { entities ->
            entities.map { it.toDomain(emptyList()) }
        }
    }

    fun getSalesByCustomer(customerId: String): Flow<List<Sale>> {
        return saleDao.getSalesByCustomer(customerId).map { entities ->
            entities.map { it.toDomain(emptyList()) }
        }
    }

    suspend fun getSaleItems(saleId: String): List<SaleItem> {
        return saleDao.getItemsBySale(saleId).map { it.toDomain() }
    }

    suspend fun getNextSaleId(): String {
        val count = saleDao.getSalesCount()
        return (count + 1).toString()
    }

    fun generateUniqueSaleId(branchId: String, terminalId: String?, prefix: String = "S"): String {
        val now = com.abtsplazita.posplazita.currentTimeMillis()
        val bid = if (branchId.length > 3) branchId.takeLast(3) else branchId
        val tid = terminalId?.takeLast(1) ?: "0"
        
        // Formato: PREFIX-BRANCH(3)-TERM(1)-RANDOM(5)
        // Ejemplo: S-809-1-49215
        val random = (10000..99999).random()
        return "$prefix-$bid-$tid-$random"
    }

    // --- Ventas en Espera (Local) ---
    fun getHeldSales(branchId: String): Flow<List<HeldSale>> {
        return heldSaleDao?.getHeldSalesByBranch(branchId)?.map { entities ->
            entities.map { it.toDomain(emptyList()) }
        } ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }

    suspend fun getHeldSaleItems(heldSaleId: String): List<SaleItem> {
        return heldSaleDao?.getItemsByHeldSale(heldSaleId)?.map { it.toDomain() } ?: emptyList()
    }

    suspend fun saveHeldSale(heldSale: HeldSale) {
        val entity = heldSale.toEntity()
        val itemEntities = heldSale.items.map { it.toHeldEntity(heldSale.id) }
        heldSaleDao?.saveHeldSale(entity, itemEntities)
    }

    suspend fun deleteHeldSale(id: String) {
        heldSaleDao?.removeHeldSale(id)
    }

    suspend fun refreshSales(branchId: String) {
        println("SALE_REPO: Sincronizando ventas desde la nube...")
        val cloudSales = firebaseManager?.fetchSales(branchId) ?: emptyList()
        if (cloudSales.isEmpty()) return
        
        // Optimización: Preparar lote para una sola transacción
        val salesBatch = cloudSales.map { sale ->
            val saleEntity = sale.toEntity().copy(isSynced = true)
            val itemEntities = sale.items.map { it.toEntity(sale.id) }
            saleEntity to itemEntities
        }
        
        saleDao.insertSalesBatch(salesBatch)
        println("SALE_REPO: Sincronización completada (${cloudSales.size} ventas).")
    }

    fun startCloudSync(branchId: String) {
        // Modo tiempo real deshabilitado para ahorrar datos.
        // Se usa carga bajo demanda en reportes.
    }
}
