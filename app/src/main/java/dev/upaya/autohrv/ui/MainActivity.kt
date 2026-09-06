package dev.upaya.autohrv.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import dev.upaya.autohrv.ui.permissions.BluetoothPermissionRequester
import dev.upaya.autohrv.ui.theme.AutoHrvTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val getBluetoothPermissions =
        BluetoothPermissionRequester(
            activity = this,
            onGranted = { viewModel.connect() },
            onDenied = {
                Toast.makeText(applicationContext, "Needed Bluetooth permissions are missing", Toast.LENGTH_LONG).show()
            },
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AutoHrvTheme {
                MainScreen(viewModel = viewModel)
            }
        }
        getBluetoothPermissions()
    }
}
