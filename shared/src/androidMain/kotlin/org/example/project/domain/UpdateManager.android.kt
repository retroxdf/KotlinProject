package com.abtsplazita.posplazita.domain

class AndroidUpdateManager : UpdateManager {
    override fun getAppVersion(): String = "1.0.7"
    override suspend fun downloadAndInstall(url: String, onProgress: (Float) -> Unit): Boolean = false
}

actual fun getUpdateManager(): UpdateManager = AndroidUpdateManager()
