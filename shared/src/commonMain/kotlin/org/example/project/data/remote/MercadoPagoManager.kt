package com.abtsplazita.posplazita.data.remote

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

class MercadoPagoManager {
    
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { 
                ignoreUnknownKeys = true 
                prettyPrint = true
                isLenient = true
            })
        }
    }

    private val _status = MutableStateFlow("Desconectado")
    val status = _status.asStateFlow()

    private var accessToken = "APP_USR-571829913797874-082201-4a83171dceabcd3f89f147b59575f4e2-274357159"
    private var appId = "571829913797874"
    private var userId = "274357159"

    fun setCredentials(token: String, clientId: String, uId: String = "") {
        accessToken = token.trim()
        appId = clientId.trim()
        userId = uId.trim()
    }

    /**
     * Envía una orden de pago al dispositivo Point Smart.
     * Retorna: (Éxito, ID de Intento, Referencia Externa)
     */
    suspend fun sendPaymentToPoint(deviceId: String, amount: Double, description: String, idempotencyKey: String? = null): Triple<Boolean, String, String> {
        if (deviceId.isBlank()) return Triple(false, "ID de dispositivo no configurado", "")
        if (accessToken.isBlank()) return Triple(false, "Access Token no configurado", "")
        
        try {
            _status.value = "Enviando pago..."
            val finalIdempotencyKey = idempotencyKey ?: "POS_${com.abtsplazita.posplazita.currentTimeMillis()}"
            
            // Limpieza y formato del ID de dispositivo para México (Point Smart)
            val cleanId = deviceId.trim()
            val fullId = if (!cleanId.contains("__")) "NEWLAND_N950__$cleanId" else cleanId
            
            // URL OFICIAL DE INTEGRACIÓN POINT
            val url = "https://api.mercadopago.com/point/integration-api/devices/$fullId/payment-intents"
            
            println("MP_DEBUG_PAYMENT: POST a $url")

            val response = client.post(url) {
                header("Authorization", "Bearer $accessToken")
                header("X-Idempotency-Key", finalIdempotencyKey)
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject {
                    // Monto en centavos (Requerido) - Usamos roundToInt para evitar errores de precisión
                    put("amount", (amount * 100).roundToInt()) 
                    
                    // Intentamos enviar info adicional permitida para forzar el flujo directo
                    put("additional_info", buildJsonObject {
                        put("external_reference", finalIdempotencyKey)
                        put("print_on_terminal", true) // Esto ayuda a que la terminal reaccione de inmediato
                    })
                })
            }

            val responseBody = response.bodyAsText()
            println("MP_DEBUG_RESPONSE: ${response.status} - $responseBody")

            return if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
                _status.value = "Orden enviada con éxito"
                val json = Json.parseToJsonElement(responseBody).jsonObject
                val id = json["id"]?.jsonPrimitive?.content ?: ""
                Triple(true, id, finalIdempotencyKey)
            } else {
                val errorMsg = try {
                    val json = Json.parseToJsonElement(responseBody).jsonObject
                    json["message"]?.jsonPrimitive?.content ?: "Error del servidor (404/403)"
                } catch(e: Exception) { responseBody }
                
                _status.value = "Error: $errorMsg"
                Triple(false, errorMsg, finalIdempotencyKey)
            }
        } catch (e: Exception) {
            _status.value = "Fallo de red"
            return Triple(false, e.message ?: "Error de red", idempotencyKey ?: "")
        }
    }

    /**
     * Obtiene la lista de terminales Point vinculadas a la cuenta.
     */
    suspend fun getDevices(): List<String> {
        if (accessToken.isBlank()) return emptyList()
        try {
            _status.value = "Buscando dispositivos..."
            // URL estándar para listar dispositivos Point
            val url = "https://api.mercadopago.com/point/integration-api/devices"
            val response = client.get(url) {
                header("Authorization", "Bearer $accessToken")
            }
            
            val body = response.bodyAsText()
            println("MP_DEVICES_LIST: ${response.status} - $body")

            if (response.status == HttpStatusCode.OK) {
                val json = Json.parseToJsonElement(body).jsonObject
                val devices = json["devices"]?.jsonArray ?: return emptyList()
                val list = devices.mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.content }
                _status.value = if (list.isEmpty()) "Sin dispositivos" else "${list.size} encontrados"
                return list
            } else {
                _status.value = "Error al buscar: ${response.status}"
            }
        } catch (e: Exception) {
            _status.value = "Fallo red dispositivos"
        }
        return emptyList()
    }

    /**
     * Activa el modo Punto de Venta (PDV) en una terminal.
     */
    suspend fun activatePdvMode(deviceId: String): Pair<Boolean, String> {
        if (accessToken.isBlank()) return false to "Access Token no configurado"
        if (deviceId.isBlank()) return false to "ID de dispositivo vacío"
        
        try {
            _status.value = "Activando PDV..."
            
            // Usar el ID tal cual viene en la lista (que ya incluye el modelo si es necesario)
            val cleanId = deviceId.trim()
            
            // La URL correcta para actualizar el modo es el recurso del dispositivo directamente
            val url = "https://api.mercadopago.com/point/integration-api/devices/$cleanId"
            println("MP_DEBUG_PDV: PATCH a $url")
            
            val response = client.patch(url) {
                header("Authorization", "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject {
                    put("operating_mode", "PDV")
                })
            }

            val body = response.bodyAsText()
            println("MP_DEBUG_PDV_RESPONSE: ${response.status} - $body")

            return if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.NoContent || response.status == HttpStatusCode.Accepted) {
                _status.value = "Modo PDV Activado"
                true to "OK"
            } else {
                val errorMsg = try {
                    val json = Json.parseToJsonElement(body).jsonObject
                    json["message"]?.jsonPrimitive?.content ?: "Error $body"
                } catch(e: Exception) { body }
                _status.value = "Error PDV: $errorMsg"
                false to errorMsg
            }
        } catch (e: Exception) {
            _status.value = "Error de red PDV"
            return false to (e.message ?: "Error desconocido")
        }
    }

    /**
     * Consulta el estado de un intento de pago.
     * Retorna: "SUCCESS", "REJECTED", "CANCELED", "ERROR" o "OPEN"
     */
    suspend fun checkPaymentStatus(paymentIntentId: String): String {
        try {
            val url = "https://api.mercadopago.com/point/integration-api/payment-intents/$paymentIntentId"
            val response = client.get(url) {
                header("Authorization", "Bearer $accessToken")
            }
            
            if (response.status == HttpStatusCode.OK) {
                val responseText = response.bodyAsText()
                val json = Json.parseToJsonElement(responseText).jsonObject
                val state = json["state"]?.jsonPrimitive?.content ?: "UNKNOWN"
                
                // PRIORIDAD: Si ya existe un objeto payment en la respuesta, lo tomamos como verdad
                // independientemente del estado "state" de la intención.
                val payment = json["payment"]?.jsonObject
                if (payment != null) {
                    val status = payment["status"]?.jsonPrimitive?.content
                    val detail = payment["status_detail"]?.jsonPrimitive?.content ?: ""
                    
                    when (status) {
                        "approved" -> {
                            _status.value = "Pago Aprobado ✅"
                            return "SUCCESS"
                        }
                        "in_process", "authorized", "pending" -> {
                            _status.value = "Confirmando ($status)..."
                            return "OPEN"
                        }
                        "rejected", "cancelled" -> {
                            if (detail == "cc_rejected_duplicated_payment") {
                                _status.value = "Advertencia: Pago Duplicado. Revisa la terminal."
                                return "OPEN" // Mantenemos abierto para permitir que el usuario acepte el duplicado en la terminal
                            }
                            _status.value = "Rechazado: $status ($detail)"
                            return "REJECTED"
                        }
                    }
                }

                when (state) {
                    "FINISHED" -> {
                        // Si es FINISHED pero no hay payment object o status es nulo, 
                        // es un error o rechazo implícito.
                        _status.value = "Finalizado sin aprobación ⚠️"
                        return "REJECTED"
                    }
                    "CANCELED" -> {
                        _status.value = "Pago Cancelado ❌"
                        return "CANCELED"
                    }
                    "ERROR", "ABORTED", "EXPIRED" -> {
                        _status.value = "Transacción fallida ($state) ⚠️"
                        return "ERROR"
                    }
                    "OPEN" -> {
                        _status.value = "Esperando acción en terminal..."
                        return "OPEN"
                    }
                    else -> {
                        _status.value = "Estado: $state..."
                        return "OPEN"
                    }
                }
            } else if (response.status.value in 500..599) {
                _status.value = "Error MP (${response.status.value}), reintentando..."
                return "OPEN"
            }
        } catch (e: Exception) {
            println("MP_STATUS_ERROR: ${e.message}")
            _status.value = "Buscando pago..."
            return "OPEN"
        }
        return "ERROR"
    }

    /**
     * Búsqueda de pago por referencia externa.
     * Útil cuando la API de intención de pago tiene retraso.
     */
    suspend fun searchPaymentByReference(externalReference: String): String {
        if (externalReference.isBlank()) return "OPEN"
        try {
            val url = "https://api.mercadopago.com/v1/payments/search?external_reference=$externalReference"
            val response = client.get(url) {
                header("Authorization", "Bearer $accessToken")
            }
            
            if (response.status == HttpStatusCode.OK) {
                val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                val results = json["results"]?.jsonArray ?: return "OPEN"
                
                if (results.isNotEmpty()) {
                    val payment = results[0].jsonObject
                    val status = payment["status"]?.jsonPrimitive?.content
                    
                    return when (status) {
                        "approved" -> {
                            _status.value = "Pago Aprobado (Search) ✅"
                            "SUCCESS"
                        }
                        "rejected", "cancelled" -> {
                            val detail = payment["status_detail"]?.jsonPrimitive?.content ?: ""
                            if (detail == "cc_rejected_duplicated_payment") {
                                _status.value = "Advertencia: Pago Duplicado (Search). Revisa la terminal."
                                "OPEN" // Seguir esperando
                            } else {
                                _status.value = "Rechazado (Search): $detail"
                                "REJECTED"
                            }
                        }
                        "in_process", "authorized", "pending" -> "OPEN"
                        else -> "OPEN"
                    }
                }
            }
        } catch (e: Exception) {
            println("MP_SEARCH_ERROR: ${e.message}")
        }
        return "OPEN"
    }
}
