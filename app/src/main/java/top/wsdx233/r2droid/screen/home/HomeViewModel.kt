package top.wsdx233.r2droid.screen.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import top.wsdx233.r2droid.util.R2PipeManager
import java.io.File
import java.io.FileOutputStream

sealed class HomeUiEvent {
    data object NavigateToProject : HomeUiEvent()
    data class ShowError(val message: String) : HomeUiEvent()
}

class HomeViewModel : ViewModel() {
    private val _uiEvent = Channel<HomeUiEvent>()
    val uiEvent: Flow<HomeUiEvent> = _uiEvent.receiveAsFlow()

    // History projects data (Placeholder)
    val historyProjects: List<String> = emptyList()

    fun onFileSelected(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                // 如果之前的会话未关闭，先强制关闭
                if (R2PipeManager.isConnected) {
                    R2PipeManager.quit()
                }

                // Determine file path. 
                // Since R2Pipe likely requires a real file path (native access),
                // if it's a content URI, we might need to copy it to a temp file.
                val filePath = resolvePath(context, uri)

                if (filePath != null) {
                    R2PipeManager.pendingFilePath = filePath
                    _uiEvent.send(HomeUiEvent.NavigateToProject)
                } else {
                    _uiEvent.send(HomeUiEvent.ShowError(context.getString(top.wsdx233.r2droid.R.string.home_error_resolve_path)))
                }
            } catch (e: Exception) {
                _uiEvent.send(HomeUiEvent.ShowError(e.message ?: context.getString(top.wsdx233.r2droid.R.string.home_error_unknown)))
            }
        }
    }

    private suspend fun resolvePath(context: Context, uri: Uri): String? {
        // 1. Try to get the real path first
        try {
            val realPath = top.wsdx233.r2droid.util.UriUtils.getPath(context, uri)
            if (realPath != null) {
                val file = File(realPath)
                if (file.exists() && file.canRead()) {
                    return realPath
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Fallback: Copy to internal cache
        return copyContentUriToCache(context, uri)
    }

    private fun copyContentUriToCache(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            // Use timestamp to ensure unique path, forcing R2 to treat it as a new file
            // Also try to preserve extension if possible (though R2 detects by magic bytes usually)
            val fileName = "r2_target_${System.currentTimeMillis()}" 
            val file = File(context.cacheDir, fileName)
            
            // Cleanup old temp files to save space? (Optional)
            // context.cacheDir.listFiles()?.forEach { if (it.name.startsWith("r2_target")) it.delete() }
            
            val outputStream = FileOutputStream(file)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun onHistoryItemClicked(item: String) {
        // Placeholder
    }

    fun onSettingsClicked() {
        // Placeholder
    }

    fun onAboutClicked() {
        // Placeholder
    }
}
