package au.edu.jcu.assessment.utilityapp.data

import android.content.SharedPreferences
import androidx.core.content.edit
import au.edu.jcu.assessment.utilityapp.domain.AppSettings
import au.edu.jcu.assessment.utilityapp.domain.AppTheme
import au.edu.jcu.assessment.utilityapp.domain.City
import au.edu.jcu.assessment.utilityapp.domain.TemperatureUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface SettingsRepository {
    val settings: StateFlow<AppSettings>
    fun update(settings: AppSettings)
}

class PreferenceSettingsRepository(private val preferences: SharedPreferences) : SettingsRepository {
    private val mutableSettings = MutableStateFlow(readSettings())
    override val settings: StateFlow<AppSettings> = mutableSettings.asStateFlow()

    override fun update(settings: AppSettings) {
        preferences.edit {
            putString("city", settings.city.name)
            putInt("walk_minutes", settings.walkMinutes)
            putInt("max_rain", settings.maxRainPercent)
            putString("temperature_unit", settings.temperatureUnit.name)
            putString("theme", settings.theme.name)
        }
        mutableSettings.value = settings
    }

    private fun readSettings(): AppSettings = runCatching {
        AppSettings(
            city = City.entries.firstOrNull { it.name == preferences.getString("city", null) } ?: City.TOWNSVILLE,
            walkMinutes = preferences.getInt("walk_minutes", 30).takeIf { it in listOf(30, 60, 90) } ?: 30,
            maxRainPercent = preferences.getInt("max_rain", 40).takeIf { it in listOf(20, 40, 60) } ?: 40,
            temperatureUnit = TemperatureUnit.entries.firstOrNull {
                it.name == preferences.getString("temperature_unit", null)
            } ?: TemperatureUnit.CELSIUS,
            theme = AppTheme.entries.firstOrNull { it.name == preferences.getString("theme", null) } ?: AppTheme.SYSTEM
        )
    }.getOrDefault(AppSettings())
}
