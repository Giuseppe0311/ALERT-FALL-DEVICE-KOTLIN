package com.example.cuidadoabuelitonative

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.example.cuidadoabuelitonative.components.AppLayout
import com.example.cuidadoabuelitonative.navigation.NavGraph
import com.example.cuidadoabuelitonative.ui.theme.CuidadoAbuelitoNativeTheme

class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            finishAffinity()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // 1) Crea el canal TAN PRONTO arranque la app (API ≥26)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "FALL_ALARM_CHANNEL",
            "Alarmas de Caídas",
            NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de caídas detectadas"
                enableVibration(true)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        // 2) Pide el permiso en Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPerm = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPerm) {
                requestNotificationPermissionLauncher
                    .launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // 3) Tu UI Compose
        setContent {
            CuidadoAbuelitoNativeTheme {
                val navController = rememberNavController()
                AppLayout {
                    NavGraph(navController = navController)
                }
            }
        }
    }
}

