package com.yenaly.han1meviewer.ui.navigation.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.firebase.analytics.analytics
import com.yenaly.han1meviewer.R

internal data class LauncherItem(
    val name: String,
    @param:DrawableRes val iconRes: Int,
    val alias: String,
)

/** Explains what analytics collects; the person can acknowledge it or turn analytics off. */
@Composable
internal fun AnalyticsInfoDialog(
    onDeny: () -> Unit,
    onDismiss: () -> Unit,
) {
    val analyticsHtml = stringResource(R.string.about_analytics_summary)
    val analyticsMessage = remember(analyticsHtml) {
        AnnotatedString.fromHtml(analyticsHtml)
    }
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.about_analytics)) },
        text = { Text(text = analyticsMessage) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDeny) {
                Text(stringResource(R.string.deny))
            }
        },
    )
}

/** Lists the selectable launcher icons / names. */
@Composable
internal fun LauncherPickerDialog(
    items: List<LauncherItem>,
    onSelect: (LauncherItem) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(R.string.fake_app_icon),
                    style = MaterialTheme.typography.titleLarge,
                )
                items.forEach { item ->
                    TextButton(
                        onClick = { onSelect(item) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                painter = painterResource(item.iconRes),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(30.dp),
                            )
                            Text(item.name)
                        }
                    }
                }
            }
        }
    }
}
