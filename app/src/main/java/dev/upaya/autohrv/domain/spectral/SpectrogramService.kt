package dev.upaya.autohrv.domain.spectral

import dev.upaya.autohrv.data.hrv.HrvRepository
import dev.upaya.autohrv.di.ApplicationScope
import dev.upaya.autohrv.domain.signal.frequencyBinsHzIn
import dev.upaya.autohrv.domain.spectral.model.SpectrogramBandInfoBO
import dev.upaya.autohrv.domain.spectral.model.SpectrogramSliceBO
import dev.upaya.autohrv.domain.spectral.usecase.ComputeSpectrogramUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpectrogramService
    @Inject
    internal constructor(
        @param:ApplicationScope private val scope: CoroutineScope,
        computeSpectrogramUseCase: ComputeSpectrogramUseCase,
        hrvRepository: HrvRepository,
    ) {
        private val config = SpectrogramConfig.DEFAULT

        private val maxWindowSeconds: Int = config.bands.maxOf { it.windowSeconds }

        /** Smallest band window — the history needed before the first (fastest) band appears. */
        val firstBandWindowSeconds: Int = config.bands.minOf { it.windowSeconds }

        /** Static per-band info, index-aligned with [bandSlices]. */
        val bands: List<SpectrogramBandInfoBO> =
            config.bands.map { band ->
                SpectrogramBandInfoBO(
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
        val historySeconds: Flow<Int> = rrsMsHistory.map { it.size }.distinctUntilChanged()

        /** Rolling slices per band, index-aligned with [bands]. */
        val bandSlices: StateFlow<List<List<SpectrogramSliceBO>>> =
            combine(
                config.bands.map { band ->
                    computeSpectrogramUseCase(rrsMsHistory, band)
                },
            ) { perBand -> perBand.toList() }
                .stateIn(scope, SharingStarted.Eagerly, config.bands.map { emptyList() })
    }
