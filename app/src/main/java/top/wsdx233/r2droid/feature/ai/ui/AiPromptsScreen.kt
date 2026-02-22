package top.wsdx233.r2droid.feature.ai.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import top.wsdx233.r2droid.R
import top.wsdx233.r2droid.feature.ai.data.AiSettingsManager

private enum class PromptType { Agent, InstrExplain, DisasmPolish }

@Composable
fun AiPromptsScreen() {
    var editingType by remember { mutableStateOf<PromptType?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        PromptCard(
            title = stringResource(R.string.ai_prompt_agent_title),
            desc = stringResource(R.string.ai_prompt_agent_desc),
            preview = AiSettingsManager.systemPrompt,
            onClick = { editingType = PromptType.Agent }
        )
        Spacer(Modifier.height(8.dp))
        PromptCard(
            title = stringResource(R.string.ai_prompt_instr_title),
            desc = stringResource(R.string.ai_prompt_instr_desc),
            preview = AiSettingsManager.instrExplainPrompt,
            onClick = { editingType = PromptType.InstrExplain }
        )
        Spacer(Modifier.height(8.dp))
        PromptCard(
            title = stringResource(R.string.ai_prompt_disasm_title),
            desc = stringResource(R.string.ai_prompt_disasm_desc),
            preview = AiSettingsManager.disasmPolishPrompt,
            onClick = { editingType = PromptType.DisasmPolish }
        )
    }

    editingType?.let { type ->
        val (current, default) = when (type) {
            PromptType.Agent -> AiSettingsManager.systemPrompt to AiSettingsManager.DEFAULT_SYSTEM_PROMPT
            PromptType.InstrExplain -> AiSettingsManager.instrExplainPrompt to AiSettingsManager.DEFAULT_INSTR_EXPLAIN_PROMPT
            PromptType.DisasmPolish -> AiSettingsManager.disasmPolishPrompt to AiSettingsManager.DEFAULT_DISASM_POLISH_PROMPT
        }
        val title = when (type) {
            PromptType.Agent -> stringResource(R.string.ai_prompt_agent_title)
            PromptType.InstrExplain -> stringResource(R.string.ai_prompt_instr_title)
            PromptType.DisasmPolish -> stringResource(R.string.ai_prompt_disasm_title)
        }
        PromptEditDialog(
            title = title,
            currentPrompt = current,
            defaultPrompt = default,
            onDismiss = { editingType = null },
            onSave = { value ->
                when (type) {
                    PromptType.Agent -> AiSettingsManager.systemPrompt = value
                    PromptType.InstrExplain -> AiSettingsManager.instrExplainPrompt = value
                    PromptType.DisasmPolish -> AiSettingsManager.disasmPolishPrompt = value
                }
                editingType = null
            },
            onReset = {
                when (type) {
                    PromptType.Agent -> AiSettingsManager.systemPrompt = default
                    PromptType.InstrExplain -> AiSettingsManager.instrExplainPrompt = default
                    PromptType.DisasmPolish -> AiSettingsManager.disasmPolishPrompt = default
                }
                editingType = null
            }
        )
    }
}

@Composable
private fun PromptCard(
    title: String,
    desc: String,
    preview: String,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                preview,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun PromptEditDialog(
    title: String,
    currentPrompt: String,
    defaultPrompt: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onReset: () -> Unit
) {
    var text by remember { mutableStateOf(currentPrompt) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.focusable()) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 8,
                    maxLines = 15
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }) {
                Text(stringResource(R.string.ai_save))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onReset) {
                    Text(stringResource(R.string.ai_prompt_reset))
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.ai_cancel))
                }
            }
        }
    )
}
