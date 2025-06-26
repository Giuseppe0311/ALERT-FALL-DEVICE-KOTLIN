// FallDetectionService.kt
package com.example.cuidadoabuelitonative.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.cuidadoabuelitonative.MainActivity
import com.example.cuidadoabuelitonative.R
import com.example.cuidadoabuelitonative.dto.FallInfo
import com.example.cuidadoabuelitonative.network.RetrofitInstance
import kotlinx.coroutines.*
import androidx.core.content.edit

class FallDetectionService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var detectionJob: Job? = null
    private var previousFallCount = 0
    private var deviceId: String? = null

    companion object {
        const val CHANNEL_ID = "FALL_DETECTION_CHANNEL"
        const val NOTIFICATION_ID = 2001
        const val ACTION_START_DETECTION = "START_DETECTION"
        const val ACTION_STOP_DETECTION = "STOP_DETECTION"
        const val EXTRA_DEVICE_ID = "DEVICE_ID"
        const val POLLING_INTERVAL = 5_000L // 5 segundos

        fun startDetection(context: Context, deviceId: String) {
            val intent = Intent(context, FallDetectionService::class.java).apply {
                action = ACTION_START_DETECTION
                putExtra(EXTRA_DEVICE_ID, deviceId)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopDetection(context: Context) {
            val intent = Intent(context, FallDetectionService::class.java).apply {
                action = ACTION_STOP_DETECTION
            }
            context.startService(intent)
        }

        fun isServiceRunning(context: Context): Boolean {
            val manager =
                context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            @Suppress("DEPRECATION")
            for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
                if (FallDetectionService::class.java.name == service.service.className) {
                    return true
                }
            }
            return false
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
        setupWakeLock()
        Log.d("FallDetectionService", "Service created")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_DETECTION -> {
                deviceId = intent.getStringExtra(EXTRA_DEVICE_ID)
                startDetection()
            }

            ACTION_STOP_DETECTION -> {
                stopDetection()
                stopSelf()
            }
        }

        return START_STICKY // El servicio se reinicia si es terminado
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Detección de Caídas",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Monitoreo continuo de caídas"
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun setupWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "FallDetectionService:WakeLock"
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startDetection() {
        if (detectionJob?.isActive == true) return

        val currentDeviceId = deviceId ?: run {
            Log.e("FallDetectionService", "No device ID provided")
            stopSelf()
            return
        }

        // Adquirir wake lock para mantener el servicio activo
        wakeLock?.acquire(10 * 60 * 1000L) // 10 minutos, se renovará automáticamente

        val notification = createForegroundNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Inicializar el contador de caídas
        initializeFallCount(currentDeviceId)

        // Iniciar el polling
        detectionJob = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            while (isActive) {
                try {
                    if (isNetworkAvailable()) {
                        checkForNewFalls(currentDeviceId)
                    } else {
                        Log.w("FallDetectionService", "No network available, skipping check")
                    }

                    // Renovar wake lock cada 5 minutos
                    wakeLock?.let { wl ->
                        if (!wl.isHeld) {
                            wl.acquire(10 * 60 * 1000L)
                        }
                    }

                } catch (e: Exception) {
                    Log.e("FallDetectionService", "Error in detection loop", e)
                }

                delay(POLLING_INTERVAL)
            }
        }

        Log.d("FallDetectionService", "Fall detection started for device: $currentDeviceId")
    }

    private fun createForegroundNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Detección de Caídas Activa")
            .setContentText("Monitoreando dispositivo en segundo plano")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openAppPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSound(null)
            .setVibrate(null)
            .build()
    }

    private fun initializeFallCount(deviceId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val initialList = RetrofitInstance.api.getFallDetected(deviceId)
                previousFallCount = initialList.size
                Log.d("FallDetectionService", "Initial fall count: $previousFallCount")
            } catch (e: Exception) {
                Log.e("FallDetectionService", "Error getting initial fall count", e)
                previousFallCount = 0
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun checkForNewFalls(deviceId: String) {
        try {
            val currentFalls = RetrofitInstance.api.getFallDetected(deviceId)
            val currentFallCount = currentFalls.size

            if (currentFallCount > previousFallCount && previousFallCount >= 0) {
                // Se detectó una nueva caída
                val latestFall = currentFalls.firstOrNull()
                if (latestFall != null) {
                    Log.d("FallDetectionService", "New fall detected: ${latestFall.occurredAt}")
                    triggerFallAlarm(latestFall)

                    // Actualizar SharedPreferences para notificar a la UI
                    updateUIState(true)
                }
            }

            previousFallCount = currentFallCount

        } catch (e: Exception) {
            Log.e("FallDetectionService", "Error checking for falls", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun triggerFallAlarm(fallInfo: FallInfo) {
        FallAlarmService.startAlarm(this, fallInfo)
        Log.d("FallDetectionService", "Fall alarm triggered from background service")
    }

    private fun updateUIState(alarmActive: Boolean) {
        val sharedPrefs = getSharedPreferences("CuidadoAbuelito", Context.MODE_PRIVATE)
        sharedPrefs.edit {
            putBoolean("alarm_active", alarmActive)
                .putLong("last_fall_time", System.currentTimeMillis())
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun stopDetection() {
        detectionJob?.cancel()
        detectionJob = null

        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }

        Log.d("FallDetectionService", "Fall detection stopped")
    }

    override fun onDestroy() {
        super.onDestroy()
        stopDetection()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // El servicio continúa funcionando incluso cuando se cierra la app
        Log.d("FallDetectionService", "App task removed, but detection service continues")
    }
}