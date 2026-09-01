package com.abtsplazita.posplazita.data

import com.abtsplazita.posplazita.data.local.*
import com.abtsplazita.posplazita.domain.*

// --- Product Mappers ---
fun ProductEntity.toDomain() = Product(
    id = id,
    name = name,
    barcode = barcode,
    category = category,
    imagePath = imagePath,
    unit = try { UnitType.valueOf(unit) } catch (e: Exception) { UnitType.PIECE },
    tax = tax,
    cost = cost,
    isBulk = isBulk,
    useScale = useScale,
    price1 = price1,
    price2 = price2,
    price3 = price3,
    price4 = price4,
    isService = isService,
    barcode2 = barcode2,
    barcode3 = barcode3,
    barcode4 = barcode4,
    satCode = satCode,
    ieps = ieps,
    showInWebShop = showInWebShop,
    lastUpdated = lastUpdated,
    lastPurchaseUnit = lastPurchaseUnit,
    lastPurchaseFactor = lastPurchaseFactor,
    lastPurchaseTax = lastPurchaseTax,
    lastPurchaseCost = lastPurchaseCost
)

fun Product.toEntity() = ProductEntity(
    id = id,
    name = name,
    barcode = barcode,
    category = category,
    imagePath = imagePath,
    unit = unit.name,
    tax = tax,
    cost = cost,
    isBulk = isBulk,
    useScale = useScale,
    price1 = price1,
    price2 = price2,
    price3 = price3,
    price4 = price4,
    isService = isService,
    barcode2 = barcode2,
    barcode3 = barcode3,
    barcode4 = barcode4,
    satCode = satCode,
    ieps = ieps,
    showInWebShop = showInWebShop,
    lastUpdated = lastUpdated,
    lastPurchaseUnit = lastPurchaseUnit,
    lastPurchaseFactor = lastPurchaseFactor,
    lastPurchaseTax = lastPurchaseTax,
    lastPurchaseCost = lastPurchaseCost
)

// --- Inventory Mappers ---
fun InventoryEntity.toDomain() = Inventory(
    productId = productId,
    branchId = branchId,
    stock = stock,
    minStock = minStock,
    maxStock = maxStock,
    lastUpdated = lastUpdated
)

fun Inventory.toEntity() = InventoryEntity(
    productId = productId,
    branchId = branchId,
    stock = stock,
    minStock = minStock,
    maxStock = maxStock,
    lastUpdated = lastUpdated
)

// --- Branch Mappers ---
fun BranchEntity.toDomain() = Branch(
    id = id,
    name = name,
    address = address,
    lastUpdated = lastUpdated
)

fun Branch.toEntity() = BranchEntity(
    id = id,
    name = name,
    address = address,
    lastUpdated = lastUpdated
)

// --- User Mappers ---
fun UserEntity.toDomain() = User(
    id = id,
    username = username,
    firstName = firstName,
    lastName = lastName,
    nip = nip,
    role = try { Role.valueOf(role) } catch (e: Exception) { Role.CAJERO },
    phone = phone,
    employeeId = employeeId,
    mustChangeNip = mustChangeNip,
    lastLoginTime = lastLoginTime,
    isActive = isActive,
    lastUpdated = lastUpdated
)

fun User.toEntity() = UserEntity(
    id = id,
    username = username,
    firstName = firstName,
    lastName = lastName,
    nip = nip,
    role = role.name,
    phone = phone,
    employeeId = employeeId,
    mustChangeNip = mustChangeNip,
    lastLoginTime = lastLoginTime,
    isActive = isActive,
    lastUpdated = lastUpdated
)

// --- Sale Mappers ---
fun SaleEntity.toDomain(items: List<SaleItem>) = Sale(
    id = id,
    timestamp = timestamp,
    userId = userId,
    branchId = branchId,
    terminalId = terminalId,
    customerId = customerId,
    items = items,
    total = total,
    netTotal = netTotal,
    cashAmount = cashAmount,
    creditAmount = creditAmount,
    receivedAmount = receivedAmount,
    changeAmount = changeAmount,
    paymentMethod = paymentMethod,
    status = try { SaleStatus.valueOf(status) } catch (e: Exception) { SaleStatus.COMPLETED },
    comment = comment,
    isSynced = isSynced,
    originalWebOrderId = originalWebOrderId
)

