package ceui.lisa.jcstaff.screens

import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.RuntimeShader
import android.graphics.Shader
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
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

private const val SHADER_SAKURA_CARD = """
uniform float2 iResolution;
uniform float iTime;

const float PI = 3.14159265;
const float TAU = 6.28318530;

float2 rot2(float2 p, float a) {
    float c = cos(a), s = sin(a);
    return float2(c*p.x - s*p.y, s*p.x + c*p.y);
}
float sdBox(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + r;
    return length(max(q, 0.0)) - r;
}
float sdSeg(float2 p, float2 a, float2 b) {
    float2 pa = p - a, ba = b - a;
    return length(pa - ba * clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0));
}
float hsh(float n) { return fract(sin(n * 127.1) * 43758.5453); }
float stroke(float d, float w) { return smoothstep(w + 0.002, w, d); }
float fill(float d) { return smoothstep(0.003, 0.0, d); }

float3 cardFront(float2 p, float cw, float ch, float rotA) {
    float3 col = float3(0.98, 0.78, 0.84);
    float d;

    // Gold border
    d = abs(sdBox(p, float2(cw - 0.010, ch - 0.010), 0.015));
    col = mix(col, float3(0.78, 0.58, 0.38), stroke(d, 0.003));

    // Corner ornaments
    for (int i = 0; i < 4; i++) {
        float sx = (i == 0 || i == 2) ? -1.0 : 1.0;
        float sy = (i < 2) ? -1.0 : 1.0;
        float2 cp = p - float2(sx * (cw - 0.032), sy * (ch - 0.032));
        float cr = length(cp);
        col = mix(col, float3(0.78, 0.58, 0.40), stroke(abs(cr - 0.016), 0.002));
        col = mix(col, float3(0.92, 0.64, 0.72), fill(cr - 0.009));
        col = mix(col, float3(0.78, 0.58, 0.40), fill(cr - 0.004));
    }

    // ── MAGIC CIRCLE ──
    float2 mc = p - float2(0.0, -0.06);
    float mr = length(mc);
    float ma = atan(mc.y, mc.x);

    // Outer thick pink ring
    float outerR = 0.20;
    d = abs(mr - outerR);
    col = mix(col, float3(0.90, 0.50, 0.64), smoothstep(0.010, 0.007, d));
    col = mix(col, float3(0.96, 0.74, 0.82), stroke(d, 0.001));
    col = mix(col, float3(0.96, 0.74, 0.82), stroke(abs(d - 0.009), 0.001));

    // Tick marks (zodiac-style)
    float tickA = abs(fract((ma + PI) / TAU * 24.0 + 0.5) - 0.5);
    float tickB = smoothstep(outerR, outerR - 0.001, mr)
                * smoothstep(outerR - 0.010, outerR - 0.008, mr);
    col = mix(col, float3(0.92, 0.64, 0.72), smoothstep(0.20, 0.15, tickA) * tickB * 0.7);

    // Inner ring
    float innerR = 0.16;
    d = abs(mr - innerR);
    col = mix(col, float3(0.92, 0.58, 0.68), stroke(d, 0.0015));
    float inside = smoothstep(0.0, -0.005, mr - innerR);
    col = mix(col, float3(0.97, 0.85, 0.90), inside * 0.35);

    // 6-pointed star (golden yellow)
    float starR = 0.13;
    for (int tri = 0; tri < 2; tri++) {
        float off = float(tri) * PI / 3.0;
        for (int j = 0; j < 3; j++) {
            float aS = off + float(j) * TAU / 3.0 - PI / 2.0;
            float aE = off + float(j + 1) * TAU / 3.0 - PI / 2.0;
            float ld = sdSeg(mc, float2(cos(aS), sin(aS)) * starR,
                                 float2(cos(aE), sin(aE)) * starR);
            col = mix(col, float3(0.96, 0.88, 0.40), stroke(ld, 0.004));
        }
    }

    // Star hexagon fill
    float hexA = mod(ma + PI / 6.0, PI / 3.0) - PI / 6.0;
    float hexD = mr * cos(hexA) - starR * 0.48;
    col = mix(col, float3(0.98, 0.93, 0.52), fill(-hexD) * inside * 0.55);

    // Center bright
    col = mix(col, float3(1.0, 0.96, 0.58), smoothstep(0.035, 0.015, mr));
    col = mix(col, float3(1.0, 0.98, 0.78), smoothstep(0.015, 0.005, mr));

    // Radial lines through star
    for (int i = 0; i < 6; i++) {
        float la = float(i) * PI / 3.0;
        float ld = abs(dot(mc, float2(-sin(la), cos(la))));
        float lm = smoothstep(innerR, innerR - 0.005, mr) * smoothstep(0.025, 0.03, mr);
        col = mix(col, float3(0.92, 0.72, 0.78), stroke(ld, 0.001) * lm * 0.4);
    }

    // Holographic sheen (shifts with 3D rotation)
    float holo = sin(ma * 3.0 + rotA * 2.0) * 0.5 + 0.5;
    float holoM = smoothstep(outerR, outerR - 0.01, mr) * smoothstep(0.03, 0.04, mr);
    col += float3(0.15, 0.08, 0.22) * holo * holoM * 0.15;

    // ── MOON (left) ──
    float2 moonP = mc - float2(-0.19, 0.0);
    float moonR = length(moonP);
    col = mix(col, float3(0.84, 0.52, 0.58), stroke(abs(moonR - 0.028), 0.002));
    col = mix(col, float3(0.94, 0.72, 0.78),
              fill(moonR - 0.024) * (1.0 - fill(length(moonP - float2(0.007, 0.0)) - 0.019)));

    // ── SUN (right) ──
    float2 sunP = mc - float2(0.19, 0.0);
    float sunR = length(sunP);
    float sunA = atan(sunP.y, sunP.x);
    col = mix(col, float3(0.86, 0.62, 0.42), fill(sunR - 0.020));
    float rayM = smoothstep(0.028, 0.020, sunR) * smoothstep(0.018, 0.022, sunR);
    col = mix(col, float3(0.92, 0.70, 0.42), (sin(sunA * 8.0) * 0.5 + 0.5) * rayM);
    col = mix(col, float3(0.82, 0.56, 0.38), stroke(abs(sunR - 0.028), 0.001));

    // ── WINGS (top) ──
    for (int s = 0; s < 2; s++) {
        float sx = (s == 0) ? -1.0 : 1.0;
        for (int f = 0; f < 4; f++) {
            float fa = sx * (float(f) - 1.5) * 0.28;
            float2 fc = float2(sx * (0.035 + float(f) * 0.014), -0.30);
            float2 fp = rot2(p - fc, -fa);
            float fl = 0.052 - abs(float(f) - 1.5) * 0.010;
            float fw = 0.009;
            float fd = length(fp / float2(fl, fw)) - 1.0;
            col = mix(col, float3(1.0, 1.0, 1.0),
                      smoothstep(0.0, -0.08, fd) * step(0.0, fp.x * sx) * 0.8);
        }
    }

    // ── TOP STAR EMBLEM ──
    float2 topP = p - float2(0.0, -0.33);
    float topR = length(topP);
    float topA = atan(topP.y, topP.x);
    col = mix(col, float3(0.82, 0.64, 0.36), stroke(abs(topR - 0.018), 0.002));
    col = mix(col, float3(0.96, 0.90, 0.52), fill(topR - 0.014));
    float tsSh = cos(topA * 6.0) * 0.003 + 0.008;
    col = mix(col, float3(0.98, 0.94, 0.45), fill(topR - tsSh));

    // ── BOTTOM CRESCENT MOON ──
    float2 botP = p - float2(0.0, 0.26);
    float botR = length(botP);
    col = mix(col, float3(0.82, 0.62, 0.42), stroke(abs(botR - 0.038), 0.002));
    col = mix(col, float3(0.92, 0.74, 0.54), fill(botR - 0.034));
    col = mix(col, float3(0.98, 0.78, 0.84),
              fill(-(length(botP - float2(0.0, -0.012)) - 0.028)) * fill(botR - 0.006));

    // ── WINGS (bottom) ──
    for (int s = 0; s < 2; s++) {
        float sx = (s == 0) ? -1.0 : 1.0;
        for (int f = 0; f < 4; f++) {
            float fa = sx * (float(f) - 1.5) * 0.28;
            float2 fc = float2(sx * (0.035 + float(f) * 0.014), 0.30);
            float2 fp = rot2(p - fc, fa);
            float fl = 0.052 - abs(float(f) - 1.5) * 0.010;
            float fw = 0.009;
            float fd = length(fp / float2(fl, fw)) - 1.0;
            col = mix(col, float3(1.0, 1.0, 1.0),
                      smoothstep(0.0, -0.08, fd) * step(0.0, fp.x * sx) * 0.8);
        }
    }

    // ── SAKURA BANNER ──
    float2 banP = p - float2(0.0, 0.36);
    float banD = sdBox(banP, float2(0.09, 0.013), 0.004);
    col = mix(col, float3(0.78, 0.58, 0.38), stroke(abs(banD), 0.002));
    col = mix(col, float3(0.85, 0.66, 0.44), fill(-banD));
    // Letter placeholders
    for (int i = 0; i < 6; i++) {
        float2 lp = banP - float2((float(i) - 2.5) * 0.022, 0.0);
        col = mix(col, float3(0.32, 0.20, 0.12),
                  fill(max(abs(lp.x) - 0.005, abs(lp.y) - 0.007)));
    }

    return col;
}

float3 cardBack(float2 p, float cw, float ch) {
    float3 col = float3(0.90, 0.58, 0.68);

    // Gold border
    float border = abs(sdBox(p, float2(cw - 0.010, ch - 0.010), 0.015));
    col = mix(col, float3(0.78, 0.58, 0.38), stroke(border, 0.003));

    // Inner lighter area
    float inner = sdBox(p, float2(cw - 0.022, ch - 0.022), 0.012);
    col = mix(col, float3(0.94, 0.65, 0.74), fill(-inner));

    // Diamond lattice
    float2 dp = p * 28.0;
    float diamonds = abs(fract(dp.x + dp.y) - 0.5) + abs(fract(dp.x - dp.y) - 0.5);
    col = mix(col, float3(0.96, 0.74, 0.82),
              smoothstep(0.06, 0.0, abs(diamonds - 0.5)) * 0.35 * fill(-inner));

    // Central medallion
    float mr = length(p);
    float ma = atan(p.y, p.x);
    col = mix(col, float3(0.78, 0.56, 0.40), stroke(abs(mr - 0.15), 0.002));
    float starSh = cos(ma * 4.0) * 0.03 + 0.11;
    col = mix(col, float3(0.96, 0.78, 0.84), fill(mr - starSh) * fill(-mr + 0.15));
    col = mix(col, float3(0.78, 0.56, 0.40), stroke(abs(mr - 0.06), 0.002));
    col = mix(col, float3(0.94, 0.70, 0.78), fill(mr - 0.04));
    float ctrStar = cos(ma * 8.0) * 0.004 + 0.018;
    col = mix(col, float3(0.82, 0.60, 0.44), fill(mr - ctrStar));

    return col;
}

half4 main(float2 fragCoord) {
    float2 uv = (fragCoord - 0.5 * iResolution) / iResolution.y;
    float t = iTime;

    // Y-axis rotation with gentle bobbing
    float angle = t * 0.7;
    float ca = cos(angle);
    float sa = sin(angle);
    float bob = sin(t * 0.5) * 0.01;
    float2 uvShift = float2(uv.x, uv.y - bob);

    float camD = 2.2;
    float cw = 0.24;
    float ch = 0.42;

    // Background
    float3 bg = mix(float3(0.06, 0.03, 0.14), float3(0.04, 0.02, 0.10), uvShift.y + 0.5);
    // Sparkle particles
    float sp = hsh(floor(uv.x * 60.0) * 100.0 + floor(uv.y * 60.0));
    sp = step(0.996, sp) * (0.3 + 0.3 * sin(t * 3.0 + sp * 100.0));
    bg += float3(0.6, 0.3, 0.7) * sp;

    // Inverse perspective mapping
    float denom = ca - uvShift.x * sa / camD;
    if (abs(denom) < 0.0005) return half4(half3(bg), 1.0);

    float cardX = uvShift.x / denom;
    float depth = 1.0 + cardX * sa / camD;
    float cardY = uvShift.y * depth;
    if (depth <= 0.0) return half4(half3(bg), 1.0);

    float cardD = sdBox(float2(cardX, cardY), float2(cw, ch), 0.018);
    if (cardD > 0.004) {
        // Glow + shadow
        float glow = smoothstep(0.06, 0.0, cardD);
        bg += float3(0.9, 0.5, 0.7) * glow * 0.12;
        return half4(half3(bg), 1.0);
    }

    // Determine face
    bool isFront = denom > 0.0;
    float2 cp = float2(isFront ? cardX : -cardX, cardY);

    float3 col;
    if (isFront) {
        col = cardFront(cp, cw, ch, angle);
    } else {
        col = cardBack(cp, cw, ch);
    }

    // Lighting
    float facing = abs(ca);
    col *= 0.72 + 0.28 * facing;
    col += float3(1.0, 0.95, 0.98) * pow(facing, 6.0) * 0.18;

    // Card edge
    float edgeF = smoothstep(0.004, 0.0, cardD) * smoothstep(-0.004, 0.0, cardD);
    col = mix(col, float3(0.96, 0.92, 0.88), edgeF * (1.0 - facing) * 0.5);

    return half4(half3(col), 1.0);
}
"""

