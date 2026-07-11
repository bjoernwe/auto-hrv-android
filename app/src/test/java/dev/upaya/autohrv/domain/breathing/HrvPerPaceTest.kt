package dev.upaya.autohrv.domain.breathing

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HrvPerPaceTest {

    // --- updateHrvPerPace ---

    @Test
    fun `empty accumulator starts from ones of the given size`() {
        val out = updateHrvPerPace(emptyList(), size = 5, paceSeconds = 2f, hrv = 40f)
        assertEquals(listOf(1f, 1f, 40f, 1f, 1f), out)
    }

    @Test
    fun `pace is rounded to the nearest integer index`() {
        val out = updateHrvPerPace(List(5) { 0f }, size = 5, paceSeconds = 1.6f, hrv = 40f)
        assertEquals(listOf(0f, 0f, 40f, 0f, 0f), out)
    }

    @Test
    fun `out of range pace is clamped into bounds`() {
        val low = updateHrvPerPace(List(5) { 0f }, size = 5, paceSeconds = -3f, hrv = 40f)
        assertEquals(listOf(40f, 0f, 0f, 0f, 0f), low)

        val high = updateHrvPerPace(List(5) { 0f }, size = 5, paceSeconds = 99f, hrv = 40f)
        assertEquals(listOf(0f, 0f, 0f, 0f, 40f), high)
    }

    @Test
    fun `other bars are preserved`() {
        val acc = listOf(10f, 20f, 30f)
        val out = updateHrvPerPace(acc, size = 3, paceSeconds = 1f, hrv = 99f)
        assertEquals(listOf(10f, 99f, 30f), out)
    }

    @Test
    fun `later write at the same pace overwrites the earlier one`() {
        var acc = updateHrvPerPace(List(3) { 0f }, size = 3, paceSeconds = 1f, hrv = 10f)
        acc = updateHrvPerPace(acc, size = 3, paceSeconds = 1f, hrv = 50f)
        assertEquals(listOf(0f, 50f, 0f), acc)
    }

    @Test
    fun `size mismatch restarts from ones`() {
        val acc = updateHrvPerPace(listOf(1f, 2f, 3f), size = 2, paceSeconds = 1f, hrv = 9f)
        assertEquals(listOf(1f, 9f), acc)
    }

    // --- normalizeHrvPerPace ---

    @Test
    fun `normalize divides by the max`() {
        val out = normalizeHrvPerPace(listOf(0f, 25f, 50f, 100f))
        assertEquals(listOf(0f, 0.25f, 0.5f, 1f), out)
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

    // --- accumulatedHrvPerPace flow ---

    @Test
    fun `flow records samples at their rounded pace and normalizes relative to the max`() =
        runTest {
            val results =
                flowOf(
                    HrvPaceSample(paceSeconds = 1f, hrv = 20f),
                    HrvPaceSample(paceSeconds = 3f, hrv = 40f),
                ).accumulatedHrvPerPace(size = 5).toList()

            // scan seeds with the all-ones accumulator, then one emission per sample.
            assertEquals(3, results.size)
            assertEquals(listOf(1f, 1f, 1f, 1f, 1f), results[0])
            assertEquals(listOf(0.05f, 1f, 0.05f, 0.05f, 0.05f), results[1])
            assertEquals(listOf(0.025f, 0.5f, 0.025f, 1f, 0.025f), results[2])
        }

    @Test
    fun `flow overwrites the bar when a later sample lands on the same pace`() =
        runTest {
            val results =
                flowOf(
                    HrvPaceSample(paceSeconds = 2f, hrv = 10f),
                    HrvPaceSample(paceSeconds = 2f, hrv = 50f),
                ).accumulatedHrvPerPace(size = 4).toList()

            assertEquals(listOf(0.02f, 0.02f, 1f, 0.02f), results.last())
        }
}
