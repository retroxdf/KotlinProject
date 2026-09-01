package com.abtsplazita.posplazita.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.abtsplazita.posplazita.domain.Sale
import com.abtsplazita.posplazita.domain.Expense
import com.abtsplazita.posplazita.domain.repository.SaleRepository
import com.abtsplazita.posplazita.domain.repository.ExpenseRepository
import com.abtsplazita.posplazita.domain.repository.ProductRepository
import com.abtsplazita.posplazita.currentTimeMillis
import kotlinx.datetime.*

class DashboardViewModel(
    private val saleRepository: SaleRepository,
    private val expenseRepository: ExpenseRepository,
    private val productRepository: ProductRepository,
    private val terminalRepository: com.abtsplazita.posplazita.domain.repository.PosTerminalRepository? = null,
    private val branchId: String
) : ViewModel() {

    private val _period = MutableStateFlow(DashboardPeriod.TODAY)
    val period = _period.asStateFlow()

    private val _selectedTerminalId = MutableStateFlow<String?>(null)
    val selectedTerminalId = _selectedTerminalId.asStateFlow()

    val availableTerminals = if (terminalRepository != null) {
        terminalRepository.getTerminalsByBranch(branchId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    } else {
        MutableStateFlow(emptyList())
    }

    fun setPeriod(p: DashboardPeriod) { _period.value = p }
    fun selectTerminal(id: String?) { _selectedTerminalId.value = id }

    val dashboardData = combine(
        saleRepository.getSales(branchId),
        expenseRepository.getExpenses(branchId),
        productRepository.getProducts(),
        _period,
        _selectedTerminalId
    ) { sales, expenses, allProducts, p, terminalId ->
        val now = currentTimeMillis()
        val startOfPeriod = getStartOfPeriod(p, now)
        
        val filteredSales = sales.filter { 
            it.timestamp >= startOfPeriod && (terminalId == null || it.terminalId == terminalId)
        }
        val filteredExpenses = expenses.filter { it.timestamp >= startOfPeriod }
        
        val totalSales = filteredSales.sumOf { it.netTotal }
        val totalExpenses = filteredExpenses.sumOf { it.amount }
        val netProfit = totalSales - totalExpenses
        
        // Optimización: Usar mapa para búsqueda de nombres de productos
        val productMap = allProducts.associateBy { it.id }
        
        // Top 30 productos vendidos
        val topProducts = filteredSales.flatMap { it.items }
            .groupBy { it.productId }
            .map { (id, items) -> 
                val name = productMap[id]?.name ?: "Producto #$id"
                name to items.sumOf { it.quantity } 
            }
            .sortedByDescending { it.second }
            .take(30)

        DashboardStats(
            totalSales = totalSales,
            totalExpenses = totalExpenses,
            netProfit = netProfit,
            topProducts = topProducts,
            salesCount = filteredSales.size
        )
    }.flowOn(kotlinx.coroutines.Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    private fun getStartOfPeriod(p: DashboardPeriod, now: Long): Long {
        val dt = Instant.fromEpochMilliseconds(now).toLocalDateTime(TimeZone.currentSystemDefault())
        return when(p) {
            DashboardPeriod.TODAY -> {
                val start = LocalDateTime(dt.year, dt.month, dt.dayOfMonth, 0, 0)
                start.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
            }
            DashboardPeriod.WEEK -> {
                now - (7 * 24 * 60 * 60 * 1000L)
            }
            DashboardPeriod.MONTH -> {
                now - (30 * 24 * 60 * 60 * 1000L)
            }
        }
    }
}

enum class DashboardPeriod { TODAY, WEEK, MONTH }

data class DashboardStats(
    val totalSales: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netProfit: Double = 0.0,
    val topProducts: List<Pair<String, Double>> = emptyList(),
    val salesCount: Int = 0
)
