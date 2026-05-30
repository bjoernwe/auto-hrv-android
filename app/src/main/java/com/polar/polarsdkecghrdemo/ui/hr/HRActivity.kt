package com.polar.polarsdkecghrdemo.ui.hr

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.polar.polarsdkecghrdemo.domain.bluetooth.GetBluetoothPermissionUseCase
import com.polar.polarsdkecghrdemo.ui.breathing.BreathingPacerViewModel
import com.polar.polarsdkecghrdemo.ui.theme.AutoHrvTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HRActivity : ComponentActivity() {

    private val hrViewModel: PolarViewModel by viewModels()
    private val breathingViewModel: BreathingPacerViewModel by viewModels()

    private val bluetoothPermissionHelper = GetBluetoothPermissionUseCase(
        activity = this,
        onGranted = { hrViewModel.connect() },
        onDenied = {
            Toast.makeText(applicationContext, "Needed permissions are missing", Toast.LENGTH_LONG).show()
        },
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AutoHrvTheme {
                HRScreen(
                    hrViewModel = hrViewModel,
                    breathingViewModel = breathingViewModel,
                )
            }
        }
        bluetoothPermissionHelper.checkAndRequest()
    }
}
