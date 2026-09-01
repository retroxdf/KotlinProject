package com.abtsplazita.posplazita.data.remote

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.abtsplazita.posplazita.domain.Product
import com.abtsplazita.posplazita.domain.Sale
import com.abtsplazita.posplazita.domain.Customer
import com.abtsplazita.posplazita.domain.User
import com.abtsplazita.posplazita.domain.StockMovement
import com.abtsplazita.posplazita.domain.Branch
import com.abtsplazita.posplazita.domain.PosTerminal
import com.abtsplazita.posplazita.domain.HeldSale
import com.abtsplazita.posplazita.domain.ProductReturn
import com.abtsplazita.posplazita.domain.Inventory
import com.abtsplazita.posplazita.domain.WebOrder
import com.abtsplazita.posplazita.domain.DeletionRequest
import com.abtsplazita.posplazita.domain.DeletionLog

class AndroidCloudProvider : CloudProvider {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val auth by lazy { Firebase.auth }
    private val firestore by lazy { Firebase.firestore }

    init {
        scope.launch {
            try {
                auth.signInAnonymously()
                println("CLOUD_ANDROID: Sesión iniciada.")
            } catch (e: Exception) {
                println("CLOUD_ANDROID_AUTH_ERROR: ${e.message}")
            }
        }
    }

    override fun syncProduct(product: Product) {
        scope.launch {
            try {
                val updated = product.copy(lastUpdated = com.abtsplazita.posplazita.currentTimeMillis())
                firestore.collection("products").document(product.id).set(Product.serializer(), updated)
            } catch (e: Exception) {
                println("CLOUD_ANDROID_SYNC_ERROR: ${e.message}")
            }
        }
    }

    override fun observeProductsIncremental(since: Long, onUpdate: (List<Product>) -> Unit) {
        scope.launch {
            try {
                firestore.collection("products")
                    .where { "lastUpdated" greaterThan since }
                    .snapshots().collect { snapshot ->
                        onUpdate(snapshot.documents.mapNotNull { 
                            try { it.data(Product.serializer()) } catch (e: Exception) { null }
                        })
                    }
            } catch (e: Exception) {}
        }
    }

    override fun deleteProduct(id: String) {
        scope.launch {
            try {
                firestore.collection("products").document(id).delete()
            } catch (e: Exception) {
                println("CLOUD_ANDROID_DELETE_ERROR: ${e.message}")
            }
        }
    }

    override fun deleteBranch(id: String) {
        scope.launch {
            try {
                firestore.collection("branches").document(id).delete()
            } catch (e: Exception) {
                println("CLOUD_ANDROID_BRANCH_DELETE_ERROR: ${e.message}")
            }
        }
    }

    override fun observeProducts(onUpdate: (List<Product>) -> Unit) {
        scope.launch {
            try {
                firestore.collection("products").snapshots().collect { snapshot ->
                    onUpdate(snapshot.documents.mapNotNull { 
                        try { it.data(Product.serializer()) } catch (e: Exception) { null }
                    })
                }
            } catch (e: Exception) {}
        }
    }

    override fun syncSale(sale: Sale) {
        scope.launch {
            try {
                println("CLOUD_ANDROID: Sincronizando venta ${sale.id}...")
                firestore.collection("sales").document(sale.id).set(Sale.serializer(), sale)
            } catch (e: Exception) {
                println("CLOUD_ANDROID: Error al sincronizar venta: ${e.message}")
            }
        }
    }

    override fun observeSales(branchId: String, onUpdate: (List<Sale>) -> Unit) {
        // Deshabilitado tiempo real para ahorrar recursos. Se usa fetchSales bajo demanda.
    }

    override fun syncCustomer(customer: Customer) {
        scope.launch {
            try {
                val updated = customer.copy(lastUpdated = com.abtsplazita.posplazita.currentTimeMillis())
                firestore.collection("customers").document(customer.id).set(Customer.serializer(), updated)
            } catch (e: Exception) {}
        }
    }

    override fun observeCustomersIncremental(since: Long, onUpdate: (List<Customer>) -> Unit) {
        scope.launch {
            try {
                firestore.collection("customers")
                    .where { "lastUpdated" greaterThan since }
                    .snapshots().collect { snapshot ->
                        onUpdate(snapshot.documents.map { it.data(Customer.serializer()) })
                    }
            } catch (e: Exception) {}
        }
    }

    override fun deleteCustomer(id: String) {
        scope.launch {
            try {
                firestore.collection("customers").document(id).delete()
            } catch (e: Exception) {}
        }
    }

