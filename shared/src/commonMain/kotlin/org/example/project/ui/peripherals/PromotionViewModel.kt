package com.abtsplazita.posplazita.ui.peripherals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.abtsplazita.posplazita.domain.*
import com.abtsplazita.posplazita.domain.repository.PromotionRepository
import com.abtsplazita.posplazita.domain.repository.ProductRepository
import com.abtsplazita.posplazita.currentTimeMillis

class PromotionViewModel(
    private val repository: PromotionRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    val promotions = repository.getAllPromotions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products = productRepository.getProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories = productRepository.getCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _editingPromotion = MutableStateFlow<Promotion?>(null)
    val editingPromotion = _editingPromotion.asStateFlow()

    fun startNewPromotion(type: PromotionType) {
        val now = currentTimeMillis()
        _editingPromotion.value = Promotion(
            id = "",
            name = "",
            type = type,
            startDate = now,
            endDate = now + (7 * 24 * 60 * 60 * 1000L), // 1 semana por defecto
            triggerQuantity = 1
        )
    }

    fun updateEditingPromotion(promo: Promotion) {
        _editingPromotion.value = promo
    }

    fun savePromotion() {
        val promo = _editingPromotion.value ?: return
        if (promo.name.isBlank()) return

        viewModelScope.launch {
            val toSave = if (promo.id.isEmpty()) {
                promo.copy(id = "PROM_${currentTimeMillis()}")
            } else promo
            repository.savePromotion(toSave)
            _editingPromotion.value = null
        }
    }

    fun deletePromotion(promo: Promotion) {
        viewModelScope.launch {
            repository.deletePromotion(promo)
        }
    }

    fun togglePromotion(promo: Promotion) {
        viewModelScope.launch {
            repository.togglePromotion(promo.id, !promo.isActive)
        }
    }

    fun cancelEdit() {
        _editingPromotion.value = null
    }
}
