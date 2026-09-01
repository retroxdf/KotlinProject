package com.abtsplazita.posplazita.ui.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.abtsplazita.posplazita.domain.Expense
import com.abtsplazita.posplazita.domain.ExpenseCategory
import com.abtsplazita.posplazita.domain.formatPrice
import kotlinx.datetime.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseModule(viewModel: ExpenseViewModel, onBack: () -> Unit) {
    val expenses by viewModel.expenses.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestión de Gastos") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, "Nuevo Gasto", tint = Color.White)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            if (expenses.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay gastos registrados.", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(expenses) { expense ->
                        ExpenseItem(expense, onDelete = { viewModel.deleteExpense(expense) })
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddExpenseDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { cat, amount, reason ->
                viewModel.saveExpense(cat, amount, reason)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ExpenseItem(expense: Expense, onDelete: () -> Unit) {
    val dt = Instant.fromEpochMilliseconds(expense.timestamp).toLocalDateTime(TimeZone.currentSystemDefault())
    
    ListItem(
        headlineContent = { Text(expense.category.name, fontWeight = FontWeight.Bold) },
        supportingContent = { 
            Column {
                Text(expense.reason ?: "Sin descripción", style = MaterialTheme.typography.bodySmall)
                Text("${dt.date} ${dt.time.toString().take(5)}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$${expense.amount.formatPrice()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.Red)
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.6f)) }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
fun AddExpenseDialog(onDismiss: () -> Unit, onConfirm: (ExpenseCategory, Double, String?) -> Unit) {
    var selectedCat by remember { mutableStateOf(ExpenseCategory.OTRO) }
    var amountText by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var catExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Nuevo Gasto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box {
                    OutlinedButton(onClick = { catExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Categoría: ${selectedCat.name}")
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                        ExpenseCategory.values().forEach { cat ->
                            DropdownMenuItem(text = { Text(cat.name) }, onClick = { selectedCat = cat; catExpanded = false })
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() || c == '.' }) amountText = it },
                    label = { Text("Monto ($)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Descripción / Motivo") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0) onConfirm(selectedCat, amount, reason)
                },
                enabled = amountText.isNotBlank()
            ) { Text("GUARDAR") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } }
    )
}
