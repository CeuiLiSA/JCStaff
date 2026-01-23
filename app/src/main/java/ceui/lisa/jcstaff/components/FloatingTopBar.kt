package ceui.lisa.jcstaff.components

import android.content.Intent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.R

/**
 * 通用浮动顶部栏组件
 * 包含渐变背景、返回按钮、分享按钮和更多菜单
 * 可选支持共享元素过渡（用于 IllustDetailScreen 等需要在过渡动画期间显示的场景）
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FloatingTopBar(
    shareUrl: String,
    shareTitle: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null,
    onReportClick: () -> Unit = {},
    onBlockClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    var showMoreMenu by remember { mutableStateOf(false) }

    // 判断是否使用共享元素过渡
    val useSharedTransition = sharedTransitionScope != null && animatedContentScope != null

    // 渐变背景的 modifier
    val gradientModifier = if (useSharedTransition && sharedTransitionScope != null) {
        with(sharedTransitionScope) {
            modifier
                .renderInSharedTransitionScopeOverlay(zIndexInOverlay = 1f)
                .fillMaxWidth()
                .height(statusBarPadding.calculateTopPadding() + 60.dp)
        }
    } else {
        Modifier
            .fillMaxWidth()
            .height(statusBarPadding.calculateTopPadding() + 60.dp)
    }

    // 按钮行的 modifier
    val buttonRowModifier = if (useSharedTransition && sharedTransitionScope != null) {
        with(sharedTransitionScope) {
            Modifier
                .renderInSharedTransitionScopeOverlay(zIndexInOverlay = 1f)
                .fillMaxWidth()
                .padding(
                    top = statusBarPadding.calculateTopPadding() + 4.dp,
                    start = 4.dp,
                    end = 4.dp
                )
        }
    } else {
        Modifier
            .fillMaxWidth()
            .padding(
                top = statusBarPadding.calculateTopPadding() + 4.dp,
                start = 4.dp,
                end = 4.dp
            )
    }

    Box(modifier = modifier) {
        // 顶部阴影渐变
        Box(
            modifier = gradientModifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.7f),
                            Color.Transparent
                        )
                    )
                )
        )

        // 按钮行
        Row(
            modifier = buttonRowModifier,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 返回按钮
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = Color.White
                )
            }

            // 右侧按钮组
            Row {
                // 分享按钮
                val shareText = stringResource(R.string.share)
                IconButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareUrl)
                            putExtra(Intent.EXTRA_TITLE, shareTitle)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, shareText))
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = shareText,
                        tint = Color.White
                    )
                }

                // 更多按钮
                Box {
                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.more),
                            tint = Color.White
                        )
                    }

                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.report)) },
                            onClick = {
                                showMoreMenu = false
                                onReportClick()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Flag,
                                    contentDescription = null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.block)) },
                            onClick = {
                                showMoreMenu = false
                                onBlockClick()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Block,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
