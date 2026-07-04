package dev.upaya.autohrv.domain.breathing

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AcfHistogramTest {

    private val expGain = BreathingConfig.DEFAULT.acfHistogramExpGain
    private val steepness = BreathingConfig.DEFAULT.acfHistogramSigmoidSteepness
    private val midpoint = BreathingConfig.DEFAULT.acfHistogramSigmoidMidpoint

    private fun shape(sums: List<Float>) = shapeAcfHistogram(sums, expGain, steepness, midpoint)

    // --- accumulateAcf ---

    @Test
    fun `empty accumulator starts from the first emission`() {
        assertEquals(listOf(1f, 2f, 3f), accumulateAcf(emptyList(), listOf(1f, 2f, 3f)))
    }

    @Test
    fun `sums element-wise across emissions`() {
        var acc = accumulateAcf(emptyList(), listOf(1f, 2f, 3f))
        acc = accumulateAcf(acc, listOf(0.5f, -1f, 4f))
        acc = accumulateAcf(acc, listOf(-1.5f, 3f, 0f))
        assertEquals(listOf(0f, 4f, 7f), acc)
    }

    @Test
    fun `size mismatch restarts the sum`() {
        val acc = accumulateAcf(listOf(1f, 2f, 3f), listOf(9f, 9f))
        assertEquals(listOf(9f, 9f), acc)
    }

    // --- shapeAcfHistogram ---

    @Test
    fun `empty shapes to empty`() {
        assertTrue(shape(emptyList()).isEmpty())
    }

    @Test
    fun `all outputs stay within zero and one`() {
        val out = shape(listOf(-3f, 0.2f, 5f, 1f, -1f))
        out.forEach { assertTrue("out of bounds: $it", it in 0f..1f) }
    }

    @Test
    fun `shaping is order preserving`() {
        // Every stage (min-max, exp, sigmoid) is monotone increasing, so ranking is preserved.
        val sums = listOf(0.1f, 3f, -2f, 1.5f)
        val out = shape(sums)
        val byInput = sums.indices.sortedBy { sums[it] }
        val byOutput = out.indices.sortedBy { out[it] }
        assertEquals(byInput, byOutput)
    }

    @Test
    fun `constant sums shape to a constant`() {
        val out = shape(listOf(2f, 2f, 2f))
        out.forEach { assertEquals(out.first(), it, 1e-6f) }
    }

    @Test
    fun `secondary peak survives the exponential`() {
        // A dominant lag plus a clear secondary; the sigmoid should keep the secondary well above
        // the noise floor rather than letting exp collapse it toward zero.
        val out = shape(listOf(1.0f, 0.05f, 0.6f, 0.1f, 0.0f))
        val secondary = out[2]
        val floor = out[4] // the lowest bin sets the noise floor
        assertTrue("secondary should stay clear of the noise floor: $secondary vs $floor", secondary > floor * 2f)
        assertTrue("secondary should exceed the low bins", secondary > out[1] && secondary > out[3])
        assertTrue("primary should still be the tallest", out[0] > secondary)
    }

    // --- accumulatedAcfHistogram flow ---

    @Test
    fun `flow accumulates distinct emissions`() =
        runTest {
            val results =
                flowOf<List<Float>?>(
                    listOf(1f, 0f, 0f),
                    listOf(0f, 1f, 0f),
                    listOf(0f, 0f, 1f),
                ).accumulatedAcfHistogram(expGain, steepness, midpoint).toList()

            // scan seeds with the empty accumulator, then one emission per input.
            assertEquals(4, results.size)
            assertTrue(results.first().isEmpty())
            // After three distinct unit vectors the running sum is [1,1,1] → constant histogram.
            val last = results.last()
            assertEquals(3, last.size)
            last.forEach { assertEquals(last.first(), it, 1e-6f) }
        }

    @Test
    fun `flow deduplicates repeated identical acfs`() =
        runTest {
            val repeated =
                flowOf<List<Float>?>(
                    listOf(1f, 0f, 0f),
                    listOf(1f, 0f, 0f),
                    listOf(1f, 0f, 0f),
                ).accumulatedAcfHistogram(expGain, steepness, midpoint).toList()

            val single =
                flowOf<List<Float>?>(
                    listOf(1f, 0f, 0f),
                ).accumulatedAcfHistogram(expGain, steepness, midpoint).toList()

            // Duplicates collapse via distinctUntilChanged: same accumulated result as a single emit.
            assertEquals(single.last(), repeated.last())
        }

    @Test
    fun `flow ignores leading nulls`() =
        runTest {
            val results =
                flowOf(
                    null,
                    null,
                    listOf(2f, 1f, 0f),
                ).accumulatedAcfHistogram(expGain, steepness, midpoint).toList()

            // Only the initial empty seed and the single real emission survive.
            assertEquals(2, results.size)
            assertEquals(shape(listOf(2f, 1f, 0f)), results.last())
        }
}
