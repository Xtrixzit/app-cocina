package com.example.ai

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.settings.AppSettingsPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Lightweight Android [TextToSpeech] wrapper that reads voice answers aloud
 * in the active app language (`es`, `en`, `pt`, `fr`) with zero dangerous permissions.
 */
class VoiceSpeechPlayer(context: Context) {

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var isReady = false
    private var pendingText: String? = null

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private fun ensureInitialized() {
        if (tts != null) return
        try {
            tts = TextToSpeech(appContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isReady = true
                    applyActiveLocale()
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _isSpeaking.value = true
                        }

                        override fun onDone(utteranceId: String?) {
                            _isSpeaking.value = false
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            _isSpeaking.value = false
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            _isSpeaking.value = false
                        }
                    })
                    val queued = pendingText
                    pendingText = null
                    if (!queued.isNullOrBlank()) {
                        speakInternal(queued)
                    }
                } else {
                    isReady = false
                    pendingText = null
                }
            }
        } catch (_: Exception) {
            isReady = false
            pendingText = null
        }
    }

    private fun applyActiveLocale() {
        val code = AppSettingsPreferences.getLanguageCode(appContext)
        val locale = when (code) {
            "es" -> Locale("es", "ES")
            "en" -> Locale.US
            "pt" -> Locale("pt", "BR")
            "fr" -> Locale.FRENCH
            else -> Locale.getDefault()
        }
        try {
            tts?.language = locale
        } catch (_: Exception) {
        }
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        ensureInitialized()
        if (isReady) {
            applyActiveLocale()
            speakInternal(text)
        } else {
            pendingText = text
        }
    }

    private fun speakInternal(text: String) {
        try {
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "recetario_voice_answer")
            }
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "recetario_voice_answer")
        } catch (_: Exception) {
            _isSpeaking.value = false
        }
    }

    fun stop() {
        pendingText = null
        _isSpeaking.value = false
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
    }

    fun release() {
        stop()
        try {
            tts?.shutdown()
        } catch (_: Exception) {
        }
        tts = null
        isReady = false
    }
}
