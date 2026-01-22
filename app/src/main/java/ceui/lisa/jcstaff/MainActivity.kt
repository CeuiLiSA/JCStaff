package ceui.lisa.jcstaff

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import ceui.lisa.jcstaff.cache.ApiCacheManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.auth.AuthState
import ceui.lisa.jcstaff.auth.AuthViewModel
import ceui.lisa.jcstaff.auth.LoginState
import ceui.lisa.jcstaff.home.HomeScreen
import ceui.lisa.jcstaff.navigation.NavRoute
import ceui.lisa.jcstaff.screens.BookmarksScreen
import ceui.lisa.jcstaff.screens.IllustDetailScreen
import ceui.lisa.jcstaff.screens.LandingScreen
import ceui.lisa.jcstaff.screens.LoginScreen
import ceui.lisa.jcstaff.screens.SettingsScreen
import ceui.lisa.jcstaff.screens.ImageViewerScreen
import ceui.lisa.jcstaff.core.LoadTaskManager
import ceui.lisa.jcstaff.core.SettingsStore
import ceui.lisa.jcstaff.ui.theme.JCStaffTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 初始化 API 缓存
        ApiCacheManager.initialize(this)

        // 初始化设置存储
        SettingsStore.initialize(this)

        // 初始化加载任务管理器
        LoadTaskManager.init(this)

        setContent {
            JCStaffTheme {
                AppNavigation()
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavigation(authViewModel: AuthViewModel = viewModel()) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    val loginState by authViewModel.loginState.collectAsState()
    val backStack = remember { mutableStateListOf<NavRoute>() }

    // Handle login state changes
    LaunchedEffect(loginState) {
        when (val state = loginState) {
            is LoginState.Success -> {
                Toast.makeText(context, "登录成功", Toast.LENGTH_SHORT).show()
                authViewModel.resetLoginState()
            }
            is LoginState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                authViewModel.resetLoginState()
            }
            else -> {}
        }
    }

    // 根据认证状态设置初始路由
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                if (backStack.isEmpty() || backStack.first() == NavRoute.Landing || backStack.any { it is NavRoute.Login }) {
                    backStack.clear()
                    backStack.add(NavRoute.Home)
                }
            }
            is AuthState.NotAuthenticated -> {
                if (backStack.none { it is NavRoute.Login }) {
                    backStack.clear()
                    backStack.add(NavRoute.Landing)
                }
            }
            is AuthState.Loading -> {
                // 保持当前状态，等待加载完成
            }
        }
    }

    val currentRoute = backStack.lastOrNull()

    BackHandler(enabled = backStack.size > 1) {
        backStack.removeLast()
    }

    // Loading 状态显示加载指示器
    if (authState is AuthState.Loading || currentRoute == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // Show loading overlay when logging in
    if (loginState is LoginState.Loading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    SharedTransitionLayout(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AnimatedContent(
            targetState = currentRoute,
            transitionSpec = {
                fadeIn() togetherWith fadeOut() using SizeTransform(clip = false)
            },
            label = "navigation"
        ) { route ->
            when (route) {
                is NavRoute.Landing -> {
                    LandingScreen(
                        onLoginClick = {
                            backStack.add(NavRoute.Login(isSignup = false))
                        },
                        onSignupClick = {
                            backStack.add(NavRoute.Login(isSignup = true))
                        }
                    )
                }
                is NavRoute.Login -> {
                    LoginScreen(
                        isSignup = route.isSignup,
                        onBack = {
                            backStack.removeLast()
                        },
                        onLoginCallback = { uri ->
                            authViewModel.handleCallback(uri)
                        }
                    )
                }
                is NavRoute.Home -> {
                    val currentUser = (authState as? AuthState.Authenticated)?.user
                    HomeScreen(
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedContentScope = this@AnimatedContent,
                        currentUser = currentUser,
                        onIllustClick = { data ->
                            backStack.add(NavRoute.IllustDetail(
                                illustId = data.id,
                                title = data.title,
                                previewUrl = data.previewUrl,
                                aspectRatio = data.aspectRatio
                            ))
                        },
                        onBookmarksClick = {
                            currentUser?.let { user ->
                                backStack.add(NavRoute.Bookmarks(userId = user.id))
                            }
                        },
                        onSettingsClick = {
                            backStack.add(NavRoute.Settings)
                        },
                        onLogoutClick = {
                            authViewModel.logout()
                        }
                    )
                }
                is NavRoute.IllustDetail -> {
                    IllustDetailScreen(
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedContentScope = this@AnimatedContent,
                        illustId = route.illustId,
                        title = route.title,
                        previewUrl = route.previewUrl,
                        aspectRatio = route.aspectRatio,
                        onBackClick = {
                            backStack.removeLast()
                        },
                        onRelatedIllustClick = { illust ->
                            backStack.add(NavRoute.IllustDetail(
                                illustId = illust.id,
                                title = illust.title ?: "",
                                previewUrl = illust.previewUrl(),
                                aspectRatio = illust.aspectRatio()
                            ))
                        },
                        onImageClick = { previewUrl, originalUrl ->
                            backStack.add(NavRoute.ImageViewer(
                                imageUrl = previewUrl,
                                originalUrl = originalUrl
                            ))
                        }
                    )
                }
                is NavRoute.Bookmarks -> {
                    BookmarksScreen(
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedContentScope = this@AnimatedContent,
                        userId = route.userId,
                        onBackClick = {
                            backStack.removeLast()
                        },
                        onIllustClick = { illust ->
                            backStack.add(NavRoute.IllustDetail(
                                illustId = illust.id,
                                title = illust.title ?: "",
                                previewUrl = illust.previewUrl(),
                                aspectRatio = illust.aspectRatio()
                            ))
                        }
                    )
                }
                is NavRoute.Settings -> {
                    SettingsScreen(
                        onBackClick = {
                            backStack.removeLast()
                        }
                    )
                }
                is NavRoute.ImageViewer -> {
                    ImageViewerScreen(
                        imageUrl = route.imageUrl,
                        originalUrl = route.originalUrl,
                        onBackClick = {
                            backStack.removeLast()
                        }
                    )
                }
            }
        }
    }
}