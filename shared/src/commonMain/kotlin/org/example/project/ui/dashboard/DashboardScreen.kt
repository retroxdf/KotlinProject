package com.abtsplazita.posplazita.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.abtsplazita.posplazita.domain.formatPrice

@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val stats by viewModel.dashboardData.collectAsState()
    val period by viewModel.period.collectAsState()
    val terminals by viewModel.availableTerminals.collectAsState()
    val selectedTerminalId by viewModel.selectedTerminalId.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Panel de Control", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
                Text("Resumen ejecutivo de tu negocio", color = Color.Gray)
            }
            
            // Selector de Terminal (Filtro por Caja)
            if (terminals.size > 1) {
                var showTerminalMenu by remember { mutableStateOf(false) }
                Box(modifier = Modifier.padding(end = 16.dp)) {
                    OutlinedButton(onClick = { showTerminalMenu = true }) {
                        Icon(Icons.Default.Store, null)
                        Spacer(Modifier.width(8.dp))
                        Text(terminals.find { it.id == selectedTerminalId }?.name ?: "Todas las Cajas")
                    }
                    DropdownMenu(expanded = showTerminalMenu, onDismissRequest = { showTerminalMenu = false }) {
                        DropdownMenuItem(text = { Text("Todas las Cajas") }, onClick = { viewModel.selectTerminal(null); showTerminalMenu = false })
                        terminals.forEach { t ->
                            DropdownMenuItem(text = { Text(t.name) }, onClick = { viewModel.selectTerminal(t.id); showTerminalMenu = false })
                        }
                    }
                }
            }
            
            // Selector de Periodo
            SingleChoiceSegmentedButtonRow {
                DashboardPeriod.values().forEachIndexed { index, p ->
                    SegmentedButton(
                        selected = period == p,
                        onClick = { viewModel.setPeriod(p) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = DashboardPeriod.values().size)
                    ) {
                        Text(when(p) {
                            DashboardPeriod.TODAY -> "Hoy"
                            DashboardPeriod.WEEK -> "7D"
                            DashboardPeriod.MONTH -> "30D"
                        })
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Tarjetas Principales ADAPTATIVAS
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val gridCols = if (maxWidth < 600.dp) 1 else 3
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val statsList = listOf(
                    Triple("Ventas Totales", "$${stats.totalSales.formatPrice()}", Icons.Default.TrendingUp to Color(0xFF2E7D32)),
                    Triple("Gastos", "$${stats.totalExpenses.formatPrice()}", Icons.Default.TrendingDown to Color.Red),
                    Triple("Ganancia Neta", "$${stats.netProfit.formatPrice()}", Icons.Default.AccountBalanceWallet to Color(0xFF2196F3))
                )
                
                statsList.chunked(gridCols).forEach { rowStats ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        rowStats.forEach { (title, value, meta) ->
                            val (icon, color) = meta
                            StatCard(
                                modifier = Modifier.weight(1f),
                                title = title,
                                value = value,
                                icon = icon,
                                color = color
                            )
                        }
                        if (rowStats.size < gridCols) {
                            repeat(gridCols - rowStats.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Gráfica de Top Productos
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Top 30 Productos más vendidos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                
                if (stats.topProducts.isEmpty()) {
                    Text("No hay datos de ventas para este periodo.", color = Color.Gray)
                } else {
                    val maxQty = stats.topProducts.maxOf { it.second }
                    stats.topProducts.forEach { (name, qty) ->
                        Column(modifier = Modifier.padding(bottom = 16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                                Text("${qty.toInt()} unidades", fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(if (maxQty > 0) (qty / maxQty).toFloat() else 0f)
                                    .height(12.dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Reporte generado al momento", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.align(Alignment.End))
    }
}

@Composable
fun StatCard(modifier: Modifier, title: String, value: String, icon: ImageVector, color: Color) {
    Card(
        modifier = modifier.heightIn(min = 100.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.labelMedium, color = Color.Gray, maxLines = 1)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value, 
                style = MaterialTheme.typography.titleLarge, 
                fontWeight = FontWeight.Black, 
                color = color,
                maxLines = 1
            )
        }
    }
}
