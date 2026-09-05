package com.abtsplazita.posplazita.domain

import kotlinx.serialization.Serializable

@Serializable
data class Category(val id: String, val name: String)

@Serializable
data class Tax(val id: String, val name: String, val rate: Double)

@Serializable
data class AppSettings(val key: String, val value: String)

@Serializable
data class UserPanelStats(
    val daysWorked: Int,
    val restDay: String,
    val checkInTime: Long?,
    val checkOutTime: Long?,
    val dailyBasePay: Double,
    val earnings: Double,
    val bonus: Double,
    val weekDetails: List<DayStatusInfo>
)

@Serializable
data class DayStatusInfo(
    val name: String,
    val status: String, // TRABAJADO, PENDIENTE, DESCANSO, FALTA
    val amount: Double
)

@Serializable
data class Branch(
    val id: String,
    val name: String,
    val address: String,
    val phone: String = "",
    val lastUpdated: Long = 0L
)

@Serializable
data class PaymentMethod(
    val id: String,
    val name: String,
    val isSystem: Boolean = false // Si es de sistema (Efectivo/Tarjeta) no se borran
)

@Serializable
data class Product(
    val id: String,
    val name: String,
    val barcode: String = "",
    val category: String = "",
    val imagePath: String? = null,
    val unit: UnitType = UnitType.PIECE,
    val tax: Double = 0.0,
    val cost: Double = 0.0,
    val isBulk: Boolean = false, // Venta a granel
    val useScale: Boolean = false, // Obtener peso de báscula automáticamente
    val price1: Double = 0.0,
    val price2: Double = 0.0,
    val price3: Double = 0.0,
    val price4: Double = 0.0,
    val isService: Boolean = false, // Si es true, se excluye del corte de caja
    val barcode2: String? = null,
    val barcode3: String? = null,
    val barcode4: String? = null,
    val satCode: String? = null,
    val ieps: Double = 0.0,
    val showInWebShop: Boolean = false,
    val lastUpdated: Long = 0L,
    val lastPurchaseUnit: String? = null,
    val lastPurchaseFactor: Double = 1.0,
    val lastPurchaseTax: Double = 0.0,
    val lastPurchaseCost: Double = 0.0
)

@Serializable
data class Inventory(
    val productId: String,
    val branchId: String,
    val stock: Double,
    val minStock: Double = 0.0,
    val maxStock: Double = 0.0,
    val lastUpdated: Long = 0L
)

@Serializable
enum class UnitType { KG, PIECE }

// --- Seguridad y Roles ---

@Serializable
enum class Permission {
    // Ventas
    MAKE_SALE,
    SELL_ON_CREDIT,
    ACCEPT_CARD_PAYMENT,
    DELETE_SALE_ITEM,
    CANCEL_SALE,
    OPEN_CASH_DRAWER,
    
    // Caja y Operación
    PERFORM_CASH_OUT,
    PERFORM_PRE_CUT,
    MANAGE_CASH_MOVEMENTS,
    
    // Catálogo Productos
    PRODUCT_VIEW,
    PRODUCT_CREATE,
    PRODUCT_EDIT,
    PRODUCT_DELETE,
    
    // Catálogo Clientes
    CUSTOMER_VIEW,
    CUSTOMER_CREATE,
    CUSTOMER_EDIT,
    CUSTOMER_DELETE,
    
    // Catálogo Proveedores
    SUPPLIER_VIEW,
    SUPPLIER_CREATE,
    SUPPLIER_EDIT,
    SUPPLIER_DELETE,
    
    // Almacén
    MANAGE_PURCHASES,
    
    // Retiros y Servicios
    MANAGE_WITHDRAWALS,

    // Administración y Reportes
    VIEW_REPORTS,
    VIEW_ACCOUNTING,
    MANAGE_SETTINGS,
    MANAGE_USERS
}

@Serializable
enum class PermissionLevel {
    DISABLED,   // Apagado (No visible/No permitido)
    RESTRICTED, // Medio (Permiso para Administrador/Pide PIN)
    ENABLED     // Habilitado (Libre para el usuario)
}

@Serializable
enum class Role {
    SUPER_ADMIN,
    GERENTE,
    CAJERO,
    ALMACEN,
    AUDITOR
}

@Serializable
data class RolePermission(
    val role: Role,
    val permission: Permission,
    val level: PermissionLevel
)

/**
 * Corrige URLs de servicios como Dropbox para que funcionen como links directos de imagen.
 */
fun String.toDirectImageUrl(): String {
    if (this.isBlank()) return this
    
    // Dropbox: Cambiar www.dropbox.com por dl.dropboxusercontent.com
    if (this.contains("www.dropbox.com")) {
        return this.replace("www.dropbox.com", "dl.dropboxusercontent.com")
            .replace("dl=0", "dl=1")
    }
    
    return this
}

