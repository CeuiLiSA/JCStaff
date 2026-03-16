package ceui.lisa.jcstaff.screens

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.core.ContentFilterManager
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel

/** 作品举报理由的 string resource IDs */
private val ILLUST_REPORT_REASON_IDS = listOf(
    R.string.report_reason_illust_sexual,
    R.string.report_reason_illust_privacy,
    R.string.report_reason_illust_repost,
    R.string.report_reason_illust_ad,
    R.string.report_reason_illust_grotesque,
    R.string.report_reason_illust_child,
    R.string.report_reason_illust_other
)

/** 用户举报理由的 string resource IDs */
private val USER_REPORT_REASON_IDS = listOf(
    R.string.report_reason_user_inappropriate,
    R.string.report_reason_user_harassment,
    R.string.report_reason_user_malicious_link,
    R.string.report_reason_user_profile,
    R.string.report_reason_user_repost,
    R.string.report_reason_user_privacy,
    R.string.report_reason_user_child,
    R.string.report_reason_user_threat,
    R.string.report_reason_user_wiki,
    R.string.report_reason_user_other
)

/**
 * 举报页面
 *
 * 参照 Pixiv 官方举报表单的原生 MD3 实现。
 * 提交后将对应内容/用户加入本地屏蔽列表。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    objectId: Long,
    objectType: String
) {
    val navViewModel = LocalNavigationViewModel.current
    val context = LocalContext.current

    val isIllust = objectType == "illust"
    val reasonIds = if (isIllust) ILLUST_REPORT_REASON_IDS else USER_REPORT_REASON_IDS
    val reasons = reasonIds.map { stringResource(it) }
    var selectedReasonIndex by remember { mutableIntStateOf(0) }
    var detailText by remember { mutableStateOf("") }
    var reasonDropdownExpanded by remember { mutableStateOf(false) }

    val maxDetailLength = 3000
    val canSubmit = detailText.isNotBlank()

    val titleText = stringResource(
        if (isIllust) R.string.report_illust_title else R.string.report_user_title
    )

    // 字数颜色：接近上限时变色
    val charCountColor by animateColorAsState(
        targetValue = when {
            detailText.length > 2800 -> MaterialTheme.colorScheme.error
            detailText.length > 2000 -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(300),
        label = "charCountColor"
    )

    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navViewModel.goBack() }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cancel)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // ── 报告理由 ──
                Text(
                    text = stringResource(R.string.report_reason),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 0.3.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = reasonDropdownExpanded,
                    onExpandedChange = { reasonDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = reasons[selectedReasonIndex],
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = reasonDropdownExpanded)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = reasonDropdownExpanded,
                        onDismissRequest = { reasonDropdownExpanded = false }
                    ) {
                        reasons.forEachIndexed { index, reason ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = reason,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (index == selectedReasonIndex) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        fontWeight = if (index == selectedReasonIndex) {
                                            FontWeight.Medium
                                        } else {
                                            FontWeight.Normal
                                        }
                                    )
                                },
                                onClick = {
                                    selectedReasonIndex = index
                                    reasonDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // ── 详情 ──
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.report_detail),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 0.3.sp
                    )
                    Text(
                        text = stringResource(R.string.report_required),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = detailText,
                    onValueChange = { if (it.length <= maxDetailLength) detailText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.report_detail_placeholder),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                )
                // 字数统计
                Text(
                    text = "${detailText.length} / $maxDetailLength",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, end = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = charCountColor,
                    textAlign = TextAlign.End
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ── 提示信息卡片（仅作品举报显示） ──
                if (isIllust) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                        border = BorderStroke(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(top = 2.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.report_info_title),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.report_info_body),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.weight(1f))
            }

            // ── 底部按钮区域 ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = navBarPadding.calculateBottomPadding() + 16.dp)
            ) {
                // 发送按钮
                Button(
                    onClick = {
                        if (isIllust) {
                            ContentFilterManager.blockContent(objectId)
                        } else {
                            ContentFilterManager.blockUser(objectId)
                        }
                        Toast.makeText(
                            context,
                            context.getString(R.string.report_success),
                            Toast.LENGTH_SHORT
                        ).show()
                        navViewModel.goBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = canSubmit,
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                ) {
                    Text(
                        text = stringResource(R.string.report_submit),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 取消按钮
                OutlinedButton(
                    onClick = { navViewModel.goBack() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
