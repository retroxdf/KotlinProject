package com.abtsplazita.posplazita.domain

class AndroidScaleManager : ScaleManager {
    override fun connect(port: String, baudRate: Int, sequence: String, delay: Int): Boolean = false
    override fun disconnect() {}
    override fun readWeight(): Double? = null
    override fun isConnected(): Boolean = false
    override fun getAvailablePorts(): List<String> = emptyList()
}

actual fun getScaleManager(): ScaleManager = AndroidScaleManager()
