package ceui.lisa.jcstaff.components

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush

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
private fun ShaderLayer(shader: RuntimeShader, time: FloatState) {
    val brush = remember { ShaderBrush(shader) }
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
