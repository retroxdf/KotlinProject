package com.abtsplazita.posplazita

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.abtsplazita.posplazita.data.local.createDatabase
import com.abtsplazita.posplazita.domain.*
import com.abtsplazita.posplazita.domain.repository.*
import com.abtsplazita.posplazita.ui.*
import com.abtsplazita.posplazita.ui.auth.*
import com.abtsplazita.posplazita.ui.branches.*
import com.abtsplazita.posplazita.ui.customers.*
import com.abtsplazita.posplazita.ui.dashboard.*
import com.abtsplazita.posplazita.ui.expenses.*
import com.abtsplazita.posplazita.ui.history.*
import com.abtsplazita.posplazita.ui.inventory.*
import com.abtsplazita.posplazita.ui.peripherals.*
import com.abtsplazita.posplazita.ui.products.*
import com.abtsplazita.posplazita.ui.purchases.*
import com.abtsplazita.posplazita.ui.suppliers.*
import com.abtsplazita.posplazita.ui.users.*
import com.abtsplazita.posplazita.ui.contabilidad.*
import com.abtsplazita.posplazita.data.remote.FirebaseManager
import com.abtsplazita.posplazita.data.remote.MercadoPagoManager
import com.abtsplazita.posplazita.data.SyncManager
import com.abtsplazita.posplazita.domain.formatPrice
import com.abtsplazita.posplazita.formatTimestamp
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
    
    LaunchedEffect(Unit) {
        initializeFirebase()
    }

    val firebaseManager = remember { FirebaseManager() }
    val globalScope = rememberCoroutineScope()

    val productRepository = remember { ProductRepository(database.productDao(), database.inventoryDao(), database.stockMovementDao(), database.categoryDao(), database.taxDao(), firebaseManager, globalScope) }
    val saleRepository = remember { SaleRepository(database.saleDao(), database.heldSaleDao(), firebaseManager) }
    val userRepository = remember { UserRepository(database.userDao(), database.rolePermissionDao(), firebaseManager, globalScope) }
    val purchaseRepository = remember { PurchaseRepository(database.purchaseDao(), productRepository) }
    val purchaseUnitRepository = remember { PurchaseUnitRepository(database.purchaseUnitDao()) }
    val branchRepository = remember { BranchRepository(database.branchDao(), firebaseManager, globalScope) }
    val posTerminalRepository = remember { PosTerminalRepository(database.posTerminalDao(), firebaseManager) }
    val movementRepository = remember { StockMovementRepository(database.stockMovementDao(), firebaseManager) }
    val cashMovementRepository = remember { CashMovementRepository(database.cashMovementDao(), firebaseManager) }
    val cashOutRepository = remember { CashOutRepository(database.cashOutDao(), firebaseManager) }
    val preCutRepository = remember { PreCutRepository(database.preCutDao(), firebaseManager) }
    val expenseRepository = remember { ExpenseRepository(database.expenseDao()) }
    val promotionRepository = remember { PromotionRepository(database.promotionDao(), firebaseManager) }
    val permissionRepository = remember { PermissionRepository(database.rolePermissionDao()) }
    val supplierRepository = remember { SupplierRepository(database.supplierDao(), database.supplierPaymentDao(), database.productSupplierDao()) }
    val customerRepository = remember { CustomerRepository(database.customerDao(), database.customerPaymentDao(), database.customerProductPriceDao(), firebaseManager, globalScope) }
    val employeeRepository = remember { EmployeeRepository(database.employeeDao(), database.scheduleDao(), database.loanDao(), database.absenceReplacementDao(), database.cashBoxDao(), database.contaplaTransactionDao(), database.corteContaplaDao(), database.paymentRecordDao(), database.attendanceDao(), firebaseManager) }
    val settingsRepository = remember { SettingsRepository(database.appSettingsDao()) }
    val deletionLogRepository = remember { DeletionLogRepository(database.deletionLogDao(), firebaseManager) }
    val productReturnRepository = remember { ProductReturnRepository(database.productReturnDao(), firebaseManager) }
    
    val mercadoPagoManager = remember { MercadoPagoManager() }
    val printerManager = remember { getRealPrinterManager() }
    val scaleManager = remember { getScaleManager() }
    val currentSaleManager = remember { CurrentSaleManager(settingsRepository, globalScope) }
    
    val syncManager = remember { SyncManager(saleRepository, cashMovementRepository, productRepository, branchRepository, userRepository, employeeRepository, customerRepository, settingsRepository, globalScope) }
    val updateViewModel = remember { UpdateViewModel() }
    
    LaunchedEffect(Unit) {
        syncManager.startAutoSync()
        updateViewModel.checkForUpdates()
    }
    
    val authViewModel = remember { AuthViewModel(userRepository, employeeRepository, permissionRepository, settingsRepository) }
    val branchViewModel = remember { BranchViewModel(branchRepository) }
    val peripheralViewModel = remember { PeripheralViewModel(settingsRepository, printerManager, posTerminalRepository, firebaseManager, mercadoPagoManager, productRepository, scaleManager, "") }

    LaunchedEffect(database) {
        if (settingsRepository.getSetting("db_initial_clear_v5") != "true") {
            try {
                println("APP: Realizando limpieza de mantenimiento y re-sincronización...")
                database.clearAllTablesManual()
                userRepository.initializeAdmin()
                
                // Resetear banderas de sincronización para forzar descarga total
                settingsRepository.saveSetting("is_initial_sync_completed", "false")
                // Borrar banderas de inventario por sucursal (simplificado reset de settings)
                
                settingsRepository.saveSetting("db_initial_clear_v5", "true")
            } catch (e: Exception) {}
        }
    }

    var selectedBranch by remember { mutableStateOf<Branch?>(null) }
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val appLogoUrl by peripheralViewModel.appLogoUrl.collectAsState()

    if (!isLoggedIn) {
        LoginScreen(authViewModel, appLogoUrl)
        return
    }

    val currentBranch = selectedBranch
    if (currentBranch == null) {
        BranchSelectionScreen(viewModel = branchViewModel, onBranchSelected = { selectedBranch = it }, onLogout = { authViewModel.logout() })
        return
    }

    val branchId = currentBranch.id
    LaunchedEffect(branchId) {
        if (branchId.isNotBlank()) {
            currentSaleManager.setBranchId(branchId)
            syncManager.setBranchId(branchId)
            try { posTerminalRepository.refreshTerminals(branchId) } catch (e: Exception) {}
        }
    }
    
    val posViewModel = remember(branchId) { PosViewModel(productRepository, saleRepository, customerRepository, posTerminalRepository, userRepository, settingsRepository, cashMovementRepository, cashOutRepository, preCutRepository, employeeRepository, mercadoPagoManager, currentSaleManager, branchId, promotionRepository, deletionLogRepository, productReturnRepository, printerManager, firebaseManager, scaleManager) }
    val prodVM = remember(branchId) { ProductViewModel(productRepository, branchRepository, movementRepository, userRepository, branchId) }
    val purchaseViewModel = remember(branchId) { PurchaseViewModel(productRepository, purchaseRepository, supplierRepository, cashMovementRepository, purchaseUnitRepository, branchId) }
    val historyViewModel = remember(branchId) { 
        HistoryViewModel(
            saleRepository, purchaseRepository, productRepository, customerRepository, 
            posTerminalRepository, cashOutRepository, cashMovementRepository, preCutRepository, 
            deletionLogRepository, productReturnRepository, supplierRepository, 
            branchRepository, employeeRepository, settingsRepository, printerManager, branchId
        ) 
    }
    val inventoryViewModel = remember(branchId) { InventoryViewModel(productRepository, branchRepository, branchId) }
    val restockViewModel = remember(branchId) { RestockViewModel(productRepository, saleRepository, movementRepository, branchId) }
    val supplierViewModel = remember(branchId) { SupplierViewModel(supplierRepository, purchaseRepository, cashMovementRepository, saleRepository, cashOutRepository, branchId) }
    val userViewModel = remember(branchId) { UserViewModel(userRepository, employeeRepository, permissionRepository, branchId) }
    val promotionViewModel = remember { PromotionViewModel(promotionRepository, productRepository) }
    val customerViewModel = remember(branchId) { CustomerViewModel(customerRepository, productRepository, saleRepository, cashMovementRepository, printerManager, branchId) }
    val contabilidadViewModel = remember(branchId) { ContabilidadViewModel(employeeRepository, userRepository, branchRepository, expenseRepository, settingsRepository, branchId) }
    val expenseViewModel = remember(branchId) { ExpenseViewModel(expenseRepository, branchId) }
    val dashboardViewModel = remember(branchId) { DashboardViewModel(saleRepository, expenseRepository, productRepository, posTerminalRepository, branchId) }
    val branchPeripheralViewModel = remember(branchId) { PeripheralViewModel(settingsRepository, printerManager, posTerminalRepository, firebaseManager, mercadoPagoManager, productRepository, scaleManager, branchId) }

    val selectedTerminalPos by posViewModel.selectedTerminal.collectAsState()
    val defaultPriceLevel by branchPeripheralViewModel.defaultPriceLevel.collectAsState()
    val allowNegativeStock by branchPeripheralViewModel.allowNegativeStock.collectAsState()
    val addAtTop by branchPeripheralViewModel.addAtTop.collectAsState()
    
    LaunchedEffect(selectedTerminalPos) {
        historyViewModel.filterByTerminal(selectedTerminalPos?.id)
        customerViewModel.setTerminalId(selectedTerminalPos?.id)
    }
    
    LaunchedEffect(defaultPriceLevel) {
        posViewModel.setDefaultPriceLevel(defaultPriceLevel)
        prodVM.setDefaultPriceLevel(defaultPriceLevel)
    }

    LaunchedEffect(allowNegativeStock) { posViewModel.setAllowNegativeStock(allowNegativeStock) }
    LaunchedEffect(addAtTop) { posViewModel.setAddAtTop(addAtTop) }

    val userPermissions by authViewModel.userPermissions.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

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
            return@filter currentUser?.role == Role.SUPER_ADMIN || currentUser?.role == Role.GERENTE
        }
        val p = item.permission
        if (p == null) true
        else (userPermissions[p] ?: PermissionLevel.DISABLED) != PermissionLevel.DISABLED
    }

    MaterialTheme {
        BoxWithConstraints {
            val isCompact = maxWidth < 600.dp
            val content = @Composable {
                Surface(
                    modifier = Modifier.focusRequester(mainFocusRequester).focusable().onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            val isDialogSubScreen = currentScreen == "checkout" || currentScreen == "purchases"
                            when (event.key) {
                                Key.F1 -> if (!isDialogSubScreen && currentScreen != "pos" && (userPermissions[Permission.MAKE_SALE] ?: PermissionLevel.DISABLED) != PermissionLevel.DISABLED) { currentScreen = "pos"; true } else false
                                Key.F2 -> if (!isDialogSubScreen && currentScreen != "customers" && (userPermissions[Permission.CUSTOMER_VIEW] ?: PermissionLevel.DISABLED) != PermissionLevel.DISABLED) { customerViewModel.selectCustomer(null); currentScreen = "customers"; true } else false
                                Key.F3 -> if (!isDialogSubScreen && currentScreen != "products" && (userPermissions[Permission.PRODUCT_VIEW] ?: PermissionLevel.DISABLED) != PermissionLevel.DISABLED) { prodVM.resetToCatalog(); currentScreen = "products"; true } else false
                                Key.F4 -> if (currentScreen != "checkout" && currentScreen != "purchases" && (userPermissions[Permission.MANAGE_PURCHASES] ?: PermissionLevel.DISABLED) != PermissionLevel.DISABLED) { currentScreen = "purchases"; true } else false
                                Key.F5 -> if (!isDialogSubScreen && (userPermissions[Permission.PRODUCT_VIEW] ?: PermissionLevel.DISABLED) != PermissionLevel.DISABLED) { currentScreen = "inventory"; true } else false
                                Key.F6 -> if (!isDialogSubScreen && (userPermissions[Permission.MANAGE_CASH_MOVEMENTS] ?: PermissionLevel.DISABLED) != PermissionLevel.DISABLED) { posViewModel.openCashMovementDialog(CashMovementType.IN); true } else false
                                Key.F7 -> if (!isDialogSubScreen && (userPermissions[Permission.MANAGE_CASH_MOVEMENTS] ?: PermissionLevel.DISABLED) != PermissionLevel.DISABLED) { posViewModel.openCashMovementDialog(CashMovementType.OUT); true } else false
                                Key.F8 -> if (!isDialogSubScreen && (userPermissions[Permission.VIEW_REPORTS] ?: PermissionLevel.DISABLED) != PermissionLevel.DISABLED) { currentScreen = "history"; true } else false
                                Key.F10 -> if (!isDialogSubScreen && (currentUser?.role == Role.SUPER_ADMIN || (userPermissions[Permission.MANAGE_SETTINGS] ?: PermissionLevel.DISABLED) != PermissionLevel.DISABLED)) { currentScreen = "settings"; true } else false
                                Key.F11 -> if (!isDialogSubScreen && (userPermissions[Permission.PERFORM_CASH_OUT] ?: PermissionLevel.DISABLED) != PermissionLevel.DISABLED) { currentScreen = "cash_out"; true } else false
                                else -> false
                            }
                        } else false
                    }
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        if (!isCompact) {
                            Surface(modifier = Modifier.width(if (isMenuExpanded) 200.dp else 72.dp).fillMaxHeight(), color = MaterialTheme.colorScheme.surfaceVariant) {
                                Column(modifier = Modifier.fillMaxSize().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isMenuExpanded) Arrangement.SpaceEvenly else Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { authViewModel.logout() }) { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color.Red) }
                                        IconButton(onClick = { isMenuExpanded = !isMenuExpanded }) { Icon(if (isMenuExpanded) Icons.AutoMirrored.Filled.ArrowBack else Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Menu") }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()), horizontalAlignment = if (isMenuExpanded) Alignment.Start else Alignment.CenterHorizontally) {
                                        navigationItems.forEach { item ->
                                            val isSelected = currentScreen == item.id
                                            Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 1.dp).clickable {
                                                if (item.id == "change_branch") { selectedBranch = null; return@clickable }
                                                if (item.id == "products") prodVM.resetToCatalog()
                                                if (item.id == "customers") customerViewModel.selectCustomer(null)
                                                if (item.id == "cash_in") { posViewModel.openCashMovementDialog(CashMovementType.IN); return@clickable }
                                                if (item.id == "cash_out_move") { posViewModel.openCashMovementDialog(CashMovementType.OUT); return@clickable }
                                                currentScreen = item.id
                                            }, color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, shape = MaterialTheme.shapes.small) {
                                                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = if (isMenuExpanded) Arrangement.Start else Arrangement.Center) {
                                                    if (item.shortcut != null) { Text(text = item.shortcut, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray, modifier = Modifier.padding(end = if (isMenuExpanded) 8.dp else 4.dp)) }
                                                    Icon(item.icon, null, modifier = Modifier.size(22.dp), tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                                    if (isMenuExpanded) {
                                                        Spacer(Modifier.width(12.dp))
                                                        Text(text = item.label, style = MaterialTheme.typography.labelLarge, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    if (currentUser != null) {
                                        val showUserPanel by authViewModel.showUserPanel.collectAsState()
                                        Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp).clickable { authViewModel.openUserPanel() }) {
                                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                                                if (isMenuExpanded) {
                                                    Spacer(Modifier.width(8.dp))
                                                    Column {
                                                        Text(text = currentUser!!.username, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                                        Text(text = currentUser!!.role.name, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                                    }
                                                }
                                            }
                                        }
                                        if (showUserPanel) { UserPanelFullScreen(authViewModel, onDismiss = { authViewModel.closeUserPanel() }) }
                                    }
                                    
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        text = "v1.0.7",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                            }
                        }

                        Scaffold(
                            topBar = {
                                if (isCompact) {
                                    CenterAlignedTopAppBar(
                                        title = { val currentLabel = navigationItems.find { it.id == currentScreen }?.label ?: ""; Text(currentLabel) },
                                        navigationIcon = { IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, "Menú") } },
                                        actions = {
                                            IconButton(onClick = { posViewModel.refreshCatalog() }) { Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.primary) }
                                            if (currentScreen == "pos") {
                                                var showPosMenu by remember { mutableStateOf(false) }
                                                Box {
                                                    IconButton(onClick = { showPosMenu = true }) { Icon(Icons.Default.MoreVert, "Opciones") }
                                                    DropdownMenu(expanded = showPosMenu, onDismissRequest = { showPosMenu = false }) {
                                                        DropdownMenuItem(text = { Text("Traer Cliente") }, leadingIcon = { Icon(Icons.Default.People, null, tint = Color(0xFF673AB7)) }, onClick = { showPosMenu = false; posViewModel.openCustomerDialog() })
                                                        DropdownMenuItem(text = { Text("Poner en Espera") }, leadingIcon = { Icon(Icons.Default.Pause, null, tint = Color(0xFFFFA500)) }, onClick = { showPosMenu = false; posViewModel.putSaleOnHold() })
                                                        DropdownMenuItem(text = { Text("Retiro Efectivo") }, leadingIcon = { Icon(Icons.Default.Atm, null, tint = Color(0xFF2196F3)) }, onClick = { showPosMenu = false; posViewModel.openWithdrawalDialog() })
                                                        DropdownMenuItem(text = { Text("Tickets Guardados") }, leadingIcon = { Icon(Icons.Default.Save, null, tint = Color(0xFF2196F3)) }, onClick = { showPosMenu = false; posViewModel.openHeldSalesDialog() })
                                                        DropdownMenuItem(text = { Text("Devolución / Cambio") }, leadingIcon = { Icon(Icons.Default.SyncAlt, null, tint = Color(0xFFE91E63)) }, onClick = { showPosMenu = false; posViewModel.openReturnDialog() })
                                                        HorizontalDivider()
                                                        DropdownMenuItem(text = { Text("Abonos / Deuda") }, leadingIcon = { Icon(Icons.Default.Payments, null, tint = Color(0xFF4CAF50)) }, onClick = { showPosMenu = false; posViewModel.openDebtPaymentDialog() })
                                                        DropdownMenuItem(text = { Text("Realizar Precorte") }, leadingIcon = { Icon(Icons.Default.Analytics, null, tint = Color(0xFF2196F3)) }, onClick = { showPosMenu = false; posViewModel.openPreCutDialog() })
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        ) { padding ->
                            Surface(modifier = Modifier.padding(padding)) {
                                val showCashMovementDialog by posViewModel.showCashMovementDialog.collectAsState()
                                if (showCashMovementDialog != null) {
                                    CashMovementDialog(posViewModel, type = showCashMovementDialog!!)
                                }

                                val showPreCutDialog by posViewModel.showPreCutDialog.collectAsState()
                                if (showPreCutDialog) {
                                    PreCutDialog(posViewModel, currentUserId = currentUser?.username ?: "admin")
                                }

                                when (currentScreen) {
                                    "dashboard" -> DashboardScreen(dashboardViewModel)
                                    "pos" -> {
                                        val adImages by branchPeripheralViewModel.adImages.collectAsState()
                                        val currentAdIndex by branchPeripheralViewModel.currentAdIndex.collectAsState()
                                        LaunchedEffect(adImages) { while (adImages.size > 1) { kotlinx.coroutines.delay(120_000); branchPeripheralViewModel.nextAd() } }
                                        PosMainScreen(viewModel = posViewModel, userViewModel = userViewModel, adImageUrl = adImages.getOrNull(currentAdIndex) ?: "", currentUserId = currentUser?.username ?: "admin", onLogout = { authViewModel.logout() }, onNavigateToCheckout = { posViewModel.prepareCheckout(); currentScreen = "checkout" }, onNavigateToHistory = { currentScreen = "history" }, onNavigateToSettings = { currentScreen = "settings" }, onNavigateToInventory = { currentScreen = "inventory" })
                                    }
                                    "checkout" -> CheckoutScreen(viewModel = posViewModel, onCancel = { currentScreen = "pos" })
                                    "web_orders" -> WebOrdersScreen(viewModel = posViewModel, onBack = { currentScreen = "pos" }, onNavigateToPos = { currentScreen = "pos" })
                                    "purchases" -> PurchaseModule(viewModel = purchaseViewModel)
                                    "advanced_purchases" -> AdvancedPurchaseModule(viewModel = purchaseViewModel)
                                    "customers" -> CustomerModule(customerViewModel)
                                    "products" -> ProductModule(prodVM)
                                    "expenses" -> ExpenseModule(expenseViewModel, onBack = { currentScreen = "dashboard" })
                                    "history" -> HistoryModule(historyViewModel, onLogout = { authViewModel.logout() })
                                    "inventory" -> InventoryModule(inventoryViewModel)
                                    "restock" -> RestockScreen(restockViewModel, onBack = { currentScreen = "inventory" })
                                    "cash_out" -> CashOutScreen(viewModel = historyViewModel, onLogout = { authViewModel.logout() }, showTotalPreference = branchPeripheralViewModel.showCashOutTotal.collectAsState().value, onNavigateToPos = { currentScreen = "pos" })
                                    "suppliers" -> SupplierModule(supplierViewModel)
                                    "users" -> UserModule(userViewModel)
                                    "contabilidad" -> ContabilidadModule(contabilidadViewModel)
                                    "settings" -> PeripheralSettingsScreen(branchPeripheralViewModel, posViewModel, userViewModel, promotionViewModel, productRepository)
                                }
                            }
                        }
                    }
                }
                LaunchedEffect(currentScreen) { mainFocusRequester.requestFocus() }
            }

            // Diálogo de Actualización
            val updateInfo by updateViewModel.updateInfo.collectAsState()
            if (updateInfo != null) {
                UpdateDialog(updateViewModel)
            }

            if (isCompact) {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                Spacer(Modifier.height(12.dp))
                                if (currentUser != null) {
                                    val showUserPanel by authViewModel.showUserPanel.collectAsState()
                                    Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), modifier = Modifier.fillMaxWidth().clickable { authViewModel.openUserPanel() }) {
                                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.AccountCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                                            Spacer(Modifier.width(16.dp))
                                            Column {
                                                Text(currentUser!!.username.uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                Text(currentUser!!.role.name, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                                Text("Ver mi cuenta", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                    if (showUserPanel) { UserPanelFullScreen(authViewModel, onDismiss = { authViewModel.closeUserPanel() }) }
                                }
                                Text("MENÚ PRINCIPAL", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.labelMedium)
                                navigationItems.forEach { item ->
                                    NavigationDrawerItem(
                                        icon = { Icon(item.icon, null) },
                                        label = { Text(item.label) },
                                        selected = currentScreen == item.id,
                                        onClick = {
                                            if (item.id == "change_branch") { selectedBranch = null; scope.launch { drawerState.close() }; return@NavigationDrawerItem }
                                            if (item.id == "products") prodVM.resetToCatalog()
                                            if (item.id == "customers") customerViewModel.selectCustomer(null)
                                            if (item.id == "cash_in") { posViewModel.openCashMovementDialog(CashMovementType.IN); scope.launch { drawerState.close() }; return@NavigationDrawerItem }
                                            if (item.id == "cash_out_move") { posViewModel.openCashMovementDialog(CashMovementType.OUT); scope.launch { drawerState.close() }; return@NavigationDrawerItem }
                                            currentScreen = item.id
                                            scope.launch { drawerState.close() }
                                        },
                                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                    )
                                }
                                NavigationDrawerItem(icon = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color.Red) }, label = { Text("CERRAR SESIÓN", color = Color.Red, fontWeight = FontWeight.Bold) }, selected = false, onClick = { scope.launch { drawerState.close() }; authViewModel.logout() }, modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding))
                                Spacer(Modifier.height(24.dp))
                            }
                        }
                    }
                ) { content() }
            } else { content() }
        }
    }
}

