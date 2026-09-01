package com.abtsplazita.posplazita.data.remote

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

class FirebaseManager {
    
    // Obtenemos el proveedor específico para cada plataforma (Android vs PC)
    private val cloudProvider = getCloudProvider()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    fun syncProduct(product: Product) {
        cloudProvider.syncProduct(product)
    }

    fun deleteProduct(id: String) {
        cloudProvider.deleteProduct(id)
    }

    fun observeProducts(onUpdate: (List<Product>) -> Unit) {
        cloudProvider.observeProducts(onUpdate)
    }

    fun observeProductsIncremental(since: Long, onUpdate: (List<Product>) -> Unit) {
        cloudProvider.observeProductsIncremental(since, onUpdate)
    }
    
    fun syncSale(sale: Sale) {
        cloudProvider.syncSale(sale)
    }
    
    fun observeSales(branchId: String, onUpdate: (List<Sale>) -> Unit) {
        cloudProvider.observeSales(branchId, onUpdate)
    }
    
    fun syncCustomer(customer: Customer) {
        cloudProvider.syncCustomer(customer)
    }

    fun deleteCustomer(id: String) {
        cloudProvider.deleteCustomer(id)
    }

    fun observeCustomers(onUpdate: (List<Customer>) -> Unit) {
        cloudProvider.observeCustomers(onUpdate)
    }

    fun observeCustomersIncremental(since: Long, onUpdate: (List<Customer>) -> Unit) {
        cloudProvider.observeCustomersIncremental(since, onUpdate)
    }

    fun syncUser(user: User) {
        cloudProvider.syncUser(user)
    }

    fun observeUsers(onUpdate: (List<User>) -> Unit) {
        cloudProvider.observeUsers(onUpdate)
    }

    fun observeUsersIncremental(since: Long, onUpdate: (List<User>) -> Unit) {
        cloudProvider.observeUsersIncremental(since, onUpdate)
    }

    fun syncStockMovement(movement: StockMovement) {
        cloudProvider.syncStockMovement(movement)
    }

    fun observeStockMovements(onUpdate: (List<StockMovement>) -> Unit) {
        cloudProvider.observeStockMovements(onUpdate)
    }

    fun syncBranch(branch: Branch) {
        cloudProvider.syncBranch(branch)
    }

    fun deleteBranch(id: String) {
        cloudProvider.deleteBranch(id)
    }

    fun observeBranches(onUpdate: (List<Branch>) -> Unit) {
        cloudProvider.observeBranches(onUpdate)
    }

    fun observeBranchesIncremental(since: Long, onUpdate: (List<Branch>) -> Unit) {
        cloudProvider.observeBranchesIncremental(since, onUpdate)
    }

    fun syncTerminal(terminal: PosTerminal) {
        cloudProvider.syncTerminal(terminal)
    }

    fun deleteTerminal(id: String) {
        cloudProvider.deleteTerminal(id)
    }

    fun observeTerminals(branchId: String, onUpdate: (List<PosTerminal>) -> Unit) {
        cloudProvider.observeTerminals(branchId, onUpdate)
    }

    fun syncHeldSale(heldSale: HeldSale) {
        cloudProvider.syncHeldSale(heldSale)
    }

    fun deleteHeldSale(id: String) {
        cloudProvider.deleteHeldSale(id)
    }

    fun observeHeldSales(branchId: String, onUpdate: (List<HeldSale>) -> Unit) {
        cloudProvider.observeHeldSales(branchId, onUpdate)
    }

    fun syncReturn(productReturn: ProductReturn) {
        cloudProvider.syncReturn(productReturn)
    }

    fun observeReturns(branchId: String, onUpdate: (List<ProductReturn>) -> Unit) {
        cloudProvider.observeReturns(branchId, onUpdate)
    }

    fun syncCashOut(cashOut: com.abtsplazita.posplazita.domain.CashOut) {
        cloudProvider.syncCashOut(cashOut)
    }

    fun observeCashOuts(branchId: String, onUpdate: (List<com.abtsplazita.posplazita.domain.CashOut>) -> Unit) {
        cloudProvider.observeCashOuts(branchId, onUpdate)
    }

    fun syncCashMovement(movement: com.abtsplazita.posplazita.domain.CashMovement) {
        cloudProvider.syncCashMovement(movement)
    }

