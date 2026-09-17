package com.example.focusdo

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_UNSPECIFIED) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

        super.onCreate(savedInstanceState)

        val appLocales = AppCompatDelegate.getApplicationLocales()
        val languageTag = if (appLocales.isEmpty()) {
            java.util.Locale.getDefault().toLanguageTag()
        } else {
            appLocales.toLanguageTags()
        }

        setContent {
            FocusDoTheme {
                val layoutDirection =
                    if (languageTag.startsWith("fa")) LayoutDirection.Rtl else LayoutDirection.Ltr

                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun FocusDoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val lightColors = lightColorScheme(
        primary = Color(0xFF4F46E5),
        secondary = Color(0xFF7C3AED),
        tertiary = Color(0xFF10B981),
        background = Color(0xFFF8FAFC),
        surface = Color(0xFFFFFFFF),
        onPrimary = Color.White,
        onBackground = Color(0xFF111827),
        onSurface = Color(0xFF111827)
    )

    val darkColors = darkColorScheme(
        primary = Color(0xFF8B5CF6),
        secondary = Color(0xFF6366F1),
        tertiary = Color(0xFF34D399),
        background = Color(0xFF0F172A),
        surface = Color(0xFF111827),
        onPrimary = Color.White,
        onBackground = Color(0xFFE5E7EB),
        onSurface = Color(0xFFE5E7EB)
    )

    MaterialTheme(
        colorScheme = if (darkTheme) darkColors else lightColors,
        content = content
    )
}