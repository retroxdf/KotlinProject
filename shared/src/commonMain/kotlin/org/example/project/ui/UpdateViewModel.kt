package com.abtsplazita.posplazita.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abtsplazita.posplazita.data.remote.FirebaseManager
import com.abtsplazita.posplazita.domain.AppUpdateInfo
import com.abtsplazita.posplazita.domain.getUpdateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UpdateViewModel : ViewModel() {
    private val updateManager = getUpdateManager()

    private val _updateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    val updateInfo = _updateInfo.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading = _isDownloading.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress = _downloadProgress.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun checkForUpdates() {
        viewModelScope.launch {
            try {
                println("UPDATE_VM: Iniciando búsqueda de actualización...")
                kotlinx.coroutines.delay(2000) // Esperar a que la UI esté lista
                val latest = updateManager.fetchLatestRelease()
                val current = updateManager.getAppVersion()
                
                println("UPDATE_VM: Versión instalada: $current | Última en GitHub: ${latest?.version ?: "No encontrada"}")
                
                if (latest != null) {
                    if (isNewer(latest.version, current)) {
                        println("UPDATE_VM: ¡Nueva versión disponible!")
                        _updateInfo.value = latest
                    } else {
                        println("UPDATE_VM: El sistema está actualizado.")
                    }
                }
            } catch (e: Exception) {
                println("UPDATE_VM_ERROR: ${e.message}")
            }
        }
    }

    private fun isNewer(latest: String, current: String): Boolean {
        val l = latest.split(".").mapNotNull { it.toIntOrNull() }
        val c = current.split(".").mapNotNull { it.toIntOrNull() }
        
        for (i in 0 until minOf(l.size, c.size)) {
            if (l[i] > c[i]) return true
            if (l[i] < c[i]) return false
        }
        return l.size > c.size
    }

    fun startUpdate() {
        val info = _updateInfo.value ?: return
        viewModelScope.launch {
            _isDownloading.value = true
            val success = updateManager.downloadAndInstall(info.downloadUrl) { progress ->
                _downloadProgress.value = progress
            }
            if (!success) {
                _isDownloading.value = false
                _error.value = "Error al descargar o instalar la actualización."
            }
        }
    }

    fun dismissUpdate() {
        _updateInfo.value = null
    }

    fun clearError() {
        _error.value = null
    }
}
