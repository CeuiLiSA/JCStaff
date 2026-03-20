package ceui.lisa.jcstaff.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.MaterialDynamicColors
import com.google.android.material.color.utilities.SchemeContent

fun colorSchemeFromSeed(seedColor: Int, isDark: Boolean): ColorScheme {
    val hct = Hct.fromInt(seedColor)
    val scheme = SchemeContent(hct, isDark, 0.0)
    val c = MaterialDynamicColors()

    fun resolve(dc: com.google.android.material.color.utilities.DynamicColor): Color =
        Color(dc.getArgb(scheme))

    return if (isDark) {
        darkColorScheme(
            primary = resolve(c.primary()),
            onPrimary = resolve(c.onPrimary()),
            primaryContainer = resolve(c.primaryContainer()),
            onPrimaryContainer = resolve(c.onPrimaryContainer()),
            secondary = resolve(c.secondary()),
            onSecondary = resolve(c.onSecondary()),
            secondaryContainer = resolve(c.secondaryContainer()),
            onSecondaryContainer = resolve(c.onSecondaryContainer()),
            tertiary = resolve(c.tertiary()),
            onTertiary = resolve(c.onTertiary()),
            tertiaryContainer = resolve(c.tertiaryContainer()),
            onTertiaryContainer = resolve(c.onTertiaryContainer()),
            error = resolve(c.error()),
            onError = resolve(c.onError()),
            errorContainer = resolve(c.errorContainer()),
            onErrorContainer = resolve(c.onErrorContainer()),
            background = resolve(c.background()),
            onBackground = resolve(c.onBackground()),
            surface = resolve(c.surface()),
            onSurface = resolve(c.onSurface()),
            surfaceVariant = resolve(c.surfaceVariant()),
            onSurfaceVariant = resolve(c.onSurfaceVariant()),
            outline = resolve(c.outline()),
            outlineVariant = resolve(c.outlineVariant()),
            inverseSurface = resolve(c.inverseSurface()),
            inverseOnSurface = resolve(c.inverseOnSurface()),
            inversePrimary = resolve(c.inversePrimary()),
            scrim = resolve(c.scrim()),
        )
    } else {
        lightColorScheme(
            primary = resolve(c.primary()),
            onPrimary = resolve(c.onPrimary()),
            primaryContainer = resolve(c.primaryContainer()),
            onPrimaryContainer = resolve(c.onPrimaryContainer()),
            secondary = resolve(c.secondary()),
            onSecondary = resolve(c.onSecondary()),
            secondaryContainer = resolve(c.secondaryContainer()),
            onSecondaryContainer = resolve(c.onSecondaryContainer()),
            tertiary = resolve(c.tertiary()),
            onTertiary = resolve(c.onTertiary()),
            tertiaryContainer = resolve(c.tertiaryContainer()),
            onTertiaryContainer = resolve(c.onTertiaryContainer()),
            error = resolve(c.error()),
            onError = resolve(c.onError()),
            errorContainer = resolve(c.errorContainer()),
            onErrorContainer = resolve(c.onErrorContainer()),
            background = resolve(c.background()),
            onBackground = resolve(c.onBackground()),
            surface = resolve(c.surface()),
            onSurface = resolve(c.onSurface()),
            surfaceVariant = resolve(c.surfaceVariant()),
            onSurfaceVariant = resolve(c.onSurfaceVariant()),
            outline = resolve(c.outline()),
            outlineVariant = resolve(c.outlineVariant()),
            inverseSurface = resolve(c.inverseSurface()),
            inverseOnSurface = resolve(c.inverseOnSurface()),
            inversePrimary = resolve(c.inversePrimary()),
            scrim = resolve(c.scrim()),
        )
    }
}

data class PresetColor(val nameResId: Int, val argb: Int)
