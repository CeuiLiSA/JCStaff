package ceui.lisa.jcstaff.components

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.R

enum class BlockType { USER, CONTENT }

@Composable
fun BlockedContentOverlay(
    isBlocked: Boolean,
    blockType: BlockType,
    onUnblock: () -> Unit,
    content: @Composable () -> Unit
) {
    val blurRadius: Dp by animateDpAsState(
        targetValue = if (isBlocked) 40.dp else 0.dp,
        animationSpec = tween(durationMillis = 500),
        label = "blur"
    )

    val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Box(modifier = Modifier.fillMaxSize()) {
        // 内容层 + 模糊
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (supportsBlur && blurRadius > 0.dp) {
                        Modifier.blur(blurRadius)
                    } else {
                        Modifier
                    }
                )
        ) {
            content()
        }

        // 遮罩层 — 拦截所有触摸
        AnimatedVisibility(
            visible = isBlocked,
            enter = fadeIn(animationSpec = tween(400)),
            exit = fadeOut(animationSpec = tween(400))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) { detectTapGestures { } }
            ) {
                // 暗色遮罩
                val scrimAlpha = if (supportsBlur) 0.5f else 0.7f
                Spacer(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            drawRect(Color.Black.copy(alpha = scrimAlpha))
                        }
                )

                // 居中 UI
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VisibilityOff,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color.White.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(
                            if (blockType == BlockType.USER) R.string.user_blocked_title
                            else R.string.content_blocked_title
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(
                            if (blockType == BlockType.USER) R.string.user_blocked_message
                            else R.string.content_blocked_message
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onUnblock,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            stringResource(
                                if (blockType == BlockType.USER) R.string.unblock_user
                                else R.string.unblock_work
                            )
                        )
                    }
                }
            }
        }
    }
}
