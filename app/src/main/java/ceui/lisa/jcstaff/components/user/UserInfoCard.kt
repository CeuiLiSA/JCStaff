package ceui.lisa.jcstaff.components.user

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.components.MetaInfoRow
import ceui.lisa.jcstaff.network.UserProfile

/**
 * 用户详细信息卡片组件
 * 显示职业、地区、生日、网站和社交账号
 */
@Composable
fun UserInfoCard(
    profile: UserProfile?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val hasDetailInfo = !profile?.job.isNullOrBlank() ||
            !profile?.region.isNullOrBlank() ||
            profile?.birth_day != null ||
            !profile?.webpage.isNullOrBlank() ||
            !profile?.twitter_account.isNullOrBlank()

    if (!hasDetailInfo) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 职业
            if (!profile?.job.isNullOrBlank()) {
                MetaInfoRow(
                    icon = Icons.Default.Work,
                    label = stringResource(R.string.job),
                    value = profile?.job ?: "",
                    labelWidth = 48.dp
                )
            }

            // 地区
            if (!profile?.region.isNullOrBlank()) {
                MetaInfoRow(
                    icon = Icons.Default.LocationOn,
                    label = stringResource(R.string.region),
                    value = profile?.region ?: "",
                    labelWidth = 48.dp
                )
            }

            // 生日
            if (profile?.birth_day != null) {
                val birthText = if (profile.birth_year != null && profile.birth_year > 0) {
                    "${profile.birth_year}${stringResource(R.string.year_suffix)}${profile.birth_day}"
                } else {
                    profile.birth_day
                }
                MetaInfoRow(
                    icon = Icons.Default.Cake,
                    label = stringResource(R.string.birthday),
                    value = birthText,
                    labelWidth = 48.dp
                )
            }

            // 网站
            if (!profile?.webpage.isNullOrBlank()) {
                MetaInfoRow(
                    icon = Icons.Default.Language,
                    label = stringResource(R.string.website),
                    value = profile?.webpage ?: "",
                    labelWidth = 48.dp,
                    isLink = true,
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(profile?.webpage))
                            context.startActivity(intent)
                        } catch (_: Exception) { }
                    }
                )
            }

            // Twitter
            if (!profile?.twitter_account.isNullOrBlank()) {
                MetaInfoRow(
                    icon = Icons.Default.Language,
                    label = stringResource(R.string.twitter),
                    value = "@${profile?.twitter_account}",
                    labelWidth = 48.dp,
                    isLink = true,
                    onClick = {
                        try {
                            val url = profile?.twitter_url ?: "https://twitter.com/${profile?.twitter_account}"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        } catch (_: Exception) { }
                    }
                )
            }
        }
    }
}
