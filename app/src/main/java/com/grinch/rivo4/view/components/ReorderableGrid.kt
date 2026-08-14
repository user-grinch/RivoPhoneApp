package com.grinch.rivo4.view.components

import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel

/**
 * Lightweight long-press drag-and-drop reordering for a [LazyGridState], following the
 * canonical Compose reorderable-grid pattern. Hit-testing is done against the grid's own
 * visible-item layout info, so it works for any fixed-column grid whose items map 1:1 to
 * the backing list (no headers).
 */
@Composable
fun rememberGridDragDropState(
    gridState: LazyGridState,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit
): GridDragDropState {
    val scope = rememberCoroutineScope()
    return remember(gridState) {
        GridDragDropState(state = gridState, scope = scope, onMove = onMove)
    }
}

class GridDragDropState internal constructor(
    private val state: LazyGridState,
    private val scope: CoroutineScope,
    private val onMove: (Int, Int) -> Unit
) {
    var draggingItemIndex by mutableStateOf<Int?>(null)
        private set

    internal val scrollChannel = Channel<Float>()

    private var draggingItemDraggedDelta by mutableStateOf(Offset.Zero)
    private var draggingItemInitialOffset by mutableStateOf(Offset.Zero)

    internal val draggingItemOffset: Offset
        get() = draggingItemLayoutInfo?.let { item ->
            draggingItemInitialOffset + draggingItemDraggedDelta - item.offset.toOffset()
        } ?: Offset.Zero

    private val draggingItemLayoutInfo: LazyGridItemInfo?
        get() = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == draggingItemIndex }

    internal fun onDragStart(offset: Offset) {
        state.layoutInfo.visibleItemsInfo.firstOrNull { item ->
            offset.x.toInt() in item.offset.x..(item.offset.x + item.size.width) &&
                offset.y.toInt() in item.offset.y..(item.offset.y + item.size.height)
        }?.also {
            draggingItemIndex = it.index
            draggingItemInitialOffset = it.offset.toOffset()
        }
    }

    internal fun onDragInterrupted() {
        draggingItemIndex = null
        draggingItemDraggedDelta = Offset.Zero
        draggingItemInitialOffset = Offset.Zero
    }

    internal fun onDrag(offset: Offset) {
        draggingItemDraggedDelta += offset

        val draggingItem = draggingItemLayoutInfo ?: return
        val startOffset = draggingItemInitialOffset + draggingItemDraggedDelta
        val endOffset = startOffset + Offset(
            draggingItem.size.width.toFloat(),
            draggingItem.size.height.toFloat()
        )
        val middleOffset = (startOffset + endOffset) / 2f

        val targetItem = state.layoutInfo.visibleItemsInfo.find { item ->
            middleOffset.x.toInt() in item.offset.x..(item.offset.x + item.size.width) &&
                middleOffset.y.toInt() in item.offset.y..(item.offset.y + item.size.height) &&
                draggingItem.index != item.index
        }

        if (targetItem != null) {
            onMove(draggingItem.index, targetItem.index)
            draggingItemIndex = targetItem.index
        } else {
            val overscroll = when {
                draggingItemDraggedDelta.y < 0 ->
                    (startOffset.y - state.layoutInfo.viewportStartOffset).coerceAtMost(0f)
                draggingItemDraggedDelta.y > 0 ->
                    (endOffset.y - state.layoutInfo.viewportEndOffset).coerceAtLeast(0f)
                else -> 0f
            }
            if (overscroll != 0f) scrollChannel.trySend(overscroll)
        }
    }

    private fun androidx.compose.ui.unit.IntOffset.toOffset() = Offset(x.toFloat(), y.toFloat())
}

@Composable
fun rememberRowDragDropState(
    lazyListState: androidx.compose.foundation.lazy.LazyListState,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit
): RowDragDropState {
    val scope = rememberCoroutineScope()
    return remember(lazyListState) {
        RowDragDropState(state = lazyListState, scope = scope, onMove = onMove)
    }
}

class RowDragDropState internal constructor(
    private val state: androidx.compose.foundation.lazy.LazyListState,
    private val scope: CoroutineScope,
    private val onMove: (Int, Int) -> Unit
) {
    var draggingItemIndex by mutableStateOf<Int?>(null)
        private set

    internal val scrollChannel = Channel<Float>()

    private var draggingItemDraggedDelta by mutableStateOf(Offset.Zero)
    private var draggingItemInitialOffset by mutableStateOf(Offset.Zero)

    internal val draggingItemOffset: Offset
        get() = draggingItemLayoutInfo?.let { item ->
            draggingItemInitialOffset + draggingItemDraggedDelta - Offset(item.offset.toFloat(), 0f)
        } ?: Offset.Zero

    private val draggingItemLayoutInfo: androidx.compose.foundation.lazy.LazyListItemInfo?
        get() = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == draggingItemIndex }

    internal fun onDragStart(offset: Offset) {
        state.layoutInfo.visibleItemsInfo.firstOrNull { item ->
            offset.x.toInt() in item.offset..(item.offset + item.size)
        }?.also {
            draggingItemIndex = it.index
            draggingItemInitialOffset = Offset(it.offset.toFloat(), 0f)
        }
    }

    internal fun onDragInterrupted() {
        draggingItemIndex = null
        draggingItemDraggedDelta = Offset.Zero
        draggingItemInitialOffset = Offset.Zero
    }

    internal fun onDrag(offset: Offset) {
        draggingItemDraggedDelta += offset

        val draggingItem = draggingItemLayoutInfo ?: return
        val startOffset = draggingItemInitialOffset + draggingItemDraggedDelta
        val middleX = startOffset.x + (draggingItem.size / 2f)

        val targetItem = state.layoutInfo.visibleItemsInfo.find { item ->
            middleX.toInt() in item.offset..(item.offset + item.size) &&
                draggingItem.index != item.index
        }

        if (targetItem != null) {
            onMove(draggingItem.index, targetItem.index)
            draggingItemIndex = targetItem.index
        }
    }
}
