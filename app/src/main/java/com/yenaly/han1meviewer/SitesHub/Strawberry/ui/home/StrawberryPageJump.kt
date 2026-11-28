package com.yenaly.han1meviewer.SitesHub.Strawberry.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R

data class StrawberryPageStart(val page: Int, val startIndex: Int)

fun List<StrawberryPageStart>.pageAt(cardIndex: Int, fallback: Int): Int =
    lastOrNull { it.startIndex <= cardIndex }?.page ?: fallback

@Composable
fun StrawberryPageChip(
    page: Int,
    total: Int?,
    onJump: (Int) -> Boolean,
    modifier: Modifier = Modifier,
    showTotal: Boolean = false,
) {
    var showDialog by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .clickable { showDialog = true },
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (showTotal && total != null) {
                    stringResource(R.string.strawberry_page_of, page, total)
                } else {
                    stringResource(R.string.strawberry_page_only, page)
                },
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
    if (showDialog) {
        StrawberryPageJumpDialog(
            currentPage = page,
            total = total,
            onDismiss = { showDialog = false },
            onJump = { p ->
                if (onJump(p)) {
                    showDialog = false
                    true
                } else false
            },
        )
    }
}

@Composable
private fun StrawberryPageJumpDialog(
    currentPage: Int,
    total: Int?,
    onDismiss: () -> Unit,
    onJump: (Int) -> Boolean,
) {
    var text by remember { mutableStateOf(currentPage.toString()) }
    var invalid by remember { mutableStateOf(false) }

    fun submit() {
        val p = text.trim().toIntOrNull()
        val valid = p != null && p >= 1 && (total == null || p <= total)
        if (!valid || !onJump(p)) invalid = true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.strawberry_go_to_page)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { v ->
                    text = v.filter { it.isDigit() }.take(6)
                    invalid = false
                },
                singleLine = true,
                label = { Text(stringResource(R.string.strawberry_page_number)) },
                isError = invalid,
                supportingText = {
                    Text(
                        when {
                            invalid && total != null -> stringResource(R.string.strawberry_page_invalid, total)
                            invalid -> stringResource(R.string.strawberry_page_invalid_any)
                            total != null -> stringResource(R.string.strawberry_page_hint, total)
                            else -> ""
                        }
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Go,
                ),
                keyboardActions = KeyboardActions(onGo = { submit() }),
            )
        },
        confirmButton = {
            TextButton(onClick = { submit() }) { Text(stringResource(R.string.strawberry_go)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
