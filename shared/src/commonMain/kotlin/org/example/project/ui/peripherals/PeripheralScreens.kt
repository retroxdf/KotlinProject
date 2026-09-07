package com.abtsplazita.posplazita.ui.peripherals

import com.abtsplazita.posplazita.ui.PosViewModel
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.abtsplazita.posplazita.domain.repository.ProductRepository
import com.abtsplazita.posplazita.rememberFilePicker
import com.abtsplazita.posplazita.domain.formatPrice
import androidx.compose.foundation.background
import com.abtsplazita.posplazita.domain.PermissionLevel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.abtsplazita.posplazita.domain.*
import com.abtsplazita.posplazita.domain.DeletionLog
import com.abtsplazita.posplazita.ui.users.UserViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.FlowRow

@Composable
fun PeripheralSettingsScreen(
    viewModel: PeripheralViewModel, 
    posViewModel: PosViewModel,
    userViewModel: UserViewModel,
    promotionViewModel: PromotionViewModel,
    productRepository: ProductRepository
) {
    var currentSubMenu by remember { mutableStateOf<String?>(null) }

    when (currentSubMenu) {
        "sucursal" -> BranchInfoSubMenu(viewModel, onBack = { currentSubMenu = null })
        "borrados" -> DeletionRequestsSubMenu(posViewModel, onBack = { currentSubMenu = null })
        "historial_borrados" -> DeletionLogsSubMenu(posViewModel, onBack = { currentSubMenu = null })
        "pagos" -> PaymentMethodsSubMenu(viewModel, onBack = { currentSubMenu = null })
        "ticket_designer" -> TicketDesignerScreen(viewModel, onBack = { currentSubMenu = "ticket" })
        "roles" -> SubMenuLayout(title = "Roles y Permisos", onBack = { currentSubMenu = null }, scrollable = false) {
            RolePermissionsScreen(userViewModel)
        }
        "productos_conf" -> ProductSettingsSubMenu(viewModel, productRepository, onBack = { currentSubMenu = null }, onNavigate = { currentSubMenu = it })
        "categorias" -> CategoryManagementSubMenu(viewModel, productRepository, onBack = { currentSubMenu = "productos_conf" })
        "operatividad" -> OperativitySubMenu(viewModel, posViewModel, onBack = { currentSubMenu = null })
        "ticket" -> TicketSettingsSubMenu(viewModel, onBack = { currentSubMenu = null }, onNavigate = { currentSubMenu = it })
        "punto_venta" -> PosAppSubMenu(viewModel, onBack = { currentSubMenu = null })
        "promociones" -> PromotionsSubMenu(promotionViewModel, onBack = { currentSubMenu = null })
        "monedero" -> LoyaltySubMenu(viewModel, productRepository, onBack = { currentSubMenu = null })
        "cajas" -> CajaManagementSubMenu(viewModel, onBack = { currentSubMenu = null })
        "importar" -> ProductImportSubMenu(viewModel, productRepository, onBack = { currentSubMenu = null })
        "impresora" -> PrinterSettingsSubMenu(viewModel, onBack = { currentSubMenu = null })
        "bascula" -> ScaleSettingsSubMenu(viewModel, onBack = { currentSubMenu = null })
        "recargas" -> ServicesSettingsSubMenu(viewModel, onBack = { currentSubMenu = null })
        "publicidad" -> MarketingSettingsSubMenu(viewModel, onBack = { currentSubMenu = null })
        "whatsapp_ai" -> WhatsAppAiSubMenu(viewModel, onBack = { currentSubMenu = null })
        "firebase" -> FirebaseSettingsSubMenu(viewModel, onBack = { currentSubMenu = null })
        "contapla_settings" -> ContaplaSettingsSubMenu(onBack = { currentSubMenu = null })
        else -> MainSettingsList(
            onNavigate = { currentSubMenu = it }
        )
    }
}

@Composable
fun MainSettingsList(onNavigate: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Configuración", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Personaliza el comportamiento y dispositivos del sistema", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        
        Spacer(modifier = Modifier.height(24.dp))

        SettingsGroupHeader("Mi Negocio")
        NavigationSettingCard(
            title = "Sucursal",
            subtitle = "Nombre, dirección y teléfono del local",
            icon = Icons.Default.Store,
            color = Color(0xFF673AB7),
            onClick = { onNavigate("sucursal") }
        )
        NavigationSettingCard(
            title = "Autorizaciones",
            subtitle = "Solicitudes de borrado de tickets",
            icon = Icons.Default.FactCheck,
            color = Color(0xFFE91E63),
            onClick = { onNavigate("borrados") }
        )
        NavigationSettingCard(
            title = "Reporte de Borrados",
            subtitle = "Historial de tickets eliminados",
            icon = Icons.Default.History,
            color = Color(0xFFE91E63),
            onClick = { onNavigate("historial_borrados") }
        )
        NavigationSettingCard(
            title = "Formas de Pago",
            subtitle = "Efectivo, Tarjeta y métodos personalizados",
            icon = Icons.Default.Payments,
            color = Color(0xFF4CAF50),
            onClick = { onNavigate("pagos") }
        )
        NavigationSettingCard(
            title = "Roles y Permisos",
            subtitle = "Configura qué puede hacer cada nivel de usuario",
            icon = Icons.Default.AdminPanelSettings,
            color = Color(0xFFF44336),
            onClick = { onNavigate("roles") }
        )

        Spacer(modifier = Modifier.height(16.dp))
        SettingsGroupHeader("Catálogo")
        NavigationSettingCard(
            title = "Productos",
            subtitle = "Categorías, impuestos y niveles de precio",
            icon = Icons.Default.Category,
            color = Color(0xFF2196F3),
            onClick = { onNavigate("productos_conf") }
        )

        Spacer(modifier = Modifier.height(16.dp))
        SettingsGroupHeader("Operación")
        NavigationSettingCard(
            title = "Operatividad",
            subtitle = "Permisos de venta, mayoreo y comportamiento",
            icon = Icons.Default.SettingsSuggest,
            color = Color(0xFF009688),
            onClick = { onNavigate("operatividad") }
        )
        NavigationSettingCard(
            title = "Diseño de Ticket",
            subtitle = "Personaliza cabecera, pie y líneas",
            icon = Icons.AutoMirrored.Filled.ReceiptLong,
            color = Color(0xFF607D8B),
            onClick = { onNavigate("ticket") }
        )

        Spacer(modifier = Modifier.height(16.dp))
        SettingsGroupHeader("Promociones y Lealtad")
        NavigationSettingCard(
            title = "Promociones",
            subtitle = "Ofertas, 2x1 y descuentos por fecha",
            icon = Icons.Default.ConfirmationNumber,
            color = Color(0xFFFF5722),
            onClick = { onNavigate("promociones") }
        )
        NavigationSettingCard(
            title = "Monedero Electrónico",
            subtitle = "Puntos y crédito para clientes",
            icon = Icons.Default.Wallet,
            color = Color(0xFF8BC34A),
            onClick = { onNavigate("monedero") }
        )

        Spacer(modifier = Modifier.height(16.dp))
        SettingsGroupHeader("Aplicación")
        NavigationSettingCard(
            title = "Punto de Venta",
            subtitle = "Auto-inicio y bloqueos de seguridad",
            icon = Icons.Default.Computer,
            color = Color(0xFF3F51B5),
            onClick = { onNavigate("punto_venta") }
        )
        NavigationSettingCard(
            title = "Gestión de Cajas",
            subtitle = "Configura los terminales de esta sucursal",
            icon = Icons.Default.PointOfSale,
            color = Color(0xFFE91E63),
            onClick = { onNavigate("cajas") }
        )

        Spacer(modifier = Modifier.height(16.dp))
        SettingsGroupHeader("Hardware")
        NavigationSettingCard(
            title = "Impresora de Tickets",
            subtitle = "Configuración de red y bluetooth",
            icon = Icons.Default.Print,
            color = Color(0xFF795548),
            onClick = { onNavigate("impresora") }
        )
        NavigationSettingCard(
            title = "Báscula Electrónica",
            subtitle = "Puerto COM y parámetros de pesaje",
            icon = Icons.Default.Scale,
            color = Color(0xFF00BCD4),
            onClick = { onNavigate("bascula") }
        )
        NavigationSettingCard(
            title = "Servicios y Pagos",
            subtitle = "Recargas Red Más y Mercado Pago Point",
            icon = Icons.Default.FlashOn,
            color = Color(0xFF2196F3),
            onClick = { onNavigate("recargas") }
        )

        Spacer(modifier = Modifier.height(16.dp))
        SettingsGroupHeader("Marketing y Nube")
        NavigationSettingCard(
            title = "Asistente IA WhatsApp",
            subtitle = "Configura la IA que atiende clientes",
            icon = Icons.Default.SmartToy,
            color = Color(0xFF4CAF50),
            onClick = { onNavigate("whatsapp_ai") }
        )
        NavigationSettingCard(
            title = "Anuncios y Publicidad",
            subtitle = "Gestionar imágenes del carrusel",
            icon = Icons.Default.Campaign,
            color = Color(0xFFFF9800),
            onClick = { onNavigate("publicidad") }
        )
        NavigationSettingCard(
            title = "Sincronización Cloud",
            subtitle = "Respaldo en Firebase",
            icon = Icons.Default.CloudSync,
            color = Color(0xFFFFA000),
            onClick = { onNavigate("firebase") }
        )

        Spacer(modifier = Modifier.height(16.dp))
        SettingsGroupHeader("Contabilidad y Personal")
        NavigationSettingCard(
            title = "Ajustes Contapla",
            subtitle = "Parámetros de nómina y caja contable",
            icon = Icons.Default.Calculate,
            color = Color(0xFF607D8B),
            onClick = { onNavigate("contapla_settings") }
        )
        
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun NavigationSettingCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        ListItem(
            headlineContent = { Text(title, fontWeight = FontWeight.Bold) },
            supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
            leadingContent = { 
                Surface(
                    color = color.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
                    }
                }
            },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.Gray) }
        )
    }
}

@Composable
fun SettingsGroupHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Black,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
    )
}

// --- SUBMENÚS ESPECÍFICOS ---

