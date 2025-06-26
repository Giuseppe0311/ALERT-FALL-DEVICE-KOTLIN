// FallAlarmService.kt
package com.example.cuidadoabuelitonative.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.cuidadoabuelitonative.MainActivity
import com.example.cuidadoabuelitonative.R
import com.example.cuidadoabuelitonative.dto.FallInfo

class FallAlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var isAlarming = false

    companion object {
        const val CHANNEL_ID = "FALL_ALARM_CHANNEL"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START_ALARM = "START_ALARM"
        const val ACTION_STOP_ALARM = "STOP_ALARM"
        const val EXTRA_FALL_INFO = "FALL_INFO"

        @RequiresApi(Build.VERSION_CODES.O)
        fun startAlarm(context: Context, fallInfo: FallInfo) {
            val intent = Intent(context, FallAlarmService::class.java).apply {
                action = ACTION_START_ALARM
                putExtra(EXTRA_FALL_INFO, fallInfo.occurredAt)
            }
            context.startForegroundService(intent)
        }

        fun stopAlarm(context: Context) {
            val intent = Intent(context, FallAlarmService::class.java).apply {
                action = ACTION_STOP_ALARM
            }
            context.startService(intent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupVibrator()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_ALARM -> {
                val fallTime = intent.getStringExtra(EXTRA_FALL_INFO) ?: "Desconocido"
                startAlarmSystem(fallTime)
            }
            ACTION_STOP_ALARM -> {
                Log.d("FallAlarmService", "STOP_ALARM recibido")
                stopAlarmSystem() // Cambio: usar stopAlarmSystem() completo
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Alarmas de Caídas",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificaciones de caídas detectadas"
            enableVibration(true)
            setSound(null, null)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun setupVibrator() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun startAlarmSystem(fallTime: String) {
        if (isAlarming) return

        isAlarming = true

        val notification = createAlarmNotification(fallTime)
        startForeground(NOTIFICATION_ID, notification)

        startRingtoneSound()
        startContinuousVibration()

        Log.d("FallAlarmService", "Alarm system started for fall at: $fallTime")
    }

    private fun createAlarmNotification(fallTime: String): Notification {
        // Intent para abrir la app
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent para detener la alarma
        val stopAlarmIntent = Intent(this, FallAlarmService::class.java).apply {
            action = ACTION_STOP_ALARM
        }
        val stopAlarmPendingIntent = PendingIntent.getService(
            this, 1, stopAlarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🚨 CAÍDA DETECTADA")
            .setContentText("Se ha detectado una caída a las $fallTime")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Se ha detectado una caída a las $fallTime.\n\nToca para abrir la aplicación o detén la alarma."))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openAppPendingIntent)
            .addAction(R.drawable.ic_stop, "DETENER ALARMA", stopAlarmPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSound(null)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .build()
    }

    private fun startRingtoneSound() {
        try {
            // Liberar MediaPlayer anterior si existe
            stopSound()

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )

                // Usar el tono de llamada predeterminado
                val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                setDataSource(this@FallAlarmService, ringtoneUri)
                isLooping = true

                setOnPreparedListener {
                    if (isAlarming) { // Solo iniciar si la alarma sigue activa
                        start()
                        Log.d("FallAlarmService", "Ringtone started successfully")
                    }
                }

                setOnErrorListener { _, what, extra ->
                    Log.e("FallAlarmService", "MediaPlayer error: what=$what, extra=$extra")
                    tryFallbackRingtone()
                    true
                }

                prepareAsync() // Usar prepareAsync para evitar bloqueos
            }
        } catch (e: Exception) {
            Log.e("FallAlarmService", "Error starting ringtone sound", e)
            tryFallbackRingtone()
        }
    }

    private fun tryFallbackRingtone() {
        try {
            stopSound()

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )

                // Usar tono de notificación como fallback
                val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                setDataSource(this@FallAlarmService, fallbackUri)
                isLooping = true

                setOnPreparedListener {
                    if (isAlarming) {
                        start()
                        Log.d("FallAlarmService", "Fallback ringtone started")
                    }
                }

                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("FallAlarmService", "Error starting fallback ringtone", e)
        }
    }

    private fun stopSound() {
        mediaPlayer?.apply {
            try {
                if (isPlaying) {
                    stop()
                }
                reset()
                release()
            } catch (e: Exception) {
                Log.e("FallAlarmService", "Error stopping sound", e)
            }
        }
        mediaPlayer = null
    }

    private fun startContinuousVibration() {
        try {
            val vibrationPattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrationEffect = VibrationEffect.createWaveform(vibrationPattern, 0)
                vibrator?.vibrate(vibrationEffect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(vibrationPattern, 0)
            }
        } catch (e: Exception) {
            Log.e("FallAlarmService", "Error starting vibration", e)
        }
    }

    private fun stopAlarmSystem() {
        Log.d("FallAlarmService", "Stopping alarm system...")

        isAlarming = false

        // Detener sonido
        stopSound()

        // Detener vibración
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e("FallAlarmService", "Error stopping vibration", e)
        }

        // Cancelar notificación
        try {
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            Log.e("FallAlarmService", "Error canceling notification", e)
        }

        // Detener el servicio foreground
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        Log.d("FallAlarmService", "Alarm system stopped completely")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("FallAlarmService", "Service being destroyed")
        stopAlarmSystem()
    }
}