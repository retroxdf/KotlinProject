package com.abtsplazita.posplazita.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.abtsplazita.posplazita.data.local.RolePermissionDao
import com.abtsplazita.posplazita.data.local.RolePermissionEntity
import com.abtsplazita.posplazita.domain.Permission
import com.abtsplazita.posplazita.domain.PermissionLevel
import com.abtsplazita.posplazita.domain.Role

class PermissionRepository(private val dao: RolePermissionDao) {

    fun getPermissionsForRole(role: Role): Flow<Map<Permission, PermissionLevel>> {
        return dao.getPermissionsForRole(role.name).map { entities ->
            val map = mutableMapOf<Permission, PermissionLevel>()
            entities.forEach { entity ->
                try {
                    val perm = Permission.valueOf(entity.permission)
                    val level = PermissionLevel.valueOf(entity.level)
                    map[perm] = level
                } catch (e: Exception) {}
            }
            map
        }
    }

    suspend fun updatePermission(role: Role, permission: Permission, level: PermissionLevel) {
        dao.insertPermission(RolePermissionEntity(role.name, permission.name, level.name))
    }

    suspend fun clearPermissions(role: Role) {
        dao.clearRolePermissions(role.name)
    }
}
