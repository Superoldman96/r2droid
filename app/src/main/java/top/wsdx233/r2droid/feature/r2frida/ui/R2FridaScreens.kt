package top.wsdx233.r2droid.feature.r2frida.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.rosemoe.sora.widget.CodeEditor
import top.wsdx233.r2droid.R
import top.wsdx233.r2droid.core.ui.components.SoraCodeEditor
import top.wsdx233.r2droid.feature.r2frida.data.*
import top.wsdx233.r2droid.util.LogEntry
import top.wsdx233.r2droid.util.LogType

@Composable
fun FridaOverviewScreen(info: FridaInfo?) {
    if (info == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            stringResource(R.string.r2frida_overview_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        OverviewInfoCard(
            icon = Icons.Default.Memory,
            title = "Runtime",
            items = listOf(
                "Arch" to "${info.arch} (${info.bits}bit)",
                "OS" to info.os,
                "Runtime" to info.runtime,
                "Page Size" to "${info.pageSize}",
                "Pointer Size" to "${info.pointerSize}"
            )
        )
        OverviewInfoCard(
            icon = Icons.Default.Apps,
            title = "Process",
            items = listOf(
                "PID" to "${info.pid}",
                "UID" to "${info.uid}",
                "Module" to info.moduleName,
                "Base" to info.moduleBase
            )
        )
        if (info.packageName.isNotEmpty()) {
            OverviewInfoCard(
                icon = Icons.Default.Android,
                title = "Android",
                items = listOf(
                    "Package" to info.packageName,
                    "Data Dir" to info.dataDir,
                    "CWD" to info.cwd,
                    "Code Path" to info.codePath
                )
            )
        }
        OverviewInfoCard(
            icon = Icons.Default.Extension,
            title = "Features",
            items = listOf(
                "Java" to if (info.java) "Yes" else "No",
                "ObjC" to if (info.objc) "Yes" else "No",
                "Swift" to if (info.swift) "Yes" else "No"
            )
        )
    }
}

@Composable
private fun OverviewInfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    items: List<Pair<String, String>>
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp))
            items.forEach { (key, value) ->
                if (value.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(key, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        SelectionContainer {
                            Text(value, style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 220.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FridaScriptScreen(
    logs: List<LogEntry>,
    running: Boolean,
    onRun: (String) -> Unit
) {
    var editorRef by remember { mutableStateOf<CodeEditor?>(null) }
    var logPanelVisible by remember { mutableStateOf(false) }

    // Auto-open log panel when script starts running
    LaunchedEffect(running) {
        if (running) logPanelVisible = true
    }

    Box(Modifier.fillMaxSize()) {
        // Full-screen Sora editor
        SoraCodeEditor(
            modifier = Modifier.fillMaxSize(),
            scopeName = "source.js",
            onEditorReady = { editorRef = it }
        )

        // Floating action buttons (top-right)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Log toggle button
            FloatingActionButton(
                onClick = { logPanelVisible = !logPanelVisible },
                modifier = Modifier.size(40.dp),
                containerColor = if (logPanelVisible)
                    MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                BadgedBox(badge = {
                    if (logs.isNotEmpty()) Badge { Text("${logs.size}") }
                }) {
                    Icon(Icons.Default.Terminal, null, modifier = Modifier.size(20.dp))
                }
            }
            // Run button
            FloatingActionButton(
                onClick = {
                    editorRef?.let { onRun(it.text.toString()) }
                },
                modifier = Modifier.size(40.dp),
                containerColor = if (running)
                    MaterialTheme.colorScheme.surfaceContainerHigh
                else MaterialTheme.colorScheme.primary
            ) {
                if (running) {
                    CircularProgressIndicator(
                        Modifier.size(20.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Icon(
                        Icons.Default.PlayArrow, null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        // Log panel (bottom)
        AnimatedVisibility(
            visible = logPanelVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) {
            FridaLogPanel(logs, onClose = { logPanelVisible = false })
        }
    }
}

@Composable
private fun FridaLogPanel(logs: List<LogEntry>, onClose: () -> Unit) {
    val bg = colorResource(R.color.command_output_background)
    val fg = colorResource(R.color.command_output_text)
    val ph = colorResource(R.color.command_output_placeholder)
    val scrollState = rememberScrollState()

    // Auto-scroll to bottom on new logs
    LaunchedEffect(logs.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Surface(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.4f),
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        shadowElevation = 8.dp,
        color = bg
    ) {
        Column {
            // Header
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.r2frida_script_output),
                    style = MaterialTheme.typography.labelMedium,
                    color = ph, modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp), tint = ph)
                }
            }
            HorizontalDivider(color = ph.copy(alpha = 0.3f))
            // Log content
            Box(
                Modifier.fillMaxSize().padding(8.dp).verticalScroll(scrollState)
            ) {
                if (logs.isEmpty()) {
                    Text(
                        stringResource(R.string.r2frida_script_no_output),
                        fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = ph
                    )
                } else {
                    SelectionContainer {
                        Column {
                            logs.forEach { entry ->
                                val color = when (entry.type) {
                                    LogType.ERROR -> MaterialTheme.colorScheme.error
                                    LogType.WARNING -> ph
                                    else -> fg
                                }
                                Text(
                                    entry.message, fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp, color = color
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
