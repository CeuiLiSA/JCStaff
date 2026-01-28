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

private const val SHADER_MAGIC_CIRCLE = """
uniform float2 iResolution;
uniform float iTime;

const float PI = 3.14159265;
const float TAU = 6.28318530;

float2 rot2(float2 p, float a) {
    float c = cos(a), s = sin(a);
    return float2(c*p.x - s*p.y, s*p.x + c*p.y);
}

float hash11(float n) {
    return fract(sin(n * 127.1) * 43758.5453);
}

float sdSeg(float2 p, float2 a, float2 b) {
    float2 pa = p - a, ba = b - a;
    return length(pa - ba * clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0));
}

float draw(float d) {
    return smoothstep(0.002, 0.0, d) + smoothstep(0.015, 0.0, d) * 0.3;
}

float drawThin(float d) {
    return smoothstep(0.001, 0.0, d) + smoothstep(0.010, 0.0, d) * 0.15;
}

half4 main(float2 fragCoord) {
    float2 uv = (fragCoord - 0.5 * iResolution) / min(iResolution.x, iResolution.y);
    float t = iTime;
    float pulse = 0.85 + 0.15 * sin(t * 1.5);
    float val = 0.0;
    float d;

    // ── OUTER LAYER (slow clockwise) ──
    float2 p1 = rot2(uv, t * 0.10);
    float r1 = length(p1);
    float a1 = atan(p1.y, p1.x);
    float na1 = (a1 + PI) / TAU;

    // Double outer ring
    val += draw(abs(r1 - 0.44));
    val += draw(abs(r1 - 0.40));

    // Rune-like marks between the two rings
    float band = smoothstep(0.403, 0.408, r1) * smoothstep(0.437, 0.432, r1);
    float runeI = floor(na1 * 48.0);
    float runeF = fract(na1 * 48.0);
    float nearC = abs(fract(na1 * 8.0 + 0.5) - 0.5) / 8.0;
    float skipC = smoothstep(0.015, 0.025, nearC);
    float rh = hash11(runeI);
    float rh2 = hash11(runeI + 100.0);
    float tickH = 0.005 + rh * 0.013;
    float tickW = 0.12 + rh2 * 0.18;
    float tick = smoothstep(tickW, tickW - 0.06, abs(runeF - 0.5))
              * smoothstep(0.42 - tickH, 0.42 - tickH + 0.002, r1)
              * smoothstep(0.42 + tickH, 0.42 + tickH - 0.002, r1);
    float crossR = 0.42 + (rh2 - 0.5) * 0.005;
    float crossW = 0.18 + rh * 0.2;
    float cross2 = step(0.5, rh)
                 * smoothstep(0.001, 0.0, abs(r1 - crossR))
                 * smoothstep(0.5 - crossW, 0.5 - crossW + 0.06, runeF)
                 * smoothstep(0.5 + crossW, 0.5 + crossW - 0.06, runeF);
    val += (tick + cross2 * 0.6) * band * skipC * 0.5;

    // 8 decorated circles on the outer ring
    for (int i = 0; i < 8; i++) {
        float ca = float(i) * TAU / 8.0;
        float2 cc = float2(cos(ca), sin(ca)) * 0.42;
        float cd = length(p1 - cc);
        val += draw(abs(cd - 0.022));
        val += drawThin(abs(cd - 0.011)) * 0.5;
        val += smoothstep(0.004, 0.001, cd) * 0.6;
        if (hash11(float(i) * 73.1) > 0.5) {
            float2 lp = rot2(p1 - cc, float(i) * 0.7);
            val += smoothstep(0.001, 0.0, min(abs(lp.x), abs(lp.y)))
                 * step(cd, 0.015) * 0.35;
        } else {
            float2 off = float2(cos(ca), sin(ca)) * 0.006;
            val += drawThin(abs(cd - 0.014))
                 * step(0.014, length(p1 - cc - off)) * 0.4;
        }
    }

    // Sparkles on outer ring
    float sparkleI = floor(na1 * 24.0);
    float sparkle = pow(max(sin(t * 3.0 + hash11(sparkleI * 37.7) * TAU), 0.0), 12.0);
    val += sparkle * smoothstep(0.012, 0.0, abs(r1 - 0.44)) * 0.6;

    // Energy flow wave
    float flow = pow(sin(na1 * TAU * 3.0 - t * 2.0) * 0.5 + 0.5, 4.0);
    val += flow * smoothstep(0.015, 0.0, abs(r1 - 0.42)) * 0.25;

    // ── MIDDLE LAYER (slow counter-clockwise) ──
    float2 p2 = rot2(uv, -t * 0.07);
    float r2 = length(p2);

    val += draw(abs(r2 - 0.30));

    // Hexagram (two overlapping triangles)
    for (int tri = 0; tri < 2; tri++) {
        float off = float(tri) * PI / 3.0;
        for (int j = 0; j < 3; j++) {
            float aS = off + float(j) * TAU / 3.0;
            float aE = off + float(j + 1) * TAU / 3.0;
            d = sdSeg(p2, float2(cos(aS), sin(aS)) * 0.37,
                          float2(cos(aE), sin(aE)) * 0.37);
            val += draw(d);
        }
    }

    // Octagram web lines
    for (int i = 0; i < 8; i++) {
        float aS = float(i) * TAU / 8.0;
        float aE = float(i + 3) * TAU / 8.0;
        d = sdSeg(p2, float2(cos(aS), sin(aS)) * 0.37,
                      float2(cos(aE), sin(aE)) * 0.37);
        val += drawThin(d) * 0.3;
    }

    // 4 accent circles on middle ring
    for (int i = 0; i < 4; i++) {
        float ca = float(i) * TAU / 4.0 + PI / 4.0;
        float2 cc = float2(cos(ca), sin(ca)) * 0.30;
        float cd = length(p2 - cc);
        val += draw(abs(cd - 0.016));
        val += smoothstep(0.003, 0.001, cd) * 0.5;
    }

    // Thin radial spokes
    for (int i = 0; i < 12; i++) {
        float ca = float(i) * TAU / 12.0;
        d = sdSeg(p2, float2(cos(ca), sin(ca)) * 0.19,
                      float2(cos(ca), sin(ca)) * 0.30);
        val += drawThin(d) * 0.2;
    }

    // ── INNER LAYER (faster clockwise) ──
    float2 p3 = rot2(uv, t * 0.15);
    float r3 = length(p3);

    val += draw(abs(r3 - 0.18));

    // Upward triangle
    for (int j = 0; j < 3; j++) {
        float aS = float(j) * TAU / 3.0 - PI / 2.0;
        float aE = float(j + 1) * TAU / 3.0 - PI / 2.0;
        d = sdSeg(p3, float2(cos(aS), sin(aS)) * 0.16,
                      float2(cos(aE), sin(aE)) * 0.16);
        val += draw(d);
    }

    // Inverted triangle
    for (int j = 0; j < 3; j++) {
        float aS = float(j) * TAU / 3.0 + PI / 2.0;
        float aE = float(j + 1) * TAU / 3.0 + PI / 2.0;
        d = sdSeg(p3, float2(cos(aS), sin(aS)) * 0.12,
                      float2(cos(aE), sin(aE)) * 0.12);
        val += draw(d);
    }

    // Inner small ring
    val += draw(abs(r3 - 0.07));

    // Central square (45 deg rotated)
    float2 sq = abs(rot2(p3, PI / 4.0));
    val += draw(abs(max(sq.x, sq.y) - 0.03));

    // Center dot with glow
    val += smoothstep(0.008, 0.002, r3) + smoothstep(0.03, 0.0, r3) * 0.4;

    // ── FINAL COLOR ──
    float3 col = float3(0.005, 0.005, 0.018);
    float v = min(val, 2.5);
    col += float3(0.3, 0.7, 1.0) * v * 0.4 * pulse;
    col += float3(0.7, 0.85, 1.0) * max(v - 0.6, 0.0) * 0.5 * pulse;
    col += float3(0.35, 0.15, 0.7) * v * 0.08 * (0.9 + 0.1 * sin(t * 2.3));
    col += float3(0.2, 0.4, 0.8) * smoothstep(0.5, 0.1, length(uv)) * 0.02 * pulse;

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
    ShaderEntry("Traced Tunnel", SHADER_TRACED_TUNNEL),
    ShaderEntry("Magic Circle", SHADER_MAGIC_CIRCLE)
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
