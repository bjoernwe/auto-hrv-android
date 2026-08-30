package dev.upaya.autohrv.domain.spectral

import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SpectrogramConfigTest {

    @Test
    fun `band window must be a power of two`() {
        assertThrows(IllegalArgumentException::class.java) {
            SpectrogramBand(label = "X", windowSeconds = 100, hopSeconds = 5, maxSlices = 10, freqRangeHz = 0f..0.1f)
        }
    }

    @Test
    fun `default bands each resolve into multiple in-range bins`() {
        SpectrogramConfig.DEFAULT.bands.forEach { band ->
            val bins = frequencyBinsHzIn(band.windowSeconds, sampleRateHz = 1.0, band.freqRangeHz)
            assertTrue(
                "band ${band.label} should resolve several bins, got ${bins.size}",
                bins.size >= 2,
            )
        }
    }

    @Test
    fun `each default band window is long enough to resolve its slowest in-range bin`() {
        SpectrogramConfig.DEFAULT.bands.forEach { band ->
            val slowestHz = frequencyBinsHzIn(band.windowSeconds, sampleRateHz = 1.0, band.freqRangeHz).first()
            // The slowest bin an FFT resolves is one full cycle per window (1/windowSeconds Hz), so
            // the band's slowest displayed frequency must sit at or above that floor.
            assertTrue(
                "band ${band.label}: slowest bin ${slowestHz}Hz below the ${band.windowSeconds}s window floor",
                slowestHz >= 1f / band.windowSeconds - 1e-6f,
            )
        }
    }
}
