package au.edu.jcu.assessment.utilityapp.domain

import java.time.Duration
import java.time.Instant

/** Finds the earliest fully covered daylight walk, using hourly estimates only. */
fun findWalkWindow(forecast: Forecast, settings: AppSettings, now: Instant): WalkWindow? {
    if (forecast.city != settings.city) return null
    if (Duration.between(forecast.fetchedAt, now) >= Duration.ofHours(1)) return null
    val horizon = now.plus(Duration.ofHours(12))
    val length = Duration.ofMinutes(settings.walkMinutes.toLong())
    val hours = forecast.hours.sortedBy { it.time }
    val candidates = (listOf(now) + hours.map { it.time }.filter { it > now && it < horizon }).distinct()
    for (start in candidates) {
        val end = start.plus(length)
        if (end > horizon) continue
        val covering = hours.filter { it.time < end && it.time.plusSeconds(3600) > start }
        if (covering.isEmpty()) continue
        var coveredUntil = start
        var highestRisk = 0
        var eligible = true
        for (hour in covering) {
            val risk = hour.rainPercent
            // Missing probabilities and gaps are unknown, never interpreted as dry weather.
            if (hour.time > coveredUntil || !hour.isDay || risk == null || risk > settings.maxRainPercent) {
                eligible = false
                break
            }
            highestRisk = maxOf(highestRisk, risk)
            coveredUntil = maxOf(coveredUntil, hour.time.plusSeconds(3600))
        }
        if (eligible && coveredUntil >= end) return WalkWindow(start, end, highestRisk)
    }
    return null
}
