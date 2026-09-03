package com.abtsplazita.posplazita.ui.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.*
import com.abtsplazita.posplazita.domain.formatPrice

@Composable
fun InventoryModule(viewModel: InventoryViewModel) {
    val inventoryData by viewModel.inventoryData.collectAsState()
    val branches by viewModel.branches.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    var showAdjustmentDialog by remember { mutableStateOf(false) }
    var editingProductId by remember { mutableStateOf<String?>(null) }
    var editingValue by remember { mutableStateOf(TextFieldValue("")) }

    val lazyListState = rememberLazyListState()

    // Trigger para cargar más
    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastIndex ->
                if (lastIndex != null && lastIndex >= inventoryData.size - 10) {
                    viewModel.loadMore()
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventarios Multisucursal") },
                actions = {
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, "Menú")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Ajuste de inventario") },
                                leadingIcon = { Icon(Icons.Default.EditAttributes, null) },
                                onClick = { 
                                    showMenu = false
                                    showAdjustmentDialog = true 
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            // Buscador Principal
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                label = { Text("Buscar producto por nombre o código...") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { /* Ya se filtra por flow */ })
            )

            // Cabecera Dinámica
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Código", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold)
                    Text("Producto", modifier = Modifier.weight(3f), fontWeight = FontWeight.Bold)
                    
                    branches.forEach { branch ->
                        val isCurrent = branch.id == viewModel.branchId
                        Text(
                            text = if (isCurrent) branch.name else branch.name,
                            modifier = Modifier.weight(2f),
                            fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Bold,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Unspecified,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize()) {
                items(inventoryData) { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.product.barcode, modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.bodySmall)
                        Text(item.product.name, modifier = Modifier.weight(3f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        
                        branches.forEach { branch ->
                            val stock = item.branchStocks[branch.id] ?: 0.0
                            val isCurrent = branch.id == viewModel.branchId
                            
                            Box(modifier = Modifier.weight(2f), contentAlignment = Alignment.Center) {
                                if (isCurrent && editingProductId == item.product.id) {
                                    OutlinedTextField(
                                        value = editingValue,
                                        onValueChange = { if (it.text.isEmpty() || it.text.all { c -> c.isDigit() || c == '.' }) editingValue = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onFocusChanged { 
                                                if (it.isFocused) {
                                                    editingValue = editingValue.copy(selection = TextRange(0, editingValue.text.length))
                                                }
                                            },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                                        trailingIcon = {
                                            IconButton(onClick = {
                                                val next = editingValue.text.toDoubleOrNull() ?: stock
                                                viewModel.updateQuickStock(item.product.id, next)
                                                editingProductId = null
                                            }) {
                                                Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                                            }
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                } else {
                                    Text(
                                        text = stock.formatPrice(),
                                        style = if (isCurrent) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Normal,
                                        color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Gray,
                                        modifier = Modifier.clickable(enabled = isCurrent) {
                                            editingProductId = item.product.id
                                            editingValue = TextFieldValue(stock.toString())
                                        }
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                }
                
                if (isLoading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }

    if (showAdjustmentDialog) {
        InventoryAdjustmentDialog(
            viewModel = viewModel,
            onDismiss = { showAdjustmentDialog = false }
        )
    }

    val barcodeToCreate by viewModel.showQuickCreate.collectAsState()
    if (barcodeToCreate != null) {
        QuickCreateInventoryDialog(
            barcode = barcodeToCreate!!,
            onSave = { name, price, img -> viewModel.quickCreateAndAdd(barcodeToCreate!!, name, price, img) },
            onCancel = { viewModel.cancelQuickCreate() }
        )
    }
}

@Composable
fun InventoryAdjustmentDialog(
    viewModel: InventoryViewModel,
    onDismiss: () -> Unit
) {
    val capturedItems by viewModel.capturedItems.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.95f),
        confirmButton = {
            Button(
                onClick = { 
                    viewModel.finalizeAdjustment()
                    onDismiss()
                },
                enabled = capturedItems.isNotEmpty()
            ) {
                Text("FINALIZAR AJUSTE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCELAR") }
        },
        title = { Text("Módulo de Ajuste Físico") },
        text = {
            Column(modifier = Modifier.fillMaxSize()) {
                // Buscador de captura
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    label = { Text("Escanea o escribe código para capturar") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && (event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                                viewModel.onSearchSubmit()
                                true
                            } else false
                        },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF2196F3),
                        focusedBorderColor = Color(0xFF2196F3),
                        focusedLabelColor = Color(0xFF2196F3)
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { viewModel.onSearchSubmit() })
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Productos Capturados (Los últimos aparecen primero)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(capturedItems) { item ->
                        var qtyText by remember(item.product.id) { mutableStateOf(TextFieldValue(item.count.toString())) }
                        
                        // Sincronizar instantáneamente si el conteo cambia desde afuera (re-escaneo)
                        LaunchedEffect(item.count) {
                            val newText = item.count.toString()
                            if (qtyText.text != newText) {
                                qtyText = qtyText.copy(text = newText)
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.product.name, fontWeight = FontWeight.Bold)
                                    Text(item.product.barcode, style = MaterialTheme.typography.labelSmall)
                                }
                                
                                OutlinedTextField(
                                    value = qtyText,
                                    onValueChange = { 
                                        if (it.text.isEmpty() || it.text.all { c -> c.isDigit() || c == '.' }) {
                                            qtyText = it
                                            it.text.toDoubleOrNull()?.let { q ->
                                                viewModel.updateCapturedQuantity(item.product.id, q)
                                            }
                                        }
                                    },
                                    modifier = Modifier.width(100.dp).onFocusChanged {
                                        if (it.isFocused) qtyText = qtyText.copy(selection = TextRange(0, qtyText.text.length))
                                    },
                                    label = { Text("Contado") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color(0xFF2196F3),
                                        focusedBorderColor = Color(0xFF2196F3)
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                
                                IconButton(onClick = { viewModel.removeCapturedItem(item.product.id) }) {
                                    Icon(Icons.Default.Delete, null, tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
        }
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
fun QuickCreateInventoryDialog(
    barcode: String,
    onSave: (String, Double, String?) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    val nameFocus = remember { FocusRequester() }
    val priceFocus = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Producto Nuevo Detectado") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("El código $barcode no existe. Regístralo rápidamente:", style = MaterialTheme.typography.bodySmall)
                
                OutlinedTextField(
                    value = barcode,
                    onValueChange = {},
                    label = { Text("Código") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2196F3))
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Producto") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(nameFocus)
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && (event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                                priceFocus.requestFocus()
                                true
                            } else false
                        },
                    singleLine = true
                )

                OutlinedTextField(
                    value = price,
                    onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() || c == '.' }) price = it },
                    label = { Text("Precio Público (P2)") },
                    prefix = { Text("$ ") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(priceFocus),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("URL de Imagen (Opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, price.toDoubleOrNull() ?: 0.0, imageUrl.ifBlank { null }) },
                enabled = name.isNotBlank() && (price.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("GUARDAR Y AÑADIR")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("CANCELAR") }
        }
    )

    LaunchedEffect(Unit) {
        nameFocus.requestFocus()
    }
}