    override fun observeCustomers(onUpdate: (List<Customer>) -> Unit) {
        scope.launch {
            try {
                firestore.collection("customers").snapshots().collect { snapshot ->
                    onUpdate(snapshot.documents.map { it.data(Customer.serializer()) })
                }
            } catch (e: Exception) {}
        }
    }

    override fun syncUser(user: User) {
        scope.launch {
            try {
                val updated = user.copy(lastUpdated = com.abtsplazita.posplazita.currentTimeMillis())
                firestore.collection("users").document(user.id).set(User.serializer(), updated)
            } catch (e: Exception) {}
        }
    }

    override fun observeUsersIncremental(since: Long, onUpdate: (List<User>) -> Unit) {
        scope.launch {
            try {
                firestore.collection("users")
                    .where { "lastUpdated" greaterThan since }
                    .snapshots().collect { snapshot ->
                        onUpdate(snapshot.documents.map { it.data(User.serializer()) })
                    }
            } catch (e: Exception) {}
        }
    }

    override fun observeUsers(onUpdate: (List<User>) -> Unit) {
        scope.launch {
            try {
                firestore.collection("users").snapshots().collect { snapshot ->
                    onUpdate(snapshot.documents.map { it.data(User.serializer()) })
                }
            } catch (e: Exception) {}
        }
    }

    override fun syncStockMovement(movement: StockMovement) {
        scope.launch {
            try {
                firestore.collection("movements").document(movement.id.toString()).set(StockMovement.serializer(), movement)
            } catch (e: Exception) {}
        }
    }

    override fun observeStockMovements(onUpdate: (List<StockMovement>) -> Unit) {
        scope.launch {
            try {
                firestore.collection("movements").snapshots().collect { snapshot ->
                    onUpdate(snapshot.documents.map { it.data(StockMovement.serializer()) })
                }
            } catch (e: Exception) {}
        }
    }

    override fun syncBranch(branch: Branch) {
        scope.launch {
            try {
                val updated = branch.copy(lastUpdated = com.abtsplazita.posplazita.currentTimeMillis())
                firestore.collection("branches").document(branch.id).set(Branch.serializer(), updated)
            } catch (e: Exception) {}
        }
    }

    override fun observeBranchesIncremental(since: Long, onUpdate: (List<Branch>) -> Unit) {
        scope.launch {
            try {
                firestore.collection("branches")
                    .where { "lastUpdated" greaterThan since }
                    .snapshots().collect { snapshot ->
                        onUpdate(snapshot.documents.map { it.data(Branch.serializer()) })
                    }
            } catch (e: Exception) {}
        }
    }

    override fun observeBranches(onUpdate: (List<Branch>) -> Unit) {
        scope.launch {
            try {
                firestore.collection("branches").snapshots().collect { snapshot ->
                    onUpdate(snapshot.documents.map { it.data(Branch.serializer()) })
                }
            } catch (e: Exception) {}
        }
    }

    override fun syncTerminal(terminal: PosTerminal) {
        scope.launch {
            try {
                firestore.collection("terminals").document(terminal.id).set(PosTerminal.serializer(), terminal)
            } catch (e: Exception) {}
        }
    }

    override fun deleteTerminal(id: String) {
        scope.launch {
            try {
                firestore.collection("terminals").document(id).delete()
            } catch (e: Exception) {}
        }
    }

    override fun observeTerminals(branchId: String, onUpdate: (List<PosTerminal>) -> Unit) {
        scope.launch {
            try {
                firestore.collection("terminals")
                    .where { "branchId" equalTo branchId }
                    .snapshots().collect { snapshot ->
                        onUpdate(snapshot.documents.map { it.data(PosTerminal.serializer()) })
                    }
            } catch (e: Exception) {}
        }
    }

    override fun syncHeldSale(heldSale: HeldSale) {
        scope.launch {
            try {
                firestore.collection("held_sales").document(heldSale.id).set(HeldSale.serializer(), heldSale)
            } catch (e: Exception) {}
        }
    }

    override fun deleteHeldSale(id: String) {
        scope.launch {
            try {
                firestore.collection("held_sales").document(id).delete()
            } catch (e: Exception) {}
        }
    }

    override fun observeHeldSales(branchId: String, onUpdate: (List<HeldSale>) -> Unit) {
        scope.launch {
            try {
                firestore.collection("held_sales").where { "branchId" equalTo branchId }.snapshots().collect { snapshot ->
                    onUpdate(snapshot.documents.map { it.data(HeldSale.serializer()) })
                }
            } catch (e: Exception) {}
        }
    }

