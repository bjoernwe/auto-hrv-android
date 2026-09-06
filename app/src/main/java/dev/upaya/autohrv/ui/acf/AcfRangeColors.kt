package dev.upaya.autohrv.ui.acf

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private const val IN_RANGE_ALPHA = 0.30f
private const val OUT_RANGE_ALPHA = 0.1f

/** Shared color for the target-band-matching parts of the ACF chart and its range slider. */
@Composable
fun acfInRangeColor(): Color = MaterialTheme.colorScheme.primary.copy(alpha = IN_RANGE_ALPHA)

/**
 * Bar color for a negative principal-component loading. The heart tone against the breath tone of
 * [acfInRangeColor] makes "these lags move the other way" readable at a glance, in the two colors
 * the chart already speaks in.
 */
@Composable
fun acfNegativeLoadingColor(): Color = MaterialTheme.colorScheme.secondary.copy(alpha = IN_RANGE_ALPHA)

/** Shared color for the out-of-band parts of the ACF chart and its range slider. */
@Composable
fun acfOutRangeColor(): Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = OUT_RANGE_ALPHA)
