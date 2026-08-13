package com.grinch.rivo4.view.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.grinch.rivo4.R

@Composable
fun NumberPickerDialog(
    numbers: List<String>,
    onDismissRequest: () -> Unit,
    onNumberSelected: (String) -> Unit,
    title: String = stringResource(R.string.select_number_title),
    icon: ImageVector = Icons.Default.Phone,
    numberIcon: (String) -> ImageVector = { icon },
    numberSupporting: ((String) -> String)? = null,
    isSelected: (String) -> Boolean = { false }
) {
    RivoSelectionDialog(
        onDismissRequest = onDismissRequest,
        title = title,
        items = numbers,
        itemLabel = { it },
        onItemSelected = onNumberSelected,
        itemSupporting = numberSupporting,
        icon = icon,
        itemIcon = numberIcon,
        isSelected = isSelected
    )
}
