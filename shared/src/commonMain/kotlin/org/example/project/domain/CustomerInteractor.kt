package com.abtsplazita.posplazita.domain

import com.abtsplazita.posplazita.domain.repository.CustomerRepository
import com.abtsplazita.posplazita.domain.repository.SettingsRepository
import com.abtsplazita.posplazita.ui.history.PrinterManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class CustomerInteractor(
    private val customerRepository: CustomerRepository?,
    private val settingsRepository: SettingsRepository?,
    private val printerManager: PrinterManager?,
    private val cashManager: CashManager,
    private val scope: CoroutineScope
) {
    private val _selectedCustomer = MutableStateFlow<Customer?>(null)
    val selectedCustomer = _selectedCustomer.asStateFlow()

    private val _showCustomerDialog = MutableStateFlow(false)
    val showCustomerDialog = _showCustomerDialog.asStateFlow()

    private val _customerSearchQuery = MutableStateFlow("")
    val customerSearchQuery = _customerSearchQuery.asStateFlow()

    private val _selectedCustomerIndex = MutableStateFlow(0)
    val selectedCustomerIndex = _selectedCustomerIndex.asStateFlow()

    private val _showAddCustomerDialog = MutableStateFlow(false)
    val showAddCustomerDialog = _showAddCustomerDialog.asStateFlow()

    private val _editingCustomer = MutableStateFlow<Customer?>(null)
    val editingCustomer = _editingCustomer.asStateFlow()

    private val _showDebtPaymentDialog = MutableStateFlow(false)
    val showDebtPaymentDialog = _showDebtPaymentDialog.asStateFlow()

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
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

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
            selectCustomer(customers[_selectedCustomerIndex.value])
        }
    }

    fun selectCustomer(customer: Customer?) {
        _selectedCustomer.value = customer
        closeCustomerDialog()
        
        if (customer != null && customerRepository != null) {
            scope.launch {
                try {
                    customerRepository.refreshCustomer(customer.id)?.let { refreshed ->
                        if (_selectedCustomer.value?.id == refreshed.id) {
                            _selectedCustomer.value = refreshed
                        }
                    }
                } catch (e: Exception) {}
            }
        }
    }

    fun openAddCustomerDialog() {
        _showCustomerDialog.value = false
        _editingCustomer.value = Customer(id = "", name = "")
        _showAddCustomerDialog.value = true
    }

    fun closeAddCustomerDialog(shouldReopenSelection: Boolean = true) { 
        _showAddCustomerDialog.value = false 
        _editingCustomer.value = null
        if (shouldReopenSelection) {
            _showCustomerDialog.value = true
        }
    }

    fun updateEditingCustomer(customer: Customer) {
        _editingCustomer.value = customer
    }

    fun saveNewCustomer(onError: (String) -> Unit, onSuccess: (String) -> Unit) {
        val customer = _editingCustomer.value ?: return
        if (customer.name.isBlank()) return

        scope.launch {
            try {
                val toSave = customer.copy(id = "C${com.abtsplazita.posplazita.currentTimeMillis()}")
                customerRepository?.saveCustomer(toSave)
                _selectedCustomer.value = toSave
                _editingCustomer.value = null
                _showAddCustomerDialog.value = false
                onSuccess("Cliente '${toSave.name}' guardado y seleccionado.")
            } catch (e: Exception) {
                onError("Error al guardar cliente: ${e.message}")
            }
        }
    }

    fun openDebtPaymentDialog() { _showDebtPaymentDialog.value = true }
    fun closeDebtPaymentDialog() { _showDebtPaymentDialog.value = false }

    fun processDebtPayment(
        customer: Customer,
        amount: Double,
        branchId: String,
        selectedTerminal: PosTerminal?,
        currentUser: User?,
        cashInDrawer: Double,
        ticketConfig: TicketConfig,
        branchName: String,
        onError: (String) -> Unit,
        onSuccess: (String) -> Unit
    ) {
        if (amount <= 0) return
        scope.launch {
            try {
                cashManager.addCashMovement(
                    amount = amount,
                    reason = "Abono de deuda: ${customer.name}",
                    branchId = branchId,
                    selectedTerminal = selectedTerminal,
                    currentUser = currentUser,
                    cashInDrawer = cashInDrawer,
                    isManual = false,
                    onError = onError,
                    onSuccess = {}
                )

                customerRepository?.addPayment(CustomerPayment(
                    id = "PAY_${com.abtsplazita.posplazita.currentTimeMillis()}",
                    customerId = customer.id,
                    amount = amount,
                    timestamp = com.abtsplazita.posplazita.currentTimeMillis(),
                    userId = currentUser?.username ?: "admin"
                ))
                
                val updatedCustomer = customerRepository?.getCustomerById(customer.id)
                _selectedCustomer.value = updatedCustomer
                
                if (updatedCustomer != null && printerManager != null) {
                    printerManager.printDebtPayment(
                        customer = updatedCustomer,
                        amountPaid = amount,
                        remainingDebt = updatedCustomer.currentDebt,
                        config = ticketConfig,
                        branchName = branchName
                    )
                }

                closeDebtPaymentDialog()
                onSuccess("Abono de $${amount.formatPrice()} registrado e impreso.")
            } catch (e: Exception) {
                onError("Error al procesar abono: ${e.message}")
            }
        }
    }
}
