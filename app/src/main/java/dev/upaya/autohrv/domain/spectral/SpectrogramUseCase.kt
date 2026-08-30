package dev.upaya.autohrv.domain.spectral

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.scan
import javax.inject.Inject

/** Sample rate of the RR history this use case consumes — the repository's uniform 1 Hz grid. */
private const val SAMPLE_RATE_HZ = 1.0

internal class SpectrogramUseCase
    @Inject
    constructor() {

        /**
         * @param rrHistory1Hz RR intervals on the uniform 1 Hz grid, windowed to at least
         *   [SpectrogramConfig.windowSeconds] (see
         *   [dev.upaya.autohrv.data.repository.HrvRepository.getRrsMs1HzHistory]).
         * @param config supplies the window/hop/display-range tuning.
         * @return a rolling list of the most recent [SpectrogramConfig.maxSlices] slices, one new
         *   slice appended every [SpectrogramConfig.hopSeconds] once the window has filled.
         */
        @OptIn(FlowPreview::class)
        operator fun invoke(
            rrHistory1Hz: Flow<List<Int>>,
            config: SpectrogramConfig,
        ): Flow<List<SpectrogramSlice>> {
            val displayedIndices = frequencyBinIndicesIn(config.windowSeconds, SAMPLE_RATE_HZ, config.displayFreqRangeHz)
            return rrHistory1Hz
                .filter { it.size == config.windowSeconds }
                .sample(config.hopSeconds * 1000L)
                .map { window ->
                    val power = powerSpectrum(window, SAMPLE_RATE_HZ)
                    SpectrogramSlice(
                        timestampMillis = System.currentTimeMillis(),
                        powerByFreqBin = displayedIndices.map { power[it] },
                    )
                }.scan(emptyList<SpectrogramSlice>()) { acc, slice -> (acc + slice).takeLast(config.maxSlices) }
        }
    }
