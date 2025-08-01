package com.strmr.ai.ui.theme

import android.util.Log
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun StrmrTheme(
    isInDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    Log.d("FontLoading", "🎨 StrmrTheme composable called, isInDarkTheme: $isInDarkTheme")

    val colorScheme =
        if (isInDarkTheme) {
            darkColorScheme(
                primary = Purple80,
                secondary = PurpleGrey80,
                tertiary = Pink80,
            )
        } else {
            lightColorScheme(
                primary = Purple40,
                secondary = PurpleGrey40,
                tertiary = Pink40,
            )
        }

    Log.d("FontLoading", "📝 Applying MaterialTheme with Typography: ${Typography.displayLarge.fontFamily}")

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    ).also {
        Log.d("FontLoading", "✅ MaterialTheme applied successfully with Figtree typography")
    }
}
