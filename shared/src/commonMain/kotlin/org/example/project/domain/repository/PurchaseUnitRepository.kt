package com.abtsplazita.posplazita.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.abtsplazita.posplazita.data.local.PurchaseUnitDao
import com.abtsplazita.posplazita.data.local.PurchaseUnitEntity
import com.abtsplazita.posplazita.domain.PurchaseUnit
import com.abtsplazita.posplazita.data.*

class PurchaseUnitRepository(
    private val unitDao: PurchaseUnitDao
) {
    fun getAllUnits(): Flow<List<PurchaseUnit>> {
        return unitDao.getAllUnits().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun saveUnit(unit: PurchaseUnit) {
        unitDao.insertUnit(unit.toEntity().copy(lastUpdated = com.abtsplazita.posplazita.currentTimeMillis()))
    }

    suspend fun deleteUnit(unit: PurchaseUnit) {
        unitDao.deleteUnit(unit.toEntity())
    }
}
