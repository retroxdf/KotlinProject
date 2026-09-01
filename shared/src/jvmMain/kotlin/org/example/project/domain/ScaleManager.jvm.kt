package com.abtsplazita.posplazita.domain

import com.fazecast.jSerialComm.SerialPort

class JvmScaleManager : ScaleManager {
    private var serialPort: SerialPort? = null

    override fun connect(port: String, baudRate: Int, sequence: String, delay: Int): Boolean {
        disconnect()
        return try {
            val portObj = SerialPort.getCommPort(port)
            portObj.baudRate = baudRate
            portObj.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 1000, 0)
            
            if (portObj.openPort()) {
                serialPort = portObj
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    override fun disconnect() {
        serialPort?.closePort()
        serialPort = null
    }

    override fun readWeight(): Double? {
        val port = serialPort ?: return null
        if (!port.isOpen) return null

        return try {
            // Enviar comando 'P' (estándar Rhino BAR8)
            val output = port.outputStream
            output.write('P'.code)
            output.flush()

            Thread.sleep(150) // Esperar respuesta

            val input = port.inputStream
            val available = input.available()
            if (available > 0) {
                val buffer = ByteArray(available)
                val read = input.read(buffer)
                if (read > 0) {
                    val response = String(buffer, 0, read).trim()
                    // Limpiar respuesta para obtener solo el número
                    // Rhino suele responder algo como "0.000kg" o similar
                    val cleanWeight = response.replace("[^0-9.]".toRegex(), "")
                    cleanWeight.toDoubleOrNull()
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    override fun isConnected(): Boolean = serialPort?.isOpen ?: false

    override fun getAvailablePorts(): List<String> {
        return SerialPort.getCommPorts().map { it.systemPortName }
    }
}

actual fun getScaleManager(): ScaleManager = JvmScaleManager()
