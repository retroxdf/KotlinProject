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
import kotlinx.coroutines.flow.flowOf
import com.abtsplazita.posplazita.currentTimeMillis
import androidx.compose.ui.text.style.TextAlign
import kotlinx.datetime.*

@Composable
fun PurchaseModule(viewModel: PurchaseViewModel) {
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

    val currentUser by viewModel.currentUser.collectAsState()
    val isAdmin = currentUser?.role == Role.SUPER_ADMIN || currentUser?.role == Role.GERENTE

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

    if (selectedProduct != null) {
        SimplePurchaseProductDetail(viewModel, selectedProduct!!)
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
            val headerModifier = if (isCompact) Modifier.fillMaxWidth() else Modifier.weight(1f)
            
            // --- CABECERA (PROVEEDOR + BUSCADOR) ---
            if (isCompact) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Captura de Compras", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    SupplierSelector(selectedSupplier, availableSuppliers, viewModel, onShowCreate = { showSupplierQuickCreate = true })
                    SearchBar(searchQuery, viewModel, focusRequester)
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Captura de Compras", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
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
                        Surface(
                            color = Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "⚠️ SELECCIONA UN PROVEEDOR PARA PODER GUARDAR", 
                                color = Color.Red, 
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                }
            },
            confirmButton = { TextButton(onClick = { showSaveDialog = false }) { Text("CERRAR (ESC)") } }
        )
        LaunchedEffect(Unit) { saveDialogFocusRequester.requestFocus() }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@Composable
fun SupplierSelector(selectedSupplier: Supplier?, availableSuppliers: List<Supplier>, viewModel: PurchaseViewModel, onShowCreate: () -> Unit) {
    var showSupplierMenu by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box {
            OutlinedButton(onClick = { showSupplierMenu = true }, border = BorderStroke(1.dp, if (selectedSupplier == null) Color.Red else MaterialTheme.colorScheme.primary)) {
                Icon(Icons.Default.LocalShipping, null); Spacer(Modifier.width(8.dp)); Text(selectedSupplier?.name ?: "SELECCIONAR PROVEEDOR")
            }
            DropdownMenu(expanded = showSupplierMenu, onDismissRequest = { showSupplierMenu = false }) {
                DropdownMenuItem(text = { Text("SIN PROVEEDOR") }, onClick = { viewModel.selectSupplier(null); showSupplierMenu = false })
                availableSuppliers.forEach { supplier ->
                    DropdownMenuItem(text = { Text(supplier.name) }, onClick = { viewModel.selectSupplier(supplier); showSupplierMenu = false }, leadingIcon = { if (selectedSupplier?.id == supplier.id) Icon(Icons.Default.Check, null) })
                }
                HorizontalDivider()
                DropdownMenuItem(text = { Text("CREAR PROVEEDOR...") }, onClick = { onShowCreate(); showSupplierMenu = false }, leadingIcon = { Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary) })
            }
        }
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onShowCreate) { Icon(Icons.Default.AddCircle, "Nuevo", tint = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
fun SearchBar(query: TextFieldValue, viewModel: PurchaseViewModel, focusRequester: FocusRequester) {
    OutlinedTextField(
        value = query,
        onValueChange = { viewModel.updateSearchQuery(it) },
        label = { Text("Escanea o busca producto...") },
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester).onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown) {
                when (event.key) {
                    Key.DirectionDown -> { viewModel.moveFocus(1); true }
                    Key.DirectionUp -> { viewModel.moveFocus(-1); true }
                    Key.Enter, Key.NumPadEnter -> { viewModel.onSearchSubmit(); true }
                    else -> false
                }
            } else false
        },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { viewModel.onSearchSubmit() })
    )
}

