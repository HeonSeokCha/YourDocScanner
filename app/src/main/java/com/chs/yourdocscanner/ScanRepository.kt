package com.chs.yourdocscanner

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Single
class ScanRepository(
    private val context: Context
) {
    suspend fun saveImage(bitmap: Bitmap): File? {
        return withContext(Dispatchers.IO) {
            val fileName = "DOC_${
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            }.jpg"

            val docDir = File(context.filesDir, "documents")
            if (!docDir.exists()) {
                docDir.mkdir()
            }
            val file = File(docDir, fileName)

            return@withContext try {
                file.outputStream().use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                }
                file
            } catch (e: Exception) {
                Log.e("CHS_123", e.message.toString())
                null
            }
        }
    }
}