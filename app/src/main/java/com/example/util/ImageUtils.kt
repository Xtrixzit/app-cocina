package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.R
import java.io.ByteArrayOutputStream
import kotlin.math.max

data class ReferencePhotoPreset(
    val uriKey: String,
    @DrawableRes val drawableResId: Int,
    @StringRes val labelResId: Int
)

object ImageUtils {

    private const val MAX_DIMENSION_PX = 480
    private const val MAX_BYTES = 42_000

    const val REF_TORTILLA = "res://img_ref_tortilla_patatas"
    const val REF_PASTA = "res://img_ref_pasta_pollo"
    const val REF_PANCAKES = "res://img_ref_panqueques_avena"
    const val REF_LUNCH = "res://img_ref_almuerzo_casero"
    const val REF_DESSERT = "res://img_ref_postre_casero"

    val referentialPhotoPresets: List<ReferencePhotoPreset> = listOf(
        ReferencePhotoPreset(REF_LUNCH, R.drawable.img_ref_almuerzo_casero, R.string.ref_photo_lunch),
        ReferencePhotoPreset(REF_TORTILLA, R.drawable.img_ref_tortilla_patatas, R.string.ref_photo_tortilla),
        ReferencePhotoPreset(REF_PASTA, R.drawable.img_ref_pasta_pollo, R.string.ref_photo_pasta),
        ReferencePhotoPreset(REF_PANCAKES, R.drawable.img_ref_panqueques_avena, R.string.ref_photo_pancakes),
        ReferencePhotoPreset(REF_DESSERT, R.drawable.img_ref_postre_casero, R.string.ref_photo_dessert)
    )

    @DrawableRes
    fun resolveBundledDrawableOrNull(imageUrl: String): Int? = when (imageUrl.trim()) {
        REF_TORTILLA -> R.drawable.img_ref_tortilla_patatas
        REF_PASTA -> R.drawable.img_ref_pasta_pollo
        REF_PANCAKES -> R.drawable.img_ref_panqueques_avena
        REF_LUNCH -> R.drawable.img_ref_almuerzo_casero
        REF_DESSERT -> R.drawable.img_ref_postre_casero
        else -> null
    }

    fun defaultUriForCategory(category: String): String {
        val norm = category.trim().lowercase()
        return when {
            norm.contains("desayuno") || norm.contains("breakfast") || norm.contains("café") || norm.contains("petit") -> REF_PANCAKES
            norm.contains("postre") || norm.contains("dessert") || norm.contains("sobremesa") -> REF_DESSERT
            norm.contains("cena") || norm.contains("dinner") || norm.contains("jantar") || norm.contains("pasta") -> REF_PASTA
            norm.contains("snack") || norm.contains("tortilla") -> REF_TORTILLA
            else -> REF_LUNCH
        }
    }

    @DrawableRes
    fun defaultDrawableForCategory(category: String): Int =
        resolveBundledDrawableOrNull(defaultUriForCategory(category)) ?: R.drawable.img_ref_almuerzo_casero

    fun createCameraTempImageUri(context: Context): Uri {
        val dir = java.io.File(context.cacheDir, "camera_photos").apply { mkdirs() }
        val file = java.io.File(dir, "dish_${System.currentTimeMillis()}.jpg")
        return androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Scales and compresses an in-memory [Bitmap] (for example, an AI-generated photo)
     * into a portable JPEG Base64 data URI under Firestore's field size budget.
     */
    fun encodeBitmapToDataUri(source: Bitmap, recycleSource: Boolean = false): String {
        return try {
            val maxSide = max(source.width, source.height)
            val scaled = if (maxSide > MAX_DIMENSION_PX) {
                val ratio = MAX_DIMENSION_PX.toFloat() / maxSide.toFloat()
                val targetW = (source.width * ratio).toInt().coerceAtLeast(1)
                val targetH = (source.height * ratio).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(source, targetW, targetH, true)
            } else {
                source
            }

            val baos = ByteArrayOutputStream()
            var quality = 80
            do {
                baos.reset()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                quality -= 12
            } while (baos.size() > MAX_BYTES && quality >= 28)

            val bytes = baos.toByteArray()
            if (scaled !== source) {
                scaled.recycle()
            }
            if (recycleSource && !source.isRecycled) {
                source.recycle()
            }
            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            "data:image/jpeg;base64,$b64"
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Compresses an image picked via Android Photo Picker or Camera into a portable JPEG Base64 data URI
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

            val encoded = encodeBitmapToDataUri(decoded, recycleSource = true)
            encoded.ifBlank { uri.toString() }
        } catch (_: Exception) {
            uri.toString()
        }
    }

    suspend fun decodeImageModelToBitmap(context: Context, imageUrl: String): Bitmap? =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val trimmed = imageUrl.trim()
            if (trimmed.isBlank()) return@withContext null
            try {
                resolveBundledDrawableOrNull(trimmed)?.let { resId ->
                    return@withContext BitmapFactory.decodeResource(context.resources, resId)
                }
                if (trimmed.startsWith("data:image", ignoreCase = true) && trimmed.contains("base64,")) {
                    val bytes = Base64.decode(trimmed.substringAfter("base64,"), Base64.DEFAULT)
                    return@withContext BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
                if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
                    val conn = java.net.URL(trimmed).openConnection().apply {
                        connectTimeout = 8000
                        readTimeout = 8000
                    }
                    conn.getInputStream().use { stream ->
                        return@withContext BitmapFactory.decodeStream(stream)
                    }
                }
                val parsedUri = Uri.parse(trimmed)
                context.contentResolver.openInputStream(parsedUri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } catch (_: Exception) {
                null
            }
        }

    /**
     * Converts bundled referential photo keys (`res://...`), portable data URIs (`data:image/...`),
     * or regular HTTPS/local URLs into a model consumable by Coil `AsyncImage`.
     */
    fun toImageModel(imageUrl: String): Any {
        val trimmed = imageUrl.trim()
        resolveBundledDrawableOrNull(trimmed)?.let { return it }
        if (trimmed.startsWith("data:image", ignoreCase = true) && trimmed.contains("base64,")) {
            val base64Part = trimmed.substringAfter("base64,")
            return try {
                Base64.decode(base64Part, Base64.DEFAULT)
            } catch (_: Exception) {
                trimmed
            }
        }
        return trimmed
    }
}
