package ceui.lisa.jcstaff.components

import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Domain-warping FBM background + oscilloscope wave lines.
 * Written in AGSL (Android Graphics Shading Language).
 */
private const val SHADER_SRC = """
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
    float2 shift = float2(100.0);
    for (int i = 0; i < 5; ++i) {
        v += a * noise(p);
        p = p * 2.0 + shift;
        a *= 0.5;
    }
    return v;
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    float t = iTime;

    // ── Domain-warping background ──────────────────────────
    float2 q = float2(
        fbm(uv * 2.0 + t * 0.15),
        fbm(uv * 2.0 + float2(5.2, 1.3) + t * 0.12)
    );
    float2 r = float2(
        fbm(uv * 2.0 + 4.0 * q + float2(1.7, 9.2) + t * 0.08),
        fbm(uv * 2.0 + 4.0 * q + float2(8.3, 2.8) + t * 0.10)
    );
    float f = fbm(uv * 2.0 + 4.0 * r);

    // Cycling color palette — five hues on staggered periods
    float s1 = sin(t * 0.45) * 0.5 + 0.5;          // ~14s
    float s2 = sin(t * 0.35) * 0.5 + 0.5;          // ~18s
    float s3 = sin(t * 0.40) * 0.5 + 0.5;          // ~16s
    float s4 = sin(t * 0.30 + 1.0) * 0.5 + 0.5;   // ~21s
    float s5 = sin(t * 0.38 + 2.5) * 0.5 + 0.5;   // ~17s

    // Dark base: deep indigo ↔ dark teal
    float3 colA = mix(float3(0.05, 0.02, 0.14), float3(0.02, 0.10, 0.12), s1);
    // Mid tone: violet ↔ ocean blue
    float3 colB = mix(float3(0.22, 0.05, 0.40), float3(0.05, 0.22, 0.48), s2);
    // Accent 1: magenta ↔ emerald
    float3 colC = mix(float3(0.48, 0.10, 0.38), float3(0.08, 0.38, 0.42), s3);
    // Accent 2: amber-rose ↔ sky blue
    float3 colD = mix(float3(0.45, 0.18, 0.12), float3(0.12, 0.30, 0.55), s4);
    // Accent 3: coral ↔ lavender
    float3 colE = mix(float3(0.50, 0.15, 0.25), float3(0.25, 0.18, 0.50), s5);

    float3 color = mix(colA, colB, clamp(f * f * 4.0, 0.0, 1.0));
    color = mix(color, colC, clamp(length(q), 0.0, 1.0));
    color = mix(color, colD, clamp(length(r.x), 0.0, 1.0));
    color = mix(color, colE, clamp(f * length(r), 0.0, 1.0) * 0.5);
    color += float3(0.02, 0.015, 0.04) * (f * f * f + 0.6 * f * f + 0.5 * f);

    // ── Intertwined oscilloscope wave lines (bottom area) ──
    float aspect = iResolution.x / iResolution.y;
    float wx = uv.x * aspect;
    float center = 0.85;
    float helixAmp = 0.055;
    float lt = t * 0.18;
    float helixPhase = wx * 6.0 + lt * 1.5;

    // Wave 1 — cyan-blue, main helix + harmonics
    float w1 = center + helixAmp * sin(helixPhase)
             + 0.025 * sin(wx * 11.0 - lt * 2.2)
             + 0.012 * sin(wx * 18.0 + lt * 3.0);
    float d1 = abs(uv.y - w1);
    float line1 = smoothstep(0.0015, 0.0, d1);
    float glow1 = smoothstep(0.018, 0.0, d1);

    // Wave 2 — magenta-pink, PI-offset helix + harmonics
    float w2 = center + helixAmp * sin(helixPhase + 3.14159)
             + 0.020 * sin(wx * 13.0 + lt * 1.9)
             + 0.010 * sin(wx * 20.0 - lt * 2.7);
    float d2 = abs(uv.y - w2);
    float line2 = smoothstep(0.003, 0.0, d2);
    float glow2 = smoothstep(0.045, 0.0, d2);

    color += float3(0.45, 0.80, 1.00) * (line1 * 1.0 + glow1 * 0.3);
    color += float3(0.90, 0.45, 1.00) * (line2 * 0.9 + glow2 * 0.18);

    return half4(half3(color), 1.0);
}
"""

