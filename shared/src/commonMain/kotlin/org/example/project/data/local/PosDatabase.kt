package com.abtsplazita.posplazita.data.local

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM ProductEntity")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM ProductEntity ORDER BY name ASC LIMIT :limit OFFSET :offset")
    fun getProductsPaginated(limit: Int, offset: Int): Flow<List<ProductEntity>>

    @Query("SELECT * FROM ProductEntity WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): ProductEntity?

    @Query("SELECT * FROM ProductEntity WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: String): ProductEntity?

    @Query("""
        SELECT * FROM ProductEntity 
        WHERE (name LIKE '%' || :query || '%' 
           OR barcode LIKE '%' || :query || '%'
           OR barcode2 LIKE '%' || :query || '%'
           OR barcode3 LIKE '%' || :query || '%'
           OR barcode4 LIKE '%' || :query || '%')
        ORDER BY 
            CASE 
                WHEN name LIKE :query || '%' THEN 1 
                WHEN barcode LIKE :query || '%' THEN 2 
                ELSE 3 
            END, name ASC
        LIMIT :limit OFFSET :offset
    """)
    fun searchProductsPaginated(query: String, limit: Int, offset: Int): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProductsBatch(products: List<ProductEntity>)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("DELETE FROM ProductEntity")
    suspend fun clearAll()

    @Query("SELECT MAX(lastUpdated) FROM ProductEntity")
    suspend fun getLastUpdated(): Long?

    @Query("SELECT * FROM ProductEntity WHERE id IN (:ids)")
    fun getProductsByIds(ids: List<String>): Flow<List<ProductEntity>>
}

@Dao
interface InventoryDao {
    @Query("SELECT stock FROM InventoryEntity WHERE productId = :productId AND branchId = :branchId")
    suspend fun getStock(productId: String, branchId: String): Double?

    @Query("SELECT * FROM InventoryEntity WHERE productId = :productId")
    fun getStockForProduct(productId: String): Flow<List<InventoryEntity>>

    @Query("SELECT * FROM InventoryEntity")
    fun getAllInventory(): Flow<List<InventoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateInventory(inventory: InventoryEntity)

    @Query("UPDATE InventoryEntity SET stock = stock - :quantity WHERE productId = :productId AND branchId = :branchId")
    suspend fun decreaseStock(productId: String, branchId: String, quantity: Double)

    @Query("UPDATE InventoryEntity SET stock = 0 WHERE branchId = :branchId")
    suspend fun clearStockForBranch(branchId: String)

    @Query("SELECT MAX(lastUpdated) FROM InventoryEntity WHERE branchId = :branchId")
    suspend fun getLastUpdated(branchId: String): Long?
}

@Dao
interface BranchDao {
    @Query("SELECT * FROM BranchEntity")
    fun getAllBranches(): Flow<List<BranchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBranch(branch: BranchEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBranchesBatch(branches: List<BranchEntity>)

    @Delete
    suspend fun deleteBranch(branch: BranchEntity)

    @Query("DELETE FROM BranchEntity")
    suspend fun clearAll()

    @Query("SELECT MAX(lastUpdated) FROM BranchEntity")
    suspend fun getLastUpdated(): Long?
}

@Dao
interface SaleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleEntity)

    @Query("DELETE FROM SaleItemEntity WHERE saleId = :saleId")
    suspend fun deleteItemsBySale(saleId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItemEntity>)

    @Transaction
    suspend fun insertSalesBatch(sales: List<Pair<SaleEntity, List<SaleItemEntity>>>) {
        sales.forEach { (sale, items) ->
            insertSale(sale)
            deleteItemsBySale(sale.id)
            insertSaleItems(items)
        }
    }

    @Transaction
    suspend fun insertSaleWithItems(sale: SaleEntity, items: List<SaleItemEntity>) {
        insertSale(sale)
        deleteItemsBySale(sale.id)
        insertSaleItems(items)
    }

    @Query("SELECT * FROM SaleEntity WHERE id = :id LIMIT 1")
    suspend fun getSaleById(id: String): SaleEntity?

    @Query("SELECT * FROM SaleEntity WHERE branchId = :branchId AND id LIKE '%' || :query || '%' ORDER BY timestamp DESC LIMIT 10")
    fun searchSales(branchId: String, query: String): Flow<List<SaleEntity>>

    @Query("SELECT * FROM SaleEntity WHERE branchId = :branchId ORDER BY timestamp DESC")
    fun getSalesByBranch(branchId: String): Flow<List<SaleEntity>>

    @Query("SELECT * FROM SaleEntity WHERE branchId = :branchId AND terminalId = :terminalId ORDER BY timestamp DESC")
    fun getSalesByTerminal(branchId: String, terminalId: String): Flow<List<SaleEntity>>

    @Query("SELECT * FROM SaleEntity WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getSalesByCustomer(customerId: String): Flow<List<SaleEntity>>

    @Query("SELECT * FROM SaleItemEntity WHERE saleId = :saleId")
    suspend fun getItemsBySale(saleId: String): List<SaleItemEntity>

    @Query("SELECT * FROM SaleItemEntity WHERE saleId IN (:saleIds)")
    suspend fun getItemsBySales(saleIds: List<String>): List<SaleItemEntity>

    @Query("SELECT COUNT(*) FROM SaleEntity")
    suspend fun getSalesCount(): Int

    @Query("SELECT * FROM SaleEntity WHERE isSynced = 0")
    suspend fun getUnsyncedSales(): List<SaleEntity>

    @Query("UPDATE SaleEntity SET isSynced = 1 WHERE id = :saleId")
    suspend fun markAsSynced(saleId: String)

    @Query("DELETE FROM SaleEntity")
    suspend fun clearAll()

    @Query("DELETE FROM SaleItemEntity")
    suspend fun clearAllItems()
}

@Dao
interface UserDao {
    @Query("SELECT * FROM UserEntity")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM UserEntity WHERE id = :id")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM UserEntity WHERE nip = :nip AND isActive = 1 LIMIT 1")
    suspend fun getUserByNip(nip: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsersBatch(users: List<UserEntity>)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("DELETE FROM UserEntity")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM UserEntity")
    suspend fun getUserCount(): Int

    @Query("SELECT MAX(lastUpdated) FROM UserEntity")
    suspend fun getLastUpdated(): Long?
}

@Dao
interface CashSessionDao {
    @Query("SELECT * FROM CashSessionEntity WHERE status = 'OPEN' LIMIT 1")
    fun getActiveSession(): Flow<CashSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: CashSessionEntity)

    @Update
    suspend fun updateSession(session: CashSessionEntity)
}

