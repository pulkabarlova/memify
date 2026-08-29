package com.codekotliners.memify.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import com.codekotliners.memify.core.navigation.BottomNavigationBar
import com.codekotliners.memify.core.navigation.entities.AppRoute
import com.codekotliners.memify.core.navigation.entities.TopLevelDestination
import com.codekotliners.memify.core.navigation.navigateToSettings
import com.codekotliners.memify.core.navigation.navigateToTopLevelDestination
import com.codekotliners.memify.core.ui.LocalNavAnimatedVisibilityScope
import com.codekotliners.memify.features.auth.presentation.ui.AuthScreen
import com.codekotliners.memify.features.auth.presentation.ui.LoginScreen
import com.codekotliners.memify.features.auth.presentation.ui.RegistrationScreen
import com.codekotliners.memify.features.create.presentation.ui.CreateScreen
import com.codekotliners.memify.features.home.presentation.ui.HomeScreen
import com.codekotliners.memify.features.profile.presentation.ui.ProfileScreen
import com.codekotliners.memify.features.settings.presentation.ui.AboutAppScreen
import com.codekotliners.memify.features.settings.presentation.ui.SettingsScreen
import com.codekotliners.memify.features.viewer.presentation.ui.ImageViewerScreen

@Suppress("detekt.LongMethod")
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MemifyNavHost(
    navController: NavHostController,
    onDrawBehindStatusBarChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val drawBehindStatusBar =
        currentBackStackEntry?.destination?.hasRoute<AppRoute.Auth>() == true

    LaunchedEffect(drawBehindStatusBar) {
        onDrawBehindStatusBarChanged(drawBehindStatusBar)
    }

    val bottomBar: @Composable () -> Unit = {
        BottomNavigationBar(
            currentDestination = currentBackStackEntry?.destination,
            onDestinationSelected = navController::navigateToTopLevelDestination,
        )
    }

    NavHost(
        navController = navController,
        startDestination = AppRoute.Home,
        modifier = modifier,
    ) {
        composable<AppRoute.Home> { backStackEntry ->
            val authenticated = backStackEntry.navigationResult(NavigationResult.AUTHENTICATED)
            val refreshRequested = backStackEntry.navigationResult(NavigationResult.HOME_REFRESH)

            CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                HomeScreen(
                    refreshRequested = authenticated || refreshRequested,
                    onRefreshHandled = {
                        backStackEntry.consumeNavigationResult(NavigationResult.AUTHENTICATED)
                        backStackEntry.consumeNavigationResult(NavigationResult.HOME_REFRESH)
                    },
                    onNavigateToAuth = { navController.navigate(AppRoute.Auth) },
                    onPostClick = { postId ->
                        navController.navigate(
                            AppRoute.ImageViewer(
                                imageId = postId,
                            ),
                        )
                    },
                    bottomBar = bottomBar,
                )
            }
        }

        composable<AppRoute.Create> { backStackEntry ->
            val route = backStackEntry.toRoute<AppRoute.Create>()
            val authenticated = backStackEntry.navigationResult(NavigationResult.AUTHENTICATED)

            CreateScreen(
                imageUrl = route.imageUrl,
                authChanged = authenticated,
                onAuthChangedHandled = {
                    backStackEntry.consumeNavigationResult(NavigationResult.AUTHENTICATED)
                },
                onNavigateHome = {
                    navController.navigateToTopLevelDestination(TopLevelDestination.Home)
                },
                onLogin = { navController.navigate(AppRoute.Auth) },
                onImagePublished = {
                    navController.previousBackStackEntry
                        ?.setNavigationResult(NavigationResult.HOME_REFRESH)
                },
                bottomBar = bottomBar,
            )
        }

        composable<AppRoute.Profile> { backStackEntry ->
            val authenticated = backStackEntry.navigationResult(NavigationResult.AUTHENTICATED)
            val refreshRequested = backStackEntry.navigationResult(NavigationResult.PROFILE_REFRESH)

            ProfileScreen(
                refreshRequested = authenticated || refreshRequested,
                onRefreshHandled = {
                    backStackEntry.consumeNavigationResult(NavigationResult.AUTHENTICATED)
                    backStackEntry.consumeNavigationResult(NavigationResult.PROFILE_REFRESH)
                },
                onSettingsClick = { isAuthenticated, displayName, avatarUrl ->
                    navController.navigateToSettings(
                        isAuthenticated = isAuthenticated,
                        displayName = displayName,
                        avatarUrl = avatarUrl,
                    )
                },
                onLoginClick = { navController.navigate(AppRoute.Auth) },
                bottomBar = bottomBar,
            )
        }

        composable<AppRoute.Settings> { backStackEntry ->
            val authenticated = backStackEntry.navigationResult(NavigationResult.AUTHENTICATED)

            SettingsScreen(
                authChanged = authenticated,
                onAuthChangedHandled = {
                    backStackEntry.consumeNavigationResult(NavigationResult.AUTHENTICATED)
                },
                onNavigateToAuth = { navController.navigate(AppRoute.Auth) },
                onAccountChanged = {
                    navController.previousBackStackEntry
                        ?.setNavigationResult(NavigationResult.PROFILE_REFRESH)
                },
                onBackClick = { navController.popBackStack() },
                onAboutAppClick = { navController.navigate(AppRoute.AboutApp) },
                bottomBar = bottomBar,
            )
        }

        composable<AppRoute.AboutApp> {
            AboutAppScreen(onBackClick = { navController.popBackStack() })
        }

        composable<AppRoute.Auth> { backStackEntry ->
            val branchAuthenticationCompleted =
                backStackEntry.navigationResult(NavigationResult.BRANCH_AUTHENTICATED)

            AuthScreen(
                branchAuthenticationCompleted = branchAuthenticationCompleted,
                onBranchAuthenticationHandled = {
                    backStackEntry.consumeNavigationResult(NavigationResult.BRANCH_AUTHENTICATED)
                },
                onAuthenticated = {
                    if (navController.popBackStack<AppRoute.Auth>(inclusive = true)) {
                        navController.currentBackStackEntry
                            ?.setNavigationResult(NavigationResult.AUTHENTICATED)
                    }
                },
                onNavigateToLogin = { navController.navigate(AppRoute.Login) },
                onNavigateToRegister = { navController.navigate(AppRoute.Register) },
            )
        }

        composable<AppRoute.Login>(
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { width -> width },
                    animationSpec = tween(durationMillis = 400),
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { width -> width },
                    animationSpec = tween(durationMillis = 300),
                )
            },
        ) {
            LoginScreen(
                onBackClick = { navController.popBackStack() },
                onLoginSucceeded = {
                    navController.previousBackStackEntry
                        ?.setNavigationResult(NavigationResult.BRANCH_AUTHENTICATED)
                    navController.popBackStack<AppRoute.Auth>(inclusive = false)
                },
            )
        }

        composable<AppRoute.Register>(
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { width -> width },
                    animationSpec = tween(durationMillis = 400),
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { width -> width },
                    animationSpec = tween(durationMillis = 300),
                )
            },
        ) {
            RegistrationScreen(
                onBackClick = { navController.popBackStack() },
                onRegistrationSucceeded = {
                    navController.previousBackStackEntry
                        ?.setNavigationResult(NavigationResult.BRANCH_AUTHENTICATED)
                    navController.popBackStack<AppRoute.Auth>(inclusive = false)
                },
            )
        }

        composable<AppRoute.ImageViewer> { backStackEntry ->
            val route = backStackEntry.toRoute<AppRoute.ImageViewer>()

            CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                ImageViewerScreen(
                    imageId = route.imageId,
                    onBack = { navController.popBackStack() },
                    onUseAsTemplate = { imageUrl ->
                        navController.navigate(AppRoute.Create(imageUrl = imageUrl)) {
                            popUpTo<AppRoute.Home>()
                            launchSingleTop = true
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun NavBackStackEntry.navigationResult(result: NavigationResult): Boolean {
    val value by
        savedStateHandle
            .getStateFlow(result.savedStateKey, false)
            .collectAsStateWithLifecycle()
    return value
}

private fun NavBackStackEntry.setNavigationResult(result: NavigationResult) {
    savedStateHandle[result.savedStateKey] = true
}

private fun NavBackStackEntry.consumeNavigationResult(result: NavigationResult) {
    savedStateHandle[result.savedStateKey] = false
}

private enum class NavigationResult(
    val savedStateKey: String,
) {
    AUTHENTICATED("navigation_result_authenticated"),
    BRANCH_AUTHENTICATED("navigation_result_branch_authenticated"),
    HOME_REFRESH("navigation_result_home_refresh"),
    PROFILE_REFRESH("navigation_result_profile_refresh"),
}
