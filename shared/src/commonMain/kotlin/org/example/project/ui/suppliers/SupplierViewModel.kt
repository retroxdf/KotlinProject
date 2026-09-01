package com.abtsplazita.posplazita.ui.suppliers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.abtsplazita.posplazita.domain.Supplier
import com.abtsplazita.posplazita.domain.SupplierPayment
import com.abtsplazita.posplazita.domain.Purchase
import com.abtsplazita.posplazita.domain.PurchaseStatus
import com.abtsplazita.posplazita.domain.CashMovement
import com.abtsplazita.posplazita.domain.CashMovementType
import com.abtsplazita.posplazita.domain.repository.SupplierRepository
import com.abtsplazita.posplazita.domain.repository.PurchaseRepository
import com.abtsplazita.posplazita.domain.repository.CashMovementRepository
import com.abtsplazita.posplazita.domain.repository.SaleRepository
import com.abtsplazita.posplazita.domain.repository.CashOutRepository
import com.abtsplazita.posplazita.currentTimeMillis
import com.abtsplazita.posplazita.domain.formatPrice
import kotlin.math.abs

class SupplierViewModel(
    private val repository: SupplierRepository,
    private val purchaseRepository: PurchaseRepository? = null,
    private val cashMovementRepository: CashMovementRepository? = null,
    private val saleRepository: SaleRepository? = null,
    private val cashOutRepository: CashOutRepository? = null,
    private val branchId: String = ""
) : ViewModel() {

    private val _suppliers = repository.getAllSuppliers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val suppliers = _suppliers

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val filteredSuppliers = combine(_suppliers, _searchQuery) { list, query ->
        if (query.isBlank()) list
        else list.filter { it.name.contains(query, ignoreCase = true) || it.contactName?.contains(query, ignoreCase = true) == true }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedSupplier = MutableStateFlow<Supplier?>(null)
    val selectedSupplier = _selectedSupplier.asStateFlow()

    private val _isEditing = MutableStateFlow(false)
    val isEditing = _isEditing.asStateFlow()

    fun selectSupplier(supplier: Supplier?) {
        _selectedSupplier.value = supplier
        _isEditing.value = false
        if (supplier != null) {
            loadPayments(supplier.id)
            loadPurchases(supplier.id)
        }
    }

    private val _payments = MutableStateFlow<List<SupplierPayment>>(emptyList())
    val payments = _payments.asStateFlow()

    private val _supplierPurchases = MutableStateFlow<List<Purchase>>(emptyList())
    val supplierPurchases = _supplierPurchases.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _cashInBranch = MutableStateFlow(0.0)
    val cashInBranch = _cashInBranch.asStateFlow()

    private val _currentUser = MutableStateFlow<com.abtsplazita.posplazita.domain.User?>(null)

    fun setUserInfo(user: com.abtsplazita.posplazita.domain.User?) {
        _currentUser.value = user
    }

    init {
        // Observar balance total de efectivo en la sucursal
        if (saleRepository != null && cashMovementRepository != null && cashOutRepository != null) {
            viewModelScope.launch {
                combine(
                    saleRepository.getSales(branchId),
                    cashMovementRepository.getMovements(branchId),
                    cashOutRepository.getCashOuts(branchId)
                ) { allSales, allMovements, allCashOuts ->
                    // Calculamos el efectivo disponible sumando lo de todas las terminales
                    // desde sus respectivos últimos cortes.
                    val terminals = allSales.mapNotNull { it.terminalId }.distinct()
                    var totalAvailable = 0.0
                    
                    terminals.forEach { tId ->
                        val lastCashOut = allCashOuts.filter { it.terminalId == tId }.maxByOrNull { it.timestamp }
                        val startTime = lastCashOut?.timestamp ?: 0L
                        
                        val sCash = allSales.filter { it.terminalId == tId && it.timestamp > startTime }.sumOf { it.cashAmount }
                        val mIn = allMovements.filter { it.terminalId == tId && it.timestamp > startTime && it.type == CashMovementType.IN }.sumOf { it.amount }
                        val mOut = allMovements.filter { it.terminalId == tId && it.timestamp > startTime && it.type == CashMovementType.OUT }.sumOf { it.amount }
                        
                        totalAvailable += (sCash + mIn - mOut)
                    }
                    totalAvailable
                }.collect {
                    _cashInBranch.value = it
                }
            }
        }
    }

    fun loadPayments(supplierId: String) {
        viewModelScope.launch {
            repository.getPayments(supplierId).collect {
                _payments.value = it
            }
        }
    }

    fun loadPurchases(supplierId: String) {
        viewModelScope.launch {
            purchaseRepository?.getPurchasesBySupplier(supplierId)?.collect {
                _supplierPurchases.value = it
            }
        }
    }

    fun makePayment(amount: Double, method: String, notes: String, purchaseId: String? = null) {
        val supplier = _selectedSupplier.value ?: return
        
        // Validación de saldo si es en EFECTIVO
        if (method == "Efectivo" && amount > _cashInBranch.value) {
            _errorMessage.value = "No hay suficiente efectivo en caja ($${_cashInBranch.value.formatPrice()}) para realizar este pago."
            return
        }

        viewModelScope.launch {
            val now = currentTimeMillis()
            val payment = SupplierPayment(
                id = "SPAY_${now}",
                supplierId = supplier.id,
                amount = amount,
                timestamp = now,
                method = method,
                userId = _currentUser.value?.username ?: "admin",
                notes = notes
            )
            repository.addPayment(payment)

            // Si es en EFECTIVO, registrar salida de caja
            if (method == "Efectivo") {
                cashMovementRepository?.saveMovement(
                    CashMovement(
                        id = "M_SPAY_${now}",
                        timestamp = now,
                        branchId = branchId,
                        terminalId = null,
                        type = CashMovementType.OUT,
                        amount = amount,
                        reason = "Pago a proveedor: ${supplier.name}. $notes",
                        userId = _currentUser.value?.username ?: "admin"
                    )
                )
            }

            // Si se pagó un ticket específico, marcarlo como pagado
            if (purchaseId != null) {
                purchaseRepository?.getPurchasesBySupplier(supplier.id)?.first()?.find { it.id == purchaseId }?.let { purchase ->
                    purchaseRepository.updatePurchaseStatus(purchase, PurchaseStatus.PAID)
                }
            }

            // Recargar datos
            _selectedSupplier.value = repository.getSupplierById(supplier.id)
            loadPurchases(supplier.id)
            loadPayments(supplier.id)
        }
    }

    fun startEditing() {
        _isEditing.value = true
    }

    fun cancelEditing() {
        if (_selectedSupplier.value?.id?.startsWith("NEW") == true) {
            _selectedSupplier.value = null
        }
        _isEditing.value = false
    }

    fun prepareNewSupplier() {
        val newSup = Supplier(
            id = "NEW_${currentTimeMillis()}",
            name = "",
            contactName = "",
            phone = "",
            givesCredit = false,
            creditDays = 0
        )
        _selectedSupplier.value = newSup
        _isEditing.value = true
    }

    fun saveSupplier(supplier: Supplier) {
        viewModelScope.launch {
            val toSave = if (supplier.id.startsWith("NEW")) {
                supplier.copy(id = "S${currentTimeMillis()}")
            } else {
                supplier
            }
            repository.saveSupplier(toSave)
            _selectedSupplier.value = toSave
            _isEditing.value = false
        }
    }

    fun deleteSupplier(supplier: Supplier) {
        viewModelScope.launch {
            repository.deleteSupplier(supplier)
            _selectedSupplier.value = null
            _isEditing.value = false
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
