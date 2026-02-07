package ceui.lisa.jcstaff.screens

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Message
import android.util.Log
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel
import ceui.lisa.jcstaff.navigation.NavRoute
import ceui.lisa.jcstaff.navigation.NavigationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import kotlin.math.min

private const val TAG = "SauceNaoScreen"

private sealed class SauceNaoState {
    data object Idle : SauceNaoState()
    data object Uploading : SauceNaoState()
    data class Result(val html: String) : SauceNaoState()
    data class Error(val message: String) : SauceNaoState()
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SauceNaoScreen() {
    val navViewModel = LocalNavigationViewModel.current
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    val coroutineScope = rememberCoroutineScope()

    var state by remember { mutableStateOf<SauceNaoState>(SauceNaoState.Idle) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    // Clean up WebView native resources when leaving composition
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            webView?.destroy()
            webView = null
        }
    }
    var canGoBack by remember { mutableStateOf(false) }
    var pageTitle by remember { mutableStateOf(context.getString(R.string.sauce_nao)) }
    var loadingProgress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(false) }

    // File chooser callback for WebView
    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            state = SauceNaoState.Uploading
            coroutineScope.launch {
                val result = uploadImageToSauceNao(context, uri)
                state = result
            }
        }
        // Also handle WebView file chooser callback if present
        filePathCallback?.onReceiveValue(uri?.let { arrayOf(it) } ?: arrayOf())
        filePathCallback = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = pageTitle,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            navigationIcon = {
                IconButton(onClick = {
                    when {
                        canGoBack -> webView?.goBack()
                        state is SauceNaoState.Result || state is SauceNaoState.Error -> {
                            state = SauceNaoState.Idle
                            canGoBack = false
                            pageTitle = context.getString(R.string.sauce_nao)
                        }
                        else -> navViewModel.goBack()
                    }
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            },
            actions = {
                if (state is SauceNaoState.Result) {
                    IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Select Image"
                        )
                    }
                    IconButton(onClick = { webView?.reload() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            ),
            windowInsets = WindowInsets.statusBars
        )

        // Progress bar
        if (isLoading && loadingProgress < 1f) {
            LinearProgressIndicator(
                progress = { loadingProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (val currentState = state) {
                is SauceNaoState.Idle -> {
                    // Show image picker UI
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "SauceNAO",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "选择图片搜索其来源",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { imagePickerLauncher.launch("image/*") }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("选择图片")
                        }
                    }
                }

                is SauceNaoState.Uploading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "正在搜索...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is SauceNaoState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "搜索失败",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { imagePickerLauncher.launch("image/*") }
                        ) {
                            Text("重新选择图片")
                        }
                    }
                }

                is SauceNaoState.Result -> {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                    setSupportZoom(true)
                                    allowFileAccess = true
                                    allowContentAccess = true
                                    // Support multiple windows for target="_blank" links
                                    setSupportMultipleWindows(true)
                                    javaScriptCanOpenWindowsAutomatically = true
                                }

                                // Enable dark mode
                                if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                                    WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, isDarkTheme)
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        val url = request?.url?.toString() ?: return false
                                        Log.d(TAG, "shouldOverrideUrlLoading: $url")
                                        return handleUrlIntercept(url, navViewModel)
                                    }

                                    @Deprecated("Deprecated in Java")
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        url: String?
                                    ): Boolean {
                                        Log.d(TAG, "shouldOverrideUrlLoading (deprecated): $url")
                                        return url?.let { handleUrlIntercept(it, navViewModel) } ?: false
                                    }

                                    override fun onPageStarted(
                                        view: WebView?,
                                        url: String?,
                                        favicon: Bitmap?
                                    ) {
                                        super.onPageStarted(view, url, favicon)
                                        isLoading = true
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isLoading = false
                                        canGoBack = view?.canGoBack() == true
                                    }
                                }

                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        loadingProgress = newProgress / 100f
                                    }

                                    override fun onReceivedTitle(view: WebView?, title: String?) {
                                        pageTitle = title ?: context.getString(R.string.sauce_nao)
                                    }

                                    override fun onShowFileChooser(
                                        webView: WebView?,
                                        callback: ValueCallback<Array<Uri>>?,
                                        fileChooserParams: FileChooserParams?
                                    ): Boolean {
                                        filePathCallback?.onReceiveValue(null)
                                        filePathCallback = callback
                                        imagePickerLauncher.launch("image/*")
                                        return true
                                    }

                                    // Handle target="_blank" and window.open()
                                    override fun onCreateWindow(
                                        view: WebView?,
                                        isDialog: Boolean,
                                        isUserGesture: Boolean,
                                        resultMsg: Message?
                                    ): Boolean {
                                        Log.d(TAG, "onCreateWindow called")
                                        // Get the URL from the clicked link
                                        val result = view?.hitTestResult
                                        val url = result?.extra
                                        Log.d(TAG, "onCreateWindow URL from hitTest: $url")

                                        if (url != null && handleUrlIntercept(url, navViewModel)) {
                                            return false // URL handled, don't create new window
                                        }

                                        // Create a temporary WebView to capture the URL
                                        val tempWebView = WebView(view?.context ?: return false)
                                        tempWebView.webViewClient = object : WebViewClient() {
                                            override fun shouldOverrideUrlLoading(
                                                view: WebView?,
                                                request: WebResourceRequest?
                                            ): Boolean {
                                                val targetUrl = request?.url?.toString() ?: return false
                                                Log.d(TAG, "onCreateWindow captured URL: $targetUrl")
                                                if (handleUrlIntercept(targetUrl, navViewModel)) {
                                                    tempWebView.destroy()
                                                    return true
                                                }
                                                // Load in main WebView instead of new window
                                                webView?.loadUrl(targetUrl)
                                                tempWebView.destroy()
                                                return true
                                            }
                                        }

                                        val transport = resultMsg?.obj as? WebView.WebViewTransport
                                        transport?.webView = tempWebView
                                        resultMsg?.sendToTarget()
                                        return true
                                    }

                                    override fun onCloseWindow(window: WebView?) {
                                        // Do nothing, we handle window closure ourselves
                                    }
                                }

                                // Load HTML result
                                loadDataWithBaseURL(
                                    "https://saucenao.com/",
                                    currentState.html,
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                                webView = this
                            }
                        },
                        update = { view ->
                            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                                WebSettingsCompat.setAlgorithmicDarkeningAllowed(view.settings, isDarkTheme)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * Upload image to SauceNAO using POST request
 */
private suspend fun uploadImageToSauceNao(context: Context, uri: Uri): SauceNaoState {
    return withContext(Dispatchers.IO) {
        try {
            // Read and compress image
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext SauceNaoState.Error("无法读取图片")

            val originalBytes = inputStream.readBytes()
            inputStream.close()

            Log.d(TAG, "Original image size: ${originalBytes.size}")
            val compressedBytes = compressImage(originalBytes)
            Log.d(TAG, "Compressed image size: ${compressedBytes.size}")

            // Build multipart request - match pixez-flutter format
            val client = OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .build()

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    "image.jpg",
                    compressedBytes.toRequestBody("image/jpeg".toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url("https://saucenao.com/search.php")
                .header("Host", "saucenao.com")
                .post(requestBody)
                .build()

            Log.d(TAG, "Sending POST request to SauceNAO...")
            val response = client.newCall(request).execute()
            Log.d(TAG, "Response code: ${response.code}")

            if (response.isSuccessful) {
                val html = response.body?.string() ?: return@withContext SauceNaoState.Error("响应为空")
                Log.d(TAG, "Response HTML length: ${html.length}")

                // Check if HTML contains error message
                if (html.contains("Anonymous GET queries") && html.contains("blocked due to abuse")) {
                    Log.w(TAG, "SauceNAO returned blocked message in results")
                    // This is normal - it's part of search results, not a request error
                }

                SauceNaoState.Result(html)
            } else {
                val errorBody = response.body?.string() ?: ""
                Log.e(TAG, "Request failed: ${response.code}, body: $errorBody")
                SauceNaoState.Error("请求失败: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}", e)
            SauceNaoState.Error(e.message ?: "未知错误")
        }
    }
}

/**
 * Compress image to reduce upload size
 * Target: max 720px on shortest side, JPEG quality 75%
 */
private fun compressImage(originalBytes: ByteArray): ByteArray {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.size, options)

    val originalWidth = options.outWidth
    val originalHeight = options.outHeight

    // Calculate sample size for initial decode
    val targetSize = 720
    val minSide = min(originalWidth, originalHeight)
    var sampleSize = 1
    while (minSide / sampleSize > targetSize * 2) {
        sampleSize *= 2
    }

    // Decode with sample size
    val decodeOptions = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
    }
    val bitmap = BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.size, decodeOptions)
        ?: return originalBytes

    // Scale if needed
    val scaledBitmap = if (min(bitmap.width, bitmap.height) > targetSize) {
        val scale = targetSize.toFloat() / min(bitmap.width, bitmap.height)
        val newWidth = (bitmap.width * scale).toInt()
        val newHeight = (bitmap.height * scale).toInt()
        Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true).also {
            if (it != bitmap) bitmap.recycle()
        }
    } else {
        bitmap
    }

    // Compress to JPEG
    val outputStream = ByteArrayOutputStream()
    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
    scaledBitmap.recycle()

    return outputStream.toByteArray()
}

/**
 * Intercept URLs and navigate within the app when possible.
 */
private fun handleUrlIntercept(url: String, navViewModel: NavigationViewModel): Boolean {
    Log.d(TAG, "handleUrlIntercept: $url")
    val uri = Uri.parse(url)
    val host = uri.host ?: return false
    val scheme = uri.scheme ?: ""

    // Handle pixiv:// scheme
    if (scheme == "pixiv") {
        Log.d(TAG, "Intercepted pixiv:// URL")
        // Handle pixiv://illusts/123456 or similar
        val pathSegments = uri.pathSegments
        if (pathSegments.isNotEmpty()) {
            when (pathSegments[0]) {
                "illusts", "artworks" -> {
                    val illustId = pathSegments.getOrNull(1)?.toLongOrNull()
                    if (illustId != null) {
                        navViewModel.navigate(
                            NavRoute.IllustDetail(
                                illustId = illustId,
                                title = "",
                                previewUrl = "",
                                aspectRatio = 1f
                            )
                        )
                        return true
                    }
                }
                "users" -> {
                    val userId = pathSegments.getOrNull(1)?.toLongOrNull()
                    if (userId != null) {
                        navViewModel.navigate(NavRoute.UserProfile(userId = userId))
                        return true
                    }
                }
            }
        }
        return true // Consume pixiv:// URLs even if not parsed
    }

    // Handle Pixiv web URLs
    if (host.contains("pixiv")) {
        Log.d(TAG, "Intercepted pixiv web URL")
        val path = uri.path ?: ""
        val pathSegments = uri.pathSegments

        // Artwork/Illust detail: /artworks/123456 or /en/artworks/123456
        if (pathSegments.contains("artworks")) {
            val artworkIndex = pathSegments.indexOf("artworks")
            if (artworkIndex >= 0 && artworkIndex < pathSegments.size - 1) {
                val illustId = pathSegments[artworkIndex + 1].toLongOrNull()
                if (illustId != null) {
                    Log.d(TAG, "Navigating to IllustDetail: $illustId")
                    navViewModel.navigate(
                        NavRoute.IllustDetail(
                            illustId = illustId,
                            title = "",
                            previewUrl = "",
                            aspectRatio = 1f
                        )
                    )
                    return true
                }
            }
        }

        // User profile: /users/123456 or /en/users/123456
        if (pathSegments.contains("users")) {
            val userIndex = pathSegments.indexOf("users")
            if (userIndex >= 0 && userIndex < pathSegments.size - 1) {
                val userId = pathSegments[userIndex + 1].toLongOrNull()
                if (userId != null) {
                    Log.d(TAG, "Navigating to UserProfile: $userId")
                    navViewModel.navigate(NavRoute.UserProfile(userId = userId))
                    return true
                }
            }
        }

        // Old format: member_illust.php?illust_id=123456
        if (path.contains("member_illust.php")) {
            val illustId = uri.getQueryParameter("illust_id")?.toLongOrNull()
            if (illustId != null) {
                Log.d(TAG, "Navigating to IllustDetail (old format): $illustId")
                navViewModel.navigate(
                    NavRoute.IllustDetail(
                        illustId = illustId,
                        title = "",
                        previewUrl = "",
                        aspectRatio = 1f
                    )
                )
                return true
            }
        }

        // Old format: member.php?id=123456
        if (path.contains("member.php")) {
            val userId = uri.getQueryParameter("id")?.toLongOrNull()
            if (userId != null) {
                Log.d(TAG, "Navigating to UserProfile (old format): $userId")
                navViewModel.navigate(NavRoute.UserProfile(userId = userId))
                return true
            }
        }

        // Generic pixiv URL - still intercept to prevent opening in browser
        Log.d(TAG, "Generic pixiv URL intercepted but not handled: $url")
        return true
    }

    return false
}
