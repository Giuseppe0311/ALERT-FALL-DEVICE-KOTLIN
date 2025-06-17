package com.example.cuidadoabuelitonative.components.wifiDeviceScreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.cuidadoabuelitonative.dto.WiFiNetwork

@Composable
fun WiFiPasswordDialog(
    network: WiFiNetwork,
    password: String,
    passwordVisible: Boolean,
    isWifiConnecting: Boolean,
    onPasswordChange: (String) -> Unit,
    onPasswordVisibilityChange: () -> Unit,
    onConnect: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = {
            Text("Conectar a ${network.ssid}")
        },
        text = {
            Column {
                Text(
                    text = "Ingresa la contraseña para conectarte a esta red:",
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("Contraseña") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(
                            onClick = onPasswordVisibilityChange
                        ) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConnect,
                enabled = password.isNotBlank() && !isWifiConnecting
            ) {
                Text(if (isWifiConnecting) "Conectando..." else "conectar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isWifiConnecting) {
                Text("Cancelar")
            }
        }
    )
}