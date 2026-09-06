package com.abtsplazita.posplazita.domain

import com.abtsplazita.posplazita.data.remote.FirebaseManager
import com.abtsplazita.posplazita.data.remote.MercadoPagoManager
import com.abtsplazita.posplazita.domain.repository.*
import com.abtsplazita.posplazita.ui.history.PrinterManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class CheckoutManager(
    private val saleRepository: SaleRepository,
    private val productRepository: ProductRepository,
    private val customerRepository: CustomerRepository?,
    private val settingsRepository: SettingsRepository?,
    private val productReturnRepository: ProductReturnRepository?,
    private val firebaseManager: FirebaseManager?,
    private val mercadoPagoManager: MercadoPagoManager?,
    private val printerManager: PrinterManager?,
    private val currentSaleManager: CurrentSaleManager,
    private val scope: CoroutineScope
) {
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    private val _isWaitingForMP = MutableStateFlow(false)
    val isWaitingForMP = _isWaitingForMP.asStateFlow()

    private val _mpStatus = MutableStateFlow<String?>(null)
    val mpStatus = _mpStatus.asStateFlow()

    private val _saleChange = MutableStateFlow<Double?>(null)
    val saleChange = _saleChange.asStateFlow()

    private val _showSaleSuccessOverlay = MutableStateFlow(false)
    val showSaleSuccessOverlay = _showSaleSuccessOverlay.asStateFlow()

    private val _showCardSuccess = MutableStateFlow(false)
    val showCardSuccess = _showCardSuccess.asStateFlow()

    private var mpCancelRequested = false
    private var currentMpIdempotencyKey: String? = null
    private var successJob: Job? = null

    fun updateChange(amountPaid: Double, total: Double, method: String) {
        if (method == "Efectivo") {
            val diff = amountPaid - total
            _saleChange.value = diff
        } else {
            _saleChange.value = null
        }
    }

    fun cancelMpPayment() {
        mpCancelRequested = true
        _isWaitingForMP.value = false
        _isProcessing.value = false
        _mpStatus.value = null
    }

    suspend fun performTerminalPayment(
        deviceId: String,
        accessToken: String,
        amount: Double,
        description: String,
        idempotencyKeyPrefix: String
    ): Boolean {
        if (mercadoPagoManager == null) return false
        
        mercadoPagoManager.setCredentials(accessToken, "", "")
        _isWaitingForMP.value = true
        _mpStatus.value = "Conectando con terminal $deviceId..."
        
        val idempotencyKey = currentMpIdempotencyKey ?: "${idempotencyKeyPrefix}_${com.abtsplazita.posplazita.currentTimeMillis()}"
        currentMpIdempotencyKey = idempotencyKey

        val (success, intentId, externalRef) = mercadoPagoManager.sendPaymentToPoint(deviceId, amount, description, idempotencyKey)
        
        if (success) {
            _mpStatus.value = "Esperando aprobación en terminal..."
            var paid = false
            mpCancelRequested = false
            
            val startTime = com.abtsplazita.posplazita.currentTimeMillis()
            var iteration = 0
            while (com.abtsplazita.posplazita.currentTimeMillis() - startTime < 120_000 && !mpCancelRequested) {
                iteration++
                val result = mercadoPagoManager.checkPaymentStatus(intentId)
                if (result == "SUCCESS") { paid = true; break }
                if (result == "REJECTED" || result == "CANCELED" || result == "ERROR") {
                    val finalSearch = mercadoPagoManager.searchPaymentByReference(externalRef)
                    if (finalSearch == "SUCCESS") { paid = true; break }
                    break 
                }
                if (iteration % 2 == 0 || (com.abtsplazita.posplazita.currentTimeMillis() - startTime > 100_000)) {
                    val searchResult = mercadoPagoManager.searchPaymentByReference(externalRef)
                    if (searchResult == "SUCCESS") { paid = true; break }
                }
                delay(800)
            }
            
            if (mpCancelRequested) {
                val lastCheck = mercadoPagoManager.checkPaymentStatus(intentId)
                val lastSearch = mercadoPagoManager.searchPaymentByReference(externalRef)
                if (lastCheck == "SUCCESS" || lastSearch == "SUCCESS") paid = true
                else {
                    _isWaitingForMP.value = false
                    return false
                }
            }

            _isWaitingForMP.value = false
            if (!paid) {
                _mpStatus.value = "Pago no completado."
                return false
            }
            return true
        } else {
            _isWaitingForMP.value = false
            _mpStatus.value = "Error: $intentId"
            return false
        }
    }

    suspend fun executeCheckout(
        amountPaid: Double,
        paymentMethod: String,
        branchId: String,
        selectedTerminal: PosTerminal?,
        selectedCustomer: Customer?,
        currentUser: User?,
        saleComment: String,
        ticketConfig: TicketConfig,
        branchName: String,
        shouldPrint: Boolean,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (selectedTerminal == null) {
            onError("Debes seleccionar una caja (terminal) antes de cobrar.")
            return
        }

        val currentTotal = currentSaleManager.total.value
        val isCredit = paymentMethod == "Crédito"
        val isMP = paymentMethod == "Tarjeta"
        val customerId = selectedCustomer?.id

        // --- LÓGICA DE MERCADO PAGO ---
        if (isMP) {
            val settings = settingsRepository?.getAllSettings()?.first() ?: emptyMap()
            val deviceId = settings["mp_terminal_id"] ?: ""
            val accessToken = settings["mp_access_token"] ?: ""
            
            if (deviceId.isBlank() || accessToken.isBlank()) {
                onError("Mercado Pago no configurado.")
                return
            }

            val paid = performTerminalPayment(deviceId, accessToken, currentTotal, "Venta POS", "POS")
            if (!paid) {
                onError("Pago no completado: ${_mpStatus.value ?: "desconocido"}")
                return
            }
        }

        _isProcessing.value = true
        try {
            if (paymentMethod == "Efectivo" && amountPaid < (currentTotal - 0.01)) {
                onError("El monto pagado ($${amountPaid.formatPrice()}) es insuficiente.")
                _isProcessing.value = false
                return
            }

            if (paymentMethod == "Monedero") {
                val balance = selectedCustomer?.walletBalance ?: 0.0
                if (balance < currentTotal) {
                    onError("Saldo insuficiente en monedero ($${balance.formatPrice()}).")
                    _isProcessing.value = false
                    return
                }
            }

            if (isCredit && customerId == null) {
                onError("Debes seleccionar un cliente para realizar una venta a crédito.")
                _isProcessing.value = false
                return
            }

            val now = com.abtsplazita.posplazita.currentTimeMillis()
            val saleId = saleRepository.generateUniqueSaleId(branchId, selectedTerminal.id, prefix = ticketConfig.ticketIdPrefix)
            
            val diff = amountPaid - currentTotal
            val calculatedChange = if (paymentMethod == "Efectivo" && diff > 0.005) diff else 0.0

            val sale = Sale(
                id = saleId,
                timestamp = now,
                userId = currentUser?.username ?: "admin",
                branchId = branchId,
                terminalId = selectedTerminal.id,
                customerId = customerId,
                items = currentSaleManager.currentItems.value,
                total = currentTotal,
                netTotal = currentSaleManager.currentItems.value.filter { !it.isService }.sumOf { it.subtotal },
                cashAmount = if (paymentMethod == "Efectivo") currentTotal else 0.0,
                creditAmount = if (isCredit) currentTotal else 0.0,
                receivedAmount = if (paymentMethod == "Efectivo") amountPaid else currentTotal,
                changeAmount = calculatedChange,
                paymentMethod = paymentMethod,
                comment = saleComment,
                originalWebOrderId = currentSaleManager.currentWebOrderId.value
            )

            saleRepository.saveSale(sale)
            
            _saleChange.value = calculatedChange
            _showSaleSuccessOverlay.value = true
            
            // Limpiar la venta
            currentSaleManager.clear()
            currentMpIdempotencyKey = null

            successJob?.cancel()
            successJob = scope.launch {
                delay(120000) 
                _showSaleSuccessOverlay.value = false
                _saleChange.value = null
                _showCardSuccess.value = false
            }

            // Lógica de Monedero
            if (customerId != null && customerRepository != null) {
                if (paymentMethod == "Monedero") {
                    customerRepository.updateWalletBalance(customerId, -currentTotal)
                }

                val settings = settingsRepository?.getAllSettings()?.first() ?: emptyMap()
                var walletBonus = 0.0
                sale.items.forEach { item ->
                    if (!item.isPromoApplied && !item.isWebDiscounted) {
                        val percent = settings["wallet_percent_${item.category}"]?.toDoubleOrNull() ?: 0.0
                        if (percent > 0) {
                            walletBonus += item.subtotal * (percent / 100.0)
                        }
                    }
                }
                if (walletBonus > 0) {
                    customerRepository.updateWalletBalance(customerId, walletBonus)
                }
            }

            if (isCredit && customerId != null) {
                customerRepository?.updateDebt(customerId, currentTotal)
            }

            sale.items.forEach { item ->
                if (!item.isService) {
                    productRepository.decreaseStock(item.productId, branchId, item.quantity, currentUser?.username ?: "admin", "Venta $saleId")
                }
            }

            val returns = sale.items.filter { it.quantity < 0 }
            if (returns.isNotEmpty()) {
                returns.forEach { ret ->
                    val productReturn = ProductReturn(
                        id = "RET_${com.abtsplazita.posplazita.currentTimeMillis()}_${ret.productId}",
                        timestamp = now,
                        branchId = branchId,
                        returnedItem = ret,
                        userId = currentUser?.username ?: "admin",
                        difference = ret.subtotal 
                    )
                    productReturnRepository?.saveReturn(productReturn)
                }
            }

            if (shouldPrint) {
                val finalCustomer = if (customerId != null) customerRepository?.getCustomerById(customerId) else null
                val openDrawer = paymentMethod == "Efectivo"
                
                printerManager?.printTicket(
                    sale, 
                    sale.items, 
                    openDrawer = openDrawer,
                    walletBalance = finalCustomer?.walletBalance,
                    config = ticketConfig,
                    branchName = branchName
                )
            } else if (paymentMethod == "Efectivo") {
                // REGLA: F12 abre cajón solo para efectivo
                printerManager?.openDrawer()
            }

            if (paymentMethod != "Efectivo") {
                _showCardSuccess.value = true
            }

            onDone()
        } catch (e: Exception) {
            onError("Error al procesar venta: ${e.message}")
        } finally {
            _isProcessing.value = false
        }
    }

    fun resetOverlay() {
        _showSaleSuccessOverlay.value = false
        _saleChange.value = null
        _showCardSuccess.value = false
        successJob?.cancel()
    }
}
