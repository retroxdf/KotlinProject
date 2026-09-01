package com.abtsplazita.posplazita.data.remote

import com.abtsplazita.posplazita.domain.*

class IosCloudProvider : CloudProvider {
    override fun syncProduct(product: Product) {}
    override fun deleteProduct(id: String) {}
    override fun observeProducts(onUpdate: (List<Product>) -> Unit) {}
    override fun observeProductsIncremental(since: Long, onUpdate: (List<Product>) -> Unit) {}
    
    override fun syncSale(sale: Sale) {}
    override fun observeSales(branchId: String, onUpdate: (List<Sale>) -> Unit) {}
    
    override fun syncCustomer(customer: Customer) {}
    override fun deleteCustomer(id: String) {}
    override fun observeCustomers(onUpdate: (List<Customer>) -> Unit) {}
    override fun observeCustomersIncremental(since: Long, onUpdate: (List<Customer>) -> Unit) {}

    override fun syncUser(user: User) {}
    override fun observeUsers(onUpdate: (List<User>) -> Unit) {}
    override fun observeUsersIncremental(since: Long, onUpdate: (List<User>) -> Unit) {}

    override fun syncStockMovement(movement: StockMovement) {}
    override fun observeStockMovements(onUpdate: (List<StockMovement>) -> Unit) {}

    override fun syncBranch(branch: Branch) {}
    override fun deleteBranch(id: String) {}
    override fun observeBranches(onUpdate: (List<Branch>) -> Unit) {}
    override fun observeBranchesIncremental(since: Long, onUpdate: (List<Branch>) -> Unit) {}

    override fun syncTerminal(terminal: PosTerminal) {}
    override fun deleteTerminal(id: String) {}
    override fun observeTerminals(branchId: String, onUpdate: (List<PosTerminal>) -> Unit) {}

    override fun syncHeldSale(heldSale: HeldSale) {}
    override fun deleteHeldSale(id: String) {}
    override fun observeHeldSales(branchId: String, onUpdate: (List<HeldSale>) -> Unit) {}

    override fun syncReturn(productReturn: ProductReturn) {}
    override fun observeReturns(branchId: String, onUpdate: (List<ProductReturn>) -> Unit) {}

    override fun syncCashOut(cashOut: CashOut) {}
    override fun observeCashOuts(branchId: String, onUpdate: (List<CashOut>) -> Unit) {}

    override fun syncCashMovement(movement: CashMovement) {}
    override fun observeCashMovements(branchId: String, onUpdate: (List<CashMovement>) -> Unit) {}

    override fun syncPreCut(preCut: PreCut) {}
    override fun observePreCuts(branchId: String, onUpdate: (List<PreCut>) -> Unit) {}

    override fun syncWebOrder(order: WebOrder) {}
    override fun observeWebOrders(branchId: String, onUpdate: (List<WebOrder>) -> Unit) {}

    override fun syncInventory(inventory: Inventory) {}
    override fun syncInventoryBatch(branchId: String, items: List<Inventory>) {}
    override fun observeInventoryIncremental(branchId: String, since: Long, onUpdate: (List<Inventory>) -> Unit) {}
    override fun syncProductBatch(products: List<Product>) {}
    override fun syncAiConfig(enabled: Boolean) {}

    override fun syncGlobalAds(urls: List<String>) {}
    override fun observeGlobalAds(onUpdate: (List<String>) -> Unit) {}

    override suspend fun fetchProducts(): List<Product> = emptyList()
    override suspend fun fetchProductsIncremental(since: Long): List<Product> = emptyList()
    override suspend fun fetchSales(branchId: String): List<Sale> = emptyList()
    override suspend fun fetchCashOuts(branchId: String): List<CashOut> = emptyList()
    override suspend fun fetchCashMovements(branchId: String): List<CashMovement> = emptyList()
    override suspend fun fetchPreCuts(branchId: String): List<PreCut> = emptyList()

    override suspend fun fetchUsers(): List<User> = emptyList()
    override suspend fun fetchBranches(): List<Branch> = emptyList()
    override suspend fun fetchCustomers(): List<Customer> = emptyList()
    override suspend fun fetchTerminals(branchId: String): List<PosTerminal> = emptyList()
    override suspend fun fetchInventory(branchId: String): List<Inventory> = emptyList()
    override suspend fun fetchInventoryIncremental(branchId: String, since: Long): List<Inventory> = emptyList()
}

actual fun getCloudProvider(): CloudProvider = IosCloudProvider()
