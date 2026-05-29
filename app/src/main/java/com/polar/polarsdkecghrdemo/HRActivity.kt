package com.polar.polarsdkecghrdemo

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.androidplot.xy.BoundaryMode
import com.androidplot.xy.StepMode
import com.androidplot.xy.XYGraphWidget
import com.androidplot.xy.XYPlot
import kotlinx.coroutines.launch
import java.text.DecimalFormat

class HRActivity : AppCompatActivity(), PlotterListener {
    companion object {
        private const val TAG = "HRActivity"
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

    private lateinit var deviceId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hr)
        deviceId = intent.getStringExtra("id") ?: throw Exception("HRActivity couldn't be created, no deviceId given")
        
        textViewHR = findViewById(R.id.hr_view_hr)
        textViewRR = findViewById(R.id.hr_view_rr)
        textViewDeviceId = findViewById(R.id.hr_view_deviceId)
        textViewBattery = findViewById(R.id.hr_view_battery_level)
        textViewFwVersion = findViewById(R.id.hr_view_fw_version)
        plot = findViewById(R.id.hr_view_plot)

        textViewDeviceId.text = "ID: $deviceId"

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

        viewModel.connect(deviceId)
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
