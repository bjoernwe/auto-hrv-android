package dev.upaya.autohrv.domain.breathing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class WeightedAutoCorrelationTest {

    /** Sine on a 1 Hz grid with a constant offset to mimic RR intervals in ms. */
    private fun sine(
        count: Int,
        periodSeconds: Double,
        amplitude: Double = 30.0,
        offset: Double = 800.0,
    ) = List(count) { t -> (offset + amplitude * sin(2.0 * PI * t / periodSeconds)).toFloat() }

    private fun argmaxLag(
        acf: List<Float>,
        range: IntRange,
    ) = range.maxByOrNull { acf[it] }!!

    @Test
    fun `peak lands at the sine period`() {
        // Period 11 avoids a harmonic competitor at lag 2*period within the 4..20 search range.
        val values = sine(count = 35, periodSeconds = 11.0)

        val uniform = weightedAutoCorrelation(values, maxLag = 20, halfLifeSeconds = null)
        assertNotNull(uniform)
        assertEquals(11, argmaxLag(uniform!!, 4..20))
        assertTrue("expected strong peak, got ${uniform[11]}", uniform[11] > 0.9f)

        val weighted = weightedAutoCorrelation(values, maxLag = 20, halfLifeSeconds = 12f)
        assertNotNull(weighted)
        assertEquals(11, argmaxLag(weighted!!, 4..20))
        assertTrue(weighted[11] > 0.9f)
    }

    @Test
    fun `lag zero is exactly one`() {
        val values = sine(count = 35, periodSeconds = 9.0)

        val uniform = weightedAutoCorrelation(values, maxLag = 20, halfLifeSeconds = null)!!
        assertEquals(1f, uniform[0], 1e-6f)

        val weighted = weightedAutoCorrelation(values, maxLag = 20, halfLifeSeconds = 12f)!!
        assertEquals(1f, weighted[0], 1e-6f)
    }

    @Test
    fun `values stay within minus one and one`() {
        val rng = Random(42)
        val values = List(40) { (800.0 + rng.nextDouble(-50.0, 50.0)).toFloat() }

        for (halfLife in listOf(null, 8f)) {
            val acf = weightedAutoCorrelation(values, maxLag = 20, halfLifeSeconds = halfLife)!!
            acf.forEachIndexed { lag, r ->
                assertTrue("lag $lag out of bounds: $r", r <= 1f + 1e-6f && r >= -1f - 1e-6f)
            }
        }
    }

    @Test
    fun `constant signal returns null`() {
        val values = List(35) { 800f }
        assertNull(weightedAutoCorrelation(values, maxLag = 20, halfLifeSeconds = null))
        assertNull(weightedAutoCorrelation(values, maxLag = 20, halfLifeSeconds = 12f))
    }

    @Test
    fun `too little data returns null`() {
        // n = 25 < maxLag (20) + MIN_OVERLAP (8) = 28
        val values = sine(count = 25, periodSeconds = 9.0)
        assertNull(weightedAutoCorrelation(values, maxLag = 20, halfLifeSeconds = null))
    }

    @Test
    fun `recency weighting tracks the newer frequency`() {
        // First 20 s at period 17, then 20 s at period 11. Neither period has a 2x harmonic
        // inside the 4..20 search range, so the peak location is an unambiguous frequency readout.
        val values = sine(count = 20, periodSeconds = 17.0) + sine(count = 20, periodSeconds = 11.0)

        val uniform = weightedAutoCorrelation(values, maxLag = 20, halfLifeSeconds = null)!!
        val weighted = weightedAutoCorrelation(values, maxLag = 20, halfLifeSeconds = 5f)!!

        // Recency weighting emphasizes the recent period-11 segment.
        assertTrue(
            "weighted[11]=${weighted[11]} should exceed uniform[11]=${uniform[11]}",
            weighted[11] > uniform[11],
        )
        assertTrue("weighted peak ${argmaxLag(weighted, 4..20)} not near 11", argmaxLag(weighted, 4..20) in 10..12)
    }
}