fun Sale.toEntity() = SaleEntity(
    id = id,
    timestamp = timestamp,
    userId = userId,
    branchId = branchId,
    terminalId = terminalId,
    customerId = customerId,
    total = total,
    netTotal = netTotal,
    cashAmount = cashAmount,
    creditAmount = creditAmount,
    receivedAmount = receivedAmount,
    changeAmount = changeAmount,
    paymentMethod = paymentMethod,
    status = status.name,
    comment = comment,
    isSynced = isSynced,
    originalWebOrderId = originalWebOrderId
)

fun SaleItemEntity.toDomain() = SaleItem(
    productId = productId,
    productName = productName,
    productImagePath = productImagePath,
    quantity = quantity,
    priceAtSale = priceAtSale,
    subtotal = subtotal,
    category = category,
    isService = isService,
    isBulk = isBulk,
    isWebDiscounted = isWebDiscounted
)

fun SaleItem.toEntity(saleId: String) = SaleItemEntity(
    saleId = saleId,
    productId = productId,
    productName = productName,
    productImagePath = productImagePath,
    quantity = quantity,
    priceAtSale = priceAtSale,
    subtotal = subtotal,
    category = category,
    isService = isService,
    isBulk = isBulk,
    isWebDiscounted = isWebDiscounted
)

// --- CashSession Mappers ---
fun CashSessionEntity.toDomain() = CashSession(
    id = id,
    userId = userId,
    openingTime = openingTime,
    closingTime = closingTime,
    initialAmount = initialAmount,
    declaredAmount = declaredAmount,
    systemAmount = systemAmount,
    status = CashSessionStatus.valueOf(status)
)

fun CashSession.toEntity() = CashSessionEntity(
    id = id,
    userId = userId,
    openingTime = openingTime,
    closingTime = closingTime,
    initialAmount = initialAmount,
    declaredAmount = declaredAmount,
    systemAmount = systemAmount,
    status = status.name
)

// --- StockMovement Mappers ---
fun StockMovementEntity.toDomain() = StockMovement(
    id = id,
    productId = productId,
    branchId = branchId,
    type = MovementType.valueOf(type),
    quantity = quantity,
    timestamp = timestamp,
    userId = userId,
    reason = reason
)

fun StockMovement.toEntity() = StockMovementEntity(
    id = id,
    productId = productId,
    branchId = branchId,
    type = type.name,
    quantity = quantity,
    timestamp = timestamp,
    userId = userId,
    reason = reason
)

// --- Customer Mappers ---
fun CustomerEntity.toDomain() = Customer(
    id = id,
    name = name,
    email = email,
    phone = phone,
    creditLimit = creditLimit,
    creditLimitWeekly = creditLimitWeekly,
    creditDays = creditDays,
    currentDebt = currentDebt,
    loyaltyPoints = loyaltyPoints,
    walletBalance = walletBalance,
    lastUpdated = lastUpdated
)

fun Customer.toEntity() = CustomerEntity(
    id = id,
    name = name,
    email = email,
    phone = phone,
    creditLimit = creditLimit,
    creditLimitWeekly = creditLimitWeekly,
    creditDays = creditDays,
    currentDebt = currentDebt,
    loyaltyPoints = loyaltyPoints,
    walletBalance = walletBalance,
    lastUpdated = lastUpdated
)

fun CustomerPaymentEntity.toDomain() = CustomerPayment(
    id = id,
    customerId = customerId,
    amount = amount,
    timestamp = timestamp,
    userId = userId,
    paymentMethod = paymentMethod,
    notes = notes
)

fun CustomerPayment.toEntity() = CustomerPaymentEntity(
    id = id,
    customerId = customerId,
    amount = amount,
    timestamp = timestamp,
    userId = userId,
    paymentMethod = paymentMethod,
    notes = notes
)

fun CustomerProductPriceEntity.toDomain() = CustomerProductPrice(
    customerId = customerId,
    productId = productId,
    specialPrice = specialPrice
)

fun CustomerProductPrice.toEntity() = CustomerProductPriceEntity(
    customerId = customerId,
    productId = productId,
    specialPrice = specialPrice
)

// --- PosTerminal Mappers ---
fun PosTerminalEntity.toDomain() = PosTerminal(
    id = id,
    branchId = branchId,
    name = name,
    isActive = isActive,
    lastUpdated = lastUpdated
)

fun PosTerminal.toEntity() = PosTerminalEntity(
    id = id,
    branchId = branchId,
    name = name,
    isActive = isActive,
    lastUpdated = lastUpdated
)

// --- Category Mapper ---
fun CategoryEntity.toDomain() = Category(id, name)
fun Category.toEntity() = CategoryEntity(id, name)

