package com.kletaq.app.features.journey.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Backwards-compatible wrapper delegating to MetroRoadmapNode.
 */
@Composable
fun DuolingoPathNode(
    lesson: LessonNode,
    currentOffset: Dp = 0.dp,
    onNodeClick: (LessonNode) -> Unit,
    modifier: Modifier = Modifier,
    retentionPercentage: Int? = null,
    isPriority: Boolean = false
) {
    MetroRoadmapNode(
        lesson = lesson,
        stationIndex = lesson.lessonNumber,
        totalStationsInModule = 5,
        isFirstInModule = false,
        isLastInModule = false,
        retentionPercentage = retentionPercentage,
        isPriority = isPriority,
        onNodeClick = onNodeClick,
        modifier = modifier
    )
}
