package com.example.cuidadoabuelitonative.screens

import android.app.Application
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.cuidadoabuelitonative.components.bluetoothDeviceScreen.DevicesList
import com.example.cuidadoabuelitonative.components.bluetoothDeviceScreen.NotFoundContent
import com.example.cuidadoabuelitonative.components.bluetoothDeviceScreen.SearchingContent
import com.example.cuidadoabuelitonative.navigation.Screen


@Composable
fun BluetoothDeviceScreen(
    onavController: NavHostController,
    btvm: BluetoothDeviceViewModel,
    dashboardDeviceViewModel: DashboardDeviceViewModel
) {

    val isSearching by btvm.isSearching.collectAsState()
    val devices by btvm.devices.collectAsState()
    var hasSearched by remember { mutableStateOf(false) }
    val bluetoothEnabled by btvm.bluetoothEnabled.collectAsState()
    val errorMessage by btvm.errorMessage.collectAsState()
    val isConnected by btvm.isConnected.collectAsState()
    val isConnecting by btvm.isConnecting.collectAsState()
    val context = LocalContext.current
    val deviceId by dashboardDeviceViewModel.deviceId.collectAsState()

    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Android 12+
        arrayOf(
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        // Android < 12
        arrayOf(
            android.Manifest.permission.BLUETOOTH,
            android.Manifest.permission.BLUETOOTH_ADMIN,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsResult ->
        val allPermissionsGranted = permissionsResult.values.all { it }

        if (allPermissionsGranted) {
            hasSearched = true
            btvm.clearError()
            btvm.searchForDevices()
        } else {
            btvm.setError("Se necesitan todos los permisos de Bluetooth para buscar dispositivos")
        }
    }

    val handleSearchClick = {
        if (!isSearching) {
            if (btvm.hasRequiredPermissions()) {
                // Ya tenemos permisos, proceder con la búsqueda
                hasSearched = true
                btvm.clearError()
                btvm.searchForDevices()
            } else {
                // No tenemos permisos, solicitarlos
                permissionLauncher.launch(permissions)
            }
        }
    }

    LaunchedEffect(isConnected) {

        if (isConnecting) {
            Toast.makeText(context, "Conectando...", Toast.LENGTH_SHORT).show()
        }

        if (isConnected) {
            Toast.makeText(context, "¡Dispositivo conectado!", Toast.LENGTH_SHORT).show()
            onavController.navigate(Screen.WiFi.route)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7FAFC))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Título de sección
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4A90E2),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Vincular Dispositivo",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D3748)
                )
            }

            if (deviceId != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        onavController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Bluetooth.route) { inclusive = true }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Volver al dashboard")
                }
            }

            errorMessage?.let { message ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = message,
                            color = Color(0xFFDC2626),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Sección de búsqueda
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Buscar dispositivo",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2D3748),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Asegúrate de que el dispositivo esté encendido y en modo de emparejamiento",
                        fontSize = 14.sp,
                        color = Color(0xFF718096),
                        lineHeight = 20.sp
                    )
                }
            }
            // Botón de búsqueda
            Button(
                onClick = handleSearchClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        isSearching -> Color(0xFF94A3B8)
                        !bluetoothEnabled -> Color(0xFFDC2626)
                        else -> Color(0xFF4A90E2)
                    }
                ),
                shape = RoundedCornerShape(16.dp),
                enabled = !isSearching && !isConnecting
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSearching) "Buscando..." else "Buscar dispositivo",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }

            // Contenido dinámico basado en el estado
            when {
                isSearching -> {
                    // Estado de búsqueda (Loader)
                    SearchingContent()
                }

                devices.isNotEmpty() -> {
                    DevicesList(
                        devices = devices,
                        onDeviceClick = { device ->
                            btvm.connectToDevice(device)
                        },
                        !isConnecting
                    )
                }

                hasSearched && devices.isEmpty() && errorMessage == null -> {
                    // No se encontraron dispositivos después de buscar
                    NotFoundContent()
                }
            }

        }
    }
}



