package com.abtsplazita.posplazita.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.abtsplazita.posplazita.domain.Sale
import com.abtsplazita.posplazita.domain.SaleItem
import com.abtsplazita.posplazita.domain.Product
import com.abtsplazita.posplazita.domain.PosTerminal
import com.abtsplazita.posplazita.domain.CashOut
import com.abtsplazita.posplazita.domain.Purchase
import com.abtsplazita.posplazita.domain.PurchaseItem
import com.abtsplazita.posplazita.domain.PurchaseStatus
import com.abtsplazita.posplazita.domain.CashMovement
import com.abtsplazita.posplazita.domain.CashMovementType
import com.abtsplazita.posplazita.domain.SupplierPayment
import com.abtsplazita.posplazita.domain.PreCut
import com.abtsplazita.posplazita.domain.TicketConfig
import com.abtsplazita.posplazita.domain.Inventory
import com.abtsplazita.posplazita.domain.repository.PreCutRepository
import com.abtsplazita.posplazita.domain.repository.SupplierRepository
import com.abtsplazita.posplazita.domain.Branch
import com.abtsplazita.posplazita.domain.repository.BranchRepository
import com.abtsplazita.posplazita.domain.repository.SaleRepository
import com.abtsplazita.posplazita.domain.repository.CustomerRepository
import com.abtsplazita.posplazita.domain.repository.PosTerminalRepository
import com.abtsplazita.posplazita.domain.repository.CashOutRepository
import com.abtsplazita.posplazita.domain.repository.PurchaseRepository
import com.abtsplazita.posplazita.domain.repository.ProductRepository
import com.abtsplazita.posplazita.domain.repository.CashMovementRepository
import kotlinx.datetime.*

data class TerminalBalance(
    val terminalId: String,
    val terminalName: String,
    val amount: Double
)

data class PriceAdjustment(
    val productId: String,
    val newCost: Double,
    val newPrice1: Double,
    val newPrice2: Double,
    val newPrice3: Double,
    val newPrice4: Double
)

data class InventoryReportItem(
    val product: Product,
    val currentStock: Double,
    val totalCost: Double
)

