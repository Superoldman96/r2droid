package top.wsdx233.r2droid.data

import android.content.Context
import android.content.SharedPreferences

object SettingsManager {
    private const val PREFS_NAME = "r2droid_settings"
    private const val KEY_R2RC_PATH = "r2rc_path"
    private const val KEY_FONT_PATH = "font_path"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_PROJECT_HOME = "project_home"

    private lateinit var prefs: SharedPreferences

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var r2rcPath: String?
        get() = prefs.getString(KEY_R2RC_PATH, null)
        set(value) { prefs.edit().putString(KEY_R2RC_PATH, value).apply() }
        
    // Custom r2rc content (stored in internal file)
    private var _r2rcFile: java.io.File? = null
    
    fun getR2rcFile(context: Context): java.io.File {
        if (_r2rcFile == null) {
            val binDir = java.io.File(context.filesDir, "radare2/bin")
            if (!binDir.exists()) binDir.mkdirs()
            _r2rcFile = java.io.File(binDir, ".radare2rc")
        }
        return _r2rcFile!!
    }

    fun getR2rcContent(context: Context): String {
        val file = getR2rcFile(context)
        return if (file.exists()) file.readText() else ""
    }
    
    fun setR2rcContent(context: Context, content: String) {
        val file = getR2rcFile(context)
        file.writeText(content)
    }

    var fontPath: String?
        get() = prefs.getString(KEY_FONT_PATH, null)
        set(value) { prefs.edit().putString(KEY_FONT_PATH, value).apply() }

    fun getCustomFont(): androidx.compose.ui.text.font.FontFamily? {
        val path = fontPath ?: return null
        val file = java.io.File(path)
        if (!file.exists()) return null
        return try {
            androidx.compose.ui.text.font.FontFamily(
                androidx.compose.ui.text.font.Font(file)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, "system") ?: "system"
        set(value) { prefs.edit().putString(KEY_LANGUAGE, value).apply() }

    var projectHome: String?
        get() = prefs.getString(KEY_PROJECT_HOME, null)
        set(value) { prefs.edit().putString(KEY_PROJECT_HOME, value).apply() }
}
