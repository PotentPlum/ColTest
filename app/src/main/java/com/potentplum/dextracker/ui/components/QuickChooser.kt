package com.potentplum.dextracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.potentplum.dextracker.domain.CardCondition
import com.potentplum.dextracker.domain.CardLanguage
import com.potentplum.dextracker.ui.theme.DexText
import com.potentplum.dextracker.ui.theme.Screen
import com.potentplum.dextracker.ui.theme.ScreenDeep
import com.potentplum.dextracker.ui.theme.ScreenGlow
import com.potentplum.dextracker.ui.theme.ScreenLine
import com.potentplum.dextracker.ui.theme.ScreenText
import com.potentplum.dextracker.ui.theme.ScreenTextDim

/** One choice in a [QuickChooser]. */
data class ChooserOption(
    val label: String,
    /** Shown dimmed on the right, e.g. how many are owned. */
    val detail: String? = null,
    val icon: ImageVector? = null,
    val onClick: () -> Unit,
)

/**
 * A short sheet with one big button per choice, for the moments a single tap can't know what
 * to do, like which version of a card to add. Picking an option closes the sheet.
 */
@Composable
fun QuickChooser(
    title: String,
    subtitle: String,
    options: List<ChooserOption>,
    onDismiss: () -> Unit,
) {
    DexSheet(onDismiss) {
        QuickChooserContent(title, subtitle, options, onDismiss)
    }
}

@Composable
internal fun QuickChooserContent(
    title: String,
    subtitle: String,
    options: List<ChooserOption>,
    onDismiss: () -> Unit,
) {
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
    ) {
        Text(title.uppercase(), style = DexText.ScreenLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(subtitle, style = DexText.ScreenSmall, color = ScreenTextDim)
        Spacer(Modifier.height(12.dp))
        options.forEach { option ->
            ChooserRow(option, onClick = {
                option.onClick()
                onDismiss()
            })
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ChooserRow(option: ChooserOption, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(ScreenDeep)
            .border(1.dp, ScreenLine, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (option.icon != null) {
            Icon(option.icon, contentDescription = null, tint = ScreenGlow, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(option.label, style = DexText.Screen, color = ScreenText, modifier = Modifier.weight(1f))
        if (option.detail != null) {
            Spacer(Modifier.width(8.dp))
            Text(option.detail, style = DexText.ScreenSmall, color = ScreenTextDim)
        }
    }
}

/** Picks the condition and language that Quick add and Got it give each new copy. */
@Composable
fun AddingAsSheet(
    condition: CardCondition,
    language: CardLanguage,
    onConditionChange: (CardCondition) -> Unit,
    onLanguageChange: (CardLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    DexSheet(onDismiss) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 20.dp)) {
            Text("ADDING AS", style = DexText.ScreenLarge)
            Text(
                "Every copy you add with one tap gets this condition and language.",
                style = DexText.ScreenSmall,
                color = ScreenTextDim,
            )
            Spacer(Modifier.height(14.dp))
            ChoiceRow("Condition", CardCondition.entries, condition, { it.label }, onConditionChange)
            Spacer(Modifier.height(10.dp))
            ChoiceRow("Language", CardLanguage.entries, language, { it.label }, onLanguageChange)
            Spacer(Modifier.height(16.dp))
            DexButton(text = "Done", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** The app's bottom sheet: dark screen colours, opened fully. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DexSheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Screen,
        contentColor = ScreenText,
        dragHandle = { BottomSheetDefaults.DragHandle(color = ScreenLine) },
    ) {
        content()
    }
}

/** "Near Mint · English", the way a copy is described on buttons and snackbars. */
fun copyLabel(condition: CardCondition, language: CardLanguage) = "${condition.label} · ${language.label}"
