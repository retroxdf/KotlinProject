package com.abtsplazita.posplazita.data

import kotlinx.coroutines.*
import com.abtsplazita.posplazita.domain.repository.*
import kotlin.time.Duration.Companion.minutes

class SyncManager(
    private val saleRepository: SaleRepository,
    private val movementRepository: CashMovementRepository,
    private val productRepository: ProductRepository,
    private val branchRepository: BranchRepository,
    private val userRepository: UserRepository,
    private val customerRepository: CustomerRepository,
    private val settingsRepository: SettingsRepository,
    private val scope: CoroutineScope
) {
    private var syncJob: Job? = null
    private var currentBranchId: String? = null

    fun setBranchId(id: String?) {
        if (currentBranchId != id && id != null) {
            currentBranchId = id
            // Reiniciar observación de inventario para la nueva sucursal
            productRepository.startIncrementalSync(id)
        } else {
            currentBranchId = id
        }
    }

    fun startAutoSync() {
        if (syncJob != null) return
        
        // Iniciamos observadores incrementales (Tiempo Real en Android, Polling en PC)
        productRepository.startIncrementalSync(currentBranchId ?: "")
        userRepository.startIncrementalSync()
        branchRepository.startIncrementalSync()
        customerRepository.startIncrementalSync()

        syncJob = scope.launch {
            delay(5000) // Unos segundos de calma al iniciar
            
            // --- LOGICA DE SINCRONIZACIÓN INICIAL (Local-First) ---
            val isInitialSyncDone = settingsRepository.getSetting("is_initial_sync_completed") == "true"
            if (!isInitialSyncDone) {
                println("SYNC_MANAGER: Realizando descarga inicial masiva...")
                try {
                    // Descargar todo una sola vez
                    productRepository.refreshProducts(isInitial = true)
                    userRepository.refreshUsers()
                    branchRepository.refreshBranches()
                    customerRepository.refreshCustomers()
                    
                    if (currentBranchId != null) {
                        productRepository.refreshInventory(currentBranchId!!, isInitial = true)
                    }
                    
                    settingsRepository.saveSetting("is_initial_sync_completed", "true")
                    println("SYNC_MANAGER: Descarga inicial completada exitosamente.")
                } catch (e: Exception) {
                    println("SYNC_MANAGER: Error en carga inicial: ${e.message}")
                }
            }

            while (isActive) {
                // Sincronización cada 15 minutos solo para SUBIR datos locales
                println("SYNC_MANAGER: Sincronizando datos locales pendientes (Cada 15 min)...")
                
                try {
                    // Subir ventas y movimientos locales pendientes
                    saleRepository.syncPendingSalesWithCloud()
                    movementRepository.syncPendingMovementsWithCloud()
                    println("SYNC_MANAGER: Datos locales sincronizados.")
                } catch (e: Exception) {
                    println("SYNC_MANAGER: Error al subir datos: ${e.message}")
                }
                
                // Esperar 15 minutos
                delay(15.minutes)
            }
        }
    }

    fun stopAutoSync() {
        syncJob?.cancel()
        syncJob = null
    }
}
