package com.polar.polarsdkecghrdemo.ui.hr

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.androidplot.xy.BoundaryMode
import com.androidplot.xy.StepMode
import com.androidplot.xy.XYGraphWidget
import com.androidplot.xy.XYPlot
import com.polar.polarsdkecghrdemo.PolarApplication
import com.polar.polarsdkecghrdemo.R
import com.polar.polarsdkecghrdemo.data.model.ConnectionState

import com.polar.polarsdkecghrdemo.ui.plot.HrAndRrPlotter
import com.polar.polarsdkecghrdemo.ui.plot.PlotterListener
import kotlinx.coroutines.launch
import java.text.DecimalFormat

class HRActivity : AppCompatActivity(), PlotterListener {
    companion object {
        private const val TAG = "HRActivity"
        private const val PERMISSION_REQUEST_CODE = 1
    }

    private val viewModel: PolarViewModel by viewModels {
        PolarViewModel.Factory((application as PolarApplication).repository)
    }

    private lateinit var plotter: HrAndRrPlotter
    private lateinit var textViewHR: TextView
    private lateinit var textViewRR: TextView
    private lateinit var textViewDeviceId: TextView
    private lateinit var textViewBattery: TextView
    private lateinit var textViewFwVersion: TextView
    private lateinit var plot: XYPlot

    private val bluetoothOnActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode != RESULT_OK) {
            Log.w(TAG, "Bluetooth off")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hr)

        textViewHR = findViewById(R.id.hr_view_hr)
        textViewRR = findViewById(R.id.hr_view_rr)
        textViewDeviceId = findViewById(R.id.hr_view_deviceId)
        textViewBattery = findViewById(R.id.hr_view_battery_level)
        textViewFwVersion = findViewById(R.id.hr_view_fw_version)
        plot = findViewById(R.id.hr_view_plot)

        textViewDeviceId.text = "ID: ${viewModel.deviceId}"

        plotter = HrAndRrPlotter()
        plotter.setListener(this)
        plot.addSeries(plotter.hrSeries, plotter.hrFormatter)
        plot.addSeries(plotter.rrSeries, plotter.rrFormatter)
        plot.setRangeBoundaries(50, 100, BoundaryMode.AUTO)
        plot.setDomainBoundaries(0, 360000, BoundaryMode.AUTO)
        plot.setRangeStep(StepMode.INCREMENT_BY_VAL, 10.0)
        plot.setDomainStep(StepMode.INCREMENT_BY_VAL, 60000.0)
        plot.graph.getLineLabelStyle(XYGraphWidget.Edge.LEFT).format = DecimalFormat("#")
        plot.linesPerRangeLabel = 2

        observeViewModel()
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
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            bluetoothOnActivityResultLauncher.launch(enableBtIntent)
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
            requestPermissions(permissions, PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (index in 0..grantResults.lastIndex) {
                if (grantResults[index] == PackageManager.PERMISSION_DENIED) {
                    Log.w(TAG, "Needed permissions are missing")
                    Toast.makeText(applicationContext, "Needed permissions are missing", Toast.LENGTH_LONG).show()
                    return
                }
            }
            Log.d(TAG, "Needed permissions are granted")
            viewModel.connect()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.connectionState.collect { state ->
                        when (state) {
                            is ConnectionState.Connected -> {
                                Toast.makeText(applicationContext, R.string.connected, Toast.LENGTH_SHORT).show()
                            }
                            is ConnectionState.Disconnected -> {
                                Log.d(TAG, "Device disconnected ${state.deviceId}")
                            }
                            is ConnectionState.Connecting -> {
                                Log.d(TAG, "Device connecting ${state.deviceId}")
                            }
                            else -> {}
                        }
                    }
                }

                launch {
                    viewModel.hrSharedFlow.collect { hrData ->
                        for (sample in hrData.samples) {
                            Log.d(TAG, "HR ${sample.hr} RR ${sample.rrsMs}")
                            if (sample.rrsMs.isNotEmpty()) {
                                textViewRR.text = "(${sample.rrsMs.joinToString(separator = "ms, ")}ms)"
                            }
                            textViewHR.text = sample.hr.toString()
                            plotter.addValues(sample)
                        }
                    }
                }

                launch {
                    viewModel.batteryLevel.collect { level ->
                        if (level != null) {
                            textViewBattery.text = "Battery level: $level%"
                        }
                    }
                }

                launch {
                    viewModel.firmwareVersion.collect { version ->
                        if (version != null) {
                            textViewFwVersion.text = "Firmware: $version"
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // We could disconnect here if we want to follow the previous behavior
        // viewModel.disconnect(deviceId)
    }

    override fun update() {
        runOnUiThread { plot.redraw() }
    }
}
