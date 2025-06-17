package com.example.cuidadoabuelitonative.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cuidadoabuelitonative.components.wifiDeviceScreen.WiFiNetworksList
import com.example.cuidadoabuelitonative.components.wifiDeviceScreen.WiFiPasswordDialog
import com.example.cuidadoabuelitonative.components.wifiDeviceScreen.WiFiScanButton
import com.example.cuidadoabuelitonative.dto.WiFiNetwork


@Composable
fun WiFiScannerScreen(
    bluetoothViewModel: BluetoothDeviceViewModel,
    onNavigateToBluetooth: () -> Unit = {},
    onNavigateToDashboard: () -> Unit = {}
) {
    var selectedNetwork by remember { mutableStateOf<WiFiNetwork?>(null) }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }

    val isWifiScanning by bluetoothViewModel.isWifiScanning.collectAsState()
    val wifiNetworks by bluetoothViewModel.wifiNetworks.collectAsState()

    val isWifiConnecting by bluetoothViewModel.isWifiConnecting.collectAsState()
    val wifiConnectionFailMessage by bluetoothViewModel.wifiConnectionFailMessage.collectAsState()
    val context = LocalContext.current
    val isWifiConnected by bluetoothViewModel.isWifiConnected.collectAsState()


    LaunchedEffect(wifiConnectionFailMessage) {
        if (!wifiConnectionFailMessage.isNullOrBlank()) {
            Toast
                .makeText(context, wifiConnectionFailMessage, Toast.LENGTH_LONG)
                .show()
        }
    }

    LaunchedEffect(isWifiConnected) {
        if (isWifiConnected) {
            showPasswordDialog = false
            selectedNetwork = null
            password = ""
            onNavigateToDashboard()

        }
    }

    DisposableEffect(Unit) {
        onDispose {
            bluetoothViewModel.clearWiFiScanState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Redes WiFi",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onNavigateToBluetooth,
                modifier = Modifier.weight(1f)
            ) {
                Text("Volver")
            }
        }

        WiFiScanButton(
            isScanning = isWifiScanning,
            onScanClick = {
                bluetoothViewModel.requestDeviceWiFiScan()
            }
        )


        Spacer(modifier = Modifier.height(16.dp))



        WiFiNetworksList(
            networks = wifiNetworks,
            onNetworkClick = { network ->
                selectedNetwork = network
                showPasswordDialog = true
                password = ""
            }
        )

    }

    // Dialog para ingresar contraseña
    if (showPasswordDialog && selectedNetwork != null) {
        WiFiPasswordDialog(
            network = selectedNetwork!!,
            password = password,
            passwordVisible = passwordVisible,
            isWifiConnecting = isWifiConnecting,
            onPasswordChange = { password = it },
            onPasswordVisibilityChange = { passwordVisible = !passwordVisible },
            onConnect = {
                Log.d(
                    "WiFiScannerScreen",
                    "Conectando a ${selectedNetwork!!.ssid} con contraseña $password"
                )
                bluetoothViewModel.connectWifiOfDevice(selectedNetwork!!.ssid, password)
            },
            onDismiss = {
                showPasswordDialog = false
                selectedNetwork = null
                password = ""
            }
        )
    }

}
