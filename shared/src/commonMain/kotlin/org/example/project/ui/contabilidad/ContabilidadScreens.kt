package com.abtsplazita.posplazita.ui.contabilidad

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.input.key.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.abtsplazita.posplazita.domain.Employee
import com.abtsplazita.posplazita.domain.formatPrice
import com.abtsplazita.posplazita.domain.PayrollCalculator
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun ContabilidadModule(viewModel: ContabilidadViewModel) {
    var currentTab by remember { mutableStateOf(0) } // 0: Dashboard, 1: Empleados, 2: Nómina, 3: Asistencia
    var showAddEmployee by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && (event.key == Key.Plus || event.key == Key.NumPadAdd)) {
                    if (currentTab == 1) {
                        showAddEmployee = true
                        true
                    } else false
                } else false
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TabRow(selectedTabIndex = currentTab) {
                Tab(selected = currentTab == 0, onClick = { currentTab = 0 }, text = { Text("Dashboard") })
                Tab(selected = currentTab == 1, onClick = { currentTab = 1 }, text = { Text("Empleados") })
                Tab(selected = currentTab == 2, onClick = { currentTab = 2 }, text = { Text("Nómina") })
                Tab(selected = currentTab == 3, onClick = { currentTab = 3 }, text = { Text("Asistencia") })
            }

            when (currentTab) {
                0 -> ContabilidadDashboard(viewModel)
                1 -> EmployeeListScreen(viewModel, showAddDialog = showAddEmployee, onShowAdd = { showAddEmployee = it })
                2 -> PayrollScreen(viewModel)
                3 -> AttendanceScreen(viewModel)
            }
        }
    }
}

@Composable
fun ContabilidadDashboard(viewModel: ContabilidadViewModel) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        val isCompact = maxWidth < 600.dp
        
        Column(modifier = Modifier.fillMaxSize()) {
            Text("Resumen Contable", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            
            if (isCompact) {
                DashboardCard("Saldo en Caja", "$0.00", Color(0xFF4CAF50), Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                DashboardCard("Egresos Hoy", "$0.00", Color(0xFFF44336), Modifier.fillMaxWidth())
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DashboardCard("Saldo en Caja", "$0.00", Color(0xFF4CAF50), Modifier.weight(1f))
                    DashboardCard("Egresos Hoy", "$0.00", Color(0xFFF44336), Modifier.weight(1f))
                }
            }
            
            Spacer(Modifier.height(16.dp))
            Text("Últimas Transacciones", style = MaterialTheme.typography.titleMedium)
            Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay transacciones recientes", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun DashboardCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = color)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = color)
        }
    }
}

@Composable
fun EmployeeListScreen(viewModel: ContabilidadViewModel, showAddDialog: Boolean, onShowAdd: (Boolean) -> Unit) {
    val employees by viewModel.combinedEmployees.collectAsState()
    var selectedEmpForSchedule by remember { mutableStateOf<Employee?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { onShowAdd(true) }) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(employees) { emp ->
                val isUser = emp.id < 0
                ListItem(
                    headlineContent = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(emp.fullName, fontWeight = FontWeight.Bold)
                            if (isUser) {
                                Spacer(Modifier.width(8.dp))
                                Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(4.dp)) {
                                    Text("USUARIO", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    },
                    supportingContent = { Text("${emp.branch} • ${emp.phoneNumber}") },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("$${emp.baseSalary.formatPrice()}", fontWeight = FontWeight.Bold)
                            IconButton(onClick = { selectedEmpForSchedule = emp }) {
                                Icon(Icons.Default.CalendarMonth, "Horarios", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    },
                    modifier = Modifier.padding(8.dp)
                )
                HorizontalDivider()
            }
        }
    }

    if (showAddDialog) {
        EmployeeEditDialog(onDismiss = { onShowAdd(false) }, onSave = { viewModel.saveEmployee(it) })
    }

    if (selectedEmpForSchedule != null) {
        ScheduleConfigDialog(
            employee = selectedEmpForSchedule!!,
            viewModel = viewModel,
            onDismiss = { selectedEmpForSchedule = null }
        )
    }
}

