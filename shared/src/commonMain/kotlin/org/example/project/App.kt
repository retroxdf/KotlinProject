package com.abtsplazita.posplazita

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.focusable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import com.abtsplazita.posplazita.data.local.createDatabase
import com.abtsplazita.posplazita.domain.CurrentSaleManager
import com.abtsplazita.posplazita.domain.repository.ProductRepository
import com.abtsplazita.posplazita.domain.repository.SaleRepository
import com.abtsplazita.posplazita.ui.CheckoutScreen
import com.abtsplazita.posplazita.ui.PosMainScreen
import com.abtsplazita.posplazita.ui.PosViewModel
import com.abtsplazita.posplazita.ui.history.CashOutScreen
import com.abtsplazita.posplazita.ui.history.HistoryModule
import com.abtsplazita.posplazita.ui.history.HistoryViewModel
import com.abtsplazita.posplazita.ui.history.getRealPrinterManager
import com.abtsplazita.posplazita.ui.inventory.InventoryModule
import com.abtsplazita.posplazita.ui.inventory.InventoryViewModel
import com.abtsplazita.posplazita.ui.inventory.RestockViewModel
import com.abtsplazita.posplazita.ui.inventory.RestockScreen
import com.abtsplazita.posplazita.ui.purchases.PurchaseModule
import com.abtsplazita.posplazita.ui.purchases.AdvancedPurchaseModule
import com.abtsplazita.posplazita.ui.purchases.PurchaseViewModel
import com.abtsplazita.posplazita.ui.products.ProductModule
import com.abtsplazita.posplazita.domain.repository.StockMovementRepository
import com.abtsplazita.posplazita.ui.products.ProductViewModel
import com.abtsplazita.posplazita.ui.contabilidad.ContabilidadViewModel
import com.abtsplazita.posplazita.ui.contabilidad.ContabilidadModule
import com.abtsplazita.posplazita.domain.repository.EmployeeRepository
import com.abtsplazita.posplazita.domain.repository.SettingsRepository
import com.abtsplazita.posplazita.data.remote.FirebaseManager
import com.abtsplazita.posplazita.data.remote.MercadoPagoManager
import com.abtsplazita.posplazita.ui.peripherals.PeripheralViewModel
import com.abtsplazita.posplazita.ui.peripherals.PeripheralSettingsScreen
import com.abtsplazita.posplazita.ui.customers.CustomerViewModel
import com.abtsplazita.posplazita.ui.customers.CustomerModule
import com.abtsplazita.posplazita.domain.repository.ExpenseRepository
import com.abtsplazita.posplazita.ui.expenses.ExpenseViewModel
import com.abtsplazita.posplazita.ui.expenses.ExpenseModule
import com.abtsplazita.posplazita.ui.dashboard.DashboardViewModel
import com.abtsplazita.posplazita.ui.dashboard.DashboardScreen
import com.abtsplazita.posplazita.domain.repository.CustomerRepository
import com.abtsplazita.posplazita.ui.auth.AuthViewModel
import com.abtsplazita.posplazita.ui.auth.LoginScreen
import com.abtsplazita.posplazita.ui.branches.BranchViewModel
import com.abtsplazita.posplazita.ui.branches.BranchSelectionScreen
import com.abtsplazita.posplazita.domain.repository.UserRepository
import com.abtsplazita.posplazita.domain.repository.BranchRepository
import com.abtsplazita.posplazita.domain.repository.PurchaseRepository
import com.abtsplazita.posplazita.domain.repository.PosTerminalRepository
import com.abtsplazita.posplazita.domain.repository.CashOutRepository
import com.abtsplazita.posplazita.domain.repository.CashMovementRepository
import com.abtsplazita.posplazita.domain.repository.PreCutRepository
import com.abtsplazita.posplazita.domain.repository.SupplierRepository
import com.abtsplazita.posplazita.domain.repository.PermissionRepository
import com.abtsplazita.posplazita.domain.Permission
import com.abtsplazita.posplazita.domain.PermissionLevel
import com.abtsplazita.posplazita.ui.suppliers.SupplierViewModel
import com.abtsplazita.posplazita.ui.suppliers.SupplierModule
import com.abtsplazita.posplazita.ui.users.UserViewModel
import com.abtsplazita.posplazita.ui.users.UserModule
import com.abtsplazita.posplazita.domain.Branch
import com.abtsplazita.posplazita.domain.CashMovementType
import com.abtsplazita.posplazita.domain.repository.PurchaseUnitRepository
import com.abtsplazita.posplazita.domain.repository.DeletionLogRepository
import com.abtsplazita.posplazita.domain.repository.ProductReturnRepository
import androidx.compose.ui.tooling.preview.Preview
import io.kamel.core.config.KamelConfig
import io.kamel.core.config.Core
import io.kamel.core.config.takeFrom
import io.kamel.image.config.*

