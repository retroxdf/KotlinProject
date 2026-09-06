package com.abtsplazita.posplazita

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    AppContent()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContent() {
    val database = remember { 
        try { createDatabase() } catch (e: Exception) { null }
    }
    
    if (database == null) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Text("Error crítico: No se pudo inicializar la base de datos.", color = Color.Red)
        }
        return
    }

    LaunchedEffect(Unit) {
        try { initializeFirebase() } catch (e: Exception) {}
    }

    val firebaseManager = remember { FirebaseManager() }
    val globalScope = rememberCoroutineScope()

    val productRepository = remember { ProductRepository(database.productDao(), database.inventoryDao(), database.stockMovementDao(), database.categoryDao(), database.taxDao(), firebaseManager, globalScope) }
    val saleRepository = remember { SaleRepository(database.saleDao(), database.heldSaleDao(), firebaseManager) }
    val userRepository = remember { UserRepository(database.userDao(), database.rolePermissionDao(), firebaseManager, globalScope) }
    val branchRepository = remember { BranchRepository(database.branchDao(), firebaseManager, globalScope) }
    val employeeRepository = remember { EmployeeRepository(database.employeeDao(), database.scheduleDao(), database.loanDao(), database.absenceReplacementDao(), database.cashBoxDao(), database.contaplaTransactionDao(), database.corteContaplaDao(), database.paymentRecordDao(), database.attendanceDao(), firebaseManager) }
    val settingsRepository = remember { SettingsRepository(database.appSettingsDao()) }
    
    val purchaseRepository = remember { PurchaseRepository(database.purchaseDao(), productRepository, firebaseManager) }
    val cashMovementRepository = remember { CashMovementRepository(database.cashMovementDao(), firebaseManager) }
    val cashOutRepository = remember { CashOutRepository(database.cashOutDao(), firebaseManager) }
    val preCutRepository = remember { PreCutRepository(database.preCutDao(), firebaseManager) }
    val expenseRepository = remember { ExpenseRepository(database.expenseDao()) }
    val promotionRepository = remember { PromotionRepository(database.promotionDao(), firebaseManager) }
    val posTerminalRepository = remember { PosTerminalRepository(database.posTerminalDao(), firebaseManager) }
    val permissionRepository = remember { PermissionRepository(database.rolePermissionDao()) }
    val supplierRepository = remember { SupplierRepository(database.supplierDao(), database.supplierPaymentDao(), database.productSupplierDao(), firebaseManager) }
    val customerRepository = remember { CustomerRepository(database.customerDao(), database.customerPaymentDao(), database.customerProductPriceDao(), firebaseManager, globalScope) }
    val deletionLogRepository = remember { DeletionLogRepository(database.deletionLogDao(), firebaseManager) }
    val productReturnRepository = remember { ProductReturnRepository(database.productReturnDao(), firebaseManager) }
    val movementRepository = remember { StockMovementRepository(database.stockMovementDao(), firebaseManager) }

    val mercadoPagoManager = remember { MercadoPagoManager() }
    val printerManager = remember { getRealPrinterManager() }
    val scaleManager = remember { getScaleManager() }
    val currentSaleManager = remember { CurrentSaleManager(settingsRepository, globalScope) }

    val checkoutManager = remember { 
        com.abtsplazita.posplazita.domain.CheckoutManager(
            saleRepository, productRepository, customerRepository, settingsRepository, 
            productReturnRepository, firebaseManager, mercadoPagoManager, printerManager, 
            currentSaleManager, globalScope
        ) 
    }
    val cashManager = remember {
        com.abtsplazita.posplazita.domain.CashManager(
            cashMovementRepository, settingsRepository, checkoutManager, printerManager, globalScope
        )
    }
    val customerInteractor = remember {
        com.abtsplazita.posplazita.domain.CustomerInteractor(
            customerRepository, settingsRepository, printerManager, cashManager, globalScope
        )
    }
    
    val syncManager = remember { SyncManager(saleRepository, cashMovementRepository, productRepository, branchRepository, userRepository, employeeRepository, customerRepository, purchaseRepository, supplierRepository, promotionRepository, deletionLogRepository, settingsRepository, firebaseManager, globalScope) }
    val updateViewModel = remember { UpdateViewModel() }
    
    LaunchedEffect(Unit) {
        syncManager.startAutoSync()
        updateViewModel.checkForUpdates()
    }
    
    val authViewModel = remember { AuthViewModel(userRepository, employeeRepository, permissionRepository, settingsRepository) }
    val branchViewModel = remember { BranchViewModel(branchRepository) }

    LaunchedEffect(database) {
        if (settingsRepository.getSetting("db_initial_clear_v13") != "true") {
            try {
                database.clearAllTablesManual()
                userRepository.initializeAdmin()
                settingsRepository.saveSetting("is_initial_sync_completed", "false")
                settingsRepository.saveSetting("db_initial_clear_v13", "true")
            } catch (e: Exception) {}
        }
    }

    var selectedBranch by remember { mutableStateOf<Branch?>(null) }
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    
    if (!isLoggedIn) {
        LoginScreen(authViewModel, "")
        return
    }

    val currentBranch = selectedBranch
    if (currentBranch == null) {
        BranchSelectionScreen(viewModel = branchViewModel, onBranchSelected = { selectedBranch = it }, onLogout = { authViewModel.logout() })
        return
    }

    BranchMainLayout(
        branch = currentBranch,
        productRepository = productRepository,
        saleRepository = saleRepository,
        userRepository = userRepository,
        branchRepository = branchRepository,
        employeeRepository = employeeRepository,
        settingsRepository = settingsRepository,
        purchaseRepository = purchaseRepository,
        cashMovementRepository = cashMovementRepository,
        cashOutRepository = cashOutRepository,
        preCutRepository = preCutRepository,
        expenseRepository = expenseRepository,
        promotionRepository = promotionRepository,
        posTerminalRepository = posTerminalRepository,
        permissionRepository = permissionRepository,
        supplierRepository = supplierRepository,
        customerRepository = customerRepository,
        deletionLogRepository = deletionLogRepository,
        productReturnRepository = productReturnRepository,
        movementRepository = movementRepository,
        firebaseManager = firebaseManager,
        mercadoPagoManager = mercadoPagoManager,
        printerManager = printerManager,
        scaleManager = scaleManager,
        currentSaleManager = currentSaleManager,
        checkoutManager = checkoutManager,
        cashManager = cashManager,
        customerInteractor = customerInteractor,
        syncManager = syncManager,
        authViewModel = authViewModel,
        updateViewModel = updateViewModel,
        onLogout = { selectedBranch = null; authViewModel.logout() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchMainLayout(
    branch: Branch,
    productRepository: ProductRepository,
    saleRepository: SaleRepository,
    userRepository: UserRepository,
    branchRepository: BranchRepository,
    employeeRepository: EmployeeRepository,
    settingsRepository: SettingsRepository,
    purchaseRepository: PurchaseRepository,
    cashMovementRepository: CashMovementRepository,
    cashOutRepository: CashOutRepository,
    preCutRepository: PreCutRepository,
    expenseRepository: ExpenseRepository,
    promotionRepository: PromotionRepository,
    posTerminalRepository: PosTerminalRepository,
    permissionRepository: PermissionRepository,
    supplierRepository: SupplierRepository,
    customerRepository: CustomerRepository,
    deletionLogRepository: DeletionLogRepository,
    productReturnRepository: ProductReturnRepository,
    movementRepository: StockMovementRepository,
    firebaseManager: FirebaseManager,
    mercadoPagoManager: MercadoPagoManager,
    printerManager: PrinterManager,
    scaleManager: ScaleManager,
    currentSaleManager: CurrentSaleManager,
    checkoutManager: CheckoutManager,
    cashManager: CashManager,
    customerInteractor: CustomerInteractor,
    syncManager: SyncManager,
    authViewModel: AuthViewModel,
    updateViewModel: UpdateViewModel,
    onLogout: () -> Unit
) {
    val branchId = branch.id
    
    val posViewModel = remember(branchId) { PosViewModel(productRepository, saleRepository, customerRepository, posTerminalRepository, userRepository, settingsRepository, cashMovementRepository, cashOutRepository, preCutRepository, employeeRepository, mercadoPagoManager, currentSaleManager, branchId, promotionRepository, deletionLogRepository, productReturnRepository, printerManager, firebaseManager, scaleManager, checkoutManager, cashManager, customerInteractor) }
    val prodVM = remember(branchId) { ProductViewModel(productRepository, branchRepository, movementRepository, userRepository, branchId) }
    val purchaseViewModel = remember(branchId) { PurchaseViewModel(productRepository, purchaseRepository, supplierRepository, cashMovementRepository, null, branchId) }
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

    LaunchedEffect(branchId) {
        currentSaleManager.setBranchId(branchId)
        syncManager.setBranchId(branchId)
        try { posTerminalRepository.refreshTerminals(branchId) } catch (e: Exception) {}
    }

    val userPermissions by authViewModel.userPermissions.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val allSettings by settingsRepository.getAllSettings().collectAsState(emptyMap())

    var currentScreen by remember { mutableStateOf("pos") }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val mainFocusRequester = remember { FocusRequester() }

    val navigationItems = listOf(
        NavigationItem("pos", "Venta", Icons.Default.ShoppingCart, "F1", Permission.MAKE_SALE),
        NavigationItem("customers", "Clientes", Icons.Default.People, "F2", Permission.CUSTOMER_VIEW),
        NavigationItem("products", "Productos", Icons.AutoMirrored.Filled.List, "F3", Permission.PRODUCT_VIEW),
        NavigationItem("purchases", "Compras", Icons.Default.AddShoppingCart, "F4", Permission.MANAGE_PURCHASES),
        NavigationItem("inventory", "Inventario", Icons.Default.Star, "F5", Permission.PRODUCT_VIEW),
        NavigationItem("history", "Consultas", Icons.Default.Assessment, "F8", Permission.VIEW_REPORTS),
        NavigationItem("contabilidad", "Contabilidad", Icons.Default.Payments, "F9", permission = Permission.VIEW_ACCOUNTING),
        NavigationItem("settings", "Ajustes", Icons.Default.Settings, "F10", Permission.MANAGE_SETTINGS),
        NavigationItem("cash_out", "Corte Caja", Icons.Default.Info, "F11", Permission.PERFORM_CASH_OUT),
        NavigationItem("dashboard", "Dashboard", Icons.Default.Dashboard, "F12", Permission.VIEW_REPORTS),
        NavigationItem("change_branch", "Sucursal", Icons.Default.SyncAlt, permission = Permission.MANAGE_SETTINGS),
    ).filter { item ->
        if (item.id == "change_branch") {
            val isLocked = allSettings["app_lock_branch_change"] == "true"
            if (isLocked && currentUser?.role != Role.SUPER_ADMIN) return@filter false
            return@filter currentUser?.role == Role.SUPER_ADMIN || currentUser?.role == Role.GERENTE
        }
        val p = item.permission
        if (p == null) true
        else (userPermissions[p] ?: PermissionLevel.DISABLED) != PermissionLevel.DISABLED
    }

    BoxWithConstraints {
        // Forzamos modo compacto en Android para evitar saturar la TopAppBar
        val isAndroid = getPlatform().name.contains("Android")
        val isCompact = isAndroid || maxWidth < 1100.dp
        
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                AppDrawerContent(
                    navigationItems = navigationItems,
                    currentScreen = currentScreen,
                    onNavigate = { screen ->
                        if (screen == "change_branch") { onLogout() }
                        else { currentScreen = screen }
                    },
                    onLogout = onLogout,
                    currentUser = currentUser,
                    authViewModel = authViewModel,
                    onClose = { scope.launch { drawerState.close() } }
                )
            }
        ) {
            Scaffold(
                topBar = {
                    val currentLabel = navigationItems.find { it.id == currentScreen }?.label ?: ""
                    TopAppBar(
                        title = { 
                            Column {
                                Text(currentLabel, fontWeight = FontWeight.Black, fontSize = 18.sp)
                                Text(branch.name.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        },
                        navigationIcon = { IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, "Menú") } },
                        actions = {
                            if (currentScreen == "pos" && !isCompact) {
                                PosActionIcons(posViewModel)
                            }
                            IconButton(onClick = { posViewModel.refreshCatalog() }) { Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.primary) }
                            Box {
                                var showMenu by remember { mutableStateOf(false) }
                                IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, "Opciones") }
                                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                    if (isCompact && currentScreen == "pos") {
                                        PosMenuContent(posViewModel) { showMenu = false }
                                        HorizontalDivider()
                                    }
                                    DropdownMenuItem(text = { Text("Comentarios Ticket (Alt+P)") }, leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, null, tint = Color(0xFF2196F3)) }, onClick = { showMenu = false; posViewModel.openCommentDialog() })
                                    DropdownMenuItem(text = { Text("Realizar Precorte (Alt+K)") }, leadingIcon = { Icon(Icons.Default.Analytics, null, tint = Color(0xFF2196F3)) }, onClick = { showMenu = false; posViewModel.openPreCutDialog() })
                                    HorizontalDivider()
                                    DropdownMenuItem(text = { Text("Abrir Cajón (Alt+N)") }, leadingIcon = { Icon(Icons.Default.LockOpen, null, tint = Color.Gray) }, onClick = { showMenu = false; posViewModel.openCashDrawer() })
                                    DropdownMenuItem(text = { Text("Limpiar Venta (F5)") }, leadingIcon = { Icon(Icons.Default.DeleteSweep, null, tint = Color.Red) }, onClick = { showMenu = false; posViewModel.clearSale() })
                                }
                            }
                        }
                    )
                }
            ) { padding ->
                Surface(
                    modifier = Modifier.padding(padding)
                        .focusRequester(mainFocusRequester)
                        .focusable()
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown) {
                                if (event.key == Key.Escape) {
                                    if (currentScreen == "pos") { posViewModel.handleGlobalEscape { currentScreen = "checkout" }; return@onPreviewKeyEvent true }
                                    else if (currentScreen == "checkout") { currentScreen = "pos"; return@onPreviewKeyEvent true }
                                }
                                if (currentScreen == "pos" && event.isAltPressed) {
                                    when (event.key) {
                                        Key.C -> { posViewModel.openCustomerDialog(); return@onPreviewKeyEvent true }
                                        Key.W -> { posViewModel.putSaleOnHold(); return@onPreviewKeyEvent true }
                                        Key.R -> { posViewModel.openWithdrawalDialog(); return@onPreviewKeyEvent true }
                                        Key.G -> { posViewModel.openHeldSalesDialog(); return@onPreviewKeyEvent true }
                                        Key.D -> { posViewModel.openReturnDialog(); return@onPreviewKeyEvent true }
                                        Key.P -> { posViewModel.openCommentDialog(); return@onPreviewKeyEvent true }
                                        Key.A -> { posViewModel.openDebtPaymentDialog(); return@onPreviewKeyEvent true }
                                        Key.K -> { posViewModel.openPreCutDialog(); return@onPreviewKeyEvent true }
                                        Key.N -> { posViewModel.openCashDrawer(); return@onPreviewKeyEvent true }
                                        Key.I -> { posViewModel.reprintLastSale(); return@onPreviewKeyEvent true }
                                    }
                                }
                                when (event.key) {
                                    Key.F1 -> if (currentScreen != "pos") { currentScreen = "pos"; true } else false
                                    Key.F2 -> if (currentScreen != "customers") { customerViewModel.selectCustomer(null); currentScreen = "customers"; true } else false
                                    Key.F3 -> if (currentScreen != "products") { prodVM.resetToCatalog(); currentScreen = "products"; true } else false
                                    Key.F4 -> if (currentScreen != "purchases") { currentScreen = "purchases"; true } else false
                                    Key.F5 -> if (currentScreen == "pos") { posViewModel.clearSale(); true } else { currentScreen = "inventory"; true }
                                    Key.F8 -> if (currentScreen != "history") { currentScreen = "history"; true } else false
                                    Key.F10 -> if (currentScreen != "settings") { currentScreen = "settings"; true } else false
                                    Key.F11 -> if (currentScreen != "cash_out") { currentScreen = "cash_out"; true } else false
                                    Key.F12 -> if (currentScreen != "dashboard") { currentScreen = "dashboard"; true } else false
                                    else -> false
                                }
                            } else false
                        }
                ) {
                    val showCashMovementDialog by posViewModel.showCashMovementDialog.collectAsState()
                    if (showCashMovementDialog != null) { CashMovementDialog(posViewModel, type = showCashMovementDialog!!) }
                    val showPreCutDialog by posViewModel.showPreCutDialog.collectAsState()
                    if (showPreCutDialog) { PreCutDialog(posViewModel, currentUserId = currentUser?.username ?: "admin") }
                    when (currentScreen) {
                        "dashboard" -> DashboardScreen(dashboardViewModel)
                        "pos" -> {
                            val adImages by branchPeripheralViewModel.adImages.collectAsState()
                            val currentAdIndex by branchPeripheralViewModel.currentAdIndex.collectAsState()
                            LaunchedEffect(adImages) { while (adImages.size > 1) { kotlinx.coroutines.delay(120_000); branchPeripheralViewModel.nextAd() } }
                            PosMainScreen(viewModel = posViewModel, userViewModel = userViewModel, adImageUrl = adImages.getOrNull(currentAdIndex) ?: "", currentUserId = currentUser?.username ?: "admin", onLogout = { onLogout() }, onNavigateToCheckout = { posViewModel.prepareCheckout(); currentScreen = "checkout" }, onNavigateToHistory = { currentScreen = "history" }, onNavigateToSettings = { currentScreen = "settings" }, onNavigateToInventory = { currentScreen = "inventory" })
                        }
                        "checkout" -> CheckoutScreen(viewModel = posViewModel, onCancel = { currentScreen = "pos" })
                        "web_orders" -> WebOrdersScreen(viewModel = posViewModel, onBack = { currentScreen = "pos" }, onNavigateToPos = { currentScreen = "pos" })
                        "purchases" -> PurchaseModule(viewModel = purchaseViewModel)
                        "advanced_purchases" -> AdvancedPurchaseModule(viewModel = purchaseViewModel)
                        "customers" -> CustomerModule(customerViewModel)
                        "products" -> ProductModule(prodVM)
                        "expenses" -> ExpenseModule(expenseViewModel, onBack = { currentScreen = "dashboard" })
                        "history" -> HistoryModule(historyViewModel, onLogout = { onLogout() })
                        "inventory" -> InventoryModule(inventoryViewModel)
                        "restock" -> RestockScreen(restockViewModel, onBack = { currentScreen = "inventory" })
                        "cash_out" -> CashOutScreen(viewModel = historyViewModel, onLogout = { onLogout() }, showTotalPreference = branchPeripheralViewModel.showCashOutTotal.collectAsState().value, onNavigateToPos = { currentScreen = "pos" })
                        "suppliers" -> SupplierModule(supplierViewModel)
                        "users" -> UserModule(userViewModel)
                        "contabilidad" -> ContabilidadModule(contabilidadViewModel)
                        "settings" -> PeripheralSettingsScreen(branchPeripheralViewModel, posViewModel, userViewModel, promotionViewModel, productRepository)
                    }
                }
            }
        }
        LaunchedEffect(currentScreen) { mainFocusRequester.requestFocus() }
        val updateInfo by updateViewModel.updateInfo.collectAsState()
        if (updateInfo != null) { UpdateDialog(updateViewModel) }
        val showUserPanel by authViewModel.showUserPanel.collectAsState()
        if (showUserPanel) { UserPanelFullScreen(authViewModel, onDismiss = { authViewModel.closeUserPanel() }) }
    }
}