// Traced Tunnel with Image tiles - each tile shows a different image from atlas
private const val SHADER_TRACED_TUNNEL_IMAGE = """
uniform float2 iResolution;
uniform float  iTime;
uniform float2 iAtlasSize;   // Atlas dimensions in pixels
uniform shader tileImage;

const float GRID_COLS = 32.0;  // 32 columns in atlas
const float GRID_ROWS = 26.0;  // 26 rows in atlas (832 images total)
const float TILE_SIZE = 180.0; // Each image is 180x180

float tick(float t, float d) {
    float m = fract(t / d);
    m = smoothstep(0.0, 1.0, m);
    m = smoothstep(0.0, 1.0, m);
    return (floor(t / d) + m) * d;
}
float tickTime(float t) { return t * 1.0 + tick(t, 6.0) * 0.5; }  // Slower forward speed

float2 rot2(float2 v, float a) {
    float c = cos(a); float s = sin(a);
    return float2(v.x * c + v.y * s, -v.x * s + v.y * c);
}

float3 camXform(float3 p, float tTime) {
    p.xz = rot2(p.xz, sin(tTime * 0.3) * 0.4);
    p.xy = rot2(p.xy, sin(tTime * 0.1) * 2.0);
    return p;
}

float rayPlane(float3 ro, float3 rd, float3 n, float d) {
    float ndotdir = dot(rd, n);
    if (ndotdir < 0.0) {
        float dist = (-d - dot(ro, n) + 9e-7) / ndotdir;
        if (dist > 0.0) return dist;
    }
    return 1e8;
}

// Hash function to pick image index based on tile ID
float hash21(float2 p) {
    float3 p3 = fract(float3(p.x, p.y, p.x) * float3(0.1031, 0.1030, 0.0973));
    p3 += dot(p3, float3(p3.y, p3.z, p3.x) + 33.33);
    return fract((p3.x + p3.y) * p3.z);
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
    const float sc = 2.0;

    for (int i = 0; i < 1; i++) {  // Single bounce for performance
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

        // Tile shape - rounded box
        float bx = length(max(abs(luv) - 0.42, 0.0)) - 0.05;
        float sh = clamp(0.5 - bx / 0.1, 0.0, 1.0);
        float inside = 1.0 - smoothstep(0.0, 0.008, bx);

        // Pick a random image from atlas based on tile ID
        float totalImages = GRID_COLS * GRID_ROWS;
        float imgIndex = floor(hash21(id) * totalImages);
        float atlasRow = floor(imgIndex / GRID_COLS);
        float atlasCol = mod(imgIndex, GRID_COLS);

        // Map tile-local UV to the selected image in atlas
        float2 normUV = (luv + 0.5);  // 0 to 1 within tile
        float2 atlasOffset = float2(atlasCol, atlasRow) * TILE_SIZE;
        float2 imgUV = atlasOffset + normUV * TILE_SIZE;
        float3 imgCol = tileImage.eval(imgUV).rgb;

        float3 sqCol = imgCol;

        // Dark gap between tiles
        float3 gapCol = float3(0.02, 0.02, 0.03);
        float3 sampleCol = mix(gapCol, sqCol * sh, inside);

        // Simple lighting - keep images solid
        float3 ld = normalize(ca + float3(0.0, 0.0, 3.0) - hp);
        float dif = max(dot(ld, n), 0.0);
        float spe = pow(max(dot(reflect(ld, -n), -r), 0.0), 16.0);

        sampleCol *= dif * 0.4 + 0.6;  // Less dramatic, more base light
        sampleCol += float3(1.0, 1.0, 1.0) * spe * 0.2;

        // Fog - less aggressive
        sampleCol *= 1.0 / (1.0 + fogD * fogD * 0.02);

        col += sampleCol * alpha;
        alpha *= 0.3;  // Less transparency for reflections

        r = reflect(r, n);
        ro = hp + n * 0.002;
    }

    col = pow(max(col, float3(0.0)), float3(0.4545));
    return half4(half3(col), 1.0);
}
"""

