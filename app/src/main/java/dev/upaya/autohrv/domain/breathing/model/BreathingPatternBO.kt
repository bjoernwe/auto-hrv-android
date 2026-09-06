package dev.upaya.autohrv.domain.breathing.model

data class BreathingPatternBO(
    val bias: Float,
    val cycleLengthSeconds: Float,
) {
    operator fun plus(other: BreathingPatternBO) =
        BreathingPatternBO(
            bias + other.bias,
            cycleLengthSeconds + other.cycleLengthSeconds,
        )

    operator fun div(x: Float) = BreathingPatternBO(bias / x, cycleLengthSeconds / x)
}
