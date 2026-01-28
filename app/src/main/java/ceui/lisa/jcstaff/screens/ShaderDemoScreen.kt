package ceui.lisa.jcstaff.screens

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.components.SHADER_TRACED_TUNNEL
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel

// region ── Shader sources ──────────────────────────────────────────

private const val SHADER_NEON_PLASMA = """
uniform float2 iResolution;
uniform float  iTime;

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    float t = iTime * 0.8;

    float v = 0.0;
    v += sin(uv.x * 10.0 + t);
    v += sin((uv.y * 10.0 + t) * 0.5);
    v += sin((uv.x * 10.0 + uv.y * 10.0 + t) * 0.3);
    v += sin(length(uv * 10.0 - 5.0) + t);
    v *= 0.25;

    float3 col;
    col.r = sin(v * 3.14159 + t * 0.5) * 0.5 + 0.5;
    col.g = sin(v * 3.14159 + t * 0.5 + 2.094) * 0.5 + 0.5;
    col.b = sin(v * 3.14159 + t * 0.5 + 4.189) * 0.5 + 0.5;

    // Boost saturation toward neon cyan/magenta/yellow
    col = pow(col, float3(0.8));
    col = mix(col, float3(col.g, col.b, col.r) * 1.2, 0.3);

    return half4(half3(col), 1.0);
}
"""

private const val SHADER_VORONOI = """
uniform float2 iResolution;
uniform float  iTime;

float2 hash2(float2 p) {
    p = float2(dot(p, float2(127.1, 311.7)),
               dot(p, float2(269.5, 183.3)));
    return fract(sin(p) * 43758.5453);
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    uv.x *= iResolution.x / iResolution.y;
    float scale = 5.0;
    float2 st = uv * scale;
    float2 i_st = floor(st);
    float2 f_st = fract(st);

    float minDist = 1.0;
    float secondDist = 1.0;

    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            float2 neighbor = float2(float(x), float(y));
            float2 point = hash2(i_st + neighbor);
            point = 0.5 + 0.5 * sin(iTime * 0.6 + 6.2831 * point);
            float2 diff = neighbor + point - f_st;
            float dist = length(diff);
            if (dist < minDist) {
                secondDist = minDist;
                minDist = dist;
            } else if (dist < secondDist) {
                secondDist = dist;
            }
        }
    }

    float edge = secondDist - minDist;
    float glow = smoothstep(0.0, 0.08, edge);

    float3 col = mix(
        float3(0.9, 0.3, 0.1),
        float3(0.1, 0.4, 0.9),
        minDist
    );
    col = mix(float3(1.0, 0.95, 0.8), col, glow);
    col *= 0.7 + 0.3 * smoothstep(0.0, 0.5, minDist);

    return half4(half3(col), 1.0);
}
"""

