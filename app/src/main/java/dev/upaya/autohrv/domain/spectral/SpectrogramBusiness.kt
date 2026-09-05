package dev.upaya.autohrv.domain.spectral

import dev.upaya.autohrv.data.hrv.HrvRepository
import dev.upaya.autohrv.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/** Static per-band info the chart needs; the frequency axis is fixed by the band's window/range. */
data class SpectrogramBandInfo(
    val label: String,
    val freqBinsHz: List<Float>,
)

@Singleton
class SpectrogramBusiness
    @Inject
    internal constructor(
        @param:ApplicationScope private val scope: CoroutineScope,
        spectrogramUseCase: SpectrogramUseCase,
        hrvRepository: HrvRepository,
    ) {
        private val config = SpectrogramConfig.DEFAULT

        // Collect the RR history once, windowed to the largest band's window — one repository consumer
        // instead of one per band (see the shared-buffer note in CLAUDE.md). Each band then takes its
        // own trailing window from this buffer; the per-band `filter { size >= windowSeconds }` gate
        // makes each band activate as the buffer fills, so smaller (faster) bands start first.
        private val maxWindowSeconds: Int = config.bands.maxOf { it.windowSeconds }

        /** Smallest band window — the history needed before the first (fastest) band appears. */
        val firstBandWindowSeconds: Int = config.bands.minOf { it.windowSeconds }

        /** Static per-band info, index-aligned with [bandSlices]. */
        val bands: List<SpectrogramBandInfo> =
            config.bands.map { band ->
                SpectrogramBandInfo(
                    label = band.label,
                    freqBinsHz = frequencyBinsHzIn(band.windowSeconds, sampleRateHz = 1.0, band.freqRangeHz),
                )
            }

        private val rrsMsHistory: StateFlow<List<Int>> =
            hrvRepository
                .getRrsMs1HzHistory(maxWindowSeconds)
                .stateIn(scope, SharingStarted.Eagerly, emptyList())

        // rrsMsHistory holds one sample per second (capped at maxWindowSeconds), so its size doubles
        // as "seconds of history collected so far" — the loading-progress signal for the card.
        val historySeconds: StateFlow<Int> =
            rrsMsHistory
                .map { it.size }
                .stateIn(scope, SharingStarted.Eagerly, 0)

        /** Rolling slices per band, index-aligned with [bands]. */
        val bandSlices: StateFlow<List<List<SpectrogramSlice>>> =
            combine(
                config.bands.map { band ->
                    spectrogramUseCase(rrsMsHistory, band)
                },
            ) { perBand -> perBand.toList() }
                .stateIn(scope, SharingStarted.Eagerly, config.bands.map { emptyList() })
    }
