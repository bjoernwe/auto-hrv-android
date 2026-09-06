package dev.upaya.autohrv.ui.acf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AcfHistogramShapingTest {

    private fun shape(sums: List<Float>) = shapeAcfHistogram(sums, ignoredLeadingLags = 0)

    @Test
    fun `empty shapes to empty`() {
        assertTrue(shape(emptyList()).isEmpty())
    }

    @Test
    fun `lag 0 is always forced to zero regardless of its input value`() {
        val out = shape(listOf(999f, 1f, 2f, 3f))
        assertEquals(0f, out[0], 1e-6f)
    }

    @Test
    fun `ignored leading lags are capped into range instead of zeroed`() {
        val sums = listOf(0f, 100f, 90f, 0.1f, 0.5f, 1f) // lag 0 placeholder, then lags 1-5
        val out = shapeAcfHistogram(sums, ignoredLeadingLags = 2)
        assertEquals(0f, out[0], 1e-6f)
        // Lags 1-2 are excluded from establishing the range (which is set by lags 3-5) but are
        // capped to its ceiling rather than dropped, so they read as "at least as tall as the
        // real peak" (lag 5) instead of vanishing.
        assertEquals(out[5], out[1], 1e-6f)
        assertEquals(out[5], out[2], 1e-6f)
        assertTrue("capped lags should reach the top", out[1] > 0.9f)
        // Lag 3, the minimum of the establishing range, sets the floor.
        assertTrue("the establishing-range minimum should be clearly lower", out[3] < out[1])
    }

    @Test
    fun `ignoredLeadingLags beyond available lags is clamped to leave a normalization range`() {
        val out = shapeAcfHistogram(listOf(0f, 1f, 2f, 3f), ignoredLeadingLags = 10)
        assertEquals(0f, out[0], 1e-6f)
        // Clamped to leave the last lag as the (degenerate, single-element) range, so the rest
        // collapse to the same neutral shape instead of crashing or vanishing.
        assertEquals(out[1], out[2], 1e-6f)
        assertEquals(out[1], out[3], 1e-6f)
    }

    @Test
    fun `all outputs stay within zero and one`() {
        val out = shape(listOf(-3f, 0.2f, 5f, 1f, -1f))
        out.forEach { assertTrue("out of bounds: $it", it in 0f..1f) }
    }

    @Test
    fun `shaping is order preserving beyond lag 0`() {
        // Every stage (cap, exp, normalize, sigmoid) is monotone increasing, so ranking among the
        // non-lag-0 bins is preserved. Lag 0 is excluded — its output is hardcoded to zero.
        val sums = listOf(0f, 0.1f, 3f, -2f, 1.5f)
        val out = shape(sums)
        val tailSums = sums.drop(1)
        val tailOut = out.drop(1)
        val byInput = tailSums.indices.sortedBy { tailSums[it] }
        val byOutput = tailOut.indices.sortedBy { tailOut[it] }
        assertEquals(byInput, byOutput)
    }

    @Test
    fun `constant sums beyond lag 0 shape to a constant`() {
        val out = shape(listOf(5f, 2f, 2f, 2f))
        assertEquals(0f, out[0], 1e-6f)
        val tail = out.drop(1)
        tail.forEach { assertEquals(tail.first(), it, 1e-6f) }
    }

    @Test
    fun `secondary peak survives the exponential`() {
        // A dominant lag plus a clear secondary; the sigmoid should keep the secondary well above
        // the noise floor rather than letting exp collapse it toward zero. (Index 0 is a lag-0
        // placeholder and is not part of this comparison.)
        val out = shape(listOf(0f, 1.0f, 0.05f, 0.6f, 0.1f, 0.0f))
        val primary = out[1]
        val secondary = out[3]
        val floor = out[5] // the lowest bin sets the noise floor
        assertTrue("secondary should stay clear of the noise floor: $secondary vs $floor", secondary > floor * 2f)
        assertTrue("secondary should exceed the low bins", secondary > out[2] && secondary > out[4])
        assertTrue("primary should still be the tallest", primary > secondary)
    }
}
