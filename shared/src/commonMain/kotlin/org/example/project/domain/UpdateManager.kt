package com.abtsplazita.posplazita.domain

interface UpdateManager {
    fun getAppVersion(): String
    suspend fun fetchLatestRelease(): AppUpdateInfo?
    suspend fun downloadAndInstall(url: String, onProgress: (Float) -> Unit): Boolean
}

expect fun getUpdateManager(): UpdateManager
