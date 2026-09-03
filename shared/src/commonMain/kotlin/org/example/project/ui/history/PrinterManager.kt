package com.abtsplazita.posplazita.ui.history

import com.abtsplazita.posplazita.domain.Sale
import com.abtsplazita.posplazita.domain.SaleItem
import com.abtsplazita.posplazita.domain.Customer
import com.abtsplazita.posplazita.domain.TicketConfig
import com.abtsplazita.posplazita.domain.TicketElementType
import com.abtsplazita.posplazita.domain.formatPrice
import com.abtsplazita.posplazita.domain.formatWeight

interface PrinterManager {
    /**
     * Envía una venta a imprimir en formato ticket.
     */
    fun printTicket(
        sale: Sale, 
        items: List<SaleItem>, 
        openDrawer: Boolean = true, 
        comment: String? = null, 
        walletBalance: Double? = null,
        config: TicketConfig? = null,
        branchName: String? = null
    )
    
    /**
     * Imprime un comprobante de abono a deuda.
     */
    fun printDebtPayment(
        customer: Customer,
        amountPaid: Double,
        remainingDebt: Double,
        config: TicketConfig? = null,
        branchName: String? = null
    )

    /**
     * Imprime una tarjeta de membresía para un cliente (Formato Gafete).
     */
    fun printMemberCard(customer: Customer)

    /**
     * Imprime una tarjeta de membresía usando el sistema nativo (Permite PDF/Cualquier impresora).
     */
    fun printMemberCardGraphic(customer: Customer)

    /**
     * Abre el cajón de dinero sin imprimir ticket.
     */
    fun openDrawer()

    /**
     * Configura la impresora activa.
     */
    fun setConfig(
        name: String, 
        type: String, 
        address: String, 
        paperSize: Int = 80,
        autoCut: Boolean = true,
        openDrawer: Boolean = true,
        drawerCommand: String = "EPSON_PIN2"
    )

    /**
     * Imprime una página de prueba.
     */
    fun printTestPage()

    /**
     * Obtiene dispositivos vinculados (Bluetooth).
     */
    fun getPairedDevices(): List<Pair<String, String>>

    /**
     * Obtiene impresoras instaladas en el sistema (Desktop).
     */
    fun getSystemPrinters(): List<String>
}

/**
 * Función que entregará la implementación real según la plataforma (Android/Desktop)
 */
expect fun getRealPrinterManager(): PrinterManager

/**
 * Utilidad común para construir el contenido del ticket basado en el diseño.
 */
fun buildTicketContentCommon(
    sale: Sale,
    items: List<SaleItem>,
    comment: String?,
    walletBalance: Double?,
    config: TicketConfig?,
    lineChars: Int,
    dateStr: String,
    branchName: String? = null
): String {
    val layout = config?.layout ?: TicketConfig.defaultLayout
    val divider = "-".repeat(lineChars)
    val sb = StringBuilder()

    layout.forEach { element ->
        if (!element.visible) return@forEach
        
        when (element.type) {
            TicketElementType.LOGO -> {
                if (!config?.logoPath.isNullOrBlank()) {
                    val text = "[ LOGO ]"
                    sb.append(alignText(text, lineChars, element.alignment) + "\n")
                }
            }
            TicketElementType.HEADER -> {
                val text = element.label ?: "TICKET DE VENTA"
                if (text.isNotBlank()) {
                    sb.append(alignText(text, lineChars, element.alignment) + "\n")
                }
            }
            TicketElementType.BRANCH_INFO -> {
                if (config?.showBranchInfo == true) {
                    val name = if (!branchName.isNullOrBlank()) branchName else "Abarrotes Delany"
                    sb.append(alignText(name, lineChars, element.alignment) + "\n")
                }
            }
            TicketElementType.BRANCH_ADDRESS -> {
                config?.branchAddress?.let {
                    sb.append(alignText(it, lineChars, element.alignment) + "\n")
                }
            }
            TicketElementType.BRANCH_PHONE -> {
                config?.branchPhone?.let {
                    sb.append(alignText(it, lineChars, element.alignment) + "\n")
                }
            }
            TicketElementType.DIVIDER -> sb.append("$divider\n")
            TicketElementType.TICKET_ID -> {
                val displayId = sale.id.split("-").lastOrNull() ?: sale.id
                val parts = dateStr.split(" ")
                val onlyTime = if (parts.size >= 3) "${parts[1]} ${parts[2]}" else if (parts.size >= 2) parts[1] else ""
                val line = displayId.padEnd((lineChars - onlyTime.length).coerceAtLeast(0)) + onlyTime
                sb.append(line + "\n")
            }
            TicketElementType.DATE -> {
                val parts = dateStr.split(" ")
                val onlyDate = parts[0]
                val label = "Venta"
                val line = label.padEnd((lineChars - onlyDate.length).coerceAtLeast(0)) + onlyDate
                sb.append(line + "\n")
            }
            TicketElementType.CUSTOMER_INFO -> {
                val name = if (sale.customerId != null) "Cliente: ${sale.customerId}" else "Cliente: Público en General"
                sb.append(alignText(name, lineChars, element.alignment) + "\n")
            }
            TicketElementType.ITEMS_TABLE -> {
                items.forEach {
                    // Linea 1: Cantidad x Nombre
                    val line1 = "${it.quantity.formatWeight()} x ${it.productName}"
                    sb.append(line1.take(lineChars) + "\n")
                    
                    // Linea 2: Precio unitario [espacios] Subtotal
                    val unitPrice = "$${it.priceAtSale.formatPrice()}"
                    val subtotal = "$${it.subtotal.formatPrice()}"
                    val spaces = (lineChars - unitPrice.length - subtotal.length).coerceAtLeast(1)
                    sb.append(unitPrice + " ".repeat(spaces) + subtotal + "\n")
                }
            }
            TicketElementType.TOTAL -> {
                val totalQty = items.sumOf { it.quantity }.toInt()
                val totalLabel = "Total($totalQty) MXN:"
                val totalVal = "$${sale.total.formatPrice()}"
                sb.append(totalLabel.padEnd((lineChars - totalVal.length).coerceAtLeast(0)) + totalVal + "\n")
            }
            TicketElementType.PAYMENT_INFO -> {
                val methodLabel = "${sale.paymentMethod} MXN:"
                val methodVal = if (sale.paymentMethod == "Efectivo") "$${(sale.receivedAmount).formatPrice()}" else "$${sale.total.formatPrice()}"
                sb.append(methodLabel.padEnd((lineChars - methodVal.length).coerceAtLeast(0)) + methodVal + "\n")
                
                if (sale.paymentMethod == "Efectivo") {
                    val cLabel = "Cambio MXN:"
                    val cVal = "$${sale.changeAmount.formatPrice()}"
                    sb.append(cLabel.padEnd((lineChars - cVal.length).coerceAtLeast(0)) + cVal + "\n")
                }
            }
            TicketElementType.TERMINAL_INFO -> {
                val term = sale.terminalId ?: "1"
                sb.append(alignText("Caja $term", lineChars, element.alignment) + "\n")
            }
            TicketElementType.WALLET_BALANCE -> {
                if (walletBalance != null) {
                    val wLabel = "SALDO MONEDERO:"
                    val wVal = "$${walletBalance.formatPrice()}"
                    sb.append(wLabel.padEnd((lineChars - wVal.length).coerceAtLeast(0)) + wVal + "\n")
                }
            }
            TicketElementType.COMMENT -> {
                if (!comment.isNullOrBlank()) {
                    sb.append(alignText("NOTA: $comment", lineChars, element.alignment) + "\n")
                }
            }
            TicketElementType.THANKS_MESSAGE -> {
                val msg = config?.thanksMessage
                if (!msg.isNullOrBlank()) {
                    sb.append(alignText(msg, lineChars, element.alignment) + "\n")
                }
            }
            TicketElementType.SOCIAL_MEDIA -> {
                val fb = config?.facebook
                val ig = config?.instagram
                val wa = config?.whatsapp
                if (!fb.isNullOrBlank()) sb.append(alignText("FB: $fb", lineChars, element.alignment) + "\n")
                if (!ig.isNullOrBlank()) sb.append(alignText("IG: $ig", lineChars, element.alignment) + "\n")
                if (!wa.isNullOrBlank()) sb.append(alignText("WA: $wa", lineChars, element.alignment) + "\n")
            }
            TicketElementType.SPACE -> sb.append("\n")
        }
    }
    return sb.toString()
}

