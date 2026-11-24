package com.example.ui.components

import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

val LocalTutorialTargets = staticCompositionLocalOf<SnapshotStateMap<String, Rect>?> { null }

fun Modifier.tutorialTarget(id: String): Modifier = composed {
    val registry = LocalTutorialTargets.current
    if (registry == null) {
        this
    } else {
        this.onGloballyPositioned { coordinates ->
            if (coordinates.isAttached) {
                val bounds = coordinates.boundsInRoot()
                if (bounds.width > 0f && bounds.height > 0f) {
                    registry[id] = bounds
                }
            }
        }
    }
}
