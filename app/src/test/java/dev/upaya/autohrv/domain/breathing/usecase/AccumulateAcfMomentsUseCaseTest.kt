package dev.upaya.autohrv.domain.breathing.usecase

import dev.upaya.autohrv.domain.breathing.BreathingConfig
import dev.upaya.autohrv.domain.breathing.model.AcfMomentsBO
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccumulateAcfMomentsUseCaseTest {

    private val defaultConfig = BreathingConfig.DEFAULT
    private val useCase = AccumulateAcfMomentsUseCase()

    /** An ACF whose lag 0 is the usual 1.0 and whose remaining lags are [lags]. */
    private fun acf(vararg lags: Float) = listOf(1f) + lags.toList()

    // --- accumulate ---

    @Test
    fun `lag 0 is dropped from the moments`() {
        val moments = useCase.accumulate(AcfMomentsBO.EMPTY, acf(2f, 3f), decay = 1f)
        assertEquals(2, moments.dimension)
        assertEquals(listOf(2.0, 3.0), moments.sum)
    }

    @Test
    fun `the first curve seeds weight and observation count`() {
        val moments = useCase.accumulate(AcfMomentsBO.EMPTY, acf(2f, 3f), decay = 1f)
        assertEquals(1, moments.observations)
        assertEquals(1.0, moments.weight, 1e-9)
        // Outer product of (2, 3) with itself.
        assertEquals(listOf(listOf(4.0, 6.0), listOf(6.0, 9.0)), moments.sumOuter)
    }

    @Test
    fun `moments sum element-wise across curves`() {
        var moments = useCase.accumulate(AcfMomentsBO.EMPTY, acf(1f, 0f), decay = 1f)
        moments = useCase.accumulate(moments, acf(0f, 2f), decay = 1f)

        assertEquals(2, moments.observations)
        assertEquals(2.0, moments.weight, 1e-9)
        assertEquals(listOf(1.0, 2.0), moments.sum)
        assertEquals(listOf(listOf(1.0, 0.0), listOf(0.0, 4.0)), moments.sumOuter)
    }

    @Test
    fun `decay shrinks the prior moments before adding`() {
        var moments = useCase.accumulate(AcfMomentsBO.EMPTY, acf(2f, 0f), decay = 0.5f)
        moments = useCase.accumulate(moments, acf(2f, 0f), decay = 0.5f)

        assertEquals(1.5, moments.weight, 1e-9)
        assertEquals(listOf(3.0, 0.0), moments.sum)
        assertEquals(6.0, moments.sumOuter[0][0], 1e-9)
    }

    @Test
    fun `dimension mismatch restarts the moments`() {
        val seeded = useCase.accumulate(AcfMomentsBO.EMPTY, acf(1f, 2f, 3f), decay = 1f)
        val restarted = useCase.accumulate(seeded, acf(5f, 6f), decay = 1f)

        assertEquals(1, restarted.observations)
        assertEquals(1.0, restarted.weight, 1e-9)
        assertEquals(listOf(5.0, 6.0), restarted.sum)
    }

    @Test
    fun `the covariance derived from the moments matches the sample covariance`() {
        // Two lags that move in exact opposition: x = (1, -1) then (-1, 1).
        var moments = useCase.accumulate(AcfMomentsBO.EMPTY, acf(1f, -1f), decay = 1f)
        moments = useCase.accumulate(moments, acf(-1f, 1f), decay = 1f)

        val mean = moments.sum.map { it / moments.weight }
        val cov =
            mean.indices.map { i ->
                mean.indices.map { j -> moments.sumOuter[i][j] / moments.weight - mean[i] * mean[j] }
            }
        assertEquals(listOf(listOf(1.0, -1.0), listOf(-1.0, 1.0)), cov)
    }

    // --- invoke (flow) ---

    @Test
    fun `flow ignores nulls and deduplicates repeated curves`() =
        runTest {
            val moments =
                useCase(
                    flowOf(null, acf(1f, 2f), acf(1f, 2f), acf(3f, 4f)),
                    defaultConfig.copy(acfPcaHalfLifeSeconds = null),
                ).toList()

            // The seed is dropped, the null ignored and the duplicate collapsed: two emissions.
            assertEquals(2, moments.size)
            assertEquals(1, moments.first().observations)
            assertEquals(2, moments.last().observations)
            assertEquals(listOf(4.0, 6.0), moments.last().sum)
        }

    @Test
    fun `a finite half-life de-weights older curves`() =
        runTest {
            val decayed =
                useCase(
                    flowOf(acf(4f, 0f), acf(0f, 1f)),
                    defaultConfig.copy(acfPcaHalfLifeSeconds = 1f),
                ).toList().last()

            // The first curve halves before the second is added, so lag 2's fresh 1 outweighs
            // lag 1's decayed 4 in the second moment relative to a plain sum.
            assertEquals(2.0, decayed.sum[0], 1e-6)
            assertEquals(1.0, decayed.sum[1], 1e-6)
            assertTrue("weight should reflect the decay: ${decayed.weight}", decayed.weight < 2.0)
        }
}
