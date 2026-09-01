package com.abtsplazita.posplazita.ui.customers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.abtsplazita.posplazita.domain.formatPrice
import com.abtsplazita.posplazita.domain.Customer
import com.abtsplazita.posplazita.domain.CustomerPayment
import com.abtsplazita.posplazita.domain.Sale
import com.abtsplazita.posplazita.domain.SaleItem
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import androidx.compose.ui.layout.ContentScale

@Composable
fun CustomerModule(viewModel: CustomerViewModel) {
    val selectedCustomer by viewModel.selectedCustomer.collectAsState()
    val editingCustomer by viewModel.editingCustomer.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier.fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && (event.key == Key.Plus || event.key == Key.NumPadAdd)) {
                    if (editingCustomer == null) {
                        viewModel.startNewCustomer()
                        true
                    } else false
                } else false
            }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isCompact = maxWidth < 800.dp

            if (isCompact) {
                if (selectedCustomer == null) {
                    CustomerListScreen(viewModel)
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        IconButton(onClick = { viewModel.selectCustomer(null) }) {
                            Icon(Icons.Default.ArrowBackIosNew, "Volver")
                        }
                        CustomerDetailScreen(customer = selectedCustomer!!, viewModel = viewModel)
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(0.45f).fillMaxHeight()) {
                        CustomerListScreen(viewModel)
                    }
                    VerticalDivider()
                    Box(modifier = Modifier.weight(0.55f).fillMaxHeight()) {
                        if (selectedCustomer != null) {
                            CustomerDetailScreen(customer = selectedCustomer!!, viewModel = viewModel)
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Selecciona un cliente para ver su detalle", color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }

    if (editingCustomer != null) {
        CustomerEditDialog(
            customer = editingCustomer!!,
            onUpdate = { viewModel.updateEditingCustomer(it) },
            onSave = { viewModel.saveCustomer() },
            onCancel = { viewModel.cancelEdit() }
        )
    }

    val previewCustomer by viewModel.showCardPreview.collectAsState()
    if (previewCustomer != null) {
        MemberCardPreviewDialog(
            customer = previewCustomer!!,
            onPrintThermal = { viewModel.printMemberCard(it) },
            onPrintGraphic = { viewModel.printMemberCardGraphic(it) },
            onDismiss = { viewModel.closeCardPreview() }
        )
    }

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Atención") },
            text = { Text(errorMessage!!) },
            confirmButton = { Button(onClick = { viewModel.clearError() }) { Text("Aceptar") } }
        )
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
fun CustomerListScreen(viewModel: CustomerViewModel) {
    val customers by viewModel.customers.collectAsState()
    val selectedCustomer by viewModel.selectedCustomer.collectAsState()
    var customerToDelete by remember { mutableStateOf<Customer?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Lista de Clientes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            IconButton(onClick = { viewModel.startNewCustomer() }) {
                Icon(Icons.Default.PersonAdd, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(customers) { customer ->
                val isSelected = selectedCustomer?.id == customer.id
                ListItem(
                    headlineContent = { Text(customer.name, fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium) },
                    supportingContent = { Text("Tel: ${customer.phone ?: "---"}") },
                    trailingContent = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (customer.walletBalance > 0) {
                                Surface(color = Color(0xFF673AB7).copy(alpha = 0.1f), shape = MaterialTheme.shapes.extraSmall, modifier = Modifier.padding(end = 8.dp)) {
                                    Text("$${customer.walletBalance.formatPrice()}", color = Color(0xFF673AB7), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp))
                                }
                            }
                            if (customer.currentDebt > 0) Text("$${customer.currentDebt.formatPrice()}", color = Color.Red, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
                            IconButton(onClick = { customerToDelete = customer }) { Icon(Icons.Default.Delete, "Borrar", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)) }
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
                    modifier = Modifier.clickable { viewModel.selectCustomer(customer) }
                )
                HorizontalDivider()
            }
        }
    }

    if (customerToDelete != null) {
        AlertDialog(
            onDismissRequest = { customerToDelete = null },
            title = { Text("Eliminar Cliente") },
            text = { Text("¿Estás seguro de que deseas eliminar a '${customerToDelete!!.name}'? Esta acción no se puede deshacer.") },
            confirmButton = { Button(onClick = { viewModel.deleteCustomer(customerToDelete!!); customerToDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("ELIMINAR") } },
            dismissButton = { TextButton(onClick = { customerToDelete = null }) { Text("CANCELAR") } }
        )
    }
}

@Composable
fun CustomerDetailScreen(customer: Customer, viewModel: CustomerViewModel) {
    val payments by viewModel.customerPayments.collectAsState()
    val sales by viewModel.customerSales.collectAsState()
    val viewingItems by viewModel.viewingSaleItems.collectAsState()
    var showPaymentDialog by remember { mutableStateOf(false) }
    var selectedSaleId by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), 
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val buttonModifier = Modifier.widthIn(min = 120.dp)
            OutlinedButton(onClick = { viewModel.editCustomer(customer) }, modifier = buttonModifier) { Icon(Icons.Default.Person, null); Spacer(Modifier.width(8.dp)); Text("PERFIL") }
            OutlinedButton(onClick = { viewModel.openCardPreview(customer) }, modifier = buttonModifier) { Icon(Icons.Default.Badge, null); Spacer(Modifier.width(8.dp)); Text("CARD") }
            OutlinedButton(
                onClick = { viewModel.shareDebtReport(customer) }, 
                modifier = buttonModifier,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF25D366)) // Color WhatsApp
            ) { 
                Icon(Icons.Default.Share, null); Spacer(Modifier.width(8.dp)); Text("ENVIAR E.C.") 
            }
            Button(onClick = { showPaymentDialog = true }, modifier = buttonModifier, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Icon(Icons.Default.Payments, null); Spacer(Modifier.width(8.dp)); Text("ABONAR") }
        }
        Spacer(Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("RESUMEN DE CUENTA", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("$${customer.currentDebt.formatPrice()}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = Color.Red)
                        Text("Saldo Deudor Actual", style = MaterialTheme.typography.bodySmall)
                    }
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$${customer.walletBalance.formatPrice()}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = Color(0xFF673AB7))
                        Text("Monedero Electrónico", style = MaterialTheme.typography.bodySmall)
                    }
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text("$${customer.creditLimit.formatPrice()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Límite Global", style = MaterialTheme.typography.bodySmall)
                        Text("${customer.creditDays} días de plazo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("DETALLE DE TICKETS A CRÉDITO", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            val creditSales = sales.filter { it.creditAmount > 0 }
            if (creditSales.isEmpty()) { item { Text("No hay tickets con adeudo.", modifier = Modifier.padding(16.dp), color = Color.Gray) } }
            else {
                items(creditSales) { sale ->
                    val dateTime = Instant.fromEpochMilliseconds(sale.timestamp).toLocalDateTime(TimeZone.currentSystemDefault())
                    ListItem(
                        headlineContent = { Text("Ticket: ${sale.id}", fontWeight = FontWeight.Bold) },
                        supportingContent = { Column { Text("${dateTime.date} ${dateTime.time}"); Text("Total: $${sale.total.formatPrice()} | Pagó: $${sale.cashAmount.formatPrice()}") } },
                        trailingContent = { Column(horizontalAlignment = Alignment.End) { Text("ADEUDO", style = MaterialTheme.typography.labelSmall, color = Color.Red); Text("$${sale.creditAmount.formatPrice()}", color = Color.Red, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) } },
                        modifier = Modifier.clickable { selectedSaleId = sale.id; viewModel.loadSaleItems(sale.id) }
                    )
                    HorizontalDivider()
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("HISTORIAL DE ABONOS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.weight(0.8f)) {
            items(payments) { payment ->
                val dateTime = Instant.fromEpochMilliseconds(payment.timestamp).toLocalDateTime(TimeZone.currentSystemDefault())
                ListItem(headlineContent = { Text("Abono: $${payment.amount.formatPrice()}") }, supportingContent = { Text("${dateTime.date} ${dateTime.time}") }, trailingContent = { Icon(Icons.Default.Check, null, tint = Color(0xFF4CAF50)) })
                HorizontalDivider()
            }
        }
    }

    if (selectedSaleId != null) {
        AlertDialog(
            onDismissRequest = { selectedSaleId = null },
            title = { Text("Detalle de Ticket $selectedSaleId") },
            text = {
                Column(modifier = Modifier.widthIn(min = 400.dp)) {
                    if (viewingItems.isEmpty()) { CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally)) }
                    else { viewingItems.forEach { item -> ListItem(headlineContent = { Text(item.productName) }, supportingContent = { Text("${item.quantity} x $${item.priceAtSale.formatPrice()}") }, trailingContent = { Text("$${item.subtotal.formatPrice()}", fontWeight = FontWeight.Bold) }); HorizontalDivider() } }
                }
            },
            confirmButton = { Button(onClick = { selectedSaleId = null }) { Text("CERRAR") } }
        )
    }

    if (showPaymentDialog) {
        var amountText by remember { mutableStateOf("") }
        var selectedMethod by remember { mutableStateOf("Efectivo") }
        
        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            title = { Text("Registrar Abono") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = amountText, 
                        onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() || c == '.' }) amountText = it }, 
                        label = { Text("Monto del Abono") }, 
                        prefix = { Text("$ ") }, 
                        modifier = Modifier.fillMaxWidth(), 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    
                    Text("Método de Pago:", style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Efectivo", "Tarjeta", "Transferencia").forEach { method ->
                            FilterChip(
                                selected = selectedMethod == method,
                                onClick = { selectedMethod = method },
                                label = { Text(method) }
                            )
                        }
                    }
                }
            },
            confirmButton = { 
                Button(onClick = { 
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0) { 
                        viewModel.addPayment(amount, selectedMethod, "Abono manual") 
                        showPaymentDialog = false 
                    } 
                }) { Text("GUARDAR ABONO") } 
            },
            dismissButton = { TextButton(onClick = { showPaymentDialog = false }) { Text("CANCELAR") } }
        )
    }
}

