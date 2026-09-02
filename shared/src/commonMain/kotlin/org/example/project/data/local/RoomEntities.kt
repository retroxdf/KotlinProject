package com.abtsplazita.posplazita.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val barcode: String,
    val category: String,
    val imagePath: String?,
    val unit: String,
    val tax: Double,
    val cost: Double,
    val isBulk: Boolean,
    val useScale: Boolean = false,
    val price1: Double,
    val price2: Double,
    val price3: Double,
    val price4: Double,
    val isService: Boolean,
    val barcode2: String? = null,
    val barcode3: String? = null,
    val barcode4: String? = null,
    val satCode: String? = null,
    @ColumnInfo(defaultValue = "0.0") val ieps: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val showInWebShop: Boolean = false,
    val lastUpdated: Long = 0L,
    val lastPurchaseUnit: String? = null,
    @ColumnInfo(defaultValue = "1.0") val lastPurchaseFactor: Double = 1.0,
    @ColumnInfo(defaultValue = "0.0") val lastPurchaseTax: Double = 0.0,
    @ColumnInfo(defaultValue = "0.0") val lastPurchaseCost: Double = 0.0
)

@Entity(primaryKeys = ["productId", "branchId"])
data class InventoryEntity(
    val productId: String,
    val branchId: String,
    val stock: Double,
    @ColumnInfo(defaultValue = "0.0") val minStock: Double = 0.0,
    @ColumnInfo(defaultValue = "0.0") val maxStock: Double = 0.0,
    val lastUpdated: Long = 0L
)

@Entity
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val firstName: String,
    val lastName: String,
    val nip: String,
    val role: String,
    val phone: String?,
    val employeeId: Long?,
    val mustChangeNip: Boolean,
    val lastLoginTime: Long? = null,
    val isActive: Boolean,
    val lastUpdated: Long = 0L
)

@Entity
data class BranchEntity(
    @PrimaryKey val id: String,
    val name: String,
    val address: String,
    val lastUpdated: Long = 0L
)

@Entity
data class SaleEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val userId: String, // Auditoría
    val branchId: String,
    val terminalId: String?,
    val customerId: String? = null,
    val total: Double,
    val netTotal: Double = 0.0,
    val cashAmount: Double = 0.0,
    val creditAmount: Double = 0.0,
    val receivedAmount: Double = 0.0,
    val changeAmount: Double = 0.0,
    val paymentMethod: String,
    val status: String,
    val comment: String? = null,
    @ColumnInfo(defaultValue = "0") val isSynced: Boolean = false,
    val originalWebOrderId: String? = null
)

@Entity
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: String,
    val productId: String,
    val productName: String,
    val productImagePath: String? = null,
    val quantity: Double,
    val priceAtSale: Double,
    val subtotal: Double,
    @ColumnInfo(defaultValue = "General") val category: String = "General",
    @ColumnInfo(defaultValue = "0") val isService: Boolean = false,
    @ColumnInfo(defaultValue = "0") val isBulk: Boolean = false,
    @ColumnInfo(defaultValue = "0") val isWebDiscounted: Boolean = false
)

@Entity
data class CashSessionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val openingTime: Long,
    val closingTime: Long?,
    val initialAmount: Double,
    val declaredAmount: Double?,
    val systemAmount: Double,
    val status: String
)

@Entity
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: String,
    val branchId: String,
    val type: String,
    val quantity: Double,
    val timestamp: Long,
    val userId: String,
    val reason: String?
)

@Entity
data class CustomerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String?,
    val phone: String?,
    val creditLimit: Double,
    @ColumnInfo(defaultValue = "0.0") val creditLimitWeekly: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val creditDays: Int = 0,
    val currentDebt: Double,
    val loyaltyPoints: Int,
    @ColumnInfo(defaultValue = "0.0") val walletBalance: Double = 0.0,
    val lastUpdated: Long = 0L
)

@Entity
data class CustomerPaymentEntity(
    @PrimaryKey val id: String,
    val customerId: String,
    val amount: Double,
    val timestamp: Long,
    val userId: String,
    @ColumnInfo(defaultValue = "Efectivo") val paymentMethod: String = "Efectivo",
    val notes: String?
)

@Entity(primaryKeys = ["customerId", "productId"])
data class CustomerProductPriceEntity(
    val customerId: String,
    val productId: String,
    val specialPrice: Double
)

@Entity
data class PosTerminalEntity(
    @PrimaryKey val id: String,
    val branchId: String,
    val name: String,
    val isActive: Boolean,
    val lastUpdated: Long = 0L
)

@Entity
data class SupplierEntity(
    @PrimaryKey val id: String,
    val name: String,
    val contactName: String?,
    val phone: String?,
    val email: String?,
    val address: String?,
    val givesCredit: Boolean,
    val creditDays: Int,
    val currentDebt: Double = 0.0
)

@Entity
data class SupplierPaymentEntity(
    @PrimaryKey val id: String,
    val supplierId: String,
    val amount: Double,
    val timestamp: Long,
    val method: String,
    val userId: String,
    val notes: String?
)

@Entity(primaryKeys = ["productId", "supplierId"])
data class ProductSupplierEntity(
    val productId: String,
    val supplierId: String,
    val lastCost: Double,
    val lastPurchaseDate: Long
)

@Entity(primaryKeys = ["role", "permission"])
data class RolePermissionEntity(
    val role: String,
    val permission: String,
    val level: String // DISABLED, RESTRICTED, ENABLED
)

@Entity
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String
)

@Entity
data class TaxEntity(
    @PrimaryKey val id: String,
    val name: String,
    val rate: Double
)