@Composable
fun PosActionIcons(viewModel: PosViewModel) {
    IconButton(onClick = { viewModel.openCustomerDialog() }) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.People, "Cliente", tint = Color(0xFF673AB7), modifier = Modifier.size(20.dp)); Text("Alt+C", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp) } }
    IconButton(onClick = { viewModel.putSaleOnHold() }) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Pause, "Espera", tint = Color(0xFFFFA500), modifier = Modifier.size(20.dp)); Text("Alt+W", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp) } }
    IconButton(onClick = { viewModel.openHeldSalesDialog() }) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Save, "Guardados", tint = Color(0xFF2196F3), modifier = Modifier.size(20.dp)); Text("Alt+G", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp) } }
    IconButton(onClick = { viewModel.openWithdrawalDialog() }) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Atm, "Retiro", tint = Color(0xFF2196F3), modifier = Modifier.size(20.dp)); Text("Alt+R", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp) } }
    IconButton(onClick = { viewModel.openReturnDialog() }) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.SyncAlt, "Devolución", tint = Color(0xFFE91E63), modifier = Modifier.size(20.dp)); Text("Alt+D", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp) } }
    IconButton(onClick = { viewModel.openDebtPaymentDialog() }) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Payments, "Abono", tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp)); Text("Alt+A", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp) } }
    IconButton(onClick = { viewModel.reprintLastSale() }) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Print, "Reimprimir", tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp)); Text("Alt+I", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp) } }
    VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp))
    IconButton(onClick = { viewModel.openCashMovementDialog(CashMovementType.IN) }) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.AddCircle, "Entrada", tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp)); Text("F6", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp) } }
    IconButton(onClick = { viewModel.openCashMovementDialog(CashMovementType.OUT) }) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.RemoveCircle, "Salida", tint = Color.Red, modifier = Modifier.size(20.dp)); Text("F7", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp) } }
}

