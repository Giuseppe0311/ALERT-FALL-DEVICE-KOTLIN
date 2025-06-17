package com.example.cuidadoabuelitonative.components.wifiDeviceScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cuidadoabuelitonative.dto.WiFiNetwork

@Composable
fun WiFiNetworksList(
    networks: List<WiFiNetwork>,
    onNetworkClick: (WiFiNetwork) -> Unit
) {
    if (networks.isNotEmpty()) {
        Text(
            text = "Redes disponibles:",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(networks) { network ->
                WiFiNetworkItem(
                    network = network,
                    onNetworkClick = onNetworkClick
                )
            }
        }
    } else {
        WiFiEmptyState()
    }
}