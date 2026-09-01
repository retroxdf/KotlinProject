package com.abtsplazita.posplazita.ui.branches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.abtsplazita.posplazita.domain.Branch
import com.abtsplazita.posplazita.domain.repository.BranchRepository
import com.abtsplazita.posplazita.currentTimeMillis

class BranchViewModel(private val branchRepository: BranchRepository) : ViewModel() {

    val branches = branchRepository.getAllBranches().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog = _showAddDialog.asStateFlow()

    fun openAddDialog() { _showAddDialog.value = true }
    fun closeAddDialog() { _showAddDialog.value = false }

    fun addBranch(name: String, address: String) {
        viewModelScope.launch {
            val id = "b${currentTimeMillis()}"
            branchRepository.addBranch(Branch(id, name, address))
            closeAddDialog()
        }
    }

    fun deleteBranch(branch: Branch) {
        viewModelScope.launch {
            branchRepository.deleteBranch(branch)
        }
    }

    fun refreshBranches() {
        viewModelScope.launch {
            branchRepository.refreshBranches()
        }
    }
}
