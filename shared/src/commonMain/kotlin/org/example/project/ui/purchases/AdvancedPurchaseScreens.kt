package com.abtsplazita.posplazita.ui.purchases

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.abtsplazita.posplazita.domain.formatPrice
import com.abtsplazita.posplazita.domain.PurchaseItem
import com.abtsplazita.posplazita.domain.Product
import com.abtsplazita.posplazita.domain.Supplier
import com.abtsplazita.posplazita.domain.Purchase
import com.abtsplazita.posplazita.domain.PurchaseUnit
import com.abtsplazita.posplazita.domain.Role
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.flow.flowOf
import com.abtsplazita.posplazita.currentTimeMillis
import androidx.compose.ui.text.style.TextAlign
import kotlinx.datetime.*

@Composable
fun AdvancedPurchaseModule(viewModel: PurchaseViewModel) {
    val cartItems by viewModel.cartItems.collectAsState()
    val total by viewModel.total.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val showResults by viewModel.showSearchResults.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val selectedIndex by viewModel.selectedSearchIndex.collectAsState()
    val lastSearchedBarcode by viewModel.lastSearchedBarcode.collectAsState()
    val selectedProduct by viewModel.selectedProduct.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    val selectedSupplier by viewModel.selectedSupplier.collectAsState()
    val availableSuppliers by viewModel.availableSuppliers.collectAsState(emptyList())

    val focusRequester = remember { FocusRequester() }
    val dialogFocusRequester = remember { FocusRequester() }
    val saveDialogFocusRequester = remember { FocusRequester() }
    val lazyListState = rememberLazyListState()

    var showQuickCreate by remember { mutableStateOf(false) }
    var showSupplierQuickCreate by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showSupplierSelection by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // --- CONTENIDO PRINCIPAL ---
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isCompact = maxWidth < 900.dp

            Column(modifier = Modifier.fillMaxSize().padding(16.dp)
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && !isSaving) {
                        when (event.key) {
                            Key.Escape -> {
                                if (cartItems.isNotEmpty() && !showResults) {
                                    if (selectedSupplier == null) showSupplierSelection = true else showSaveDialog = !showSaveDialog
                                    true
                                } else false
                            }
                            Key.F1 -> { if (cartItems.isNotEmpty()) { viewModel.savePurchase("Efectivo (Fondo)") { showSaveDialog = false }; true } else false }
                            Key.F12 -> { if (cartItems.isNotEmpty()) { viewModel.savePurchase("Transferencia") { showSaveDialog = false }; true } else false }
                            Key.F4 -> { if (cartItems.isNotEmpty()) { if (selectedSupplier != null) viewModel.savePurchase("Crédito") { showSaveDialog = false } else viewModel.setErrorMessage("Selecciona un proveedor para crédito (F4)"); true } else false }
                            else -> false
                        }
                    } else false
                }
            ) {
                // --- CABECERA (PROVEEDOR + BUSCADOR) ---
                if (isCompact) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Captura de Compras Avanzada", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        SupplierSelector(selectedSupplier, availableSuppliers, viewModel, onShowCreate = { showSupplierQuickCreate = true })
                        SearchBar(searchQuery, viewModel, focusRequester)
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Captura de Compras Avanzada", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        SupplierSelector(selectedSupplier, availableSuppliers, viewModel, onShowCreate = { showSupplierQuickCreate = true })
                    }
                    Spacer(Modifier.height(16.dp))
                    SearchBar(searchQuery, viewModel, focusRequester)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- LISTA DE COMPRA ---
                Card(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Cant.", modifier = Modifier.width(60.dp), fontWeight = FontWeight.Bold)
                            Text("Producto", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                            if (!isCompact) {
                                Text("Costo Unit.", modifier = Modifier.width(100.dp), fontWeight = FontWeight.Bold)
                                Text("Subtotal", modifier = Modifier.width(100.dp), fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(48.dp))
                        }
                        HorizontalDivider()
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(cartItems) { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                        .clickable { viewModel.editCartItem(item) }
                                        .padding(vertical = 4.dp), 
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${item.quantity}", modifier = Modifier.width(60.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.productName, fontWeight = FontWeight.Medium)
                                        if (isCompact) Text("$${item.costAtPurchase.formatPrice()} c/u | Total: $${item.subtotal.formatPrice()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    if (!isCompact) {
                                    val finalCost = item.costAtPurchase * (1 + item.taxRate / 100)
                                    Text("$${finalCost.formatPrice()}", modifier = Modifier.width(100.dp))
                                    Text("$${item.subtotal.formatPrice()}", modifier = Modifier.width(100.dp), fontWeight = FontWeight.Bold)
                                }
                                    IconButton(onClick = { viewModel.removeItem(item) }) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- RESUMEN Y BOTÓN GUARDAR ---
                Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.tertiaryContainer, shape = MaterialTheme.shapes.medium) {
                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("TOTAL COMPRA", style = MaterialTheme.typography.labelMedium)
                            Text("$${total.formatPrice()}", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                        }
                        Button(onClick = { showSaveDialog = true }, modifier = Modifier.height(56.dp).widthIn(min = 200.dp), enabled = cartItems.isNotEmpty() && !isSaving, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                            if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            else Text(if (isCompact) "GUARDAR" else "GUARDAR ENTRADA (ESC)", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }

        // --- VENTANA DE DETALLE DE PRODUCTO (DIALOG) ---
        if (selectedProduct != null) {
            Dialog(
                onDismissRequest = { viewModel.onSearchQueryClear() },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                AdvancedProductDetail(
                    viewModel = viewModel,
                    product = selectedProduct!!,
                    onDismiss = { viewModel.onSearchQueryClear() }
                )
            }
        }

        // --- OTROS DIÁLOGOS ---
        if (showSupplierSelection) {
            var supSearch by remember { mutableStateOf("") }
            val filtered = availableSuppliers.filter { it.name.contains(supSearch, ignoreCase = true) }
            val supFocus = remember { FocusRequester() }
            var focusedIdx by remember { mutableStateOf(0) }
            val lazyListStateSup = rememberLazyListState()

            AlertDialog(
                onDismissRequest = { showSupplierSelection = false },
                title = { Text("Seleccionar Proveedor") },
                text = {
                    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                        OutlinedTextField(
                            value = supSearch,
                            onValueChange = { supSearch = it; focusedIdx = 0 },
                            label = { Text("Buscar proveedor...") },
                            modifier = Modifier.fillMaxWidth().focusRequester(supFocus).onPreviewKeyEvent {
                                if (it.type == KeyEventType.KeyDown) {
                                    when (it.key) {
                                        Key.Enter, Key.NumPadEnter -> {
                                            if (filtered.isNotEmpty()) {
                                                viewModel.selectSupplier(filtered[focusedIdx])
                                                showSupplierSelection = false
                                                showSaveDialog = true
                                            }
                                            true
                                        }
                                        Key.DirectionDown -> {
                                            if (filtered.isNotEmpty()) {
                                                focusedIdx = (focusedIdx + 1) % filtered.size
                                            }
                                            true
                                        }
                                        Key.DirectionUp -> {
                                            if (filtered.isNotEmpty()) {
                                                focusedIdx = (focusedIdx - 1 + filtered.size) % filtered.size
                                            }
                                            true
                                        }
                                        else -> false
                                    }
                                } else false
                            },
                            singleLine = true
                        )
                        
                        Spacer(Modifier.height(12.dp))
                        
                        Button(
                            onClick = { 
                                showSupplierSelection = false
                                showSupplierQuickCreate = true 
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("NUEVO PROVEEDOR (Enter si no existe)")
                        }

                        Spacer(Modifier.height(12.dp))

                        LazyColumn(state = lazyListStateSup, modifier = Modifier.weight(1f)) {
                            itemsIndexed(filtered) { index, s ->
                                val isFocused = index == focusedIdx
                                Surface(
                                    modifier = Modifier.fillMaxWidth().clickable { 
                                        viewModel.selectSupplier(s)
                                        showSupplierSelection = false
                                        showSaveDialog = true
                                    },
                                    color = if (isFocused) Color(0xFF2196F3) else Color.Transparent,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    ListItem(
                                        headlineContent = { 
                                            Text(
                                                s.name, 
                                                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isFocused) Color.White else Color.Unspecified
                                            ) 
                                        },
                                        supportingContent = { 
                                            Text(
                                                s.contactName ?: "",
                                                color = if (isFocused) Color.White.copy(alpha = 0.8f) else Color.Unspecified
                                            ) 
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSupplierSelection = false }) { Text("CANCELAR") }
                }
            )
            
            LaunchedEffect(focusedIdx) {
                if (filtered.isNotEmpty()) {
                    lazyListStateSup.animateScrollToItem(focusedIdx)
                }
            }
            LaunchedEffect(Unit) {
                repeat(3) {
                    kotlinx.coroutines.delay(200)
                    try { supFocus.requestFocus() } catch(e: Exception) {}
                }
            }
        }

        if (showSupplierQuickCreate) {
            var supName by remember { mutableStateOf("") }
            var supContact by remember { mutableStateOf("") }
            var supPhone by remember { mutableStateOf("") }
            var givesCredit by remember { mutableStateOf(false) }
            var creditDays by remember { mutableStateOf("0") }

            val nameFR = remember { FocusRequester() }
            val contactFR = remember { FocusRequester() }
            val phoneFR = remember { FocusRequester() }
            val daysFR = remember { FocusRequester() }

            val saveAction = {
                val newSup = Supplier(
                    id = "S${currentTimeMillis()}",
                    name = supName,
                    contactName = supContact,
                    phone = supPhone,
                    givesCredit = givesCredit,
                    creditDays = creditDays.toIntOrNull() ?: 0
                )
                viewModel.quickCreateSupplier(newSup)
                showSupplierQuickCreate = false
            }

            AlertDialog(
                onDismissRequest = { showSupplierQuickCreate = false },
                title = { Text("Nuevo Proveedor Rápido") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = supName,
                            onValueChange = { supName = it },
                            label = { Text("Nombre Empresa") },
                            modifier = Modifier.fillMaxWidth().focusRequester(nameFR).onPreviewKeyEvent {
                                if (it.key == Key.Enter || it.key == Key.NumPadEnter) {
                                    if (it.type == KeyEventType.KeyDown) contactFR.requestFocus()
                                    true
                                } else false
                            },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = supContact,
                            onValueChange = { supContact = it },
                            label = { Text("Encargado") },
                            modifier = Modifier.fillMaxWidth().focusRequester(contactFR).onPreviewKeyEvent {
                                if (it.key == Key.Enter || it.key == Key.NumPadEnter) {
                                    if (it.type == KeyEventType.KeyDown) phoneFR.requestFocus()
                                    true
                                } else false
                            },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = supPhone,
                            onValueChange = { supPhone = it },
                            label = { Text("Teléfono") },
                            modifier = Modifier.fillMaxWidth().focusRequester(phoneFR).onPreviewKeyEvent {
                                if (it.key == Key.Enter || it.key == Key.NumPadEnter) {
                                    if (it.type == KeyEventType.KeyDown) {
                                        if (givesCredit) daysFR.requestFocus() else if (supName.isNotBlank()) saveAction()
                                    }
                                    true
                                } else false
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = givesCredit, onCheckedChange = { givesCredit = it })
                            Text("¿Da Crédito?")
                            if (givesCredit) {
                                Spacer(Modifier.width(8.dp))
                                OutlinedTextField(
                                    value = creditDays,
                                    onValueChange = { if (it.all { c -> c.isDigit() }) creditDays = it },
                                    label = { Text("Días") },
                                    modifier = Modifier.width(80.dp).focusRequester(daysFR).onPreviewKeyEvent {
                                        if (it.key == Key.Enter || it.key == Key.NumPadEnter) {
                                            if (it.type == KeyEventType.KeyDown && supName.isNotBlank()) saveAction()
                                            true
                                        } else false
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = saveAction, enabled = supName.isNotBlank()) { Text("GUARDAR (Enter)") }
                },
                dismissButton = { TextButton(onClick = { showSupplierQuickCreate = false }) { Text("CANCELAR") } }
            )
            LaunchedEffect(Unit) {
                repeat(3) {
                    kotlinx.coroutines.delay(200)
                    try { nameFR.requestFocus() } catch(e: Exception) {}
                }
            }
        }

        if (showQuickCreate) {
            var quickName by remember { mutableStateOf("") }
            var quickPrice by remember { mutableStateOf("") }
            var quickImage by remember { mutableStateOf("") }
            val nameFocus = remember { FocusRequester() }
            val priceFocus = remember { FocusRequester() }

            AlertDialog(
                onDismissRequest = { showQuickCreate = false },
                title = { Text("Registro Rápido") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = lastSearchedBarcode,
                            onValueChange = {},
                            label = { Text("Código") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2196F3))
                        )
                        OutlinedTextField(
                            value = quickName,
                            onValueChange = { quickName = it },
                            label = { Text("Nombre del Producto") },
                            modifier = Modifier.fillMaxWidth().focusRequester(nameFocus).onPreviewKeyEvent {
                                if (it.key == Key.Enter || it.key == Key.NumPadEnter) {
                                    if (it.type == KeyEventType.KeyDown) priceFocus.requestFocus()
                                    true
                                } else false
                            },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = quickPrice,
                            onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) quickPrice = it },
                            label = { Text("Costo Unitario") },
                            modifier = Modifier.fillMaxWidth().focusRequester(priceFocus),
                            prefix = { Text("$ ") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )

                        OutlinedTextField(
                            value = quickImage,
                            onValueChange = { quickImage = it },
                            label = { Text("URL Imagen (Opcional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { 
                        viewModel.quickCreateProduct(
                            lastSearchedBarcode, 
                            quickName, 
                            quickPrice.toDoubleOrNull() ?: 0.0,
                            quickImage.ifBlank { null }
                        )
                        showQuickCreate = false 
                    }, enabled = quickName.isNotBlank()) { Text("GUARDAR Y AGREGAR") }
                },
                dismissButton = { TextButton(onClick = { showQuickCreate = false }) { Text("CANCELAR") } }
            )
            LaunchedEffect(Unit) { 
                repeat(3) {
                    kotlinx.coroutines.delay(200)
                    try { nameFocus.requestFocus() } catch(e: Exception) {}
                }
            }
        }

        if (errorMessage != null) {
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                confirmButton = { Button(onClick = { viewModel.clearError() }) { Text("OK") } },
                title = { Text("Error") },
                text = { Text(errorMessage!!) }
            )
        }

        if (showResults) {
            AlertDialog(
                onDismissRequest = { viewModel.onSearchQueryClear() },
                confirmButton = {
                    Button(onClick = { viewModel.onSearchQueryClear(); showQuickCreate = true }) {
                        Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("REGISTRAR NUEVO")
                    }
                },
                dismissButton = { TextButton(onClick = { viewModel.onSearchQueryClear() }) { Text("Cerrar (Esc)") } },
                title = { Text("Resultados para Compra") },
                text = {
                    Box(modifier = Modifier.fillMaxSize().focusRequester(dialogFocusRequester).focusable()
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown) {
                                when (event.key) {
                                    Key.DirectionDown -> { viewModel.moveFocus(1); true }
                                    Key.DirectionUp -> { viewModel.moveFocus(-1); true }
                                    Key.Enter, Key.NumPadEnter -> { viewModel.selectCurrentSearchItem(); true }
                                    else -> false
                                }
                            } else false
                        }
                    ) {
                        if (searchResults.isEmpty()) {
                            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.SearchOff, null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                                Spacer(Modifier.height(16.dp))
                                Text("No se encontró ningún producto con: $lastSearchedBarcode", textAlign = TextAlign.Center)
                            }
                        } else {
                            LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize()) {
                                itemsIndexed(searchResults) { index, product ->
                                    val isSelected = index == selectedIndex
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).clickable { viewModel.selectProduct(product) },
                                        color = if (isSelected) Color(0xFF2196F3) else Color.Transparent,
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        ListItem(
                                            headlineContent = { 
                                                Text(
                                                    product.name, 
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) Color.White else Color.Unspecified
                                                ) 
                                            },
                                            supportingContent = { 
                                                Text(
                                                    "Código: ${product.barcode} | Costo actual: $${product.cost.formatPrice()}",
                                                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.Unspecified
                                                ) 
                                            },
                                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                        )
                                    }
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            )
            LaunchedEffect(Unit) { kotlinx.coroutines.delay(100); dialogFocusRequester.requestFocus() }
            LaunchedEffect(selectedIndex) { if (searchResults.isNotEmpty()) lazyListState.animateScrollToItem(selectedIndex) }
        }

        if (showSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                modifier = Modifier.onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && !isSaving) {
                        when (event.key) {
                            Key.F1 -> { viewModel.savePurchase("Efectivo (Fondo)") { showSaveDialog = false }; true }
                            Key.F12 -> { viewModel.savePurchase("Transferencia") { showSaveDialog = false }; true }
                            Key.F4 -> { if (selectedSupplier != null) viewModel.savePurchase("Crédito") { showSaveDialog = false } else viewModel.setErrorMessage("Selecciona un proveedor para crédito (F4)"); true }
                            Key.Escape -> { showSaveDialog = false; true }
                            else -> false
                        }
                    } else false
                },
                title = { Text("Finalizar Entrada") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.focusRequester(saveDialogFocusRequester).focusable()) {
                        Text("¿Cómo se pagó esta compra? Presiona la tecla rápida:", style = MaterialTheme.typography.bodyMedium)
                        listOf("F1" to "Efectivo (Fondo)", "F12" to "Transferencia", "F4" to "Crédito").forEach { (key, method) ->
                            Card(modifier = Modifier.fillMaxWidth().clickable { if (method != "Crédito" || selectedSupplier != null) viewModel.savePurchase(method) { showSaveDialog = false } }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                                    Surface(color = MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small) { Text(key, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontWeight = FontWeight.Bold) }
                                    Spacer(Modifier.width(16.dp))
                                    Text(method, style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                        if (selectedSupplier == null) {
                            Text(
                                "⚠️ SELECCIONE UN PROVEEDOR", 
                                color = Color.Red, 
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Black
                            )
                        }

                    }
                },
                confirmButton = { TextButton(onClick = { showSaveDialog = false }) { Text("CERRAR (ESC)") } }
            )
            LaunchedEffect(Unit) { saveDialogFocusRequester.requestFocus() }
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@Composable
fun AdvancedProductDetail(
    viewModel: PurchaseViewModel,
    product: Product,
    onDismiss: () -> Unit
) {
    val purchaseUnit by viewModel.purchaseUnit.collectAsState()
    val purchaseQuantityText by viewModel.purchaseQuantityText.collectAsState()
    val purchaseCostText by viewModel.purchaseCostText.collectAsState()
    val purchaseDiscountAmountText by viewModel.purchaseDiscountAmountText.collectAsState()
    val purchaseFactorText by viewModel.purchaseFactorText.collectAsState()
    val discountPercentText by viewModel.discountPercentText.collectAsState()
    val taxRateText by viewModel.taxRateText.collectAsState()
    val showUnitDialog by viewModel.showUnitDialog.collectAsState()

    var showTaxMenu by remember { mutableStateOf(false) }
    var showEditMaster by remember { mutableStateOf(false) }

    val qtyFocus = remember { FocusRequester() }
    val factorFocus = remember { FocusRequester() }
    val taxFocus = remember { FocusRequester() }
    val costFocus = remember { FocusRequester() }
    val addFocus = remember { FocusRequester() }

    val pQty = purchaseQuantityText.text.toDoubleOrNull() ?: 0.0
    val pCost = purchaseCostText.text.toDoubleOrNull() ?: 0.0
    val pFactor = purchaseFactorText.text.toDoubleOrNull() ?: 1.0
    val pDiscountPercent = discountPercentText.text.toDoubleOrNull() ?: 0.0
    val pDiscountAmount = purchaseDiscountAmountText.text.toDoubleOrNull() ?: 0.0
    val pTax = taxRateText.text.toDoubleOrNull() ?: 0.0

    val totalCost = pQty * pCost
    val discountFromPercent = totalCost * (pDiscountPercent / 100)
    val subtotalWithoutTax = totalCost - pDiscountAmount - discountFromPercent
    
    val baseQuantity = pQty * pFactor

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF5F5F5)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Detalle de Compra", color = Color.White, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showEditMaster = true }) {
                        Icon(Icons.Default.Edit, "Editar Producto", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1976D2))
            )

            Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                // --- 1. CARD: INFORMACIÓN DEL PRODUCTO ---
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(60.dp).background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Image, null, modifier = Modifier.size(30.dp), tint = Color.LightGray)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(product.barcode, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Costo Actual (PZA)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text("Sin IVA: $${product.cost.formatPrice()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            if (product.tax > 0.0) {
                                Text("Con IVA: $${(product.cost * (1 + product.tax / 100)).formatPrice()}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32))
                            }
                        }
                    }
                }

                // --- 2. CARD: CANTIDAD Y UNIDAD ---
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("CAPTURA DE CANTIDAD", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(Modifier.height(12.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Cantidad", style = MaterialTheme.typography.labelMedium)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { 
                                        val current = pQty
                                        if (current > 0) viewModel.updatePurchaseQuantity(TextFieldValue((current - 1).toString()))
                                    }) { Icon(Icons.Default.RemoveCircle, null, tint = Color.LightGray) }
                                    
                                    OutlinedTextField(
                                        value = purchaseQuantityText,
                                        onValueChange = { viewModel.updatePurchaseQuantity(it) },
                                        modifier = Modifier.width(90.dp).focusRequester(qtyFocus).onPreviewKeyEvent {
                                            if (it.type == KeyEventType.KeyDown && (it.key == Key.Enter || it.key == Key.NumPadEnter)) {
                                                factorFocus.requestFocus()
                                                true
                                            } else false
                                        }.onFocusChanged { 
                                            if (it.isFocused) viewModel.updatePurchaseQuantity(purchaseQuantityText.copy(selection = TextRange(0, purchaseQuantityText.text.length)))
                                        },
                                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Black),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true
                                    )
                                    
                                    IconButton(onClick = { 
                                        val current = pQty
                                        viewModel.updatePurchaseQuantity(TextFieldValue((current + 1).toString()))
                                    }) { Icon(Icons.Default.AddCircle, null, tint = Color(0xFF1976D2)) }
                                }
                            }
                            
                            Spacer(Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Unidad / Caja", style = MaterialTheme.typography.labelMedium)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Selector de Unidad
                                    Box(
                                        modifier = Modifier.weight(0.4f).height(56.dp)
                                            .background(Color(0xFFF8F9FA), RoundedCornerShape(4.dp))
                                            .clickable { viewModel.openUnitDialog() }
                                            .padding(horizontal = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(purchaseUnit?.name ?: "PZA", fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                    
                                    Spacer(Modifier.width(8.dp))
                                    
                                    // Factor (Editable en azul)
                                    OutlinedTextField(
                                        value = purchaseFactorText,
                                        onValueChange = { viewModel.updatePurchaseFactor(it) },
                                        modifier = Modifier.weight(0.6f).focusRequester(factorFocus).onPreviewKeyEvent {
                                            if (it.type == KeyEventType.KeyDown && (it.key == Key.Enter || it.key == Key.NumPadEnter)) {
                                                taxFocus.requestFocus()
                                                true
                                            } else false
                                        }.onFocusChanged {
                                            if (it.isFocused) viewModel.updatePurchaseFactor(purchaseFactorText.copy(selection = TextRange(0, purchaseFactorText.text.length)))
                                        },
                                        textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Black, color = Color(0xFF1976D2)),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        label = { Text("Contenido") }
                                    )
                                }
                            }
                        }
                        
                        val baseUnitLabel = if (product.unit == com.abtsplazita.posplazita.domain.UnitType.KG) "KG" else "PZA"
                        Text(
                            "Total en Inventario: ${baseQuantity.formatPrice()} $baseUnitLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF1976D2),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                // --- 3. CARD: COSTOS E IMPUESTOS ---
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("COSTOS E IMPUESTOS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(Modifier.height(12.dp))
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            // Selector de Impuestos
                            Box(modifier = Modifier.weight(1f)) {
                                Column {
                                    val taxOptions = listOf(0.0, 8.0, 16.0)
                                    Text("Impuesto", style = MaterialTheme.typography.labelMedium)
                                    OutlinedButton(
                                        onClick = { showTaxMenu = true },
                                        modifier = Modifier.fillMaxWidth().height(56.dp).focusRequester(taxFocus).onPreviewKeyEvent {
                                            if (it.type == KeyEventType.KeyDown) {
                                                when (it.key) {
                                                    Key.Spacebar -> { showTaxMenu = true; true }
                                                    Key.Enter, Key.NumPadEnter -> { costFocus.requestFocus(); true }
                                                    Key.DirectionDown -> {
                                                        val nextIdx = (taxOptions.indexOf(pTax) + 1) % taxOptions.size
                                                        viewModel.updateTaxRate(TextFieldValue(taxOptions[nextIdx].toString()))
                                                        true
                                                    }
                                                    Key.DirectionUp -> {
                                                        val nextIdx = (taxOptions.indexOf(pTax) - 1 + taxOptions.size) % taxOptions.size
                                                        viewModel.updateTaxRate(TextFieldValue(taxOptions[nextIdx].toString()))
                                                        true
                                                    }
                                                    else -> false
                                                }
                                            } else false
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(if (pTax > 0) "IVA ${pTax.toInt()}%" else "Sin IVA")
                                        Spacer(Modifier.weight(1f))
                                        Icon(Icons.Default.ArrowDropDown, null)
                                    }
                                }
                                DropdownMenu(expanded = showTaxMenu, onDismissRequest = { showTaxMenu = false }) {
                                    listOf(0.0, 8.0, 16.0).forEach { rate ->
                                        DropdownMenuItem(
                                            text = { Text(if (rate == 0.0) "Sin IVA" else "IVA ${rate.toInt()}%") },
                                            onClick = {
                                                viewModel.updateTaxRate(TextFieldValue(rate.toString()))
                                                showTaxMenu = false
                                                costFocus.requestFocus()
                                            }
                                        )
                                    }
                                }
                            }

                            // Costo Base
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Costo Sin IVA", style = MaterialTheme.typography.labelMedium, color = Color(0xFF1976D2))
                                OutlinedTextField(
                                    value = purchaseCostText,
                                    onValueChange = { viewModel.updatePurchaseCost(it) },
                                    modifier = Modifier.fillMaxWidth().focusRequester(costFocus).onPreviewKeyEvent {
                                        if (it.type == KeyEventType.KeyDown && (it.key == Key.Enter || it.key == Key.NumPadEnter)) {
                                            addFocus.requestFocus()
                                            true
                                        } else false
                                    }.onFocusChanged {
                                        if (it.isFocused) viewModel.updatePurchaseCost(purchaseCostText.copy(selection = TextRange(0, purchaseCostText.text.length)))
                                    },
                                    textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold),
                                    prefix = { Text("$ ") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true
                                )
                            }

                            // Costo con IVA
                            if (pTax > 0) {
                                val purchaseFinalCostText by viewModel.purchaseFinalCostText.collectAsState()
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Costo Con IVA", style = MaterialTheme.typography.labelMedium, color = Color(0xFF2E7D32))
                                    OutlinedTextField(
                                        value = purchaseFinalCostText,
                                        onValueChange = { viewModel.updatePurchaseFinalCost(it) },
                                        modifier = Modifier.fillMaxWidth(),
                                        textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32)),
                                        prefix = { Text("$ ") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color(0xFFE8F5E9),
                                            unfocusedContainerColor = Color(0xFFE8F5E9)
                                        )
                                    )
                                }
                            }

                            // Costo Informativo por Unidad
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text("Costo p/Unidad", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                val unitLabel = if (product.unit == com.abtsplazita.posplazita.domain.UnitType.KG) "kg" else "pza"
                                if (pFactor > 0) {
                                    Text("Neto: $${(pCost / pFactor).formatPrice()} / $unitLabel", color = Color(0xFF1976D2), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    if (pTax > 0) {
                                        Text("c/IVA: $${((pCost * (1 + pTax / 100)) / pFactor).formatPrice()} / $unitLabel", color = Color(0xFF2E7D32), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // --- 4. LINEA FINAL COMPACTA (DESCUENTOS, TOTAL, AGREGAR) ---
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Descuentos en chiquito
                    Column(modifier = Modifier.weight(0.2f)) {
                        OutlinedTextField(
                            value = discountPercentText,
                            onValueChange = { viewModel.updateDiscountPercent(it) },
                            label = { Text("% Desc", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodySmall,
                            prefix = { Text("%", style = MaterialTheme.typography.labelSmall) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                    }
                    Column(modifier = Modifier.weight(0.25f)) {
                        OutlinedTextField(
                            value = purchaseDiscountAmountText,
                            onValueChange = { viewModel.updatePurchaseDiscountAmount(it) },
                            label = { Text("$ Desc", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodySmall,
                            prefix = { Text("$", style = MaterialTheme.typography.labelSmall) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                    }

                    // Total
                    Surface(
                        modifier = Modifier.weight(0.35f),
                        color = Color(0xFFE3F2FD),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("TOTAL LÍNEA", style = MaterialTheme.typography.labelSmall, color = Color(0xFF0D47A1))
                            Text("$${subtotalWithoutTax.formatPrice()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color(0xFF0D47A1))
                        }
                    }

                    // Botón Agregar
                    Button(
                        onClick = { viewModel.addToCart(true) },
                        modifier = Modifier.weight(0.2f).height(56.dp).focusRequester(addFocus).onPreviewKeyEvent {
                            if (it.type == KeyEventType.KeyDown && (it.key == Key.Enter || it.key == Key.NumPadEnter)) {
                                viewModel.addToCart(true)
                                true
                            } else false
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                    ) {
                        Text("AGREGAR", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }

    LaunchedEffect(Unit) {
        qtyFocus.requestFocus()
    }

    if (showUnitDialog) {
        AdvancedUnitSelectionDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.closeUnitDialog() }
        )
    }

    if (showEditMaster) {
        var newName by remember { mutableStateOf(product.name) }
        var newBarcode by remember { mutableStateOf(product.barcode) }
        
        AlertDialog(
            onDismissRequest = { showEditMaster = false },
            title = { Text("Editar Datos del Producto") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Nombre") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newBarcode,
                        onValueChange = { newBarcode = it },
                        label = { Text("Código de Barras") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateProductMasterData(newName, newBarcode)
                    showEditMaster = false
                }) { Text("GUARDAR") }
            },
            dismissButton = {
                TextButton(onClick = { showEditMaster = false }) { Text("CANCELAR") }
            }
        )
    }
}


@Composable
fun AdvancedUnitSelectionDialog(
    viewModel: PurchaseViewModel,
    onDismiss: () -> Unit
) {
    val units by viewModel.availableUnits.collectAsState(emptyList())
    var searchText by remember { mutableStateOf("") }
    val filtered = units.filter { it.name.contains(searchText, ignoreCase = true) }

    var showAddDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, null, tint = Color.Gray)
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("Buscar") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, null, tint = Color(0xFF4CAF50))
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                LazyColumn {
                    items(filtered) { unit ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { 
                                viewModel.updatePurchaseUnit(unit)
                                onDismiss()
                            }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val selectedUnit by viewModel.purchaseUnit.collectAsState()
                            Icon(
                                Icons.Default.Check, 
                                null, 
                                tint = if (selectedUnit?.id == unit.id) Color(0xFF1976D2) else Color.Transparent
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(unit.name, modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.deleteUnit(unit) }) {
                                Icon(Icons.Default.Delete, null, tint = Color.Red)
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {}
    )

    if (showAddDialog) {
        var newName by remember { mutableStateOf("") }
        var newFactor by remember { mutableStateOf("1") }
        
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Nueva Unidad de Compra") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Nombre (Ej: CAJA)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newFactor,
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) newFactor = it },
                        label = { Text("Factor (Piezas por unidad)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.quickCreateUnit(newName, newFactor.toDoubleOrNull() ?: 1.0)
                    showAddDialog = false
                }, enabled = newName.isNotBlank()) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancelar") }
            }
        )
    }
}
