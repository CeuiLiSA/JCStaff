package ceui.lisa.jcstaff.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import ceui.lisa.jcstaff.core.SettingsStore
import ceui.lisa.jcstaff.core.ThemeMode

@Composable
fun JCStaffTheme(
    content: @Composable () -> Unit
) {
    val themeMode by SettingsStore.themeMode.collectAsState()
    val useDynamicColor by SettingsStore.useDynamicColor.collectAsState()
    val customSeedColor by SettingsStore.customSeedColor.collectAsState()

    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> colorSchemeFromSeed(seedColor = customSeedColor, isDark = darkTheme)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
