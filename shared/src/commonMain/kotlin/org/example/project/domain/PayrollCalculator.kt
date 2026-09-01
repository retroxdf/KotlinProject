package com.abtsplazita.posplazita.domain

import kotlinx.datetime.*
import kotlin.math.abs

class PayrollCalculator {

    data class DayDetail(
        val dayOfWeek: Int,
        val dayName: String,
        val date: Long,
        val scheduledHours: Double,
        val workedHours: Double,
        val salaryEarned: Double,
        val isAbsence: Boolean,
        val isJustified: Boolean,
        val isRestDay: Boolean,
        val isAlreadyPaid: Boolean,
        val coverageType: String? = null, 
        val coverageAmount: Double = 0.0,
        val branchName: String? = null,
        val checkIn: String? = null,
        val checkOut: String? = null
    )

    data class PayrollResult(
        val employee: Employee,
        val dailyBreakdown: List<DayDetail>,
        val totalHoursWorked: Double,
        val salaryEarned: Double,
        val proportionalBonus: Double,
        val coveragePay: Double,
        val absenceDeduction: Double,
        val pendingLoans: List<Loan>,
        val totalLoansAmount: Double,
        val totalNet: Double,
        val detailedReport: String,
        val weekStart: Long,
        val weekEnd: Long
    )

