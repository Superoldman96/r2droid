package top.wsdx233.r2droid.feature.project

import androidx.annotation.StringRes

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MenuOpen
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerInput
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import top.wsdx233.r2droid.R
import top.wsdx233.r2droid.util.R2PipeManager
import androidx.core.net.toUri
import top.wsdx233.r2droid.feature.hex.ui.HexViewer
import top.wsdx233.r2droid.feature.disasm.ui.DisassemblyViewer
import top.wsdx233.r2droid.feature.hex.HexEvent
import top.wsdx233.r2droid.feature.disasm.DisasmEvent
import top.wsdx233.r2droid.feature.decompiler.ui.DecompilationViewer
import top.wsdx233.r2droid.core.ui.dialogs.JumpDialog
import top.wsdx233.r2droid.core.ui.dialogs.XrefsDialog
import top.wsdx233.r2droid.feature.bininfo.ui.*
import top.wsdx233.r2droid.feature.terminal.ui.CommandScreen
import top.wsdx233.r2droid.feature.terminal.ui.TerminalScreen
import top.wsdx233.r2droid.core.ui.components.ListItemActions

enum class MainCategory(@StringRes val titleRes: Int, val icon: ImageVector) {
    List(R.string.proj_category_list, Icons.Filled.List),
    Detail(R.string.proj_category_detail, Icons.Filled.Info),
    Project(R.string.proj_category_project, Icons.Filled.Build)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectScreen(
    viewModel: ProjectViewModel = hiltViewModel(),
    hexViewModel: top.wsdx233.r2droid.feature.hex.HexViewModel = hiltViewModel(),
    disasmViewModel: top.wsdx233.r2droid.feature.disasm.DisasmViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val saveState by viewModel.saveProjectState.collectAsState()
    val context = LocalContext.current
    
    // Observe global data invalidation
    val globalInvalidation by viewModel.globalDataInvalidated.collectAsState()
    androidx.compose.runtime.LaunchedEffect(globalInvalidation) {
        if (globalInvalidation > 0) {
            hexViewModel.onEvent(top.wsdx233.r2droid.feature.hex.HexEvent.RefreshData)
            disasmViewModel.onEvent(top.wsdx233.r2droid.feature.disasm.DisasmEvent.RefreshData)
        }
    }
    
    // Hex/Disasm data modification events (to sync with each other if needed, though global covers most)
    // For specific fine-grained updates, we could listen to hexViewModel.dataModifiedEvent

    
    // State for exit confirmation dialog
    var showExitDialog by remember { mutableStateOf(false) }
    var showSaveBeforeExitDialog by remember { mutableStateOf(false) }
    var exitProjectName by remember { mutableStateOf("") }
    
    // Initialize saved project repository - No op now as Hilt handles it
    // Check intent on entry (handle new file selection)
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.onEvent(ProjectEvent.Initialize)
    }
    
    // Handle project restoration
    androidx.compose.runtime.LaunchedEffect(uiState) {
        if (uiState is ProjectUiState.Analyzing && R2PipeManager.pendingRestoreFlags != null) {
            viewModel.onEvent(ProjectEvent.StartRestoreSession)
        }
    }
    
    // Handle back press with save confirmation
    androidx.activity.compose.BackHandler(
        enabled = uiState is ProjectUiState.Success && R2PipeManager.currentProjectId == null
    ) {
        // Show save confirmation dialog if not already saved
        showExitDialog = true
    }
    
    // Handle save completion when exiting
    androidx.compose.runtime.LaunchedEffect(saveState) {
        if (showSaveBeforeExitDialog && saveState is SaveProjectState.Success) {
            showSaveBeforeExitDialog = false
            onNavigateBack()
        }
    }
    
    // Exit confirmation dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(stringResource(top.wsdx233.r2droid.R.string.project_exit_title)) },
            text = { Text(stringResource(top.wsdx233.r2droid.R.string.project_exit_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        showSaveBeforeExitDialog = true
                        // Set default name from file
                        exitProjectName = R2PipeManager.currentFilePath?.let { 
                            java.io.File(it).name 
                        } ?: "Project"
                    }
                ) {
                    Text(stringResource(top.wsdx233.r2droid.R.string.project_exit_save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text(stringResource(top.wsdx233.r2droid.R.string.project_exit_discard))
                }
            }
        )
    }
    
