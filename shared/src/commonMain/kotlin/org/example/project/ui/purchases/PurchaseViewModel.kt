package com.abtsplazita.posplazita.ui.purchases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.abtsplazita.posplazita.domain.*
import com.abtsplazita.posplazita.domain.repository.ProductRepository
import com.abtsplazita.posplazita.domain.repository.PurchaseRepository
import com.abtsplazita.posplazita.domain.repository.SupplierRepository
import com.abtsplazita.posplazita.domain.repository.CashMovementRepository
import com.abtsplazita.posplazita.currentTimeMillis
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange

class PurchaseViewModel(
    private val productRepository: ProductRepository,
    private val purchaseRepository: PurchaseRepository,
    val supplierRepository: SupplierRepository? = null,
    private val cashMovementRepository: CashMovementRepository? = null,
    private val unitRepository: com.abtsplazita.posplazita.domain.repository.PurchaseUnitRepository? = null,
    private val branchId: String
) : ViewModel() {

    private val _cartItems = MutableStateFlow<List<PurchaseItem>>(emptyList())
    val cartItems = _cartItems.asStateFlow()

    private val _total = MutableStateFlow(0.0)
    val total = _total.asStateFlow()

    private val _selectedSupplier = MutableStateFlow<Supplier?>(null)
    val selectedSupplier = _selectedSupplier.asStateFlow()

    val availableSuppliers = supplierRepository?.getAllSuppliers() ?: flowOf(emptyList())

    private val _searchQuery = MutableStateFlow(TextFieldValue(""))
    val searchQuery = _searchQuery.asStateFlow()

    private val _showSearchResults = MutableStateFlow(false)
    val showSearchResults = _showSearchResults.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Product>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _selectedSearchIndex = MutableStateFlow(0)
    val selectedSearchIndex = _selectedSearchIndex.asStateFlow()

    private val _lastSearchedBarcode = MutableStateFlow("")
    val lastSearchedBarcode = _lastSearchedBarcode.asStateFlow()

    init {
        viewModelScope.launch {
            refreshData()
        }
    }

    private suspend fun refreshData() {
        try {
            purchaseRepository.refreshPurchases(branchId)
            supplierRepository?.refreshSuppliers()
        } catch (e: Exception) {}
    }

    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct = _selectedProduct.asStateFlow()

    private val _quantityText = MutableStateFlow(TextFieldValue("1", TextRange(0, 1)))
    val quantityText = _quantityText.asStateFlow()

    private val _costText = MutableStateFlow(TextFieldValue("0"))
    val costText = _costText.asStateFlow()

    // --- Advanced Purchase Fields ---
    private val _purchaseUnit = MutableStateFlow<PurchaseUnit?>(null)
    val purchaseUnit = _purchaseUnit.asStateFlow()

    private val _purchaseQuantityText = MutableStateFlow(TextFieldValue("1"))
    val purchaseQuantityText = _purchaseQuantityText.asStateFlow()

    private val _purchaseCostText = MutableStateFlow(TextFieldValue("0"))
    val purchaseCostText = _purchaseCostText.asStateFlow()

    private val _purchaseFinalCostText = MutableStateFlow(TextFieldValue("0"))
    val purchaseFinalCostText = _purchaseFinalCostText.asStateFlow()

    private val _purchaseDiscountAmountText = MutableStateFlow(TextFieldValue("0"))
    val purchaseDiscountAmountText = _purchaseDiscountAmountText.asStateFlow()

    private val _purchaseFactorText = MutableStateFlow(TextFieldValue("1"))
    val purchaseFactorText = _purchaseFactorText.asStateFlow()

    private val _discountPercentText = MutableStateFlow(TextFieldValue("0"))
    val discountPercentText = _discountPercentText.asStateFlow()

    private val _taxRateText = MutableStateFlow(TextFieldValue("0"))
    val taxRateText = _taxRateText.asStateFlow()

    val availableUnits = unitRepository?.getAllUnits() ?: flowOf(emptyList())

    private val _showUnitDialog = MutableStateFlow(false)
    val showUnitDialog = _showUnitDialog.asStateFlow()

    fun openUnitDialog() { _showUnitDialog.value = true }
    fun closeUnitDialog() { _showUnitDialog.value = false }

    fun saveUnit(unit: PurchaseUnit) {
        viewModelScope.launch {
            unitRepository?.saveUnit(unit)
        }
    }

    fun deleteUnit(unit: PurchaseUnit) {
        viewModelScope.launch {
            unitRepository?.deleteUnit(unit)
        }
    }

    fun quickCreateUnit(name: String, factor: Double) {
        viewModelScope.launch {
            val unit = PurchaseUnit(
                id = "${name}-${factor}",
                name = name,
                factor = factor
            )
            unitRepository?.saveUnit(unit)
        }
    }

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()

    fun setUserInfo(user: User?) {
        _currentUser.value = user
    }

    fun updateSearchQuery(query: TextFieldValue) {
        _searchQuery.value = query
    }

    fun onSearchSubmit() {
        val query = _searchQuery.value.text
        if (query.isBlank()) return

        viewModelScope.launch {
            _lastSearchedBarcode.value = query
            val exactMatch = productRepository.getProductByBarcode(query)
            if (exactMatch != null) {
                selectProduct(exactMatch)
                selectSearchQuery()
            } else {
                productRepository.searchProducts(query).first().let { results ->
                    _searchResults.value = results
                    _selectedSearchIndex.value = 0
                    _showSearchResults.value = true
                }
            }
        }
    }

    fun selectSearchQuery() {
        val currentText = _searchQuery.value.text
        _searchQuery.value = _searchQuery.value.copy(
            selection = TextRange(0, currentText.length)
        )
    }

    fun onSearchQueryClear() {
        _searchQuery.value = TextFieldValue("")
        _showSearchResults.value = false
        _selectedProduct.value = null
    }

    fun moveFocus(delta: Int) {
        if (_showSearchResults.value && searchResults.value.isNotEmpty()) {
            val next = (_selectedSearchIndex.value + delta).coerceIn(0, searchResults.value.size - 1)
            _selectedSearchIndex.value = next
        }
    }

    fun selectCurrentSearchItem() {
        if (_showSearchResults.value && searchResults.value.isNotEmpty()) {
            val product = searchResults.value[_selectedSearchIndex.value]
            selectProduct(product)
            _showSearchResults.value = false
            selectSearchQuery()
        }
    }

    fun selectProduct(product: Product) {
        _selectedProduct.value = product
        _quantityText.value = TextFieldValue("1", TextRange(0, 1))
        
        val initialFinalCost = product.cost * (1 + product.tax / 100)
        _costText.value = TextFieldValue(initialFinalCost.formatPrice())
        
        // --- LOAD MEMORY FROM PRODUCT ---
        val lastTax = product.lastPurchaseTax
        val lastUnitName = product.lastPurchaseUnit
        val lastFactor = product.lastPurchaseFactor
        val lastCost = product.lastPurchaseCost // This is the price per UNIT of PURCHASE (e.g. CAJA)
        
        _purchaseQuantityText.value = TextFieldValue("1")
        
        // Use the remembered cost if available, otherwise use product cost
        val initialBaseCost = if (lastCost > 0) lastCost else product.cost
        _purchaseCostText.value = TextFieldValue(initialBaseCost.formatPrice())
        
        // Calculate initial final cost with the remembered tax
        val effectiveTax = if (lastTax > 0) lastTax else product.tax
        val currentFinalCost = initialBaseCost * (1 + effectiveTax / 100)
        _purchaseFinalCostText.value = TextFieldValue(currentFinalCost.formatPrice())
        
        _purchaseDiscountAmountText.value = TextFieldValue("0")
        _purchaseFactorText.value = TextFieldValue(lastFactor.toString())
        _discountPercentText.value = TextFieldValue("0")
        _taxRateText.value = TextFieldValue(effectiveTax.toString())
        
        // Try to restore the purchase unit
        viewModelScope.launch {
            if (lastUnitName != null) {
                availableUnits.first().find { it.name == lastUnitName }?.let {
                    _purchaseUnit.value = it
                }
            } else {
                _purchaseUnit.value = null
            }
        }

        _searchResults.value = emptyList()
        _showSearchResults.value = false
        _searchQuery.value = TextFieldValue("")
    }

    fun selectSupplier(supplier: Supplier?) {
        _selectedSupplier.value = supplier
    }

    fun updateQuantity(value: TextFieldValue) {
        if (value.text.isEmpty() || value.text.all { it.isDigit() || it == '.' }) {
            _quantityText.value = value
        }
    }

    fun updateCost(value: TextFieldValue) {
        if (value.text.isEmpty() || value.text.all { it.isDigit() || it == '.' }) {
            _costText.value = value
        }
    }

    fun updatePurchaseUnit(unit: PurchaseUnit?) {
        _purchaseUnit.value = unit
        if (unit != null) {
            _purchaseFactorText.value = TextFieldValue(unit.factor.toString())
        }
    }

    fun updatePurchaseQuantity(value: TextFieldValue) {
        if (value.text.isEmpty() || value.text.all { it.isDigit() || it == '.' }) {
            _purchaseQuantityText.value = value
        }
    }

    fun updatePurchaseCost(value: TextFieldValue) {
        if (value.text.isEmpty() || value.text.all { it.isDigit() || it == '.' }) {
            _purchaseCostText.value = value
            val taxRate = _taxRateText.value.text.toDoubleOrNull() ?: 0.0
            val base = value.text.toDoubleOrNull() ?: 0.0
            val final = base * (1 + taxRate / 100)
            _purchaseFinalCostText.value = _purchaseFinalCostText.value.copy(text = final.formatPrice())
        }
    }

    fun updatePurchaseFinalCost(value: TextFieldValue) {
        if (value.text.isEmpty() || value.text.all { it.isDigit() || it == '.' }) {
            _purchaseFinalCostText.value = value
            val taxRate = _taxRateText.value.text.toDoubleOrNull() ?: 0.0
            val final = value.text.toDoubleOrNull() ?: 0.0
            val base = final / (1 + taxRate / 100)
            _purchaseCostText.value = _purchaseCostText.value.copy(text = base.formatPrice())
        }
    }

    fun updatePurchaseDiscountAmount(value: TextFieldValue) {
        if (value.text.isEmpty() || value.text.all { it.isDigit() || it == '.' }) {
            _purchaseDiscountAmountText.value = value
        }
    }

    fun updatePurchaseFactor(value: TextFieldValue) {
        if (value.text.isEmpty() || value.text.all { it.isDigit() || it == '.' }) {
            _purchaseFactorText.value = value
        }
    }

    fun updateDiscountPercent(value: TextFieldValue) {
        if (value.text.isEmpty() || value.text.all { it.isDigit() || it == '.' }) {
            _discountPercentText.value = value
        }
    }

    fun updateTaxRate(value: TextFieldValue) {
        if (value.text.isEmpty() || value.text.all { it.isDigit() || it == '.' }) {
            _taxRateText.value = value
            val taxRate = value.text.toDoubleOrNull() ?: 0.0
            val base = _purchaseCostText.value.text.toDoubleOrNull() ?: 0.0
            val final = base * (1 + taxRate / 100)
            _purchaseFinalCostText.value = _purchaseFinalCostText.value.copy(text = final.formatPrice())
        }
    }

    fun addToCart(advanced: Boolean = false) {
        val product = _selectedProduct.value ?: return
        
        if (advanced) {
            val pQty = _purchaseQuantityText.value.text.toDoubleOrNull() ?: 0.0
            val pCost = _purchaseCostText.value.text.toDoubleOrNull() ?: 0.0
            val pFactor = _purchaseFactorText.value.text.toDoubleOrNull() ?: 1.0
            val pDiscountPercent = _discountPercentText.value.text.toDoubleOrNull() ?: 0.0
            val pDiscountAmount = _purchaseDiscountAmountText.value.text.toDoubleOrNull() ?: 0.0
            val pTax = _taxRateText.value.text.toDoubleOrNull() ?: 0.0
            
            if (pQty > 0) {
                val baseQuantity = pQty * pFactor
                // Importe sin impuesto = (Cant * Costo) - Desc$ - (Cant * Costo * Desc%)
                val totalCost = pQty * pCost
                val discountFromPercent = totalCost * (pDiscountPercent / 100)
                val subtotalWithoutTax = totalCost - pDiscountAmount - discountFromPercent
                
                val baseUnitCost = if (baseQuantity > 0) subtotalWithoutTax / baseQuantity else 0.0
                
                val items = _cartItems.value.toMutableList()
                val existingIndex = items.indexOfFirst { it.productId == product.id }
                
                val newItem = PurchaseItem(
                    productId = product.id,
                    productName = product.name,
                    quantity = baseQuantity,
                    costAtPurchase = baseUnitCost,
                    subtotal = subtotalWithoutTax,
                    purchaseUnit = _purchaseUnit.value?.name ?: "PZA",
                    purchaseFactor = pFactor,
                    purchaseQuantity = pQty,
                    purchaseCost = pCost,
                    discountPercent = pDiscountPercent,
                    taxRate = pTax
                )

                if (existingIndex != -1) {
                    items[existingIndex] = newItem
                } else {
                    items.add(newItem)
                }
                
                _cartItems.value = items
                _selectedProduct.value = null
                updateTotal()
            }
        } else {
            val qty = _quantityText.value.text.toDoubleOrNull() ?: 0.0
            val enteredFinalCost = _costText.value.text.toDoubleOrNull() ?: 0.0
            
            if (qty > 0) {
                val taxRate = product.tax
                val baseCost = enteredFinalCost / (1 + taxRate / 100)
                
                val items = _cartItems.value.toMutableList()
                val existingIndex = items.indexOfFirst { it.productId == product.id }
                
                val newItem = PurchaseItem(
                    productId = product.id,
                    productName = product.name,
                    quantity = qty,
                    costAtPurchase = baseCost,
                    subtotal = qty * enteredFinalCost,
                    taxRate = taxRate
                )

                if (existingIndex != -1) {
                    items[existingIndex] = newItem
                } else {
                    items.add(newItem)
                }
                
                _cartItems.value = items
                _selectedProduct.value = null
                updateTotal()
            }
        }
    }

    private fun updateTotal() {
        _total.value = _cartItems.value.sumOf { it.subtotal }
    }

    fun removeItem(item: PurchaseItem) {
        _cartItems.value -= item
        updateTotal()
    }

    fun editCartItem(item: PurchaseItem) {
        viewModelScope.launch {
            val product = productRepository.getProductById(item.productId) ?: return@launch
            _selectedProduct.value = product
            
            // Cargar datos en modo Simple
            _quantityText.value = TextFieldValue(item.quantity.toString())
            _costText.value = TextFieldValue((item.costAtPurchase * (1 + item.taxRate / 100)).formatPrice())

            // Cargar datos en modo Avanzado
            _purchaseQuantityText.value = TextFieldValue(item.purchaseQuantity.toString())
            _purchaseCostText.value = TextFieldValue(item.purchaseCost.formatPrice())
            _purchaseFactorText.value = TextFieldValue(item.purchaseFactor.toString())
            _discountPercentText.value = TextFieldValue(item.discountPercent.toString())
            _taxRateText.value = TextFieldValue(item.taxRate.toString())
            
            val finalCost = item.purchaseCost * (1 + item.taxRate / 100)
            _purchaseFinalCostText.value = TextFieldValue(finalCost.formatPrice())

            availableUnits.first().find { it.name == item.purchaseUnit }?.let {
                _purchaseUnit.value = it
            } ?: run {
                _purchaseUnit.value = null
            }
        }
    }

    fun updateProductMasterData(name: String, barcode: String) {
        val product = _selectedProduct.value ?: return
        viewModelScope.launch {
            val updatedProduct = product.copy(name = name, barcode = barcode)
            productRepository.saveProduct(updatedProduct)
            _selectedProduct.value = updatedProduct
            
            // Actualizar nombre en el carrito si ya existe
            val items = _cartItems.value.toMutableList()
            val index = items.indexOfFirst { it.productId == product.id }
            if (index != -1) {
                items[index] = items[index].copy(productName = name)
                _cartItems.value = items
            }
        }
    }

    fun savePurchase(method: String, onSuccess: () -> Unit) {
        if (_cartItems.value.isEmpty()) return
        
        if (_selectedSupplier.value == null) {
            _errorMessage.value = "Atención: Debe seleccionar un Proveedor antes de guardar la compra."
            return
        }

        viewModelScope.launch {

            _isSaving.value = true
            try {
                val nextId = purchaseRepository.getNextPurchaseId()
                val now = currentTimeMillis()
                val currentTotal = _total.value
                val purchase = Purchase(
                    id = "C${nextId.padStart(4, '0')}",
                    timestamp = now,
                    userId = _currentUser.value?.username ?: "admin", 
                    branchId = branchId,
                    supplierId = _selectedSupplier.value?.id,
                    items = _cartItems.value,
                    total = currentTotal,
                    paymentMethod = method,
                    status = PurchaseStatus.PENDING_PRICE_UPDATE
                )
                purchaseRepository.savePurchase(purchase)
                
                // --- ACTUALIZAR PRODUCTOS (IVA, COSTO Y MEMORIA) ---
                _cartItems.value.forEach { item ->
                    val product = productRepository.getProductById(item.productId)
                    if (product != null) {
                        val updatedProduct = product.copy(
                            cost = item.costAtPurchase,
                            tax = item.taxRate,
                            lastPurchaseUnit = item.purchaseUnit,
                            lastPurchaseFactor = item.purchaseFactor,
                            lastPurchaseTax = item.taxRate,
                            lastPurchaseCost = item.purchaseCost,
                            lastUpdated = now
                        )
                        productRepository.saveProduct(updatedProduct)
                    }
                }
                
                // Si fue en EFECTIVO (FONDO), registrar salida de caja
                if (method == "Efectivo (Fondo)") {
                    cashMovementRepository?.saveMovement(
                        CashMovement(
                            id = "M_PUR_${purchase.id}",
                            timestamp = now,
                            branchId = branchId,
                            terminalId = null, // Salida general o ligar a una?
                            type = CashMovementType.OUT,
                            amount = currentTotal,
                            reason = "Pago de compra: ${purchase.id}",
                            userId = _currentUser.value?.username ?: "admin"
                        )
                    )
                }

                // Si fue a CRÉDITO, aumentar deuda del proveedor
                if (method == "Crédito" && _selectedSupplier.value != null) {
                    supplierRepository?.updateDebt(_selectedSupplier.value!!.id, currentTotal)
                }

                // Ligar productos al proveedor (historial de costos)
                _selectedSupplier.value?.let { supplier ->
                    _cartItems.value.forEach { item ->
                        supplierRepository?.saveProductSupplierLink(
                            ProductSupplier(
                                productId = item.productId,
                                supplierId = supplier.id,
                                lastCost = item.costAtPurchase,
                                lastPurchaseDate = now
                            )
                        )
                    }
                }

                _cartItems.value = emptyList()
                _total.value = 0.0
                _selectedSupplier.value = null
                onSuccess()
            } catch (e: Exception) {
                _errorMessage.value = "Error al guardar compra: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun clearError() { _errorMessage.value = null }
    fun setErrorMessage(msg: String) { _errorMessage.value = msg }

    fun quickCreateSupplier(supplier: Supplier) {
        viewModelScope.launch {
            try {
                supplierRepository?.saveSupplier(supplier)
                selectSupplier(supplier)
            } catch (e: Exception) {
                _errorMessage.value = "Error al crear proveedor: ${e.message}"
            }
        }
    }

    fun quickCreateProduct(barcode: String, name: String, price: Double, imagePath: String? = null) {
        viewModelScope.launch {
            val product = Product(
                id = "P${currentTimeMillis()}",
                name = name,
                barcode = barcode,
                price2 = price, // Precio Público por defecto (P2 - Nuevo Default)
                imagePath = imagePath,
                price1 = price * 0.9,
                price3 = price + 0.50,
                price4 = 0.0,
                unit = UnitType.PIECE
            )
            try {
                productRepository.saveProduct(product)
                // Una vez creado, lo seleccionamos para capturar cantidad/costo de la compra
                selectProduct(product)
            } catch (e: Exception) {
                _errorMessage.value = "Error al crear producto: ${e.message}"
            }
        }
    }
}
