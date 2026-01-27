package ceui.lisa.jcstaff.components.user

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.network.User
import ceui.lisa.jcstaff.network.UserProfile
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * 用户头像区域组件
 * 包含背景图、头像和 Premium 徽章
 */
@Composable
fun UserAvatarSection(
    user: User?,
    profile: UserProfile?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .height(200.dp)
    ) {
        // 背景图或渐变色
        if (profile?.background_image_url != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(profile.background_image_url)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
            )
        }

        // 头像（底部居中，向下偏移）
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 48.dp)
        ) {
            if (isLoading && user == null) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(user?.profile_image_urls?.findAvatarUrl())
                        .crossfade(true)
                        .build(),
                    contentDescription = user?.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .border(
                            width = 4.dp,
                            color = MaterialTheme.colorScheme.surface,
                            shape = CircleShape
                        )
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                // Premium 徽章
                if (user?.is_premium == true || profile?.is_premium == true) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-4).dp, y = (-4).dp)
                            .size(28.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Premium",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .padding(4.dp)
                                .size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