private const val SHADER_AURORA = """
uniform float2 iResolution;
uniform float  iTime;

float hash(float2 p) {
    float h = dot(p, float2(127.1, 311.7));
    return fract(sin(h) * 43758.5453123);
}

float noise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float2 u = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(hash(i), hash(i + float2(1.0, 0.0)), u.x),
        mix(hash(i + float2(0.0, 1.0)), hash(i + float2(1.0, 1.0)), u.x),
        u.y
    );
}

float fbm(float2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 5; i++) {
        v += a * noise(p);
        p = p * 2.0 + float2(100.0);
        a *= 0.5;
    }
    return v;
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    float t = iTime * 0.3;

    // Dark sky background
    float3 sky = mix(float3(0.0, 0.02, 0.08), float3(0.02, 0.0, 0.06), uv.y);

    // Aurora curtains — layered FBM with vertical emphasis
    float aurora = 0.0;
    aurora += fbm(float2(uv.x * 3.0 + t * 0.4, uv.y * 0.8 - t * 0.1)) * 0.6;
    aurora += fbm(float2(uv.x * 5.0 - t * 0.3, uv.y * 1.2 + t * 0.05)) * 0.4;

    // Vertical streaks
    float streak = sin(uv.x * 20.0 + t * 2.0) * 0.5 + 0.5;
    streak *= smoothstep(0.7, 0.3, uv.y) * smoothstep(0.0, 0.2, uv.y);
    aurora *= streak + 0.5;

    // Mask to upper portion of screen
    float mask = smoothstep(0.8, 0.2, uv.y) * smoothstep(0.0, 0.15, uv.y);
    aurora *= mask;

    // Green-purple-blue palette
    float3 auroraColor = mix(
        float3(0.1, 0.8, 0.3),
        float3(0.4, 0.1, 0.8),
        aurora
    );
    auroraColor = mix(auroraColor, float3(0.2, 0.5, 0.9), sin(uv.x * 8.0 + t) * 0.3 + 0.3);

    float3 col = sky + auroraColor * aurora * 1.5;

    // Subtle stars
    float star = hash(floor(uv * 300.0));
    star = step(0.998, star) * (0.5 + 0.5 * sin(iTime * 3.0 + star * 100.0));
    col += float3(star);

    return half4(half3(col), 1.0);
}
"""

private const val SHADER_FIRE = """
uniform float2 iResolution;
uniform float  iTime;

float hash(float2 p) {
    float h = dot(p, float2(127.1, 311.7));
    return fract(sin(h) * 43758.5453123);
}

float noise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float2 u = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(hash(i), hash(i + float2(1.0, 0.0)), u.x),
        mix(hash(i + float2(0.0, 1.0)), hash(i + float2(1.0, 1.0)), u.x),
        u.y
    );
}

float fbm(float2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 6; i++) {
        v += a * noise(p);
        p = p * 2.0 + float2(100.0);
        a *= 0.5;
    }
    return v;
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    float t = iTime;

    // Scroll noise upward for fire effect
    float2 fireUV = float2(uv.x * 4.0, uv.y * 3.0 - t * 1.5);
    float n = fbm(fireUV);
    n += fbm(fireUV * 2.0 + t * 0.5) * 0.5;

    // Intensity — stronger at bottom, fading up
    float intensity = n * smoothstep(1.0, 0.1, uv.y);
    intensity = pow(intensity, 1.5);

    // Fire color gradient: black -> red -> orange -> yellow -> white
    float3 col;
    if (intensity < 0.3) {
        col = mix(float3(0.0, 0.0, 0.0), float3(0.5, 0.0, 0.0), intensity / 0.3);
    } else if (intensity < 0.6) {
        col = mix(float3(0.5, 0.0, 0.0), float3(1.0, 0.5, 0.0), (intensity - 0.3) / 0.3);
    } else if (intensity < 0.85) {
        col = mix(float3(1.0, 0.5, 0.0), float3(1.0, 1.0, 0.2), (intensity - 0.6) / 0.25);
    } else {
        col = mix(float3(1.0, 1.0, 0.2), float3(1.0, 1.0, 0.9), (intensity - 0.85) / 0.15);
    }

    return half4(half3(col), 1.0);
}
"""

