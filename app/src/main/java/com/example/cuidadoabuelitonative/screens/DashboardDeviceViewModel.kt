// DashboardDeviceViewModel.kt
package com.example.cuidadoabuelitonative.screens

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cuidadoabuelitonative.dto.DeviceInfo
import com.example.cuidadoabuelitonative.dto.FallInfo
import com.example.cuidadoabuelitonative.network.RetrofitInstance
import com.example.cuidadoabuelitonative.services.FallAlarmService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class DashboardDeviceViewModel(app: Application) : AndroidViewModel(app) {
    private val _deviceId = MutableStateFlow<String?>(null)
    val deviceId = _deviceId.asStateFlow()

    private val sharedPreferences = getApplication<Application>()
        .getSharedPreferences("CuidadoAbuelito", Context.MODE_PRIVATE)

    private val _deviceInfo = MutableStateFlow<DeviceInfo?>(null)
    val deviceInfo = _deviceInfo.asStateFlow()

    private val _fallInfo = MutableStateFlow<List<FallInfo>?>(null)
    val fallInfo = _fallInfo.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _lastUpdateTime = MutableStateFlow<Long>(0)
    val lastUpdateTime = _lastUpdateTime.asStateFlow()

    // Para controlar si hay una alarma activa
    private val _isAlarmActive = MutableStateFlow(false)
    val isAlarmActive = _isAlarmActive.asStateFlow()

    private var pollingJob: Job? = null
    private var previousFallCount = 0

    fun getDeviceIdPreferences() {
        val id = sharedPreferences.getString("device_id", null)
        if (id != null) {
            _deviceId.value = id
            Log.d("DashboardVM", "Device ID loaded: $id")
        } else {
            _deviceId.value = null
            Log.d("DashboardVM", "No device ID in prefs")
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getApplication<Application>()
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun fetchDeviceInfo() {
        val id = _deviceId.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val info = RetrofitInstance.api.getDeviceStatus(id)
                _deviceInfo.value = info
                _lastUpdateTime.value = System.currentTimeMillis()
                Log.d("DashboardVM", "Info: $info")
            } catch (e: Exception) {
                Log.e("DashboardVM", "Error fetching device info", e)
                _deviceInfo.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun fetchFallDetected() {
        val id = _deviceId.value ?: return
        viewModelScope.launch {
            try {
                val list = RetrofitInstance.api.getFallDetected(id)

                // Verificar si hay nuevas caídas
                val currentFallCount = list.size
                if (currentFallCount > previousFallCount && previousFallCount > 0) {
                    // Se detectó una nueva caída
                    val latestFall = list.firstOrNull()
                    if (latestFall != null) {
                        triggerFallAlarm(latestFall)
                    }
                }

                previousFallCount = currentFallCount
                _fallInfo.value = list
                Log.d("DashboardVM", "Falls count: ${list.size}")
            } catch (e: Exception) {
                Log.e("DashboardVM", "Error fetching falls", e)
                _fallInfo.value = emptyList()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun triggerFallAlarm(fallInfo: FallInfo) {
        _isAlarmActive.value = true
        FallAlarmService.startAlarm(getApplication(), fallInfo)
        Log.d("DashboardVM", "Fall alarm triggered for: ${fallInfo.occurredAt}")
    }

    fun stopAlarm() {
        _isAlarmActive.value = false
        FallAlarmService.stopAlarm(getApplication())
        Log.d("DashboardVM", "Fall alarm stopped by user")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun startPolling(intervalMs: Long = 5_000) {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            // Inicializar el contador en la primera consulta
            if (previousFallCount == 0) {
                try {
                    val initialList = RetrofitInstance.api.getFallDetected(_deviceId.value ?: "")
                    previousFallCount = initialList.size
                } catch (e: Exception) {
                    Log.e("DashboardVM", "Error in initial fall count", e)
                }
            }

            while (isActive) {
                if (isNetworkAvailable()) {
                    fetchDeviceInfo()
                    fetchFallDetected()
                }
                delay(intervalMs)
            }
        }
        Log.d("DashboardVM", "Polling started")
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        Log.d("DashboardVM", "Polling stopped")
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
        stopAlarm()
    }
}