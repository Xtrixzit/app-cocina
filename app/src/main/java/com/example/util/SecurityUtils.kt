package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.io.File
import java.net.URI

/**
 * Centralized input validation, output encoding, URL allowlisting, path confinement,
 * and prompt-framing sanitization helpers.
 */
object SecurityUtils {

    private val VALID_DOC_ID_REGEX = Regex("^[a-zA-Z0-9_\\-]{1,128}$")
    private val VALID_RES_IMAGE_REGEX = Regex("^res://[a-zA-Z0-9_]{1,100}$")
    private val CONTROL_CHARS_REGEX = Regex("[\\p{Cntrl}&&[^\r\n\t]]|[\\u200B-\\u200D\\uFEFF]")
    private val MULTISPACE_REGEX = Regex("\\s+")

    const val MAX_NUMERIC_VALUE = 1_000_000.0
    const val MAX_PRODUCTION_CURRENCY_VALUE = 100_000_000.0
    const val MAX_MARGIN_PERCENT = 500
    const val MAX_IMAGE_DATA_URI_LENGTH = 80_000
    const val MAX_URL_LENGTH = 2048

    fun isValidDocId(id: String): Boolean {
        return VALID_DOC_ID_REGEX.matches(id.trim())
    }

    fun clampNonNegative(value: Double, max: Double = MAX_NUMERIC_VALUE): Double {
        if (!value.isFinite()) return 0.0
        return value.coerceIn(0.0, max)
    }

    fun clampPositive(value: Double, min: Double = 0.0001, max: Double = MAX_NUMERIC_VALUE, fallback: Double = min): Double {
        if (!value.isFinite() || value <= 0.0) return fallback
        return value.coerceIn(min, max)
    }

    fun clampMarginPercent(margin: Int): Int {
        return margin.coerceIn(0, MAX_MARGIN_PERCENT)
    }

    /**
     * Allows only HTTPS URLs with a non-empty host and no embedded whitespace or control chars.
     */
    fun isSafeHttpsUrl(rawUrl: String): Boolean {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty() || trimmed.length > MAX_URL_LENGTH) return false
        if (trimmed.any { it.isWhitespace() || it.isISOControl() }) return false
        return try {
            val uri = URI(trimmed)
            val scheme = uri.scheme?.lowercase()
            val host = uri.host
            scheme == "https" && !host.isNullOrBlank() && host.contains('.')
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Safely opens an external browser only when [rawUrl] passes strict HTTPS validation.
     */
    fun safeOpenHttpsUrl(context: Context, rawUrl: String): Boolean {
        val trimmed = rawUrl.trim()
        if (!isSafeHttpsUrl(trimmed)) return false
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(trimmed)).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Sanitizes persisted recipe `imageUrl` values.
     * Permits bundled referential keys (`res://...`), compact `data:image/jpeg;base64,...` data URIs,
     * system Photo Picker / FileProvider `content://` URIs, and strict `https://` URLs.
     * Blocks `http://`, `javascript:`, `file://`, and malformed schemes.
     */
    fun sanitizeImageUri(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return ""
        return when {
            VALID_RES_IMAGE_REGEX.matches(trimmed) -> trimmed
            trimmed.startsWith("data:image/", ignoreCase = true) && trimmed.contains("base64,", ignoreCase = true) -> {
                if (trimmed.length <= MAX_IMAGE_DATA_URI_LENGTH) trimmed else ""
            }
            trimmed.startsWith("content://", ignoreCase = true) -> {
                if (trimmed.length <= MAX_URL_LENGTH && !trimmed.any { it.isISOControl() }) trimmed else ""
            }
            isSafeHttpsUrl(trimmed) -> trimmed
            else -> ""
        }
    }

    /**
     * Prevents CSV spreadsheet formula injection by neutralizing leading formula triggers
     * (`=`, `+`, `-`, `@`, `\t`, `\r`) and applying RFC-4180 quoting.
     */
    fun sanitizeCsvCell(raw: String): String {
        val cleaned = raw.replace("\r\n", " ").replace("\n", " ").replace("\r", " ")
        val trimmedStart = cleaned.trimStart()
        val firstChar = trimmedStart.firstOrNull()
        val safeValue = if (firstChar == '=' || firstChar == '+' || firstChar == '-' || firstChar == '@' || firstChar == '\t') {
            "'$cleaned"
        } else {
            cleaned
        }
        return if (safeValue.contains(',') || safeValue.contains('"') || safeValue.contains(';') || safeValue != cleaned) {
            "\"${safeValue.replace("\"", "\"\"")}\""
        } else {
            safeValue
        }
    }

    /**
     * Sanitizes user-entered or stored data embedded into AI prompts to prevent delimiter breakout
     * or instruction spoofing inside untrusted data blocks.
     */
    fun sanitizePromptInput(raw: String, maxLength: Int = 500): String {
        if (raw.isBlank()) return ""
        val withoutControls = CONTROL_CHARS_REGEX.replace(raw, "")
        val safeDelimiters = withoutControls
            .replace('\r', ' ')
            .replace('\n', ' ')
            .replace('\t', ' ')
            .replace('<', '(')
            .replace('>', ')')
            .replace('[', '(')
            .replace(']', ')')
        return MULTISPACE_REGEX.replace(safeDelimiters, " ").trim().take(maxLength)
    }

    /**
     * Resolves an export filename strictly inside [baseDir], throwing [SecurityException] if any
     * path traversal is detected.
     */
    fun resolveConfinedFile(baseDir: File, requestedName: String): File {
        val baseOnly = File(requestedName).name
        val safeName = baseOnly.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "export.txt" }.take(120)
        val candidate = File(baseDir, safeName)
        val canonicalBase = baseDir.canonicalPath
        val canonicalTarget = candidate.canonicalPath
        if (canonicalTarget != canonicalBase && !canonicalTarget.startsWith(canonicalBase + File.separator)) {
            throw SecurityException("Blocked path traversal outside export cache")
        }
        return candidate
    }
}