@Dao
interface StockMovementDao {
    @Query("SELECT * FROM StockMovementEntity WHERE productId = :productId ORDER BY timestamp DESC")
    fun getMovementsByProduct(productId: String): kotlinx.coroutines.flow.Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM StockMovementEntity WHERE branchId = :branchId ORDER BY timestamp DESC")
    fun getMovementsByBranch(branchId: String): kotlinx.coroutines.flow.Flow<List<StockMovementEntity>>

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: StockMovementEntity)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM CustomerEntity")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM CustomerEntity WHERE id = :id")
    suspend fun getCustomerById(id: String): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity)

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)

    @Query("DELETE FROM CustomerEntity")
    suspend fun clearAll()

    @Query("SELECT MAX(lastUpdated) FROM CustomerEntity")
    suspend fun getLastUpdated(): Long?

    @Query("UPDATE CustomerEntity SET currentDebt = currentDebt + :amount WHERE id = :customerId")
    suspend fun updateDebt(customerId: String, amount: Double)

    @Query("UPDATE CustomerEntity SET walletBalance = walletBalance + :amount WHERE id = :customerId")
    suspend fun updateWalletBalance(customerId: String, amount: Double)
}

@Dao
interface CustomerPaymentDao {
    @Query("SELECT * FROM CustomerPaymentEntity WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getPaymentsByCustomer(customerId: String): Flow<List<CustomerPaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: CustomerPaymentEntity)
}

@Dao
interface CustomerProductPriceDao {
    @Query("SELECT * FROM CustomerProductPriceEntity WHERE customerId = :customerId")
    fun getSpecialPricesForCustomer(customerId: String): Flow<List<CustomerProductPriceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpecialPrice(price: CustomerProductPriceEntity)

    @Delete
    suspend fun deleteSpecialPrice(price: CustomerProductPriceEntity)
}

@Dao
interface PosTerminalDao {
    @Query("SELECT * FROM PosTerminalEntity WHERE branchId = :branchId")
    fun getTerminalsByBranch(branchId: String): Flow<List<PosTerminalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTerminal(terminal: PosTerminalEntity)

