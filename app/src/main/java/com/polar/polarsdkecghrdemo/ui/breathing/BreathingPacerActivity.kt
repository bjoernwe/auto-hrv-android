package com.polar.polarsdkecghrdemo.ui.breathing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.polar.polarsdkecghrdemo.ui.theme.AutoHrvTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BreathingPacerActivity : ComponentActivity() {

    private val viewModel: BreathingPacerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AutoHrvTheme {
                BreathingPacerScreen(
                    viewModel = viewModel,
                    onBack = ::finish,
                )
            }
        }
    }
}
