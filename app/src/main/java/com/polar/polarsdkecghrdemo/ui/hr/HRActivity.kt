package com.polar.polarsdkecghrdemo.ui.hr

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.polar.polarsdkecghrdemo.PolarApplication
import com.polar.polarsdkecghrdemo.ui.theme.AutoHrvTheme

class HRActivity : ComponentActivity() {
    companion object {
        private const val TAG = "HRActivity"
    }

    private val viewModel: PolarViewModel by viewModels {
        PolarViewModel.Factory((application as PolarApplication).repository)
    }

    private val bluetoothOnLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) {
                Log.w(TAG, "Bluetooth off")
            }
        }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            if (grants.values.all { it }) {
                Log.d(TAG, "Needed permissions are granted")
                viewModel.connect()
            } else {
                Log.w(TAG, "Needed permissions are missing")
                Toast.makeText(applicationContext, "Needed permissions are missing", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AutoHrvTheme {
                HRScreen(viewModel = viewModel)
            }
        }
        checkBT()
    }

    private fun checkBT() {
        val btManager = applicationContext.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter: BluetoothAdapter? = btManager.adapter
        if (bluetoothAdapter == null) {
            Toast.makeText(applicationContext, "Device doesn't support Bluetooth", Toast.LENGTH_LONG).show()
            return
        }
        if (!bluetoothAdapter.isEnabled) {
            bluetoothOnLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }

        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (permissions.all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }) {
            viewModel.connect()
        } else {
            permissionLauncher.launch(permissions)
        }
    }
}