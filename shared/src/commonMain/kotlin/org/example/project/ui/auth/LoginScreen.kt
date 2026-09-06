package com.abtsplazita.posplazita.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.layout.ContentScale
import com.abtsplazita.posplazita.domain.User
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@Composable
fun LoginScreen(viewModel: AuthViewModel, logoUrl: String) {
    val users by viewModel.allUsers.collectAsState()
    val showNipPrompt by viewModel.showNipPrompt.collectAsState()
    val mustChangeNip by viewModel.mustChangeNip.collectAsState()
    val error by viewModel.error.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.TopCenter) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally, 
            modifier = Modifier.padding(top = 48.dp, start = 32.dp, end = 32.dp, bottom = 32.dp)
        ) {
            // Logo del Sistema
            Card(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    KamelImage(
                        resource = { asyncPainterResource(data = logoUrl) },
                        contentDescription = "Logo",
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentScale = ContentScale.Fit,
                        onLoading = { Icon(Icons.Default.Store, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary) },
                        onFailure = { Icon(Icons.Default.Store, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "BIENVENIDO A PLAZITA POS",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            Text("Selecciona tu usuario para ingresar", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(4.dp))
            Text("Versión 1.0.9", style = MaterialTheme.typography.labelSmall, color = Color.Gray.copy(alpha = 0.6f))
            
            Spacer(modifier = Modifier.height(44.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 180.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.widthIn(max = 900.dp)
            ) {
                items(users) { user ->
                    UserLoginCard(user) { viewModel.selectUserForLogin(user) }
                }
            }
        }
    }

    if (showNipPrompt != null) {
        NipPromptDialog(
            user = showNipPrompt!!,
            error = error,
            onConfirm = { viewModel.loginWithNip(it) },
            onDismiss = { viewModel.closeNipPrompt() }
        )
    }

    if (mustChangeNip != null) {
        ChangeNipDialog(
            user = mustChangeNip!!,
            onConfirm = { viewModel.changeNipAndLogin(it) }
        )
    }
}

@Composable
fun UserLoginCard(user: User, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .height(120.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    Icons.Default.Person,
                    null,
                    modifier = Modifier.padding(8.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(user.username.uppercase(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(user.role.name, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}

@Composable
fun NipPromptDialog(user: User, error: String?, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var nip by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ingresar NIP") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Hola ${user.username}, ingresa tu NIP de 4 dígitos", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = nip,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) nip = it },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.width(150.dp).focusRequester(focusRequester).onPreviewKeyEvent {
                        if (it.type == KeyEventType.KeyDown && (it.key == Key.Enter || it.key == Key.NumPadEnter)) {
                            if (nip.length == 4) onConfirm(nip)
                            true
                        } else false
                    },
                    textStyle = MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.Center),
                    singleLine = true
                )
                if (error != null) {
                    Text(error, color = Color.Red, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(nip) }, enabled = nip.length == 4) { Text("ENTRAR") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCELAR") }
        }
    )

    LaunchedEffect(Unit) {
        // Intentar varias veces el foco para asegurar que el diálogo esté listo
        repeat(3) {
            kotlinx.coroutines.delay(200)
            try { focusRequester.requestFocus() } catch (e: Exception) {}
        }
    }
}

@Composable
fun ChangeNipDialog(user: User, onConfirm: (String) -> Unit) {
    var nip1 by remember { mutableStateOf("") }
    var nip2 by remember { mutableStateOf("") }
    val focus1 = remember { FocusRequester() }
    val focus2 = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = {}, // Forzar cambio
        title = { Text("Actualizar NIP de Seguridad") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Por seguridad, debes cambiar tu NIP inicial (1111).", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = nip1,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) nip1 = it },
                    label = { Text("Nuevo NIP (4 dígitos)") },
                    modifier = Modifier.fillMaxWidth().focusRequester(focus1).onPreviewKeyEvent {
                        if (it.type == KeyEventType.KeyDown && (it.key == Key.Enter || it.key == Key.NumPadEnter)) {
                            focus2.requestFocus(); true
                        } else false
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )
                OutlinedTextField(
                    value = nip2,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) nip2 = it },
                    label = { Text("Confirmar NIP") },
                    modifier = Modifier.fillMaxWidth().focusRequester(focus2).onPreviewKeyEvent {
                        if (it.type == KeyEventType.KeyDown && (it.key == Key.Enter || it.key == Key.NumPadEnter)) {
                            if (nip1 == nip2 && nip1.length == 4) onConfirm(nip1)
                            true
                        } else false
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )
                if (nip1.isNotEmpty() && nip2.isNotEmpty() && nip1 != nip2) {
                    Text("Los NIP no coinciden", color = Color.Red, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(nip1) }, enabled = nip1 == nip2 && nip1.length == 4 && nip1 != "1111") {
                Text("ACTUALIZAR E INGRESAR")
            }
        }
    )

    LaunchedEffect(Unit) {
        repeat(3) {
            kotlinx.coroutines.delay(200)
            try { focus1.requestFocus() } catch (e: Exception) {}
        }
    }
}
