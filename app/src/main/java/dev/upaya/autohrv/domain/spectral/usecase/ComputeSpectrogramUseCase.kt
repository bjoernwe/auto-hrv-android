package dev.upaya.autohrv.domain.spectral.usecase

import dev.upaya.autohrv.domain.signal.frequencyBinIndicesIn
import dev.upaya.autohrv.domain.signal.powerSpectrum
import dev.upaya.autohrv.domain.spectral.SpectrogramBand
import dev.upaya.autohrv.domain.spectral.model.SpectrogramSliceBO
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.scan
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/** Sample rate of the RR history this use case consumes — the repository's uniform 1 Hz grid. */
private const val SAMPLE_RATE_HZ = 1.0

internal class ComputeSpectrogramUseCase
    @Inject
    constructor() {

        /**
         * @param rrHistory1Hz RR intervals on the uniform 1 Hz grid, holding at least
         *   [SpectrogramBand.windowSeconds] samples once filled (a shared buffer windowed to the
         *   largest band, see [dev.upaya.autohrv.domain.spectral.SpectrogramService]); the trailing
         *   [SpectrogramBand.windowSeconds] are taken per slice.
         * @param band supplies the window/hop/display-range tuning for this band.
         * @return a rolling list of the most recent [SpectrogramBand.maxSlices] slices, one new
         *   slice appended every [SpectrogramBand.hopSeconds] once the window has filled.
         */
        @OptIn(FlowPreview::class)
        operator fun invoke(
            rrHistory1Hz: Flow<List<Int>>,
            band: SpectrogramBand,
        ): Flow<List<SpectrogramSliceBO>> {
            val displayedIndices = frequencyBinIndicesIn(band.windowSeconds, SAMPLE_RATE_HZ, band.freqRangeHz)
            return rrHistory1Hz
                .filter { it.size >= band.windowSeconds }
                // Slice to this band's window only at the hop rate, not on every 1 Hz tick upstream.
                .sample(band.hopSeconds.seconds)
                .map { history ->
                    val power = powerSpectrum(history.takeLast(band.windowSeconds))
                    SpectrogramSliceBO(
                        timestampMillis = System.currentTimeMillis(),
                        powerByFreqBin = displayedIndices.map { power[it] },
                    )
                }.scan(emptyList<SpectrogramSliceBO>()) { acc, slice -> (acc + slice).takeLast(band.maxSlices) }
        }
    }
