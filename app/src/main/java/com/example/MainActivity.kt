package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppThemeMode
import com.example.domain.ExternalAiLauncher
import com.example.ui.FolioPrepApp
import com.example.ui.theme.FolioPrepTheme
import com.example.viewmodel.FolioPrepViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: FolioPrepViewModel by viewModels {
        FolioPrepViewModel.factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
            val darkTheme = when (appSettings.themeMode) {
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            FolioPrepTheme(darkTheme = darkTheme) {
                FolioPrepApp(viewModel = viewModel)
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) return
        // Android 10+ allows clipboard reads cleanly once the Activity window gains input focus
        val clipboardText = ExternalAiLauncher.readClipboardText(this)
        viewModel.onAppWindowFocused(clipboardText)
    }
}
