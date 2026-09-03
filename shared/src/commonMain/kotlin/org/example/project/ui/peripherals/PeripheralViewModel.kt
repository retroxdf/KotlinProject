package com.abtsplazita.posplazita.ui.peripherals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.abtsplazita.posplazita.domain.PosTerminal
import com.abtsplazita.posplazita.domain.repository.PosTerminalRepository
import com.abtsplazita.posplazita.domain.Product
import com.abtsplazita.posplazita.domain.UnitType
import com.abtsplazita.posplazita.domain.repository.ProductRepository
import com.abtsplazita.posplazita.domain.ScaleManager
import com.abtsplazita.posplazita.domain.getScaleManager
import com.abtsplazita.posplazita.domain.repository.SettingsRepository
import com.abtsplazita.posplazita.data.remote.FirebaseManager
import com.abtsplazita.posplazita.data.remote.MercadoPagoManager
import com.abtsplazita.posplazita.isBluetoothSupported
import com.abtsplazita.posplazita.ui.history.PrinterManager
import com.abtsplazita.posplazita.domain.TicketConfig
import com.abtsplazita.posplazita.domain.TicketElement
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

enum class PrinterConnectionType { NETWORK, BLUETOOTH, SERIAL, SYSTEM }

class PeripheralViewModel(
    private val settingsRepository: SettingsRepository? = null,
    private val printerManager: PrinterManager? = null,
    private val terminalRepository: PosTerminalRepository? = null,
    private val firebaseManager: FirebaseManager? = null,
    private val mercadoPagoManager: MercadoPagoManager? = null,
    private val productRepository: ProductRepository? = null,
    val branchId: String = ""
) : ViewModel() {

    private val scaleManager = getScaleManager()

    // --- Gestión de Cajas (Terminales) ---
    val terminals: StateFlow<List<PosTerminal>> = if (terminalRepository != null && branchId.isNotBlank()) {
        terminalRepository.getTerminalsByBranch(branchId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    } else {
        MutableStateFlow(emptyList())
    }

    private val _selectedTerminalId = MutableStateFlow("")
    val selectedTerminalId = _selectedTerminalId.asStateFlow()

    // --- Preferencias de Corte ---
    private val _showCashOutTotal = MutableStateFlow(false)
    val showCashOutTotal = _showCashOutTotal.asStateFlow()

    private val _allowNegativeStock = MutableStateFlow(false)
    val allowNegativeStock = _allowNegativeStock.asStateFlow()

    private val _defaultPriceLevel = MutableStateFlow(2) // Por defecto P2 (Público)
    val defaultPriceLevel = _defaultPriceLevel.asStateFlow()

    // --- Nuevas Preferencias de Operatividad ---
    private val _askQuantityOnAdd = MutableStateFlow(false)
    val askQuantityOnAdd = _askQuantityOnAdd.asStateFlow()

    private val _addAtTop = MutableStateFlow(false)
    val addAtTop = _addAtTop.asStateFlow()

    private val _isWholesaleEnabled = MutableStateFlow(false)
    val isWholesaleEnabled = _isWholesaleEnabled.asStateFlow()

    private val _autoBranchLogin = MutableStateFlow(false)
    val autoBranchLogin = _autoBranchLogin.asStateFlow()

    private val _lockBranchChange = MutableStateFlow(false)
    val lockBranchChange = _lockBranchChange.asStateFlow()

    private val _maxCashLimit = MutableStateFlow(5000.0) // Límite para retiro parcial
    val maxCashLimit = _maxCashLimit.asStateFlow()

    private val _isWebshopEnabled = MutableStateFlow(false)
    val isWebshopEnabled = _isWebshopEnabled.asStateFlow()

    private val _lastWebshopSync = MutableStateFlow<Long?>(null)
    val lastWebshopSync = _lastWebshopSync.asStateFlow()

    private val _isWebshopSyncing = MutableStateFlow(false)
    val isWebshopSyncing = _isWebshopSyncing.asStateFlow()

    // --- Asistente IA WhatsApp ---
    private val _isAiEnabled = MutableStateFlow(false)
    val isAiEnabled = _isAiEnabled.asStateFlow()

    private val _lastAiSync = MutableStateFlow<Long?>(null)
    val lastAiSync = _lastAiSync.asStateFlow()

    private val _isAiSyncing = MutableStateFlow(false)
    val isAiSyncing = _isAiSyncing.asStateFlow()

    // --- Logo y Branding ---
    private val _appLogoUrl = MutableStateFlow("https://firebasestorage.googleapis.com/v0/b/posplazita.appspot.com/o/logo.png?alt=media")
    val appLogoUrl = _appLogoUrl.asStateFlow()

    // --- Datos de Sucursal Actual ---
    private val _branchName = MutableStateFlow("")
    val branchName = _branchName.asStateFlow()

    private val _branchAddress = MutableStateFlow("")
    val branchAddress = _branchAddress.asStateFlow()

    private val _branchPhone = MutableStateFlow("")
    val branchPhone = _branchPhone.asStateFlow()

    // --- Gestión de Monedero ---
    private val _walletPercentages = MutableStateFlow<Map<String, Double>>(emptyMap())
    val walletPercentages = _walletPercentages.asStateFlow()

    fun updateWalletPercentage(category: String, percent: Double) {
        val current = _walletPercentages.value.toMutableMap()
        current[category] = percent
        _walletPercentages.value = current
        saveSetting("wallet_percent_$category", percent.toString())
    }

    init {
        loadSettings()
        startGlobalAdsSync()
    }

    private fun startGlobalAdsSync() {
        firebaseManager?.observeGlobalAds { urls ->
            // Sincronizar siempre, incluso si llega una lista vacía
            _adImages.value = urls
            // También guardar localmente para uso offline
            saveSetting("ad_images", urls.joinToString("|"))
        }
    }

    private fun loadSettings() {
        if (settingsRepository == null) return
        viewModelScope.launch {
            settingsRepository.getAllSettings().collect { settings ->
                // Las preferencias de venta ahora son por sucursal
                settings["${branchId}_show_cash_out_total"]?.let { _showCashOutTotal.value = it.toBoolean() }
                settings["${branchId}_allow_negative_stock"]?.let { _allowNegativeStock.value = it.toBoolean() }
                settings["${branchId}_default_price_level"]?.let { _defaultPriceLevel.value = it.toIntOrNull() ?: 2 }
                settings["${branchId}_ask_qty_add"]?.let { _askQuantityOnAdd.value = it.toBoolean() }
                settings["${branchId}_add_at_top"]?.let { _addAtTop.value = it.toBoolean() }
                settings["${branchId}_wholesale_enabled"]?.let { _isWholesaleEnabled.value = it.toBoolean() }
                
                settings["${branchId}_name"]?.let { _branchName.value = it }
                settings["${branchId}_address"]?.let { _branchAddress.value = it }
                settings["${branchId}_phone"]?.let { _branchPhone.value = it }

                settings["app_auto_branch_login"]?.let { _autoBranchLogin.value = it.toBoolean() }
                settings["app_lock_branch_change"]?.let { _lockBranchChange.value = it.toBoolean() }
                settings["${branchId}_max_cash_limit"]?.let { _maxCashLimit.value = it.toDoubleOrNull() ?: 5000.0 }
                settings["app_logo_url"]?.let { _appLogoUrl.value = it }
                
                settings["${branchId}_webshop_enabled"]?.let { _isWebshopEnabled.value = it.toBoolean() }
                settings["${branchId}_last_webshop_sync"]?.let { _lastWebshopSync.value = it.toLongOrNull() }

                settings["whatsapp_ai_enabled"]?.let { _isAiEnabled.value = it.toBoolean() }
                settings["last_ai_sync"]?.let { _lastAiSync.value = it.toLongOrNull() }

                // Las de hardware siguen siendo globales/por dispositivo
                settings["printer_name"]?.let { _printerName.value = it }
                settings["printer_address"]?.let { _printerAddress.value = it }
                settings["connection_type"]?.let { 
                    try { _connectionType.value = PrinterConnectionType.valueOf(it) } catch(e: Exception) {} 
                }
                settings["bluetooth_mac"]?.let { _bluetoothMac.value = it }
                
                settings["printer_paper_size"]?.let { _paperSize.value = it.toIntOrNull() ?: 80 }
                settings["printer_auto_cut"]?.let { _autoCut.value = it.toBoolean() }
                settings["printer_open_drawer"]?.let { _openDrawerOnPrint.value = it.toBoolean() }

                // Cargar estado de conexión de la impresora
                settings["printer_active"]?.let { active ->
                    val shouldBeConnected = active.toBoolean()
                    if (shouldBeConnected && !_isPrinterConnected.value) {
                        _isPrinterConnected.value = true
                        // Pequeño retardo para asegurar que los otros valores ya se asignaron
                        viewModelScope.launch {
                            kotlinx.coroutines.delay(800)
                            syncConfigIfConnected()
                        }
                    } else if (!shouldBeConnected) {
                        _isPrinterConnected.value = false
                    }
                }

                // Cargar diseño del ticket
                settings["ticket_layout_json"]?.let {
                    try {
                        _ticketLayout.value = Json.decodeFromString(it)
                    } catch (e: Exception) {
                        _ticketLayout.value = TicketConfig.defaultLayout
                    }
                }

                settings["scale_port"]?.let { _scalePort.value = it }
                settings["scale_baud"]?.let { _scaleBaudRate.value = it }
                settings["scale_seq"]?.let { _scaleSequence.value = it }
                settings["scale_delay"]?.let { _scaleDelay.value = it }

                settings["redmas_user"]?.let { _redMasUser.value = it }
                settings["redmas_pass"]?.let { _redMasPass.value = it }
                settings["redmas_active"]?.let { _isRedMasActive.value = it.toBoolean() }
                
                settings["mp_terminal_id"]?.let { _mpTerminalId.value = it }
                settings["mp_access_token"]?.let { _mpAccessToken.value = it }
                settings["mp_client_id"]?.let { _mpClientId.value = it }
                settings["mp_user_id"]?.let { _mpUserId.value = it }
                settings["mp_public_key"]?.let { _mpPublicKey.value = it }
                
                val mpToken = _mpAccessToken.value
                val mpAid = _mpClientId.value
                val mpUid = _mpUserId.value
                if (mpToken.isNotBlank()) {
                    mercadoPagoManager?.setCredentials(mpToken, mpAid, mpUid)
                }

                settings["ad_images"]?.let { if (it.isNotBlank()) _adImages.value = it.split("|") }

                // Cargar porcentajes de monedero por categoría (Modo robusto)
                val percents = settings.filter { it.key.startsWith("wallet_percent_") }
                    .mapKeys { it.key.removePrefix("wallet_percent_") }
                    .mapValues { it.value.toDoubleOrNull() ?: 0.0 }
                _walletPercentages.value = percents
            }
        }
    }

    private fun saveSetting(key: String, value: String) {
        if (settingsRepository == null) return
        viewModelScope.launch {
            settingsRepository.saveSetting(key, value)
        }
    }

    fun toggleShowCashOutTotal(show: Boolean) {
        _showCashOutTotal.value = show
        saveSetting("${branchId}_show_cash_out_total", show.toString())
    }

    fun toggleAllowNegativeStock(allow: Boolean) {
        _allowNegativeStock.value = allow
        saveSetting("${branchId}_allow_negative_stock", allow.toString())
    }

    fun toggleAskQuantityOnAdd(ask: Boolean) {
        _askQuantityOnAdd.value = ask
        saveSetting("${branchId}_ask_qty_add", ask.toString())
    }

    fun toggleAddAtTop(top: Boolean) {
        _addAtTop.value = top
        saveSetting("${branchId}_add_at_top", top.toString())
    }

    fun toggleWholesale(enabled: Boolean) {
        _isWholesaleEnabled.value = enabled
        saveSetting("${branchId}_wholesale_enabled", enabled.toString())
    }

    fun toggleAutoBranchLogin(auto: Boolean) {
        _autoBranchLogin.value = auto
        saveSetting("app_auto_branch_login", auto.toString())
    }

    fun toggleLockBranchChange(lock: Boolean) {
        _lockBranchChange.value = lock
        saveSetting("app_lock_branch_change", lock.toString())
    }

    fun updateMaxCashLimit(limit: Double) {
        _maxCashLimit.value = limit
        saveSetting("${branchId}_max_cash_limit", limit.toString())
    }

    fun updateAppLogoUrl(url: String) {
        _appLogoUrl.value = url
        saveSetting("app_logo_url", url)
    }

    fun updateBranchInfo(name: String, address: String, phone: String) {
        _branchName.value = name
        _branchAddress.value = address
        _branchPhone.value = phone
        saveSetting("${branchId}_name", name)
        saveSetting("${branchId}_address", address)
        saveSetting("${branchId}_phone", phone)
    }

    fun setDefaultPriceLevel(level: Int) {
        if (level in 1..4) {
            _defaultPriceLevel.value = level
            saveSetting("${branchId}_default_price_level", level.toString())
        }
    }

    fun toggleWebshop(enabled: Boolean) {
        _isWebshopEnabled.value = enabled
        saveSetting("${branchId}_webshop_enabled", enabled.toString())
    }

    fun toggleAi(enabled: Boolean) {
        _isAiEnabled.value = enabled
        saveSetting("whatsapp_ai_enabled", enabled.toString())
        // Sincronizar el estado maestro con Firebase para que la Function sepa si responder
        firebaseManager?.syncAiConfig(enabled)
    }

    fun syncAiKnowledgeBase() {
        if (productRepository == null || firebaseManager == null) return
        
        viewModelScope.launch {
            try {
                _isAiSyncing.value = true
                
                // 1. Obtener todos los productos y su stock total de todas las sucursales
                // para que la IA sepa qué hay en todo el negocio
                val allProducts = productRepository.getProducts().first()
                val allInventory = productRepository.getAllInventory().first()
                
                // 2. Subir a Firestore (esto ya lo tenemos con syncProductBatch y syncInventoryBatch)
                // La IA consultará estas mismas colecciones.
                if (allProducts.isNotEmpty()) {
                    firebaseManager.syncProductBatch(allProducts)
                }
                
                // Sincronizar stock de la sucursal actual
                val currentBranchStock = allInventory.filter { it.branchId == branchId }
                if (currentBranchStock.isNotEmpty()) {
                    firebaseManager.syncInventoryBatch(branchId, currentBranchStock)
                }

                val now = com.abtsplazita.posplazita.currentTimeMillis()
                _lastAiSync.value = now
                saveSetting("last_ai_sync", now.toString())
                
            } catch (e: Exception) {
                println("AI_SYNC_ERROR: ${e.message}")
            } finally {
                _isAiSyncing.value = false
            }
        }
    }

    fun performManualWebshopSync() {
        if (branchId.isBlank() || productRepository == null || firebaseManager == null) return
        
        viewModelScope.launch {
            try {
                _isWebshopSyncing.value = true
                
                // 1. Sincronizar TODAS las definiciones de productos primero
                // (Para asegurar que la Webshop tenga nombres, fotos y precios actualizados)
                val allProducts = productRepository.getProducts().first()
                if (allProducts.isNotEmpty()) {
                    firebaseManager.syncProductBatch(allProducts)
                }

                // 2. Obtener todo el inventario local para esta sucursal
                val allInventory = productRepository.getAllInventory().first()
                    .filter { it.branchId == branchId }
                
                if (allInventory.isNotEmpty()) {
                    // 3. Sincronizar existencias por lotes
                    firebaseManager.syncInventoryBatch(branchId, allInventory)
                    
                    // 4. Actualizar fecha de última sincronización
                    val now = com.abtsplazita.posplazita.currentTimeMillis()
                    _lastWebshopSync.value = now
                    saveSetting("${branchId}_last_webshop_sync", now.toString())
                }
            } catch (e: Exception) {
                println("SYNC_ERROR: ${e.message}")
            } finally {
                _isWebshopSyncing.value = false
            }
        }
    }

    fun addTerminal(name: String) {
        if (terminalRepository == null || branchId.isBlank() || name.isBlank()) return
        viewModelScope.launch {
            val newTerminal = PosTerminal(
                id = "T_${com.abtsplazita.posplazita.currentTimeMillis()}",
                branchId = branchId,
                name = name
            )
            terminalRepository.addTerminal(newTerminal)
        }
    }

    fun deleteTerminal(id: String) {
        if (terminalRepository == null) return
        viewModelScope.launch {
            terminalRepository.deleteTerminal(id)
            if (_selectedTerminalId.value == id) {
                _selectedTerminalId.value = ""
            }
        }
    }

    fun selectTerminal(id: String) {
        _selectedTerminalId.value = id
    }
    
    // --- Publicidad ---
    private val _adImages = MutableStateFlow<List<String>>(emptyList())
    val adImages = _adImages.asStateFlow()

    private val _currentAdIndex = MutableStateFlow(0)
    val currentAdIndex = _currentAdIndex.asStateFlow()

    // --- Configuración de Impresora ---
    private val _isBluetoothAvailable = MutableStateFlow(isBluetoothSupported())
    val isBluetoothAvailable = _isBluetoothAvailable.asStateFlow()

    private val _connectionType = MutableStateFlow(PrinterConnectionType.NETWORK)
    val connectionType = _connectionType.asStateFlow()

    private val _printerName = MutableStateFlow("Impresora Termica 80mm")
    val printerName = _printerName.asStateFlow()

    private val _printerAddress = MutableStateFlow("192.168.1.100")
    val printerAddress = _printerAddress.asStateFlow()

    private val _bluetoothMac = MutableStateFlow("")
    val bluetoothMac = _bluetoothMac.asStateFlow()

    private val _paperSize = MutableStateFlow(80)
    val paperSize = _paperSize.asStateFlow()

    private val _autoCut = MutableStateFlow(true)
    val autoCut = _autoCut.asStateFlow()

    private val _openDrawerOnPrint = MutableStateFlow(true)
    val openDrawerOnPrint = _openDrawerOnPrint.asStateFlow()

    private val _availableSystemPrinters = MutableStateFlow<List<String>>(emptyList())
    val availableSystemPrinters = _availableSystemPrinters.asStateFlow()

    private val _ticketLayout = MutableStateFlow(TicketConfig.defaultLayout)
    val ticketLayout = _ticketLayout.asStateFlow()

    private val _isPrinterConnected = MutableStateFlow(false)
    val isPrinterConnected = _isPrinterConnected.asStateFlow()

    val allSettings: StateFlow<Map<String, String>> = if (settingsRepository != null) {
        settingsRepository.getAllSettings()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    } else {
        MutableStateFlow(emptyMap())
    }

    private val _availableBluetoothDevices = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val availableBluetoothDevices = _availableBluetoothDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    fun setConnectionType(type: PrinterConnectionType) {
        _connectionType.value = type
        _isPrinterConnected.value = false
        saveSetting("connection_type", type.name)
        saveSetting("printer_active", "false")
    }

    // --- Configuración Mercado Pago (PRODUCCIÓN) ---
    private val _mpTerminalId = MutableStateFlow("N950NCD100349356")
    val mpTerminalId = _mpTerminalId.asStateFlow()

    private val _mpAccessToken = MutableStateFlow("APP_USR-571829913797874-082201-4a83171dceabcd3f89f147b59575f4e2-274357159")
    val mpAccessToken = _mpAccessToken.asStateFlow()

    private val _mpClientId = MutableStateFlow("571829913797874")
    val mpClientId = _mpClientId.asStateFlow()

    private val _mpUserId = MutableStateFlow("274357159")
    val mpUserId = _mpUserId.asStateFlow()

    private val _mpPublicKey = MutableStateFlow("APP_USR-5032bb2e-e44b-437d-b16b-e8f9218b7a73")
    val mpPublicKey = _mpPublicKey.asStateFlow()

    private val _isMpConnected = MutableStateFlow(false)
    val isMpConnected = _isMpConnected.asStateFlow()

    private val _mpStatus = MutableStateFlow("Desconectado")
    val mpStatus = _mpStatus.asStateFlow()

    // --- Configuración de Báscula ---
    private val _scalePort = MutableStateFlow("COM1")
    val scalePort = _scalePort.asStateFlow()

    private val _availablePorts = MutableStateFlow(listOf("COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9"))
    val availablePorts = _availablePorts.asStateFlow()

    private val _scaleBaudRate = MutableStateFlow("9600")
    val scaleBaudRate = _scaleBaudRate.asStateFlow()

    private val _scaleSequence = MutableStateFlow("P") // Secuencia corregida a 'P'
    val scaleSequence = _scaleSequence.asStateFlow()

    private val _scaleDelay = MutableStateFlow("100") // Retardo en ms
    val scaleDelay = _scaleDelay.asStateFlow()

    private val _isScaleConnected = MutableStateFlow(false) // Asegurar que inicie en FALSE
    val isScaleConnected = _isScaleConnected.asStateFlow()

    private val _lastScaleWeight = MutableStateFlow<Double?>(null)
    val lastScaleWeight = _lastScaleWeight.asStateFlow()

    private val _isTestingScale = MutableStateFlow(false)
    val isTestingScale = _isTestingScale.asStateFlow()

    fun updateScalePort(port: String) {
        _scalePort.value = port
        saveSetting("scale_port", port)
    }

    fun refreshAvailablePorts() {
        _availablePorts.value = scaleManager.getAvailablePorts()
    }

    fun updateScaleBaudRate(baud: String) {
        _scaleBaudRate.value = baud
        saveSetting("scale_baud", baud)
    }

    fun updateScaleSequence(seq: String) {
        _scaleSequence.value = seq
        saveSetting("scale_seq", seq)
    }

    fun updateScaleDelay(delay: String) {
        if (delay.all { it.isDigit() }) {
            _scaleDelay.value = delay
            saveSetting("scale_delay", delay)
        }
    }

    fun toggleScaleConnection() {
        viewModelScope.launch {
            _isTestingScale.value = true
            
            if (_isScaleConnected.value) {
                scaleManager.disconnect()
                _isScaleConnected.value = false
            } else {
                val baud = _scaleBaudRate.value.toIntOrNull() ?: 9600
                val delay = _scaleDelay.value.toIntOrNull() ?: 100
                val connected = scaleManager.connect(
                    port = _scalePort.value,
                    baudRate = baud,
                    sequence = _scaleSequence.value,
                    delay = delay
                )
                _isScaleConnected.value = connected
            }
            
            _isTestingScale.value = false
        }
    }

    fun testScale() {
        if (!_isScaleConnected.value) return
        
        viewModelScope.launch {
            _isTestingScale.value = true
            val weight = scaleManager.readWeight()
            _lastScaleWeight.value = weight
            _isTestingScale.value = false
        }
    }

    // --- Configuración de Recargas (Red Más) ---
    private val _redMasUser = MutableStateFlow("")
    val redMasUser = _redMasUser.asStateFlow()

    private val _redMasUserValue get() = _redMasUser.value
    private val _redMasPassValue get() = _redMasPass.value

    private val _redMasPass = MutableStateFlow("")
    val redMasPass = _redMasPass.asStateFlow()

    private val _isRedMasActive = MutableStateFlow(false)
    val isRedMasActive = _isRedMasActive.asStateFlow()

    fun updateRedMasCredentials(user: String, pass: String) {
        _redMasUser.value = user
        _redMasPass.value = pass
        saveSetting("redmas_user", user)
        saveSetting("redmas_pass", pass)
    }

    fun toggleRedMas(active: Boolean) {
        _isRedMasActive.value = active
        saveSetting("redmas_active", active.toString())
    }

    fun updatePrinterName(name: String) {
        _printerName.value = name
        syncConfigIfConnected()
        saveSetting("printer_name", name)
    }

    fun updatePrinterAddress(address: String) {
        _printerAddress.value = address
        syncConfigIfConnected()
        saveSetting("printer_address", address)
    }

    fun updateBluetoothMac(mac: String) {
        _bluetoothMac.value = mac
        syncConfigIfConnected()
        saveSetting("bluetooth_mac", mac)
    }

    fun updatePaperSize(size: Int) {
        _paperSize.value = size
        syncConfigIfConnected()
        saveSetting("printer_paper_size", size.toString())
    }

    fun toggleAutoCut(enabled: Boolean) {
        _autoCut.value = enabled
        syncConfigIfConnected()
        saveSetting("printer_auto_cut", enabled.toString())
    }

    fun toggleOpenDrawerOnPrint(enabled: Boolean) {
        _openDrawerOnPrint.value = enabled
        syncConfigIfConnected()
        saveSetting("printer_open_drawer", enabled.toString())
    }

    private fun syncConfigIfConnected() {
        if (_isPrinterConnected.value) {
            val addr = if (_connectionType.value == PrinterConnectionType.NETWORK) _printerAddress.value else _bluetoothMac.value
            printerManager?.setConfig(
                name = _printerName.value,
                type = _connectionType.value.name,
                address = addr,
                paperSize = _paperSize.value,
                autoCut = _autoCut.value,
                openDrawer = _openDrawerOnPrint.value
            )
        }
    }

    fun startBluetoothScan() {
        _isScanning.value = true
        // Obtener dispositivos vinculados reales del Manager
        _availableBluetoothDevices.value = printerManager?.getPairedDevices() ?: emptyList()
        _isScanning.value = false
    }

    fun refreshSystemPrinters() {
        _availableSystemPrinters.value = printerManager?.getSystemPrinters() ?: emptyList()
    }

    fun printTest() {
        if (_isPrinterConnected.value) {
            printerManager?.printTestPage()
        }
    }

    fun openDrawerAndPrintTest() {
        if (_isPrinterConnected.value) {
            viewModelScope.launch {
                printerManager?.openDrawer()
                kotlinx.coroutines.delay(500)
                printerManager?.printTestPage()
            }
        }
    }

    fun togglePrinterConnection() {
        _isPrinterConnected.value = !_isPrinterConnected.value
        saveSetting("printer_active", _isPrinterConnected.value.toString())
        
        if (_isPrinterConnected.value) {
            syncConfigIfConnected()
        }
    }

    fun updateMpTerminalId(id: String) {
        _mpTerminalId.value = id
        saveSetting("mp_terminal_id", id)
    }

    fun saveMpCredentials(token: String, clientId: String, userId: String, publicKey: String) {
        _mpAccessToken.value = token
        _mpClientId.value = clientId
        _mpUserId.value = userId
        _mpPublicKey.value = publicKey
        saveSetting("mp_access_token", token)
        saveSetting("mp_client_id", clientId)
        saveSetting("mp_user_id", userId)
        saveSetting("mp_public_key", publicKey)
        mercadoPagoManager?.setCredentials(token, clientId, userId)
    }

    private val _availableMpDevices = MutableStateFlow<List<String>>(emptyList())
    val availableMpDevices = _availableMpDevices.asStateFlow()

    fun refreshMpDevices() {
        if (_mpAccessToken.value.isBlank()) {
            _mpStatus.value = "Ingresa el Access Token primero"
            return
        }
        viewModelScope.launch {
            _availableMpDevices.value = mercadoPagoManager?.getDevices() ?: emptyList()
            if (_availableMpDevices.value.isEmpty()) {
                _mpStatus.value = "No se encontraron terminales PDV. ¿Ya activaste el modo?"
            } else {
                _mpStatus.value = "Terminales detectadas"
            }
        }
    }

    fun activatePdvMode(deviceId: String) {
        viewModelScope.launch {
            val (success, error) = mercadoPagoManager?.activatePdvMode(deviceId) ?: (false to "Error")
            if (success) {
                _mpStatus.value = "PDV ACTIVADO ✅ - APAGA Y ENCIENDE TU TERMINAL POINT"
                refreshMpDevices()
            } else {
                _mpStatus.value = "Fallo al activar PDV: $error"
            }
        }
    }

    fun disconnectMercadoPago() {
        _isMpConnected.value = false
        _mpStatus.value = "Desconectado"
    }

    fun updateAdImageUrl(url: String) {
        // Este método se mantiene para compatibilidad si solo hay una, pero ahora manejamos lista
        _adImages.value = listOf(url)
    }

    fun updateTicketConfig(logo: String, fb: String, ig: String, wa: String, thanks: String, showBranch: Boolean, prefix: String = "S") {
        saveSetting("ticket_logo_path", logo)
        saveSetting("ticket_facebook", fb)
        saveSetting("ticket_instagram", ig)
        saveSetting("ticket_whatsapp", wa)
        saveSetting("ticket_thanks_message", thanks)
        saveSetting("ticket_show_branch", showBranch.toString())
        saveSetting("ticket_id_prefix", prefix)
    }

    fun updateTicketLayout(newLayout: List<TicketElement>) {
        _ticketLayout.value = newLayout
        saveSetting("ticket_layout_json", Json.encodeToString(newLayout))
    }

    fun addAdImage(url: String) {
        if (url.isNotBlank()) {
            val newList = _adImages.value + url
            _adImages.value = newList
            saveSetting("ad_images", newList.joinToString("|"))
            firebaseManager?.syncGlobalAds(newList)
        }
    }

    fun removeAdImage(url: String) {
        val newList = _adImages.value - url
        _adImages.value = newList
        saveSetting("ad_images", newList.joinToString("|"))
        firebaseManager?.syncGlobalAds(newList)
    }

    fun nextAd() {
        if (_adImages.value.isNotEmpty()) {
            _currentAdIndex.value = (_currentAdIndex.value + 1) % _adImages.value.size
        }
    }

    // --- Importador de Productos ---
    private val _importData = MutableStateFlow<List<List<String>>>(emptyList())
    val importData = _importData.asStateFlow()

    private val _importError = MutableStateFlow<String?>(null)
    val importError = _importError.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting = _isImporting.asStateFlow()

    fun processImportText(text: String) {
        if (text.isBlank()) {
            _importError.value = "El texto está vacío"
            return
        }
        
        try {
            // Detectar si es Tab-Separated (Excel Paste) o Comma-Separated (CSV)
            val delimiter = if (text.contains("\t")) "\t" else if (text.contains(";")) ";" else ","
            val lines = text.lines().filter { it.isNotBlank() }
            val data = lines.map { it.split(delimiter).map { cell -> cell.trim().removeSurrounding("\"") } }
            _importData.value = data
            _importError.value = null
        } catch (e: Exception) {
            _importError.value = "Error al procesar: ${e.message}"
        }
    }

    fun processImportFile(bytes: ByteArray) {
        try {
            // Convertimos bytes a String asumiendo UTF-8 (para CSV)
            val content = bytes.decodeToString()
            processImportText(content)
        } catch (e: Exception) {
            _importError.value = "Error al leer el archivo: ${e.message}"
        }
    }

    fun clearImport() {
        _importData.value = emptyList()
        _importError.value = null
    }

    fun executeImport(
        repository: ProductRepository,
        columnMapping: Map<String, Int>, // Mapping: "name" -> 0, "barcode" -> 1, etc.
        onSuccess: (Int) -> Unit
    ) {
        val data = _importData.value
        if (data.isEmpty() || columnMapping.isEmpty()) return

        viewModelScope.launch {
            _isImporting.value = true
            var importedCount = 0
            try {
                data.forEach { row ->
                    val name = columnMapping["name"]?.let { row.getOrNull(it) } ?: ""
                    val barcode = columnMapping["barcode"]?.let { row.getOrNull(it) } ?: ""
                    
                    if (name.isNotBlank() && barcode.isNotBlank()) {
                        val isBulkValue = columnMapping["isBulk"]?.let { row.getOrNull(it)?.lowercase() == "si" } ?: false
                        val useScaleValue = columnMapping["useScale"]?.let { row.getOrNull(it)?.lowercase() == "si" } ?: false
                        
                        val costVal = columnMapping["cost"]?.let { row.getOrNull(it)?.toDoubleOrNull() } ?: 0.0
                        
                        // Si no vienen precios en el Excel, los calculamos como en la carga manual
                        val p1Val = columnMapping["price1"]?.let { row.getOrNull(it)?.toDoubleOrNull() } ?: (costVal * 1.1)
                        val p2Val = columnMapping["price2"]?.let { row.getOrNull(it)?.toDoubleOrNull() } ?: (costVal * 1.2)
                        val p3Val = columnMapping["price3"]?.let { row.getOrNull(it)?.toDoubleOrNull() } ?: (costVal * 1.3)
                        val p4Val = columnMapping["price4"]?.let { row.getOrNull(it)?.toDoubleOrNull() } ?: (p3Val + 0.5)

                        val product = Product(
                            id = "P_${com.abtsplazita.posplazita.currentTimeMillis()}_${importedCount}",
                            name = name,
                            barcode = barcode,
                            cost = costVal,
                            price3 = p3Val,
                            price1 = p1Val,
                            price2 = p2Val,
                            price4 = p4Val,
                            category = columnMapping["category"]?.let { row.getOrNull(it) } ?: "General",
                            unit = if (isBulkValue || useScaleValue) UnitType.KG else UnitType.PIECE,
                            tax = columnMapping["tax"]?.let { row.getOrNull(it) }?.toDoubleOrNull() ?: 0.0,
                            isBulk = isBulkValue || useScaleValue, // Si usa báscula, forzosamente es a granel
                            useScale = useScaleValue
                        )
                        repository.saveProduct(product)
                        importedCount++
                    }
                }
                _importData.value = emptyList()
                onSuccess(importedCount)
            } catch (e: Exception) {
                _importError.value = "Error al guardar: ${e.message}"
            } finally {
                _isImporting.value = false
            }
        }
    }
}
