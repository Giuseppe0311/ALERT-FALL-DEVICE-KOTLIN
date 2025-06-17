package com.example.cuidadoabuelitonative.screens

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cuidadoabuelitonative.dto.BluetoothDevice
import com.example.cuidadoabuelitonative.dto.WiFiNetwork
import com.juul.kable.Advertisement
import com.juul.kable.Filter
import com.juul.kable.Peripheral
import com.juul.kable.Scanner
import com.juul.kable.WriteType
import com.juul.kable.logs.Logging
import com.juul.kable.logs.SystemLogEngine
import com.juul.kable.peripheral
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.UUID
import androidx.core.content.edit

class BluetoothDeviceViewModel(
    application: Application
) : AndroidViewModel(application) {


    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _devices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val devices: StateFlow<List<BluetoothDevice>> = _devices.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _bluetoothEnabled = MutableStateFlow(false)
    val bluetoothEnabled: StateFlow<Boolean> = _bluetoothEnabled.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isWifiScanning = MutableStateFlow(false)
    val isWifiScanning: StateFlow<Boolean> = _isWifiScanning.asStateFlow()


    private val _isWifiConnecting = MutableStateFlow(false)
    val isWifiConnecting: StateFlow<Boolean> = _isWifiConnecting.asStateFlow()


    private val _isWifiConnected = MutableStateFlow(false)
    val isWifiConnected: StateFlow<Boolean> = _isWifiConnected.asStateFlow()

    private val _wifiConnectionFailMessage = MutableStateFlow<String?>(null)
    val wifiConnectionFailMessage: StateFlow<String?> = _wifiConnectionFailMessage.asStateFlow()

    private val _wifiNetworks = MutableStateFlow<List<WiFiNetwork>>(emptyList())
    val wifiNetworks: StateFlow<List<WiFiNetwork>> = _wifiNetworks.asStateFlow()

    private var activePeripheral: Peripheral? = null

    private var scanJob: Job? = null

    private val bluetoothManager =
        getApplication<Application>().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    //VARIABLES DE ENVIO PARA BLE
    private val uuidDevice = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")

    private var notificationJob: Job? = null

    private var wifiBuffer = ""

    private var wifiConnectionResponseBuffer = ""

    // Variables para control de reintentos WiFi
    private var wifiScanRetryCount = 0
    private val maxWifiRetries = 3

    companion object {
        private const val DELIM_START = "<<START>>"
        private const val DELIM_END = "<<END>>"
    }

    private val sharedPreferences =
        getApplication<Application>().getSharedPreferences("CuidadoAbuelito", Context.MODE_PRIVATE)

    init {
        _bluetoothEnabled.value = bluetoothAdapter?.isEnabled == true
    }

    fun searchForDevices() {
        //

        scanJob?.cancel()

        scanJob = viewModelScope.launch {
            disconnectSync()

            _devices.value = emptyList()
            _errorMessage.value = null
            _isSearching.value = true

            try {
                // Verificar permisos y Bluetooth
                if (!hasRequiredPermissions()) {
                    _errorMessage.value =
                        "Se necesitan permisos de Bluetooth para buscar dispositivos"
                    return@launch
                }

                if (bluetoothAdapter?.isEnabled != true) {
                    _errorMessage.value = "Bluetooth no está habilitado"
                    return@launch
                }
                if (!isLocationEnabled()) {
                    _errorMessage.value =
                        "Debes habilitar la ubicación para encontrar el Dispositivo"
                    return@launch
                }
                _isSearching.value = true
                _devices.value = emptyList()
                _errorMessage.value = null

                // Usar Kable para escanear dispositivos BLE
                val scanner = Scanner {
                    filters = listOf(Filter.Service(uuidDevice))
                }
                val foundDevices = mutableSetOf<BluetoothDevice>()

                withTimeout(5000) { // 30 segundos de timeout
                    scanner.advertisements
                        .catch { exception ->
                            Log.e("BluetoothScan", "Error durante el escaneo", exception)
                            _errorMessage.value = "Error durante el escaneo: ${exception.message}"
                        }
                        .collect { advertisement ->
                            Log.d(
                                "BluetoothScan",
                                "Advertisement recibido: ${advertisement.identifier}"
                            )
                            Log.d("BluetoothScan", "Nombre: ${advertisement.name}")
                            Log.d("BluetoothScan", "RSSI: ${advertisement.rssi}")
                            Log.d("BluetoothScan", "Servicios: ${advertisement.address}")

                            val device = mapAdvertisementToDevice(advertisement)
                            if (device != null) {
                                val wasEmpty = foundDevices.isEmpty()
                                if (foundDevices.none { it.address == device.address }) {
                                    foundDevices.add(device)
                                    _devices.value = foundDevices.toList()
                                    Log.d(
                                        "BluetoothScan",
                                        "Dispositivo agregado: ${device.name} (${device.address})"
                                    )

                                    if (wasEmpty) {
                                        Log.d("BluetoothScan", "Primer dispositivo encontrado!")
                                    }
                                }
                            }
                        }
                }

            } catch (e: TimeoutCancellationException) {
                Log.d(
                    "BluetoothScan",
                    "Escaneo completado por timeout. Dispositivos encontrados: ${_devices.value.size}"
                )
                if (_devices.value.isEmpty()) {
                    _errorMessage.value = "No se encontraron dispositivos BLE en el área"
                }
            } catch (e: Exception) {
                Log.e("BluetoothScan", "Error en searchForDevices", e)
                _errorMessage.value = "Error al buscar dispositivos: ${e.message}"
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun connectToDevice(device: BluetoothDevice) = viewModelScope.launch {
        if (_isConnected.value) {
            disconnectSync()
        }
        _isConnecting.value = true
        val peripheral = peripheral(device.address) {
            logging {
                engine = SystemLogEngine
                level = Logging.Level.Events
                format = Logging.Format.Compact
            }
        }

        activePeripheral = peripheral

        try {
            peripheral.connect()
            _errorMessage.value = null
            _isConnected.value = true
            Log.d("BluetoothScan", "Conexión exitosa con ${device.name}")
        } catch (e: Exception) {
            _isConnected.value = false
            _errorMessage.value = "Fallo al conectar con el dispositivo: ${e.message}"
        } finally {
            _isConnecting.value = false
        }
    }


    fun disconnectAndClearDevices() {
        viewModelScope.launch {
            disconnectSync()
            _isWifiConnected.value = false
            _isWifiScanning.value = false
            _wifiNetworks.value = emptyList()
            _wifiConnectionFailMessage.value = null
            _isWifiConnecting.value = false
        }
    }


    fun requestDeviceWiFiScan() = viewModelScope.launch {
        if (!_isConnected.value || activePeripheral == null) {
            Log.d("BLE_WiFi", "No hay dispositivo BLE conectado")
            _errorMessage.value = "No hay dispositivo BLE conectado"
            return@launch
        }

        // Si es un nuevo escaneo (llamado manualmente), resetear contador
        if (!_isWifiScanning.value) {
            wifiScanRetryCount = 0
        }

        _isWifiScanning.value = true
        _wifiNetworks.value = emptyList()
        _errorMessage.value = null

        try {
            val peripheral = activePeripheral!!

            // Descubrir servicios si no se han descubierto
            val services = peripheral.services ?: run {
                Log.d("BLE_WiFi", "Descubriendo servicios...")
                peripheral.services
            }

            val uartService = services?.find { it.serviceUuid == uuidDevice }
            if (uartService == null) {
                Log.d("BLE_WiFi", "Servicio UART no encontrado en el dispositivo")
                _errorMessage.value = "Servicio UART no encontrado en el dispositivo"
                _isWifiScanning.value = false
                return@launch
            }

            // Obtener características correctas
            val writeChar = uartService.characteristics.find {
                it.characteristicUuid.toString()
                    .split("-")
                    .first()
                    .equals("6E400002", ignoreCase = true)
            } ?: run {
                Log.d("BLE_WiFi", "No se encontró característica de escritura")
                _errorMessage.value = "No se encontró característica de escritura"
                _isWifiScanning.value = false
                return@launch
            }
            val notifyChar = uartService.characteristics.find {
                it.characteristicUuid.toString()
                    .split("-")
                    .first()
                    .equals("6E400003", ignoreCase = true)
            } ?: run {
                Log.d("BLE_WiFi", "No se encontró característica de notificación")
                _errorMessage.value = "No se encontró característica de notificación"
                _isWifiScanning.value = false
                return@launch
            }

            notificationJob?.cancel()

            // Suscribirse a notificaciones (TX)
            notificationJob = peripheral
                .observe(notifyChar)
                .onEach { data ->
                    val text = data.decodeToString()
                    Log.d("BLE_WiFi", "Chunk recibido: $text")
                    handleWiFiScanResponse(data)
                }
                .catch { e ->
                    Log.e("BLE_WiFi", "Error en notificaciones", e)
                }
                .launchIn(this)

            // Construir y fragmentar comando
            val jsonPayload = """{"service":"gw","action":"scan"}"""
            val fullMessage = "<<START>>$jsonPayload<<END>>"
            val bytes = fullMessage.toByteArray(Charsets.UTF_8)
            val chunkSize = 20

            for (offset in bytes.indices step chunkSize) {
                val end = minOf(offset + chunkSize, bytes.size)
                val slice = bytes.copyOfRange(offset, end)
                peripheral.write(writeChar, slice, WriteType.WithResponse)
                delay(30)
            }
            Log.d(
                "BLE_WiFi",
                "Comando de escaneo WiFi enviado en ${bytes.size} bytes (Intento ${wifiScanRetryCount + 1}/$maxWifiRetries)"
            )
        } catch (e: Exception) {
            Log.e("BLE_WiFi", "Error solicitando escaneo WiFi", e)
            _errorMessage.value = "Error al solicitar escaneo WiFi: ${e.message}"
            _isWifiScanning.value = false
        }
    }


    fun connectWifiOfDevice(ssid: String, password: String) = viewModelScope.launch {

        if (!_isConnected.value || activePeripheral == null) {
            Log.d("BLE_CONNECTING_WIFI", "No hay dispositivo BLE conectado")
            _errorMessage.value = "No hay dispositivo BLE conectado"
            return@launch
        }

        _isWifiConnecting.value = true
        _wifiConnectionFailMessage.value = null
        _isWifiConnected.value = false

        try {
            val peripheral = activePeripheral!!

            val services = peripheral.services ?: run {
                Log.d("BLE_CONNECTING_WIFI", "Descubriendo servicios...")
                peripheral.services
            }

            val uartService = services?.find { it.serviceUuid == uuidDevice }
            if (uartService == null) {
                Log.d("BLE_CONNECTING_WIFI", "Servicio UART no encontrado en el dispositivo")
                _errorMessage.value = "Servicio UART no encontrado en el dispositivo"
                _isWifiConnecting.value = false
                return@launch
            }

            // Obtener características correctas
            val writeChar = uartService.characteristics.find {
                it.characteristicUuid.toString()
                    .split("-")
                    .first()
                    .equals("6E400002", ignoreCase = true)
            } ?: run {
                Log.d("BLE_CONNECTING_WIFI", "No se encontró característica de escritura")
                _errorMessage.value = "No se encontró característica de escritura"
                _isWifiScanning.value = false
                return@launch
            }

            val notifyChar = uartService.characteristics.find {
                it.characteristicUuid.toString()
                    .split("-")
                    .first()
                    .equals("6E400003", ignoreCase = true)
            } ?: run {
                Log.d("BLE_CONNECTING_WIFI", "No se encontró característica de notificación")
                _errorMessage.value = "No se encontró característica de notificación"
                _isWifiScanning.value = false
                return@launch
            }


            notificationJob?.cancel()

            // Suscribirse a notificaciones (TX)
            notificationJob = peripheral
                .observe(notifyChar)
                .onEach { data ->
                    val text = data.decodeToString()
                    Log.d("BLE_CONNECTING_WIFI", "Chunk recibido: $text")
                    handleConnectionWifiResponse(data)
                }
                .catch { e ->
                    Log.e("BLE_CONNECTING_WIFI", "Error en notificaciones", e)
                }
                .launchIn(this)


            val jsonPayload =
                """{"service":"gw","action":"connect","ssid":"$ssid","password":"$password"}"""
            Log.d("BLE_CONNECTING_WIFI", "JSON: $jsonPayload")
            val fullMessage = "<<START>>$jsonPayload<<END>>"
            val bytes = fullMessage.toByteArray(Charsets.UTF_8)
            val chunkSize = 20

            for (offset in bytes.indices step chunkSize) {
                val end = minOf(offset + chunkSize, bytes.size)
                val slice = bytes.copyOfRange(offset, end)
                peripheral.write(writeChar, slice, WriteType.WithResponse)
                delay(30)
            }
            Log.d(
                "BLE_CONNECTING_WIFI",
                "Comando de conexion WiFi enviado en ${bytes.size} bytes (Intento ${wifiScanRetryCount + 1}/$maxWifiRetries)"
            )

        } catch (e: Exception) {
            Log.e("BLE_CONNECTING_WIFI", "Error Connectando al  WiFi", e)
            _errorMessage.value = "Error al Connectar el  WiFi: ${e.message}"
            _isWifiConnecting.value = false
        }
    }


    private suspend fun disconnectSync() {
        Log.d("BluetoothScan", "Desconectando...")
        activePeripheral?.let { per ->
            try {
                per.disconnect()
                Log.d("BluetoothScan", "Desconectado exitosamente")
            } catch (e: Exception) {
                // si no estaba conectado, lo ignoramos
                Log.w(
                    "BluetoothScan",
                    "Intento de desconectar pero no estaba conectado: ${e.message}"
                )
            }
        }
        _isConnected.value = false
        _isConnecting.value = false
        activePeripheral = null
        // Resetear contador de reintentos al desconectar
        wifiScanRetryCount = 0
        delay(100)
    }

    private fun mapAdvertisementToDevice(advertisement: Advertisement): BluetoothDevice? {
        return try {
            val name = advertisement.name?.takeIf { it.isNotBlank() }
                ?: "Dispositivo desconocido"

            val address = advertisement.identifier

            val signalStrength = when {
                advertisement.rssi > -50 -> "Excelente"
                advertisement.rssi > -70 -> "Buena"
                advertisement.rssi > -85 -> "Regular"
                else -> "Débil"
            }

            val deviceType = determineDeviceType(advertisement)

            BluetoothDevice(
                name = name,
                address = address,
                signalStrength = signalStrength,
                deviceType = deviceType
            )
        } catch (e: Exception) {
            Log.e("BluetoothScan", "Error mapeando dispositivo", e)
            null
        }
    }

    private fun isLocationEnabled(): Boolean {
        val lm = getApplication<Application>()
            .getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun determineDeviceType(advertisement: Advertisement): String {
        return "Cuidado a ancianos"
    }

    private fun handleWiFiScanResponse(data: ByteArray) {
        val chunk = data.decodeToString()
        wifiBuffer += chunk
        Log.d("BLE_WiFi", "Buffer acumulado: $wifiBuffer")

        // 2) Busca delimitadores
        val startIdx = wifiBuffer.indexOf(DELIM_START)
        val endIdx = wifiBuffer.indexOf(DELIM_END, startIdx + DELIM_START.length)

        if (startIdx != -1 && endIdx != -1) {
            // 3) Extrae el JSON
            val jsonString = wifiBuffer
                .substring(startIdx + DELIM_START.length, endIdx)
                .trim()
            // resetea buffer para futuros mensajes
            wifiBuffer = wifiBuffer.substring(endIdx + DELIM_END.length)

            try {
                // 4) Parsea sólo el array "networks"
                val jsonObj = org.json.JSONObject(jsonString)
                val arr = jsonObj.getJSONArray("networks")
                val list = mutableListOf<WiFiNetwork>()
                for (i in 0 until arr.length()) {
                    val ssid = arr.getString(i)
                    list.add(WiFiNetwork(ssid = ssid))
                }

                // 5) Actualiza el StateFlow
                _wifiNetworks.value = list

                // 6) Lógica de reintentos mejorada
                if (list.isEmpty() && wifiScanRetryCount < maxWifiRetries) {
                    wifiScanRetryCount++
                    Log.d(
                        "BLE_WiFi",
                        "Lista de redes vacía. Reintentando... (${wifiScanRetryCount}/$maxWifiRetries)"
                    )

                    // Pequeña pausa antes del reintento
                    viewModelScope.launch {
                        delay(1000) // Esperar 1 segundo antes del reintento
                        requestDeviceWiFiScan()
                    }
                } else {
                    // Finalizar escaneo
                    _isWifiScanning.value = false

                    if (list.isEmpty() && wifiScanRetryCount >= maxWifiRetries) {
                        Log.d(
                            "BLE_WiFi",
                            "Máximo número de reintentos alcanzado. No se encontraron redes WiFi."
                        )
                        _errorMessage.value =
                            "No se encontraron redes WiFi después de $maxWifiRetries intentos"
                        // Resetear contador para futuros escaneos
                        wifiScanRetryCount = 0
                    } else if (list.isNotEmpty()) {
                        Log.d(
                            "BLE_WiFi",
                            "Escaneo WiFi completado exitosamente. ${list.size} redes encontradas."
                        )
                        // Resetear contador para futuros escaneos
                        wifiScanRetryCount = 0
                    }
                }

            } catch (e: Exception) {
                Log.e("BLE_WiFi", "Error parseando redes WiFi", e)
                _errorMessage.value = "Respuesta WiFi malformada"
                _isWifiScanning.value = false
                // Resetear contador en caso de error
                wifiScanRetryCount = 0
            }
        }
    }

    private fun handleConnectionWifiResponse(data: ByteArray) {
        val chunk = data.decodeToString()
        wifiConnectionResponseBuffer += chunk
        Log.d("BLE_CONNECTING_WIFI", "Buffer acumulado: $wifiConnectionResponseBuffer")

        // 2) Busca delimitadores
        val startIdx = wifiConnectionResponseBuffer.indexOf(DELIM_START)
        val endIdx = wifiConnectionResponseBuffer.indexOf(DELIM_END, startIdx + DELIM_START.length)

        if (startIdx != -1 && endIdx != -1) {
            // 3) Extrae el JSON
            val jsonString = wifiConnectionResponseBuffer
                .substring(startIdx + DELIM_START.length, endIdx)
                .trim()
            // resetea buffer para futuros mensajes
            wifiConnectionResponseBuffer =
                wifiConnectionResponseBuffer.substring(endIdx + DELIM_END.length)

            try {
                // 4) Parsea sólo el array "networks"
                val jsonObj = org.json.JSONObject(jsonString)
                Log.d("BLE_CONNECTING_WIFI", "Respuesta del dispositivo: $jsonObj")
                val status = jsonObj.getString("status")
                if (status.equals("error")) {
                    _wifiConnectionFailMessage.value = jsonObj.getString("message")
                } else if (status.equals("success")) {
                    val deviceId = if (jsonObj.has("device_id")) {
                        jsonObj.getString("device_id")
                    } else null
                    if (!deviceId.isNullOrEmpty()) {
                        _isWifiConnected.value = true
                        _wifiConnectionFailMessage.value = null
                        saveDeviceInfo(deviceId)
                    } else {
                        _wifiConnectionFailMessage.value = "ID de dispositivo no encontrado"
                    }
                } else {
                    _wifiConnectionFailMessage.value = "Error desconocido"
                }
                _isWifiConnecting.value = false
            } catch (e: Exception) {
                Log.e("BLE_CONNECTING_WIFI", "Error parseando redes WiFi", e)
                _errorMessage.value = "Respuesta WiFi malformada"
                _isWifiConnecting.value = false
            }
        }
    }

    private fun saveDeviceInfo(deviceId: String?) {
        sharedPreferences.edit {

            if (!deviceId.isNullOrEmpty()) {
                putString("device_id", deviceId)
                Log.d("DeviceStorage", "Device ID guardado: $deviceId")
            }

        }
    }


    fun hasRequiredPermissions(): Boolean {
        val ctx = getApplication<Application>()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PermissionChecker.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        ctx,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PermissionChecker.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PermissionChecker.PERMISSION_GRANTED
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun setError(message: String) {
        _errorMessage.value = message
    }

    private fun stopSearch() {
        _isSearching.value = false
    }


    fun clearWiFiScanState() {
        notificationJob?.cancel()
        _isWifiScanning.value = false
        _wifiNetworks.value = emptyList()
        wifiBuffer = ""
        wifiScanRetryCount = 0
    }

    override fun onCleared() {
        super.onCleared()
        notificationJob?.cancel()
        stopSearch()
        // Resetear contador al limpiar el ViewModel
        wifiScanRetryCount = 0
    }
}