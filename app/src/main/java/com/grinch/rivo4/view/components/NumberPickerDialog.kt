package com.grinch.rivo4.view.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.runtime.Composable

@Composable
fun NumberPickerDialog(
    numbers: List<String>,
    onDismissRequest: () -> Unit,
    onNumberSelected: (String) -> Unit
) {
    RivoSelectionDialog(
        onDismissRequest = onDismissRequest,
        title = "Select Number",
        items = numbers,
        itemLabel = { it },
        onItemSelected = onNumberSelected,
        icon = Icons.Default.Phone,
        itemIcon = { Icons.Default.Phone }
    )
}
