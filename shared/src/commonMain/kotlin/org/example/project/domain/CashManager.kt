package com.abtsplazita.posplazita.domain

import com.abtsplazita.posplazita.domain.repository.CashMovementRepository
import com.abtsplazita.posplazita.domain.repository.SettingsRepository
import com.abtsplazita.posplazita.ui.history.PrinterManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.abs

class CashManager(
    private val cashMovementRepository: CashMovementRepository?,
    private val settingsRepository: SettingsRepository?,
    private val checkoutManager: CheckoutManager,
    private val printerManager: PrinterManager?,
    private val scope: CoroutineScope
) {
    private val _showCashMovementDialog = MutableStateFlow<CashMovementType?>(null)
    val showCashMovementDialog = _showCashMovementDialog.asStateFlow()

    private val _showWithdrawalDialog = MutableStateFlow(false)
    val showWithdrawalDialog = _showWithdrawalDialog.asStateFlow()

    private val _isProcessingWithdrawal = MutableStateFlow(false)
    val isProcessingWithdrawal = _isProcessingWithdrawal.asStateFlow()

    fun openCashMovementDialog(type: CashMovementType) { _showCashMovementDialog.value = type }
    fun closeCashMovementDialog() { _showCashMovementDialog.value = null }

    fun openWithdrawalDialog() { _showWithdrawalDialog.value = true }
    fun closeWithdrawalDialog() { _showWithdrawalDialog.value = false }

    fun addCashMovement(
        amount: Double,
        reason: String,
        branchId: String,
        selectedTerminal: PosTerminal?,
        currentUser: User?,
        cashInDrawer: Double,
        isManual: Boolean = true,
        onError: (String) -> Unit,
        onSuccess: (String) -> Unit
    ) {
        val type = _showCashMovementDialog.value ?: (if (amount >= 0) CashMovementType.IN else CashMovementType.OUT)
        val absAmount = abs(amount)
        
        if (isManual && type == CashMovementType.OUT && absAmount > (cashInDrawer + 0.01)) {
            onError("Fondo insuficiente ($${cashInDrawer.formatPrice()})")
            return
        }

        scope.launch {
            try {
                val movId = "M-${branchId}-${selectedTerminal?.id ?: "0"}-${com.abtsplazita.posplazita.currentTimeMillis()}"
                val movement = CashMovement(
                    id = movId,
                    timestamp = com.abtsplazita.posplazita.currentTimeMillis(),
                    branchId = branchId,
                    terminalId = selectedTerminal?.id,
                    type = type,
                    amount = absAmount,
                    reason = reason,
                    userId = currentUser?.username ?: "admin"
                )
                cashMovementRepository?.saveMovement(movement)
                
                // Abrir cajón para cualquier movimiento de efectivo
                printerManager?.openDrawer()

                if (isManual) {
                    onSuccess("${if(type == CashMovementType.IN) "Entrada" else "Salida"} registrada.")
                    closeCashMovementDialog()
                }
            } catch (e: Exception) {
                onError("Error al registrar movimiento: ${e.message}")
            }
        }
    }

    fun processWithdrawal(
        withdrawalAmount: Double,
        commission: Double,
        commissionInCash: Boolean,
        branchId: String,
        selectedTerminal: PosTerminal?,
        currentUser: User?,
        cashInDrawer: Double,
        onError: (String) -> Unit,
        onSuccess: (String) -> Unit
    ) {
        if (_isProcessingWithdrawal.value) return
        
        scope.launch {
            if (selectedTerminal == null) {
                onError("Debes seleccionar una caja (terminal) para registrar el retiro.")
                return@launch
            }

            val withdrawalSettings = settingsRepository?.getAllSettings()?.first() ?: emptyMap()
            val deviceId = withdrawalSettings["mp_terminal_id"] ?: ""
            val accessToken = withdrawalSettings["mp_access_token"] ?: ""
            
            if (deviceId.isBlank() || accessToken.isBlank()) {
                onError("Terminal Point no configurada.")
                return@launch
            }

            if (withdrawalAmount > cashInDrawer) {
                onError("No hay suficiente efectivo en caja para este retiro.")
                return@launch
            }

            _isProcessingWithdrawal.value = true
            try {
                val chargeAmount = if (commissionInCash) withdrawalAmount else (withdrawalAmount + commission)
                val paid = checkoutManager.performTerminalPayment(deviceId, accessToken, chargeAmount, "Retiro de Efectivo", "WDR")
                
                if (paid) {
                    // Registrar SALIDA y opcionalmente ENTRADA de comisión
                    addCashMovement(
                        amount = -withdrawalAmount,
                        reason = "Retiro de efectivo (Tarjeta)",
                        branchId = branchId,
                        selectedTerminal = selectedTerminal,
                        currentUser = currentUser,
                        cashInDrawer = cashInDrawer,
                        isManual = false,
                        onError = onError,
                        onSuccess = {}
                    )
                    
                    if (commissionInCash) {
                        addCashMovement(
                            amount = commission,
                            reason = "Comisión por retiro (Efectivo)",
                            branchId = branchId,
                            selectedTerminal = selectedTerminal,
                            currentUser = currentUser,
                            cashInDrawer = cashInDrawer,
                            isManual = false,
                            onError = onError,
                            onSuccess = {}
                        )
                    }

                    onSuccess("Retiro procesado correctamente.\nEntrega $${withdrawalAmount.formatPrice()} al cliente.")
                    
                    // NUEVO: Imprimir ticket de comprobante de retiro
                    val ticketContent = """
                        COMPROBANTE DE RETIRO
                        --------------------------------
                        FECHA: ${com.abtsplazita.posplazita.formatTimestamp(com.abtsplazita.posplazita.currentTimeMillis())}
                        CAJA: ${selectedTerminal?.name ?: "1"}
                        USUARIO: ${currentUser?.username ?: "admin"}
                        
                        MONTO RETIRO:  $${withdrawalAmount.formatPrice()}
                        COMISION:      $${commission.formatPrice()}
                        --------------------------------
                        TOTAL TARJETA: $${chargeAmount.formatPrice()}
                        
                        
                        
                    """.trimIndent()
                    
                    printerManager?.printRawText(ticketContent, openDrawer = true)
                    
                    closeWithdrawalDialog()
                } else {
                    onError("Cobro en tarjeta no completado.")
                }
            } catch (e: Exception) {
                onError("Error en retiro: ${e.message}")
            } finally {
                _isProcessingWithdrawal.value = false
            }
        }
    }
}
