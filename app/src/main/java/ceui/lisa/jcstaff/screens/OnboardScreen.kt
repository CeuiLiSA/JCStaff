package ceui.lisa.jcstaff.screens

import android.content.Context
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.R
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import kotlin.random.Random

// region — JSON parsing helpers

internal fun extractOriginalUrl(illust: JsonObject): String? {
    illust.getAsJsonObject("meta_single_page")
        ?.get("original_image_url")
        ?.takeIf { !it.isJsonNull }
        ?.asString
        ?.takeIf { it.isNotBlank() }
        ?.let { return it }

    illust.getAsJsonArray("meta_pages")
        ?.takeIf { it.size() > 0 }
        ?.get(0)?.asJsonObject
        ?.getAsJsonObject("image_urls")
        ?.get("original")
        ?.asString
        ?.takeIf { it.isNotBlank() }
        ?.let { return it }

    return illust.getAsJsonObject("image_urls")
        ?.get("large")
        ?.asString
}

internal fun loadLandingImageUrls(context: Context): List<String> {
    return try {
        val json = context.assets.open("landing_bg.json")
            .bufferedReader()
            .use { it.readText() }
        val root = Gson().fromJson(json, JsonObject::class.java)
        val illusts = root.getAsJsonArray("illusts")
        val type = object : TypeToken<List<JsonObject>>() {}.type
        val list: List<JsonObject> = Gson().fromJson(illusts, type)
        list.mapNotNull { extractOriginalUrl(it) }
            .distinct()
            .shuffled()
    } catch (_: Exception) {
        emptyList()
    }
}

// endregion

internal fun createLandingImageLoader(context: Context): ImageLoader {
    val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Referer", "https://app-api.pixiv.net/")
                .build()
            chain.proceed(request)
        }
        .build()

    return ImageLoader.Builder(context)
        .okHttpClient(client)
        .memoryCache {
            MemoryCache.Builder(context)
                .maxSizePercent(0.10)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("landing_cache"))
                .maxSizeBytes(80L * 1024 * 1024)
                .build()
        }
        .crossfade(false)
        .build()
}

// region — Ken Burns animation parameters

private data class KenBurnsParams(
    val startScale: Float,
    val endScale: Float,
    val startTranslateX: Float,
    val endTranslateX: Float,
    val startTranslateY: Float,
    val endTranslateY: Float,
)

private fun randomKenBurnsParams(): KenBurnsParams {
    val zoomIn = Random.nextBoolean()
    val startScale = if (zoomIn) 1.0f else 1.12f
    val endScale = if (zoomIn) 1.12f else 1.0f

    val driftX = Random.nextFloat() * 60f - 30f
    val driftY = Random.nextFloat() * 40f - 20f

    return KenBurnsParams(
        startScale = startScale,
        endScale = endScale,
        startTranslateX = -driftX,
        endTranslateX = driftX,
        startTranslateY = -driftY,
        endTranslateY = driftY,
    )
}

// endregion

private const val SLIDE_INTERVAL_MS = 8_000L
private const val CROSSFADE_DURATION_MS = 2_800

@Composable
fun OnboardScreen(
    imageUrls: List<String>,
    imageLoader: ImageLoader,
    currentIndex: Int,
    onIndexChange: (Int) -> Unit,
    onLoginClick: () -> Unit,
    onSignupClick: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(imageUrls) {
        if (imageUrls.isEmpty()) return@LaunchedEffect
        var index = currentIndex
        while (true) {
            val nextIndex = (index + 1) % imageUrls.size
            val preloadRequest = ImageRequest.Builder(context)
                .data(imageUrls[nextIndex])
                .size(Size.ORIGINAL)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()
            imageLoader.enqueue(preloadRequest)
            delay(SLIDE_INTERVAL_MS)
            onIndexChange(nextIndex)
            index = nextIndex
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background slideshow with Ken Burns effect
        if (imageUrls.isNotEmpty()) {
            Crossfade(
                targetState = currentIndex,
                animationSpec = tween(CROSSFADE_DURATION_MS),
                label = "onboard_slideshow"
            ) { index ->
                val params = remember { randomKenBurnsParams() }
                val progress = remember { Animatable(0f) }

                LaunchedEffect(Unit) {
                    progress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = (SLIDE_INTERVAL_MS + CROSSFADE_DURATION_MS).toInt(),
                            easing = LinearEasing
                        )
                    )
                }

                val p = progress.value
                val scale = params.startScale + (params.endScale - params.startScale) * p
                val tx = params.startTranslateX + (params.endTranslateX - params.startTranslateX) * p
                val ty = params.startTranslateY + (params.endTranslateY - params.startTranslateY) * p

                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUrls[index % imageUrls.size])
                        .size(Size.ORIGINAL)
                        .crossfade(false)
                        .build(),
                    imageLoader = imageLoader,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = tx
                            translationY = ty
                        }
                )
            }
        }

        // Gradient scrim
        GradientScrim()

        // Bottom-anchored content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.please_login_to_continue),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.70f)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Primary action — Login
            Button(
                onClick = onLoginClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                )
            ) {
                Text(
                    text = stringResource(R.string.login),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Secondary action — Sign up
            OutlinedButton(
                onClick = onSignupClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.45f)
                )
            ) {
                Text(
                    text = stringResource(R.string.signup),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}
