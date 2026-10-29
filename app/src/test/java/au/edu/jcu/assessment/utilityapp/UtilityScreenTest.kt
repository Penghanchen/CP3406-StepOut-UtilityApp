package au.edu.jcu.assessment.utilityapp

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import au.edu.jcu.assessment.utilityapp.domain.AppSettings
import au.edu.jcu.assessment.utilityapp.domain.AppTheme
import au.edu.jcu.assessment.utilityapp.domain.City
import au.edu.jcu.assessment.utilityapp.domain.Forecast
import au.edu.jcu.assessment.utilityapp.domain.ForecastHour
import au.edu.jcu.assessment.utilityapp.ui.theme.MobileTech_UtilityAppTheme
import java.time.Instant
import java.time.ZoneId
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Exercises the actual Compose screens locally, without a device or live weather service. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp")
class UtilityScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val now = Instant.parse("2026-10-29T01:15:00Z")
    private var observedBackground = Color.Unspecified
    private var observedSettings = AppSettings()

    private fun readyState(): UtilityUiState {
        val forecast = Forecast(
            City.TOWNSVILLE, ZoneId.of("Australia/Brisbane"), now, now, 20.0, 0, true,
            List(24) { index ->
                ForecastHour(Instant.parse("2026-10-29T01:00:00Z").plusSeconds(index * 3600L), 21.0, 10, 0, true)
            }
        )
        return UtilityUiState(forecast = forecast, now = now)
    }

    private fun render(initial: UtilityUiState, onRefresh: () -> Unit = {}) {
        compose.setContent {
            var state by remember { mutableStateOf(initial) }
            MobileTech_UtilityAppTheme(darkTheme = state.settings.theme == AppTheme.DARK) {
                val background = MaterialTheme.colorScheme.background
                SideEffect { observedBackground = background }
                UtilityApp(state, onRefresh) { transform ->
                    val settings = transform(state.settings)
                    observedSettings = settings
                    val cityChanged = settings.city != state.settings.city
                    state = state.copy(
                        settings = settings,
                        forecast = if (cityChanged) null else state.forecast,
                        isLoading = cityChanged
                    )
                }
            }
        }
    }

    private fun openSettings() {
        compose.onNode(hasText("Settings") and hasClickAction()).performClick()
    }

    @Test fun initialLoadingShowsProgressAndDisablesRefresh() {
        render(UtilityUiState(isLoading = true, now = now))
        compose.onNodeWithText("Updating weather…").assertIsDisplayed()
        compose.onNodeWithContentDescription("Refresh forecast").assertIsNotEnabled()
        compose.onNodeWithText("Townsville").assertIsDisplayed()
    }

    @Test fun errorRetryInvokesRefresh() {
        var refreshes = 0
        render(UtilityUiState(errorMessage = "Could not connect. Please retry.", now = now)) { refreshes++ }
        compose.onNodeWithText("Weather unavailable").assertIsDisplayed()
        compose.onNodeWithText("Could not connect. Please retry.").assertIsDisplayed()
        compose.onNodeWithText("Try again").performClick()
        compose.runOnIdle { assertEquals(1, refreshes) }
    }

    @Test fun changingDurationRainAndUnitsUpdatesMainScreen() {
        render(readyState())
        openSettings()
        compose.onNodeWithText("90 min").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("20%").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("Fahrenheit").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("Walk").performClick()
        compose.onNodeWithText("Thu 29 Oct · 90 min · up to 10% rain chance").assertIsDisplayed()
        compose.onNodeWithText("68°F").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Earliest daylight window within your 20% rain limit.")
            .performScrollTo().assertIsDisplayed()
    }

    @Test fun appearanceAndCityControlsApplyTheirSelections() {
        render(readyState())
        var initialBackground = Color.Unspecified
        compose.runOnIdle { initialBackground = observedBackground }
        openSettings()
        compose.onNodeWithText("Dark").performScrollTo().performClick().assertIsSelected()
        compose.runOnIdle {
            assertEquals(AppTheme.DARK, observedSettings.theme)
            assertNotEquals(initialBackground, observedBackground)
        }
        compose.onNodeWithText("Townsville").performScrollTo().performClick()
        compose.onNodeWithText("Sydney").performClick()
        compose.runOnIdle { assertEquals(City.SYDNEY, observedSettings.city) }
        compose.onNodeWithText("Walk").performClick()
        compose.onNodeWithText("Sydney").assertIsDisplayed()
        compose.onNodeWithText("Updating weather…").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [34], qualifiers = "w411dp-h891dp")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun exportScreensForVisualReview() {
        fun capture(name: String) {
            val bitmap = compose.runOnIdle {
                val view = compose.activity.window.decorView
                Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also {
                    // Draw directly because PixelCopy requires a live display compositor.
                    view.draw(Canvas(it))
                }
            }
            val destination = File("build/reports/screenshots/$name.png")
            destination.parentFile!!.mkdirs()
            destination.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        render(readyState())
        compose.onNodeWithText("StepOut").assertIsDisplayed()
        capture("walk-light")
        openSettings()
        capture("settings-light")
        compose.onNodeWithText("Dark").performScrollTo().performClick()
        compose.onNodeWithText("Walk").performClick()
        capture("walk-dark")
    }
}
