package com.abtsplazita.posplazita.ui.inventory

import androidx.compose.foundation.background
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
import com.abtsplazita.posplazita.domain.formatPrice
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.datetime.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestockScreen(viewModel: RestockViewModel, onBack: () -> Unit) {
    val suggestions by viewModel.restockSuggestions.collectAsState()
    val analysisDays by viewModel.analysisDays.collectAsState()
    val leadTimeDays by viewModel.leadTimeDays.collectAsState()
    val isUpdating by viewModel.isUpdating.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resurtimiento Inteligente") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            // --- PANEL DE CONTROL ---
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Configuración de Análisis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = analysisDays.toString(),
                            onValueChange = { it.toIntOrNull()?.let { d -> viewModel.setAnalysisDays(d) } },
                            label = { Text("Días Historial") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            supportingText = { Text("Tiempo a analizar") }
                        )
                        OutlinedTextField(
                            value = leadTimeDays.toString(),
                            onValueChange = { it.toIntOrNull()?.let { d -> viewModel.setLeadTimeDays(d) } },
                            label = { Text("Días a Cubrir") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            supportingText = { Text("Tiempo resurtido") }
                        )
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Button(
                        onClick = { viewModel.applySuggestions() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isUpdating && suggestions.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isUpdating) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Icon(Icons.Default.AutoFixHigh, null)
                            Spacer(Modifier.width(8.dp))
                            Text("ACTUALIZAR MÍNIMOS Y MÁXIMOS EN CATÁLOGO")
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // --- CABECERA DE TABLA ---
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Producto", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold)
                    Text("VDP", modifier = Modifier.weight(0.8f), fontWeight = FontWeight.Bold)
                    Text("Stock", modifier = Modifier.weight(0.8f), fontWeight = FontWeight.Bold)
                    Text("Sugerido", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold)
                    Text("Pedir", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }

            // --- LISTA DE PRODUCTOS ---
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(suggestions) { item ->
                    RestockRow(item)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun RestockRow(item: RestockItem) {
    val bgColor = if (item.isForgotten) Color(0xFFFFEBEE) else Color.Transparent
    
    Row(
        modifier = Modifier.fillMaxWidth().background(bgColor).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(2f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.product.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                if (item.isForgotten) {
                    Spacer(Modifier.width(8.dp))
                    Surface(color = Color.Red, shape = RoundedCornerShape(4.dp)) {
                        Text("OLVIDADO", color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp))
                    }
                }
            }
            Text("Cód: ${item.product.barcode}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            if (item.isForgotten && item.lastRestockDate != null) {
                val dt = Instant.fromEpochMilliseconds(item.lastRestockDate).toLocalDateTime(TimeZone.currentSystemDefault())
                Text("Últ. resurtido: ${dt.date}", style = MaterialTheme.typography.labelSmall, color = Color.Red)
            }
        }
        
        // VDP (Venta Diaria Promedio)
        Text(
            text = item.avgDailySales.formatPrice(), 
            modifier = Modifier.weight(0.8f),
            style = MaterialTheme.typography.bodySmall
        )
        
        // Stock Actual
        Text(
            text = item.currentStock.formatPrice(), 
            modifier = Modifier.weight(0.8f),
            style = MaterialTheme.typography.bodySmall
        )

        // Sugerencia (Min/Max)
        Column(modifier = Modifier.weight(1.2f)) {
            Text("Min: ${item.suggestedMin.formatPrice()}", style = MaterialTheme.typography.labelSmall)
            Text("Max: ${item.suggestedMax.formatPrice()}", style = MaterialTheme.typography.labelSmall)
        }

        // Faltante (A Pedir)
        Text(
            text = item.missingQuantity.formatPrice(), 
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = if (item.missingQuantity > 0) MaterialTheme.colorScheme.primary else Color.Gray
        )
    }
}