// --- Tax Mapper ---
fun TaxEntity.toDomain() = Tax(id, name, rate)
fun Tax.toEntity() = TaxEntity(id, name, rate)

// --- CashOut Mappers ---
fun CashOutEntity.toDomain() = CashOut(
    id = id,
    timestamp = timestamp,
    branchId = branchId,
    terminalId = terminalId,
    expectedAmount = expectedAmount,
    countedAmount = countedAmount,
    difference = difference,
    ticketCount = ticketCount,
    userId = userId,
    isSynced = isSynced
)

fun CashOut.toEntity() = CashOutEntity(
    id = id,
    timestamp = timestamp,
    branchId = branchId,
    terminalId = terminalId,
    expectedAmount = expectedAmount,
    countedAmount = countedAmount,
    difference = difference,
    ticketCount = ticketCount,
    userId = userId,
    isSynced = isSynced
)

// --- CashMovement Mappers ---
fun CashMovementEntity.toDomain() = CashMovement(
    id = id,
    timestamp = timestamp,
    branchId = branchId,
    terminalId = terminalId,
    type = CashMovementType.valueOf(type),
    amount = amount,
    reason = reason,
    userId = userId,
    isSynced = isSynced
)

fun CashMovement.toEntity() = CashMovementEntity(
    id = id,
    timestamp = timestamp,
    branchId = branchId,
    terminalId = terminalId,
    type = type.name,
    amount = amount,
    reason = reason,
    userId = userId,
    isSynced = isSynced
)

// --- PreCut Mappers ---
fun PreCutEntity.toDomain() = PreCut(
    id = id,
    timestamp = timestamp,
    branchId = branchId,
    terminalId = terminalId,
    expectedAmount = expectedAmount,
    countedAmount = countedAmount,
    difference = difference,
    userId = userId,
    isSynced = isSynced
)

fun PreCut.toEntity() = PreCutEntity(
    id = id,
    timestamp = timestamp,
    branchId = branchId,
    terminalId = terminalId,
    expectedAmount = expectedAmount,
    countedAmount = countedAmount,
    difference = difference,
    userId = userId,
    isSynced = isSynced
)

// --- Supplier Mappers ---
fun SupplierEntity.toDomain() = Supplier(
    id = id,
    name = name,
    contactName = contactName,
    phone = phone,
    email = email,
    address = address,
    givesCredit = givesCredit,
    creditDays = creditDays,
    currentDebt = currentDebt
)

fun Supplier.toEntity() = SupplierEntity(
    id = id,
    name = name,
    contactName = contactName,
    phone = phone,
    email = email,
    address = address,
    givesCredit = givesCredit,
    creditDays = creditDays,
    currentDebt = currentDebt
)

fun SupplierPaymentEntity.toDomain() = SupplierPayment(
    id = id,
    supplierId = supplierId,
    amount = amount,
    timestamp = timestamp,
    method = method,
    userId = userId,
    notes = notes
)

fun SupplierPayment.toEntity() = SupplierPaymentEntity(
    id = id,
    supplierId = supplierId,
    amount = amount,
    timestamp = timestamp,
    method = method,
    userId = userId,
    notes = notes
)

fun ProductSupplierEntity.toDomain() = ProductSupplier(
    productId = productId,
    supplierId = supplierId,
    lastCost = lastCost,
    lastPurchaseDate = lastPurchaseDate
)

fun ProductSupplier.toEntity() = ProductSupplierEntity(
    productId = productId,
    supplierId = supplierId,
    lastCost = lastCost,
    lastPurchaseDate = lastPurchaseDate
)

// --- AppSettings Mappers ---
fun AppSettingsEntity.toDomain() = AppSettings(key, value)
fun AppSettings.toEntity() = AppSettingsEntity(key, value)

// --- Purchase Mappers ---
fun PurchaseEntity.toDomain(items: List<PurchaseItem>) = Purchase(
    id = id,
    timestamp = timestamp,
    userId = userId,
    branchId = branchId,
    supplierId = supplierId,
    items = items,
    total = total,
    paymentMethod = paymentMethod,
    status = PurchaseStatus.valueOf(status)
)

fun Purchase.toEntity() = PurchaseEntity(
    id = id,
    timestamp = timestamp,
    userId = userId,
    branchId = branchId,
    supplierId = supplierId,
    total = total,
    paymentMethod = paymentMethod,
    status = status.name
)