@Composable
fun BranchInfoSubMenu(viewModel: PeripheralViewModel, onBack: () -> Unit) {
    val branchName by viewModel.branchName.collectAsState()
    val branchAddress by viewModel.branchAddress.collectAsState()
    val branchPhone by viewModel.branchPhone.collectAsState()

    var nameEdit by remember(branchName) { mutableStateOf(branchName) }
    var addressEdit by remember(branchAddress) { mutableStateOf(branchAddress) }
    var phoneEdit by remember(branchPhone) { mutableStateOf(branchPhone) }

    SubMenuLayout(title = "Datos de Sucursal", onBack = onBack) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(value = nameEdit, onValueChange = { nameEdit = it }, label = { Text("Nombre de Sucursal") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = addressEdit, onValueChange = { addressEdit = it }, label = { Text("Dirección") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phoneEdit, onValueChange = { phoneEdit = it }, label = { Text("Teléfono") }, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = { 
                        viewModel.updateBranchInfo(nameEdit, addressEdit, phoneEdit)
                        onBack()
                    }, 
                    modifier = Modifier.align(Alignment.End)
                ) { Text("ACTUALIZAR DATOS") }
            }
        }
    }
}

@Composable
fun PaymentMethodsSubMenu(viewModel: PeripheralViewModel, onBack: () -> Unit) {
    SubMenuLayout(title = "Formas de Pago", onBack = onBack) {
        Text("Métodos Habilitados", style = MaterialTheme.typography.titleMedium)
        Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Column {
                ListItem(headlineContent = { Text("Efectivo") }, trailingContent = { Icon(Icons.Default.Lock, null, tint = Color.Gray) })
                HorizontalDivider()
                ListItem(headlineContent = { Text("Tarjeta (Mercado Pago)") }, trailingContent = { Icon(Icons.Default.Lock, null, tint = Color.Gray) })
                HorizontalDivider()
                ListItem(headlineContent = { Text("Transferencia") }, trailingContent = { Switch(checked = true, onCheckedChange = {}) })
            }
        }
        OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Icon(Icons.Default.Add, null)
            Text("AÑADIR MÉTODO PERSONALIZADO")
        }
    }
}

