package dev.upaya.autohrv.domain.metrics

data class MetricsConfig(
    val beatWindowSeconds: Int,
) {
    companion object {
        val DEFAULT = MetricsConfig(beatWindowSeconds = 20)
    }
}
