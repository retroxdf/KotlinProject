package com.abtsplazita.posplazita.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.abtsplazita.posplazita.domain.User
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