fun PurchaseItemEntity.toDomain() = PurchaseItem(
    productId = productId,
    productName = productName,
    quantity = quantity,
    costAtPurchase = costAtPurchase,
    subtotal = subtotal,
    purchaseUnit = purchaseUnit,
    purchaseFactor = purchaseFactor,
    purchaseQuantity = purchaseQuantity,
    purchaseCost = purchaseCost,
    discountPercent = discountPercent,
    taxRate = taxRate
)

fun PurchaseItem.toEntity(purchaseId: String) = PurchaseItemEntity(
    purchaseId = purchaseId,
    productId = productId,
    productName = productName,
    quantity = quantity,
    costAtPurchase = costAtPurchase,
    subtotal = subtotal,
    purchaseUnit = purchaseUnit,
    purchaseFactor = purchaseFactor,
    purchaseQuantity = purchaseQuantity,
    purchaseCost = purchaseCost,
    discountPercent = discountPercent,
    taxRate = taxRate
)

fun PurchaseUnitEntity.toDomain() = PurchaseUnit(id, name, factor, lastUpdated)
fun PurchaseUnit.toEntity() = PurchaseUnitEntity(id, name, factor, lastUpdated)

// --- HeldSale Mappers ---
fun HeldSaleEntity.toDomain(items: List<SaleItem>) = HeldSale(
    id = id,
    timestamp = timestamp,
    branchId = branchId,
    terminalId = terminalId,
    items = items,
    customerId = customerId,
    total = total
)

fun HeldSale.toEntity() = HeldSaleEntity(
    id = id,
    timestamp = timestamp,
    branchId = branchId,
    terminalId = terminalId,
    customerId = customerId,
    total = total
)

fun HeldSaleItemEntity.toDomain() = SaleItem(
    productId = productId,
    productName = productName,
    productImagePath = productImagePath,
    quantity = quantity,
    priceAtSale = priceAtSale,
    subtotal = subtotal,
    category = category,
    isService = isService,
    isBulk = isBulk,
    isWebDiscounted = isWebDiscounted
)

fun SaleItem.toHeldEntity(heldSaleId: String) = HeldSaleItemEntity(
    heldSaleId = heldSaleId,
    productId = productId,
    productName = productName,
    productImagePath = productImagePath,
    quantity = quantity,
    priceAtSale = priceAtSale,
    subtotal = subtotal,
    category = category,
    isService = isService,
    isBulk = isBulk,
    isWebDiscounted = isWebDiscounted
)

// --- Employee Mappers ---
fun EmployeeEntity.toDomain() = Employee(
    id = id,
    fullName = fullName,
    phoneNumber = phoneNumber,
    branch = branch,
    baseSalary = baseSalary,
    bonus = bonus,
    vacationWeeks = vacationWeeks,
    lastPaidTimestamp = lastPaidTimestamp,
    color = color
)

fun Employee.toEntity() = EmployeeEntity(
    id = id,
    fullName = fullName,
    phoneNumber = phoneNumber,
    branch = branch,
    baseSalary = baseSalary,
    bonus = bonus,
    vacationWeeks = vacationWeeks,
    lastPaidTimestamp = lastPaidTimestamp,
    color = color
)

fun ScheduleEntity.toDomain() = Schedule(
    id = id,
    employeeId = employeeId,
    dayOfWeek = dayOfWeek,
    checkInTime = checkInTime,
    checkOutTime = checkOutTime,
    isRestDay = isRestDay,
    branchName = branchName
)

fun Schedule.toEntity() = ScheduleEntity(
    id = id,
    employeeId = employeeId,
    dayOfWeek = dayOfWeek,
    checkInTime = checkInTime,
    checkOutTime = checkOutTime,
    isRestDay = isRestDay,
    branchName = branchName
)

fun LoanEntity.toDomain() = Loan(id, employeeId, amount, date, isPaid)
fun Loan.toEntity() = LoanEntity(id, employeeId, amount, date, isPaid)

fun AbsenceReplacementEntity.toDomain() = AbsenceReplacement(
    id = id,
    absentEmployeeId = absentEmployeeId,
    replacementEmployeeId = replacementEmployeeId,
    date = date,
    shiftDetails = shiftDetails,
    isExcused = isExcused,
    replacementType = replacementType
)

fun AbsenceReplacement.toEntity() = AbsenceReplacementEntity(
    id = id,
    absentEmployeeId = absentEmployeeId,
    replacementEmployeeId = replacementEmployeeId,
    date = date,
    shiftDetails = shiftDetails,
    isExcused = isExcused,
    replacementType = replacementType
)