// endregion

private data class ShaderEntry(
    val title: String,
    val source: String,
    val usesImage: Boolean = false
)

private val shaderEntries = listOf(
    ShaderEntry("Neon Plasma", SHADER_NEON_PLASMA),
    ShaderEntry("Fire Storm", SHADER_FIRE),
    ShaderEntry("Traced Tunnel", SHADER_TRACED_TUNNEL),
    ShaderEntry("Tunnel (Image)", SHADER_TRACED_TUNNEL_IMAGE, usesImage = true),
    ShaderEntry("Magic Circle", SHADER_MAGIC_CIRCLE),
)

@Composable
fun ShaderDemoScreen() {
    val navViewModel = LocalNavigationViewModel.current
    val pagerState = rememberPagerState(pageCount = { shaderEntries.size })
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val entry = shaderEntries[page]
            ShaderPage(
                shaderSrc = entry.source,
                title = entry.title,
                usesImage = entry.usesImage
            )
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
private fun ShaderPage(shaderSrc: String, title: String, usesImage: Boolean = false) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (usesImage) {
                ImageShaderCanvas(shaderSrc = shaderSrc)
            } else {
                ShaderCanvas(shaderSrc = shaderSrc)
            }
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

private const val ATLAS_COLS = 32       // 32 columns
private const val ATLAS_ROWS = 26       // 26 rows = 832 images
private const val TILE_SIZE = 180       // Downscale to 180x180

private data class AtlasData(
    val shader: BitmapShader,
    val width: Float,
    val height: Float
)

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun ImageShaderCanvas(shaderSrc: String) {
    val context = LocalContext.current
    val shader = remember(shaderSrc) { RuntimeShader(shaderSrc) }
    val time = remember { mutableFloatStateOf(0f) }
    val brush = remember(shader) { ShaderBrush(shader) }

    // Load atlas asynchronously
    var atlasData by remember { mutableStateOf<AtlasData?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        atlasData = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            loadAtlas(context)
        }
        isLoading = false
    }

    LaunchedEffect(shaderSrc) {
        var startNanos = 0L
        while (true) {
            withFrameNanos { nanos ->
                if (startNanos == 0L) startNanos = nanos
                time.floatValue = (nanos - startNanos) / 1_000_000_000f
            }
        }
    }

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
                .drawBehind {
                    shader.setFloatUniform("iResolution", size.width, size.height)
                    shader.setFloatUniform("iTime", time.floatValue)
                    shader.setFloatUniform("iAtlasSize", data.width, data.height)
                    shader.setInputShader("tileImage", data.shader)
                    drawRect(brush = brush)
                }
        )
    }
}