enum class HistoryPeriod { TODAY, YESTERDAY, LAST_7_DAYS, MONTH_ACTUAL, MONTH_PREVIOUS, CUSTOM }

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    private val saleRepository: SaleRepository,
    private val purchaseRepository: PurchaseRepository? = null,
    private val productRepository: ProductRepository? = null,
    private val customerRepository: CustomerRepository? = null,
    private val terminalRepository: PosTerminalRepository? = null,
    private val cashOutRepository: CashOutRepository? = null,
    private val cashMovementRepository: CashMovementRepository? = null,
    private val preCutRepository: PreCutRepository? = null,
    private val deletionLogRepository: com.abtsplazita.posplazita.domain.repository.DeletionLogRepository? = null,
    private val productReturnRepository: com.abtsplazita.posplazita.domain.repository.ProductReturnRepository? = null,
    private val supplierRepository: SupplierRepository? = null,
    private val branchRepository: BranchRepository? = null,
    private val employeeRepository: com.abtsplazita.posplazita.domain.repository.EmployeeRepository? = null,
    private val settingsRepository: com.abtsplazita.posplazita.domain.repository.SettingsRepository? = null,
    private val printerManager: PrinterManager,
    private var _branchId: String
) : ViewModel() {

    private val _period = MutableStateFlow(HistoryPeriod.TODAY)
    val period = _period.asStateFlow()

    private val _currentUser = MutableStateFlow<com.abtsplazita.posplazita.domain.User?>(null)
    val currentUser = _currentUser.asStateFlow()

    fun setCurrentUser(user: com.abtsplazita.posplazita.domain.User?) {
        _currentUser.value = user
    }

    val branchId: String get() = _branchId

    private val _selectedBranchId = MutableStateFlow(_branchId)
    val selectedBranchId = _selectedBranchId.asStateFlow()

    private val _selectedTerminalId = MutableStateFlow<String?>(null)
    val selectedTerminalId = _selectedTerminalId.asStateFlow()

    private val _filterUser = MutableStateFlow<String?>(null)
    val filterUser = _filterUser.asStateFlow()

    private val _filterStartDate = MutableStateFlow<Long?>(null)
    val filterStartDate = _filterStartDate.asStateFlow()

    private val _filterEndDate = MutableStateFlow<Long?>(null)
    val filterEndDate = _filterEndDate.asStateFlow()

    fun setPeriod(p: HistoryPeriod) {
        _period.value = p
        if (p != HistoryPeriod.CUSTOM) {
            updateDateRangeFromPeriod(p)
        }
    }

    private fun updateDateRangeFromPeriod(p: HistoryPeriod) {
        val now = com.abtsplazita.posplazita.currentTimeMillis()
        val tz = TimeZone.currentSystemDefault()
        val dt = Instant.fromEpochMilliseconds(now).toLocalDateTime(tz)
        
        when(p) {
            HistoryPeriod.TODAY -> {
                val start = LocalDateTime(dt.year, dt.month, dt.dayOfMonth, 0, 0, 0)
                _filterStartDate.value = start.toInstant(tz).toEpochMilliseconds()
                _filterEndDate.value = null
            }
            HistoryPeriod.YESTERDAY -> {
                val startDay = Instant.fromEpochMilliseconds(now).minus(1, DateTimeUnit.DAY, tz).toLocalDateTime(tz)
                val start = LocalDateTime(startDay.year, startDay.month, startDay.dayOfMonth, 0, 0, 0)
                val end = LocalDateTime(startDay.year, startDay.month, startDay.dayOfMonth, 23, 59, 59)
                _filterStartDate.value = start.toInstant(tz).toEpochMilliseconds()
                _filterEndDate.value = end.toInstant(tz).toEpochMilliseconds()
            }
            HistoryPeriod.LAST_7_DAYS -> {
                _filterStartDate.value = now - (7 * 24 * 60 * 60 * 1000L)
                _filterEndDate.value = null
            }
            HistoryPeriod.MONTH_ACTUAL -> {
                val start = LocalDateTime(dt.year, dt.month, 1, 0, 0, 0)
                _filterStartDate.value = start.toInstant(tz).toEpochMilliseconds()
                _filterEndDate.value = null
            }
            HistoryPeriod.MONTH_PREVIOUS -> {
                val firstOfCurrent = LocalDateTime(dt.year, dt.month, 1, 0, 0, 0).toInstant(tz)
                val lastOfPrev = firstOfCurrent.minus(1, DateTimeUnit.SECOND, tz)
                val firstOfPrev = lastOfPrev.toLocalDateTime(tz).let { 
                    LocalDateTime(it.year, it.month, 1, 0, 0, 0)
                }
                _filterStartDate.value = firstOfPrev.toInstant(tz).toEpochMilliseconds()
                _filterEndDate.value = lastOfPrev.toEpochMilliseconds()
            }
            HistoryPeriod.CUSTOM -> {} 
        }
    }

    init {
        updateDateRangeFromPeriod(HistoryPeriod.TODAY)
    }

    val availableBranches = if (branchRepository != null) {
        branchRepository.getAllBranches()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    } else {
        MutableStateFlow(emptyList())
    }

    val availableTerminals = _selectedBranchId.flatMapLatest { bId ->
        if (terminalRepository != null) {
            terminalRepository.getTerminalsByBranch(bId)
        } else {
            flowOf(emptyList())
        }
    }.onEach { terminals ->
        // Auto-seleccionar si solo hay una caja
        if (_selectedTerminalId.value == null && terminals.size == 1) {
            _selectedTerminalId.value = terminals.first().id
        }

        if (terminals.isNotEmpty() && _selectedTerminalId.value != null && terminals.none { it.id == _selectedTerminalId.value }) {
            _selectedTerminalId.value = null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sales = combine(_selectedBranchId, _selectedTerminalId) { bId, terminalId ->
        bId to terminalId
    }.flatMapLatest { (bId, terminalId) ->
        if (terminalId == null) {
            saleRepository.getSales(bId)
        } else {
            saleRepository.getSalesByTerminal(bId, terminalId)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cashOuts = _selectedBranchId.flatMapLatest { bId ->
        if (cashOutRepository != null) {
            cashOutRepository.getCashOuts(bId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val purchases = _selectedBranchId.flatMapLatest { bId ->
        if (purchaseRepository != null) {
            purchaseRepository.getPurchases(bId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hasPendingPurchases = purchases.map { list ->
        list.any { it.status == PurchaseStatus.PENDING_PRICE_UPDATE }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val movements = _selectedBranchId.flatMapLatest { bId ->
        if (cashMovementRepository != null) {
            cashMovementRepository.getMovements(bId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val preCuts = _selectedBranchId.flatMapLatest { bId ->
        if (preCutRepository != null) {
            preCutRepository.getPreCuts(bId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deletionLogs = _selectedBranchId.flatMapLatest { bId ->
        if (deletionLogRepository != null) {
            deletionLogRepository.getLogs(bId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val productReturns = _selectedBranchId.flatMapLatest { bId ->
        if (productReturnRepository != null) {
            productReturnRepository.getReturns(bId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val movementsFiltered = combine(movements, _selectedTerminalId, _filterUser, _filterStartDate, _filterEndDate) { allMovements, terminalId, userId, start, end ->
        allMovements.filter { mov ->
            val matchTerminal = terminalId == null || mov.terminalId == terminalId
            val matchSearch = userId == null || mov.userId.contains(userId, ignoreCase = true) || mov.reason.contains(userId, ignoreCase = true)
            val matchDate = (start == null || mov.timestamp >= start) && (end == null || mov.timestamp <= end)
            matchTerminal && matchSearch && matchDate
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val withdrawalsFiltered = movements.map { allMovements ->
        allMovements.filter { it.reason.contains("Retiro", ignoreCase = true) }
    }.combine(_selectedTerminalId) { list, terminalId ->
        list.filter { terminalId == null || it.terminalId == terminalId }
    }.combine(combine(_filterUser, _filterStartDate, _filterEndDate) { u, s, e -> Triple(u, s, e) }) { list, (userId, start, end) ->
        list.filter { mov ->
            val matchSearch = userId == null || mov.userId.contains(userId, ignoreCase = true) || mov.reason.contains(userId, ignoreCase = true)
            val matchDate = (start == null || mov.timestamp >= start) && (end == null || mov.timestamp <= end)
            matchSearch && matchDate
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val supplierPaymentsFiltered = combine(_selectedBranchId, _filterUser, _filterStartDate, _filterEndDate) { bId, userId, start, end ->
        if (supplierRepository != null) {
            supplierRepository.getAllSuppliers().flatMapLatest { all ->
                val flows = all.map { s -> supplierRepository.getPayments(s.id) }
                if (flows.isEmpty()) flowOf(emptyList<SupplierPayment>())
                else combine(flows) { it.flatMap { l -> l } }
            }.map { list ->
                list.filter { pay ->
                    val matchSearch = userId == null || pay.userId.contains(userId, ignoreCase = true) || pay.notes?.contains(userId, ignoreCase = true) == true
                    val matchDate = (start == null || pay.timestamp >= start) && (end == null || pay.timestamp <= end)
                    matchSearch && matchDate
                }
            }
        } else flowOf(emptyList())
    }.flatMapLatest { it }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val preCutsFiltered = combine(preCuts, _selectedTerminalId, _filterUser, _filterStartDate, _filterEndDate) { allPreCuts, terminalId, userId, start, end ->
        allPreCuts.filter { pc ->
            val matchTerminal = terminalId == null || pc.terminalId == terminalId
            val matchUser = userId == null || pc.userId == userId
            val matchDate = (start == null || pc.timestamp >= start) && (end == null || pc.timestamp <= end)
            matchTerminal && matchUser && matchDate
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingSales = combine(sales, cashOuts, _selectedTerminalId) { allSales, allCashOuts, terminalId ->
        if (terminalId == null) return@combine emptyList<Sale>() // Forzar selección de caja para arqueo

        val lastCashOut = allCashOuts
            .filter { it.terminalId == terminalId }
            .maxByOrNull { it.timestamp }
        
        val startTime = lastCashOut?.timestamp ?: 0L
        // sales ya viene filtrado por terminal y branch en el flatMapLatest superior
        allSales.filter { it.timestamp > startTime }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingMovements = combine(movements, cashOuts, _selectedTerminalId) { allMovements, allCashOuts, terminalId ->
        if (terminalId == null) return@combine emptyList<CashMovement>()

        val lastCashOut = allCashOuts
            .filter { it.terminalId == terminalId }
            .maxByOrNull { it.timestamp }
        
        val startTime = lastCashOut?.timestamp ?: 0L
        allMovements.filter { mov ->
            mov.terminalId == terminalId && mov.timestamp > startTime
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Nuevo: Flujo de ventas de toda la sucursal (sin filtro de terminal de la UI) 
    // para que la pantalla de saldos sea siempre correcta
    private val allSalesOfBranch = _selectedBranchId.flatMapLatest { bId ->
        saleRepository.getSales(bId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val terminalBalances = combine(allSalesOfBranch, cashOuts, movements, availableTerminals) { allSales, allCashOuts, allMovements, terminals ->
        terminals.map { terminal ->
            val lastCashOut = allCashOuts
                .filter { it.terminalId == terminal.id }
                .maxByOrNull { it.timestamp }
            val startTime = lastCashOut?.timestamp ?: 0L
            
            val pendingSalesForTerminal = allSales.filter { it.terminalId == terminal.id && it.timestamp > startTime }
            val pendingMovementsForTerminal = allMovements.filter { it.terminalId == terminal.id && it.timestamp > startTime }
            
            val salesCash = pendingSalesForTerminal.sumOf { it.cashAmount }
            val entriesCash = pendingMovementsForTerminal.filter { it.type == com.abtsplazita.posplazita.domain.CashMovementType.IN }.sumOf { it.amount }
            val exitsCash = pendingMovementsForTerminal.filter { it.type == com.abtsplazita.posplazita.domain.CashMovementType.OUT }.sumOf { it.amount }

            TerminalBalance(
                terminalId = terminal.id,
                terminalName = terminal.name,
                amount = salesCash + entriesCash - exitsCash
            )
        }
    }.flowOn(kotlinx.coroutines.Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inventoryReport = if (productRepository != null) {
        combine(
            productRepository.getProducts(),
            productRepository.getAllInventory(),
            _selectedBranchId
        ) { products, allInventory, bId ->
            val branchInventoryMap = allInventory.filter { it.branchId == bId }.associateBy { it.productId }
            products.filter { !it.isService }.map { product ->
                val stock = branchInventoryMap[product.id]?.stock ?: 0.0
                InventoryReportItem(
                    product = product,
                    currentStock = stock,
                    totalCost = stock * product.cost
                )
            }.filter { it.currentStock != 0.0 || it.product.cost != 0.0 }
             .sortedBy { it.product.name }
        }.flowOn(kotlinx.coroutines.Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    } else {
        MutableStateFlow(emptyList())
    }

    val totalInventoryCost = inventoryReport.map { items ->
        items.sumOf { it.totalCost }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val employeePayments = if (employeeRepository != null) {
        employeeRepository.allPaymentRecords.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    } else {
        MutableStateFlow(emptyList())
    }

    private val _cashOutSuccess = MutableSharedFlow<Unit>()
    val cashOutSuccess = _cashOutSuccess.asSharedFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _warningMessage = MutableStateFlow<String?>(null)
    val warningMessage = _warningMessage.asStateFlow()

    private val _selectedSale = MutableStateFlow<Sale?>(null)
    val selectedSale = _selectedSale.asStateFlow()

    private val _selectedSaleItems = MutableStateFlow<List<SaleItem>>(emptyList())
    val selectedSaleItems = _selectedSaleItems.asStateFlow()

    private val _selectedPurchase = MutableStateFlow<Purchase?>(null)
    val selectedPurchase = _selectedPurchase.asStateFlow()

    private val _selectedPurchaseItems = MutableStateFlow<List<PurchaseItem>>(emptyList())
    val selectedPurchaseItems = _selectedPurchaseItems.asStateFlow()

    private val _currentPurchaseProducts = MutableStateFlow<Map<String, Product>>(emptyMap())
    val currentPurchaseProducts = _currentPurchaseProducts.asStateFlow()

    private val _ticketConfig = MutableStateFlow(TicketConfig())

    init {
        viewModelScope.launch {
            settingsRepository?.getAllSettings()?.collect { settings ->
                val layoutJson = settings["ticket_layout_json"]
                val customLayout = layoutJson?.let {
                    try {
                        kotlinx.serialization.json.Json.decodeFromString<List<com.abtsplazita.posplazita.domain.TicketElement>>(it)
                    } catch (e: Exception) { null }
                }

                _ticketConfig.value = TicketConfig(
                    logoPath = settings["ticket_logo_path"],
                    facebook = settings["ticket_facebook"],
                    instagram = settings["ticket_instagram"],
                    whatsapp = settings["ticket_whatsapp"],
                    thanksMessage = settings["ticket_thanks_message"] ?: "Gracias por su compra!",
                showBranchInfo = settings["ticket_show_branch"]?.toBoolean() ?: true,
                layout = customLayout ?: TicketConfig.defaultLayout
                )
                
                _branchName.value = settings["${_branchId}_name"] ?: ""
            }
        }
        
        // No refrescar automáticamente al iniciar para evitar bloqueos
        // Se hará cuando el usuario entre a una pantalla específica o pulse Actualizar
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    fun refreshDashboardData() {
        val bId = _selectedBranchId.value
        if (bId.isBlank()) return

        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                saleRepository.refreshSales(bId)
                cashOutRepository?.refreshCashOuts(bId)
                cashMovementRepository?.refreshMovements(bId)
                preCutRepository?.refreshPreCuts(bId)
                deletionLogRepository?.refreshLogs(bId)
                productReturnRepository?.refreshReturns(bId)
                productRepository?.refreshProducts()
                productRepository?.refreshInventory(bId)
                customerRepository?.refreshCustomers()
                terminalRepository?.refreshTerminals(bId)
                println("HISTORY_VM: Sincronización bajo demanda completada para $bId")
            } catch (e: Exception) {
                println("HISTORY_VM: Error en refresco: ${e.message}")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private val _branchName = MutableStateFlow("")
    val branchName = _branchName.asStateFlow()

    fun selectSale(sale: Sale) {
        _selectedSale.value = sale
        viewModelScope.launch {
            _selectedSaleItems.value = saleRepository.getSaleItems(sale.id)
        }
    }

    fun selectPurchase(purchase: Purchase) {
        _selectedPurchase.value = purchase
        viewModelScope.launch {
            val items = purchaseRepository?.getPurchaseItems(purchase.id) ?: emptyList()
            _selectedPurchaseItems.value = items
            
            val productsMap = mutableMapOf<String, Product>()
            items.forEach { item ->
                productRepository?.getProductById(item.productId)?.let {
                    productsMap[item.productId] = it
                }
            }
            _currentPurchaseProducts.value = productsMap
        }
    }

    fun confirmPurchasePrices(purchase: Purchase) {
        viewModelScope.launch {
            purchaseRepository?.updatePurchaseStatus(purchase, PurchaseStatus.COMPLETED)
            _selectedPurchase.value = null
        }
    }

    fun adjustProductPrices(purchase: Purchase, adjustments: List<PriceAdjustment>) {
        viewModelScope.launch {
            try {
                adjustments.forEach { adj ->
                    val product = productRepository?.getProductById(adj.productId)
                    if (product != null) {
                        val updatedProduct = product.copy(
                            cost = adj.newCost,
                            price1 = adj.newPrice1,
                            price2 = adj.newPrice2,
                            price3 = adj.newPrice3,
                            price4 = adj.newPrice4,
                            lastUpdated = com.abtsplazita.posplazita.currentTimeMillis()
                        )
                        productRepository.saveProduct(updatedProduct, syncWithCloud = true)
                    }
                }
                purchaseRepository?.updatePurchaseStatus(purchase, PurchaseStatus.COMPLETED)
                _selectedPurchase.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Error al ajustar precios: ${e.message}"
            }
        }
    }

    fun clearSelection() {
        _selectedSale.value = null
        _selectedSaleItems.value = emptyList()
        _selectedPurchase.value = null
        _selectedPurchaseItems.value = emptyList()
        _currentPurchaseProducts.value = emptyMap()
    }

    fun reprintSelectedSale() {
        val sale = _selectedSale.value ?: return
        val items = _selectedSaleItems.value
        viewModelScope.launch {
            val customer = if (sale.customerId != null) customerRepository?.getCustomerById(sale.customerId) else null
            printerManager.printTicket(
                sale, 
                items, 
                openDrawer = false, 
                walletBalance = customer?.walletBalance,
                config = _ticketConfig.value,
                branchName = _branchName.value
            )
        }
    }

    fun filterByBranch(branchId: String) {
        _selectedBranchId.value = branchId
        _selectedTerminalId.value = null
    }

    fun filterByTerminal(terminalId: String?) {
        _selectedTerminalId.value = terminalId
    }

    fun filterByUser(userId: String?) {
        _filterUser.value = userId
    }

    fun setDateRange(start: Long?, end: Long?) {
        _filterStartDate.value = start
        _filterEndDate.value = end
    }

    fun saveCashOut(countedAmount: Double, expectedAmount: Double, ticketCount: Int, currentUserId: String = "admin", onDone: () -> Unit = {}) {
        if (cashOutRepository == null) return
        if (ticketCount == 0) {
            _warningMessage.value = "No hay movimientos de venta para realizar el corte."
            return
        }
        viewModelScope.launch {
            val now = com.abtsplazita.posplazita.currentTimeMillis()
            val coId = "CO-${branchId}-${_selectedTerminalId.value ?: "0"}-$now"
            val cashOut = CashOut(
                id = coId,
                timestamp = now,
                branchId = branchId,
                terminalId = _selectedTerminalId.value,
                expectedAmount = expectedAmount,
                countedAmount = countedAmount,
                difference = countedAmount - expectedAmount,
                ticketCount = ticketCount,
                userId = currentUserId
            )
            cashOutRepository.saveCashOut(cashOut)

            // Lógica de cierre de turno al hacer Corte
            employeeRepository?.let { empRepo ->
                val openShift = empRepo.getOpenShift(currentUserId)
                if (openShift != null) {
                    val baseSalary8h = settingsRepository?.getSetting("base_salary_8h")?.toDoubleOrNull() ?: 315.0
                    val hourlyRate = baseSalary8h / 8.0
                    
                    val durationMillis = now - openShift.startTime
                    val hoursWorked = durationMillis.toDouble() / (1000.0 * 60.0 * 60.0)
                    val pay = hoursWorked * hourlyRate
                    
                    val closedShift = openShift.copy(
                        endTime = now,
                        hoursWorked = hoursWorked,
                        payAmount = pay,
                        isClosed = true
                    )
                    empRepo.updateAttendance(closedShift)
                }
            }

            _cashOutSuccess.emit(Unit)
            onDone()
        }
    }

    fun clearWarning() {
        _warningMessage.value = null
    }

    fun getTodaySalesTotal(): Double {
        return sales.value.sumOf { it.netTotal }
    }
}
