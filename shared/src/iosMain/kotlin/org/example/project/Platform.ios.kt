package com.abtsplazita.posplazita

import platform.UIKit.UIDevice
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

actual fun isBluetoothSupported(): Boolean = true

actual fun playErrorSound() {
    // En iOS usualmente se usa AudioServicesPlaySystemSound
    // Para simplificar en este entorno lo dejamos preparado
}

actual fun initializeFirebase() {
    // En iOS es automático vía GoogleService-Info.plist si se usa el plugin
}

actual fun isFirebaseInitialized(): Boolean {
    // En iOS por ahora simplificamos
    return true
}

@Composable
actual fun rememberFilePicker(): FilePicker {
    return remember { 
        object : FilePicker {
            override fun pickFile(onFilePicked: (ByteArray?) -> Unit) {
                // TODO: Implementar UIDocumentPickerViewController
                onFilePicked(null)
            }
        }
    }
}

actual fun shareText(text: String, phone: String) {
    // TODO: Implementar UIActivityViewController en iOS
}

actual fun generateAndShareDebtPdf(data: com.abtsplazita.posplazita.ui.customers.DebtReportData, phone: String) {
    // TODO: Implementar generación de PDF en iOS
}
