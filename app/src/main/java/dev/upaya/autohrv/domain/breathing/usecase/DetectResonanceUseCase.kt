package dev.upaya.autohrv.domain.breathing.usecase

import dev.upaya.autohrv.domain.breathing.BreathingConfig
import dev.upaya.autohrv.domain.breathing.model.AutoCorrelationBO
import dev.upaya.autohrv.domain.breathing.model.BreathingPatternBO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import kotlin.math.abs

internal class DetectResonanceUseCase
    @Inject
    constructor() {

        operator fun invoke(
            autoCorrelation: Flow<AutoCorrelationBO?>,
            currentPattern: Flow<BreathingPatternBO>,
            config: BreathingConfig,
        ): Flow<Boolean> =
            combine(autoCorrelation, currentPattern) { acf, pattern ->
                val peak = acf?.peakLagSeconds ?: return@combine false
                val peakValue = acf.values.getOrNull(peak.toInt()) ?: return@combine false
                abs(peak - pattern.cycleLengthSeconds) <= config.resonancePeakToleranceSeconds &&
                    peakValue > config.resonanceMinPeakValue
            }
    }
