package com.polar.polarsdkecghrdemo.ui.hr

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.polar.polarsdkecghrdemo.PolarApplication
import com.polar.polarsdkecghrdemo.ui.theme.AutoHrvTheme

class HRActivity : ComponentActivity() {

    private val viewModel: PolarViewModel by viewModels {
        PolarViewModel.Factory((application as PolarApplication).repository)
    }

    private val bluetoothPermissionHelper = BluetoothPermissionHelper(
        activity = this,
        onGranted = { viewModel.connect() },
        onDenied = {
            Toast.makeText(applicationContext, "Needed permissions are missing", Toast.LENGTH_LONG).show()
        },
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AutoHrvTheme {
                HRScreen(viewModel = viewModel)
            }
        }
        bluetoothPermissionHelper.checkAndRequest()
    }
}
