package com.abtsplazita.posplazita.data.remote

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
import com.abtsplazita.posplazita.domain.AttendanceRecord
import com.abtsplazita.posplazita.domain.Schedule
import com.abtsplazita.posplazita.domain.Employee

interface CloudProvider {
    fun syncProduct(product: Product)
    fun deleteProduct(id: String)
    fun observeProducts(onUpdate: (List<Product>) -> Unit)
    fun observeProductsIncremental(since: Long, onUpdate: (List<Product>) -> Unit)
    
    fun syncSale(sale: Sale)
    fun observeSales(branchId: String, onUpdate: (List<Sale>) -> Unit)
    
    fun syncCustomer(customer: Customer)
    fun deleteCustomer(id: String)
    fun observeCustomers(onUpdate: (List<Customer>) -> Unit)
    fun observeCustomersIncremental(since: Long, onUpdate: (List<Customer>) -> Unit)

    fun syncUser(user: User)
    fun observeUsers(onUpdate: (List<User>) -> Unit)
    fun observeUsersIncremental(since: Long, onUpdate: (List<User>) -> Unit)

    fun syncStockMovement(movement: StockMovement)
    fun observeStockMovements(onUpdate: (List<StockMovement>) -> Unit)

    fun syncBranch(branch: Branch)
    fun deleteBranch(id: String)
    fun observeBranches(onUpdate: (List<Branch>) -> Unit)
    fun observeBranchesIncremental(since: Long, onUpdate: (List<Branch>) -> Unit)

    fun syncTerminal(terminal: PosTerminal)
    fun deleteTerminal(id: String)
    fun observeTerminals(branchId: String, onUpdate: (List<PosTerminal>) -> Unit)

    fun syncHeldSale(heldSale: HeldSale)
    fun deleteHeldSale(id: String)
    fun observeHeldSales(branchId: String, onUpdate: (List<HeldSale>) -> Unit)

    fun syncReturn(productReturn: ProductReturn)
    fun observeReturns(branchId: String, onUpdate: (List<ProductReturn>) -> Unit)

    fun syncCashOut(cashOut: com.abtsplazita.posplazita.domain.CashOut)
    fun observeCashOuts(branchId: String, onUpdate: (List<com.abtsplazita.posplazita.domain.CashOut>) -> Unit)

    fun syncCashMovement(movement: com.abtsplazita.posplazita.domain.CashMovement)
    fun observeCashMovements(branchId: String, onUpdate: (List<com.abtsplazita.posplazita.domain.CashMovement>) -> Unit)

    fun syncPreCut(preCut: com.abtsplazita.posplazita.domain.PreCut)
    fun observePreCuts(branchId: String, onUpdate: (List<com.abtsplazita.posplazita.domain.PreCut>) -> Unit)

    fun syncWebOrder(order: WebOrder)
    fun observeWebOrders(branchId: String, onUpdate: (List<WebOrder>) -> Unit)

    fun syncDeletionRequest(request: DeletionRequest)
    fun deleteDeletionRequest(id: String)
    fun observeDeletionRequests(branchId: String, onUpdate: (List<DeletionRequest>) -> Unit)

    fun syncDeletionLog(log: DeletionLog)
    fun observeDeletionLogs(branchId: String, onUpdate: (List<DeletionLog>) -> Unit)

    fun syncAttendance(record: AttendanceRecord)
    fun syncSchedule(schedule: Schedule)
    fun syncEmployee(employee: Employee)
    fun deleteEmployee(id: Long)

    fun syncInventory(inventory: Inventory)
    fun syncInventoryBatch(branchId: String, items: List<Inventory>)
    fun observeInventoryIncremental(branchId: String, since: Long, onUpdate: (List<Inventory>) -> Unit)
    fun syncProductBatch(products: List<Product>)
    fun syncAiConfig(enabled: Boolean)

    fun syncGlobalAds(urls: List<String>)
    fun observeGlobalAds(onUpdate: (List<String>) -> Unit)

    suspend fun fetchProducts(): List<Product>
    suspend fun fetchProductsIncremental(since: Long): List<Product>
    suspend fun fetchSales(branchId: String): List<Sale>
    suspend fun fetchCashOuts(branchId: String): List<com.abtsplazita.posplazita.domain.CashOut>
    suspend fun fetchCashMovements(branchId: String): List<com.abtsplazita.posplazita.domain.CashMovement>
    suspend fun fetchPreCuts(branchId: String): List<com.abtsplazita.posplazita.domain.PreCut>
    suspend fun fetchDeletionLogs(branchId: String): List<DeletionLog>
    suspend fun fetchReturns(branchId: String): List<ProductReturn>

    suspend fun fetchUsers(): List<User>
    suspend fun fetchBranches(): List<Branch>
    suspend fun fetchCustomers(): List<Customer>
    suspend fun fetchTerminals(branchId: String): List<PosTerminal>
    suspend fun fetchInventory(branchId: String): List<Inventory>
    suspend fun fetchInventoryIncremental(branchId: String, since: Long): List<Inventory>
    suspend fun fetchAttendance(userId: String): List<AttendanceRecord>
    suspend fun fetchSchedules(employeeId: Long): List<Schedule>
    suspend fun fetchEmployees(): List<Employee>
}

expect fun getCloudProvider(): CloudProvider
