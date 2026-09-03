package com.abtsplazita.posplazita.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer

class CurrentSaleManager(
    private val settingsRepository: com.abtsplazita.posplazita.domain.repository.SettingsRepository? = null,
    private val scope: kotlinx.coroutines.CoroutineScope? = null
) {
    private val _currentItems = MutableStateFlow<List<SaleItem>>(emptyList())
    val currentItems: StateFlow<List<SaleItem>> = _currentItems.asStateFlow()

    private val _total = MutableStateFlow(0.0)
    val total: StateFlow<Double> = _total.asStateFlow()

    private val _itemCount = MutableStateFlow(0)
    val itemCount: StateFlow<Int> = _itemCount.asStateFlow()

    private var groupItems = true
    private var allowNegativeStock = false
    private var defaultPriceLevel = 2
    private var addAtTop = false
    private var isWholesaleEnabled = false
    private var activePromotions: List<Promotion> = emptyList()
    private var branchId: String = ""

    private var _currentWebOrderId = MutableStateFlow<String?>(null)
    val currentWebOrderId = _currentWebOrderId.asStateFlow()

    fun setWebOrderId(id: String?) {
        _currentWebOrderId.value = id
    }

    fun setBranchId(id: String) {
        branchId = id
        restoreState()
    }

    private fun saveState() {
        if (branchId.isBlank() || settingsRepository == null || scope == null) return
        scope.launch {
            try {
                val itemsJson = kotlinx.serialization.json.Json.encodeToString(
                    kotlinx.serialization.builtins.ListSerializer(SaleItem.serializer()), 
                    _currentItems.value
                )
                settingsRepository.saveSetting("recovery_cart_$branchId", itemsJson)
                
                // Persistir ID de pedido web si existe
                settingsRepository.saveSetting("recovery_web_id_$branchId", _currentWebOrderId.value ?: "")
            } catch (e: Exception) {}
        }
    }

    private fun restoreState() {
        if (branchId.isBlank() || settingsRepository == null || scope == null) return
        scope.launch {
            try {
                val savedItems = settingsRepository.getSetting("recovery_cart_$branchId")
                if (!savedItems.isNullOrBlank()) {
                    val restored = kotlinx.serialization.json.Json.decodeFromString(
                        kotlinx.serialization.builtins.ListSerializer(SaleItem.serializer()), 
                        savedItems
                    )
                    _currentItems.value = restored
                    updateTotal()
                } else {
                    _currentItems.value = emptyList()
                    _total.value = 0.0
                }

                // Restaurar ID de pedido web
                val savedWebId = settingsRepository.getSetting("recovery_web_id_$branchId")
                _currentWebOrderId.value = if (savedWebId.isNullOrBlank()) null else savedWebId
            } catch (e: Exception) {}
        }
    }

    fun setPromotions(promotions: List<Promotion>) {
        activePromotions = promotions
        // Re-calcular total al cambiar promos
        updateTotal()
    }

    fun setGrouping(enabled: Boolean) {
        groupItems = enabled
    }

    fun setAllowNegativeStock(enabled: Boolean) {
        allowNegativeStock = enabled
    }

    fun setDefaultPriceLevel(level: Int) {
        defaultPriceLevel = level
    }

    fun setAddAtTop(enabled: Boolean) {
        addAtTop = enabled
    }

    fun setWholesaleEnabled(enabled: Boolean) {
        isWholesaleEnabled = enabled
        updateTotal()
    }

    fun addItem(product: Product, branchId: String, currentStock: Double, quantity: Double? = null, isWebDiscounted: Boolean = false, isReturn: Boolean = false): Boolean {
        val qty = quantity ?: 1.0
        if (!isReturn && qty <= 0) return false

        // Validar si el producto permite decimales (Granel)
        if (!product.isBulk && (qty % 1.0 != 0.0)) {
            return false 
        }

        // Validar stock (Solo si no es devolución)
        if (!isReturn && !product.isService && !allowNegativeStock && !isWebDiscounted) {
            val currentInCart = _currentItems.value.filter { it.productId == product.id }.sumOf { it.quantity }
            if (currentInCart + qty > currentStock) return false
        }

        val items = _currentItems.value.toMutableList()
        val basePrice = when(defaultPriceLevel) {
            1 -> product.price1
            2 -> product.price2
            else -> product.price3
        }

        // Aplicar Promociones Simples (Precio Fijo o Categoría %)
        var finalPrice = basePrice
        var promoApplied = false
        val now = com.abtsplazita.posplazita.currentTimeMillis()
        
        val promo = if (isReturn) null else activePromotions.find { 
            it.isActive && now >= it.startDate && now <= it.endDate &&
            ((it.type == PromotionType.FIXED_PRICE && it.productId == product.id) ||
             (it.type == PromotionType.CATEGORY_PERCENT && it.category == product.category))
        }

        if (promo != null) {
            finalPrice = if (promo.type == PromotionType.FIXED_PRICE) promo.discountValue 
                        else basePrice * (1.0 - (promo.discountValue / 100.0))
            promoApplied = true
        }

        val mainPrice = finalPrice.roundToNearestHalf()
        val finalQty = if (isReturn) -kotlin.math.abs(qty) else qty
        
        if (groupItems) {
            val existingIndex = items.indexOfFirst { it.productId == product.id && it.priceAtSale == mainPrice && it.isWebDiscounted == isWebDiscounted && (it.quantity < 0) == isReturn }
            if (existingIndex != -1) {
                val existingItem = items[existingIndex]
                val updatedQty = existingItem.quantity + finalQty
                items[existingIndex] = existingItem.copy(quantity = updatedQty)
                _currentItems.value = items
                updateTotal()
                saveState()
                return true
            }
        }

        val newItem = SaleItem(
            productId = product.id,
            productName = if (isReturn) "[DEVOLUCIÓN] ${product.name}" else product.name,
            productImagePath = product.imagePath,
            quantity = finalQty,
            priceAtSale = mainPrice,
            subtotal = 0.0, // Se calcula en updateTotal
            category = product.category,
            isService = product.isService,
            isBulk = product.isBulk,
            isWebDiscounted = isWebDiscounted,
            price1 = product.price1,
            price2 = product.price2,
            price3 = product.price3,
            isPromoApplied = promoApplied
        )

        if (addAtTop) _currentItems.value = listOf(newItem) + _currentItems.value
        else _currentItems.value += newItem
        
        updateTotal()
        saveState()
        return true
    }

    fun removeItem(item: SaleItem) {
        _currentItems.value -= item
        updateTotal()
        saveState()
    }

    fun updateItemQuantityAt(index: Int, newQuantity: Double) {
        if (newQuantity <= 0) return
        val items = _currentItems.value.toMutableList()
        if (index in items.indices) {
            val existingItem = items[index]
            
            // Validar granel
            if (!existingItem.isBulk && (newQuantity % 1.0 != 0.0)) return

            val rawSubtotal = newQuantity * existingItem.priceAtSale
            items[index] = existingItem.copy(
                quantity = newQuantity,
                subtotal = rawSubtotal.roundToNearestHalf()
            )
            _currentItems.value = items
            updateTotal()
            saveState()
        }
    }

    fun updateItemQuantity(item: SaleItem, newQuantity: Double) {
        if (newQuantity <= 0) return
        val items = _currentItems.value.toMutableList()
        val index = items.indexOf(item)
        if (index != -1) {
            updateItemQuantityAt(index, newQuantity)
        }
    }

    private fun updateTotal() {
        val now = com.abtsplazita.posplazita.currentTimeMillis()
        val itemsWithPromos = _currentItems.value.map { item ->
            var currentPrice = item.priceAtSale
            var promoApplied = item.isPromoApplied
            
            var subtotal = item.quantity * currentPrice
            
            val bulkPromo = activePromotions.find { 
                it.isActive && now >= it.startDate && now <= it.endDate &&
                it.type == PromotionType.BULK_OFFER && it.productId == item.productId &&
                item.quantity >= it.triggerQuantity
            }

            if (bulkPromo != null) {
                val numPackages = (item.quantity / bulkPromo.triggerQuantity).toInt()
                val remainder = item.quantity % bulkPromo.triggerQuantity
                subtotal = (numPackages * bulkPromo.discountValue) + (remainder * currentPrice)
                promoApplied = true
            }

            item.copy(priceAtSale = currentPrice, subtotal = subtotal.roundToNearestHalf(), isPromoApplied = promoApplied)
        }
        
        var rawTotal = itemsWithPromos.sumOf { it.subtotal }
        
        // Aplicar Promociones por Monto Total (Escalonadas)
        // Buscamos la promo de este tipo con el valor de activación (triggerQuantity usado aquí como monto mínimo) más alto que se cumpla
        val totalPromo = activePromotions
            .filter { 
                it.isActive && now >= it.startDate && now <= it.endDate &&
                it.type == PromotionType.TOTAL_AMOUNT_PERCENT &&
                rawTotal >= it.triggerQuantity // triggerQuantity actúa como monto mínimo para este tipo
            }
            .maxByOrNull { it.triggerQuantity }

        if (totalPromo != null) {
            rawTotal *= (1.0 - (totalPromo.discountValue / 100.0))
        }
        
        _currentItems.value = itemsWithPromos
        _total.value = rawTotal.roundToNearestHalf()
        _itemCount.value = itemsWithPromos.size
    }

    fun clear() {
        _currentItems.value = emptyList()
        _total.value = 0.0
        _itemCount.value = 0
        _currentWebOrderId.value = null
        saveState()
    }

    /**
     * Carga una lista completa de artículos al carrito (usado para recuperar ventas en espera).
     * Ignora validaciones de stock ya que se asume que son artículos ya reservados.
     */
    fun loadItems(items: List<SaleItem>) {
        _currentItems.value = items
        updateTotal()
    }
}
