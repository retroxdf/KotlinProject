package com.abtsplazita.posplazita.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import com.abtsplazita.posplazita.domain.Product
import com.abtsplazita.posplazita.domain.UnitType
import com.abtsplazita.posplazita.domain.Inventory
import com.abtsplazita.posplazita.domain.StockMovement
import com.abtsplazita.posplazita.domain.MovementType
import com.abtsplazita.posplazita.domain.formatPrice
import com.abtsplazita.posplazita.domain.toDirectImageUrl
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.Instant

import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductModule(viewModel: ProductViewModel) {
    val detailProduct by viewModel.detailProduct.collectAsState()
    val editingProduct by viewModel.editingProduct.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isFetching by viewModel.isFetching.collectAsState()
    
    val taxes by viewModel.taxes.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var showImportScreen by remember { mutableStateOf(false) }

    if (showImportScreen) {
        ProductImportScreen(
            viewModel = viewModel,
            onBack = { showImportScreen = false }
        )
        return
    }

    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier.fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && (event.key == Key.Plus || event.key == Key.NumPadAdd)) {
                    if (editingProduct == null) {
                        viewModel.startNewProduct()
                        true
                    } else false
                } else false
            }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWide = maxWidth > 900.dp

            if (isWide) {
                // --- VISTA DE ESCRITORIO (DOS PANELES) ---
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Bar Global
                    Surface(
                        color = Color(0xFF0056A0),
                        contentColor = Color.White,
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { /* Menu / Back if needed */ }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("Producto", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            
                            Spacer(Modifier.weight(1f))
                            
                            IconButton(onClick = { /* Refresh */ }) {
                                Icon(Icons.Default.Refresh, null)
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxSize()) {
                        ProductListScreen(
                            viewModel = viewModel,
                            onImportClick = { showImportScreen = true },
                            modifier = Modifier.weight(0.35f),
                            showTopBar = false
                        )
                        VerticalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                        Box(modifier = Modifier.weight(0.65f).fillMaxHeight().background(Color(0xFFF5F5F5))) {
                            if (detailProduct != null) {
                                ProductDetailScreen(
                                    product = detailProduct!!,
                                    viewModel = viewModel,
                                    onEdit = { viewModel.editProduct(detailProduct!!) },
                                    onClose = { viewModel.closeProductDetail() }
                                )
                            } else {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Selecciona un producto para ver los detalles", color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            } else {
                // --- VISTA MÓVIL (PANELES CONMUTADOS) ---
                if (detailProduct != null) {
                    ProductDetailScreen(
                        product = detailProduct!!,
                        viewModel = viewModel,
                        onEdit = { viewModel.editProduct(detailProduct!!) },
                        onClose = { viewModel.closeProductDetail() }
                    )
                } else {
                    val userPermissions by viewModel.userPermissions.collectAsState()
                    val canCreate = (userPermissions[com.abtsplazita.posplazita.domain.Permission.PRODUCT_CREATE] ?: com.abtsplazita.posplazita.domain.PermissionLevel.DISABLED) != com.abtsplazita.posplazita.domain.PermissionLevel.DISABLED

                    Scaffold(
                        floatingActionButton = {
                            if (canCreate) {
                                FloatingActionButton(
                                    onClick = { viewModel.startNewProduct() },
                                    containerColor = Color(0xFF4CAF50),
                                    contentColor = Color.White
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Product")
                                }
                            }
                        }
                    ) { padding ->
                        ProductListScreen(
                            viewModel = viewModel,
                            onImportClick = { showImportScreen = true },
                            modifier = Modifier.padding(padding)
                        )
                    }
                }
            }
        }
    }

    if (editingProduct != null) {
        ProductEditDialog(
            product = editingProduct!!,
            error = errorMessage,
            isFetching = isFetching,
            taxes = taxes,
            categories = categories,
            onUpdate = { viewModel.updateProduct(it) },
            onSave = { viewModel.saveProduct() },
            onCancel = { viewModel.cancelEdit() },
            onClearError = { viewModel.clearError() },
            onFetchInfo = { viewModel.fetchInfoByBarcode(it) },
            onAddTax = { viewModel.addTax(it) },
            onAddCategory = { viewModel.addCategory(it) }
        )
    }

    val showAuthDialog by viewModel.showAuthDialog.collectAsState()
    val authTitle by viewModel.authTitle.collectAsState()
    
    if (showAuthDialog) {
        var pin by remember { mutableStateOf("") }
        val authFocusRequester = remember { FocusRequester() }

        AlertDialog(
            onDismissRequest = { viewModel.closeAuthDialog() },
            title = { Text("Autorización de Administrador") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Esta acción requiere permiso de un Gerente.")
                    Text(authTitle, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it },
                        label = { Text("PIN de Administrador") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.width(180.dp).focusRequester(authFocusRequester).onPreviewKeyEvent {
                            if (it.type == KeyEventType.KeyDown && (it.key == Key.Enter || it.key == Key.NumPadEnter)) {
                                if (pin.length == 4) viewModel.authorizeWithPin(pin)
                                true
                            } else false
                        },
                        textStyle = MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.Center),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.authorizeWithPin(pin) }, enabled = pin.length == 4) { Text("AUTORIZAR") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeAuthDialog() }) { Text("CANCELAR") }
            }
        )
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(100)
            authFocusRequester.requestFocus()
        }
    }

    if (errorMessage != null && editingProduct == null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Aviso") },
            text = { Text(errorMessage!!) },
            confirmButton = {
                Button(onClick = { viewModel.clearError() }) { Text("OK") }
            }
        )
    }


    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
