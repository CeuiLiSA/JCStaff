package ceui.lisa.jcstaff.components.user

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.network.UserProfile

/**
 * 用户统计数据行组件
 * 显示插画数、漫画数、关注数和收藏数
 */
@Composable
fun UserStatsRow(
    profile: UserProfile?,
    modifier: Modifier = Modifier,
    onFollowingClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(
            value = profile?.total_illusts ?: 0,
            label = stringResource(R.string.illustrations)
        )
        StatItem(
            value = profile?.total_manga ?: 0,
            label = stringResource(R.string.manga)
        )
        StatItem(
            value = profile?.total_follow_users ?: 0,
            label = stringResource(R.string.follow),
            modifier = if (onFollowingClick != null) {
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onFollowingClick)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            } else {
                Modifier
            }
        )
        StatItem(
            value = profile?.total_illust_bookmarks_public ?: 0,
            label = stringResource(R.string.bookmarks)
        )
    }
}
