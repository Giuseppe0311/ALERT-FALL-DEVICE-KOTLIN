package com.example.cuidadoabuelitonative.screens

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DashboardDeviceViewModel(app: Application) : AndroidViewModel(app) {
    private val _deviceId = MutableStateFlow<String?>(null)
    val deviceId = _deviceId.asStateFlow()
    private val sharedPreferences =
        getApplication<Application>().getSharedPreferences("CuidadoAbuelito", Context.MODE_PRIVATE)


    fun getDeviceIdPreferences() {
        val deviceId = sharedPreferences.getString("device_id", null)
        if (deviceId != null) {
            _deviceId.value = deviceId
        } else {
            _deviceId.value = null
        }
    }
}