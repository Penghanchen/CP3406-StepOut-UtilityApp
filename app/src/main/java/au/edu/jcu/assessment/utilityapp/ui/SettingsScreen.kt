package au.edu.jcu.assessment.utilityapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import au.edu.jcu.assessment.utilityapp.domain.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(settings: AppSettings, onChange: ((AppSettings) -> AppSettings) -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 840.dp).fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("Settings", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Normal,
                modifier = Modifier.semantics { heading() })
            Text("Make your next walk fit your day. Changes apply immediately and are saved on this device.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            PreferenceSection {
                SettingHeading("Forecast city")
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(value = settings.city.label, onValueChange = {}, readOnly = true,
                        label = { Text("City") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth())
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        City.entries.forEach { city ->
                            DropdownMenuItem(text = { Text(city.label) }, onClick = {
                                onChange { it.copy(city = city) }; expanded = false
                            })
                        }
                    }
                }
                Text("Uses a city centre forecast. No location permission is needed.",
                    style = MaterialTheme.typography.bodySmall)
            }
            PreferenceSection {
                SettingHeading("Walk duration")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30, 60, 90).forEach { minutes ->
                        FilterChip(selected = settings.walkMinutes == minutes,
                            onClick = { onChange { it.copy(walkMinutes = minutes) } },
                            label = { Text("$minutes min") }, shape = RoundedCornerShape(8.dp),
                            colors = preferenceChipColors())
                    }
                }
            }
            PreferenceSection {
                SettingHeading("Maximum rain chance")
                Text("Each hour of the walk must meet this limit.", style = MaterialTheme.typography.bodySmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(20, 40, 60).forEach { percent ->
                        FilterChip(selected = settings.maxRainPercent == percent,
                            onClick = { onChange { it.copy(maxRainPercent = percent) } },
                            label = { Text("$percent%") }, shape = RoundedCornerShape(8.dp),
                            colors = preferenceChipColors())
                    }
                }
            }
            PreferenceSection {
                SettingHeading("Temperature unit")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TemperatureUnit.entries.forEach { unit ->
                        FilterChip(selected = settings.temperatureUnit == unit,
                            onClick = { onChange { it.copy(temperatureUnit = unit) } },
                            label = { Text(unit.label) }, shape = RoundedCornerShape(8.dp),
                            colors = preferenceChipColors())
                    }
                }
            }
            PreferenceSection {
                SettingHeading("Appearance")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppTheme.entries.forEach { theme ->
                        FilterChip(selected = settings.theme == theme,
                            onClick = { onChange { it.copy(theme = theme) } },
                            label = { Text(theme.label) }, shape = RoundedCornerShape(8.dp),
                            colors = preferenceChipColors())
                    }
                }
            }
            OutlinedCard(shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingHeading("About the suggestion")
                    Text("StepOut finds the earliest daylight start in the next 12 hours, checking now and then each hour. It checks all forecast hours touched by your walk.")
                    Text("Rain chance is the highest hourly probability in that window, not a combined trip probability. Daylight is estimated hourly; conditions can change between updates.")
                    Text("Weather is kept in memory for this session. Preferences stay on this device. Requests send only the selected city's coordinates to Open-Meteo.")
                }
            }
            DataSourceNote()
            Text("StepOut 1.0 · CP3406 Utility App\nPenghan Chen · 14758250",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PreferenceSection(content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content)
    }
}

@Composable
private fun preferenceChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primary,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
)

@Composable
private fun SettingHeading(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.semantics { heading() })
}

@Composable
internal fun DataSourceNote() {
    val uriHandler = LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Weather data by Open-Meteo · CC BY 4.0", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Model forecasts are estimates, not local observations or weather alerts.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row {
            TextButton(onClick = { uriHandler.openUri("https://open-meteo.com/") }) { Text("Data source") }
            TextButton(onClick = { uriHandler.openUri("https://creativecommons.org/licenses/by/4.0/") }) { Text("Data licence") }
        }
    }
}
