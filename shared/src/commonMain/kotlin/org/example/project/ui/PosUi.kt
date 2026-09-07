package com.abtsplazita.posplazita.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.abtsplazita.posplazita.domain.*
import com.abtsplazita.posplazita.ui.users.UserViewModel
import com.abtsplazita.posplazita.ui.customers.CustomerEditDialog
import com.abtsplazita.posplazita.formatTimestamp
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosMainScreen(
    viewModel: PosViewModel,
    userViewModel: UserViewModel,
    adImageUrl: String,
    currentUserId: String,
    onNavigateToCheckout: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onLogout: () -> Unit
) {
    val items by viewModel.currentItems.collectAsState()
    val total by viewModel.total.collectAsState()
    val itemCount by viewModel.itemCount.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val searchStocks by viewModel.searchStocks.collectAsState()
    val selectedSearchIndex by viewModel.selectedSearchIndex.collectAsState()
    val selectedCartIndex by viewModel.selectedCartIndex.collectAsState()
    val currentFocusArea by viewModel.currentFocusArea.collectAsState()
    val showSearchResults by viewModel.showSearchResults.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val warningMessage by viewModel.warningMessage.collectAsState()
    val isProcessingSale by viewModel.isProcessingSale.collectAsState()
    
    val terminals by viewModel.availableTerminals.collectAsState()
    val selectedTerminal by viewModel.selectedTerminal.collectAsState()

    val showQuantityDialog by viewModel.showQuantityDialog.collectAsState()
    val showBulkQuantityDialog by viewModel.showBulkQuantityDialog.collectAsState()
    val editingItem by viewModel.editingItem.collectAsState()
    val bulkEditingProduct by viewModel.bulkEditingProduct.collectAsState()
    val editingCustomer by viewModel.editingCustomer.collectAsState()
    val showCustomerDialog by viewModel.showCustomerDialog.collectAsState()
    val showAddCustomerDialog by viewModel.showAddCustomerDialog.collectAsState()
    val showDebtPaymentDialog by viewModel.showDebtPaymentDialog.collectAsState()
    val selectedPriceLevel by viewModel.selectedPriceLevel.collectAsState()
    val showHeldSalesDialog by viewModel.showHeldSalesDialog.collectAsState()
    val showReturnDialog by viewModel.showReturnDialog.collectAsState()
    val showWithdrawalDialog by viewModel.showWithdrawalDialog.collectAsState()
    val showPreCutDialog by viewModel.showPreCutDialog.collectAsState()
    val showCommonDialog by viewModel.showCommonProductDialog.collectAsState()
    val showMultiserviceDialog by viewModel.showMultiserviceDialog.collectAsState()
    val showCommentDialog by viewModel.showCommentDialog.collectAsState()
    val showNotFound by viewModel.showNotFoundDialog.collectAsState()
    val showSaleSuccess by viewModel.showSaleSuccessOverlay.collectAsState()
    val preCutResult by viewModel.preCutResult.collectAsState()
    val showCashMovementDialog by viewModel.showCashMovementDialog.collectAsState()
    
    val sidebarItems by viewModel.sidebarItems.collectAsState()
    val sidebarIndex by viewModel.sidebarIndex.collectAsState()

    val focusRequester = remember { FocusRequester() }
    val bulkFocusRequester = remember { FocusRequester() }
    val qtyFocusRequester = remember { FocusRequester() }

    var preCutAmount by remember { mutableStateOf(TextFieldValue("")) }

    LaunchedEffect(Unit) {
        delay(300)
        focusRequester.requestFocus()
    }

    LaunchedEffect(currentFocusArea) {
        if (currentFocusArea == PosViewModel.FocusArea.SEARCH_BAR) {
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.focusSearchRequest.collect {
            focusRequester.requestFocus()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isCompact = maxWidth < 700.dp
        val isExtended = maxWidth > 1200.dp

        Row(modifier = Modifier.fillMaxSize()) {
            // --- ÁREA IZQUIERDA (CONTENIDO PRINCIPAL) ---
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                // Buscador
                Row(modifier = Modifier.fillMaxWidth().padding(if (isCompact) 8.dp else 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (terminals.isNotEmpty()) {
                        var showTerminalMenu by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.padding(end = 8.dp)) {
                            InputChip(
                                selected = selectedTerminal != null,
                                onClick = { showTerminalMenu = true },
                                label = { Text(selectedTerminal?.name ?: "CAJA", fontWeight = FontWeight.Bold, maxLines = 1) },
                                leadingIcon = { Icon(Icons.Default.Store, null, modifier = Modifier.size(18.dp), tint = if (selectedTerminal == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) },
                                colors = InputChipDefaults.inputChipColors(containerColor = if (selectedTerminal == null) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant),
                                border = if (selectedTerminal == null) BorderStroke(2.dp, MaterialTheme.colorScheme.error) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            )
                            DropdownMenu(expanded = showTerminalMenu, onDismissRequest = { showTerminalMenu = false }) {
                                DropdownMenuItem(text = { Text("SIN CAJA SELECCIONADA") }, onClick = { viewModel.selectTerminal(null); showTerminalMenu = false })
                                terminals.forEach { terminal ->
                                    DropdownMenuItem(text = { Text(terminal.name) }, onClick = { viewModel.selectTerminal(terminal); showTerminalMenu = false }, leadingIcon = { if (terminal.id == selectedTerminal?.id) Icon(Icons.Default.Check, null) })
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        modifier = Modifier.weight(1f).focusRequester(focusRequester).onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown) {
                                if (event.isAltPressed && event.key == Key.V) { onNavigateToCheckout(); true }
                                else if (event.isAltPressed && event.key == Key.W) { viewModel.putSaleOnHold(); true }
                                else if (event.isAltPressed && event.key == Key.G) { viewModel.openHeldSalesDialog(); true }
                                else if (event.isAltPressed && event.key == Key.R) { viewModel.openWithdrawalDialog(); true }
                                else if (event.isAltPressed && event.key == Key.C) { viewModel.openCustomerDialog(); true }
                                else if (event.isAltPressed && event.key == Key.D) { viewModel.openReturnDialog(); true }
                                else if (event.isAltPressed && event.key == Key.P) { viewModel.openCommentDialog(); true }
                                else if (event.isAltPressed && event.key == Key.N) { viewModel.openCashDrawer(); true }
                                else if (event.isAltPressed && event.key == Key.I) { viewModel.reprintLastSale(); true }
                                else if (event.isAltPressed && event.key == Key.A) { viewModel.openDebtPaymentDialog(); true }
                                else if (event.isAltPressed && event.key == Key.E) { viewModel.openMultiserviceDialog(); true }
                                else if (event.isAltPressed && event.key == Key.F) { viewModel.openCommonProductDialog(); true }
                                else if (event.isAltPressed && event.key == Key.K) { viewModel.openPreCutDialog(); true }
                                else {
                                    when (event.key) {
                                        Key.Plus, Key.NumPadAdd -> {
                                            val text = searchQuery.text
                                            if (text.isNotEmpty() && text.all { it.isDigit() || it == '.' }) { viewModel.openCommonWithShortcut(text); true } else false
                                        }
                                        Key.DirectionDown -> { viewModel.moveFocus(1); true }
                                        Key.DirectionUp -> { viewModel.moveFocus(-1); true }
                                        Key.DirectionRight -> { viewModel.incrementSelectedCartItem(); true }
                                        Key.DirectionLeft -> { viewModel.decrementSelectedCartItem(); true }
                                        Key.Enter, Key.NumPadEnter -> { 
                                            if (showSaleSuccess) {
                                                viewModel.clearChange()
                                            } else if (showSearchResults) {
                                                viewModel.selectCurrentItem()
                                            } else {
                                                viewModel.onSearchSubmit()
                                            }
                                            true 
                                        }
                                        Key.F1 -> {
                                            if (showSaleSuccess) {
                                                viewModel.clearChange()
                                                true
                                            } else false
                                        }
                                        Key.Delete, Key.Backspace -> {
                                            if (showSearchResults) {
                                                viewModel.onSearchQueryClear()
                                                true
                                            } else if (currentFocusArea == PosViewModel.FocusArea.CART) {
                                                // Escenario 1: Producto seleccionado (azul). Borrar solo ese.
                                                if (selectedCartIndex in items.indices) {
                                                    viewModel.removeSaleItem(items[selectedCartIndex])
                                                    true
                                                } else false
                                            } else if (currentFocusArea == PosViewModel.FocusArea.SEARCH_BAR && searchQuery.text.isEmpty()) {
                                                // Escenario 2: En buscador vacío. Borrar TODA la venta (solo con Delete/Supr).
                                                if (event.key == Key.Delete) {
                                                    viewModel.clearSale()
                                                    true
                                                } else false
                                            } else false
                                        }
                                        Key.Escape -> { 
                                            if (showSaleSuccess) {
                                                viewModel.clearChange()
                                            } else if (showSearchResults) {
                                                viewModel.onSearchQueryClear()
                                            } else if (items.isNotEmpty()) {
                                                onNavigateToCheckout()
                                            } else {
                                                viewModel.onSearchQueryClear()
                                                viewModel.selectSearchQuery()
                                            }
                                            true 
                                        }
                                        else -> false
                                    }
                                }
                            } else false
                        },
                        placeholder = { Text("Escanea código o busca por nombre...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search, keyboardType = KeyboardType.Text, autoCorrect = false),
                        keyboardActions = KeyboardActions(onSearch = { viewModel.onSearchSubmit() }, onDone = { viewModel.onSearchSubmit() })
                    )
                }

                // Carrito
                val contentModifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                if (isCompact) {
                    Column(modifier = contentModifier) {
                        Text("Carrito (${items.size} ítems)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.outline)
                        if (items.isEmpty()) Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Sin productos", color = Color.Gray) }
                        else LazyColumn(modifier = Modifier.fillMaxSize()) {
                            itemsIndexed(items) { index, item ->
                                val isSelected = index == selectedCartIndex && currentFocusArea == PosViewModel.FocusArea.CART
                                Row(
                                    modifier = Modifier.fillMaxWidth().background(if (isSelected) Color(0xFF2196F3) else Color.Transparent).clickable { viewModel.setSelectedCartIndex(index); viewModel.setFocusArea(PosViewModel.FocusArea.CART) }.padding(vertical = 8.dp, horizontal = 4.dp), 
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Card(
                                        modifier = Modifier.size(45.dp), 
                                        shape = RoundedCornerShape(4.dp), 
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                                    ) {
                                        if (!item.productImagePath.isNullOrBlank()) {
                                            KamelImage(resource = { asyncPainterResource(data = item.productImagePath.toDirectImageUrl()) }, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                                        } else {
                                            Box(Modifier.fillMaxSize(), Alignment.Center) {
                                                Icon(Icons.Default.Image, null, modifier = Modifier.size(20.dp), tint = Color.LightGray)
                                            }
                                        }
                                    }
                                    Spacer(Modifier.width(28.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(item.productName, style = MaterialTheme.typography.bodyLarge, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) Color.White else Color.Unspecified)
                                        Surface(color = Color.Transparent, onClick = { viewModel.openQuantityDialog(item) }) {
                                            Text("${item.quantity.formatWeight()} x $${item.priceAtSale.formatPrice()}", style = MaterialTheme.typography.bodySmall, color = if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Text("$${item.subtotal.formatPrice()}", fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else Color.Unspecified)
                                    IconButton(onClick = { viewModel.removeSaleItem(item) }) { Icon(Icons.Default.Delete, null, Modifier.size(20.dp), tint = if (isSelected) Color.White else MaterialTheme.colorScheme.error) }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                } else {
                    Column(modifier = contentModifier) {
                        Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Cant.", modifier = Modifier.width(70.dp), fontWeight = FontWeight.Bold)
                            Text("Producto", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                            Text("Precio", modifier = Modifier.width(80.dp), textAlign = TextAlign.End, fontWeight = FontWeight.Bold)
                            Text("Subtotal", modifier = Modifier.width(90.dp), textAlign = TextAlign.End, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(40.dp))
                        }
                        if (items.isEmpty()) Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Esperando productos...", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.outline) }
                        else LazyColumn(modifier = Modifier.fillMaxSize()) {
                            itemsIndexed(items) { index, item ->
                                val isSelected = index == selectedCartIndex && currentFocusArea == PosViewModel.FocusArea.CART
                                Row(
                                    modifier = Modifier.fillMaxWidth().background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent).clickable { viewModel.setSelectedCartIndex(index); viewModel.setFocusArea(PosViewModel.FocusArea.CART) }.padding(vertical = 4.dp, horizontal = 4.dp), 
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(modifier = Modifier.width(70.dp), color = Color.Transparent, onClick = { viewModel.setSelectedCartIndex(index); viewModel.openQuantityDialog(item) }) {
                                        Text(item.quantity.formatWeight(), textAlign = TextAlign.Center, color = if (isSelected) Color.White else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, style = MaterialTheme.typography.bodyLarge)
                                    }
                                    
                                    Card(
                                        modifier = Modifier.size(40.dp), 
                                        shape = RoundedCornerShape(4.dp), 
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                                    ) {
                                        if (!item.productImagePath.isNullOrBlank()) {
                                            KamelImage(resource = { asyncPainterResource(data = item.productImagePath.toDirectImageUrl()) }, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                                        } else {
                                            Box(Modifier.fillMaxSize(), Alignment.Center) {
                                                Icon(Icons.Default.Image, null, modifier = Modifier.size(20.dp), tint = Color.LightGray)
                                            }
                                        }
                                    }
                                    
                                    Spacer(Modifier.width(28.dp))

                                    Text(item.productName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = if (isSelected) Color.White else Color.Unspecified, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, maxLines = 2)
                                    Text("$${item.priceAtSale.formatPrice()}", modifier = Modifier.width(80.dp), textAlign = TextAlign.End, color = if (isSelected) Color.White else Color.Unspecified)
                                    Text("$${item.subtotal.formatPrice()}", modifier = Modifier.width(90.dp), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else Color.Unspecified)
                                    IconButton(onClick = { viewModel.removeSaleItem(item) }) { Icon(Icons.Default.Delete, null, Modifier.size(20.dp), tint = if (isSelected) Color.White else MaterialTheme.colorScheme.error) }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }

                // Botón de Cobro
                Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primaryContainer, tonalElevation = 8.dp) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { if (selectedTerminal == null) viewModel.setErrorMessage("Debes seleccionar una caja.") else onNavigateToCheckout() }, 
                            modifier = Modifier.height(64.dp).fillMaxWidth(), 
                            enabled = items.isNotEmpty() && !isProcessingSale, 
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text("TOTAL A COBRAR", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                                    Text("$${total.formatPrice()} (${itemCount}pz)", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Color.White)
                                }
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Default.FlashOn, null, modifier = Modifier.size(32.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(if (isCompact) "COBRAR" else "COBRAR (ESC)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // --- SIDEBAR DE OFERTAS (SOLO SI HAY PROMOS ACTIVAS) ---
            if (isExtended && sidebarItems.isNotEmpty()) {
                Box(modifier = Modifier.width(400.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)).padding(12.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("¡OFERTAS!", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = Color(0xFFE91E63))
                        Spacer(Modifier.height(16.dp))
                        val currentItem = sidebarItems.getOrNull(sidebarIndex)
                        if (currentItem is Promotion) {
                            PromotionVisualCard(currentItem, viewModel)
                        } else if (currentItem is String) {
                            Card(modifier = Modifier.fillMaxSize(), elevation = CardDefaults.cardElevation(defaultElevation = 6.dp), shape = MaterialTheme.shapes.large) {
                                KamelImage(resource = { asyncPainterResource(data = currentItem.toDirectImageUrl()) }, contentDescription = "Publicidad", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, onLoading = { Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() } })
                            }
                        }
                    }
                }
            }
        }

        // --- OVERLAY DE VENTA COMPLETADA ---
        val saleChangeOverlay by viewModel.saleChange.collectAsState()
        val showCardSuccess by viewModel.showCardSuccess.collectAsState()
        
        if (showSaleSuccess && (saleChangeOverlay != null || showCardSuccess)) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Card(
                    modifier = Modifier.size(300.dp), 
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(4.dp, Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("VENTA LISTA", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.DarkGray)
                        if (showCardSuccess) {
                            Text("PAGO OK", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF4CAF50), fontWeight = FontWeight.Black)
                        } else {
                            Text("CAMBIO:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text("$${saleChangeOverlay?.formatPrice()}", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = Color(0xFF4CAF50))
                        }
                    }
                }
            }
        }

        if (errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Card(
                    modifier = Modifier.size(200.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.Error, null, tint = Color.White, modifier = Modifier.size(50.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(errorMessage!!, color = Color.White, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        if (warningMessage != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Card(
                    modifier = Modifier.size(200.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(50.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(warningMessage!!, color = Color.White, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        // --- VENTANA DE BÚSQUEDA (DIALOG) ---
        if (showSearchResults && searchResults.isNotEmpty()) {
            val dialogFocusRequester = remember { FocusRequester() }
            val listState = rememberLazyListState()
            
            LaunchedEffect(showSearchResults) {
                delay(100)
                dialogFocusRequester.requestFocus()
            }

            LaunchedEffect(selectedSearchIndex) {
                if (selectedSearchIndex >= 0) {
                    listState.animateScrollToItem(selectedSearchIndex)
                }
            }

            Dialog(
                onDismissRequest = { viewModel.onSearchQueryClear() },
                properties = androidx.compose.ui.window.DialogProperties(
                    usePlatformDefaultWidth = false,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true
                )
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .fillMaxHeight(0.85f),
                    elevation = CardDefaults.cardElevation(24.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column {
                        Surface(
                            color = Color(0xFF0056A0),
                            contentColor = Color.White,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.padding(8.dp).fillMaxWidth()) {
                                IconButton(
                                    onClick = { viewModel.onSearchQueryClear() },
                                    modifier = Modifier.align(Alignment.CenterStart).size(32.dp)
                                ) {
                                    Icon(Icons.Default.Close, null, tint = Color.White)
                                }
                                Text(
                                    "Buscador",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                        
                        Box(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.onSearchQueryChange(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(dialogFocusRequester)
                                    .onPreviewKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyDown) {
                                            when (event.key) {
                                                Key.DirectionDown -> { viewModel.moveFocus(1); true }
                                                Key.DirectionUp -> { viewModel.moveFocus(-1); true }
                                                Key.Enter, Key.NumPadEnter -> { viewModel.selectCurrentItem(); true }
                                                Key.Escape -> { viewModel.onSearchQueryClear(); true }
                                                else -> false
                                            }
                                        } else false
                                    },
                                trailingIcon = { Icon(Icons.Default.Tune, null, tint = Color(0xFF0056A0)) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF0056A0),
                                    cursorColor = Color(0xFF0056A0)
                                )
                            )
                        }
                        
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                            val isSmall = maxWidth < 600.dp
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Producto", style = MaterialTheme.typography.labelLarge, color = Color.Gray, modifier = Modifier.weight(1f))
                                Text("Existencia", style = MaterialTheme.typography.labelLarge, color = Color.Gray, modifier = Modifier.width(if (isSmall) 70.dp else 120.dp), textAlign = TextAlign.End)
                                Text("Precio", style = MaterialTheme.typography.labelLarge, color = Color.Gray, modifier = Modifier.width(if (isSmall) 80.dp else 120.dp), textAlign = TextAlign.End)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            state = listState
                        ) {
                            itemsIndexed(searchResults) { index, product ->
                                val isSelected = index == selectedSearchIndex
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.onResultClick(product) },
                                    color = if (isSelected) Color(0xFF2196F3) else Color.Transparent
                                ) {
                                    BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                                        val isSmall = maxWidth < 600.dp
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Card(
                                                modifier = Modifier.size(if (isSmall) 50.dp else 80.dp),
                                                shape = RoundedCornerShape(4.dp),
                                                colors = CardDefaults.cardColors(containerColor = Color.White)
                                            ) {
                                                if (!product.imagePath.isNullOrBlank()) {
                                                    KamelImage(
                                                        resource = { asyncPainterResource(data = product.imagePath!!.toDirectImageUrl()) },
                                                        contentDescription = null,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Fit
                                                    )
                                                } else {
                                                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                                                        Icon(Icons.Default.Image, null, tint = Color.LightGray)
                                                    }
                                                }
                                            }
                                            
                                            Spacer(Modifier.width(if (isSmall) 8.dp else 16.dp))
                                            
                                            Column(Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "PZA", 
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Black,
                                                        color = if (isSelected) Color.White else Color(0xFF0056A0)
                                                    )
                                                    Spacer(Modifier.width(8.dp))
                                                    Text(
                                                        text = product.barcode,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (isSelected) Color.White.copy(0.8f) else Color.Gray,
                                                        maxLines = 1
                                                    )
                                                }
                                                Text(
                                                    product.name, 
                                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium, 
                                                    style = if (isSmall) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                                                    color = if (isSelected) Color.White else Color.Black,
                                                    maxLines = 2
                                                )
                                            }
                                            
                                            Text(
                                                text = "${searchStocks[product.barcode]?.toInt() ?: 0}",
                                                style = if (isSmall) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.width(if (isSmall) 70.dp else 120.dp),
                                                textAlign = TextAlign.End,
                                                color = if (isSelected) Color.White else Color.Black
                                            )
                                            
                                            Text(
                                                text = "$${when(selectedPriceLevel) {
                                                    1 -> product.price1
                                                    2 -> product.price2
                                                    3 -> product.price3
                                                    4 -> product.price4
                                                    else -> product.price2
                                                }.formatPrice()}",
                                                style = if (isSmall) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.width(if (isSmall) 80.dp else 120.dp),
                                                textAlign = TextAlign.End,
                                                color = if (isSelected) Color.White else Color.Black
                                            )
                                        }
                                    }
                                }
                                if (!isSelected) HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIÁLOGOS ---
    if (showQuantityDialog && editingItem != null) {
        var qtyText by remember { mutableStateOf(TextFieldValue(editingItem!!.quantity.toString())) }
        AlertDialog(
            onDismissRequest = { viewModel.closeQuantityDialog() },
            title = { Text("Ajustar Cantidad") },
            text = {
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { 
                        if (it.text.isEmpty() || it.text.all { c -> c.isDigit() || c == '.' }) {
                            qtyText = it
                            viewModel.onEditingQuantityChange(it)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().focusRequester(qtyFocusRequester),
                    label = { Text("Cantidad") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            },
            confirmButton = { Button(onClick = { viewModel.confirmQuantityUpdate() }) { Text("GUARDAR") } },
            dismissButton = { TextButton(onClick = { viewModel.closeQuantityDialog() }) { Text("CANCELAR") } }
        )
        LaunchedEffect(Unit) { delay(100); qtyFocusRequester.requestFocus() }
    }

    if (showBulkQuantityDialog && bulkEditingProduct != null) {
        var qtyText by remember { mutableStateOf(TextFieldValue("")) }
        Dialog(onDismissRequest = { viewModel.closeBulkQuantityDialog() }) {
            Card(
                modifier = Modifier.width(400.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Venta a Granel",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            bulkEditingProduct!!.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Ingresa el peso o cantidad", color = Color.Gray)
                        
                        Spacer(Modifier.height(24.dp))
                        
                        OutlinedTextField(
                            value = qtyText,
                            onValueChange = { 
                                if (it.text.isEmpty() || it.text.all { c -> c.isDigit() || c == '.' }) {
                                    qtyText = it
                                    viewModel.onBulkQuantityChange(it)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().focusRequester(bulkFocusRequester).onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown && (event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                                    viewModel.confirmBulkQuantity()
                                    true
                                } else false
                            },
                            label = { Text("Peso / Cantidad") },
                            textStyle = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black, textAlign = TextAlign.Center),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )

                        Spacer(Modifier.height(32.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TextButton(onClick = { viewModel.closeBulkQuantityDialog() }, modifier = Modifier.weight(1f)) {
                                Text("CANCELAR")
                            }
                            Button(
                                onClick = { viewModel.confirmBulkQuantity() },
                                modifier = Modifier.weight(1.5f).height(48.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("AGREGAR", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        LaunchedEffect(Unit) { delay(100); bulkFocusRequester.requestFocus() }
    }

    if (showCustomerDialog) CustomerSelectionDialog(viewModel, onDismiss = { viewModel.closeCustomerDialog() })
    if (showAddCustomerDialog && editingCustomer != null) {
        CustomerEditDialog(
            customer = editingCustomer!!,
            onUpdate = { viewModel.updateEditingCustomer(it) },
            onSave = { viewModel.saveNewCustomer() },
            onCancel = { viewModel.closeAddCustomerDialog() }
        )
    }
    if (showDebtPaymentDialog) DebtPaymentDialog(viewModel, onDismiss = { viewModel.closeDebtPaymentDialog() })
    if (showHeldSalesDialog) HeldSalesDialog(viewModel, onDismiss = { viewModel.closeHeldSalesDialog() })
    if (showReturnDialog) ReturnDialog(viewModel, onDismiss = { viewModel.closeReturnDialog() })
    if (showWithdrawalDialog) WithdrawalDialog(viewModel)
    
    if (showCommonDialog) CommonProductDialog(viewModel, onDismiss = { viewModel.closeCommonProductDialog() })
    if (showMultiserviceDialog) MultiserviceDialog(viewModel, onDismiss = { viewModel.closeMultiserviceDialog() })
    if (showCommentDialog) {
        var comment by remember { mutableStateOf(viewModel.saleComment.value) }
        AlertDialog(
            onDismissRequest = { viewModel.closeCommentDialog() },
            title = { Text("Comentario de la Venta") },
            text = { OutlinedTextField(value = comment, onValueChange = { comment = it }, label = { Text("Escribe una nota...") }, modifier = Modifier.fillMaxWidth(), minLines = 3) },
            confirmButton = { Button(onClick = { viewModel.setSaleComment(comment); viewModel.closeCommentDialog() }) { Text("GUARDAR") } },
            dismissButton = { TextButton(onClick = { viewModel.closeCommentDialog() }) { Text("CANCELAR") } }
        )
    }
    if (showNotFound) {
        AlertDialog(
            onDismissRequest = { viewModel.closeNotFoundDialog() },
            title = { Text("Producto No Encontrado", color = MaterialTheme.colorScheme.error) },
            text = { Text("El código '${viewModel.notFoundQuery.value}' no existe en el catálogo.") },
            confirmButton = { Button(onClick = { viewModel.closeNotFoundDialog() }) { Text("ENTENDIDO") } }
        )
    }

    if (showCashMovementDialog != null) {
        CashMovementDialog(viewModel, type = showCashMovementDialog!!)
    }

    if (preCutResult != null) {
        val (message, isError) = preCutResult!!
        AlertDialog(
            onDismissRequest = { viewModel.clearPreCutResult() },
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isError) Icons.Default.Warning else Icons.Default.CheckCircle, 
                        contentDescription = null,
                        tint = if (isError) Color.Red else Color(0xFF4CAF50)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(if (isError) "Resultado del Precorte" else "Corte Exitoso")
                }
            },
            text = {
                Text(message, style = MaterialTheme.typography.bodyLarge)
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearPreCutResult() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isError) Color.Red else Color(0xFF4CAF50)
                    )
                ) {
                    Text("ENTENDIDO", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// Pantallas y Diálogos de soporte

@Composable
fun CustomerSelectionDialog(viewModel: PosViewModel, onDismiss: () -> Unit, onSelectOverride: (() -> Unit)? = null) {
    val customers by viewModel.filteredCustomers.collectAsState()
    val searchQuery by viewModel.customerSearchQuery.collectAsState()
    val selectedIndex by viewModel.selectedCustomerIndex.collectAsState()
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
    }

    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0 && customers.isNotEmpty()) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Seleccionar Cliente", fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                Button(onClick = { viewModel.openAddCustomerDialog() }) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("NUEVO")
                }
            }
        },
        text = {
            Column(modifier = Modifier.width(500.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateCustomerSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester).onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            when (event.key) {
                                Key.DirectionDown -> { viewModel.moveCustomerFocus(1); true }
                                Key.DirectionUp -> { viewModel.moveCustomerFocus(-1); true }
                                Key.Enter, Key.NumPadEnter -> { 
                                    viewModel.selectFocusedCustomer()
                                    if (onSelectOverride != null) onSelectOverride() else onDismiss()
                                    true 
                                }
                                else -> false
                            }
                        } else false
                    },
                    placeholder = { Text("Buscar por nombre o teléfono...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true
                )
                
                Spacer(Modifier.height(16.dp))
                
                if (customers.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(200.dp), Alignment.Center) {
                        Text("No se encontraron clientes", color = Color.Gray)
                    }
                } else {
                    LazyColumn(modifier = Modifier.height(300.dp), state = listState) {
                        itemsIndexed(customers) { index, customer ->
                            val isSelected = index == selectedIndex
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable { 
                                    viewModel.selectCustomer(customer)
                                    if (onSelectOverride != null) onSelectOverride() else onDismiss()
                                },
                                color = if (isSelected) Color(0xFF2196F3) else Color.Transparent,
                                shape = MaterialTheme.shapes.medium
                            ) {
                                ListItem(
                                    headlineContent = { Text(customer.name, fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal, color = if(isSelected) Color.White else Color.Unspecified) },
                                    supportingContent = { Text(customer.phone ?: "Sin teléfono", color = if(isSelected) Color.White.copy(0.8f) else Color.Unspecified) },
                                    trailingContent = {
                                        if (customer.currentDebt > 0) {
                                            Text("DEUDA: $${customer.currentDebt.formatPrice()}", color = if(isSelected) Color.White else Color.Red, fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } }
    )
}

@Composable
fun WithdrawalDialog(viewModel: PosViewModel) {
    var amountText by remember { mutableStateOf("") }
    var totalChargeText by remember { mutableStateOf("") }
    var isCommissionInCash by remember { mutableStateOf(false) }
    val isProcessing by viewModel.isProcessingWithdrawal.collectAsState()

    // Sincronización inteligente de campos
    val amount = amountText.toDoubleOrNull() ?: 0.0
    val commission = amount * 0.05
    val totalToCharge = if (isCommissionInCash) amount else (amount + commission)

    AlertDialog(
        onDismissRequest = { if (!isProcessing) viewModel.closeWithdrawalDialog() },
        title = { Text("Retiro de Efectivo (Venta con Tarjeta)", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Este módulo permite entregar efectivo al cliente cobrándole con su tarjeta. Se aplica una comisión fija del 5%.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.all { c -> c.isDigit() || c == '.' }) {
                            amountText = input
                            val v = input.toDoubleOrNull() ?: 0.0
                            if (v > 0) {
                                val total = if (isCommissionInCash) v else (v * 1.05)
                                totalChargeText = total.formatPrice().replace(",", "")
                            } else {
                                totalChargeText = ""
                            }
                        }
                    },
                    label = { Text("Monto a Entregar (Efectivo)") },
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                OutlinedTextField(
                    value = totalChargeText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.all { c -> c.isDigit() || c == '.' }) {
                            totalChargeText = input
                            val v = input.toDoubleOrNull() ?: 0.0
                            if (v > 0) {
                                val delivered = if (isCommissionInCash) v else (v / 1.05)
                                amountText = delivered.formatPrice().replace(",", "")
                            } else {
                                amountText = ""
                            }
                        }
                    },
                    label = { Text("Total a Cobrar en Tarjeta") },
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )

                Surface(
                    color = Color.Gray.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Comisión (5%)", style = MaterialTheme.typography.labelSmall)
                            Text("$${commission.formatPrice()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { 
                            isCommissionInCash = !isCommissionInCash 
                            val v = amountText.toDoubleOrNull() ?: 0.0
                            if (v > 0) {
                                totalChargeText = (if (isCommissionInCash) v else (v * 1.05)).formatPrice().replace(",", "")
                            }
                        }) {
                            Checkbox(checked = isCommissionInCash, onCheckedChange = { 
                                isCommissionInCash = it 
                                val v = amountText.toDoubleOrNull() ?: 0.0
                                if (v > 0) {
                                    totalChargeText = (if (it) v else (v * 1.05)).formatPrice().replace(",", "")
                                }
                            })
                            Text("Paga en Efectivo", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                
                if (amount > 0) {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (isCommissionInCash) "COBRAR EN TERMINAL (Efectivo neto)" else "TOTAL FINAL A COBRAR EN TERMINAL", style = MaterialTheme.typography.labelSmall)
                            Text("$${totalToCharge.formatPrice()}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                            Text("ENTREGAR AL CLIENTE: $${amount.formatPrice()}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (amount > 0) {
                        viewModel.processWithdrawal(amount, commission, isCommissionInCash)
                    }
                },
                enabled = !isProcessing && amount > 0
            ) {
                if (isProcessing) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                else Text("INICIAR COBRO EN POINT")
            }
        },
        dismissButton = {
            TextButton(onClick = { if(!isProcessing) viewModel.closeWithdrawalDialog() }, enabled = !isProcessing) {
                Text("CANCELAR")
            }
        }
    )
}

@Composable
fun DebtPaymentDialog(viewModel: PosViewModel, onDismiss: () -> Unit) {
    val selectedCustomer by viewModel.selectedCustomer.collectAsState()
    
    if (selectedCustomer == null) {
        // Al seleccionar cliente NO llamamos a onDismiss, para que la ventana de abono se muestre inmediatamente
        CustomerSelectionDialog(viewModel, onDismiss = { /* Si el usuario cancela la búsqueda, cerramos todo */ onDismiss() }, onSelectOverride = { /* No cerrar */ })
    } else {
        var amountText by remember { mutableStateOf("") }
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            delay(100)
            focusRequester.requestFocus()
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Payments, null, tint = Color(0xFF4CAF50))
                    Spacer(Modifier.width(12.dp))
                    Text("Registrar Abono", fontWeight = FontWeight.Black)
                }
            },
            text = {
                Column(modifier = Modifier.width(400.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("CLIENTE", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(selectedCustomer!!.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            Text("DEUDA ACTUAL", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text("$${selectedCustomer!!.currentDebt.formatPrice()}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = Color(0xFFC62828))
                        }
                    }

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() || c == '.' }) amountText = it },
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester).onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && (event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                                val amount = amountText.toDoubleOrNull() ?: 0.0
                                if (amount > 0) {
                                    viewModel.processDebtPayment(selectedCustomer!!, amount)
                                }
                                true
                            } else false
                        },
                        label = { Text("Monto a Abonar") },
                        placeholder = { Text("0.00") },
                        prefix = { Text("$", fontWeight = FontWeight.Bold) },
                        textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4CAF50),
                            focusedLabelColor = Color(0xFF4CAF50)
                        )
                    )
                    
                    Surface(
                        color = Color.Gray.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Al confirmar, se registrará el abono y se imprimirá un comprobante con el saldo restante.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        val amount = amountText.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            viewModel.processDebtPayment(selectedCustomer!!, amount)
                        }
                    },
                    enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("ABONAR E IMPRIMIR", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.selectCustomer(null) }) { 
                    Text("CAMBIAR CLIENTE", color = MaterialTheme.colorScheme.primary) 
                }
            }
        )
    }
}

@Composable
fun HeldSalesDialog(viewModel: PosViewModel, onDismiss: () -> Unit) {
    val heldSales by viewModel.heldSales.collectAsState()
    val selectedIndex by viewModel.selectedHeldSaleIndex.collectAsState()
    val pendingDeletions by viewModel.pendingDeletionTicketIds.collectAsState()
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val now = remember { com.abtsplazita.posplazita.currentTimeMillis() }

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
    }

    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0 && heldSales.isNotEmpty()) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.7f)
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        when (event.key) {
                            Key.DirectionDown -> { viewModel.moveHeldSaleFocus(1); true }
                            Key.DirectionUp -> { viewModel.moveHeldSaleFocus(-1); true }
                            Key.Enter, Key.NumPadEnter -> { viewModel.selectFocusedHeldSale(); onDismiss(); true }
                            Key.Escape -> { onDismiss(); true }
                            else -> false
                        }
                    } else false
                },
            elevation = CardDefaults.cardElevation(16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column {
                Surface(
                    color = Color(0xFF0056A0),
                    contentColor = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.align(Alignment.CenterStart).size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, null, tint = Color.White)
                        }
                        Text(
                            "Ventas en espera",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (heldSales.isEmpty()) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Text("No hay ventas en espera", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        itemsIndexed(heldSales) { index, sale ->
                            val isSelected = index == selectedIndex
                            val isPendingDelete = pendingDeletions.contains(sale.id)
                            
                            val diffSecs = (now - sale.timestamp) / 1000
                            val timeText = when {
                                diffSecs < 60 -> "Hace $diffSecs segundos"
                                diffSecs < 3600 -> "Hace ${diffSecs / 60} minutos"
                                else -> "Hace ${diffSecs / 3600} horas"
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { if (!isPendingDelete) { viewModel.resumeHeldSale(sale); onDismiss() } },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(0xFFFFA500) else Color.LightGray.copy(alpha = 0.5f)
                                ),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = if (sale.customerId != null) "Cliente: ${sale.customerId}" else "Público en General",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                        Text(
                                            text = timeText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                    }

                                    Text(
                                        text = "$${sale.total.formatPrice()} MXN",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0056A0)
                                    )

                                    Spacer(Modifier.width(24.dp))

                                    IconButton(
                                        onClick = { viewModel.deleteHeldSale(sale) },
                                        enabled = !isPendingDelete,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.RemoveCircleOutline,
                                            contentDescription = "Eliminar",
                                            tint = if (isPendingDelete) Color.LightGray else Color.Red
                                        )
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

@Composable
fun ReturnDialog(viewModel: PosViewModel, onDismiss: () -> Unit) {
    var ticketSearchQuery by remember { mutableStateOf("") }
    var productSearchQuery by remember { mutableStateOf("") }
    
    val foundSales by viewModel.searchSales(ticketSearchQuery).collectAsState(emptyList())
    var selectedSale by remember { mutableStateOf<Sale?>(null) }
    
    // Nueva lógica para búsqueda de productos manual
    val foundProducts by viewModel.searchProductsForReturn(productSearchQuery).collectAsState(emptyList())
    
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Devolución / Cambio de Producto", fontWeight = FontWeight.Black) },
        text = {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isSmall = maxWidth < 500.dp
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // SECCIÓN 1: BUSCAR TICKET
                    Column {
                        Text("1. BUSCAR TICKET POR ID (Ej: 001)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        OutlinedTextField(
                            value = ticketSearchQuery,
                            onValueChange = { 
                                ticketSearchQuery = it 
                                selectedSale = null // Resetear al escribir
                            },
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                            placeholder = { Text("Escribe parte del folio...") },
                            leadingIcon = { Icon(Icons.Default.Receipt, null) },
                            singleLine = true
                        )
                    }

                    // LISTA DE TICKETS ENCONTRADOS
                    if (ticketSearchQuery.length >= 2 && selectedSale == null) {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                            LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
                                items(foundSales) { sale ->
                                    ListItem(
                                        headlineContent = { Text("Ticket: ${sale.id}", fontWeight = FontWeight.Bold, style = if(isSmall) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyLarge) },
                                        supportingContent = { Text(formatTimestamp(sale.timestamp), style = MaterialTheme.typography.labelSmall) },
                                        trailingContent = {
                                            TextButton(onClick = { 
                                                scope.launch {
                                                    val fullSale = viewModel.getSaleById(sale.id)
                                                    selectedSale = fullSale
                                                }
                                            }) { Text("VER", style = MaterialTheme.typography.labelSmall) }
                                        },
                                        modifier = Modifier.clickable { 
                                            scope.launch {
                                                selectedSale = viewModel.getSaleById(sale.id)
                                            }
                                        }
                                    )
                                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
                                }
                            }
                        }
                    }

                    // PRODUCTOS DEL TICKET SELECCIONADO
                    if (selectedSale != null) {
                        Surface(color = Color(0xFFE3F2FD), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Productos del ticket:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                                    items(selectedSale!!.items) { item ->
                                        ListItem(
                                            headlineContent = { Text(item.productName, style = if(isSmall) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyLarge) },
                                            supportingContent = { Text("${item.quantity} x $${item.priceAtSale.formatPrice()}", style = MaterialTheme.typography.labelSmall) },
                                            trailingContent = {
                                                Button(
                                                    onClick = {
                                                        scope.launch {
                                                            val product = viewModel.getProductInfo(item.productId)
                                                            if (product != null) {
                                                                viewModel.addProduct(product, 9999.0, item.quantity, isReturn = true)
                                                                onDismiss()
                                                            }
                                                        }
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(32.dp)
                                                ) {
                                                    Text("DEV", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    // SECCIÓN 2: BUSCAR PRODUCTO MANUAL
                    Column {
                        Text("2. O BUSCAR PRODUCTO DIRECTAMENTE", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        OutlinedTextField(
                            value = productSearchQuery,
                            onValueChange = { productSearchQuery = it },
                            modifier = Modifier.fillMaxWidth().onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown && (event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                                    scope.launch {
                                        val product = viewModel.findProductForReturn(productSearchQuery)
                                        if (product != null) {
                                            viewModel.addProduct(product, 9999.0, 1.0, isReturn = true)
                                            onDismiss()
                                        } else {
                                            // Si no hay match exacto, ya se están mostrando resultados en la lista de abajo
                                        }
                                    }
                                    true
                                } else false
                            },
                            placeholder = { Text("Código o nombre...") },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            singleLine = true
                        )
                    }

                    // LISTA DE PRODUCTOS ENCONTRADOS (BÚSQUEDA MANUAL)
                    if (productSearchQuery.length >= 2) {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                            LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                                items(foundProducts) { product ->
                                    ListItem(
                                        headlineContent = { Text(product.name, fontWeight = FontWeight.Bold, style = if(isSmall) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyLarge) },
                                        supportingContent = { Text(product.barcode, style = MaterialTheme.typography.labelSmall) },
                                        trailingContent = {
                                            Button(
                                                onClick = {
                                                    viewModel.addProduct(product, 9999.0, 1.0, isReturn = true)
                                                    onDismiss()
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("DEV", style = MaterialTheme.typography.labelSmall)
                                            }
                                        },
                                        modifier = Modifier.clickable { 
                                            viewModel.addProduct(product, 9999.0, 1.0, isReturn = true)
                                            onDismiss()
                                        }
                                    )
                                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
                                }
                            }
                        }
                    }
                    
                    Text(
                        "Al seleccionar un producto, se agregará al carrito con saldo a favor para el cliente.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        },
        confirmButton = { },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCELAR") }
        }
    )
}

@Composable
fun PromotionVisualCard(promo: Promotion, viewModel: PosViewModel) {
    var product by remember { mutableStateOf<Product?>(null) }
    
    LaunchedEffect(promo.productId) {
        if (!promo.productId.isNullOrBlank()) {
            product = viewModel.getProductInfo(promo.productId)
        }
    }

    Card(
        modifier = Modifier.fillMaxSize(),
        border = BorderStroke(3.dp, Color(0xFFE91E63)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = Color(0xFFE91E63),
                modifier = Modifier.fillMaxWidth(),
                contentColor = Color.White
            ) {
                Text(
                    text = "PROMOCIÓN DEL DÍA",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(Modifier.height(24.dp))
            
            if (product != null) {
                if (!product!!.imagePath.isNullOrBlank()) {
                    Card(
                        modifier = Modifier.size(200.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        KamelImage(
                            resource = { asyncPainterResource(data = product!!.imagePath!!.toDirectImageUrl()) },
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    Box(Modifier.size(200.dp).background(Color.Gray.copy(0.1f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Image, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                
                Text(
                    text = product!!.name,
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 32.sp),
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    maxLines = 2
                )
                
                Spacer(Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$${product!!.price3.formatPrice()}",
                        style = MaterialTheme.typography.titleLarge.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough),
                        color = Color.Gray
                    )
                    Spacer(Modifier.width(16.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color(0xFFE91E63))
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = "$${promo.discountValue.formatPrice()}",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp),
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Black
                    )
                }
            } else {
                Text(
                    text = promo.name,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
                
                if (promo.discountValue > 0) {
                    Text(
                        text = "PRECIO: $${promo.discountValue.formatPrice()}",
                        style = MaterialTheme.typography.displayLarge,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Black
                    )
                }
            }
            
            Spacer(Modifier.weight(1f))
            
            Text(
                "¡APROVECHA AHORA!",
                modifier = Modifier.padding(bottom = 24.dp),
                style = MaterialTheme.typography.labelLarge,
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun StatPanelRow(label: String, value: String, icon: ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = color.copy(alpha = 0.1f)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CommonProductDialog(viewModel: PosViewModel, onDismiss: () -> Unit) {
    val name by viewModel.commonProductName.collectAsState()
    val price by viewModel.commonProductPrice.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Producto Común / Varios", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Ingresa los datos del producto manual:")
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { viewModel.onCommonProductNameChange(it) },
                    label = { Text("Nombre del Producto") },
                    modifier = Modifier.fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && (event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                                viewModel.addCommonProduct()
                                true
                            } else false
                        },
                    singleLine = true
                )

                OutlinedTextField(
                    value = price,
                    onValueChange = { 
                        if (it.text.isEmpty() || it.text.all { c -> c.isDigit() || c == '.' }) {
                            viewModel.onCommonProductPriceChange(it)
                        }
                    },
                    label = { Text("Precio de Venta") },
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text("$") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.addCommonProduct() },
                enabled = (price.text.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("AGREGAR AL CARRITO")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR")
            }
        }
    )
}

@Composable
fun MultiserviceDialog(viewModel: PosViewModel, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Multiservicios") }, text = { Text("Opciones...") }, confirmButton = { Button(onClick = onDismiss) { Text("CERRAR") } })
}

@Composable
fun PreCutDialog(viewModel: PosViewModel, currentUserId: String) {
    var preCutAmount by remember { mutableStateOf(TextFieldValue("")) }
    
    Dialog(onDismissRequest = { viewModel.closePreCutDialog() }) {
        Card(
            modifier = Modifier.width(450.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column {
                Surface(
                    color = Color(0xFF0056A0),
                    contentColor = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Analytics, null)
                        Spacer(Modifier.width(12.dp))
                        Text("Precorte de Caja", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }

                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Verificación de efectivo en turno", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Spacer(Modifier.height(24.dp))
                    
                    OutlinedTextField(
                        value = preCutAmount,
                        onValueChange = { if (it.text.isEmpty() || it.text.all { c -> c.isDigit() || c == '.' }) preCutAmount = it },
                        label = { Text("Efectivo Contado en Caja") },
                        modifier = Modifier.fillMaxWidth(),
                        prefix = { Text("$", fontWeight = FontWeight.Bold) },
                        textStyle = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Nota: Este proceso cerrará tu turno actual y guardará el registro para auditoría.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )

                    Spacer(Modifier.height(32.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(onClick = { viewModel.closePreCutDialog() }, modifier = Modifier.weight(1f)) {
                            Text("CANCELAR")
                        }
                        Button(
                            onClick = { preCutAmount.text.toDoubleOrNull()?.let { viewModel.savePreCut(it, currentUserId) } },
                            modifier = Modifier.weight(1.5f).height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0056A0))
                        ) {
                            Text("REALIZAR PRECORTE", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CashMovementDialog(viewModel: PosViewModel, type: CashMovementType) {
    var amountText by remember { mutableStateOf("") }
    var reasonText by remember { mutableStateOf("") }
    var showInsufficientFundsError by remember { mutableStateOf(false) }
    
    val cashInDrawer by viewModel.cashInDrawer.collectAsState()
    val amountFocusRequester = remember { FocusRequester() }
    val reasonFocusRequester = remember { FocusRequester() }
    
    val isIn = type == CashMovementType.IN
    val amount = amountText.toDoubleOrNull() ?: 0.0

    fun attemptSubmit() {
        if (amount <= 0 || reasonText.isBlank()) return
        
        val isInsufficient = !isIn && amount > (cashInDrawer + 0.01)
        if (isInsufficient) {
            showInsufficientFundsError = true
            com.abtsplazita.posplazita.playErrorSound()
        } else {
            viewModel.addCashMovement(amount, reasonText)
        }
    }

    AlertDialog(
        onDismissRequest = { viewModel.closeCashMovementDialog() },
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (isIn) Icons.Default.AddCircle else Icons.Default.RemoveCircle, null, tint = if (isIn) Color(0xFF2E7D32) else Color.Red)
                Spacer(Modifier.width(12.dp))
                Text(if (isIn) "Entrada de Dinero" else "Salida de Dinero", fontWeight = FontWeight.Black)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Registra un movimiento manual de efectivo en el cajón.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                
                Column {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { 
                            if (it.isEmpty() || it.all { c -> c.isDigit() || c == '.' }) {
                                amountText = it
                                showInsufficientFundsError = false // Reset error while typing
                            }
                        },
                        label = { Text("Cantidad ($)") },
                        modifier = Modifier.fillMaxWidth().focusRequester(amountFocusRequester)
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown && (event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                                    reasonFocusRequester.requestFocus()
                                    true
                                } else false
                            },
                        isError = showInsufficientFundsError,
                        textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    
                    if (showInsufficientFundsError) {
                        Text(
                            text = "Fondo insuficiente",
                            color = Color.Red,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 4.dp, start = 8.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = reasonText,
                    onValueChange = { 
                        reasonText = it 
                        showInsufficientFundsError = false // Reset error while typing
                    },
                    label = { Text("Concepto / Motivo") },
                    modifier = Modifier.fillMaxWidth().focusRequester(reasonFocusRequester)
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && (event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                                attemptSubmit()
                                true
                            } else false
                        },
                    placeholder = { Text("Ej: Pago a proveedor, fondo inicial...") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { attemptSubmit() },
                enabled = amountText.isNotEmpty() && reasonText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = if (isIn) Color(0xFF2E7D32) else Color.Red)
            ) {
                Text("REGISTRAR ${if(isIn) "ENTRADA" else "SALIDA"}")
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.closeCashMovementDialog() }) {
                Text("CANCELAR")
            }
        }
    )

    LaunchedEffect(Unit) {
        delay(100)
        amountFocusRequester.requestFocus()
    }
}
