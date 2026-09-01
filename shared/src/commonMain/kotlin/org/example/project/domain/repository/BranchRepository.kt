package com.abtsplazita.posplazita.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.MainScope
import com.abtsplazita.posplazita.data.local.BranchDao
import com.abtsplazita.posplazita.data.local.BranchEntity
import com.abtsplazita.posplazita.domain.Branch
import com.abtsplazita.posplazita.data.remote.FirebaseManager
import com.abtsplazita.posplazita.data.*

class BranchRepository(
    private val branchDao: BranchDao,
    private val firebaseManager: FirebaseManager? = null,
    private val scope: kotlinx.coroutines.CoroutineScope? = null
) {

    fun getAllBranches(): Flow<List<Branch>> {
        return branchDao.getAllBranches().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun addBranch(branch: Branch) {
        val updated = branch.copy(lastUpdated = com.abtsplazita.posplazita.currentTimeMillis())
        branchDao.insertBranch(updated.toEntity())
        firebaseManager?.syncBranch(updated)
    }

    suspend fun deleteBranch(branch: Branch) {
        branchDao.deleteBranch(branch.toEntity())
        firebaseManager?.deleteBranch(branch.id)
    }

    suspend fun refreshBranches() {
        println("BRANCH_REPO: Actualizando sucursales desde la nube...")
        val cloudBranches = firebaseManager?.fetchBranches() ?: emptyList()
        if (cloudBranches.isNotEmpty()) {
            val entities = cloudBranches.map { it.toEntity() }
            branchDao.insertBranchesBatch(entities)
            
            // Sincronizar eliminaciones
            val cloudIds = cloudBranches.map { it.id }.toSet()
            val localBranches = branchDao.getAllBranches().first()
            localBranches.forEach { local ->
                if (local.id !in cloudIds) {
                    branchDao.deleteBranch(local)
                }
            }
            println("BRANCH_REPO: Sucursales actualizadas (${cloudBranches.size}).")
        }
    }

    fun startIncrementalSync() {
        val activeScope = scope ?: kotlinx.coroutines.GlobalScope
        println("BRANCH_REPO: Iniciando observación incremental...")
        activeScope.launch {
            try {
                val since = branchDao.getLastUpdated() ?: 0L
                firebaseManager?.observeBranchesIncremental(since) { cloudBranches ->
                    if (cloudBranches.isNotEmpty()) {
                        activeScope.launch {
                            branchDao.insertBranchesBatch(cloudBranches.map { it.toEntity() })
                            println("BRANCH_REPO: ${cloudBranches.size} sucursales actualizadas incrementalmente.")
                        }
                    }
                }
            } catch (e: Exception) {
                println("BRANCH_REPO_SYNC_ERROR: ${e.message}")
            }
        }
    }
}
