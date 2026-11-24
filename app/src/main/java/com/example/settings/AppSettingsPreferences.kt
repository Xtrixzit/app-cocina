package com.example.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import kotlin.math.abs

enum class AppLanguage(val code: String, val nativeName: String) {
    SYSTEM("", "System"),
    SPANISH("es", "Español"),
    ENGLISH("en", "English"),
    PORTUGUESE("pt", "Português"),
    FRENCH("fr", "Français");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code.equals(code.orEmpty(), ignoreCase = true) } ?: SYSTEM
    }
}

enum class CurrencyOption(val code: String, val symbol: String, val displayLabel: String) {
    USD("USD", "$", "USD ($)"),
    EUR("EUR", "€", "EUR (€)"),
    MXN("MXN", "MX$", "MXN (MX$)"),
    COP("COP", "COP$", "COP ($)"),
    CLP("CLP", "CLP$", "CLP ($)"),
    ARS("ARS", "ARS$", "ARS ($)"),
    PEN("PEN", "S/", "PEN (S/)"),
    BRL("BRL", "R$", "BRL (R$)"),
    GBP("GBP", "£", "GBP (£)");

    companion object {
        fun fromCode(code: String?): CurrencyOption =
            entries.firstOrNull { it.code.equals(code.orEmpty(), ignoreCase = true) } ?: USD
    }
}

data class PersistedTimerState(
    val endTimeMillis: Long = 0L,
    val durationSeconds: Int = 0,
    val pausedRemainingSeconds: Int = 0,
    val label: String = "",
    val isRunning: Boolean = false
)

data class DriveSyncInfo(
    val autoSyncEnabled: Boolean = true,
    val lastSyncMillis: Long = 0L,
    val folderId: String = "",
    val docFileId: String = "",
    val docWebLink: String = "",
    val sheetFileId: String = "",
    val sheetWebLink: String = "",
    val markdownFileId: String = "",
    val markdownWebLink: String = "",
    val linkedEmail: String = ""
)

object AppSettingsPreferences {
    const val PREFS_NAME = "recetario_settings"
    private const val KEY_CURRENCY = "currency_code"
    private const val KEY_MARGIN = "profit_margin_percent"
    private const val KEY_LANGUAGE = "app_language"
    private const val KEY_LICENSE_KEY = "license_key"
    private const val KEY_TIMER_END = "timer_end_millis"
    private const val KEY_TIMER_DURATION = "timer_duration_sec"
    private const val KEY_TIMER_PAUSED_REM = "timer_paused_rem_sec"
    private const val KEY_TIMER_LABEL = "timer_label"
    private const val KEY_TIMER_RUNNING = "timer_running"
    private const val KEY_DRIVE_AUTO_SYNC = "drive_auto_sync"
    private const val KEY_DRIVE_LAST_SYNC = "drive_last_sync_millis"
    private const val KEY_DRIVE_FOLDER_ID = "drive_folder_id"
    private const val KEY_DRIVE_DOC_ID = "drive_doc_id"
    private const val KEY_DRIVE_DOC_LINK = "drive_doc_link"
    private const val KEY_DRIVE_SHEET_ID = "drive_sheet_id"
    private const val KEY_DRIVE_SHEET_LINK = "drive_sheet_link"
    private const val KEY_DRIVE_MD_ID = "drive_md_id"
    private const val KEY_DRIVE_MD_LINK = "drive_md_link"
    private const val KEY_DRIVE_EMAIL = "drive_linked_email"
    private const val KEY_GUIDED_TUTORIAL_DONE = "guided_tutorial_completed"
    private const val KEY_GITHUB_REPO = "github_update_repo"

    private val _currencyFlow = MutableStateFlow(CurrencyOption.USD)
    private val _marginFlow = MutableStateFlow(30)
    private val _languageFlow = MutableStateFlow(AppLanguage.SYSTEM)
    private val _licenseKeyFlow = MutableStateFlow("")
    private val _timerFlow = MutableStateFlow(PersistedTimerState())
    private val _driveSyncFlow = MutableStateFlow(DriveSyncInfo())
    private val _guidedTutorialCompletedFlow = MutableStateFlow(false)
    private val _githubRepoFlow = MutableStateFlow("")

    @Volatile
    private var initialized = false

