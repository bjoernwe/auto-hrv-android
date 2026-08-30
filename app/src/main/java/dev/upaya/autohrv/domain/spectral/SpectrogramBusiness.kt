package dev.upaya.autohrv.domain.spectral

import dev.upaya.autohrv.data.repository.HrvRepository
import dev.upaya.autohrv.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpectrogramBusiness
    @Inject
    internal constructor(
        @param:ApplicationScope private val scope: CoroutineScope,
        spectrogramUseCase: SpectrogramUseCase,
        hrvRepository: HrvRepository,
    ) {
        private val config = SpectrogramConfig.DEFAULT

        /** Seconds of 1 Hz RR history the sliding-window FFT needs before the first slice appears. */
        val windowSeconds: Int = config.windowSeconds

        /** Frequency (Hz) of each bin in every [SpectrogramSlice.powerByFreqBin], fixed by [config]. */
        val freqBinsHz: List<Float> =
            frequencyBinsHzIn(config.windowSeconds, sampleRateHz = 1.0, config.displayFreqRangeHz)

        /** Mayer-wave / LF band, for the chart to mark as reference lines. */
        val mayerBandHz: ClosedFloatingPointRange<Float> = config.mayerBandHz

        private val rrsMsHistory: StateFlow<List<Int>> =
            hrvRepository
                .getRrsMs1HzHistory(config.windowSeconds)
                .stateIn(scope, SharingStarted.Eagerly, emptyList())

        // rrsMsHistory holds one sample per second (capped at windowSeconds), so its size doubles
        // as "seconds of history collected so far" — the loading-progress signal for the card.
        val historySeconds: StateFlow<Int> =
            rrsMsHistory
                .map { it.size }
                .stateIn(scope, SharingStarted.Eagerly, 0)

        val slices: StateFlow<List<SpectrogramSlice>> =
            spectrogramUseCase(rrsMsHistory, config)
                .stateIn(scope, SharingStarted.Eagerly, emptyList())
    }
