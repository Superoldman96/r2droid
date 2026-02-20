package top.wsdx233.r2droid.feature.r2frida

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import top.wsdx233.r2droid.R
import top.wsdx233.r2droid.data.SettingsManager
import top.wsdx233.r2droid.util.R2FridaInstallState
import top.wsdx233.r2droid.util.R2FridaInstaller

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun R2FridaScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var installed by remember { mutableStateOf(R2FridaInstaller.isInstalled(context)) }
    val installState by R2FridaInstaller.state.collectAsState()

    // After install completes, refresh
    LaunchedEffect(installState.status) {
        if (installState.status == R2FridaInstallState.Status.DONE) {
            installed = R2FridaInstaller.isInstalled(context)
        }
    }

    DisposableEffect(Unit) {
        onDispose { R2FridaInstaller.resetState() }
    }

    if (installed && installState.status != R2FridaInstallState.Status.DONE) {
        R2FridaFeatureScreen(onBack = onBack)
    } else {
        R2FridaInstallScreen(onBack = onBack, installState = installState, onInstalled = { installed = true })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun R2FridaInstallScreen(
    onBack: () -> Unit,
    installState: R2FridaInstallState,
    onInstalled: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var useChinaSource by remember { mutableStateOf(SettingsManager.language == "zh") }
    val isWorking = installState.status in listOf(
        R2FridaInstallState.Status.FETCHING,
        R2FridaInstallState.Status.DOWNLOADING,
        R2FridaInstallState.Status.EXTRACTING
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.r2frida_install_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isWorking) {
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header icon
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.BugReport,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Description card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp).fillMaxWidth()) {
                    Text(
                        stringResource(R.string.r2frida_install_what),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.r2frida_install_what_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Status area
            AnimatedContent(
                targetState = installState.status,
                label = "install_status"
            ) { status ->
                when (status) {
                    R2FridaInstallState.Status.IDLE -> {
                        // Ready to install
                    }
                    R2FridaInstallState.Status.FETCHING -> {
                        StatusCard(
                            icon = Icons.Default.CloudDownload,
                            text = stringResource(R.string.r2frida_install_fetching),
                            showProgress = true
                        )
                    }
                    R2FridaInstallState.Status.DOWNLOADING -> {
                        StatusCard(
                            icon = Icons.Default.Download,
                            text = stringResource(R.string.r2frida_install_downloading),
                            progress = installState.progress
                        )
                    }
                    R2FridaInstallState.Status.EXTRACTING -> {
                        StatusCard(
                            icon = Icons.Default.FolderZip,
                            text = stringResource(R.string.r2frida_install_extracting),
                            showProgress = true
                        )
                    }
                    R2FridaInstallState.Status.DONE -> {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        stringResource(R.string.r2frida_install_done),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (installState.version.isNotEmpty()) {
                                        Text(
                                            "v${installState.version}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                    R2FridaInstallState.Status.ERROR -> {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(Modifier.padding(16.dp).fillMaxWidth()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Error,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        installState.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    stringResource(R.string.r2frida_install_switch_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Action buttons
            if (installState.status == R2FridaInstallState.Status.DONE) {
                Button(
                    onClick = onInstalled,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.r2frida_install_continue))
                }
            } else {
                Button(
                    onClick = {
                        scope.launch { R2FridaInstaller.install(context, useChinaSource) }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isWorking
                ) {
                    if (isWorking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.r2frida_install_installing))
                    } else {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (installState.status == R2FridaInstallState.Status.ERROR)
                                stringResource(R.string.common_retry)
                            else
                                stringResource(R.string.r2frida_install_btn)
                        )
                    }
                }

                // Clickable source toggle
                TextButton(
                    onClick = { useChinaSource = !useChinaSource },
                    enabled = !isWorking,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(
                            if (useChinaSource) R.string.r2frida_source_gitee
                            else R.string.r2frida_source_github
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    progress: Float? = null,
    showProgress: Boolean = false
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp).fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (showProgress) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun R2FridaFeatureScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feature_r2frida_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.BugReport,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.r2frida_feature_wip),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.r2frida_feature_wip_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 48.dp)
            )
        }
    }
}