@Composable
fun PosMenuContent(viewModel: PosViewModel, onDismiss: () -> Unit) {
    DropdownMenuItem(text = { Text("Traer Cliente") }, leadingIcon = { Icon(Icons.Default.People, null, tint = Color(0xFF673AB7)) }, onClick = { onDismiss(); viewModel.openCustomerDialog() })
    DropdownMenuItem(text = { Text("Poner en Espera") }, leadingIcon = { Icon(Icons.Default.Pause, null, tint = Color(0xFFFFA500)) }, onClick = { onDismiss(); viewModel.putSaleOnHold() })
    DropdownMenuItem(text = { Text("Tickets Guardados") }, leadingIcon = { Icon(Icons.Default.Save, null, tint = Color(0xFF2196F3)) }, onClick = { onDismiss(); viewModel.openHeldSalesDialog() })
    DropdownMenuItem(text = { Text("Retiro Efectivo") }, leadingIcon = { Icon(Icons.Default.Atm, null, tint = Color(0xFF2196F3)) }, onClick = { onDismiss(); viewModel.openWithdrawalDialog() })
    DropdownMenuItem(text = { Text("Devolución / Cambio") }, leadingIcon = { Icon(Icons.Default.SyncAlt, null, tint = Color(0xFFE91E63)) }, onClick = { onDismiss(); viewModel.openReturnDialog() })
    DropdownMenuItem(text = { Text("Abonos / Deuda") }, leadingIcon = { Icon(Icons.Default.Payments, null, tint = Color(0xFF4CAF50)) }, onClick = { onDismiss(); viewModel.openDebtPaymentDialog() })
    DropdownMenuItem(text = { Text("Reimprimir Última") }, leadingIcon = { Icon(Icons.Default.Print, null, tint = Color(0xFF4CAF50)) }, onClick = { onDismiss(); viewModel.reprintLastSale() })
    HorizontalDivider()
    DropdownMenuItem(text = { Text("Entrada de Dinero (F6)") }, leadingIcon = { Icon(Icons.Default.AddCircle, null, tint = Color(0xFF4CAF50)) }, onClick = { onDismiss(); viewModel.openCashMovementDialog(CashMovementType.IN) })
    DropdownMenuItem(text = { Text("Salida de Dinero (F7)") }, leadingIcon = { Icon(Icons.Default.RemoveCircle, null, tint = Color.Red) }, onClick = { onDismiss(); viewModel.openCashMovementDialog(CashMovementType.OUT) })
}