    fun observeCashMovements(branchId: String, onUpdate: (List<com.abtsplazita.posplazita.domain.CashMovement>) -> Unit) {
        cloudProvider.observeCashMovements(branchId, onUpdate)
    }

    fun syncPreCut(preCut: com.abtsplazita.posplazita.domain.PreCut) {
        cloudProvider.syncPreCut(preCut)
    }

    fun observePreCuts(branchId: String, onUpdate: (List<com.abtsplazita.posplazita.domain.PreCut>) -> Unit) {
        cloudProvider.observePreCuts(branchId, onUpdate)
    }

    fun syncInventory(inventory: Inventory) {
        cloudProvider.syncInventory(inventory)
    }

    fun syncInventoryBatch(branchId: String, items: List<Inventory>) {
        cloudProvider.syncInventoryBatch(branchId, items)
    }

    fun observeInventoryIncremental(branchId: String, since: Long, onUpdate: (List<Inventory>) -> Unit) {
        cloudProvider.observeInventoryIncremental(branchId, since, onUpdate)
    }

    fun syncProductBatch(products: List<Product>) {
        cloudProvider.syncProductBatch(products)
    }

    fun syncAiConfig(enabled: Boolean) {
        cloudProvider.syncAiConfig(enabled)
    }

    fun syncWebOrder(order: WebOrder) {
        cloudProvider.syncWebOrder(order)
    }

    fun observeWebOrders(branchId: String, onUpdate: (List<WebOrder>) -> Unit) {
        cloudProvider.observeWebOrders(branchId, onUpdate)
    }

    fun syncDeletionRequest(request: DeletionRequest) {
        cloudProvider.syncDeletionRequest(request)
    }

    fun deleteDeletionRequest(id: String) {
        cloudProvider.deleteDeletionRequest(id)
    }

    fun observeDeletionRequests(branchId: String, onUpdate: (List<DeletionRequest>) -> Unit) {
        cloudProvider.observeDeletionRequests(branchId, onUpdate)
    }

    fun syncDeletionLog(log: DeletionLog) {
        cloudProvider.syncDeletionLog(log)
    }

    fun observeDeletionLogs(branchId: String, onUpdate: (List<DeletionLog>) -> Unit) {
        cloudProvider.observeDeletionLogs(branchId, onUpdate)
    }

    private val _globalAds = MutableStateFlow<List<String>>(emptyList())
    val globalAds = _globalAds.asStateFlow()

    init {
        observeGlobalAds { _globalAds.value = it }
    }

    fun syncGlobalAds(urls: List<String>) {
        cloudProvider.syncGlobalAds(urls)
    }

    fun observeGlobalAds(onUpdate: (List<String>) -> Unit) {
        cloudProvider.observeGlobalAds(onUpdate)
    }

    suspend fun fetchProducts() = cloudProvider.fetchProducts()
    suspend fun fetchProductsIncremental(since: Long) = cloudProvider.fetchProductsIncremental(since)
    suspend fun fetchSales(branchId: String) = cloudProvider.fetchSales(branchId)
    suspend fun fetchCashOuts(branchId: String) = cloudProvider.fetchCashOuts(branchId)
    suspend fun fetchCashMovements(branchId: String) = cloudProvider.fetchCashMovements(branchId)
    suspend fun fetchPreCuts(branchId: String) = cloudProvider.fetchPreCuts(branchId)
    suspend fun fetchDeletionLogs(branchId: String) = cloudProvider.fetchDeletionLogs(branchId)
    suspend fun fetchReturns(branchId: String) = cloudProvider.fetchReturns(branchId)

    suspend fun fetchUsers() = cloudProvider.fetchUsers()
    suspend fun fetchBranches() = cloudProvider.fetchBranches()
    suspend fun fetchCustomers() = cloudProvider.fetchCustomers()
    suspend fun fetchTerminals(branchId: String) = cloudProvider.fetchTerminals(branchId)
    suspend fun fetchInventory(branchId: String) = cloudProvider.fetchInventory(branchId)
    suspend fun fetchInventoryIncremental(branchId: String, since: Long) = cloudProvider.fetchInventoryIncremental(branchId, since)
}
