package com.abtsplazita.posplazita.data.remote

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*
import kotlinx.serialization.encodeToString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.abtsplazita.posplazita.domain.*
import io.ktor.http.*

class JvmCloudProvider : CloudProvider {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { 
                ignoreUnknownKeys = true 
                isLenient = true
                encodeDefaults = true
            })
        }
    }
    
    private val apiKey = "AIzaSyAu02c7DOc_1r1jyuIRqBQX6IegUtNYRag"
    private val projectId = "posplazita"
    private val baseUrl = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents"
    
    private var idToken: String? = null

    // --- INTERVALOS DE TIEMPO ---
    private val PULSE_FAST = 30000L // 30 segundos para Inventario y Pedidos
    private val PULSE_NORMAL = 120000L // 2 minutos para el resto
    private val PULSE_SLOW = 600000L // 10 minutos

    init {
        authenticateAnonymously()
    }

    private fun authenticateAnonymously() {
        scope.launch {
            try {
                val url = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$apiKey"
                val response: JsonObject = httpClient.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(buildJsonObject { put("returnSecureToken", true) })
                }.body()
                
                idToken = response["idToken"]?.jsonPrimitive?.content
                println("CLOUD_JVM: Sesión iniciada.")
            } catch (e: Exception) {
                println("CLOUD_JVM_AUTH_ERROR: ${e.message}")
            }
        }
    }

    private fun HttpRequestBuilder.authHeader() {
        idToken?.let { header("Authorization", "Bearer $it") }
    }

    // --- Helpers para compatibilidad con Firestore SDK (Android) ---
    
    private fun JsonObject.toFirestoreFields(): JsonObject {
        val fields = mutableMapOf<String, JsonElement>()
        this.forEach { (key, value) ->
            fields[key] = value.toFirestoreValue()
        }
        return buildJsonObject { put("fields", buildJsonObject { fields.forEach { (k, v) -> put(k, v) } }) }
    }

    private fun JsonElement.toFirestoreValue(): JsonElement = when (this) {
        is JsonPrimitive -> when {
            isString -> buildJsonObject { put("stringValue", content) }
            content == "true" || content == "false" -> buildJsonObject { put("booleanValue", content.toBoolean()) }
            content.contains(".") -> buildJsonObject { put("doubleValue", content.toDouble()) }
            else -> buildJsonObject { put("integerValue", content.toLong()) }
        }
        is JsonObject -> buildJsonObject { put("mapValue", buildJsonObject { put("fields", this@toFirestoreValue.toFirestoreFields()["fields"]!!) }) }
        is JsonArray -> buildJsonObject { 
            put("arrayValue", buildJsonObject { 
                put("values", buildJsonArray { 
                    this@toFirestoreValue.forEach { add(it.toFirestoreValue()) }
                })
            }) 
        }
        else -> buildJsonObject { put("nullValue", JsonNull) }
    }

    private fun JsonObject.fromFirestoreFields(): JsonObject {
        val result = mutableMapOf<String, JsonElement>()
        val fields = this["fields"]?.jsonObject ?: this // Fallback si ya estamos en el nivel de campos
        fields.forEach { (key, value) ->
            result[key] = value.fromFirestoreValue()
        }
        return JsonObject(result)
    }

    private fun JsonElement.fromFirestoreValue(): JsonElement {
        val vObj = this.jsonObject
        return when {
            vObj.containsKey("stringValue") -> JsonPrimitive(vObj["stringValue"]!!.jsonPrimitive.content)
            vObj.containsKey("integerValue") -> JsonPrimitive(vObj["integerValue"]!!.jsonPrimitive.content.toLong())
            vObj.containsKey("doubleValue") -> JsonPrimitive(vObj["doubleValue"]!!.jsonPrimitive.content.toDouble())
            vObj.containsKey("booleanValue") -> JsonPrimitive(vObj["booleanValue"]!!.jsonPrimitive.content.toBoolean())
            vObj.containsKey("mapValue") -> vObj["mapValue"]!!.jsonObject.fromFirestoreFields()
            vObj.containsKey("arrayValue") -> {
                val values = vObj["arrayValue"]!!.jsonObject["values"]?.jsonArray ?: buildJsonArray {}
                buildJsonArray { values.forEach { add(it.fromFirestoreValue()) } }
            }
            else -> JsonNull
        }
    }

    private suspend fun patchDocument(collection: String, id: String, data: JsonObject) {
        try {
            val url = "$baseUrl/$collection/$id"
            httpClient.patch(url) {
                authHeader()
                contentType(ContentType.Application.Json)
                setBody(data.toFirestoreFields())
            }
        } catch (e: Exception) {
            println("CLOUD_JVM_PATCH_ERROR [$collection/$id]: ${e.message}")
        }
    }

    private suspend fun deleteDocument(collection: String, id: String) {
        try {
            val url = "$baseUrl/$collection/$id"
            httpClient.delete(url) { authHeader() }
        } catch (e: Exception) {}
    }

    private suspend fun queryDocuments(collection: String, branchId: String? = null): List<JsonObject> {
        return try {
            val url = "$baseUrl:runQuery"
            val response: JsonArray = httpClient.post(url) {
                authHeader()
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject {
                    put("structuredQuery", buildJsonObject {
                        put("from", buildJsonArray { add(buildJsonObject { put("collectionId", collection) }) })
                        if (branchId != null) {
                            put("where", buildJsonObject {
                                put("fieldFilter", buildJsonObject {
                                    put("field", buildJsonObject { put("fieldPath", "branchId") })
                                    put("op", "EQUAL")
                                    put("value", buildJsonObject { put("stringValue", branchId) })
                                })
                            })
                        }
                    })
                })
            }.body()

            response.mapNotNull { element ->
                try {
                    val doc = element.jsonObject["document"]?.jsonObject ?: return@mapNotNull null
                    doc.fromFirestoreFields()
                } catch (e: Exception) { null }
            }
        } catch (e: Exception) { emptyList() }
    }

    private suspend fun listDocuments(collection: String): List<JsonObject> {
        return try {
            val url = "$baseUrl/$collection"
            val response: JsonObject = httpClient.get(url) { authHeader() }.body()
            response["documents"]?.jsonArray?.mapNotNull { it.jsonObject.fromFirestoreFields() } ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    // --- Implementación de CloudProvider ---

    override fun syncProduct(product: Product) {
        scope.launch { patchDocument("products", product.id, Json.encodeToJsonElement(product).jsonObject) }
    }
    override fun deleteProduct(id: String) {
        scope.launch { deleteDocument("products", id) }
    }
    override fun observeProducts(onUpdate: (List<Product>) -> Unit) {
        // Solo carga inicial para la PC. Luego confiamos en observeProductsIncremental
        scope.launch { 
            onUpdate(fetchProducts())
        }
    }
    override fun observeProductsIncremental(since: Long, onUpdate: (List<Product>) -> Unit) {
        scope.launch { 
            var lastSeen = since
            while(true) { 
                val news = fetchProductsIncremental(lastSeen)
                if (news.isNotEmpty()) {
                    onUpdate(news)
                    lastSeen = news.maxOf { it.lastUpdated }
                }
                delay(PULSE_NORMAL) 
            } 
        }
    }

    override fun syncSale(sale: Sale) {
        scope.launch { patchDocument("sales", sale.id, Json.encodeToJsonElement(sale).jsonObject) }
    }
    override fun observeSales(branchId: String, onUpdate: (List<Sale>) -> Unit) {
        scope.launch { while(true) { onUpdate(fetchSales(branchId)); delay(PULSE_NORMAL) } }
    }

    override fun syncCustomer(customer: Customer) {
        scope.launch { patchDocument("customers", customer.id, Json.encodeToJsonElement(customer).jsonObject) }
    }
    override fun deleteCustomer(id: String) {
        scope.launch { deleteDocument("customers", id) }
    }
    override fun observeCustomers(onUpdate: (List<Customer>) -> Unit) {
        scope.launch { onUpdate(fetchCustomers()) }
    }
    override fun observeCustomersIncremental(since: Long, onUpdate: (List<Customer>) -> Unit) {
        scope.launch { 
            var lastSeen = since
            while(true) { 
                val news = fetchCustomersIncremental(lastSeen)
                if (news.isNotEmpty()) {
                    onUpdate(news)
                    lastSeen = news.maxOf { it.lastUpdated }
                }
                delay(PULSE_NORMAL) 
            } 
        }
    }

    override fun syncUser(user: User) {
        scope.launch { patchDocument("users", user.id, Json.encodeToJsonElement(user).jsonObject) }
    }
    override fun observeUsers(onUpdate: (List<User>) -> Unit) {
        scope.launch { onUpdate(fetchUsers()) }
    }
    override fun observeUsersIncremental(since: Long, onUpdate: (List<User>) -> Unit) {
        scope.launch { 
            var lastSeen = since
            while(true) { 
                val news = fetchUsersIncremental(lastSeen)
                if (news.isNotEmpty()) {
                    onUpdate(news)
                    lastSeen = news.maxOf { it.lastUpdated }
                }
                delay(PULSE_NORMAL) 
            } 
        }
    }

    override fun syncStockMovement(movement: StockMovement) {
        scope.launch { patchDocument("movements", movement.id.toString(), Json.encodeToJsonElement(movement).jsonObject) }
    }
    override fun observeStockMovements(onUpdate: (List<StockMovement>) -> Unit) {}

    override fun syncBranch(branch: Branch) {
        scope.launch { patchDocument("branches", branch.id, Json.encodeToJsonElement(branch).jsonObject) }
    }
    override fun deleteBranch(id: String) {
        scope.launch { deleteDocument("branches", id) }
    }
    override fun observeBranches(onUpdate: (List<Branch>) -> Unit) {
        scope.launch { onUpdate(fetchBranches()) }
    }
    override fun observeBranchesIncremental(since: Long, onUpdate: (List<Branch>) -> Unit) {
        scope.launch { 
            var lastSeen = since
            while(true) { 
                val news = fetchBranchesIncremental(lastSeen)
                if (news.isNotEmpty()) {
                    onUpdate(news)
                    lastSeen = news.maxOf { it.lastUpdated }
                }
                delay(PULSE_NORMAL) 
            } 
        }
    }

    override fun syncTerminal(terminal: PosTerminal) {
        scope.launch { patchDocument("terminals", terminal.id, Json.encodeToJsonElement(terminal).jsonObject) }
    }
    override fun deleteTerminal(id: String) {
        scope.launch { deleteDocument("terminals", id) }
    }
    override fun observeTerminals(branchId: String, onUpdate: (List<PosTerminal>) -> Unit) {
        scope.launch { while(true) { onUpdate(fetchTerminals(branchId)); delay(PULSE_NORMAL) } }
    }

    override fun syncHeldSale(heldSale: HeldSale) {}
    override fun deleteHeldSale(id: String) {}
    override fun observeHeldSales(branchId: String, onUpdate: (List<HeldSale>) -> Unit) {}

    override fun syncReturn(productReturn: ProductReturn) {}
    override fun observeReturns(branchId: String, onUpdate: (List<ProductReturn>) -> Unit) {}

    override fun syncCashOut(cashOut: CashOut) {
        scope.launch { patchDocument("cash_outs", cashOut.id, Json.encodeToJsonElement(cashOut).jsonObject) }
    }
    override fun observeCashOuts(branchId: String, onUpdate: (List<CashOut>) -> Unit) {
        scope.launch { while(true) { onUpdate(fetchCashOuts(branchId)); delay(PULSE_NORMAL) } }
    }

    override fun syncCashMovement(movement: CashMovement) {
        scope.launch { patchDocument("cash_movements", movement.id, Json.encodeToJsonElement(movement).jsonObject) }
    }
    override fun observeCashMovements(branchId: String, onUpdate: (List<CashMovement>) -> Unit) {
        scope.launch { while(true) { onUpdate(fetchCashMovements(branchId)); delay(PULSE_NORMAL) } }
    }

    override fun syncPreCut(preCut: PreCut) {
        scope.launch { patchDocument("pre_cuts", preCut.id, Json.encodeToJsonElement(preCut).jsonObject) }
    }
    override fun observePreCuts(branchId: String, onUpdate: (List<PreCut>) -> Unit) {
        scope.launch { while(true) { onUpdate(fetchPreCuts(branchId)); delay(PULSE_NORMAL) } }
    }

    override fun syncWebOrder(order: WebOrder) {
        scope.launch { patchDocument("web_orders", order.id, Json.encodeToJsonElement(order).jsonObject) }
    }
    override fun observeWebOrders(branchId: String, onUpdate: (List<WebOrder>) -> Unit) {
        scope.launch { while(true) { onUpdate(queryDocuments("web_orders", branchId).map { Json.decodeFromJsonElement(it) }); delay(PULSE_FAST) } }
    }

    override fun syncDeletionRequest(request: DeletionRequest) {
        scope.launch { patchDocument("deletion_requests", request.id, Json.encodeToJsonElement(request).jsonObject) }
    }
    override fun deleteDeletionRequest(id: String) {
        scope.launch { deleteDocument("deletion_requests", id) }
    }
    override fun observeDeletionRequests(branchId: String, onUpdate: (List<DeletionRequest>) -> Unit) {
        scope.launch { while(true) { onUpdate(queryDocuments("deletion_requests", branchId).map { Json.decodeFromJsonElement(it) }); delay(PULSE_FAST) } }
    }

    override fun syncDeletionLog(log: DeletionLog) {
        scope.launch { patchDocument("deletion_logs", log.id, Json.encodeToJsonElement(log).jsonObject) }
    }
    override fun observeDeletionLogs(branchId: String, onUpdate: (List<DeletionLog>) -> Unit) {
        scope.launch { while(true) { onUpdate(queryDocuments("deletion_logs", branchId).map { Json.decodeFromJsonElement(it) }); delay(PULSE_FAST) } }
    }

    override fun syncAttendance(record: AttendanceRecord) {
        scope.launch { patchDocument("attendance", record.id.toString(), Json.encodeToJsonElement(record).jsonObject) }
    }
    override fun syncSchedule(schedule: Schedule) {
        scope.launch { patchDocument("schedules", schedule.id.toString(), Json.encodeToJsonElement(schedule).jsonObject) }
    }

    override fun syncEmployee(employee: Employee) {
        scope.launch { patchDocument("employees", employee.id.toString(), Json.encodeToJsonElement(employee).jsonObject) }
    }

    override fun deleteEmployee(id: Long) {
        scope.launch { deleteDocument("employees", id.toString()) }
    }

    override fun syncInventory(inventory: Inventory) {
        scope.launch { patchDocument("inventory", "${inventory.branchId}_${inventory.productId}", Json.encodeToJsonElement(inventory).jsonObject) }
    }
    override fun syncInventoryBatch(branchId: String, items: List<Inventory>) {
        items.forEach { syncInventory(it) }
    }
    override fun observeInventoryIncremental(branchId: String, since: Long, onUpdate: (List<Inventory>) -> Unit) {
        scope.launch { 
            var lastSeen = since
            while(true) { 
                val news = fetchInventoryIncremental(branchId, lastSeen)
                if (news.isNotEmpty()) {
                    onUpdate(news)
                    lastSeen = news.maxOf { it.lastUpdated }
                }
                delay(PULSE_FAST) 
            } 
        }
    }

    override fun syncProductBatch(products: List<Product>) {
        products.forEach { syncProduct(it) }
    }

    override fun syncAiConfig(enabled: Boolean) {}

    override fun syncGlobalAds(urls: List<String>) {}
    override fun observeGlobalAds(onUpdate: (List<String>) -> Unit) {}

    override suspend fun fetchProducts(): List<Product> = listDocuments("products").map { Json.decodeFromJsonElement(it) }
    override suspend fun fetchProductsIncremental(since: Long): List<Product> = queryIncremental("products", since).map { Json.decodeFromJsonElement(it) }
    
    override suspend fun fetchSales(branchId: String): List<Sale> = queryDocuments("sales", branchId).map { Json.decodeFromJsonElement(it) }
    
    override suspend fun fetchCashOuts(branchId: String): List<CashOut> = queryDocuments("cash_outs", branchId).map { Json.decodeFromJsonElement(it) }
    
    override suspend fun fetchCashMovements(branchId: String): List<CashMovement> = queryDocuments("cash_movements", branchId).map { Json.decodeFromJsonElement(it) }
    
    override suspend fun fetchPreCuts(branchId: String): List<PreCut> = queryDocuments("pre_cuts", branchId).map { Json.decodeFromJsonElement(it) }
    override suspend fun fetchDeletionLogs(branchId: String): List<DeletionLog> = queryDocuments("deletion_logs", branchId).map { Json.decodeFromJsonElement(it) }
    override suspend fun fetchReturns(branchId: String): List<ProductReturn> = queryDocuments("returns", branchId).map { Json.decodeFromJsonElement(it) }
    
    override suspend fun fetchInventory(branchId: String): List<Inventory> = queryDocuments("inventory", branchId).map { Json.decodeFromJsonElement(it) }
    override suspend fun fetchInventoryIncremental(branchId: String, since: Long): List<Inventory> = queryIncremental("inventory", since, branchId).map { Json.decodeFromJsonElement(it) }

    override suspend fun fetchAttendance(userId: String): List<AttendanceRecord> {
        return queryDocuments("attendance").mapNotNull { 
            try { Json.decodeFromJsonElement<AttendanceRecord>(it) } catch(e: Exception) { null }
        }.filter { it.userId == userId }
    }

    override suspend fun fetchSchedules(employeeId: Long): List<Schedule> {
        return queryDocuments("schedules").mapNotNull {
            try { Json.decodeFromJsonElement<Schedule>(it) } catch(e: Exception) { null }
        }.filter { it.employeeId == employeeId }
    }

    override suspend fun fetchEmployees(): List<Employee> {
        return queryDocuments("employees").mapNotNull {
            try { Json.decodeFromJsonElement<Employee>(it) } catch(e: Exception) { null }
        }
    }

    override suspend fun fetchUsers(): List<User> = listDocuments("users").map { Json.decodeFromJsonElement(it) }
    suspend fun fetchUsersIncremental(since: Long): List<User> = queryIncremental("users", since).map { Json.decodeFromJsonElement(it) }

    override suspend fun fetchBranches(): List<Branch> = listDocuments("branches").map { Json.decodeFromJsonElement(it) }
    suspend fun fetchBranchesIncremental(since: Long): List<Branch> = queryIncremental("branches", since).map { Json.decodeFromJsonElement(it) }

    override suspend fun fetchCustomers(): List<Customer> = listDocuments("customers").map { Json.decodeFromJsonElement(it) }
    suspend fun fetchCustomersIncremental(since: Long): List<Customer> = queryIncremental("customers", since).map { Json.decodeFromJsonElement(it) }

    override suspend fun fetchTerminals(branchId: String): List<PosTerminal> = queryDocuments("terminals", branchId).map { Json.decodeFromJsonElement(it) }
    
    private suspend fun queryIncremental(collection: String, since: Long, branchId: String? = null): List<JsonObject> {
        return try {
            val url = "$baseUrl:runQuery"
            val response: JsonArray = httpClient.post(url) {
                authHeader()
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject {
                    put("structuredQuery", buildJsonObject {
                        put("from", buildJsonArray { add(buildJsonObject { put("collectionId", collection) }) })
                        
                        val filters = mutableListOf<JsonObject>()
                        
                        filters.add(buildJsonObject {
                            put("fieldFilter", buildJsonObject {
                                put("field", buildJsonObject { put("fieldPath", "lastUpdated") })
                                put("op", "GREATER_THAN")
                                put("value", buildJsonObject { put("integerValue", since) })
                            })
                        })
                        
                        if (branchId != null) {
                            filters.add(buildJsonObject {
                                put("fieldFilter", buildJsonObject {
                                    put("field", buildJsonObject { put("fieldPath", "branchId") })
                                    put("op", "EQUAL")
                                    put("value", buildJsonObject { put("stringValue", branchId) })
                                })
                            })
                        }
                        
                        if (filters.size > 1) {
                            put("where", buildJsonObject {
                                put("compositeFilter", buildJsonObject {
                                    put("op", "AND")
                                    put("filters", buildJsonArray { filters.forEach { add(it) } })
                                })
                            })
                        } else {
                            put("where", filters[0])
                        }
                    })
                })
            }.body()

            response.mapNotNull { element ->
                try {
                    val doc = element.jsonObject["document"]?.jsonObject ?: return@mapNotNull null
                    doc.fromFirestoreFields()
                } catch (e: Exception) { null }
            }
        } catch (e: Exception) { emptyList() }
    }
}

actual fun getCloudProvider(): CloudProvider = JvmCloudProvider()
