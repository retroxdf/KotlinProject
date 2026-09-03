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

/**
 * Formatea un peso o cantidad sin redondear a 0.50.
 * Muestra hasta 3 decimales si es necesario.
 */
fun Double.formatWeight(): String {
    val stringVal = this.toString()
    return if (stringVal.contains(".")) {
        val parts = stringVal.split(".")
        val integerPart = parts[0]
        val decimalPart = parts[1]
        if (decimalPart.length > 3) {
            "$integerPart.${decimalPart.take(3)}"
        } else {
            stringVal
        }
    } else {
        stringVal
    }
}

/**
 * Calcula la utilidad (markup) basado en el costo y precio.
 * Retorna el porcentaje sobre el costo (Markup).
 * Ejemplo: Costo 10, Precio 13 -> 30%
 */
fun calculateUtility(cost: Double, price: Double): Double {
    if (cost <= 0) return 0.0
    return ((price - cost) / cost) * 100.0
}

/**
 * Calcula el precio sugerido basado en un porcentaje de utilidad (markup) deseado.
 * Price = Cost * (1 + Markup)
 */
fun calculatePriceFromUtility(cost: Double, utilityPercent: Double): Double {
    val markup = utilityPercent / 100.0
    return (cost * (1.0 + markup)).roundToNearestHalf()
}

/**
 * Calcula el Precio 1 (Mayoreo) basado en el costo según las reglas del negocio.
 */
fun calculateDefaultPrice1(cost: Double): Double {
    val utility = when {
        cost <= 10.0 -> 17.0
        cost <= 20.0 -> 15.0
        cost <= 30.0 -> 10.0
        cost <= 40.0 -> 9.0
        cost <= 50.0 -> 8.0
        cost <= 60.0 -> 7.0
        cost <= 70.0 -> 6.0
        cost <= 80.0 -> 5.0
        cost <= 90.0 -> 5.0 
        cost <= 110.0 -> 4.0
        cost <= 500.0 -> 3.0
        else -> 3.0
    }
    return calculatePriceFromUtility(cost, utility)
}

/**
 * Calcula el Precio 2 (Público - DEFAULT) basado en el costo según las reglas del negocio (Markup escalonado).
 */
fun calculateDefaultPrice2(cost: Double): Double {
    val utility = when {
        cost <= 30.0 -> 30.0
        cost <= 50.0 -> 25.0
        cost <= 80.0 -> 20.0
        cost <= 100.0 -> 15.0
        cost <= 200.0 -> 13.0
        cost <= 500.0 -> 10.0
        else -> 10.0
    }
    return calculatePriceFromUtility(cost, utility)
}

/**
 * Calcula el Precio 3 (Público + 0.50) basado en el Precio 2.
 */
fun calculateDefaultPrice3(cost: Double): Double {
    val p2 = calculateDefaultPrice2(cost)
    return p2 + 0.50
}