    override fun syncReturn(productReturn: ProductReturn) {
        scope.launch {
            try {
                firestore.collection("returns").document(productReturn.id).set(ProductReturn.serializer(), productReturn)
            } catch (e: Exception) {}
        }
    }

    override fun observeReturns(branchId: String, onUpdate: (List<ProductReturn>) -> Unit) {
        // Deshabilitado tiempo real para ahorrar recursos.
    }

    override fun syncCashOut(cashOut: com.abtsplazita.posplazita.domain.CashOut) {
        scope.launch {
            try {
                println("CLOUD_ANDROID: Sincronizando corte ${cashOut.id}...")
                firestore.collection("cash_outs").document(cashOut.id).set(com.abtsplazita.posplazita.domain.CashOut.serializer(), cashOut)
            } catch (e: Exception) {
                println("CLOUD_ANDROID: Error al sincronizar corte: ${e.message}")
            }
        }
    }

    override fun observeCashOuts(branchId: String, onUpdate: (List<com.abtsplazita.posplazita.domain.CashOut>) -> Unit) {
        // Deshabilitado tiempo real para ahorrar recursos.
    }

    override fun syncCashMovement(movement: com.abtsplazita.posplazita.domain.CashMovement) {
        scope.launch {
            try {
                println("CLOUD_ANDROID: Sincronizando movimiento ${movement.id}...")
                firestore.collection("cash_movements").document(movement.id).set(com.abtsplazita.posplazita.domain.CashMovement.serializer(), movement)
            } catch (e: Exception) {
                println("CLOUD_ANDROID: Error al sincronizar movimiento: ${e.message}")
            }
        }
    }

    override fun observeCashMovements(branchId: String, onUpdate: (List<com.abtsplazita.posplazita.domain.CashMovement>) -> Unit) {
        // Deshabilitado tiempo real para ahorrar recursos.
    }

    override fun syncPreCut(preCut: com.abtsplazita.posplazita.domain.PreCut) {
        scope.launch {
            try {
                println("CLOUD_ANDROID: Sincronizando precorte ${preCut.id}...")
                firestore.collection("pre_cuts").document(preCut.id).set(com.abtsplazita.posplazita.domain.PreCut.serializer(), preCut)
            } catch (e: Exception) {
                println("CLOUD_ANDROID: Error al sincronizar precorte: ${e.message}")
            }
        }
    }

    override fun observePreCuts(branchId: String, onUpdate: (List<com.abtsplazita.posplazita.domain.PreCut>) -> Unit) {
        // Deshabilitado tiempo real para ahorrar recursos.
    }

    override fun syncGlobalAds(urls: List<String>) {
        scope.launch {
            try {
                firestore.collection("global_config").document("ads").set(buildMap {
                    put("urls", urls.joinToString("|"))
                })
            } catch (e: Exception) {}
        }
    }

    override fun syncInventoryBatch(branchId: String, items: List<Inventory>) {
        scope.launch {
            try {
                // Firestore permite hasta 500 operaciones por batch
                val chunks = items.chunked(450)
                chunks.forEach { chunk ->
                    val batch = firestore.batch()
                    chunk.forEach { item ->
                        // Document ID único: {branchId}_{productId}
                        val docRef = firestore.collection("inventory").document("${branchId}_${item.productId}")
                        batch.set(docRef, Inventory.serializer(), item)
                    }
                    batch.commit()
                }
                println("CLOUD_ANDROID: Sincronización batch de inventario completada (${items.size} ítems).")
            } catch (e: Exception) {
                println("CLOUD_ANDROID_BATCH_ERROR: ${e.message}")
            }
        }
    }

    override fun syncProductBatch(products: List<Product>) {
        scope.launch {
            try {
                val chunks = products.chunked(450)
                chunks.forEach { chunk ->
                    val batch = firestore.batch()
                    chunk.forEach { product ->
                        val docRef = firestore.collection("products").document(product.id)
                        batch.set(docRef, Product.serializer(), product)
                    }
                    batch.commit()
                }
                println("CLOUD_ANDROID: Sincronización batch de productos completada (${products.size} ítems).")
            } catch (e: Exception) {
                println("CLOUD_ANDROID_PROD_BATCH_ERROR: ${e.message}")
            }
        }
    }

