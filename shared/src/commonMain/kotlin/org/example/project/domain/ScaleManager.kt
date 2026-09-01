package com.abtsplazita.posplazita.domain

interface ScaleManager {
    fun connect(port: String, baudRate: Int, sequence: String, delay: Int): Boolean
    fun disconnect()
    fun readWeight(): Double?
    fun isConnected(): Boolean
    fun getAvailablePorts(): List<String>
}

expect fun getScaleManager(): ScaleManager
