package com.polar.polarsdkecghrdemo.domain.bluetooth

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

class GetBluetoothPermissionUseCase(
    private val activity: ComponentActivity,
    private val onGranted: () -> Unit,
    private val onDenied: () -> Unit,
) {
    private val bluetoothOnLauncher =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) {
                Log.w(TAG, "Bluetooth off")
            }
        }

    private val permissionLauncher =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            if (grants.values.all { it }) {
                Log.d(TAG, "Needed permissions are granted")
                onGranted()
            } else {
                Log.w(TAG, "Needed permissions are missing")
                onDenied()
            }
        }

    fun checkAndRequest() {
        val btManager = activity.getSystemService(BluetoothManager::class.java)
        val adapter = btManager?.adapter
        if (adapter == null) {
            Toast.makeText(activity, "Device doesn't support Bluetooth", Toast.LENGTH_LONG).show()
            return
        }
        if (!adapter.isEnabled) {
            bluetoothOnLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (permissions.all { activity.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }) {
            onGranted()
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    companion object {
        private const val TAG = "BluetoothPermissionHelper"
    }
}
