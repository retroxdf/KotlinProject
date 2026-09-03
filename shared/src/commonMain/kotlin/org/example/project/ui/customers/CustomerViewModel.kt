package com.abtsplazita.posplazita.ui.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.abtsplazita.posplazita.domain.*
import com.abtsplazita.posplazita.domain.repository.CustomerRepository
import com.abtsplazita.posplazita.domain.repository.ProductRepository
import com.abtsplazita.posplazita.domain.repository.SaleRepository
import com.abtsplazita.posplazita.domain.repository.CashMovementRepository
import com.abtsplazita.posplazita.currentTimeMillis

import com.abtsplazita.posplazita.shareText
import kotlinx.datetime.*

class CustomerViewModel(
    private val repository: CustomerRepository,
    private val productRepository: ProductRepository,
    private val saleRepository: SaleRepository? = null,
    private val cashMovementRepository: CashMovementRepository? = null,
    private val printerManager: com.abtsplazita.posplazita.ui.history.PrinterManager? = null,
    private val branchId: String = ""
) : ViewModel() {

    private val _selectedTerminalId = MutableStateFlow<String?>(null)
    fun setTerminalId(id: String?) { _selectedTerminalId.value = id }

    val customers = repository.getAllCustomers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCustomer = MutableStateFlow<Customer?>(null)
    val selectedCustomer = _selectedCustomer.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)

    fun setUserInfo(user: User?) {
        _currentUser.value = user
    }

    private val _editingCustomer = MutableStateFlow<Customer?>(null)
    val editingCustomer = _editingCustomer.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _customerPayments = _selectedCustomer.flatMapLatest { customer ->
        if (customer != null) repository.getPayments(customer.id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val customerPayments = _customerPayments

    private val _customerSales = _selectedCustomer.flatMapLatest { customer ->
        if (customer != null && saleRepository != null) saleRepository.getSalesByCustomer(customer.id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val customerSales = _customerSales

    private val _viewingSaleItems = MutableStateFlow<List<SaleItem>>(emptyList())
    val viewingSaleItems = _viewingSaleItems.asStateFlow()

    private val _specialPrices = _selectedCustomer.flatMapLatest { customer ->
        if (customer != null) repository.getSpecialPrices(customer.id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val specialPrices = _specialPrices

    fun selectCustomer(customer: Customer?) {
        _selectedCustomer.value = customer
        if (customer != null && saleRepository != null) {
            viewModelScope.launch {
                repository.recalculateDebt(customer.id, saleRepository)
            }
        }
    }

    fun loadSaleItems(saleId: String) {
        viewModelScope.launch {
            if (saleRepository != null) {
                _viewingSaleItems.value = saleRepository.getSaleItems(saleId)
            }
        }
    }

    fun startNewCustomer() {
        _editingCustomer.value = Customer(id = "", name = "")
    }

    fun editCustomer(customer: Customer) {
        _editingCustomer.value = customer
    }

    fun cancelEdit() {
        _editingCustomer.value = null
    }

    fun updateEditingCustomer(customer: Customer) {
        _editingCustomer.value = customer
    }

    fun saveCustomer() {
        val customer = _editingCustomer.value ?: return
        if (customer.name.isBlank()) return

        viewModelScope.launch {
            // Validar teléfono duplicado
            if (!customer.phone.isNullOrBlank()) {
                val exists = customers.value.any { it.phone == customer.phone && it.id != customer.id }
                if (exists) {
                    _errorMessage.value = "Ya existe un cliente con el teléfono: ${customer.phone}"
                    return@launch
                }
            }

            val toSave = if (customer.id.isEmpty()) {
                customer.copy(id = "CUST_${currentTimeMillis()}")
            } else customer
            repository.saveCustomer(toSave)
            _editingCustomer.value = null
            _errorMessage.value = null
            if (_selectedCustomer.value?.id == toSave.id) {
                _selectedCustomer.value = toSave
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun addPayment(amount: Double, method: String, notes: String?) {
        val customer = _selectedCustomer.value ?: return
        if (amount <= 0) return

        viewModelScope.launch {
            try {
                val now = currentTimeMillis()
                val payment = CustomerPayment(
                    id = "PAY_${now}",
                    customerId = customer.id,
                    amount = amount,
                    timestamp = now,
                    userId = _currentUser.value?.username ?: "admin",
                    paymentMethod = method,
                    notes = notes
                )
                repository.addPayment(payment)

                // Si el pago es en EFECTIVO, registrar movimiento en caja y abrir cajón
                if (method == "Efectivo") {
                    printerManager?.openDrawer()
                    
                    if (_selectedTerminalId.value == null) {
                        _errorMessage.value = "⚠️ Pago registrado, pero NO se sumó a caja porque no hay una caja seleccionada."
                    } else {
                        cashMovementRepository?.saveMovement(
                            CashMovement(
                                id = "M_PAY_${now}",
                                timestamp = now,
                                branchId = branchId,
                                terminalId = _selectedTerminalId.value,
                                type = CashMovementType.IN,
                                amount = amount,
                                reason = "Abono Cliente: ${customer.name}",
                                userId = _currentUser.value?.username ?: "admin"
                            )
                        )
                    }
                }

                // Recargar cliente para ver deuda actualizada
                val updated = repository.getAllCustomers().first().find { it.id == customer.id }
                _selectedCustomer.value = updated
            } catch (e: Exception) {
                _errorMessage.value = "Error al registrar abono: ${e.message}"
            }
        }
    }

    fun addSpecialPrice(productId: String, price: Double) {
        val customer = _selectedCustomer.value ?: return
        viewModelScope.launch {
            val specialPrice = CustomerProductPrice(customer.id, productId, price)
            repository.saveSpecialPrice(specialPrice)
        }
    }

    fun removeSpecialPrice(specialPrice: CustomerProductPrice) {
        viewModelScope.launch {
            repository.deleteSpecialPrice(specialPrice)
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
            if (_selectedCustomer.value?.id == customer.id) {
                _selectedCustomer.value = null
            }
        }
    }

    private val _showCardPreview = MutableStateFlow<Customer?>(null)
    val showCardPreview = _showCardPreview.asStateFlow()

    fun openCardPreview(customer: Customer) {
        _showCardPreview.value = customer
    }

    fun closeCardPreview() {
        _showCardPreview.value = null
    }

    fun printMemberCard(customer: Customer) {
        printerManager?.printMemberCard(customer)
    }

    fun printMemberCardGraphic(customer: Customer) {
        printerManager?.printMemberCardGraphic(customer)
    }

    fun shareDebtReport(customer: Customer) {
        viewModelScope.launch {
            if (saleRepository == null) return@launch
            
            val payments = _customerPayments.value
            val creditSales = _customerSales.value.filter { it.creditAmount > 0 }
            
            // Recopilar detalles de cada ticket (desglosado)
            val salesWithItems = creditSales.map { sale ->
                sale to saleRepository.getSaleItems(sale.id)
            }
            
            val reportData = DebtReportData(
                customer = customer,
                sales = salesWithItems,
                payments = payments,
                totalDebt = customer.currentDebt
            )

            // Llamar a la función de plataforma para generar y compartir PDF
            com.abtsplazita.posplazita.generateAndShareDebtPdf(reportData, customer.phone ?: "")
        }
    }
}

data class DebtReportData(
    val customer: Customer,
    val sales: List<Pair<Sale, List<SaleItem>>>,
    val payments: List<CustomerPayment>,
    val totalDebt: Double
)
private fun Long.toDateTimeString(): String {
    val dt = kotlinx.datetime.Instant.fromEpochMilliseconds(this).toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
    return "${dt.dayOfMonth}/${dt.monthNumber}/${dt.year} ${dt.time.toString().take(5)}"
}

private fun Long.toDateString(): String {
    val dt = kotlinx.datetime.Instant.fromEpochMilliseconds(this).toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
    return "${dt.dayOfMonth}/${dt.monthNumber}/${dt.year}"
}