@Serializable
data class User(
    val id: String,
    val username: String, // Este será el "Alias" o Nickname
    val firstName: String,
    val lastName: String,
    val nip: String = "1111",
    val role: Role,
    val phone: String? = null,
    val employeeId: Long? = null, // Ligado a Empleado de Contapla
    val mustChangeNip: Boolean = true,
    val lastLoginTime: Long? = null,
    val isActive: Boolean = true,
    val lastUpdated: Long = 0L
)

// --- Ventas Profesional ---

@Serializable
data class Sale(
    val id: String,
    val timestamp: Long,
    val userId: String, // Auditoría: quién vendió
    val branchId: String,
    val terminalId: String? = null, // Caja que realizó la venta
    val customerId: String? = null, // Cliente de la venta
    val items: List<SaleItem>,
    val total: Double, // Total cobrado al cliente (incluye servicios)
    val netTotal: Double = 0.0, // Total para el corte (excluye servicios)
    val cashAmount: Double = 0.0, // Parte pagada en efectivo
    val creditAmount: Double = 0.0, // Parte enviada a deuda (fiado)
    val receivedAmount: Double = 0.0, // Monto entregado por el cliente
    val changeAmount: Double = 0.0, // Cambio devuelto al cliente
    val paymentMethod: String = "Efectivo",
    val status: SaleStatus = SaleStatus.COMPLETED,
    val comment: String? = null,
    val isSynced: Boolean = false,
    val originalWebOrderId: String? = null
)

@Serializable
enum class SaleStatus {
    COMPLETED,
    CANCELLED,
    REFUNDED
}

@Serializable
data class SaleItem(
    val productId: String,
    val productName: String,
    val productImagePath: String? = null,
    val quantity: Double,
    val priceAtSale: Double,
    val subtotal: Double,
    val category: String = "General",
    val isService: Boolean = false,
    val isBulk: Boolean = false,
    val isWebDiscounted: Boolean = false,
    val price1: Double? = null, // Precio de mayoreo (NUEVO)
    val price2: Double? = null, // Precio público (DEFAULT)
    val price3: Double? = null, // Precio adicional (P2 + 0.50)
    val isPromoApplied: Boolean = false // Si se aplicó una promoción (no genera monedero)
)

// --- Control de Caja ---

@Serializable
data class CashSession(
    val id: String,
    val userId: String,
    val openingTime: Long,
    val closingTime: Long? = null,
    val initialAmount: Double,
    val declaredAmount: Double? = null,
    val systemAmount: Double = 0.0,
    val status: CashSessionStatus = CashSessionStatus.OPEN
)

@Serializable
enum class CashSessionStatus { OPEN, CLOSED }

// --- Inventario Auditoría (Kardex) ---

@Serializable
data class StockMovement(
    val id: Long = 0,
    val productId: String,
    val branchId: String,
    val type: MovementType,
    val quantity: Double,
    val timestamp: Long,
    val userId: String,
    val reason: String? = null
)

@Serializable
enum class MovementType {
    IN_PURCHASE,    // Entrada por compra
    OUT_SALE,       // Salida por venta
    ADJUSTMENT,     // Ajuste manual (merma, robo)
    TRANSFER        // Traspaso
}

// --- CRM y Clientes ---

@Serializable
data class Customer(
    val id: String,
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val creditLimit: Double = 0.0,
    val creditLimitWeekly: Double = 0.0,
    val creditDays: Int = 0,
    val currentDebt: Double = 0.0,
    val loyaltyPoints: Int = 0,
    val walletBalance: Double = 0.0,
    val lastUpdated: Long = 0L
)

@Serializable
data class CustomerPayment(
    val id: String,
    val customerId: String,
    val amount: Double,
    val timestamp: Long,
    val userId: String,
    val paymentMethod: String = "Efectivo",
    val notes: String? = null
)

@Serializable
data class CustomerProductPrice(
    val customerId: String,
    val productId: String,
    val specialPrice: Double
)

// --- Terminales de Punto de Venta (Cajas) ---

@Serializable
data class PosTerminal(
    val id: String,
    val branchId: String,
    val name: String,
    val isActive: Boolean = true,
    val lastUpdated: Long = 0L
)

// --- Recargas y Servicios ---

@Serializable
data class Carrier(
    val id: String,
    val name: String,
    val iconUrl: String? = null,
    val amounts: List<Double> = listOf(10.0, 20.0, 30.0, 50.0, 100.0, 200.0, 500.0)
)

@Serializable
data class RechargeRequest(
    val phoneNumber: String,
    val carrierId: String,
    val amount: Double,
    val terminalId: String,
    val timestamp: Long
)

