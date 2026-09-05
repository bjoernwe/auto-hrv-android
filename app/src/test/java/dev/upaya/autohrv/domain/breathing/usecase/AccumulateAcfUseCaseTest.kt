package dev.upaya.autohrv.domain.breathing.usecase

import dev.upaya.autohrv.domain.breathing.BreathingConfig
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccumulateAcfUseCaseTest {

    private val defaultConfig = BreathingConfig.DEFAULT
    private val useCase = AccumulateAcfUseCase()

    // --- accumulate ---

    @Test
    fun `empty accumulator starts from the first emission`() {
        assertEquals(listOf(1f, 2f, 3f), useCase.accumulate(emptyList(), listOf(1f, 2f, 3f), decay = 1f))
    }

    @Test
    fun `sums element-wise across emissions`() {
        var acc = useCase.accumulate(emptyList(), listOf(1f, 2f, 3f), decay = 1f)
        acc = useCase.accumulate(acc, listOf(0.5f, -1f, 4f), decay = 1f)
        acc = useCase.accumulate(acc, listOf(-1.5f, 3f, 0f), decay = 1f)
        assertEquals(listOf(0f, 4f, 7f), acc)
    }

    @Test
    fun `size mismatch restarts the sum`() {
        val acc = useCase.accumulate(listOf(1f, 2f, 3f), listOf(9f, 9f), decay = 1f)
        assertEquals(listOf(9f, 9f), acc)
    }

    @Test
    fun `decay shrinks the prior accumulator before adding`() {
        var acc = useCase.accumulate(emptyList(), listOf(1f, 2f, 3f), decay = 0.5f)
        acc = useCase.accumulate(acc, listOf(1f, 2f, 3f), decay = 0.5f)
        // acc*0.5 + new = [0.5,1,1.5] + [1,2,3]
        assertEquals(listOf(1.5f, 3f, 4.5f), acc)
    }

    // --- invoke (flow) ---

    @Test
    fun `flow accumulates distinct emissions`() =
        runTest {
            val results =
                useCase(
                    flowOf<List<Float>?>(
                        listOf(1f, 0f, 0f),
                        listOf(0f, 1f, 0f),
                        listOf(0f, 0f, 1f),
                    ),
                    defaultConfig.copy(acfHistogramHalfLifeSeconds = null),
                ).toList()

            // scan seeds with the empty accumulator, then one emission per input.
            assertEquals(4, results.size)
            assertTrue(results.first().isEmpty())
            assertEquals(listOf(1f, 1f, 1f), results.last())
        }

    @Test
    fun `flow deduplicates repeated identical acfs`() =
        runTest {
            val repeated =
                useCase(
                    flowOf<List<Float>?>(
                        listOf(1f, 0f, 0f),
                        listOf(1f, 0f, 0f),
                        listOf(1f, 0f, 0f),
                    ),
                    defaultConfig.copy(acfHistogramHalfLifeSeconds = null),
                ).toList()

            val single =
                useCase(
                    flowOf<List<Float>?>(
                        listOf(1f, 0f, 0f),
                    ),
                    defaultConfig.copy(acfHistogramHalfLifeSeconds = null),
                ).toList()

            // Duplicates collapse via distinctUntilChanged: same accumulated result as a single emit.
            assertEquals(single.last(), repeated.last())
        }

    @Test
    fun `flow ignores leading nulls`() =
        runTest {
            val results =
                useCase(
                    flowOf(
                        null,
                        null,
                        listOf(2f, 1f, 0f),
                    ),
                    defaultConfig.copy(acfHistogramHalfLifeSeconds = null),
                ).toList()

            // Only the initial empty seed and the single real emission survive.
            assertEquals(2, results.size)
            assertEquals(listOf(2f, 1f, 0f), results.last())
        }

    @Test
    fun `finite half-life de-weights older emissions`() =
        runTest {
            // Lag 1 accumulates repeatedly early, lag 2 spikes once at the end; with decay the
            // recent lag-2 spike should outweigh the decayed history at lag 1.
            val results =
                useCase(
                    flowOf<List<Float>?>(
                        listOf(0f, 1f, 0f),
                        listOf(0f, 1f, 0f),
                        listOf(0f, 1f, 0f),
                        listOf(0f, 0f, 1f),
                    ),
                    defaultConfig.copy(acfHistogramHalfLifeSeconds = 1f),
                ).toList()

            val last = results.last()
            assertTrue("recent lag should outshine the decayed history: $last", last[2] > last[1])
        }
}
