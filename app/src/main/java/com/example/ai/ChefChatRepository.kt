package com.example.ai

import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import com.example.R
import com.example.data.model.PantryItem
import com.example.data.model.Recipe
import com.example.settings.AppSettingsPreferences
import com.example.util.ImageUtils
import com.example.util.LocaleHelper
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

enum class ChefChatRole(
    val modelName: String,
    @StringRes val labelResId: Int
) {
    GENERAL("gemini-3.8-flash", R.string.chef_chat_role_general),
    REASONING("gemini-4-argon", R.string.chef_chat_role_reasoning),
    FAST("gemini-3.5-flash-lite", R.string.chef_chat_role_fast)
}

data class ChefChatMessage(
    val id: String,
    val role: String, // "user" or "model"
    val text: String,
    val attachedImageUri: String = "",
    val isError: Boolean = false
)

sealed interface ChefChatStreamEvent {
    data class Chunk(val accumulatedText: String) : ChefChatStreamEvent
    data class Completed(val finalText: String) : ChefChatStreamEvent
    data class Failed(val errorMessage: String) : ChefChatStreamEvent
}

/**
 * Multi-turn culinary Gemini assistant powered by Firebase AI Logic (`Firebase.ai`),
 * maintaining chat history and grounding answers in the user's real pantry & recipes.
 */
object ChefChatRepository {

    private const val TAG = "ChefChatRepo"
    private const val STREAM_TIMEOUT_MS = 60_000L

    fun buildSystemInstructionText(
        context: Context,
        role: ChefChatRole,
        recipes: List<Recipe>,
        pantry: List<PantryItem>
    ): String {
        val langCode = AppSettingsPreferences.getLanguageCode(context).ifBlank {
            java.util.Locale.getDefault().language
        }
        val languageName = when (langCode.lowercase()) {
            "en" -> "English"
            "pt" -> "Portuguese"
            "fr" -> "French"
            else -> "Spanish"
        }

        val roleDirective = when (role) {
            ChefChatRole.GENERAL -> "You are a practical, realistic culinary chef & kitchen manager."
            ChefChatRole.REASONING -> "You are a meticulous culinary production & nutrition strategist focused on real batch cost optimization, accurate yields, and practical ingredient substitutions."
            ChefChatRole.FAST -> "You are a concise, fast kitchen helper giving direct step-by-step culinary answers in seconds."
        }

        val pantrySummary = if (pantry.isEmpty()) {
            "(empty)"
        } else {
            pantry.take(35).joinToString("; ") { item ->
                val qty = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
                "${item.name}: $qty ${item.unit}"
            }
        }

        val recipesSummary = if (recipes.isEmpty()) {
            "(none saved)"
        } else {
            recipes.take(25).joinToString("; ") { recipe ->
                val ingNames = recipe.ingredients.take(6).joinToString(", ") { it.name }
                "${recipe.title} (${recipe.servings} serv., $ingNames)"
            }
        }

        return "$roleDirective " +
            "Always reply in $languageName. " +
            "Base every suggestion strictly on real cooking techniques and the user's actual inventory—do not invent fake ingredients in their pantry. " +
            "USER PANTRY (${pantry.size} items): $pantrySummary. " +
            "USER SAVED RECIPES (${recipes.size}): $recipesSummary."
    }

    fun sendChatMessageStream(
        context: Context,
        role: ChefChatRole,
        historyMessages: List<ChefChatMessage>,
        userPrompt: String,
        attachedImageUri: String,
        recipes: List<Recipe>,
        pantry: List<PantryItem>
    ): Flow<ChefChatStreamEvent> = flow {
        val loc = LocaleHelper.localizedContext(context)
        val systemText = buildSystemInstructionText(context, role, recipes, pantry)

        val config = generationConfig {
            temperature = when (role) {
                ChefChatRole.REASONING -> 0.4f
                ChefChatRole.FAST -> 0.5f
                ChefChatRole.GENERAL -> 0.65f
            }
        }

        val model = Firebase.ai.generativeModel(
            modelName = role.modelName,
            generationConfig = config,
            systemInstruction = content { text(systemText) }
        )

        val sdkHistory: List<Content> = historyMessages
            .filter { !it.isError && it.text.isNotBlank() }
            .takeLast(16)
            .map { msg ->
                content(role = if (msg.role == "model") "model" else "user") {
                    text(msg.text)
                }
            }

        val attachedBitmap = if (attachedImageUri.isNotBlank()) {
            ImageUtils.decodeImageModelToBitmap(context, attachedImageUri)
        } else {
            null
        }

        val trimmedPrompt = userPrompt.trim().ifBlank {
            loc.getString(R.string.chef_chat_user_photo_badge)
        }

        val outgoingContent = content(role = "user") {
            if (attachedBitmap != null) {
                image(attachedBitmap)
            }
            text(trimmedPrompt)
        }

        val chat = model.startChat(history = sdkHistory)
        val accumulated = StringBuilder()

        withTimeout(STREAM_TIMEOUT_MS) {
            chat.sendMessageStream(outgoingContent).collect { chunk ->
                val piece = chunk.text
                if (!piece.isNullOrEmpty()) {
                    accumulated.append(piece)
                    emit(ChefChatStreamEvent.Chunk(accumulated.toString()))
                }
            }
        }

        val finalText = accumulated.toString().trim()
        if (finalText.isBlank()) {
            emit(ChefChatStreamEvent.Failed(loc.getString(R.string.chef_chat_empty_response)))
        } else {
            emit(ChefChatStreamEvent.Completed(finalText))
        }
    }.catch { err ->
        if (err is CancellationException) throw err
        val loc = LocaleHelper.localizedContext(context)
        Log.e(TAG, "Chef AI chat failed (${role.modelName})", err)
        val msg = when (err) {
            is TimeoutCancellationException -> loc.getString(R.string.chef_chat_timeout)
            else -> err.localizedMessage?.takeIf { it.isNotBlank() }
                ?: loc.getString(R.string.chef_chat_error_generic)
        }
        emit(ChefChatStreamEvent.Failed(msg))
    }
}
