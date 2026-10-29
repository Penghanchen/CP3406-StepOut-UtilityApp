package au.edu.jcu.assessment.utilityapp.domain

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WalkPlannerTest {
    private val now = Instant.parse("2026-10-29T01:15:00Z")
    private val hourStart = Instant.parse("2026-10-29T01:00:00Z")
    private val settings = AppSettings()

    private fun forecast(
        risks: List<Int?>,
        daylight: List<Boolean> = List(risks.size) { true },
        fetchedAt: Instant = now
    ) = Forecast(
        City.TOWNSVILLE, ZoneId.of("Australia/Brisbane"), now, fetchedAt, 27.0, 0, true,
        risks.mapIndexed { index, risk ->
            ForecastHour(hourStart.plusSeconds(index * 3600L), 27.0, risk, 0, daylight[index])
        }
    )

    @Test fun `earliest valid window starts now rather than rounding to next hour`() {
        val window = findWalkWindow(forecast(listOf(20, 30)), settings, now)!!
        assertEquals(now, window.start)
        assertEquals(now.plusSeconds(1800), window.end)
        assertEquals(20, window.maxRainPercent)
    }

    @Test fun `ninety minute walk checks all overlapping hours`() {
        val window = findWalkWindow(forecast(listOf(20, 60, 10, 30)), settings.copy(walkMinutes = 90), now)!!
        assertEquals(hourStart.plusSeconds(7200), window.start)
        assertEquals(30, window.maxRainPercent)
    }

    @Test fun `rain equal to configured limit is included`() {
        assertEquals(40, findWalkWindow(forecast(listOf(40)), settings, now)!!.maxRainPercent)
    }

    @Test fun `end exclusive does not include the following wet hour`() {
        val start = hourStart.plusSeconds(1800)
        val window = findWalkWindow(forecast(listOf(10, 90)), settings, start)!!
        assertEquals(hourStart.plusSeconds(3600), window.end)
        assertEquals(10, window.maxRainPercent)
    }

    @Test fun `missing risk is unknown rather than dry`() {
        val window = findWalkWindow(forecast(listOf(null, 10)), settings, now)!!
        assertEquals(hourStart.plusSeconds(3600), window.start)
    }

    @Test fun `night intervals are excluded`() {
        val window = findWalkWindow(forecast(listOf(10, 10), listOf(false, true)), settings, now)!!
        assertEquals(hourStart.plusSeconds(3600), window.start)
    }

    @Test fun `no complete coverage cannot produce a window`() {
        assertNull(findWalkWindow(forecast(listOf(10)), settings.copy(walkMinutes = 90), now))
    }

    @Test fun `gap between hours cannot be bridged`() {
        val complete = forecast(listOf(10, 10, 10))
        val withGap = complete.copy(hours = complete.hours.filterIndexed { index, _ -> index != 1 })
        assertNull(findWalkWindow(withGap, settings.copy(walkMinutes = 90), now))
    }

    @Test fun `rainy outlook returns no recommendation`() {
        assertNull(findWalkWindow(forecast(List(15) { 80 }), settings, now))
    }

    @Test fun `window must finish within twelve hour horizon`() {
        val data = forecast(List(15) { if (it >= 12) 10 else 80 })
        assertNull(findWalkWindow(data, settings.copy(walkMinutes = 90), now))
    }

    @Test fun `stale forecast cannot produce a recommendation`() {
        assertNull(findWalkWindow(forecast(listOf(10, 10), fetchedAt = now.minusSeconds(3601)), settings, now))
    }

    @Test fun `forecast exactly one hour old cannot produce a recommendation`() {
        assertNull(findWalkWindow(forecast(listOf(10, 10), fetchedAt = now.minusSeconds(3600)), settings, now))
    }

    @Test fun `different city forecast cannot produce a recommendation`() {
        assertNull(findWalkWindow(forecast(listOf(10, 10)), settings.copy(city = City.SYDNEY), now))
    }

    @Test fun `temperature conversion rounds consistently`() {
        assertEquals("32°F", TemperatureUnit.FAHRENHEIT.format(0.0))
        assertEquals("27°C", TemperatureUnit.CELSIUS.format(26.7))
    }
}