    @Query("DELETE FROM PosTerminalEntity WHERE id = :id")
    suspend fun deleteTerminal(id: String)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM CategoryEntity")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)
}

@Dao
interface TaxDao {
    @Query("SELECT * FROM TaxEntity")
    fun getAllTaxes(): Flow<List<TaxEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTax(tax: TaxEntity)

    @Delete
    suspend fun deleteTax(tax: TaxEntity)
}

@Dao
interface CashOutDao {
    @Query("SELECT * FROM CashOutEntity WHERE branchId = :branchId ORDER BY timestamp DESC")
    fun getCashOutsByBranch(branchId: String): Flow<List<CashOutEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashOut(cashOut: CashOutEntity)
}

@Dao
interface PreCutDao {
    @Query("SELECT * FROM PreCutEntity WHERE branchId = :branchId ORDER BY timestamp DESC")
    fun getPreCutsByBranch(branchId: String): Flow<List<PreCutEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreCut(preCut: PreCutEntity)

    @Query("SELECT COUNT(*) FROM PreCutEntity WHERE userId = :userId AND timestamp >= :since")
    suspend fun getPreCutCountForUserSince(userId: String, since: Long): Int
}

@Dao
interface CashMovementDao {
    @Query("SELECT * FROM CashMovementEntity WHERE branchId = :branchId ORDER BY timestamp DESC")
    fun getMovementsByBranch(branchId: String): Flow<List<CashMovementEntity>>

    @Query("SELECT * FROM CashMovementEntity WHERE branchId = :branchId AND terminalId = :terminalId ORDER BY timestamp DESC")
    fun getMovementsByTerminal(branchId: String, terminalId: String): Flow<List<CashMovementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: CashMovementEntity)

    @Query("SELECT * FROM CashMovementEntity WHERE isSynced = 0")
    suspend fun getUnsyncedMovements(): List<CashMovementEntity>

    @Query("UPDATE CashMovementEntity SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
}

@Dao
interface PurchaseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: PurchaseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseItems(items: List<PurchaseItemEntity>)

    @Transaction
    suspend fun insertPurchaseWithItems(purchase: PurchaseEntity, items: List<PurchaseItemEntity>) {
        insertPurchase(purchase)
        insertPurchaseItems(items)
    }

    @Query("SELECT * FROM PurchaseEntity WHERE branchId = :branchId ORDER BY timestamp DESC")
    fun getPurchasesByBranch(branchId: String): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM PurchaseEntity WHERE supplierId = :supplierId ORDER BY timestamp DESC")
    fun getPurchasesBySupplier(supplierId: String): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM PurchaseItemEntity WHERE purchaseId = :purchaseId")
    suspend fun getItemsByPurchase(purchaseId: String): List<PurchaseItemEntity>

    @Update
    suspend fun updatePurchase(purchase: PurchaseEntity)

    @Query("SELECT COUNT(*) FROM PurchaseEntity")
    suspend fun getPurchasesCount(): Int
}

@Dao
interface SupplierDao {
    @Query("SELECT * FROM SupplierEntity ORDER BY name ASC")
    fun getAllSuppliers(): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM SupplierEntity WHERE id = :id")
    suspend fun getSupplierById(id: String): SupplierEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: SupplierEntity)

    @Delete
    suspend fun deleteSupplier(supplier: SupplierEntity)

    @Query("UPDATE SupplierEntity SET currentDebt = currentDebt + :amount WHERE id = :supplierId")
    suspend fun updateDebt(supplierId: String, amount: Double)
}

@Dao
interface SupplierPaymentDao {
    @Query("SELECT * FROM SupplierPaymentEntity WHERE supplierId = :supplierId ORDER BY timestamp DESC")
    fun getPaymentsBySupplier(supplierId: String): Flow<List<SupplierPaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: SupplierPaymentEntity)
}

@Dao
interface ProductSupplierDao {
    @Query("SELECT * FROM ProductSupplierEntity WHERE productId = :productId ORDER BY lastCost ASC")
    fun getSuppliersForProduct(productId: String): Flow<List<ProductSupplierEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProductSupplier(link: ProductSupplierEntity)
}

