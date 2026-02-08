package top.wsdx233.r2droid.screen.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import top.wsdx233.r2droid.R
import top.wsdx233.r2droid.data.SettingsManager
import top.wsdx233.r2droid.util.UriUtils

class SettingsViewModel : androidx.lifecycle.ViewModel() {
    // R2RC Content State
    private val _r2rcContent = MutableStateFlow("")
    val r2rcContent = _r2rcContent.asStateFlow()

    private val _fontPath = MutableStateFlow(SettingsManager.fontPath)
    val fontPath = _fontPath.asStateFlow()

    private val _language = MutableStateFlow(SettingsManager.language)
    val language = _language.asStateFlow()

    private val _projectHome = MutableStateFlow(SettingsManager.projectHome)
    val projectHome = _projectHome.asStateFlow()
    
    // Initialize r2rc content
    fun loadR2rcContent(context: Context) {
        _r2rcContent.value = SettingsManager.getR2rcContent(context)
    }
    
    fun saveR2rcContent(context: Context, content: String) {
        SettingsManager.setR2rcContent(context, content)
        _r2rcContent.value = content
    }
    
    fun setFontPath(path: String?) {
        SettingsManager.fontPath = path
        _fontPath.value = path
    }
    
    fun setLanguage(lang: String) {
        SettingsManager.language = lang
        _language.value = lang
    }
    
    fun setProjectHome(path: String?) {
        SettingsManager.projectHome = path
        _projectHome.value = path
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onBackClick: () -> Unit
) {
    val r2rcContent by viewModel.r2rcContent.collectAsState()
    val fontPath by viewModel.fontPath.collectAsState()
    val language by viewModel.language.collectAsState()
    val projectHome by viewModel.projectHome.collectAsState()
    
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.loadR2rcContent(context)
    }
    
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showR2rcDialog by remember { mutableStateOf(false) }
    
    // R2RC Dialog state
    var tempR2rcContent by remember { mutableStateOf("") }
    
    val fontPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
             viewModel.setFontPath(UriUtils.getPath(context, it))
        }
    }
    
    val dirPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            viewModel.setProjectHome(UriUtils.getPath(context, it))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                SettingsSectionHeader(stringResource(R.string.settings_general))
            }
            
            item {
                SettingsItem(
                    title = stringResource(R.string.settings_r2rc),
                    subtitle = if (r2rcContent.isBlank()) "Default" else "Customized",
                    icon = Icons.Default.Settings,
                    onClick = { 
                        tempR2rcContent = r2rcContent
                        showR2rcDialog = true 
                    }
                )
            }
            
            item {
                SettingsItem(
                    title = stringResource(R.string.settings_project_dir),
                    subtitle = projectHome ?: stringResource(R.string.settings_project_dir_desc),
                    icon = Icons.Default.Folder,
                    onClick = { dirPicker.launch(null) }
                )
            }
            
            item {
                HorizontalDivider()
                SettingsSectionHeader(stringResource(R.string.settings_appearance))
            }
            
            item {
                SettingsItem(
                    title = stringResource(R.string.settings_font),
                    subtitle = fontPath ?: stringResource(R.string.settings_font_default),
                    icon = Icons.Default.FontDownload,
                    onClick = { fontPicker.launch(arrayOf("font/ttf", "font/otf", "*/*")) }
                )
            }
            
            item {
                val languageLabel = when(language) {
                    "en" -> "English"
                    "zh" -> "中文"
                    else -> stringResource(R.string.settings_font_default) // "Default" (System)
                }
                SettingsItem(
                    title = stringResource(R.string.settings_language),
                    subtitle = languageLabel,
                    icon = Icons.Default.Language,
                    onClick = { showLanguageDialog = true }
                )
            }
        }
    }
    
    if (showR2rcDialog) {
        AlertDialog(
            onDismissRequest = { showR2rcDialog = false },
            title = { Text(stringResource(R.string.settings_r2rc)) },
            text = {
                OutlinedTextField(
                    value = tempR2rcContent,
                    onValueChange = { tempR2rcContent = it },
                    label = { Text("Content") },
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    maxLines = 20
                )
            },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.saveR2rcContent(context, tempR2rcContent)
                    showR2rcDialog = false 
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showR2rcDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.settings_language)) },
            text = {
                Column {
                    LanguageOption("System Default", "system", language) { viewModel.setLanguage(it); showLanguageDialog = false }
                    LanguageOption("English", "en", language) { viewModel.setLanguage(it); showLanguageDialog = false }
                    LanguageOption("中文", "zh", language) { viewModel.setLanguage(it); showLanguageDialog = false }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun LanguageOption(label: String, value: String, currentValue: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(value) }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = (value == currentValue),
            onClick = { onSelect(value) }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label)
    }
}