    private fun prefs(context: Context): SharedPreferences =
        (context.applicationContext ?: context).getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun ensureLoaded(context: Context) {
        if (!initialized) {
            synchronized(this) {
                if (!initialized) {
                    val p = prefs(context)
                    _currencyFlow.value = CurrencyOption.fromCode(p.getString(KEY_CURRENCY, CurrencyOption.USD.code))
                    _marginFlow.value = p.getInt(KEY_MARGIN, 30).coerceIn(0, 500)
                    _languageFlow.value = AppLanguage.fromCode(p.getString(KEY_LANGUAGE, ""))
                    _licenseKeyFlow.value = p.getString(KEY_LICENSE_KEY, "") ?: ""
                    _timerFlow.value = readTimerState(p)
                    _driveSyncFlow.value = readDriveSyncInfo(p)
                    _guidedTutorialCompletedFlow.value = p.getBoolean(KEY_GUIDED_TUTORIAL_DONE, false)
                    _githubRepoFlow.value = p.getString(KEY_GITHUB_REPO, "") ?: ""
                    initialized = true
                }
            }
        }
    }

    private fun readDriveSyncInfo(p: SharedPreferences): DriveSyncInfo {
        return DriveSyncInfo(
            autoSyncEnabled = p.getBoolean(KEY_DRIVE_AUTO_SYNC, true),
            lastSyncMillis = p.getLong(KEY_DRIVE_LAST_SYNC, 0L),
            folderId = p.getString(KEY_DRIVE_FOLDER_ID, "") ?: "",
            docFileId = p.getString(KEY_DRIVE_DOC_ID, "") ?: "",
            docWebLink = p.getString(KEY_DRIVE_DOC_LINK, "") ?: "",
            sheetFileId = p.getString(KEY_DRIVE_SHEET_ID, "") ?: "",
            sheetWebLink = p.getString(KEY_DRIVE_SHEET_LINK, "") ?: "",
            markdownFileId = p.getString(KEY_DRIVE_MD_ID, "") ?: "",
            markdownWebLink = p.getString(KEY_DRIVE_MD_LINK, "") ?: "",
            linkedEmail = p.getString(KEY_DRIVE_EMAIL, "") ?: ""
        )
    }

    private fun readTimerState(p: SharedPreferences): PersistedTimerState {
        return PersistedTimerState(
            endTimeMillis = p.getLong(KEY_TIMER_END, 0L),
            durationSeconds = p.getInt(KEY_TIMER_DURATION, 0),
            pausedRemainingSeconds = p.getInt(KEY_TIMER_PAUSED_REM, 0),
            label = p.getString(KEY_TIMER_LABEL, "") ?: "",
            isRunning = p.getBoolean(KEY_TIMER_RUNNING, false)
        )
    }

    fun observeCurrency(context: Context): StateFlow<CurrencyOption> {
        ensureLoaded(context)
        return _currencyFlow.asStateFlow()
    }

    fun getCurrency(context: Context): CurrencyOption {
        ensureLoaded(context)
        return _currencyFlow.value
    }

    fun setCurrency(context: Context, option: CurrencyOption) {
        ensureLoaded(context)
        prefs(context).edit().putString(KEY_CURRENCY, option.code).apply()
        _currencyFlow.value = option
    }

    fun observeMarginPercent(context: Context): StateFlow<Int> {
        ensureLoaded(context)
        return _marginFlow.asStateFlow()
    }

    fun getMarginPercent(context: Context): Int {
        ensureLoaded(context)
        return _marginFlow.value
    }

    fun setMarginPercent(context: Context, percent: Int) {
        ensureLoaded(context)
        val clamped = percent.coerceIn(0, 500)
        prefs(context).edit().putInt(KEY_MARGIN, clamped).apply()
        _marginFlow.value = clamped
    }

    fun observeLanguage(context: Context): StateFlow<AppLanguage> {
        ensureLoaded(context)
        return _languageFlow.asStateFlow()
    }

    fun getLanguageCode(context: Context): String {
        val p = prefs(context)
        val code = p.getString(KEY_LANGUAGE, "") ?: ""
        return code
    }

    fun setLanguageCode(context: Context, code: String) {
        val lang = AppLanguage.fromCode(code)
        prefs(context).edit().putString(KEY_LANGUAGE, lang.code).commit()
        _languageFlow.value = lang
    }

    fun observeLicenseKey(context: Context): StateFlow<String> {
        ensureLoaded(context)
        return _licenseKeyFlow.asStateFlow()
    }

    fun getLicenseKey(context: Context): String {
        ensureLoaded(context)
        return _licenseKeyFlow.value
    }

    fun setLicenseKey(context: Context, licenseKey: String) {
        ensureLoaded(context)
        val clean = licenseKey.trim()
        prefs(context).edit().putString(KEY_LICENSE_KEY, clean).apply()
        _licenseKeyFlow.value = clean
    }

    fun observeTimerState(context: Context): StateFlow<PersistedTimerState> {
        ensureLoaded(context)
        return _timerFlow.asStateFlow()
    }

