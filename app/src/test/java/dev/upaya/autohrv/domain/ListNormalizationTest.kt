package dev.upaya.autohrv.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ListNormalizationTest {

    @Test
    fun `empty stays empty`() {
        assertTrue(emptyList<Float>().normalizeMinMax().isEmpty())
    }

    @Test
    fun `single element maps to midpoint`() {
        assertEquals(listOf(0.5f), listOf(7f).normalizeMinMax())
    }

    @Test
    fun `all equal maps to midpoint`() {
        val out = listOf(3f, 3f, 3f).normalizeMinMax()
        out.forEach { assertEquals(0.5f, it, 1e-6f) }
    }

    @Test
    fun `min maps to zero max to one midpoint to half`() {
        val out = listOf(0f, 5f, 10f).normalizeMinMax()
        assertEquals(0f, out[0], 1e-6f)
        assertEquals(0.5f, out[1], 1e-6f)
        assertEquals(1f, out[2], 1e-6f)
    }

    @Test
    fun `handles negative values`() {
        val out = listOf(-2f, 0f, 2f).normalizeMinMax()
        assertEquals(0f, out[0], 1e-6f)
        assertEquals(0.5f, out[1], 1e-6f)
        assertEquals(1f, out[2], 1e-6f)
    }

    @Test
    fun `preserves order`() {
        val out = listOf(3f, 1f, 4f, 1.5f, 2f).normalizeMinMax()
        assertEquals(0f, out.min(), 1e-6f)
        assertEquals(1f, out.max(), 1e-6f)
        // Monotone: larger input stays larger output.
        assertTrue(out[2] > out[0])
        assertTrue(out[0] > out[4])
        assertTrue(out[4] > out[3])
        assertTrue(out[3] > out[1])
    }

    // --- normalizeMinMax(min, max) ---

    @Test
    fun `external range empty stays empty`() {
        assertTrue(emptyList<Float>().normalizeMinMax(0f, 1f).isEmpty())
    }

    @Test
    fun `external range degenerate maps to midpoint`() {
        val out = listOf(1f, 2f, 3f).normalizeMinMax(5f, 5f)
        out.forEach { assertEquals(0.5f, it, 1e-6f) }
    }

    @Test
    fun `external range scales values inside the range`() {
        val out = listOf(0f, 5f, 10f).normalizeMinMax(0f, 10f)
        assertEquals(0f, out[0], 1e-6f)
        assertEquals(0.5f, out[1], 1e-6f)
        assertEquals(1f, out[2], 1e-6f)
    }

    @Test
    fun `external range caps values outside the range to zero and one`() {
        val out = listOf(-5f, 15f).normalizeMinMax(0f, 10f)
        assertEquals(0f, out[0], 1e-6f)
        assertEquals(1f, out[1], 1e-6f)
    }
}
