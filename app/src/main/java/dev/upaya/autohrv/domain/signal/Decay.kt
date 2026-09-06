package dev.upaya.autohrv.domain.signal

import kotlin.math.pow

/**
 * Per-emission decay factor of an exponentially decaying accumulator with the given half-life.
 * Emissions are assumed to arrive on a 1 Hz grid, so an old contribution halves every
 * [halfLifeSeconds] emissions. A `null` half-life means no decay (`1f`) — a plain running sum over
 * the whole session.
 */
internal fun decayPerEmission(halfLifeSeconds: Float?): Float = if (halfLifeSeconds == null) 1f else 0.5f.pow(1f / halfLifeSeconds)
