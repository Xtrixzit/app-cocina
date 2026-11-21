package com.example.data.sync

import android.content.Context
import android.content.Intent
import android.util.Base64
import com.example.R
import com.example.data.model.PantryItem
import com.example.util.LocaleHelper
import com.example.util.LocalizedLabels
import org.json.JSONArray
import org.json.JSONObject

object InventorySyncManager {

    private const val SYNC_HEADER = "RECETARIO_SYNC_V1:"

    /**
     * Serializes pantry items into a compact sync token string that can be shared via WhatsApp, SMS, or clipboard.
     */
    fun createSyncToken(items: List<PantryItem>): String {
        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("name", item.name)
                put("category", item.category)
                put("quantity", item.quantity)
                put("unit", item.unit)
                put("minQuantity", item.minQuantity)
                put("expirationDate", item.expirationDate)
            }
            array.put(obj)
        }
        val rawJson = array.toString()
        val base64 = Base64.encodeToString(rawJson.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        return "$SYNC_HEADER$base64"
    }

    /**
     * Generates a formatted human-readable summary for messaging apps.
     */
    fun createReadableShareText(context: Context, items: List<PantryItem>): String {
        val loc = LocaleHelper.localizedContext(context)
        val sb = StringBuilder()
        sb.append("📋 *${loc.getString(R.string.pantry_header)} - ${loc.getString(R.string.app_name)}*\n")
        sb.append("${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}\n\n")

        val grouped = items.groupBy { it.category }
        for ((category, catItems) in grouped) {
            val catDisp = LocalizedLabels.pantryCategory(loc, category)
            sb.append("🔹 *${catDisp.uppercase()}*\n")
            for (item in catItems) {
                val formattedQty = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else "%.1f".format(item.quantity)
                val uDisp = LocalizedLabels.unit(loc, item.unit)
                sb.append("  • ${item.name}: $formattedQty $uDisp\n")
            }
            sb.append("\n")
        }

        sb.append("---\n")
        sb.append("${loc.getString(R.string.sync_copy_token_btn)}:\n")
        sb.append(createSyncToken(items))
        return sb.toString()
    }

    /**
     * Parses a sync token string or JSON string from another device.
     */
    fun parseSyncToken(input: String): List<PantryItem>? {
        val trimmed = input.trim()
        val jsonString = if (trimmed.contains(SYNC_HEADER)) {
            val tokenPart = trimmed.substringAfter(SYNC_HEADER).trim().takeWhile { !it.isWhitespace() }
            try {
                String(Base64.decode(tokenPart, Base64.NO_WRAP), Charsets.UTF_8)
            } catch (e: Exception) {
                return null
            }
        } else if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            trimmed
        } else {
            return null
        }

        return try {
            val array = JSONArray(jsonString)
            val result = mutableListOf<PantryItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(
                    PantryItem(
                        id = "imported_${java.util.UUID.randomUUID().toString().take(8)}",
                        name = obj.getString("name"),
                        category = obj.optString("category", "Despensa"),
                        quantity = obj.optDouble("quantity", 0.0),
                        unit = obj.optString("unit", "g"),
                        minQuantity = obj.optDouble("minQuantity", 0.0),
                        expirationDate = obj.optString("expirationDate", "")
                    )
                )
            }
            result
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Opens Android's native share sheet to send the inventory to another device or chat.
     */
    fun shareInventory(context: Context, items: List<PantryItem>) {
        val loc = LocaleHelper.localizedContext(context)
        val shareText = createReadableShareText(context, items)
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, loc.getString(R.string.sync_share_pantry_btn))
        context.startActivity(shareIntent)
    }
}