@Composable
fun AnimatedShaderBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ShaderBackgroundImpl(modifier, content)
    } else {
        FallbackBackground(modifier, content)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun ShaderBackgroundImpl(
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    val shader = remember { RuntimeShader(SHADER_SRC) }
    val time = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var startNanos = 0L
        while (true) {
            withFrameNanos { nanos ->
                if (startNanos == 0L) startNanos = nanos
                time.floatValue = (nanos - startNanos) / 1_000_000_000f
            }
        }
    }

    Box(modifier = modifier) {
        // time is read only in the draw phase — no recomposition from animation
        ShaderLayer(shader = shader, time = time)
        content()
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun ShaderLayer(
    shader: RuntimeShader,
    time: FloatState,
    modifier: Modifier = Modifier
) {
    val brush = remember { ShaderBrush(shader) }
    Spacer(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                shader.setFloatUniform("iResolution", size.width, size.height)
                shader.setFloatUniform("iTime", time.floatValue)
                drawRect(brush = brush)
            }
    )
}

@Composable
private fun FallbackBackground(
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0A0418),
                    Color(0xFF1A0A30),
                    Color(0xFF0A1A3A)
                )
            )
        )
    ) {
        content()
    }
}

// ── Traced Tunnel shader ────────────────────────────────────────────
// AGSL port of Shane's "Traced Tunnel" (shadertoy.com/view/tdjfDR)
// Optimized: 1 sample, 2 bounces, no FBM — cheap hash patterns replace texture lookups.
internal const val SHADER_TRACED_TUNNEL = """
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

@Composable
fun TracedTunnelBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        TracedTunnelBackgroundImpl(modifier, content)
    } else {
        FallbackBackground(modifier, content)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun TracedTunnelBackgroundImpl(
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    val shader = remember { RuntimeShader(SHADER_TRACED_TUNNEL) }
    val time = remember { mutableFloatStateOf(0f) }
    val fadeAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        fadeAlpha.animateTo(1f, animationSpec = tween(durationMillis = 2000))
    }

    LaunchedEffect(Unit) {
        var startNanos = 0L
        while (true) {
            withFrameNanos { nanos ->
                if (startNanos == 0L) startNanos = nanos
                time.floatValue = (nanos - startNanos) / 1_000_000_000f
            }
        }
    }

    Box(modifier = modifier) {
        ShaderLayer(
            shader = shader,
            time = time,
            modifier = Modifier.graphicsLayer { alpha = fadeAlpha.value }
        )
        content()
    }
}

// ── Traced Tunnel with Image tiles ────────────────────────────────────────────
private const val ATLAS_COLS = 28
private const val ATLAS_ROWS = 16
private const val IMAGE_TILE_SIZE = 200

private data class AtlasData(
    val shader: BitmapShader,
    val width: Float,
    val height: Float
)

private const val SHADER_TRACED_TUNNEL_IMAGE = """
uniform float2 iResolution;
uniform float  iTime;
uniform float2 iAtlasSize;
uniform shader tileImage;

const float GRID_COLS = 28.0;
const float GRID_ROWS = 16.0;
const float TILE_SIZE = 200.0;
const float TOTAL_IMAGES = 448.0;  // GRID_COLS * GRID_ROWS
const float INV_GRID_COLS = 0.0357142857;  // 1.0 / GRID_COLS

float tick(float t, float d) {
    float m = fract(t / d);
    m = m * m * (3.0 - 2.0 * m);  // Faster than double smoothstep
    return (floor(t / d) + m) * d;
}

float2 rot2(float2 v, float c, float s) {
    return float2(v.x * c + v.y * s, -v.x * s + v.y * c);
}

