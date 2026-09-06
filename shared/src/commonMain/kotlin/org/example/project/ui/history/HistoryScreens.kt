package com.abtsplazita.posplazita.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abtsplazita.posplazita.domain.formatPrice
import com.abtsplazita.posplazita.domain.formatWeight
import com.abtsplazita.posplazita.domain.calculatePriceFromUtility
import com.abtsplazita.posplazita.domain.calculateDefaultPrice1
import com.abtsplazita.posplazita.domain.calculateDefaultPrice2
import com.abtsplazita.posplazita.domain.calculateDefaultPrice3
import com.abtsplazita.posplazita.domain.calculateUtility
import com.abtsplazita.posplazita.domain.Sale
import com.abtsplazita.posplazita.domain.SaleItem
import com.abtsplazita.posplazita.domain.Product
import com.abtsplazita.posplazita.domain.CashMovementType
import com.abtsplazita.posplazita.domain.Purchase
import com.abtsplazita.posplazita.domain.PurchaseItem
import com.abtsplazita.posplazita.domain.PurchaseStatus
import com.abtsplazita.posplazita.domain.CashOut
import com.abtsplazita.posplazita.domain.PreCut
import com.abtsplazita.posplazita.formatTimestamp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.datetime.*
import kotlin.math.abs

@Composable
fun HistoryModule(viewModel: HistoryViewModel, onLogout: () -> Unit = {}) {
    var currentSubScreen by remember { mutableStateOf("dashboard") }
    val warningMessage by viewModel.warningMessage.collectAsState()

    if (warningMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearWarning() },
            confirmButton = { Button(onClick = { viewModel.clearWarning() }) { Text("Aceptar") } },
            title = { Text("Aviso") },
            text = { Text(warningMessage!!) }
        )
    }

    when (currentSubScreen) {
        "dashboard" -> ConsultasDashboard(
            viewModel = viewModel,
            onNavigateToSales = { currentSubScreen = "sales" },
            onNavigateToCashOuts = { currentSubScreen = "cash_outs" },
            onNavigateToPurchases = { currentSubScreen = "purchases" },
            onNavigateToMovements = { currentSubScreen = "movements" },
            onNavigateToWithdrawals = { currentSubScreen = "withdrawals" },
            onNavigateToPreCuts = { currentSubScreen = "precutes" },
            onNavigateToSupplierPayments = { currentSubScreen = "supplier_payments" },
            onNavigateToInventoryReport = { currentSubScreen = "inventory_report" },
            onNavigateToEmployeePayments = { currentSubScreen = "employee_payments" },
            onNavigateToDeletionLogs = { currentSubScreen = "deletion_logs" },
            onNavigateToProductReturns = { currentSubScreen = "product_returns" }
        )
        "sales" -> SalesHistoryScreen(viewModel, onBack = { currentSubScreen = "dashboard" })
        "cash_outs" -> CashOutHistoryScreen(viewModel, onLogout = onLogout, onBack = { currentSubScreen = "dashboard" })
        "purchases" -> PurchasesHistoryScreen(viewModel, onBack = { currentSubScreen = "dashboard" })
        "movements" -> CashMovementsHistoryScreen(viewModel, onBack = { currentSubScreen = "dashboard" })
        "withdrawals" -> WithdrawalsHistoryScreen(viewModel, onBack = { currentSubScreen = "dashboard" })
        "precutes" -> PreCutsHistoryScreen(viewModel, onBack = { currentSubScreen = "dashboard" })
        "supplier_payments" -> SupplierPaymentsHistoryScreen(viewModel, onBack = { currentSubScreen = "dashboard" })
        "inventory_report" -> InventoryReportScreen(viewModel, onBack = { currentSubScreen = "dashboard" })
        "employee_payments" -> EmployeePaymentsHistoryScreen(viewModel, onBack = { currentSubScreen = "dashboard" })
        "deletion_logs" -> DeletionLogsHistoryScreen(viewModel, onBack = { currentSubScreen = "dashboard" })
        "product_returns" -> ProductReturnsHistoryScreen(viewModel, onBack = { currentSubScreen = "dashboard" })
    }
}