    fun getTimerState(context: Context): PersistedTimerState {
        ensureLoaded(context)
        return _timerFlow.value
    }

    fun saveTimerState(context: Context, state: PersistedTimerState) {
        ensureLoaded(context)
        prefs(context).edit()
            .putLong(KEY_TIMER_END, state.endTimeMillis)
            .putInt(KEY_TIMER_DURATION, state.durationSeconds)
            .putInt(KEY_TIMER_PAUSED_REM, state.pausedRemainingSeconds)
            .putString(KEY_TIMER_LABEL, state.label)
            .putBoolean(KEY_TIMER_RUNNING, state.isRunning)
            .apply()
        _timerFlow.value = state
    }

    fun clearTimerState(context: Context) {
        saveTimerState(context, PersistedTimerState())
    }

    fun observeDriveSyncInfo(context: Context): StateFlow<DriveSyncInfo> {
        ensureLoaded(context)
        return _driveSyncFlow.asStateFlow()
    }

    fun getDriveSyncInfo(context: Context): DriveSyncInfo {
        ensureLoaded(context)
        return _driveSyncFlow.value
    }

    fun setDriveAutoSyncEnabled(context: Context, enabled: Boolean) {
        ensureLoaded(context)
        prefs(context).edit().putBoolean(KEY_DRIVE_AUTO_SYNC, enabled).apply()
        _driveSyncFlow.value = _driveSyncFlow.value.copy(autoSyncEnabled = enabled)
    }

    fun saveDriveSyncInfo(context: Context, info: DriveSyncInfo) {
        ensureLoaded(context)
        prefs(context).edit()
            .putBoolean(KEY_DRIVE_AUTO_SYNC, info.autoSyncEnabled)
            .putLong(KEY_DRIVE_LAST_SYNC, info.lastSyncMillis)
            .putString(KEY_DRIVE_FOLDER_ID, info.folderId)
            .putString(KEY_DRIVE_DOC_ID, info.docFileId)
            .putString(KEY_DRIVE_DOC_LINK, info.docWebLink)
            .putString(KEY_DRIVE_SHEET_ID, info.sheetFileId)
            .putString(KEY_DRIVE_SHEET_LINK, info.sheetWebLink)
            .putString(KEY_DRIVE_MD_ID, info.markdownFileId)
            .putString(KEY_DRIVE_MD_LINK, info.markdownWebLink)
            .putString(KEY_DRIVE_EMAIL, info.linkedEmail)
            .apply()
        _driveSyncFlow.value = info
    }

    fun clearDriveSyncInfo(context: Context) {
        saveDriveSyncInfo(context, DriveSyncInfo())
    }

    fun observeGuidedTutorialCompleted(context: Context): StateFlow<Boolean> {
        ensureLoaded(context)
        return _guidedTutorialCompletedFlow.asStateFlow()
    }

    fun isGuidedTutorialCompleted(context: Context): Boolean {
        ensureLoaded(context)
        return _guidedTutorialCompletedFlow.value
    }

    fun setGuidedTutorialCompleted(context: Context, completed: Boolean) {
        ensureLoaded(context)
        prefs(context).edit().putBoolean(KEY_GUIDED_TUTORIAL_DONE, completed).commit()
        _guidedTutorialCompletedFlow.value = completed
    }

    fun observeGithubRepo(context: Context): StateFlow<String> {
        ensureLoaded(context)
        return _githubRepoFlow.asStateFlow()
    }

    fun getGithubRepo(context: Context): String {
        ensureLoaded(context)
        return _githubRepoFlow.value
    }

    fun setGithubRepo(context: Context, rawRepo: String) {
        ensureLoaded(context)
        val clean = rawRepo.trim()
        prefs(context).edit().putString(KEY_GITHUB_REPO, clean).apply()
        _githubRepoFlow.value = clean
    }

    fun suggestedSellingPrice(cost: Double, marginPercent: Int): Double {
        if (cost <= 0.0) return 0.0
        return cost * (1.0 + marginPercent.coerceIn(0, 500) / 100.0)
    }

    fun estimatedProfit(cost: Double, marginPercent: Int): Double {
        if (cost <= 0.0) return 0.0
        return suggestedSellingPrice(cost, marginPercent) - cost
    }

    fun formatMoney(amount: Double, currency: CurrencyOption, forceFourDecimalsIfTiny: Boolean = false): String {
        val absVal = abs(amount)
        val pattern = if (forceFourDecimalsIfTiny && absVal > 0.0 && absVal < 0.01) "%.4f" else "%.2f"
        return "${currency.symbol}${String.format(Locale.US, pattern, amount)}"
    }
}
