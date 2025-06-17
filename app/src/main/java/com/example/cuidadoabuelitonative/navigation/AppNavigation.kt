package com.example.cuidadoabuelitonative.navigation

sealed class Screen(val route: String) {
    object Bluetooth : Screen("bluetooth")
    object WiFi : Screen("wifi")
    object Dashboard: Screen("dashboard")
    object Loading : Screen("loading")

}