data class NavigationItem(val id: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val shortcut: String? = null, val permission: Permission? = null)

@Composable
fun UserPanelFullScreen(viewModel: AuthViewModel, onDismiss: () -> Unit) {
    val stats by viewModel.userStats.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(color = Color(0xFF0056A0), contentColor = Color.White) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
                        Spacer(Modifier.width(8.dp))
                        Text("MI CUENTA", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
                val currentStats = stats
                if (currentStats == null) { Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() } }
                else {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val isSmall = maxWidth < 700.dp
                        if (isSmall) {
                            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                UserPanelHeader(currentUser, currentStats)
                                RightContent(currentStats, onDismiss, true)
                            }
                        } else {
                            Row(modifier = Modifier.fillMaxSize()) {
                                Column(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFF5F7FA)).padding(32.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                    UserPanelHeaderContent(currentUser, currentStats, true)
                                    Spacer(Modifier.weight(1f))
                                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp)) { Text("CERRAR PANEL", fontWeight = FontWeight.Bold) }
                                }
                                VerticalDivider(thickness = 1.dp, color = Color.LightGray.copy(alpha = 0.5f))
                                Surface(modifier = Modifier.weight(1.5f).fillMaxHeight(), color = Color.White) { RightContent(currentStats, onDismiss, false) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserPanelHeader(currentUser: User?, stats: UserPanelStats) {
    Column(modifier = Modifier.fillMaxWidth().background(Color(0xFFF5F7FA)).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        UserPanelHeaderContent(currentUser, stats, false)
    }
}

@Composable
fun UserPanelHeaderContent(currentUser: User?, stats: UserPanelStats, isLarge: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(if(isLarge) 80.dp else 60.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.AccountCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(if(isLarge) 56.dp else 40.dp)) }
        }
        Spacer(Modifier.width(if(isLarge) 20.dp else 16.dp))
        Column {
            Text(currentUser?.username?.uppercase() ?: "USUARIO", style = if(isLarge) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text(currentUser?.role?.name ?: "ROL", style = if(isLarge) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
    }
    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
    InfoRow("Empresa", "PLAZITA POS", Icons.Default.Business, Color(0xFF0056A0))
    InfoRow("Estatus de Cuenta", "ACTIVA", Icons.Default.VerifiedUser, Color(0xFF2E7D32))
    InfoRow("Días Trabajados (Semana)", "${stats.daysWorked} de 7", Icons.Default.EventAvailable, Color(0xFFFF9800))
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
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(8.dp)) { Text("CERRAR MI CUENTA") }
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
                } else { Text("Hay una nueva versión del sistema disponible para descargar e instalar.") }
                if (isDownloading) {
                    Spacer(Modifier.height(16.dp))
                    Text("Descargando actualización... ${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)))
                }
                if (error != null) {
                    Text(error!!, color = Color.Red, style = MaterialTheme.typography.labelSmall)
                    Button(onClick = { viewModel.clearError() }) { Text("REINTENTAR") }
                }
            }
        },
        confirmButton = { if (!isDownloading) { Button(onClick = { viewModel.startUpdate() }) { Text("ACTUALIZAR AHORA") } } },
        dismissButton = { if (!info!!.forceUpdate && !isDownloading) { TextButton(onClick = { viewModel.dismissUpdate() }) { Text("MÁS TARDE") } } }
    )
}

