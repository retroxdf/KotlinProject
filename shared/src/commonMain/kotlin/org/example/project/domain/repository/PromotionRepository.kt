package com.abtsplazita.posplazita.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.abtsplazita.posplazita.data.local.PromotionDao
import com.abtsplazita.posplazita.data.toDomain
import com.abtsplazita.posplazita.data.toEntity
import com.abtsplazita.posplazita.domain.Promotion
import com.abtsplazita.posplazita.data.remote.FirebaseManager

class PromotionRepository(
    private val promotionDao: PromotionDao,
    private val firebaseManager: FirebaseManager? = null
) {
    fun getAllPromotions(): Flow<List<Promotion>> {
        return promotionDao.getAllPromotions().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun savePromotion(promotion: Promotion) {
        promotionDao.insertPromotion(promotion.toEntity())
        // firebaseManager?.syncPromotion(promotion) // TODO: Implementar si se requiere sync
    }

    suspend fun deletePromotion(promotion: Promotion) {
        promotionDao.deletePromotion(promotion.toEntity())
    }

    suspend fun togglePromotion(id: String, active: Boolean) {
        promotionDao.togglePromotion(id, active)
    }
}
