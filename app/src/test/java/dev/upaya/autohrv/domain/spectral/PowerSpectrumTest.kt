package dev.upaya.autohrv.domain.spectral

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class PowerSpectrumTest {

    /** Sine on a 1 Hz grid with a constant offset to mimic RR intervals in ms. */
    private fun sine(
        count: Int,
        freqHz: Double,
        amplitude: Double = 30.0,
        offsetMs: Double = 800.0,
    ) = List(count) { t -> (offsetMs + amplitude * sin(2.0 * PI * freqHz * t)).toInt() }

    @Test
    fun `peak lands at the sine frequency`() {
        val samples = sine(count = 128, freqHz = 0.1)
        val power = powerSpectrum(samples, sampleRateHz = 1.0)
        val bins = frequencyBinsHz(windowSize = 128, sampleRateHz = 1.0)

        val peakIndex = power.indices.maxByOrNull { power[it] }!!
        assertEquals(0.1f, bins[peakIndex], 0.01f)
    }

    @Test
    fun `dc offset does not dominate the spectrum`() {
        val samples = sine(count = 128, freqHz = 0.1, offsetMs = 5000.0)
        val power = powerSpectrum(samples, sampleRateHz = 1.0)

        assertTrue("DC bin should not dominate: power[0]=${power[0]}, max=${power.max()}", power[0] < power.max())
    }

    @Test
    fun `frequency axis spans DC to Nyquist at the expected resolution`() {
        val bins = frequencyBinsHz(windowSize = 128, sampleRateHz = 1.0)

        assertEquals(65, bins.size)
        assertEquals(0f, bins.first(), 1e-6f)
        assertEquals(0.5f, bins.last(), 1e-6f)
        assertEquals(1f / 128f, bins[1] - bins[0], 1e-6f)
    }

    @Test
    fun `mayer band selection resolves into multiple bins`() {
        val indices = frequencyBinIndicesIn(windowSize = 128, sampleRateHz = 1.0, range = 0.04f..0.15f)

        assertTrue("expected several bins across the Mayer band, got ${indices.size}", indices.size >= 10)
    }

    @Test
    fun `DC bin is excluded even when the display range starts at zero`() {
        val indices = frequencyBinIndicesIn(windowSize = 128, sampleRateHz = 1.0, range = 0f..0.2f)
        assertTrue("bin 0 (DC) should never be selected", 0 !in indices)

        // The slowest bin an FFT can resolve is one full cycle per window — its period equals
        // the window length exactly.
        val slowestHz = frequencyBinsHzIn(windowSize = 128, sampleRateHz = 1.0, range = 0f..0.2f).first()
        assertEquals(128f, 1f / slowestHz, 1e-3f)
    }
}
