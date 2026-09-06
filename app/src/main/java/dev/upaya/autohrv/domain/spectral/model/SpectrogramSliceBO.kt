package dev.upaya.autohrv.domain.spectral.model

/** One time-slice of the spectrogram: power per frequency bin, indices matching a shared frequency axis. */
data class SpectrogramSliceBO(
    val timestampMillis: Long,
    val powerByFreqBin: List<Float>,
)