float hash21(float2 p) {
    float3 p3 = fract(float3(p.x, p.y, p.x) * float3(0.1031, 0.1030, 0.0973));
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

half4 main(float2 fragCoord) {
    float2 uv = (fragCoord - iResolution * 0.5) / iResolution.y;

    float t = iTime;
    float tickTm = t + tick(t, 6.0) * 0.5;

    // Precompute sin/cos for camera rotation
    float a1 = sin(tickTm * 0.3) * 0.4;
    float a2 = sin(tickTm * 0.1) * 2.0;
    float c1 = cos(a1), s1 = sin(a1);
    float c2 = cos(a2), s2 = sin(a2);

    float3 ro = float3(0.0, 0.0, tickTm);
    float3 r = normalize(float3(uv, 1.0));

    // Apply camera rotation
    r.xz = rot2(r.xz, c1, s1);
    r.xy = rot2(r.xy, c2, s2);

    // Inline ray-plane intersections (avoid function call overhead)
    float dB = r.y < 0.0 ? (-1.0 - ro.y) / r.y : 1e8;
    float dT = r.y > 0.0 ? ( 1.0 - ro.y) / r.y : 1e8;
    float dL = r.x < 0.0 ? (-1.0 - ro.x) / r.x : 1e8;
    float dR = r.x > 0.0 ? ( 1.0 - ro.x) / r.x : 1e8;

    float dH = min(dB, dT);
    float dV = min(dL, dR);
    float d  = min(dH, dV);

    float3 hp = ro + r * d;

    float3 n;
    float2 tuv;
    if (dH < dV) {
        n   = float3(0.0, dB < dT ? 1.0 : -1.0, 0.0);
        tuv = hp.xz + float2(0.0, n.y);
    } else {
        n   = float3(dL < dR ? 1.0 : -1.0, 0.0, 0.0);
        tuv = hp.yz + float2(n.x, 0.0);
    }

    tuv *= 2.0;
    float2 id = floor(tuv);
    float2 luv = tuv - id - 0.5;

    // Tile shape
    float bx = length(max(abs(luv) - 0.42, 0.0)) - 0.05;
    float inside = smoothstep(0.008, 0.0, bx);
    float sh = clamp(0.5 - bx * 10.0, 0.0, 1.0);

    // Atlas lookup - avoid mod() with fract
    float imgIndex = floor(hash21(id) * TOTAL_IMAGES);
    float atlasRow = floor(imgIndex * INV_GRID_COLS);
    float atlasCol = imgIndex - atlasRow * GRID_COLS;

    float2 imgUV = (float2(atlasCol, atlasRow) + luv + 0.5) * TILE_SIZE;
    float3 imgCol = tileImage.eval(imgUV).rgb;

    // Simple lighting (removed specular for performance)
    float3 sampleCol = mix(float3(0.02), imgCol * sh, inside);
    float dif = max(dot(normalize(float3(0.0, 0.0, 3.0) - hp + ro), n), 0.0);
    sampleCol *= dif * 0.35 + 0.65;

    // Fog
    sampleCol *= 1.0 / (1.0 + d * d * 0.02);

    // Gamma correction
    return half4(half3(pow(max(sampleCol, float3(0.0)), float3(0.4545))), 1.0);
}
"""

@Composable
fun TracedTunnelImageBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        TracedTunnelImageBackgroundImpl(modifier, content)
    } else {
        FallbackBackground(modifier, content)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun TracedTunnelImageBackgroundImpl(
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val shader = remember { RuntimeShader(SHADER_TRACED_TUNNEL_IMAGE) }
    val time = remember { mutableFloatStateOf(0f) }
    val brush = remember { ShaderBrush(shader) }
    val fadeAlpha = remember { Animatable(0f) }

    var atlasData by remember { mutableStateOf<AtlasData?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        atlasData = withContext(Dispatchers.IO) {
            loadAtlas(context)
        }
        isLoading = false
    }

    LaunchedEffect(isLoading) {
        if (!isLoading && atlasData != null) {
            fadeAlpha.animateTo(1f, animationSpec = tween(durationMillis = 1500))
        }
    }

    LaunchedEffect(Unit) {
        var startNanos = 0L
        while (true) {
            withFrameNanos { nanos ->
                if (startNanos == 0L) startNanos = nanos
                time.floatValue = (nanos - startNanos) / 1_000_000_000f
            }
        }
    }

    Box(modifier = modifier) {
        if (isLoading || atlasData == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            val data = atlasData!!
            Spacer(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = fadeAlpha.value }
                    .drawBehind {
                        shader.setFloatUniform("iResolution", size.width, size.height)
                        shader.setFloatUniform("iTime", time.floatValue)
                        shader.setFloatUniform("iAtlasSize", data.width, data.height)
                        shader.setInputShader("tileImage", data.shader)
                        drawRect(brush = brush)
                    }
            )
        }
        content()
    }
}

private fun loadAtlas(context: android.content.Context): AtlasData? {
    val assetManager = context.assets
    val files = assetManager.list("prime_square")?.toList()?.shuffled() ?: return null
    if (files.isEmpty()) return null

    val atlasWidth = ATLAS_COLS * IMAGE_TILE_SIZE
    val atlasHeight = ATLAS_ROWS * IMAGE_TILE_SIZE
    val atlasBitmap = android.graphics.Bitmap.createBitmap(
        atlasWidth, atlasHeight, android.graphics.Bitmap.Config.RGB_565
    )
    val canvas = android.graphics.Canvas(atlasBitmap)

    val options = BitmapFactory.Options().apply {
        inSampleSize = 2  // Decode at 270x270, then scale to 200x200
    }
    val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)

    for (row in 0 until ATLAS_ROWS) {
        for (col in 0 until ATLAS_COLS) {
            val index = (row * ATLAS_COLS + col) % files.size
            val inputStream = assetManager.open("prime_square/${files[index]}")
            val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()
            bitmap?.let {
                val destRect = android.graphics.RectF(
                    (col * IMAGE_TILE_SIZE).toFloat(),
                    (row * IMAGE_TILE_SIZE).toFloat(),
                    ((col + 1) * IMAGE_TILE_SIZE).toFloat(),
                    ((row + 1) * IMAGE_TILE_SIZE).toFloat()
                )
                canvas.drawBitmap(it, null, destRect, paint)
                it.recycle()
            }
        }
    }

    val bitmapShader = BitmapShader(atlasBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    return AtlasData(bitmapShader, atlasWidth.toFloat(), atlasHeight.toFloat())
}
