package au.edu.jcu.assessment.utilityapp

import au.edu.jcu.assessment.utilityapp.data.ForecastRepository
import au.edu.jcu.assessment.utilityapp.data.SettingsRepository
import au.edu.jcu.assessment.utilityapp.domain.AppSettings
import au.edu.jcu.assessment.utilityapp.domain.City
import au.edu.jcu.assessment.utilityapp.domain.Forecast
import au.edu.jcu.assessment.utilityapp.domain.ForecastHour
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import androidx.lifecycle.viewModelScope
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UtilityViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val now = Instant.parse("2026-10-29T01:00:00Z")
    private val clock = Clock.fixed(now, ZoneId.of("UTC"))
    private val models = mutableListOf<UtilityViewModel>()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }

    @After fun tearDown() {
        models.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
    }

    private fun forecast(city: City) = Forecast(
        city, ZoneId.of("Australia/Brisbane"), now, now, 27.0, 0, true,
        listOf(ForecastHour(now, 27.0, 10, 0, true))
    )

    private class MemorySettings : SettingsRepository {
        override val settings = MutableStateFlow(AppSettings())
        override fun update(settings: AppSettings) { this.settings.value = settings }
    }

    private fun model(repository: ForecastRepository, settings: SettingsRepository = MemorySettings()) =
        UtilityViewModel(repository, settings, clock).also { models += it }

    private fun modelTest(block: suspend TestScope.() -> Unit) = runTest(dispatcher) {
        try {
            block()
        } finally {
            // Stop the app's periodic clock before runTest drains its virtual scheduler.
            models.forEach { it.viewModelScope.cancel() }
        }
    }

    @Test fun `initial request moves from loading to forecast`() = modelTest {
        val model = model(object : ForecastRepository {
            override suspend fun fetch(city: City) = forecast(city)
        })
        assertTrue(model.state.value.isLoading)
        runCurrent()
        assertFalse(model.state.value.isLoading)
        assertEquals(City.TOWNSVILLE, model.state.value.forecast!!.city)
    }

    @Test fun `failed refresh retains successful data and reports error`() = modelTest {
        var fail = false
        val model = model(object : ForecastRepository {
            override suspend fun fetch(city: City): Forecast {
                if (fail) throw IOException("Offline")
                return forecast(city)
            }
        })
        runCurrent()
        fail = true
        model.refresh()
        runCurrent()
        assertNotNull(model.state.value.forecast)
        assertNotNull(model.state.value.errorMessage)
        assertFalse(model.state.value.isLoading)
    }

    @Test fun `old city response cannot overwrite newer selection even when it ignores cancellation`() = modelTest {
        val first = CompletableDeferred<Forecast>()
        val second = CompletableDeferred<Forecast>()
        val settings = MemorySettings()
        val model = model(object : ForecastRepository {
            override suspend fun fetch(city: City): Forecast = withContext(NonCancellable) {
                if (city == City.TOWNSVILLE) first.await() else second.await()
            }
        }, settings)
        runCurrent()
        model.updateSettings { it.copy(city = City.SYDNEY) }
        runCurrent()
        assertNull(model.state.value.forecast)
        second.complete(forecast(City.SYDNEY))
        runCurrent()
        first.complete(forecast(City.TOWNSVILLE))
        runCurrent()
        assertEquals(City.SYDNEY, model.state.value.forecast!!.city)
        assertEquals(City.SYDNEY, model.state.value.settings.city)
    }

    @Test fun `changing preferences recomputes UI without another network request`() = modelTest {
        var requests = 0
        val model = model(object : ForecastRepository {
            override suspend fun fetch(city: City): Forecast {
                requests++
                return forecast(city)
            }
        })
        runCurrent()
        model.updateSettings { it.copy(walkMinutes = 90, maxRainPercent = 20) }
        runCurrent()
        assertEquals(1, requests)
        assertEquals(90, model.state.value.settings.walkMinutes)
        assertNull(model.state.value.recommendation)
    }

    @Test fun `failed request for new city never displays old city forecast`() = modelTest {
        val model = model(object : ForecastRepository {
            override suspend fun fetch(city: City): Forecast {
                if (city == City.SYDNEY) throw IOException("Offline")
                return forecast(city)
            }
        })
        runCurrent()
        assertNotNull(model.state.value.forecast)
        model.updateSettings { it.copy(city = City.SYDNEY) }
        runCurrent()
        assertEquals(City.SYDNEY, model.state.value.settings.city)
        assertNull(model.state.value.forecast)
        assertNotNull(model.state.value.errorMessage)
    }

    @Test fun `foreground refresh reuses fresh data and fetches after thirty minutes`() = modelTest {
        var requests = 0
        val movingClock = object : Clock() {
            var instant = now
            override fun getZone(): ZoneId = ZoneId.of("UTC")
            override fun withZone(zone: ZoneId): Clock = this
            override fun instant(): Instant = instant
        }
        val repository = object : ForecastRepository {
            override suspend fun fetch(city: City): Forecast {
                requests++
                return forecast(city).copy(fetchedAt = movingClock.instant())
            }
        }
        val model = UtilityViewModel(repository, MemorySettings(), movingClock).also { models += it }
        runCurrent()
        model.refreshIfStale()
        runCurrent()
        assertEquals(1, requests)
        movingClock.instant = now.plusSeconds(1800)
        model.refreshIfStale()
        runCurrent()
        assertEquals(2, requests)
        assertFalse(model.state.value.isStale)
    }
}
