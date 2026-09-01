package com.abtsplazita.posplazita.domain

import kotlin.math.round

/**
 * Redondea un valor al múltiplo de 0.50 más cercano.
 * Ejemplo: 18.30 -> 18.50, 18.20 -> 18.00
 */
fun Double.roundToNearestHalf(): Double {
    return round(this * 2.0) / 2.0
}

/**
 * Formatea un valor Double a un String con exactamente 2 decimales.
 * Útil para visualización consistente (ej. 2.00 en lugar de 2.0).
 */
fun Double.formatPrice(): String {
    val rounded = this.roundToNearestHalf()
    val stringVal = rounded.toString()
    
    // Manejo básico de formato para asegurar .XX
    return if (stringVal.contains(".")) {
        val parts = stringVal.split(".")
        val integerPart = parts[0]
        val decimalPart = parts[1]
        if (decimalPart.length == 1) {
            "$integerPart.${decimalPart}0"
        } else if (decimalPart.length > 2) {
            "$integerPart.${decimalPart.take(2)}"
        } else {
            stringVal
        }
    } else {
        "$stringVal.00"
    }
}