@Composable
fun AppDrawerContent(
    navigationItems: List<NavigationItem>,
    currentScreen: String,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    currentUser: User?,
    authViewModel: AuthViewModel,
    onClose: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = Color.White,
        drawerContentColor = Color.Black,
        modifier = Modifier.width(320.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 16.dp)) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape, modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Store, null, tint = MaterialTheme.colorScheme.primary) }
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("PLAZITA POS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("Sistema de Gestión", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp), color = Color.LightGray.copy(alpha = 0.2f))

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                navigationItems.forEach { item ->
                    val isSelected = currentScreen == item.id
                    NavigationDrawerItem(
                        label = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (item.shortcut != null) {
                                    Surface(color = if (isSelected) Color.White else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                                        Text(item.shortcut, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                    }
                                    Spacer(Modifier.width(12.dp))
                                }
                                Text(item.label, fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium)
                            }
                        },
                        selected = isSelected,
                        onClick = { onNavigate(item.id); onClose() },
                        icon = { Icon(item.icon, contentDescription = null, tint = if (isSelected) Color.White else MaterialTheme.colorScheme.primary) },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            unselectedIconColor = MaterialTheme.colorScheme.primary,
                            unselectedTextColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }

            if (currentUser != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color.LightGray.copy(alpha = 0.2f))
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().clickable { authViewModel.openUserPanel() }
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(text = currentUser.username.uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                            Text(text = currentUser.role.name, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("v1.0.9", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                TextButton(onClick = onLogout) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null, modifier = Modifier.size(18.dp), tint = Color.Red)
                    Spacer(Modifier.width(8.dp))
                    Text("CERRAR SESIÓN", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
