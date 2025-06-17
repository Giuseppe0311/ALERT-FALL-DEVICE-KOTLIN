package com.example.cuidadoabuelitonative.navigation

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.cuidadoabuelitonative.screens.BluetoothDeviceScreen
import com.example.cuidadoabuelitonative.screens.BluetoothDeviceViewModel
import com.example.cuidadoabuelitonative.screens.DashboardDeviceViewModel
import com.example.cuidadoabuelitonative.screens.DashboardScreen
import com.example.cuidadoabuelitonative.screens.LoadingScreen
import com.example.cuidadoabuelitonative.screens.WiFiScannerScreen

@Composable
fun NavGraph(navController: NavHostController) {

    val application = LocalContext.current.applicationContext as Application

    val btvm: BluetoothDeviceViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    )

    val dashVm: DashboardDeviceViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    )

    val deviceId by dashVm.deviceId.collectAsState()

    LaunchedEffect(Unit) {
        dashVm.getDeviceIdPreferences()
    }

    LaunchedEffect(deviceId) {
        kotlinx.coroutines.delay(1000)
        when {
            deviceId != null -> {
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.Loading.route) { inclusive = true }
                }
            }

            else -> {
                navController.navigate(Screen.Bluetooth.route) {
                    popUpTo(Screen.Loading.route) { inclusive = true }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Loading.route
    ) {
        composable(Screen.Bluetooth.route) {
            BluetoothDeviceScreen(
                navController,
                btvm = btvm,
                dashboardDeviceViewModel = dashVm
            )
        }
        composable(Screen.WiFi.route) {
            WiFiScannerScreen(
                bluetoothViewModel = btvm,
                onNavigateToBluetooth = {
                    btvm.disconnectAndClearDevices()
                    navController.popBackStack()
                },
                onNavigateToDashboard = {
                    dashVm.getDeviceIdPreferences()
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Bluetooth.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Loading.route) {
            LoadingScreen()
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                navController = navController,
                dashboardDeviceViewModel = dashVm,
                btvm = btvm
            )
        }


    }
}