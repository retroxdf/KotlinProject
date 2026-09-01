package com.abtsplazita.posplazita

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.abtsplazita.posplazita.domain.formatPrice

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun isBluetoothSupported(): Boolean {
    return android.bluetooth.BluetoothAdapter.getDefaultAdapter() != null
}

actual fun playErrorSound() {
    try {
        val toneG = android.media.ToneGenerator(android.media.AudioManager.STREAM_ALARM, 100)
        toneG.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 200)
    } catch (e: Exception) {
        // Silencioso si falla
    }
}

actual fun initializeFirebase() {
    // En Android es automático vía plugin google-services
}

actual fun isFirebaseInitialized(): Boolean {
    return try {
        com.google.firebase.FirebaseApp.getInstance()
        true
    } catch (e: Exception) {
        false
    }
}

actual fun shareText(text: String, phone: String) {
    val context = com.abtsplazita.posplazita.data.local.currentActivity ?: return
    
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_TEXT, text)
        
        if (phone.isNotBlank()) {
            val cleanPhone = phone.replace("[^0-9]".toRegex(), "")
            if (cleanPhone.length >= 10) {
                setPackage("com.whatsapp")
                putExtra("jid", "$cleanPhone@s.whatsapp.net")
            }
        }
    }
    
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        val shareIntent = android.content.Intent.createChooser(
            android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_TEXT, text)
            }, 
            "Compartir"
        )
        context.startActivity(shareIntent)
    }
}

actual fun generateAndShareDebtPdf(data: com.abtsplazita.posplazita.ui.customers.DebtReportData, phone: String) {
    val context = com.abtsplazita.posplazita.data.local.currentActivity ?: return
    
    val pdfDocument = android.graphics.pdf.PdfDocument()
    val titlePaint = android.graphics.Paint().apply {
        isFakeBoldText = true
        textSize = 20f
    }
    val boldPaint = android.graphics.Paint().apply {
        isFakeBoldText = true
        textSize = 14f
    }
    val textPaint = android.graphics.Paint().apply {
        textSize = 12f
    }
    
    var pageNumber = 1
    var pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
    var page = pdfDocument.startPage(pageInfo)
    var canvas = page.canvas
    var y = 50f
    val margin = 50f
    
    // Encabezado
    canvas.drawText("ESTADO DE CUENTA DETALLADO", margin, y, titlePaint)
    y += 30f
    canvas.drawText("Cliente: ${data.customer.name}", margin, y, boldPaint)
    y += 20f
    canvas.drawText("Saldo Total: $${data.totalDebt.formatPrice()}", margin, y, boldPaint)
    y += 40f
    
    canvas.drawText("DETALLE DE TICKETS PENDIENTES:", margin, y, boldPaint)
    y += 25f
    
    data.sales.forEach { (sale, items) ->
        if (y > 750) {
            pdfDocument.finishPage(page)
            pageNumber++
            pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            y = 50f
        }
        
        val date = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date(sale.timestamp))
        canvas.drawText("Ticket: ${sale.id} - Fecha: $date - Total: $${sale.total.formatPrice()}", margin, y, boldPaint)
        y += 20f
        
        items.forEach { item ->
            val detailText = "  • ${item.productName} (${item.quantity} x $${item.priceAtSale.formatPrice()}) = $${item.subtotal.formatPrice()}"
            canvas.drawText(detailText, margin, y, textPaint)
            y += 15f
            
            if (y > 780) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = 50f
            }
        }
        y += 15f
    }
    
    y += 20f
    canvas.drawText("HISTORIAL DE ABONOS:", margin, y, boldPaint)
    y += 25f
    
    data.payments.forEach { pay ->
        if (y > 780) {
            pdfDocument.finishPage(page)
            pageNumber++
            pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            y = 50f
        }
        val date = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(pay.timestamp))
        canvas.drawText("• $date: -$${pay.amount.formatPrice()} (${pay.paymentMethod})", margin, y, textPaint)
        y += 15f
    }
    
    pdfDocument.finishPage(page)
    
    val fileName = "EstadoCuenta_${data.customer.name.replace(" ", "_")}.pdf"
    val file = java.io.File(context.cacheDir, fileName)
    try {
        pdfDocument.writeTo(java.io.FileOutputStream(file))
        pdfDocument.close()
        
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "com.abtsplazita.posplazita.fileprovider", file)
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            
            if (phone.isNotBlank()) {
                val cleanPhone = phone.replace("[^0-9]".toRegex(), "")
                if (cleanPhone.length >= 10) {
                    setPackage("com.whatsapp")
                    putExtra("jid", "$cleanPhone@s.whatsapp.net")
                }
            }
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Enviar PDF"))
        
    } catch (e: Exception) {
        println("Error PDF: ${e.message}")
    }
}

@Composable
actual fun rememberFilePicker(): FilePicker {
    val context = LocalContext.current
    var callback by remember { mutableStateOf<((ByteArray?) -> Unit)?>(null) }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                callback?.invoke(bytes)
            } catch (e: Exception) {
                callback?.invoke(null)
            }
        } else {
            callback?.invoke(null)
        }
    }
    
    return remember { 
        object : FilePicker {
            override fun pickFile(onFilePicked: (ByteArray?) -> Unit) {
                callback = onFilePicked
                launcher.launch("*/*")
            }
        }
    }
}
