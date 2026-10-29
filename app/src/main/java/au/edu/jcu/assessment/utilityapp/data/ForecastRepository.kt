package au.edu.jcu.assessment.utilityapp.data

import au.edu.jcu.assessment.utilityapp.domain.City
import au.edu.jcu.assessment.utilityapp.domain.Forecast

interface ForecastRepository {
    suspend fun fetch(city: City): Forecast
}