@Composable
fun ConsultasDashboard(
    viewModel: HistoryViewModel,
    onNavigateToSales: () -> Unit,
    onNavigateToCashOuts: () -> Unit,
    onNavigateToPurchases: () -> Unit,
    onNavigateToMovements: () -> Unit,
    onNavigateToWithdrawals: () -> Unit,
    onNavigateToPreCuts: () -> Unit,
    onNavigateToSupplierPayments: () -> Unit,
    onNavigateToInventoryReport: () -> Unit,
    onNavigateToEmployeePayments: () -> Unit,
    onNavigateToDeletionLogs: () -> Unit,
    onNavigateToProductReturns: () -> Unit
) {
    val terminalBalances by viewModel.terminalBalances.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val selectedBranchId by viewModel.selectedBranchId.collectAsState()
    val hasPendingPurchases by viewModel.hasPendingPurchases.collectAsState()

    LaunchedEffect(selectedBranchId) {
        viewModel.refreshDashboardData()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Centro de Consultas", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text("Resumen de saldos por caja", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            
            HistoryPeriodSelector(viewModel)
            Spacer(Modifier.width(8.dp))
            
            if (isRefreshing) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                IconButton(onClick = { viewModel.refreshDashboardData() }) {
                    Icon(Icons.Default.Refresh, "Actualizar")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // --- TARJETAS DE SALDO POR CAJA ---
        if (terminalBalances.isNotEmpty()) {
            terminalBalances.forEach { balance ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(balance.terminalName.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "$${balance.amount.formatPrice()}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Reportes Detallados", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        // --- BOTONES DE MENÚ ADAPTATIVOS ---
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val gridColumns = if (maxWidth < 600.dp) 1 else if (maxWidth < 900.dp) 2 else 3
            
            val menuItems = listOf(
                MenuData("Ventas", "Tickets y detalles", Icons.AutoMirrored.Filled.ReceiptLong, Color(0xFF2196F3), onNavigateToSales),
                MenuData("Compras", "Entradas de almacén", Icons.Default.ShoppingCart, if(hasPendingPurchases) Color.Red else Color(0xFFFF9800), onNavigateToPurchases),
                MenuData("Inventario", "Costos y existencias", Icons.Default.Inventory, Color(0xFF673AB7), onNavigateToInventoryReport),
                MenuData("Cortes", "Cierres y arqueos", Icons.Default.Assessment, Color(0xFF4CAF50), onNavigateToCashOuts),
                MenuData("Precortes", "Validaciones rápidas", Icons.AutoMirrored.Filled.FactCheck, Color(0xFF2196F3), onNavigateToPreCuts),
                MenuData("E/S Efectivo", "Movimientos caja", Icons.AutoMirrored.Filled.CompareArrows, Color(0xFFE91E63), onNavigateToMovements),
                MenuData("Retiros", "Efectivo vs Tarjeta", Icons.Default.Atm, Color(0xFF2196F3), onNavigateToWithdrawals),
                MenuData("Proveedores", "Abonos y deudas", Icons.Default.Handshake, Color(0xFF673AB7), onNavigateToSupplierPayments),
                MenuData("Nómina", "Sueldos empleados", Icons.Default.Engineering, Color(0xFF009688), onNavigateToEmployeePayments),
                MenuData("Borrados", "Cancelaciones", Icons.Default.DeleteForever, Color.Red, onNavigateToDeletionLogs),
                MenuData("Cambios", "Devoluciones", Icons.Default.SyncAlt, Color(0xFFE91E63), onNavigateToProductReturns)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                menuItems.chunked(gridColumns).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowItems.forEach { item ->
                            MenuButton(
                                modifier = Modifier.weight(1f),
                                title = item.title,
                                subtitle = if (gridColumns > 1) item.subtitle else "",
                                icon = item.icon,
                                color = item.color,
                                onClick = item.onClick
                            )
                        }
                        if (rowItems.size < gridColumns) {
                            repeat(gridColumns - rowItems.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

data class MenuData(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

@Composable
fun MenuButton(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.heightIn(min = 60.dp).clickable { onClick() },
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = color.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title, 
                    style = MaterialTheme.typography.titleSmall, 
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle, 
                        style = MaterialTheme.typography.labelSmall, 
                        color = Color.Gray,
                        maxLines = 1
                    )
                }
            }
            
            Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun InventoryReportScreen(viewModel: HistoryViewModel, onBack: () -> Unit) {
    val items by viewModel.inventoryReport.collectAsState()
    val totalCost by viewModel.totalInventoryCost.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Text("Valor de Inventario", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("VALOR TOTAL (COSTO)", style = MaterialTheme.typography.labelSmall)
                Text("$${totalCost.formatPrice()}", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
            }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(items) { item ->
                ListItem(
                    headlineContent = { Text(item.product.name) },
                    supportingContent = { Text("Stock: ${item.currentStock} | Costo Unit: $${item.product.cost.formatPrice()}") },
                    trailingContent = { Text("$${item.totalCost.formatPrice()}", fontWeight = FontWeight.Bold) }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun EmployeePaymentsHistoryScreen(viewModel: HistoryViewModel, onBack: () -> Unit) {
    val payments by viewModel.employeePayments.collectAsState()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Text("Historial de Nómina", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
            items(payments) { p ->
                ListItem(
                    headlineContent = { Text(p.employeeName, fontWeight = FontWeight.Bold) },
                    supportingContent = { Text(formatTimestamp(p.date)) },
                    trailingContent = { Text("$${p.amount.formatPrice()}", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge) }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun SupplierPaymentsHistoryScreen(viewModel: HistoryViewModel, onBack: () -> Unit) {
    val payments by viewModel.supplierPaymentsFiltered.collectAsState()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Text("Pagos a Proveedores", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            HistoryPeriodSelector(viewModel)
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
            items(payments) { pay ->
                val dateTime = Instant.fromEpochMilliseconds(pay.timestamp).toLocalDateTime(TimeZone.currentSystemDefault())
                ListItem(
                    headlineContent = { Text(pay.notes ?: "Abono a cuenta", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("${dateTime.date} ${dateTime.time.toString().take(5)} | ${pay.method}") },
                    trailingContent = { Text("$${pay.amount.formatPrice()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color(0xFF2E7D32)) }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun HistoryPeriodSelector(viewModel: HistoryViewModel) {
    val currentPeriod by viewModel.period.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    val options = listOf(
        HistoryPeriod.TODAY to "Hoy",
        HistoryPeriod.YESTERDAY to "Ayer",
        HistoryPeriod.LAST_7_DAYS to "Últimos 7 días",
        HistoryPeriod.MONTH_ACTUAL to "Mes Actual",
        HistoryPeriod.MONTH_PREVIOUS to "Mes Anterior",
        HistoryPeriod.CUSTOM to "Personalizado..."
    )

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(Icons.Default.DateRange, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(options.find { it.first == currentPeriod }?.second ?: "Seleccionar Periodo")
            Icon(Icons.Default.ArrowDropDown, null)
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (p, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        viewModel.setPeriod(p)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SalesHistoryScreen(viewModel: HistoryViewModel, onBack: () -> Unit) {
    val sales by viewModel.sales.collectAsState()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Text("Historial de Ventas", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            
            HistoryPeriodSelector(viewModel)
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { viewModel.refreshDashboardData() }) { Icon(Icons.Default.Refresh, null) }
        }
        LazyColumn(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
            items(sales.sortedByDescending { it.timestamp }) { sale ->
                ListItem(
                    headlineContent = { Text("TICKET: ${sale.id}", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text(formatTimestamp(sale.timestamp)) },
                    trailingContent = { Text("$${sale.total.formatPrice()}", fontWeight = FontWeight.Black) },
                    modifier = Modifier.clickable { viewModel.selectSale(sale) }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun PurchasesHistoryScreen(viewModel: HistoryViewModel, onBack: () -> Unit) {
    val purchases by viewModel.purchases.collectAsState()
    val selectedPurchase by viewModel.selectedPurchase.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Text("Historial de Compras", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            HistoryPeriodSelector(viewModel)
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
            items(purchases) { p ->
                val isPending = p.status == PurchaseStatus.PENDING_PRICE_UPDATE
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { viewModel.selectPurchase(p) },
                    border = if (isPending) BorderStroke(2.dp, Color.Red) else null,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPending) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    ListItem(
                        headlineContent = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("COMPRA: ${p.id}", fontWeight = FontWeight.Bold, color = if(isPending) Color.Red else Color.Unspecified)
                                if (isPending) {
                                    Spacer(Modifier.width(8.dp))
                                    Surface(color = Color.Red, shape = RoundedCornerShape(4.dp)) {
                                        Text("REVISAR PRECIOS", color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp))
                                    }
                                }
                            }
                        },
                        supportingContent = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(formatTimestamp(p.timestamp))
                                Spacer(Modifier.width(12.dp))
                                Surface(
                                    color = when {
                                        p.paymentMethod.contains("Efectivo", true) -> Color(0xFF2E7D32)
                                        p.paymentMethod.contains("Tarjeta", true) -> Color(0xFF1976D2)
                                        p.paymentMethod.contains("Transferencia", true) -> Color(0xFF7B1FA2)
                                        p.paymentMethod.contains("Crédito", true) -> Color(0xFFD32F2F)
                                        else -> Color.Gray
                                    }.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = p.paymentMethod.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            p.paymentMethod.contains("Efectivo", true) -> Color(0xFF2E7D32)
                                            p.paymentMethod.contains("Tarjeta", true) -> Color(0xFF1976D2)
                                            p.paymentMethod.contains("Transferencia", true) -> Color(0xFF7B1FA2)
                                            p.paymentMethod.contains("Crédito", true) -> Color(0xFFD32F2F)
                                            else -> Color.Gray
                                        },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        },
                        trailingContent = { Text("$${p.total.formatPrice()}", fontWeight = FontWeight.Black, color = if(isPending) Color.Red else Color.Unspecified) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }

    if (selectedPurchase != null) {
        PurchasePriceAdjustmentDialog(
            purchase = selectedPurchase!!,
            viewModel = viewModel,
            onDismiss = { viewModel.clearSelection() }
        )
    }
}

@Composable
fun PurchasePriceAdjustmentDialog(purchase: Purchase, viewModel: HistoryViewModel, onDismiss: () -> Unit) {
    val items by viewModel.selectedPurchaseItems.collectAsState()
    val products by viewModel.currentPurchaseProducts.collectAsState()
    
    // Mapa para rastrear cambios locales de TODA la compra
    val adjustments = remember(items) { mutableStateMapOf<String, PriceAdjustment>() }

    // Inicializar o actualizar ajustes cuando los productos se carguen
    LaunchedEffect(items, products) {
        if (items.isNotEmpty() && products.isNotEmpty()) {
            items.forEach { item ->
                if (!adjustments.containsKey(item.productId)) {
                    val cost = item.costAtPurchase
                    
                    // Cálculos sugeridos según reglas de negocio
                    val p2 = calculateDefaultPrice2(cost)
                    val p1 = calculateDefaultPrice1(cost)
                    val p3 = p2 + 0.50
                    
                    adjustments[item.productId] = PriceAdjustment(
                        productId = item.productId,
                        newCost = cost,
                        newPrice1 = p1,
                        newPrice2 = p2,
                        newPrice3 = p3,
                        newPrice4 = 0.0
                    )
                }
            }
        }
    }

    // Estado para el producto que se está editando en detalle
    var editingProductItem by remember { mutableStateOf<Pair<PurchaseItem, Product?>?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF0F2F5)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header (Estilo Imagen 1/2)
                Surface(color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = Color.White) }
                        Spacer(Modifier.width(8.dp))
                        Text("Revisión de Precios de Compra #${purchase.id}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }

                // Tabla de productos (Estilo Imagen 1)
                Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                    // Encabezados
                    Surface(color = Color.White, shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp), border = BorderStroke(1.dp, Color.LightGray)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Spacer(Modifier.width(40.dp))
                            Text("Producto", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                            Text("Costo", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = Color.Gray, textAlign = TextAlign.Center)
                            Text("Costo prom. / ant.", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.labelMedium, color = Color.Gray, textAlign = TextAlign.Center)
                            Text("Utilidad / ant.", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.labelMedium, color = Color.Gray, textAlign = TextAlign.Center)
                            Text("Precio de venta", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = Color.Gray, textAlign = TextAlign.Center)
                            Spacer(Modifier.width(48.dp))
                        }
                    }

                    if (items.isEmpty()) {
                        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f).background(Color.White).border(1.dp, Color.LightGray)) {
                            items(items) { item ->
                                val product = products[item.productId]
                                val adj = adjustments[item.productId]
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { editingProductItem = item to product }.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckBoxOutlineBlank, null, tint = Color.LightGray, modifier = Modifier.size(24.dp).padding(start = 8.dp))
                                    Spacer(Modifier.width(16.dp))
                                    
                                    // Info Producto
                                    Row(modifier = Modifier.weight(1.5f), verticalAlignment = Alignment.CenterVertically) {
                                        Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(4.dp), color = Color(0xFFF5F5F5)) {
                                            Icon(Icons.Default.Image, null, tint = Color.LightGray, modifier = Modifier.padding(8.dp))
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(product?.barcode ?: "---", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                            Text(item.productName, fontWeight = FontWeight.Bold, maxLines = 1)
                                        }
                                    }

                                    // Costo de esta compra
                                    Text("$${item.costAtPurchase.formatPrice()}", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)

                                    // Costo Promedio
                                    Column(modifier = Modifier.weight(1.2f), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("$${(adj?.newCost ?: item.costAtPurchase).formatPrice()}", fontWeight = FontWeight.Bold)
                                        Text("$${(product?.cost ?: 0.0).formatPrice()}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    }

                                    // Utilidad
                                    val currentPrice2 = adj?.newPrice2 ?: product?.price2 ?: 0.0
                                    val currentCost = adj?.newCost ?: item.costAtPurchase
                                    val utility = calculateUtility(currentCost, currentPrice2)
                                    val oldPrice2 = product?.price2 ?: 0.0
                                    val oldUtility = calculateUtility(product?.cost ?: 0.0, oldPrice2)
                                    
                                    Column(modifier = Modifier.weight(1.2f), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("${utility.formatPrice()}%", fontWeight = FontWeight.Bold)
                                        Text("${oldUtility.formatPrice()}%", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    }

                                    // Precio
                                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("$${currentPrice2.formatPrice()}", fontWeight = FontWeight.Black)
                                        Text("1 de 3", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    }

                                    IconButton(onClick = { editingProductItem = item to product }) {
                                        Icon(Icons.Default.SettingsBackupRestore, null, tint = Color(0xFF0056A0))
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                            }
                        }
                    }
                }

                // Footer Aceptar
                Button(
                    onClick = { 
                        viewModel.adjustProductPrices(purchase, adjustments.values.toList())
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("ACEPTAR", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }

    // DIALOGO DETALLADO (Estilo Imagen 2)
    if (editingProductItem != null) {
        val (item, product) = editingProductItem!!
        val adj = adjustments[item.productId] ?: PriceAdjustment(item.productId, item.costAtPurchase, 0.0, 0.0, 0.0, 0.0)

        Dialog(
            onDismissRequest = { editingProductItem = null }
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.9f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Azul
                    Surface(color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { editingProductItem = null }) { Icon(Icons.Default.Close, null, tint = Color.White) }
                            Spacer(Modifier.width(16.dp))
                            Text("Producto", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                    }

                    Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                        // Info Superior
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(modifier = Modifier.size(100.dp), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color.LightGray)) {
                                Icon(Icons.Default.Image, null, tint = Color.LightGray, modifier = Modifier.padding(24.dp))
                            }
                            Spacer(Modifier.width(20.dp))
                            Column {
                                Text(product?.barcode ?: "---", color = Color.Gray)
                                Text(item.productName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                                Text("Impuestos", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                                Text("Costo promedio anterior: $${(product?.cost ?: 0.0).formatPrice()}", color = Color.Gray)
                                Text("Costo promedio: $${adj.newCost.formatPrice()}", color = Color.Gray)
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        // Tabla de Precios
                        Surface(border = BorderStroke(1.dp, Color.LightGray), shape = RoundedCornerShape(8.dp)) {
                            Column {
                                // Header Tabla
                                Row(
                                    modifier = Modifier.fillMaxWidth().background(Color(0xFFF8F9FA)).padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckBoxOutlineBlank, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text("No.", modifier = Modifier.width(30.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    Text("Mayoreo", modifier = Modifier.width(60.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                    Text("Desc.", modifier = Modifier.width(60.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                    Text("Util. Ant.", modifier = Modifier.weight(1.3f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                    Text("Utilidad", modifier = Modifier.weight(1.3f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                    Text("Precio Ant.", modifier = Modifier.width(85.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                    Text("Precio", modifier = Modifier.width(110.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                    Spacer(Modifier.width(40.dp))
                                }

                                val prices = listOf(
                                    Triple(1, adj.newPrice1, product?.price1 ?: 0.0),
                                    Triple(2, adj.newPrice2, product?.price2 ?: 0.0),
                                    Triple(3, adj.newPrice3, product?.price3 ?: 0.0)
                                )

                                prices.forEach { (no, currentP, oldP) ->
                                    val util = calculateUtility(adj.newCost, currentP)
                                    val oldUtil = calculateUtility(product?.cost ?: 0.0, oldP)

                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CheckBoxOutlineBlank, null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Text("$no", modifier = Modifier.width(30.dp), fontWeight = FontWeight.Bold)
                                        Text("0", modifier = Modifier.width(60.dp), color = Color.Gray, textAlign = TextAlign.Center)
                                        Text("--", modifier = Modifier.width(60.dp), color = Color.Gray, textAlign = TextAlign.Center)
                                        Text("${oldUtil.formatPrice()}%", modifier = Modifier.weight(1.3f), color = Color.Gray, textAlign = TextAlign.Center, maxLines = 1)
                                        Text("${util.formatPrice()}%", modifier = Modifier.weight(1.3f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = if(util >= oldUtil) Color(0xFF2E7D32) else Color.Red, maxLines = 1)
                                        Text("$${oldP.formatPrice()}", modifier = Modifier.width(85.dp), color = Color.Gray, textAlign = TextAlign.Center)
                                        
                                        // Campo de Precio editable resaltado en azul
                                        OutlinedTextField(
                                            value = currentP.toString(),
                                            onValueChange = { 
                                                val newVal = it.toDoubleOrNull() ?: 0.0
                                                val newAdj = when(no) {
                                                    1 -> adj.copy(newPrice1 = newVal)
                                                    2 -> adj.copy(newPrice2 = newVal)
                                                    3 -> adj.copy(newPrice3 = newVal)
                                                    else -> adj.copy(newPrice4 = newVal)
                                                }
                                                adjustments[item.productId] = newAdj
                                            },
                                            modifier = Modifier.width(110.dp).height(48.dp),
                                            prefix = { Text("$", style = MaterialTheme.typography.bodySmall) },
                                            textStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black, textAlign = TextAlign.Center),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = Color(0xFFE3F2FD),
                                                unfocusedContainerColor = Color(0xFFF0F7FF),
                                                focusedBorderColor = Color(0xFF2196F3),
                                                unfocusedBorderColor = Color(0xFFBBDEFB)
                                            ),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                        )

                                        IconButton(onClick = { 
                                            // Restaurar al precio anterior
                                            val newAdj = when(no) {
                                                1 -> adj.copy(newPrice1 = oldP)
                                                2 -> adj.copy(newPrice2 = oldP)
                                                3 -> adj.copy(newPrice3 = oldP)
                                                else -> adj.copy(newPrice4 = oldP)
                                            }
                                            adjustments[item.productId] = newAdj
                                        }, modifier = Modifier.size(40.dp)) {
                                            Icon(Icons.Default.History, null, tint = Color(0xFF0056A0), modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { editingProductItem = null },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0056A0))
                        ) {
                            Text("ACEPTAR", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CashOutHistoryScreen(viewModel: HistoryViewModel, onLogout: () -> Unit, onBack: () -> Unit) {
    val outs by viewModel.cashOuts.collectAsState()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Text("Historial de Cortes", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
            items(outs) { co ->
                ListItem(
                    headlineContent = { Text("CORTE: ${co.id}", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text(formatTimestamp(co.timestamp)) },
                    trailingContent = { Text("$${co.countedAmount.formatPrice()}", fontWeight = FontWeight.Black) }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun CashMovementsHistoryScreen(viewModel: HistoryViewModel, onBack: () -> Unit) {
    val movs by viewModel.movementsFiltered.collectAsState()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Text("Movimientos de Caja", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            HistoryPeriodSelector(viewModel)
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
            items(movs) { m ->
                val isIn = m.type == CashMovementType.IN
                ListItem(
                    headlineContent = { Text(m.reason, fontWeight = FontWeight.Bold) },
                    supportingContent = { Text(formatTimestamp(m.timestamp)) },
                    trailingContent = { Text("${if(isIn) "+" else "-"}$${m.amount.formatPrice()}", color = if(isIn) Color(0xFF2E7D32) else Color.Red, fontWeight = FontWeight.Bold) }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun PreCutsHistoryScreen(viewModel: HistoryViewModel, onBack: () -> Unit) {
    val pcs by viewModel.preCutsFiltered.collectAsState()
    val branches by viewModel.availableBranches.collectAsState()
    val terminals by viewModel.availableTerminals.collectAsState()
    
    val branchMap = remember(branches) { branches.associate { it.id to it.name } }
    val terminalMap = remember(terminals) { terminals.associate { it.id to it.name } }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Text("Historial de Precortes", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            HistoryPeriodSelector(viewModel)
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
            items(pcs.sortedByDescending { it.timestamp }) { pc ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("PRECORTE: ${pc.userId.uppercase()}", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                                Text(formatTimestamp(pc.timestamp), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                            val diffColor = when {
                                pc.difference > 0.01 -> Color(0xFF2E7D32) // Sobrante
                                pc.difference < -0.01 -> Color.Red // Faltante
                                else -> Color(0xFF0056A0) // Cuadrado
                            }
                            Surface(color = diffColor, shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    text = if (pc.difference > 0.01) "SOBRANTE" else if (pc.difference < -0.01) "FALTANTE" else "CUADRADO",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(12.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            InfoCol("Sucursal", branchMap[pc.branchId] ?: "Principal", Modifier.weight(1f))
                            InfoCol("Caja", terminalMap[pc.terminalId] ?: "Caja 1", Modifier.weight(1f))
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.2f))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("DEBE HABER", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text("$${pc.expectedAmount.formatPrice()}", fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("CONTADO", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text("$${pc.countedAmount.formatPrice()}", fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("DIFERENCIA", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(
                                    text = "${if(pc.difference > 0) "+" else ""}$${pc.difference.formatPrice()}",
                                    fontWeight = FontWeight.Black,
                                    color = if(pc.difference < -0.01) Color.Red else if(pc.difference > 0.01) Color(0xFF2E7D32) else Color.Unspecified
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoCol(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
fun WithdrawalsHistoryScreen(viewModel: HistoryViewModel, onBack: () -> Unit) {
    val ws by viewModel.withdrawalsFiltered.collectAsState()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Text("Reporte de Retiros", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            HistoryPeriodSelector(viewModel)
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
            items(ws) { m ->
                ListItem(
                    headlineContent = { Text(m.reason, fontWeight = FontWeight.Bold) },
                    supportingContent = { Text(formatTimestamp(m.timestamp)) },
                    trailingContent = { Text("$${m.amount.formatPrice()}", fontWeight = FontWeight.Black) }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun DeletionLogsHistoryScreen(viewModel: HistoryViewModel, onBack: () -> Unit) {
    val logs by viewModel.deletionLogs.collectAsState()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Text("Tickets Borrados", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            HistoryPeriodSelector(viewModel)
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
            items(logs) { log ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("TICKET ID: ${log.ticketId}", fontWeight = FontWeight.Bold, color = Color.Red)
                        Text(formatTimestamp(log.timestamp), style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.height(8.dp))
                        Text("Solicitó: ${log.requesterId} | Aprobó: ${log.approverId}")
                        Text("Total: $${log.total.formatPrice()}", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun CashOutScreen(
    viewModel: HistoryViewModel, 
    onLogout: () -> Unit, 
    showTotalPreference: Boolean, 
    onNavigateToPos: () -> Unit
) {
    val pendingSales by viewModel.pendingSales.collectAsState()
    val pendingMovements by viewModel.pendingMovements.collectAsState()
    val terminalBalances by viewModel.terminalBalances.collectAsState()
    val selectedTerminalId by viewModel.selectedTerminalId.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    
    var countedAmountText by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    val totalCashSales = pendingSales.sumOf { it.cashAmount }
    val totalIn = pendingMovements.filter { it.type == CashMovementType.IN }.sumOf { it.amount }
    val totalOut = pendingMovements.filter { it.type == CashMovementType.OUT }.sumOf { it.amount }
    
    val expectedAmount = totalCashSales + totalIn - totalOut

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        // Cabecera Azul
        Surface(color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
            Row(modifier = Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateToPos) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
                Text("CORTE DE CAJA", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                TextButton(onClick = onLogout) { Text("SALIR", color = Color.White) }
            }
        }

        Row(modifier = Modifier.fillMaxSize()) {
            // LADO IZQUIERDO: RESUMEN
            Column(modifier = Modifier.weight(1f).padding(24.dp).verticalScroll(scrollState)) {
                Text("RESUMEN DE MOVIMIENTOS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                
                Card(colors = CardDefaults.cardColors(containerColor = Color.Gray.copy(alpha = 0.05f))) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        DetailRow("Ventas en Efectivo", "$${totalCashSales.formatPrice()}", color = Color(0xFF2E7D32))
                        DetailRow("Entradas de Dinero", "+$${totalIn.formatPrice()}", color = Color(0xFF2E7D32))
                        DetailRow("Salidas de Dinero", "-$${totalOut.formatPrice()}", color = Color.Red)
                        HorizontalDivider()
                        DetailRow("TOTAL ESPERADO", "$${expectedAmount.formatPrice()}", isBold = true, color = Color(0xFF0056A0))
                    }
                }

                Spacer(Modifier.height(24.dp))
                Text("FILTRAR POR CAJA", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    terminalBalances.forEach { terminal ->
                        FilterChip(
                            selected = selectedTerminalId == terminal.terminalId,
                            onClick = { viewModel.filterByTerminal(terminal.terminalId) },
                            label = { Text(terminal.terminalName) }
                        )
                    }
                }
            }

            // LADO DERECHO: ACCIÓN
            Surface(modifier = Modifier.weight(0.8f).fillMaxHeight(), color = Color.White, shadowElevation = 8.dp) {
                Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ARQUEO DE CAJA", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(32.dp))

                    OutlinedTextField(
                        value = countedAmountText,
                        onValueChange = { if(it.isEmpty() || it.all { c -> c.isDigit() || c == '.' }) countedAmountText = it },
                        label = { Text("Monto Contado en Caja") },
                        modifier = Modifier.fillMaxWidth(),
                        prefix = { Text("$ ") },
                        textStyle = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black, textAlign = TextAlign.Center),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )

                    Spacer(Modifier.height(32.dp))

                    val counted = countedAmountText.toDoubleOrNull() ?: 0.0
                    val diff = counted - expectedAmount

                    if (countedAmountText.isNotEmpty()) {
                        Surface(
                            color = if(diff >= 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(if(diff >= 0) "SOBRANTE" else "FALTANTE", fontWeight = FontWeight.Bold, color = if(diff >= 0) Color(0xFF2E7D32) else Color.Red)
                                Text("$${abs(diff).formatPrice()}", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = if(diff >= 0) Color(0xFF2E7D32) else Color.Red)
                            }
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    Button(
                        onClick = { 
                            viewModel.saveCashOut(
                                countedAmount = counted,
                                expectedAmount = expectedAmount,
                                ticketCount = pendingSales.size,
                                currentUserId = currentUser?.username ?: "admin",
                                onDone = onNavigateToPos
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = countedAmountText.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Check, null)
                        Spacer(Modifier.width(12.dp))
                        Text("REALIZAR CORTE FINAL", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, color: Color = Color.Unspecified, isBold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = if(isBold) FontWeight.Bold else FontWeight.Normal, color = color)
    }
}

@Composable
fun DateRangeFilter(start: Long?, end: Long?, onRangeSelected: (Long?, Long?) -> Unit) {
    Row { Text("Filtro de Fecha") }
}

@Composable
fun SaleDetailsDialog(sale: Sale, items: List<SaleItem>, onReprint: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, confirmButton = { Button(onClick = onDismiss) { Text("Cerrar") } }, text = { Text("Detalles del ticket") })
}

@Composable
fun ProductReturnsHistoryScreen(viewModel: HistoryViewModel, onBack: () -> Unit) {
    val returns by viewModel.productReturns.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Text("Cambios y Devoluciones", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            HistoryPeriodSelector(viewModel)
        }
        
        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
            items(returns.sortedByDescending { it.timestamp }) { ret ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SyncAlt, null, tint = Color(0xFFE91E63))
                            Spacer(Modifier.width(8.dp))
                            Text(formatTimestamp(ret.timestamp), style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("DEVUELTO:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("${ret.returnedItem.quantity.formatWeight()}x ${ret.returnedItem.productName}", fontWeight = FontWeight.Bold)
                        
                        ret.takenItem?.let {
                            Spacer(Modifier.height(8.dp))
                            Text("CAMBIADO POR:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text("${it.quantity.formatWeight()}x ${it.productName}", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Text("Saldo: ", style = MaterialTheme.typography.labelSmall)
                            Text("$${ret.difference.formatPrice()}", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}
