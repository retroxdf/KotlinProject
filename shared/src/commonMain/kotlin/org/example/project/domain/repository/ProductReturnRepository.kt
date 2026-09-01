package com.abtsplazita.posplazita.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.abtsplazita.posplazita.data.local.ProductReturnDao
import com.abtsplazita.posplazita.data.toDomain
import com.abtsplazita.posplazita.data.toEntity
import com.abtsplazita.posplazita.domain.ProductReturn
import com.abtsplazita.posplazita.data.remote.FirebaseManager

class ProductReturnRepository(
    private val returnDao: ProductReturnDao,
    private val firebaseManager: FirebaseManager? = null
) {
    fun getReturns(branchId: String): Flow<List<ProductReturn>> {
        return returnDao.getReturnsByBranch(branchId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun saveReturn(productReturn: ProductReturn) {
        returnDao.insertReturn(productReturn.toEntity())
        firebaseManager?.syncReturn(productReturn)
    }

    suspend fun refreshReturns(branchId: String) {
        println("RETURN_REPO: Sincronizando devoluciones...")
        val cloudItems = firebaseManager?.fetchReturns(branchId) ?: emptyList()
        if (cloudItems.isNotEmpty()) {
            for (item in cloudItems) {
                val entity = item.toEntity().copy(isSynced = true)
                returnDao.insertReturn(entity)
            }
        }
    }
}
