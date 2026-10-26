package com.kletaq.app.features.journey.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Backwards-compatible wrapper delegating to MetroModuleHeader.
 */
@Composable
fun UnitSectionHeader(
    unit: UnitJourney,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier,
    onShareUnitClick: (() -> Unit)? = null
) {
    MetroModuleHeader(
        unit = unit,
        onToggleExpand = onToggleExpand,
        modifier = modifier,
        onShareUnitClick = onShareUnitClick
    )
}
