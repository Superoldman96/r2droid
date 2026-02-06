

package top.wsdx233.r2droid.screen.project


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectScreen(
    viewModel: ProjectViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // Check intent on entry (handle new file selection)
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.initialize()
    }
    
    // Config state check
    if (uiState is ProjectUiState.Configuring) {
        val state = uiState as ProjectUiState.Configuring
        AnalysisConfigScreen(
            filePath = state.filePath,
            onStartAnalysis = { cmd, writable, flags ->
                viewModel.startAnalysisSession(context, cmd, writable, flags)
            }
        )
        return
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Sections", "Symbols", "Imports", "Relocs", "Strings", "Functions")

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Project Analysis") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            // Only show bottom bar when we have success (or maybe allow navigation even if empty?)
            // For now, let's allow it to be visible so the screen structure feels stable, 
            // even if content is loading/error.
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shadowElevation = 8.dp
            ) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent, // Transparent to use Surface color
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(text = title) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (val state = uiState) {
                is ProjectUiState.Idle -> {
                    Text("Idle", Modifier.align(Alignment.Center))
                }
                is ProjectUiState.Loading -> {
                     CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ProjectUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadAllData() }) {
                            Text("Retry")
                        }
                    }
                }
                is ProjectUiState.Success -> {
                    when (selectedTabIndex) {
                        0 -> state.binInfo?.let { OverviewCard(it) } ?: Text("No Data", Modifier.align(Alignment.Center))
                        1 -> SectionList(state.sections)
                        2 -> SymbolList(state.symbols)
                        3 -> ImportList(state.imports)
                        4 -> RelocationList(state.relocations)
                        5 -> StringList(state.strings)
                        6 -> FunctionList(state.functions)
                    }
                }
                else -> {} // Configuring handled above
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisConfigScreen(
    filePath: String,
    onStartAnalysis: (cmd: String, writable: Boolean, flags: String) -> Unit
) {
    var selectedLevel by remember { mutableStateOf("aaa") }
    var customCmd by remember { mutableStateOf("") }
    var isWritable by remember { mutableStateOf(false) }
    var customFlags by remember { mutableStateOf("") }
    
    val levels = listOf(
        "None" to "none",
        "Auto (aaa)" to "aaa",
        "Experimental (aaaa)" to "aaaa",
        "Custom" to "custom"
    )

    Scaffold(
        topBar = {
             CenterAlignedTopAppBar(title = { Text("Configure Analysis") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // File Info
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(16.dp).fillMaxWidth()) {
                    Text("Target File", style = MaterialTheme.typography.labelMedium)
                    Text(filePath, style = MaterialTheme.typography.bodyMedium)
                }
            }
            
            // Analysis Level
            Text("Analysis Level", style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                levels.forEach { (label, value) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedLevel = value }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = (selectedLevel == value),
                            onClick = { selectedLevel = value }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(text = label)
                    }
                }
            }
            
            if (selectedLevel == "custom") {
                OutlinedTextField(
                    value = customCmd,
                    onValueChange = { customCmd = it },
                    label = { Text("Custom Analysis Command") },
                    placeholder = { Text("e.g. aa") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            HorizontalDivider()
            
            // Startup Options
            Text("Startup Options", style = MaterialTheme.typography.titleMedium)
            
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { isWritable = !isWritable }) {
                Checkbox(checked = isWritable, onCheckedChange = { isWritable = it })
                Text("Open in Writable Mode (-w)")
            }
            
            OutlinedTextField(
                value = customFlags,
                onValueChange = { customFlags = it },
                label = { Text("Additional Startup Flags") },
                placeholder = { Text("-n -h") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = {
                    val finalCmd = if (selectedLevel == "custom") customCmd else selectedLevel
                    onStartAnalysis(finalCmd, isWritable, customFlags)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Start Analysis")
            }
        }
    }
}
