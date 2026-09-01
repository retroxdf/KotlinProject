package com.abtsplazita.posplazita.domain

import kotlinx.serialization.Serializable

@Serializable
data class Employee(
    val id: Long = 0,
    val fullName: String,
    val phoneNumber: String,
    val branch: String,
    val baseSalary: Double,
    val bonus: Double = 0.0,
    val vacationWeeks: Int = 0,
    val lastPaidTimestamp: Long = 0L,
    val color: Int = -16711681 // Cyan por defecto
)

@Serializable
data class Schedule(
    val id: Long = 0,
    val employeeId: Long,
    val dayOfWeek: Int, // 1 (Lunes) a 7 (Domingo)
    val checkInTime: String?, // Formato "HH:mm"
    val checkOutTime: String?, // Formato "HH:mm"
    val isRestDay: Boolean = false,
    val branchName: String? = null
)

@Serializable
data class Loan(
    val id: Long = 0,
    val employeeId: Long,
    val amount: Double,
    val date: Long,
    val isPaid: Boolean = false
)

@Serializable
data class AbsenceReplacement(
    val id: Long = 0,
    val absentEmployeeId: Long,
    val replacementEmployeeId: Long?,
    val date: Long,
    val shiftDetails: String? = null,
    val isExcused: Boolean = false,
    val replacementType: String = "EXTRA"
)

@Serializable
data class CashBox(
    val id: Long = 0,
    val branchId: String, // Usamos String para compatibilidad con Branch de KotlinProject
    val name: String,
    val currentBalance: Double = 0.0
)

@Serializable
data class AccountingTransaction( // Renombrado de CashTransaction para evitar conflictos
    val id: Long = 0,
    val type: TransactionType,
    val amount: Double,
    val concept: String,
    val timestamp: Long,
    val initialBalance: Double,
    val finalBalance: Double
)

@Serializable
enum class TransactionType {
    INCOME, EXPENSE
}

@Serializable
data class CorteContapla( // Renombrado de Corte para evitar conflictos con CashOut si los hay
    val id: Long = 0,
    val branchId: String,
    val cashBoxId: Long,
    val amount: Double,
    val timestamp: Long
)

@Serializable
data class PaymentRecord(
    val id: Long = 0,
    val employeeId: Long,
    val employeeName: String,
    val date: Long,
    val amount: Double,
    val reportText: String
)

@Serializable
data class AttendanceRecord(
    val id: Long = 0,
    val userId: String,
    val employeeId: Long?,
    val startTime: Long,
    val endTime: Long? = null,
    val hoursWorked: Double = 0.0,
    val payAmount: Double = 0.0,
    val isClosed: Boolean = false
)
