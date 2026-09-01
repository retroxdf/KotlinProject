package com.abtsplazita.posplazita.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.abtsplazita.posplazita.data.local.SupplierDao
import com.abtsplazita.posplazita.data.local.SupplierPaymentDao
import com.abtsplazita.posplazita.data.local.ProductSupplierDao
import com.abtsplazita.posplazita.data.toDomain
import com.abtsplazita.posplazita.data.toEntity
import com.abtsplazita.posplazita.domain.Supplier
import com.abtsplazita.posplazita.domain.SupplierPayment
import com.abtsplazita.posplazita.domain.ProductSupplier

class SupplierRepository(
    private val supplierDao: SupplierDao,
    private val paymentDao: SupplierPaymentDao? = null,
    private val productSupplierDao: ProductSupplierDao
) {
    fun getAllSuppliers(): Flow<List<Supplier>> {
        return supplierDao.getAllSuppliers().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getSupplierById(id: String): Supplier? {
        return supplierDao.getSupplierById(id)?.toDomain()
    }

    suspend fun saveSupplier(supplier: Supplier) {
        supplierDao.insertSupplier(supplier.toEntity())
    }

    suspend fun deleteSupplier(supplier: Supplier) {
        supplierDao.deleteSupplier(supplier.toEntity())
    }

    suspend fun updateDebt(supplierId: String, amount: Double) {
        supplierDao.updateDebt(supplierId, amount)
    }

    fun getPayments(supplierId: String): Flow<List<SupplierPayment>> {
        return paymentDao?.getPaymentsBySupplier(supplierId)?.map { entities ->
            entities.map { it.toDomain() }
        } ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }

    suspend fun addPayment(payment: SupplierPayment) {
        paymentDao?.insertPayment(payment.toEntity())
        updateDebt(payment.supplierId, -payment.amount)
    }

    fun getSuppliersForProduct(productId: String): Flow<List<ProductSupplier>> {
        return productSupplierDao.getSuppliersForProduct(productId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun saveProductSupplierLink(link: ProductSupplier) {
        productSupplierDao.insertProductSupplier(link.toEntity())
    }
}
