package top.wsdx233.r2droid.screen.home

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import top.wsdx233.r2droid.R
import top.wsdx233.r2droid.activity.TerminalActivity
import top.wsdx233.r2droid.feature.manual.R2ManualScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeaturesScreen(
    onBackClick: () -> Unit,
    onCustomStart: (String) -> Unit
) {
    val context = LocalContext.current
    var showManual by remember { mutableStateOf(false) }
    var showCustomStartDialog by remember { mutableStateOf(false) }

    if (showManual) {
        Dialog(
            onDismissRequest = { showManual = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            R2ManualScreen(onBack = { showManual = false })
        }
    }

    if (showCustomStartDialog) {
        var customCommand by remember { mutableStateOf("r2 ") }
        AlertDialog(
            onDismissRequest = { showCustomStartDialog = false },
            title = { Text(stringResource(R.string.features_custom_start_dialog_title)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = customCommand,
                        onValueChange = { customCommand = it },
                        label = { Text(stringResource(R.string.features_custom_start_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.features_custom_start_examples),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCustomStartDialog = false
                        onCustomStart(customCommand)
                    },
                    enabled = customCommand.isNotBlank()
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomStartDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.features_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FeatureCard(
                title = stringResource(R.string.feature_r2frida_title),
                description = stringResource(R.string.feature_r2frida_desc),
                icon = Icons.Default.BugReport,
                iconTint = Color(0xFFEF5350),
                onClick = {}
            )

            FeatureCard(
                title = stringResource(R.string.feature_custom_start_title),
                description = stringResource(R.string.feature_custom_start_desc),
                icon = Icons.Default.Code,
                iconTint = Color(0xFF42A5F5),
                onClick = { showCustomStartDialog = true }
            )

            FeatureCard(
                title = stringResource(R.string.feature_terminal_title),
                description = stringResource(R.string.feature_terminal_desc),
                icon = Icons.Default.Terminal,
                iconTint = Color(0xFF546E7A),
                onClick = {
                    context.startActivity(Intent(context, TerminalActivity::class.java))
                }
            )

            FeatureCard(
                title = stringResource(R.string.feature_manual_title),
                description = stringResource(R.string.feature_manual_desc),
                icon = Icons.Default.MenuBook,
                iconTint = Color(0xFFFFA726),
                onClick = { showManual = true }
            )

            FeatureCard(
                title = stringResource(R.string.feature_plugins_title),
                description = stringResource(R.string.feature_plugins_desc),
                icon = Icons.Default.Extension,
                iconTint = Color(0xFFAB47BC),
                onClick = {}
            )
        }
    }
}

@Composable
fun FeatureCard(
    title: String,
    description: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
