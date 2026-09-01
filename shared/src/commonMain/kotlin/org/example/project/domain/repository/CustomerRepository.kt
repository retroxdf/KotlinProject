package com.abtsplazita.posplazita.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.MainScope
import com.abtsplazita.posplazita.data.local.CustomerDao
import com.abtsplazita.posplazita.data.local.CustomerPaymentDao
import com.abtsplazita.posplazita.data.local.CustomerProductPriceDao
import com.abtsplazita.posplazita.data.toDomain
import com.abtsplazita.posplazita.data.toEntity
import com.abtsplazita.posplazita.domain.Customer
import com.abtsplazita.posplazita.domain.CustomerPayment
import com.abtsplazita.posplazita.domain.CustomerProductPrice
import com.abtsplazita.posplazita.data.remote.FirebaseManager

class CustomerRepository(
    private val customerDao: CustomerDao,
    private val paymentDao: CustomerPaymentDao,
    private val specialPriceDao: CustomerProductPriceDao,
    private val firebaseManager: FirebaseManager? = null,
    private val scope: kotlinx.coroutines.CoroutineScope? = null
) {
    fun getAllCustomers(): Flow<List<Customer>> {
        return customerDao.getAllCustomers().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun saveCustomer(customer: Customer) {
        val updated = customer.copy(lastUpdated = com.abtsplazita.posplazita.currentTimeMillis())
        customerDao.insertCustomer(updated.toEntity())
        firebaseManager?.syncCustomer(updated)
    }

    suspend fun deleteCustomer(customer: Customer) {
        customerDao.deleteCustomer(customer.toEntity())
        firebaseManager?.deleteCustomer(customer.id)
    }

    suspend fun getCustomerById(id: String): Customer? {
        return customerDao.getCustomerById(id)?.toDomain()
    }

    suspend fun updateDebt(customerId: String, amount: Double) {
        customerDao.updateDebt(customerId, amount)
        // Sincronizar el cliente actualizado con la nube
        customerDao.getCustomerById(customerId)?.let { entity ->
            val updated = entity.toDomain().copy(lastUpdated = com.abtsplazita.posplazita.currentTimeMillis())
            customerDao.insertCustomer(updated.toEntity())
            firebaseManager?.syncCustomer(updated)
        }
    }

    suspend fun updateWalletBalance(customerId: String, amount: Double) {
        customerDao.updateWalletBalance(customerId, amount)
        customerDao.getCustomerById(customerId)?.let { entity ->
            val updated = entity.toDomain().copy(lastUpdated = com.abtsplazita.posplazita.currentTimeMillis())
            customerDao.insertCustomer(updated.toEntity())
            firebaseManager?.syncCustomer(updated)
        }
    }

    fun getPayments(customerId: String): Flow<List<CustomerPayment>> {
        return paymentDao.getPaymentsByCustomer(customerId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun addPayment(payment: CustomerPayment) {
        paymentDao.insertPayment(payment.toEntity())
        // Usar el método del repositorio para asegurar sincronización con la nube
        updateDebt(payment.customerId, -payment.amount)
    }

    suspend fun recalculateDebt(customerId: String, saleRepository: SaleRepository) {
        val payments = paymentDao.getPaymentsByCustomer(customerId).first().sumOf { it.amount }
        val creditSales = saleRepository.getSalesByCustomer(customerId).first().sumOf { it.creditAmount }
        val newDebt = creditSales - payments
        
        val customer = getCustomerById(customerId)
        if (customer != null) {
            val updated = customer.copy(currentDebt = newDebt)
            customerDao.insertCustomer(updated.toEntity())
            firebaseManager?.syncCustomer(updated)
        }
    }

    fun getSpecialPrices(customerId: String): Flow<List<CustomerProductPrice>> {
        return specialPriceDao.getSpecialPricesForCustomer(customerId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun saveSpecialPrice(specialPrice: CustomerProductPrice) {
        specialPriceDao.insertSpecialPrice(specialPrice.toEntity())
    }



    suspend fun deleteSpecialPrice(specialPrice: CustomerProductPrice) {
        specialPriceDao.deleteSpecialPrice(specialPrice.toEntity())
    }

    suspend fun refreshCustomers() {
        println("CUSTOMER_REPO: Actualizando clientes desde la nube...")
        val cloudCustomers = firebaseManager?.fetchCustomers() ?: emptyList()
        if (cloudCustomers.isNotEmpty()) {
            cloudCustomers.forEach { customer ->
                customerDao.insertCustomer(customer.toEntity())
            }
            
            // Sincronizar eliminaciones
            val cloudIds = cloudCustomers.map { it.id }.toSet()
            val localCustomers = customerDao.getAllCustomers().first()
            localCustomers.forEach { local ->
                if (local.id !in cloudIds) {
                    customerDao.deleteCustomer(local)
                }
            }
            println("CUSTOMER_REPO: Clientes actualizados (${cloudCustomers.size}).")
        }
    }

    fun startIncrementalSync() {
        val activeScope = scope ?: kotlinx.coroutines.GlobalScope
        println("CUSTOMER_REPO: Iniciando observación incremental...")
        activeScope.launch {
            try {
                val since = customerDao.getLastUpdated() ?: 0L
                firebaseManager?.observeCustomersIncremental(since) { cloudCustomers ->
                    if (cloudCustomers.isNotEmpty()) {
                        activeScope.launch {
                            cloudCustomers.forEach { customer ->
                                customerDao.insertCustomer(customer.toEntity())
                            }
                            println("CUSTOMER_REPO: ${cloudCustomers.size} clientes actualizados incrementalmente.")
                        }
                    }
                }
            } catch (e: Exception) {
                println("CUSTOMER_REPO_SYNC_ERROR: ${e.message}")
            }
        }
    }
}