data class NavigationItem(val id: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val shortcut: String? = null, val permission: Permission? = null)

@Composable
fun UserPanelFullScreen(viewModel: AuthViewModel, onDismiss: () -> Unit) {
    val stats by viewModel.userStats.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Surface(color = Color(0xFF0056A0), contentColor = Color.White) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
                        Spacer(Modifier.width(8.dp))
                        Text("MI CUENTA", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }

                if (stats == null) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                } else {
                    val days = listOf("Lun", "Mar", "Mie", "Jue", "Vie", "Sab", "Dom")
                    
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val isSmall = maxWidth < 700.dp
                        
                        @Composable
                        fun ColumnScope.LeftPart() {
                            Surface(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                color = Color(0xFFF5F7FA)
                            ) {
                                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(modifier = Modifier.size(60.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.AccountCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                                            }
                                        }
                                        Spacer(Modifier.width(16.dp))
                                        Column {
                                            Text(currentUser?.username?.uppercase() ?: "", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                                            Text(currentUser?.role?.name ?: "", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                                        }
                                    }

                                    HorizontalDivider()

                                    Text("REGISTRO DE HOY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        InfoRow(label = "ENTRADA REAL", value = stats!!.checkInTime?.let { formatTimestamp(it).split(" ").last().take(5) } ?: "--:--", icon = Icons.AutoMirrored.Filled.Login, color = Color(0xFF1976D2))
                                        InfoRow(label = "SALIDA REAL", value = stats!!.checkOutTime?.let { formatTimestamp(it).split(" ").last().take(5) } ?: "--:--", icon = Icons.AutoMirrored.Filled.Logout, color = Color(0xFFD32F2F))
                                    }
                                    
                                    Spacer(Modifier.height(12.dp))
                                    Text("JORNADA ASIGNADA", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Día de descanso:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        Text(stats!!.restDay, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = if(stats!!.restDay == "No definido") Color.Red else Color.Unspecified)
                                    }
                                }
                            }
                        }

                        @Composable
                        fun RowScope.LeftPartRow() {
                            Surface(
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                color = Color(0xFFF5F7FA)
                            ) {
                                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(modifier = Modifier.size(60.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.AccountCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                                            }
                                        }
                                        Spacer(Modifier.width(16.dp))
                                        Column {
                                            Text(currentUser?.username?.uppercase() ?: "", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                                            Text(currentUser?.role?.name ?: "", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                                        }
                                    }

                                    HorizontalDivider()

                                    Text("REGISTRO DE HOY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        InfoRow(label = "ENTRADA REAL", value = stats!!.checkInTime?.let { formatTimestamp(it).split(" ").last().take(5) } ?: "--:--", icon = Icons.AutoMirrored.Filled.Login, color = Color(0xFF1976D2))
                                        InfoRow(label = "SALIDA REAL", value = stats!!.checkOutTime?.let { formatTimestamp(it).split(" ").last().take(5) } ?: "--:--", icon = Icons.AutoMirrored.Filled.Logout, color = Color(0xFFD32F2F))
                                    }
                                    
                                    Spacer(Modifier.height(12.dp))
                                    Text("JORNADA ASIGNADA", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Día de descanso:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        Text(stats!!.restDay, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = if(stats!!.restDay == "No definido") Color.Red else Color.Unspecified)
                                    }
                                    
                                    Spacer(Modifier.weight(1f))
                                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                                        Text("CERRAR")
                                    }
                                }
                            }
                        }

                        @Composable
                        fun ColumnScope.RightPart() {
                            Surface(modifier = Modifier.weight(1.5f).fillMaxWidth(), color = Color.White) {
                                RightContent(stats, onDismiss, true)
                            }
                        }

                        @Composable
                        fun RowScope.RightPartRow() {
                            Surface(modifier = Modifier.weight(1.5f).fillMaxHeight(), color = Color.White) {
                                RightContent(stats, onDismiss, false)
                            }
                        }

                        if (isSmall) {
                            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                LeftPart()
                                RightPart()
                            }
                        } else {
                            Row(modifier = Modifier.fillMaxSize()) {
                                LeftPartRow()
                                VerticalDivider(thickness = 1.dp, color = Color.LightGray.copy(alpha = 0.5f))
                                RightPartRow()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RightContent(stats: UserPanelStats?, onDismiss: () -> Unit, isSmall: Boolean) {
    if (stats == null) return
    Column(modifier = Modifier.padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ChevronLeft, null, tint = Color.Gray)
            Text("ESTA SEMANA", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF0056A0), modifier = Modifier.padding(horizontal = 16.dp))
            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
        }

        Spacer(Modifier.height(16.dp))
        Text("📅 DÍAS DE LA SEMANA", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F4F9)), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                stats.weekDetails.forEach { day ->
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(day.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val (icon, color) = when(day.status) {
                                    "TRABAJADO" -> Icons.Default.Work to Color(0xFF795548)
                                    "DESCANSO" -> Icons.Default.Bedtime to Color(0xFFFF9800)
                                    "FALTA" -> Icons.Default.Cancel to Color.Red
                                    else -> Icons.Default.HourglassEmpty to Color.Gray
                                }
                                Icon(icon, null, tint = color, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(day.status, style = MaterialTheme.typography.labelSmall, color = color)
                            }
                        }
                        Text("$${day.amount.formatPrice()}", fontWeight = FontWeight.Black, style = MaterialTheme.typography.bodyLarge)
                    }
                    if (day.name != "Domingo") HorizontalDivider(color = Color.White.copy(alpha = 0.5f))
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("📈 RESUMEN DE INGRESOS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                IncomeRow("Sueldo Base Acumulado", "$${(stats.earnings - stats.bonus).formatPrice()}")
                IncomeRow("Bono Semanal", "$${stats.bonus.formatPrice()}")
                HorizontalDivider()
                IncomeRow("TOTAL ESTIMADO", "$${stats.earnings.formatPrice()}", isBold = true, color = Color(0xFF0056A0))
            }
        }
        if (stats.daysWorked > 0 && stats.bonus == 0.0) {
            Surface(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = Color.Red, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("BONO PERDIDO POR FALTA", style = MaterialTheme.typography.labelSmall, color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (isSmall) {
            Spacer(Modifier.height(24.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(8.dp)) {
                Text("CERRAR MI CUENTA")
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, icon: ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun IncomeRow(label: String, value: String, isBold: Boolean = false, color: Color = Color.Unspecified) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = if(isBold) Color.Black else Color.Gray)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = if(isBold) FontWeight.Black else FontWeight.Bold, color = color)
    }
}

@Composable
fun UpdateDialog(viewModel: com.abtsplazita.posplazita.ui.UpdateViewModel) {
    val info by viewModel.updateInfo.collectAsState()
    val isDownloading by viewModel.isDownloading.collectAsState()
    val progress by viewModel.downloadProgress.collectAsState()
    val error by viewModel.error.collectAsState()

    if (info == null) return

    AlertDialog(
        onDismissRequest = { if (!info!!.forceUpdate && !isDownloading) viewModel.dismissUpdate() },
        title = { Text("Actualización Disponible (v${info!!.version})", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!info!!.releaseNotes.isNullOrBlank()) {
                    Text("Novedades:", fontWeight = FontWeight.Bold)
                    Text(info!!.releaseNotes!!, style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text("Hay una nueva versión del sistema disponible para descargar e instalar.")
                }

                if (isDownloading) {
                    Spacer(Modifier.height(16.dp))
                    Text("Descargando actualización... ${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                    )
                }

                if (error != null) {
                    Text(error!!, color = Color.Red, style = MaterialTheme.typography.labelSmall)
                    Button(onClick = { viewModel.clearError() }) { Text("REINTENTAR") }
                }
            }
        },
        confirmButton = {
            if (!isDownloading) {
                Button(onClick = { viewModel.startUpdate() }) {
                    Text("ACTUALIZAR AHORA")
                }
            }
        },
        dismissButton = {
            if (!info!!.forceUpdate && !isDownloading) {
                TextButton(onClick = { viewModel.dismissUpdate() }) {
                    Text("MÁS TARDE")
                }
            }
        }
    )
}

