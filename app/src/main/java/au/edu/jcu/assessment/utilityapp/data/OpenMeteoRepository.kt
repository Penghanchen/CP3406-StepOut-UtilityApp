package au.edu.jcu.assessment.utilityapp.data

import au.edu.jcu.assessment.utilityapp.domain.City
import au.edu.jcu.assessment.utilityapp.domain.Forecast
import au.edu.jcu.assessment.utilityapp.domain.ForecastHour
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class InvalidForecastException : Exception("The weather service returned incomplete data.")

class OpenMeteoRepository(
    private val api: WeatherApi,
    private val clock: Clock = Clock.systemUTC()
) : ForecastRepository {
    override suspend fun fetch(city: City): Forecast = mapResponse(
        api.forecast(city.latitude, city.longitude), city, clock.instant()
    )
}

internal fun mapResponse(response: ForecastResponse, city: City, fetchedAt: Instant): Forecast {
    fun invalid(): Nothing = throw InvalidForecastException()
    val zone = try { ZoneId.of(response.timezone ?: invalid()) } catch (_: Exception) { invalid() }
    val current = response.current ?: invalid()
    val temperature = current.temperature?.takeIf { it.isFinite() && it in -100.0..70.0 } ?: invalid()
    val observedAt = current.time?.let(Instant::ofEpochSecond) ?: invalid()
    val code = current.weatherCode ?: invalid()
    val day = current.isDay?.takeIf { it == 0 || it == 1 } ?: invalid()
    val hourly = response.hourly ?: invalid()
    val times = hourly.time ?: invalid()
    val temperatures = hourly.temperature ?: invalid()
    val probabilities = hourly.rainProbability ?: invalid()
    val codes = hourly.weatherCode ?: invalid()
    val daylight = hourly.isDay ?: invalid()
    if (times.size < 2 || listOf(temperatures.size, probabilities.size, codes.size, daylight.size).any { it != times.size }) {
        invalid()
    }
    val epochs = times.map { it ?: invalid() }
    if (epochs.zipWithNext().any { (a, b) -> b - a != 3600L }) invalid()
    val hours = (0 until epochs.lastIndex).mapNotNull { index ->
        val temp = temperatures[index]?.takeIf { it.isFinite() && it in -100.0..70.0 } ?: return@mapNotNull null
        val weatherCode = codes[index] ?: return@mapNotNull null
        val isDay = daylight[index]?.takeIf { it == 0 || it == 1 } ?: return@mapNotNull null
        // Open-Meteo rain at t describes the preceding hour, so shift it to [t-1h, t).
        val rain = probabilities[index + 1]?.takeIf { it in 0..100 }
        ForecastHour(Instant.ofEpochSecond(epochs[index]), temp, rain, weatherCode, isDay == 1)
    }
    if (hours.isEmpty()) invalid()
    return Forecast(city, zone, observedAt, fetchedAt, temperature, code, day == 1, hours)
}
