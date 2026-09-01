package com.abtsplazita.posplazita.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.abtsplazita.posplazita.domain.*
import com.abtsplazita.posplazita.domain.repository.ProductRepository
import com.abtsplazita.posplazita.domain.repository.SaleRepository
import com.abtsplazita.posplazita.currentTimeMillis

class RestockViewModel(
    private val productRepository: ProductRepository,
    private val saleRepository: SaleRepository,
    private val movementRepository: com.abtsplazita.posplazita.domain.repository.StockMovementRepository,
    private val branchId: String
) : ViewModel() {

    private val _analysisDays = MutableStateFlow(30)
    val analysisDays = _analysisDays.asStateFlow()

    private val _leadTimeDays = MutableStateFlow(7)
    val leadTimeDays = _leadTimeDays.asStateFlow()

    private val _isUpdating = MutableStateFlow(false)
    val isUpdating = _isUpdating.asStateFlow()

    val restockSuggestions = combine(
        productRepository.getProducts(),
        productRepository.getAllInventory(),
        saleRepository.getSalesWithItems(branchId),
        movementRepository.getMovementsByBranch(branchId),
        _analysisDays,
        _leadTimeDays
    ) { args: Array<Any> ->
        val products = args[0] as List<Product>
        val allInventory = args[1] as List<Inventory>
        val sales = args[2] as List<Sale>
        val movements = args[3] as List<StockMovement>
        val days = args[4] as Int
        val leadTime = args[5] as Int

        val now = currentTimeMillis()
        val startOfPeriod = now - (days.toLong() * 24 * 60 * 60 * 1000L)
        
        val filteredSales = sales.filter { it.timestamp >= startOfPeriod }
        val branchInventory = allInventory.filter { it.branchId == branchId }
        val branchRestocks = movements.filter { it.type == MovementType.IN_PURCHASE }
        
        products.filter { !it.isService }.map { product ->
            val stock = branchInventory.find { it.productId == product.id }?.stock ?: 0.0
            
            // 1. Calcular Venta Diaria Promedio (VDP)
            val productSales = filteredSales.flatMap { it.items }.filter { it.productId == product.id }
            val totalSold = productSales.sumOf { it.quantity }
            val vdp = if (days > 0) totalSold / days.toDouble() else 0.0
            
            // 2. Encontrar último resurtimiento o movimiento de entrada
            val lastRestock = branchRestocks
                .filter { it.productId == product.id }
                .maxByOrNull { it.timestamp }
            
            // 3. Encontrar última venta
            val lastSale = filteredSales.filter { s -> s.items.any { it.productId == product.id } }
                .maxByOrNull { it.timestamp }?.timestamp

            // 4. Detectar si el producto está "olvidado" (Mas de un mes sin entradas ni salidas)
            val lastActivity = maxOf(lastRestock?.timestamp ?: 0L, lastSale ?: 0L)
            val oneMonthMillis = 30L * 24 * 60 * 60 * 1000L
            val isForgotten = (now - lastActivity) > oneMonthMillis && stock > 0
            
            // 5. Sugerencias (LeadTime + Buffer)
            // Si vende mucho, el mínimo debe ser mayor
            val suggestedMin = (vdp * leadTime) * 1.5 // 50% de margen de seguridad
            val suggestedMax = suggestedMin * 2.5
            
            RestockItem(
                product = product,
                currentStock = stock,
                avgDailySales = vdp,
                suggestedMin = suggestedMin,
                suggestedMax = suggestedMax,
                missingQuantity = (suggestedMax - stock).coerceAtLeast(0.0),
                lastRestockDate = lastRestock?.timestamp,
                isForgotten = isForgotten
            )
        }.sortedWith(compareByDescending<RestockItem> { it.missingQuantity }.thenBy { it.isForgotten })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun setAnalysisDays(days: Int) { _analysisDays.value = days }
    fun setLeadTimeDays(days: Int) { _leadTimeDays.value = days }

    fun applySuggestions() {
        viewModelScope.launch {
            _isUpdating.value = true
            try {
                restockSuggestions.value.forEach { suggestion ->
                    productRepository.updateStockLimits(
                        productId = suggestion.product.id,
                        branchId = branchId,
                        min = suggestion.suggestedMin,
                        max = suggestion.suggestedMax
                    )
                }
            } finally {
                _isUpdating.value = false
            }
        }
    }
}

data class RestockItem(
    val product: Product,
    val currentStock: Double,
    val avgDailySales: Double,
    val suggestedMin: Double,
    val suggestedMax: Double,
    val missingQuantity: Double,
    val lastRestockDate: Long? = null,
    val isForgotten: Boolean = false
)