@Serializable
data class ServiceCompany(
    val id: String,
    val name: String,
    val category: String, // CFE, Agua, Teléfono, etc.
    val fee: Double = 0.0 // Comisión por el servicio
)

@Serializable
data class ServicePaymentRequest(
    val companyId: String,
    val reference: String,
    val amount: Double,
    val fee: Double,
    val terminalId: String,
    val timestamp: Long
)

// --- Historial de Cortes de Caja ---

@Serializable
data class CashOut(
    val id: String,
    val timestamp: Long,
    val branchId: String,
    val terminalId: String?,
    val expectedAmount: Double,
    val countedAmount: Double,
    val difference: Double,
    val ticketCount: Int,
    val userId: String,
    val isSynced: Boolean = false
)

@Serializable
data class PreCut(
    val id: String,
    val timestamp: Long,
    val branchId: String,
    val terminalId: String?,
    val expectedAmount: Double,
    val countedAmount: Double,
    val difference: Double,
    val userId: String,
    val isSynced: Boolean = false
)

@Serializable
data class CashMovement(
    val id: String,
    val timestamp: Long,
    val branchId: String,
    val terminalId: String?,
    val type: CashMovementType,
    val amount: Double,
    val reason: String,
    val userId: String,
    val isSynced: Boolean = false
)

@Serializable
enum class CashMovementType { IN, OUT }

// --- Compras ---

@Serializable
data class Purchase(
    val id: String,
    val timestamp: Long,
    val userId: String,
    val branchId: String,
    val supplierId: String? = null,
    val items: List<PurchaseItem>,
    val total: Double,
    val paymentMethod: String = "Efectivo", // Efectivo (Fondo), Transferencia, Crédito
    val status: PurchaseStatus = PurchaseStatus.PENDING_PRICE_UPDATE
)

@Serializable
enum class PurchaseStatus {
    PENDING_PRICE_UPDATE, // Rojo en la consulta (pendiente de modificar precios/costos)
    COMPLETED,            // Normal en la consulta
    PAID                 // Pagado al proveedor
}

@Serializable
data class PurchaseItem(
    val productId: String,
    val productName: String,
    val quantity: Double,
    val costAtPurchase: Double,
    val subtotal: Double,
    val purchaseUnit: String = "PZA",
    val purchaseFactor: Double = 1.0,
    val purchaseQuantity: Double = 0.0,
    val purchaseCost: Double = 0.0,
    val discountPercent: Double = 0.0,
    val taxRate: Double = 0.0
)

@Serializable
data class PurchaseUnit(
    val id: String,
    val name: String,
    val factor: Double,
    val lastUpdated: Long = 0L
)

@Serializable
data class Supplier(
    val id: String,
    val name: String,
    val contactName: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val givesCredit: Boolean = false,
    val creditDays: Int = 0,
    val currentDebt: Double = 0.0
)

@Serializable
data class SupplierPayment(
    val id: String,
    val supplierId: String,
    val amount: Double,
    val timestamp: Long,
    val method: String, // Efectivo, Transferencia, etc.
    val userId: String,
    val notes: String? = null
)

@Serializable
data class ProductSupplier(
    val productId: String,
    val supplierId: String,
    val lastCost: Double,
    val lastPurchaseDate: Long
)

@Serializable
data class HeldSale(
    val id: String,
    val timestamp: Long,
    val branchId: String,
    val terminalId: String? = null,
    val items: List<SaleItem>,
    val customerId: String? = null,
    val total: Double
)

@Serializable
data class ProductReturn(
    val id: String,
    val timestamp: Long,
    val branchId: String,
    val returnedItem: SaleItem,
    val takenItem: SaleItem? = null,
    val difference: Double, // Positivo: cobrar, Negativo: devolver
    val userId: String,
    val reason: String = "Devolución / Cambio"
)

// --- Promociones y Descuentos ---

@Serializable
enum class PromotionType {
    FIXED_PRICE,      // Precio especial fijo por producto
    CATEGORY_PERCENT, // % de descuento a toda una categoría
    BULK_OFFER,       // X unidades por $precio (Ej: 2 por $20)
    TOTAL_AMOUNT_PERCENT // % de descuento si el total supera un monto
}

@Serializable
data class Promotion(
    val id: String,
    val name: String,
    val type: PromotionType,
    val productId: String? = null,
    val category: String? = null,
    val discountValue: Double = 0.0, // Precio fijo, % o precio paquete
    val triggerQuantity: Int = 1,    // Para bulk: cuántos activan la promo
    val startDate: Long,
    val endDate: Long,
    val isActive: Boolean = true,
    val lastUpdated: Long = 0L
)

// --- Webshop y Pedidos ---

@Serializable
data class DeletionRequest(
    val id: String,
    val ticketId: String,
    val timestamp: Long,
    val userId: String,
    val total: Double,
    val itemsSummary: String,
    val branchId: String,
    val status: String = "PENDING" // PENDING, APPROVED, REJECTED
)

