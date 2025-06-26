package com.example.cuidadoabuelitonative.components.wifiDeviceScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.NetworkWifi1Bar
import androidx.compose.material.icons.rounded.NetworkWifi2Bar
import androidx.compose.material.icons.rounded.NetworkWifi3Bar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cuidadoabuelitonative.dto.WiFiNetwork

@Composable
fun WiFiNetworkItem(
    network: WiFiNetwork,
    onNetworkClick: (WiFiNetwork) -> Unit
) {
    val signalIcon = when {
        network.rssi >= -50 -> Icons.Filled.NetworkWifi
        network.rssi >= -60 -> Icons.Rounded.NetworkWifi3Bar
        network.rssi >= -70 -> Icons.Rounded.NetworkWifi2Bar
        network.rssi >= -80 -> Icons.Rounded.NetworkWifi1Bar
        else                -> Icons.Filled.NetworkCheck
    }
    val isSecure = network.auth == "x"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .clickable { onNetworkClick(network) },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono de señal WiFi según RSSI
            Icon(
                imageVector = signalIcon,
                contentDescription = "Señal: ${network.rssi} dBm",
                tint = if (network.isConnected) MaterialTheme.colorScheme.primary else Color.Gray,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = network.ssid,
                        fontSize = 16.sp,
                        fontWeight = if (network.isConnected) FontWeight.Bold else FontWeight.Medium,
                        color = if (network.isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    if (network.isConnected) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Conectado",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${network.rssi} dBm • ${if (isSecure) "Segura" else "Abierta"}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Icono de seguridad
            Icon(
                imageVector = if (isSecure) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = if (isSecure) "Red segura" else "Red abierta",
                tint = if (isSecure) MaterialTheme.colorScheme.primary else Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
