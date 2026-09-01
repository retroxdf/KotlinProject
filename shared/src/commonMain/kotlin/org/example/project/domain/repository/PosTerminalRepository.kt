package com.abtsplazita.posplazita.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.MainScope
import com.abtsplazita.posplazita.data.local.PosTerminalDao
import com.abtsplazita.posplazita.data.local.PosTerminalEntity
import com.abtsplazita.posplazita.data.toDomain
import com.abtsplazita.posplazita.data.toEntity
import com.abtsplazita.posplazita.domain.PosTerminal
import com.abtsplazita.posplazita.data.remote.FirebaseManager

class PosTerminalRepository(
    private val posTerminalDao: PosTerminalDao,
    private val firebaseManager: FirebaseManager? = null
) {

    fun getTerminalsByBranch(branchId: String): Flow<List<PosTerminal>> {
        return posTerminalDao.getTerminalsByBranch(branchId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun addTerminal(terminal: PosTerminal) {
        posTerminalDao.insertTerminal(terminal.toEntity())
        firebaseManager?.syncTerminal(terminal)
    }

    suspend fun deleteTerminal(id: String) {
        posTerminalDao.deleteTerminal(id)
        firebaseManager?.deleteTerminal(id)
    }

    suspend fun refreshTerminals(branchId: String) {
        println("TERMINAL_REPO: Actualizando cajas para sucursal $branchId...")
        val cloudTerminals = firebaseManager?.fetchTerminals(branchId) ?: emptyList()
        cloudTerminals.forEach { terminal ->
            posTerminalDao.insertTerminal(terminal.toEntity())
        }
        println("TERMINAL_REPO: Cajas actualizadas (${cloudTerminals.size}).")
    }

    fun startCloudSync(branchId: String) {
        // Snapshots deshabilitados para optimizar datos
    }
}