fun ProductImportScreen(
    viewModel: ProductViewModel,
    onBack: () -> Unit
) {
    val importData by viewModel.importData.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    val filePicker = com.abtsplazita.posplazita.rememberFilePicker()
    
    var rawText by remember { mutableStateOf("") }
    var mapping by remember { mutableStateOf(mutableMapOf<String, Int>()) }

                                    val fields = listOf(
                                        com.abtsplazita.posplazita.domain.ImportFields.BARCODE to "Código de Barras",
                                        com.abtsplazita.posplazita.domain.ImportFields.NAME to "Nombre del Producto",
                                        com.abtsplazita.posplazita.domain.ImportFields.COST to "Costo",
                                        com.abtsplazita.posplazita.domain.ImportFields.PRICE to "Precio Público (P3)",
                                        com.abtsplazita.posplazita.domain.ImportFields.CATEGORY to "Categoría",
                                        com.abtsplazita.posplazita.domain.ImportFields.UNIT to "Unidad (PZA/KG)",
                                        com.abtsplazita.posplazita.domain.ImportFields.STOCK to "Stock Inicial",
                                        com.abtsplazita.posplazita.domain.ImportFields.MIN_STOCK to "Stock Mínimo",
                                        com.abtsplazita.posplazita.domain.ImportFields.MAX_STOCK to "Stock Máximo",
                                        com.abtsplazita.posplazita.domain.ImportFields.TAX_16 to "¿Aplica IVA 16%? (s/n)",
                                        com.abtsplazita.posplazita.domain.ImportFields.TAX_8 to "¿Aplica IVA 8%? (s/n)",
                                        com.abtsplazita.posplazita.domain.ImportFields.SAT_CODE to "Código SAT",
                                        com.abtsplazita.posplazita.domain.ImportFields.IS_BULK to "¿Es a Granel?",
                                        com.abtsplazita.posplazita.domain.ImportFields.USE_SCALE to "¿Usa Báscula?"
                                    )

    Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                viewModel.cancelImport()
                onBack()
            }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Text("Importar Productos", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        if (errorMessage != null) {
            Text(errorMessage!!, color = Color.Red, modifier = Modifier.padding(bottom = 8.dp))
        }

        if (importData.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Carga tus productos", style = MaterialTheme.typography.titleLarge)
                    Text("Solo archivos .csv o contenido pegado desde Excel.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Button(
                        onClick = { 
                            filePicker.pickFile { bytes ->
                                if (bytes != null) {
                                    try {
                                        viewModel.prepareImport(bytes.decodeToString())
                                    } catch (e: Exception) {
                                        // Posiblemente archivo binario (.xlsx)
                                        viewModel.prepareImport("") // Reset
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.FileUpload, null)
                        Spacer(Modifier.width(8.dp))
                        Text("SELECCIONAR ARCHIVO (.CSV)")
                    }

                    Spacer(Modifier.height(24.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(24.dp))

                    Text("O pega directamente desde Excel:", style = MaterialTheme.typography.labelMedium)
                    Text("Selecciona tus celdas en Excel, cópialas (Ctrl+C) y pégalas aquí.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rawText,
                        onValueChange = { rawText = it },
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        placeholder = { Text("Pega aquí tus columnas...") },
                        label = { Text("Contenido de Excel / CSV") }
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Button(
                        onClick = { viewModel.prepareImport(rawText) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = rawText.isNotBlank()
                    ) {
                        Text("ANALIZAR DATOS PEGADOS")
                    }
                }
            }
        } else {
            Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
                Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                    Text("Relacionar Columnas", style = MaterialTheme.typography.titleLarge)
                    Text("Indica qué contiene cada columna de tu tabla.", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(16.dp))
                    
                    val firstRow = importData.first()
                    
                    fields.forEach { (key, label) ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            
                            var expanded by remember { mutableStateOf(false) }
                            val currentMapping = mapping[key]
                            
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                                    val cellText = currentMapping?.let { firstRow.getOrNull(it) } ?: "Omitir"
                                    Text(if (currentMapping != null) "Col. ${currentMapping + 1} ($cellText)" else "Omitir", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    DropdownMenuItem(text = { Text("Omitir") }, onClick = { 
                                        mapping = mapping.toMutableMap().apply { remove(key) }
                                        expanded = false 
                                    })
                                    firstRow.forEachIndexed { index, cell ->
                                        DropdownMenuItem(
                                            text = { Text("Columna ${index + 1}: $cell") }, 
                                            onClick = { 
                                                mapping = mapping.toMutableMap().apply { put(key, index) }
                                                expanded = false 
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    
                    Button(
                        onClick = { 
                            viewModel.executeMappedImport(mapping)
                            onBack()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isImporting && mapping.containsKey(com.abtsplazita.posplazita.domain.ImportFields.NAME) && 
                                  mapping.containsKey(com.abtsplazita.posplazita.domain.ImportFields.BARCODE)
                    ) {
                        if (isImporting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        else Text("IMPORTAR ${importData.size} REGISTROS")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    viewModel: ProductViewModel, 
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier,
    showTopBar: Boolean = true
) {
    val filteredProducts by viewModel.filteredProducts.collectAsState()
    val allInventory by viewModel.allInventory.collectAsState()
    val defaultPriceLevel by viewModel.defaultPriceLevel.collectAsState()
    val searchQuery by viewModel.catalogSearchQuery.collectAsState()
    val detailProduct by viewModel.detailProduct.collectAsState()

    Column(modifier = modifier.fillMaxSize().background(Color.White)) {
        if (showTopBar) {
            // Top bar para móvil
            TopAppBar(
                title = { Text("Productos") },
                actions = {
                    IconButton(onClick = { /* Refresh */ }) { Icon(Icons.Default.Refresh, null) }
                }
            )
        }
        
        // --- BARRA DE BÚSQUEDA Y FILTRO ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateCatalogSearchQuery(it) },
                placeholder = { Text("Buscar por nombre o código...") },
                modifier = Modifier.weight(1f),
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.LightGray,
                    focusedBorderColor = Color(0xFF0056A0)
                ),
                singleLine = true
            )
            
            val userPermissions by viewModel.userPermissions.collectAsState()
            val canCreate = (userPermissions[com.abtsplazita.posplazita.domain.Permission.PRODUCT_CREATE] ?: com.abtsplazita.posplazita.domain.PermissionLevel.DISABLED) != com.abtsplazita.posplazita.domain.PermissionLevel.DISABLED

            if (canCreate) {
                Spacer(Modifier.width(12.dp))
                
                IconButton(
                    onClick = { viewModel.startNewProduct() },
                    modifier = Modifier.background(Color(0xFF4CAF50), CircleShape)
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.White)
                }

                Spacer(Modifier.width(8.dp))

                IconButton(
                    onClick = onImportClick,
                    modifier = Modifier.background(Color(0xFF2196F3), CircleShape)
                ) {
                    Icon(Icons.Default.FileUpload, null, tint = Color.White)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(filteredProducts) { product ->
                val isSelected = detailProduct?.id == product.id
                val inventoryItem = allInventory.find { it.productId == product.id && it.branchId == viewModel.currentBranchId }
                val stock = inventoryItem?.stock ?: 0.0
                val minStock = inventoryItem?.minStock ?: 0.0
                
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .clickable { viewModel.showProductDetail(product) },
                    color = if (isSelected) Color(0xFFE3F2FD) else Color.White,
                    shape = RoundedCornerShape(8.dp),
                    border = if (isSelected) BorderStroke(1.dp, Color(0xFF2196F3)) else null,
                    shadowElevation = if (isSelected) 0.dp else 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Imagen miniatura
                        Card(
                            modifier = Modifier.size(50.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            if (!product.imagePath.isNullOrBlank()) {
                                KamelImage(
                                    resource = { asyncPainterResource(data = product.imagePath.toDirectImageUrl()) },
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Image, null, tint = Color.LightGray)
                                }
                            }
                        }
                        
                        Spacer(Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(product.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text("Cód: ${product.barcode}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            val priceToDisplay = when(defaultPriceLevel) {
                                1 -> product.price1
                                2 -> product.price2
                                else -> product.price3
                            }
                            Text("$${priceToDisplay.formatPrice()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color(0xFF2E7D32))
                            
                            val isLowStock = !product.isService && stock <= minStock && stock > 0
                            val isOutOfStock = !product.isService && stock <= 0
                            
                            Surface(
                                color = when {
                                    isOutOfStock -> Color(0xFFFFEBEE)
                                    isLowStock -> Color(0xFFFFF3E0) // Naranja muy claro
                                    else -> Color(0xFFE8F5E9)
                                },
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "${stock.formatPrice()} ${if(product.unit == UnitType.KG) "kg" else "pza"}",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        isOutOfStock -> Color(0xFFC62828)
                                        isLowStock -> Color(0xFFEF6C00) // Naranja fuerte
                                        else -> Color(0xFF2E7D32)
                                    }
                                )
                            }
                            if (isLowStock) {
                                Text("Stock Bajo", style = MaterialTheme.typography.labelSmall, color = Color(0xFFEF6C00), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductDetailScreen(
    product: Product,
    viewModel: ProductViewModel,
    onEdit: () -> Unit,
    onClose: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Información", "Inventario", "Historial")

    Column(modifier = Modifier.fillMaxSize()) {
        // Cabecera Detalle
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(product.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        Text("Categoría: ${product.category}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, null) }
                }
                
                Spacer(Modifier.height(16.dp))
                
                val userPermissions by viewModel.userPermissions.collectAsState()
                val canEdit = (userPermissions[com.abtsplazita.posplazita.domain.Permission.PRODUCT_EDIT] ?: com.abtsplazita.posplazita.domain.PermissionLevel.DISABLED) != com.abtsplazita.posplazita.domain.PermissionLevel.DISABLED

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (canEdit) {
                        Button(onClick = onEdit) {
                            Icon(Icons.Default.Edit, null); Spacer(Modifier.width(8.dp)); Text("EDITAR")
                        }
                    }
                    OutlinedButton(onClick = { /* Imprimir Etiqueta */ }) {
                        Icon(Icons.Default.Print, null); Spacer(Modifier.width(8.dp)); Text("ETIQUETA")
                    }

                    val canDelete = (userPermissions[com.abtsplazita.posplazita.domain.Permission.PRODUCT_DELETE] ?: com.abtsplazita.posplazita.domain.PermissionLevel.DISABLED) != com.abtsplazita.posplazita.domain.PermissionLevel.DISABLED
                    
                    if (canDelete) {
                        var showConfirmDelete by remember { mutableStateOf(false) }
                        val allInventory by viewModel.allInventory.collectAsState()
                        val totalStock = allInventory.filter { it.productId == product.id }.sumOf { it.stock }

                        IconButton(
                            onClick = { showConfirmDelete = true },
                            modifier = Modifier.background(MaterialTheme.colorScheme.errorContainer, CircleShape)
                        ) {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                        }

                        if (showConfirmDelete) {
                            AlertDialog(
                                onDismissRequest = { showConfirmDelete = false },
                                title = { Text("Eliminar Producto") },
                                text = { 
                                    if (totalStock > 0) {
                                        Text("No se puede eliminar '${product.name}' porque aún tiene existencias ($totalStock ${if(product.unit == UnitType.KG) "kg" else "pza"}). Primero debes dejar el inventario en 0.")
                                    } else {
                                        Text("¿Estás seguro de que deseas eliminar '${product.name}'? Esta acción no se puede deshacer.")
                                    }
                                },
                                confirmButton = {
                                    if (totalStock <= 0) {
                                        Button(
                                            onClick = { 
                                                viewModel.deleteProduct(product)
                                                showConfirmDelete = false 
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                        ) { Text("ELIMINAR") }
                                    } else {
                                        Button(onClick = { showConfirmDelete = false }) { Text("ENTENDIDO") }
                                    }
                                },
                                dismissButton = {
                                    if (totalStock <= 0) {
                                        TextButton(onClick = { showConfirmDelete = false }) { Text("CANCELAR") }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
        
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }
        
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTab) {
                0 -> ProductInfoTab(product)
                1 -> ProductInventoryTab(product, viewModel)
                2 -> ProductHistoryTab(product, viewModel)
            }
        }
    }
}

@Composable
fun ProductInfoTab(product: Product) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        DetailRow("Código de Barras", product.barcode)
        DetailRow("Tipo de Venta", if(product.isBulk) "A granel (Peso)" else "Por pieza")
        DetailRow("Usa Báscula", if(product.useScale) "Sí" else "No")
        DetailRow("URL Imagen", product.imagePath ?: "Sin imagen")
        
        Spacer(Modifier.height(24.dp))
        Text("ESQUEMA DE PRECIOS", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = Color.Gray)
        Spacer(Modifier.height(12.dp))
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PriceRow("Costo de Compra", "$${product.cost.formatPrice()}", Color.Gray)
                if (product.tax > 0.0) {
                    PriceRow("Costo Con IVA", "$${(product.cost * (1 + product.tax / 100)).formatPrice()}", Color(0xFF4CAF50))
                }
                HorizontalDivider()
                PriceRow("Precio Público (P2)", "$${product.price2.formatPrice()}", Color(0xFF2E7D32))
                PriceRow("Precio Mayoreo (P1)", "$${product.price1.formatPrice()}", Color(0xFF2196F3))
                PriceRow("Precio Adicional (P3)", "$${product.price3.formatPrice()}", Color(0xFF673AB7))
            }
        }
    }
}

@Composable
fun ProductInventoryTab(product: Product, viewModel: ProductViewModel) {
    val inventory by viewModel.allInventory.collectAsState()
    val branches by viewModel.branches.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("EXISTENCIAS POR SUCURSAL", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(16.dp))
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(branches) { branch ->
                val inv = inventory.find { it.productId == product.id && it.branchId == branch.id }
                val branchStock = inv?.stock ?: 0.0
                val min = inv?.minStock ?: 0.0
                val max = inv?.maxStock ?: 0.0
                val isCurrentBranch = branch.id == viewModel.currentBranchId
                
                BranchStockEditor(
                    branchName = branch.name + (if(isCurrentBranch) " (Esta sucursal)" else ""),
                    stock = branchStock,
                    minStock = min,
                    maxStock = max,
                    unit = if(product.unit == UnitType.KG) "kg" else "pza",
                    canEdit = isCurrentBranch,
                    onUpdateStock = { newValue ->
                        viewModel.updateBranchStock(product.id, branch.id, newValue)
                    },
                    onUpdateLimits = { minVal, maxVal ->
                        viewModel.updateStockLimits(product.id, minVal, maxVal)
                    }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun ProductHistoryTab(product: Product, viewModel: ProductViewModel) {
    val movements by viewModel.productMovements.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("ÚLTIMOS MOVIMIENTOS", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(16.dp))
        
        if (movements.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay movimientos registrados", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(movements) { move ->
                    StockMovementItem(move)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun PriceRow(label: String, value: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Black, color = color)
    }
}

@Composable
fun BranchStockEditor(
    branchName: String,
    stock: Double,
    minStock: Double = 0.0,
    maxStock: Double = 0.0,
    unit: String,
    canEdit: Boolean = false,
    onUpdateStock: (Double) -> Unit,
    onUpdateLimits: (Double, Double) -> Unit = { _, _ -> }
) {
    var isEditing by remember { mutableStateOf(false) }
    var editValue by remember { mutableStateOf(stock.toString()) }
    var editMin by remember { mutableStateOf(minStock.toString()) }
    var editMax by remember { mutableStateOf(maxStock.toString()) }
    
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(branchName, style = MaterialTheme.typography.bodyLarge, fontWeight = if(canEdit) FontWeight.Black else FontWeight.Bold)
                if (isEditing) {
                    OutlinedTextField(
                        value = editValue,
                        onValueChange = { if(it.all { c -> c.isDigit() || c == '.' }) editValue = it },
                        modifier = Modifier.width(120.dp),
                        label = { Text("Stock Actual") },
                        suffix = { Text(unit) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                } else {
                    Text("${stock.formatPrice()} $unit", color = if(stock > 0) Color(0xFF2E7D32) else Color.Red, fontWeight = FontWeight.Bold)
                }
            }
            
            if (canEdit) {
                if (isEditing) {
                    IconButton(onClick = { 
                        editValue.toDoubleOrNull()?.let { onUpdateStock(it) }
                        editMin.toDoubleOrNull()?.let { min -> 
                            editMax.toDoubleOrNull()?.let { max -> onUpdateLimits(min, max) }
                        }
                        isEditing = false 
                    }) { Icon(Icons.Default.Check, null, tint = Color(0xFF4CAF50)) }
                    IconButton(onClick = { isEditing = false }) { Icon(Icons.Default.Close, null, tint = Color.Red) }
                } else {
                    IconButton(onClick = { 
                        editValue = stock.toString()
                        editMin = minStock.toString()
                        editMax = maxStock.toString()
                        isEditing = true 
                    }) { Icon(Icons.Default.Edit, null, tint = Color.Gray) }
                }
            }
        }

        if (isEditing || (minStock > 0 || maxStock > 0)) {
            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (isEditing) {
                    OutlinedTextField(
                        value = editMin,
                        onValueChange = { if(it.all { c -> c.isDigit() || c == '.' }) editMin = it },
                        modifier = Modifier.width(100.dp),
                        label = { Text("Mínimo") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = editMax,
                        onValueChange = { if(it.all { c -> c.isDigit() || c == '.' }) editMax = it },
                        modifier = Modifier.width(100.dp),
                        label = { Text("Máximo") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                } else {
                    Text("Mín: ${minStock.formatPrice()}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("Máx: ${maxStock.formatPrice()}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun StockMovementItem(movement: StockMovement) {
    val dt = Instant.fromEpochMilliseconds(movement.timestamp).toLocalDateTime(TimeZone.currentSystemDefault())
    
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icono según tipo
        val (icon, color) = when(movement.type) {
            MovementType.IN_PURCHASE -> Icons.Default.AddShoppingCart to Color(0xFF4CAF50)
            MovementType.OUT_SALE -> Icons.Default.Sell to Color(0xFF2196F3)
            MovementType.ADJUSTMENT -> Icons.Default.Tune to Color(0xFFFFA000)
            MovementType.TRANSFER -> Icons.Default.SwapHoriz to Color(0xFF673AB7)
        }
        
        Surface(color = color.copy(alpha = 0.1f), shape = CircleShape, modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
        }
        
        Spacer(Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(movement.reason ?: movement.type.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text("${dt.date} ${dt.time.toString().take(5)} • Por ${movement.userId}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
        
        Text(
            text = (if(movement.type == MovementType.IN_PURCHASE) "+" else "") + movement.quantity.formatPrice(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Black,
            color = if(movement.quantity > 0) Color(0xFF2E7D32) else Color.Red
        )
    }
}

@Composable
fun ProductEditDialog(
    product: Product,
    error: String?,
    isFetching: Boolean,
    taxes: List<Double>,
    categories: List<String>,
    onUpdate: (Product) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onClearError: () -> Unit,
    onFetchInfo: (String) -> Unit,
    onAddTax: (Double) -> Unit,
    onAddCategory: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val barcodeFocusRequester = remember { FocusRequester() }
    val nameFocusRequester = remember { FocusRequester() }
    val categoryFocusRequester = remember { FocusRequester() }
    val costFocusRequester = remember { FocusRequester() }
    val ivaFocusRequester = remember { FocusRequester() }
    val saveButtonFocusRequester = remember { FocusRequester() }
    val p3FocusRequester = remember { FocusRequester() }
    val p1FocusRequester = remember { FocusRequester() }
    val p2FocusRequester = remember { FocusRequester() }
    
    AlertDialog(
        onDismissRequest = onCancel,
        modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.9f),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if(product.id.isEmpty()) Icons.Default.AddBox else Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(if(product.id.isEmpty()) "Nuevo Producto" else "Editar Producto") 
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
                if (error != null) {
                    Surface(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, null, tint = Color.Red)
                            Spacer(Modifier.width(12.dp))
                            Text(error, color = Color.Red, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            IconButton(onClick = onClearError) { Icon(Icons.Default.Close, null, tint = Color.Red) }
                        }
                    }
                }

                // 1. CODIGO Y BUSQUEDA NUBE
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = product.barcode,
                        onValueChange = { onUpdate(product.copy(barcode = it)) },
                        label = { Text("1. Código de Barras") },
                        modifier = Modifier.weight(1f).focusRequester(barcodeFocusRequester).onPreviewKeyEvent {
                            if ((it.key == Key.Enter || it.key == Key.NumPadEnter) && it.type == KeyEventType.KeyDown) {
                                nameFocusRequester.requestFocus()
                                true
                            } else false
                        },
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onFetchInfo(product.barcode) },
                        modifier = Modifier.height(56.dp),
                        enabled = product.barcode.length >= 8 && !isFetching
                    ) {
                        if(isFetching) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        else Icon(Icons.Default.CloudDownload, null)
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                
                // 2. NOMBRE
                OutlinedTextField(
                    value = product.name,
                    onValueChange = { onUpdate(product.copy(name = it)) },
                    label = { Text("2. Nombre del Producto") },
                    modifier = Modifier.fillMaxWidth().focusRequester(nameFocusRequester).onPreviewKeyEvent {
                        if ((it.key == Key.Enter || it.key == Key.NumPadEnter) && it.type == KeyEventType.KeyDown) {
                            categoryFocusRequester.requestFocus()
                            true
                        } else false
                    },
                    singleLine = true
                )

                Spacer(Modifier.height(16.dp))

                // 3. CATEGORIA E IVA
                var showNewCategoryDialog by remember { mutableStateOf(false) }
                var catExpanded by remember { mutableStateOf(false) }
                var ivaExpanded by remember { mutableStateOf(false) }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Categoría", style = MaterialTheme.typography.labelSmall)
                        Box {
                            OutlinedButton(
                                onClick = { catExpanded = true }, 
                                modifier = Modifier.fillMaxWidth().focusRequester(categoryFocusRequester).onPreviewKeyEvent {
                                    if ((it.key == Key.Enter || it.key == Key.NumPadEnter) && it.type == KeyEventType.KeyDown) {
                                        ivaFocusRequester.requestFocus()
                                        true
                                    } else false
                                }
                            ) {
                                Text(product.category.ifBlank { "Seleccionar..." })
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                            DropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(text = { Text(cat) }, onClick = { onUpdate(product.copy(category = cat)); catExpanded = false })
                                }
                                HorizontalDivider()
                                DropdownMenuItem(text = { Text("+ Nueva Categoría") }, onClick = { showNewCategoryDialog = true; catExpanded = false })
                            }
                        }
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text("IVA (%)", style = MaterialTheme.typography.labelSmall)
                        Box {
                            OutlinedButton(
                                onClick = { ivaExpanded = true }, 
                                modifier = Modifier.fillMaxWidth().focusRequester(ivaFocusRequester).onPreviewKeyEvent {
                                    if ((it.key == Key.Enter || it.key == Key.NumPadEnter) && it.type == KeyEventType.KeyDown) {
                                        costFocusRequester.requestFocus()
                                        true
                                    } else false
                                }
                            ) {
                                Text(if (product.tax > 0) "IVA ${product.tax.toInt()}%" else "Sin Impuestos")
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                            DropdownMenu(expanded = ivaExpanded, onDismissRequest = { ivaExpanded = false }) {
                                taxes.forEach { tax ->
                                    DropdownMenuItem(
                                        text = { Text(if (tax == 0.0) "Sin Impuestos" else "IVA ${tax.toInt()}%") }, 
                                        onClick = { onUpdate(product.copy(tax = tax)); ivaExpanded = false }
                                    )
                                }
                            }
                        }
                    }
                }

                if (showNewCategoryDialog) {
                    var newCatName by remember { mutableStateOf("") }
                    val newCatFocus = remember { FocusRequester() }
                    
                    AlertDialog(
                        onDismissRequest = { showNewCategoryDialog = false },
                        title = { Text("Nueva Categoría") },
                        text = {
                            OutlinedTextField(
                                value = newCatName,
                                onValueChange = { newCatName = it },
                                label = { Text("Nombre de la categoría") },
                                modifier = Modifier.fillMaxWidth().focusRequester(newCatFocus).onPreviewKeyEvent {
                                    if (it.key == Key.Enter || it.key == Key.NumPadEnter) {
                                        if (it.type == KeyEventType.KeyDown && newCatName.isNotBlank()) {
                                            onAddCategory(newCatName)
                                            onUpdate(product.copy(category = newCatName.trim()))
                                            showNewCategoryDialog = false
                                        }
                                        true
                                    } else false
                                },
                                singleLine = true
                            )
                        },
                        confirmButton = {
                            Button(onClick = {
                                if (newCatName.isNotBlank()) {
                                    onAddCategory(newCatName)
                                    onUpdate(product.copy(category = newCatName.trim()))
                                }
                                showNewCategoryDialog = false
                            }) { Text("AÑADIR") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showNewCategoryDialog = false }) { Text("CANCELAR") }
                        }
                    )
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(100)
                        newCatFocus.requestFocus()
                    }
                }

                Spacer(Modifier.height(16.dp))
                
                // 4. COSTOS Y PRECIOS
                Text("Costos y Precios", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Costo Sin IVA", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1976D2))
                        OutlinedTextField(
                            value = product.cost.toString(),
                            onValueChange = { it.toDoubleOrNull()?.let { v -> onUpdate(product.copy(cost = v)) } },
                            modifier = Modifier.fillMaxWidth().focusRequester(costFocusRequester).onPreviewKeyEvent {
                                if ((it.key == Key.Enter || it.key == Key.NumPadEnter) && it.type == KeyEventType.KeyDown) {
                                    saveButtonFocusRequester.requestFocus()
                                    true
                                } else false
                            },
                            prefix = { Text("$") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                    }

                    if (product.tax > 0.0) {
                        val costWithTax = product.cost * (1 + product.tax / 100)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Costo Con IVA", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
                            OutlinedTextField(
                                value = costWithTax.formatPrice(),
                                onValueChange = { input ->
                                    input.toDoubleOrNull()?.let { v -> 
                                        val base = v / (1 + product.tax / 100)
                                        onUpdate(product.copy(cost = base))
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().background(Color(0xFFE8F5E9), RoundedCornerShape(4.dp)),
                                textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32)),
                                prefix = { Text("$") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFFC8E6C9),
                                    unfocusedContainerColor = Color(0xFFC8E6C9),
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = product.price1.toString(),
                        onValueChange = { it.toDoubleOrNull()?.let { v -> onUpdate(product.copy(price1 = v)) } },
                        label = { Text("P1 (Mayoreo)") },
                        modifier = Modifier.weight(1f).focusRequester(p1FocusRequester).onPreviewKeyEvent {
                            if ((it.key == Key.Enter || it.key == Key.NumPadEnter) && it.type == KeyEventType.KeyDown) {
                                p2FocusRequester.requestFocus()
                                true
                            } else false
                        },
                        prefix = { Text("$") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = product.price2.toString(),
                        onValueChange = { it.toDoubleOrNull()?.let { v -> onUpdate(product.copy(price2 = v)) } },
                        label = { Text("P2 (Público - DEFAULT)") },
                        modifier = Modifier.weight(1f).focusRequester(p2FocusRequester).onPreviewKeyEvent {
                            if ((it.key == Key.Enter || it.key == Key.NumPadEnter) && it.type == KeyEventType.KeyDown) {
                                p3FocusRequester.requestFocus()
                                true
                            } else false
                        },
                        prefix = { Text("$") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
                
                Spacer(Modifier.height(12.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = product.price3.toString(),
                        onValueChange = { it.toDoubleOrNull()?.let { v -> onUpdate(product.copy(price3 = v)) } },
                        label = { Text("P3 (Adicional +0.50)") },
                        modifier = Modifier.weight(1f).focusRequester(p3FocusRequester).onPreviewKeyEvent {
                            if ((it.key == Key.Enter || it.key == Key.NumPadEnter) && it.type == KeyEventType.KeyDown) {
                                saveButtonFocusRequester.requestFocus()
                                true
                            } else false
                        },
                        prefix = { Text("$") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    Box(modifier = Modifier.weight(1f)) 
                }

                Spacer(Modifier.height(16.dp))
                
                // 5. OPCIONES DE VENTA
                Text("Opciones de Venta", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = product.isBulk, onCheckedChange = { onUpdate(product.copy(isBulk = it)) })
                    Text("Venta a granel / Peso")
                }
                if (product.isBulk) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 24.dp)) {
                        Checkbox(checked = product.useScale, onCheckedChange = { onUpdate(product.copy(useScale = it)) })
                        Text("Usar báscula automática")
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = product.isService, onCheckedChange = { onUpdate(product.copy(isService = it)) })
                    Text("Es un Servicio / Recarga (No resta stock)")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = product.showInWebShop, onCheckedChange = { onUpdate(product.copy(showInWebShop = it)) })
                    Text("Mostrar en WebShop")
                }

                Spacer(Modifier.height(16.dp))
                
                // EXTRA: URL DE IMAGEN Y SAT (Menos prominente ahora)
                var showExtraInfo by remember { mutableStateOf(false) }
                TextButton(onClick = { showExtraInfo = !showExtraInfo }) {
                    Text(if(showExtraInfo) "Ocultar Datos Extra (Imagen/SAT)" else "Ver Datos Extra (Imagen/SAT)")
                }
                if (showExtraInfo) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = product.imagePath ?: "",
                            onValueChange = { onUpdate(product.copy(imagePath = it.ifBlank { null })) },
                            label = { Text("URL de Imagen") },
                            modifier = Modifier.weight(1f),
                            leadingIcon = { Icon(Icons.Default.Link, null) },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = product.satCode ?: "",
                            onValueChange = { onUpdate(product.copy(satCode = it.ifBlank { null })) },
                            label = { Text("Clave SAT") },
                            modifier = Modifier.weight(0.5f),
                            singleLine = true
                        )
                    }
                }
                
                // 6. CODIGOS ADICIONALES
                var showExtraBarcodes by remember { mutableStateOf(false) }
                TextButton(onClick = { showExtraBarcodes = !showExtraBarcodes }) {
                    Text(if(showExtraBarcodes) "Ocultar Códigos Extra" else "Ver Códigos de Barras Extra")
                }
                if (showExtraBarcodes) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = product.barcode2 ?: "", onValueChange = { onUpdate(product.copy(barcode2 = it.ifBlank { null })) }, label = { Text("Código Extra 2") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = product.barcode3 ?: "", onValueChange = { onUpdate(product.copy(barcode3 = it.ifBlank { null })) }, label = { Text("Código Extra 3") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = product.barcode4 ?: "", onValueChange = { onUpdate(product.copy(barcode4 = it.ifBlank { null })) }, label = { Text("Código Extra 4") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave, 
                enabled = product.name.isNotBlank(),
                modifier = Modifier.focusRequester(saveButtonFocusRequester).onPreviewKeyEvent {
                    if ((it.key == Key.Enter || it.key == Key.NumPadEnter) && it.type == KeyEventType.KeyDown && product.name.isNotBlank()) {
                        onSave()
                        true
                    } else false
                }
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("GUARDAR PRODUCTO")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("CANCELAR") }
        }
    )

    LaunchedEffect(Unit) {
        repeat(3) {
            kotlinx.coroutines.delay(200)
            try { barcodeFocusRequester.requestFocus() } catch(e: Exception) {}
        }
    }
}