private const val SHADER_GALAXY = """
uniform float2 iResolution;
uniform float  iTime;

float hash(float2 p) {
    float h = dot(p, float2(127.1, 311.7));
    return fract(sin(h) * 43758.5453123);
}

float noise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float2 u = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(hash(i), hash(i + float2(1.0, 0.0)), u.x),
        mix(hash(i + float2(0.0, 1.0)), hash(i + float2(1.0, 1.0)), u.x),
        u.y
    );
}

half4 main(float2 fragCoord) {
    float2 uv = (fragCoord - 0.5 * iResolution) / iResolution.y;
    float t = iTime * 0.2;

    // Polar coordinates
    float r = length(uv);
    float a = atan(uv.y, uv.x);

    // Spiral arms
    float arms = sin(a * 3.0 - r * 12.0 + t * 3.0) * 0.5 + 0.5;
    arms *= exp(-r * 2.5);

    // Star noise
    float stars = noise(fragCoord * 0.15);
    stars = pow(stars, 15.0) * 3.0;

    // Core glow
    float core = exp(-r * 5.0);
    float coreGlow = exp(-r * 2.0) * 0.3;

    // Background nebula
    float nebula = noise(uv * 3.0 + t * 0.5) * 0.15;
    nebula *= smoothstep(0.8, 0.2, r);

    // Colors
    float3 deepSpace = float3(0.02, 0.01, 0.05);
    float3 armColor = mix(float3(0.3, 0.2, 0.6), float3(0.5, 0.3, 0.8), arms);
    float3 coreColor = mix(float3(1.0, 0.9, 0.7), float3(0.9, 0.8, 1.0), r * 2.0);
    float3 nebulaColor = float3(0.15, 0.1, 0.25);

    float3 col = deepSpace;
    col += nebulaColor * nebula;
    col += armColor * arms * 0.8;
    col += coreColor * core;
    col += coreColor * coreGlow;
    col += float3(stars);

    // Subtle rotation tint
    float tint = sin(a + t) * 0.5 + 0.5;
    col += float3(0.1, 0.05, 0.15) * tint * smoothstep(0.6, 0.1, r) * 0.3;

    return half4(half3(col), 1.0);
}
"""

// endregion

private data class ShaderEntry(val title: String, val source: String)

private val shaderEntries = listOf(
    ShaderEntry("Neon Plasma", SHADER_NEON_PLASMA),
    ShaderEntry("Voronoi Cells", SHADER_VORONOI),
    ShaderEntry("Aurora Borealis", SHADER_AURORA),
    ShaderEntry("Fire Storm", SHADER_FIRE),
    ShaderEntry("Galaxy Spiral", SHADER_GALAXY),
    ShaderEntry("Traced Tunnel", SHADER_TRACED_TUNNEL)
)

@Composable
fun ShaderDemoScreen() {
    val navViewModel = LocalNavigationViewModel.current
    val pagerState = rememberPagerState(pageCount = { shaderEntries.size })
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val entry = shaderEntries[page]
            ShaderPage(shaderSrc = entry.source, title = entry.title)
        }

        // Back button
        IconButton(
            onClick = { navViewModel.goBack() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 4.dp, top = statusBarTop + 4.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        // Page indicator dots
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = navBarBottom + 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(shaderEntries.size) { index ->
                Box(
                    modifier = Modifier
                        .size(if (index == pagerState.currentPage) 10.dp else 8.dp)
                        .background(
                            color = if (index == pagerState.currentPage)
                                Color.White
                            else
                                Color.White.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

@Composable
private fun ShaderPage(shaderSrc: String, title: String) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ShaderCanvas(shaderSrc = shaderSrc)
        } else {
            // Fallback gradient
            Spacer(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0A0418),
                                Color(0xFF1A0A30),
                                Color(0xFF0A1A3A)
                            )
                        )
                    )
            )
        }

        // Title overlay with bottom gradient
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                    )
                )
                .padding(start = 24.dp, bottom = 64.dp, end = 24.dp, top = 48.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun ShaderCanvas(shaderSrc: String) {
    val shader = remember(shaderSrc) { RuntimeShader(shaderSrc) }
    val time = remember { mutableFloatStateOf(0f) }
    val brush = remember(shader) { ShaderBrush(shader) }

    LaunchedEffect(shaderSrc) {
        var startNanos = 0L
        while (true) {
            withFrameNanos { nanos ->
                if (startNanos == 0L) startNanos = nanos
                time.floatValue = (nanos - startNanos) / 1_000_000_000f
            }
        }
    }

    Spacer(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                shader.setFloatUniform("iResolution", size.width, size.height)
                shader.setFloatUniform("iTime", time.floatValue)
                drawRect(brush = brush)
            }
    )
}
