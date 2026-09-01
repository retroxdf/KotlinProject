package com.abtsplazita.posplazita.data.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class OffProductResponse(
    val status: Int,
    val status_verbose: String? = null,
    val product: OffProduct? = null
)

@Serializable
data class OffProduct(
    val product_name: String? = null,
    val image_url: String? = null,
    val categories: String? = null,
    val brands: String? = null
)

class ProductApiService {
    private val jsonConfig = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        encodeDefaults = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(jsonConfig)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15000
            connectTimeoutMillis = 15000
        }
        defaultRequest {
            header(HttpHeaders.UserAgent, "KotlinPOS/1.0 (https://com.abtsplazita.posplazita)")
        }
    }

    suspend fun fetchFromOpenFoodFacts(barcode: String): OffProduct? {
        if (barcode.isBlank()) return null
        
        return try {
            val url = "https://world.openfoodfacts.org/api/v0/product/$barcode.json"
            val response = client.get(url)
            
            if (response.status == HttpStatusCode.OK) {
                val body: OffProductResponse = response.body()
                if (body.status == 1) body.product else null
            } else null
        } catch (e: Exception) {
            println("PRODUCT_API_ERROR: ${e.message}")
            null
        }
    }
}
