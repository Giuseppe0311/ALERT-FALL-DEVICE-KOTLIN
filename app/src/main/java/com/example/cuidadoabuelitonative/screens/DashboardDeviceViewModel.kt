// DashboardDeviceViewModel.kt
package com.example.cuidadoabuelitonative.screens

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
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
import com.example.cuidadoabuelitonative.services.FallDetectionService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.core.content.edit

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

    // Para controlar si el servicio de detección está activo
    private val _isDetectionServiceActive = MutableStateFlow(false)
    val isDetectionServiceActive = _isDetectionServiceActive.asStateFlow()

    private var uiPollingJob: Job? = null
    private var sharedPrefsListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    init {
        // Escuchar cambios en SharedPreferences para detectar alarmas activadas por el servicio
        setupSharedPreferencesListener()
        checkDetectionServiceStatus()
    }

    private fun setupSharedPreferencesListener() {
        sharedPrefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "alarm_active" -> {
                    val isActive = sharedPreferences.getBoolean("alarm_active", false)
                    _isAlarmActive.value = isActive
                    if (isActive) {
                        // Actualizar la lista de caídas cuando se detecta una nueva
                        fetchFallDetected()
                    }
                }
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPrefsListener)
    }

    private fun checkDetectionServiceStatus() {
        _isDetectionServiceActive.value = FallDetectionService.isServiceRunning(getApplication())
    }

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

    fun fetchFallDetected() {
        val id = _deviceId.value ?: return
        viewModelScope.launch {
            try {
                val list = RetrofitInstance.api.getFallDetected(id)
                _fallInfo.value = list
                Log.d("DashboardVM", "Falls count: ${list.size}")
            } catch (e: Exception) {
                Log.e("DashboardVM", "Error fetching falls", e)
                _fallInfo.value = emptyList()
            }
        }
    }

    // Iniciar el servicio de detección en segundo plano
    fun startBackgroundDetection() {
        val id = _deviceId.value
        if (id != null) {
            FallDetectionService.startDetection(getApplication(), id)
            _isDetectionServiceActive.value = true
            Log.d("DashboardVM", "Background detection service started")
        } else {
            Log.e("DashboardVM", "Cannot start detection: no device ID")
        }
    }

    // Detener el servicio de detección
    fun stopBackgroundDetection() {
        FallDetectionService.stopDetection(getApplication())
        _isDetectionServiceActive.value = false
        Log.d("DashboardVM", "Background detection service stopped")
    }

    // Para detener manualmente una alarma
    fun stopAlarm() {
        Log.d("DashboardVM", "Stopping alarm from ViewModel")

        // 1. Actualizar el estado local primero
        _isAlarmActive.value = false

        // 2. Limpiar SharedPreferences
        sharedPreferences.edit {
            putBoolean("alarm_active", false)
            remove("last_fall_time") // Opcional: limpiar timestamp
        }

        // 3. Detener el servicio de alarma
        FallAlarmService.stopAlarm(getApplication())

        Log.d("DashboardVM", "Fall alarm stopped by user")
    }

    // También agrega este método para verificar el estado al inicializar
    fun checkAlarmStatus() {
        val alarmActive = sharedPreferences.getBoolean("alarm_active", false)
        _isAlarmActive.value = alarmActive
        Log.d("DashboardVM", "Alarm status checked: $alarmActive")
    }

    // Polling solo para la UI (información del dispositivo)
    fun startUIPolling(intervalMs: Long = 10_000) { // Menos frecuente para la UI
        if (uiPollingJob?.isActive == true) return

        uiPollingJob = viewModelScope.launch {
            while (isActive) {
                if (isNetworkAvailable()) {
                    fetchDeviceInfo()
                    fetchFallDetected()
                }
                delay(intervalMs)
            }
        }
        Log.d("DashboardVM", "UI polling started")
    }

    fun stopUIPolling() {
        uiPollingJob?.cancel()
        uiPollingJob = null
        Log.d("DashboardVM", "UI polling stopped")
    }

    // Método para verificar y sincronizar el estado
    fun syncAlarmState() {
        val alarmActive = sharedPreferences.getBoolean("alarm_active", false)
        _isAlarmActive.value = alarmActive

        val serviceActive = FallDetectionService.isServiceRunning(getApplication())
        _isDetectionServiceActive.value = serviceActive
    }

    override fun onCleared() {
        super.onCleared()
        stopUIPolling()

        // NO detenemos el servicio de detección aquí, para que continúe en segundo plano
        // stopBackgroundDetection()

        // Desregistrar el listener
        sharedPrefsListener?.let { listener ->
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }

        Log.d("DashboardVM", "ViewModel cleared, but background service continues")
    }
}