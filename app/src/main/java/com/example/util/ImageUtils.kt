package com.example.util

import android.content.Context
import android.net.Uri
import java.io.File

object ImageUtils {
    /**
     * Copies a content URI (e.g. from Photo Picker) to app internal storage
     * so that it persists permanently without expiring permission grants.
     */
    fun saveImageLocally(context: Context, uri: Uri): String {
        return try {
            val dir = File(context.filesDir, "recipe_photos").apply {
                if (!exists()) mkdirs()
            }
            val extension = when (context.contentResolver.getType(uri)) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> "jpg"
            }
            val destination = File(dir, "rec_${System.currentTimeMillis()}.$extension")
            context.contentResolver.openInputStream(uri)?.use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            destination.absolutePath
        } catch (_: Exception) {
            uri.toString()
        }
    }
}