@Dao
interface DeletionRequestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: DeletionRequestEntity)

    @Query("SELECT * FROM DeletionRequestEntity WHERE branchId = :branchId ORDER BY timestamp DESC")
    fun getRequestsByBranch(branchId: String): Flow<List<DeletionRequestEntity>>

    @Query("DELETE FROM DeletionRequestEntity WHERE id = :id")
    suspend fun deleteRequest(id: String)

    @Query("UPDATE DeletionRequestEntity SET status = :status WHERE id = :id")
    suspend fun updateRequestStatus(id: String, status: String)
}

@Dao
interface ProductReturnDao {
    @Query("SELECT * FROM ProductReturnEntity WHERE branchId = :branchId ORDER BY timestamp DESC")
    fun getReturnsByBranch(branchId: String): Flow<List<ProductReturnEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturn(productReturn: ProductReturnEntity)

    @Query("DELETE FROM ProductReturnEntity")
    suspend fun clearAll()
}

@Dao
interface DeletionLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DeletionLogEntity)

    @Query("SELECT * FROM DeletionLogEntity WHERE branchId = :branchId ORDER BY timestamp DESC")
    fun getLogsByBranch(branchId: String): Flow<List<DeletionLogEntity>>
}

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM AppSettingsEntity")
    fun getAllSettings(): Flow<List<AppSettingsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSetting(setting: AppSettingsEntity)

    @Query("SELECT value FROM AppSettingsEntity WHERE `key` = :key LIMIT 1")
    suspend fun getSetting(key: String): String?
}

@Dao
interface HeldSaleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeldSale(heldSale: HeldSaleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeldSaleItems(items: List<HeldSaleItemEntity>)

    @Transaction
    suspend fun saveHeldSale(heldSale: HeldSaleEntity, items: List<HeldSaleItemEntity>) {
        insertHeldSale(heldSale)
        insertHeldSaleItems(items)
    }

    @Query("SELECT * FROM HeldSaleEntity WHERE branchId = :branchId ORDER BY timestamp DESC")
    fun getHeldSalesByBranch(branchId: String): Flow<List<HeldSaleEntity>>

    @Query("SELECT * FROM HeldSaleItemEntity WHERE heldSaleId = :heldSaleId")
    suspend fun getItemsByHeldSale(heldSaleId: String): List<HeldSaleItemEntity>

    @Query("DELETE FROM HeldSaleEntity WHERE id = :id")
    suspend fun deleteHeldSale(id: String)

    @Query("DELETE FROM HeldSaleItemEntity WHERE heldSaleId = :heldSaleId")
    suspend fun deleteHeldSaleItems(heldSaleId: String)

    @Transaction
    suspend fun removeHeldSale(id: String) {
        deleteHeldSale(id)
        deleteHeldSaleItems(id)
    }
}

@Dao
interface EmployeeDao {
    @Query("SELECT * FROM EmployeeEntity ORDER BY fullName ASC")
    fun getAllEmployees(): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM EmployeeEntity WHERE id = :id")
    suspend fun getEmployeeById(id: Long): EmployeeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: EmployeeEntity): Long

    @Update
    suspend fun updateEmployee(employee: EmployeeEntity)

    @Delete
    suspend fun deleteEmployee(employee: EmployeeEntity)
}

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM ScheduleEntity")
    fun getAllSchedules(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM ScheduleEntity WHERE employeeId = :employeeId")
    fun getSchedulesForEmployee(employeeId: Long): Flow<List<ScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ScheduleEntity)

    @Update
    suspend fun updateSchedule(schedule: ScheduleEntity)

    @Delete
    suspend fun deleteSchedule(schedule: ScheduleEntity)

    @Query("DELETE FROM ScheduleEntity WHERE employeeId = :employeeId")
    suspend fun deleteSchedulesForEmployee(employeeId: Long)
}

@Dao
interface LoanDao {
    @Query("SELECT * FROM LoanEntity WHERE employeeId = :employeeId")
    fun getLoansForEmployee(employeeId: Long): Flow<List<LoanEntity>>

    @Query("SELECT * FROM LoanEntity ORDER BY date DESC")
    fun getAllLoans(): Flow<List<LoanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanEntity)

    @Update
    suspend fun updateLoan(loan: LoanEntity)

    @Query("SELECT SUM(amount) FROM LoanEntity WHERE employeeId = :employeeId AND isPaid = 0")
    suspend fun getPendingLoanTotal(employeeId: Long): Double?

    @Query("UPDATE LoanEntity SET isPaid = 1 WHERE employeeId = :employeeId")
    suspend fun markLoansAsPaid(employeeId: Long)
}

