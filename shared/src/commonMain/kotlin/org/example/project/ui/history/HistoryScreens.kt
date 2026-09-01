package com.abtsplazita.posplazita.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
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
import com.abtsplazita.posplazita.domain.Sale
import com.abtsplazita.posplazita.domain.SaleItem
import com.abtsplazita.posplazita.domain.Product
import com.abtsplazita.posplazita.domain.CashMovementType
import com.abtsplazita.posplazita.domain.Purchase
import com.abtsplazita.posplazita.domain.PurchaseItem
import com.abtsplazita.posplazita.domain.CashOut
import com.abtsplazita.posplazita.domain.PreCut
import com.abtsplazita.posplazita.formatTimestamp
import kotlinx.datetime.*

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

    LaunchedEffect(selectedBranchId) {
        viewModel.refreshDashboardData()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Centro de Consultas", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text("Resumen de saldos por caja", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
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
                MenuData("Compras", "Entradas de almacén", Icons.Default.ShoppingCart, Color(0xFFFF9800), onNavigateToPurchases),
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
            Text("Pagos a Proveedores", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
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
fun SalesHistoryScreen(viewModel: HistoryViewModel, onBack: () -> Unit) {
    val sales by viewModel.sales.collectAsState()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Text("Historial de Ventas", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        LazyColumn(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
            items(sales) { sale ->
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
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Text("Historial de Compras", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
            items(purchases) { p ->
                ListItem(
                    headlineContent = { Text("COMPRA: ${p.id}", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text(formatTimestamp(p.timestamp)) },
                    trailingContent = { Text("$${p.total.formatPrice()}", fontWeight = FontWeight.Black) }
                )
                HorizontalDivider()
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
            Text("Movimientos de Caja", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
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
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Text("Historial de Precortes", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
            items(pcs) { pc ->
                ListItem(
                    headlineContent = { Text("PRECORTE: ${pc.userId}", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text(formatTimestamp(pc.timestamp)) },
                    trailingContent = { Text("$${pc.countedAmount.formatPrice()}", fontWeight = FontWeight.Black) }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun WithdrawalsHistoryScreen(viewModel: HistoryViewModel, onBack: () -> Unit) {
    val ws by viewModel.withdrawalsFiltered.collectAsState()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Text("Reporte de Retiros", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
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
            Text("Tickets Borrados", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
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
fun CashOutScreen(viewModel: HistoryViewModel, onLogout: () -> Unit, showTotalPreference: Boolean, onNavigateToPos: () -> Unit) {
    Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Pantalla de Corte") }
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
            Text("Cambios y Devoluciones", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
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
                        Text("${ret.returnedItem.quantity}x ${ret.returnedItem.productName}", fontWeight = FontWeight.Bold)
                        
                        ret.takenItem?.let {
                            Spacer(Modifier.height(8.dp))
                            Text("CAMBIADO POR:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text("${it.quantity}x ${it.productName}", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
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
