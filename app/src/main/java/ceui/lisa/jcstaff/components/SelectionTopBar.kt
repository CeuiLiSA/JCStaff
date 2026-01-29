package ceui.lisa.jcstaff.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.core.BatchDownloadProgress
import ceui.lisa.jcstaff.core.ImageDownloader
import ceui.lisa.jcstaff.core.LocalSelectionManager
import ceui.lisa.jcstaff.network.Illust
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopBar(
    allIllusts: List<Illust>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectionManager = LocalSelectionManager.current
    val coroutineScope = rememberCoroutineScope()
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf<BatchDownloadProgress?>(null) }

    AnimatedVisibility(
        visible = selectionManager.isSelectionMode,
        enter = slideInVertically { -it },
        exit = slideOutVertically { -it },
        modifier = modifier
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isDownloading && downloadProgress != null) {
                        Text(
                            text = stringResource(R.string.downloading) + "${downloadProgress!!.current}/${downloadProgress!!.total}",
                            style = MaterialTheme.typography.titleMedium
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.selected_count, selectionManager.selectedCount),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = { selectionManager.clearSelection() },
                    enabled = !isDownloading
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.deselect)
                    )
                }
            },
            actions = {
                // 全选按钮
                IconButton(
                    onClick = { selectionManager.selectAll(allIllusts) },
                    enabled = !isDownloading
                ) {
                    Icon(
                        imageVector = Icons.Default.SelectAll,
                        contentDescription = stringResource(R.string.select_all)
                    )
                }

                // 下载按钮
                IconButton(
                    onClick = {
                        val selectedIllusts = selectionManager.getSelectedIllusts()
                        if (selectedIllusts.isEmpty()) return@IconButton

                        coroutineScope.launch {
                            isDownloading = true
                            val successCount = ImageDownloader.batchDownloadToGallery(
                                context = context,
                                illusts = selectedIllusts,
                                onProgress = { progress ->
                                    downloadProgress = progress
                                }
                            )
                            isDownloading = false
                            downloadProgress = null

                            Toast.makeText(
                                context,
                                context.getString(R.string.download_complete, successCount, selectedIllusts.size),
                                Toast.LENGTH_SHORT
                            ).show()

                            selectionManager.clearSelection()
                        }
                    },
                    enabled = !isDownloading && selectionManager.selectedCount > 0
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = stringResource(R.string.download_selected)
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
    }
}
