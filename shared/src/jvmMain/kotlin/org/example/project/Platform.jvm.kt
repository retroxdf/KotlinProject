package com.abtsplazita.posplazita

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.io.File
import java.awt.FileDialog
import java.awt.Frame

actual fun getPlatform(): Platform = object : Platform {
    override val name: String = "Java " + System.getProperty("java.version")
}

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun isBluetoothSupported(): Boolean = true // En desktop usualmente hay soporte o dongles

actual fun playErrorSound() {
    try {
        java.awt.Toolkit.getDefaultToolkit().beep()
    } catch (e: Exception) {
        // Silencioso si falla
    }
}

actual fun initializeFirebase() {
    // Desactivado en PC para evitar conflicto de librerías de Android
}

actual fun isFirebaseInitialized(): Boolean = false

class JvmFilePicker(private val window: Frame?) : FilePicker {
    override fun pickFile(onFilePicked: (ByteArray?) -> Unit) {
        val fileDialog = FileDialog(window, "Seleccionar archivo CSV", FileDialog.LOAD)
        fileDialog.file = "*.csv"
        fileDialog.isVisible = true
        
        val file = fileDialog.file
        val directory = fileDialog.directory
        
        if (file != null && directory != null) {
            val selectedFile = File(directory, file)
            onFilePicked(selectedFile.readBytes())
        } else {
            onFilePicked(null)
        }
    }
}

@Composable
actual fun rememberFilePicker(): FilePicker {
    return remember { JvmFilePicker(null) }
}

actual fun shareText(text: String, phone: String) {
    try {
        val encodedText = text.replace(" ", "%20").replace("\n", "%0A")
        val cleanPhone = phone.replace("[^0-9]".toRegex(), "")
        val url = if (cleanPhone.isNotBlank()) {
            "https://wa.me/$cleanPhone?text=$encodedText"
        } else {
            "mailto:?body=$encodedText"
        }
        
        val os = System.getProperty("os.name").lowercase()
        if (os.contains("win")) {
            Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler $url")
        } else if (os.contains("mac")) {
            Runtime.getRuntime().exec("open $url")
        } else {
            Runtime.getRuntime().exec("xdg-open $url")
        }
    } catch (e: Exception) {
        println("Error al compartir: ${e.message}")
    }
}

actual fun generateAndShareDebtPdf(data: com.abtsplazita.posplazita.ui.customers.DebtReportData, phone: String) {
    // En Desktop, usamos el fallback de texto por ahora
    val sb = StringBuilder()
    sb.append("ESTADO DE CUENTA - ${data.customer.name}\n\n")
    data.sales.forEach { (sale, items) ->
        sb.append("Ticket: ${sale.id} - $${sale.total}\n")
        items.forEach { sb.append("  - ${it.productName} x ${it.quantity}\n") }
    }
    sb.append("\nSALDO TOTAL: $${data.totalDebt}")
    shareText(sb.toString(), phone)
}