    override fun observeGlobalAds(onUpdate: (List<String>) -> Unit) {
        scope.launch {
            try {
                firestore.collection("global_config").document("ads").snapshots().collect { snapshot ->
                    if (snapshot.exists) {
                        val urlsStr = snapshot.get<String>("urls") ?: ""
                        onUpdate(if (urlsStr.isBlank()) emptyList() else urlsStr.split("|"))
                    } else {
                        onUpdate(emptyList())
                    }
                }
            } catch (e: Exception) {}
        }
    }

    override fun syncWebOrder(order: WebOrder) {
        scope.launch {
            try {
                firestore.collection("web_orders").document(order.id).set(WebOrder.serializer(), order)
            } catch (e: Exception) {
                println("CLOUD_ANDROID_WEBORDER_SYNC_ERROR: ${e.message}")
            }
        }
    }

    override fun syncInventory(inventory: Inventory) {
        scope.launch {
            try {
                val updated = inventory.copy(lastUpdated = com.abtsplazita.posplazita.currentTimeMillis())
                val docId = "${inventory.branchId}_${inventory.productId}"
                firestore.collection("inventory").document(docId).set(Inventory.serializer(), updated)
            } catch (e: Exception) {
                println("CLOUD_ANDROID_INV_SYNC_ERROR: ${e.message}")
            }
        }
    }

    override fun observeInventoryIncremental(branchId: String, since: Long, onUpdate: (List<Inventory>) -> Unit) {
        scope.launch {
            try {
                firestore.collection("inventory")
                    .where { "branchId" equalTo branchId }
                    .where { "lastUpdated" greaterThan since }
                    .snapshots().collect { snapshot ->
                        onUpdate(snapshot.documents.mapNotNull { 
                            try { it.data(Inventory.serializer()) } catch (e: Exception) { null }
                        })
                    }
            } catch (e: Exception) {}
        }
    }

    override fun observeWebOrders(branchId: String, onUpdate: (List<WebOrder>) -> Unit) {
        scope.launch {
            try {
                firestore.collection("web_orders")
                    .where { "branchId" equalTo branchId }
                    .snapshots().collect { snapshot ->
                        onUpdate(snapshot.documents.map { it.data(WebOrder.serializer()) })
                    }
            } catch (e: Exception) {
                println("CLOUD_ANDROID_WEBORDER_OBSERVE_ERROR: ${e.message}")
            }
        }
    }

    override fun syncAiConfig(enabled: Boolean) {
        scope.launch {
            try {
                firestore.collection("global_config").document("whatsapp_ai").set(mapOf("enabled" to enabled))
            } catch (e: Exception) {
                println("CLOUD_ANDROID_AI_CONFIG_ERROR: ${e.message}")
            }
        }
    }

    override fun syncDeletionRequest(request: DeletionRequest) {
        scope.launch {
            try {
                firestore.collection("deletion_requests").document(request.id).set(DeletionRequest.serializer(), request)
            } catch (e: Exception) {}
        }
    }

    override fun deleteDeletionRequest(id: String) {
        scope.launch {
            try {
                firestore.collection("deletion_requests").document(id).delete()
            } catch (e: Exception) {}
        }
    }

    override fun observeDeletionRequests(branchId: String, onUpdate: (List<DeletionRequest>) -> Unit) {
        scope.launch {
            try {
                firestore.collection("deletion_requests")
                    .where { "branchId" equalTo branchId }
                    .snapshots().collect { snapshot ->
                        onUpdate(snapshot.documents.mapNotNull { 
                            try { it.data(DeletionRequest.serializer()) } catch (e: Exception) { null }
                        })
                    }
            } catch (e: Exception) {}
        }
    }

    override fun syncDeletionLog(log: DeletionLog) {
        scope.launch {
            try {
                firestore.collection("deletion_logs").document(log.id).set(DeletionLog.serializer(), log)
            } catch (e: Exception) {}
        }
    }

    override fun observeDeletionLogs(branchId: String, onUpdate: (List<DeletionLog>) -> Unit) {
        // Deshabilitado tiempo real para ahorrar recursos.
    }

    override suspend fun fetchProducts(): List<Product> {
        return try {
            firestore.collection("products").get().documents.mapNotNull { 
                try { it.data(Product.serializer()) } catch (e: Exception) { null }
            }
        } catch (e: Exception) { emptyList() }
    }

    override suspend fun fetchProductsIncremental(since: Long): List<Product> {
        return try {
            firestore.collection("products")
                .where { "lastUpdated" greaterThan since }
                .get().documents.mapNotNull { 
                    try { it.data(Product.serializer()) } catch (e: Exception) { null }
                }
        } catch (e: Exception) { emptyList() }
    }

