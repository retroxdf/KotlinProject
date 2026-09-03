package com.abtsplazita.posplazita.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.abtsplazita.posplazita.data.local.UserDao
import com.abtsplazita.posplazita.data.local.UserEntity
import com.abtsplazita.posplazita.domain.Role
import com.abtsplazita.posplazita.domain.User

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.MainScope
import com.abtsplazita.posplazita.data.toDomain
import com.abtsplazita.posplazita.data.toEntity
import com.abtsplazita.posplazita.data.local.RolePermissionDao
import com.abtsplazita.posplazita.data.local.RolePermissionEntity
import com.abtsplazita.posplazita.domain.Permission
import com.abtsplazita.posplazita.data.remote.FirebaseManager

class UserRepository(
    private val userDao: UserDao,
    private val rolePermissionDao: RolePermissionDao? = null,
    private val firebaseManager: FirebaseManager? = null,
    private val scope: kotlinx.coroutines.CoroutineScope? = null
) {

    init {
        initializeAdmin()
    }

    fun initializeAdmin() {
        val activeScope = scope ?: MainScope()
        activeScope.launch {
            try {
                // 1. Asegurar Admin u001 (siempre debe existir)
                val adminId = "u001"
                val existingAdmin = userDao.getUserById(adminId)
                
                if (existingAdmin == null) {
                    val admin = User(
                        id = adminId,
                        username = "admin",
                        firstName = "Administrador",
                        lastName = "Principal",
                        nip = "1385",
                        role = Role.SUPER_ADMIN,
                        mustChangeNip = false,
                        isActive = true,
                        lastUpdated = com.abtsplazita.posplazita.currentTimeMillis()
                    )
                    userDao.insertUser(admin.toEntity())
                    firebaseManager?.syncUser(admin)
                    println("USER_REPO: Administrador u001 creado y sincronizado.")
                } else {
                    // Asegurar que el admin local esté en la nube
                    firebaseManager?.syncUser(existingAdmin.toDomain())
                }


                // 2. Asegurar que SUPER_ADMIN tenga todos los permisos (Integridad de Datos)
                // Usamos first() para obtener el estado actual una sola vez
                val currentPerms = rolePermissionDao?.getPermissionsForRole(Role.SUPER_ADMIN.name)?.first() ?: emptyList()
                if (currentPerms.size < Permission.entries.size) {
                    Permission.entries.forEach { perm ->
                        rolePermissionDao?.insertPermission(
                            com.abtsplazita.posplazita.data.local.RolePermissionEntity(
                                role = Role.SUPER_ADMIN.name,
                                permission = perm.name,
                                level = com.abtsplazita.posplazita.domain.PermissionLevel.ENABLED.name
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                println("DB_INIT_ERROR: ${e.message}")
            }
        }
    }

    fun getAllUsers(): Flow<List<User>> {
        return userDao.getAllUsers().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getUserByNip(nip: String): User? {
        return userDao.getUserByNip(nip)?.toDomain()
    }

    suspend fun getUserById(id: String): User? {
        return userDao.getUserById(id)?.toDomain()
    }

    suspend fun deleteUser(user: User) {
        userDao.deleteUser(user.toEntity())
    }

    suspend fun login(username: String, password: String): User? {
        try {
            // Buscar usuario (Compatibilidad con login viejo si es necesario, pero migramos a NIP)
            val allUsers = userDao.getAllUsers().first()
            val userEntity = allUsers.find { 
                it.username.equals(username, ignoreCase = true) && it.nip == password 
            }
            
            return userEntity?.toDomain()
        } catch (e: Exception) {
            println("DB_LOGIN_ERROR: ${e.message}")
            throw e
        }
    }

    suspend fun saveUser(user: User) {
        val updated = user.copy(lastUpdated = com.abtsplazita.posplazita.currentTimeMillis())
        userDao.insertUser(updated.toEntity())
        firebaseManager?.syncUser(updated)
    }

    suspend fun refreshUsers() {
        println("USER_REPO: Actualizando usuarios desde la nube...")
        val cloudUsers = firebaseManager?.fetchUsers() ?: emptyList()
        if (cloudUsers.isNotEmpty()) {
            val entities = cloudUsers.map { user ->
                user.toEntity() // Ahora sincronizamos el NIP también (todo lo mismo)
            }
            userDao.insertUsersBatch(entities)

            
            // Sincronizar eliminaciones
            val cloudIds = cloudUsers.map { it.id }.toSet()
            val localUsers = userDao.getAllUsers().first()
            localUsers.forEach { local ->
                if (local.id !in cloudIds && local.id != "u001") {
                    userDao.deleteUser(local)
                }
            }
            println("USER_REPO: Usuarios actualizados (${cloudUsers.size}).")
        }
    }

    fun startIncrementalSync() {
        val activeScope = scope ?: kotlinx.coroutines.GlobalScope
        println("USER_REPO: Iniciando observación incremental...")
        activeScope.launch {
            try {
                val since = userDao.getLastUpdated() ?: 0L
                firebaseManager?.observeUsersIncremental(since) { cloudUsers ->
                    if (cloudUsers.isNotEmpty()) {
                        activeScope.launch {
                            userDao.insertUsersBatch(cloudUsers.map { it.toEntity() })
                            println("USER_REPO: ${cloudUsers.size} usuarios actualizados incrementalmente.")
                        }
                    }
                }
            } catch (e: Exception) {
                println("USER_REPO_SYNC_ERROR: ${e.message}")
            }
        }
    }
}