private fun loadAtlas(context: android.content.Context): AtlasData? {
    val assetManager = context.assets
    val files = assetManager.list("prime_square")?.toList()?.shuffled() ?: return null
    if (files.isEmpty()) return null

    val atlasWidth = ATLAS_COLS * TILE_SIZE   // 32 * 135 = 4320
    val atlasHeight = ATLAS_ROWS * TILE_SIZE  // 16 * 135 = 2160
    val atlasBitmap = android.graphics.Bitmap.createBitmap(
        atlasWidth, atlasHeight, android.graphics.Bitmap.Config.RGB_565
    )
    val canvas = android.graphics.Canvas(atlasBitmap)

    val options = BitmapFactory.Options().apply {
        inSampleSize = 3
    }

    // Fill atlas with images
    for (row in 0 until ATLAS_ROWS) {
        for (col in 0 until ATLAS_COLS) {
            val index = (row * ATLAS_COLS + col) % files.size
            val inputStream = assetManager.open("prime_square/${files[index]}")
            val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()
            bitmap?.let {
                canvas.drawBitmap(
                    it,
                    (col * TILE_SIZE).toFloat(),
                    (row * TILE_SIZE).toFloat(),
                    null
                )
                it.recycle()
            }
        }
    }

    val bitmapShader = BitmapShader(atlasBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    return AtlasData(bitmapShader, atlasWidth.toFloat(), atlasHeight.toFloat())
}
