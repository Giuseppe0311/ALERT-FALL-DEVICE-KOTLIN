package com.example.cuidadoabuelitonative.components.bluetoothDeviceScreen

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cuidadoabuelitonative.dto.BluetoothDevice

@Composable
fun DevicesList(
    devices: List<BluetoothDevice>,
    onDeviceClick: (BluetoothDevice) -> Unit,
    isEnable: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Dispositivos encontrados (${devices.size})",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF2D3748),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(devices) { device ->
                DeviceItem(
                    device = device,
                    enabled = isEnable,
                    onDeviceClick = onDeviceClick
                )
            }
        }
    }
}