    override suspend fun fetchSales(branchId: String): List<Sale> {
        return try {
            firestore.collection("sales").where { "branchId" equalTo branchId }.get().documents.mapNotNull { 
                try { it.data(Sale.serializer()) } catch (e: Exception) { null }
            }
        } catch (e: Exception) { emptyList() }
    }

    override suspend fun fetchCashOuts(branchId: String): List<com.abtsplazita.posplazita.domain.CashOut> {
        return try {
            firestore.collection("cash_outs").where { "branchId" equalTo branchId }.get().documents.mapNotNull { 
                try { it.data(com.abtsplazita.posplazita.domain.CashOut.serializer()) } catch (e: Exception) { null }
            }
        } catch (e: Exception) { emptyList() }
    }

    override suspend fun fetchCashMovements(branchId: String): List<com.abtsplazita.posplazita.domain.CashMovement> {
        return try {
            firestore.collection("cash_movements").where { "branchId" equalTo branchId }.get().documents.mapNotNull { 
                try { it.data(com.abtsplazita.posplazita.domain.CashMovement.serializer()) } catch (e: Exception) { null }
            }
        } catch (e: Exception) { emptyList() }
    }

    override suspend fun fetchPreCuts(branchId: String): List<com.abtsplazita.posplazita.domain.PreCut> {
        return try {
            firestore.collection("pre_cuts").where { "branchId" equalTo branchId }.get().documents.mapNotNull { 
                try { it.data(com.abtsplazita.posplazita.domain.PreCut.serializer()) } catch (e: Exception) { null }
            }
        } catch (e: Exception) { emptyList() }
    }

    override suspend fun fetchDeletionLogs(branchId: String): List<DeletionLog> {
        return try {
            firestore.collection("deletion_logs").where { "branchId" equalTo branchId }.get().documents.mapNotNull { 
                try { it.data(DeletionLog.serializer()) } catch (e: Exception) { null }
            }
        } catch (e: Exception) { emptyList() }
    }

    override suspend fun fetchReturns(branchId: String): List<ProductReturn> {
        return try {
            firestore.collection("returns").where { "branchId" equalTo branchId }.get().documents.mapNotNull { 
                try { it.data(ProductReturn.serializer()) } catch (e: Exception) { null }
            }
        } catch (e: Exception) { emptyList() }
    }

    override suspend fun fetchUsers(): List<User> {
        return try {
            firestore.collection("users").get().documents.mapNotNull { 
                try { it.data(User.serializer()) } catch (e: Exception) { null }
            }
        } catch (e: Exception) { emptyList() }
    }

    override suspend fun fetchBranches(): List<Branch> {
        return try {
            firestore.collection("branches").get().documents.mapNotNull { 
                try { it.data(Branch.serializer()) } catch (e: Exception) { null }
            }
        } catch (e: Exception) { emptyList() }
    }

    override suspend fun fetchCustomers(): List<Customer> {
        return try {
            firestore.collection("customers").get().documents.mapNotNull { 
                try { it.data(Customer.serializer()) } catch (e: Exception) { null }
            }
        } catch (e: Exception) { emptyList() }
    }

    override suspend fun fetchTerminals(branchId: String): List<PosTerminal> {
        return try {
            firestore.collection("terminals").where { "branchId" equalTo branchId }.get().documents.mapNotNull { 
                try { it.data(PosTerminal.serializer()) } catch (e: Exception) { null }
            }
        } catch (e: Exception) { emptyList() }
    }

    override suspend fun fetchInventory(branchId: String): List<Inventory> {
        return try {
            firestore.collection("inventory")
                .where { "branchId" equalTo branchId }
                .get().documents.mapNotNull { 
                    try { it.data(Inventory.serializer()) } catch (e: Exception) { null }
                }
        } catch (e: Exception) { emptyList() }
    }

    override suspend fun fetchInventoryIncremental(branchId: String, since: Long): List<Inventory> {
        return try {
            firestore.collection("inventory")
                .where { "branchId" equalTo branchId }
                .where { "lastUpdated" greaterThan since }
                .get().documents.mapNotNull { 
                    try { it.data(Inventory.serializer()) } catch (e: Exception) { null }
                }
        } catch (e: Exception) { emptyList() }
    }
}

actual fun getCloudProvider(): CloudProvider = AndroidCloudProvider()
