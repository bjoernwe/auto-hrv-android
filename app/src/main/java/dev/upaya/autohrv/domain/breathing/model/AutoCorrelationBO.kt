package dev.upaya.autohrv.domain.breathing.model

/** Per-lag autocorrelation of the RR signal; index `k` is the correlation at lag `k` seconds. */
data class AutoCorrelationBO(
    val values: List<Float>,
    val peakLagSeconds: Float?,
)
