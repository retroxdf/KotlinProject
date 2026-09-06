package com.abtsplazita.posplazita.data

import kotlinx.coroutines.*
import com.abtsplazita.posplazita.domain.repository.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class SyncManager(
    private val saleRepository: SaleRepository,
    private val movementRepository: CashMovementRepository,
    private val productRepository: ProductRepository,
    private val branchRepository: BranchRepository,
    private val userRepository: UserRepository,
    private val employeeRepository: EmployeeRepository,
    private val customerRepository: CustomerRepository,
    private val purchaseRepository: PurchaseRepository,
    private val supplierRepository: SupplierRepository,
    private val promotionRepository: PromotionRepository,
    private val deletionLogRepository: DeletionLogRepository,
    private val settingsRepository: SettingsRepository,
    private val firebaseManager: com.abtsplazita.posplazita.data.remote.FirebaseManager,
    private val scope: CoroutineScope
) {
    private var syncJob: Job? = null
    private var periodic1hJob: Job? = null
    private var periodic3hJob: Job? = null
    private var approvalsJob: Job? = null
    
    private var currentBranchId: String? = null
    private var currentUsername: String? = null

    fun setUserInfo(username: String?) {
        currentUsername = username
    }

    fun setBranchId(id: String?) {
        if (currentBranchId != id && id != null) {
            currentBranchId = id
            // Reiniciar observación de inventario para la nueva sucursal
            productRepository.startIncrementalSync(id)
            startApprovalsObservation(id)
            
            scope.launch {
                val syncKey = "is_inv_sync_done_$id"
                if (settingsRepository.getSetting(syncKey) != "true") {
                    println("SYNC_MANAGER: Descargando inventario inicial para sucursal $id...")
                    try {
                        productRepository.refreshInventory(id, isInitial = true)
                        settingsRepository.saveSetting(syncKey, "true")
                    } catch (e: Exception) {
                        println("SYNC_MANAGER: Error al descargar inventario: ${e.message}")
                    }
                }
            }
        } else {
            currentBranchId = id
        }
    }

    private fun startApprovalsObservation(branchId: String) {
        approvalsJob?.cancel()
        approvalsJob = scope.launch {
            firebaseManager.observeDeletionRequests(branchId) { requests ->
                // Procesar aprobaciones de forma global para que el solicitante lo reciba 
                // sin importar en qué pantalla esté.
                val approved = requests.filter { it.status == "APPROVED" }
                approved.forEach { req ->
                    if (req.userId == currentUsername) {
                        scope.launch {
                            println("SYNC_MANAGER: Procesando aprobación de borrado para ticket ${req.ticketId}")
                            saleRepository.deleteHeldSale(req.ticketId)
                            firebaseManager.deleteDeletionRequest(req.id)
                        }
                    }
                }
            }
        }
    }

    fun startAutoSync() {
        if (syncJob != null) return
        
        // 1. Iniciamos observadores incrementales (Para Android)
        productRepository.startIncrementalSync(currentBranchId ?: "")
        userRepository.startIncrementalSync()
        branchRepository.startIncrementalSync()
        customerRepository.startIncrementalSync()

        // 2. Tarea de Sincronización Inicial y Subida cada 15 min
        syncJob = scope.launch {
            delay(5000)
            
            val isInitialSyncDone = settingsRepository.getSetting("is_initial_sync_completed") == "true"
            if (!isInitialSyncDone) {
                performFullInitialSync()
                settingsRepository.saveSetting("is_initial_sync_completed", "true")
            }

            while (isActive) {
                println("SYNC_MANAGER: Subiendo datos locales pendientes (15 min)...")
                try {
                    saleRepository.syncPendingSalesWithCloud()
                    movementRepository.syncPendingMovementsWithCloud()
                } catch (e: Exception) {
                    println("SYNC_MANAGER: Error al subir datos: ${e.message}")
                }
                delay(15.minutes)
            }
        }

        // 3. Tarea de Descarga de Productos cada 1 hora
        periodic1hJob = scope.launch {
            while (isActive) {
                delay(1.hours)
                println("SYNC_MANAGER: Actualizando catálogo de productos (1h)...")
                try {
                    productRepository.refreshProducts(isInitial = false)
                } catch (e: Exception) {}
            }
        }

        // 4. Tarea de Descarga de Clientes, Promociones y Configuración cada 3 horas
        periodic3hJob = scope.launch {
            while (isActive) {
                delay(3.hours)
                println("SYNC_MANAGER: Sincronización trihoraria (Clientes, Promos, Borrados)...")
                try {
                    customerRepository.refreshCustomers()
                    promotionRepository.refreshPromotions()
                    supplierRepository.refreshSuppliers()
                    if (currentBranchId != null) {
                        purchaseRepository.refreshPurchases(currentBranchId!!)
                        deletionLogRepository.refreshLogs(currentBranchId!!)
                    }
                } catch (e: Exception) {}
            }
        }
    }

    private suspend fun performFullInitialSync() {
        println("SYNC_MANAGER: Realizando descarga inicial masiva...")
        try {
            productRepository.refreshProducts(isInitial = true)
            userRepository.refreshUsers()
            employeeRepository.refreshEmployees()
            branchRepository.refreshBranches()
            customerRepository.refreshCustomers()
            
            if (currentBranchId != null) {
                productRepository.refreshInventory(currentBranchId!!, isInitial = true)
            }
            println("SYNC_MANAGER: Descarga inicial completada exitosamente.")
        } catch (e: Exception) {
            println("SYNC_MANAGER: Error en carga inicial: ${e.message}")
        }
    }

    fun stopAutoSync() {
        syncJob?.cancel()
        periodic1hJob?.cancel()
        periodic3hJob?.cancel()
        syncJob = null
    }
}

