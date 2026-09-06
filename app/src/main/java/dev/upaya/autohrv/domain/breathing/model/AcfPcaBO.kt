package dev.upaya.autohrv.domain.breathing.model

/**
 * Principal components of the ACF curves collected over the session, together with where the
 * current curve sits along each of them. [firstLag] is the lag of `loadings[0]` — lag 0 is excluded
 * from the analysis, so it is 1.
 */
data class AcfPcaBO(
    val firstLag: Int,
    val components: List<AcfPrincipalComponentBO>,
)

/** One principal component of the ACF curves, ordered by descending [explainedVarianceRatio]. */
data class AcfPrincipalComponentBO(
    /** Unit-length loading per lag, starting at [AcfPcaBO.firstLag]. */
    val loadings: List<Float>,
    /** Share of the total ACF variance this component accounts for, in `[0, 1]`. */
    val explainedVarianceRatio: Float,
    /** `sqrt(eigenvalue)` — the natural scale against which [score] is read. */
    val standardDeviation: Float,
    /** Projection of the current curve onto [loadings], in raw ACF units. */
    val score: Float,
)
