package com.abtsplazita.posplazita.ui

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.abtsplazita.posplazita.currentTimeMillis
import com.abtsplazita.posplazita.playErrorSound
import com.abtsplazita.posplazita.domain.*
import com.abtsplazita.posplazita.domain.getScaleManager
import com.abtsplazita.posplazita.domain.repository.ProductRepository
import com.abtsplazita.posplazita.domain.repository.SaleRepository
import com.abtsplazita.posplazita.domain.repository.CustomerRepository
import com.abtsplazita.posplazita.domain.repository.PosTerminalRepository
import com.abtsplazita.posplazita.domain.repository.SettingsRepository
import com.abtsplazita.posplazita.data.remote.FirebaseManager
import com.abtsplazita.posplazita.data.remote.MercadoPagoManager
import com.abtsplazita.posplazita.ui.history.PrinterManager
import com.abtsplazita.posplazita.domain.repository.CashMovementRepository
import com.abtsplazita.posplazita.domain.repository.CashOutRepository
import com.abtsplazita.posplazita.domain.repository.PreCutRepository
import com.abtsplazita.posplazita.domain.repository.EmployeeRepository
import com.abtsplazita.posplazita.domain.repository.UserRepository
import kotlinx.datetime.*
import kotlin.math.abs

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PosViewModel(
    private val repository: ProductRepository,
    private val saleRepository: SaleRepository,
    private val customerRepository: CustomerRepository? = null,
    private val terminalRepository: PosTerminalRepository? = null,
    private val userRepository: UserRepository? = null,
    private val settingsRepository: SettingsRepository? = null,
    private val cashMovementRepository: CashMovementRepository? = null,
    private val cashOutRepository: CashOutRepository? = null,
    private val preCutRepository: PreCutRepository? = null,
    private val employeeRepository: EmployeeRepository? = null,
    private val mercadoPagoManager: MercadoPagoManager? = null,
    private val currentSaleManager: CurrentSaleManager,
    val branchId: String,
    private val promotionRepository: com.abtsplazita.posplazita.domain.repository.PromotionRepository? = null,
    private val deletionLogRepository: com.abtsplazita.posplazita.domain.repository.DeletionLogRepository? = null,
    private val productReturnRepository: com.abtsplazita.posplazita.domain.repository.ProductReturnRepository? = null,
    private val printerManager: PrinterManager? = null,
    private val firebaseManager: FirebaseManager? = null,
    private val scaleManager: ScaleManager = getScaleManager(),
    private val checkoutManager: com.abtsplazita.posplazita.domain.CheckoutManager,
    private val cashManager: com.abtsplazita.posplazita.domain.CashManager,
    private val customerInteractor: com.abtsplazita.posplazita.domain.CustomerInteractor
) : ViewModel() {

    enum class FocusArea { SEARCH_BAR, SEARCH_RESULTS, CART }

    val currentItems = currentSaleManager.currentItems
    val total = currentSaleManager.total
    val itemCount = currentSaleManager.itemCount

    // --- Permisos del Usuario Actual ---
    private val _userPermissions = MutableStateFlow<Map<Permission, PermissionLevel>>(emptyMap())
    val userPermissions = _userPermissions.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()

    fun setUserInfo(user: User?, permissions: Map<Permission, PermissionLevel>) {
        _currentUser.value = user
        _userPermissions.value = permissions
    }

    private fun hasPermission(permission: Permission): Boolean {
        val role = _currentUser.value?.role
        if (role == Role.SUPER_ADMIN || role == Role.GERENTE) return true
        return _userPermissions.value[permission] == PermissionLevel.ENABLED
    }

    private fun isRestricted(permission: Permission): Boolean {
        val role = _currentUser.value?.role
        if (role == Role.SUPER_ADMIN || role == Role.GERENTE) return false
        return _userPermissions.value[permission] == PermissionLevel.RESTRICTED
    }

    // --- Autorización de Administrador ---
    private val _showAuthDialog = MutableStateFlow(false)
    val showAuthDialog = _showAuthDialog.asStateFlow()

    private val _authTitle = MutableStateFlow("")
    val authTitle = _authTitle.asStateFlow()

    private var pendingAction: (() -> Unit)? = null

    private fun requestAuthorization(title: String, action: () -> Unit) {
        _authTitle.value = title
        pendingAction = action
        _showAuthDialog.value = true
    }

    fun closeAuthDialog() {
        _showAuthDialog.value = false
        pendingAction = null
    }

    fun authorizeWithPin(pin: String) {
        viewModelScope.launch {
            val user = userRepository?.getUserByNip(pin)
            if (user != null && (user.role == Role.SUPER_ADMIN || user.role == Role.GERENTE)) {
                pendingAction?.invoke()
                closeAuthDialog()
            } else {
                setErrorMessage("NIP de administrador inválido o sin permisos.")
                playErrorSound()
            }
        }
    }

    // --- Gestión de Caja Seleccionada ---
    private val _selectedTerminal = MutableStateFlow<PosTerminal?>(null)
    val selectedTerminal = _selectedTerminal.asStateFlow()

    private val _cashInDrawer = MutableStateFlow(0.0)
    val cashInDrawer = _cashInDrawer.asStateFlow()

    // --- Promociones ---
    private val _activePromotions = MutableStateFlow<List<Promotion>>(emptyList())
    val activePromotions = _activePromotions.asStateFlow()

    // --- Sidebar de Ofertas y Publicidad ---
    val sidebarItems: StateFlow<List<Any>> = combine(
        activePromotions, 
        firebaseManager?.globalAds ?: MutableStateFlow(emptyList())
    ) { promos, ads ->
        promos + ads
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _sidebarIndex = MutableStateFlow(0)
    val sidebarIndex = _sidebarIndex.asStateFlow()

    val availableTerminals: StateFlow<List<PosTerminal>> = if (terminalRepository != null) {
        terminalRepository.getTerminalsByBranch(branchId)
            .onEach { terminals ->
                if (_selectedTerminal.value == null && terminals.size == 1) {
                    _selectedTerminal.value = terminals.first()
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    } else {
        MutableStateFlow(emptyList())
    }

    fun selectTerminal(terminal: PosTerminal?) {
        _selectedTerminal.value = terminal
    }

    // --- Configuración Operativa ---
    private val _isGroupingEnabled = MutableStateFlow(true)
    val isGroupingEnabled = _isGroupingEnabled.asStateFlow()

    private val _addAtTop = MutableStateFlow(false)
    val addAtTop = _addAtTop.asStateFlow()

    private val _allowNegativeStock = MutableStateFlow(false)
    val allowNegativeStock = _allowNegativeStock.asStateFlow()

    private val _selectedPriceLevel = MutableStateFlow(2)
    val selectedPriceLevel = _selectedPriceLevel.asStateFlow()

    // --- Buscador ---
    private val _searchQuery = MutableStateFlow(TextFieldValue(""))
    val searchQuery = _searchQuery.asStateFlow()

    private val _searchMultiplier = MutableStateFlow<Double?>(null)

    private val _showSearchResults = MutableStateFlow(false)
    val showSearchResults = _showSearchResults.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Product>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    val searchStocks: StateFlow<Map<String, Double>> = combine(
        _searchResults,
        repository.getAllInventory()
    ) { results, inventory ->
        results.associate { p -> 
            val stock = inventory.find { it.productId == p.id && it.branchId == branchId }?.stock ?: 0.0
            p.id to stock
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _selectedSearchIndex = MutableStateFlow(0)
    val selectedSearchIndex = _selectedSearchIndex.asStateFlow()

    private val _currentFocusArea = MutableStateFlow(FocusArea.SEARCH_BAR)
    val currentFocusArea = _currentFocusArea.asStateFlow()

    private val _selectedCartIndex = MutableStateFlow(0)
    val selectedCartIndex = _selectedCartIndex.asStateFlow()

    private val _showNotFoundDialog = MutableStateFlow(false)
    val showNotFoundDialog = _showNotFoundDialog.asStateFlow()

    private val _notFoundQuery = MutableStateFlow("")
    val notFoundQuery = _notFoundQuery.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _warningMessage = MutableStateFlow<String?>(null)
    val warningMessage = _warningMessage.asStateFlow()

    // --- Redirecciones de CheckoutManager ---
    val isProcessingSale = checkoutManager.isProcessing
    val isWaitingForMP = checkoutManager.isWaitingForMP
    val mpStatus = checkoutManager.mpStatus
    val saleChange = checkoutManager.saleChange
    val showSaleSuccessOverlay = checkoutManager.showSaleSuccessOverlay
    val showCardSuccess = checkoutManager.showCardSuccess

    fun cancelMpPayment() = checkoutManager.cancelMpPayment()

    private val _amountPaidText = MutableStateFlow(TextFieldValue(""))
    val amountPaidText = _amountPaidText.asStateFlow()

    private val _paymentMethod = MutableStateFlow("Efectivo")
    val paymentMethod = _paymentMethod.asStateFlow()

    private val _saleComment = MutableStateFlow("")
    val saleComment = _saleComment.asStateFlow()

    private val _ticketConfig = MutableStateFlow(TicketConfig())
    val ticketConfig = _ticketConfig.asStateFlow()

    private val _branchName = MutableStateFlow("")
    val branchName = _branchName.asStateFlow()

    private val _lastSale = MutableStateFlow<Sale?>(null)
    private val _lastSaleItems = MutableStateFlow<List<SaleItem>>(emptyList())

    init {
        startLiveSearch()
        // Rotación automática del sidebar cada 10 segundos
        viewModelScope.launch {
            while (true) {
                delay(10000)
                val items = sidebarItems.value
                if (items.isNotEmpty()) {
                    _sidebarIndex.value = (_sidebarIndex.value + 1) % items.size
                }
            }
        }
        viewModelScope.launch {
            promotionRepository?.getAllPromotions()?.collect { promos ->
                val now = com.abtsplazita.posplazita.currentTimeMillis()
                val currentPromos = promos.filter { 
                    it.isActive && now >= it.startDate && now <= it.endDate 
                }
                _activePromotions.value = currentPromos
                currentSaleManager.setPromotions(currentPromos)
            }
        }
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
                    branchAddress = settings["ticket_branch_address"],
                    branchPhone = settings["ticket_branch_phone"],
                    showBranchInfo = settings["ticket_show_branch"]?.toBoolean() ?: true,
                    ticketIdPrefix = settings["ticket_id_prefix"] ?: "S",
                    layout = customLayout ?: TicketConfig.defaultLayout
                )
                
                _branchName.value = settings["${branchId}_name"] ?: ""

                // Configuración de Mayoreo Automático
                val wholesaleEnabled = settings["${branchId}_wholesale_enabled"]?.toBoolean() ?: false
                currentSaleManager.setWholesaleEnabled(wholesaleEnabled)

                // Restaurar configuración de "Agregar al principio" y "Permitir inventario negativo"
                val addAtTopEnabled = settings["${branchId}_add_at_top"]?.toBoolean() ?: false
                currentSaleManager.setAddAtTop(addAtTopEnabled)
                _addAtTop.value = addAtTopEnabled

                val allowNegativeEnabled = settings["${branchId}_allow_negative_stock"]?.toBoolean() ?: false
                currentSaleManager.setAllowNegativeStock(allowNegativeEnabled)
                _allowNegativeStock.value = allowNegativeEnabled
            }
        }
        startWebOrdersObservation()
        startDeletionRequestsObservation()
        startDeletionLogsObservation()
        viewModelScope.launch {
            combine(
                _selectedTerminal, 
                saleRepository.getSales(branchId), 
                cashMovementRepository?.getMovements(branchId) ?: flowOf(emptyList()),
                cashOutRepository?.getCashOuts(branchId) ?: flowOf(emptyList())
            ) { terminal, allSales, allMovements, allCashOuts ->
                if (terminal == null) 0.0
                else {
                    val lastCashOut = allCashOuts.filter { it.terminalId == terminal.id }.maxByOrNull { it.timestamp }
                    val startTime = lastCashOut?.timestamp ?: 0L

                    val salesCash = allSales.filter { it.terminalId == terminal.id && it.timestamp > startTime }.sumOf { it.cashAmount }
                    val entriesCash = allMovements.filter { it.terminalId == terminal.id && it.timestamp > startTime && it.type == CashMovementType.IN }.sumOf { it.amount }
                    val exitsCash = allMovements.filter { it.terminalId == terminal.id && it.timestamp > startTime && it.type == CashMovementType.OUT }.sumOf { it.amount }
                    
                    salesCash + entriesCash - exitsCash
                }
            }.flowOn(kotlinx.coroutines.Dispatchers.Default).collect { balance ->
                _cashInDrawer.value = balance
            }
        }
    }

    // --- Pedidos Web ---
    private val _webOrders = MutableStateFlow<List<WebOrder>>(emptyList())
    val webOrders = _webOrders.asStateFlow()

    private fun startWebOrdersObservation() {
        firebaseManager?.observeWebOrders(branchId) { orders ->
            _webOrders.value = orders.sortedByDescending { it.timestamp }
        }
    }

    fun updateWebOrderStatus(order: WebOrder, newStatus: WebOrderStatus) {
        viewModelScope.launch {
            try {
                val updatedOrder = order.copy(status = newStatus)
                firebaseManager?.syncWebOrder(updatedOrder)

                // Si el pedido se cancela o rechaza, devolver el stock a la nube SOLO si fue descontado previamente
                if (newStatus == WebOrderStatus.CANCELLED) {
                    order.items.forEach { item ->
                        if (item.isWebDiscounted) {
                            repository.increaseStock(item.productId, branchId, item.quantity, _currentUser.value?.username ?: "admin", "Cancelación Pedido Web ${order.id}")
                        }
                    }
                }
            } catch (e: Exception) {
                setErrorMessage("Error al actualizar estado: ${e.message}")
            }
        }
    }

    fun acceptWebOrder(order: WebOrder) {
        viewModelScope.launch {
            // 1. Cargar el pedido en el carrito actual
            currentSaleManager.clear()
            currentSaleManager.setWebOrderId(order.id)
            order.items.forEach { item ->
                val product = repository.getProductById(item.productId)
                if (product != null) {
                    currentSaleManager.addItem(
                        product, 
                        branchId, 
                        repository.getStock(product.id, branchId), 
                        item.quantity,
                        isWebDiscounted = item.isWebDiscounted // Usar el valor que viene del pedido web
                    )
                }
            }
            
            // 2. Marcar como preparando
            updateWebOrderStatus(order, WebOrderStatus.PREPARING)
            
            // 3. (Opcional) Asignar cliente si existe
            // TODO: Buscar cliente por nombre/teléfono
        }
    }

    // --- Redirecciones de CustomerInteractor ---
    val selectedCustomer = customerInteractor.selectedCustomer
    val showCustomerDialog = customerInteractor.showCustomerDialog
    val customerSearchQuery = customerInteractor.customerSearchQuery
    val selectedCustomerIndex = customerInteractor.selectedCustomerIndex
    val filteredCustomers = customerInteractor.filteredCustomers
    val showAddCustomerDialog = customerInteractor.showAddCustomerDialog
    val editingCustomer = customerInteractor.editingCustomer
    val showDebtPaymentDialog = customerInteractor.showDebtPaymentDialog

    fun openCustomerDialog() = customerInteractor.openCustomerDialog()
    fun closeCustomerDialog() = customerInteractor.closeCustomerDialog()
    fun updateCustomerSearchQuery(query: String) = customerInteractor.updateCustomerSearchQuery(query)
    fun moveCustomerFocus(delta: Int) = customerInteractor.moveCustomerFocus(delta)
    fun selectFocusedCustomer() = customerInteractor.selectFocusedCustomer()
    fun selectCustomer(customer: Customer?) = customerInteractor.selectCustomer(customer)
    fun openAddCustomerDialog() {
        if (hasPermission(Permission.CUSTOMER_CREATE)) {
            customerInteractor.openAddCustomerDialog()
        } else if (isRestricted(Permission.CUSTOMER_CREATE)) {
            requestAuthorization("Crear Cliente") { 
                customerInteractor.openAddCustomerDialog()
            }
        } else {
            setErrorMessage("No tienes permiso para crear clientes.")
        }
    }
    fun updateEditingCustomer(customer: Customer) = customerInteractor.updateEditingCustomer(customer)
    fun saveNewCustomer() = customerInteractor.saveNewCustomer(onError = { setErrorMessage(it) }, onSuccess = { setWarningMessage(it) })
    fun closeAddCustomerDialog(shouldReopenSelection: Boolean = true) = customerInteractor.closeAddCustomerDialog(shouldReopenSelection)
    fun openDebtPaymentDialog() = customerInteractor.openDebtPaymentDialog()
    fun closeDebtPaymentDialog() = customerInteractor.closeDebtPaymentDialog()
    fun processDebtPayment(customer: Customer, amount: Double) {
        customerInteractor.processDebtPayment(
            customer = customer,
            amount = amount,
            branchId = branchId,
            selectedTerminal = _selectedTerminal.value,
            currentUser = _currentUser.value,
            cashInDrawer = _cashInDrawer.value,
            ticketConfig = _ticketConfig.value,
            branchName = _branchName.value,
            onError = { setErrorMessage(it) },
            onSuccess = { setWarningMessage(it) }
        )
    }

    // --- Producto Común (Comodín) ---
    private val _showCommonProductDialog = MutableStateFlow(false)
    val showCommonProductDialog = _showCommonProductDialog.asStateFlow()

    private val _commonProductName = MutableStateFlow(TextFieldValue("PRODUCTO COMÚN"))
    val commonProductName = _commonProductName.asStateFlow()

    private val _commonProductPrice = MutableStateFlow(TextFieldValue("0"))
    val commonProductPrice = _commonProductPrice.asStateFlow()

    // --- Precorte ---
    private val _showPreCutDialog = MutableStateFlow(false)
    val showPreCutDialog = _showPreCutDialog.asStateFlow()

    private val _preCutResult = MutableStateFlow<Pair<String, Boolean>?>(null) // Message, isError/Missing
    val preCutResult = _preCutResult.asStateFlow()

    fun openPreCutDialog() { 
        _preCutResult.value = null
        _showPreCutDialog.value = true 
    }
    fun closePreCutDialog() { _showPreCutDialog.value = false }
    fun clearPreCutResult() { _preCutResult.value = null }

    fun savePreCut(countedAmount: Double, currentUserId: String = "admin", onDone: () -> Unit = {}) {
        viewModelScope.launch {
            // Validar límite de 2 precortes por día
            val countToday = preCutRepository?.getPreCutCountForUserToday(currentUserId) ?: 0
            if (countToday >= 2) {
                setErrorMessage("Límite alcanzado: Solo se permiten 2 precortes por usuario al día.")
                return@launch
            }

            val expected = _cashInDrawer.value
            val diff = countedAmount - expected
            val now = currentTimeMillis()
            val pcId = "PC-${branchId}-${_selectedTerminal.value?.id ?: "0"}-$now"
            
            val preCut = PreCut(
                id = pcId,
                timestamp = now,
                branchId = branchId,
                terminalId = _selectedTerminal.value?.id,
                expectedAmount = expected,
                countedAmount = countedAmount,
                difference = diff,
                userId = currentUserId
            )
            
            preCutRepository?.savePreCut(preCut)

            // Lógica de cierre de turno y pago
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
            
            if (diff < 0) {
                _preCutResult.value = "Precorte guardado.\nTe falta $${abs(diff).formatPrice()}" to true
            } else {
                _preCutResult.value = "Precorte guardado.\nTu corte está bien." to false
            }
            closePreCutDialog()
            onDone()
        }
    }

    fun openCommonProductDialog() {
        _commonProductName.value = TextFieldValue("PRODUCTO COMÚN", TextRange(0, "PRODUCTO COMÚN".length))
        _commonProductPrice.value = TextFieldValue("0")
        _showCommonProductDialog.value = true
    }
    fun closeCommonProductDialog() { _showCommonProductDialog.value = false }
    fun onCommonProductPriceChange(value: TextFieldValue) { _commonProductPrice.value = value }
    fun onCommonProductNameChange(value: TextFieldValue) { _commonProductName.value = value }

    fun addCommonProduct() {
        val name = _commonProductName.value.text
        val price = _commonProductPrice.value.text.toDoubleOrNull() ?: 0.0
        if (price > 0) {
            val dummyProduct = Product(
                id = "COMMON_${currentTimeMillis()}",
                name = name,
                barcode = "0",
                price1 = price,
                price2 = price,
                price3 = price,
                price4 = price,
                isService = true
            )
            addProduct(dummyProduct, 9999.0)
            onSearchQueryClear() // Limpiar el buscador principal tras agregar
            closeCommonProductDialog()
        } else {
            setErrorMessage("No puedes vender un producto sin precio.")
        }
    }

    fun openCommonWithShortcut(text: String) {
        val price = text.toDoubleOrNull() ?: 0.0
        if (price > 0) {
            _commonProductPrice.value = TextFieldValue(text)
            _commonProductName.value = TextFieldValue("PRODUCTO COMÚN", TextRange(0, "PRODUCTO COMÚN".length))
            _showCommonProductDialog.value = true
        }
    }

    // --- Edición de Cantidad ---
    private val _showQuantityDialog = MutableStateFlow(false)
    val showQuantityDialog = _showQuantityDialog.asStateFlow()

    private val _editingQuantityText = MutableStateFlow(TextFieldValue(""))
    val editingQuantityText = _editingQuantityText.asStateFlow()

    private val _editingItem = MutableStateFlow<SaleItem?>(null)
    val editingItem = _editingItem.asStateFlow()

    fun openQuantityDialog(item: SaleItem) {
        _editingItem.value = item
        _editingQuantityText.value = TextFieldValue(item.quantity.toString())
        _showQuantityDialog.value = true
    }

    fun onEditingQuantityChange(value: TextFieldValue) { _editingQuantityText.value = value }

    fun confirmQuantityUpdate() {
        val item = _editingItem.value ?: return
        val newQty = _editingQuantityText.value.text.toDoubleOrNull() ?: 0.0
        if (newQty > 0) {
            // Validar granel
            if (!item.isBulk && (newQty % 1.0 != 0.0)) {
                setErrorMessage("El producto '${item.productName}' no permite venta fraccionada (Granel).")
                playErrorSound()
                return
            }
            currentSaleManager.updateItemQuantity(item, newQty)
            closeQuantityDialog()
        }
    }

    fun closeQuantityDialog() {
        _showQuantityDialog.value = false
        _editingItem.value = null
    }

    // --- Venta a Granel ---
    private val _showBulkQuantityDialog = MutableStateFlow(false)
    val showBulkQuantityDialog = _showBulkQuantityDialog.asStateFlow()

    private val _bulkEditingProduct = MutableStateFlow<Product?>(null)
    val bulkEditingProduct = _bulkEditingProduct.asStateFlow()
    
    private var _bulkEditingStock = 0.0

    private val _bulkQuantityText = MutableStateFlow(TextFieldValue("0.000"))
    val bulkQuantityText = _bulkQuantityText.asStateFlow()

    fun openBulkQuantityDialog(product: Product, stock: Double) {
        _bulkEditingProduct.value = product
        _bulkEditingStock = stock
        _bulkQuantityText.value = TextFieldValue("0.000")
        _showBulkQuantityDialog.value = true
    }

    fun onBulkQuantityChange(value: TextFieldValue) { _bulkQuantityText.value = value }

    fun confirmBulkQuantity() {
        val product = _bulkEditingProduct.value ?: return
        val qty = _bulkQuantityText.value.text.toDoubleOrNull() ?: 0.0
        if (qty > 0) {
            addProduct(product, _bulkEditingStock, qty)
            closeBulkQuantityDialog()
        }
    }

    fun closeBulkQuantityDialog() {
        _showBulkQuantityDialog.value = false
        _bulkEditingProduct.value = null
    }

    fun readScale() {
        viewModelScope.launch {
            val weight = scaleManager.readWeight()
            if (weight != null && weight > 0) {
                _bulkQuantityText.value = TextFieldValue(weight.toString())
            } else {
                setErrorMessage("No se pudo obtener peso de la báscula.")
            }
        }
    }

    // --- Recargas ---
    private val _showRechargeDialog = MutableStateFlow(false)
    val showRechargeDialog = _showRechargeDialog.asStateFlow()

    private val _showMultiserviceDialog = MutableStateFlow(false)
    val showMultiserviceDialog = _showMultiserviceDialog.asStateFlow()

    private val _rechargePhone = MutableStateFlow("")
    val rechargePhone = _rechargePhone.asStateFlow()

    private val _selectedCarrier = MutableStateFlow<Carrier?>(null)
    val selectedCarrier = _selectedCarrier.asStateFlow()

    private val _rechargeAmount = MutableStateFlow(0.0)
    val rechargeAmount = _rechargeAmount.asStateFlow()

    private val _isProcessingRecharge = MutableStateFlow(false)
    val isProcessingRecharge = _isProcessingRecharge.asStateFlow()

    val carriers = listOf(
        Carrier("telcel", "Telcel", ""),
        Carrier("movistar", "Movistar", ""),
        Carrier("att", "AT&T", ""),
        Carrier("bait", "Bait", ""),
        Carrier("pillofon", "Pillofon", ""),
        Carrier("unefon", "Unefon", "")
    )



    fun closeRechargeDialog() { _showRechargeDialog.value = false }

    fun updateRechargePhone(phone: String) {
        if (phone.length <= 10 && phone.all { it.isDigit() }) {
            _rechargePhone.value = phone
        }
    }

    fun selectCarrier(carrier: Carrier) { _selectedCarrier.value = carrier }
    fun selectRechargeAmount(amount: Double) { _rechargeAmount.value = amount }

    fun processRecharge(onDone: () -> Unit) {
        if (_isProcessingRecharge.value) return
        viewModelScope.launch {
            _isProcessingRecharge.value = true
            try {
                kotlinx.coroutines.delay(2000) // Simulación
                val carrier = _selectedCarrier.value!!
                val phone = _rechargePhone.value
                val amount = _rechargeAmount.value
                
                val product = Product(
                    id = "REC_${carrier.id}",
                    name = "RECARGA ${carrier.name} $phone",
                    barcode = "0",
                    price3 = amount,
                    isService = true
                )
                addProduct(product, 9999.0)
                closeRechargeDialog()
                onDone()
            } catch (e: Exception) {
                setErrorMessage("Error al procesar recarga: ${e.message}")
            } finally {
                _isProcessingRecharge.value = false
            }
        }
    }

    // --- Pago de Servicios ---
    private val _showServiceDialog = MutableStateFlow(false)
    val showServiceDialog = _showServiceDialog.asStateFlow()

    private val _selectedService = MutableStateFlow<ServiceCompany?>(null)
    val selectedService = _selectedService.asStateFlow()

    private val _serviceReference = MutableStateFlow("")
    val serviceReference = _serviceReference.asStateFlow()

    private val _serviceAmountText = MutableStateFlow(TextFieldValue(""))
    val serviceAmountText = _serviceAmountText.asStateFlow()

    private val _isProcessingService = MutableStateFlow(false)
    val isProcessingService = _isProcessingService.asStateFlow()

    val serviceCompanies = listOf(
        ServiceCompany("cfe", "CFE", "Luz", 12.0),
        ServiceCompany("telmex", "Telmex", "Teléfono", 10.0),
        ServiceCompany("siapa", "SIAPA", "Agua", 10.0),
        ServiceCompany("naturgy", "Naturgy", "Gas", 10.0),
        ServiceCompany("dish", "Dish", "TV", 10.0),
        ServiceCompany("sky", "Sky", "TV", 10.0)
    )

    fun openServiceDialog() {
        _selectedService.value = null
        _serviceReference.value = ""
        _serviceAmountText.value = TextFieldValue("")
        _showServiceDialog.value = true
        _showMultiserviceDialog.value = false
    }

    fun openRechargeDialog() {
        _rechargePhone.value = ""
        _selectedCarrier.value = null
        _rechargeAmount.value = 0.0
        _showRechargeDialog.value = true
        _showMultiserviceDialog.value = false
    }

    fun openMultiserviceDialog() { _showMultiserviceDialog.value = true }
    fun closeMultiserviceDialog() { _showMultiserviceDialog.value = false }

    fun closeServiceDialog() { _showServiceDialog.value = false }

    fun selectService(company: ServiceCompany) { _selectedService.value = company }
    fun updateServiceReference(ref: String) { _serviceReference.value = ref }
    fun updateServiceAmount(value: TextFieldValue) { _serviceAmountText.value = value }

    fun processServicePayment(onDone: () -> Unit) {
        if (_isProcessingService.value) return
        viewModelScope.launch {
            _isProcessingService.value = true
            try {
                kotlinx.coroutines.delay(2000)
                val company = _selectedService.value!!
                val amount = _serviceAmountText.value.text.toDoubleOrNull() ?: 0.0
                
                val product = Product(
                    id = "SERV_${company.id}",
                    name = "PAGO ${company.name} - ${_serviceReference.value}",
                    barcode = "0",
                    price3 = amount + company.fee,
                    isService = true
                )
                addProduct(product, 9999.0)
                closeServiceDialog()
                onDone()
            } catch (e: Exception) {
                setErrorMessage("Error al procesar servicio: ${e.message}")
            } finally {
                _isProcessingService.value = false
            }
        }
    }

    // --- Tickets Guardados (En Espera) ---
    private val _showHeldSalesDialog = MutableStateFlow(false)
    val showHeldSalesDialog = _showHeldSalesDialog.asStateFlow()

    val heldSales: StateFlow<List<HeldSale>> = saleRepository.getHeldSales(branchId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedHeldSaleIndex = MutableStateFlow(0)
    val selectedHeldSaleIndex = _selectedHeldSaleIndex.asStateFlow()

    fun putSaleOnHold() {
        if (currentItems.value.isEmpty()) return
        
        // Limitar a máximo 5 ventas en espera
        if (heldSales.value.size >= 5) {
            setErrorMessage("Límite alcanzado: Solo puedes tener 5 ventas en espera.")
            playErrorSound()
            return
        }

        viewModelScope.launch {
            val heldSale = HeldSale(
                id = "H${currentTimeMillis()}",
                timestamp = currentTimeMillis(),
                branchId = branchId,
                terminalId = _selectedTerminal.value?.id,
                items = currentItems.value,
                customerId = selectedCustomer.value?.id,
                total = total.value
            )
            saleRepository.saveHeldSale(heldSale)
            // Usar limpieza automática (isManual = false) para no pedir PIN al guardar
            clearSale(isManual = false)
            setWarningMessage("Venta guardada en espera.")
        }
    }

    fun openHeldSalesDialog() {
        _selectedHeldSaleIndex.value = 0
        _showHeldSalesDialog.value = true
    }
    fun closeHeldSalesDialog() { _showHeldSalesDialog.value = false }

    fun moveHeldSaleFocus(delta: Int) {
        val count = heldSales.value.size
        if (count > 0) {
            _selectedHeldSaleIndex.value = (_selectedHeldSaleIndex.value + delta).coerceIn(0, count - 1)
        }
    }

    fun selectFocusedHeldSale() {
        val sales = heldSales.value
        if (_selectedHeldSaleIndex.value in sales.indices) {
            resumeHeldSale(sales[_selectedHeldSaleIndex.value])
        }
    }

    fun resumeHeldSale(heldSale: HeldSale) {
        viewModelScope.launch {
            // Si ya hay algo en el carrito, guardarlo primero de forma síncrona
            if (currentItems.value.isNotEmpty()) {
                val currentHeld = HeldSale(
                    id = "H${currentTimeMillis()}",
                    timestamp = currentTimeMillis(),
                    branchId = branchId,
                    terminalId = _selectedTerminal.value?.id,
                    items = currentItems.value,
                    customerId = selectedCustomer.value?.id,
                    total = total.value
                )
                saleRepository.saveHeldSale(currentHeld)
            }
            
            // 1. Recuperar los artículos reales de la base de datos
            val realItems = saleRepository.getHeldSaleItems(heldSale.id)

            // 2. Cargar la nueva venta
            currentSaleManager.loadItems(realItems)
            val customer = if (!heldSale.customerId.isNullOrBlank()) {
                customerRepository?.getCustomerById(heldSale.customerId)
            } else null
            selectCustomer(customer)
            
            saleRepository.deleteHeldSale(heldSale.id)
            closeHeldSalesDialog()
        }
    }

    fun deleteHeldSale(heldSale: HeldSale) {
        // Evitar duplicados si ya está en revisión (local o remoto)
        if (_localPendingTicketIds.value.contains(heldSale.id) || _pendingDeletionTicketIds.value.contains(heldSale.id)) return

        val action = {
            viewModelScope.launch {
                saleRepository.deleteHeldSale(heldSale.id)
                firebaseManager?.deleteHeldSale(heldSale.id)
            }
            Unit
        }

        if (hasPermission(Permission.CANCEL_SALE)) {
            action()
        } else {
            // MODIFICACIÓN: Si no tiene permiso directo (Admin/Gerente), 
            // siempre mandamos solicitud de borrado al administrador.
            viewModelScope.launch {
                // Optimista: Bloquear botón de inmediato
                _localPendingTicketIds.value += heldSale.id

                // Cargar los items reales si vienen vacíos
                val realItems = if (heldSale.items.isEmpty()) {
                    saleRepository.getHeldSaleItems(heldSale.id)
                } else heldSale.items

                val request = DeletionRequest(
                    id = "DR_${currentTimeMillis()}",
                    ticketId = heldSale.id,
                    timestamp = currentTimeMillis(),
                    userId = _currentUser.value?.username ?: "admin",
                    total = heldSale.total,
                    itemsSummary = realItems.joinToString(", ") { "${it.quantity}x ${it.productName}" },
                    branchId = branchId,
                    status = "PENDING"
                )
                firebaseManager?.syncDeletionRequest(request)
                setWarningMessage("Solicitud enviada a revisión del administrador.")
            }
        }
    }

    // --- Checkout y Métodos de Venta ---
    fun updateAmountPaid(text: TextFieldValue) {
        if (showSaleSuccessOverlay.value) return 
        
        if (text.text.isEmpty() || text.text.all { it.isDigit() || it == '.' }) {
            _amountPaidText.value = text
            val amount = text.text.toDoubleOrNull() ?: 0.0
            checkoutManager.updateChange(amount, currentSaleManager.total.value, _paymentMethod.value)
        }
    }

    fun prepareCheckout() {
        val totalAmount = currentSaleManager.total.value
        _amountPaidText.value = TextFieldValue(totalAmount.formatPrice(), TextRange(0, totalAmount.formatPrice().length))
        _paymentMethod.value = "Efectivo"
        _saleComment.value = ""
        checkoutManager.resetOverlay()
        checkoutManager.updateChange(totalAmount, totalAmount, "Efectivo")
    }

    fun cancelCheckout() {
        checkoutManager.resetOverlay()
        _paymentMethod.value = "Efectivo"
        checkoutManager.updateChange(0.0, 0.0, "Tarjeta") // Ocultar cambio
    }

    fun setPaymentMethod(method: String) {
        _paymentMethod.value = method
        val amount = _amountPaidText.value.text.toDoubleOrNull() ?: currentSaleManager.total.value
        checkoutManager.updateChange(amount, currentSaleManager.total.value, method)

        // Si es crédito y no hay cliente, abrir buscador de clientes
        if (method == "Crédito" && selectedCustomer.value == null) {
            openCustomerDialog()
        }
    }

    fun setSaleComment(comment: String) { _saleComment.value = comment }

    fun completeSale(shouldPrint: Boolean, onDone: () -> Unit) {
        if (currentItems.value.isEmpty()) return
        if (isProcessingSale.value) return

        viewModelScope.launch {
            val paymentMethod = _paymentMethod.value
            val isCredit = paymentMethod == "Crédito"
            
            if (isCredit) {
                if (hasPermission(Permission.SELL_ON_CREDIT)) {
                    // OK
                } else if (isRestricted(Permission.SELL_ON_CREDIT)) {
                    requestAuthorization("Venta a Crédito") {
                        executeCompleteSale(shouldPrint, onDone)
                    }
                    return@launch
                } else {
                    setErrorMessage("No tienes permiso para vender a crédito.")
                    return@launch
                }
            }

            executeCompleteSale(shouldPrint, onDone)
        }
    }

    private fun executeCompleteSale(shouldPrint: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            val rawPaidText = _amountPaidText.value.text.trim()
            val currentTotal = total.value
            val amountPaid = if (rawPaidText.isEmpty()) currentTotal else (rawPaidText.toDoubleOrNull() ?: currentTotal)

            checkoutManager.executeCheckout(
                amountPaid = amountPaid,
                paymentMethod = _paymentMethod.value,
                branchId = branchId,
                selectedTerminal = _selectedTerminal.value,
                selectedCustomer = selectedCustomer.value,
                currentUser = _currentUser.value,
                saleComment = _saleComment.value,
                ticketConfig = _ticketConfig.value,
                branchName = _branchName.value,
                shouldPrint = shouldPrint,
                onDone = {
                    // Limpiar datos de búsqueda al terminar la venta
                    onSearchQueryClear()
                    onDone()
                },
                onError = { setErrorMessage(it) }
            )
        }
    }

    // --- Solicitudes de Borrado ---
    private val _deletionRequests = MutableStateFlow<List<DeletionRequest>>(emptyList())
    val deletionRequests = _deletionRequests.asStateFlow()

    private val _pendingDeletionTicketIds = MutableStateFlow<Set<String>>(emptySet())
    private val _localPendingTicketIds = MutableStateFlow<Set<String>>(emptySet())

    val pendingDeletionTicketIds: StateFlow<Set<String>> = combine(_pendingDeletionTicketIds, _localPendingTicketIds) { remote, local ->
        remote + local
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val _deletionLogs = MutableStateFlow<List<DeletionLog>>(emptyList())
    val deletionLogs = _deletionLogs.asStateFlow()

    private fun startDeletionRequestsObservation() {
        firebaseManager?.observeDeletionRequests(branchId) { requests ->
            // Ahora la eliminación local la maneja SyncManager de forma global.
            // Aquí solo mostramos pendientes para el admin (Autorizaciones)
            val pending = requests.filter { it.status == "PENDING" }
            _deletionRequests.value = pending.sortedByDescending { it.timestamp }
            
            val remoteIds = pending.map { it.ticketId }.toSet()
            _pendingDeletionTicketIds.value = remoteIds
            
            // Limpiar de locales los que ya llegaron al servidor
            _localPendingTicketIds.value = _localPendingTicketIds.value.filter { !remoteIds.contains(it) }.toSet()
        }
    }

    private fun startDeletionLogsObservation() {
        firebaseManager?.observeDeletionLogs(branchId) { logs ->
            _deletionLogs.value = logs.sortedByDescending { it.timestamp }
        }
    }

    fun approveDeletionRequest(request: DeletionRequest) {
        viewModelScope.launch {
            try {
                // 1. Si somos el admin que aprueba, marcamos como aprobado en la nube
                val approvedRequest = request.copy(status = "APPROVED")
                firebaseManager?.syncDeletionRequest(approvedRequest)

                // 2. Crear registro en el Log de Borrados (Reporte)
                val log = DeletionLog(
                    id = "DL_${currentTimeMillis()}",
                    ticketId = request.ticketId,
                    timestamp = currentTimeMillis(),
                    requesterId = request.userId,
                    approverId = _currentUser.value?.username ?: "admin",
                    total = request.total,
                    itemsSummary = request.itemsSummary,
                    branchId = branchId
                )
                deletionLogRepository?.saveLog(log)

                // 3. Ejecutar el borrado real de la venta guardada (en local y nube)
                saleRepository.deleteHeldSale(request.ticketId)
                firebaseManager?.deleteHeldSale(request.ticketId)
                
                setWarningMessage("Borrado Exitoso: Ticket aprobado y eliminado.")
            } catch (e: Exception) {
                setErrorMessage("Error al aprobar: ${e.message}")
            }
        }
    }

    fun rejectDeletionRequest(request: DeletionRequest) {
        viewModelScope.launch {
            try {
                firebaseManager?.deleteDeletionRequest(request.id)
                setWarningMessage("Solicitud rechazada.")
            } catch (e: Exception) {
                setErrorMessage("Error al rechazar: ${e.message}")
            }
        }
    }

    // --- Buscador y Carrito ---
    fun onSearchQueryChange(query: TextFieldValue) { 
        val text = query.text
        
        // Quitar ventana de cambio al empezar a escribir algo nuevo
        if (text.isNotEmpty() && showSaleSuccessOverlay.value) {
            checkoutManager.resetOverlay()
        }
        
        // Soporte para atajo instantáneo (ej: 15+)
        if (text.endsWith("+") && text.length > 1) {
            val part = text.removeSuffix("+")
            if (part.all { it.isDigit() || it == '.' }) {
                // Siempre abrir diálogo de producto común para el atajo "precio+"
                // Se eliminó la actualización de cantidad con "+" para evitar conflictos con precios
                openCommonWithShortcut(part)
                _searchQuery.value = TextFieldValue("")
                return
            }
        }
        _searchQuery.value = query 
    }

    fun setSelectedCartIndex(index: Int) {
        _selectedCartIndex.value = index
        _currentFocusArea.value = FocusArea.CART
    }

    fun onSearchSubmit() {
        val rawQuery = _searchQuery.value.text.trim()
        
        // Si se presiona Enter con el buscador vacío pero abierto, seleccionamos el primero
        if (rawQuery.isBlank()) {
            if (_showSearchResults.value && searchResults.value.isNotEmpty()) {
                selectCurrentItem()
            }
            return
        }
        
        // Quitar ventana de cambio si se inicia nueva búsqueda
        checkoutManager.resetOverlay()

        var multiplier: Double? = null
        var query = rawQuery

        // --- Soporte para cantidad * producto ---
        if (rawQuery.contains("*")) {
            val parts = rawQuery.split("*")
            if (parts.size == 2) {
                multiplier = parts[0].trim().toDoubleOrNull()
                query = parts[1].trim()
            }
        }
        
        _searchMultiplier.value = multiplier

        // --- Detección de Código de Barras de Cliente (Monedero) ---
        if (query.startsWith("CLI-", ignoreCase = true)) {
            val customerId = query.substring(4)
            viewModelScope.launch {
                val customer = customerRepository?.getCustomerById(customerId)
                if (customer != null) {
                    selectCustomer(customer)
                    onSearchQueryClear()
                } else {
                    _notFoundQuery.value = query
                    _showNotFoundDialog.value = true
                    playErrorSound()
                }
            }
            return
        }

        viewModelScope.launch {
            // 1. Intentar por código de barras exacto (Escaneo)
            var exactMatch = repository.getProductByBarcode(query)
            
            // --- NUEVO: Si no existe localmente, buscar en la nube ---
            if (exactMatch == null) {
                val cloudProduct = firebaseManager?.fetchProductByBarcode(query)
                if (cloudProduct != null) {
                    // Guardar localmente para futuras consultas
                    repository.syncProductBatch(listOf(cloudProduct))
                    exactMatch = cloudProduct
                }
            }

            if (exactMatch != null) {
                val mult = _searchMultiplier.value
                if (mult != null) {
                    if (!exactMatch.isBulk && (mult % 1.0 != 0.0)) {
                        setErrorMessage("El producto '${exactMatch.name}' no permite venta fraccionada.")
                        playErrorSound()
                        return@launch
                    }
                    addProduct(exactMatch, repository.getStock(exactMatch.id, branchId), mult)
                } else if (exactMatch.useScale) {
                    val weight = scaleManager.readWeight()
                    if (weight != null && weight > 0) {
                        addProduct(exactMatch, repository.getStock(exactMatch.id, branchId), weight)
                    } else {
                        openBulkQuantityDialog(exactMatch, repository.getStock(exactMatch.id, branchId))
                    }
                } else if (exactMatch.isBulk) {
                    openBulkQuantityDialog(exactMatch, repository.getStock(exactMatch.id, branchId))
                } else {
                    val stock = repository.getStock(exactMatch.id, branchId)
                    addProduct(exactMatch, stock)
                }
                selectSearchQuery()
                _searchMultiplier.value = null 
                return@launch
            }

            // 2. Si no hay coincidencia exacta, realizar búsqueda por nombre/fragmentos
            repository.searchProducts(query).first().let { results ->
                if (results.isNotEmpty()) {
                    _searchResults.value = results
                    _selectedSearchIndex.value = 0
                    _showSearchResults.value = true
                    _currentFocusArea.value = FocusArea.SEARCH_RESULTS
                    
                    // Nota: Ya no auto-seleccionamos si hay solo 1 resultado, 
                    // para forzar que sea el usuario quien elija o que el código de barras sea exacto.
                } else {
                    _notFoundQuery.value = query
                    _showNotFoundDialog.value = true
                    playErrorSound() 
                }
            }
        }
    }

    fun setFocusArea(area: FocusArea) {
        _currentFocusArea.value = area
    }

    private val _focusSearchRequest = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val focusSearchRequest = _focusSearchRequest.asSharedFlow()

    fun selectSearchQuery() {
        val text = _searchQuery.value.text
        _searchQuery.value = _searchQuery.value.copy(selection = TextRange(0, text.length))
        _currentFocusArea.value = FocusArea.SEARCH_BAR
        _focusSearchRequest.tryEmit(Unit)
    }

    fun handleGlobalEscape(onNavigateToCheckout: () -> Unit) {
        if (_showSearchResults.value) {
            onSearchQueryClear()
            selectSearchQuery()
        } else if (itemCount.value > 0 && _currentFocusArea.value == FocusArea.SEARCH_BAR) {
            prepareCheckout()
            onNavigateToCheckout()
        } else {
            onSearchQueryClear() // Asegurar que esté vacío si no hay resultados
            selectSearchQuery()
        }
    }

    fun onSearchQueryClear() {
        _searchQuery.value = TextFieldValue("")
        _showSearchResults.value = false
        _searchResults.value = emptyList()
        _searchMultiplier.value = null
        
        // También quitar ventana de cambio si se limpia el buscador
        checkoutManager.resetOverlay()
    }

    fun moveFocus(delta: Int) {
        if (_showSearchResults.value) {
            val count = _searchResults.value.size
            if (count > 0) {
                _selectedSearchIndex.value = (_selectedSearchIndex.value + delta).coerceIn(0, count - 1)
            }
        } else {
            val count = currentItems.value.size
            if (_currentFocusArea.value == FocusArea.SEARCH_BAR) {
                if (delta > 0 && count > 0) {
                    _currentFocusArea.value = FocusArea.CART
                    _selectedCartIndex.value = 0
                }
            } else if (_currentFocusArea.value == FocusArea.CART) {
                val newIndex = _selectedCartIndex.value + delta
                if (newIndex < 0) {
                    _currentFocusArea.value = FocusArea.SEARCH_BAR
                    _selectedCartIndex.value = 0
                } else if (count > 0) {
                    _selectedCartIndex.value = newIndex.coerceAtMost(count - 1)
                }
            }
        }
    }

    fun selectCurrentItem() {
        if (_showSearchResults.value) {
            val p = searchResults.value[_selectedSearchIndex.value]
            onResultClick(p)
        }
    }

    fun onResultClick(product: Product) {
        val mult = _searchMultiplier.value
        val stock = searchStocks.value[product.id] ?: 0.0
        
        if (mult != null) {
            if (!product.isBulk && (mult % 1.0 != 0.0)) {
                setErrorMessage("El producto '${product.name}' no permite venta fraccionada (Granel).")
                playErrorSound()
                return
            }
            addProduct(product, stock, mult)
        } else if (product.useScale) {
            val weight = scaleManager.readWeight()
            if (weight != null && weight > 0) {
                addProduct(product, stock, weight)
            } else {
                openBulkQuantityDialog(product, stock)
            }
        } else if (product.isBulk) {
            openBulkQuantityDialog(product, stock)
        } else {
            addProduct(product, stock)
        }
        _showSearchResults.value = false
        _searchMultiplier.value = null
        selectSearchQuery()
    }

    fun addProduct(product: Product, stock: Double, quantity: Double? = null, isReturn: Boolean = false) {
        checkoutManager.resetOverlay()

        // Validar que el producto tenga precio según el nivel seleccionado
        val price = when(_selectedPriceLevel.value) {
            1 -> product.price1
            2 -> product.price2
            3 -> product.price3
            4 -> product.price4
            else -> product.price2
        }
        
        if (price <= 0 && !isReturn && !product.isService) {
            setErrorMessage("No puedes vender un producto sin precio.")
            return
        }
        
        val added = currentSaleManager.addItem(product, branchId, stock, quantity, isReturn = isReturn)
        if (added) {
            // Mantener el foco en la barra de búsqueda para seguir escaneando
            _selectedCartIndex.value = currentItems.value.size - 1
            _currentFocusArea.value = FocusArea.SEARCH_BAR
        } else {
            setWarningMessage("Stock insuficiente para: ${product.name}")
        }
    }

    fun removeSaleItem(item: SaleItem) { 
        if (hasPermission(Permission.DELETE_SALE_ITEM)) {
            currentSaleManager.removeItem(item) 
        } else if (isRestricted(Permission.DELETE_SALE_ITEM)) {
            requestAuthorization("Eliminar Producto") { currentSaleManager.removeItem(item) }
        }
    }

    fun clearSale(isManual: Boolean = true) {
        val action = {
            currentSaleManager.clear()
            _searchQuery.value = TextFieldValue("")
            _amountPaidText.value = TextFieldValue("") // Limpiar monto pagado
            selectCustomer(null)
            _saleComment.value = ""
        }

        if (!isManual || hasPermission(Permission.CANCEL_SALE)) {
            action()
        } else if (isRestricted(Permission.CANCEL_SALE)) {
            if (currentItems.value.isNotEmpty()) {
                requestAuthorization("Cancelar Venta", action)
            }
        }
    }

    fun incrementSelectedCartItem() {
        val items = currentItems.value
        val index = selectedCartIndex.value
        if (index in items.indices) {
            val item = items[index]
            currentSaleManager.updateItemQuantity(item, item.quantity + 1)
        }
    }

    fun decrementSelectedCartItem() {
        val items = currentItems.value
        val index = selectedCartIndex.value
        if (index in items.indices) {
            val item = items[index]
            if (item.quantity > 1) {
                currentSaleManager.updateItemQuantity(item, item.quantity - 1)
            } else {
                removeSaleItem(item)
            }
        }
    }

    // --- Otros ---
    private var errorJob: kotlinx.coroutines.Job? = null
    private var warningJob: kotlinx.coroutines.Job? = null
    private var successJob: kotlinx.coroutines.Job? = null

    fun clearError() { 
        errorJob?.cancel()
        _errorMessage.value = null 
        selectSearchQuery()
    }
    fun setErrorMessage(msg: String) { 
        errorJob?.cancel()
        _errorMessage.value = msg 
        errorJob = viewModelScope.launch {
            delay(2000)
            _errorMessage.value = null
        }
    }
    fun clearWarning() { 
        warningJob?.cancel()
        _warningMessage.value = null 
    }
    fun setWarningMessage(msg: String) {
        warningJob?.cancel()
        _warningMessage.value = msg
        warningJob = viewModelScope.launch {
            delay(2000)
            _warningMessage.value = null
        }
    }
    fun closeNotFoundDialog() { 
        _showNotFoundDialog.value = false 
        selectSearchQuery()
    }
    fun clearChange() { 
        checkoutManager.resetOverlay()
    }
    fun reprintLastSale() {
        val sale = _lastSale.value ?: return
        val items = _lastSaleItems.value
        viewModelScope.launch {
            val customer = if (sale.customerId != null) customerRepository?.getCustomerById(sale.customerId) else null
            printerManager?.printTicket(
                sale, 
                items, 
                openDrawer = sale.paymentMethod == "Efectivo",
                walletBalance = customer?.walletBalance,
                config = _ticketConfig.value,
                branchName = _branchName.value
            )
        }
    }
    fun openCashDrawer() {
        if (hasPermission(Permission.OPEN_CASH_DRAWER)) {
            printerManager?.openDrawer()
        } else if (isRestricted(Permission.OPEN_CASH_DRAWER)) {
            requestAuthorization("Abrir Cajón") { printerManager?.openDrawer() }
        }
        // Si está DISABLED, no hace nada (o avisar)
    }
    fun setDefaultPriceLevel(level: Int) {
        _selectedPriceLevel.value = level
        currentSaleManager.setDefaultPriceLevel(level)
    }

    // --- Comentarios ---
    private val _showCommentDialog = MutableStateFlow(false)
    val showCommentDialog = _showCommentDialog.asStateFlow()

    fun openCommentDialog() { _showCommentDialog.value = true }
    fun closeCommentDialog() { _showCommentDialog.value = false }

    fun updateSaleComment(comment: String) {
        _saleComment.value = comment
    }
    fun toggleGrouping(enabled: Boolean) {
        _isGroupingEnabled.value = enabled
        currentSaleManager.setGrouping(enabled)
    }

    fun setAllowNegativeStock(enabled: Boolean) {
        _allowNegativeStock.value = enabled
        currentSaleManager.setAllowNegativeStock(enabled)
        viewModelScope.launch {
            settingsRepository?.saveSetting("${branchId}_allow_negative_stock", enabled.toString())
        }
    }

    fun setAddAtTop(enabled: Boolean) {
        _addAtTop.value = enabled
        currentSaleManager.setAddAtTop(enabled)
        viewModelScope.launch {
            settingsRepository?.saveSetting("${branchId}_add_at_top", enabled.toString())
        }
    }

    // --- Caja y Movimientos ---
    val showCashMovementDialog = cashManager.showCashMovementDialog
    val showWithdrawalDialog = cashManager.showWithdrawalDialog
    val isProcessingWithdrawal = cashManager.isProcessingWithdrawal

    fun openCashMovementDialog(type: CashMovementType) = cashManager.openCashMovementDialog(type)
    fun closeCashMovementDialog() = cashManager.closeCashMovementDialog()

    fun addCashMovement(amount: Double, reason: String, isManual: Boolean = true) {
        cashManager.addCashMovement(
            amount = amount,
            reason = reason,
            branchId = branchId,
            selectedTerminal = _selectedTerminal.value,
            currentUser = _currentUser.value,
            cashInDrawer = _cashInDrawer.value,
            isManual = isManual,
            onError = { setErrorMessage(it) },
            onSuccess = { setWarningMessage(it) }
        )
    }

    fun openWithdrawalDialog() = cashManager.openWithdrawalDialog()
    fun closeWithdrawalDialog() = cashManager.closeWithdrawalDialog()

    fun processWithdrawal(withdrawalAmount: Double, commission: Double, commissionInCash: Boolean) {
        cashManager.processWithdrawal(
            withdrawalAmount = withdrawalAmount,
            commission = commission,
            commissionInCash = commissionInCash,
            branchId = branchId,
            selectedTerminal = _selectedTerminal.value,
            currentUser = _currentUser.value,
            cashInDrawer = _cashInDrawer.value,
            onError = { setErrorMessage(it) },
            onSuccess = { setWarningMessage(it) }
        )
    }

    // --- Devoluciones / Cambios ---
    private val _showReturnDialog = MutableStateFlow(false)
    val showReturnDialog = _showReturnDialog.asStateFlow()

    suspend fun findProductForReturn(query: String): Product? {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return null
        
        // 1. Intentar por código de barras exacto (escaneo)
        val byBarcode = repository.getProductByBarcode(trimmed)
        if (byBarcode != null) return byBarcode
        
        // 2. Intentar búsqueda por nombre o fragmentos
        try {
            val results = repository.searchProducts(trimmed, limit = 5).first()
            return results.firstOrNull()
        } catch (e: Exception) {
            return null
        }
    }

    fun searchSales(query: String): Flow<List<Sale>> {
        if (query.isBlank()) return flowOf(emptyList())
        return saleRepository.searchSales(branchId, query)
    }

    fun searchProductsForReturn(query: String): Flow<List<Product>> {
        if (query.isBlank()) return flowOf(emptyList())
        return repository.searchProducts(query, limit = 10)
    }

    suspend fun getSaleById(id: String): Sale? {
        return saleRepository.getSaleById(id)
    }

    fun openReturnDialog() { _showReturnDialog.value = true }
    fun closeReturnDialog() { _showReturnDialog.value = false }

    fun processReturn(returnedItem: SaleItem, takenItem: SaleItem?, difference: Double) {
        viewModelScope.launch {
            try {
                val returnId = "RET_${currentTimeMillis()}"
                repository.increaseStock(returnedItem.productId, branchId, returnedItem.quantity, _currentUser.value?.username ?: "admin", "Devolución $returnId")
                if (takenItem != null) {
                    repository.decreaseStock(takenItem.productId, branchId, takenItem.quantity, _currentUser.value?.username ?: "admin", "Cambio $returnId")
                }
                val type = if (difference >= 0) CashMovementType.IN else CashMovementType.OUT
                val movement = CashMovement(
                    id = "M_RET_${currentTimeMillis()}",
                    timestamp = currentTimeMillis(),
                    branchId = branchId,
                    terminalId = _selectedTerminal.value?.id,
                    type = type,
                    amount = abs(difference),
                    reason = "Devolución/Cambio $returnId",
                    userId = _currentUser.value?.username ?: "admin"
                )
                cashMovementRepository?.saveMovement(movement)
                closeReturnDialog()
                setWarningMessage("Devolución procesada.")
            } catch (e: Exception) {
                setErrorMessage("Error: ${e.message}")
            }
        }
    }

    suspend fun getProductInfo(productId: String): Product? {
        return repository.getProductById(productId)
    }

    fun refreshCatalog() {
        setWarningMessage("Iniciando sincronización total del catálogo...")
        viewModelScope.launch {
            try {
                // Forzar descarga completa
                repository.refreshProducts(isInitial = true)
                repository.refreshInventory(branchId, isInitial = true)
                setWarningMessage("Sincronización total completada con éxito.")
            } catch (e: Exception) {
                setErrorMessage("Error en sincronización: ${e.message}")
            }
        }
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun startLiveSearch() {
        viewModelScope.launch {
            _searchQuery
                .debounce(250)
                .map { it.text.trim() }
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.length < 2) {
                        if (query.isEmpty()) {
                            _searchResults.value = emptyList()
                            _showSearchResults.value = false
                        }
                        return@collectLatest
                    }

                    repository.searchProducts(query, limit = 50).collect { results ->
                        if (_searchQuery.value.text.isNotBlank()) {
                            _searchResults.value = results
                            // MODIFICACIÓN: Ya no abrimos automáticamente la lista de resultados.
                            // Obligamos a que el usuario presione ENTER para mostrar la lista o procesar código.
                        }
                    }
                }
        }
    }
}
