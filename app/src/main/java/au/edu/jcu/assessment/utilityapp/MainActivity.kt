package au.edu.jcu.assessment.utilityapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import au.edu.jcu.assessment.utilityapp.domain.AppSettings
import au.edu.jcu.assessment.utilityapp.domain.AppTheme
import au.edu.jcu.assessment.utilityapp.ui.SettingsScreen
import au.edu.jcu.assessment.utilityapp.ui.UtilityScreen
import au.edu.jcu.assessment.utilityapp.ui.theme.MobileTech_UtilityAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val model: UtilityViewModel = viewModel(
                factory = UtilityViewModel.Factory((application as UtilityApplication).container)
            )
            val state by model.state.collectAsStateWithLifecycle()
            val darkTheme = when (state.settings.theme) {
                AppTheme.SYSTEM -> isSystemInDarkTheme()
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
            }
            SideEffect {
                val transparent = android.graphics.Color.TRANSPARENT
                val bars = if (darkTheme) SystemBarStyle.dark(transparent)
                    else SystemBarStyle.light(transparent, transparent)
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
            }
            val owner = LocalLifecycleOwner.current
            DisposableEffect(owner, model) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_START) model.refreshIfStale()
                }
                owner.lifecycle.addObserver(observer)
                onDispose { owner.lifecycle.removeObserver(observer) }
            }
            MobileTech_UtilityAppTheme(darkTheme = darkTheme) {
                UtilityApp(state, model::refresh, model::updateSettings)
            }
        }
    }
}

/** Keeps the starter's single activity and two destinations, including rotation state. */
@Composable
fun UtilityApp(
    state: UtilityUiState,
    onRefresh: () -> Unit,
    onSettingsChange: ((AppSettings) -> AppSettings) -> Unit
) {
    var settingsSelected by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = settingsSelected) { settingsSelected = false }
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = !settingsSelected,
                    onClick = { settingsSelected = false },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Walk") }
                )
                NavigationBarItem(
                    selected = settingsSelected,
                    onClick = { settingsSelected = true },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") }
                )
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).clipToBounds()) {
            if (settingsSelected) {
                SettingsScreen(state.settings, onSettingsChange)
            } else {
                UtilityScreen(state, onRefresh, onOpenSettings = { settingsSelected = true })
            }
        }
    }
}
