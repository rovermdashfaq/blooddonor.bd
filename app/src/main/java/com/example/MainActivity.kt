package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.BloodDonorBdTheme
import com.example.util.ThemeController
import com.example.util.configureAppCheck

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check with launch intent before UI or Firebase operations
        configureAppCheck(this, intent)
        ThemeController.init(this)

        enableEdgeToEdge()
        setContent {
            val themeSettings by ThemeController.settings.collectAsStateWithLifecycle()
            BloodDonorBdTheme(themeSettings = themeSettings) {
                AppNavigation()
            }
        }
    }
}

