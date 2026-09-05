package com.abtsplazita.posplazita.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.abtsplazita.posplazita.domain.*
import com.abtsplazita.posplazita.domain.repository.ProductRepository
import com.abtsplazita.posplazita.domain.repository.BranchRepository

import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange

class InventoryViewModel(
    private val repository: ProductRepository,
    private val branchRepository: BranchRepository,
    val branchId: String
) : ViewModel() {

    private val _branches = MutableStateFlow<List<Branch>>(emptyList())
    val branches = _branches.asStateFlow()

    private val _inventoryData = MutableStateFlow<List<ProductInventory>>(emptyList())
    val inventoryData: StateFlow<List<ProductInventory>> = combine(
        _inventoryData,
        repository.getAllInventory(),
        branchRepository.getAllBranches()
    ) { currentData, allInventoryItems, allBranches ->
        if (currentData.isEmpty() || allBranches.isEmpty()) return@combine emptyList<ProductInventory>()
        
        // Optimización: Mapa de acceso rápido O(1)
        val stockMap = allInventoryItems.associateBy { it.productId + "_" + it.branchId }
        
        currentData.map { item ->
            val stocks = allBranches.associate { b ->
                b.id to (stockMap[item.product.id + "_" + b.id]?.stock ?: 0.0)
            }
            item.copy(branchStocks = stocks)
        }
    }.flowOn(Dispatchers.Default)
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)

    fun setUserInfo(user: User?) {
        _currentUser.value = user
    }

    private var currentOffset = 0
    private val limit = 30
    private var hasMore = true

    // --- Ajuste de Inventario ---
    private val _capturedItems = MutableStateFlow<List<CapturedAdjustment>>(emptyList())
    val capturedItems = _capturedItems.asStateFlow()

    private val _searchQuery = MutableStateFlow(TextFieldValue(""))
    val searchQuery = _searchQuery.asStateFlow()

    private val _showQuickCreate = MutableStateFlow<String?>(null) // barcode to create
    val showQuickCreate = _showQuickCreate.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            // Cargar sucursales
            branchRepository.getAllBranches().first().let { 
                _branches.value = it
            }
            
            // Cargar primeros productos
            loadMore()
        }
    }

    private var loadJob: kotlinx.coroutines.Job? = null

    fun loadMore() {
        if (!hasMore) return
        val currentJob = loadJob
        if (currentJob != null && currentJob.isActive) return

        loadJob = viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Cargar stock desde la nube para esta página bajo demanda
                repository.refreshInventoryPaged(branchId, limit, currentOffset)
                
                val queryText = _searchQuery.value.text
                val products = if (queryText.isBlank()) {
                    repository.getProductsPaginated(limit, currentOffset).first()
                } else {
                    repository.searchProducts(queryText, limit, currentOffset).first()
                }
                
                if (products.isEmpty() || products.size < limit) hasMore = false
                
                val allStocks = repository.getAllInventory().first()
                
                val newData = products.map { product ->
                    val branchStocks = _branches.value.associate { branch ->
                        branch.id to (allStocks.find { it.productId == product.id && it.branchId == branch.id }?.stock ?: 0.0)
                    }
                    ProductInventory(product, branchStocks)
                }
                
                _inventoryData.value += newData
                currentOffset += limit
            } catch (e: Exception) {
                println("INVENTORY_LOAD_ERROR: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // --- Lógica de Ajuste ---

    fun updateSearchQuery(query: TextFieldValue) {
        val oldText = _searchQuery.value.text
        _searchQuery.value = query
        
        if (query.text != oldText) {
            refreshMainList()
        }
    }

    fun onSearchSubmit() {
        val query = _searchQuery.value.text
        if (query.isBlank()) return
        
        viewModelScope.launch {
            val product = repository.getProductByBarcode(query)
            if (product != null) {
                addCapturedItem(product)
                // Marcar en azul (seleccionar todo) para el siguiente escaneo
                _searchQuery.value = _searchQuery.value.copy(
                    selection = TextRange(0, query.length)
                )
            } else {
                // Producto no encontrado, disparar creación rápida
                _showQuickCreate.value = query
            }
        }
    }

    fun quickCreateAndAdd(barcode: String, name: String, price: Double, imagePath: String? = null) {
        viewModelScope.launch {
            val product = Product(
                id = "P${com.abtsplazita.posplazita.currentTimeMillis()}",
                name = name,
                barcode = barcode,
                price2 = price, // Default Público
                price1 = price * 0.9, // Sugerencia Mayoreo
                price3 = price + 0.50, // Adicional
                price4 = 0.0,
                unit = UnitType.PIECE,
                imagePath = imagePath
            )
            try {
                repository.saveProduct(product)
                addCapturedItem(product)
                _showQuickCreate.value = null
                _searchQuery.value = TextFieldValue("")
            } catch (e: Exception) {
                // TODO: Error handling if needed
            }
        }
    }

    fun cancelQuickCreate() {
        _showQuickCreate.value = null
    }

    private fun addCapturedItem(product: Product) {
        val currentList = _capturedItems.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.product.id == product.id }
        
        if (existingIndex != -1) {
            val existing = currentList.removeAt(existingIndex)
            currentList.add(0, existing.copy(count = existing.count + 1))
        } else {
            currentList.add(0, CapturedAdjustment(product, 1.0))
        }
        _capturedItems.value = currentList
    }

    fun updateCapturedQuantity(productId: String, quantity: Double) {
        val currentList = _capturedItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == productId }
        if (index != -1) {
            currentList[index] = currentList[index].copy(count = quantity)
            _capturedItems.value = currentList
        }
    }

    fun removeCapturedItem(productId: String) {
        _capturedItems.value = _capturedItems.value.filter { it.product.id != productId }
    }

    fun finalizeAdjustment() {
        viewModelScope.launch {
            _capturedItems.value.forEach { item ->
                // Actualizar stock a la cantidad contada (item.count)
                repository.updateStock(
                    productId = item.product.id,
                    branchId = branchId,
                    newStock = item.count,
                    userId = _currentUser.value?.username ?: "admin",
                    reason = "Ajuste de inventario físico"
                )
            }
            _capturedItems.value = emptyList()
            // Recargar vista principal
            refreshMainList()
        }
    }

    private fun refreshMainList() {
        loadJob?.cancel()
        loadJob = null
        currentOffset = 0
        hasMore = true
        _inventoryData.value = emptyList()
        loadMore()
    }

    fun updateQuickStock(productId: String, newStock: Double) {
        viewModelScope.launch {
            repository.updateStock(productId, branchId, newStock, userId = _currentUser.value?.username ?: "admin", reason = "Ajuste rápido desde lista")
            // Actualizar solo ese item en la lista local para no recargar todo
            val newList = _inventoryData.value.map { item ->
                if (item.product.id == productId) {
                    val newStocks = item.branchStocks.toMutableMap().apply { put(branchId, newStock) }
                    item.copy(branchStocks = newStocks)
                } else item
            }
            _inventoryData.value = newList
        }
    }
}

data class ProductInventory(
    val product: Product,
    val branchStocks: Map<String, Double> // branchId -> stock
)

data class CapturedAdjustment(
    val product: Product,
    val count: Double
)
