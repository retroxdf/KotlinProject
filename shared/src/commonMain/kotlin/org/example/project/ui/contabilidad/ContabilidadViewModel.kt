package com.abtsplazita.posplazita.ui.contabilidad

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abtsplazita.posplazita.domain.*
import com.abtsplazita.posplazita.domain.repository.EmployeeRepository
import com.abtsplazita.posplazita.domain.repository.SettingsRepository
import com.abtsplazita.posplazita.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*

class ContabilidadViewModel(
    private val repository: EmployeeRepository,
    private val userRepository: com.abtsplazita.posplazita.domain.repository.UserRepository? = null,
    private val branchRepository: com.abtsplazita.posplazita.domain.repository.BranchRepository? = null,
    private val expenseRepository: ExpenseRepository? = null,
    private val settingsRepository: SettingsRepository? = null,
    private val branchId: String = ""
) : ViewModel() {

    private val calculator = PayrollCalculator()

    val allEmployees = repository.allEmployees.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allLoans = repository.allLoans.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allSchedules = repository.allSchedules.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allReplacements = repository.allAbsenceReplacements.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allAttendance = repository.allAttendance.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBranches = branchRepository?.getAllBranches()?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()) ?: MutableStateFlow(emptyList())

    val combinedEmployees = combine(allEmployees, userRepository?.getAllUsers() ?: flowOf(emptyList())) { employees, users ->
        // Convertir usuarios que no son empleados aún
        val userAsEmployees = users.map { user ->
            // Intentar encontrar si ya es empleado
            val existing = employees.find { it.fullName == "${user.firstName} ${user.lastName}" || it.fullName == user.username }
            existing ?: Employee(
                id = -(user.id.hashCode().toLong()), // ID temporal negativo para usuarios
                fullName = "${user.firstName} ${user.lastName}".ifBlank { user.username },
                phoneNumber = user.phone ?: "",
                branch = branchId,
                baseSalary = 0.0
            )
        }
        (employees + userAsEmployees).distinctBy { it.fullName }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _baseSalary8h = MutableStateFlow(315.0)
    val baseSalary8h = _baseSalary8h.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()

    fun setUserInfo(user: User?) {
        _currentUser.value = user
    }

    private val _manualAmountText = MutableStateFlow(TextFieldValue("0.00"))
    val manualAmountText = _manualAmountText.asStateFlow()

    fun updateManualAmount(text: TextFieldValue) {
        _manualAmountText.value = text
    }

    private val _isEmployeePendingPayment = MutableStateFlow<Map<Long, Double>>(emptyMap())
    val isEmployeePendingPayment = _isEmployeePendingPayment.asStateFlow()

    init {
        loadSettings()
        observeEmployeesForPendingStatus()
    }

    private fun observeEmployeesForPendingStatus() {
        combine(allEmployees, allSchedules, allReplacements, allAttendance, _baseSalary8h) { emp, sch, rep, att, base ->
            val pendingMap = mutableMapOf<Long, Double>()
            
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val daysToSubtract = (now.dayOfWeek.ordinal) % 7
            val weekStart = Clock.System.now().minus(daysToSubtract, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
            val weekStartTimestamp = weekStart.toLocalDateTime(TimeZone.currentSystemDefault()).let { ldt ->
                LocalDateTime(ldt.year, ldt.month, ldt.dayOfMonth, 0, 0, 0, 0).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
            }

            emp.forEach { e ->
                if (e.lastPaidTimestamp < weekStartTimestamp) {
                    val mySchedules = sch.filter { it.employeeId == e.id }
                    val myAttendance = att.filter { it.employeeId == e.id }
                    val loans = allLoans.value.filter { it.employeeId == e.id && !it.isPaid }
                    
                    val result = calculator.calculatePayroll(e, mySchedules, rep, emp, loans, myAttendance, base, weekStartTimestamp)
                    if (result.totalNet > 0) {
                        pendingMap[e.id] = result.totalNet
                    }
                }
            }
            pendingMap
        }.flowOn(kotlinx.coroutines.Dispatchers.Default).onEach { _isEmployeePendingPayment.value = it }.launchIn(viewModelScope)
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository?.getSetting("base_salary_8h")?.let {
                _baseSalary8h.value = it.toDoubleOrNull() ?: 315.0
            }
        }
    }

    fun updateBaseSalary8h(amount: Double) {
        viewModelScope.launch {
            _baseSalary8h.value = amount
            settingsRepository?.saveSetting("base_salary_8h", amount.toString())
        }
    }
    
    private val _selectedEmployee = MutableStateFlow<Employee?>(null)
    val selectedEmployee = _selectedEmployee.asStateFlow()

    private val _payrollResult = MutableStateFlow<PayrollCalculator.PayrollResult?>(null)
    val payrollResult = _payrollResult.asStateFlow()

    fun selectEmployee(employee: Employee?) {
        _selectedEmployee.value = employee
        if (employee != null) {
            calculatePayroll(employee)
        } else {
            _payrollResult.value = null
        }
    }

    private fun calculatePayroll(employee: Employee) {
        viewModelScope.launch {
            val schedules = allSchedules.value.filter { it.employeeId == employee.id }
            val replacements = allReplacements.value 
            val loans = allLoans.value.filter { it.employeeId == employee.id && !it.isPaid }
            
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val daysToSubtract = (now.dayOfWeek.ordinal) % 7
            val weekStart = Clock.System.now().minus(daysToSubtract, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
            val weekStartTimestamp = weekStart.toLocalDateTime(TimeZone.currentSystemDefault()).let { ldt ->
                LocalDateTime(ldt.year, ldt.month, ldt.dayOfMonth, 0, 0, 0, 0).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
            }

            val myAttendance = allAttendance.value.filter { it.employeeId == employee.id }

            val payroll = calculator.calculatePayroll(
                employee = employee,
                schedules = schedules,
                replacements = replacements,
                allEmployees = allEmployees.value,
                loans = loans,
                attendance = myAttendance,
                baseSalary8h = _baseSalary8h.value,
                weekStartTimestamp = weekStartTimestamp
            )
            
            _payrollResult.value = payroll
            _manualAmountText.value = TextFieldValue(payroll.totalNet.formatPrice())
        }
    }

    fun saveEmployee(employee: Employee) {
        viewModelScope.launch {
            if (employee.id == 0L) {
                repository.insertEmployee(employee)
            } else {
                repository.updateEmployee(employee)
            }
        }
    }

    fun deleteEmployee(employee: Employee) {
        viewModelScope.launch {
            repository.deleteEmployee(employee)
        }
    }

    fun deleteSchedule(schedule: com.abtsplazita.posplazita.domain.Schedule) {
        viewModelScope.launch {
            repository.deleteSchedule(schedule)
        }
    }

    fun saveSchedule(schedule: com.abtsplazita.posplazita.domain.Schedule) {
        viewModelScope.launch {
            repository.insertSchedule(schedule)
        }
    }

    fun saveScheduleToAllDays(baseSchedule: com.abtsplazita.posplazita.domain.Schedule) {
        viewModelScope.launch {
            // Primero borrar horarios previos de la semana para este empleado
            repository.deleteSchedulesForEmployee(baseSchedule.employeeId)
            // Guardar para cada día de la semana (1-7)
            (1..7).forEach { day ->
                repository.insertSchedule(baseSchedule.copy(id = 0, dayOfWeek = day))
            }
        }
    }

    fun saveLoan(loan: Loan) {
        viewModelScope.launch {
            repository.insertLoan(loan)
        }
    }

    fun saveAbsence(employee: Employee, date: Long, isJustified: Boolean) {
        viewModelScope.launch {
            val record = AbsenceReplacement(
                absentEmployeeId = employee.id,
                replacementEmployeeId = null,
                date = date,
                isExcused = isJustified,
                replacementType = "ABSENCE",
                shiftDetails = "Falta manual registrada"
            )
            repository.insertAbsenceReplacement(record)
            calculatePayroll(employee)
        }
    }

    fun payPayroll(result: PayrollCalculator.PayrollResult) {
        viewModelScope.launch {
            val now = Clock.System.now().toEpochMilliseconds()
            val actualAmount = _manualAmountText.value.text.replace("$", "").trim().toDoubleOrNull() ?: result.totalNet
            val currentUserId = _currentUser.value?.username ?: "admin"
            
            repository.markLoansAsPaid(result.employee.id)
            
            repository.insertPaymentRecord(PaymentRecord(
                employeeId = result.employee.id,
                employeeName = result.employee.fullName,
                date = now,
                amount = actualAmount,
                reportText = result.detailedReport
            ))
            
            repository.updateEmployee(result.employee.copy(lastPaidTimestamp = result.weekEnd))
            
            repository.insertTransaction(AccountingTransaction(
                type = TransactionType.EXPENSE,
                amount = actualAmount,
                concept = "Pago de Nómina: ${result.employee.fullName}",
                timestamp = now,
                initialBalance = repository.getLastBalance(),
                finalBalance = repository.getLastBalance() - actualAmount
            ))

            expenseRepository?.saveExpense(Expense(
                id = "EXP_PAY_${now}",
                category = ExpenseCategory.NOMINA,
                amount = actualAmount,
                timestamp = now,
                branchId = branchId,
                reason = "Pago de Nómina: ${result.employee.fullName}",
                userId = currentUserId
            ))

            selectEmployee(result.employee)
        }
    }
}
