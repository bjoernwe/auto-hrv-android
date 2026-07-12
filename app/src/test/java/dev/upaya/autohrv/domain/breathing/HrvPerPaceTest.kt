package dev.upaya.autohrv.domain.breathing

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class HrvPerPaceTest {

    // --- updateHrvPerPace ---

    @Test
    fun `pace is rounded to the nearest integer index`() {
        val out = updateHrvPerPace(List(5) { 0f }, paceSeconds = 1.6f, hrv = 40f)
        assertEquals(listOf(0f, 0f, 40f, 0f, 0f), out)
    }

    @Test
    fun `out of range pace is clamped into bounds`() {
        val low = updateHrvPerPace(List(5) { 0f }, paceSeconds = -3f, hrv = 40f)
        assertEquals(listOf(40f, 0f, 0f, 0f, 0f), low)

        val high = updateHrvPerPace(List(5) { 0f }, paceSeconds = 99f, hrv = 40f)
        assertEquals(listOf(0f, 0f, 0f, 0f, 40f), high)
    }

    @Test
    fun `other bars are preserved`() {
        val acc = listOf(10f, 20f, 30f)
        val out = updateHrvPerPace(acc, paceSeconds = 1f, hrv = 99f)
        assertEquals(listOf(10f, 99f, 30f), out)
    }

    @Test
    fun `later write at the same pace overwrites the earlier one`() {
        var acc = updateHrvPerPace(List(3) { 0f }, paceSeconds = 1f, hrv = 10f)
        acc = updateHrvPerPace(acc, paceSeconds = 1f, hrv = 50f)
        assertEquals(listOf(0f, 50f, 0f), acc)
    }

    @Test
    fun `first sample replaces the null outright, a later one blends by decay`() {
        // decay 0.5f: an unsampled bar takes the sample as-is; the next sample is a half-half blend.
        var acc = updateHrvPerPace(List<Float?>(3) { null }, paceSeconds = 1f, hrv = 40f, decay = 0.5f)
        assertEquals(listOf(null, 40f, null), acc)
        acc = updateHrvPerPace(acc, paceSeconds = 1f, hrv = 20f, decay = 0.5f)
        assertEquals(listOf(null, 30f, null), acc)
    }

    // --- accumulatedHrvPerPace flow ---

    @Test
    fun `flow records raw samples at their rounded pace, leaving unsampled paces null`() =
        runTest {
            val results =
                flowOf(
                    HrvPaceSample(paceSeconds = 1f, hrv = 20f),
                    HrvPaceSample(paceSeconds = 3f, hrv = 40f),
                ).accumulatedHrvPerPace(size = 5).toList()

            // scan seeds with the all-null accumulator, then one emission per sample.
            assertEquals(3, results.size)
            assertEquals(listOf(null, null, null, null, null), results[0])
            assertEquals(listOf(null, 20f, null, null, null), results[1])
            assertEquals(listOf(null, 20f, null, 40f, null), results[2])
        }

    @Test
    fun `flow overwrites the bar when a later sample lands on the same pace`() =
        runTest {
            val results =
                flowOf(
                    HrvPaceSample(paceSeconds = 2f, hrv = 10f),
                    HrvPaceSample(paceSeconds = 2f, hrv = 50f),
                ).accumulatedHrvPerPace(size = 4).toList()

            assertEquals(listOf(null, null, 50f, null), results.last())
        }
}