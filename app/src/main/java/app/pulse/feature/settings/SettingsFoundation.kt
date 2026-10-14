package app.pulse.feature.settings

enum class ThemePreference { SYSTEM, LIGHT, DARK }

data class SettingsState(
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val accent: String = "blue",
    val textScale: Float = 1f,
    val dataSaver: Boolean = false,
    val crashReportingConsent: Boolean = false,
    val language: String = "en",
)

fun SettingsState.withTextScale(value: Float): SettingsState = copy(textScale = value.coerceIn(0.8f, 2f))
