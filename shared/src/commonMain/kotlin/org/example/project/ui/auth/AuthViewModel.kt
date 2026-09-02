package com.abtsplazita.posplazita.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.abtsplazita.posplazita.domain.*
import kotlinx.datetime.*
import kotlinx.coroutines.flow.combine
import com.abtsplazita.posplazita.domain.repository.UserRepository
import com.abtsplazita.posplazita.domain.repository.EmployeeRepository
import com.abtsplazita.posplazita.domain.repository.SettingsRepository
import com.abtsplazita.posplazita.domain.repository.PermissionRepository
import com.abtsplazita.posplazita.domain.Role
import com.abtsplazita.posplazita.domain.Permission
import com.abtsplazita.posplazita.domain.PermissionLevel

class AuthViewModel(
    private val userRepository: UserRepository,
    private val employeeRepository: EmployeeRepository? = null,
    private val permissionRepository: PermissionRepository? = null,
    private val settingsRepository: SettingsRepository? = null
) : ViewModel() {

    private val _username = MutableStateFlow("")
    val username = _username.asStateFlow()

    private val _password = MutableStateFlow("")
    val password = _password.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn = _isLoggedIn.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _userPermissions = MutableStateFlow<Map<Permission, PermissionLevel>>(emptyMap())
    val userPermissions = _userPermissions.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _showUserPanel = MutableStateFlow(false)
    val showUserPanel = _showUserPanel.asStateFlow()

    fun openUserPanel() { 
        _showUserPanel.value = true 
        refreshStats()
    }
    fun closeUserPanel() { _showUserPanel.value = false }

    private var syncsToday = 0
    private var lastSyncDate = ""

    private fun refreshStats() {
        val user = _currentUser.value ?: return
        val tz = TimeZone.currentSystemDefault()
        val now = Clock.System.now().toLocalDateTime(tz)
        val dateStr = "${now.year}-${now.monthNumber}-${now.dayOfMonth}"
        
        if (dateStr != lastSyncDate) {
            syncsToday = 0
            lastSyncDate = dateStr
        }
        
        if (syncsToday < 3) {
            viewModelScope.launch {
                try {
                    employeeRepository?.refreshAttendance(user.username)
                    user.employeeId?.let { employeeRepository?.refreshSchedules(it) }
                    syncsToday++
                    println("AUTH_VM: Sincronización de cuenta ($syncsToday/3 hoy)")
                } catch (e: Exception) {}
            }
        }
    }

    val userStats: StateFlow<UserPanelStats?> = combine(
        _currentUser,
        employeeRepository?.allEmployees ?: flowOf(emptyList()),
        employeeRepository?.allSchedules ?: flowOf(emptyList()),
        _currentUser.flatMapLatest { user ->
            if (user != null && employeeRepository != null) {
                val now = Clock.System.now()
                val tz = TimeZone.currentSystemDefault()
                val today = now.toLocalDateTime(tz)
                val daysSinceMonday = today.dayOfWeek.ordinal 
                val mondayDate = today.date.minus(daysSinceMonday, DateTimeUnit.DAY)
                val mondayStart = mondayDate.atTime(0, 0).toInstant(tz).toEpochMilliseconds()
                val sundayEnd = mondayDate.plus(6, DateTimeUnit.DAY).atTime(23, 59, 59).toInstant(tz).toEpochMilliseconds()
                employeeRepository.getAttendanceInRange(user.username, mondayStart, sundayEnd)
            } else flowOf(emptyList())
        }
    ) { user, employees, schedules, attendance ->
        if (user == null) return@combine null
        
        val employee = employees.find { 
            it.id == user.employeeId || 
            it.fullName.equals(user.username, ignoreCase = true) ||
            it.fullName.contains(user.firstName, ignoreCase = true) ||
            (user.username == "admin" && it.fullName.contains("ADMIN", ignoreCase = true))
        }
        val empId = employee?.id ?: user.employeeId
        val mySchedules = if (empId != null) schedules.filter { it.employeeId == empId } else emptyList()
        
        val tz = TimeZone.currentSystemDefault()
        val now = Clock.System.now()
        val today = now.toLocalDateTime(tz)
        
        // Determinar el inicio de la semana (Lunes)
        val daysSinceMonday = today.dayOfWeek.ordinal
        val mondayDate = today.date.minus(daysSinceMonday, DateTimeUnit.DAY)
        
        val restDayStr = mySchedules.find { it.isRestDay }?.let { 
            when(it.dayOfWeek) {
                1 -> "Lunes"; 2 -> "Martes"; 3 -> "Miércoles"; 4 -> "Jueves"; 5 -> "Viernes"; 6 -> "Sábado"; 7 -> "Domingo"; else -> "N/A"
            }
        } ?: "No definido"

        val attendanceByDate = attendance.associateBy { Instant.fromEpochMilliseconds(it.startTime).toLocalDateTime(tz).date }
        val daysWorked = attendanceByDate.size
        
        val baseSalary8h = employee?.baseSalary ?: 315.0
        val hourlyRate = baseSalary8h / 8.0

        // Construir detalles de la semana (Lunes a Domingo)
        val dayNames = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
        val weekDetails = mutableListOf<DayStatusInfo>()
        var hasAbsence = false
        var accumulatedBaseEarnings = 0.0

        for (i in 0..6) {
            val dateToCheck = mondayDate.plus(i, DateTimeUnit.DAY)
            val dow = i + 1
            val sch = mySchedules.find { it.dayOfWeek == dow }
            val record = attendanceByDate[dateToCheck]
            
            var dayAmount = 0.0
            val status: String
            
            if (record != null) {
                status = "TRABAJADO"
                // Calcular ganancia del día según horario programado
                var hours = 8.0
                if (sch?.checkInTime != null && sch.checkOutTime != null) {
                    try {
                        val inP = sch.checkInTime!!.split(":")
                        val outP = sch.checkOutTime!!.split(":")
                        hours = ((outP[0].toInt() * 60 + outP[1].toInt()) - (inP[0].toInt() * 60 + inP[1].toInt())) / 60.0
                    } catch (e: Exception) {}
                }
                dayAmount = hours * hourlyRate
                accumulatedBaseEarnings += dayAmount
            } else if (sch?.isRestDay == true) {
                status = "DESCANSO"
            } else if (dateToCheck < today.date) {
                status = "FALTA"
                hasAbsence = true
            } else {
                status = "PENDIENTE"
            }
            
            weekDetails.add(DayStatusInfo(dayNames[i], status, dayAmount))
        }

        // Bono de 200 dividido entre 6 días. Penalty si hay falta.
        val dailyBonus = 200.0 / 6.0
        val earnedBonus = if (hasAbsence) 0.0 else daysWorked * dailyBonus
        
        // Info de hoy para el panel izquierdo
        val todayRecord = attendanceByDate[today.date]

        UserPanelStats(
            daysWorked = daysWorked,
            restDay = restDayStr,
            checkInTime = todayRecord?.startTime,
            checkOutTime = todayRecord?.endTime,
            dailyBasePay = weekDetails.find { it.name == dayNames[today.dayOfWeek.ordinal] }?.amount ?: 0.0,
            earnings = accumulatedBaseEarnings + earnedBonus,
            bonus = earnedBonus,
            weekDetails = weekDetails
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allUsers = userRepository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<User>())

    private val _showNipPrompt = MutableStateFlow<User?>(null)
    val showNipPrompt = _showNipPrompt.asStateFlow()

    private val _mustChangeNip = MutableStateFlow<User?>(null)
    val mustChangeNip = _mustChangeNip.asStateFlow()

    init {
        loadLastUser()
    }

    private fun loadLastUser() {
        viewModelScope.launch {
            val lastUser = settingsRepository?.getSetting("last_logged_username")
            if (lastUser != null) {
                _username.value = lastUser
            }
        }
    }

    fun onUsernameChange(value: String) { _username.value = value }
    fun onPasswordChange(value: String) { _password.value = value }

    fun selectUserForLogin(user: User) {
        _showNipPrompt.value = user
        _error.value = null
    }

    fun closeNipPrompt() {
        _showNipPrompt.value = null
    }

    fun loginWithNip(nip: String) {
        val userToLogin = _showNipPrompt.value ?: return
        viewModelScope.launch {
            try {
                val user = userRepository.getUserByNip(nip)
                if (user != null && user.id == userToLogin.id) {
                    if (user.mustChangeNip || user.nip == "1111") {
                        _mustChangeNip.value = user
                        _showNipPrompt.value = null
                    } else {
                        completeLogin(user)
                    }
                } else {
                    _error.value = "NIP Incorrecto"
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            }
        }
    }

    fun changeNipAndLogin(newNip: String) {
        val user = _mustChangeNip.value ?: return
        viewModelScope.launch {
            val updated = user.copy(nip = newNip, mustChangeNip = false)
            userRepository.saveUser(updated)
            completeLogin(updated)
            _mustChangeNip.value = null
        }
    }

    private fun completeLogin(user: User) {
        // Registrar inicio de turno solo si no hay uno ya abierto
        viewModelScope.launch {
            // Cargar permisos
            if (user.role == Role.SUPER_ADMIN) {
                _userPermissions.value = Permission.entries.associateWith { PermissionLevel.ENABLED }
            } else {
                permissionRepository?.getPermissionsForRole(user.role)?.first()?.let {
                    _userPermissions.value = it
                }
            }

            val openShift = employeeRepository?.getOpenShift(user.username)
            if (openShift == null) {
                val now = com.abtsplazita.posplazita.currentTimeMillis()
                employeeRepository?.insertAttendance(
                    com.abtsplazita.posplazita.domain.AttendanceRecord(
                        userId = user.username,
                        employeeId = user.employeeId,
                        startTime = now,
                        isClosed = false
                    )
                )
            }
            
            _currentUser.value = user
            _isLoggedIn.value = true
            _error.value = null
            _showNipPrompt.value = null
        }
    }

    fun login() {
        viewModelScope.launch {
            try {
                val user = userRepository.login(_username.value, _password.value)
                if (user != null) {
                    // Recordar el último usuario exitoso
                    settingsRepository?.saveSetting("last_logged_username", _username.value)
                    
                    _currentUser.value = user
                    _isLoggedIn.value = true
                    _error.value = null
                } else {
                    _error.value = "Usuario o contraseña incorrectos"
                }
            } catch (e: Exception) {
                _error.value = "Fallo técnico: ${e.message}"
            }
        }
    }

    fun logout() {
        _isLoggedIn.value = false
        _currentUser.value = null
        _userPermissions.value = emptyMap()
        _username.value = ""
        _password.value = ""
        _showNipPrompt.value = null
        _error.value = null
    }
}
