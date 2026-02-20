package top.wsdx233.r2droid.feature.project

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import top.wsdx233.r2droid.util.R2PipeManager
import java.io.OutputStream

enum class ExportFormat {
    MARKDOWN, HTML, JSON, FRIDA
}

data class ExportOptions(
    val format: ExportFormat,
    val includeFunctions: Boolean,
    val maxFunctions: Int,
    val includeStrings: Boolean,
    val includeSymbols: Boolean // Imports and Exports
)

object ReportExporter {
    
    suspend fun exportReport(
        context: Context,
        uri: Uri,
        options: ExportOptions,
        onProgress: (String) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!R2PipeManager.isConnected) {
                return@withContext Result.failure(Exception("Radare2 session is not connected"))
            }

            onProgress("正在收集数据...")
            
            // Gather basic info
            val infoStr = R2PipeManager.execute("ij").getOrNull()
            val infoJson = try { if (infoStr != null) JSONObject(infoStr) else null } catch(e: Exception) { null }

            val fileName = infoJson?.optJSONObject("core")?.optString("file", "unknown") ?: "unknown"
            val formatStr = infoJson?.optJSONObject("core")?.optString("format", "") ?: ""
            val archStr = infoJson?.optJSONObject("bin")?.optString("arch", "") ?: ""

            // Gather elements based on options
            var functionsArray: JSONArray? = null
            if (options.includeFunctions) {
                onProgress("正在提取函数列表...")
                val result = R2PipeManager.execute("aflj").getOrNull() ?: "[]"
                try {
                    val arr = JSONArray(result)
                    if (arr.length() > options.maxFunctions) {
                        functionsArray = JSONArray()
                        for (i in 0 until options.maxFunctions) {
                            functionsArray.put(arr.optJSONObject(i))
                        }
                    } else {
                        functionsArray = arr
                    }
                } catch (_: Exception) {}
            }

            var stringsArray: JSONArray? = null
            if (options.includeStrings) {
                onProgress("正在提取字符串列表...")
                val result = R2PipeManager.execute("izj").getOrNull() ?: "[]"
                try { stringsArray = JSONArray(result) } catch (_: Exception) {}
            }

            var importsArray: JSONArray? = null
            var exportsArray: JSONArray? = null
            if (options.includeSymbols) {
                onProgress("正在提取导入/导出表...")
                try { importsArray = JSONArray(R2PipeManager.execute("iij").getOrNull() ?: "[]") } catch (_: Exception) {}
                try { exportsArray = JSONArray(R2PipeManager.execute("iEj").getOrNull() ?: "[]") } catch (_: Exception) {}
            }

            onProgress("正在生成报告...")
            val content = when (options.format) {
                ExportFormat.MARKDOWN -> generateMarkdown(fileName, archStr, formatStr, functionsArray, stringsArray, importsArray, exportsArray)
                ExportFormat.HTML -> generateHtml(fileName, archStr, formatStr, functionsArray, stringsArray, importsArray, exportsArray)
                ExportFormat.JSON -> generateJson(fileName, functionsArray, stringsArray, importsArray, exportsArray)
                ExportFormat.FRIDA -> generateFrida(fileName, functionsArray, importsArray, exportsArray)
            }

            onProgress("正在写入文件...")
            context.contentResolver.openOutputStream(uri)?.use { outputStream: OutputStream ->
                outputStream.write(content.toByteArray())
            } ?: return@withContext Result.failure(Exception("无法打开写入留"))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generateMarkdown(
        fileName: String, arch: String, format: String,
        functions: JSONArray?, strings: JSONArray?,
        imports: JSONArray?, exports: JSONArray?
    ): String {
        val sb = StringBuilder()
        sb.append("# 静态分析报告 - $fileName\n\n")
        sb.append("**架构**: $arch | **格式**: $format\n\n")
        sb.append("---\n\n")

        functions?.let {
            sb.append("## 函数列表 (${it.length()})\n\n")
            sb.append("| 地址 | 大小 | 名称 |\n")
            sb.append("| --- | --- | --- |\n")
            for (i in 0 until it.length()) {
                val f = it.optJSONObject(i) ?: continue
                sb.append("| `0x${java.lang.Long.toHexString(f.optLong("offset"))}` | ${f.optInt("size")} | `${f.optString("name")}` |\n")
            }
            sb.append("\n")
        }

        imports?.let {
            sb.append("## 导入表 (${it.length()})\n\n")
            sb.append("| 名称 | 类型 |\n")
            sb.append("| --- | --- |\n")
            for (i in 0 until it.length()) {
                val item = it.optJSONObject(i) ?: continue
                sb.append("| `${item.optString("name")}` | ${item.optString("type")} |\n")
            }
            sb.append("\n")
        }

        exports?.let {
            sb.append("## 导出表 (${it.length()})\n\n")
            sb.append("| 名称 | 大小 | 地址 |\n")
            sb.append("| --- | --- | --- |\n")
            for (i in 0 until it.length()) {
                val item = it.optJSONObject(i) ?: continue
                sb.append("| `${item.optString("name")}` | ${item.optInt("size")} | `0x${java.lang.Long.toHexString(item.optLong("vaddr"))}` |\n")
            }
            sb.append("\n")
        }

        strings?.let {
            sb.append("## 字符串列表 (${it.length()})\n\n")
            sb.append("| 地址 | 长度 | 内容 |\n")
            sb.append("| --- | --- | --- |\n")
            for (i in 0 until it.length()) {
                val item = it.optJSONObject(i) ?: continue
                val str = item.optString("string").replace("\n", "\\n").replace("\r", "")
                sb.append("| `0x${java.lang.Long.toHexString(item.optLong("vaddr"))}` | ${item.optInt("size")} | `$str` |\n")
            }
            sb.append("\n")
        }

        return sb.toString()
    }

