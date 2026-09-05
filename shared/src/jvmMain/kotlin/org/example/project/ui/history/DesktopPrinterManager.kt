package com.abtsplazita.posplazita.ui.history

import com.abtsplazita.posplazita.domain.Sale
import com.abtsplazita.posplazita.domain.SaleItem
import com.abtsplazita.posplazita.domain.Customer
import com.abtsplazita.posplazita.domain.TicketConfig
import com.abtsplazita.posplazita.domain.TicketElementType
import com.abtsplazita.posplazita.domain.TicketAlignment
import com.abtsplazita.posplazita.domain.formatPrice
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
    private var drawerCommand: String = "EPSON_PIN2"

    override fun setConfig(
        name: String, 
        type: String, 
        address: String, 
        paperSize: Int, 
        autoCut: Boolean, 
        openDrawer: Boolean,
        drawerCommand: String
    ) {
        this.printerName = name
        this.connectionType = type
        this.address = address
        this.paperSize = paperSize
        this.autoCut = autoCut
        this.openDrawerOnPrint = openDrawer
        this.drawerCommand = drawerCommand
    }

    private fun getDrawerCommandBytes(): ByteArray {
        return when (drawerCommand) {
            "EPSON_PIN2" -> byteArrayOf(0x1B, 0x70, 0x00, 0x19, 0xFA.toByte())
            "EPSON_PIN5" -> byteArrayOf(0x1B, 0x70, 0x01, 0x19, 0xFA.toByte())
            "STAR" -> byteArrayOf(0x07)
            else -> byteArrayOf(0x1B, 0x70, 0x00, 0x19, 0xFA.toByte())
        }
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
        if (connectionType == "NETWORK" || connectionType == "BLUETOOTH" || connectionType == "SERIAL") {
            sendEscPos { outputStream ->
                // 1. Abrir Cajón (ANTES de imprimir)
                if (openDrawer && openDrawerOnPrint) {
                    outputStream.write(getDrawerCommandBytes())
                }

                val lineChars = if (paperSize == 58) 30 else 40
                val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm:ss a", Locale.getDefault())
                val dateStr = sdf.format(Date(sale.timestamp))
                
                val content = buildTicketContentCommon(sale, items, comment, walletBalance, config, lineChars, dateStr, branchName)
                
                // Usamos ISO-8859-1 para caracteres latinos
                outputStream.write(content.toByteArray(Charsets.ISO_8859_1))
                outputStream.write(byteArrayOf(0x0A, 0x0A, 0x0A, 0x0A)) // Espacio final

                // 2. Corte de papel
                if (autoCut) {
                    outputStream.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00))
                }
            }
        } else if (connectionType == "SYSTEM") {
            // Para impresoras con Driver (SYSTEM), enviamos cada etapa como un trabajo RAW independiente.
            // Esto evita que el spooler de Windows ignore comandos ESC/POS mezclados con texto.
            
            // 1. Abrir Cajón (ANTES de imprimir como se solicitó)
            if (openDrawer && openDrawerOnPrint) {
                openDrawer()
            }

            // 2. Imprimir contenido del ticket
            sendToSystemPrinter(sale, items, false, comment, walletBalance, config, branchName)

            // 3. Cortar papel
            if (autoCut) {
                sendRawToSystemPrinter(byteArrayOf(0x1D, 0x56, 0x42, 0x00))
            }
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
            
            val baos = java.io.ByteArrayOutputStream()
            
            // 1. Reset e Inicio de comandos
            baos.write(byteArrayOf(0x1B, 0x40))

            // 2. Abrir Cajón (ANTES de imprimir como se solicitó)
            if (openDrawer && openDrawerOnPrint) {
                baos.write(getDrawerCommandBytes())
            }

            // 3. Contenido del Ticket
            baos.write(content.toByteArray(Charsets.ISO_8859_1))
            baos.write(byteArrayOf(0x0A, 0x0A, 0x0A, 0x0A)) // Espacio final
            
            val bytes = baos.toByteArray()
            val docFlavor = javax.print.DocFlavor.BYTE_ARRAY.AUTOSENSE
            val doc = javax.print.SimpleDoc(bytes, docFlavor, null)
            val job = selectedService.createPrintJob()
            job.print(doc, null)
            
        } catch (e: Exception) {
            println("Desktop: Error al imprimir vía sistema: ${e.message}")
        }
    }

    override fun printDebtPayment(
        customer: Customer,
        amountPaid: Double,
        remainingDebt: Double,
        config: TicketConfig?,
        branchName: String?
    ) {
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

        val contentBytes = sb.toString().toByteArray(Charsets.ISO_8859_1)

        if (connectionType == "NETWORK" || connectionType == "BLUETOOTH" || connectionType == "SERIAL") {
            sendEscPos { outputStream ->
                if (openDrawerOnPrint) {
                    outputStream.write(getDrawerCommandBytes())
                }
                outputStream.write(contentBytes)
                if (autoCut) {
                    outputStream.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00))
                }
            }
        } else if (connectionType == "SYSTEM") {
            // Soporte RAW para Driver de Windows
            if (openDrawerOnPrint) openDrawer()
            sendRawToSystemPrinter(contentBytes)
            if (autoCut) sendRawToSystemPrinter(byteArrayOf(0x1D, 0x56, 0x42, 0x00))
        } else {
            println("Desktop: Abono de $amountPaid para ${customer.name}. Restante: $remainingDebt")
        }
    }

    override fun printMemberCard(customer: Customer) {
        val sb = StringBuilder()
        sb.append("\n\n")
        sb.append("   TARJETA DEL CLIENTE\n")
        sb.append("--------------------------------\n")
        sb.append("CLIENTE: ${customer.name}\n")
        sb.append("ID:      ${customer.id}\n")
        sb.append("\n")
        
        val barcodeContent = "CLI-${customer.id}"
        val barcodeCmd = byteArrayOf(0x1D, 0x68, 0x50) + // Altura
                        byteArrayOf(0x1D, 0x6B, 0x49, barcodeContent.length.toByte()) + barcodeContent.toByteArray()
        
        val footer = "\n$barcodeContent\n--------------------------------\n\n\n\n"

        if (connectionType == "NETWORK" || connectionType == "BLUETOOTH" || connectionType == "SERIAL") {
            sendEscPos { outputStream ->
                outputStream.write(sb.toString().toByteArray(Charsets.US_ASCII))
                outputStream.write(byteArrayOf(0x1B, 0x61, 0x01)) // Centrar
                outputStream.write(barcodeCmd)
                outputStream.write(byteArrayOf(0x1B, 0x61, 0x00)) // Reset
                outputStream.write(footer.toByteArray(Charsets.US_ASCII))
            }
        } else if (connectionType == "SYSTEM") {
            // Mandamos comandos formateados por separado para máxima compatibilidad
            sendRawToSystemPrinter(sb.toString().toByteArray(Charsets.ISO_8859_1))
            sendRawToSystemPrinter(byteArrayOf(0x1B, 0x61, 0x01)) // Centrar
            sendRawToSystemPrinter(barcodeCmd)
            sendRawToSystemPrinter(byteArrayOf(0x1B, 0x61, 0x00)) // Reset align
            sendRawToSystemPrinter(footer.toByteArray(Charsets.ISO_8859_1))
            if (autoCut) sendRawToSystemPrinter(byteArrayOf(0x1D, 0x56, 0x42, 0x00))
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
        if (connectionType == "NETWORK" || connectionType == "BLUETOOTH" || connectionType == "SERIAL") {
            sendEscPos { 
                it.write(getDrawerCommandBytes())
            }
        } else if (connectionType == "SYSTEM") {
            sendRawToSystemPrinter(getDrawerCommandBytes())
        } else {
            println("Desktop: Abriendo cajón en $printerName")
        }
    }

    override fun printTestPage() {
        if (connectionType == "NETWORK" || connectionType == "BLUETOOTH" || connectionType == "SERIAL") {
            sendEscPos {
                val test = "\n\n   PRUEBA DE IMPRESION\n   SISTEMA POS DESKTOP\n   CONEXION: OK\n\n\n\n"
                it.write(test.toByteArray(Charsets.US_ASCII))
                
                if (autoCut) {
                    it.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00))
                }
            }
        } else if (connectionType == "SYSTEM") {
            val test = "\n\n   PRUEBA DE IMPRESION\n   SISTEMA POS DESKTOP (DRIVER)\n   CONEXION: OK\n\n\n\n"
            var bytes = test.toByteArray(Charsets.US_ASCII)
            if (autoCut) {
                bytes += byteArrayOf(0x1D, 0x56, 0x42, 0x00)
            }
            sendRawToSystemPrinter(bytes)
        } else {
            println("Desktop: Prueba a $printerName")
        }
    }

    private fun sendRawToSystemPrinter(bytes: ByteArray) {
        if (printerName.isBlank()) return
        try {
            val services = javax.print.PrintServiceLookup.lookupPrintServices(null, null)
            val selectedService = services.find { it.name == printerName } ?: return
            
            val docFlavor = javax.print.DocFlavor.BYTE_ARRAY.AUTOSENSE
            val doc = javax.print.SimpleDoc(bytes, docFlavor, null)
            val job = selectedService.createPrintJob()
            job.print(doc, null)
            
        } catch (e: Exception) {
            println("Desktop: Error raw SYSTEM: ${e.message}")
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
            if (connectionType == "NETWORK") {
                val parts = address.split(":")
                val ip = parts[0]
                val port = if (parts.size > 1) parts[1].toInt() else 9100
                
                java.net.Socket().use { socket ->
                    socket.connect(java.net.InetSocketAddress(ip, port), 5000)
                    val outputStream = socket.getOutputStream()
                    outputStream.write(byteArrayOf(0x1B, 0x40)) // Reset
                    block(outputStream)
                    outputStream.flush()
                }
            } else {
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
            }
        } catch (e: Exception) {
            println("Desktop: Error en comunicación ($connectionType - $address): ${e.message}")
        }
    }
}

actual fun getRealPrinterManager(): PrinterManager = DesktopPrinterManager()
