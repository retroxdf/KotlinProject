package com.abtsplazita.posplazita.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import com.abtsplazita.posplazita.data.local.*
import kotlinx.coroutines.launch
import com.abtsplazita.posplazita.data.*
import com.abtsplazita.posplazita.domain.*
import com.abtsplazita.posplazita.data.remote.FirebaseManager

class ProductRepository(
    private val productDao: ProductDao,
    private val inventoryDao: InventoryDao,
    private val stockMovementDao: StockMovementDao? = null,
    private val categoryDao: CategoryDao? = null,
    private val taxDao: TaxDao? = null,
    private val firebaseManager: FirebaseManager? = null,
    private val scope: kotlinx.coroutines.CoroutineScope? = null
) {
    fun getProducts(): Flow<List<Product>> {
        return productDao.getAllProducts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getProductsPaginated(limit: Int, offset: Int): Flow<List<Product>> {
        return productDao.getProductsPaginated(limit, offset).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getCategories(): Flow<List<String>> {
        return categoryDao?.getAllCategories()?.map { entities ->
            if (entities.isEmpty()) {
                listOf("General", "Abarrotes", "Bebidas", "Limpieza", "Frituras")
            } else {
                entities.map { it.name }
            }
        } ?: flowOf(listOf("General"))
    }

    fun getTaxes(): Flow<List<Double>> {
        return taxDao?.getAllTaxes()?.map { entities ->
            if (entities.isEmpty()) listOf(0.0, 8.0, 16.0)
            else entities.map { it.rate }
        } ?: flowOf(listOf(0.0, 8.0, 16.0)) // Valores por defecto
    }

    suspend fun addCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        
        // Verificar si ya existe para evitar duplicados visuales
        val current = getCategories().first()
        if (current.any { it.equals(trimmed, ignoreCase = true) }) return

        categoryDao?.insertCategory(CategoryEntity("CAT_${com.abtsplazita.posplazita.currentTimeMillis()}", trimmed))
    }

    suspend fun addTax(rate: Double) {
        taxDao?.insertTax(TaxEntity("TAX_${com.abtsplazita.posplazita.currentTimeMillis()}", "${rate}%", rate))
    }

    suspend fun getProductByBarcode(barcode: String): Product? {
        return productDao.getProductByBarcode(barcode)?.toDomain()
    }

    suspend fun getProductById(id: String): Product? {
        return productDao.getProductById(id)?.toDomain()
    }

    fun searchProducts(query: String, limit: Int = 100, offset: Int = 0): Flow<List<Product>> {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) return flowOf(emptyList())

        // Normalizamos la consulta (minúsculas y sin acentos)
        val normalizedQuery = trimmedQuery.normalizeForSearch()
        val fragments = normalizedQuery.split(" ").filter { it.isNotBlank() }

        // Búsqueda híbrida: Cargamos productos y filtramos con normalización para soportar acentos
        return productDao.getAllProducts().map { entities ->
            entities.map { it.toDomain() }
                .filter { product ->
                    val searchArea = "${product.name} ${product.barcode} ${product.barcode2 ?: ""} ${product.barcode3 ?: ""} ${product.barcode4 ?: ""}".normalizeForSearch()
                    // Debe contener todos los fragmentos de la búsqueda
                    fragments.all { fragment -> searchArea.contains(fragment) }
                }
                .sortedByDescending { product ->
                    // SISTEMA DE PUNTUACIÓN DE RELEVANCIA
                    var score = 0
                    val nameNorm = product.name.normalizeForSearch()
                    val barcode = product.barcode.lowercase()
                    
                    // 1. Coincidencia exacta con código de barras (Prioridad Máxima)
                    if (barcode == normalizedQuery) score += 1000
                    
                    // 2. Empieza exactamente con la consulta completa
                    if (nameNorm.startsWith(normalizedQuery)) score += 500
                    
                    // 3. Contiene todos los fragmentos EN ORDEN (ej: "coc 355" en "Coca Cola 355ml")
                    var lastIndex = -1
                    val inOrder = fragments.all { f ->
                        val index = nameNorm.indexOf(f, lastIndex + 1)
                        if (index != -1) {
                            lastIndex = index
                            true
                        } else false
                    }
                    if (inOrder) score += 300

                    // 4. Empieza con el primer fragmento (ej: "coc" -> "coca")
                    if (nameNorm.startsWith(fragments[0])) score += 200
                    
                    // 5. Bonus por cada fragmento que sea una palabra completa exacta
                    val nameWords = nameNorm.split(" ", "-", ".", "/")
                    fragments.forEach { f ->
                        if (nameWords.contains(f)) score += 100
                    }
                    
                    // 6. Coincidencia parcial de código de barras
                    if (barcode.contains(normalizedQuery)) score += 50
                    
                    score
                }
                .drop(offset)
                .take(limit)
        }
    }

    suspend fun getStock(productId: String, branchId: String): Double {
        return inventoryDao.getStock(productId, branchId) ?: 0.0
    }

    fun getAllInventory(): Flow<List<Inventory>> {
        return inventoryDao.getAllInventory().map { entities ->
            entities.map { Inventory(it.productId, it.branchId, it.stock, it.minStock, it.maxStock, it.lastUpdated) }
        }
    }

    suspend fun saveProduct(product: Product, syncWithCloud: Boolean = true) {
        val updated = product.copy(lastUpdated = com.abtsplazita.posplazita.currentTimeMillis())
        productDao.insertProduct(updated.toEntity())
        if (syncWithCloud) {
            firebaseManager?.syncProduct(updated)
        }
    }

    suspend fun syncProductBatch(products: List<Product>) {
        val now = com.abtsplazita.posplazita.currentTimeMillis()
        val updated = products.map { it.copy(lastUpdated = now) }
        productDao.insertProductsBatch(updated.map { it.toEntity() })
        firebaseManager?.syncProductBatch(updated)
    }

    suspend fun syncInventoryBatch(branchId: String, items: List<Inventory>) {
        val now = com.abtsplazita.posplazita.currentTimeMillis()
        val updated = items.map { it.copy(lastUpdated = now) }
        updated.forEach { inventoryDao.updateInventory(it.toEntity()) }
        firebaseManager?.syncInventoryBatch(branchId, updated)
    }

    suspend fun deleteProduct(product: Product) {
        productDao.deleteProduct(product.toEntity())
        firebaseManager?.deleteProduct(product.id)
    }

    suspend fun refreshProducts(isInitial: Boolean = false) {
        if (isInitial) {
            println("PRODUCT_REPO: Realizando descarga inicial COMPLETA del catálogo...")
            val cloudProducts = firebaseManager?.fetchProducts() ?: emptyList()
            if (cloudProducts.isNotEmpty()) {
                productDao.insertProductsBatch(cloudProducts.map { it.toEntity() })
                println("PRODUCT_REPO: Carga inicial completada (${cloudProducts.size} productos).")
            }
            return
        }

        val lastUpdated = productDao.getLastUpdated() ?: 0L
        println("PRODUCT_REPO: Actualizando catálogo incrementalmente (desde $lastUpdated)...")
        
        val cloudProducts = firebaseManager?.fetchProductsIncremental(lastUpdated) ?: emptyList()
        if (cloudProducts.isNotEmpty()) {
            val entities = cloudProducts.map { it.toEntity() }
            productDao.insertProductsBatch(entities)
            println("PRODUCT_REPO: Catálogo actualizado (${cloudProducts.size} productos nuevos/editados).")
        } else {
            println("PRODUCT_REPO: Catálogo ya está al día.")
        }
    }

    suspend fun refreshInventory(branchId: String, isInitial: Boolean = false) {
        if (isInitial) {
            println("PRODUCT_REPO: Realizando descarga inicial COMPLETA de inventario sucursal $branchId...")
            val cloudInventory = firebaseManager?.fetchInventory(branchId) ?: emptyList()
            if (cloudInventory.isNotEmpty()) {
                for (inv in cloudInventory) {
                    inventoryDao.updateInventory(inv.toEntity())
                }
                println("PRODUCT_REPO: Inventario inicial cargado (${cloudInventory.size} registros).")
            }
            return
        }

        val lastUpdated = inventoryDao.getLastUpdated(branchId) ?: 0L
        println("PRODUCT_REPO: Actualizando existencias incrementalmente para $branchId (desde $lastUpdated)...")
        
        val cloudInventory = firebaseManager?.fetchInventoryIncremental(branchId, lastUpdated) ?: emptyList()
        if (cloudInventory.isNotEmpty()) {
            // Usar un bucle for tradicional para permitir llamadas suspendidas (updateInventory)
            for (inv in cloudInventory) {
                inventoryDao.updateInventory(inv.toEntity())
            }
            println("PRODUCT_REPO: Existencias actualizadas (${cloudInventory.size} registros).")
        } else {
            println("PRODUCT_REPO: Existencias ya están al día.")
        }
    }

    fun startIncrementalSync(branchId: String) {
        val activeScope = scope ?: kotlinx.coroutines.GlobalScope
        println("PRODUCT_REPO: Iniciando observación incremental para sucursal $branchId...")
        
        activeScope.launch {
            try {
                // 1. Observar Productos
                val productSince = productDao.getLastUpdated() ?: 0L
                firebaseManager?.observeProductsIncremental(productSince) { cloudProducts ->
                    if (cloudProducts.isNotEmpty()) {
                        activeScope.launch {
                            productDao.insertProductsBatch(cloudProducts.map { it.toEntity() })
                            println("PRODUCT_REPO: ${cloudProducts.size} productos actualizados incrementalmente.")
                        }
                    }
                }

                // 2. Observar Inventario
                val inventorySince = inventoryDao.getLastUpdated(branchId) ?: 0L
                firebaseManager?.observeInventoryIncremental(branchId, inventorySince) { cloudInventory ->
                    if (cloudInventory.isNotEmpty()) {
                        activeScope.launch {
                            cloudInventory.forEach { inv ->
                                inventoryDao.updateInventory(inv.toEntity())
                            }
                            println("PRODUCT_REPO: ${cloudInventory.size} registros de inventario actualizados incrementalmente.")
                        }
                    }
                }
            } catch (e: Exception) {
                println("PRODUCT_REPO_SYNC_ERROR: ${e.message}")
            }
        }
    }

    fun startCloudSync() {
        // El modo tiempo real se deshabilita para ahorrar datos.
        // Ahora se usa polling cada 15 min vía SyncManager.
    }

    suspend fun updateStock(
        productId: String, 
        branchId: String, 
        newStock: Double, 
        userId: String = "system", 
        type: MovementType = MovementType.ADJUSTMENT,
        reason: String? = null,
        syncWithCloud: Boolean = true
    ) {
        val current = inventoryDao.getStock(productId, branchId) ?: 0.0
        val currentMin = 0.0 // Default si no existe
        val currentMax = 0.0
        
        // Intentar obtener el registro existente para no perder min/max
        val existing = inventoryDao.getAllInventory().first().find { it.productId == productId && it.branchId == branchId }
        
        val now = com.abtsplazita.posplazita.currentTimeMillis()
        val updatedInventory = Inventory(
            productId = productId, 
            branchId = branchId, 
            stock = newStock,
            minStock = existing?.minStock ?: 0.0,
            maxStock = existing?.maxStock ?: 0.0,
            lastUpdated = now
        )
        inventoryDao.updateInventory(updatedInventory.toEntity())
        
        if (syncWithCloud) {
            firebaseManager?.syncInventory(updatedInventory)
        }
        
        val diff = newStock - current
        if (diff != 0.0) {
            val movement = StockMovement(
                id = now,
                productId = productId,
                branchId = branchId,
                type = if (type == MovementType.ADJUSTMENT && diff > 0) MovementType.IN_PURCHASE else type,
                quantity = kotlin.math.abs(diff),
                timestamp = com.abtsplazita.posplazita.currentTimeMillis(),
                userId = userId,
                reason = reason ?: (if (type == MovementType.OUT_SALE) "Venta" else "Ajuste de inventario")
            )
            stockMovementDao?.insertMovement(movement.toEntity())
            firebaseManager?.syncStockMovement(movement)
        }
    }

    suspend fun updateStockLimits(productId: String, branchId: String, min: Double, max: Double) {
        val existingStock = inventoryDao.getStock(productId, branchId) ?: 0.0
        val now = com.abtsplazita.posplazita.currentTimeMillis()
        val updatedInventory = Inventory(
            productId = productId,
            branchId = branchId,
            stock = existingStock,
            minStock = min,
            maxStock = max,
            lastUpdated = now
        )
        inventoryDao.updateInventory(updatedInventory.toEntity())
        firebaseManager?.syncInventory(updatedInventory)
    }

    suspend fun decreaseStock(productId: String, branchId: String, quantity: Double, userId: String = "system", reason: String? = null) {
        val currentStock = getStock(productId, branchId)
        updateStock(productId, branchId, currentStock - quantity, userId, MovementType.OUT_SALE, reason ?: "Venta")
    }

    suspend fun increaseStock(productId: String, branchId: String, quantity: Double, userId: String = "system", reason: String? = null) {
        val currentStock = getStock(productId, branchId)
        updateStock(productId, branchId, currentStock + quantity, userId, MovementType.IN_PURCHASE, reason ?: "Entrada")
    }
}
