package com.example.data.sync

import android.content.Context
import android.content.Intent
import android.util.Base64
import com.example.data.model.PantryItem
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
     * Generates a beautifully formatted human-readable summary for WhatsApp or messaging.
     */
    fun createReadableShareText(items: List<PantryItem>): String {
        val sb = StringBuilder()
        sb.append("📋 *Inventario de Despensa - Recetario*\n")
        sb.append("Actualizado: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}\n\n")

        val grouped = items.groupBy { it.category }
        for ((category, catItems) in grouped) {
            sb.append("🔹 *${category.uppercase()}*\n")
            for (item in catItems) {
                val formattedQty = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else "%.1f".format(item.quantity)
                sb.append("  • ${item.name}: $formattedQty ${item.unit}\n")
            }
            sb.append("\n")
        }

        sb.append("---\n")
        sb.append("📲 *Código para sincronizar en tu app:*\n")
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
        val shareText = createReadableShareText(items)
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Compartir inventario de despensa con otro dispositivo")
        context.startActivity(shareIntent)
    }
}
