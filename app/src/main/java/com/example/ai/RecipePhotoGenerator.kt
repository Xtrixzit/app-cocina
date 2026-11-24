package com.example.ai

import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import com.example.R
import com.example.data.model.RecipeIngredient
import com.example.util.ImageUtils
import com.example.util.LocaleHelper
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.IOException

enum class PhotoAspectOption(
    val aspectRatio: AspectRatio,
    @StringRes val labelResId: Int
) {
    RATIO_4_3(AspectRatio.LANDSCAPE_4x3, R.string.recipe_form_aspect_4_3),
    LANDSCAPE_16_9(AspectRatio.LANDSCAPE_16x9, R.string.recipe_form_aspect_16_9),
    PORTRAIT_9_16(AspectRatio.PORTRAIT_9x16, R.string.recipe_form_aspect_9_16)
}

sealed interface RecipePhotoGenState {
    data object Idle : RecipePhotoGenState
    data object Generating : RecipePhotoGenState
    data class Success(val dataUri: String) : RecipePhotoGenState
    data class Error(val message: String) : RecipePhotoGenState
}

/**
 * Generates and enhances realistic editorial culinary photographs for recipes via Firebase AI Logic
 * (`gemini-nano-banana-2.1`) and compresses them for persistent storage in Room & Firestore.
 */
object RecipePhotoGenerator {

    private const val TAG = "RecipePhotoGen"

    private val modelCache = mutableMapOf<PhotoAspectOption, GenerativeModel>()

    private val minimalBaselineModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            }
        )
    }

    private fun modelForAspect(aspectOption: PhotoAspectOption): GenerativeModel {
        return synchronized(modelCache) {
            modelCache.getOrPut(aspectOption) {
                val config = generationConfig {
                    responseModalities = listOf(ResponseModality.IMAGE)
                    imageConfig = ImageConfig.builder()
                        .setAspectRatio(aspectOption.aspectRatio)
                        .setImageSize(ImageSize.SIZE_1K)
                        .build()
                }
                Firebase.ai.generativeModel(
                    modelName = "gemini-nano-banana-2.1",
                    generationConfig = config
                )
            }
        }
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

    fun buildEnhancementPrompt(title: String, category: String): String {
        val dishName = title.trim().ifBlank { category.ifBlank { "homemade dish" } }
        return "Enhance this real-life photograph of '$dishName' into a natural, appetizing DSLR culinary photograph. " +
            "Preserve the exact real food, ingredients, and authentic plating, while improving natural window daylight, " +
            "surface food texture, focus sharpness, and clean rustic wooden table presentation. " +
            "Strictly NO cartoon, NO illustration, NO artificial plastic look, NO text overlays."
    }

    internal fun classifyPhotoError(loc: Context, err: Throwable): String {
        val raw = err.localizedMessage.orEmpty()
        val lower = raw.lowercase()
        return when {
            err is TimeoutCancellationException -> loc.getString(R.string.recipe_photo_ai_timeout)
            err is QuotaExceededException ||
                lower.contains("quota") ||
                lower.contains("429") ||
                lower.contains("billing") ||
                lower.contains("blaze") ||
                lower.contains("resource_exhausted") -> loc.getString(R.string.recipe_photo_ai_quota_blaze)
            lower.contains("403") ||
                lower.contains("appcheck") ||
                lower.contains("app check") ||
                lower.contains("attestation") -> loc.getString(R.string.recipe_photo_ai_appcheck)
            err is IOException ||
                lower.contains("unable to resolve host") ||
                lower.contains("network") -> loc.getString(R.string.recipe_photo_ai_offline)
            raw.isNotBlank() -> raw
            else -> loc.getString(R.string.recipe_photo_ai_failed)
        }
    }

    suspend fun generateRealisticRecipePhoto(
        context: Context,
        title: String,
        category: String,
        ingredients: List<RecipeIngredient>,
        aspectOption: PhotoAspectOption = PhotoAspectOption.RATIO_4_3
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
            val response = try {
                withTimeout(60_000L) {
                    modelForAspect(aspectOption).generateContent(prompt)
                }
            } catch (primaryErr: TimeoutCancellationException) {
                throw primaryErr
            } catch (primaryErr: CancellationException) {
                throw primaryErr
            } catch (primaryErr: Exception) {
                val msg = primaryErr.localizedMessage.orEmpty().lowercase()
                if (primaryErr is QuotaExceededException || msg.contains("403") || msg.contains("quota") || msg.contains("billing")) {
                    throw primaryErr
                }
                Log.w(TAG, "Custom aspect generation failed; trying minimal 1:1 baseline", primaryErr)
                withTimeout(60_000L) {
                    minimalBaselineModel.generateContent(prompt)
                }
            }
            extractEncodedPhoto(loc, response)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI recipe photo generation timed out", e)
            Result.failure(IllegalStateException(loc.getString(R.string.recipe_photo_ai_timeout)))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI recipe photo generation failed", e)
            Result.failure(IllegalStateException(classifyPhotoError(loc, e)))
        }
    }

    suspend fun enhanceRealPhotoWithAi(
        context: Context,
        sourceImageUrl: String,
        title: String,
        category: String,
        aspectOption: PhotoAspectOption = PhotoAspectOption.RATIO_4_3
    ): Result<String> {
        val loc = LocaleHelper.localizedContext(context)
        val sourceBitmap = ImageUtils.decodeScaledBitmapForAi(context, sourceImageUrl, 768)
            ?: return Result.failure(
                IllegalArgumentException(loc.getString(R.string.recipe_photo_ai_need_source))
            )

        val prompt = buildEnhancementPrompt(title, category)
        val multimodalPrompt = content {
            image(sourceBitmap)
            text(prompt)
        }

        return try {
            val response = try {
                withTimeout(60_000L) {
                    modelForAspect(aspectOption).generateContent(multimodalPrompt)
                }
            } catch (primaryErr: TimeoutCancellationException) {
                throw primaryErr
            } catch (primaryErr: CancellationException) {
                throw primaryErr
            } catch (primaryErr: Exception) {
                val msg = primaryErr.localizedMessage.orEmpty().lowercase()
                if (primaryErr is QuotaExceededException || msg.contains("403") || msg.contains("quota") || msg.contains("billing")) {
                    throw primaryErr
                }
                Log.w(TAG, "Custom aspect photo enhancement failed; trying minimal baseline", primaryErr)
                withTimeout(60_000L) {
                    minimalBaselineModel.generateContent(multimodalPrompt)
                }
            }
            extractEncodedPhoto(loc, response)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI photo realism enhancement timed out", e)
            Result.failure(IllegalStateException(loc.getString(R.string.recipe_photo_ai_timeout)))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI photo realism enhancement failed", e)
            Result.failure(IllegalStateException(classifyPhotoError(loc, e)))
        }
    }

    private suspend fun extractEncodedPhoto(
        localizedContext: Context,
        response: com.google.firebase.ai.type.GenerateContentResponse
    ): Result<String> {
        val bitmap = response.candidates.firstOrNull()
            ?.content?.parts?.filterIsInstance<ImagePart>()
            ?.firstOrNull()
            ?.image

        return if (bitmap == null) {
            Result.failure(
                IllegalStateException(localizedContext.getString(R.string.recipe_photo_ai_empty))
            )
        } else {
            val dataUri = withContext(Dispatchers.Default) {
                ImageUtils.encodeBitmapToDataUri(bitmap)
            }
            if (dataUri.isBlank()) {
                Result.failure(
                    IllegalStateException(localizedContext.getString(R.string.recipe_photo_ai_empty))
                )
            } else {
                Result.success(dataUri)
            }
        }
    }
}
