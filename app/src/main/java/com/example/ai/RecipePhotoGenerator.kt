package com.example.ai

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.RecipeIngredient
import com.example.util.ImageUtils
import com.example.util.LocaleHelper
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

sealed interface RecipePhotoGenState {
    data object Idle : RecipePhotoGenState
    data object Generating : RecipePhotoGenState
    data class Success(val dataUri: String) : RecipePhotoGenState
    data class Error(val message: String) : RecipePhotoGenState
}

/**
 * Generates realistic editorial culinary photographs for recipes via Firebase AI Logic
 * (`gemini-nano-banana-2.1`) and compresses them for persistent storage in Room & Firestore.
 */
object RecipePhotoGenerator {

    private const val TAG = "RecipePhotoGen"

    private val imageModel by lazy {
        val config = generationConfig {
            responseModalities = listOf(ResponseModality.IMAGE)
            imageConfig = ImageConfig.builder()
                .setAspectRatio(AspectRatio.LANDSCAPE_4x3)
                .setImageSize(ImageSize.SIZE_1K)
                .build()
        }
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = config
        )
    }

    fun buildPhotorealisticPrompt(
        title: String,
        category: String,
        ingredients: List<RecipeIngredient>
    ): String {
        val topIngredients = ingredients
            .map { it.name.trim() }
            .filter { it.isNotBlank() }
            .take(5)
            .joinToString(", ")
        val ingredientsClause = if (topIngredients.isNotBlank()) {
            " prepared with $topIngredients"
        } else {
            ""
        }
        return "Photorealistic DSLR food photography of a freshly cooked dish of '${title.trim()}' " +
            "($category)$ingredientsClause, served appetizingly on handmade ceramic dishware on a rustic " +
            "wooden kitchen table, warm natural window daylight, authentic cooking textures, shallow depth " +
            "of field, crisp culinary detail, strictly NO illustration, NO cartoon, NO drawing, NO text, NO watermarks."
    }

    suspend fun generateRealisticRecipePhoto(
        context: Context,
        title: String,
        category: String,
        ingredients: List<RecipeIngredient>
    ): Result<String> {
        val loc = LocaleHelper.localizedContext(context)
        val trimmedTitle = title.trim()
        if (trimmedTitle.isBlank()) {
            return Result.failure(
                IllegalArgumentException(loc.getString(R.string.recipe_photo_ai_need_title))
            )
        }

        val prompt = buildPhotorealisticPrompt(trimmedTitle, category, ingredients)
        return try {
            val response = withTimeout(60_000L) {
                imageModel.generateContent(prompt)
            }
            val bitmap = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()
                ?.image

            if (bitmap == null) {
                Result.failure(
                    IllegalStateException(loc.getString(R.string.recipe_photo_ai_empty))
                )
            } else {
                val dataUri = withContext(Dispatchers.Default) {
                    ImageUtils.encodeBitmapToDataUri(bitmap)
                }
                if (dataUri.isBlank()) {
                    Result.failure(
                        IllegalStateException(loc.getString(R.string.recipe_photo_ai_empty))
                    )
                } else {
                    Result.success(dataUri)
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI recipe photo generation timed out", e)
            Result.failure(
                IllegalStateException(loc.getString(R.string.recipe_photo_ai_timeout))
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI recipe photo generation failed", e)
            Result.failure(
                IllegalStateException(e.localizedMessage ?: loc.getString(R.string.recipe_photo_ai_failed))
            )
        }
    }
}
