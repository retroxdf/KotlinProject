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
    private val scaleManager: ScaleManager = getScaleManager()
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
        return _userPermissions.value[permission] == PermissionLevel.ENABLED
    }

    private fun isRestricted(permission: Permission): Boolean {
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

    private val _saleChange = MutableStateFlow<Double?>(null)
    val saleChange = _saleChange.asStateFlow()

    private val _showSaleSuccessOverlay = MutableStateFlow(false)
    val showSaleSuccessOverlay = _showSaleSuccessOverlay.asStateFlow()

    private val _showCardSuccess = MutableStateFlow(false)
    val showCardSuccess = _showCardSuccess.asStateFlow()

    private val _isProcessingSale = MutableStateFlow(false)
    val isProcessingSale = _isProcessingSale.asStateFlow()

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

    // --- Estado de Mercado Pago ---
    private val _mpStatus = MutableStateFlow<String?>(null)
    val mpStatus = _mpStatus.asStateFlow()

    private val _isWaitingForMP = MutableStateFlow(false)
    val isWaitingForMP = _isWaitingForMP.asStateFlow()

    private var mpCancelRequested = false
    private var currentMpIdempotencyKey: String? = null

    fun cancelMpPayment() {
        mpCancelRequested = true
        _isWaitingForMP.value = false
        _isProcessingSale.value = false
        _isProcessingWithdrawal.value = false
        _mpStatus.value = null
    }

    // --- Gestión de Clientes ---
    private val _selectedCustomer = MutableStateFlow<Customer?>(null)
    val selectedCustomer = _selectedCustomer.asStateFlow()

    private val _showCustomerDialog = MutableStateFlow(false)
    val showCustomerDialog = _showCustomerDialog.asStateFlow()

    private val _customerSearchQuery = MutableStateFlow("")
    val customerSearchQuery = _customerSearchQuery.asStateFlow()

    private val _selectedCustomerIndex = MutableStateFlow(0)
    val selectedCustomerIndex = _selectedCustomerIndex.asStateFlow()

    val filteredCustomers: StateFlow<List<Customer>> = combine(
        customerRepository?.getAllCustomers() ?: flowOf(emptyList()),
        _customerSearchQuery
    ) { allCustomers, query ->
        if (query.isBlank()) allCustomers
        else {
            val normQuery = query.normalizeForSearch()
            allCustomers.filter { 
                it.name.normalizeForSearch().contains(normQuery) || 
                it.phone?.contains(query) == true 
            }.sortedBy { it.name.normalizeForSearch().indexOf(normQuery) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showAddCustomerDialog = MutableStateFlow(false)
    val showAddCustomerDialog = _showAddCustomerDialog.asStateFlow()

    private val _editingCustomer = MutableStateFlow<Customer?>(null)
    val editingCustomer = _editingCustomer.asStateFlow()

    fun openCustomerDialog() { _showCustomerDialog.value = true }
    fun closeCustomerDialog() { _showCustomerDialog.value = false }

    fun updateCustomerSearchQuery(query: String) {
        _customerSearchQuery.value = query
        _selectedCustomerIndex.value = 0
    }

    fun moveCustomerFocus(delta: Int) {
        val count = filteredCustomers.value.size
        if (count > 0) {
            _selectedCustomerIndex.value = (_selectedCustomerIndex.value + delta).coerceIn(0, count - 1)
        }
    }

    fun selectFocusedCustomer() {
        val customers = filteredCustomers.value
        if (_selectedCustomerIndex.value in customers.indices) {
            _selectedCustomer.value = customers[_selectedCustomerIndex.value]
            closeCustomerDialog()
        }
    }

    fun selectCustomer(customer: Customer?) {
        _selectedCustomer.value = customer
        closeCustomerDialog()
        
        // Bajo demanda: Actualizar saldo y monedero desde la nube
        if (customer != null) {
            viewModelScope.launch {
                try {
                    customerRepository?.refreshCustomer(customer.id)?.let { refreshed ->
                        if (_selectedCustomer.value?.id == refreshed.id) {
                            _selectedCustomer.value = refreshed
                        }
                    }
                } catch (e: Exception) {}
            }
        }
    }

    fun openAddCustomerDialog() { 
        if (hasPermission(Permission.CUSTOMER_CREATE)) {
            _showCustomerDialog.value = false // Cerrar búsqueda para evitar encimar diálogos
            _editingCustomer.value = Customer(id = "", name = "")
            _showAddCustomerDialog.value = true 
        } else if (isRestricted(Permission.CUSTOMER_CREATE)) {
            // No cerramos búsqueda aquí aún, lo hacemos al autorizar
            requestAuthorization("Crear Cliente") { 
                _showCustomerDialog.value = false
                _editingCustomer.value = Customer(id = "", name = "")
                _showAddCustomerDialog.value = true 
            }
        } else {
            setErrorMessage("No tienes permiso para crear clientes.")
        }
    }

    fun updateEditingCustomer(customer: Customer) {
        _editingCustomer.value = customer
    }

    fun saveNewCustomer() {
        val customer = _editingCustomer.value ?: return
        if (customer.name.isBlank()) return

        viewModelScope.launch {
            try {
                val toSave = customer.copy(id = "C${currentTimeMillis()}")
                customerRepository?.saveCustomer(toSave)
                _selectedCustomer.value = toSave
                _editingCustomer.value = null
                _showAddCustomerDialog.value = false
                setWarningMessage("Cliente '${toSave.name}' guardado y seleccionado.")
            } catch (e: Exception) {
                setErrorMessage("Error al guardar cliente: ${e.message}")
            }
        }
    }

    fun closeAddCustomerDialog(shouldReopenSelection: Boolean = true) { 
        _showAddCustomerDialog.value = false 
        _editingCustomer.value = null
        if (shouldReopenSelection) {
            _showCustomerDialog.value = true
        }
    }

    fun addCustomer(name: String, phone: String, days: Int, limit: Double, weekly: Double) {
        viewModelScope.launch {
            try {
                val newCustomer = Customer(
                    id = "C${currentTimeMillis()}",
                    name = name,
                    phone = phone,
                    creditDays = days,
                    creditLimit = limit,
                    creditLimitWeekly = weekly
                )
                customerRepository?.saveCustomer(newCustomer)
                _selectedCustomer.value = newCustomer
                closeAddCustomerDialog(shouldReopenSelection = false)
            } catch (e: Exception) {
                setErrorMessage("Error al guardar cliente: ${e.message}")
            }
        }
    }

    // --- Pagos de Deuda ---
    private val _showDebtPaymentDialog = MutableStateFlow(false)
    val showDebtPaymentDialog = _showDebtPaymentDialog.asStateFlow()

    fun openDebtPaymentDialog() {
        _showDebtPaymentDialog.value = true
    }

    fun closeDebtPaymentDialog() { _showDebtPaymentDialog.value = false }

    fun processDebtPayment(customer: Customer, amount: Double) {
        if (amount <= 0) return
        viewModelScope.launch {
            try {
                addCashMovement(amount, "Abono de deuda: ${customer.name}", isManual = false)
                customerRepository?.addPayment(CustomerPayment(
                    id = "PAY_${currentTimeMillis()}",
                    customerId = customer.id,
                    amount = amount,
                    timestamp = currentTimeMillis(),
                    userId = _currentUser.value?.username ?: "admin"
                ))
                
                val updatedCustomer = customerRepository?.getCustomerById(customer.id)
                _selectedCustomer.value = updatedCustomer
                
                // Imprimir comprobante de abono
                if (updatedCustomer != null) {
                    printerManager?.printDebtPayment(
                        customer = updatedCustomer,
                        amountPaid = amount,
                        remainingDebt = updatedCustomer.currentDebt,
                        config = _ticketConfig.value,
                        branchName = _branchName.value
                    )
                }

                closeDebtPaymentDialog()
                setWarningMessage("Abono de $${amount.formatPrice()} registrado e impreso.")
            } catch (e: Exception) {
                setErrorMessage("Error al procesar abono: ${e.message}")
            }
        }
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
                customerId = _selectedCustomer.value?.id,
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
                    customerId = _selectedCustomer.value?.id,
                    total = total.value
                )
                saleRepository.saveHeldSale(currentHeld)
            }
            
            // 1. Recuperar los artículos reales de la base de datos
            val realItems = saleRepository.getHeldSaleItems(heldSale.id)

            // 2. Cargar la nueva venta
            currentSaleManager.loadItems(realItems)
            _selectedCustomer.value = if (!heldSale.customerId.isNullOrBlank()) {
                customerRepository?.getCustomerById(heldSale.customerId)
            } else null
            
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
        } else if (isRestricted(Permission.CANCEL_SALE)) {
            // En lugar de pedir PIN, enviamos solicitud
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
        } else {
            setErrorMessage("No tienes permiso para eliminar ventas guardadas.")
        }
    }

    // --- Checkout y Métodos de Venta ---
    fun updateAmountPaid(text: TextFieldValue) {
        if (_showSaleSuccessOverlay.value) return // Evitar recálculos si la venta ya finalizó
        
        if (text.text.isEmpty() || text.text.all { it.isDigit() || it == '.' }) {
            _amountPaidText.value = text
            val amount = text.text.toDoubleOrNull() ?: 0.0
            val currentTotal = currentSaleManager.total.value
            _saleChange.value = if (amount > currentTotal) amount - currentTotal else 0.0
        }
    }

    fun prepareCheckout() {
        val totalAmount = currentSaleManager.total.value
        _amountPaidText.value = TextFieldValue(totalAmount.formatPrice(), TextRange(0, totalAmount.formatPrice().length))
        _saleChange.value = 0.0 // Inicializamos en 0 ya que el texto predeterminado es el total
        _paymentMethod.value = "Efectivo"
        _saleComment.value = ""
        _showSaleSuccessOverlay.value = false
    }

    fun cancelCheckout() {
        _saleChange.value = null
        _showSaleSuccessOverlay.value = false
        _paymentMethod.value = "Efectivo"
    }

    fun setPaymentMethod(method: String) {
        _paymentMethod.value = method
        if (method == "Efectivo") {
            val amount = _amountPaidText.value.text.toDoubleOrNull() ?: 0.0
            _saleChange.value = amount - currentSaleManager.total.value
        } else {
            _saleChange.value = null // Ocultar cambio si no es efectivo
        }
        
        if (method == "Tarjeta") {
            _mpStatus.value = "Listo para cobrar"
        } else {
            _mpStatus.value = null
        }

        // Si es crédito y no hay cliente, abrir buscador de clientes
        if (method == "Crédito" && _selectedCustomer.value == null) {
            openCustomerDialog()
        }
    }

    fun setSaleComment(comment: String) { _saleComment.value = comment }

    fun completeSale(shouldPrint: Boolean, onDone: () -> Unit) {
        if (currentItems.value.isEmpty()) return
        if (_isProcessingSale.value) return

        viewModelScope.launch {
            val paymentMethod = _paymentMethod.value
            val isCredit = paymentMethod == "Crédito"
            
            // 1. Validar permiso de crédito si aplica
            if (isCredit) {
                if (hasPermission(Permission.SELL_ON_CREDIT)) {
                    // OK
                } else if (isRestricted(Permission.SELL_ON_CREDIT)) {
                    requestAuthorization("Venta a Crédito") {
                        // Re-ejecutar venta tras autorización
                        executeCompleteSale(shouldPrint, onDone)
                    }
                    return@launch
                } else {
                    setErrorMessage("No tienes permiso para vender a crédito.")
                    return@launch
                }
            }

            // Si es venta normal o ya se validó el crédito
            executeCompleteSale(shouldPrint, onDone)
        }
    }

    private fun executeCompleteSale(shouldPrint: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            // 1. Validar que haya una caja seleccionada
            if (_selectedTerminal.value == null) {
                setErrorMessage("Debes seleccionar una caja (terminal) antes de cobrar.")
                playErrorSound()
                return@launch
            }

            val currentTotal = currentSaleManager.total.value
            
            // Capturar el monto pagado del texto de forma segura y robusta
            val rawPaidText = _amountPaidText.value.text.trim()
            val amountPaid = if (rawPaidText.isEmpty() || rawPaidText == currentTotal.formatPrice().replace(",", "")) {
                currentTotal
            } else {
                rawPaidText.toDoubleOrNull() ?: currentTotal
            }

            val paymentMethod = _paymentMethod.value
            val isCredit = paymentMethod == "Crédito"
            val isMP = paymentMethod == "Tarjeta"
            val customerId = _selectedCustomer.value?.id

            // --- LÓGICA DE MERCADO PAGO ---
            if (isMP) {
                if (mercadoPagoManager == null) {
                    setErrorMessage("Servicio de Mercado Pago no inicializado.")
                    return@launch
                }

                val settings = settingsRepository?.getAllSettings()?.first() ?: emptyMap()
                val deviceId = settings["mp_terminal_id"] ?: ""
                val accessToken = settings["mp_access_token"] ?: ""
                
                if (deviceId.isBlank() || accessToken.isBlank()) {
                    setErrorMessage("Mercado Pago no configurado (Token o Terminal falta).")
                    return@launch
                }
                
                mercadoPagoManager.setCredentials(accessToken, "", "")

                _isWaitingForMP.value = true
                _mpStatus.value = "Conectando con terminal $deviceId..."
                
                val idempotencyKey = currentMpIdempotencyKey ?: "POS_${currentTimeMillis()}"
                currentMpIdempotencyKey = idempotencyKey

                val (success, intentId, externalRef) = mercadoPagoManager.sendPaymentToPoint(deviceId, currentTotal, "Venta POS", idempotencyKey)
                
                if (success) {
                    _mpStatus.value = "Esperando aprobación en terminal..."
                    var paid = false
                    mpCancelRequested = false
                    
                    // Polling dual-channel por 120 segundos (más agresivo)
                    val startTime = currentTimeMillis()
                    var iteration = 0
                    while (currentTimeMillis() - startTime < 120_000 && !mpCancelRequested) {
                        iteration++
                        
                        // 1. Verificar estado del intento
                        val result = mercadoPagoManager.checkPaymentStatus(intentId)
                        if (result == "SUCCESS") { paid = true; break }
                        if (result == "REJECTED" || result == "CANCELED" || result == "ERROR") {
                            // Verificación extra antes de rechazar definitivamente
                            val finalSearch = mercadoPagoManager.searchPaymentByReference(externalRef)
                            if (finalSearch == "SUCCESS") { paid = true; break }
                            break 
                        }
                        
                        // 2. Fallback: Buscar pago por referencia
                        if (iteration % 2 == 0 || (currentTimeMillis() - startTime > 100_000)) {
                            val searchResult = mercadoPagoManager.searchPaymentByReference(externalRef)
                            if (searchResult == "SUCCESS") { paid = true; break }
                        }

                        kotlinx.coroutines.delay(800)
                    }
                    
                    if (mpCancelRequested) {
                        // Verificación final inmediata en ambos canales
                        val lastCheck = mercadoPagoManager.checkPaymentStatus(intentId)
                        val lastSearch = mercadoPagoManager.searchPaymentByReference(externalRef)
                        
                        if (lastCheck == "SUCCESS" || lastSearch == "SUCCESS") {
                            paid = true
                        } else {
                            _isWaitingForMP.value = false
                            _isProcessingSale.value = false
                            return@launch
                        }
                    }

                    _isWaitingForMP.value = false
                    if (!paid) {
                        val status = _mpStatus.value ?: "desconocido"
                        setErrorMessage("Pago no completado: $status")
                        _isProcessingSale.value = false
                        return@launch
                    }
                } else {
                    _isWaitingForMP.value = false
                    setErrorMessage("Error al conectar con Point: $intentId")
                    return@launch
                }
            }

            _isProcessingSale.value = true
            try {
                if (paymentMethod == "Efectivo" && amountPaid < (currentTotal - 0.01)) {
                    setErrorMessage("El monto pagado ($${amountPaid.formatPrice()}) es insuficiente.")
                    _isProcessingSale.value = false
                    return@launch
                }

                if (paymentMethod == "Monedero") {
                    val balance = _selectedCustomer.value?.walletBalance ?: 0.0
                    if (balance < currentTotal) {
                        setErrorMessage("Saldo insuficiente en monedero ($${balance.formatPrice()}).")
                        _isProcessingSale.value = false
                        return@launch
                    }
                }

                // 2. Validar cliente para crédito
                if (isCredit && customerId == null) {
                    setErrorMessage("Debes seleccionar un cliente para realizar una venta a crédito.")
                    _isProcessingSale.value = false
                    return@launch
                }

                val now = currentTimeMillis()
                val saleId = saleRepository.generateUniqueSaleId(branchId, _selectedTerminal.value?.id, prefix = _ticketConfig.value.ticketIdPrefix)
                
                // Cálculo estático y blindado del cambio final
                val finalDiff = amountPaid - currentTotal
                val calculatedChange = if (paymentMethod == "Efectivo" && finalDiff > 0.005) finalDiff else 0.0

                val sale = Sale(
                    id = saleId,
                    timestamp = now,
                    userId = _currentUser.value?.username ?: "admin",
                    branchId = branchId,
                    terminalId = _selectedTerminal.value?.id,
                    customerId = customerId,
                    items = currentItems.value,
                    total = currentTotal,
                    netTotal = currentItems.value.filter { !it.isService }.sumOf { it.subtotal },
                    cashAmount = if (paymentMethod == "Efectivo") currentTotal else 0.0,
                    creditAmount = if (isCredit) currentTotal else 0.0,
                    receivedAmount = if (paymentMethod == "Efectivo") amountPaid else currentTotal,
                    changeAmount = calculatedChange,
                    paymentMethod = paymentMethod,
                    comment = _saleComment.value,
                    originalWebOrderId = currentSaleManager.currentWebOrderId.value
                )

                saleRepository.saveSale(sale)
                
                // IMPORTANTE: Primero establecemos el cambio y mostramos el overlay
                _saleChange.value = calculatedChange
                _showSaleSuccessOverlay.value = true
                
                // Limpiar la venta (Esto ya no sobreescribirá _saleChange porque lo bloqueamos arriba)
                clearSale(isManual = false)
                
                successJob?.cancel()
                successJob = viewModelScope.launch {
                    delay(120000) // Esperar 2 minutos
                    _showSaleSuccessOverlay.value = false
                    _saleChange.value = null
                    _showCardSuccess.value = false
                }

                // 3. Lógica de Monedero Electrónico (Acumulación y Pago)
                if (customerId != null) {
                    if (paymentMethod == "Monedero") {
                        // Restar del monedero lo pagado
                        customerRepository?.updateWalletBalance(customerId, -currentTotal)
                    }

                    val settings = settingsRepository?.getAllSettings()?.first() ?: emptyMap()
                    var walletBonus = 0.0
                    sale.items.forEach { item ->
                        // Si se aplicó una promoción o descuento web, no genera monedero
                        if (!item.isPromoApplied && !item.isWebDiscounted) {
                            val percent = settings["wallet_percent_${item.category}"]?.toDoubleOrNull() ?: 0.0
                            if (percent > 0) {
                                walletBonus += item.subtotal * (percent / 100.0)
                            }
                        }
                    }
                    if (walletBonus > 0) {
                        customerRepository?.updateWalletBalance(customerId, walletBonus)
                    }
                }

                // 4. Actualizar deuda del cliente si es crédito
                if (isCredit && customerId != null) {
                    customerRepository?.updateDebt(customerId, currentTotal)
                }

                // Descontar stock de todos los productos (incluyendo pedidos web para mantener sincronía local)
                sale.items.forEach { item ->
                    if (!item.isService) {
                        repository.decreaseStock(item.productId, branchId, item.quantity, _currentUser.value?.username ?: "admin", "Venta $saleId")
                    }
                }

                // NUEVO: Generar Reporte de Cambios/Devoluciones si hay items negativos
                val returns = sale.items.filter { it.quantity < 0 }
                if (returns.isNotEmpty()) {
                    returns.forEach { ret ->
                        val productReturn = ProductReturn(
                            id = "RET_${currentTimeMillis()}_${ret.productId}",
                            timestamp = now,
                            branchId = branchId,
                            returnedItem = ret,
                            userId = _currentUser.value?.username ?: "admin",
                            difference = ret.subtotal // Esto es el saldo que aportó a favor
                        )
                        productReturnRepository?.saveReturn(productReturn)
                    }
                }

                // 5. Si viene de un pedido web, marcarlo como entregado
                currentSaleManager.currentWebOrderId.value?.let { webId ->
                    val order = _webOrders.value.find { it.id == webId }
                    if (order != null) {
                        updateWebOrderStatus(order, WebOrderStatus.DELIVERED)
                    }
                }

                _lastSale.value = sale
                _lastSaleItems.value = sale.items
                
                if (shouldPrint) {
                    val finalCustomer = if (customerId != null) customerRepository?.getCustomerById(customerId) else null
                    // Abrir cajón solo si es pago en efectivo
                    val openDrawer = _paymentMethod.value == "Efectivo"
                    
                    printerManager?.printTicket(
                        sale, 
                        sale.items, 
                        openDrawer = openDrawer,
                        walletBalance = finalCustomer?.walletBalance,
                        config = _ticketConfig.value,
                        branchName = _branchName.value
                    )
                } else if (_paymentMethod.value == "Efectivo") {
                    // Si no imprime pero es efectivo, abrir cajón (caso F12)
                    printerManager?.openDrawer()
                }

                if (paymentMethod != "Efectivo") {
                    _showCardSuccess.value = true
                }

                onDone()
            } catch (e: Exception) {
                setErrorMessage("Error al procesar venta: ${e.message}")
            } finally {
                _isProcessingSale.value = false
            }
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
            // 1. Procesar aprobaciones (Cualquier dispositivo que vea una aprobación de su sucursal intenta borrar localmente)
            val approved = requests.filter { it.status == "APPROVED" }
            approved.forEach { req ->
                viewModelScope.launch {
                    // Borrar de la base de datos local si existe
                    saleRepository.deleteHeldSale(req.ticketId)
                    // Borrar de la nube (limpieza)
                    firebaseManager?.deleteHeldSale(req.ticketId)
                    // Finalmente borrar la solicitud
                    firebaseManager?.deleteDeletionRequest(req.id)
                }
            }

            // 2. Mostrar pendientes para el admin
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
                
                setWarningMessage("Borrado Exitoso: Ticket aprobado para eliminación.")
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
        if (text.isNotEmpty() && _showSaleSuccessOverlay.value) {
            _showSaleSuccessOverlay.value = false
            _saleChange.value = null
            successJob?.cancel()
        }
        
        // Soporte para atajo instantáneo (ej: 15+)
        if (text.endsWith("+") && text.length > 1) {
            val part = text.removeSuffix("+")
            if (part.all { it.isDigit() || it == '.' }) {
                val value = part.toDoubleOrNull() ?: 0.0
                val items = currentItems.value
                
                // Si hay items en el carrito, el + actúa como actualizador de cantidad
                if (items.isNotEmpty()) {
                    var index = _selectedCartIndex.value
                    if (index !in items.indices) index = items.size - 1
                    val item = items[index]
                    
                    if (item.isBulk || (value % 1.0 == 0.0)) {
                        currentSaleManager.updateItemQuantity(item, value)
                        _searchQuery.value = TextFieldValue("")
                        return
                    }
                }
                
                // Si no hay items o es para un producto nuevo, abrir diálogo de producto común
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
        _showSaleSuccessOverlay.value = false
        _saleChange.value = null
        _showCardSuccess.value = false
        successJob?.cancel()

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
                    _selectedCustomer.value = customer
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
            val exactMatch = repository.getProductByBarcode(query)
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
        _showSaleSuccessOverlay.value = false
        _saleChange.value = null
        successJob?.cancel()
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
        if (_showSaleSuccessOverlay.value) {
            _showSaleSuccessOverlay.value = false
            _saleChange.value = null
            _showCardSuccess.value = false
            successJob?.cancel()
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
            _selectedCustomer.value = null
            _saleComment.value = ""
            currentMpIdempotencyKey = null
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
        _saleChange.value = null 
        _showCardSuccess.value = false
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
        currentSaleManager.setAllowNegativeStock(enabled)
    }

    fun setAddAtTop(enabled: Boolean) {
        currentSaleManager.setAddAtTop(enabled)
    }

    // --- Caja y Movimientos ---
    private val _showCashMovementDialog = MutableStateFlow<CashMovementType?>(null)
    val showCashMovementDialog = _showCashMovementDialog.asStateFlow()

    fun openCashMovementDialog(type: CashMovementType) { _showCashMovementDialog.value = type }
    fun closeCashMovementDialog() { _showCashMovementDialog.value = null }

    fun addCashMovement(amount: Double, reason: String, isManual: Boolean = true) {
        val type = _showCashMovementDialog.value ?: (if (amount >= 0) CashMovementType.IN else CashMovementType.OUT)
        val absAmount = abs(amount)
        if (isManual && type == CashMovementType.OUT && absAmount > (_cashInDrawer.value + 0.01)) {
            setErrorMessage("Fondo insuficiente ($${_cashInDrawer.value.formatPrice()})")
            playErrorSound()
            return
        }
        viewModelScope.launch {
            val movId = "M-${branchId}-${_selectedTerminal.value?.id ?: "0"}-${currentTimeMillis()}"
            val movement = CashMovement(
                id = movId,
                timestamp = currentTimeMillis(),
                branchId = branchId,
                terminalId = _selectedTerminal.value?.id,
                type = type,
                amount = absAmount,
                reason = reason,
                userId = _currentUser.value?.username ?: "admin"
            )
            cashMovementRepository?.saveMovement(movement)
            
            // Abrir cajón para cualquier movimiento de efectivo (Entrada o Salida)
            openCashDrawer()

            if (isManual) {
                setWarningMessage("${if(type == CashMovementType.IN) "Entrada" else "Salida"} registrada.")
                closeCashMovementDialog()
            }
        }
    }

    // --- Devoluciones / Cambios ---
    private val _showReturnDialog = MutableStateFlow(false)
    val showReturnDialog = _showReturnDialog.asStateFlow()

    // --- Retiros de Efectivo (Tarjeta) ---
    private val _showWithdrawalDialog = MutableStateFlow(false)
    val showWithdrawalDialog = _showWithdrawalDialog.asStateFlow()

    private val _isProcessingWithdrawal = MutableStateFlow(false)
    val isProcessingWithdrawal = _isProcessingWithdrawal.asStateFlow()

    fun openWithdrawalDialog() { _showWithdrawalDialog.value = true }
    fun closeWithdrawalDialog() { 
        _showWithdrawalDialog.value = false 
        currentMpIdempotencyKey = null
    }

    fun processWithdrawal(withdrawalAmount: Double, commission: Double, commissionInCash: Boolean) {
        if (_isProcessingWithdrawal.value) return
        
        viewModelScope.launch {
            if (_selectedTerminal.value == null) {
                setErrorMessage("Debes seleccionar una caja (terminal) para registrar el retiro.")
                return@launch
            }

            if (mercadoPagoManager == null) {
                setErrorMessage("Mercado Pago no está configurado.")
                return@launch
            }

            if (withdrawalAmount > _cashInDrawer.value) {
                setErrorMessage("No hay suficiente efectivo en caja para este retiro.")
                return@launch
            }

            _isProcessingWithdrawal.value = true
            try {
                val settings = settingsRepository?.getAllSettings()?.first() ?: emptyMap()
                val deviceId = settings["mp_terminal_id"] ?: ""
                
                if (deviceId.isBlank()) {
                    setErrorMessage("Terminal Point no configurada.")
                    _isProcessingWithdrawal.value = false
                    return@launch
                }

                // Cantidad a cobrar en la tarjeta
                val chargeAmount = if (commissionInCash) withdrawalAmount else (withdrawalAmount + commission)
                
                _mpStatus.value = "Iniciando cobro en tarjeta de $${chargeAmount.formatPrice()}..."
                
                val idempotencyKey = currentMpIdempotencyKey ?: "WDR_${currentTimeMillis()}"
                currentMpIdempotencyKey = idempotencyKey

                val (success, intentId, externalRef) = mercadoPagoManager.sendPaymentToPoint(deviceId, chargeAmount, "Retiro de Efectivo", idempotencyKey)
                
                if (success) {
                    _mpStatus.value = "Esperando aprobación en terminal..."
                    var approved = false
                    mpCancelRequested = false
                    
                    val startTime = currentTimeMillis()
                    var iteration = 0
                    while (currentTimeMillis() - startTime < 120_000 && !mpCancelRequested) {
                        iteration++
                        
                        val result = mercadoPagoManager.checkPaymentStatus(intentId)
                        if (result == "SUCCESS") { approved = true; break }
                        if (result == "REJECTED" || result == "CANCELED" || result == "ERROR") {
                            val finalSearch = mercadoPagoManager.searchPaymentByReference(externalRef)
                            if (finalSearch == "SUCCESS") { approved = true; break }
                            break 
                        }
                        
                        if (iteration % 2 == 0) {
                            val searchResult = mercadoPagoManager.searchPaymentByReference(externalRef)
                            if (searchResult == "SUCCESS") { approved = true; break }
                            if (searchResult == "REJECTED") {
                                val lastCheck = mercadoPagoManager.checkPaymentStatus(intentId)
                                if (lastCheck == "SUCCESS") { approved = true; break }
                                break
                            }
                        }

                        kotlinx.coroutines.delay(600)
                    }

                    if (mpCancelRequested) {
                        // Verificación de seguridad inmediata en ambos canales
                        val lastCheck = mercadoPagoManager.checkPaymentStatus(intentId)
                        val lastSearch = mercadoPagoManager.searchPaymentByReference(externalRef)
                        
                        if (lastCheck == "SUCCESS" || lastSearch == "SUCCESS") {
                            approved = true
                        } else {
                            _isWaitingForMP.value = false
                            return@launch
                        }
                    }

                    if (approved) {
                        // 1. SALIDA de caja por el monto que se le da al cliente (Monto negativo para OUT)
                        addCashMovement(-withdrawalAmount, "Retiro de efectivo (Tarjeta)", isManual = false)
                        
                        // 2. Si la comisión se pagó en EFECTIVO, registrar ENTRADA (Monto positivo para IN)
                        if (commissionInCash) {
                            addCashMovement(commission, "Comisión por retiro (Efectivo)", isManual = false)
                        }

                        setWarningMessage("Retiro procesado correctamente.\nEntrega $${withdrawalAmount.formatPrice()} al cliente.")
                        closeWithdrawalDialog()
                    } else {
                        val status = _mpStatus.value ?: "desconocido"
                        setErrorMessage("Cobro en tarjeta no completado: $status")
                    }
                } else {
                    setErrorMessage("Error al conectar con Point: $intentId")
                }
            } catch (e: Exception) {
                setErrorMessage("Error en retiro: ${e.message}")
            } finally {
                _isProcessingWithdrawal.value = false
                _mpStatus.value = null
            }
        }
    }

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
        viewModelScope.launch {
            try {
                repository.refreshProducts(isInitial = false)
                repository.refreshInventory(branchId, isInitial = false)
                setWarningMessage("Sincronización incremental completada.")
            } catch (e: Exception) {
                setErrorMessage("Error al sincronizar: ${e.message}")
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
                            if (results.isNotEmpty() && !_showSearchResults.value && _currentFocusArea.value != FocusArea.CART) {
                                _showSearchResults.value = true
                                _currentFocusArea.value = FocusArea.SEARCH_RESULTS
                                _selectedSearchIndex.value = 0
                            }
                        }
                    }
                }
        }
    }
}
