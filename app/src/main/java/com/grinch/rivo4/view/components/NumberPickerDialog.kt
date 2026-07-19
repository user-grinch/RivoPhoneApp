package com.grinch.rivo4.view.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.grinch.rivo4.R

@Composable
fun NumberPickerDialog(
    numbers: List<String>,
    onDismissRequest: () -> Unit,
    onNumberSelected: (String) -> Unit
) {
    RivoSelectionDialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.select_number_title),
        items = numbers,
        itemLabel = { it },
        onItemSelected = onNumberSelected,
        icon = Icons.Default.Phone,
        itemIcon = { Icons.Default.Phone }
    )
}
