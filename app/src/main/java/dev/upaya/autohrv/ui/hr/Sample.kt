package dev.upaya.autohrv.ui.hr

data class Sample(val tMillis: Long, val value: Float)

fun List<Sample>.pruneOlderThan(windowMs: Long, nowMs: Long): List<Sample> =
    dropWhile { nowMs - it.tMillis > windowMs }
