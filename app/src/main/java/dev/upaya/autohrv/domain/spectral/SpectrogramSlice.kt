package dev.upaya.autohrv.domain.spectral

/** One time-slice of the spectrogram: power per frequency bin, indices matching a shared frequency axis. */
data class SpectrogramSlice(
    val timestampMillis: Long,
    val powerByFreqBin: List<Float>,
)
