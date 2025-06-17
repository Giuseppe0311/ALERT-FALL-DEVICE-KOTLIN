package com.example.cuidadoabuelitonative.components.bluetoothDeviceScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cuidadoabuelitonative.dto.BluetoothDevice

@Composable
fun DeviceItem(
    device: BluetoothDevice,
    enabled: Boolean,
    onDeviceClick: (BluetoothDevice) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onDeviceClick(device) },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono del dispositivo
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = Color(0xFFEBF8FF),
                        shape = RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4A90E2),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Información del dispositivo
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2D3748)
                )
                Text(
                    text = device.deviceType,
                    fontSize = 14.sp,
                    color = Color(0xFF718096),
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = device.address,
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Indicador de señal
            Column(horizontalAlignment = Alignment.End) {
                val signalColor = when (device.signalStrength) {
                    "Fuerte" -> Color(0xFF10B981)
                    "Medio" -> Color(0xFFF59E0B)
                    else -> Color(0xFFEF4444)
                }

                Text(
                    text = device.signalStrength,
                    fontSize = 12.sp,
                    color = signalColor,
                    fontWeight = FontWeight.Medium
                )

                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier
                        .size(16.dp)
                        .padding(top = 4.dp)
                )
            }
        }
    }
}