package com.abtsplazita.posplazita.ui.history

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import com.abtsplazita.posplazita.domain.Sale
import com.abtsplazita.posplazita.domain.SaleItem
import com.abtsplazita.posplazita.domain.Customer
import com.abtsplazita.posplazita.domain.TicketConfig
import com.abtsplazita.posplazita.domain.formatPrice
import com.abtsplazita.posplazita.domain.TicketAlignment
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class AndroidPrinterManager : PrinterManager {
    private var printerName: String = ""
    private var connectionType: String = ""
    private var address: String = ""
    private var paperSize: Int = 80
    private var autoCut: Boolean = true
    private var openDrawerOnPrint: Boolean = true
    
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    override fun setConfig(
        name: String, 
        type: String, 
        address: String, 
        paperSize: Int, 
        autoCut: Boolean, 
        openDrawer: Boolean
    ) {
        this.printerName = name
        this.connectionType = type
        this.address = address
        this.paperSize = paperSize
        this.autoCut = autoCut
        this.openDrawerOnPrint = openDrawer
    }

    override fun printTicket(
        sale: Sale, 
        items: List<SaleItem>, 
        openDrawer: Boolean, 
        comment: String?, 
        walletBalance: Double?,
        config: TicketConfig?,
        branchName: String?
    ) {
        if (connectionType == "BLUETOOTH") {
            printViaBluetooth { outputStream ->
                if (openDrawer && openDrawerOnPrint) {
                    outputStream.write(byteArrayOf(0x1B, 0x70, 0x00, 0x19, 0xFA.toByte()))
                }

                val lineChars = if (paperSize == 58) 30 else 40
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                val dateStr = sdf.format(Date(sale.timestamp))
                
                val content = buildTicketContentCommon(sale, items, comment, walletBalance, config, lineChars, dateStr, branchName)
                
                outputStream.write(content.toByteArray(Charsets.ISO_8859_1))
                outputStream.write(byteArrayOf(0x0A, 0x0A, 0x0A, 0x0A))

                if (autoCut) {
                    outputStream.write(byteArrayOf(0x1D, 0x56, 0x00))
                }
            }
        }
    }

    override fun printDebtPayment(
        customer: Customer,
        amountPaid: Double,
        remainingDebt: Double,
        config: TicketConfig?,
        branchName: String?
    ) {
        if (connectionType == "BLUETOOTH") {
            printViaBluetooth { outputStream ->
                if (openDrawerOnPrint) {
                    outputStream.write(byteArrayOf(0x1B, 0x70, 0x00, 0x19, 0xFA.toByte()))
                }

                val lineChars = if (paperSize == 58) 30 else 40
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                val dateStr = sdf.format(Date())
                
                val divider = "-".repeat(lineChars)
                val sb = StringBuilder()
                sb.append("\n")
                sb.append(alignText(branchName ?: "PLAZITA POS", lineChars, TicketAlignment.CENTER) + "\n")
                sb.append(divider + "\n")
                sb.append(alignText("COMPROBANTE DE ABONO", lineChars, TicketAlignment.CENTER) + "\n")
                sb.append(divider + "\n")
                sb.append("FECHA:   $dateStr\n")
                sb.append("CLIENTE: ${customer.name}\n")
                sb.append(divider + "\n")
                
                val prevDebtLabel = "SALDO ANTERIOR:"
                val prevDebtVal = "$${(remainingDebt + amountPaid).formatPrice()}"
                sb.append(prevDebtLabel.padEnd(lineChars - prevDebtVal.length) + prevDebtVal + "\n")
                
                val paidLabel = "MONTO ABONADO:"
                val paidVal = "$${amountPaid.formatPrice()}"
                sb.append(paidLabel.padEnd(lineChars - paidVal.length) + paidVal + "\n")
                
                sb.append(divider + "\n")
                
                val newDebtLabel = "NUEVO SALDO:"
                val newDebtVal = "$${remainingDebt.formatPrice()}"
                sb.append(newDebtLabel.padEnd(lineChars - newDebtVal.length) + newDebtVal + "\n")
                
                sb.append(divider + "\n")
                sb.append("\n" + alignText("GRACIAS POR SU PAGO", lineChars, TicketAlignment.CENTER) + "\n\n\n\n")

                outputStream.write(sb.toString().toByteArray(Charsets.ISO_8859_1))

                if (autoCut) {
                    outputStream.write(byteArrayOf(0x1D, 0x56, 0x00))
                }
            }
        }
    }

    override fun printMemberCard(customer: Customer) {
        if (connectionType == "BLUETOOTH") {
            printViaBluetooth { outputStream ->
                val sb = StringBuilder()
                sb.append("\n\n")
                sb.append("   TARJETA DEL CLIENTE\n")
                sb.append("--------------------------------\n")
                sb.append("CLIENTE: ${customer.name}\n")
                sb.append("ID:      ${customer.id}\n")
                sb.append("\n")
                
                val barcodeContent = "CLI-${customer.id}"
                outputStream.write(byteArrayOf(0x1B, 0x61, 0x01)) 
                outputStream.write(byteArrayOf(0x1D, 0x68, 0x50)) 
                val barcodeData = byteArrayOf(0x1D, 0x6B, 0x49, barcodeContent.length.toByte()) + barcodeContent.toByteArray()
                outputStream.write(barcodeData)
                
                sb.append("\n$barcodeContent\n")
                sb.append("--------------------------------\n")
                sb.append("\n\n\n\n")
                outputStream.write(sb.toString().toByteArray(Charsets.US_ASCII))
                outputStream.write(byteArrayOf(0x1B, 0x61, 0x00))
            }
        }
    }

    override fun printMemberCardGraphic(customer: Customer) {
        val activity = com.abtsplazita.posplazita.data.local.currentActivity ?: return
        val htmlContent = """
            <html>
            <body style="text-align:center; padding: 20px; font-family: Arial, sans-serif;">
                <div style="border: 3px solid black; padding: 30px; display: inline-block; border-radius: 15px; background: white;">
                    <h1 style="margin: 0; color: #673AB7;">PLAZITA POS</h1>
                    <div style="margin: 25px 0;">
                        <img src="https://bwipjs-api.metafloor.com/?bcid=code128&text=CLI-${customer.id}&scale=4&rotate=N&includetext" style="max-width: 100%;">
                    </div>
                    <h2 style="margin: 10px 0; font-size: 32px; text-transform: uppercase;">${customer.name}</h2>
                    <p style="margin: 5px 0; font-weight: bold; font-size: 18px;">ID: ${customer.id}</p>
                    <hr style="margin: 20px 0;">
                    <p style="font-size: 14px; color: #666;">TARJETA DEL CLIENTE</p>
                </div>
            </body>
            </html>
        """.trimIndent()

        activity.runOnUiThread {
            val webView = android.webkit.WebView(activity)
            webView.webViewClient = object : android.webkit.WebViewClient() {
                override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                    val printManager = activity.getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
                    val jobName = "Gafete_${customer.name}"
                    val printAdapter = webView.createPrintDocumentAdapter(jobName)
                    printManager.print(jobName, printAdapter, android.print.PrintAttributes.Builder().build())
                }
            }
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        }
    }

    override fun openDrawer() {
        if (connectionType == "BLUETOOTH") {
            printViaBluetooth { it.write(byteArrayOf(0x1B, 0x70, 0x00, 0x19, 0xFA.toByte())) }
        }
    }

    override fun printTestPage() {
        printViaBluetooth {
            val test = "\n\n   PRUEBA DE IMPRESION\n   SISTEMA POS ANDROID\n   CONEXION: OK\n\n\n\n"
            it.write(test.toByteArray(Charsets.US_ASCII))
        }
    }

    @SuppressLint("MissingPermission")
    override fun getPairedDevices(): List<Pair<String, String>> {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        return adapter.bondedDevices.map { it.name to it.address }
    }

    override fun getSystemPrinters(): List<String> = emptyList()

    @SuppressLint("MissingPermission")
    private fun printViaBluetooth(block: (OutputStream) -> Unit) {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return
        if (!adapter.isEnabled || address.isBlank()) return
        try {
            val device = adapter.getRemoteDevice(address)
            val socket: BluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            val outputStream: OutputStream = socket.outputStream
            outputStream.write(byteArrayOf(0x1B, 0x40)) 
            block(outputStream)
            outputStream.flush()
            socket.close()
        } catch (e: Exception) {
            println("Error BT: ${e.message}")
        }
    }
}

actual fun getRealPrinterManager(): PrinterManager = AndroidPrinterManager()
