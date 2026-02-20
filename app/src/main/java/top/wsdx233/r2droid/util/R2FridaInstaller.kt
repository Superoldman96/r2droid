package top.wsdx233.r2droid.util

import android.content.Context
import android.system.Os
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

data class R2FridaInstallState(
    val status: Status = Status.IDLE,
    val progress: Float = 0f,
    val message: String = "",
    val version: String = ""
) {
    enum class Status { IDLE, FETCHING, DOWNLOADING, EXTRACTING, DONE, ERROR }
}

object R2FridaInstaller {
    private const val TAG = "R2FridaInstaller"
    private const val GITHUB_API = "https://api.github.com/repos/wsdx233/r2frida-android-arm64-build/releases/latest"
    private const val GITEE_API = "https://gitee.com/api/v5/repos/wsdx233/r2frida-android-arm64-build/releases/latest"

    private val _state = MutableStateFlow(R2FridaInstallState())
    val state = _state.asStateFlow()

    fun getPluginsDir(context: Context): File =
        File(context.filesDir, "r2work/radare2/plugins")

    fun isInstalled(context: Context): Boolean =
        getPluginsDir(context).listFiles()?.any { it.name.startsWith("io_frida") && it.name.endsWith(".so") } == true

    fun resetState() {
        _state.value = R2FridaInstallState()
    }

    suspend fun install(context: Context, useChinaSource: Boolean) = withContext(Dispatchers.IO) {
        try {
            _state.value = R2FridaInstallState(R2FridaInstallState.Status.FETCHING, message = "Fetching release info...")

            val apiUrl = if (useChinaSource) GITEE_API else GITHUB_API
            val json = fetchJson(apiUrl)
            val version = json["tag_name"]?.jsonPrimitive?.content ?: "unknown"
            val assets = json["assets"]?.jsonArray ?: throw Exception("No assets found")

            // Find .so asset first, then .zip
            val soAsset = assets.firstOrNull {
                it.jsonObject["name"]?.jsonPrimitive?.content?.endsWith(".so") == true
            }
            val zipAsset = assets.firstOrNull {
                val name = it.jsonObject["name"]?.jsonPrimitive?.content ?: ""
                name.endsWith(".zip") && !name.startsWith("v") // exclude source archives
            }

            val pluginsDir = getPluginsDir(context)
            pluginsDir.mkdirs()

            if (soAsset != null) {
                val downloadUrl = soAsset.jsonObject["browser_download_url"]!!.jsonPrimitive.content
                val fileName = soAsset.jsonObject["name"]!!.jsonPrimitive.content
                val targetFile = File(pluginsDir, fileName)

                _state.value = R2FridaInstallState(R2FridaInstallState.Status.DOWNLOADING, version = version)
                downloadFile(downloadUrl, targetFile)
                Os.chmod(targetFile.absolutePath, 493) // 0755
            } else if (zipAsset != null) {
                val downloadUrl = zipAsset.jsonObject["browser_download_url"]!!.jsonPrimitive.content
                val tempZip = File(context.cacheDir, "r2frida_temp.zip")

                _state.value = R2FridaInstallState(R2FridaInstallState.Status.DOWNLOADING, version = version)
                downloadFile(downloadUrl, tempZip)

                _state.value = R2FridaInstallState(R2FridaInstallState.Status.EXTRACTING, progress = 0.9f, version = version)
                extractSoFromZip(tempZip, pluginsDir)
                tempZip.delete()
            } else {
                throw Exception("No suitable asset found")
            }

            _state.value = R2FridaInstallState(R2FridaInstallState.Status.DONE, progress = 1f, version = version)
        } catch (e: Exception) {
            Log.e(TAG, "Install failed", e)
            _state.value = R2FridaInstallState(
                R2FridaInstallState.Status.ERROR,
                message = e.message ?: "Unknown error"
            )
        }
    }

    private fun fetchJson(apiUrl: String): JsonObject {
        val conn = URL(apiUrl).openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        conn.setRequestProperty("Accept", "application/json")
        return try {
            val text = conn.inputStream.bufferedReader().readText()
            Json { ignoreUnknownKeys = true }.parseToJsonElement(text).jsonObject
        } finally {
            conn.disconnect()
        }
    }

    private fun downloadFile(url: String, target: File) {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.instanceFollowRedirects = true
        val totalBytes = conn.contentLength.toLong()
        var downloaded = 0L

        BufferedInputStream(conn.inputStream).use { input ->
            FileOutputStream(target).use { output ->
                val buffer = ByteArray(8192)
                var len: Int
                while (input.read(buffer).also { len = it } != -1) {
                    output.write(buffer, 0, len)
                    downloaded += len
                    if (totalBytes > 0) {
                        _state.value = _state.value.copy(
                            progress = (downloaded.toFloat() / totalBytes).coerceAtMost(0.9f)
                        )
                    }
                }
            }
        }
        conn.disconnect()
    }

    private fun extractSoFromZip(zipFile: File, targetDir: File) {
        ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(".so")) {
                    val outFile = File(targetDir, File(entry.name).name)
                    FileOutputStream(outFile).use { fos -> zis.copyTo(fos) }
                    Os.chmod(outFile.absolutePath, 493)
                }
                entry = zis.nextEntry
            }
        }
    }
}
