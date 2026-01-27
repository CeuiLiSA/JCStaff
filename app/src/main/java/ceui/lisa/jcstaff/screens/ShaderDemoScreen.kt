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

// Traced Tunnel — AGSL port of Shane's "Traced Tunnel" (shadertoy.com/view/tdjfDR)
// Optimized: 1 sample, 2 bounces, no FBM — cheap hash patterns replace texture lookups.
private const val SHADER_TRACED_TUNNEL = """
uniform float2 iResolution;
uniform float  iTime;

// Dave Hoskins hash — no sin(), immune to large-number float32 precision loss.
float hash21(float2 p) {
    float3 p3 = fract(float3(p.x, p.y, p.x) * float3(0.1031, 0.1030, 0.0973));
    p3 += dot(p3, float3(p3.y, p3.z, p3.x) + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float tick(float t, float d) {
    float m = fract(t / d);
    m = smoothstep(0.0, 1.0, m);
    m = smoothstep(0.0, 1.0, m);
    return (floor(t / d) + m) * d;
}
float tickTime(float t) { return t * 2.0 + tick(t, 4.0) * 0.75; }

float2 rot2(float2 v, float a) {
    float c = cos(a); float s = sin(a);
    return float2(v.x * c + v.y * s, -v.x * s + v.y * c);
}

float3 camXform(float3 p, float tTime) {
    p.xz = rot2(p.xz, sin(tTime * 0.3) * 0.4);
    p.xy = rot2(p.xy, sin(tTime * 0.1) * 2.0);
    return p;
}

// ── 5 cosine-palette themes (a = center, b = amplitude) ─────
// Each pair produces 120°-separated triadic colors.
// Dominant channel shifts around the color wheel so transitions
// are clearly visible: purple → cyan → green → red/orange → pink.
//   0  Violet Dream   — deep purple / magenta / teal
//   1  Ocean Abyss    — coral / deep cyan / jade
//   2  Aurora          — rose / electric blue / vivid green
//   3  Ember Glow     — crimson-orange / sapphire / gold
//   4  Sakura Mist    — hot pink / mint / lavender
float3 palCenter(int idx) {
    if (idx == 1) return float3(0.08, 0.38, 0.50);
    if (idx == 2) return float3(0.12, 0.48, 0.18);
    if (idx == 3) return float3(0.52, 0.18, 0.08);
    if (idx == 4) return float3(0.48, 0.12, 0.42);
    return float3(0.25, 0.10, 0.48); // 0
}
float3 palAmp(int idx) {
    if (idx == 1) return float3(0.32, 0.22, 0.18);
    if (idx == 2) return float3(0.25, 0.32, 0.35);
    if (idx == 3) return float3(0.30, 0.30, 0.28);
    if (idx == 4) return float3(0.22, 0.32, 0.20);
    return float3(0.32, 0.22, 0.28); // 0
}

float rayPlane(float3 ro, float3 rd, float3 n, float d) {
    float ndotdir = dot(rd, n);
    if (ndotdir < 0.0) {
        float dist = (-d - dot(ro, n) + 9e-7) / ndotdir;
        if (dist > 0.0) return dist;
    }
    return 1e8;
}

half4 main(float2 fragCoord) {
    float2 uv = (fragCoord - iResolution * 0.5) / iResolution.y;

    float tickTm = tickTime(iTime);
    float3 ca = float3(0.0, 0.0, tickTm);

    float3 ro = float3(0.0);
    float3 r = normalize(float3(uv, 1.0));
    ro = camXform(ro, tickTm);
    r  = camXform(r,  tickTm);
    ro.z += ca.z;

    float3 col = float3(0.0);
    float alpha = 1.0;
    float fogD = 0.0;
    const float sc = 5.0;

    // Cycle through 5 palettes — 16s each, 80s full loop, seamless.
    float cyc = mod(iTime * 0.0625, 5.0);
    int pi0 = int(floor(cyc));
    int pi1 = pi0 < 4 ? pi0 + 1 : 0;
    float pblend = smoothstep(0.0, 1.0, fract(cyc));
    float3 pa = mix(palCenter(pi0), palCenter(pi1), pblend);
    float3 pb = mix(palAmp(pi0),    palAmp(pi1),    pblend);

    for (int i = 0; i < 2; i++) {
        float plB = rayPlane(ro, r, float3(0.0,  1.0, 0.0), 1.0);
        float plT = rayPlane(ro, r, float3(0.0, -1.0, 0.0), 1.0);
        float plL = rayPlane(ro, r, float3( 1.0, 0.0, 0.0), 1.0);
        float plR = rayPlane(ro, r, float3(-1.0, 0.0, 0.0), 1.0);

        float dH = min(plB, plT);
        float dV = min(plL, plR);
        float d  = min(dH, dV);
        if (i == 0) fogD = d;

        float3 hp = ro + r * d;

        float3 n;
        float2 tuv;
        if (dH < dV) {
            n   = float3(0.0, plB < plT ? 1.0 : -1.0, 0.0);
            tuv = hp.xz + float2(0.0, n.y);
        } else {
            n   = float3(plL < plR ? 1.0 : -1.0, 0.0, 0.0);
            tuv = hp.yz + float2(n.x, 0.0);
        }

        tuv *= sc;
        float2 id = floor(tuv);
        float2 luv = tuv - id - 0.5;

        // rounded-box tile
        float bx = length(max(abs(luv) - 0.3, 0.0)) - 0.1;
        float sh = clamp(0.5 - bx / 0.2, 0.0, 1.0);
        float inside = 1.0 - smoothstep(0.0, 0.005, bx);

        // ── Per-tile cosine palette (indigo / violet / magenta / teal) ──
        // Dave Hoskins hash uses fract() internally, no mod needed.
        float hid = hash21(id + 0.2);
        float hpat = hash21(id * 7.31 + 0.53);
        float pat = smoothstep(0.3, 0.7, hpat);

        // Per-tile phase from spatial hash; palette (pa, pb) cycles over time.
        // 120° offsets (0.0, 0.33, 0.67) maximise hue spread within each theme.
        float phase = hid + hpat * 0.3;
        float3 sqCol = pa + pb * cos(6.2831 * (phase + float3(0.0, 0.33, 0.67)));
        sqCol *= 2.2;

        // gap color tinted by current palette mood
        float3 gapCol = pa * 0.12;
        float3 sampleCol = mix(gapCol, sqCol * sh, inside) * (0.6 + pat * 0.5);

        // lighting — cyan-blue specular, magenta-pink fresnel (matching ShaderBackground waves)
        float3 ld = normalize(ca + float3(0.0, 0.0, 3.0) - hp);
        float dif = max(dot(ld, n), 0.0);
        float spe = pow(max(dot(reflect(ld, -n), -r), 0.0), 8.0);
        float fre = max(1.0 - abs(dot(r, n)) * 0.5, 0.0);

        sampleCol *= dif + float3(0.45, 0.80, 1.00) * spe * 4.0 + float3(0.90, 0.45, 1.00) * fre;
        sampleCol *= 1.35 / (1.0 + fogD * fogD * 0.05);

        col += sampleCol * alpha * fre;
        alpha *= 0.7;

        // pure reflection for next bounce
        r = reflect(r, n);
        ro = hp + n * 0.002;
    }

    col = pow(max(col, float3(0.0)), float3(0.4545));
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
