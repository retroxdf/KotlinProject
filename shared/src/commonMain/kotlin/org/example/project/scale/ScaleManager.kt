package com.abtsplazita.posplazita.scale

import kotlinx.coroutines.flow.StateFlow

interface ScaleManager {
    /**
     * Peso actual en kilogramos.
     */
    val currentWeight: StateFlow<Double>

    /**
     * Estado de la conexión con la báscula.
     */
    val isConnected: StateFlow<Boolean>

    /**
     * Inicia la búsqueda y conexión con la báscula (USB o Bluetooth).
     */
    fun connect()

    /**
     * Cierra la conexión.
     */
    fun disconnect()
}
