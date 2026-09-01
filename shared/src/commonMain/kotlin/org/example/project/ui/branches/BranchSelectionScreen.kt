package com.abtsplazita.posplazita.ui.branches

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.abtsplazita.posplazita.domain.Branch

@Composable
fun BranchSelectionScreen(
    viewModel: BranchViewModel,
    onBranchSelected: (Branch) -> Unit,
    onLogout: () -> Unit
) {
    val branches by viewModel.branches.collectAsState()
    val showDialog by viewModel.showAddDialog.collectAsState()
    var branchToDelete by remember { mutableStateOf<Branch?>(null) }

    // Refrescar al entrar a la pantalla para ver cambios de otros dispositivos
    LaunchedEffect(Unit) {
        viewModel.refreshBranches()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Seleccionar Sucursal", fontWeight = FontWeight.Bold) },
                actions = {
                    Button(onClick = { viewModel.openAddDialog() }) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Nueva Sucursal")
                    }
                    TextButton(onClick = onLogout) {
                        Text("Cerrar Sesión", color = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        if (branches.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Store, null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                    Text("No hay sucursales registradas", color = Color.Gray)
                    Button(onClick = { viewModel.openAddDialog() }, modifier = Modifier.padding(16.dp)) {
                        Text("Registrar Primera Sucursal")
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 300.dp),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(branches) { branch ->
                    BranchCard(
                        branch = branch, 
                        onClick = { onBranchSelected(branch) },
                        onDelete = { branchToDelete = branch }
                    )
                }
            }
        }
    }

    if (showDialog) {
        AddBranchDialog(
            onDismiss = { viewModel.closeAddDialog() },
            onConfirm = { name, address -> viewModel.addBranch(name, address) }
        )
    }

    if (branchToDelete != null) {
        AlertDialog(
            onDismissRequest = { branchToDelete = null },
            title = { Text("Eliminar Sucursal") },
            text = { Text("¿Estás seguro de que deseas eliminar '${branchToDelete?.name}'? Esta acción eliminará la sucursal de todos los dispositivos.") },
            confirmButton = {
                Button(
                    onClick = {
                        branchToDelete?.let { viewModel.deleteBranch(it) }
                        branchToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("ELIMINAR")
                }
            },
            dismissButton = {
                TextButton(onClick = { branchToDelete = null }) {
                    Text("CANCELAR")
                }
            }
        )
    }
}

@Composable
fun BranchCard(branch: Branch, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Store, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(branch.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                    Text(branch.address, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun AddBranchDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Sucursal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la sucursal") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Dirección / Ubicación") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name, address) }) {
                Text("REGISTRAR")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCELAR") }
        }
    )
}
