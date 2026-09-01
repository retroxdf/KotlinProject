package com.abtsplazita.posplazita.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.abtsplazita.posplazita.data.local.ExpenseDao
import com.abtsplazita.posplazita.data.toDomain
import com.abtsplazita.posplazita.data.toEntity
import com.abtsplazita.posplazita.domain.Expense

class ExpenseRepository(private val expenseDao: ExpenseDao) {
    fun getExpenses(branchId: String): Flow<List<Expense>> {
        return expenseDao.getExpensesByBranch(branchId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun saveExpense(expense: Expense) {
        expenseDao.insertExpense(expense.toEntity())
    }

    suspend fun deleteExpense(expense: Expense) {
        expenseDao.deleteExpense(expense.toEntity())
    }
}
