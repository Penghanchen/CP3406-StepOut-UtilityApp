package au.edu.jcu.assessment.utilityapp.data

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("v1/forecast")
    suspend fun forecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,weather_code,is_day",
        @Query("hourly") hourly: String = "temperature_2m,precipitation_probability,weather_code,is_day",
        @Query("timezone") timezone: String = "auto",
        @Query("timeformat") timeFormat: String = "unixtime",
        @Query("forecast_days") forecastDays: Int = 2
    ): ForecastResponse
}

// Nullable DTOs make absent API data an explicit validation decision at the boundary.
data class ForecastResponse(
    val timezone: String?,
    val current: CurrentResponse?,
    val hourly: HourlyResponse?
)

data class CurrentResponse(
    val time: Long?,
    @SerializedName("temperature_2m") val temperature: Double?,
    @SerializedName("weather_code") val weatherCode: Int?,
    @SerializedName("is_day") val isDay: Int?
)

data class HourlyResponse(
    val time: List<Long?>?,
    @SerializedName("temperature_2m") val temperature: List<Double?>?,
    @SerializedName("precipitation_probability") val rainProbability: List<Int?>?,
    @SerializedName("weather_code") val weatherCode: List<Int?>?,
    @SerializedName("is_day") val isDay: List<Int?>?
)
