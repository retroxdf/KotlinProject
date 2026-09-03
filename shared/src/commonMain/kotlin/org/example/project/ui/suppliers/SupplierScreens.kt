package com.abtsplazita.posplazita.ui.suppliers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import com.abtsplazita.posplazita.domain.Supplier
import com.abtsplazita.posplazita.domain.PurchaseStatus
import com.abtsplazita.posplazita.domain.formatPrice
import kotlinx.datetime.*
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange

@Composable
fun SupplierModule(viewModel: SupplierViewModel) {
    val suppliers by viewModel.filteredSuppliers.collectAsState()
    val selectedSupplier by viewModel.selectedSupplier.collectAsState()
    val isEditing by viewModel.isEditing.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val supplierPurchases by viewModel.supplierPurchases.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val focusRequester = remember { FocusRequester() }

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            confirmButton = { Button(onClick = { viewModel.clearError() }) { Text("Aceptar") } },
            title = { Text("Atención") },
            text = { Text(errorMessage!!) }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && (event.key == Key.Plus || event.key == Key.NumPadAdd)) {
                    if (!isEditing) {
                        viewModel.prepareNewSupplier()
                        true
                    } else false
                } else false
            }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isCompact = maxWidth < 800.dp
            
            if (isCompact) {
                // VISTA MÓVIL
                if (selectedSupplier == null) {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Text("Proveedores", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            label = { Text("Buscar...") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            singleLine = true
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.prepareNewSupplier() }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("AGREGAR (+)")
                        }
                        Spacer(Modifier.height(16.dp))
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(suppliers) { supplier ->
                                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { viewModel.selectSupplier(supplier) }) {
                                    ListItem(
                                        headlineContent = { Text(supplier.name, fontWeight = FontWeight.Bold) },
                                        supportingContent = { Text(supplier.contactName ?: "") },
                                        trailingContent = { Icon(Icons.Default.ArrowForwardIos, null, modifier = Modifier.size(16.dp)) }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        if (isEditing) {
                            SupplierEditForm(
                                supplier = selectedSupplier!!,
                                onSave = { viewModel.saveSupplier(it) },
                                onCancel = { viewModel.cancelEditing() }
                            )
                        } else {
                            Column {
                                IconButton(onClick = { viewModel.selectSupplier(null) }) {
                                    Icon(Icons.Default.ArrowBackIosNew, "Volver")
                                }
                                SupplierDetailView(
                                    supplier = selectedSupplier!!,
                                    onEdit = { viewModel.startEditing() },
                                    onDelete = { viewModel.deleteSupplier(it) },
                                    onPayment = { amount, method, notes, pId -> viewModel.makePayment(amount, method, notes, pId) },
                                    purchases = supplierPurchases
                                )
                            }
                        }
                    }
                }
            } else {
                // VISTA ESCRITORIO
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.weight(0.4f).fillMaxHeight().padding(16.dp)) {
                        Text("Proveedores", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            label = { Text("Buscar proveedor...") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            singleLine = true
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.prepareNewSupplier() }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("AGREGAR PROVEEDOR (+)")
                        }
                        Spacer(Modifier.height(16.dp))
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(suppliers) { supplier ->
                                val isSelected = selectedSupplier?.id == supplier.id
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { viewModel.selectSupplier(supplier) },
                                    border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                    colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                                ) {
                                    ListItem(
                                        headlineContent = { Text(supplier.name, fontWeight = FontWeight.Bold) },
                                        supportingContent = { Text(supplier.contactName ?: "Sin encargado") },
                                        trailingContent = { if (supplier.givesCredit) Icon(Icons.Default.CreditCard, null, tint = Color(0xFF4CAF50)) }
                                    )
                                }
                            }
                        }
                    }

                    VerticalDivider()

                    Box(modifier = Modifier.weight(0.6f).fillMaxHeight().padding(24.dp)) {
                        if (selectedSupplier != null) {
                            if (isEditing) {
                                SupplierEditForm(supplier = selectedSupplier!!, onSave = { viewModel.saveSupplier(it) }, onCancel = { viewModel.cancelEditing() })
                            } else {
                                SupplierDetailView(
                                    supplier = selectedSupplier!!,
                                    onEdit = { viewModel.startEditing() },
                                    onDelete = { viewModel.deleteSupplier(it) },
                                    onPayment = { amount, method, notes, pId -> viewModel.makePayment(amount, method, notes, pId) },
                                    purchases = supplierPurchases
                                )
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.LocalShipping, null, modifier = Modifier.size(100.dp), tint = Color.LightGray)
                                Spacer(Modifier.height(16.dp))
                                Text("Selecciona un proveedor para ver detalles", color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
fun SupplierDetailView(
    supplier: Supplier, 
    onEdit: () -> Unit, 
    onDelete: (Supplier) -> Unit,
    onPayment: (Double, String, String, String?) -> Unit,
    purchases: List<com.abtsplazita.posplazita.domain.Purchase> = emptyList()
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var paymentPrefillAmount by remember { mutableStateOf(0.0) }
    var paymentNotesPrefill by remember { mutableStateOf("") }
    var selectedPurchaseId by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(supplier.name, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
                if (supplier.currentDebt > 0) {
                    Text("DEUDA TOTAL: $${supplier.currentDebt.formatPrice()}", color = Color.Red, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                } else {
                    Text("SIN DEUDA PENDIENTE", color = Color(0xFF4CAF50), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Editar", tint = MaterialTheme.colorScheme.primary) }
            IconButton(onClick = { showDeleteConfirm = true }) { Icon(Icons.Default.Delete, "Borrar", tint = Color.Red) }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                DetailRow("Encargado:", supplier.contactName ?: "No asignado")
                DetailRow("Teléfono:", supplier.phone ?: "No asignado")
                DetailRow("Correo:", supplier.email ?: "No asignado")
                DetailRow("Dirección:", supplier.address ?: "No asignada")
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Card(colors = CardDefaults.cardColors(containerColor = if (supplier.givesCredit) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (supplier.givesCredit) Icons.Default.CheckCircle else Icons.Default.Cancel, null, tint = if (supplier.givesCredit) Color(0xFF2E7D32) else Color(0xFFC62828))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(if (supplier.givesCredit) "OFRECE CRÉDITO" else "SOLO CONTADO", fontWeight = FontWeight.Bold)
                            if (supplier.givesCredit) Text("Plazo de pago: ${supplier.creditDays} días", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { 
                        paymentPrefillAmount = supplier.currentDebt
                        paymentNotesPrefill = "Abono general"
                        selectedPurchaseId = null
                        showPaymentDialog = true 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Icon(Icons.Default.Payments, null); Spacer(Modifier.width(8.dp)); Text("ABONAR A LA DEUDA")
                }
            }
        }

        val creditPurchases = purchases.filter { it.paymentMethod == "Crédito" && it.status != com.abtsplazita.posplazita.domain.PurchaseStatus.PAID }
        if (creditPurchases.isNotEmpty()) {
            Spacer(Modifier.height(32.dp))
            Text("Tickets Pendientes (Crédito)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            
            creditPurchases.forEach { purchase ->
                val isPendingPrices = purchase.status == PurchaseStatus.PENDING_PRICE_UPDATE
                
                val limitDate = if (supplier.creditDays > 0) {
                    val daysMillis = supplier.creditDays.toLong() * 24 * 60 * 60 * 1000L
                    val limitMillis = purchase.timestamp + daysMillis
                    val dt = Instant.fromEpochMilliseconds(limitMillis).toLocalDateTime(TimeZone.currentSystemDefault())
                    "${dt.dayOfMonth}/${dt.monthNumber}/${dt.year}"
                } else "N/A"

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPendingPrices) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = if (isPendingPrices) BorderStroke(2.dp, Color.Red) else null
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Compra #${purchase.id}", fontWeight = FontWeight.Bold, color = if(isPendingPrices) Color.Red else Color.Unspecified)
                            Text("Total: $${purchase.total.formatPrice()}", color = if(isPendingPrices) Color.Red else MaterialTheme.colorScheme.primary)
                        }
                        if (isPendingPrices) {
                            Column(modifier = Modifier.weight(1.5f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(color = Color.Red, shape = RoundedCornerShape(4.dp)) {
                                    Text("REVISAR PRECIOS", color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp))
                                }
                            }
                        } else {
                            Column(modifier = Modifier.weight(1.5f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Límite de pago:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(limitDate, fontWeight = FontWeight.Bold, color = if (limitDate != "N/A") Color.Red else Color.Gray)
                            }
                        }
                        Button(
                            onClick = {
                                paymentPrefillAmount = purchase.total
                                paymentNotesPrefill = "Pago de Ticket #${purchase.id}"
                                selectedPurchaseId = purchase.id
                                showPaymentDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("PAGAR", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }

    if (showPaymentDialog) {
        var amountText by remember { mutableStateOf(paymentPrefillAmount.toString()) }
        var method by remember { mutableStateOf("Efectivo") }
        var notes by remember { mutableStateOf(paymentNotesPrefill) }

        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            title = { Text(if (selectedPurchaseId != null) "Pagar Ticket #$selectedPurchaseId" else "Registrar Abono a Proveedor") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() || c == '.' }) amountText = it },
                        label = { Text("Monto del Pago") },
                        modifier = Modifier.fillMaxWidth(),
                        prefix = { Text("$ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Text("Método de Pago:", style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Efectivo", "Transferencia", "Cheque").forEach { m ->
                            FilterChip(selected = method == m, onClick = { method = m }, label = { Text(m) })
                        }
                    }
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notas / Referencia") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        onPayment(amount, method, notes, selectedPurchaseId)
                        showPaymentDialog = false
                    }
                }) { Text("GUARDAR PAGO") }
            },
            dismissButton = { TextButton(onClick = { showPaymentDialog = false }) { Text("CANCELAR") } }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Confirmar Eliminación") },
            text = { Text("¿Estás seguro de que deseas eliminar al proveedor ${supplier.name}? Esta acción no se puede deshacer.") },
            confirmButton = { Button(onClick = { onDelete(supplier); showDeleteConfirm = false }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("ELIMINAR") } },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("CANCELAR") } }
        )
    }
}

@Composable
fun SupplierEditForm(supplier: Supplier, onSave: (Supplier) -> Unit, onCancel: () -> Unit) {
    var name by remember { mutableStateOf(supplier.name) }
    var contact by remember { mutableStateOf(supplier.contactName ?: "") }
    var phone by remember { mutableStateOf(supplier.phone ?: "") }
    var email by remember { mutableStateOf(supplier.email ?: "") }
    var address by remember { mutableStateOf(supplier.address ?: "") }
    var givesCredit by remember { mutableStateOf(supplier.givesCredit) }
    var creditDays by remember { mutableStateOf(supplier.creditDays.toString()) }

    val nameFR = remember { FocusRequester() }
    val contactFR = remember { FocusRequester() }
    val phoneFR = remember { FocusRequester() }
    val emailFR = remember { FocusRequester() }
    val addressFR = remember { FocusRequester() }
    val daysFR = remember { FocusRequester() }

    val saveAction = {
        if (name.isNotBlank()) {
            onSave(supplier.copy(name = name, contactName = contact, phone = phone, email = email, address = address, givesCredit = givesCredit, creditDays = creditDays.toIntOrNull() ?: 0))
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(if (supplier.id.startsWith("NEW")) "Nuevo Proveedor" else "Editar Proveedor", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre de la Empresa") }, modifier = Modifier.fillMaxWidth().focusRequester(nameFR).onPreviewKeyEvent { if (it.key == Key.Enter || it.key == Key.NumPadEnter) { if (it.type == KeyEventType.KeyDown) contactFR.requestFocus(); true } else false }, singleLine = true)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = contact, onValueChange = { contact = it }, label = { Text("Nombre del Encargado") }, modifier = Modifier.fillMaxWidth().focusRequester(contactFR).onPreviewKeyEvent { if (it.key == Key.Enter || it.key == Key.NumPadEnter) { if (it.type == KeyEventType.KeyDown) phoneFR.requestFocus(); true } else false }, singleLine = true)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Teléfono") }, modifier = Modifier.fillMaxWidth().focusRequester(phoneFR).onPreviewKeyEvent { if (it.key == Key.Enter || it.key == Key.NumPadEnter) { if (it.type == KeyEventType.KeyDown) emailFR.requestFocus(); true } else false }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Correo Electrónico") }, modifier = Modifier.fillMaxWidth().focusRequester(emailFR).onPreviewKeyEvent { if (it.key == Key.Enter || it.key == Key.NumPadEnter) { if (it.type == KeyEventType.KeyDown) addressFR.requestFocus(); true } else false }, singleLine = true)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Dirección") }, modifier = Modifier.fillMaxWidth().focusRequester(addressFR).onPreviewKeyEvent { if (it.key == Key.Enter || it.key == Key.NumPadEnter) { if (it.type == KeyEventType.KeyDown) { if (givesCredit) daysFR.requestFocus() else saveAction() }; true } else false }, minLines = 2)
        Spacer(Modifier.height(24.dp))
        Text("Condiciones Comerciales", fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = givesCredit, onCheckedChange = { givesCredit = it })
            Text("¿Ofrece Crédito?")
        }
        if (givesCredit) {
            OutlinedTextField(value = creditDays, onValueChange = { if (it.all { c -> c.isDigit() }) creditDays = it }, label = { Text("Días de Crédito") }, modifier = Modifier.width(150.dp).focusRequester(daysFR).onPreviewKeyEvent { if (it.key == Key.Enter || it.key == Key.NumPadEnter) { if (it.type == KeyEventType.KeyDown) saveAction(); true } else false }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }
        Spacer(Modifier.height(32.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onCancel) { Text("CANCELAR") }
            Spacer(Modifier.width(16.dp))
            Button(onClick = saveAction, enabled = name.isNotBlank()) { Text("GUARDAR PROVEEDOR") }
        }
    }
    LaunchedEffect(Unit) {
        repeat(3) {
            kotlinx.coroutines.delay(200)
            try { nameFR.requestFocus() } catch(e: Exception) {}
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, fontWeight = FontWeight.Bold, modifier = Modifier.width(100.dp))
        Text(value)
    }
}
