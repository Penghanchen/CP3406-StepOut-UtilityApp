package au.edu.jcu.assessment.utilityapp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.edu.jcu.assessment.utilityapp.UtilityUiState
import au.edu.jcu.assessment.utilityapp.domain.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ClockFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
private val DayFormat = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)
private fun time(value: Instant, zone: ZoneId) = ClockFormat.format(value.atZone(zone))
private fun day(value: Instant, zone: ZoneId) = DayFormat.format(value.atZone(zone))

@Composable
fun UtilityScreen(state: UtilityUiState, onRefresh: () -> Unit, onOpenSettings: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 840.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("WALK WEATHER", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary, letterSpacing = 3.sp)
                    Text("StepOut", style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Normal, modifier = Modifier.semantics { heading() })
                }
                FilledTonalIconButton(onClick = onRefresh, enabled = !state.isLoading) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh forecast")
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(state.settings.city.label, style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold)
                    state.forecast?.let {
                        Text("${day(state.now, it.zoneId)}  |  ${time(state.now, it.zoneId)} local",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                OutlinedButton(onClick = onOpenSettings) { Text("Change city") }
            }
            if (state.isLoading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text("Updating weather…", style = MaterialTheme.typography.bodySmall)
            }
            state.errorMessage?.let { message ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (state.forecast == null) "Weather unavailable" else "Refresh unsuccessful",
                            fontWeight = FontWeight.SemiBold)
                        Text(message)
                        if (state.forecast != null) Text("Showing the last successful forecast.")
                        OutlinedButton(onClick = onRefresh, enabled = !state.isLoading) { Text("Try again") }
                    }
                }
            }
            if (state.forecast != null) {
                val forecast = state.forecast
                if (state.isStale) {
                    Text("Forecast is over 30 minutes old. Refresh before planning your walk.",
                        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
                WalkWindowCard(state)
                CurrentWeatherCard(forecast, state.settings.temperatureUnit)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("The next 12 hours", style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() })
                    Text("Hourly chance of precipitation", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.upcomingHours.forEach { hour ->
                            HourCard(hour, forecast.zoneId, state.settings.temperatureUnit, state.settings.maxRainPercent)
                        }
                    }
                }
                Text("Updated ${day(forecast.fetchedAt, forecast.zoneId)} at ${time(forecast.fetchedAt, forecast.zoneId)} · ${forecast.zoneId.id}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (!state.isLoading && state.errorMessage == null) {
                Text("Tap refresh to check the weather for your next walk.")
            }
            DataSourceNote()
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun WalkWindowCard(state: UtilityUiState) {
    val forecast = state.forecast ?: return
    val window = state.recommendation
    Card(shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("YOUR NEXT DAYLIGHT WALK", style = MaterialTheme.typography.labelMedium,
                letterSpacing = 1.8.sp, color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.semantics { heading() })
            Text(when {
                state.isStale && java.time.Duration.between(forecast.fetchedAt, state.now).toMinutes() >= 60 -> "Refresh to plan"
                window == null -> "Wait for a better window"
                window.start <= state.now -> "A good time to step out"
                else -> "A little later looks better"
            }, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Normal)
            if (window != null) {
                Text("${time(window.start, forecast.zoneId)} – ${time(window.end, forecast.zoneId)}",
                    style = MaterialTheme.typography.headlineLarge, fontFamily = FontFamily.Monospace,
                    fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.Medium)
                Text("${day(window.start, forecast.zoneId)} · ${state.settings.walkMinutes} min · up to ${window.maxRainPercent}% rain chance")
                Text("Earliest daylight window within your ${state.settings.maxRainPercent}% rain limit.",
                    style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(if (java.time.Duration.between(forecast.fetchedAt, state.now).toMinutes() >= 60)
                    "This forecast is over an hour old. Refresh for a new suggestion."
                else "No ${state.settings.walkMinutes}-minute daylight window within your ${state.settings.maxRainPercent}% rain limit in the next 12 hours.")
            }
            Text("Hourly estimates only. Check local conditions before leaving.",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun CurrentWeatherCard(forecast: Forecast, unit: TemperatureUnit) {
    OutlinedCard(shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                WeatherMark(forecast.weatherCode, forecast.isDay, Modifier.padding(16.dp).size(36.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("CURRENT WEATHER", style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 1.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(unit.format(forecast.temperatureCelsius), style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Medium)
                Text(weatherDescription(forecast.weatherCode, forecast.isDay))
                Text("Model time ${time(forecast.observedAt, forecast.zoneId)} local",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HourCard(hour: ForecastHour, zone: ZoneId, unit: TemperatureUnit, limit: Int) {
    val withinLimit = hour.rainPercent?.let { it <= limit } == true
    OutlinedCard(Modifier.width(112.dp), shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (withinLimit) MaterialTheme.colorScheme.primary.copy(alpha = .45f)
            else MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(time(hour.time, zone), style = MaterialTheme.typography.labelLarge,
                fontFamily = FontFamily.Monospace)
            WeatherMark(hour.weatherCode, hour.isDay, Modifier.size(28.dp).semantics {
                contentDescription = weatherDescription(hour.weatherCode, hour.isDay)
            })
            Text(unit.format(hour.temperatureCelsius), fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace)
            Text(hour.rainPercent?.let { "$it% rain" } ?: "Rain N/A", style = MaterialTheme.typography.bodySmall,
                color = if (withinLimit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            Text(if (hour.isDay) "Daylight" else "Night", style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** Decorative vector marks accompany a textual condition; no image network requests. */
@Composable
private fun WeatherMark(code: Int, isDay: Boolean, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val line = w * .055f
        if (code <= 1) {
            if (isDay) {
                drawCircle(color, w * .21f, center = center, style = Stroke(line))
                for (index in 0..7) {
                    val angle = Math.PI * index / 4
                    fun point(radius: Float) = Offset(center.x + kotlin.math.cos(angle).toFloat() * radius,
                        center.y + kotlin.math.sin(angle).toFloat() * radius)
                    drawLine(color, point(w * .34f), point(w * .43f), line, StrokeCap.Round)
                }
            } else drawArc(color, 30f, 290f, false, topLeft = Offset(w * .2f, h * .15f),
                size = androidx.compose.ui.geometry.Size(w * .6f, h * .65f), style = Stroke(line))
        } else {
            drawRoundRect(color, topLeft = Offset(w * .12f, h * .38f),
                size = androidx.compose.ui.geometry.Size(w * .76f, h * .36f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * .18f), style = Stroke(line))
            drawCircle(color, w * .19f, Offset(w * .46f, h * .37f))
            if (code >= 51) for (index in 0..2) {
                drawLine(color, Offset(w * (.3f + index * .2f), h * .84f),
                    Offset(w * (.26f + index * .2f), h * .94f), line, StrokeCap.Round)
            }
        }
    }
}
