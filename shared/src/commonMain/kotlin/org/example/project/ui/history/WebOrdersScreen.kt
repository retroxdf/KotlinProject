package com.abtsplazita.posplazita.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.abtsplazita.posplazita.domain.*
import com.abtsplazita.posplazita.ui.PosViewModel
import kotlinx.datetime.*

@Composable
fun WebOrdersScreen(viewModel: PosViewModel, onBack: () -> Unit, onNavigateToPos: () -> Unit) {
    val orders by viewModel.webOrders.collectAsState()
    var selectedOrder by remember { mutableStateOf<WebOrder?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Text("Pedidos Recibidos de la Web", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (orders.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay pedidos web pendientes.", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(orders) { order ->
                    WebOrderCard(
                        order = order,
                        onClick = { selectedOrder = order },
                        onAccept = {
                            viewModel.acceptWebOrder(order)
                            onNavigateToPos()
                        },
                        onCancel = {
                            viewModel.updateWebOrderStatus(order, WebOrderStatus.CANCELLED)
                        }
                    )
                }
            }
        }
    }

    if (selectedOrder != null) {
        WebOrderDetailsDialog(
            order = selectedOrder!!,
            onDismiss = { selectedOrder = null }
        )
    }
}

@Composable
fun WebOrderCard(
    order: WebOrder,
    onClick: () -> Unit,
    onAccept: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = when(order.status) {
                WebOrderStatus.PENDING -> MaterialTheme.colorScheme.primaryContainer
                WebOrderStatus.PREPARING -> Color(0xFFFFF9C4) // Amarillo
                WebOrderStatus.READY -> Color(0xFFC8E6C9) // Verde claro
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Pedido #${order.id.takeLast(5)}", fontWeight = FontWeight.Bold)
                    val dt = Instant.fromEpochMilliseconds(order.timestamp).toLocalDateTime(TimeZone.currentSystemDefault())
                    Text("${dt.time.toString().take(5)} - ${order.customerName}", style = MaterialTheme.typography.bodySmall)
                }
                Text("$${order.total.formatPrice()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            }
            
            if (order.address != null) {
                Text("Destino: ${order.address}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(12.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (order.status == WebOrderStatus.PENDING) {
                    Button(onClick = onAccept, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Check, null)
                        Spacer(Modifier.width(8.dp))
                        Text("ACEPTAR")
                    }
                }
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(0.5f), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)) {
                    Text("RECHAZAR")
                }
            }
        }
    }
}

@Composable
fun WebOrderDetailsDialog(order: WebOrder, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Detalle de Pedido Web") },
        text = {
            Column(modifier = Modifier.widthIn(min = 400.dp)) {
                Text("Cliente: ${order.customerName}", fontWeight = FontWeight.Bold)
                if (order.customerPhone != null) Text("Tel: ${order.customerPhone}")
                if (order.notes != null) Text("Notas: ${order.notes}", color = Color.Gray)
                
                Spacer(Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp)) {
                    Text("Cant.", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Text("Producto", modifier = Modifier.weight(3f), fontWeight = FontWeight.Bold)
                    Text("Subtotal", modifier = Modifier.weight(1.5f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold)
                }
                
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(order.items) { item ->
                        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${item.quantity}", modifier = Modifier.weight(1f))
                            Text(item.productName, modifier = Modifier.weight(3f))
                            Text("$${item.subtotal.formatPrice()}", modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("CERRAR") } }
    )
}
