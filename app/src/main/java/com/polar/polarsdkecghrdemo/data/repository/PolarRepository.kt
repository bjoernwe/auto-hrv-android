package com.polar.polarsdkecghrdemo.data.repository

import android.content.Context
import android.util.Log
import com.polar.polarsdkecghrdemo.data.model.ConnectionState
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.model.PolarDeviceInfo
import com.polar.sdk.api.model.PolarHrData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import java.util.UUID

class PolarRepository(private val context: Context) {
    companion object {
        private const val TAG = "PolarRepository"
        const val DEVICE_ID = "E7A9AB27"
    }

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _batteryLevel = MutableStateFlow<Int?>(null)
    val batteryLevel: StateFlow<Int?> = _batteryLevel.asStateFlow()

    private val _firmwareVersion = MutableStateFlow<String?>(null)
    val firmwareVersion: StateFlow<String?> = _firmwareVersion.asStateFlow()

    private val _readyFeatures = MutableStateFlow<Set<PolarBleApi.PolarBleSdkFeature>>(emptySet())
    val readyFeatures: StateFlow<Set<PolarBleApi.PolarBleSdkFeature>> = _readyFeatures.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val hrFlow: Flow<PolarHrData.PolarHrSample> = readyFeatures.flatMapLatest { features ->
        if (features.contains(PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING)) {
            _isStreaming.value = true
            createHrStream(DEVICE_ID)
        } else {
            _isStreaming.value = false
            flowOf()
        }
    }

    val simpleHr: Flow<Int> = hrFlow.map { it.hr }

    private val api: PolarBleApi by lazy {
        PolarBleApiDefaultImpl.defaultImplementation(
            context,
            setOf(
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_BATTERY_INFO,
                PolarBleApi.PolarBleSdkFeature.FEATURE_DEVICE_INFO
            )
        ).apply {
            setApiLogger { str: String -> Log.d("SDK", str) }
            setApiCallback(object : PolarBleApiCallback() {
                override fun blePowerStateChanged(powered: Boolean) {
                    Log.d(TAG, "BluetoothStateChanged $powered")
                }

                override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                    Log.d(TAG, "Device connected ${polarDeviceInfo.deviceId}")
                    _connectionState.value = ConnectionState.Connected(polarDeviceInfo)
                }

                override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
                    Log.d(TAG, "Device connecting ${polarDeviceInfo.deviceId}")
                    _connectionState.value = ConnectionState.Connecting(polarDeviceInfo.deviceId)
                }

                override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                    Log.d(TAG, "Device disconnected ${polarDeviceInfo.deviceId}")
                    _connectionState.value = ConnectionState.Disconnected(polarDeviceInfo.deviceId)
                }

                override fun bleSdkFeatureReady(identifier: String, feature: PolarBleApi.PolarBleSdkFeature) {
                    Log.d(TAG, "feature ready $feature")
                    _readyFeatures.value = _readyFeatures.value + feature
                }

                override fun disInformationReceived(identifier: String, uuid: UUID, value: String) {
                    if (uuid == UUID.fromString("00002a28-0000-1000-8000-00805f9b34fb")) {
                        _firmwareVersion.value = value.trim()
                    }
                }

                override fun batteryLevelReceived(identifier: String, level: Int) {
                    _batteryLevel.value = level
                }
            })
        }
    }

    fun connect() {
        _readyFeatures.value = emptySet()
        try {
            api.connectToDevice(DEVICE_ID)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to device $DEVICE_ID", e)
        }
    }

    fun disconnect() {
        try {
            api.disconnectFromDevice(DEVICE_ID)
            _isStreaming.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disconnect from device $DEVICE_ID", e)
        }
    }

    fun getHrHistory(length: Int): Flow<List<Int>> {
        return hrFlow.scan<PolarHrData.PolarHrSample, List<Int>>(emptyList()) { acc, sample ->
            acc + sample.hr
        }.map { samples ->
            samples.takeLast(length)
        }
    }

    fun createHrStream(deviceId: String): Flow<PolarHrData.PolarHrSample> = callbackFlow {
        val disposable = api.startHrStreaming(deviceId)
            .subscribe(
                { hrData ->
                    for (hrDat in hrData.samples) {
                        trySend(hrDat)
                    }
                },
                { error -> close(error) },
                { close() }
            )
        awaitClose { disposable.dispose() }
    }

    fun shutDown() {
        api.shutDown()
    }
}
