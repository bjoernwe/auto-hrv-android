package dev.upaya.autohrv.domain.signal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class PcaTest {

    private fun diagonal(vararg variances: Double): List<List<Double>> =
        variances.indices.map { i -> variances.indices.map { j -> if (i == j) variances[i] else 0.0 } }

    @Test
    fun `empty covariance yields an empty basis`() {
        val basis = principalComponents(emptyList(), count = 3)
        assertTrue(basis.components.isEmpty())
        assertEquals(0f, basis.totalVariance, 1e-6f)
    }

    @Test
    fun `non-square covariance yields an empty basis`() {
        val basis = principalComponents(listOf(listOf(1.0, 0.0), listOf(0.0)), count = 2)
        assertTrue(basis.components.isEmpty())
    }

    @Test
    fun `zero covariance yields an empty basis`() {
        assertTrue(principalComponents(diagonal(0.0, 0.0, 0.0), count = 3).components.isEmpty())
    }

    @Test
    fun `diagonal covariance yields axis-aligned components ordered by descending variance`() {
        val basis = principalComponents(diagonal(1.0, 5.0, 3.0), count = 3)

        assertEquals(listOf(5f, 3f, 1f), basis.components.map { it.variance })
        // Each component is the axis of the variance it carries: index 1, then 2, then 0.
        listOf(1, 2, 0).forEachIndexed { rank, axis ->
            val loadings = basis.components[rank].loadings
            assertEquals("component $rank should be axis $axis", 1f, abs(loadings[axis]), 1e-5f)
            loadings.indices.filter { it != axis }.forEach { assertEquals(0f, loadings[it], 1e-5f) }
        }
    }

    @Test
    fun `totalVariance sums all eigenvalues not just the returned ones`() {
        val basis = principalComponents(diagonal(1.0, 5.0, 3.0), count = 1)
        assertEquals(1, basis.components.size)
        assertEquals(9f, basis.totalVariance, 1e-5f)
    }

    @Test
    fun `count larger than the dimension is clamped`() {
        assertEquals(2, principalComponents(diagonal(2.0, 1.0), count = 7).components.size)
    }

    @Test
    fun `rotated covariance recovers its rotation`() {
        // Variances 4 and 1 along axes rotated by 30 degrees: C = R diag(4, 1) Rᵀ.
        val angle = Math.PI / 6
        val c = cos(angle)
        val s = sin(angle)
        val covariance =
            listOf(
                listOf(4 * c * c + 1 * s * s, 4 * c * s - 1 * c * s),
                listOf(4 * c * s - 1 * c * s, 4 * s * s + 1 * c * c),
            )

        val basis = principalComponents(covariance, count = 2)

        assertEquals(4f, basis.components[0].variance, 1e-5f)
        assertEquals(1f, basis.components[1].variance, 1e-5f)
        assertEquals(c.toFloat(), basis.components[0].loadings[0], 1e-5f)
        assertEquals(s.toFloat(), basis.components[0].loadings[1], 1e-5f)
    }

    @Test
    fun `the sign is canonicalized so the dominant loading is positive`() {
        // Both (0.6, 0.8) and its negation are valid eigenvectors; the canonical form is the one
        // whose largest-magnitude entry — here index 1 — is positive.
        val covariance = listOf(listOf(0.36, 0.48), listOf(0.48, 0.64))
        val loadings = principalComponents(covariance, count = 1).components.single().loadings
        assertTrue("dominant loading should be positive: $loadings", loadings[1] > 0f)
        assertEquals(0.6f, loadings[0], 1e-5f)
        assertEquals(0.8f, loadings[1], 1e-5f)
    }
}
