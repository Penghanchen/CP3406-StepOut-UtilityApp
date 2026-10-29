package au.edu.jcu.assessment.utilityapp.data

import au.edu.jcu.assessment.utilityapp.domain.City
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpenMeteoRepositoryTest {
    private val time = Instant.parse("2026-10-29T01:00:00Z")
    private fun response() = ForecastResponse(
        "Australia/Brisbane",
        CurrentResponse(time.epochSecond, 27.5, 2, 1),
        HourlyResponse(
            List(4) { time.epochSecond + it * 3600L },
            listOf(27.0, 28.0, 29.0, 30.0),
            listOf(90, 20, null, 40),
            listOf(2, 2, 3, 3),
            listOf(1, 1, 1, 1)
        )
    )

    @Test fun `preceding hour rain is shifted to domain interval start`() {
        val forecast = mapResponse(response(), City.TOWNSVILLE, time)
        assertEquals(3, forecast.hours.size)
        assertEquals(time, forecast.hours[0].time)
        assertEquals(20, forecast.hours[0].rainPercent)
        assertNull(forecast.hours[1].rainPercent)
        assertEquals(40, forecast.hours[2].rainPercent)
        assertEquals(27.0, forecast.hours[0].temperatureCelsius, 0.001)
        assertEquals(ZoneId.of("Australia/Brisbane"), forecast.zoneId)
    }

    @Test(expected = InvalidForecastException::class)
    fun `mismatched arrays are rejected`() {
        val original = response()
        mapResponse(original.copy(hourly = original.hourly!!.copy(temperature = listOf(27.0))), City.TOWNSVILLE, time)
    }

    @Test(expected = InvalidForecastException::class)
    fun `invalid timezone is rejected`() {
        mapResponse(response().copy(timezone = "not-a-timezone"), City.TOWNSVILLE, time)
    }

    @Test(expected = InvalidForecastException::class)
    fun `non chronological timestamps are rejected`() {
        val original = response()
        val timestamps = original.hourly!!.time!!.reversed()
        mapResponse(original.copy(hourly = original.hourly.copy(time = timestamps)), City.TOWNSVILLE, time)
    }

    @Test(expected = InvalidForecastException::class)
    fun `missing current temperature is rejected`() {
        val original = response()
        mapResponse(original.copy(current = original.current!!.copy(temperature = null)), City.TOWNSVILLE, time)
    }

    @Test fun `invalid probability remains unknown rather than being clamped`() {
        val original = response()
        val forecast = mapResponse(
            original.copy(hourly = original.hourly!!.copy(rainProbability = listOf(0, 140, 20, 30))),
            City.TOWNSVILLE, time
        )
        assertNull(forecast.hours.first().rainPercent)
    }
}
