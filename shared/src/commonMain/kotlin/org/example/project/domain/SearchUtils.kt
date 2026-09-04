package com.abtsplazita.posplazita.domain

/**
 * Normaliza una cadena para búsqueda:
 * 1. Pasa a minúsculas.
 * 2. Elimina acentos y diéresis comunes.
 */
fun String.normalizeForSearch(): String {
    return this.lowercase()
        .replace('á', 'a')
        .replace('é', 'e')
        .replace('í', 'i')
        .replace('ó', 'o')
        .replace('ú', 'u')
        .replace('ü', 'u')
        .replace('ñ', 'n')
        .replace('Á', 'a')
        .replace('É', 'e')
        .replace('Í', 'i')
        .replace('Ó', 'o')
        .replace('Ú', 'u')
        .replace('Ü', 'u')
        .replace('Ñ', 'n')
}
