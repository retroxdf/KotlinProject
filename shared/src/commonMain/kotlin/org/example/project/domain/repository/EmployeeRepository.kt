package com.abtsplazita.posplazita.domain.repository

import com.abtsplazita.posplazita.data.local.*
import com.abtsplazita.posplazita.data.*
import com.abtsplazita.posplazita.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EmployeeRepository(
    private val employeeDao: EmployeeDao,
    private val scheduleDao: ScheduleDao,
    private val loanDao: LoanDao,
    private val absenceReplacementDao: AbsenceReplacementDao,
    private val cashBoxDao: CashBoxDao,
    private val transactionDao: ContaplaTransactionDao,
    private val corteDao: CorteContaplaDao,
    private val paymentRecordDao: PaymentRecordDao,
    private val attendanceDao: AttendanceDao
) {
    // Employees
    val allEmployees: Flow<List<Employee>> = employeeDao.getAllEmployees().map { list -> list.map { it.toDomain() } }

    suspend fun getEmployeeById(id: Long): Employee? = employeeDao.getEmployeeById(id)?.toDomain()

    suspend fun insertEmployee(employee: Employee): Long = employeeDao.insertEmployee(employee.toEntity())

    suspend fun updateEmployee(employee: Employee) = employeeDao.updateEmployee(employee.toEntity())

    suspend fun deleteEmployee(employee: Employee) = employeeDao.deleteEmployee(employee.toEntity())

    // Schedules
    val allSchedules: Flow<List<Schedule>> = scheduleDao.getAllSchedules().map { list -> list.map { it.toDomain() } }

    fun getSchedulesForEmployee(employeeId: Long): Flow<List<Schedule>> =
        scheduleDao.getSchedulesForEmployee(employeeId).map { list -> list.map { it.toDomain() } }

    suspend fun insertSchedule(schedule: Schedule) = scheduleDao.insertSchedule(schedule.toEntity())

    suspend fun updateSchedule(schedule: Schedule) = scheduleDao.updateSchedule(schedule.toEntity())

    suspend fun deleteSchedule(schedule: Schedule) = scheduleDao.deleteSchedule(schedule.toEntity())
    
    suspend fun deleteSchedulesForEmployee(employeeId: Long) = scheduleDao.deleteSchedulesForEmployee(employeeId)

    // Loans
    val allLoans: Flow<List<Loan>> = loanDao.getAllLoans().map { list -> list.map { it.toDomain() } }

    fun getLoansForEmployee(employeeId: Long): Flow<List<Loan>> =
        loanDao.getLoansForEmployee(employeeId).map { list -> list.map { it.toDomain() } }

    suspend fun insertLoan(loan: Loan) = loanDao.insertLoan(loan.toEntity())

    suspend fun getPendingLoanTotal(employeeId: Long): Double =
        loanDao.getPendingLoanTotal(employeeId) ?: 0.0

    suspend fun markLoansAsPaid(employeeId: Long) =
        loanDao.markLoansAsPaid(employeeId)

    // Absence/Replacement
    val allAbsenceReplacements: Flow<List<AbsenceReplacement>> =
        absenceReplacementDao.getAllAbsenceReplacements().map { list -> list.map { it.toDomain() } }

    fun getReplacementsForEmployee(employeeId: Long): Flow<List<AbsenceReplacement>> =
        absenceReplacementDao.getReplacementsForEmployee(employeeId).map { list -> list.map { it.toDomain() } }

    suspend fun insertAbsenceReplacement(absenceReplacement: AbsenceReplacement) =
        absenceReplacementDao.insertAbsenceReplacement(absenceReplacement.toEntity())

    suspend fun updateAbsenceReplacement(absenceReplacement: AbsenceReplacement) =
        absenceReplacementDao.updateAbsenceReplacement(absenceReplacement.toEntity())

    suspend fun deleteAbsenceReplacement(absenceReplacement: AbsenceReplacement) =
        absenceReplacementDao.deleteAbsenceReplacement(absenceReplacement.toEntity())

    suspend fun getReplacementsInDateRange(startDate: Long, endDate: Long): List<AbsenceReplacement> =
        absenceReplacementDao.getReplacementsInDateRange(startDate, endDate).map { it.toDomain() }

    // Cash Boxes
    val allCashBoxes: Flow<List<CashBox>> = cashBoxDao.getAllCashBoxes().map { list -> list.map { it.toDomain() } }

    fun getCashBoxesByBranch(branchId: String): Flow<List<CashBox>> = cashBoxDao.getCashBoxesByBranch(branchId).map { list -> list.map { it.toDomain() } }

    suspend fun insertCashBox(cashBox: CashBox) = cashBoxDao.insertCashBox(cashBox.toEntity())

    suspend fun deleteCashBox(cashBox: CashBox) = cashBoxDao.deleteCashBox(cashBox.toEntity())

    suspend fun updateCashBox(cashBox: CashBox) = cashBoxDao.updateCashBox(cashBox.toEntity())

    // Accounting Transactions
    val allTransactions: Flow<List<AccountingTransaction>> = transactionDao.getAllTransactions().map { list -> list.map { it.toDomain() } }

    suspend fun insertTransaction(transaction: AccountingTransaction) = 
        transactionDao.insertTransaction(transaction.toEntity())

    suspend fun getLastBalance(): Double = transactionDao.getLastBalance() ?: 0.0

    // Cortes
    val allCortes: Flow<List<CorteContapla>> = corteDao.getAllCortes().map { list -> list.map { it.toDomain() } }

    suspend fun insertCorte(corte: CorteContapla) = corteDao.insertCorte(corte.toEntity())

    // Payment Records
    val allPaymentRecords: Flow<List<PaymentRecord>> = paymentRecordDao.getAllPaymentRecords().map { list -> list.map { it.toDomain() } }

    val allAttendance: Flow<List<AttendanceRecord>> = attendanceDao.getAllAttendance().map { list -> list.map { it.toDomain() } }

    suspend fun getOpenShift(userId: String): AttendanceRecord? {
        return attendanceDao.getOpenShift(userId)?.toDomain()
    }

    suspend fun insertAttendance(record: AttendanceRecord) {
        attendanceDao.insertAttendance(record.toEntity())
    }

    suspend fun updateAttendance(record: AttendanceRecord) {
        attendanceDao.updateAttendance(record.toEntity())
    }

    suspend fun insertPaymentRecord(record: PaymentRecord) = paymentRecordDao.insertPaymentRecord(record.toEntity())

    suspend fun deletePaymentRecord(record: PaymentRecord) = paymentRecordDao.deletePaymentRecord(record.toEntity())
}
