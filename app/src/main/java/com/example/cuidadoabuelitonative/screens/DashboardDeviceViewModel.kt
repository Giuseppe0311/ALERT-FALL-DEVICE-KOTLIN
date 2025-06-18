package com.example.cuidadoabuelitonative.screens

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cuidadoabuelitonative.dto.DeviceInfo
import com.example.cuidadoabuelitonative.network.RetrofitInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardDeviceViewModel(app: Application) : AndroidViewModel(app) {
    private val _deviceId = MutableStateFlow<String?>(null)
    val deviceId = _deviceId.asStateFlow()
    private val sharedPreferences =
        getApplication<Application>().getSharedPreferences("CuidadoAbuelito", Context.MODE_PRIVATE)

    private val _deviceInfo = MutableStateFlow<DeviceInfo?>(null)
    val deviceInfo = _deviceInfo.asStateFlow()

    fun getDeviceIdPreferences() {
        val deviceId = sharedPreferences.getString("device_id", null)
        if (deviceId != null) {
            _deviceId.value = deviceId
        } else {
            _deviceId.value = null
        }
    }

    fun fetchDeviceInfo() {
        Log.d("DashboardDeviceViewModel", "Fetching device info for device ID: ${_deviceId.value}")
        val id = _deviceId.value ?: return
        viewModelScope.launch {
            try {
                val info = RetrofitInstance.api.getDeviceStatus(id)
                Log.d("DashboardDeviceViewModel", "Device info fetched: $info")
                _deviceInfo.value = info
            } catch (e: Exception) {
                Log.e("DashboardVM", "   • ERROR fetching info", e)
                _deviceInfo.value = null
            }
        }
    }
}