@Dao
interface AbsenceReplacementDao {
    @Query("SELECT * FROM AbsenceReplacementEntity ORDER BY date DESC")
    fun getAllAbsenceReplacements(): Flow<List<AbsenceReplacementEntity>>

    @Query("SELECT * FROM AbsenceReplacementEntity WHERE absentEmployeeId = :employeeId OR replacementEmployeeId = :employeeId")
    fun getReplacementsForEmployee(employeeId: Long): Flow<List<AbsenceReplacementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAbsenceReplacement(absenceReplacement: AbsenceReplacementEntity)

    @Update
    suspend fun updateAbsenceReplacement(absenceReplacement: AbsenceReplacementEntity)

    @Delete
    suspend fun deleteAbsenceReplacement(absenceReplacement: AbsenceReplacementEntity)

    @Query("SELECT * FROM AbsenceReplacementEntity WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getReplacementsInDateRange(startDate: Long, endDate: Long): List<AbsenceReplacementEntity>
}

@Dao
interface CashBoxDao {
    @Query("SELECT * FROM CashBoxEntity")
    fun getAllCashBoxes(): Flow<List<CashBoxEntity>>

    @Query("SELECT * FROM CashBoxEntity WHERE branchId = :branchId")
    fun getCashBoxesByBranch(branchId: String): Flow<List<CashBoxEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashBox(cashBox: CashBoxEntity)

    @Update
    suspend fun updateCashBox(cashBox: CashBoxEntity)

    @Delete
    suspend fun deleteCashBox(cashBox: CashBoxEntity)
}

@Dao
interface ContaplaTransactionDao {
    @Query("SELECT * FROM AccountingTransactionEntity ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<AccountingTransactionEntity>>

    @Insert
    suspend fun insertTransaction(transaction: AccountingTransactionEntity)

    @Query("SELECT finalBalance FROM AccountingTransactionEntity ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastBalance(): Double?
}

@Dao
interface CorteContaplaDao {
    @Query("SELECT * FROM CorteContaplaEntity ORDER BY timestamp DESC")
    fun getAllCortes(): Flow<List<CorteContaplaEntity>>

    @Insert
    suspend fun insertCorte(corte: CorteContaplaEntity)
}

@Dao
interface PaymentRecordDao {
    @Query("SELECT * FROM PaymentRecordEntity ORDER BY date DESC")
    fun getAllPaymentRecords(): Flow<List<PaymentRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentRecord(record: PaymentRecordEntity)

    @Delete
    suspend fun deletePaymentRecord(record: PaymentRecordEntity)
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM AttendanceEntity ORDER BY startTime DESC")
    fun getAllAttendance(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM AttendanceEntity WHERE userId = :userId AND isClosed = 0 LIMIT 1")
    suspend fun getOpenShift(userId: String): AttendanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: AttendanceEntity)

    @Update
    suspend fun updateAttendance(attendance: AttendanceEntity)

    @Query("SELECT * FROM AttendanceEntity WHERE userId = :userId AND startTime BETWEEN :start AND :end")
    fun getAttendanceInRange(userId: String, start: Long, end: Long): Flow<List<AttendanceEntity>>
}

@Dao
interface RolePermissionDao {
    @Query("SELECT * FROM RolePermissionEntity")
    fun getAllPermissions(): Flow<List<RolePermissionEntity>>

    @Query("SELECT * FROM RolePermissionEntity WHERE role = :role")
    fun getPermissionsForRole(role: String): Flow<List<RolePermissionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPermission(permission: RolePermissionEntity)

    @Query("DELETE FROM RolePermissionEntity WHERE role = :role")
    suspend fun clearRolePermissions(role: String)
}

@Dao
interface PromotionDao {
    @Query("SELECT * FROM PromotionEntity")
    fun getAllPromotions(): Flow<List<PromotionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPromotion(promotion: PromotionEntity)

    @Delete
    suspend fun deletePromotion(promotion: PromotionEntity)

    @Query("UPDATE PromotionEntity SET isActive = :active WHERE id = :id")
    suspend fun togglePromotion(id: String, active: Boolean)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM ExpenseEntity WHERE branchId = :branchId ORDER BY timestamp DESC")
    fun getExpensesByBranch(branchId: String): kotlinx.coroutines.flow.Flow<List<ExpenseEntity>>

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)
}

@Dao
interface PurchaseUnitDao {
    @Query("SELECT * FROM PurchaseUnitEntity")
    fun getAllUnits(): Flow<List<PurchaseUnitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnit(unit: PurchaseUnitEntity)

