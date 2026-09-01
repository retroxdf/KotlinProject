package com.abtsplazita.posplazita.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.abtsplazita.posplazita.domain.Expense
import com.abtsplazita.posplazita.domain.ExpenseCategory
import com.abtsplazita.posplazita.domain.repository.ExpenseRepository
import com.abtsplazita.posplazita.currentTimeMillis

class ExpenseViewModel(
    private val repository: ExpenseRepository,
    private val branchId: String
) : ViewModel() {

    val expenses = repository.getExpenses(branchId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    private val _currentUser = MutableStateFlow<com.abtsplazita.posplazita.domain.User?>(null)

    fun setUserInfo(user: com.abtsplazita.posplazita.domain.User?) {
        _currentUser.value = user
    }

    fun saveExpense(category: ExpenseCategory, amount: Double, reason: String?) {
        if (amount <= 0) return
        
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val expense = Expense(
                    id = "EXP_${currentTimeMillis()}",
                    category = category,
                    amount = amount,
                    timestamp = currentTimeMillis(),
                    branchId = branchId,
                    reason = reason,
                    userId = _currentUser.value?.username ?: "admin"
                )
                repository.saveExpense(expense)
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }
}
