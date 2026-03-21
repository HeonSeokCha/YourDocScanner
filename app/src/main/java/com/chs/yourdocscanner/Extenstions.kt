package com.chs.yourdocscanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.chs.yourdocscanner.scan.DetectedQuad

fun ImageProxy.toRawBitmap(): Bitmap? {
    val buffer = planes[0].buffer
    val bytes  = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

fun Bitmap.applyRotation(degrees: Int): Bitmap {
    if (degrees == 0) return this
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
        .also { if (it != this) recycle() }
}

fun ImageProxy.toNV21ByteArray(): ByteArray {
    val y = planes[0].buffer
    val u = planes[1].buffer
    val v = planes[2].buffer
    val nv21 = ByteArray(y.remaining() + u.remaining() + v.remaining())
    y.get(nv21, 0, y.remaining())
    v.get(nv21, y.capacity(), v.remaining())
    u.get(nv21, y.capacity() + v.capacity(), u.remaining())
    return nv21
}

fun computeFitRect(
    canvasW: Float, canvasH: Float,
    imageW: Float,  imageH: Float
): Rect {
    val scale  = minOf(canvasW / imageW, canvasH / imageH)
    val scaledW = imageW * scale
    val scaledH = imageH * scale
    val left   = (canvasW - scaledW) / 2f
    val top    = (canvasH - scaledH) / 2f
    return Rect(left, top, left + scaledW, top + scaledH)
}

fun Offset.imageToCanvas(rect: Rect, bitmap: Bitmap): Offset = Offset(
    x = rect.left + x / bitmap.width  * rect.width,
    y = rect.top  + y / bitmap.height * rect.height
)

fun Offset.canvasToImage(rect: Rect, bitmap: Bitmap): Offset = Offset(
    x = (x - rect.left) / rect.width  * bitmap.width,
    y = (y - rect.top)  / rect.height * bitmap.height
)

fun DetectedQuad.toCanvasCorners(rect: Rect, bitmap: Bitmap?): List<Offset>? {
    if (bitmap == null) return null
    return listOf(
        topLeft.imageToCanvas(rect, bitmap),
        topRight.imageToCanvas(rect, bitmap),
        bottomRight.imageToCanvas(rect, bitmap),
        bottomLeft.imageToCanvas(rect, bitmap)
    )
}

fun List<Offset>.toImagePoints(rect: Rect, bitmap: Bitmap): FloatArray {
    val pts = map { it.canvasToImage(rect, bitmap) }
    return floatArrayOf(
        pts[0].x, pts[0].y,  // TL
        pts[1].x, pts[1].y,  // TR
        pts[2].x, pts[2].y,  // BR
        pts[3].x, pts[3].y   // BL
    )
}

fun Rect.defaultCorners(): List<Offset> = listOf(
    Offset(left,  top),     // TL
    Offset(right, top),     // TR
    Offset(right, bottom),  // BR
    Offset(left,  bottom)   // BL
)

fun List<Offset>.indexOfMinDistanceTo(
    target: Offset,
    threshold: Float
): Int {
    var minDist = Float.MAX_VALUE
    var minIdx  = -1
    forEachIndexed { idx, corner ->
        val dist = (corner - target).getDistance()
        if (dist < minDist && dist < threshold) {
            minDist = dist
            minIdx  = idx
        }
    }
    return minIdx
}

fun Offset.clampTo(rect: Rect): Offset = Offset(
    x = x.coerceIn(rect.left, rect.right),
    y = y.coerceIn(rect.top,  rect.bottom)
)

fun FloatArray.toDetectQuad(): DetectedQuad {
    return DetectedQuad(
        topLeft = Offset(this[0], this[1]),
        topRight = Offset(this[2], this[3]),
        bottomLeft = Offset(this[4], this[5]),
        bottomRight = Offset(this[6], this[7]),
        imageWidth = 0,
        imageHeight = 0,
        rotationDegrees = 0
    )
}