package top.wsdx233.r2droid.feature.hex.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import top.wsdx233.r2droid.R

@Composable
fun HexContextMenu(
    expanded: Boolean,
    address: Long,
    onDismiss: () -> Unit,
    onCopy: (String) -> Unit,
    onModify: (String) -> Unit, // type: hex, string, asm
    onXrefs: () -> Unit,
    onCustomCommand: () -> Unit,
    offset: DpOffset = DpOffset.Zero
) {
    if (expanded) {
        var currentMenu by remember { mutableStateOf("main") }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss,
            offset = offset
        ) {
            when (currentMenu) {
                "main" -> {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_copy_submenu)) },
                        onClick = { currentMenu = "copy" },
                        trailingIcon = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Submenu") }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_modify_submenu)) },
                        onClick = { currentMenu = "modify" },
                        trailingIcon = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Submenu") }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_xrefs)) },
                        onClick = { onXrefs() }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_custom_command)) },
                        onClick = { onCustomCommand() }
                    )
                }
                "copy" -> {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_back)) },
                        onClick = { currentMenu = "main" },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.copy_address)) },
                        onClick = { onCopy("0x%08x".format(address)) }
                    )
                }
                "modify" -> {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_back)) },
                        onClick = { currentMenu = "main" },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.hex_modify_hex)) },
                        onClick = { onModify("hex") }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.hex_modify_string)) },
                        onClick = { onModify("string") }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.hex_modify_opcode)) },
                        onClick = { onModify("asm") }
                    )
                }
            }
        }
    }
}