@Entity
data class CashOutEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val branchId: String,
    val terminalId: String?,
    val expectedAmount: Double,
    val countedAmount: Double,
    val difference: Double,
    val ticketCount: Int,
    val userId: String,
    @ColumnInfo(defaultValue = "0") val isSynced: Boolean = false
)

@Entity
data class PreCutEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val branchId: String,
    val terminalId: String?,
    val expectedAmount: Double,
    val countedAmount: Double,
    val difference: Double,
    val userId: String,
    @ColumnInfo(defaultValue = "0") val isSynced: Boolean = false
)

@Entity
data class CashMovementEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val branchId: String,
    val terminalId: String?,
    val type: String,
    val amount: Double,
    val reason: String,
    val userId: String,
    @ColumnInfo(defaultValue = "0") val isSynced: Boolean = false
)

@Entity
data class AppSettingsEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Entity
data class PurchaseEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val userId: String,
    val branchId: String,
    val supplierId: String?,
    val total: Double,
    val paymentMethod: String = "Efectivo",
    val status: String
)

@Entity
data class PurchaseItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val purchaseId: String,
    val productId: String,
    val productName: String,
    val minStock: Double = 0.0,
    val maxStock: Double = 0.0,
    val quantity: Double,
    val costAtPurchase: Double,
    val subtotal: Double,
    @ColumnInfo(defaultValue = "PZA") val purchaseUnit: String = "PZA",
    @ColumnInfo(defaultValue = "1.0") val purchaseFactor: Double = 1.0,
    val purchaseQuantity: Double = 0.0,
    val purchaseCost: Double = 0.0,
    val discountPercent: Double = 0.0,
    val taxRate: Double = 0.0
)

@Entity
data class PurchaseUnitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val factor: Double,
    val lastUpdated: Long = 0L
)

@Entity
data class HeldSaleEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val branchId: String,
    val terminalId: String?,
    val customerId: String?,
    val total: Double
)

@Entity
data class HeldSaleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val heldSaleId: String,
    val productId: String,
    val productName: String,
    val productImagePath: String? = null,
    val quantity: Double,
    val priceAtSale: Double,
    val subtotal: Double,
    @ColumnInfo(defaultValue = "General") val category: String = "General",
    @ColumnInfo(defaultValue = "0") val isService: Boolean = false,
    @ColumnInfo(defaultValue = "0") val isBulk: Boolean = false,
    @ColumnInfo(defaultValue = "0") val isWebDiscounted: Boolean = false
)

@Entity
data class EmployeeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val phoneNumber: String,
    val branch: String,
    val baseSalary: Double,
    val bonus: Double = 0.0,
    val vacationWeeks: Int = 0,
    val lastPaidTimestamp: Long = 0L,
    val color: Int = -16711681
)

@Entity
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeId: Long,
    val dayOfWeek: Int,
    val checkInTime: String?,
    val checkOutTime: String?,
    val isRestDay: Boolean = false,
    val branchName: String? = null,
    @ColumnInfo(defaultValue = "0") val isSynced: Boolean = false
)

@Entity
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeId: Long,
    val amount: Double,
    val date: Long,
    val isPaid: Boolean = false
)

@Entity
data class AbsenceReplacementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val absentEmployeeId: Long,
    val replacementEmployeeId: Long?,
    val date: Long,
    val shiftDetails: String? = null,
    val isExcused: Boolean = false,
    val replacementType: String = "EXTRA"
)

@Entity
data class CashBoxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val branchId: String,
    val name: String,
    val currentBalance: Double = 0.0
)

@Entity
data class AccountingTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val amount: Double,
    val concept: String,
    val timestamp: Long,
    val initialBalance: Double,
    val finalBalance: Double
)

@Entity
data class CorteContaplaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val branchId: String,
    val cashBoxId: Long,
    val amount: Double,
    val timestamp: Long
)

@Entity
data class PaymentRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeId: Long,
    val employeeName: String,
    val date: Long,
    val amount: Double,
    val reportText: String
)

@Entity
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val employeeId: Long?,
    val startTime: Long,
    val endTime: Long?,
    val hoursWorked: Double,
    val payAmount: Double,
    val isClosed: Boolean,
    @ColumnInfo(defaultValue = "0") val isSynced: Boolean = false
)

@Entity
data class PromotionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val productId: String?,
    val category: String?,
    val discountValue: Double,
    val triggerQuantity: Int,
    val startDate: Long,
    val endDate: Long,
    val isActive: Boolean,
    val lastUpdated: Long = 0L
)

@Entity
data class DeletionRequestEntity(
    @PrimaryKey val id: String,
    val ticketId: String,
    val timestamp: Long,
    val userId: String,
    val total: Double,
    val itemsSummary: String,
    val branchId: String,
    @ColumnInfo(defaultValue = "PENDING") val status: String = "PENDING"
)

@Entity
data class DeletionLogEntity(
    @PrimaryKey val id: String,
    val ticketId: String,
    val timestamp: Long,
    val requesterId: String,
    val approverId: String,
    val total: Double,
    val itemsSummary: String,
    val branchId: String,
    val reason: String? = null
)

@Entity
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val category: String,
    val amount: Double,
    val timestamp: Long,
    val branchId: String,
    val reason: String?,
    val userId: String,
    val lastUpdated: Long = 0L
)

@Entity
data class ProductReturnEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val branchId: String,
    val returnedProductId: String,
    val returnedProductName: String,
    val returnedQuantity: Double,
    val returnedPrice: Double,
    val takenProductId: String?,
    val takenProductName: String?,
    val takenQuantity: Double?,
    val takenPrice: Double?,
    val difference: Double,
    val userId: String,
    val reason: String = "Devolución / Cambio",
    @ColumnInfo(defaultValue = "0") val isSynced: Boolean = false
)
