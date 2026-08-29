package com.codekotliners.memify

import android.content.res.Configuration
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.codekotliners.memify.core.navigation.entities.TopLevelDestination
import com.codekotliners.memify.core.navigation.navigateToTopLevelDestination
import com.codekotliners.memify.core.theme.MemifyTheme
import com.codekotliners.memify.core.ui.LocalSharedTransitionScope
import com.codekotliners.memify.navigation.MemifyNavHost

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun App(
    launchDestination: TopLevelDestination?,
    onDrawBehindStatusBarChanged: (Boolean) -> Unit = {},
) {
    val navController = rememberNavController()

    LaunchedEffect(launchDestination) {
        if (launchDestination != null) {
            navController.navigateToTopLevelDestination(launchDestination)
        }
    }

    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            MemifyNavHost(
                navController = navController,
                onDrawBehindStatusBarChanged = onDrawBehindStatusBarChanged,
            )
        }
    }
}

@Preview(name = "Light Mode", showSystemUi = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showSystemUi = true)
@Composable
fun AppPreview() {
    MemifyTheme {
        App(launchDestination = null)
    }
}