@Composable
fun RolePermissionsScreen(viewModel: UserViewModel) {
    val selectedRole by viewModel.selectedRoleForPerms.collectAsState()
    val enabledPermissions by viewModel.currentRolePermissions.collectAsState()

    Row(modifier = Modifier.fillMaxSize()) {
        // Columna de Roles
        Column(modifier = Modifier.weight(0.3f).fillMaxHeight().padding(16.dp)) {
            Text("Roles del Sistema", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            
            Role.entries.forEach { role ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { viewModel.selectRoleForPermissions(role) },
                    colors = CardDefaults.cardColors(containerColor = if (selectedRole == role) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                ) {
                    Text(role.name, modifier = Modifier.padding(16.dp), fontWeight = if (selectedRole == role) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }

        VerticalDivider()

        // Columna de Permisos
        Column(modifier = Modifier.weight(0.7f).fillMaxHeight().padding(24.dp)) {
            Text("Configurar Permisos: ${selectedRole.name}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text("Niveles: IZQ = Apagado | CENTRO = Requiere PIN Admin | DER = Habilitado", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            
            Spacer(Modifier.height(24.dp))

            if (selectedRole == Role.SUPER_ADMIN) {
                Surface(color = Color(0xFFE8F5E9), modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                    Text("El Administrador siempre tiene todos los permisos habilitados.", modifier = Modifier.padding(16.dp), color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    Permission.entries.toList().groupBy { getPermissionGroup(it) }.forEach { (group, perms) ->
                        item {
                            Text(group, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = Color.Gray, modifier = Modifier.padding(vertical = 12.dp))
                        }
                        items(perms) { perm ->
                            val currentLevel = enabledPermissions[perm] ?: PermissionLevel.DISABLED
                            
                            ListItem(
                                headlineContent = { Text(getPermissionLabel(perm), fontWeight = FontWeight.Bold) },
                                supportingContent = { Text(getPermissionDescription(perm)) },
                                trailingContent = {
                                    ThreeStateSegmentedToggle(
                                        currentLevel = currentLevel,
                                        onLevelChange = { viewModel.updatePermissionLevel(perm, it) }
                                    )
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThreeStateSegmentedToggle(
    currentLevel: PermissionLevel,
    onLevelChange: (PermissionLevel) -> Unit
) {
    Row(
        modifier = Modifier.width(180.dp).height(36.dp).background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.extraLarge).padding(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PermissionLevel.entries.forEach { level ->
            val isSelected = currentLevel == level
            Surface(
                modifier = Modifier.weight(1f).fillMaxHeight().clickable { onLevelChange(level) },
                color = if (isSelected) {
                    when(level) {
                        PermissionLevel.DISABLED -> Color.Red
                        PermissionLevel.RESTRICTED -> Color(0xFFFF9800)
                        PermissionLevel.ENABLED -> Color(0xFF4CAF50)
                    }
                } else Color.Transparent,
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when(level) {
                            PermissionLevel.DISABLED -> Icons.Default.Block
                            PermissionLevel.RESTRICTED -> Icons.Default.AdminPanelSettings
                            PermissionLevel.ENABLED -> Icons.Default.CheckCircle
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (isSelected) Color.White else Color.Gray
                    )
                }
            }
        }
    }
}

private fun getPermissionGroup(perm: Permission): String = when(perm) {
    Permission.MAKE_SALE, Permission.SELL_ON_CREDIT, Permission.ACCEPT_CARD_PAYMENT, 
    Permission.DELETE_SALE_ITEM, Permission.CANCEL_SALE, Permission.OPEN_CASH_DRAWER -> "VENTAS"
    
    Permission.PERFORM_CASH_OUT, Permission.PERFORM_PRE_CUT, Permission.MANAGE_CASH_MOVEMENTS -> "CAJA Y DINERO"
    
    Permission.PRODUCT_VIEW, Permission.PRODUCT_CREATE, Permission.PRODUCT_EDIT, Permission.PRODUCT_DELETE -> "CATÁLOGO PRODUCTOS"
    
    Permission.CUSTOMER_VIEW, Permission.CUSTOMER_CREATE, Permission.CUSTOMER_EDIT, Permission.CUSTOMER_DELETE -> "CATÁLOGO CLIENTES"
    
    Permission.SUPPLIER_VIEW, Permission.SUPPLIER_CREATE, Permission.SUPPLIER_EDIT, Permission.SUPPLIER_DELETE -> "CATÁLOGO PROVEEDORES"
    
    Permission.MANAGE_PURCHASES -> "ALMACÉN"
    
    Permission.MANAGE_WITHDRAWALS -> "VENTAS"

    Permission.VIEW_REPORTS, Permission.VIEW_ACCOUNTING, Permission.MANAGE_SETTINGS, Permission.MANAGE_USERS -> "SISTEMA"
}

private fun getPermissionLabel(perm: Permission): String = when(perm) {
    Permission.MAKE_SALE -> "Realizar Ventas"
    Permission.SELL_ON_CREDIT -> "Venta a Crédito (Fiar)"
    Permission.ACCEPT_CARD_PAYMENT -> "Cobrar con Tarjeta"
    Permission.DELETE_SALE_ITEM -> "Eliminar Productos de Venta"
    Permission.CANCEL_SALE -> "Cancelar Venta Completa"
    Permission.OPEN_CASH_DRAWER -> "Abrir Cajón Manualmente"
    Permission.PERFORM_CASH_OUT -> "Hacer Corte de Caja"
    Permission.PERFORM_PRE_CUT -> "Realizar Precorte"
    Permission.MANAGE_CASH_MOVEMENTS -> "Movimientos de Dinero"
    Permission.PRODUCT_VIEW -> "Ver Catálogo Productos"
    Permission.PRODUCT_CREATE -> "Crear Productos"
    Permission.PRODUCT_EDIT -> "Editar Productos"
    Permission.PRODUCT_DELETE -> "Eliminar Productos"
    Permission.CUSTOMER_VIEW -> "Ver Catálogo Clientes"
    Permission.CUSTOMER_CREATE -> "Crear Clientes"
    Permission.CUSTOMER_EDIT -> "Editar Clientes"
    Permission.CUSTOMER_DELETE -> "Eliminar Clientes"
    Permission.SUPPLIER_VIEW -> "Ver Catálogo Proveedores"
    Permission.SUPPLIER_CREATE -> "Crear Proveedores"
    Permission.SUPPLIER_EDIT -> "Editar Proveedores"
    Permission.SUPPLIER_DELETE -> "Eliminar Proveedores"
    Permission.MANAGE_PURCHASES -> "Capturar Compras"
    Permission.MANAGE_WITHDRAWALS -> "Realizar Retiros de Efectivo"
    Permission.VIEW_REPORTS -> "Ver Consultas / Reportes"
    Permission.VIEW_ACCOUNTING -> "Ver Contabilidad"
    Permission.MANAGE_SETTINGS -> "Configuraciones del Sistema"
    Permission.MANAGE_USERS -> "Administrar Usuarios"
}

private fun getPermissionDescription(perm: Permission): String = when(perm) {
    Permission.MAKE_SALE -> "Acceso a la pantalla principal de ventas."
    Permission.SELL_ON_CREDIT -> "Habilitar el botón de crédito en checkout."
    Permission.ACCEPT_CARD_PAYMENT -> "Permite usar terminales bancarias / MP."
    Permission.DELETE_SALE_ITEM -> "Permite quitar un artículo ya agregado al carrito."
    Permission.CANCEL_SALE -> "Permite vaciar todo el carrito de venta."
    Permission.OPEN_CASH_DRAWER -> "Botón para abrir el cajón sin venta."
    Permission.PERFORM_CASH_OUT -> "Permite cerrar la caja y ver arqueos."
    Permission.PERFORM_PRE_CUT -> "Validación rápida de efectivo sin cierre."
    Permission.MANAGE_CASH_MOVEMENTS -> "Entradas y salidas manuales de efectivo."
    Permission.PRODUCT_VIEW -> "Visualización del catálogo de productos."
    Permission.PRODUCT_CREATE -> "Registrar nuevos artículos en el catálogo."
    Permission.PRODUCT_EDIT -> "Modificar datos de productos existentes."
    Permission.PRODUCT_DELETE -> "Dar de baja definitiva a productos."
    Permission.CUSTOMER_VIEW -> "Visualización de la lista de clientes."
    Permission.CUSTOMER_CREATE -> "Dar de alta a nuevos clientes."
    Permission.CUSTOMER_EDIT -> "Modificar perfiles de clientes."
    Permission.CUSTOMER_DELETE -> "Eliminar clientes del sistema."
    Permission.SUPPLIER_VIEW -> "Visualización de la lista de proveedores."
    Permission.SUPPLIER_CREATE -> "Registrar nuevos proveedores."
    Permission.SUPPLIER_EDIT -> "Modificar datos de proveedores."
    Permission.SUPPLIER_DELETE -> "Eliminar proveedores del sistema."
    Permission.MANAGE_PURCHASES -> "Registrar entradas de almacén y facturas."
    Permission.MANAGE_WITHDRAWALS -> "Permite dar efectivo a clientes contra cargo a tarjeta."
    Permission.VIEW_REPORTS -> "Visualización de historial de tickets y cortes."
    Permission.VIEW_ACCOUNTING -> "Acceso al resumen contable y nóminas."
    Permission.MANAGE_SETTINGS -> "Ajustes de sucursal, ticket, impresora y dispositivos."
    Permission.MANAGE_USERS -> "Configurar otros usuarios y sus permisos."
}

@Composable
fun ProductSettingsSubMenu(viewModel: PeripheralViewModel, repository: ProductRepository, onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val defaultPriceLevel by viewModel.defaultPriceLevel.collectAsState()

    SubMenuLayout(title = "Configuración de Productos", onBack = onBack) {
        Text("Nivel de Precio por Defecto", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val levels = listOf(1 to "P1 (Mayoreo)", 2 to "P2 (Público)", 3 to "P3 (Adicional)")
            levels.forEach { (level, label) ->
                FilterChip(
                    selected = defaultPriceLevel == level,
                    onClick = { viewModel.setDefaultPriceLevel(level) },
                    label = { Text(label) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))
        NavigationSettingCard(
            title = "Categorías", 
            subtitle = "Gestionar grupos de productos", 
            icon = Icons.Default.Category, 
            color = Color(0xFF2196F3), 
            onClick = { onNavigate("categorias") }
        )
        NavigationSettingCard(title = "Impuestos (IVA)", subtitle = "Tasas aplicables (0%, 8%, 16%)", icon = Icons.Default.Receipt, color = Color(0xFF673AB7), onClick = {})
        NavigationSettingCard(title = "Unidades de Medida", subtitle = "Pza, Kg, Litro, etc.", icon = Icons.Default.Straighten, color = Color(0xFF4CAF50), onClick = {})
    }
}

@Composable
fun CategoryManagementSubMenu(viewModel: PeripheralViewModel, repository: ProductRepository, onBack: () -> Unit) {
    val categories by repository.getCategories().collectAsState(emptyList())
    var newCategoryName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    
    SubMenuLayout(title = "Gestionar Categorías", onBack = onBack) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Añadir Nueva Categoría", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Nombre") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = { 
                            if (newCategoryName.isNotBlank()) {
                                scope.launch {
                                    repository.addCategory(newCategoryName)
                                    newCategoryName = ""
                                }
                            }
                        },
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Text("Añadir")
                    }
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        Text("Categorías Actuales", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column {
                if (categories.isEmpty()) {
                    Text("No hay categorías personalizadas.", modifier = Modifier.padding(16.dp), color = Color.Gray)
                } else {
                    categories.forEach { cat ->
                        ListItem(
                            headlineContent = { Text(cat) },
                            trailingContent = {
                                if (cat != "General") {
                                    IconButton(onClick = { /* Implement delete if repository supports it */ }) {
                                        Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun OperativitySubMenu(viewModel: PeripheralViewModel, posViewModel: PosViewModel, onBack: () -> Unit) {
    val allowNegativeStock by viewModel.allowNegativeStock.collectAsState()
    val askQty by viewModel.askQuantityOnAdd.collectAsState()
    val addAtTop by viewModel.addAtTop.collectAsState()
    val wholesale by viewModel.isWholesaleEnabled.collectAsState()
    val scope = rememberCoroutineScope()

    SubMenuLayout(title = "Operatividad", onBack = onBack) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column {
                SwitchSettingItem("Vender sin existencias", "Permitir ventas aunque el stock sea 0", allowNegativeStock, { viewModel.toggleAllowNegativeStock(it) })
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SwitchSettingItem("Solicitar cantidad", "Pedir cantidad siempre al agregar producto", askQty, { viewModel.toggleAskQuantityOnAdd(it) })
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SwitchSettingItem("Agregar al inicio", "Los productos nuevos aparecen arriba de la lista", addAtTop, { viewModel.toggleAddAtTop(it) })
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SwitchSettingItem("Habilitar Mayoreo", "Activar cambios automáticos de precio por volumen", wholesale, { viewModel.toggleWholesale(it) })
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                
                Button(
                    onClick = { 
                        scope.launch {
                            posViewModel.refreshCatalog() // Esto ahora llamará a una versión más robusta
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.Sync, null)
                    Spacer(Modifier.width(8.dp))
                    Text("FORZAR SINCRONIZACIÓN TOTAL")
                }
            }
        }
    }
}

@Composable
fun TicketSettingsSubMenu(viewModel: PeripheralViewModel, onBack: () -> Unit, onNavigate: (String) -> Unit) {
    var logoUrl by remember { mutableStateOf("") }
    var branchAddress by remember { mutableStateOf("") }
    var branchPhone by remember { mutableStateOf("") }
    var facebook by remember { mutableStateOf("") }
    var instagram by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }
    var thanksMsg by remember { mutableStateOf("Gracias por su compra!") }
    var showBranch by remember { mutableStateOf(true) }
    var ticketPrefix by remember { mutableStateOf("S") }

    val settings by viewModel.allSettings.collectAsState()

    LaunchedEffect(settings) {
        logoUrl = settings["ticket_logo_path"] ?: ""
        branchAddress = settings["ticket_branch_address"] ?: ""
        branchPhone = settings["ticket_branch_phone"] ?: ""
        facebook = settings["ticket_facebook"] ?: ""
        instagram = settings["ticket_instagram"] ?: ""
        whatsapp = settings["ticket_whatsapp"] ?: ""
        thanksMsg = settings["ticket_thanks_message"] ?: "Gracias por su compra!"
        showBranch = settings["ticket_show_branch"]?.toBoolean() ?: true
        ticketPrefix = settings["ticket_id_prefix"] ?: "S"
    }

    SubMenuLayout(title = "Diseño de Ticket", onBack = onBack) {
        Text("Personaliza el formato impreso para tus clientes", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Spacer(Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Información de la Tienda", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                
                OutlinedTextField(
                    value = branchAddress,
                    onValueChange = { branchAddress = it },
                    label = { Text("Dirección") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.LocationOn, null) }
                )

                OutlinedTextField(
                    value = branchPhone,
                    onValueChange = { branchPhone = it },
                    label = { Text("Teléfono") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Phone, null) }
                )

                HorizontalDivider()
                Text("Redes Sociales", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = facebook,
                        onValueChange = { facebook = it },
                        label = { Text("Facebook") },
                        modifier = Modifier.weight(1f),
                        leadingIcon = { Icon(Icons.Default.Facebook, null) }
                    )
                    OutlinedTextField(
                        value = instagram,
                        onValueChange = { instagram = it },
                        label = { Text("Instagram") },
                        modifier = Modifier.weight(1f),
                        leadingIcon = { Icon(Icons.Default.PhotoCamera, null) }
                    )
                }

                OutlinedTextField(
                    value = whatsapp,
                    onValueChange = { whatsapp = it },
                    label = { Text("WhatsApp de atención") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Phone, null) }
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Folios y Mensajes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = ticketPrefix,
                    onValueChange = { if(it.length <= 3) ticketPrefix = it.uppercase() },
                    label = { Text("Prefijo de Ticket (Ej: S, T, V)") },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("Máximo 3 letras") }
                )

                OutlinedTextField(
                    value = thanksMsg,
                    onValueChange = { thanksMsg = it },
                    label = { Text("Mensaje de Agradecimiento") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = showBranch, onCheckedChange = { showBranch = it })
                    Text("Mostrar Nombre de Sucursal en ticket")
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { onNavigate("ticket_designer") },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Icon(Icons.Default.Architecture, null)
            Spacer(Modifier.width(8.dp))
            Text("EDITOR VISUAL DE TICKET")
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                viewModel.updateTicketConfig(logoUrl, facebook, instagram, whatsapp, thanksMsg, showBranch, ticketPrefix, branchAddress, branchPhone)
                onBack()
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Icon(Icons.Default.Save, null)
            Spacer(Modifier.width(8.dp))
            Text("GUARDAR DISEÑO")
        }
    }
}

@Composable
fun PromotionsSubMenu(viewModel: PromotionViewModel, onBack: () -> Unit) {
    val promotions by viewModel.promotions.collectAsState()
    val editingPromo by viewModel.editingPromotion.collectAsState()
    
    SubMenuLayout(title = "Promociones y Ofertas", onBack = onBack) {
        if (editingPromo == null) {
            Text("Tipos de Promoción", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PromotionTypeCard(
                    title = "Precio Fijo", 
                    subtitle = "Artículos con precio especial", 
                    icon = Icons.Default.Label, 
                    color = Color(0xFFE91E63),
                    onClick = { viewModel.startNewPromotion(PromotionType.FIXED_PRICE) }
                )
                PromotionTypeCard(
                    title = "Descuento %", 
                    subtitle = "Por categoría completa", 
                    icon = Icons.Default.Percent, 
                    color = Color(0xFF2196F3),
                    onClick = { viewModel.startNewPromotion(PromotionType.CATEGORY_PERCENT) }
                )
                PromotionTypeCard(
                    title = "Multibuy", 
                    subtitle = "2 por $20, etc.", 
                    icon = Icons.Default.Inventory, 
                    color = Color(0xFFFF9800),
                    onClick = { viewModel.startNewPromotion(PromotionType.BULK_OFFER) }
                )
                PromotionTypeCard(
                    title = "Monto Total", 
                    subtitle = "Desc. si supera $1000", 
                    icon = Icons.Default.AddCard, 
                    color = Color(0xFF4CAF50),
                    onClick = { viewModel.startNewPromotion(PromotionType.TOTAL_AMOUNT_PERCENT) }
                )
            }
            
            Spacer(Modifier.height(32.dp))
            Text("Promociones Activas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column {
                    if (promotions.isEmpty()) {
                        Text("No hay promociones registradas.", modifier = Modifier.padding(16.dp), color = Color.Gray)
                    } else {
                        promotions.forEach { promo ->
                            ListItem(
                                headlineContent = { Text(promo.name, fontWeight = FontWeight.Bold) },
                                supportingContent = { 
                                    val typeText = when(promo.type) {
                                        PromotionType.FIXED_PRICE -> "Precio Fijo"
                                        PromotionType.CATEGORY_PERCENT -> "Descuento %"
                                        PromotionType.BULK_OFFER -> "Multibuy"
                                        PromotionType.TOTAL_AMOUNT_PERCENT -> "Monto Total"
                                    }
                                    Text("$typeText | Finaliza: ${Instant.fromEpochMilliseconds(promo.endDate).toLocalDateTime(TimeZone.currentSystemDefault()).date}")
                                },
                                trailingContent = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Switch(checked = promo.isActive, onCheckedChange = { viewModel.togglePromotion(promo) })
                                        IconButton(onClick = { viewModel.deletePromotion(promo) }) {
                                            Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.5f))
                                        }
                                    }
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        } else {
            PromotionEditPanel(viewModel, editingPromo!!)
        }
    }
}

@Composable
fun PromotionTypeCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(180.dp).clickable { onClick() },
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(color = color.copy(alpha = 0.1f), shape = MaterialTheme.shapes.medium, modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = color) }
            }
            Spacer(Modifier.height(12.dp))
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Color.Gray, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun PromotionEditPanel(viewModel: PromotionViewModel, promo: Promotion) {
    val products by viewModel.products.collectAsState()
    val categories by viewModel.categories.collectAsState()
    
    var name by remember { mutableStateOf(promo.name) }
    var discValue by remember { mutableStateOf(promo.discountValue.toString()) }
    var triggerQty by remember { mutableStateOf(promo.triggerQuantity.toString()) }
    var selectedProdId by remember { mutableStateOf(promo.productId) }
    var selectedCat by remember { mutableStateOf(promo.category) }
    var startDate by remember { mutableStateOf(promo.startDate) }
    var endDate by remember { mutableStateOf(promo.endDate) }
    
    val selectedProduct = products.find { it.id == selectedProdId }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Nueva Promoción: ${promo.type.name}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre de la Promo") }, modifier = Modifier.fillMaxWidth())
        
        when(promo.type) {
            PromotionType.FIXED_PRICE, PromotionType.BULK_OFFER -> {
                // Selector de Producto
                var prodExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { prodExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedProduct?.name ?: "Seleccionar Producto...")
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(expanded = prodExpanded, onDismissRequest = { prodExpanded = false }) {
                        products.take(20).forEach { p ->
                            DropdownMenuItem(text = { Text(p.name) }, onClick = { selectedProdId = p.id; prodExpanded = false })
                        }
                    }
                }
                
                if (selectedProduct != null) {
                    Text("Precio actual: $${selectedProduct.price3.formatPrice()} | Costo: $${selectedProduct.cost.formatPrice()}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    
                    if (promo.type == PromotionType.FIXED_PRICE) {
                        OutlinedTextField(
                            value = discValue, 
                            onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) discValue = it },
                            label = { Text("Precio Especial") },
                            prefix = { Text("$ ") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        // Calcular Ganancia
                        val newPrice = discValue.toDoubleOrNull() ?: 0.0
                        val margin = if (newPrice > 0) ((newPrice / selectedProduct.cost) - 1.0) * 100.0 else 0.0
                        Text("Margen de ganancia: ${margin.toInt()}%", color = if (margin < 10) Color.Red else Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = triggerQty, 
                                onValueChange = { if (it.all { c -> c.isDigit() }) triggerQty = it },
                                label = { Text("Cantidad") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = discValue, 
                                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) discValue = it },
                                label = { Text("Precio Paquete") },
                                prefix = { Text("$ ") },
                                modifier = Modifier.weight(1.5f)
                            )
                        }
                        Text("Ejemplo: Compra 2 por $20.00", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
            }
            PromotionType.CATEGORY_PERCENT -> {
                var catExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { catExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedCat ?: "Seleccionar Categoría...")
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                        categories.forEach { c ->
                            DropdownMenuItem(text = { Text(c) }, onClick = { selectedCat = c; catExpanded = false })
                        }
                    }
                }
                OutlinedTextField(
                    value = discValue, 
                    onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) discValue = it },
                    label = { Text("Porcentaje de Descuento") },
                    suffix = { Text("%") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            PromotionType.TOTAL_AMOUNT_PERCENT -> {
                OutlinedTextField(
                    value = triggerQty, 
                    onValueChange = { if (it.all { c -> c.isDigit() }) triggerQty = it },
                    label = { Text("Monto Mínimo de Compra") },
                    prefix = { Text("$ ") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = discValue, 
                    onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) discValue = it },
                    label = { Text("Porcentaje de Descuento") },
                    suffix = { Text("%") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Ejemplo: Si el total es mayor a $1000, dar 5% de descuento.", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }

        // --- SECCIÓN DE FECHAS (CALENDARIO) ---
        Text("Vigencia de la Promoción", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PromotionDateField(
                label = "Fecha Inicio",
                timestamp = startDate,
                onDateSelected = { startDate = it },
                modifier = Modifier.weight(1f)
            )
            PromotionDateField(
                label = "Fecha Fin",
                timestamp = endDate,
                onDateSelected = { endDate = it },
                modifier = Modifier.weight(1f)
            )
        }
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { viewModel.cancelEdit() }) { Text("CANCELAR") }
            Spacer(Modifier.width(16.dp))
            Button(
                onClick = { 
                    viewModel.updateEditingPromotion(promo.copy(
                        name = name,
                        productId = selectedProdId,
                        category = selectedCat,
                        discountValue = discValue.toDoubleOrNull() ?: 0.0,
                        triggerQuantity = triggerQty.toIntOrNull() ?: 1,
                        startDate = startDate,
                        endDate = endDate
                    ))
                    viewModel.savePromotion()
                },
                enabled = name.isNotBlank() && (selectedProdId != null || selectedCat != null || promo.type == PromotionType.TOTAL_AMOUNT_PERCENT)
            ) { Text("GUARDAR PROMOCIÓN") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromotionDateField(label: String, timestamp: Long, onDateSelected: (Long) -> Unit, modifier: Modifier = Modifier) {
    var showPicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = timestamp)

    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        OutlinedButton(
            onClick = { showPicker = true },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(8.dp)
        ) {
            Icon(Icons.Default.DateRange, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            val dt = Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(TimeZone.currentSystemDefault())
            Text("${dt.dayOfMonth}/${dt.monthNumber}/${dt.year}")
        }
    }

    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { onDateSelected(it) }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("CANCELAR") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun LoyaltySubMenu(viewModel: PeripheralViewModel, repository: ProductRepository, onBack: () -> Unit) {
    val walletPercentages by viewModel.walletPercentages.collectAsState()
    val categories by repository.getCategories().collectAsState(emptyList())

    SubMenuLayout(title = "Monedero Electrónico", onBack = onBack) {
        Text("Porcentaje de acumulación por categoría", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Define cuánto dinero (en %) acumulará el cliente en su monedero por cada peso comprado en esta categoría.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        
        Spacer(Modifier.height(16.dp))

        if (categories.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No hay categorías configuradas.", color = Color.Gray)
            }
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    categories.forEach { category ->
                        val currentPercent = walletPercentages[category] ?: 0.0
                        var textValue by remember(currentPercent) { mutableStateOf(currentPercent.toString()) }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(category, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                            
                            OutlinedTextField(
                                value = textValue,
                                onValueChange = { 
                                    if (it.isEmpty() || it.all { c -> c.isDigit() || c == '.' }) {
                                        textValue = it
                                        it.toDoubleOrNull()?.let { v -> viewModel.updateWalletPercentage(category, v) }
                                    }
                                },
                                label = { Text("% Acumulación") },
                                modifier = Modifier.width(150.dp),
                                suffix = { Text("%") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                        if (category != categories.last()) HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PosAppSubMenu(viewModel: PeripheralViewModel, onBack: () -> Unit) {
    val autoLogin by viewModel.autoBranchLogin.collectAsState()
    val lockBranch by viewModel.lockBranchChange.collectAsState()
    val maxCash by viewModel.maxCashLimit.collectAsState()
    val isWebshopEnabled by viewModel.isWebshopEnabled.collectAsState()

    SubMenuLayout(title = "Configuración del Punto de Venta", onBack = onBack) {
        Text("Ajustes específicos de esta computadora/dispositivo", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Spacer(Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column {
                SwitchSettingItem("Auto-inicio en sucursal", "Entrar directo a la sucursal actual al abrir", autoLogin, { viewModel.toggleAutoBranchLogin(it) })
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SwitchSettingItem("Bloquear cambio de sucursal", "Solo el administrador puede cambiar de tienda", lockBranch, { viewModel.toggleLockBranchChange(it) })
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Servicios Cloud", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Column {
                SwitchSettingItem(
                    title = "Habilitar Webshop",
                    description = "Permitir que esta sucursal provea stock para pedidos web",
                    checked = isWebshopEnabled,
                    onCheckedChange = { viewModel.toggleWebshop(it) }
                )
            }
        }
        
        Spacer(Modifier.height(24.dp))
        Text("Control de Efectivo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Column(Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = maxCash.toString(),
                    onValueChange = { it.toDoubleOrNull()?.let { v -> viewModel.updateMaxCashLimit(v) } },
                    label = { Text("Límite de efectivo en caja") },
                    prefix = { Text("$ ") },
                    supportingText = { Text("El sistema avisará cuando se deba realizar un retiro parcial.") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }

        if (lockBranch) {
            Text("Requiere autorización del administrador para modificar", color = Color.Red, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(8.dp))
        }
    }
}

@Composable
fun OperationSettingsSubMenu(viewModel: PeripheralViewModel, posViewModel: PosViewModel, onBack: () -> Unit) {
    // Redirigir o mantener por compatibilidad
    OperativitySubMenu(viewModel, posViewModel, onBack)
}

@Composable
fun PrinterSettingsSubMenu(viewModel: PeripheralViewModel, onBack: () -> Unit) {
    val connectionType by viewModel.connectionType.collectAsState()
    val printerName by viewModel.printerName.collectAsState()
    val printerAddress by viewModel.printerAddress.collectAsState()
    val bluetoothMac by viewModel.bluetoothMac.collectAsState()
    val isPrinterConnected by viewModel.isPrinterConnected.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val isBluetoothAvailable by viewModel.isBluetoothAvailable.collectAsState()
    
    val paperSize by viewModel.paperSize.collectAsState()
    val autoCut by viewModel.autoCut.collectAsState()
    val openDrawerOnPrint by viewModel.openDrawerOnPrint.collectAsState()
    val availableSystemPrinters by viewModel.availableSystemPrinters.collectAsState()
    val availableBluetoothDevices by viewModel.availableBluetoothDevices.collectAsState()

    SubMenuLayout(title = "Impresora de Tickets", onBack = onBack) {
        OutlinedTextField(
            value = printerName,
            onValueChange = { viewModel.updatePrinterName(it) },
            label = { Text("Nombre Identificador") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("Tipo de Conexión", style = MaterialTheme.typography.titleMedium)
        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = connectionType == PrinterConnectionType.NETWORK,
                onClick = { viewModel.setConnectionType(PrinterConnectionType.NETWORK) },
                label = { Text("Red / Ethernet") }
            )
            if (isBluetoothAvailable) {
                FilterChip(
                    selected = connectionType == PrinterConnectionType.BLUETOOTH,
                    onClick = { viewModel.setConnectionType(PrinterConnectionType.BLUETOOTH) },
                    label = { Text("Bluetooth") }
                )
            }
            FilterChip(
                selected = connectionType == PrinterConnectionType.SERIAL,
                onClick = { viewModel.setConnectionType(PrinterConnectionType.SERIAL) },
                label = { Text("Puerto COM") }
            )
            FilterChip(
                selected = connectionType == PrinterConnectionType.SYSTEM,
                onClick = { viewModel.setConnectionType(PrinterConnectionType.SYSTEM); viewModel.refreshSystemPrinters() },
                label = { Text("Instalada (Driver)") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        when (connectionType) {
            PrinterConnectionType.NETWORK -> {
                OutlinedTextField(
                    value = printerAddress,
                    onValueChange = { viewModel.updatePrinterAddress(it) },
                    label = { Text("Dirección IP (Ej: 192.168.1.100)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            PrinterConnectionType.BLUETOOTH -> {
                var showDevices by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = bluetoothMac,
                        onValueChange = { viewModel.updateBluetoothMac(it) },
                        label = { Text("Dirección MAC / Dispositivo") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { viewModel.startBluetoothScan(); showDevices = true }) {
                                if (isScanning) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                else Icon(Icons.Default.Bluetooth, null)
                            }
                        }
                    )
                    DropdownMenu(expanded = showDevices && availableBluetoothDevices.isNotEmpty(), onDismissRequest = { showDevices = false }) {
                        availableBluetoothDevices.forEach { dev ->
                            DropdownMenuItem(
                                text = { Text("${dev.first} (${dev.second})") },
                                onClick = { viewModel.updateBluetoothMac(dev.second); showDevices = false }
                            )
                        }
                    }
                }
            }
            PrinterConnectionType.SERIAL -> {
                OutlinedTextField(
                    value = bluetoothMac, // Reusing bluetoothMac for COM port name in Serial mode
                    onValueChange = { viewModel.updateBluetoothMac(it) },
                    label = { Text("Nombre del Puerto (Ej: COM3)") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { viewModel.startBluetoothScan() }) {
                             Icon(Icons.Default.Usb, null)
                        }
                    }
                )
            }
            PrinterConnectionType.SYSTEM -> {
                var showPrinters by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = printerName,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Seleccionar Impresora Instalada") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { IconButton(onClick = { viewModel.refreshSystemPrinters(); showPrinters = true }) { Icon(Icons.Default.ArrowDropDown, null) } }
                    )
                    DropdownMenu(expanded = showPrinters && availableSystemPrinters.isNotEmpty(), onDismissRequest = { showPrinters = false }) {
                        availableSystemPrinters.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p) },
                                onClick = { viewModel.updatePrinterName(p); showPrinters = false }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Configuración de Papel y Hardware", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Ancho de Papel", style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = paperSize == 58, onClick = { viewModel.updatePaperSize(58) })
                    Text("58mm", modifier = Modifier.clickable { viewModel.updatePaperSize(58) })
                    Spacer(Modifier.width(24.dp))
                    RadioButton(selected = paperSize == 80, onClick = { viewModel.updatePaperSize(80) })
                    Text("80mm", modifier = Modifier.clickable { viewModel.updatePaperSize(80) })
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Apertura de Cajón", fontWeight = FontWeight.Bold)
                        Text("Abrir cajón al imprimir ticket", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Switch(checked = openDrawerOnPrint, onCheckedChange = { viewModel.toggleOpenDrawerOnPrint(it) })
                }

                if (openDrawerOnPrint) {
                    val drawerCommand by viewModel.drawerCommand.collectAsState()
                    Text("Pin / Comando de Apertura", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("EPSON_PIN2" to "Epson Pin 2", "EPSON_PIN5" to "Epson Pin 5", "STAR" to "Star").forEach { (cmd, label) ->
                            FilterChip(
                                selected = drawerCommand == cmd,
                                onClick = { viewModel.setDrawerCommand(cmd) },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Corte Automático", fontWeight = FontWeight.Bold)
                        Text("Enviar comando de corte al finalizar", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Switch(checked = autoCut, onCheckedChange = { viewModel.toggleAutoCut(it) })
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { viewModel.togglePrinterConnection() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (isPrinterConnected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary)
        ) {
            Text(if (isPrinterConnected) "IMPRESORA CONECTADA" else "ESTABLECER CONEXIÓN")
        }
        
        if (isPrinterConnected) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.printTest() }, modifier = Modifier.weight(1f)) {
                    Text("PÁGINA PRUEBA", textAlign = TextAlign.Center)
                }
                Button(
                    onClick = { viewModel.openDrawerAndPrintTest() }, 
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
                ) {
                    Icon(Icons.Default.Payments, null)
                    Spacer(Modifier.width(4.dp))
                    Text("ABRIR Y PROBAR", textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun ScaleSettingsSubMenu(viewModel: PeripheralViewModel, onBack: () -> Unit) {
    val scalePort by viewModel.scalePort.collectAsState()
    val availablePorts by viewModel.availablePorts.collectAsState()
    val scaleBaudRate by viewModel.scaleBaudRate.collectAsState()
    val scaleSequence by viewModel.scaleSequence.collectAsState()
    val scaleDelay by viewModel.scaleDelay.collectAsState()
    val isScaleConnected by viewModel.isScaleConnected.collectAsState()
    val lastScaleWeight by viewModel.lastScaleWeight.collectAsState()
    val isTestingScale by viewModel.isTestingScale.collectAsState()

    SubMenuLayout(title = "Báscula Electrónica", onBack = onBack) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            var portExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = scalePort,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Puerto COM") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { IconButton(onClick = { portExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } }
                )
                DropdownMenu(expanded = portExpanded, onDismissRequest = { portExpanded = false }) {
                    availablePorts.forEach { port ->
                        DropdownMenuItem(text = { Text(port) }, onClick = { viewModel.updateScalePort(port); portExpanded = false })
                    }
                }
            }
            OutlinedTextField(value = scaleBaudRate, onValueChange = { viewModel.updateScaleBaudRate(it) }, label = { Text("Baudios") }, modifier = Modifier.weight(0.7f))
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(value = scaleSequence, onValueChange = { viewModel.updateScaleSequence(it) }, label = { Text("Secuencia") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = scaleDelay, onValueChange = { viewModel.updateScaleDelay(it) }, label = { Text("Retardo (ms)") }, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { viewModel.toggleScaleConnection() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (isScaleConnected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary)
        ) {
            if (isTestingScale && !isScaleConnected) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            else Text(if (isScaleConnected) "BÁSCULA CONECTADA" else "CONECTAR HARDWARE")
        }

        if (isScaleConnected) {
            OutlinedButton(onClick = { viewModel.testScale() }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                if (isTestingScale) CircularProgressIndicator(modifier = Modifier.size(24.dp)) else Text("CAPTURAR PESO AHORA")
            }
            if (lastScaleWeight != null) {
                Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Text("Lectura actual: ${lastScaleWeight!!.formatWeight()} kg", modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun ServicesSettingsSubMenu(viewModel: PeripheralViewModel, onBack: () -> Unit) {
    val redMasUser by viewModel.redMasUser.collectAsState()
    val redMasPass by viewModel.redMasPass.collectAsState()
    val isRedMasActive by viewModel.isRedMasActive.collectAsState()
    
    val mpTerminalId by viewModel.mpTerminalId.collectAsState()
    val mpAccessToken by viewModel.mpAccessToken.collectAsState()
    val mpClientId by viewModel.mpClientId.collectAsState()
    val mpUserId by viewModel.mpUserId.collectAsState()
    val mpPublicKey by viewModel.mpPublicKey.collectAsState()
    
    val availableMpDevices by viewModel.availableMpDevices.collectAsState()
    
    val mpStatus by viewModel.mpStatus.collectAsState()

    var mpTokenEdit by remember(mpAccessToken) { mutableStateOf(mpAccessToken) }
    var mpClientIdEdit by remember(mpClientId) { mutableStateOf(mpClientId) }
    var mpUserIdEdit by remember(mpUserId) { mutableStateOf(mpUserId) }
    var mpPublicKeyEdit by remember(mpPublicKey) { mutableStateOf(mpPublicKey) }

    SubMenuLayout(title = "Servicios y Pagos", onBack = onBack) {
        Text("Red Más (Recargas)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(value = redMasUser, onValueChange = { viewModel.updateRedMasCredentials(it, redMasPass) }, label = { Text("Usuario") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = redMasPass, onValueChange = { viewModel.updateRedMasCredentials(redMasUser, it) }, label = { Text("Contraseña / Key") }, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Servicio Habilitado")
                    Switch(checked = isRedMasActive, onCheckedChange = { viewModel.toggleRedMas(it) })
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Mercado Pago Point", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Credenciales", style = MaterialTheme.typography.labelMedium)
                
                OutlinedTextField(
                    value = mpPublicKeyEdit, 
                    onValueChange = { mpPublicKeyEdit = it }, 
                    label = { Text("Public Key") }, 
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = mpTokenEdit, 
                    onValueChange = { mpTokenEdit = it }, 
                    label = { Text("Access Token") }, 
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )
                
                Text("Detalles", style = MaterialTheme.typography.labelMedium)

                OutlinedTextField(
                    value = mpUserIdEdit, 
                    onValueChange = { mpUserIdEdit = it }, 
                    label = { Text("User ID") }, 
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = mpClientIdEdit, 
                    onValueChange = { mpClientIdEdit = it }, 
                    label = { Text("N° de aplicación") }, 
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = mpTerminalId, 
                    onValueChange = { viewModel.updateMpTerminalId(it) }, 
                    label = { Text("ID del Dispositivo (Point)") }, 
                    placeholder = { Text("Ej: SN-12345678") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Button(
                    onClick = { viewModel.saveMpCredentials(mpTokenEdit, mpClientIdEdit, mpUserIdEdit, mpPublicKeyEdit) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = mpTokenEdit.isNotBlank()
                ) {
                    Text("GUARDAR CREDENCIALES")
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Estado Point: $mpStatus")
                    Button(onClick = { 
                        viewModel.refreshMpDevices()
                    }) {
                        Text("VER TERMINALES VINCULADAS")
                    }
                }

                if (availableMpDevices.isNotEmpty()) {
                    Text("Terminales detectadas en tu cuenta:", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 16.dp))
                    availableMpDevices.forEach { deviceId ->
                        ListItem(
                            headlineContent = { Text(deviceId) },
                            trailingContent = { 
                                Row {
                                    TextButton(onClick = { viewModel.activatePdvMode(deviceId) }) { Text("ACTIVAR PDV") }
                                    Button(onClick = { viewModel.updateMpTerminalId(deviceId) }) { Text("USAR ESTA") }
                                }
                            }
                        )
                    }
                } else {
                    // Opción para activar manualmente si se conoce el ID
                    OutlinedButton(
                        onClick = { viewModel.activatePdvMode(mpTerminalId) },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        enabled = mpTerminalId.isNotBlank()
                    ) {
                        Text("FORZAR ACTIVACIÓN DE MODO PDV")
                    }
                }
            }
        }
    }
}

@Composable
fun MarketingSettingsSubMenu(viewModel: PeripheralViewModel, onBack: () -> Unit) {
    val adImages by viewModel.adImages.collectAsState()
    val appLogoUrl by viewModel.appLogoUrl.collectAsState()
    var newUrl by remember { mutableStateOf("") }
    var logoUrlEdit by remember(appLogoUrl) { mutableStateOf(appLogoUrl) }

    SubMenuLayout(title = "Imagen y Publicidad", onBack = onBack) {
        // --- SECCION LOGO ---
        Text("Logotipo del Sistema", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("URL del logo que aparece en el inicio de sesión", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = logoUrlEdit, onValueChange = { logoUrlEdit = it }, label = { Text("URL del Logo") }, modifier = Modifier.weight(1f))
            Button(onClick = { viewModel.updateAppLogoUrl(logoUrlEdit) }, modifier = Modifier.height(56.dp)) { Text("Guardar") }
        }
        
        Spacer(Modifier.height(32.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        // --- SECCION PUBLICIDAD ---
        Text("Imágenes del Carrusel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Las imágenes rotarán cada 2 minutos en la pantalla de ventas", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = newUrl, onValueChange = { newUrl = it }, label = { Text("URL de Imagen") }, modifier = Modifier.weight(1f))
            Button(onClick = { viewModel.addAdImage(newUrl); newUrl = "" }, modifier = Modifier.height(56.dp)) { Text("Añadir") }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        adImages.forEach { url ->
            ListItem(
                headlineContent = { Text(url, maxLines = 1, style = MaterialTheme.typography.bodySmall) },
                trailingContent = { IconButton(onClick = { viewModel.removeAdImage(url) }) { Icon(Icons.Default.Delete, null, tint = Color.Red) } }
            )
            HorizontalDivider()
        }
    }
}

@Composable
fun FirebaseSettingsSubMenu(viewModel: PeripheralViewModel, onBack: () -> Unit) {
    val isWebshopEnabled by viewModel.isWebshopEnabled.collectAsState()
    val lastSync by viewModel.lastWebshopSync.collectAsState()
    val isSyncing by viewModel.isWebshopSyncing.collectAsState()

    SubMenuLayout(title = "Firebase Cloud", onBack = onBack) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudDone, null, tint = Color(0xFF4CAF50))
                    Spacer(Modifier.width(12.dp))
                    Text("Sincronización Activa", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Toda tu información (Productos, Ventas y Clientes) se está respaldando automáticamente en la nube.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color(0xFFFFA000))
                Text("Sincronizado hace un momento", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Módulo Webshop", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (!isWebshopEnabled) {
                    Surface(color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f), shape = MaterialTheme.shapes.small) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(12.dp))
                            Text("Webshop desactivada para esta sucursal. Actívala en 'Punto de Venta' para sincronizar.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                } else {
                    Text("Sincroniza tus productos y existencias actuales para que aparezcan en tu tienda en línea.", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(16.dp))
                    
                    Button(
                        onClick = { viewModel.performManualWebshopSync() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSyncing,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                            Spacer(Modifier.width(12.dp))
                            Text("SINCRONIZANDO...")
                        } else {
                            Icon(Icons.Default.Upload, null)
                            Spacer(Modifier.width(8.dp))
                            Text("SINCRONIZAR TODO AHORA")
                        }
                    }

                    if (lastSync != null) {
                        Spacer(Modifier.height(12.dp))
                        val dt = Instant.fromEpochMilliseconds(lastSync!!).toLocalDateTime(TimeZone.currentSystemDefault())
                        Text("Última sincronización: ${dt.date} ${dt.time.toString().take(5)}", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("IMPORTANTE", style = MaterialTheme.typography.labelLarge, color = Color.Red, fontWeight = FontWeight.Bold)
        Text(
            "Asegúrate de haber colocado los archivos 'google-services.json' y 'GoogleService-Info.plist' en las carpetas correspondientes para que la conexión sea exitosa.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun ContaplaSettingsSubMenu(onBack: () -> Unit) {
    SubMenuLayout(title = "Ajustes Contapla", onBack = onBack) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Configuración de Nómina", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                SwitchSettingItem("Cálculo automático de bonos", "Basar bonos en puntualidad y asistencia", true, {})
                OutlinedTextField(value = "Lunes", onValueChange = {}, label = { Text("Inicio de semana") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {}, modifier = Modifier.align(Alignment.End)) { Text("GUARDAR AJUSTES") }
            }
        }
    }
}

@Composable
fun DeletionRequestsSubMenu(viewModel: PosViewModel, onBack: () -> Unit) {
    val requests by viewModel.deletionRequests.collectAsState()
    // Cambiamos a setOf<String>() para ticketIds
    var processingTicketIds by remember { mutableStateOf(setOf<String>()) }
    var successMsg by remember { mutableStateOf<String?>(null) }
    
    SubMenuLayout(title = "Solicitudes de Borrado", onBack = onBack, scrollable = false) {
        if (successMsg != null) {
            Surface(color = Color(0xFFE8F5E9), modifier = Modifier.fillMaxWidth().padding(16.dp), shape = MaterialTheme.shapes.medium) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32))
                    Spacer(Modifier.width(12.dp))
                    Text(successMsg!!, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { successMsg = null }) { Text("OK") }
                }
            }
        }

        Text("Tickets que los cajeros han solicitado eliminar", modifier = Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Spacer(Modifier.height(16.dp))

        if (requests.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay solicitudes pendientes.", color = Color.Gray)
            }
        } else {
            val visibleRequests = requests.filter { !processingTicketIds.contains(it.ticketId) }
            if (visibleRequests.isEmpty() && requests.isNotEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    items(visibleRequests) { request ->
                        val isProcessing = processingTicketIds.contains(request.ticketId)
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text("Solicitado por: @${request.userId}", fontWeight = FontWeight.Bold)
                                        val dt = Instant.fromEpochMilliseconds(request.timestamp).toLocalDateTime(TimeZone.currentSystemDefault())
                                        Text("${dt.date} ${dt.time.toString().take(5)}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    }
                                    Text("$${request.total.formatPrice()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                }
                                
                                Spacer(Modifier.height(8.dp))
                                Text("Contenido:", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                                Text(request.itemsSummary, style = MaterialTheme.typography.bodySmall)
                                
                                Spacer(Modifier.height(16.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(
                                        onClick = { 
                                            processingTicketIds += request.ticketId
                                            viewModel.approveDeletionRequest(request) 
                                            successMsg = "Ticket aprobado. Se eliminará en la terminal de origen."
                                        },
                                        modifier = Modifier.weight(1f),
                                        enabled = !isProcessing,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                    ) {
                                        if (isProcessing) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                        else {
                                            Icon(Icons.Default.Check, null)
                                            Spacer(Modifier.width(8.dp))
                                            Text("APROBAR Y BORRAR")
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = { 
                                            processingTicketIds += request.ticketId
                                            viewModel.rejectDeletionRequest(request) 
                                            successMsg = "Solicitud rechazada."
                                        },
                                        enabled = !isProcessing,
                                        modifier = Modifier.weight(0.5f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                                    ) {
                                        Text("RECHAZAR")
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
fun WhatsAppAiSubMenu(viewModel: PeripheralViewModel, onBack: () -> Unit) {
    val isEnabled by viewModel.isAiEnabled.collectAsState()
    val lastSync by viewModel.lastAiSync.collectAsState()
    val isSyncing by viewModel.isAiSyncing.collectAsState()

    SubMenuLayout(title = "Asistente IA WhatsApp", onBack = onBack) {
        Text("Configura la IA que atiende a tus clientes", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Spacer(Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SmartToy, null, tint = if (isEnabled) Color(0xFF4CAF50) else Color.Gray)
                    Spacer(Modifier.width(12.dp))
                    Text("Estado del Asistente", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Switch(checked = isEnabled, onCheckedChange = { viewModel.toggleAi(it) })
                }
                Text(
                    text = if (isEnabled) "La IA está activa y respondiendo mensajes." else "La IA está apagada. No responderá a nadie.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isEnabled) Color(0xFF2E7D32) else Color.Gray
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Base de Conocimientos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Sincroniza tus productos, precios y existencias para que la IA sepa qué responder.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.syncAiKnowledgeBase() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSyncing,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        Spacer(Modifier.width(12.dp))
                        Text("SINCRONIZANDO...")
                    } else {
                        Icon(Icons.Default.CloudSync, null)
                        Spacer(Modifier.width(8.dp))
                        Text("SINCRONIZAR DATOS PARA IA")
                    }
                }

                if (lastSync != null) {
                    Spacer(Modifier.height(12.dp))
                    val dt = Instant.fromEpochMilliseconds(lastSync!!).toLocalDateTime(TimeZone.currentSystemDefault())
                    Text("Última actualización: ${dt.date} ${dt.time.toString().take(5)}", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f), shape = MaterialTheme.shapes.medium) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text("Nota: Los mensajes que responda la IA aparecerán en tu panel de WhatsApp Business como enviados por el asistente.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}


@Composable
fun DeletionLogsSubMenu(viewModel: PosViewModel, onBack: () -> Unit) {
    val logs by viewModel.deletionLogs.collectAsState()
    
    SubMenuLayout(title = "Reporte de Tickets Borrados", onBack = onBack, scrollable = false) {
        Text("Historial de ventas en espera que fueron eliminadas con autorización", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Spacer(Modifier.height(24.dp))

        if (logs.isEmpty()) {
            Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Text("No hay registros de borrado.", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                items(logs) { log: DeletionLog ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Folio: #${log.ticketId.split("-").lastOrNull() ?: log.ticketId}", fontWeight = FontWeight.Bold)
                                    val dt = Instant.fromEpochMilliseconds(log.timestamp).toLocalDateTime(TimeZone.currentSystemDefault())
                                    Text("${dt.date} ${dt.time.toString().take(5)}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                                Text("$${log.total.formatPrice()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                            }
                            
                            Spacer(Modifier.height(8.dp))
                            Text("Solicitó: @${log.requesterId} | Autorizó: @${log.approverId}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text("Contenido: ${log.itemsSummary}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubMenuLayout(
    title: String, 
    onBack: () -> Unit, 
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val modifier = if (scrollable) {
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())
    } else {
        Modifier.fillMaxSize().padding(16.dp)
    }
    
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(24.dp))
        content()
    }
}

@Composable
fun SwitchSettingItem(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Bold) },
        supportingContent = { Text(description) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) }
    )
}

@Composable
fun ProductImportSubMenu(
    viewModel: PeripheralViewModel, 
    repository: ProductRepository, 
    onBack: () -> Unit
) {
    val importData by viewModel.importData.collectAsState()
    val importError by viewModel.importError.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    
    val filePicker = rememberFilePicker()
    
    var rawText by remember { mutableStateOf("") }
    var mapping by remember { mutableStateOf(mutableMapOf<String, Int>()) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val fields = listOf(
        "barcode" to "Código de Barras",
        "name" to "Nombre del Producto",
        "cost" to "Costo",
        "price3" to "Precio Público (P3)",
        "price1" to "Precio 1",
        "price2" to "Precio 2",
        "price4" to "Precio 4",
        "category" to "Categoría",
        "tax" to "IVA %",
        "isBulk" to "¿Es a Granel? (si/no)",
        "useScale" to "¿Usa Báscula? (si/no)"
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Text("Importar Productos", style = MaterialTheme.typography.headlineMedium)
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        if (successMessage != null) {
            AlertDialog(
                onDismissRequest = { successMessage = null },
                confirmButton = { Button(onClick = { successMessage = null }) { Text("Aceptar") } },
                title = { Text("Importación Exitosa") },
                text = { Text(successMessage!!) }
            )
        }

        if (importData.isEmpty()) {
            // PASO 1: Pegar texto o Subir archivo
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Paso 1: Carga tus productos", style = MaterialTheme.typography.titleLarge)
                    Text("Puedes subir un archivo CSV/Excel o copiar y pegar las columnas directamente.", style = MaterialTheme.typography.bodySmall)
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Button(
                        onClick = { 
                            filePicker.pickFile { bytes: ByteArray? ->
                                if (bytes != null) viewModel.processImportFile(bytes)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Icon(Icons.Default.FileUpload, null)
                        Spacer(Modifier.width(8.dp))
                        Text("SELECCIONAR ARCHIVO (CSV)")
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("O pega el contenido aquí:", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = rawText,
                        onValueChange = { rawText = it },
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        placeholder = { Text("Código\tNombre\tPrecio...") },
                        label = { Text("Contenido de Excel") }
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    if (importError != null) {
                        Text(importError!!, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                    }

                    Button(
                        onClick = { viewModel.processImportText(rawText) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = rawText.isNotBlank()
                    ) {
                        Text("ANALIZAR DATOS PEGADOS")
                    }
                }
            }
        } else {
            // PASO 2: Mapear columnas
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Paso 2: Relacionar Columnas", style = MaterialTheme.typography.titleLarge)
                        TextButton(onClick = { viewModel.clearImport() }) { Text("CANCELAR") }
                    }
                    Text("Indica qué contiene cada columna de tu tabla.", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(16.dp))
                    
                    // Vista previa de la primera fila
                    val firstRow = importData.first()
                    
                    fields.forEach { (key, label) ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            
                            var expanded by remember { mutableStateOf(false) }
                            val currentMapping = mapping[key]
                            
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                                    val cellText = currentMapping?.let { firstRow.getOrNull(it) } ?: "Omitir"
                                    Text(if (currentMapping != null) "Col. ${currentMapping + 1} ($cellText)" else "Omitir")
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
                            viewModel.executeImport(repository, mapping) { count ->
                                successMessage = "Se han importado $count productos correctamente."
                                rawText = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isImporting && mapping.containsKey("name") && mapping.containsKey("barcode")
                    ) {
                        if (isImporting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        else Text("IMPORTAR ${importData.size} REGISTROS")
                    }
                    
                    if (!mapping.containsKey("name") || !mapping.containsKey("barcode")) {
                        Text("Debes mapear al menos Nombre y Código de Barras.", color = Color.Gray, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            Text("Vista previa (3 filas):", style = MaterialTheme.typography.labelMedium)
            importData.take(3).forEach { row ->
                Text(row.joinToString(" | "), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun CajaManagementSubMenu(viewModel: PeripheralViewModel, onBack: () -> Unit) {
    val terminals by viewModel.terminals.collectAsState()
    val selectedTerminalId by viewModel.selectedTerminalId.collectAsState()
    val branchName by viewModel.branchName.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("Gestión de Cajas", style = MaterialTheme.typography.headlineMedium)
                Text("Sucursal: ${branchName.ifBlank { viewModel.branchId }}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Registrar Nueva Caja", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                
                var newTerminalName by remember { mutableStateOf("") }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newTerminalName,
                        onValueChange = { newTerminalName = it },
                        label = { Text("Nombre de la Caja") },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ej: Caja 1, Caja Principal") }
                    )
                    Button(
                        onClick = { 
                            viewModel.addTerminal(newTerminalName)
                            newTerminalName = ""
                        },
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Text("Añadir")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Cajas en esta sucursal:", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (terminals.isNotEmpty()) {
            terminals.forEach { terminal ->
                val isSelected = terminal.id == selectedTerminalId
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { viewModel.selectTerminal(terminal.id) },
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.small,
                    border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(terminal.name, style = MaterialTheme.typography.titleMedium, fontWeight = if (isSelected) FontWeight.Bold else null)
                            Text("ID único: ${terminal.id}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        if (isSelected) {
                            Icon(Icons.Default.CheckCircle, "Seleccionada", tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                        }
                        IconButton(onClick = { viewModel.deleteTerminal(terminal.id) }) {
                            Icon(Icons.Default.Delete, "Borrar", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No hay cajas configuradas.\nAñade una arriba para comenzar.",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun TicketDesignerScreen(viewModel: PeripheralViewModel, onBack: () -> Unit) {
    val layout by viewModel.ticketLayout.collectAsState()
    val branchName by viewModel.branchName.collectAsState()

    SubMenuLayout(title = "Editor Visual de Ticket", onBack = onBack, scrollable = false) {
        Text("Personaliza el orden y visibilidad de los campos", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        
        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            // Panel de Control - Lista de bloques
            Column(modifier = Modifier.weight(1.1f).verticalScroll(rememberScrollState())) {
                Text("Cuerpo del Ticket", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Usa las flechas para mover los bloques y el check para ocultarlos", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Spacer(Modifier.height(12.dp))
                
                layout.forEachIndexed { index, element ->
                    TicketElementCard(
                        element = element,
                        isFirst = index == 0,
                        isLast = index == layout.size - 1,
                        onToggleVisible = {
                            val newList = layout.toMutableList()
                            newList[index] = element.copy(visible = it)
                            viewModel.updateTicketLayout(newList)
                        },
                        onMoveUp = {
                            val newList = layout.toMutableList()
                            val tmp = newList[index]
                            newList[index] = newList[index - 1]
                            newList[index - 1] = tmp
                            viewModel.updateTicketLayout(newList)
                        },
                        onMoveDown = {
                            val newList = layout.toMutableList()
                            val tmp = newList[index]
                            newList[index] = newList[index + 1]
                            newList[index + 1] = tmp
                            viewModel.updateTicketLayout(newList)
                        },
                        onAlignmentChange = { alignment ->
                            val newList = layout.toMutableList()
                            newList[index] = element.copy(alignment = alignment)
                            viewModel.updateTicketLayout(newList)
                        },
                        onLabelChange = { newLabel ->
                            val newList = layout.toMutableList()
                            newList[index] = element.copy(label = newLabel)
                            viewModel.updateTicketLayout(newList)
                        }
                    )
                }
                Spacer(Modifier.height(32.dp))
            }

            // Vista Previa Vertical
            Column(modifier = Modifier.weight(0.9f)) {
                val paperSize by viewModel.paperSize.collectAsState()
                
                Text("Vista Previa Real (${paperSize}mm)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxSize().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color.LightGray),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("[ ÁREA DE IMPRESIÓN ]", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.height(16.dp))
                        
                        layout.filter { it.visible }.forEach { element ->
                            PreviewElement(element, paperSize, branchName)
                        }
                        
                        Spacer(Modifier.height(32.dp))
                        Text("- Fin de Ticket -", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun TicketElementCard(
    element: TicketElement,
    isFirst: Boolean,
    isLast: Boolean,
    onToggleVisible: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onAlignmentChange: (TicketAlignment) -> Unit,
    onLabelChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (element.visible) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, if (element.visible) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag Handle visual (indicativo)
            Icon(Icons.Default.DragHandle, null, tint = Color.LightGray, modifier = Modifier.padding(horizontal = 4.dp))
            
            Checkbox(checked = element.visible, onCheckedChange = onToggleVisible)
            
            Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when(element.type) {
                            TicketElementType.LOGO -> "Logo de Empresa"
                            TicketElementType.HEADER -> "Encabezado Personalizado"
                            TicketElementType.BRANCH_INFO -> "Nombre de Tienda"
                            TicketElementType.BRANCH_ADDRESS -> "Dirección de Tienda"
                            TicketElementType.BRANCH_PHONE -> "Teléfono de Tienda"
                            TicketElementType.DIVIDER -> "Línea Divisoria"
                            TicketElementType.TICKET_ID -> "Folio de Venta"
                            TicketElementType.DATE -> "Fecha y Hora"
                            TicketElementType.CUSTOMER_INFO -> "Información del Cliente"
                            TicketElementType.ITEMS_TABLE -> "Lista de Productos"
                            TicketElementType.TOTAL -> "Monto Total"
                            TicketElementType.PAYMENT_INFO -> "Métodos de Pago"
                            TicketElementType.WALLET_BALANCE -> "Saldo Monedero"
                            TicketElementType.COMMENT -> "Nota de Venta"
                            TicketElementType.THANKS_MESSAGE -> "Mensaje de Despedida"
                            TicketElementType.SOCIAL_MEDIA -> "Redes Sociales"
                            TicketElementType.SPACE -> "Espacio en Blanco"
                            TicketElementType.TERMINAL_INFO -> "Número de Caja"
                        },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (element.visible) Color.Unspecified else Color.Gray,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (element.visible && element.type != TicketElementType.DIVIDER && element.type != TicketElementType.SPACE) {
                        AlignmentToggle(element.alignment, onAlignmentChange)
                    }
                }
                
                if (element.type == TicketElementType.HEADER && element.visible) {
                    OutlinedTextField(
                        value = element.label ?: "TICKET DE VENTA",
                        onValueChange = onLabelChange,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        textStyle = MaterialTheme.typography.bodySmall,
                        singleLine = true
                    )
                }
            }

            // Controles de movimiento
            Column {
                IconButton(onClick = onMoveUp, enabled = !isFirst, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.KeyboardArrowUp, null, tint = if (isFirst) Color.LightGray else MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onMoveDown, enabled = !isLast, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = if (isLast) Color.LightGray else MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun AlignmentToggle(current: TicketAlignment, onAlignmentChange: (TicketAlignment) -> Unit) {
    Row(
        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small).padding(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val alignments = listOf(
            TicketAlignment.LEFT to Icons.Default.FormatAlignLeft,
            TicketAlignment.CENTER to Icons.Default.FormatAlignCenter,
            TicketAlignment.RIGHT to Icons.Default.FormatAlignRight
        )
        
        alignments.forEach { (align, icon) ->
            val isSelected = current == align
            Surface(
                modifier = Modifier.size(28.dp).clickable { onAlignmentChange(align) },
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = MaterialTheme.shapes.small
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isSelected) Color.White else Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun PreviewElement(element: TicketElement, paperSize: Int, branchName: String) {
    val mono = androidx.compose.ui.text.font.FontFamily.Monospace
    val lineChars = if (paperSize == 58) 30 else 40
    
    val textAlign = when(element.alignment) {
        TicketAlignment.LEFT -> TextAlign.Start
        TicketAlignment.CENTER -> TextAlign.Center
        TicketAlignment.RIGHT -> TextAlign.End
    }

    val contentModifier = Modifier.fillMaxWidth()
    
    when (element.type) {
        TicketElementType.LOGO -> {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = when(element.alignment) {
                TicketAlignment.LEFT -> Alignment.CenterStart
                TicketAlignment.CENTER -> Alignment.Center
                TicketAlignment.RIGHT -> Alignment.CenterEnd
            }) {
                Surface(modifier = Modifier.size(60.dp), color = Color.LightGray, shape = MaterialTheme.shapes.small) {
                    Icon(Icons.Default.Image, null, modifier = Modifier.padding(12.dp))
                }
            }
        }
        TicketElementType.HEADER -> {
            Text(
                text = element.label ?: "TICKET DE VENTA", 
                fontWeight = FontWeight.Black, 
                style = MaterialTheme.typography.titleMedium,
                fontFamily = mono,
                textAlign = textAlign,
                modifier = contentModifier
            )
        }
        TicketElementType.BRANCH_INFO -> {
            Text(branchName.ifBlank { "Abarrotes Joshua" }, style = MaterialTheme.typography.bodySmall, fontFamily = mono, textAlign = textAlign, modifier = contentModifier)
        }
        TicketElementType.BRANCH_ADDRESS -> {
            Text("Bugambilia, 44, la cantera, tepic, nayarit, C.P. 63506", style = MaterialTheme.typography.bodySmall, fontFamily = mono, textAlign = textAlign, modifier = contentModifier)
        }
        TicketElementType.BRANCH_PHONE -> {
            Text("3116107766", style = MaterialTheme.typography.bodySmall, fontFamily = mono, textAlign = textAlign, modifier = contentModifier)
        }
        TicketElementType.DIVIDER -> {
            Text("-".repeat(lineChars), fontFamily = mono, color = Color.Gray, maxLines = 1)
        }
        TicketElementType.TICKET_ID -> {
            Text("70450", style = MaterialTheme.typography.bodySmall, fontFamily = mono, modifier = contentModifier, textAlign = textAlign)
        }
        TicketElementType.DATE -> {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Venta", fontFamily = mono, style = MaterialTheme.typography.bodySmall)
                Text("03/09/2026", fontFamily = mono, style = MaterialTheme.typography.bodySmall)
            }
            Text("06:51:42 AM", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End, fontFamily = mono, style = MaterialTheme.typography.bodySmall)
        }
        TicketElementType.CUSTOMER_INFO -> {
            Text("Cliente: Público en General", style = MaterialTheme.typography.bodySmall, fontFamily = mono, modifier = contentModifier, textAlign = textAlign)
        }
        TicketElementType.ITEMS_TABLE -> {
            Column(modifier = Modifier.fillMaxWidth()) {
                repeat(1) {
                    val qty = "1.0"
                    val name = "Rastrillo Eco"
                    val price = "$5.00"
                    val subtotal = "$5.00"
                    
                    Text("$qty x $name", fontFamily = mono, style = MaterialTheme.typography.bodySmall)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(price, fontFamily = mono, style = MaterialTheme.typography.bodySmall)
                        Text(subtotal, fontFamily = mono, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        TicketElementType.TOTAL -> {
            val label = "Total(1) MXN:"
            val value = "$5.00"
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(label, fontFamily = mono, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(8.dp))
                Text(value, fontWeight = FontWeight.Bold, fontFamily = mono, style = MaterialTheme.typography.bodySmall)
            }
        }
        TicketElementType.PAYMENT_INFO -> {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text("Efectivo MXN:", fontFamily = mono, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(8.dp))
                Text("$5.00", fontFamily = mono, style = MaterialTheme.typography.bodySmall)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text("Cambio MXN:", fontFamily = mono, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(8.dp))
                Text("$0.00", fontFamily = mono, style = MaterialTheme.typography.bodySmall)
            }
        }
        TicketElementType.TERMINAL_INFO -> {
            Text("Caja 1", style = MaterialTheme.typography.bodySmall, fontFamily = mono, textAlign = textAlign, modifier = contentModifier)
        }
        TicketElementType.WALLET_BALANCE -> {
            Text("SALDO MONEDERO:".padEnd(lineChars - 6) + "$50.00", style = MaterialTheme.typography.labelMedium, color = Color.Blue, fontFamily = mono, textAlign = textAlign, modifier = contentModifier)
        }
        TicketElementType.COMMENT -> {
            Text("NOTA: Cliente frecuente", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray, fontFamily = mono, textAlign = textAlign, modifier = contentModifier)
        }
        TicketElementType.THANKS_MESSAGE -> {
            Text("Gracias por su compra!", style = MaterialTheme.typography.bodySmall, fontFamily = mono, textAlign = textAlign, modifier = contentModifier)
        }
        TicketElementType.SOCIAL_MEDIA -> {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = when(element.alignment) {
                TicketAlignment.LEFT -> Alignment.Start
                TicketAlignment.CENTER -> Alignment.CenterHorizontally
                TicketAlignment.RIGHT -> Alignment.End
            }) {
                Text("FB: facebook_user", style = MaterialTheme.typography.labelSmall, fontFamily = mono)
                Text("IG: @instagram_user", style = MaterialTheme.typography.labelSmall, fontFamily = mono)
                Text("WA: 1234567890", style = MaterialTheme.typography.labelSmall, fontFamily = mono)
            }
        }
        TicketElementType.SPACE -> {
            Spacer(Modifier.height(12.dp))
        }
    }
    Spacer(Modifier.height(4.dp))
}
