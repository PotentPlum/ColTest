package com.potentplum.dextracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.potentplum.dextracker.data.local.OwnedCopyEntity
import com.potentplum.dextracker.domain.CardCondition
import com.potentplum.dextracker.domain.CardLanguage
import com.potentplum.dextracker.domain.CardVariant
import com.potentplum.dextracker.ui.theme.DexText
import com.potentplum.dextracker.ui.theme.Screen
import com.potentplum.dextracker.ui.theme.ScreenDeep
import com.potentplum.dextracker.ui.theme.ScreenGlow
import com.potentplum.dextracker.ui.theme.ScreenLine
import com.potentplum.dextracker.ui.theme.ScreenText
import com.potentplum.dextracker.ui.theme.ScreenTextDim

/** The card shown at the top of a [CopiesSheet]. */
data class SheetCard(
    val cardId: String,
    val name: String,
    val subtitle: String,
    val imageSmall: String,
    val imageLarge: String,
    /** Variants copies can be added as. */
    val variantOptions: List<CardVariant>,
)

/**
 * Everything about the copies owned of one card: each variant / condition / language line
 * with a quantity stepper, and buttons to add another copy. Condition and language apply to
 * the copy being added and are remembered for next time.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CopiesSheet(
    card: SheetCard,
    copies: List<OwnedCopyEntity>,
    condition: CardCondition,
    language: CardLanguage,
    onConditionChange: (CardCondition) -> Unit,
    onLanguageChange: (CardLanguage) -> Unit,
    onAdd: (CardVariant) -> Unit,
    onChangeQuantity: (copyId: Long, delta: Int) -> Unit,
    onZoom: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Screen,
        contentColor = ScreenText,
        dragHandle = { BottomSheetDefaults.DragHandle(color = ScreenLine) },
    ) {
        CopiesSheetContent(card, copies, condition, language, onConditionChange, onLanguageChange, onAdd, onChangeQuantity, onZoom)
    }
}

/** The body of [CopiesSheet], separate so it can be previewed without the sheet popup. */
@Composable
internal fun CopiesSheetContent(
    card: SheetCard,
    copies: List<OwnedCopyEntity>,
    condition: CardCondition,
    language: CardLanguage,
    onConditionChange: (CardCondition) -> Unit,
    onLanguageChange: (CardLanguage) -> Unit,
    onAdd: (CardVariant) -> Unit,
    onChangeQuantity: (copyId: Long, delta: Int) -> Unit,
    onZoom: () -> Unit,
) {
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
    ) {
        Header(card, onZoom)
        if (copies.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            SectionLabel("Your copies")
            copies.sortedWith(compareBy({ CardVariant.fromKey(it.variant)?.ordinal }, { it.condition }, { it.language }))
                .forEach { copy ->
                    CopyRow(copy, onChangeQuantity)
                    HorizontalDivider(color = ScreenLine)
                }
        }
        Spacer(Modifier.height(16.dp))
        SectionLabel(if (copies.isEmpty()) "Add to your collection" else "Add another copy")
        AddCopyControls(card.variantOptions, condition, language, onConditionChange, onLanguageChange, onAdd)
    }
}

@Composable
private fun Header(card: SheetCard, onZoom: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TcgCardImage(
            url = card.imageSmall,
            contentDescription = "Zoom ${card.name}",
            modifier = Modifier
                .width(72.dp)
                .aspectRatio(0.716f)
                .clickable(onClickLabel = "Zoom", onClick = onZoom),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(card.name.uppercase(), style = DexText.ScreenLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(card.subtitle, style = DexText.ScreenSmall, color = ScreenTextDim)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = DexText.ScreenSmall.copy(fontSize = 12.sp),
        color = ScreenGlow,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@Composable
private fun CopyRow(copy: OwnedCopyEntity, onChangeQuantity: (Long, Int) -> Unit) {
    val variant = CardVariant.fromKey(copy.variant) ?: CardVariant.UNSPECIFIED
    val condition = CardCondition.fromCode(copy.condition)
    val language = CardLanguage.fromCode(copy.language)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) {
                    contentDescription = "${variant.label}, ${condition.label}, ${language.label}, quantity ${copy.quantity}"
                },
        ) {
            Text(
                variant.label,
                style = DexText.Screen,
                color = if (variant == CardVariant.UNSPECIFIED) ScreenTextDim else ScreenText,
            )
            Text("${condition.label} · ${language.label}", style = DexText.ScreenSmall, color = ScreenTextDim)
        }
        StepperButton(
            icon = if (copy.quantity == 1) Icons.Rounded.DeleteOutline else Icons.Rounded.Remove,
            label = if (copy.quantity == 1) "Remove this copy" else "One fewer",
            onClick = { onChangeQuantity(copy.id, -1) },
        )
        Text(
            "${copy.quantity}",
            style = DexText.ScreenLarge,
            modifier = Modifier.width(40.dp),
            textAlign = TextAlign.Center,
        )
        StepperButton(Icons.Rounded.Add, "One more", onClick = { onChangeQuantity(copy.id, 1) })
    }
}

@Composable
private fun StepperButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(ScreenDeep)
            .border(1.dp, ScreenLine, CircleShape)
            .clickable(onClickLabel = label, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = ScreenGlow)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddCopyControls(
    variants: List<CardVariant>,
    condition: CardCondition,
    language: CardLanguage,
    onConditionChange: (CardCondition) -> Unit,
    onLanguageChange: (CardLanguage) -> Unit,
    onAdd: (CardVariant) -> Unit,
) {
    Text("Condition · ${condition.label}", style = DexText.ScreenSmall, color = ScreenTextDim)
    Spacer(Modifier.height(4.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(CardCondition.entries) { option ->
            DexChoiceChip(
                text = option.code,
                selected = option == condition,
                onClick = { onConditionChange(option) },
                contentDescription = option.label,
            )
        }
    }
    Spacer(Modifier.height(10.dp))
    Text("Language · ${language.label}", style = DexText.ScreenSmall, color = ScreenTextDim)
    Spacer(Modifier.height(4.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(CardLanguage.entries) { option ->
            DexChoiceChip(
                text = option.code,
                selected = option == language,
                onClick = { onLanguageChange(option) },
                contentDescription = option.label,
            )
        }
    }
    Spacer(Modifier.height(14.dp))
    // One button per variant: picking the variant is the add. The regular print is the
    // highlighted one, since that's what most copies are.
    val usual = CardVariant.defaultFor(variants)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        variants.forEach { variant ->
            DexButton(
                text = if (variants.size == 1) "Add copy" else "+ ${variant.label}",
                onClick = { onAdd(variant) },
                style = if (variant == usual) DexButtonStyle.Primary else DexButtonStyle.Secondary,
                modifier = if (variants.size == 1) Modifier.fillMaxWidth() else Modifier,
            )
        }
    }
}
