package com.example.upsccompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.upsccompanion.data.AppStore
import com.example.upsccompanion.ui.UPSCApp
import com.example.upsccompanion.ui.theme.UPSCCompanionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = AppStore.get(applicationContext)
        setContent {
            UPSCCompanionTheme {
                UPSCApp(store)
            }
        }
    }
}
