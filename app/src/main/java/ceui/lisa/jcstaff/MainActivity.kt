package ceui.lisa.jcstaff

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.auth.AuthState
import ceui.lisa.jcstaff.auth.AuthViewModel
import ceui.lisa.jcstaff.auth.LoginState
import ceui.lisa.jcstaff.components.LoadingIndicator
import ceui.lisa.jcstaff.components.appswitcher.AppSwitcherFab
import ceui.lisa.jcstaff.components.appswitcher.AppSwitcherOverlay
import ceui.lisa.jcstaff.components.appswitcher.ScreenshotCapture
import ceui.lisa.jcstaff.core.LanguageManager
import ceui.lisa.jcstaff.core.LocalSelectionManager
import ceui.lisa.jcstaff.core.SelectionManager
import ceui.lisa.jcstaff.core.SettingsStore
import ceui.lisa.jcstaff.home.HomeScreen
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel
import ceui.lisa.jcstaff.navigation.NavRoute
import ceui.lisa.jcstaff.navigation.NavigationViewModel
import ceui.lisa.jcstaff.network.PixivClient
import ceui.lisa.jcstaff.screens.AccountManagementScreen
import ceui.lisa.jcstaff.screens.BookmarksScreen
import ceui.lisa.jcstaff.screens.BrowseHistoryScreen
import ceui.lisa.jcstaff.screens.CacheBrowserScreen
import ceui.lisa.jcstaff.screens.CommentScreen
import ceui.lisa.jcstaff.screens.DownloadHistoryScreen
import ceui.lisa.jcstaff.screens.IllustDetailScreen
import ceui.lisa.jcstaff.screens.ImageViewerScreen
import ceui.lisa.jcstaff.screens.LandingScreen
import ceui.lisa.jcstaff.screens.LatestWorksScreen
import ceui.lisa.jcstaff.screens.NovelDetailScreen
import ceui.lisa.jcstaff.screens.RankingDetailScreen
import ceui.lisa.jcstaff.screens.SauceNaoScreen
import ceui.lisa.jcstaff.screens.SearchScreen
import ceui.lisa.jcstaff.screens.SettingsScreen
import ceui.lisa.jcstaff.screens.ShaderDemoScreen
import ceui.lisa.jcstaff.screens.SpotlightDetailScreen
import ceui.lisa.jcstaff.screens.TagDetailScreen
import ceui.lisa.jcstaff.screens.UgoiraRankingScreen
import ceui.lisa.jcstaff.screens.UserProfileScreen
import ceui.lisa.jcstaff.screens.WebTagDetailScreen
import ceui.lisa.jcstaff.ui.theme.JCStaffTheme
import coil.Coil
import coil.ImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // 安装 SplashScreen，必须在 super.onCreate() 之前调用
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 设置全局 Coil ImageLoader，使用共享的 imageClient（带 Referer 头）
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .okHttpClient { PixivClient.imageClient }
                .build()
        )

        // 初始化全局设置（语言等）
        SettingsStore.initialize(this)

        // 提前触发 AuthViewModel 创建，使 auth 初始化与语言加载并行执行
        authViewModel

        // 让 SplashScreen 持续显示，直到认证状态加载完成
        // 这样可以避免 Compose 首次组合时的 Loading 动画卡顿
        splashScreen.setKeepOnScreenCondition {
            authViewModel.authState.value is AuthState.Loading
        }

        // 异步初始化语言管理器（不再阻塞主线程）
        lifecycleScope.launch(Dispatchers.IO) {
            val savedTag = SettingsStore.selectedLanguage.first()
            LanguageManager.initialize(savedTag)
        }

        // 注意：per-user 的初始化（DB、缓存、浏览历史等）由 AuthViewModel → AccountSessionManager 负责

        // 处理启动时的 deep link
        handleDeepLink(intent)

        setContent {
            JCStaffTheme {
                AppNavigation(authViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        // 处理 pixiv://account/login?code=xxx 回调
        if (uri.scheme == "pixiv" && uri.host == "account" && uri.path == "/login") {
            uri.getQueryParameter("code")?.let { code ->
                val callbackUri = Uri.parse("${PixivClient.CALLBACK_URL}?code=$code")
                authViewModel.handleCallback(callbackUri)
            }
        }
    }
}

@Composable
fun AppNavigation(authViewModel: AuthViewModel) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    val loginState by authViewModel.loginState.collectAsState()
    val activeUserId by authViewModel.activeUserId.collectAsState()
    val allAccounts by authViewModel.allAccounts.collectAsState()
    val navViewModel: NavigationViewModel = viewModel()

    // Handle login state changes
    LaunchedEffect(loginState) {
        when (val state = loginState) {
            is LoginState.Success -> {
                Toast.makeText(
                    context,
                    context.getString(R.string.login_success),
                    Toast.LENGTH_SHORT
                ).show()
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
                if (navViewModel.backStack.isEmpty() || navViewModel.backStack.first() == NavRoute.Landing) {
                    navViewModel.clearAndNavigate(NavRoute.Home)
                }
            }

            is AuthState.NotAuthenticated -> {
                navViewModel.clearAndNavigate(NavRoute.Landing)
            }

            is AuthState.Loading -> {
                // 保持当前状态，等待加载完成
            }
        }
    }

    // 在 LaunchedEffect 尚未执行时，根据 authState 同步推导初始路由，避免空白帧
    val currentRoute = navViewModel.currentRoute ?: when (authState) {
        is AuthState.Authenticated -> NavRoute.Home
        is AuthState.NotAuthenticated -> NavRoute.Landing
        else -> null
    }

    BackHandler(enabled = navViewModel.canGoBack) {
        navViewModel.goBack()
    }

    // Loading 状态显示加载指示器
    if (authState is AuthState.Loading || currentRoute == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            LoadingIndicator()
        }
        return
    }

    // Show loading overlay during the entire login flow (Loading + Success).
    // Using early-return here removes LandingScreen from composition; if we only
    // covered Loading, the pager would re-mount at page 0 during the brief Success
    // → Idle → Authenticated transition, causing a visible flash of the first page.
    if (loginState is LoginState.Loading || loginState is LoginState.Success) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            LoadingIndicator()
        }
        return
    }

    val saveableStateHolder = rememberSaveableStateHolder()
    val selectionManager = remember { SelectionManager() }
    val appSwitcherState by navViewModel.appSwitcherState

    CompositionLocalProvider(
        LocalNavigationViewModel provides navViewModel,
        LocalSelectionManager provides selectionManager
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // key(activeUserId) forces full recomposition on account switch, recreating all ViewModels
            key(activeUserId) {
                AnimatedContent(
                    targetState = currentRoute,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut() using SizeTransform(clip = false)
                    },
                    label = "navigation",
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .graphicsLayer {
                            // Hide page content while app switcher is open.
                            // The overlay's screenshot covers everything; this prevents
                            // the AnimatedContent crossfade from flashing the old page.
                            alpha = if (appSwitcherState.isVisible) 0f else 1f
                        }
                ) { route ->
                    saveableStateHolder.SaveableStateProvider(route.stableKey) {
                        // Wrap content with ScreenshotCapture for app switcher.
                        // The background is inside the capture so the screenshot
                        // includes it (GraphicsLayer captures with transparency).
                        ScreenshotCapture(
                            key = route.stableKey,
                            screenshotStore = navViewModel.screenshotStore
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background)
                            ) {
                                when (route) {
                                    is NavRoute.Landing -> {
                                        LandingScreen(
                                            onLoginClick = {
                                                authViewModel.launchLogin(context, isSignup = false)
                                            },
                                            onSignupClick = {
                                                authViewModel.launchLogin(context, isSignup = true)
                                            }
                                        )
                                    }

                                    is NavRoute.Home -> {
                                        val currentUser =
                                            (authState as? AuthState.Authenticated)?.user
                                        HomeScreen(
                                            currentUser = currentUser,
                                            allAccounts = allAccounts,
                                            activeUserId = activeUserId,
                                            onSwitchAccount = { userId ->
                                                authViewModel.switchAccount(userId)
                                            },
                                            onAddAccount = {
                                                authViewModel.launchAddAccount(context)
                                            }
                                        )
                                    }

                                    is NavRoute.Search -> {
                                        SearchScreen()
                                    }

                                    is NavRoute.IllustDetail -> {
                                        IllustDetailScreen(
                                            illustId = route.illustId,
                                            title = route.title,
                                            previewUrl = route.previewUrl,
                                            aspectRatio = route.aspectRatio
                                        )
                                    }

                                    is NavRoute.TagDetail -> {
                                        val isPremium =
                                            (authState as? AuthState.Authenticated)?.user?.is_premium == true
                                        TagDetailScreen(
                                            tag = route.tag,
                                            isPremium = isPremium,
                                            initialTab = route.initialTab
                                        )
                                    }

                                    is NavRoute.NovelDetail -> {
                                        NovelDetailScreen(
                                            novelId = route.novelId
                                        )
                                    }

                                    is NavRoute.Bookmarks -> {
                                        BookmarksScreen(
                                            userId = route.userId
                                        )
                                    }

                                    is NavRoute.Settings -> {
                                        val currentUser =
                                            (authState as? AuthState.Authenticated)?.user
                                        SettingsScreen(
                                            currentUser = currentUser,
                                            onLogoutClick = {
                                                authViewModel.logout()
                                            }
                                        )
                                    }

                                    is NavRoute.ImageViewer -> {
                                        ImageViewerScreen(
                                            imageUrl = route.imageUrl,
                                            originalUrl = route.originalUrl,
                                            sharedElementKey = route.sharedElementKey
                                        )
                                    }

                                    is NavRoute.BrowseHistory -> {
                                        BrowseHistoryScreen()
                                    }

                                    is NavRoute.UserProfile -> {
                                        UserProfileScreen(
                                            userId = route.userId
                                        )
                                    }

                                    is NavRoute.ShaderDemo -> {
                                        ShaderDemoScreen()
                                    }

                                    is NavRoute.AccountManagement -> {
                                        AccountManagementScreen(
                                            allAccounts = allAccounts,
                                            activeUserId = activeUserId,
                                            onAddAccount = {
                                                authViewModel.launchAddAccount(context)
                                            },
                                            onRemoveAccount = { userId ->
                                                authViewModel.removeAccount(userId)
                                            },
                                            onSwitchAccount = { userId ->
                                                authViewModel.switchAccount(userId)
                                            }
                                        )
                                    }

                                    is NavRoute.CommentDetail -> {
                                        val currentUserId =
                                            (authState as? AuthState.Authenticated)?.user?.id ?: 0L
                                        CommentScreen(
                                            objectId = route.objectId,
                                            objectType = route.objectType,
                                            currentUserId = currentUserId
                                        )
                                    }

                                    is NavRoute.RankingDetail -> {
                                        RankingDetailScreen(
                                            objectType = route.objectType
                                        )
                                    }

                                    is NavRoute.SpotlightDetail -> {
                                        SpotlightDetailScreen(
                                            article = route.article
                                        )
                                    }

                                    is NavRoute.UgoiraRanking -> {
                                        UgoiraRankingScreen()
                                    }

                                    is NavRoute.LatestWorks -> {
                                        LatestWorksScreen()
                                    }

                                    is NavRoute.WebTagDetail -> {
                                        WebTagDetailScreen(tag = route.tag)
                                    }

                                    is NavRoute.CacheBrowser -> {
                                        CacheBrowserScreen(initialPath = route.initialPath)
                                    }

                                    is NavRoute.SauceNao -> {
                                        SauceNaoScreen()
                                    }

                                    is NavRoute.DownloadHistory -> {
                                        DownloadHistoryScreen()
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // App Switcher FAB (visible when depth >= 3)
            AppSwitcherFab(
                navigationDepth = navViewModel.navigationDepth,
                onClick = { navViewModel.showAppSwitcher() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
            )

            // App Switcher Overlay
            AppSwitcherOverlay(
                backStack = navViewModel.backStack.toList(),
                screenshotStore = navViewModel.screenshotStore,
                state = appSwitcherState,
                onCardClick = { navViewModel.navigateToIndex(it) },
                onSelectedIndexChange = { navViewModel.updateSelectedIndex(it) },
                onDismiss = { navViewModel.hideAppSwitcher() }
            )
        }
    }
}
