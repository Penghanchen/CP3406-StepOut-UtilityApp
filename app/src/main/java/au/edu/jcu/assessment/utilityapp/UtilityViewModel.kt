package au.edu.jcu.assessment.utilityapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import au.edu.jcu.assessment.utilityapp.data.ForecastRepository
import au.edu.jcu.assessment.utilityapp.data.InvalidForecastException
import au.edu.jcu.assessment.utilityapp.data.SettingsRepository
import au.edu.jcu.assessment.utilityapp.domain.AppSettings
import au.edu.jcu.assessment.utilityapp.domain.Forecast
import au.edu.jcu.assessment.utilityapp.domain.ForecastHour
import au.edu.jcu.assessment.utilityapp.domain.WalkWindow
import au.edu.jcu.assessment.utilityapp.domain.findWalkWindow
import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class UtilityUiState(
    val settings: AppSettings = AppSettings(),
    val forecast: Forecast? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val now: Instant = Instant.now()
) {
    val isStale: Boolean get() = forecast?.let { Duration.between(it.fetchedAt, now) >= Duration.ofMinutes(30) } ?: false
    val recommendation: WalkWindow? get() = forecast?.let { findWalkWindow(it, settings, now) }
    val upcomingHours: List<ForecastHour> get() = forecast?.hours.orEmpty()
        .filter { it.time.plusSeconds(3600) > now && it.time < now.plusSeconds(12 * 3600) }.take(12)
}

class UtilityViewModel(
    private val forecastRepository: ForecastRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock = Clock.systemUTC()
) : ViewModel() {
    private val mutableState = MutableStateFlow(UtilityUiState(settingsRepository.settings.value, now = clock.instant()))
    val state: StateFlow<UtilityUiState> = mutableState.asStateFlow()
    private var request: Job? = null
    private var requestVersion = 0L

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                val cityChanged = mutableState.value.settings.city != settings.city
                mutableState.update {
                    it.copy(settings = settings, forecast = if (cityChanged) null else it.forecast, now = clock.instant())
                }
                if (cityChanged) refresh()
            }
        }
        refresh()
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                mutableState.update { it.copy(now = clock.instant()) }
            }
        }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        settingsRepository.update(transform(settingsRepository.settings.value))
    }

    fun refreshIfStale() {
        mutableState.update { it.copy(now = clock.instant()) }
        if (!state.value.isLoading && (state.value.forecast == null || state.value.isStale)) refresh()
    }

    fun refresh() {
        request?.cancel()
        val version = ++requestVersion
        val city = settingsRepository.settings.value.city
        mutableState.update {
            it.copy(
                settings = settingsRepository.settings.value,
                forecast = it.forecast?.takeIf { forecast -> forecast.city == city },
                isLoading = true,
                errorMessage = null,
                now = clock.instant()
            )
        }
        request = viewModelScope.launch {
            try {
                val forecast = forecastRepository.fetch(city)
                // A cancelled or slower response must not overwrite a newer city request.
                if (version == requestVersion && city == settingsRepository.settings.value.city) {
                    mutableState.update { it.copy(forecast = forecast, isLoading = false, now = clock.instant()) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (version == requestVersion && city == settingsRepository.settings.value.city) {
                    val message = when (error) {
                        is IOException -> "Could not connect. Check your internet connection and try again."
                        is HttpException -> "The weather service is unavailable. Please try again shortly."
                        is InvalidForecastException -> "The weather service returned incomplete data. Please retry."
                        else -> "Weather could not be loaded. Please try again."
                    }
                    mutableState.update { it.copy(isLoading = false, errorMessage = message, now = clock.instant()) }
                }
            }
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(UtilityViewModel::class.java))
            return UtilityViewModel(container.forecastRepository, container.settingsRepository, container.clock) as T
        }
    }
}