    @Delete
    suspend fun deleteUnit(unit: PurchaseUnitEntity)
}

@Database(
    entities = [
        ProductEntity::class, 
        BranchEntity::class, 
        SaleEntity::class, 
        SaleItemEntity::class, 
        InventoryEntity::class,
        UserEntity::class,
        CashSessionEntity::class,
        StockMovementEntity::class,
        CustomerEntity::class,
        PosTerminalEntity::class,
        CategoryEntity::class,
        TaxEntity::class,
        CashOutEntity::class,
        CustomerPaymentEntity::class,
        CustomerProductPriceEntity::class,
        AppSettingsEntity::class,
        PurchaseEntity::class,
        PurchaseItemEntity::class,
        HeldSaleEntity::class,
        HeldSaleItemEntity::class,
        EmployeeEntity::class,
        ScheduleEntity::class,
        LoanEntity::class,
        AbsenceReplacementEntity::class,
        CashBoxEntity::class,
        AccountingTransactionEntity::class,
        CorteContaplaEntity::class,
        PaymentRecordEntity::class,
        CashMovementEntity::class,
        PreCutEntity::class,
        SupplierEntity::class,
        SupplierPaymentEntity::class,
        ProductSupplierEntity::class,
        AttendanceEntity::class,
        RolePermissionEntity::class,
        PromotionEntity::class,
        ExpenseEntity::class,
        PurchaseUnitEntity::class,
        DeletionRequestEntity::class,
        DeletionLogEntity::class,
        ProductReturnEntity::class
    ],
    version = 54
)
@ConstructedBy(PosDatabaseConstructor::class)
abstract class PosDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun branchDao(): BranchDao
    abstract fun saleDao(): SaleDao
    abstract fun userDao(): UserDao
    abstract fun cashSessionDao(): CashSessionDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun customerDao(): CustomerDao
    abstract fun posTerminalDao(): PosTerminalDao
    abstract fun categoryDao(): CategoryDao
    abstract fun taxDao(): TaxDao
    abstract fun deletionRequestDao(): DeletionRequestDao
    abstract fun deletionLogDao(): DeletionLogDao
    abstract fun productReturnDao(): ProductReturnDao
    abstract fun cashOutDao(): CashOutDao
    abstract fun cashMovementDao(): CashMovementDao
    abstract fun preCutDao(): PreCutDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun supplierDao(): SupplierDao
    abstract fun supplierPaymentDao(): SupplierPaymentDao
    abstract fun productSupplierDao(): ProductSupplierDao
    abstract fun customerPaymentDao(): CustomerPaymentDao
    abstract fun customerProductPriceDao(): CustomerProductPriceDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun heldSaleDao(): HeldSaleDao
    abstract fun employeeDao(): EmployeeDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun loanDao(): LoanDao
    abstract fun absenceReplacementDao(): AbsenceReplacementDao
    abstract fun cashBoxDao(): CashBoxDao
    abstract fun contaplaTransactionDao(): ContaplaTransactionDao
    abstract fun corteContaplaDao(): CorteContaplaDao
    abstract fun paymentRecordDao(): PaymentRecordDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun rolePermissionDao(): RolePermissionDao
    abstract fun promotionDao(): PromotionDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun purchaseUnitDao(): PurchaseUnitDao

    suspend fun clearAllTablesManual() {
        productDao().clearAll()
        userDao().clearAll()
        customerDao().clearAll()
        branchDao().clearAll()
        saleDao().clearAll()
        saleDao().clearAllItems()
    }

