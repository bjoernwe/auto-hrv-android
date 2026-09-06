package dev.upaya.autohrv.domain.breathing.model

/** Lag 0 is excluded from the ACF PCA — its correlation is fixed at 1 — so the analysis starts here. */
internal const val ACF_PCA_FIRST_LAG = 1

/**
 * Decaying first and second moments of the ACF curves seen so far, over lags
 * `ACF_PCA_FIRST_LAG..maxLag` (lag 0 is excluded — its correlation is fixed at 1 and has no
 * variance). Everything a covariance needs, in a form that can be updated in constant time per new
 * curve.
 *
 * @param observations how many curves have been folded in, undecayed — the warm-up counter.
 * @param weight the decayed sum of the per-curve weights (`Σ wᵢ`).
 * @param latest the curve folded in last — the current one — over the same lags as [sum], so a
 *   consumer projecting it onto the components needs no second read of the ACF flow.
 * @param sum the decayed weighted sum of the curves (`Σ wᵢ·xᵢ`).
 * @param sumOuter the decayed weighted sum of their outer products (`Σ wᵢ·xᵢxᵢᵀ`).
 */
internal data class AcfMomentsBO(
    val observations: Int,
    val weight: Double,
    val latest: List<Double>,
    val sum: List<Double>,
    val sumOuter: List<List<Double>>,
) {
    val dimension: Int get() = sum.size

    companion object {
        val EMPTY =
            AcfMomentsBO(
                observations = 0,
                weight = 0.0,
                latest = emptyList(),
                sum = emptyList(),
                sumOuter = emptyList(),
            )
    }
}