    // Save before exit dialog
    if (showSaveBeforeExitDialog) {
        AlertDialog(
            onDismissRequest = { showSaveBeforeExitDialog = false },
            title = { Text(stringResource(top.wsdx233.r2droid.R.string.project_save_title)) },
            text = {
                OutlinedTextField(
                    value = exitProjectName,
                    onValueChange = { exitProjectName = it },
                    label = { Text(stringResource(top.wsdx233.r2droid.R.string.project_save_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onEvent(ProjectEvent.SaveProject(exitProjectName))
                        // Dialog will be closed by LaunchedEffect when save succeeds
                    },
                    enabled = saveState !is SaveProjectState.Saving
                ) {
                    if (saveState is SaveProjectState.Saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    Text(stringResource(top.wsdx233.r2droid.R.string.project_save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showSaveBeforeExitDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text(stringResource(top.wsdx233.r2droid.R.string.home_delete_cancel))
                }
            }
        )
    }
    
    // Config state check
    if (uiState is ProjectUiState.Configuring) {
        val state = uiState as ProjectUiState.Configuring
        AnalysisConfigScreen(
            filePath = state.filePath,
            onStartAnalysis = { cmd, writable, flags ->
                viewModel.onEvent(ProjectEvent.StartAnalysisSession(cmd, writable, flags))
            }
        )
        return
    }

    // Analyzing state check
    if (uiState is ProjectUiState.Analyzing) {
        AnalysisProgressScreen(logs = logs)
        return
    }

    // State for navigation
    var selectedCategory by remember { mutableStateOf(MainCategory.List) }
    var selectedListTabIndex by remember { mutableIntStateOf(0) }
    var selectedDetailTabIndex by remember { mutableIntStateOf(1) } // Default to Disassembly (index 1)
    var selectedProjectTabIndex by remember { mutableIntStateOf(0) } // Default to Settings (index 0)
    var showJumpDialog by remember { mutableStateOf(false) }

    val listTabs = listOf(
        R.string.proj_tab_overview, R.string.proj_tab_sections, R.string.proj_tab_symbols,
        R.string.proj_tab_imports, R.string.proj_tab_relocs, R.string.proj_tab_strings, R.string.proj_tab_functions
    )
    val detailTabs = listOf(R.string.proj_tab_hex, R.string.proj_tab_disassembly, R.string.proj_tab_decompile)
    val projectTabs = listOf(R.string.proj_tab_settings, R.string.proj_tab_cmd, R.string.proj_tab_logs)
    
    // Xrefs State

    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    
    // Define shared actions for list items
    val listItemActions = ListItemActions(
        onCopy = { text -> 
            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(text))
        },
        onJumpToHex = { addr ->
            selectedCategory = MainCategory.Detail
            selectedDetailTabIndex = 0
            viewModel.onEvent(ProjectEvent.JumpToAddress(addr))
        },
        onJumpToDisasm = { addr ->
            selectedCategory = MainCategory.Detail
            selectedDetailTabIndex = 1
            viewModel.onEvent(ProjectEvent.JumpToAddress(addr))
        },
        onShowXrefs = { addr ->
            disasmViewModel.onEvent(top.wsdx233.r2droid.feature.disasm.DisasmEvent.FetchXrefs(addr))
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                            Text(stringResource(top.wsdx233.r2droid.R.string.app_name))

                        },
                actions = {
                    if (selectedCategory == MainCategory.Detail) {
                         // Show back navigation button (undo)
                         val canGoBack by viewModel.canGoBack.collectAsState()
                         androidx.compose.material3.IconButton(
                             onClick = { viewModel.onEvent(ProjectEvent.NavigateBack) },
                             enabled = canGoBack
                         ) {
                             Icon(
                                 Icons.AutoMirrored.Filled.ArrowBack,
                                 contentDescription = stringResource(R.string.proj_nav_go_back),
                                 tint = if (canGoBack) MaterialTheme.colorScheme.onSurface 
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                             )
                         }
                         // Show scroll-to-selection button for Hex and Disassembly tabs
                         if (selectedDetailTabIndex == 0 || selectedDetailTabIndex == 1 || selectedDetailTabIndex == 2) {
                             androidx.compose.material3.IconButton(onClick = { viewModel.onEvent(ProjectEvent.RequestScrollToSelection) }) {
                                 Icon(Icons.Filled.MyLocation, contentDescription = stringResource(R.string.proj_nav_scroll_to_selection))
                             }
                         }
                         androidx.compose.material3.IconButton(onClick = { showJumpDialog = true }) {
                             Icon(Icons.AutoMirrored.Filled.MenuOpen, contentDescription = stringResource(R.string.menu_jump))
                         }
                    }
                }
            )
            
            if (showJumpDialog) {
                val currentAddress = (uiState as? ProjectUiState.Success)?.cursorAddress ?: 0L
                JumpDialog(
                    initialAddress = "0x%X".format(currentAddress),
                    onDismiss = { showJumpDialog = false },
                    onJump = { addr ->
                        viewModel.onEvent(ProjectEvent.JumpToAddress(addr))
                        showJumpDialog = false
                    }
                )
            }
            
            // Xrefs Dialog
            val xrefsState by disasmViewModel.xrefsState.collectAsState()
            if (xrefsState.visible) {
                XrefsDialog(
                    xrefsData = xrefsState.data,
                    targetAddress = xrefsState.targetAddress,
                    onDismiss = { disasmViewModel.onEvent(top.wsdx233.r2droid.feature.disasm.DisasmEvent.DismissXrefs) },
                    onJump = { addr ->
                        selectedCategory = MainCategory.Detail
                        selectedDetailTabIndex = 1 // Default to disasm for xref jumps
                        viewModel.onEvent(ProjectEvent.JumpToAddress(addr))
                        disasmViewModel.onEvent(top.wsdx233.r2droid.feature.disasm.DisasmEvent.DismissXrefs)
                    }
                )
            }
        },
        bottomBar = {
            // Two-layer Bottom Navigation
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shadowElevation = 8.dp
            ) {
                Column {
                    // Level 2: Sub-tabs (Text only)
                    when (selectedCategory) {
                        MainCategory.List -> {
                             ScrollableTabRow(
                                 selectedTabIndex = selectedListTabIndex,
                                 edgePadding = 0.dp,
                                 containerColor = MaterialTheme.colorScheme.surface,
                                 contentColor = MaterialTheme.colorScheme.primary,
                                 indicator = { tabPositions ->
                                     TabRowDefaults.SecondaryIndicator(
                                         modifier = Modifier.tabIndicatorOffset(tabPositions[selectedListTabIndex]),
                                         color = MaterialTheme.colorScheme.primary
                                     )
                                 }
                            ) {
                                listTabs.forEachIndexed { index, title ->
                                    Tab(
                                        selected = selectedListTabIndex == index,
                                        onClick = { selectedListTabIndex = index },
                                        text = { Text(text = stringResource(title)) }
                                    )
                                }
                            }
                        }
                        MainCategory.Detail -> {
                             TabRow(
                                selectedTabIndex = selectedDetailTabIndex,
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.primary,
                                indicator = { tabPositions ->
                                    TabRowDefaults.SecondaryIndicator(
                                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedDetailTabIndex]),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            ) {
                                detailTabs.forEachIndexed { index, title ->
                                    Tab(
                                        selected = selectedDetailTabIndex == index,
                                        onClick = { selectedDetailTabIndex = index },
                                        text = { Text(text = stringResource(title)) }
                                    )
                                }
                            }
                        }
                        MainCategory.Project -> {
                            TabRow(
                                selectedTabIndex = selectedProjectTabIndex,
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.primary,
                                indicator = { tabPositions ->
                                    TabRowDefaults.SecondaryIndicator(
                                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedProjectTabIndex]),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            ) {
                                projectTabs.forEachIndexed { index, title ->
                                    Tab(
                                        selected = selectedProjectTabIndex == index,
                                        onClick = { selectedProjectTabIndex = index },
                                        text = { Text(text = stringResource(title)) }
                                    )
                                }
                            }
                        }
                    }

                    // Level 1: Main Category (Icon + Title)
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ) {
                        MainCategory.values().forEach { category ->
                            NavigationBarItem(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                icon = { Icon(category.icon, contentDescription = stringResource(category.titleRes)) },
                                label = { Text(stringResource(category.titleRes)) }
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (val state = uiState) {
                is ProjectUiState.Idle -> {
                    Text(stringResource(R.string.common_idle), Modifier.align(Alignment.Center))
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
                            text = stringResource(R.string.common_error),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(16.dp))
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.retryLoadAll() }) {
                            Text(stringResource(R.string.common_retry))
                        }
                    }
                }
                is ProjectUiState.Success -> {
                    // Logic to display content
                    when (selectedCategory) {
                        MainCategory.List -> {
                            // Trigger data load based on selectedListTabIndex
                            // Tabs: Overview, Sections, Symbols, Imports, Relocs, Strings, Functions
                            androidx.compose.runtime.LaunchedEffect(selectedListTabIndex) {
                                when (selectedListTabIndex) {
                                    1 -> viewModel.onEvent(ProjectEvent.LoadSections())
                                    2 -> viewModel.onEvent(ProjectEvent.LoadSymbols())
                                    3 -> viewModel.onEvent(ProjectEvent.LoadImports())
                                    4 -> viewModel.onEvent(ProjectEvent.LoadRelocations())
                                    5 -> viewModel.onEvent(ProjectEvent.LoadStrings())
                                    6 -> viewModel.onEvent(ProjectEvent.LoadFunctions())
                                }
                            }

                            when (selectedListTabIndex) {
                                0 -> state.binInfo?.let { OverviewCard(it) } ?: Text(stringResource(R.string.hex_no_data), Modifier.align(Alignment.Center))
                                1 -> if (state.sections == null) CircularProgressIndicator(modifier = Modifier.align(Alignment.Center)) else SectionList(state.sections, listItemActions, onRefresh = { viewModel.onEvent(ProjectEvent.LoadSections(forceRefresh = true)) })
                                2 -> if (state.symbols == null) CircularProgressIndicator(modifier = Modifier.align(Alignment.Center)) else SymbolList(state.symbols, listItemActions, onRefresh = { viewModel.onEvent(ProjectEvent.LoadSymbols(forceRefresh = true)) })
                                3 -> if (state.imports == null) CircularProgressIndicator(modifier = Modifier.align(Alignment.Center)) else ImportList(state.imports, listItemActions, onRefresh = { viewModel.onEvent(ProjectEvent.LoadImports(forceRefresh = true)) })
                                4 -> if (state.relocations == null) CircularProgressIndicator(modifier = Modifier.align(Alignment.Center)) else RelocationList(state.relocations, listItemActions, onRefresh = { viewModel.onEvent(ProjectEvent.LoadRelocations(forceRefresh = true)) })
                                5 -> if (state.strings == null) CircularProgressIndicator(modifier = Modifier.align(Alignment.Center)) else StringList(state.strings, listItemActions, onRefresh = { viewModel.onEvent(ProjectEvent.LoadStrings(forceRefresh = true)) })
                                6 -> if (state.functions == null) CircularProgressIndicator(modifier = Modifier.align(Alignment.Center)) else FunctionList(state.functions, listItemActions, onRefresh = { viewModel.onEvent(ProjectEvent.LoadFunctions(forceRefresh = true)) })
                            }
                        }
                        MainCategory.Detail -> {
                            // Tabs: Hex, Disassembly, Decompile
                            // Tabs: Hex, Disassembly, Decompile
                             androidx.compose.runtime.LaunchedEffect(selectedDetailTabIndex) {
                                when (selectedDetailTabIndex) {
                                    0 -> {
                                        val sections = (uiState as? ProjectUiState.Success)?.sections ?: emptyList()
                                        val path = (uiState as? ProjectUiState.Success)?.let { _ -> R2PipeManager.currentFilePath }
                                        val cursor = (uiState as? ProjectUiState.Success)?.cursorAddress ?: 0L
                                        hexViewModel.onEvent(HexEvent.LoadHex(sections, path, cursor))
                                    }
                                    1 -> {
                                        val sections = (uiState as? ProjectUiState.Success)?.sections ?: emptyList()
                                        val path = (uiState as? ProjectUiState.Success)?.let { _ -> R2PipeManager.currentFilePath }
                                        val cursor = (uiState as? ProjectUiState.Success)?.cursorAddress ?: 0L
                                        disasmViewModel.onEvent(DisasmEvent.LoadDisassembly(sections, path, cursor))
                                    }
                                    2 -> viewModel.onEvent(ProjectEvent.LoadDecompilation)
                                }
                            }
                            
                            // React to cursor changes to preload data
                            androidx.compose.runtime.LaunchedEffect((uiState as? ProjectUiState.Success)?.cursorAddress) {
                                val cursor = (uiState as? ProjectUiState.Success)?.cursorAddress ?: return@LaunchedEffect
                                if (selectedDetailTabIndex == 0) hexViewModel.onEvent(top.wsdx233.r2droid.feature.hex.HexEvent.PreloadHex(cursor))
                                if (selectedDetailTabIndex == 1) disasmViewModel.onEvent(top.wsdx233.r2droid.feature.disasm.DisasmEvent.Preload(cursor))
                            }
                            
                            when(selectedDetailTabIndex) {
                                0 -> {
                                    // Make sure we trigger load if needed
                                    androidx.compose.runtime.LaunchedEffect(Unit) {
                                        val sections = (uiState as? ProjectUiState.Success)?.sections ?: emptyList()
                                        val path = (uiState as? ProjectUiState.Success)?.let { _ -> R2PipeManager.currentFilePath }
                                        val cursor = (uiState as? ProjectUiState.Success)?.cursorAddress ?: 0L
                                        hexViewModel.onEvent(HexEvent.LoadHex(sections, path, cursor))
                                    }
                                    
                                    if (hexViewModel.hexDataManager == null) {
                                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                                    } else {
                                        HexViewer(
                                            viewModel = hexViewModel,
                                            cursorAddress = state.cursorAddress,
                                            scrollToSelectionTrigger = viewModel.scrollToSelectionTrigger,
                                            onByteClick = { addr -> viewModel.onEvent(ProjectEvent.UpdateCursor(addr)) },
                                            onShowXrefs = { addr -> disasmViewModel.onEvent(top.wsdx233.r2droid.feature.disasm.DisasmEvent.FetchXrefs(addr)) }
                                        )
                                    }
                                }
                                1 -> {
                                     androidx.compose.runtime.LaunchedEffect(Unit) {
                                        val sections = (uiState as? ProjectUiState.Success)?.sections ?: emptyList()
                                        val path = (uiState as? ProjectUiState.Success)?.let { _ -> R2PipeManager.currentFilePath }
                                        val cursor = (uiState as? ProjectUiState.Success)?.cursorAddress ?: 0L
                                        disasmViewModel.onEvent(DisasmEvent.LoadDisassembly(sections, path, cursor))
                                    }
                                    
                                    if (disasmViewModel.disasmDataManager == null) {
                                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                                    } else {
                                        DisassemblyViewer(
                                            viewModel = disasmViewModel,
                                            cursorAddress = state.cursorAddress,
                                            scrollToSelectionTrigger = viewModel.scrollToSelectionTrigger,
                                            onInstructionClick = { addr -> viewModel.onEvent(ProjectEvent.UpdateCursor(addr)) }
                                        )
                                    }
                                }
                                2 -> if (state.decompilation == null) {
                                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                                } else {
                                    DecompilationViewer(
                                        viewModel = viewModel,
                                        data = state.decompilation,
                                        cursorAddress = state.cursorAddress,
                                        onAddressClick = { addr -> viewModel.onEvent(ProjectEvent.UpdateCursor(addr)) }
                                    )
                                }
                            }
                        }
                        MainCategory.Project -> {
                             // Project Category
                            when (selectedProjectTabIndex) {
                                0 -> ProjectSettingsScreen(viewModel)
                                1 -> CommandScreen()
                                2 -> LogList(logs, onClearLogs = { viewModel.onEvent(ProjectEvent.ClearLogs) })
                            }
                        }
                    }
                }
                else -> {} // Configuring handled above
            }
        }
    }
}

@Composable
fun ProjectSettingsScreen(viewModel: ProjectViewModel) {
    val context = LocalContext.current
    val saveState by viewModel.saveProjectState.collectAsState()
    var showSaveDialog by remember { mutableStateOf(false) }
    
    // Initialize repository
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.initializeSavedProjectRepository(context)
    }
    
