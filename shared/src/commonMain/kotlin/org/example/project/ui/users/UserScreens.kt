package com.abtsplazita.posplazita.ui.users

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import com.abtsplazita.posplazita.domain.User
import com.abtsplazita.posplazita.domain.Role
import com.abtsplazita.posplazita.domain.Permission
import com.abtsplazita.posplazita.domain.Employee

@Composable
fun UserModule(viewModel: UserViewModel) {
    val selectedUser by viewModel.selectedUser.collectAsState()
    val isEditing by viewModel.isEditing.collectAsState()
    val users by viewModel.filteredUsers.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val employees by viewModel.employees.collectAsState()
    
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier.fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && (event.key == Key.Plus || event.key == Key.NumPadAdd)) {
                    if (!isEditing) {
                        viewModel.prepareNewUser()
                        true
                    } else false
                } else false
            }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isCompact = maxWidth < 800.dp

            if (isCompact) {
                // --- VISTA MÓVIL ---
                if (selectedUser == null) {
                    // LISTA
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Text("Usuarios", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            label = { Text("Buscar...") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            singleLine = true
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.prepareNewUser() }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.PersonAdd, null); Spacer(Modifier.width(8.dp)); Text("AGREGAR USUARIO (+)")
                        }
                        Spacer(Modifier.height(16.dp))
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(users) { user ->
                                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { viewModel.selectUser(user) }) {
                                    ListItem(
                                        headlineContent = { Text("${user.firstName} ${user.lastName}", fontWeight = FontWeight.Bold) },
                                        supportingContent = { Text("@${user.username} | ${user.role.name}") },
                                        trailingContent = { Icon(Icons.Default.ArrowForwardIos, null, modifier = Modifier.size(16.dp)) }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // DETALLE O EDICIÓN
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        if (isEditing) {
                            UserEditForm(
                                user = selectedUser!!,
                                onSave = { viewModel.saveUser(it) },
                                onCancel = { viewModel.cancelEditing() }
                            )
                        } else {
                            Column {
                                IconButton(onClick = { viewModel.selectUser(null) }) {
                                    Icon(Icons.Default.ArrowBackIosNew, "Volver")
                                }
                                UserDetailView(
                                    user = selectedUser!!,
                                    employees = employees,
                                    onEdit = { viewModel.startEditing() },
                                    onDelete = { viewModel.deleteUser(it) }
                                )
                            }
                        }
                    }
                }
            } else {
                // --- VISTA ESCRITORIO (Split Pane) ---
                Row(modifier = Modifier.fillMaxSize()) {
                    // Columna Izquierda
                    Column(modifier = Modifier.weight(0.4f).fillMaxHeight().padding(16.dp)) {
                        Text("Gestión de Usuarios", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            label = { Text("Buscar usuario...") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            singleLine = true
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.prepareNewUser() }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.PersonAdd, null); Spacer(Modifier.width(8.dp)); Text("AGREGAR USUARIO (+)")
                        }
                        Spacer(Modifier.height(16.dp))
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(users) { user ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { viewModel.selectUser(user) },
                                    border = if (selectedUser?.id == user.id) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                    colors = CardDefaults.cardColors(containerColor = if (selectedUser?.id == user.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                                ) {
                                    ListItem(
                                        headlineContent = { Text("${user.firstName} ${user.lastName}", fontWeight = FontWeight.Bold) },
                                        supportingContent = { Text("@${user.username} | ${user.role.name}") },
                                        trailingContent = { if (user.employeeId != null) Icon(Icons.Default.Link, null, tint = Color(0xFF4CAF50)) }
                                    )
                                }
                            }
                        }
                    }

                    VerticalDivider()

                    // Columna Derecha
                    Box(modifier = Modifier.weight(0.6f).fillMaxHeight().padding(24.dp)) {
                        if (selectedUser != null) {
                            if (isEditing) {
                                UserEditForm(user = selectedUser!!, onSave = { viewModel.saveUser(it) }, onCancel = { viewModel.cancelEditing() })
                            } else {
                                UserDetailView(user = selectedUser!!, employees = employees, onEdit = { viewModel.startEditing() }, onDelete = { viewModel.deleteUser(it) })
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Badge, null, modifier = Modifier.size(100.dp), tint = Color.LightGray)
                                Spacer(Modifier.height(16.dp))
                                Text("Selecciona un usuario para ver detalles", color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
fun UserDetailView(
    user: User, 
    employees: List<Employee>, 
    onEdit: () -> Unit, 
    onDelete: (User) -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val linkedEmployee = employees.find { it.id == user.employeeId }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${user.firstName} ${user.lastName}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Editar", tint = MaterialTheme.colorScheme.primary) }
            IconButton(onClick = { showDeleteConfirm = true }) { Icon(Icons.Default.Delete, "Borrar", tint = Color.Red) }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        DetailRow("Usuario (Alias):", "@${user.username}")
        DetailRow("Rol de Sistema:", user.role.name)
        DetailRow("Teléfono:", user.phone ?: "No asignado")
        
        Spacer(Modifier.height(24.dp))
        
        Text("Vinculación Laboral", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            colors = CardDefaults.cardColors(containerColor = if (linkedEmployee != null) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (linkedEmployee != null) Icons.Default.Person else Icons.Default.LinkOff, null)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(linkedEmployee?.fullName ?: "USUARIO SIN EMPLEADO ASIGNADO", fontWeight = FontWeight.Bold)
                    if (linkedEmployee != null) {
                        Text("Sucursal: ${linkedEmployee.branch}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Seguridad", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, null, tint = if (user.mustChangeNip) Color(0xFFFF9800) else Color(0xFF4CAF50))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Estado de NIP")
                    Text(if (user.mustChangeNip) "Debe cambiar NIP al ingresar" else "NIP Actualizado", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Eliminar Usuario") },
            text = { Text("¿Estás seguro de que deseas eliminar a ${user.username}? No podrá ingresar al sistema.") },
            confirmButton = { Button(onClick = { onDelete(user); showDeleteConfirm = false }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("ELIMINAR") } },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("CANCELAR") } }
        )
    }
}

@Composable
fun UserEditForm(user: User, onSave: (User) -> Unit, onCancel: () -> Unit) {
    var username by remember { mutableStateOf(user.username) }
    var firstName by remember { mutableStateOf(user.firstName) }
    var lastName by remember { mutableStateOf(user.lastName) }
    var phone by remember { mutableStateOf(user.phone ?: "") }
    var selectedRole by remember { mutableStateOf(user.role) }
    var employeeId by remember { mutableStateOf(user.employeeId) }
    var nip by remember { mutableStateOf(user.nip) }

    val firstNameFR = remember { FocusRequester() }
    val lastNameFR = remember { FocusRequester() }
    val usernameFR = remember { FocusRequester() }
    val phoneFR = remember { FocusRequester() }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(if (user.id.startsWith("NEW")) "Nuevo Usuario" else "Editar Perfil", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = { Text("Nombre(s)") },
            modifier = Modifier.fillMaxWidth().focusRequester(firstNameFR).onPreviewKeyEvent {
                if (it.key == Key.Enter || it.key == Key.NumPadEnter) {
                    if (it.type == KeyEventType.KeyDown) lastNameFR.requestFocus()
                    true
                } else false
            },
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = { Text("Apellidos") },
            modifier = Modifier.fillMaxWidth().focusRequester(lastNameFR).onPreviewKeyEvent {
                if (it.key == Key.Enter || it.key == Key.NumPadEnter) {
                    if (it.type == KeyEventType.KeyDown) usernameFR.requestFocus()
                    true
                } else false
            },
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Nombre de Usuario (Alias)") },
            modifier = Modifier.fillMaxWidth().focusRequester(usernameFR).onPreviewKeyEvent {
                if (it.key == Key.Enter || it.key == Key.NumPadEnter) {
                    if (it.type == KeyEventType.KeyDown) phoneFR.requestFocus()
                    true
                } else false
            },
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Teléfono") },
            modifier = Modifier.fillMaxWidth().focusRequester(phoneFR).onPreviewKeyEvent {
                if (it.key == Key.Enter || it.key == Key.NumPadEnter) {
                    if (it.type == KeyEventType.KeyDown && username.isNotBlank() && firstName.isNotBlank()) {
                        onSave(user.copy(username = username, firstName = firstName, lastName = lastName, phone = phone, role = selectedRole, employeeId = employeeId, nip = nip))
                    }
                    true
                } else false
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )
        
        Spacer(Modifier.height(24.dp))
        Text("Rol de Sistema", fontWeight = FontWeight.Bold)
        
        var showRoleMenu by remember { mutableStateOf(false) }
        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { showRoleMenu = true }, modifier = Modifier.fillMaxWidth()) {
                Text("ROL: ${selectedRole.name}")
            }
            DropdownMenu(expanded = showRoleMenu, onDismissRequest = { showRoleMenu = false }) {
                Role.entries.forEach { role ->
                    DropdownMenuItem(text = { Text(role.name) }, onClick = { selectedRole = role; showRoleMenu = false })
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Seguridad", fontWeight = FontWeight.Bold)
        Text("NIP asignado: $nip", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        if (user.id.startsWith("NEW")) {
            Text("(Por defecto es 1111, el usuario deberá cambiarlo)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }

        Spacer(Modifier.height(32.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onCancel) { Text("CANCELAR") }
            Spacer(Modifier.width(16.dp))
            Button(
                onClick = {
                    onSave(user.copy(
                        username = username,
                        firstName = firstName,
                        lastName = lastName,
                        phone = phone,
                        role = selectedRole,
                        employeeId = employeeId,
                        nip = nip
                    ))
                },
                enabled = username.isNotBlank() && firstName.isNotBlank() && lastName.isNotBlank()
            ) {
                Text("GUARDAR USUARIO")
            }
        }
    }

    LaunchedEffect(Unit) {
        repeat(3) {
            kotlinx.coroutines.delay(200)
            try { firstNameFR.requestFocus() } catch(e: Exception) {}
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, fontWeight = FontWeight.Bold, modifier = Modifier.width(150.dp))
        Text(value)
    }
}
