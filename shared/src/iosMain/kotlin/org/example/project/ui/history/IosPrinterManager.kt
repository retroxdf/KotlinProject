package com.abtsplazita.posplazita.ui.history

import com.abtsplazita.posplazita.domain.Sale
import com.abtsplazita.posplazita.domain.SaleItem
import com.abtsplazita.posplazita.domain.Customer

import com.abtsplazita.posplazita.domain.TicketConfig

class IosPrinterManager : PrinterManager {
    override fun setConfig(
        name: String, 
        type: String, 
        address: String, 
        paperSize: Int, 
        autoCut: Boolean, 
        openDrawer: Boolean
    ) {}
    override fun printTicket(sale: Sale, items: List<SaleItem>, openDrawer: Boolean, comment: String?, walletBalance: Double?, config: TicketConfig?, branchName: String?) {}
    override fun printMemberCard(customer: Customer) {}
    override fun printMemberCardGraphic(customer: Customer) {}
    override fun openDrawer() {}
    override fun printTestPage() {}
    override fun getPairedDevices(): List<Pair<String, String>> = emptyList()
    override fun getSystemPrinters(): List<String> = emptyList()
}

actual fun getRealPrinterManager(): PrinterManager = IosPrinterManager()
