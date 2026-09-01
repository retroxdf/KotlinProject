package com.abtsplazita.posplazita.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abtsplazita.posplazita.domain.formatPrice
import com.abtsplazita.posplazita.ui.customers.CustomerEditDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    viewModel: PosViewModel,
    onCancel: () -> Unit
) {
    val total by viewModel.total.collectAsState()
    val items by viewModel.currentItems.collectAsState()
    val amountPaidText by viewModel.amountPaidText.collectAsState()
    val paymentMethod by viewModel.paymentMethod.collectAsState()
    val saleChange by viewModel.saleChange.collectAsState()
    val isProcessing by viewModel.isProcessingSale.collectAsState()
    val isWaitingMP by viewModel.isWaitingForMP.collectAsState()
    val mpStatus by viewModel.mpStatus.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val warningMessage by viewModel.warningMessage.collectAsState()
    
    val showCustomerDialog by viewModel.showCustomerDialog.collectAsState()
    val showAddCustomerDialog by viewModel.showAddCustomerDialog.collectAsState()
    val editingCustomer by viewModel.editingCustomer.collectAsState()
    
    val focusRequester = remember { FocusRequester() }
    val amountFocusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        amountFocusRequester.requestFocus()
    }

    Scaffold(
        modifier = Modifier.onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown) {
                when (event.key) {
                    Key.F1 -> { 
                        if (!isProcessing && !isWaitingMP && (paymentMethod != "Efectivo" || (saleChange ?: -1.0) >= 0)) {
                            viewModel.completeSale(shouldPrint = true, onDone = onCancel)
                        }
                        true 
                    }
                    Key.F12 -> { 
                        if (!isProcessing && !isWaitingMP && (paymentMethod != "Efectivo" || (saleChange ?: -1.0) >= 0)) {
                            viewModel.completeSale(shouldPrint = false, onDone = onCancel)
                            viewModel.openCashDrawer()
                        }
                        true 
                    }
                    Key.F4 -> { 
                        viewModel.setPaymentMethod("Crédito")
                        viewModel.completeSale(shouldPrint = true, onDone = onCancel)
                        true 
                    }
                    Key.Enter, Key.NumPadEnter -> {
                        if (!isProcessing && !isWaitingMP && (paymentMethod != "Efectivo" || (saleChange ?: -1.0) >= 0)) {
                            viewModel.completeSale(shouldPrint = true, onDone = onCancel)
                        }
                        true
                    }
                    Key.Escape -> { viewModel.cancelCheckout(); onCancel(); true }
                    else -> false
                }
            } else false
        }.focusRequester(focusRequester).focusable(),
        topBar = {
            TopAppBar(
                title = { Text("FINALIZAR VENTA", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.cancelCheckout(); onCancel() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        },
        snackbarHost = {
            errorMessage?.let { msg ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = { TextButton(onClick = { viewModel.clearError() }) { Text("OK", color = Color.White) } },
                    containerColor = MaterialTheme.colorScheme.error
                ) { Text(msg) }
            }
            warningMessage?.let { msg ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = { TextButton(onClick = { viewModel.clearWarning() }) { Text("OK", color = Color.White) } },
                    containerColor = Color(0xFF4CAF50)
                ) { Text(msg) }
            }
        }
    ) { padding ->
        BoxWithConstraints(modifier = Modifier.padding(padding).fillMaxSize()) {
            val isCompact = maxWidth < 700.dp
            
            if (isCompact) {
                // MÓVIL: Una sola columna con scroll
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    // RESUMEN RÁPIDO DEL TOTAL ARRIBA
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("TOTAL A PAGAR", style = MaterialTheme.typography.labelSmall)
                            Text("$${total.formatPrice()}", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black)
                        }
                    }

                    // MÉTODOS DE PAGO
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Método de Pago", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PaymentMethodButton("Efectivo", Icons.Default.Money, paymentMethod == "Efectivo", Modifier.weight(1f)) { viewModel.setPaymentMethod("Efectivo") }
                            PaymentMethodButton("Tarjeta", Icons.Default.CreditCard, paymentMethod == "Tarjeta", Modifier.weight(1f)) { viewModel.setPaymentMethod("Tarjeta") }
                            PaymentMethodButton("Transferencia", Icons.Default.SyncAlt, paymentMethod == "Transferencia", Modifier.weight(1f)) { viewModel.setPaymentMethod("Transferencia") }
                        }

                        if (paymentMethod == "Efectivo") {
                            Spacer(Modifier.height(24.dp))
                            Column {
                                Text("EFECTIVO RECIBIDO", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    value = amountPaidText,
                                    onValueChange = { viewModel.updateAmountPaid(it) },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).focusRequester(amountFocusRequester),
                                    textStyle = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                )
                                
                                saleChange?.let { change ->
                                    Spacer(Modifier.height(16.dp))
                                    Surface(
                                        color = if (change >= 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                        border = BorderStroke(2.dp, if (change >= 0) Color(0xFF2E7D32) else Color.Red)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(if (change >= 0) "CAMBIO" else "FALTANTE", fontWeight = FontWeight.Bold, color = if (change >= 0) Color(0xFF2E7D32) else Color.Red)
                                            Text("$${change.formatPrice()}", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = if (change >= 0) Color(0xFF2E7D32) else Color.Red)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(32.dp))
                        
                        // BOTONES DE ACCIÓN (Móvil)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.completeSale(shouldPrint = true, onDone = onCancel) },
                                modifier = Modifier.fillMaxWidth().height(60.dp),
                                enabled = !isProcessing && !isWaitingMP && (paymentMethod != "Efectivo" || (saleChange ?: -1.0) >= 0),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                            ) {
                                if (isProcessing || isWaitingMP) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                else Text("TERMINAR E IMPRIMIR", fontWeight = FontWeight.Bold)
                            }
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.setPaymentMethod("Crédito"); viewModel.completeSale(true, onCancel) },
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7)),
                                    shape = RoundedCornerShape(8.dp)
                                ) { Text("CRÉDITO", style = MaterialTheme.typography.labelMedium) }

                                Button(
                                    onClick = { viewModel.completeSale(false, onDone = onCancel); viewModel.openCashDrawer() },
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                    shape = RoundedCornerShape(8.dp),
                                    enabled = !isProcessing && !isWaitingMP && (paymentMethod != "Efectivo" || (saleChange ?: -1.0) >= 0)
                                ) { Text("SOLO CAJÓN", style = MaterialTheme.typography.labelMedium) }
                            }
                        }
                    }

                    // LISTA DE PRODUCTOS AL FINAL EN MÓVIL
                    Text("Detalle de Venta", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontWeight = FontWeight.Bold, color = Color.Gray)
                    Column(modifier = Modifier.background(Color.Gray.copy(alpha = 0.05f))) {
                        items.forEach { item ->
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("${item.quantity}", modifier = Modifier.width(40.dp), fontWeight = FontWeight.Black)
                                Text(item.productName, modifier = Modifier.weight(1f))
                                Text("$${item.subtotal.formatPrice()}", fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
                        }
                    }
                }
            } else {
                // ESCRITORIO / TABLET: Layout de dos columnas
                Row(modifier = Modifier.fillMaxSize()) {
                    // LADO IZQUIERDO: PRODUCTOS QUE LLEVAN
                    Column(modifier = Modifier.weight(1f).fillMaxHeight().background(Color.Gray.copy(alpha = 0.05f)).padding(16.dp)) {
                        Text("PRODUCTOS EN EL CARRITO", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            itemsIndexed(items) { index, item ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("${item.quantity}", modifier = Modifier.width(45.dp), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                    Text(item.productName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                                    Text("$${item.subtotal.formatPrice()}", fontWeight = FontWeight.Bold)
                                }
                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                            }
                        }
                    }

                    // LADO DERECHO: TOTAL Y OPCIONES
                    Column(modifier = Modifier.weight(1.2f).fillMaxHeight().padding(32.dp)) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("TOTAL NETO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text("$${total.formatPrice()}", style = MaterialTheme.typography.displayLarge.copy(fontSize = 70.sp), fontWeight = FontWeight.Black)
                            }
                        }

                        Spacer(Modifier.height(32.dp))

                        Text("Método de Pago", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PaymentMethodButton("Efectivo", Icons.Default.Money, paymentMethod == "Efectivo", Modifier.width(120.dp).height(80.dp)) { viewModel.setPaymentMethod("Efectivo") }
                            PaymentMethodButton("Tarjeta", Icons.Default.CreditCard, paymentMethod == "Tarjeta", Modifier.width(120.dp).height(80.dp)) { viewModel.setPaymentMethod("Tarjeta") }
                            PaymentMethodButton("Transferencia", Icons.Default.SyncAlt, paymentMethod == "Transferencia", Modifier.width(120.dp).height(80.dp)) { viewModel.setPaymentMethod("Transferencia") }
                        }

                        Spacer(Modifier.height(32.dp))

                        if (paymentMethod == "Efectivo") {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("EFECTIVO RECIBIDO", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    OutlinedTextField(
                                        value = amountPaidText,
                                        onValueChange = { viewModel.updateAmountPaid(it) },
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).focusRequester(amountFocusRequester),
                                        textStyle = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                    )
                                }
                                
                                Spacer(Modifier.width(24.dp))
                                
                                saleChange?.let { change ->
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(if (change >= 0) "CAMBIO" else "FALTANTE", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = if (change >= 0) Color(0xFF2E7D32) else Color.Red)
                                        Surface(
                                            color = if (change >= 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                            border = BorderStroke(2.dp, if (change >= 0) Color(0xFF2E7D32) else Color.Red)
                                        ) {
                                            Text(
                                                text = "$${change.formatPrice()}",
                                                style = MaterialTheme.typography.displayMedium,
                                                fontWeight = FontWeight.Black,
                                                color = if (change >= 0) Color(0xFF2E7D32) else Color.Red,
                                                modifier = Modifier.padding(16.dp),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.weight(1f))

                        // BOTONES DE ACCIÓN (F1, F12, F4)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(
                                onClick = { viewModel.setPaymentMethod("Crédito"); viewModel.completeSale(true, onCancel) },
                                modifier = Modifier.weight(1f).height(64.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("F4", fontWeight = FontWeight.Black)
                                    Text("CRÉDITO", style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            Button(
                                onClick = { 
                                    viewModel.completeSale(shouldPrint = false, onDone = onCancel)
                                    viewModel.openCashDrawer()
                                },
                                modifier = Modifier.weight(1f).height(64.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isProcessing && !isWaitingMP && (paymentMethod != "Efectivo" || (saleChange ?: -1.0) >= 0)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("F12", fontWeight = FontWeight.Black)
                                    Text("SOLO CAJÓN", style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            Button(
                                onClick = { viewModel.completeSale(shouldPrint = true, onDone = onCancel) },
                                modifier = Modifier.weight(2f).height(64.dp),
                                enabled = !isProcessing && !isWaitingMP && (paymentMethod != "Efectivo" || (saleChange ?: -1.0) >= 0),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                            ) {
                                if (isProcessing || isWaitingMP) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text(if (isWaitingMP) "CONECTANDO..." else "PROCESANDO...")
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Print, null, modifier = Modifier.size(24.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Column {
                                            Text("F1 - TERMINAR", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                                            Text("COBRAR E IMPRIMIR", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // DIALOGO DE ESPERA PARA MERCADO PAGO
    if (isWaitingMP) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Procesando Pago con Tarjeta", fontWeight = FontWeight.Black) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    CircularProgressIndicator(modifier = Modifier.size(64.dp), strokeWidth = 6.dp)
                    Spacer(Modifier.height(24.dp))
                    Text(
                        mpStatus ?: "Iniciando terminal...",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Siga las instrucciones en la terminal de Mercado Pago.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.cancelMpPayment() }) {
                    Text("CANCELAR", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // --- DIÁLOGOS DE CLIENTES (TAMBIÉN DISPONIBLES EN COBRO) ---
    if (showCustomerDialog) {
        CustomerSelectionDialog(viewModel, onDismiss = { viewModel.closeCustomerDialog() })
    }
    
    if (showAddCustomerDialog && editingCustomer != null) {
        CustomerEditDialog(
            customer = editingCustomer!!,
            onUpdate = { viewModel.updateEditingCustomer(it) },
            onSave = { viewModel.saveNewCustomer() },
            onCancel = { viewModel.closeAddCustomerDialog() }
        )
    }
}

@Composable
fun PaymentMethodButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        border = if (isSelected) null else BorderStroke(1.dp, Color.LightGray)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
        }
    }
}