    fun calculatePayroll(
        employee: Employee,
        schedules: List<Schedule>,
        replacements: List<AbsenceReplacement>,
        allEmployees: List<Employee>, 
        loans: List<Loan>,
        attendance: List<AttendanceRecord> = emptyList(),
        baseSalary8h: Double = 315.0,
        weekStartTimestamp: Long 
    ): PayrollResult {
        
        val hourlyRate = if (baseSalary8h > 0) baseSalary8h / 8.0 else 0.0
        val dailyBreakdown = mutableListOf<DayDetail>()
        
        var totalHoursWorked = 0.0
        var totalSalaryEarned = 0.0
        var totalCoveragePay = 0.0
        var hoursForBonusCalculation = 0.0

        val now = Clock.System.now().toEpochMilliseconds()
        val myAbsences = replacements.filter { it.absentEmployeeId == employee.id }
        val myCoverages = replacements.filter { it.replacementEmployeeId == employee.id }

        (1..7).forEach { day ->
            val schedule = schedules.find { it.dayOfWeek == day } ?: Schedule(employeeId = employee.id, dayOfWeek = day, isRestDay = true, checkInTime = null, checkOutTime = null)
            val dayName = when(day) { 1->"Lunes"; 2->"Martes"; 3->"Miércoles"; 4->"Jueves"; 5->"Viernes"; 6->"Sábado"; 7->"Domingo"; else->"" }
            
            val currentDayMillis = weekStartTimestamp + (day - 1) * 24 * 60 * 60 * 1000L
            val isAlreadyPaid = currentDayMillis <= employee.lastPaidTimestamp
            
            // Buscar asistencia real en este día
            val dayAttendance = attendance.filter { isSameDay(it.startTime, currentDayMillis) }
            val workedHoursReal = dayAttendance.sumOf { it.hoursWorked }

            val absenceRecord = myAbsences.find { isSameDay(it.date, currentDayMillis) }
            val isAbsence = absenceRecord != null
            val isJustified = absenceRecord?.isExcused ?: false
            val isSwapRest = absenceRecord?.replacementType == "SWAP_REST"

            val coverageRecord = myCoverages.find { isSameDay(it.date, currentDayMillis) }
            
            val baseScheduledHours = if (schedule.isRestDay) 0.0 else calculateHours(schedule.checkInTime, schedule.checkOutTime)
            
            var dayWorkedHours = workedHoursReal // Priorizar horas reales de asistencia
            var dayBaseSalary = dayWorkedHours * hourlyRate
            var dayCoverageAmount = 0.0
            var currentBranch = schedule.branchName ?: employee.branch
            var currentIn = schedule.checkInTime
            var currentOut = schedule.checkOutTime
            var effectivelyRest = schedule.isRestDay || isSwapRest

            val shouldCountEarnings = (currentDayMillis <= now) && !isAlreadyPaid

            if (shouldCountEarnings) {
                if (isAbsence && workedHoursReal == 0.0) {
                    dayBaseSalary = 0.0
                    if ((isJustified || isSwapRest) && !schedule.isRestDay) {
                        hoursForBonusCalculation += baseScheduledHours
                    }
                } else if (coverageRecord != null) {
                    // Si cubrió un turno, sumamos el pago extra si aplica
                    val coveredHours = calculateHours(coverageRecord.shiftDetails?.split("|")?.getOrNull(0), coverageRecord.shiftDetails?.split("|")?.getOrNull(1))
                    if (coverageRecord.replacementType == "EXTRA") {
                        val absentEmp = allEmployees.find { it.id == coverageRecord.absentEmployeeId }
                        val absentHourlyRate = (absentEmp?.baseSalary ?: baseSalary8h) / 8.0
                        dayCoverageAmount = coveredHours * absentHourlyRate
                        totalCoveragePay += dayCoverageAmount
                    }
                    hoursForBonusCalculation += coveredHours
                } else {
                    if (!schedule.isRestDay) hoursForBonusCalculation += baseScheduledHours
                }
            }

            totalHoursWorked += dayWorkedHours
            totalSalaryEarned += dayBaseSalary

            dailyBreakdown.add(DayDetail(
                dayOfWeek = day,
                dayName = dayName,
                date = currentDayMillis,
                scheduledHours = baseScheduledHours,
                workedHours = dayWorkedHours,
                salaryEarned = dayBaseSalary,
                isAbsence = isAbsence && !isSwapRest && workedHoursReal == 0.0,
                isJustified = isJustified,
                isRestDay = effectivelyRest,
                isAlreadyPaid = isAlreadyPaid,
                coverageType = coverageRecord?.replacementType ?: if (isSwapRest) "SWAP_REST" else null,
                coverageAmount = dayCoverageAmount,
                branchName = currentBranch,
                checkIn = currentIn,
                checkOut = currentOut
            ))
        }

        val totalScheduledHoursWeek = schedules.sumOf { 
            if (it.isRestDay) 0.0 else calculateHours(it.checkInTime, it.checkOutTime)
        }.coerceAtLeast(1.0)
        
        val activeAbsences = myAbsences.filter { it.date > employee.lastPaidTimestamp && it.replacementType != "SWAP_REST" }
        val hasUnexcusedAbsence = activeAbsences.any { !it.isExcused }
        
        val bonusPercentage = (hoursForBonusCalculation / totalScheduledHoursWeek).coerceAtMost(1.0)
        val proportionalBonus = if (hasUnexcusedAbsence) 0.0 else bonusPercentage * employee.bonus

        val pendingLoans = loans.filter { !it.isPaid }
        val totalNet = (totalSalaryEarned + proportionalBonus + totalCoveragePay) - pendingLoans.sumOf { it.amount }

        val report = generateDetailedReport(employee, dailyBreakdown, totalSalaryEarned, proportionalBonus, totalCoveragePay, pendingLoans, totalNet, weekStartTimestamp)

        return PayrollResult(
            employee = employee,
            dailyBreakdown = dailyBreakdown,
            totalHoursWorked = totalHoursWorked,
            salaryEarned = totalSalaryEarned,
            proportionalBonus = proportionalBonus,
            coveragePay = totalCoveragePay,
            absenceDeduction = activeAbsences.size * (baseSalary8h), // Suponiendo falta = 8h
            pendingLoans = pendingLoans,
            totalLoansAmount = pendingLoans.sumOf { it.amount },
            totalNet = totalNet,
            detailedReport = report,
            weekStart = weekStartTimestamp,
            weekEnd = weekStartTimestamp + (7 * 24 * 60 * 60 * 1000L) - 1
        )
    }

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val dt1 = Instant.fromEpochMilliseconds(t1).toLocalDateTime(TimeZone.currentSystemDefault())
        val dt2 = Instant.fromEpochMilliseconds(t2).toLocalDateTime(TimeZone.currentSystemDefault())
        return dt1.year == dt2.year && dt1.dayOfYear == dt2.dayOfYear
    }

    private fun calculateHours(checkIn: String?, checkOut: String?): Double {
        if (checkIn == null || checkOut == null) return 0.0
        return try {
            val inParts = checkIn.split(":")
            val outParts = checkOut.split(":")
            val inH = inParts[0].toDouble()
            val inM = inParts[1].toDouble()
            val outH = outParts[0].toDouble()
            val outM = outParts[1].toDouble()

            var diff = (outH + outM / 60.0) - (inH + inM / 60.0)
            if (diff < 0) diff += 24.0 // Turno nocturno
            diff
        } catch (e: Exception) {
            0.0
        }
    }

    private fun generateDetailedReport(
        employee: Employee,
        dailyBreakdown: List<DayDetail>,
        totalSalary: Double,
        bonus: Double,
        coverage: Double,
        loans: List<Loan>,
        net: Double,
        weekStart: Long
    ): String {
        val weekStartDt = Instant.fromEpochMilliseconds(weekStart).toLocalDateTime(TimeZone.currentSystemDefault())
        val weekEndDt = Instant.fromEpochMilliseconds(weekStart + (6 * 24 * 60 * 60 * 1000L)).toLocalDateTime(TimeZone.currentSystemDefault())
        
        return buildString {
            append("📋 *RESUMEN DE NÓMINA*\n")
            append("👤 *Empleado:* ${employee.fullName.uppercase()}\n")
            append("📅 *Periodo:* ${weekStartDt.date} - ${weekEndDt.date}\n")
            append("--------------------------------\n\n")
            
            dailyBreakdown.forEach { day ->
                val dayLabel = day.dayName.uppercase()
                val totalGanaDia = day.salaryEarned + day.coverageAmount
                
                when {
                    day.isAlreadyPaid -> {
                        append("✅ *$dayLabel:* PAGADO\n")
                    }
                    day.isRestDay && day.coverageAmount <= 0 -> {
                        append("😴 *$dayLabel:* DESCANSO\n")
                    }
                    day.isAbsence && day.coverageAmount <= 0 -> {
                        append("❌ *$dayLabel:* FALTA -> *$0.00*\n")
                    }
                    totalGanaDia > 0 -> {
                        append("⏰ *$dayLabel:* Turno realizado\n")
                        append("📍 Sucursal: ${day.branchName}")
                        append(" -> *$${formatPrice(totalGanaDia)}*")
                        
                        if (day.isAbsence) append("\n(FALTA EN BASE / CUBRIENDO TURNO)")
                        else if (day.coverageAmount > 0) append("\n(PAGO EXTRA POR COBERTURA)")
                        append("\n")
                    }
                    else -> {
                        append("⏳ *$dayLabel:* PENDIENTE\n")
                    }
                }
                append("\n")
            }
            
            append("--------------------------------\n")
            append("*💰 DETALLE DE PAGO:*")
            append("\n• Sueldo Base Acumulado: *$${formatPrice(totalSalary)}*")
            append("\n• Bono Semanal: *$${formatPrice(bonus)}*")
            if (coverage > 0) append("\n• Ganancia Coberturas: *+$${formatPrice(coverage)}*")
            
            if (loans.isNotEmpty()) {
                append("\n\n*📉 DEDUCCIONES:*")
                loans.forEach { loan ->
                    append("\n• Préstamo: *-$${formatPrice(loan.amount)}*")
                }
            }
            
            append("\n\n*💵 TOTAL NETO: $${formatPrice(net.coerceAtLeast(0.0))}*")
        }
    }

    private fun formatPrice(price: Double): String {
        val parts = price.toString().split(".")
        val integer = parts[0]
        val decimal = if (parts.size > 1) parts[1].padEnd(2, '0').take(2) else "00"
        return "$integer.$decimal"
    }
}