    private fun generateHtml(
        fileName: String, arch: String, format: String,
        functions: JSONArray?, strings: JSONArray?,
        imports: JSONArray?, exports: JSONArray?
    ): String {
        val body = StringBuilder()

        functions?.let {
            body.append("<h2>函数列表 (${it.length()})</h2><table><tr><th>地址</th><th>大小</th><th>名称</th></tr>")
            for (i in 0 until it.length()) {
                val f = it.optJSONObject(i) ?: continue
                body.append("<tr><td><code>0x${java.lang.Long.toHexString(f.optLong("offset"))}</code></td><td>${f.optInt("size")}</td><td><code>${f.optString("name")}</code></td></tr>")
            }
            body.append("</table>")
        }

        imports?.let {
            body.append("<h2>导入表 (${it.length()})</h2><table><tr><th>名称</th><th>类型</th></tr>")
            for (i in 0 until it.length()) {
                val item = it.optJSONObject(i) ?: continue
                body.append("<tr><td><code>${item.optString("name")}</code></td><td>${item.optString("type")}</td></tr>")
            }
            body.append("</table>")
        }

        exports?.let {
            body.append("<h2>导出表 (${it.length()})</h2><table><tr><th>名称</th><th>大小</th><th>地址</th></tr>")
            for (i in 0 until it.length()) {
                val item = it.optJSONObject(i) ?: continue
                body.append("<tr><td><code>${item.optString("name")}</code></td><td>${item.optInt("size")}</td><td><code>0x${java.lang.Long.toHexString(item.optLong("vaddr"))}</code></td></tr>")
            }
            body.append("</table>")
        }

        strings?.let {
            body.append("<h2>字符串列表 (${it.length()})</h2><table><tr><th>地址</th><th>长度</th><th>内容</th></tr>")
            for (i in 0 until it.length()) {
                val item = it.optJSONObject(i) ?: continue
                val str = item.optString("string").replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "\\n").replace("\r", "")
                body.append("<tr><td><code>0x${java.lang.Long.toHexString(item.optLong("vaddr"))}</code></td><td>${item.optInt("size")}</td><td><code>$str</code></td></tr>")
            }
            body.append("</table>")
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>静态分析报告 - $fileName</title>
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; padding: 20px; color: #333; line-height: 1.6; }
                    h1, h2, h3 { color: #2c3e50; }
                    table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }
                    th, td { padding: 8px 12px; border: 1px solid #ddd; text-align: left; }
                    th { background-color: #f5f6fa; }
                    code { background: #f4f4f4; padding: 2px 6px; border-radius: 4px; font-family: monospace; }
                </style>
            </head>
            <body>
                <h1>静态分析报告</h1>
                <p><strong>文件:</strong> $fileName</p>
                <p><strong>架构:</strong> $arch</p>
                <p><strong>格式:</strong> $format</p>
                <hr>
                $body
            </body>
            </html>
        """.trimIndent()
    }

    private fun generateJson(
        fileName: String,
        functions: JSONArray?, strings: JSONArray?,
        imports: JSONArray?, exports: JSONArray?
    ): String {
        val root = JSONObject()
        root.put("file", fileName)
        root.put("functions", functions ?: JSONArray())
        root.put("strings", strings ?: JSONArray())
        root.put("imports", imports ?: JSONArray())
        root.put("exports", exports ?: JSONArray())
        return root.toString(2)
    }

    private fun generateFrida(
        fileName: String,
        functions: JSONArray?,
        imports: JSONArray?, exports: JSONArray?
    ): String {
        val sb = StringBuilder()
        sb.append("// Auto-generated Frida Hook Script for $fileName\n")
        sb.append("// Generated by R2Droid\n\n")
        sb.append("function attach(targetName, addr, moduleName) {\n")
        sb.append("    try {\n")
        sb.append("        var ptr = moduleName ? Module.findExportByName(moduleName, targetName) : addr;\n")
        sb.append("        if (ptr) {\n")
        sb.append("            Interceptor.attach(ptr, {\n")
        sb.append("                onEnter: function(args) {\n")
        sb.append("                    console.log('[+] Entered: ' + targetName);\n")
        sb.append("                },\n")
        sb.append("                onLeave: function(retval) {\n")
        sb.append("                    // console.log('[-] Left: ' + targetName);\n")
        sb.append("                }\n")
        sb.append("            });\n")
        sb.append("        }\n")
        sb.append("    } catch (e) { console.error('Error attaching to ' + targetName + ': ' + e); }\n")
        sb.append("}\n\n")
        sb.append("rpc.exports = {\n")
        sb.append("    init: function() {\n")
        sb.append("        console.log('[*] Script initialized.');\n")
        
        exports?.let {
            for (i in 0 until it.length()) {
                val item = it.optJSONObject(i) ?: continue
                val name = item.optString("name")
                if (name.isNotBlank()) {
                    sb.append("        attach('$name', null, '$fileName');\n")
                }
            }
        }
        
        sb.append("    }\n")
        sb.append("};\n")
        return sb.toString()
    }
}