@Composable
fun SimplePurchaseProductDetail(viewModel: PurchaseViewModel, product: Product) {
    val suppliers by viewModel.availableSuppliers.collectAsState(emptyList())
    val productLinks by (viewModel.supplierRepository?.getSuppliersForProduct(product.id) ?: flowOf(emptyList())).collectAsState(emptyList())

    val initialFinalCost = product.cost * (1 + product.tax / 100)
    var qtyT by remember { mutableStateOf(TextFieldValue("1", TextRange(0, 1))) }
    var costT by remember { mutableStateOf(TextFieldValue(initialFinalCost.formatPrice())) }
    var totalT by remember { mutableStateOf(TextFieldValue(initialFinalCost.formatPrice())) }

    val qtyFocus = remember { FocusRequester() }
    val costFocus = remember { FocusRequester() }
    val totalFocus = remember { FocusRequester() }

    val blueColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color(0xFF2196F3),
        focusedBorderColor = Color(0xFF2196F3),
        focusedLabelColor = Color(0xFF2196F3)
    )

    var showEditMaster by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { viewModel.onSearchQueryClear() },
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Capturar Cantidad", modifier = Modifier.weight(1f))
                IconButton(onClick = { showEditMaster = true }) {
                    Icon(Icons.Default.Edit, "Editar Producto", tint = Color.Gray)
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.F12) {
                        viewModel.updateQuantity(qtyT)
                        viewModel.updateCost(costT)
                        viewModel.addToCart()
                        true
                    } else false
                }
            ) {
                Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                
                if (productLinks.isNotEmpty()) {
                    Text("Precios anteriores por proveedor:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    productLinks.take(3).forEach { link ->
                        val supName = suppliers.find { it.id == link.supplierId }?.name ?: "Desconocido"
                        Text("$supName: $${link.lastCost.formatPrice()}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                    }
                    Spacer(Modifier.height(4.dp))
                }
                OutlinedTextField(
                    value = qtyT,
                    onValueChange = { newValue ->
                        if (newValue.text.isEmpty() || newValue.text.all { it.isDigit() || it == '.' }) {
                            qtyT = newValue
                            val q = newValue.text.toDoubleOrNull() ?: 0.0
                            val c = costT.text.toDoubleOrNull() ?: 0.0
                            totalT = totalT.copy(text = (q * c).formatPrice())
                        }
                    },
                    label = { Text("Piezas") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(qtyFocus)
                        .onFocusChanged { if (it.isFocused) qtyT = qtyT.copy(selection = TextRange(0, qtyT.text.length)) }
                        .onPreviewKeyEvent { event ->
                            if (event.key == Key.Enter || event.key == Key.NumPadEnter) {
                                if (event.type == KeyEventType.KeyDown) costFocus.requestFocus()
                                true
                            } else false
                        },
                    singleLine = true,
                    colors = blueColors,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Column {
                    OutlinedTextField(
                        value = costT,
                        onValueChange = { newValue ->
                            if (newValue.text.isEmpty() || newValue.text.all { it.isDigit() || it == '.' }) {
                                costT = newValue
                                val c = newValue.text.toDoubleOrNull() ?: 0.0
                                val q = qtyT.text.toDoubleOrNull() ?: 0.0
                                totalT = totalT.copy(text = (q * c).formatPrice())
                            }
                        },
                        label = { Text("Costo Unitario") },
                        prefix = { Text("$ ") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(costFocus)
                            .onFocusChanged { if (it.isFocused) costT = costT.copy(selection = TextRange(0, costT.text.length)) }
                            .onPreviewKeyEvent { event ->
                                if (event.key == Key.Enter || event.key == Key.NumPadEnter) {
                                    if (event.type == KeyEventType.KeyDown) totalFocus.requestFocus()
                                    true
                                } else false
                            },
                        singleLine = true,
                        colors = blueColors,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Text(
                        text = if (product.tax > 0) "Costo Final (Incluye IVA ${product.tax.toInt()}%)" else "Costo Unitario",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (product.tax > 0) Color(0xFF4CAF50) else Color.Gray
                    )

                }

                OutlinedTextField(
                    value = totalT,
                    onValueChange = { newValue ->
                        if (newValue.text.isEmpty() || newValue.text.all { it.isDigit() || it == '.' }) {
                            totalT = newValue
                            val totalVal = newValue.text.toDoubleOrNull() ?: 0.0
                            val q = qtyT.text.toDoubleOrNull() ?: 1.0
                            if (q > 0) costT = costT.copy(text = (totalVal / q).formatPrice())
                        }
                    },
                    label = { Text("Total por estas piezas") },
                    prefix = { Text("$ ") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(totalFocus)
                        .onFocusChanged { if (it.isFocused) totalT = totalT.copy(selection = TextRange(0, totalT.text.length)) }
                        .onPreviewKeyEvent { event ->
                            if (event.key == Key.Enter || event.key == Key.NumPadEnter) {
                                if (event.type == KeyEventType.KeyDown) {
                                    viewModel.updateQuantity(qtyT)
                                    viewModel.updateCost(costT)
                                    viewModel.addToCart()
                                }
                                true
                            } else false
                        },
                    singleLine = true,
                    colors = blueColors,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            Button(onClick = { 
                viewModel.updateQuantity(qtyT)
                viewModel.updateCost(costT)
                viewModel.addToCart() 
            }) { Text("AGREGAR (Enter)") }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.onSearchQueryClear() }) { Text("CANCELAR") }
        }
    )
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

    LaunchedEffect(Unit) { 
        repeat(3) {
            kotlinx.coroutines.delay(200)
            try { qtyFocus.requestFocus() } catch(e: Exception) {}
        }
    }
}