    // Handle save state changes
    androidx.compose.runtime.LaunchedEffect(saveState) {
        when (saveState) {
            is SaveProjectState.Success -> {
                android.widget.Toast.makeText(
                    context, 
                    (saveState as SaveProjectState.Success).message, 
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                viewModel.onEvent(ProjectEvent.ResetSaveState)
            }
            is SaveProjectState.Error -> {
                android.widget.Toast.makeText(
                    context, 
                    (saveState as SaveProjectState.Error).message, 
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                viewModel.onEvent(ProjectEvent.ResetSaveState)
            }
            else -> {}
        }
    }
    
    // Save dialog
    if (showSaveDialog) {
        SaveProjectDialog(
            existingProjectId = viewModel.getCurrentProjectId(),
            onDismiss = { showSaveDialog = false },
            onSaveNew = { name ->
                viewModel.onEvent(ProjectEvent.SaveProject(name))
                showSaveDialog = false
            },
            onUpdate = { projectId ->
                viewModel.onEvent(ProjectEvent.UpdateProject(projectId))
                showSaveDialog = false
            }
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // File Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.proj_info_current_file),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = R2PipeManager.currentFilePath ?: stringResource(R.string.proj_info_no_file),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                
                if (R2PipeManager.currentProjectId != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.proj_info_project_id, R2PipeManager.currentProjectId ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                    )
                }
            }
        }
        
        // Save Project Section
        Text(
            text = stringResource(top.wsdx233.r2droid.R.string.project_save),
            style = MaterialTheme.typography.titleMedium
        )
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = saveState !is SaveProjectState.Saving) { 
                    showSaveDialog = true 
                },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (saveState is SaveProjectState.Saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(8.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (R2PipeManager.currentProjectId != null) 
                            stringResource(R.string.proj_save_update_title) 
                        else 
                            stringResource(top.wsdx233.r2droid.R.string.project_save_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = if (R2PipeManager.currentProjectId != null)
                            stringResource(R.string.proj_save_update_desc)
                        else
                            stringResource(R.string.proj_save_new_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
        
        // Startup Flags Section (for saved projects)
        if (R2PipeManager.currentProjectId != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(top.wsdx233.r2droid.R.string.project_startup_flags),
                style = MaterialTheme.typography.titleMedium
            )
            
            var startupFlags by remember { mutableStateOf("") }
            
            OutlinedTextField(
                value = startupFlags,
                onValueChange = { startupFlags = it },
                label = { Text(stringResource(top.wsdx233.r2droid.R.string.project_startup_flags_hint)) },
                placeholder = { Text("-w -n") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Text(
                text = stringResource(top.wsdx233.r2droid.R.string.project_startup_flags_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Terminal Section
        Text(
            text = stringResource(top.wsdx233.r2droid.R.string.terminal),
            style = MaterialTheme.typography.titleMedium
        )
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { 
                    val intent = android.content.Intent(context, top.wsdx233.r2droid.activity.TerminalActivity::class.java)
                    context.startActivity(intent)
                },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(top.wsdx233.r2droid.R.string.terminal),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = stringResource(R.string.proj_term_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
        
        // Session Info
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.proj_session_info),
            style = MaterialTheme.typography.titleMedium
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.proj_session_status), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        if (R2PipeManager.isConnected) stringResource(R.string.proj_session_connected) else stringResource(R.string.proj_session_disconnected),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (R2PipeManager.isConnected) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.error
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.proj_session_saved), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        if (R2PipeManager.currentProjectId != null) stringResource(R.string.common_yes) else stringResource(R.string.common_no),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (R2PipeManager.currentProjectId != null) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SaveProjectDialog(
    existingProjectId: String?,
    onDismiss: () -> Unit,
    onSaveNew: (name: String) -> Unit,
    onUpdate: (projectId: String) -> Unit
) {
    var projectName by remember { mutableStateOf("") }
    var saveAsNew by remember { mutableStateOf(existingProjectId == null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(top.wsdx233.r2droid.R.string.project_save_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (existingProjectId != null) {
                    // Options for existing project
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { saveAsNew = false }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !saveAsNew,
                            onClick = { saveAsNew = false }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.dialog_save_update_option))
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { saveAsNew = true }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = saveAsNew,
                            onClick = { saveAsNew = true }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.dialog_save_new_option))
                    }
                }
                
                if (saveAsNew || existingProjectId == null) {
                    OutlinedTextField(
                        value = projectName,
                        onValueChange = { projectName = it },
                        label = { Text(stringResource(top.wsdx233.r2droid.R.string.project_save_name_hint)) },
                        placeholder = { 
                            val fileName = R2PipeManager.currentFilePath?.let { 
                                java.io.File(it).name 
                            } ?: "Project"
                            Text(fileName)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (saveAsNew || existingProjectId == null) {
                        val name = projectName.ifBlank {
                            R2PipeManager.currentFilePath?.let { java.io.File(it).name } ?: "Project"
                        }
                        onSaveNew(name)
                    } else {
                        onUpdate(existingProjectId)
                    }
                }
            ) {
                Text(stringResource(top.wsdx233.r2droid.R.string.project_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(top.wsdx233.r2droid.R.string.home_delete_cancel))
            }
        }
    )
}

@Composable
fun PlaceholderScreen(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary)
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
        stringResource(R.string.analysis_level_none) to "none",
        stringResource(R.string.analysis_level_auto) to "aaa",
        stringResource(R.string.analysis_level_experimental) to "aaaa",
        stringResource(R.string.analysis_level_custom) to "custom"
    )
    
    val context = LocalContext.current
    var fileSize by remember { mutableStateOf(0L) }
    
    androidx.compose.runtime.LaunchedEffect(filePath) {
        try {
            val file = java.io.File(filePath)
            if (file.exists()) {
                fileSize = file.length()
            } else if (filePath.startsWith("content://")) {
                val uri = android.net.Uri.parse(filePath)
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                        if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    val isHeavyAnalysis = selectedLevel == "aaa" || selectedLevel == "aaaa"
    val isLargeFile = fileSize > 1024 * 1024 // 1MB
    val showWarning = isHeavyAnalysis && isLargeFile

    Scaffold(
        topBar = {
             CenterAlignedTopAppBar(title = { Text(stringResource(R.string.analysis_config_title)) })
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
                    Text(stringResource(R.string.analysis_target_file), style = MaterialTheme.typography.labelMedium)
                    Text(filePath, style = MaterialTheme.typography.bodyMedium)
                }
            }
            
            // Analysis Level
            Text(stringResource(R.string.analysis_level_title), style = MaterialTheme.typography.titleMedium)
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
                    label = { Text(stringResource(R.string.analysis_custom_cmd_label)) },
                    placeholder = { Text(stringResource(R.string.analysis_custom_cmd_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Warning Card
            if (showWarning) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info, 
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.analysis_large_file_warning_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.analysis_large_file_warning_message),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
            
            HorizontalDivider()
            
            // Startup Options
            Text(stringResource(R.string.analysis_startup_options), style = MaterialTheme.typography.titleMedium)
            
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { isWritable = !isWritable }) {
                Checkbox(checked = isWritable, onCheckedChange = { isWritable = it })
                Text(stringResource(R.string.analysis_writable_mode))
            }
            
            OutlinedTextField(
                value = customFlags,
                onValueChange = { customFlags = it },
                label = { Text(stringResource(R.string.analysis_startup_flags_label)) },
                placeholder = { Text(stringResource(R.string.analysis_startup_flags_hint)) },
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
                Text(stringResource(R.string.analysis_start_btn))
            }
        }
    }
}


