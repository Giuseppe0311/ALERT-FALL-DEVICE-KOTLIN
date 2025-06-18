package com.example.cuidadoabuelitonative.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.cuidadoabuelitonative.navigation.Screen


@Composable
fun DashboardScreen(
    navController: NavHostController,
    btvm: BluetoothDeviceViewModel,
    dashboardDeviceViewModel: DashboardDeviceViewModel
) {

    val deviceId by dashboardDeviceViewModel.deviceId.collectAsState()
    val deviceInfo by dashboardDeviceViewModel.deviceInfo.collectAsState()

    LaunchedEffect(deviceId) {
        if (deviceId != null) {
            dashboardDeviceViewModel.fetchDeviceInfo()
        }
    }

    Column {
        Text(text = "Device ID: ${deviceId ?: "no registrado"}")


        when {
            deviceId == null -> Text("Registra tu dispositivo primero")
            deviceInfo == null -> Text("Cargando información...")
            else -> {
                Text(text = "Nombre: ${deviceInfo!!.lastSeen}")
                Text(text = "Estado: ${deviceInfo!!.ip}")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))


        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            // borramos el Dashboard de la pila y volvemos al inicio (Bluetooth)
            btvm.disconnectAndClearDevices()

            navController.navigate(Screen.Bluetooth.route) {
                popUpTo(Screen.Dashboard.route) { inclusive = true }
            }
        }) {
            Text("Reconfigurar mi dispositivo")
        }
    }


}