@Composable
fun CustomerEditDialog(customer: Customer, onUpdate: (Customer) -> Unit, onSave: () -> Unit, onCancel: () -> Unit) {
    var limitText by remember { mutableStateOf(customer.creditLimit.toString()) }
    var weeklyLimitText by remember { mutableStateOf(customer.creditLimitWeekly.toString()) }
    val nameRequester = remember { FocusRequester() }
    val phoneRequester = remember { FocusRequester() }
    val limitRequester = remember { FocusRequester() }
    val weeklyRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(if (customer.id.isEmpty()) "Nuevo Cliente" else "Editar Cliente") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = customer.name, onValueChange = { onUpdate(customer.copy(name = it)) }, label = { Text("Nombre Completo") }, modifier = Modifier.fillMaxWidth().focusRequester(nameRequester).onPreviewKeyEvent { if (it.key == Key.Enter || it.key == Key.NumPadEnter) { if (it.type == KeyEventType.KeyDown) phoneRequester.requestFocus(); true } else false }, singleLine = true)
                OutlinedTextField(value = customer.phone ?: "", onValueChange = { onUpdate(customer.copy(phone = it)) }, label = { Text("Teléfono") }, modifier = Modifier.fillMaxWidth().focusRequester(phoneRequester).onPreviewKeyEvent { if (it.key == Key.Enter || it.key == Key.NumPadEnter) { if (it.type == KeyEventType.KeyDown) limitRequester.requestFocus(); true } else false }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                OutlinedTextField(value = limitText, onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() || c == '.' }) { limitText = it; onUpdate(customer.copy(creditLimit = it.toDoubleOrNull() ?: 0.0)) } }, label = { Text("Límite de Crédito Global") }, modifier = Modifier.fillMaxWidth().focusRequester(limitRequester).onPreviewKeyEvent { if (it.key == Key.Enter || it.key == Key.NumPadEnter) { if (it.type == KeyEventType.KeyDown) weeklyRequester.requestFocus(); true } else false }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = weeklyLimitText, onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() || c == '.' }) { weeklyLimitText = it; onUpdate(customer.copy(creditLimitWeekly = it.toDoubleOrNull() ?: 0.0)) } }, label = { Text("Límite de Crédito Semanal") }, modifier = Modifier.fillMaxWidth().focusRequester(weeklyRequester).onPreviewKeyEvent { if (it.key == Key.Enter || it.key == Key.NumPadEnter) { if (it.type == KeyEventType.KeyDown && customer.name.isNotBlank()) onSave(); true } else false }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        },
        confirmButton = { Button(onClick = onSave, enabled = customer.name.isNotBlank()) { Text("GUARDAR (Enter)") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("CANCELAR") } }
    )
    LaunchedEffect(Unit) {
        repeat(3) {
            kotlinx.coroutines.delay(200)
            try { nameRequester.requestFocus() } catch(e: Exception) {}
        }
    }
}

