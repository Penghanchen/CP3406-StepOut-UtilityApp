package au.edu.jcu.assessment.utilityapp

import android.app.Application
import android.content.Context
import au.edu.jcu.assessment.utilityapp.data.ForecastRepository
import au.edu.jcu.assessment.utilityapp.data.OpenMeteoRepository
import au.edu.jcu.assessment.utilityapp.data.PreferenceSettingsRepository
import au.edu.jcu.assessment.utilityapp.data.SettingsRepository
import au.edu.jcu.assessment.utilityapp.data.WeatherApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Clock
import java.util.concurrent.TimeUnit

class UtilityApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

/** Small application-scoped composition root; dependencies enter the ViewModel as interfaces. */
class AppContainer(context: Context) {
    val clock: Clock = Clock.systemUTC()
    val settingsRepository: SettingsRepository = PreferenceSettingsRepository(
        context.getSharedPreferences("stepout_settings", Context.MODE_PRIVATE)
    )
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()
    private val api = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(WeatherApi::class.java)
    val forecastRepository: ForecastRepository = OpenMeteoRepository(api, clock)
}
