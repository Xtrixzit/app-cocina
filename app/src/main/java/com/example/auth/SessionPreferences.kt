package com.example.auth

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SessionPreferences {
    private const val PREFS_NAME = "recetario_session_prefs"
    private const val KEY_GUEST_MODE = "guest_mode_enabled"

    private val _guestModeFlow = MutableStateFlow(false)
    private var initialized = false

    fun observeGuestMode(context: Context): StateFlow<Boolean> {
        ensureInitialized(context)
        return _guestModeFlow.asStateFlow()
    }

    fun isGuestMode(context: Context): Boolean {
        ensureInitialized(context)
        return _guestModeFlow.value
    }

    fun setGuestMode(context: Context, enabled: Boolean) {
        val prefs = (context.applicationContext ?: context).getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_GUEST_MODE, enabled).commit()
        initialized = true
        _guestModeFlow.value = enabled
    }

    private fun ensureInitialized(context: Context) {
        if (!initialized) {
            synchronized(this) {
                if (!initialized) {
                    val prefs = (context.applicationContext ?: context).getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    _guestModeFlow.value = prefs.getBoolean(KEY_GUEST_MODE, false)
                    initialized = true
                }
            }
        }
    }
}