    companion object {
        val MIGRATION_37_38 = object : Migration(37, 38) {
            override fun migrate(connection: SQLiteConnection) {
                try {
                    connection.execSQL("CREATE TABLE IF NOT EXISTS `PromotionEntity` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `productId` TEXT, `category` TEXT, `discountValue` REAL NOT NULL, `triggerQuantity` INTEGER NOT NULL, `startDate` INTEGER NOT NULL, `endDate` INTEGER NOT NULL, `isActive` INTEGER NOT NULL, `lastUpdated` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))")
                } catch (e: Exception) {}
            }
        }

        val MIGRATION_38_40 = object : Migration(38, 40) {
            override fun migrate(connection: SQLiteConnection) {
                // Bridge migration
            }
        }

        val MIGRATION_40_41 = object : Migration(40, 41) {
            override fun migrate(connection: SQLiteConnection) {
                try {
                    connection.execSQL("ALTER TABLE `SaleEntity` ADD COLUMN `receivedAmount` REAL NOT NULL DEFAULT 0.0")
                    connection.execSQL("ALTER TABLE `SaleEntity` ADD COLUMN `changeAmount` REAL NOT NULL DEFAULT 0.0")
                } catch (e: Exception) {}
            }
        }

        val MIGRATION_41_42 = object : Migration(41, 42) {
            override fun migrate(connection: SQLiteConnection) {
                try {
                    connection.execSQL("ALTER TABLE `SaleEntity` ADD COLUMN `isSynced` INTEGER NOT NULL DEFAULT 0")
                    connection.execSQL("ALTER TABLE `CashMovementEntity` ADD COLUMN `isSynced` INTEGER NOT NULL DEFAULT 0")
                    // Intentamos los otros por si acaso, pero la 42->43 lo asegura
                    connection.execSQL("ALTER TABLE `CashOutEntity` ADD COLUMN `isSynced` INTEGER NOT NULL DEFAULT 0")
                    connection.execSQL("ALTER TABLE `PreCutEntity` ADD COLUMN `isSynced` INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
            }
        }

        val MIGRATION_42_43 = object : Migration(42, 43) {
            override fun migrate(connection: SQLiteConnection) {
                // --- Seguridad y Sincronización ---
                try { connection.execSQL("ALTER TABLE `SaleEntity` ADD COLUMN `isSynced` INTEGER NOT NULL DEFAULT 0") } catch (e: Exception) {}
                try { connection.execSQL("ALTER TABLE `CashMovementEntity` ADD COLUMN `isSynced` INTEGER NOT NULL DEFAULT 0") } catch (e: Exception) {}
                try { connection.execSQL("ALTER TABLE `CashOutEntity` ADD COLUMN `isSynced` INTEGER NOT NULL DEFAULT 0") } catch (e: Exception) {}
                try { connection.execSQL("ALTER TABLE `PreCutEntity` ADD COLUMN `isSynced` INTEGER NOT NULL DEFAULT 0") } catch (e: Exception) {}
                
                // --- Usuarios ---
                try { connection.execSQL("ALTER TABLE `UserEntity` ADD COLUMN `isActive` INTEGER NOT NULL DEFAULT 1") } catch (e: Exception) {}
                try { connection.execSQL("ALTER TABLE `UserEntity` ADD COLUMN `lastLoginTime` INTEGER") } catch (e: Exception) {}
                
                // --- Clientes ---
                try { connection.execSQL("ALTER TABLE `CustomerEntity` ADD COLUMN `walletBalance` REAL NOT NULL DEFAULT 0.0") } catch (e: Exception) {}
                try { connection.execSQL("ALTER TABLE `CustomerEntity` ADD COLUMN `creditLimitWeekly` REAL NOT NULL DEFAULT 0.0") } catch (e: Exception) {}
                try { connection.execSQL("ALTER TABLE `CustomerEntity` ADD COLUMN `creditDays` INTEGER NOT NULL DEFAULT 0") } catch (e: Exception) {}
                
                // --- Varios ---
                try { connection.execSQL("ALTER TABLE `SaleItemEntity` ADD COLUMN `category` TEXT NOT NULL DEFAULT 'General'") } catch (e: Exception) {}
                try { connection.execSQL("ALTER TABLE `SaleItemEntity` ADD COLUMN `isService` INTEGER NOT NULL DEFAULT 0") } catch (e: Exception) {}
            }
        }

        val MIGRATION_43_44 = object : Migration(43, 44) {
            override fun migrate(connection: SQLiteConnection) {
                // Add new columns to PurchaseItemEntity
                try { connection.execSQL("ALTER TABLE `PurchaseItemEntity` ADD COLUMN `purchaseUnit` TEXT NOT NULL DEFAULT 'PZA'") } catch (e: Exception) {}
                try { connection.execSQL("ALTER TABLE `PurchaseItemEntity` ADD COLUMN `purchaseFactor` REAL NOT NULL DEFAULT 1.0") } catch (e: Exception) {}
                try { connection.execSQL("ALTER TABLE `PurchaseItemEntity` ADD COLUMN `purchaseQuantity` REAL NOT NULL DEFAULT 0.0") } catch (e: Exception) {}
                try { connection.execSQL("ALTER TABLE `PurchaseItemEntity` ADD COLUMN `purchaseCost` REAL NOT NULL DEFAULT 0.0") } catch (e: Exception) {}
                try { connection.execSQL("ALTER TABLE `PurchaseItemEntity` ADD COLUMN `discountPercent` REAL NOT NULL DEFAULT 0.0") } catch (e: Exception) {}
                try { connection.execSQL("ALTER TABLE `PurchaseItemEntity` ADD COLUMN `taxRate` REAL NOT NULL DEFAULT 0.0") } catch (e: Exception) {}
                
                // Create PurchaseUnitEntity table
                try {
                    connection.execSQL("CREATE TABLE IF NOT EXISTS `PurchaseUnitEntity` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `factor` REAL NOT NULL, `lastUpdated` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))")
                } catch (e: Exception) {}
            }
        }

        val MIGRATION_44_45 = object : Migration(44, 45) {
            override fun migrate(connection: SQLiteConnection) {
                try {
                    connection.execSQL("ALTER TABLE `ProductEntity` ADD COLUMN `lastPurchaseUnit` TEXT")
                    connection.execSQL("ALTER TABLE `ProductEntity` ADD COLUMN `lastPurchaseFactor` REAL NOT NULL DEFAULT 1.0")
                    connection.execSQL("ALTER TABLE `ProductEntity` ADD COLUMN `lastPurchaseTax` REAL NOT NULL DEFAULT 0.0")
                    connection.execSQL("ALTER TABLE `ProductEntity` ADD COLUMN `lastPurchaseCost` REAL NOT NULL DEFAULT 0.0")
                } catch (e: Exception) {}
            }
        }

        val MIGRATION_45_46 = object : Migration(45, 46) {
            override fun migrate(connection: SQLiteConnection) {
                try { connection.execSQL("ALTER TABLE `ProductEntity` ADD COLUMN `barcode2` TEXT") } catch (e: Exception) {}
                try { connection.execSQL("ALTER TABLE `ProductEntity` ADD COLUMN `barcode3` TEXT") } catch (e: Exception) {}
                try { connection.execSQL("ALTER TABLE `ProductEntity` ADD COLUMN `barcode4` TEXT") } catch (e: Exception) {}
                try { connection.execSQL("ALTER TABLE `ProductEntity` ADD COLUMN `satCode` TEXT") } catch (e: Exception) {}
                try { connection.execSQL("ALTER TABLE `ProductEntity` ADD COLUMN `ieps` REAL NOT NULL DEFAULT 0.0") } catch (e: Exception) {}
                try { connection.execSQL("ALTER TABLE `ProductEntity` ADD COLUMN `showInWebShop` INTEGER NOT NULL DEFAULT 0") } catch (e: Exception) {}
                
                // Nuevos campos para Inventario por Sucursal
                try { connection.execSQL("ALTER TABLE `InventoryEntity` ADD COLUMN `minStock` REAL NOT NULL DEFAULT 0.0") } catch (e: Exception) {}
                try { connection.execSQL("ALTER TABLE `InventoryEntity` ADD COLUMN `maxStock` REAL NOT NULL DEFAULT 0.0") } catch (e: Exception) {}
            }
        }

        val MIGRATION_36_37 = object : Migration(36, 37) {
            override fun migrate(connection: SQLiteConnection) {
                // No hay cambios de esquema en este salto, solo para asegurar que no se borren datos
            }
        }
        
        val MIGRATION_35_36 = object : Migration(35, 36) {
            override fun migrate(connection: SQLiteConnection) {
                try {
                    connection.execSQL("ALTER TABLE `CustomerPaymentEntity` ADD COLUMN `paymentMethod` TEXT NOT NULL DEFAULT 'Efectivo'")
                } catch (e: Exception) {}
            }
        }
    }
}

// Room Database Constructor for KMP
expect object PosDatabaseConstructor : RoomDatabaseConstructor<PosDatabase> {
    override fun initialize(): PosDatabase
}
