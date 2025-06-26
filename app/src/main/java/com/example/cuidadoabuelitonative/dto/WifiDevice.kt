package com.example.cuidadoabuelitonative.dto

data class WiFiNetwork(
    val ssid: String,
    val rssi: Int,
    val auth: String,
    val isConnected: Boolean = false
)