fun alignText(text: String, lineChars: Int, alignment: com.abtsplazita.posplazita.domain.TicketAlignment): String {
    val cleanText = text.trim().replace("\t", " ")
    return when (alignment) {
        com.abtsplazita.posplazita.domain.TicketAlignment.LEFT -> cleanText
        com.abtsplazita.posplazita.domain.TicketAlignment.CENTER -> {
            val totalSpaces = (lineChars - cleanText.length).coerceAtLeast(0)
            val leftSpaces = totalSpaces / 2
            " ".repeat(leftSpaces) + cleanText
        }
        com.abtsplazita.posplazita.domain.TicketAlignment.RIGHT -> {
            val leftSpaces = (lineChars - cleanText.length).coerceAtLeast(0)
            " ".repeat(leftSpaces) + cleanText
        }
    }
}

class MockPrinterManager : PrinterManager {
    private var printerName: String = "No configurada"
    private var connectionType: String = "N/A"
    private var address: String = "N/A"

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
        println("=== ENVIANDO A IMPRESORA: $printerName ($connectionType) | Abrir Cajón: $openDrawer ===")
        if (config != null) {
            println("Configuración Ticket: $config")
        }
        if (!comment.isNullOrBlank()) {
            println("Comentario: $comment")
        }
        if (walletBalance != null) {
            println("Saldo Monedero: $walletBalance")
        }
    }

    override fun printDebtPayment(
        customer: Customer,
        amountPaid: Double,
        remainingDebt: Double,
        config: TicketConfig?,
        branchName: String?
    ) {
        println("=== ENVIANDO ABONO A IMPRESORA: $printerName ===")
        println("CLIENTE: ${customer.name} | ABONO: $amountPaid | RESTANTE: $remainingDebt")
    }

    override fun printMemberCard(customer: Customer) {
        println("=== IMPRIMIENDO TARJETA DEL CLIENTE: ${customer.name} | Código: CLI-${customer.id} ===")
    }

    override fun printMemberCardGraphic(customer: Customer) {
        println("=== IMPRIMIENDO GAFETE GRÁFICO (PDF/SISTEMA): ${customer.name} ===")
    }

    override fun openDrawer() {
        println("=== ABRIENDO CAJÓN DE DINERO ===")
    }

    override fun printTestPage() {
        println("=== PÁGINA DE PRUEBA EN: $printerName ===")
    }

    override fun getPairedDevices(): List<Pair<String, String>> = emptyList()

    override fun getSystemPrinters(): List<String> = listOf("Impresora PDF", "Thermal Printer 80mm")
}
