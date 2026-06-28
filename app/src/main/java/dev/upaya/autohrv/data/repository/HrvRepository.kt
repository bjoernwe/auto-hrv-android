package dev.upaya.autohrv.data.repository

import android.content.Context
import android.util.Log
import dev.upaya.autohrv.data.model.ConnectionState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.model.PolarDeviceInfo
import com.polar.sdk.api.model.PolarHrData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class HrvRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    companion object {
        private const val TAG = "HrvRepository"
        const val DEVICE_ID = "E7A9AB27"
        const val SAMPLES_PER_SECOND = 1
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
    private val hrFlow: Flow<PolarHrData.PolarHrSample> = readyFeatures.flatMapLatest { features ->
        if (features.contains(PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING)) {
            _isStreaming.value = true
            createHrStream()
        } else {
            _isStreaming.value = false
            flowOf()
        }
    }

    /** Beat-indexed HR: one value per sample, exactly as delivered by the sensor. */
    val hrBeatFlow: Flow<Int> = hrFlow.map { it.hr }

    /** Beat-indexed RR intervals (true NN intervals), one value per heartbeat. */
    val rrMsBeatFlow: Flow<Int> = hrFlow.transform { sample -> sample.rrsMs.firstOrNull()?.let { emit(it) } }

    /** HR resampled onto a uniform 1 Hz grid (zero-order hold). */
    val hrResampled1Hz: Flow<Int> = hrBeatFlow.resampledTo1Hz()

    /** RR intervals resampled onto a uniform 1 Hz grid (zero-order hold). */
    val rrMsResampled1Hz: Flow<Int> = rrMsBeatFlow.resampledTo1Hz()

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
                    _readyFeatures.value += feature
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

    /** HR history on the uniform 1 Hz grid, covering the last [seconds] of real time. */
    fun getHrHistory(seconds: Int): Flow<List<Int>> =
        hrResampled1Hz.windowedTo(seconds * SAMPLES_PER_SECOND)

    /** RR-interval history on the uniform 1 Hz grid, covering the last [seconds] of real time. */
    fun getRrsMsHistory(seconds: Int): Flow<List<Int>> =
        rrMsResampled1Hz.windowedTo(seconds * SAMPLES_PER_SECOND)

    /**
     * Beat-indexed RR-interval history (true NN intervals) covering roughly the last [seconds] of
     * real time. Unlike [getRrsMsHistory] no resampling is applied, so each interval appears exactly
     * once — the right basis for beat-to-beat measures such as SDNN.
     */
    fun getRrsMsBeatHistory(seconds: Int): Flow<List<Int>> =
        rrMsBeatFlow.scan(emptyList<Int>()) { acc, rr -> (acc + rr).takeLastWithinMs(seconds * 1000) }

    /** Accumulates a flow into a rolling window of the most recent [maxSamples] values. */
    private fun Flow<Int>.windowedTo(maxSamples: Int): Flow<List<Int>> =
        scan(emptyList<Int>()) { acc, value -> (acc + value).takeLast(maxSamples) }

    /** Keeps the most recent intervals whose cumulative duration is within [windowMs]. */
    private fun List<Int>.takeLastWithinMs(windowMs: Int): List<Int> {
        var sum = 0
        val window = ArrayDeque<Int>()
        for (rr in asReversed()) {
            window.addFirst(rr)
            sum += rr
            if (sum >= windowMs) break
        }
        return window
    }

    /**
     * Resamples an irregular, beat-indexed stream onto a uniform 1 Hz time grid via zero-order hold:
     * a 1 Hz ticker re-emits the most recently observed value. This makes a sample index == seconds,
     * so downstream lag/frequency axes are in real time units. The breathing band (~0.07-0.17 Hz)
     * sits well below the 0.5 Hz Nyquist limit, so 1 Hz suffices.
     */
    private fun <T : Any> Flow<T>.resampledTo1Hz(): Flow<T> = channelFlow {
        val latest = MutableStateFlow<T?>(null)
        launch { collect { latest.value = it } }
        while (isActive) {
            latest.value?.let { send(it) }
            delay((1000L / SAMPLES_PER_SECOND).milliseconds)
        }
    }

    private fun createHrStream(): Flow<PolarHrData.PolarHrSample> = callbackFlow {
        val disposable = api.startHrStreaming(DEVICE_ID)
            .subscribe(
                // Emit every sample so no beats (and their RR intervals) are lost; consumers that
                // want a uniform rate go through resampledTo1Hz instead.
                { hrData -> hrData.samples.lastOrNull()?.let { trySend(it) } },
                { error -> close(error) },
                { close() }
            )
        awaitClose { disposable.dispose() }
    }

    fun shutDown() {
        api.shutDown()
    }
}
