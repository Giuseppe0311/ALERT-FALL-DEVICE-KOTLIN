package com.example.cuidadoabuelitonative.components.bluetoothDeviceScreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SearchingContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        CircularProgressIndicator(
            modifier = Modifier.size(60.dp),
            color = Color(0xFF4A90E2),
            strokeWidth = 4.dp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Buscando dispositivos...",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF2D3748)
        )

        Text(
            text = "Esto puede tomar unos segundos",
            fontSize = 14.sp,
            color = Color(0xFF718096),
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}