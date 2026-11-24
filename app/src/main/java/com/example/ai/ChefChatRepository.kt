package com.example.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.annotation.StringRes
import com.example.R
import com.example.data.model.PantryItem
import com.example.data.model.Recipe
import com.example.settings.AppSettingsPreferences
import com.example.util.ImageUtils
import com.example.util.LocaleHelper
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withTimeout
import java.io.IOException

enum class ChefChatRole(
    val modelName: String,
    @StringRes val labelResId: Int
) {
    FAST("gemini-3.5-flash-lite", R.string.chef_chat_role_fast),
    GENERAL("gemini-3.8-flash", R.string.chef_chat_role_general),
    REASONING("gemini-4-argon", R.string.chef_chat_role_reasoning)
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
 * tuned for ultra-fast streaming latency across both home cooking and commercial production kitchens,
 * with automatic resilience fallback to `gemini-3.8-flash` (no-billing model) if `gemini-4-argon`
 * or streaming endpoints return an error.
 */
object ChefChatRepository {

    private const val TAG = "ChefChatRepo"
    private const val FAST_TIMEOUT_MS = 45_000L
    private const val STANDARD_TIMEOUT_MS = 60_000L
    private const val MAX_ATTACHMENT_PX = 640

    private val baseChefInstruction = content {
        text(
            "You are a fast, practical culinary and kitchen production assistant for both home cooks " +
                "and commercial food businesses. Give direct, concise, structured answers using exact " +
                "weights, servings, yield, and real inventory items. Never invent ingredients the user does not have."
        )
    }

    private val fastModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = ChefChatRole.FAST.modelName,
            generationConfig = generationConfig {
                temperature = 0.35f
                maxOutputTokens = 750
            },
            systemInstruction = baseChefInstruction
        )
    }

    private val generalModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = ChefChatRole.GENERAL.modelName,
            generationConfig = generationConfig {
                temperature = 0.55f
                maxOutputTokens = 1100
            },
            systemInstruction = baseChefInstruction
        )
    }

    private val reasoningModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = ChefChatRole.REASONING.modelName,
            generationConfig = generationConfig {
                temperature = 0.3f
                maxOutputTokens = 1400
            },
            systemInstruction = baseChefInstruction
        )
    }

    private fun modelForRole(role: ChefChatRole): GenerativeModel = when (role) {
        ChefChatRole.FAST -> fastModel
        ChefChatRole.GENERAL -> generalModel
        ChefChatRole.REASONING -> reasoningModel
    }

    private fun hasNetworkConnection(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return true
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            true
        }
    }

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
            ChefChatRole.FAST -> "Mode: Ultra-fast concise answer (bullets, exact weights/costs, zero fluff)."
            ChefChatRole.GENERAL -> "Mode: Practical home & commercial chef."
            ChefChatRole.REASONING -> "Mode: Production batch scaling, yield & cost analysis."
        }

        val pantryLimit = if (role == ChefChatRole.FAST) 25 else 35
        val pantrySummary = if (pantry.isEmpty()) {
            "(empty)"
        } else {
            pantry.take(pantryLimit).joinToString("; ") { item ->
                val qty = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
                val costSuffix = if (item.cost > 0.0) " ($${item.cost}/${item.costBaseQuantity.toInt().coerceAtLeast(1)}${item.unit})" else ""
                "${item.name}: $qty ${item.unit}$costSuffix"
            }
        }

        val recipesLimit = if (role == ChefChatRole.FAST) 15 else 25
        val recipesSummary = if (recipes.isEmpty()) {
            "(none saved)"
        } else {
            recipes.take(recipesLimit).joinToString("; ") { recipe ->
                val ingNames = recipe.ingredients.take(5).joinToString(", ") { it.name }
                "${recipe.title} (${recipe.servings} serv., $ingNames)"
            }
        }

        return "$roleDirective Reply ONLY in $languageName. " +
            "USER PANTRY (${pantry.size} items): $pantrySummary. " +
            "USER SAVED RECIPES (${recipes.size}): $recipesSummary."
    }

    /**
     * Ensures valid strictly alternating `user` -> `model` history pairs so any previously failed turn
     * never causes consecutive `user` turns rejected by the Gemini API.
     */
    internal fun buildSanitizedHistory(historyMessages: List<ChefChatMessage>): List<Content> {
        val pairs = mutableListOf<Pair<ChefChatMessage, ChefChatMessage>>()
        var pendingUser: ChefChatMessage? = null

        for (msg in historyMessages) {
            if (msg.isError || msg.text.isBlank()) {
                if (msg.isError) pendingUser = null
                continue
            }
            if (msg.role == "user") {
                pendingUser = msg
            } else if (msg.role == "model" && pendingUser != null) {
                pairs.add(pendingUser to msg)
                pendingUser = null
            }
        }

        return pairs.takeLast(4).flatMap { (u, m) ->
            listOf(
                content(role = "user") { text(u.text) },
                content(role = "model") { text(m.text) }
            )
        }
    }

    internal fun classifyGeminiError(loc: Context, err: Throwable): String {
        val raw = err.localizedMessage.orEmpty()
        val lower = raw.lowercase()
        return when {
            err is TimeoutCancellationException -> loc.getString(R.string.chef_chat_timeout)
            err is QuotaExceededException ||
                lower.contains("quota") ||
                lower.contains("429") ||
                lower.contains("resource_exhausted") ||
                lower.contains("billing") ||
                lower.contains("blaze") -> loc.getString(R.string.chef_chat_err_quota)
            lower.contains("403") ||
                lower.contains("appcheck") ||
                lower.contains("app check") ||
                lower.contains("attestation") ||
                lower.contains("permission_denied") -> loc.getString(R.string.chef_chat_err_appcheck)
            err is IOException ||
                lower.contains("unable to resolve host") ||
                lower.contains("network") ||
                lower.contains("failed to connect") -> loc.getString(R.string.chef_chat_offline)
            raw.isNotBlank() -> raw
            else -> loc.getString(R.string.chef_chat_error_generic)
        }
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
        if (!hasNetworkConnection(context)) {
            emit(ChefChatStreamEvent.Failed(loc.getString(R.string.chef_chat_offline)))
            return@flow
        }

        val primaryModel = modelForRole(role)
        val sdkHistory = buildSanitizedHistory(historyMessages)
        val compactGrounding = buildSystemInstructionText(context, role, recipes, pantry)

        val attachedBitmap = if (attachedImageUri.isNotBlank()) {
            ImageUtils.decodeScaledBitmapForAi(context, attachedImageUri, MAX_ATTACHMENT_PX)
        } else {
            null
        }

        val trimmedPrompt = userPrompt.trim().ifBlank {
            loc.getString(R.string.chef_chat_user_photo_badge)
        }

        val fullUserTurnText = "[KITCHEN CONTEXT: $compactGrounding]\nUSER REQUEST: $trimmedPrompt"

        val outgoingContent = content(role = "user") {
            if (attachedBitmap != null) {
                image(attachedBitmap)
            }
            text(fullUserTurnText)
        }

        val timeoutMs = if (role == ChefChatRole.FAST) FAST_TIMEOUT_MS else STANDARD_TIMEOUT_MS
        val accumulated = StringBuilder()
        var primaryStreamError: Exception? = null

        try {
            val chat = primaryModel.startChat(history = sdkHistory)
            withTimeout(timeoutMs) {
                chat.sendMessageStream(outgoingContent).collect { chunk ->
                    val piece = chunk.text
                    if (!piece.isNullOrEmpty()) {
                        accumulated.append(piece)
                        emit(ChefChatStreamEvent.Chunk(accumulated.toString()))
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            primaryStreamError = e
            Log.w(TAG, "Primary stream failed (${role.modelName}); trying unary fallback", e)
        }

        var finalText = accumulated.toString().trim()
        if (finalText.isBlank()) {
            // Real fallback: try unary sendMessage with generalModel (gemini-3.8-flash, billing not required)
            val fallbackModel = if (role == ChefChatRole.REASONING || role == ChefChatRole.FAST) {
                generalModel
            } else {
                primaryModel
            }
            try {
                val fallbackChat = fallbackModel.startChat(history = sdkHistory)
                val unaryResponse = withTimeout(STANDARD_TIMEOUT_MS) {
                    fallbackChat.sendMessage(outgoingContent)
                }
                finalText = unaryResponse.text?.trim().orEmpty()
                if (finalText.isNotBlank()) {
                    emit(ChefChatStreamEvent.Chunk(finalText))
                }
            } catch (fallbackErr: TimeoutCancellationException) {
                throw fallbackErr
            } catch (fallbackErr: CancellationException) {
                throw fallbackErr
            } catch (fallbackErr: Exception) {
                Log.e(TAG, "Fallback Gemini call failed", fallbackErr)
                throw (primaryStreamError ?: fallbackErr)
            }
        }

        if (finalText.isBlank()) {
            if (primaryStreamError != null) {
                throw primaryStreamError
            }
            emit(ChefChatStreamEvent.Failed(loc.getString(R.string.chef_chat_empty_response)))
        } else {
            emit(ChefChatStreamEvent.Completed(finalText))
        }
    }.catch { err ->
        // IMPORTANT: TimeoutCancellationException extends CancellationException — check timeout FIRST!
        val loc = LocaleHelper.localizedContext(context)
        when (err) {
            is TimeoutCancellationException -> {
                Log.e(TAG, "Chef AI chat timed out (${role.modelName})", err)
                emit(ChefChatStreamEvent.Failed(loc.getString(R.string.chef_chat_timeout)))
            }
            is CancellationException -> throw err
            else -> {
                Log.e(TAG, "Chef AI chat failed (${role.modelName})", err)
                emit(ChefChatStreamEvent.Failed(classifyGeminiError(loc, err)))
            }
        }
    }.flowOn(Dispatchers.IO)
}
