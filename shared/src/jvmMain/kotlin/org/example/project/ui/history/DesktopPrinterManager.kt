package com.abtsplazita.posplazita.ui.history

import com.abtsplazita.posplazita.domain.Sale
import com.abtsplazita.posplazita.domain.SaleItem
import com.abtsplazita.posplazita.domain.Customer
import com.abtsplazita.posplazita.domain.TicketConfig
import com.abtsplazita.posplazita.domain.TicketElementType
import com.fazecast.jSerialComm.SerialPort
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class DesktopPrinterManager : PrinterManager {
    private var printerName: String = ""
    private var connectionType: String = ""
    private var address: String = ""
    private var paperSize: Int = 80
    private var autoCut: Boolean = true
    private var openDrawerOnPrint: Boolean = true

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
        if (connectionType == "BLUETOOTH" || connectionType == "SERIAL") {
            sendEscPos { outputStream ->
                // Abrir Cajón
                if (openDrawer && openDrawerOnPrint) {
                    outputStream.write(byteArrayOf(0x1B, 0x70, 0x00, 0x19, 0xFA.toByte()))
                }

                val lineChars = if (paperSize == 58) 30 else 40
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                val dateStr = sdf.format(Date(sale.timestamp))
                
                val content = buildTicketContentCommon(sale, items, comment, walletBalance, config, lineChars, dateStr, branchName)
                
                // Usamos ISO-8859-1 que es compatible con la mayoría de impresoras térmicas para caracteres latinos
                outputStream.write(content.toByteArray(Charsets.ISO_8859_1))
                outputStream.write(byteArrayOf(0x0A, 0x0A, 0x0A, 0x0A)) // Espacio final

                // Corte de papel si está habilitado
                if (autoCut) {
                    outputStream.write(byteArrayOf(0x1D, 0x56, 0x00))
                }
            }
        } else if (connectionType == "SYSTEM") {
            sendToSystemPrinter(sale, items, openDrawer, comment, walletBalance, config, branchName)
        } else {
            println("Desktop: Ticket a $printerName | Abrir Cajón: $openDrawer | Saldo Monedero: $walletBalance | Comentario: $comment")
            if (config != null) println("Config: $config")
        }
    }

    private fun sendToSystemPrinter(
        sale: Sale, 
        items: List<SaleItem>, 
        openDrawer: Boolean, 
        comment: String?, 
        walletBalance: Double?,
        config: TicketConfig?,
        branchName: String?
    ) {
        try {
            val services = javax.print.PrintServiceLookup.lookupPrintServices(null, null)
            val selectedService = services.find { it.name == printerName } ?: return
            
            val lineChars = if (paperSize == 58) 30 else 40
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            val dateStr = sdf.format(Date(sale.timestamp))
            
            val content = buildTicketContentCommon(sale, items, comment, walletBalance, config, lineChars, dateStr, branchName)
            
            val sb = StringBuilder()
            // No enviamos ESC@ a impresoras de sistema a menos que sea necesario, 
            // para evitar que se imprima un '?' al inicio.
            
            sb.append(content)
            sb.append("\n\n\n\n")
            
            val bytes = sb.toString().toByteArray(Charsets.ISO_8859_1)
            val docFlavor = javax.print.DocFlavor.BYTE_ARRAY.AUTOSENSE
            val doc = javax.print.SimpleDoc(bytes, docFlavor, null)
            val job = selectedService.createPrintJob()
            job.print(doc, null)
            
        } catch (e: Exception) {
            println("Desktop: Error al imprimir vía sistema: ${e.message}")
        }
    }

    override fun printMemberCard(customer: Customer) {
        if (connectionType == "BLUETOOTH" || connectionType == "SERIAL") {
            sendEscPos { outputStream ->
                val sb = StringBuilder()
                sb.append("\n\n")
                sb.append("   TARJETA DEL CLIENTE\n")
                sb.append("--------------------------------\n")
                sb.append("CLIENTE: ${customer.name}\n")
                sb.append("ID:      ${customer.id}\n")
                sb.append("\n")
                
                val barcodeContent = "CLI-${customer.id}"
                
                outputStream.write(byteArrayOf(0x1B, 0x61, 0x01)) // Centrar
                outputStream.write(byteArrayOf(0x1D, 0x68, 0x50)) // Altura
                
                val barcodeData = byteArrayOf(0x1D, 0x6B, 0x49, barcodeContent.length.toByte()) + barcodeContent.toByteArray()
                outputStream.write(barcodeData)
                
                sb.append("\n$barcodeContent\n")
                sb.append("--------------------------------\n")
                sb.append("\n\n\n\n")
                
                outputStream.write(sb.toString().toByteArray(Charsets.US_ASCII))
                outputStream.write(byteArrayOf(0x1B, 0x61, 0x00)) // Reset
            }
        } else {
            println("Desktop: Imprimiendo tarjeta del cliente para ${customer.name} | Código: CLI-${customer.id}")
        }
    }

    override fun printMemberCardGraphic(customer: Customer) {
        println("Desktop: Iniciando impresión gráfica de gafete para ${customer.name}")
        try {
            val job = java.awt.print.PrinterJob.getPrinterJob()
            job.setPrintable { graphics, pageFormat, pageIndex ->
                if (pageIndex > 0) {
                    java.awt.print.Printable.NO_SUCH_PAGE
                } else {
                    val g2d = graphics as java.awt.Graphics2D
                    g2d.translate(pageFormat.imageableX, pageFormat.imageableY)
                    
                    val margin = 20
                    val width = 300
                    val height = 450
                    
                    g2d.color = java.awt.Color.WHITE
                    g2d.fillRect(margin, margin, width, height)
                    g2d.color = java.awt.Color.BLACK
                    g2d.drawRect(margin, margin, width, height)
                    
                    g2d.font = java.awt.Font("Arial", java.awt.Font.BOLD, 24)
                    g2d.drawString("PLAZITA POS", margin + 70, margin + 50)
                    
                    g2d.font = java.awt.Font("Arial", java.awt.Font.PLAIN, 18)
                    g2d.drawString(customer.name.uppercase(), margin + 20, margin + 150)
                    g2d.drawString("ID: ${customer.id}", margin + 20, margin + 180)
                    
                    g2d.font = java.awt.Font("Arial", java.awt.Font.BOLD, 30)
                    g2d.drawString("||||| CLI-${customer.id} |||||", margin + 20, margin + 300)
                    
                    g2d.font = java.awt.Font("Arial", java.awt.Font.ITALIC, 14)
                    g2d.drawString("TARJETA DEL CLIENTE", margin + 80, margin + 400)
                    
                    java.awt.print.Printable.PAGE_EXISTS
                }
            }
            if (job.printDialog()) job.print()
        } catch (e: Exception) {
            println("Desktop: Error al imprimir gafete: ${e.message}")
        }
    }

    override fun openDrawer() {
        if (connectionType == "BLUETOOTH" || connectionType == "SERIAL") {
            sendEscPos { it.write(byteArrayOf(0x1B, 0x70, 0x00, 0x19, 0xFA.toByte())) }
        } else {
            println("Desktop: Abriendo cajón en $printerName")
        }
    }

    override fun printTestPage() {
        if (connectionType == "BLUETOOTH" || connectionType == "SERIAL") {
            sendEscPos {
                val test = "\n\n   PRUEBA DE IMPRESION\n   SISTEMA POS DESKTOP\n   CONEXION: OK\n\n\n\n"
                it.write(test.toByteArray(Charsets.US_ASCII))
            }
        } else {
            println("Desktop: Prueba a $printerName")
        }
    }

    override fun getPairedDevices(): List<Pair<String, String>> {
        return try {
            SerialPort.getCommPorts().map { it.descriptivePortName to it.systemPortName }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override fun getSystemPrinters(): List<String> {
        return try {
            javax.print.PrintServiceLookup.lookupPrintServices(null, null).map { it.name }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun sendEscPos(block: (OutputStream) -> Unit) {
        if (address.isBlank()) return
        try {
            val comPort = SerialPort.getCommPort(address)
            comPort.baudRate = 9600
            comPort.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 0)
            if (comPort.openPort()) {
                val outputStream = comPort.outputStream
                outputStream.write(byteArrayOf(0x1B, 0x40)) // Reset
                block(outputStream)
                outputStream.flush()
                comPort.closePort()
            } else {
                println("Desktop: No se pudo abrir el puerto $address")
            }
        } catch (e: Exception) {
            println("Desktop: Error en comunicación serial: ${e.message}")
        }
    }
}

actual fun getRealPrinterManager(): PrinterManager = DesktopPrinterManager()
