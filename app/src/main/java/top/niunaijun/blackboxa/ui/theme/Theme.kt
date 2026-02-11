package top.niunaijun.blackboxa.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightScheme =
        lightColorScheme(
                primary = BrandPrimary,
                secondary = BrandSecondary,
                tertiary = BrandTertiary,
                surface = SurfaceTone,
                background = BackgroundTone,
        )

private val DarkScheme =
        darkColorScheme(
                primary = BrandPrimaryDark,
                secondary = BrandSecondaryDark,
                tertiary = BrandTertiaryDark,
        )

@Composable
fun BlackBoxExpressiveTheme(
        darkTheme: Boolean = isSystemInDarkTheme(),
        dynamicColor: Boolean = true,
        content: @Composable () -> Unit,
) {
    val colorScheme =
            when {
                dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    val context = LocalContext.current
                    if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                }

                darkTheme -> DarkScheme
                else -> LightScheme
            }

    MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
    )
}
