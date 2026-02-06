package top.wsdx233.r2droid.screen.project

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import top.wsdx233.r2droid.data.model.*
import top.wsdx233.r2droid.data.repository.ProjectRepository
import top.wsdx233.r2droid.util.R2PipeManager

sealed class ProjectUiState {
    object Idle : ProjectUiState()
    data class Configuring(val filePath: String) : ProjectUiState()
    object Loading : ProjectUiState()
    data class Success(
        val binInfo: BinInfo? = null,
        val sections: List<Section> = emptyList(),
        val symbols: List<Symbol> = emptyList(),
        val imports: List<ImportInfo> = emptyList(),
        val relocations: List<Relocation> = emptyList(),
        val strings: List<StringInfo> = emptyList(),
        val functions: List<FunctionInfo> = emptyList()
    ) : ProjectUiState()
    data class Error(val message: String) : ProjectUiState()
}

class ProjectViewModel : ViewModel() {
    private val repository = ProjectRepository()

    private val _uiState = MutableStateFlow<ProjectUiState>(ProjectUiState.Idle)
    val uiState: StateFlow<ProjectUiState> = _uiState.asStateFlow()

    init {
        // Init logic moved to initialize() to support ViewModel reuse
        // in simple navigation setups (Activity-scoped ViewModel)
    }

    fun initialize() {
        val path = R2PipeManager.pendingFilePath
        if (path != null) {
            // New file waiting to be configured
            _uiState.value = ProjectUiState.Configuring(path)
        } else {
             // No new file pending.
             // If we are already displaying data (Success), do nothing.
             // If we are Idle/Error, try to recover session if connected.
             if (_uiState.value is ProjectUiState.Idle || _uiState.value is ProjectUiState.Error) {
                 if (R2PipeManager.isConnected) {
                    loadAllData()
                } else {
                     _uiState.value = ProjectUiState.Error("No file selected or session active")
                }
             }
        }
    }

    fun startAnalysisSession(context: Context, analysisCmd: String, writable: Boolean, startupFlags: String) {
         val currentState = _uiState.value
         if (currentState is ProjectUiState.Configuring) {
             viewModelScope.launch {
                 _uiState.value = ProjectUiState.Loading
                 
                 val flags = if (writable) "-w $startupFlags" else startupFlags
                 
                 // Open Session
                 val openResult = R2PipeManager.open(context, currentState.filePath, flags.trim())

                 if (openResult.isSuccess) {
                     // Run Analysis
                     if (analysisCmd.isNotBlank() && analysisCmd != "none") {
                         R2PipeManager.execute(analysisCmd)
                     }
                     // Load Data
                     loadAllData()
                     
                     // Clear pending path so subsequent navigations (or rotations) rely on configured state
                     R2PipeManager.pendingFilePath = null
                 } else {
                     _uiState.value = ProjectUiState.Error(openResult.exceptionOrNull()?.message ?: "Unknown error")
                 }
             }
         }
    }

    fun loadAllData() {
        viewModelScope.launch {
            // _uiState.value = ProjectUiState.Loading // Don't reset if already loading
            
            val binInfoResult = repository.getOverview()
            if (binInfoResult.isFailure) {
                _uiState.value = ProjectUiState.Error("Failed to load binary info: ${binInfoResult.exceptionOrNull()?.message}")
                return@launch
            }
            
            val sectionsResult = repository.getSections()
            val symbolsResult = repository.getSymbols()
            val importsResult = repository.getImports()
            val relocationsResult = repository.getRelocations()
            val stringsResult = repository.getStrings()
            val functionsResult = repository.getFunctions()
            
            _uiState.value = ProjectUiState.Success(
                binInfo = binInfoResult.getOrNull(),
                sections = sectionsResult.getOrDefault(emptyList()),
                symbols = symbolsResult.getOrDefault(emptyList()),
                imports = importsResult.getOrDefault(emptyList()),
                relocations = relocationsResult.getOrDefault(emptyList()),
                strings = stringsResult.getOrDefault(emptyList()),
                functions = functionsResult.getOrDefault(emptyList())
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        R2PipeManager.close()
    }
}
