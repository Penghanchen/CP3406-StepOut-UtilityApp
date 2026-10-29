package au.edu.jcu.assessment.utilityapp.domain

import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt

enum class City(val label: String, val latitude: Double, val longitude: Double) {
    TOWNSVILLE("Townsville", -19.2590, 146.8169),
    CAIRNS("Cairns", -16.9186, 145.7781),
    BRISBANE("Brisbane", -27.4698, 153.0251),
    SYDNEY("Sydney", -33.8688, 151.2093),
    SINGAPORE("Singapore", 1.3521, 103.8198)
}

enum class TemperatureUnit(val label: String) {
    CELSIUS("Celsius"), FAHRENHEIT("Fahrenheit");

    fun format(celsius: Double): String {
        val value = if (this == CELSIUS) celsius else celsius * 9 / 5 + 32
        return String.format(Locale.ENGLISH, "%d°%s", value.roundToInt(), if (this == CELSIUS) "C" else "F")
    }
}

enum class AppTheme(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }

data class AppSettings(
    val city: City = City.TOWNSVILLE,
    val walkMinutes: Int = 30,
    val maxRainPercent: Int = 40,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val theme: AppTheme = AppTheme.SYSTEM
) {
    init {
        require(walkMinutes in listOf(30, 60, 90))
        require(maxRainPercent in listOf(20, 40, 60))
    }
}

/** Rain applies to [time, time + one hour); other values are sampled at time. */
data class ForecastHour(
    val time: Instant,
    val temperatureCelsius: Double,
    val rainPercent: Int?,
    val weatherCode: Int,
    val isDay: Boolean
)

data class Forecast(
    val city: City,
    val zoneId: ZoneId,
    val observedAt: Instant,
    val fetchedAt: Instant,
    val temperatureCelsius: Double,
    val weatherCode: Int,
    val isDay: Boolean,
    val hours: List<ForecastHour>
)

data class WalkWindow(val start: Instant, val end: Instant, val maxRainPercent: Int)

fun weatherDescription(code: Int, isDay: Boolean = true): String = when (code) {
    0 -> if (isDay) "Clear sky" else "Clear night"
    1 -> "Mainly clear"
    2 -> "Partly cloudy"
    3 -> "Overcast"
    45, 48 -> "Fog"
    51, 53, 55 -> "Drizzle"
    56, 57 -> "Freezing drizzle"
    61, 63, 65 -> "Rain"
    66, 67 -> "Freezing rain"
    71, 73, 75, 77 -> "Snow"
    80, 81, 82 -> "Rain showers"
    85, 86 -> "Snow showers"
    95 -> "Thunderstorm"
    96, 99 -> "Thunderstorm with hail"
    else -> "Conditions unavailable"
}
