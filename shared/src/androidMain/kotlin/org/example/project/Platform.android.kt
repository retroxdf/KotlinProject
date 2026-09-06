package com.abtsplazita.posplazita

import android.os.Build
import com.google.firebase.FirebaseApp
import com.abtsplazita.posplazita.data.local.databaseContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.bluetooth.BluetoothAdapter
import android.media.ToneGenerator
import android.media.AudioManager

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun isBluetoothSupported(): Boolean {
    return try {
        BluetoothAdapter.getDefaultAdapter() != null
    } catch (e: Exception) {
        false
    }
}

actual fun playErrorSound() {
    try {
        val toneG = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
    } catch (e: Exception) {
    }
}

actual fun initializeFirebase() {
    try {
        val context = databaseContext
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
            println("FIREBASE_ANDROID: Inicializado manualmente.")
        }
    } catch (e: Exception) {
        println("FIREBASE_ANDROID_ERROR: ${e.message}")
    }
}

actual fun isFirebaseInitialized(): Boolean {
    return try {
        val context = databaseContext
        FirebaseApp.getApps(context).isNotEmpty()
    } catch (e: Exception) {
        false
    }
}

@Composable
actual fun rememberFilePicker(): FilePicker {
    val context = LocalContext.current
    var onFilePickedCallback by remember { mutableStateOf<((ByteArray?) -> Unit)?>(null) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                onFilePickedCallback?.invoke(bytes)
            } catch (e: Exception) {
                onFilePickedCallback?.invoke(null)
            }
        } else {
            onFilePickedCallback?.invoke(null)
        }
    }
    
    return remember {
        object : FilePicker {
            override fun pickFile(onFilePicked: (ByteArray?) -> Unit) {
                onFilePickedCallback = onFilePicked
                launcher.launch("*/*")
            }
        }
    }
}

actual fun shareText(text: String, phone: String) {
    val context = databaseContext
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(Intent.createChooser(intent, "Compartir").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}

actual fun generateAndShareDebtPdf(data: com.abtsplazita.posplazita.ui.customers.DebtReportData, phone: String) {
    // Implementación básica compartiendo texto por ahora
    val sb = StringBuilder()
    sb.append("ESTADO DE CUENTA - ${data.customer.name}\n\n")
    data.sales.forEach { (sale, items) ->
        sb.append("Ticket: ${sale.id} - $${sale.total}\n")
        items.forEach { sb.append("  - ${it.productName} x ${it.quantity}\n") }
    }
    sb.append("\nSALDO TOTAL: $${data.totalDebt}")
    shareText(sb.toString(), phone)
}
