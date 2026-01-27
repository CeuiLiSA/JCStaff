package ceui.lisa.jcstaff

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import ceui.lisa.jcstaff.cache.ApiCacheManager
import ceui.lisa.jcstaff.cache.BrowseHistoryManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ceui.lisa.jcstaff.network.PixivClient
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
import ceui.lisa.jcstaff.auth.AuthState
import ceui.lisa.jcstaff.auth.AuthViewModel
import ceui.lisa.jcstaff.auth.LoginState
import ceui.lisa.jcstaff.home.HomeScreen
import ceui.lisa.jcstaff.navigation.NavRoute
import ceui.lisa.jcstaff.screens.BookmarksScreen
import ceui.lisa.jcstaff.screens.IllustDetailScreen
import ceui.lisa.jcstaff.screens.LandingScreen
import ceui.lisa.jcstaff.screens.SettingsScreen
import ceui.lisa.jcstaff.screens.ImageViewerScreen
import ceui.lisa.jcstaff.screens.BrowseHistoryScreen
import ceui.lisa.jcstaff.screens.SearchScreen
import ceui.lisa.jcstaff.screens.NovelDetailScreen
import ceui.lisa.jcstaff.screens.TagDetailScreen
import ceui.lisa.jcstaff.screens.UserProfileScreen
import ceui.lisa.jcstaff.core.LanguageManager
import ceui.lisa.jcstaff.core.LoadTaskManager
import ceui.lisa.jcstaff.core.SettingsStore
import ceui.lisa.jcstaff.screens.LanguageSelectionScreen
import ceui.lisa.jcstaff.ui.theme.JCStaffTheme

class MainActivity : AppCompatActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 初始化 API 缓存
        ApiCacheManager.initialize(this)

        // 初始化设置存储
        SettingsStore.initialize(this)

        // 初始化语言管理器
        LanguageManager.initialize(SettingsStore.getSelectedLanguageBlocking())

        // 初始化加载任务管理器
        LoadTaskManager.init(this)

        // 初始化浏览历史管理器
        BrowseHistoryManager.initialize(this)

        // 处理启动时的 deep link
        handleDeepLink(intent)

        setContent {
            JCStaffTheme {
                val isLanguageSelected by LanguageManager.isLanguageSelected.collectAsState()
                if (isLanguageSelected) {
                    AppNavigation(authViewModel)
                } else {
                    LanguageSelectionScreen()
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavigation(authViewModel: AuthViewModel) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    val loginState by authViewModel.loginState.collectAsState()
    val backStack = remember { mutableStateListOf<NavRoute>() }

    // Handle login state changes
    LaunchedEffect(loginState) {
        when (val state = loginState) {
            is LoginState.Success -> {
                Toast.makeText(context, context.getString(R.string.login_success), Toast.LENGTH_SHORT).show()
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
                if (backStack.isEmpty() || backStack.first() == NavRoute.Landing) {
                    backStack.clear()
                    backStack.add(NavRoute.Home)
                }
            }
            is AuthState.NotAuthenticated -> {
                backStack.clear()
                backStack.add(NavRoute.Landing)
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
                            authViewModel.launchLogin(context, isSignup = false)
                        },
                        onSignupClick = {
                            authViewModel.launchLogin(context, isSignup = true)
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
                        onNovelClick = { novel ->
                            backStack.add(NavRoute.NovelDetail(novelId = novel.id))
                        },
                        onSearchClick = {
                            backStack.add(NavRoute.Search)
                        },
                        onBookmarksClick = {
                            currentUser?.let { user ->
                                backStack.add(NavRoute.Bookmarks(userId = user.id))
                            }
                        },
                        onBrowseHistoryClick = {
                            backStack.add(NavRoute.BrowseHistory)
                        },
                        onUserProfileClick = {
                            currentUser?.let { user ->
                                backStack.add(NavRoute.UserProfile(userId = user.id))
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
                is NavRoute.Search -> {
                    SearchScreen(
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedContentScope = this@AnimatedContent,
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
                        },
                        onUserClick = { userId ->
                            backStack.add(NavRoute.UserProfile(userId = userId))
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
                        onImageClick = { previewUrl, originalUrl, sharedElementKey ->
                            backStack.add(NavRoute.ImageViewer(
                                imageUrl = previewUrl,
                                originalUrl = originalUrl,
                                sharedElementKey = sharedElementKey
                            ))
                        },
                        onUserClick = { userId ->
                            backStack.add(NavRoute.UserProfile(userId = userId))
                        },
                        onTagClick = { tag ->
                            backStack.add(NavRoute.TagDetail(tag = tag))
                        }
                    )
                }
                is NavRoute.TagDetail -> {
                    val isPremium = (authState as? AuthState.Authenticated)?.user?.is_premium == true
                    TagDetailScreen(
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedContentScope = this@AnimatedContent,
                        tag = route.tag,
                        isPremium = isPremium,
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
                is NavRoute.NovelDetail -> {
                    NovelDetailScreen(
                        novelId = route.novelId,
                        onBackClick = {
                            backStack.removeLast()
                        },
                        onUserClick = { userId ->
                            backStack.add(NavRoute.UserProfile(userId = userId))
                        },
                        onTagClick = { tag ->
                            backStack.add(NavRoute.TagDetail(tag = tag))
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
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedContentScope = this@AnimatedContent,
                        imageUrl = route.imageUrl,
                        originalUrl = route.originalUrl,
                        sharedElementKey = route.sharedElementKey,
                        onBackClick = {
                            backStack.removeLast()
                        }
                    )
                }
                is NavRoute.BrowseHistory -> {
                    BrowseHistoryScreen(
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedContentScope = this@AnimatedContent,
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
                is NavRoute.UserProfile -> {
                    UserProfileScreen(
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
            }
        }
    }
}