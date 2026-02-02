package ceui.lisa.jcstaff.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class FileItem(
    val file: File,
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val childCount: Int = 0
)

data class CacheBrowserState(
    val currentPath: String = "",
    val rootPath: String = "",
    val fileItems: List<FileItem> = emptyList(),
    val totalSize: Long = 0L,
    val isLoading: Boolean = true,
    val isRoot: Boolean = true,
    val cleanableCacheSize: Long = 0L,
    val isCleaning: Boolean = false
)

class CacheBrowserViewModel : ViewModel() {

    private val _state = MutableStateFlow(CacheBrowserState())
    val state: StateFlow<CacheBrowserState> = _state.asStateFlow()

    private var rootDir: File? = null

    private var appContext: Context? = null

    fun initialize(context: Context, initialPath: String?) {
        appContext = context.applicationContext
        rootDir = context.filesDir.parentFile ?: context.filesDir
        val startPath = initialPath ?: rootDir!!.absolutePath
        _state.value = _state.value.copy(
            rootPath = rootDir!!.absolutePath,
            currentPath = startPath
        )
        loadDirectory(startPath)
        calculateCleanableCacheSize()
    }

    fun navigateTo(path: String) {
        _state.value = _state.value.copy(currentPath = path)
        loadDirectory(path)
    }

    fun navigateUp(): Boolean {
        val currentPath = _state.value.currentPath
        val rootPath = _state.value.rootPath

        if (currentPath == rootPath) {
            return false // 已经在根目录，返回 false 表示应该退出页面
        }

        val parentPath = File(currentPath).parent ?: rootPath
        navigateTo(parentPath)
        return true
    }

    fun refresh() {
        loadDirectory(_state.value.currentPath)
    }

    fun deleteItem(item: FileItem) {
        viewModelScope.launch(Dispatchers.IO) {
            if (item.isDirectory) {
                item.file.deleteRecursively()
            } else {
                item.file.delete()
            }
            // 刷新列表
            loadDirectory(_state.value.currentPath)
        }
    }

    private fun loadDirectory(path: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            val result = withContext(Dispatchers.IO) {
                val dir = File(path)
                if (dir.exists() && dir.isDirectory) {
                    val files = dir.listFiles()?.map { file ->
                        FileItem(
                            file = file,
                            name = file.name,
                            isDirectory = file.isDirectory,
                            size = if (file.isDirectory) calculateDirSize(file) else file.length(),
                            lastModified = file.lastModified(),
                            childCount = if (file.isDirectory) file.listFiles()?.size ?: 0 else 0
                        )
                    }?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                        ?: emptyList()

                    val totalSize = files.sumOf { it.size }
                    Pair(files, totalSize)
                } else {
                    Pair(emptyList<FileItem>(), 0L)
                }
            }

            _state.value = _state.value.copy(
                fileItems = result.first,
                totalSize = result.second,
                isLoading = false,
                isRoot = path == _state.value.rootPath
            )
        }
    }

    private fun calculateDirSize(dir: File): Long {
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) calculateDirSize(file) else file.length()
        }
        return size
    }

    /**
     * 计算可清理的缓存大小
     * 包括：cache、code_cache、image_load_cache_*、ugoira
     */
    private fun calculateCleanableCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = appContext ?: return@launch
            var totalCleanable = 0L

            // cache 目录
            context.cacheDir?.let { cacheDir ->
                if (cacheDir.exists()) {
                    totalCleanable += calculateDirSize(cacheDir)
                }
            }

            // code_cache 目录
            context.codeCacheDir?.let { codeCacheDir ->
                if (codeCacheDir.exists()) {
                    totalCleanable += calculateDirSize(codeCacheDir)
                }
            }

            // files 目录下的 ugoira 和 image_load_cache_*
            context.filesDir?.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    val name = file.name
                    if (name == "ugoira" || name.startsWith("image_load_cache")) {
                        totalCleanable += calculateDirSize(file)
                    }
                }
            }

            _state.value = _state.value.copy(cleanableCacheSize = totalCleanable)
        }
    }

    /**
     * 一键清理图片和 GIF 缓存
     * 清理：cache、code_cache、image_load_cache_*、ugoira
     * 保留：databases、shared_prefs、datastore、auth_prefs_* 等
     */
    fun cleanImageCache() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isCleaning = true)

            val context = appContext ?: return@launch

            // 清理 cache 目录
            context.cacheDir?.deleteRecursively()

            // 清理 code_cache 目录
            context.codeCacheDir?.deleteRecursively()

            // 清理 files 目录下的 ugoira 和 image_load_cache_*
            context.filesDir?.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    val name = file.name
                    if (name == "ugoira" || name.startsWith("image_load_cache")) {
                        file.deleteRecursively()
                    }
                }
            }

            // 刷新
            _state.value = _state.value.copy(isCleaning = false, cleanableCacheSize = 0L)
            loadDirectory(_state.value.currentPath)
        }
    }
}
