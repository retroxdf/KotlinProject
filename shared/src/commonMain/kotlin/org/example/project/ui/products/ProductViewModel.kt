package com.abtsplazita.posplazita.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.abtsplazita.posplazita.data.remote.ProductApiService
import com.abtsplazita.posplazita.domain.calculatePriceFromUtility
import com.abtsplazita.posplazita.domain.calculateDefaultPrice1
import com.abtsplazita.posplazita.domain.calculateDefaultPrice2
import com.abtsplazita.posplazita.domain.calculateDefaultPrice3
import com.abtsplazita.posplazita.domain.Product
import com.abtsplazita.posplazita.domain.User
import com.abtsplazita.posplazita.domain.StockMovement
import com.abtsplazita.posplazita.domain.repository.ProductRepository
import com.abtsplazita.posplazita.domain.repository.BranchRepository
import com.abtsplazita.posplazita.domain.repository.StockMovementRepository

class ProductViewModel(
    private val repository: ProductRepository,
    private val branchRepository: BranchRepository,
    private val movementRepository: StockMovementRepository,
    private val userRepository: com.abtsplazita.posplazita.domain.repository.UserRepository? = null,
    val currentBranchId: String
) : ViewModel() {
    
    private val apiService = ProductApiService()

    init {
        viewModelScope.launch {
            val current = repository.getCategories().first()
            if (current.size <= 1) { // Si solo hay General o nada
                listOf("General", "Abarrotes", "Bebidas", "Limpieza", "Frituras", "Lácteos", "Panadería").forEach {
                    repository.addCategory(it)
                }
            }
        }
    }

    val products: StateFlow<List<Product>> = repository.getProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val branches: StateFlow<List<com.abtsplazita.posplazita.domain.Branch>> = branchRepository.getAllBranches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allInventory: StateFlow<List<com.abtsplazita.posplazita.domain.Inventory>> = repository.getAllInventory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<String>> = repository.getCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val taxes: StateFlow<List<Double>> = repository.getTaxes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf(0.0, 8.0, 16.0))

    private val _detailProduct = MutableStateFlow<Product?>(null)
    val detailProduct = _detailProduct.asStateFlow()

    private val _productMovements = MutableStateFlow<List<StockMovement>>(emptyList())
    val productMovements = _productMovements.asStateFlow()

    private val _editingProduct = MutableStateFlow<Product?>(null)
    val editingProduct = _editingProduct.asStateFlow()

    private val _editingStock = MutableStateFlow(0.0)
    val editingStock = _editingStock.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _userPermissions = MutableStateFlow<Map<com.abtsplazita.posplazita.domain.Permission, com.abtsplazita.posplazita.domain.PermissionLevel>>(emptyMap())
    val userPermissions = _userPermissions.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _importData = MutableStateFlow<List<List<String>>>(emptyList())
    val importData = _importData.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting = _isImporting.asStateFlow()

    fun setUserInfo(user: User?, permissions: Map<com.abtsplazita.posplazita.domain.Permission, com.abtsplazita.posplazita.domain.PermissionLevel>) {
        _currentUser.value = user
        _userPermissions.value = permissions
    }

    private fun hasPermission(permission: com.abtsplazita.posplazita.domain.Permission): Boolean {
        return _userPermissions.value[permission] == com.abtsplazita.posplazita.domain.PermissionLevel.ENABLED
    }

    private fun isRestricted(permission: com.abtsplazita.posplazita.domain.Permission): Boolean {
        return _userPermissions.value[permission] == com.abtsplazita.posplazita.domain.PermissionLevel.RESTRICTED
    }

    // --- Autorización de Administrador ---
    private val _showAuthDialog = MutableStateFlow(false)
    val showAuthDialog = _showAuthDialog.asStateFlow()

    private val _authTitle = MutableStateFlow("")
    val authTitle = _authTitle.asStateFlow()

    private var pendingAction: (() -> Unit)? = null

    private fun requestAuthorization(title: String, action: () -> Unit) {
        _authTitle.value = title
        pendingAction = action
        _showAuthDialog.value = true
    }

    fun closeAuthDialog() {
        _showAuthDialog.value = false
        pendingAction = null
    }

    fun authorizeWithPin(pin: String) {
        viewModelScope.launch {
            val user = userRepository?.getUserByNip(pin)
            if (user != null && (user.role == com.abtsplazita.posplazita.domain.Role.SUPER_ADMIN || user.role == com.abtsplazita.posplazita.domain.Role.GERENTE)) {
                pendingAction?.invoke()
                closeAuthDialog()
            } else {
                _errorMessage.value = "NIP de administrador inválido o sin permisos."
            }
        }
    }


    private val _defaultPriceLevel = MutableStateFlow(2)
    val defaultPriceLevel = _defaultPriceLevel.asStateFlow()

    private val _catalogSearchQuery = MutableStateFlow("")
    val catalogSearchQuery = _catalogSearchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    val filteredProducts: StateFlow<List<Product>> = combine(products, _catalogSearchQuery, _selectedCategory) { allProducts, query, category ->
        allProducts.filter { product ->
            val matchesQuery = query.isBlank() || 
                product.name.contains(query, ignoreCase = true) || 
                product.barcode.contains(query, ignoreCase = true)
            val matchesCategory = category == null || product.category == category
            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateCatalogSearchQuery(query: String) {
        _catalogSearchQuery.value = query
    }

    fun selectCategoryFilter(category: String?) {
        _selectedCategory.value = category
    }

    fun setDefaultPriceLevel(level: Int) {
        _defaultPriceLevel.value = level
    }

    fun showProductDetail(product: Product) {
        _detailProduct.value = product
        viewModelScope.launch {
            movementRepository.getMovements(product.id).collect {
                _productMovements.value = it
            }
        }
    }

    fun closeProductDetail() {
        _detailProduct.value = null
    }

    fun resetToCatalog() {
        _detailProduct.value = null
        _editingProduct.value = null
        _errorMessage.value = null
        _catalogSearchQuery.value = ""
        _selectedCategory.value = null
    }

    fun startNewProduct(barcode: String = "") {
        val level = _userPermissions.value[com.abtsplazita.posplazita.domain.Permission.PRODUCT_CREATE] ?: com.abtsplazita.posplazita.domain.PermissionLevel.DISABLED
        if (level == com.abtsplazita.posplazita.domain.PermissionLevel.DISABLED) {
            _errorMessage.value = "No tienes permiso para crear productos."
            return
        }

        _errorMessage.value = null
        _editingProduct.value = Product(
            id = "", 
            name = "",
            barcode = barcode,
            isBulk = false
        )
        _editingStock.value = 0.0
    }

    fun editProduct(product: Product) {
        val level = _userPermissions.value[com.abtsplazita.posplazita.domain.Permission.PRODUCT_EDIT] ?: com.abtsplazita.posplazita.domain.PermissionLevel.DISABLED
        if (level == com.abtsplazita.posplazita.domain.PermissionLevel.DISABLED) {
            _errorMessage.value = "No tienes permiso para editar productos."
            return
        }

        _errorMessage.value = null
        _editingProduct.value = product
        viewModelScope.launch {
            _editingStock.value = repository.getStock(product.id, currentBranchId)
        }
    }

    fun updateProduct(product: Product) {
        val old = _editingProduct.value
        val oldBarcode = old?.barcode ?: ""
        val oldCost = old?.cost ?: 0.0
        val oldTax = old?.tax ?: 0.0
        
        var updated = product
        
        // Auto-calculo de precios si el costo o el IVA cambiaron según requerimiento
        if ((product.cost != oldCost || product.tax != oldTax) && product.cost > 0) {
            val costBase = product.cost
            val taxRate = product.tax
            val costFinal = costBase * (1 + taxRate / 100)
            
            val p2 = calculateDefaultPrice2(costFinal)
            val p1 = calculateDefaultPrice1(costFinal)
            val p3 = p2 + 0.50
            
            updated = updated.copy(
                price2 = p2,
                price1 = p1,
                price3 = p3,
                price4 = 0.0
            )
        }
        
        _editingProduct.value = updated
        
        // Auto-fetch si el código cambia y tiene longitud estándar
        if (product.barcode != oldBarcode && (product.barcode.length == 8 || product.barcode.length == 13)) {
            fetchInfoByBarcode(product.barcode)
        }
    }


    fun updateEditingStock(stock: Double) {
        _editingStock.value = stock
    }

    fun updateBranchStock(productId: String, branchId: String, stock: Double) {
        viewModelScope.launch {
            try {
                repository.updateStock(productId, branchId, stock, userId = _currentUser.value?.username ?: "admin", reason = "Ajuste manual desde catálogo")
            } catch (e: Exception) {
                _errorMessage.value = "Error al actualizar stock: ${e.message}"
            }
        }
    }

    private val _isFetching = MutableStateFlow(false)
    val isFetching = _isFetching.asStateFlow()

    fun fetchInfoByBarcode(barcode: String) {
        if (barcode.length < 8) return
        
        viewModelScope.launch {
            _isFetching.value = true
            _errorMessage.value = null
            try {
                val info = apiService.fetchFromOpenFoodFacts(barcode)
                if (info != null) {
                    val current = _editingProduct.value ?: return@launch
                    _editingProduct.value = current.copy(
                        name = info.product_name ?: current.name,
                        category = info.categories?.split(",")?.firstOrNull()?.trim() ?: current.category,
                        imagePath = info.image_url ?: current.imagePath
                    )
                } else {
                    _errorMessage.value = "Producto no registrado en la base de datos global."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Fallo de conexión: Verifica tu internet o permisos."
                println("VM_API_ERROR: ${e.stackTraceToString()}")
            } finally {
                _isFetching.value = false
            }
        }
    }

    fun saveProduct() {
        val product = _editingProduct.value ?: return
        if (product.name.isBlank() || product.barcode.isBlank() || product.price3 <= 0.0) return
        
        val isNew = product.id.isEmpty()
        val permission = if (isNew) com.abtsplazita.posplazita.domain.Permission.PRODUCT_CREATE else com.abtsplazita.posplazita.domain.Permission.PRODUCT_EDIT
        val level = _userPermissions.value[permission] ?: com.abtsplazita.posplazita.domain.PermissionLevel.DISABLED

        if (level == com.abtsplazita.posplazita.domain.PermissionLevel.DISABLED) {
            _errorMessage.value = "No tienes permiso para esta acción."
            return
        }

        if (level == com.abtsplazita.posplazita.domain.PermissionLevel.RESTRICTED) {
            requestAuthorization(if(isNew) "Crear Producto" else "Editar Producto") {
                executeSave(product, isNew)
            }
        } else {
            executeSave(product, isNew)
        }
    }

    private fun executeSave(product: Product, isNew: Boolean) {
        viewModelScope.launch {
            try {
                // Validar código de barras duplicado
                val exists = products.value.any { it.barcode == product.barcode && it.id != product.id }
                if (exists) {
                    _errorMessage.value = "Ya existe un producto con el código de barras: ${product.barcode}"
                    return@launch
                }

                val toSave = if (isNew) {
                    product.copy(id = "P${com.abtsplazita.posplazita.currentTimeMillis()}")
                } else product
                
                // 1. Guardar datos generales
                repository.saveProduct(toSave)
                
                // 4. Cerrar diálogo solo si todo salió bien
                _editingProduct.value = null
                _errorMessage.value = null
                
                // Si el producto que editamos es el que está en detalle, lo actualizamos
                if (_detailProduct.value?.id == toSave.id) {
                    _detailProduct.value = toSave
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error al guardar: ${e.message}"
            }
        }
    }


    fun clearError() {
        _errorMessage.value = null
    }

    fun cancelEdit() {
        _editingProduct.value = null
    }

    fun deleteProduct(product: Product) {
        val level = _userPermissions.value[com.abtsplazita.posplazita.domain.Permission.PRODUCT_DELETE] ?: com.abtsplazita.posplazita.domain.PermissionLevel.DISABLED
        if (level == com.abtsplazita.posplazita.domain.PermissionLevel.DISABLED) {
            _errorMessage.value = "No tienes permiso para eliminar productos."
            return
        }

        if (level == com.abtsplazita.posplazita.domain.PermissionLevel.RESTRICTED) {
            requestAuthorization("Eliminar Producto: ${product.name}") {
                executeDelete(product)
            }
        } else {
            executeDelete(product)
        }
    }

    private fun executeDelete(product: Product) {
        viewModelScope.launch {
            try {
                // Regla de negocio: No permitir eliminar si hay existencias positivas
                val allInv = repository.getAllInventory().first()
                val totalStock = allInv.filter { it.productId == product.id }.sumOf { it.stock }
                
                if (totalStock > 0) {
                    _errorMessage.value = "No se puede eliminar un producto con existencias positivas ($totalStock ${product.unit})."
                    return@launch
                }

                repository.deleteProduct(product)
                _detailProduct.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Error al eliminar: ${e.message}"
            }
        }
    }


    fun addCategory(name: String) {
        viewModelScope.launch {
            repository.addCategory(name)
        }
    }

    fun addTax(rate: Double) {
        viewModelScope.launch {
            repository.addTax(rate)
        }
    }

    fun prepareImport(content: String) {
        if (content.isBlank()) return
        
        try {
            // Detectar delimitador: Tab (Excel paste), Punto y coma, o Coma (CSV estándar)
            val delimiter = when {
                content.contains("\t") -> "\t"
                content.contains(";") -> ";"
                else -> ","
            }
            
            val lines = content.split("\n").filter { it.isNotBlank() }
            _importData.value = lines.map { it.split(delimiter).map { cell -> cell.trim().removeSurrounding("\"") } }
            _errorMessage.value = null
        } catch (e: Exception) {
            _errorMessage.value = "Error al procesar los datos: ${e.message}"
        }
    }

    fun executeMappedImport(mapping: Map<String, Int>) {
        val data = _importData.value
        if (data.isEmpty()) return

        viewModelScope.launch {
            _isImporting.value = true
            try {
                // Re-construir el CSV string para el manager o usar el mapping directamente
                // Dado que ProductImportManager.importWithMapping ya acepta el mapping y el csvContent...
                // Pero ProductImportManager.importWithMapping vuelve a splitear el csvContent.
                
                // Vamos a unir el data de nuevo a CSV para no cambiar el manager si no es necesario
                val csvContent = data.joinToString("\n") { it.joinToString(",") }
                
                val importer = com.abtsplazita.posplazita.domain.ProductImportManager(repository)
                val count = importer.importWithMapping(csvContent, currentBranchId, mapping)
                
                _errorMessage.value = "Importación completada: $count productos procesados."
                _importData.value = emptyList()
            } catch (e: Exception) {
                _errorMessage.value = "Error al importar: ${e.message}"
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun cancelImport() {
        _importData.value = emptyList()
    }

    fun updateStockLimits(productId: String, min: Double, max: Double) {
        viewModelScope.launch {
            repository.updateStockLimits(productId, currentBranchId, min, max)
        }
    }
}
