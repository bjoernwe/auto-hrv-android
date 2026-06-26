package dev.upaya.autohrv.ui.hr

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dev.upaya.autohrv.domain.bluetooth.GetBluetoothPermissionUseCase
import dev.upaya.autohrv.ui.theme.AutoHrvTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HRActivity : ComponentActivity() {

    private val hrViewModel: HrvViewModel by viewModels()

    private val getBluetoothPermissions = GetBluetoothPermissionUseCase(
        activity = this,
        onGranted = { hrViewModel.connect() },
        onDenied = {
            Toast.makeText(applicationContext, "Needed Bluetooth permissions are missing", Toast.LENGTH_LONG).show()
        },
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AutoHrvTheme {
                HRScreen(viewModel = hrViewModel)
            }
        }
        getBluetoothPermissions()
    }
}
