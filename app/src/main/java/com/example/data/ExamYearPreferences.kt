package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.examYearDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "upsc_exam_year_preferences"
)

enum class AppThemeMode(val label: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System")
}

data class ExamYearConfig(
    val latestPrelimsYear: Int = DEFAULT_EXAM_YEAR,
    val latestMainsYear: Int = DEFAULT_EXAM_YEAR
) {
    companion object {
        const val DEFAULT_EXAM_YEAR = 2026
        const val MIN_EXAM_YEAR = 2013
        const val MAX_EXAM_YEAR = 2040
    }
}

data class AppSettingsConfig(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val useCustomSkill: Boolean = false,
    val customSkillText: String = ""
)

class ExamYearPreferences(private val context: Context) {

    private val prelimsYearKey = intPreferencesKey("latest_prelims_year")
    private val mainsYearKey = intPreferencesKey("latest_mains_year")
    private val themeModeKey = stringPreferencesKey("app_theme_mode")
    private val useCustomSkillKey = booleanPreferencesKey("use_custom_skill")
    private val customSkillTextKey = stringPreferencesKey("custom_skill_text")

    val examYearConfigFlow: Flow<ExamYearConfig> = context.examYearDataStore.data.map { prefs ->
        ExamYearConfig(
            latestPrelimsYear = prefs[prelimsYearKey]
                ?.coerceIn(ExamYearConfig.MIN_EXAM_YEAR, ExamYearConfig.MAX_EXAM_YEAR)
                ?: ExamYearConfig.DEFAULT_EXAM_YEAR,
            latestMainsYear = prefs[mainsYearKey]
                ?.coerceIn(ExamYearConfig.MIN_EXAM_YEAR, ExamYearConfig.MAX_EXAM_YEAR)
                ?: ExamYearConfig.DEFAULT_EXAM_YEAR
        )
    }

    val appSettingsFlow: Flow<AppSettingsConfig> = context.examYearDataStore.data.map { prefs ->
        val rawTheme = prefs[themeModeKey]
        val parsedTheme = AppThemeMode.entries.firstOrNull { it.name == rawTheme } ?: AppThemeMode.SYSTEM
        AppSettingsConfig(
            themeMode = parsedTheme,
            useCustomSkill = prefs[useCustomSkillKey] ?: false,
            customSkillText = prefs[customSkillTextKey] ?: ""
        )
    }

    suspend fun setPrelimsYear(year: Int) {
        val safeYear = year.coerceIn(ExamYearConfig.MIN_EXAM_YEAR, ExamYearConfig.MAX_EXAM_YEAR)
        context.examYearDataStore.edit { prefs ->
            prefs[prelimsYearKey] = safeYear
        }
    }

    suspend fun setMainsYear(year: Int) {
        val safeYear = year.coerceIn(ExamYearConfig.MIN_EXAM_YEAR, ExamYearConfig.MAX_EXAM_YEAR)
        context.examYearDataStore.edit { prefs ->
            prefs[mainsYearKey] = safeYear
        }
    }

    suspend fun setBothYears(prelimsYear: Int, mainsYear: Int) {
        val safePrelims = prelimsYear.coerceIn(ExamYearConfig.MIN_EXAM_YEAR, ExamYearConfig.MAX_EXAM_YEAR)
        val safeMains = mainsYear.coerceIn(ExamYearConfig.MIN_EXAM_YEAR, ExamYearConfig.MAX_EXAM_YEAR)
        context.examYearDataStore.edit { prefs ->
            prefs[prelimsYearKey] = safePrelims
            prefs[mainsYearKey] = safeMains
        }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.examYearDataStore.edit { prefs ->
            prefs[themeModeKey] = mode.name
        }
    }

    suspend fun setUseCustomSkill(enabled: Boolean) {
        context.examYearDataStore.edit { prefs ->
            prefs[useCustomSkillKey] = enabled
        }
    }

    suspend fun setCustomSkillText(skillText: String) {
        context.examYearDataStore.edit { prefs ->
            prefs[customSkillTextKey] = skillText.trim()
        }
    }
}
