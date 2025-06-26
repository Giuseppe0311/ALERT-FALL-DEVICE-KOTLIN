// DashboardScreen.kt
package com.example.cuidadoabuelitonative.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.cuidadoabuelitonative.dto.FallInfo
import com.example.cuidadoabuelitonative.navigation.Screen

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DashboardScreen(
    navController: NavHostController,
    btvm: BluetoothDeviceViewModel,
    dashboardDeviceViewModel: DashboardDeviceViewModel
) {
    val deviceId by dashboardDeviceViewModel.deviceId.collectAsState()
    val deviceInfo by dashboardDeviceViewModel.deviceInfo.collectAsState()
    val fallInfo by dashboardDeviceViewModel.fallInfo.collectAsState()
    val isLoading by dashboardDeviceViewModel.isLoading.collectAsState()
    val lastUpdate by dashboardDeviceViewModel.lastUpdateTime.collectAsState()
    val isAlarmActive by dashboardDeviceViewModel.isAlarmActive.collectAsState()

    LaunchedEffect(Unit) {
        dashboardDeviceViewModel.getDeviceIdPreferences()
        dashboardDeviceViewModel.checkAlarmStatus()
    }

    LaunchedEffect(deviceId) {
        if (deviceId != null) {
            dashboardDeviceViewModel.startBackgroundDetection()
            dashboardDeviceViewModel.startUIPolling()
        } else {
            dashboardDeviceViewModel.stopBackgroundDetection()
            dashboardDeviceViewModel.stopUIPolling()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF667eea),
                        Color(0xFF764ba2)
                    )
                )
            )
            .padding(16.dp)
    ) {
        // Botón de alarma activa (si hay alarma)
        if (isAlarmActive) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFfed7d7)),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFf56565),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "🚨 ALARMA ACTIVA",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF742a2a)
                        )
                        Text(
                            "Caída detectada - Toca para detener",
                            fontSize = 14.sp,
                            color = Color(0xFF9c4221)
                        )
                    }
                    Button(
                        onClick = { dashboardDeviceViewModel.stopAlarm() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFf56565),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("DETENER")
                    }
                }
            }
        }
        // Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color(0xFF667eea),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Cuidado Abuelito",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2d3748)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                if (deviceInfo != null) Color(0xFF48bb78)
                                else Color(0xFFed8936)
                            )
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Dispositivo: ${deviceId ?: "No registrado"}",
                        fontSize = 16.sp,
                        color = Color(0xFF4a5568)
                    )
                }
                if (deviceInfo != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Última conexión: $lastUpdate",
                        fontSize = 14.sp,
                        color = Color(0xFF718096)
                    )
                    Text(
                        "IP: ${deviceInfo!!.ip}",
                        fontSize = 14.sp,
                        color = Color(0xFF718096)
                    )
                }
                if (isLoading) {
                    Spacer(Modifier.height(8.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF667eea)
                    )
                }
            }
        }

        // Falls Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFf56565),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Avisos del Dispositivo",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2d3748)
                    )
                    Spacer(Modifier.weight(1f))
                    if (!fallInfo.isNullOrEmpty()) {
                        Badge(containerColor = Color(0xFFf56565)) {
                            Text("${fallInfo!!.size}", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                when {
                    deviceId == null -> EmptyStateCard(
                        icon = Icons.Default.CheckCircle,
                        title = "Sin dispositivo",
                        message = "Registra tu dispositivo primero para comenzar el monitoreo"
                    )

                    deviceInfo == null -> LoadingCard()
                    fallInfo.isNullOrEmpty() -> EmptyStateCard(
                        icon = Icons.Default.CheckCircle,
                        title = "Todo bien por aquí",
                        message = "No se han detectado caídas"
                    )

                    else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(fallInfo!!) { fall ->
                            AnimatedVisibility(
                                visible = true,
                                enter = slideInVertically(
                                    initialOffsetY = { it },
                                    animationSpec = tween(300)
                                ) + fadeIn(animationSpec = tween(300))
                            ) {
                                FallCard(fall)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                btvm.disconnectAndClearDevices()
                navController.navigate(Screen.Bluetooth.route) {
                    popUpTo(Screen.Dashboard.route) { inclusive = true }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White.copy(alpha = 0.9f),
                contentColor = Color(0xFF667eea)
            )
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Reconfigurar Dispositivo", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ----------------------
// Composables auxiliares
// ----------------------

@Composable
fun FallCard(fall: FallInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFfed7d7)),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFf56565)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AccountBox,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Caída Detectada",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF742a2a)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    fall.occurredAt,
                    fontSize = 14.sp,
                    color = Color(0xFF9c4221)
                )
            }

            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF9c4221)
            )
        }
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    message: String,
    isPositive: Boolean = false
) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        Box(
            Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    if (isPositive) Color(0xFF48bb78).copy(alpha = 0.1f)
                    else Color(0xFF718096).copy(alpha = 0.1f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (isPositive) Color(0xFF48bb78) else Color(0xFF718096)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2d3748),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            fontSize = 14.sp,
            color = Color(0xFF718096),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

@Composable
fun LoadingCard() {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = Color(0xFF667eea),
            strokeWidth = 4.dp
        )
        Spacer(Modifier.height(20.dp))
        Text(
            "Cargando información...",
            fontSize = 16.sp,
            color = Color(0xFF718096),
            textAlign = TextAlign.Center
        )
    }
}
