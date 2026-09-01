package com.abtsplazita.posplazita.domain

import com.abtsplazita.posplazita.currentTimeMillis
import com.abtsplazita.posplazita.domain.repository.ProductRepository

/**
 * Llaves estandar del sistema para el mapeo de columnas
 */
object ImportFields {
    const val BARCODE = "barcode"
    const val NAME = "name"
    const val CATEGORY = "category"
    const val UNIT = "unit"
    const val COST = "cost"
    const val PRICE = "price"
    const val STOCK = "stock"
    const val MIN_STOCK = "minStock"
    const val MAX_STOCK = "maxStock"
    const val SAT_CODE = "satCode"
    const val IS_BULK = "isBulk"
    const val USE_SCALE = "useScale"
    const val TAX_16 = "tax16"
    const val TAX_8 = "tax8"
}

class ProductImportManager(private val repository: ProductRepository) {

    /**
     * Obtiene los encabezados de la primera línea del CSV para que el usuario pueda mapearlos.
     */
    fun getCsvHeaders(csvContent: String): List<String> {
        val firstLine = csvContent.split("\n").firstOrNull() ?: ""
        return firstLine.split(",").map { it.trim().lowercase() }
    }

    /**
     * Importa productos utilizando un mapa donde:
     * Key: Campo del sistema (ej: ImportFields.NAME)
     * Value: Indice de la columna en el CSV (ej: 0, 1, 2...)
     */
    suspend fun importWithMapping(
        csvContent: String,
        branchId: String,
        mapping: Map<String, Int>
    ): Int {
        val lines = csvContent.split("\n").filter { it.isNotBlank() }
        if (lines.size <= 1) return 0
        
        val dataLines = lines.drop(1) // Ignoramos la cabecera
        var importedCount = 0

        val productsToSync = mutableListOf<Product>()
        val inventoryToSync = mutableListOf<Inventory>()

        for (line in dataLines) {
            val cells = line.split(",").map { it.trim() }
            
            try {
                // 1. Obtener valores segun el mapeo
                val barcode = cells.getOrNull(mapping[ImportFields.BARCODE] ?: -1)
                    ?.replace("E+12", "")?.replace(".", "") ?: continue
                
                if (barcode.isBlank()) continue

                val name = cells.getOrNull(mapping[ImportFields.NAME] ?: -1) ?: "Producto sin nombre"
                val category = cells.getOrNull(mapping[ImportFields.CATEGORY] ?: -1) ?: "General"
                val unitStr = cells.getOrNull(mapping[ImportFields.UNIT] ?: -1)?.uppercase() ?: "PZA"
                
                val rawCost = cells.getOrNull(mapping[ImportFields.COST] ?: -1)?.toDoubleOrNull() ?: 0.0
                val price1 = cells.getOrNull(mapping[ImportFields.PRICE] ?: -1)?.toDoubleOrNull() ?: 0.0
                
                val initialStock = cells.getOrNull(mapping[ImportFields.STOCK] ?: -1)?.toDoubleOrNull() ?: 0.0
                val minStock = cells.getOrNull(mapping[ImportFields.MIN_STOCK] ?: -1)?.toDoubleOrNull() ?: 0.0
                val maxStock = cells.getOrNull(mapping[ImportFields.MAX_STOCK] ?: -1)?.toDoubleOrNull() ?: 0.0

                val satCode = cells.getOrNull(mapping[ImportFields.SAT_CODE] ?: -1)
                val isBulk = cells.getOrNull(mapping[ImportFields.IS_BULK] ?: -1)?.lowercase() == "s"
                val useScale = cells.getOrNull(mapping[ImportFields.USE_SCALE] ?: -1)?.lowercase() == "s"

                // Lógica de Impuestos (IVA 16% o 8%)
                val hasTax16 = cells.getOrNull(mapping[ImportFields.TAX_16] ?: -1)?.lowercase() == "s"
                val hasTax8 = cells.getOrNull(mapping[ImportFields.TAX_8] ?: -1)?.lowercase() == "s"
                val taxValue = when {
                    hasTax16 -> 16.0
                    hasTax8 -> 8.0
                    else -> 0.0
                }

                // 2. Buscar si el producto ya existe
                val existing = repository.getProductByBarcode(barcode)
                
                val product = Product(
                    id = existing?.id ?: "P${currentTimeMillis()}_${importedCount}",
                    name = name,
                    barcode = barcode,
                    category = category,
                    unit = if (unitStr == "KG") UnitType.KG else UnitType.PIECE,
                    tax = taxValue,
                    cost = rawCost,
                    isBulk = isBulk,
                    useScale = useScale,
                    satCode = satCode,
                    price1 = price1,
                    price3 = price1, // Opcional: replicar precios
                    isService = false,
                    lastUpdated = currentTimeMillis()
                )

                // 3. Guardar localmente
                repository.saveProduct(product, syncWithCloud = false)
                
                repository.updateStock(
                    productId = product.id, 
                    branchId = branchId, 
                    newStock = initialStock,
                    reason = "Importación dinámica desde Excel",
                    syncWithCloud = false
                )
                
                repository.updateStockLimits(product.id, branchId, minStock, maxStock)

                productsToSync.add(product)
                inventoryToSync.add(Inventory(
                    productId = product.id,
                    branchId = branchId,
                    stock = initialStock,
                    minStock = minStock,
                    maxStock = maxStock,
                    lastUpdated = currentTimeMillis()
                ))

                importedCount++
            } catch (e: Exception) {
                println("IMPORT_ERROR en línea: $line -> ${e.message}")
            }
        }

        // 4. Sincronizar con la nube en lotes (Batches)
        if (productsToSync.isNotEmpty()) {
            repository.syncProductBatch(productsToSync)
        }
        if (inventoryToSync.isNotEmpty()) {
            repository.syncInventoryBatch(branchId, inventoryToSync)
        }

        return importedCount
    }
}
