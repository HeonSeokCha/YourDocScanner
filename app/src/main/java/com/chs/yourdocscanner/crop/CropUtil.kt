package com.chs.yourdocscanner.crop

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import com.chs.yourdocscanner.clampTo
import com.chs.yourdocscanner.computeFitRect
import com.chs.yourdocscanner.defaultCorners
import com.chs.yourdocscanner.indexOfMinDistanceTo
import com.chs.yourdocscanner.scan.DetectedQuad
import com.chs.yourdocscanner.toCanvasCorners
import com.chs.yourdocscanner.toImagePoints

class CropUtil {

    fun initialize(
        current: CropState,
        bitmap: Bitmap,
        initialQuad: DetectedQuad?
    ): CropState {
        val next = current.copy(bitmap = bitmap)
        if (next.canvasSize == IntSize.Zero) return next

        val rect = computeImageRect(next.canvasSize, bitmap)
        return next.copy(
            imageRect = rect,
            corners = resolveCorners(rect, bitmap, initialQuad),
        )
    }

    fun updateCanvasSize(
        current: CropState,
        size: IntSize
    ): CropState {
        if (size == current.canvasSize) return current
        val bitmap = current.bitmap ?: return current.copy(canvasSize = size)

        val rect = computeImageRect(size, bitmap)
        val newCorners = if (current.corners.isEmpty()) {
            rect.defaultCorners()
        } else {
            remapCorners(current.corners, current.imageRect, rect)
        }
        return current.copy(
            canvasSize = size,
            imageRect = rect,
            corners = newCorners
        )
    }

    fun dragStart(
        current: CropState,
        offset: Offset,
        touchRadiusPx: Float
    ): CropState {
        val index = current.corners.indexOfMinDistanceTo(offset, touchRadiusPx)
        return current.copy(draggingIdx = index)
    }

    fun dragMove(
        current: CropState,
        offset: Offset
    ): CropState {
        if (current.draggingIdx < 0) return current
        val clamped = offset.clampTo(current.imageRect)
        val updated = current.corners.toMutableList()
            .also { it[current.draggingIdx] = clamped }
        return current.copy(corners = updated)
    }

    fun dragEnd(current: CropState): CropState =
        current.copy(draggingIdx = -1)

    fun reset(current: CropState): CropState =
        current.copy(corners = current.imageRect.defaultCorners())

    fun extractImagePoints(current: CropState): FloatArray? {
        val bitmap = current.bitmap ?: return null
        if (current.corners.size < 4) return null
        return current.corners.toImagePoints(current.imageRect, bitmap)
    }


    private fun computeImageRect(size: IntSize, bitmap: Bitmap): Rect =
        computeFitRect(
            canvasW = size.width.toFloat(),
            canvasH = size.height.toFloat(),
            imageW = bitmap.width.toFloat(),
            imageH = bitmap.height.toFloat()
        )

    private fun resolveCorners(
        rect: Rect,
        bitmap: Bitmap,
        quad: DetectedQuad?
    ): List<Offset> =
        quad?.toCanvasCorners(rect, bitmap) ?: rect.defaultCorners()

    private fun remapCorners(
        corners: List<Offset>,
        oldRect: Rect,
        newRect: Rect
    ): List<Offset> {
        if (oldRect == Rect.Zero) return newRect.defaultCorners()
        return corners.map { c ->
            val ratioX = (c.x - oldRect.left) / oldRect.width
            val ratioY = (c.y - oldRect.top) / oldRect.height
            Offset(
                x = newRect.left + ratioX * newRect.width,
                y = newRect.top + ratioY * newRect.height
            ).clampTo(newRect)
        }
    }
}