@Composable
fun MemberCardPreviewDialog(
    customer: Customer,
    onPrintThermal: (Customer) -> Unit,
    onPrintGraphic: (Customer) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Vista Previa de Gafete") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Diseño del Gafete (Visual)
                Card(
                    modifier = Modifier.size(width = 250.dp, height = 380.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(2.dp, Color.Black),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("PLAZITA POS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        
                        // Placeholder para Barcode (Usando API pública para preview)
                        val barcodeUrl = "https://bwipjs-api.metafloor.com/?bcid=code128&text=CLI-${customer.id}&scale=3&rotate=N&includetext"
                        Box(modifier = Modifier.height(80.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            KamelImage(
                                resource = { asyncPainterResource(data = barcodeUrl) },
                                contentDescription = "Barcode",
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.Fit,
                                onLoading = { Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(modifier = Modifier.size(24.dp)) } }
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(customer.name.uppercase(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Text("ID: ${customer.id}", style = MaterialTheme.typography.labelMedium)
                        }

                        Text("TARJETA DEL CLIENTE", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                Text("Opciones de Impresión:", style = MaterialTheme.typography.labelLarge)
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onPrintGraphic(customer) }) {
                    Icon(Icons.Default.PictureAsPdf, null)
                    Spacer(Modifier.width(8.dp))
                    Text("PDF / SISTEMA")
                }
                OutlinedButton(onClick = { onPrintThermal(customer) }) {
                    Icon(Icons.Default.Print, null)
                    Spacer(Modifier.width(8.dp))
                    Text("TÉRMICA")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCELAR") }
        }
    )
}