@Composable
@Preview
fun App() {
    val kamelConfig = remember {
        KamelConfig {
            takeFrom(KamelConfig.Core)
            imageBitmapDecoder()
            imageVectorDecoder()
            svgDecoder()
        }
    }
    
    CompositionLocalProvider(LocalKamelConfig provides kamelConfig) {
        AppContent()
    }
}

@Composable
fun AppContent() {
    val database = remember { createDatabase() }
    
    // Inicializar Firebase antes de usar el Manager
    LaunchedEffect(Unit) {
        initializeFirebase()
    }

    val firebaseManager = remember { FirebaseManager() }
    val globalScope = rememberCoroutineScope()

    val productRepository = remember { 
        ProductRepository(
            productDao = database.productDao(), 
            inventoryDao = database.inventoryDao(),
            categoryDao = database.categoryDao(),
            taxDao = database.taxDao(),
            stockMovementDao = database.stockMovementDao(),
            firebaseManager = firebaseManager,
            scope = globalScope
        ) // Los productos son globales
    }
    val saleRepository = remember { 
        SaleRepository(
            saleDao = database.saleDao(),
            heldSaleDao = database.heldSaleDao(),
            firebaseManager = firebaseManager
        )
    }
    val userRepository = remember { UserRepository(database.userDao(), database.rolePermissionDao(), firebaseManager, globalScope) }
    val purchaseRepository = remember { 
        PurchaseRepository(
            purchaseDao = database.purchaseDao(),
            productRepository = productRepository
        ) 
    }
    val purchaseUnitRepository = remember { PurchaseUnitRepository(database.purchaseUnitDao()) }
    val branchRepository = remember { 
        BranchRepository(
            branchDao = database.branchDao(),
            firebaseManager = firebaseManager,
            scope = globalScope
        ) // Las sucursales son globales
    }
    val posTerminalRepository = remember { 
        PosTerminalRepository(
            posTerminalDao = database.posTerminalDao(),
            firebaseManager = firebaseManager
        )
    }
    val movementRepository = remember {
        StockMovementRepository(
            movementDao = database.stockMovementDao(),
            firebaseManager = firebaseManager
        )
    }
    val cashMovementRepository = remember { CashMovementRepository(database.cashMovementDao(), firebaseManager) }
    val cashOutRepository = remember { CashOutRepository(database.cashOutDao(), firebaseManager) }
    val preCutRepository = remember { PreCutRepository(database.preCutDao(), firebaseManager) }
    val deletionLogRepository = remember { DeletionLogRepository(database.deletionLogDao(), firebaseManager) }
    val productReturnRepository = remember { ProductReturnRepository(database.productReturnDao(), firebaseManager) }
    val expenseRepository = remember { ExpenseRepository(database.expenseDao()) }
    val promotionRepository = remember { com.abtsplazita.posplazita.domain.repository.PromotionRepository(database.promotionDao(), firebaseManager) }
    val permissionRepository = remember { PermissionRepository(database.rolePermissionDao()) }
    val supplierRepository = remember { 
        SupplierRepository(
            supplierDao = database.supplierDao(),
            paymentDao = database.supplierPaymentDao(),
            productSupplierDao = database.productSupplierDao()
        ) 
    }
    val customerRepository = remember { 
        CustomerRepository(
            customerDao = database.customerDao(),
            paymentDao = database.customerPaymentDao(),
            specialPriceDao = database.customerProductPriceDao(),
            firebaseManager = firebaseManager,
            scope = globalScope
        )
    }
    val employeeRepository = remember {
        EmployeeRepository(
            employeeDao = database.employeeDao(),
            scheduleDao = database.scheduleDao(),
            loanDao = database.loanDao(),
            absenceReplacementDao = database.absenceReplacementDao(),
            cashBoxDao = database.cashBoxDao(),
            transactionDao = database.contaplaTransactionDao(),
            corteDao = database.corteContaplaDao(),
            paymentRecordDao = database.paymentRecordDao(),
            attendanceDao = database.attendanceDao()
        )
    }
    val settingsRepository = remember { SettingsRepository(database.appSettingsDao()) }
    
    val mercadoPagoManager = remember { MercadoPagoManager() }
    val printerManager = remember { getRealPrinterManager() }
    val currentSaleManager = remember { CurrentSaleManager(settingsRepository, globalScope) }
    
    val syncManager = remember { 
        com.abtsplazita.posplazita.data.SyncManager(
            saleRepository, 
            cashMovementRepository, 
            productRepository, 
            branchRepository,
            userRepository,
            customerRepository,
            settingsRepository,
            globalScope
        ) 
    }
    
    LaunchedEffect(Unit) {
        syncManager.startAutoSync()
    }
    
    val authViewModel = remember { AuthViewModel(userRepository, employeeRepository, permissionRepository, settingsRepository) }
    val branchViewModel = remember { BranchViewModel(branchRepository) }

    // --- LIMPIEZA INICIAL (SOLO UNA VEZ PARA BASE LIMPIA) ---
    LaunchedEffect(database) {
        val hasCleared = settingsRepository.getSetting("db_initial_clear_v4") == "true"
        if (!hasCleared) {
            println("APP: Realizando limpieza inicial de base de datos...")
            try {
                database.clearAllTablesManual()
                // Asegurar que el admin exista tras la limpieza
                userRepository.initializeAdmin()
                settingsRepository.saveSetting("db_initial_clear_v4", "true")
                println("APP: Base de datos limpia y admin restaurado.")
            } catch (e: Exception) {
                println("APP: Error al limpiar BD: ${e.message}")
            }
        }
    }

    // --- REFRESH INICIAL ---
    // Ahora lo maneja SyncManager de forma inteligente (Local-First)

    
    var selectedBranch by remember { mutableStateOf<Branch?>(null) }
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

    if (!isLoggedIn) {
        LoginScreen(authViewModel)
        return
    }

    val currentBranch = selectedBranch
    if (currentBranch == null) {
        BranchSelectionScreen(
            viewModel = branchViewModel,
            onBranchSelected = { selectedBranch = it },
            onLogout = { authViewModel.logout() }
        )
        return
    }

    val branchId = currentBranch.id
    
    // Configurar recuperación de carrito al cambiar de sucursal
    // Y activar sincronización aislada por sucursal
    LaunchedEffect(branchId) {
        if (branchId.isNotBlank()) {
            currentSaleManager.setBranchId(branchId)
            syncManager.setBranchId(branchId)
            // La sincronización pesada ahora la maneja SyncManager cada 2 horas
            // Solo refrescamos terminales que es algo muy ligero
            try {
                posTerminalRepository.refreshTerminals(branchId)
            } catch (e: Exception) {}
        }
    }
    
    val posViewModel = remember(branchId) { 
        PosViewModel(
            repository = productRepository, 
            saleRepository = saleRepository, 
            customerRepository = customerRepository,
            terminalRepository = posTerminalRepository,
            userRepository = userRepository,
            settingsRepository = settingsRepository,
            cashMovementRepository = cashMovementRepository,
            cashOutRepository = cashOutRepository,
            preCutRepository = preCutRepository,
            employeeRepository = employeeRepository,
            mercadoPagoManager = mercadoPagoManager,
            currentSaleManager = currentSaleManager, 
            branchId = branchId, 
            promotionRepository = promotionRepository,
            deletionLogRepository = deletionLogRepository,
            productReturnRepository = productReturnRepository,
            printerManager = printerManager,
            firebaseManager = firebaseManager
        ) 
    }
    val prodVM: com.abtsplazita.posplazita.ui.products.ProductViewModel = remember(branchId) { 
        com.abtsplazita.posplazita.ui.products.ProductViewModel(productRepository, branchRepository, movementRepository, userRepository, branchId) 
    }
    val purchaseViewModel = remember(branchId) { 
        PurchaseViewModel(
            productRepository, 
            purchaseRepository, 
            supplierRepository, 
            cashMovementRepository,
            purchaseUnitRepository,
            branchId
        ) 
    }
    val historyViewModel = remember(branchId) { 
        HistoryViewModel(
            saleRepository = saleRepository, 
            purchaseRepository = purchaseRepository,
            productRepository = productRepository,
            terminalRepository = posTerminalRepository,
            cashOutRepository = cashOutRepository,
            cashMovementRepository = cashMovementRepository,
            preCutRepository = preCutRepository,
            deletionLogRepository = deletionLogRepository,
            productReturnRepository = productReturnRepository,
            supplierRepository = supplierRepository,
            branchRepository = branchRepository,
            printerManager = printerManager, 
            _branchId = branchId
        ) 
    }
    val inventoryViewModel = remember(branchId) { InventoryViewModel(productRepository, branchRepository, branchId) }
    val restockViewModel = remember(branchId) { RestockViewModel(productRepository, saleRepository, movementRepository, branchId) }
    val supplierViewModel = remember(branchId) { 
        SupplierViewModel(
            repository = supplierRepository, 
            purchaseRepository = purchaseRepository,
            cashMovementRepository = cashMovementRepository,
            saleRepository = saleRepository,
            cashOutRepository = cashOutRepository,
            branchId = branchId
        ) 
    }
    val userViewModel = remember(branchId) { UserViewModel(userRepository, employeeRepository, permissionRepository, branchId) }
    val promotionViewModel = remember { com.abtsplazita.posplazita.ui.peripherals.PromotionViewModel(promotionRepository, productRepository) }
    val customerViewModel = remember(branchId) { 
        CustomerViewModel(
            repository = customerRepository, 
            productRepository = productRepository, 
            saleRepository = saleRepository,
            cashMovementRepository = cashMovementRepository,
            printerManager = printerManager,
            branchId = branchId
        ) 
    }
    val contabilidadViewModel = remember(branchId) { 
        ContabilidadViewModel(employeeRepository, userRepository, branchRepository, expenseRepository, settingsRepository, branchId) 
    }
    val expenseViewModel = remember(branchId) { ExpenseViewModel(expenseRepository, branchId) }
    val dashboardViewModel = remember(branchId) { 
        DashboardViewModel(saleRepository, expenseRepository, productRepository, posTerminalRepository, branchId) 
    }
    val peripheralViewModel = remember(branchId) { 
        PeripheralViewModel(
            settingsRepository = settingsRepository, 
            printerManager = printerManager, 
            terminalRepository = posTerminalRepository, 
            firebaseManager = firebaseManager,
            mercadoPagoManager = mercadoPagoManager,
            productRepository = productRepository,
            branchId = branchId
        ) 
    }

    val selectedTerminalPos by posViewModel.selectedTerminal.collectAsState()
    val defaultPriceLevel by peripheralViewModel.defaultPriceLevel.collectAsState()
    val allowNegativeStock by peripheralViewModel.allowNegativeStock.collectAsState()
    val addAtTop by peripheralViewModel.addAtTop.collectAsState()
    
    // Sincronizar la terminal de ventas con la de historial y clientes para el cierre y saldos
    LaunchedEffect(selectedTerminalPos) {
        historyViewModel.filterByTerminal(selectedTerminalPos?.id)
        customerViewModel.setTerminalId(selectedTerminalPos?.id)
    }
    
    // Sincronizar preferencias de operatividad
    LaunchedEffect(defaultPriceLevel) {
        val level = defaultPriceLevel
        posViewModel.setDefaultPriceLevel(level)
        prodVM.setDefaultPriceLevel(level)
    }

    LaunchedEffect(allowNegativeStock) {
        posViewModel.setAllowNegativeStock(allowNegativeStock)
    }

    LaunchedEffect(addAtTop) {
        posViewModel.setAddAtTop(addAtTop)
    }

    val userPermissions by authViewModel.userPermissions.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    // Sincronizar permisos y usuario actual
    LaunchedEffect(userPermissions, currentUser) {
        posViewModel.setUserInfo(currentUser, userPermissions)
        historyViewModel.setCurrentUser(currentUser)
        contabilidadViewModel.setUserInfo(currentUser)
        expenseViewModel.setUserInfo(currentUser)
        customerViewModel.setUserInfo(currentUser)
        inventoryViewModel.setUserInfo(currentUser)
        prodVM.setUserInfo(currentUser, userPermissions)
        purchaseViewModel.setUserInfo(currentUser)
        supplierViewModel.setUserInfo(currentUser)
    }

    var currentScreen by remember { mutableStateOf("pos") }
    var isMenuExpanded by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    val mainFocusRequester = remember { FocusRequester() }
    val navigationItems = listOf(
        NavigationItem("pos", "Venta", Icons.Default.ShoppingCart, "F1", Permission.MAKE_SALE),
        NavigationItem("customers", "Clientes", Icons.Default.People, "F2", Permission.CUSTOMER_VIEW),
        NavigationItem("products", "Productos", Icons.AutoMirrored.Filled.List, "F3", Permission.PRODUCT_VIEW),
        NavigationItem("purchases", "Compras", Icons.Default.AddShoppingCart, "F4", Permission.MANAGE_PURCHASES),
        NavigationItem("advanced_purchases", "Compras Avanzado", Icons.Default.LibraryAdd, permission = Permission.MANAGE_PURCHASES),
        NavigationItem("inventory", "Inventario", Icons.Default.Star, "F5", Permission.PRODUCT_VIEW),
        NavigationItem("cash_in", "Ent de dinero", Icons.Default.Add, "F6", Permission.MANAGE_CASH_MOVEMENTS),
        NavigationItem("cash_out_move", "Sal de dinero", Icons.Default.Remove, "F7", Permission.MANAGE_CASH_MOVEMENTS),
        NavigationItem("history", "Consultas", Icons.Default.Assessment, "F8", Permission.VIEW_REPORTS),
        NavigationItem("contabilidad", "Contapla (Contab.)", Icons.Default.Payments, "F9", permission = Permission.VIEW_ACCOUNTING),
        NavigationItem("settings", "Ajustes", Icons.Default.Settings, "F10", Permission.MANAGE_SETTINGS),
        NavigationItem("cash_out", "Corte de Caja", Icons.Default.Info, "F11", Permission.PERFORM_CASH_OUT),
        NavigationItem("dashboard", "Panel Control", Icons.Default.Dashboard, "F12", Permission.VIEW_REPORTS),
        NavigationItem("web_orders", "Pedidos Web", Icons.Default.Language, permission = Permission.MAKE_SALE),
        NavigationItem("restock", "Resurtimiento", Icons.Default.AutoFixHigh, permission = Permission.PRODUCT_VIEW),
        NavigationItem("expenses", "Gastos", Icons.Default.MoneyOff, permission = Permission.MANAGE_CASH_MOVEMENTS),
        NavigationItem("suppliers", "Proveedores", Icons.Default.LocalShipping, permission = Permission.SUPPLIER_VIEW),
        NavigationItem("users", "Usuarios", Icons.Default.AccountBox, permission = Permission.MANAGE_USERS),
        NavigationItem("change_branch", "Cambiar Sucursal", Icons.Default.SyncAlt, permission = Permission.MANAGE_SETTINGS),
    ).filter { item ->
        if (item.id == "change_branch") {
            return@filter currentUser?.role == com.abtsplazita.posplazita.domain.Role.SUPER_ADMIN || 
                   currentUser?.role == com.abtsplazita.posplazita.domain.Role.GERENTE
        }
        val p = item.permission
        if (p == null) true
        else {
            val level = userPermissions[p] ?: PermissionLevel.DISABLED
            level != PermissionLevel.DISABLED
        }
    }

    MaterialTheme {
        BoxWithConstraints {
            val isCompact = maxWidth < 600.dp

            val content = @Composable {
                Surface(
                    modifier = Modifier
                        .focusRequester(mainFocusRequester)
                        .focusable()
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown) {
                                // Evitar que los atajos globales interfieran con los módulos que usan teclas F
                                val isDialogSubScreen = currentScreen == "checkout" || currentScreen == "purchases"
                                
                                when (event.key) {
                                    Key.F1 -> {
                                        if (isDialogSubScreen) return@onPreviewKeyEvent false
                                        val level = userPermissions[Permission.MAKE_SALE] ?: PermissionLevel.DISABLED
                                        if (currentScreen != "pos" && level != PermissionLevel.DISABLED) {
                                            currentScreen = "pos"
                                            true
                                        } else false
                                    }
                                    Key.F2 -> {
                                        if (isDialogSubScreen) return@onPreviewKeyEvent false
                                        val level = userPermissions[Permission.CUSTOMER_VIEW] ?: PermissionLevel.DISABLED
                                        if (currentScreen != "customers" && level != PermissionLevel.DISABLED) {
                                            customerViewModel.selectCustomer(null)
                                            currentScreen = "customers"
                                            true
                                        } else false
                                    }
                                    Key.F3 -> { 
                                        if (isDialogSubScreen) return@onPreviewKeyEvent false
                                        val level = userPermissions[Permission.PRODUCT_VIEW] ?: PermissionLevel.DISABLED
                                        if (currentScreen != "products" && level != PermissionLevel.DISABLED) {
                                            prodVM.resetToCatalog()
                                            currentScreen = "products"
                                            true
                                        } else false
                                    }
                                    Key.F4 -> {
                                        // F4 es especial porque se usa para CRÉDITO en checkout y para COMPRAS fuera
                                        if (currentScreen == "checkout") return@onPreviewKeyEvent false
                                        
                                        val level = userPermissions[Permission.MANAGE_PURCHASES] ?: PermissionLevel.DISABLED
                                        if (currentScreen != "purchases" && level != PermissionLevel.DISABLED) {
                                            currentScreen = "purchases"
                                            true
                                        } else false
                                    }
                                    Key.F5 -> {
                                        if (isDialogSubScreen) return@onPreviewKeyEvent false
                                        val level = userPermissions[Permission.PRODUCT_VIEW] ?: PermissionLevel.DISABLED
                                        if (level != PermissionLevel.DISABLED) {
                                            currentScreen = "inventory"
                                            true
                                        } else false
                                    }
                                    Key.F6 -> {
                                        if (isDialogSubScreen) return@onPreviewKeyEvent false
                                        val level = userPermissions[Permission.MANAGE_CASH_MOVEMENTS] ?: PermissionLevel.DISABLED
                                        if (level != PermissionLevel.DISABLED) {
                                            posViewModel.openCashMovementDialog(CashMovementType.IN)
                                            true
                                        } else false
                                    }
                                    Key.F7 -> {
                                        if (isDialogSubScreen) return@onPreviewKeyEvent false
                                        val level = userPermissions[Permission.MANAGE_CASH_MOVEMENTS] ?: PermissionLevel.DISABLED
                                        if (level != PermissionLevel.DISABLED) {
                                            posViewModel.openCashMovementDialog(CashMovementType.OUT)
                                            true
                                        } else false
                                    }
                                    Key.F8 -> {
                                        if (isDialogSubScreen) return@onPreviewKeyEvent false
                                        val level = userPermissions[Permission.VIEW_REPORTS] ?: PermissionLevel.DISABLED
                                        if (level != PermissionLevel.DISABLED) {
                                            currentScreen = "history"
                                            true
                                        } else false
                                    }
                                    Key.F9 -> {
                                        if (isDialogSubScreen) return@onPreviewKeyEvent false
                                        val level = userPermissions[Permission.VIEW_ACCOUNTING] ?: PermissionLevel.DISABLED
                                        if (level != PermissionLevel.DISABLED) {
                                            currentScreen = "contabilidad"
                                            true
                                        } else false
                                    }
                                    Key.F10 -> {
                                        if (isDialogSubScreen) return@onPreviewKeyEvent false
                                        val cUser = authViewModel.currentUser.value
                                        val level = userPermissions[Permission.MANAGE_SETTINGS] ?: PermissionLevel.DISABLED
                                        if (cUser?.role == com.abtsplazita.posplazita.domain.Role.SUPER_ADMIN || level != PermissionLevel.DISABLED) {
                                            currentScreen = "settings"
                                            true
                                        } else false
                                    }
                                    Key.F11 -> {
                                        if (isDialogSubScreen) return@onPreviewKeyEvent false
                                        val level = userPermissions[Permission.PERFORM_CASH_OUT] ?: PermissionLevel.DISABLED
                                        if (level != PermissionLevel.DISABLED) {
                                            currentScreen = "cash_out"
                                            true
                                        } else false
                                    }
                                    else -> false
                                }
                            } else false
                        }
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        if (!isCompact) {
                            Surface(
                                modifier = Modifier
                                    .width(if (isMenuExpanded) 200.dp else 72.dp)
                                    .fillMaxHeight(),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(vertical = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // CABECERA DE MENÚ (SALIR + FLECHA)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = if (isMenuExpanded) Arrangement.SpaceEvenly else Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(onClick = { authViewModel.logout() }) {
                                            Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color.Red)
                                        }
                                        
                                        IconButton(onClick = { isMenuExpanded = !isMenuExpanded }) {
                                            Icon(
                                                if (isMenuExpanded) Icons.AutoMirrored.Filled.ArrowBack else Icons.AutoMirrored.Filled.ArrowForward, 
                                                contentDescription = "Menu"
                                            )
                                        }
                                    }
                                    
                                    Spacer(Modifier.height(8.dp))
                                    
                                    Column(
                                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                                        horizontalAlignment = if (isMenuExpanded) Alignment.Start else Alignment.CenterHorizontally
                                    ) {
                                        navigationItems.forEach { item ->
                                            val isSelected = currentScreen == item.id
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                                    .clickable {
                                                        if (item.id == "change_branch") {
                                                            selectedBranch = null
                                                            return@clickable
                                                        }
                                                        if (item.id == "products") prodVM.resetToCatalog()
                                                        if (item.id == "customers") customerViewModel.selectCustomer(null)
                                                        if (item.id == "cash_in") {
                                                            posViewModel.openCashMovementDialog(CashMovementType.IN)
                                                            return@clickable
                                                        }
                                                        if (item.id == "cash_out_move") {
                                                            posViewModel.openCashMovementDialog(CashMovementType.OUT)
                                                            return@clickable
                                                        }
                                                        currentScreen = item.id
                                                    },
                                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                                shape = MaterialTheme.shapes.small
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = if (isMenuExpanded) Arrangement.Start else Arrangement.Center
                                                ) {
                                                    // MOSTRAR TECLA DE FUNCIÓN DELANTE DEL ICONO
                                                    if (item.shortcut != null) {
                                                        Text(
                                                            text = item.shortcut,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                                            modifier = Modifier.padding(end = if (isMenuExpanded) 8.dp else 4.dp)
                                                        )
                                                    }

                                                    Icon(
                                                        item.icon, 
                                                        null, 
                                                        modifier = Modifier.size(22.dp),
                                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    
                                                    if (isMenuExpanded) {
                                                        Spacer(Modifier.width(12.dp))
                                                        Text(
                                                            text = item.label,
                                                            style = MaterialTheme.typography.labelLarge,
                                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 1,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    
                                    Spacer(Modifier.height(8.dp))
                                    
                                    // AREA DE USUARIO ACTUAL
                                    if (currentUser != null) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                            shape = MaterialTheme.shapes.medium,
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.AccountCircle, 
                                                    null, 
                                                    modifier = Modifier.size(24.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                if (isMenuExpanded) {
                                                    Spacer(Modifier.width(8.dp))
                                                    Column {
                                                        Text(
                                                            text = currentUser!!.username, 
                                                            style = MaterialTheme.typography.labelLarge,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = currentUser!!.role.name, 
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color.Gray
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(Modifier.height(8.dp))
                                    }
                                }
                            }
                        }

                        Scaffold(
                            topBar = {
                                if (isCompact) {
                                    CenterAlignedTopAppBar(
                                        title = { 
                                            val currentLabel = navigationItems.find { it.id == currentScreen }?.label ?: ""
                                            Text(currentLabel) 
                                        },
                                        navigationIcon = {
                                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                                Icon(Icons.Default.Menu, "Menú")
                                            }
                                        },
                                        actions = {
                                            IconButton(onClick = { posViewModel.refreshCatalog() }) {
                                                Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.primary)
                                            }
                                            if (currentScreen == "pos") {
                                                var showPosMenu by remember { mutableStateOf(false) }
                                                Box {
                                                    IconButton(onClick = { showPosMenu = true }) {
                                                        Icon(Icons.Default.MoreVert, "Opciones de Venta")
                                                    }
                                                    DropdownMenu(
                                                        expanded = showPosMenu, 
                                                        onDismissRequest = { showPosMenu = false }
                                                    ) {
                                                        // 1. Traer Cliente
                                                        DropdownMenuItem(
                                                            text = { Text("Traer Cliente") },
                                                            leadingIcon = { Icon(Icons.Default.People, null, tint = Color(0xFF673AB7)) },
                                                            onClick = { showPosMenu = false; posViewModel.openCustomerDialog() }
                                                        )
                                                        // 2. Poner en Espera
                                                        DropdownMenuItem(
                                                            text = { Text("Poner en Espera") }, 
                                                            leadingIcon = { Icon(Icons.Default.Pause, null, tint = Color(0xFFFFA500)) }, 
                                                            onClick = { showPosMenu = false; posViewModel.putSaleOnHold() }
                                                        )
                                                        // 3. Retiro de Efectivo
                                                        DropdownMenuItem(
                                                            text = { Text("Retiro de Efectivo") },
                                                            leadingIcon = { Icon(Icons.Default.Atm, null, tint = Color(0xFF2196F3)) },
                                                            onClick = { showPosMenu = false; posViewModel.openWithdrawalDialog() }
                                                        )
                                                        // 4. Tickets Guardados
                                                        DropdownMenuItem(
                                                            text = { Text("Tickets Guardados") }, 
                                                            leadingIcon = { Icon(Icons.Default.Save, null, tint = Color(0xFF2196F3)) }, 
                                                            onClick = { showPosMenu = false; posViewModel.openHeldSalesDialog() }
                                                        )
                                                        // 5. Devolución / Cambio
                                                        DropdownMenuItem(
                                                            text = { Text("Devolución / Cambio") }, 
                                                            leadingIcon = { Icon(Icons.Default.SyncAlt, null, tint = Color(0xFFE91E63)) }, 
                                                            onClick = { showPosMenu = false; posViewModel.openReturnDialog() }
                                                        )
                                                        HorizontalDivider()
                                                        // 6. Comentarios
                                                            DropdownMenuItem(
                                                                text = { Text("Comentarios Ticket") }, 
                                                                leadingIcon = { Icon(Icons.Default.Comment, null, tint = Color(0xFF2196F3)) }, 
                                                                onClick = { showPosMenu = false; posViewModel.openCommentDialog() }
                                                            )
                                                        HorizontalDivider()
                                                        // 7. Abrir Cajón
                                                        DropdownMenuItem(
                                                            text = { Text("Abrir Cajón") }, 
                                                            leadingIcon = { Icon(Icons.Default.LockOpen, null, tint = Color.Gray) }, 
                                                            onClick = { showPosMenu = false; posViewModel.openCashDrawer() }
                                                        )
                                                        // 8. Reimprimir Última
                                                        DropdownMenuItem(
                                                            text = { Text("Reimprimir Última") }, 
                                                            leadingIcon = { Icon(Icons.Default.Print, null, tint = Color(0xFF4CAF50)) }, 
                                                            onClick = { showPosMenu = false; posViewModel.reprintLastSale() }
                                                        )
                                                        HorizontalDivider()
                                                        // 9. Abonos
                                                        DropdownMenuItem(
                                                            text = { Text("Abonos / Deuda") },
                                                            leadingIcon = { Icon(Icons.Default.Payments, null, tint = Color(0xFF4CAF50)) },
                                                            onClick = { showPosMenu = false; posViewModel.openDebtPaymentDialog() }
                                                        )
                                                        // 10. Recargas
                                                        DropdownMenuItem(
                                                            text = { Text("Recargas") },
                                                            leadingIcon = { Icon(Icons.Default.FlashOn, null, tint = Color(0xFFFFD700)) },
                                                            onClick = { showPosMenu = false; posViewModel.openRechargeDialog() }
                                                        )
                                                        // 11. Pago Servicios
                                                        DropdownMenuItem(
                                                            text = { Text("Pago Servicios") },
                                                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, null, tint = Color(0xFF2196F3)) },
                                                            onClick = { showPosMenu = false; posViewModel.openServiceDialog() }
                                                        )
                                                        HorizontalDivider()
                                                        // 12. Producto Común
                                                        DropdownMenuItem(
                                                            text = { Text("Producto Común") },
                                                            leadingIcon = { Icon(Icons.Default.FlashOn, null, tint = Color(0xFF64DD17)) },
                                                            onClick = { 
                                                                showPosMenu = false
                                                                posViewModel.openCommonProductDialog()
                                                            }
                                                        )
                                                        // 13. Precorte
                                                        DropdownMenuItem(
                                                            text = { Text("Realizar Precorte") }, 
                                                            leadingIcon = { Icon(Icons.Default.Analytics, null, tint = Color(0xFF2196F3)) }, 
                                                            onClick = { showPosMenu = false; posViewModel.openPreCutDialog() }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        ) { padding ->
                            Surface(modifier = Modifier.padding(padding)) {
                                when (currentScreen) {
                                    "dashboard" -> DashboardScreen(dashboardViewModel)
                                    "pos" -> {
                                        val adImages by peripheralViewModel.adImages.collectAsState()
                                        val currentAdIndex by peripheralViewModel.currentAdIndex.collectAsState()
                                        
                                        LaunchedEffect(adImages) {
                                            while (adImages.size > 1) {
                                                kotlinx.coroutines.delay(120_000)
                                                peripheralViewModel.nextAd()
                                            }
                                        }

                                        val currentUser by authViewModel.currentUser.collectAsState()
                                        
                                        PosMainScreen(
                                            viewModel = posViewModel,
                                            userViewModel = userViewModel,
                                            adImageUrl = adImages.getOrNull(currentAdIndex) ?: "",
                                            currentUserId = currentUser?.username ?: "admin",
                                            onLogout = { authViewModel.logout() },
                                            onNavigateToCheckout = { 
                                                posViewModel.prepareCheckout()
                                                currentScreen = "checkout" 
                                            },
                                            onNavigateToHistory = { currentScreen = "history" },
                                            onNavigateToSettings = { currentScreen = "settings" },
                                            onNavigateToInventory = { currentScreen = "inventory" }
                                        )
                                    }
                                    "checkout" -> CheckoutScreen(
                                        viewModel = posViewModel,
                                        onCancel = { currentScreen = "pos" }
                                    )
                                    "web_orders" -> com.abtsplazita.posplazita.ui.history.WebOrdersScreen(
                                        viewModel = posViewModel,
                                        onBack = { currentScreen = "pos" },
                                        onNavigateToPos = { currentScreen = "pos" }
                                    )
                                    "purchases" -> PurchaseModule(
                                        viewModel = purchaseViewModel
                                    )
                                    "advanced_purchases" -> AdvancedPurchaseModule(
                                        viewModel = purchaseViewModel
                                    )
                                    "customers" -> CustomerModule(customerViewModel)
                                    "products" -> ProductModule(prodVM)
                                    "expenses" -> ExpenseModule(expenseViewModel, onBack = { currentScreen = "dashboard" })
                                    "history" -> HistoryModule(historyViewModel, onLogout = { authViewModel.logout() })
                                    "inventory" -> InventoryModule(inventoryViewModel)
                                    "restock" -> RestockScreen(restockViewModel, onBack = { currentScreen = "inventory" })
                                    "cash_out" -> {
                                        val showTotal by peripheralViewModel.showCashOutTotal.collectAsState()
                                        CashOutScreen(
                                            viewModel = historyViewModel, 
                                            onLogout = { authViewModel.logout() },
                                            showTotalPreference = showTotal, 
                                            onNavigateToPos = { currentScreen = "pos" }
                                        )
                                    }
                                    "suppliers" -> SupplierModule(supplierViewModel)
                                    "users" -> UserModule(userViewModel)
                                    "contabilidad" -> ContabilidadModule(contabilidadViewModel)
                                    "settings" -> PeripheralSettingsScreen(peripheralViewModel, posViewModel, userViewModel, promotionViewModel, productRepository)
                                }
                            }
                        }
                    }
                }
                
                LaunchedEffect(currentScreen) {
                    mainFocusRequester.requestFocus()
                }
            }

            if (isCompact) {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                Spacer(Modifier.height(12.dp))
                                Text("MENÚ PRINCIPAL", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.labelMedium)
                                navigationItems.forEach { item ->
                                    NavigationDrawerItem(
                                        icon = { Icon(item.icon, null) },
                                        label = { Text(item.label) },
                                        selected = currentScreen == item.id,
                                        onClick = {
                                            if (item.id == "change_branch") {
                                                selectedBranch = null
                                                scope.launch { drawerState.close() }
                                                return@NavigationDrawerItem
                                            }
                                            if (item.id == "products") prodVM.resetToCatalog()
                                            if (item.id == "customers") customerViewModel.selectCustomer(null)
                                            if (item.id == "cash_in") {
                                                posViewModel.openCashMovementDialog(CashMovementType.IN)
                                                scope.launch { drawerState.close() }
                                                return@NavigationDrawerItem
                                            }
                                            if (item.id == "cash_out_move") {
                                                posViewModel.openCashMovementDialog(CashMovementType.OUT)
                                                scope.launch { drawerState.close() }
                                                return@NavigationDrawerItem
                                            }
                                            currentScreen = item.id
                                            scope.launch { drawerState.close() }
                                        },
                                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                    )
                                }
                                NavigationDrawerItem(
                                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color.Red) },
                                    label = { Text("CERRAR SESIÓN", color = Color.Red, fontWeight = FontWeight.Bold) },
                                    selected = false,
                                    onClick = {
                                        scope.launch { drawerState.close() }
                                        authViewModel.logout()
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )
                                Spacer(Modifier.height(24.dp))
                            }
                        }
                    }
                ) {
                    content()
                }
            } else {
                content()
            }
        }
    }
}

data class NavigationItem(
    val id: String, 
    val label: String, 
    val icon: androidx.compose.ui.graphics.vector.ImageVector, 
    val shortcut: String? = null,
    val permission: Permission? = null
)
