package ceui.lisa.jcstaff

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import ceui.lisa.jcstaff.auth.AuthState
import ceui.lisa.jcstaff.auth.AuthViewModel
import ceui.lisa.jcstaff.auth.LoginState
import ceui.lisa.jcstaff.components.LoadingIndicator
import ceui.lisa.jcstaff.components.animations.LocalSharedTransitionScope
import ceui.lisa.jcstaff.components.animations.LocalAnimatedVisibilityScope
import ceui.lisa.jcstaff.components.appswitcher.AppSwitcherDemoScreen
import ceui.lisa.jcstaff.components.appswitcher.AppSwitcherFab
import ceui.lisa.jcstaff.components.appswitcher.AppSwitcherOverlay
import ceui.lisa.jcstaff.components.appswitcher.ScreenshotCapture
import ceui.lisa.jcstaff.core.LanguageManager
import ceui.lisa.jcstaff.core.LocalSelectionManager
import ceui.lisa.jcstaff.core.SelectionManager
import ceui.lisa.jcstaff.home.HomeScreen
import ceui.lisa.jcstaff.navigation.BackStackEntry
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel
import ceui.lisa.jcstaff.navigation.NavRoute
import ceui.lisa.jcstaff.navigation.NavigationDirection
import ceui.lisa.jcstaff.navigation.NavigationViewModel
import ceui.lisa.jcstaff.network.PixivClient
import ceui.lisa.jcstaff.screens.AccountManagementScreen
import ceui.lisa.jcstaff.screens.BlockSettingsScreen
import ceui.lisa.jcstaff.screens.CollectionDiscoveryScreen
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
import ceui.lisa.jcstaff.screens.UserBookmarkNovelsScreen
import ceui.lisa.jcstaff.screens.UserCreatedIllustsScreen
import ceui.lisa.jcstaff.screens.UserCreatedNovelsScreen
import ceui.lisa.jcstaff.screens.CollectionDetailScreen
import ceui.lisa.jcstaff.screens.UserFollowingScreen
import ceui.lisa.jcstaff.screens.UserProfileScreen
import ceui.lisa.jcstaff.ui.theme.JCStaffTheme
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val navViewModel: NavigationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // 安装 SplashScreen，必须在 super.onCreate() 之前调用
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.authState.collect { authState ->
                    when (authState) {
                        is AuthState.Authenticated -> {
                            val stack = navViewModel.backStack.value
                            if (stack.isEmpty() || stack.first().route == NavRoute.Landing) {
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
            }
        }

        // 让 SplashScreen 持续显示，直到导航就绪
        // 使用 StateFlow 而非 SnapshotStateList，因为 setKeepOnScreenCondition
        // 在渲染线程调用，需要线程安全的状态读取
        splashScreen.setKeepOnScreenCondition {
            !navViewModel.isReady.value
        }

        // 处理启动时的 deep link
        handleDeepLink(intent)

        setContent {
            LocalizedContent {
                JCStaffTheme {
                    AppNavigation(authViewModel, navViewModel)
                }
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

/**
 * 保留 Activity 的全部能力（startActivity 等），只覆盖 resources 返回本地化资源。
 */
private class LocaleContextWrapper(
    base: Context,
    locale: Locale
) : android.content.ContextWrapper(base) {
    private val localizedResources: android.content.res.Resources =
        base.createConfigurationContext(
            Configuration(base.resources.configuration).apply { setLocale(locale) }
        ).resources

    override fun getResources(): android.content.res.Resources = localizedResources
}

/**
 * 用 Compose 的 Context 包装实现即时语言切换，不需要 Activity 重建。
 * 当 LanguageManager.currentLanguage 变化时，提供带新 Locale 的 Context，
 * 所有 stringResource() 自动返回新语言的文本。
 */
@Composable
fun LocalizedContent(content: @Composable () -> Unit) {
    val currentLanguage by LanguageManager.currentLanguage.collectAsState()
    val baseContext = LocalContext.current

    val localizedContext = remember(currentLanguage, baseContext) {
        val language = currentLanguage ?: return@remember baseContext
        val locale = Locale.forLanguageTag(language.tag)
        LocaleContextWrapper(baseContext, locale)
    }

    CompositionLocalProvider(LocalContext provides localizedContext) {
        content()
    }
}

@Composable
fun AppNavigation(authViewModel: AuthViewModel, navViewModel: NavigationViewModel) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    val loginState by authViewModel.loginState.collectAsState()
    val activeUserId by authViewModel.activeUserId.collectAsState()
    val allAccounts by authViewModel.allAccounts.collectAsState()

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

    // Predictive back gesture state
    var predictiveBackProgress by remember { mutableFloatStateOf(0f) }
    var predictiveBackSwipeEdge by remember { mutableIntStateOf(0) } // BackEventCompat.EDGE_LEFT
    var isPredictiveBack by remember { mutableStateOf(false) }
    var skipNextTransition by remember { mutableStateOf(false) }

    // 使用 StateFlow 安全读取
    val canGoBack by navViewModel.canGoBack.collectAsState()
    val currentEntry by navViewModel.currentEntry.collectAsState()
    val backStackList by navViewModel.backStack.collectAsState()

    PredictiveBackHandler(enabled = canGoBack) { backEvents ->
        try {
            isPredictiveBack = true
            predictiveBackProgress = 0f
            backEvents.collect { event ->
                predictiveBackProgress = event.progress
                predictiveBackSwipeEdge = event.swipeEdge
            }
            // Gesture completed — navigate back
            skipNextTransition = true
            isPredictiveBack = false
            predictiveBackProgress = 0f
            navViewModel.goBack()
        } catch (_: CancellationException) {
            // Gesture cancelled — reset state
            isPredictiveBack = false
            predictiveBackProgress = 0f
        }
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
    val appSwitcherState by navViewModel.appSwitcherState.collectAsState()
    val navigationDirection by navViewModel.navigationDirection.collectAsState()
    val density = LocalDensity.current

    // Material 3 emphasized easing curves
    val emphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val emphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

    CompositionLocalProvider(
        LocalNavigationViewModel provides navViewModel,
        LocalSelectionManager provides selectionManager
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Previous page preview during predictive back gesture
            if (isPredictiveBack && Build.VERSION.SDK_INT >= 34) {
                val prevKey = navViewModel.previousRouteKey
                val prevScreenshot = prevKey?.let { navViewModel.screenshotStore.getScreenshot(it) }
                if (prevScreenshot != null) {
                    Image(
                        bitmap = prevScreenshot,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    )
                }
            }

            // key(activeUserId) forces full recomposition on account switch, recreating all ViewModels
            key(activeUserId) {
                val entry = currentEntry
                // 当 ImageViewer 是当前路由时，让 AnimatedContent 显示上一个页面，
                // ImageViewer 作为透明 overlay 渲染在上面，这样拖拽时能看到底下的详情页
                if (entry != null) {
                val isImageViewer = entry.route is NavRoute.ImageViewer
                val contentEntry = if (isImageViewer) {
                    // 取 backStack 倒数第二个（即 ImageViewer 下面的页面）
                    backStackList.getOrNull(backStackList.size - 2) ?: entry
                } else {
                    entry
                }
                @OptIn(ExperimentalSharedTransitionApi::class)
                SharedTransitionLayout {
                AnimatedContent(
                    targetState = contentEntry,
                    transitionSpec = {
                        if (skipNextTransition) {
                            skipNextTransition = false
                            EnterTransition.None togetherWith ExitTransition.None using SizeTransform(
                                clip = false
                            )
                        } else {
                            val isForward = navigationDirection == NavigationDirection.FORWARD
                            val enterSlide = slideInHorizontally(
                                animationSpec = tween(400, easing = emphasizedDecelerate),
                                initialOffsetX = { fullWidth -> if (isForward) (fullWidth * 0.30f).toInt() else -(fullWidth * 0.30f).toInt() }
                            ) + fadeIn(
                                animationSpec = tween(
                                    150,
                                    delayMillis = 50,
                                    easing = emphasizedDecelerate
                                )
                            )
                            val exitSlide = slideOutHorizontally(
                                animationSpec = tween(400, easing = emphasizedAccelerate),
                                targetOffsetX = { fullWidth -> if (isForward) -(fullWidth * 0.30f).toInt() else (fullWidth * 0.30f).toInt() }
                            ) + fadeOut(
                                animationSpec = tween(100, easing = emphasizedAccelerate)
                            )
                            enterSlide togetherWith exitSlide using SizeTransform(clip = false)
                        }
                    },
                    label = "navigation",
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (isPredictiveBack && Build.VERSION.SDK_INT >= 34) {
                                val cornerRadius =
                                    with(density) { (40.dp * predictiveBackProgress).toPx() }
                                Modifier
                                    .graphicsLayer {
                                        val scale = 1f - (0.15f * predictiveBackProgress)
                                        scaleX = scale
                                        scaleY = scale
                                        // Shift toward the swipe edge
                                        val shiftFraction = 0.1f * predictiveBackProgress
                                        translationX =
                                            if (predictiveBackSwipeEdge == 0) { // EDGE_LEFT
                                                size.width * shiftFraction
                                            } else {
                                                -size.width * shiftFraction
                                            }
                                        clip = true
                                        shape = RoundedCornerShape(cornerRadius)
                                        alpha = if (appSwitcherState.isVisible) 0f else 1f
                                    }
                            } else {
                                Modifier.graphicsLayer {
                                    // Hide page content while app switcher is open.
                                    alpha = if (appSwitcherState.isVisible) 0f else 1f
                                }
                            }
                        )
                ) { entry ->
                    CompositionLocalProvider(
                        LocalSharedTransitionScope provides this@SharedTransitionLayout,
                        LocalAnimatedVisibilityScope provides this@AnimatedContent
                    ) {
                    val route = entry.route
                    saveableStateHolder.SaveableStateProvider(entry.screenshotKey) {
                        // Wrap content with ScreenshotCapture for app switcher.
                        // The background is inside the capture so the screenshot
                        // includes it (GraphicsLayer captures with transparency).
                        ScreenshotCapture(
                            key = entry.screenshotKey,
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
                                        // ImageViewer 作为 overlay 渲染，这里留空
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

                                    is NavRoute.AppSwitcherDemo -> {
                                        AppSwitcherDemoScreen()
                                    }

                                    is NavRoute.DemoPage -> {}

                                    is NavRoute.BlockSettings -> {
                                        BlockSettingsScreen()
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
                                        val isPremium =
                                            (authState as? AuthState.Authenticated)?.user?.is_premium == true
                                        TagDetailScreen(
                                            tag = route.tag,
                                            isPremium = isPremium
                                        )
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

                                    is NavRoute.UserCreatedIllusts -> {
                                        UserCreatedIllustsScreen(
                                            userId = route.userId,
                                            type = route.type
                                        )
                                    }

                                    is NavRoute.UserCreatedNovels -> {
                                        UserCreatedNovelsScreen(
                                            userId = route.userId
                                        )
                                    }

                                    is NavRoute.UserBookmarkNovels -> {
                                        UserBookmarkNovelsScreen(
                                            userId = route.userId
                                        )
                                    }

                                    is NavRoute.UserFollowing -> {
                                        UserFollowingScreen(
                                            userId = route.userId
                                        )
                                    }

                                    is NavRoute.CollectionDetail -> {
                                        CollectionDetailScreen(
                                            collectionId = route.collectionId
                                        )
                                    }

                                    is NavRoute.CollectionDiscovery -> {
                                        CollectionDiscoveryScreen()
                                    }
                                }
                            }
                        }
                    }
                    } // CompositionLocalProvider
                }
                } // SharedTransitionLayout

                // ImageViewer 作为透明 overlay 渲染在 AnimatedContent 之上
                if (isImageViewer) {
                    val route = entry!!.route as NavRoute.ImageViewer
                    ImageViewerScreen(
                        imageUrl = route.imageUrl,
                        originalUrl = route.originalUrl,
                        sharedElementKey = route.sharedElementKey
                    )
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
                backStack = backStackList,
                screenshotStore = navViewModel.screenshotStore,
                state = appSwitcherState,
                onCardClick = { navViewModel.navigateToIndex(it) },
                onSelectedIndexChange = { navViewModel.updateSelectedIndex(it) },
                onDismiss = { navViewModel.hideAppSwitcher() }
            )
        }
    }
}