@Serializable
data class DeletionLog(
    val id: String,
    val ticketId: String,
    val timestamp: Long,
    val requesterId: String,
    val approverId: String,
    val total: Double,
    val itemsSummary: String,
    val branchId: String,
    val reason: String? = null
)

@Serializable
enum class WebOrderStatus {
    PENDING,    // Recibido en la nube, no visto en POS
    PREPARING,  // Aceptado en POS, en preparación
    READY,      // Listo para entrega/envío
    DELIVERED,  // Finalizado y convertido a Sale
    CANCELLED   // Rechazado o cancelado
}

@Serializable
data class WebOrder(
    val id: String,
    val timestamp: Long,
    val branchId: String,
    val customerName: String,
    val customerPhone: String? = null,
    val address: String? = null, // Puede ser mesa o dirección
    val items: List<WebOrderItem>,
    val total: Double,
    val status: WebOrderStatus = WebOrderStatus.PENDING,
    val notes: String? = null
)

@Serializable
data class WebOrderItem(
    val productId: String,
    val productName: String,
    val quantity: Double,
    val priceAtOrder: Double,
    val subtotal: Double,
    val isWebDiscounted: Boolean = false
)

// --- Gastos y Utilidad ---

@Serializable
enum class ExpenseCategory {
    RENTA, LUZ, AGUA, NOMINA, LIMPIEZA, MERCANCIA, OTRO
}

@Serializable
data class Expense(
    val id: String,
    val category: ExpenseCategory,
    val amount: Double,
    val timestamp: Long,
    val branchId: String,
    val reason: String?,
    val userId: String,
    val lastUpdated: Long = 0L
)

// --- Configuración de Ticket ---

@Serializable
enum class TicketElementType {
    LOGO, HEADER, BRANCH_INFO, BRANCH_ADDRESS, BRANCH_PHONE, DIVIDER, TICKET_ID, DATE, CUSTOMER_INFO, ITEMS_TABLE, TOTAL, 
    PAYMENT_INFO, WALLET_BALANCE, COMMENT, THANKS_MESSAGE, SOCIAL_MEDIA, SPACE, TERMINAL_INFO
}

@Serializable
enum class TicketAlignment { LEFT, CENTER, RIGHT }

@Serializable
data class TicketElement(
    val type: TicketElementType,
    val visible: Boolean = true,
    val label: String? = null, // Texto opcional para cabeceras personalizadas
    val alignment: TicketAlignment = TicketAlignment.LEFT
)

@Serializable
data class TicketConfig(
    val logoPath: String? = null,
    val facebook: String? = null,
    val instagram: String? = null,
    val whatsapp: String? = null,
    val thanksMessage: String = "Gracias por su compra!",
    val branchAddress: String? = null,
    val branchPhone: String? = null,
    val showBranchInfo: Boolean = true,
    val ticketIdPrefix: String = "S",
    val layout: List<TicketElement> = defaultLayout
) {
    companion object {
        val defaultLayout = listOf(
            TicketElement(TicketElementType.LOGO),
            TicketElement(TicketElementType.HEADER),
            TicketElement(TicketElementType.BRANCH_INFO, alignment = TicketAlignment.CENTER),
            TicketElement(TicketElementType.BRANCH_ADDRESS, alignment = TicketAlignment.CENTER),
            TicketElement(TicketElementType.BRANCH_PHONE, alignment = TicketAlignment.CENTER),
            TicketElement(TicketElementType.DIVIDER),
            TicketElement(TicketElementType.DATE),
            TicketElement(TicketElementType.TICKET_ID),
            TicketElement(TicketElementType.CUSTOMER_INFO),
            TicketElement(TicketElementType.DIVIDER),
            TicketElement(TicketElementType.ITEMS_TABLE),
            TicketElement(TicketElementType.DIVIDER),
            TicketElement(TicketElementType.TOTAL),
            TicketElement(TicketElementType.PAYMENT_INFO),
            TicketElement(TicketElementType.WALLET_BALANCE),
            TicketElement(TicketElementType.DIVIDER),
            TicketElement(TicketElementType.TERMINAL_INFO, alignment = TicketAlignment.CENTER),
            TicketElement(TicketElementType.COMMENT),
            TicketElement(TicketElementType.THANKS_MESSAGE, alignment = TicketAlignment.CENTER),
            TicketElement(TicketElementType.SOCIAL_MEDIA, alignment = TicketAlignment.CENTER)
        )
    }
}

@Serializable
data class AppUpdateInfo(
    val version: String,
    val downloadUrl: String,
    val releaseNotes: String? = null,
    val forceUpdate: Boolean = false,
    val timestamp: Long = 0L
)

