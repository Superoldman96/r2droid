package top.wsdx233.r2droid.core.ui.components

import android.content.Context
import android.graphics.Typeface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import org.eclipse.tm4e.core.registry.IThemeSource
import io.github.rosemoe.sora.widget.CodeEditor
import top.wsdx233.r2droid.ui.theme.LocalDarkTheme

private var textMateInitialized = false

private fun ensureTextMateInit(context: Context, isDark: Boolean) {
    if (textMateInitialized) {
        // Just switch theme if already initialized
        val themeName = if (isDark) "dark" else "light"
        try { ThemeRegistry.getInstance().setTheme(themeName) } catch (_: Exception) {}
        return
    }
    textMateInitialized = true

    FileProviderRegistry.getInstance().addFileProvider(
        AssetsFileResolver(context.applicationContext.assets)
    )

    val themeRegistry = ThemeRegistry.getInstance()
    for ((name, path) in listOf("dark" to "textmate/themes/dark.json", "light" to "textmate/themes/light.json")) {
        try {
            themeRegistry.loadTheme(
                ThemeModel(
                    IThemeSource.fromInputStream(
                        FileProviderRegistry.getInstance().tryGetInputStream(path), path, null
                    ), name
                ).apply { if (name == "dark") this.isDark = true }
            )
        } catch (_: Exception) {}
    }
    themeRegistry.setTheme(if (isDark) "dark" else "light")

    GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
}

@Composable
fun SoraCodeEditor(
    modifier: Modifier = Modifier,
    scopeName: String = "source.js",
    onEditorReady: (CodeEditor) -> Unit = {}
) {
    val isDark = LocalDarkTheme.current
    val editorRef = remember { mutableStateOf<CodeEditor?>(null) }

    // Update theme when dark mode changes
    LaunchedEffect(isDark) {
        editorRef.value?.let { editor ->
            val themeName = if (isDark) "dark" else "light"
            try {
                ThemeRegistry.getInstance().setTheme(themeName)
                editor.colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
            } catch (_: Exception) {}
        }
    }

    AndroidView(
        factory = { ctx ->
            ensureTextMateInit(ctx, isDark)
            CodeEditor(ctx).apply {
                typefaceText = Typeface.MONOSPACE
                setTextSize(14f)
                try {
                    colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
                    setEditorLanguage(TextMateLanguage.create(scopeName, true))
                } catch (_: Exception) {}
                editorRef.value = this
                onEditorReady(this)
                isHardwareAcceleratedDrawAllowed = true
            }
        },
        modifier = modifier,
        onRelease = { it.release() }
    )
}
