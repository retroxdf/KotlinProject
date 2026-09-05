package com.abtsplazita.posplazita.domain

class IosUpdateManager : UpdateManager {
    override fun getAppVersion(): String = "1.0.3"
    override suspend fun downloadAndInstall(url: String, onProgress: (Float) -> Unit): Boolean = false
}

actual fun getUpdateManager(): UpdateManager = IosUpdateManager()
