package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.PantryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppShortcutsUpdater {

    const val EXTRA_OPEN_TAB = "extra_open_tab"

    suspend fun notifyPantryUpdated(
        context: Context,
        pantryItems: List<PantryItem>
    ) = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        try {
            updateDynamicShortcuts(appContext, pantryItems)
        } catch (_: Exception) {
        }
    }

    suspend fun clearShortcuts(context: Context) = withContext(Dispatchers.IO) {
        try {
            ShortcutManagerCompat.removeAllDynamicShortcuts(context.applicationContext)
        } catch (_: Exception) {
        }
    }

    private fun updateDynamicShortcuts(
        context: Context,
        pantryItems: List<PantryItem>
    ) {
        val loc = LocaleHelper.localizedContext(context)
        val lowOrEmpty = pantryItems.filter { it.isOutOfStock || it.isLowStock }
        val summarySubtitle = if (pantryItems.isEmpty()) {
            loc.getString(R.string.shortcut_dyn_empty)
        } else {
            loc.getString(R.string.shortcut_dyn_summary, pantryItems.size, lowOrEmpty.size)
        }

        val checkPantryIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_OPEN_TAB, 1)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val checkShoppingIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_OPEN_TAB, 2)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val cookingVoiceIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_OPEN_TAB, 4)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val shortcuts = mutableListOf(
            ShortcutInfoCompat.Builder(context, "shortcut_pantry_status")
                .setShortLabel(loc.getString(R.string.shortcut_dyn_pantry_short))
                .setLongLabel(loc.getString(R.string.shortcut_dyn_pantry_long, summarySubtitle))
                .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                .setIntent(checkPantryIntent)
                .build(),
            ShortcutInfoCompat.Builder(context, "shortcut_shopping_list")
                .setShortLabel(loc.getString(R.string.shortcut_dyn_shop_short))
                .setLongLabel(loc.getString(R.string.shortcut_dyn_shop_long))
                .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                .setIntent(checkShoppingIntent)
                .build(),
            ShortcutInfoCompat.Builder(context, "shortcut_cooking_voice")
                .setShortLabel(loc.getString(R.string.shortcut_dyn_voice_short))
                .setLongLabel(loc.getString(R.string.shortcut_dyn_voice_long))
                .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                .setIntent(cookingVoiceIntent)
                .build()
        )

        if (lowOrEmpty.isNotEmpty()) {
            val firstAlert = lowOrEmpty.first()
            val qtyStr = if (firstAlert.quantity % 1.0 == 0.0) {
                firstAlert.quantity.toInt().toString()
            } else {
                "%.1f".format(firstAlert.quantity)
            }
            val unitDisp = LocalizedLabels.unit(loc, firstAlert.unit)
            shortcuts.add(
                ShortcutInfoCompat.Builder(context, "shortcut_low_stock_alert")
                    .setShortLabel(loc.getString(R.string.shortcut_dyn_low_short, firstAlert.name))
                    .setLongLabel("${firstAlert.name}: $qtyStr $unitDisp")
                    .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                    .setIntent(checkPantryIntent)
                    .build()
            )
        }

        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts.take(4))
    }
}
