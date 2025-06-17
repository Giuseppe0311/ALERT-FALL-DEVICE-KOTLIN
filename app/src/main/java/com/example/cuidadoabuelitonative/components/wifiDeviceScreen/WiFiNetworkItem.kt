package com.example.cuidadoabuelitonative.components.wifiDeviceScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
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
    Card (
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNetworkClick(network) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono de señal WiFi
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Señal WiFi",
                tint =Color(0xFF4CAF50)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Información de la red
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = network.ssid,
                        fontSize = 16.sp,
                        fontWeight = if (network.isConnected) FontWeight.Bold else FontWeight.Normal
                    )

                    if (network.isConnected) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Conectado",
                            fontSize = 12.sp,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier
                                .background(
                                    Color(0xFF4CAF50).copy(alpha = 0.1f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text =  "Red segura",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Icono de seguridad
            Icon(
                imageVector =  Icons.Default.Refresh,
                contentDescription = "Red segura",
                tint =  MaterialTheme.colorScheme.primary
            )
        }
    }
}