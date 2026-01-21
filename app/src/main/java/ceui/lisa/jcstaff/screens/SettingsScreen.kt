package ceui.lisa.jcstaff.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import kotlin.math.roundToInt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.core.SettingsStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit
) {
    val showIllustInfo by SettingsStore.showIllustInfo.collectAsState(initial = true)
    val cornerRadius by SettingsStore.illustCardCornerRadius.collectAsState(initial = 8)
    val gridSpacingEnabled by SettingsStore.gridSpacingEnabled.collectAsState(initial = true)
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 显示作品信息开关
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "显示作品信息",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "在瀑布流卡片上显示标题和作者名",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = showIllustInfo,
                    onCheckedChange = { checked ->
                        coroutineScope.launch {
                            SettingsStore.setShowIllustInfo(checked)
                        }
                    }
                )
            }

            // 卡片圆角设置
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "卡片圆角",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "调整瀑布流卡片的圆角大小",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${cornerRadius}dp",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = cornerRadius.toFloat(),
                    onValueChange = { value ->
                        coroutineScope.launch {
                            SettingsStore.setIllustCardCornerRadius(value.roundToInt())
                        }
                    },
                    valueRange = 0f..24f,
                    steps = 23,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // 瀑布流间距开关
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "卡片间距",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "开启后瀑布流卡片之间有间距",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = gridSpacingEnabled,
                    onCheckedChange = { checked ->
                        coroutineScope.launch {
                            SettingsStore.setGridSpacingEnabled(checked)
                        }
                    }
                )
            }
        }
    }
}
