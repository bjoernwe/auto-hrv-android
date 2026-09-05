package dev.upaya.autohrv.domain.spectral.model

/** Static per-band info the chart needs; the frequency axis is fixed by the band's window/range. */
data class SpectrogramBandInfoBO(
    val label: String,
    val freqBinsHz: List<Float>,
)
