package dev.upaya.autohrv.domain.breathing.usecase

import dev.upaya.autohrv.domain.breathing.BreathingConfig
import dev.upaya.autohrv.domain.breathing.model.AcfPcaBO
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sign

class ComputeAcfPcaUseCaseTest {

    private val config =
        BreathingConfig.DEFAULT.copy(
            acfMaxLagSeconds = 4,
            acfWindowSeconds = 30,
            maxCycleLengthRange = 3..4,
            acfPcaHalfLifeSeconds = null,
            acfPcaComponentCount = 2,
            acfPcaMinObservations = 5,
        )

    private val accumulate = AccumulateAcfMomentsUseCase()
    private val useCase = ComputeAcfPcaUseCase()

    /** An ACF over lags 0..4 whose lag 0 is the usual 1.0. */
    private fun acf(vararg lags: Float) = listOf(1f) + lags.toList()

    /**
     * Curves in which lags 1-2 rise and fall together while lags 3-4 do the opposite, driven by a
     * single alternating factor — so the leading component must load on both pairs with opposing
     * signs.
     */
    private fun opposingCurves(count: Int): List<List<Float>> =
        (0 until count).map { i ->
            val a = if (i % 2 == 0) 0.8f else 0.2f
            acf(a, a, 1f - a, 1f - a)
        }

    private suspend fun pcaFor(curves: List<List<Float>?>): List<AcfPcaBO?> = useCase(accumulate(curves.asFlow(), config), config).toList()

    @Test
    fun `emits null until the minimum number of observations is reached`() =
        runTest {
            val results = pcaFor(opposingCurves(config.acfPcaMinObservations))

            // scan seeds with null, then one result per accumulated curve. Only the last curve
            // reaches the observation threshold.
            assertTrue("early results should be null", results.dropLast(1).all { it == null })
            assertNotNull(results.last())
        }

    @Test
    fun `a constant curve has no variance so no components are produced`() =
        runTest {
            // distinctUntilChanged collapses identical curves, so vary a lag outside the analysis.
            val flat = List(20) { acf(0.5f, 0.5f, 0.5f, 0.5f) }
            assertTrue(pcaFor(flat).all { it == null })
        }

    @Test
    fun `the leading component separates the two opposing groups of lags`() =
        runTest {
            val pca = pcaFor(opposingCurves(20)).last()
            assertNotNull(pca)
            requireNotNull(pca)

            assertEquals(1, pca.firstLag)
            assertEquals(2, pca.components.size)

            val loadings = pca.components.first().loadings
            assertEquals(4, loadings.size)
            // Lags 1-2 co-vary, lags 3-4 co-vary, and the two pairs oppose each other.
            assertEquals(loadings[0], loadings[1], 1e-4f)
            assertEquals(loadings[2], loadings[3], 1e-4f)
            assertEquals(-sign(loadings[0]), sign(loadings[2]), 1e-6f)
            // A single driving factor: the first component should carry essentially all variance.
            assertTrue(
                "PC1 should dominate: ${pca.components.first().explainedVarianceRatio}",
                pca.components.first().explainedVarianceRatio > 0.99f,
            )
        }

    @Test
    fun `the score locates the current curve along the component in units it can be standardized by`() =
        runTest {
            val results = pcaFor(opposingCurves(20)).filterNotNull()
            val standardized =
                results.map { pca ->
                    val component = pca.components.first()
                    component.score / component.standardDeviation
                }

            // The generator alternates between the two extremes of the single factor, so the
            // standardized score alternates sign every second.
            assertTrue("scores should alternate: $standardized", standardized.zipWithNext().all { (a, b) -> a * b < 0f })
            // Every curve sits at one extreme, so once the two extremes are equally represented
            // (an even number of curves) the score is exactly one standard deviation out.
            assertEquals(1f, abs(standardized.last()), 1e-4f)
        }

    @Test
    fun `loadings keep their sign across consecutive emissions`() =
        runTest {
            // Same factor, but with an extra lag whose loading is a near-tie with the others, so a
            // fresh decomposition could easily canonicalize to the opposite sign each second.
            val curves =
                (0 until 30).map { i ->
                    val a = if (i % 2 == 0) 0.8f else 0.2f
                    acf(a, a, a + 1e-4f, 1f - a)
                }
            val loadings = pcaFor(curves).filterNotNull().map { it.components.first().loadings }

            assertTrue("expected several emissions", loadings.size > 5)
            loadings.zipWithNext().forEach { (previous, current) ->
                val dot = previous.indices.sumOf { (previous[it] * current[it]).toDouble() }
                assertTrue("consecutive loadings flipped: $previous vs $current", dot > 0.0)
            }
        }

    @Test
    fun `a curve of the wrong length restarts the accumulation so no result is produced`() =
        runTest {
            val curves = opposingCurves(20) + listOf(listOf(1f, 0.5f))
            assertNull(pcaFor(curves).last())
        }
}
