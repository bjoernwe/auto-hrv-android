package dev.upaya.autohrv.domain.signal

import org.apache.commons.math3.linear.Array2DRowRealMatrix
import org.apache.commons.math3.linear.EigenDecomposition
import kotlin.math.abs

/** Eigenvalues below this are treated as zero (no variance along that direction). */
private const val VARIANCE_EPSILON = 1e-12

/** One principal direction of a covariance matrix, with the variance it accounts for. */
internal data class PrincipalComponent(
    /** Unit-length eigenvector; index `i` is the loading of input dimension `i`. */
    val loadings: List<Float>,
    /** The corresponding eigenvalue — variance of the data projected onto [loadings]. */
    val variance: Float,
)

/**
 * The leading [PrincipalComponent]s of a covariance matrix, plus the variance of the whole matrix.
 * [totalVariance] sums *all* eigenvalues, not only the returned ones, so explained-variance ratios
 * stay correct when fewer components than dimensions are requested.
 */
internal data class PcaBasis(
    val components: List<PrincipalComponent>,
    val totalVariance: Float,
)

/**
 * Leading principal components of a symmetric [covariance] matrix, ordered by descending variance.
 *
 * An eigenvector's sign is mathematically arbitrary, so it is canonicalized here: each vector is
 * flipped such that its largest-magnitude loading is positive. That makes the result a pure
 * function of the input, but it can still flip between two nearly-tied loadings — a caller
 * displaying a sequence of decompositions should additionally align each result with the previous
 * one.
 *
 * @param count how many leading components to return; clamped to the matrix dimension.
 * @return an empty basis when the matrix is empty, non-square, or has no variance at all.
 */
internal fun principalComponents(
    covariance: List<List<Double>>,
    count: Int,
): PcaBasis {
    val n = covariance.size
    if (n == 0 || covariance.any { it.size != n }) return PcaBasis(emptyList(), 0f)

    val decomposition = EigenDecomposition(Array2DRowRealMatrix(covariance.map { it.toDoubleArray() }.toTypedArray()))
    // EigenDecomposition orders the eigenvalues of a symmetric matrix in decreasing order.
    val eigenvalues = DoubleArray(n) { decomposition.getRealEigenvalue(it) }
    val totalVariance = eigenvalues.sumOf { maxOf(it, 0.0) }
    if (totalVariance < VARIANCE_EPSILON) return PcaBasis(emptyList(), 0f)

    val components =
        (0 until count.coerceIn(0, n)).map { i ->
            PrincipalComponent(
                loadings = canonicalSign(decomposition.getEigenvector(i).toArray()),
                variance = maxOf(eigenvalues[i], 0.0).toFloat(),
            )
        }
    return PcaBasis(components, totalVariance.toFloat())
}

/** Flips [vector] so its largest-magnitude entry is positive. */
private fun canonicalSign(vector: DoubleArray): List<Float> {
    val dominant = vector.maxByOrNull { abs(it) } ?: 0.0
    val sign = if (dominant < 0.0) -1.0 else 1.0
    return vector.map { (it * sign).toFloat() }
}
