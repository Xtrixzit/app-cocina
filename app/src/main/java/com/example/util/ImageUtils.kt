package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.max

object ImageUtils {

    private const val MAX_DIMENSION_PX = 480
    private const val MAX_BYTES = 42_000

    /**
     * Compresses an image picked via Android Photo Picker into a portable JPEG Base64 data URI
     * so recipe thumbnails sync reliably across devices via Cloud Firestore & JSON backups.
     */
    fun saveImageLocally(context: Context, uri: Uri): String {
        return try {
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }

            val rawWidth = boundsOptions.outWidth
            val rawHeight = boundsOptions.outHeight
            if (rawWidth <= 0 || rawHeight <= 0) {
                return uri.toString()
            }

            var sampleSize = 1
            while ((rawWidth / sampleSize) > MAX_DIMENSION_PX * 2 || (rawHeight / sampleSize) > MAX_DIMENSION_PX * 2) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return uri.toString()

            val maxSide = max(decoded.width, decoded.height)
            val scaled = if (maxSide > MAX_DIMENSION_PX) {
                val ratio = MAX_DIMENSION_PX.toFloat() / maxSide.toFloat()
                val targetW = (decoded.width * ratio).toInt().coerceAtLeast(1)
                val targetH = (decoded.height * ratio).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(decoded, targetW, targetH, true).also {
                    if (it !== decoded) decoded.recycle()
                }
            } else {
                decoded
            }

            val baos = ByteArrayOutputStream()
            var quality = 78
            do {
                baos.reset()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                quality -= 14
            } while (baos.size() > MAX_BYTES && quality >= 30)

            val bytes = baos.toByteArray()
            scaled.recycle()

            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            "data:image/jpeg;base64,$b64"
        } catch (_: Exception) {
            uri.toString()
        }
    }

    /**
     * Converts portable data URIs into decoded ByteArray for Coil AsyncImage,
     * while leaving regular URLs / local paths intact.
     */
    fun toImageModel(imageUrl: String): Any {
        if (imageUrl.startsWith("data:image", ignoreCase = true) && imageUrl.contains("base64,")) {
            val base64Part = imageUrl.substringAfter("base64,")
            return try {
                Base64.decode(base64Part, Base64.DEFAULT)
            } catch (_: Exception) {
                imageUrl
            }
        }
        return imageUrl
    }
}
