package au.edu.jcu.assessment.utilityapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import au.edu.jcu.assessment.utilityapp.domain.AppSettings
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the actual activity independently of weather-service availability. */
@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Before fun setDefaultPreferences() = resetPreferences()

    @After fun restoreDefaultPreferences() = resetPreferences()

    private fun resetPreferences() {
        compose.runOnIdle {
            val application = InstrumentationRegistry.getInstrumentation().targetContext
                .applicationContext as UtilityApplication
            application.container.settingsRepository.update(AppSettings())
        }
    }

    private fun openSettings() {
        compose.onNode(hasText("Settings") and hasClickAction()).performClick()
    }

    private fun openWalk() {
        compose.onNode(hasText("Walk") and hasClickAction()).performClick()
    }

    @Test fun settingsAndSelectedTabSurviveActivityRecreation() {
        openSettings()
        compose.onNodeWithText("Fahrenheit").performScrollTo().performClick().assertIsSelected()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Fahrenheit").performScrollTo().assertIsSelected()
        openWalk()
        compose.onNodeWithText("StepOut").assertIsDisplayed()
    }

    @Test fun androidBackReturnsFromSettingsToWalk() {
        openSettings()
        compose.onNodeWithText("Forecast city").assertIsDisplayed()
        pressBack()
        compose.onNodeWithText("StepOut").assertIsDisplayed()
        compose.onNode(hasText("Walk") and hasClickAction()).assertIsSelected()
    }

    @Test fun allPreferencesSurviveRecreationAndSettingsCanScrollToAttribution() {
        openSettings()
        compose.onNodeWithText("90 min").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("20%").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("Fahrenheit").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("Dark").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("Townsville").performScrollTo().performClick()
        compose.onNodeWithText("Sydney").performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Sydney").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("90 min").performScrollTo().assertIsSelected()
        compose.onNodeWithText("20%").performScrollTo().assertIsSelected()
        compose.onNodeWithText("Fahrenheit").performScrollTo().assertIsSelected()
        compose.onNodeWithText("Dark").performScrollTo().assertIsSelected()
        compose.onNodeWithText("Data source").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Data licence").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("StepOut 1.0 · CP3406 Utility App\nPenghan Chen · 14758250")
            .performScrollTo().assertIsDisplayed()
        openWalk()
        compose.onNodeWithText("Sydney").assertIsDisplayed()
    }
}
