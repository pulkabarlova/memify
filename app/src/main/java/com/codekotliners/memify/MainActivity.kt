package com.codekotliners.memify

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowInsets
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.codekotliners.memify.core.navigation.entities.TopLevelDestination
import com.codekotliners.memify.core.prefs.ThemeMode
import com.codekotliners.memify.core.theme.MemifyTheme
import com.codekotliners.memify.core.theme.surfaceDark
import com.codekotliners.memify.core.theme.surfaceLight
import dagger.hilt.android.AndroidEntryPoint

@Composable
fun SetSystemBarsBackground(
    window: Window,
    isDark: Boolean,
    drawBehindStatusBar: Boolean,
) {
    val color = (if (isDark) surfaceDark else surfaceLight).toArgb()
    val decor = window.decorView
    val insetsController = WindowCompat.getInsetsController(window, decor)

    SideEffect {
        insetsController.isAppearanceLightStatusBars = drawBehindStatusBar || !isDark
        insetsController.isAppearanceLightNavigationBars = !isDark

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            @Suppress("DEPRECATION")
            window.navigationBarColor = color
        }
    }

    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM -> {
            decor.setOnApplyWindowInsetsListener { view, insets ->
                val inset = insets.getInsets(WindowInsets.Type.statusBars()).top
                view.setPadding(
                    view.paddingLeft,
                    if (drawBehindStatusBar) 0 else inset,
                    view.paddingRight,
                    view.paddingBottom,
                )
                view.setBackgroundColor(color)
                view.setOnApplyWindowInsetsListener(null)
                insets
            }
            decor.requestApplyInsets()
        }
        else -> {
            val decorView = window.decorView
            val contentView = decorView.findViewById<View>(android.R.id.content)

            ViewCompat.setOnApplyWindowInsetsListener(contentView) { view, insets ->
                val statusBarInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
                view.setPadding(
                    view.paddingLeft,
                    if (drawBehindStatusBar) 0 else statusBarInset,
                    view.paddingRight,
                    view.paddingBottom,
                )
                insets
            }
            ViewCompat.requestApplyInsets(decorView)

            @Suppress("DEPRECATION")
            window.statusBarColor = if (drawBehindStatusBar) Color.TRANSPARENT else color
        }
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appThemeViewModel: AppThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val launchDestination =
            ShortcutDestination.fromValue(
                intent?.getStringExtra(SHORTCUT_DESTINATION_EXTRA),
            )

        enableEdgeToEdge()

        setContent {
            val themeMode by appThemeViewModel.themeMode.collectAsState()
            var drawBehindStatusBar by remember { mutableStateOf(false) }
            val themeKind =
                when (themeMode) {
                    ThemeMode.DARK_MODE -> true
                    ThemeMode.LIGHT_MODE -> false
                    else -> isSystemInDarkTheme()
                }

            SetSystemBarsBackground(
                window = window,
                isDark = themeKind,
                drawBehindStatusBar = drawBehindStatusBar,
            )

            MemifyTheme(
                dynamicColor = false,
                darkTheme = themeKind,
            ) {
                App(
                    launchDestination = launchDestination,
                    onDrawBehindStatusBarChanged = { drawBehindStatusBar = it },
                )
            }
        }
    }
}

private const val SHORTCUT_DESTINATION_EXTRA = "shortcut_destination"

private enum class ShortcutDestination(
    val value: String,
    val destination: TopLevelDestination,
) {
    CREATE(
        value = "creation",
        destination = TopLevelDestination.Create,
    ),
    ;

    companion object {
        fun fromValue(value: String?): TopLevelDestination? =
            entries.firstOrNull { destination -> destination.value == value }?.destination
    }
}
