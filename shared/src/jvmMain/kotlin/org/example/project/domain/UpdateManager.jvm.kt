package com.abtsplazita.posplazita.domain

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.serialization.json.*
import java.io.File
import java.io.FileOutputStream

class JvmUpdateManager : UpdateManager {
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { 
                ignoreUnknownKeys = true 
                isLenient = true
            })
        }
    }

    override fun getAppVersion(): String = "1.0.9"

    override suspend fun fetchLatestRelease(): AppUpdateInfo? {
        return try {
            println("GITHUB_UPDATER: Consultando GitHub API...")
            val response: HttpResponse = httpClient.get("https://api.github.com/repos/retroxdf/web/releases/latest") {
                header("User-Agent", "PlazitaPOS-Updater")
                header("Accept", "application/vnd.github.v3+json")
            }
            
            if (response.status != HttpStatusCode.OK) {
                println("GITHUB_UPDATER: Error en API (${response.status})")
                return null
            }

            val body: JsonObject = response.body()
            val tagName = body["tag_name"]?.jsonPrimitive?.content ?: return null
            val version = tagName.removePrefix("v")
            val releaseNotes = body["body"]?.jsonPrimitive?.content
            
            val assets = body["assets"]?.jsonArray ?: return null
            val msiAsset = assets.find { 
                val name = it.jsonObject["name"]?.jsonPrimitive?.content ?: ""
                name.endsWith(".msi", ignoreCase = true)
            }?.jsonObject ?: return null
            
            val downloadUrl = msiAsset["browser_download_url"]?.jsonPrimitive?.content ?: return null
            
            println("GITHUB_UPDATER: Detectada versión $version. URL: $downloadUrl")
            
            AppUpdateInfo(
                version = version,
                downloadUrl = downloadUrl,
                releaseNotes = releaseNotes,
                forceUpdate = releaseNotes?.contains("#FORCE_UPDATE") == true
            )
        } catch (e: Exception) {
            println("GITHUB_UPDATER_ERROR: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    override suspend fun downloadAndInstall(url: String, onProgress: (Float) -> Unit): Boolean {
        return try {
            val response = httpClient.get(url)
            val contentLength = response.headers[HttpHeaders.ContentLength]?.toLong() ?: 0L
            val tempFile = File.createTempFile("plazita_pos_update", ".msi")
            
            val channel: ByteReadChannel = response.bodyAsChannel()
            var downloaded = 0L
            
            FileOutputStream(tempFile).use { output ->
                while (!channel.isClosedForRead) {
                    val packet = channel.readRemaining(8192)
                    while (packet.remaining > 0) {
                        val bytes = packet.readBytes()
                        output.write(bytes)
                        downloaded += bytes.size.toLong()
                        if (contentLength > 0) {
                            onProgress(downloaded.toFloat() / contentLength.toFloat())
                        }
                    }
                }
            }

            println("CLOUD_JVM: Descarga completada. Ejecutando instalador...")
            
            // Ejecutar el instalador MSI
            ProcessBuilder("msiexec", "/i", tempFile.absolutePath, "/passive").start()
            
            // Cerrar la app actual para permitir la actualización
            System.exit(0)
            
            true
        } catch (e: Exception) {
            println("CLOUD_JVM_UPDATE_ERROR: ${e.message}")
            false
        }
    }
}

actual fun getUpdateManager(): UpdateManager = JvmUpdateManager()