fun CashBoxEntity.toDomain() = CashBox(id, branchId, name, currentBalance)
fun CashBox.toEntity() = CashBoxEntity(id, branchId, name, currentBalance)

fun AccountingTransactionEntity.toDomain() = AccountingTransaction(
    id = id,
    type = TransactionType.valueOf(type),
    amount = amount,
    concept = concept,
    timestamp = timestamp,
    initialBalance = initialBalance,
    finalBalance = finalBalance
)

fun AccountingTransaction.toEntity() = AccountingTransactionEntity(
    id = id,
    type = type.name,
    amount = amount,
    concept = concept,
    timestamp = timestamp,
    initialBalance = initialBalance,
    finalBalance = finalBalance
)

fun CorteContaplaEntity.toDomain() = CorteContapla(id, branchId, cashBoxId, amount, timestamp)
fun CorteContapla.toEntity() = CorteContaplaEntity(id, branchId, cashBoxId, amount, timestamp)

fun PaymentRecordEntity.toDomain() = PaymentRecord(id, employeeId, employeeName, date, amount, reportText)
fun PaymentRecord.toEntity() = PaymentRecordEntity(id, employeeId, employeeName, date, amount, reportText)

fun AttendanceEntity.toDomain() = AttendanceRecord(id, userId, employeeId, startTime, endTime, hoursWorked, payAmount, isClosed)
fun AttendanceRecord.toEntity() = AttendanceEntity(id, userId, employeeId, startTime, endTime, hoursWorked, payAmount, isClosed)

fun RolePermissionEntity.toDomain() = RolePermission(Role.valueOf(role), Permission.valueOf(permission), PermissionLevel.valueOf(level))
fun RolePermission.toEntity() = RolePermissionEntity(role.name, permission.name, level.name)

fun PromotionEntity.toDomain() = Promotion(id, name, PromotionType.valueOf(type), productId, category, discountValue, triggerQuantity, startDate, endDate, isActive, lastUpdated)
fun Promotion.toEntity() = PromotionEntity(id, name, type.name, productId, category, discountValue, triggerQuantity, startDate, endDate, isActive, lastUpdated)

fun DeletionRequestEntity.toDomain() = DeletionRequest(id, ticketId, timestamp, userId, total, itemsSummary, branchId, status)
fun DeletionRequest.toEntity() = DeletionRequestEntity(id, ticketId, timestamp, userId, total, itemsSummary, branchId, status)

fun DeletionLogEntity.toDomain() = DeletionLog(id, ticketId, timestamp, requesterId, approverId, total, itemsSummary, branchId, reason)
fun DeletionLog.toEntity() = DeletionLogEntity(id, ticketId, timestamp, requesterId, approverId, total, itemsSummary, branchId, reason)

fun ExpenseEntity.toDomain() = Expense(
    id = id,
    category = ExpenseCategory.valueOf(category),
    amount = amount,
    timestamp = timestamp,
    branchId = branchId,
    reason = reason,
    userId = userId,
    lastUpdated = lastUpdated
)

fun Expense.toEntity() = ExpenseEntity(
    id = id,
    category = category.name,
    amount = amount,
    timestamp = timestamp,
    branchId = branchId,
    reason = reason,
    userId = userId,
    lastUpdated = lastUpdated
)

// --- ProductReturn Mappers ---
fun ProductReturnEntity.toDomain() = ProductReturn(
    id = id,
    timestamp = timestamp,
    branchId = branchId,
    returnedItem = SaleItem(returnedProductId, returnedProductName, null, returnedQuantity, returnedPrice, returnedQuantity * returnedPrice),
    takenItem = if (takenProductId != null) SaleItem(takenProductId, takenProductName!!, null, takenQuantity!!, takenPrice!!, takenQuantity * takenPrice!!) else null,
    difference = difference,
    userId = userId,
    reason = reason
)

fun ProductReturn.toEntity() = ProductReturnEntity(
    id = id,
    timestamp = timestamp,
    branchId = branchId,
    returnedProductId = returnedItem.productId,
    returnedProductName = returnedItem.productName,
    returnedQuantity = returnedItem.quantity,
    returnedPrice = returnedItem.priceAtSale,
    takenProductId = takenItem?.productId,
    takenProductName = takenItem?.productName,
    takenQuantity = takenItem?.quantity,
    takenPrice = takenItem?.priceAtSale,
    difference = difference,
    userId = userId,
    reason = reason,
    isSynced = false
)
