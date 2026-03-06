package com.chs.yourdocscanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.ImageProxy

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