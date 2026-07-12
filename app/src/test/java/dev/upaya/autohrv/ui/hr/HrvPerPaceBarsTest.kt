package dev.upaya.autohrv.ui.hr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HrvPerPaceBarsTest {

    @Test
    fun `normalize divides by the max`() {
        val out = normalizeHrvPerPace(listOf(0f, 25f, 50f, 100f))
        assertEquals(listOf(0f, 0.25f, 0.5f, 1f), out)
    }

    @Test
    fun `unsampled bars fall back to the baseline and scale with the rest`() {
        // baseline 1f fills the nulls, then everything divides by the max of 50.
        val out = normalizeHrvPerPace(listOf(null, 50f, null))
        assertEquals(listOf(0.02f, 1f, 0.02f), out)
    }

    @Test
    fun `all unsampled stays at the baseline`() {
        val out = normalizeHrvPerPace(listOf(null, null, null))
        assertEquals(listOf(1f, 1f, 1f), out)
    }

    @Test
    fun `all zeros stays all zeros`() {
        val out = normalizeHrvPerPace(listOf(0f, 0f, 0f))
        assertEquals(listOf(0f, 0f, 0f), out)
    }

    @Test
    fun `single nonzero value normalizes to one`() {
        val out = normalizeHrvPerPace(listOf(0f, 42f, 0f))
        assertEquals(listOf(0f, 1f, 0f), out)
    }

    @Test
    fun `empty stays empty`() {
        assertTrue(normalizeHrvPerPace(emptyList()).isEmpty())
    }
}