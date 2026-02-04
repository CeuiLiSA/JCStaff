package ceui.lisa.jcstaff.components.user

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.network.User
import ceui.lisa.jcstaff.network.UserProfile
import ceui.lisa.jcstaff.network.Workspace

/**
 * 用户主页头部组件
 * 包含头像、用户名、统计数据、关注按钮、简介和详细信息
 */
@Composable
fun UserProfileHeader(
    user: User?,
    profile: UserProfile?,
    workspace: Workspace?,
    isLoading: Boolean,
    isFollowing: Boolean,
    onFollowClick: () -> Unit,
    onFollowingClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // 背景图 + 头像区域
        UserAvatarSection(
            user = user,
            profile = profile,
            isLoading = isLoading,
            modifier = Modifier.fillMaxWidth()
        )

        // 头像下方空间
        Spacer(modifier = Modifier.height(56.dp))

        // 用户名和账号
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = user?.name ?: "",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "@${user?.account ?: ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 统计数据
        UserStatsRow(
            profile = profile,
            onFollowingClick = onFollowingClick
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 关注按钮
        UserFollowButton(
            user = user,
            isFollowing = isFollowing,
            onFollowClick = onFollowClick
        )

        // 简介
        if (!user?.comment.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = user?.comment ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        // 用户详细信息卡片
        Spacer(modifier = Modifier.height(20.dp))
        UserInfoCard(profile = profile)

        // 工作环境信息卡片
        UserWorkspaceCard(workspace = workspace)

        Spacer(modifier = Modifier.height(12.dp))
    }
}