@Composable
fun ScheduleConfigDialog(employee: Employee, viewModel: ContabilidadViewModel, onDismiss: () -> Unit) {
    val schedules by viewModel.allSchedules.collectAsState()
    val branches by viewModel.allBranches.collectAsState()
    val mySchedules = schedules.filter { it.employeeId == employee.id }
    
    var expandedDay by remember { mutableStateOf<Int?>(null) } // null: ninguno, 1-7: día expandido

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints {
            val isSmall = maxWidth < 700.dp
            
            Card(
                modifier = if (isSmall) Modifier.fillMaxSize() else Modifier.width(600.dp).heightIn(max = 800.dp),
                shape = if (isSmall) RoundedCornerShape(0.dp) else RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(if (isSmall) 0.dp else 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Adaptativo
                    Surface(
                        color = Color(0xFF0056A0),
                        contentColor = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, "Cerrar", tint = Color.White)
                            }
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Agenda Semanal", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(employee.fullName, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f).padding(if (isSmall) 12.dp else 24.dp)) {
                        Text("Define los horarios de trabajo y días de descanso.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Spacer(Modifier.height(16.dp))

                        val dayNames = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
                        
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(7) { index ->
                                val dayOfWeek = index + 1
                                val sch = mySchedules.find { it.dayOfWeek == dayOfWeek }
                                val isExpanded = expandedDay == dayOfWeek
                                
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    border = BorderStroke(1.dp, if(isExpanded) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.3f)),
                                    colors = CardDefaults.cardColors(containerColor = if(isExpanded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f) else Color.Transparent)
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clickable { expandedDay = if (isExpanded) null else dayOfWeek }.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(Modifier.weight(1f)) {
                                                Text(dayNames[index], fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                                if (sch == null) {
                                                    Text("Sin configurar", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                                } else if (sch.isRestDay) {
                                                    Text("DÍA DE DESCANSO", style = MaterialTheme.typography.labelSmall, color = Color.Red, fontWeight = FontWeight.Bold)
                                                } else {
                                                    Text("${sch.checkInTime} - ${sch.checkOutTime} | ${sch.branchName ?: "Cualquiera"}", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                            Icon(
                                                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = null,
                                                tint = if(isExpanded) MaterialTheme.colorScheme.primary else Color.Gray
                                            )
                                        }

                                        if (isExpanded) {
                                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                                            DayScheduleEditor(
                                                existingSchedule = sch,
                                                dayOfWeek = dayOfWeek,
                                                branches = branches,
                                                onSave = { newSch -> 
                                                    viewModel.saveSchedule(newSch.copy(employeeId = employee.id))
                                                    expandedDay = null 
                                                },
                                                onSaveToAll = { baseSch ->
                                                    viewModel.saveScheduleToAllDays(baseSch.copy(employeeId = employee.id))
                                                    expandedDay = null
                                                },
                                                onDelete = { schToDelete ->
                                                    viewModel.deleteSchedule(schToDelete)
                                                    expandedDay = null
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Nota: El registro de entrada se marca al entrar a la venta y el de salida al realizar el precorte.", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = Color.Gray, 
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                    
                    if (isSmall) {
                        Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = 2.dp) {
                            Button(
                                onClick = onDismiss,
                                modifier = Modifier.fillMaxWidth().padding(16.dp).height(50.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("FINALIZAR CONFIGURACIÓN")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayScheduleEditor(
    existingSchedule: com.abtsplazita.posplazita.domain.Schedule?,
    dayOfWeek: Int,
    branches: List<com.abtsplazita.posplazita.domain.Branch>,
    onSave: (com.abtsplazita.posplazita.domain.Schedule) -> Unit,
    onSaveToAll: (com.abtsplazita.posplazita.domain.Schedule) -> Unit,
    onDelete: (com.abtsplazita.posplazita.domain.Schedule) -> Unit
) {
    var checkIn by remember { mutableStateOf(existingSchedule?.checkInTime ?: "09:00") }
    var checkOut by remember { mutableStateOf(existingSchedule?.checkOutTime ?: "18:00") }
    var isRestDay by remember { mutableStateOf(existingSchedule?.isRestDay ?: false) }
    var selectedBranchName by remember { mutableStateOf(existingSchedule?.branchName ?: "") }

    var showTimePickerFor by remember { mutableStateOf<String?>(null) } // "IN" o "OUT"

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isRestDay, onCheckedChange = { isRestDay = it })
            Text("Es día de DESCANSO")
        }

        if (!isRestDay) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Entrada con Reloj
                OutlinedButton(
                    onClick = { showTimePickerFor = "IN" },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Schedule, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("ENTRADA", style = MaterialTheme.typography.labelSmall)
                        Text(checkIn, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    }
                }

                // Salida con Reloj
                OutlinedButton(
                    onClick = { showTimePickerFor = "OUT" },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Schedule, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("SALIDA", style = MaterialTheme.typography.labelSmall)
                        Text(checkOut, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            var expandedBranch by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { expandedBranch = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Sucursal: ${selectedBranchName.ifBlank { "Asignación actual" }}")
                }
                DropdownMenu(expanded = expandedBranch, onDismissRequest = { expandedBranch = false }, modifier = Modifier.fillMaxWidth()) {
                    DropdownMenuItem(text = { Text("Sucursal Actual / Cualquiera") }, onClick = { selectedBranchName = ""; expandedBranch = false })
                    branches.forEach { b ->
                        DropdownMenuItem(text = { Text(b.name) }, onClick = { selectedBranchName = b.name; expandedBranch = false })
                    }
                }
            }
        }

        val currentSchedule = com.abtsplazita.posplazita.domain.Schedule(
            id = existingSchedule?.id ?: 0,
            employeeId = 0,
            dayOfWeek = dayOfWeek,
            checkInTime = if (isRestDay) null else checkIn,
            checkOutTime = if (isRestDay) null else checkOut,
            isRestDay = isRestDay,
            branchName = if (isRestDay) null else selectedBranchName
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            if (existingSchedule != null) {
                TextButton(onClick = { onDelete(existingSchedule) }) {
                    Text("BORRAR", color = Color.Red)
                }
            }
            
            Spacer(Modifier.weight(1f))

            if (dayOfWeek == 1) { // LUNES
                OutlinedButton(
                    onClick = { onSaveToAll(currentSchedule) },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("COPIAR A TODA LA SEMANA", style = MaterialTheme.typography.labelSmall)
                }
            }

            Button(onClick = { onSave(currentSchedule) }) {
                Text("GUARDAR")
            }
        }
    }

    if (showTimePickerFor != null) {
        val isIn = showTimePickerFor == "IN"
        val currentTime = if (isIn) checkIn else checkOut
        val parts = currentTime.split(":")
        val state = rememberTimePickerState(
            initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 9,
            initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showTimePickerFor = null },
            confirmButton = {
                TextButton(onClick = {
                    val hour = state.hour.toString().padStart(2, '0')
                    val min = state.minute.toString().padStart(2, '0')
                    if (isIn) checkIn = "$hour:$min" else checkOut = "$hour:$min"
                    showTimePickerFor = null
                }) { Text("ACEPTAR") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerFor = null }) { Text("CANCELAR") }
            },
            title = { Text(if (isIn) "Hora de Entrada" else "Hora de Salida") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = state)
                }
            }
        )
    }
}

@Composable
fun PayrollScreen(viewModel: ContabilidadViewModel) {
    val employees by viewModel.allEmployees.collectAsState()
    val selectedEmployee by viewModel.selectedEmployee.collectAsState()
    val payrollResult by viewModel.payrollResult.collectAsState()
    val pendingStatus by viewModel.isEmployeePendingPayment.collectAsState()
    
    val baseSalary8h by viewModel.baseSalary8h.collectAsState()
    var baseSalaryText by remember(baseSalary8h) { mutableStateOf(baseSalary8h.toString()) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isCompact = maxWidth < 900.dp

        Column(modifier = Modifier.fillMaxSize()) {
            // Barra superior de ajustes de sueldo
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Ajustes:", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = baseSalaryText,
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) baseSalaryText = it },
                        label = { Text("Pago 8h") },
                        modifier = Modifier.width(100.dp),
                        prefix = { Text("$") },
                        singleLine = true
                    )
                    Button(onClick = { viewModel.updateBaseSalary8h(baseSalaryText.toDoubleOrNull() ?: 315.0) }) {
                        Text("OK")
                    }
                    if (!isCompact) {
                        Spacer(Modifier.weight(1f))
                        Text("Costo hora: $${(baseSalary8h / 8.0).formatPrice()}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (isCompact) {
                // VISTA MÓVIL: Selector arriba o navegación
                if (selectedEmployee == null) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(employees) { emp ->
                            val isPending = pendingStatus.containsKey(emp.id)
                            val pendingAmount = pendingStatus[emp.id] ?: 0.0
                            
                            ListItem(
                                headlineContent = { 
                                    Text(
                                        emp.fullName,
                                        color = if (isPending) Color.Red else Color.Unspecified,
                                        fontWeight = if (isPending) FontWeight.Bold else FontWeight.Normal
                                    ) 
                                },
                                supportingContent = {
                                    if (isPending) {
                                        Text(
                                            "PAGO PENDIENTE: $${pendingAmount.formatPrice()}", 
                                            color = Color.Red, 
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Black
                                        )
                                    } else {
                                        Text("Semana Pagada", color = Color(0xFF4CAF50), style = MaterialTheme.typography.labelSmall)
                                    }
                                },
                                modifier = Modifier.clickable { viewModel.selectEmployee(emp) }
                            )
                            HorizontalDivider()
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        IconButton(onClick = { viewModel.selectEmployee(null) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                        PayrollDetailContent(payrollResult, viewModel)
                    }
                }
            } else {
                // VISTA ESCRITORIO
                Row(modifier = Modifier.weight(1f)) {
                    Card(modifier = Modifier.width(300.dp).fillMaxHeight()) {
                        LazyColumn {
                            items(employees) { emp ->
                                val isSelected = selectedEmployee?.id == emp.id
                                val isPending = pendingStatus.containsKey(emp.id)
                                val pendingAmount = pendingStatus[emp.id] ?: 0.0
                                
                                Surface(
                                    onClick = { viewModel.selectEmployee(emp) },
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            emp.fullName, 
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isPending) Color.Red else Color.Unspecified
                                        )
                                        if (isPending) {
                                            Text(
                                                "PENDIENTE: $${pendingAmount.formatPrice()}", 
                                                style = MaterialTheme.typography.labelLarge, 
                                                color = Color.Red,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                    Box(modifier = Modifier.weight(1f).padding(16.dp)) {
                        PayrollDetailContent(payrollResult, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun PayrollDetailContent(payrollResult: PayrollCalculator.PayrollResult?, viewModel: ContabilidadViewModel) {
    if (payrollResult != null) {
        val manualAmount by viewModel.manualAmountText.collectAsState()
        
        Column(modifier = Modifier.fillMaxSize()) {
            Text("Cálculo de Nómina", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            
            Card(modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                    Text(payrollResult.detailedReport, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // CAMPO MANUAL
            OutlinedTextField(
                value = manualAmount,
                onValueChange = { viewModel.updateManualAmount(it) },
                label = { Text("Monto Real Pagado") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                prefix = { Text("$ ") },
                textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            Button(
                onClick = { viewModel.payPayroll(payrollResult) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("MARCAR COMO PAGADO ($${manualAmount.text})")
            }
            
            Spacer(Modifier.height(12.dp))
            
            var showAbsenceDialog by remember { mutableStateOf(false) }
            OutlinedButton(
                onClick = { showAbsenceDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
            ) {
                Icon(Icons.Default.EventBusy, null)
                Spacer(Modifier.width(8.dp))
                Text("REGISTRAR FALTA")
            }

            if (showAbsenceDialog) {
                AbsenceRecordDialog(
                    employee = payrollResult.employee,
                    onDismiss = { showAbsenceDialog = false },
                    onConfirm = { date, isJustified ->
                        viewModel.saveAbsence(payrollResult.employee, date, isJustified)
                        showAbsenceDialog = false
                    }
                )
            }
        }
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Selecciona un empleado para calcular su nómina", color = Color.Gray)
        }
    }
}

@Composable
fun AttendanceScreen(viewModel: ContabilidadViewModel) {
    val attendance by viewModel.allAttendance.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Historial de Asistencia y Turnos", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(attendance) { record ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    ListItem(
                        headlineContent = { Text(record.userId) },
                        supportingContent = { 
                            Column {
                                val startDt = Instant.fromEpochMilliseconds(record.startTime).toLocalDateTime(TimeZone.currentSystemDefault())
                                Text("Entrada: ${startDt.date} ${startDt.time.toString().take(5)}")
                                record.endTime?.let { 
                                    val endDt = Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault())
                                    Text("Salida: ${endDt.date} ${endDt.time.toString().take(5)}")
                                }
                            }
                        },
                        trailingContent = {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${record.hoursWorked.formatPrice()} hrs", fontWeight = FontWeight.Bold)
                                Text("$${record.payAmount.formatPrice()}", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AbsenceRecordDialog(employee: Employee, onDismiss: () -> Unit, onConfirm: (Long, Boolean) -> Unit) {
    var isJustified by remember { mutableStateOf(false) }
    val now = com.abtsplazita.posplazita.currentTimeMillis()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Falta: ${employee.fullName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Se registrará una inasistencia para el día de HOY.")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isJustified, onCheckedChange = { isJustified = it })
                    Text("¿Es falta justificada?")
                }
                Text("Nota: Las faltas injustificadas eliminan el bono semanal automáticamente.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(now, isJustified) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                Text("REGISTRAR FALTA")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCELAR") }
        }
    )
}

@Composable
fun EmployeeEditDialog(onDismiss: () -> Unit, onSave: (Employee) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var salary by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("") }
    val nameFR = remember { FocusRequester() }
    val phoneFR = remember { FocusRequester() }
    val salaryFR = remember { FocusRequester() }
    val branchFR = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo Empleado") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name, 
                    onValueChange = { name = it }, 
                    label = { Text("Nombre Completo") },
                    modifier = Modifier.fillMaxWidth().focusRequester(nameFR).onPreviewKeyEvent {
                        if (it.key == Key.Enter || it.key == Key.NumPadEnter) {
                            if (it.type == KeyEventType.KeyDown) phoneFR.requestFocus()
                            true
                        } else false
                    },
                    singleLine = true
                )
                OutlinedTextField(
                    value = phone, 
                    onValueChange = { phone = it }, 
                    label = { Text("Teléfono") }, 
                    modifier = Modifier.fillMaxWidth().focusRequester(phoneFR).onPreviewKeyEvent {
                        if (it.key == Key.Enter || it.key == Key.NumPadEnter) {
                            if (it.type == KeyEventType.KeyDown) salaryFR.requestFocus()
                            true
                        } else false
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                OutlinedTextField(
                    value = salary, 
                    onValueChange = { salary = it }, 
                    label = { Text("Sueldo Diario") }, 
                    modifier = Modifier.fillMaxWidth().focusRequester(salaryFR).onPreviewKeyEvent {
                        if (it.key == Key.Enter || it.key == Key.NumPadEnter) {
                            if (it.type == KeyEventType.KeyDown) branchFR.requestFocus()
                            true
                        } else false
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = branch, 
                    onValueChange = { branch = it }, 
                    label = { Text("Sucursal") }, 
                    modifier = Modifier.fillMaxWidth().focusRequester(branchFR).onPreviewKeyEvent {
                        if (it.key == Key.Enter || it.key == Key.NumPadEnter) {
                            if (it.type == KeyEventType.KeyDown && name.isNotBlank()) {
                                onSave(Employee(fullName = name, phoneNumber = phone, baseSalary = salary.toDoubleOrNull() ?: 0.0, branch = branch))
                            }
                            true
                        } else false
                    },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(Employee(fullName = name, phoneNumber = phone, baseSalary = salary.toDoubleOrNull() ?: 0.0, branch = branch))
                onDismiss()
            }) { Text("GUARDAR") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCELAR") }
        }
    )

    LaunchedEffect(Unit) {
        repeat(3) {
            kotlinx.coroutines.delay(200)
            try { nameFR.requestFocus() } catch(e: Exception) {}
        }
    }
}
