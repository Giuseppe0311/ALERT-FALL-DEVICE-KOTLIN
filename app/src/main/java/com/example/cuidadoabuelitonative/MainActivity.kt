package com.example.cuidadoabuelitonative

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.cuidadoabuelitonative.components.AppLayout
import com.example.cuidadoabuelitonative.navigation.NavGraph
import com.example.cuidadoabuelitonative.ui.theme.CuidadoAbuelitoNativeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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

