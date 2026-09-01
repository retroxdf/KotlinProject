package com.abtsplazita.posplazita.ui.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.abtsplazita.posplazita.domain.User
import com.abtsplazita.posplazita.domain.Role
import com.abtsplazita.posplazita.domain.Permission
import com.abtsplazita.posplazita.domain.PermissionLevel
import com.abtsplazita.posplazita.domain.Employee
import com.abtsplazita.posplazita.domain.repository.UserRepository
import com.abtsplazita.posplazita.domain.repository.EmployeeRepository
import com.abtsplazita.posplazita.domain.repository.PermissionRepository
import com.abtsplazita.posplazita.currentTimeMillis

class UserViewModel(
    private val userRepository: UserRepository,
    private val employeeRepository: EmployeeRepository,
    private val permissionRepository: PermissionRepository? = null,
    private val branchId: String = ""
) : ViewModel() {

    val users = userRepository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val employees = employeeRepository.allEmployees
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val filteredUsers = combine(users, _searchQuery) { list, query ->
        if (query.isBlank()) list
        else list.filter { it.username.contains(query, ignoreCase = true) || it.firstName.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedUser = MutableStateFlow<User?>(null)
    val selectedUser = _selectedUser.asStateFlow()

    private val _isEditing = MutableStateFlow(false)
    val isEditing = _isEditing.asStateFlow()

    fun selectUser(user: User?) {
        _selectedUser.value = user
        _isEditing.value = false
    }

    fun startEditing() {
        _isEditing.value = true
    }

    fun cancelEditing() {
        if (_selectedUser.value?.id?.startsWith("NEW") == true) {
            _selectedUser.value = null
        }
        _isEditing.value = false
    }

    fun prepareNewUser() {
        val newUser = User(
            id = "NEW_${currentTimeMillis()}",
            username = "",
            firstName = "",
            lastName = "",
            nip = "1111", // NIP default
            role = Role.CAJERO,
            mustChangeNip = true,
            isActive = true
        )
        _selectedUser.value = newUser
        _isEditing.value = true
    }

    fun saveUser(user: User) {
        viewModelScope.launch {
            var toSave = if (user.id.startsWith("NEW")) {
                user.copy(id = "U${currentTimeMillis()}")
            } else {
                user
            }

            // Crear o actualizar empleado automáticamente
            if (toSave.employeeId == null) {
                // Si no tiene empleado, creamos uno
                val employee = Employee(
                    fullName = "${toSave.firstName} ${toSave.lastName}",
                    phoneNumber = toSave.phone ?: "",
                    branch = branchId,
                    baseSalary = 315.0 // Sueldo base por defecto pedido
                )
                val newId = employeeRepository.insertEmployee(employee)
                toSave = toSave.copy(employeeId = newId)
            } else {
                // Si ya tiene empleado, lo actualizamos para que coincidan nombres/tel
                employeeRepository.getEmployeeById(toSave.employeeId!!)?.let { emp ->
                    val updatedEmp = emp.copy(
                        fullName = "${toSave.firstName} ${toSave.lastName}",
                        phoneNumber = toSave.phone ?: ""
                    )
                    employeeRepository.updateEmployee(updatedEmp)
                }
            }

            userRepository.saveUser(toSave)
            _selectedUser.value = toSave
            _isEditing.value = false
        }
    }

    fun deleteUser(user: User) {
        viewModelScope.launch {
            userRepository.deleteUser(user)
            _selectedUser.value = null
            _isEditing.value = false
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // --- Gestión de Permisos por Rol ---
    private val _selectedRoleForPerms = MutableStateFlow(Role.CAJERO)
    val selectedRoleForPerms = _selectedRoleForPerms.asStateFlow()

    val currentRolePermissions = _selectedRoleForPerms.flatMapLatest { role ->
        permissionRepository?.getPermissionsForRole(role) ?: flowOf(emptyMap())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun selectRoleForPermissions(role: Role) {
        _selectedRoleForPerms.value = role
    }

    fun updatePermissionLevel(permission: Permission, level: PermissionLevel) {
        val role = _selectedRoleForPerms.value
        if (role == Role.SUPER_ADMIN) return

        viewModelScope.launch {
            permissionRepository?.updatePermission(role, permission, level)
        }
    }
}
