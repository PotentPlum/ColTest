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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
 * with a quantity stepper, and buttons to add another copy. Tapping a line edits its variant,
 * condition or language. Condition and language apply to the copy being added and are
 * remembered for next time.
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
    onEditCopy: (copyId: Long, CardVariant, CardCondition, CardLanguage) -> Unit,
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
        CopiesSheetContent(
            card, copies, condition, language, onConditionChange, onLanguageChange, onAdd, onChangeQuantity,
            onEditCopy, onZoom,
        )
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
    onEditCopy: (copyId: Long, CardVariant, CardCondition, CardLanguage) -> Unit,
    onZoom: () -> Unit,
    initiallyEditing: Long? = null,
) {
    // The line being edited. If an edit merges it into another line, it's gone and the
    // editor simply closes.
    var editingId by rememberSaveable { mutableStateOf(initiallyEditing) }
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
                    val editing = copy.id == editingId
                    CopyRow(
                        copy = copy,
                        editing = editing,
                        onToggleEdit = { editingId = if (editing) null else copy.id },
                        onChangeQuantity = onChangeQuantity,
                    )
                    if (editing) {
                        CopyEditor(
                            copy = copy,
                            variantOptions = card.variantOptions,
                            onEdit = { variant, newCondition, newLanguage ->
                                onEditCopy(copy.id, variant, newCondition, newLanguage)
                            },
                            onDone = { editingId = null },
                        )
                    }
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
                .width(112.dp)
                .aspectRatio(0.716f)
                .clickable(onClickLabel = "Zoom", onClick = onZoom),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(card.name.uppercase(), style = DexText.ScreenLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(card.subtitle, style = DexText.ScreenSmall, color = ScreenTextDim)
            Spacer(Modifier.height(8.dp))
            Text("TAP THE CARD TO ZOOM", style = DexText.ScreenSmall.copy(fontSize = 10.sp), color = ScreenTextDim)
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
private fun CopyRow(
    copy: OwnedCopyEntity,
    editing: Boolean,
    onToggleEdit: () -> Unit,
    onChangeQuantity: (Long, Int) -> Unit,
) {
    val variant = CardVariant.fromKey(copy.variant) ?: CardVariant.UNSPECIFIED
    val condition = CardCondition.fromCode(copy.condition)
    val language = CardLanguage.fromCode(copy.language)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClickLabel = if (editing) "Close editor" else "Edit this copy", onClick = onToggleEdit)
                .padding(vertical = 4.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = "${variant.label}, ${condition.label}, ${language.label}, quantity ${copy.quantity}"
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    variant.label,
                    style = DexText.Screen,
                    color = if (variant == CardVariant.UNSPECIFIED) ScreenTextDim else ScreenText,
                )
                Text("${condition.label} · ${language.label}", style = DexText.ScreenSmall, color = ScreenTextDim)
            }
            Icon(
                if (editing) Icons.Rounded.ExpandLess else Icons.Rounded.Edit,
                contentDescription = null,
                tint = if (editing) ScreenGlow else ScreenTextDim,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(18.dp),
            )
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

/** Changes a line's variant, condition or language; each pick is saved straight away. */
@Composable
private fun CopyEditor(
    copy: OwnedCopyEntity,
    variantOptions: List<CardVariant>,
    onEdit: (CardVariant, CardCondition, CardLanguage) -> Unit,
    onDone: () -> Unit,
) {
    val variant = CardVariant.fromKey(copy.variant) ?: CardVariant.UNSPECIFIED
    val condition = CardCondition.fromCode(copy.condition)
    val language = CardLanguage.fromCode(copy.language)
    // A copy logged before variants were tracked keeps "not set" as a choice until it's picked.
    val variants = if (variant in variantOptions) variantOptions else variantOptions + variant
    // A recessed panel, so it reads as part of this line and not as the add controls below.
    Column(
        Modifier
            .padding(bottom = 10.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ScreenDeep)
            .border(1.dp, ScreenLine, RoundedCornerShape(10.dp))
            .padding(10.dp),
    ) {
        if (variants.size > 1) {
            ChoiceRow("Variant", variants, variant, { if (it == CardVariant.UNSPECIFIED) "Not set" else it.label }) {
                onEdit(it, condition, language)
            }
            Spacer(Modifier.height(10.dp))
        }
        ChoiceRow("Condition", CardCondition.entries, condition, { it.label }) { onEdit(variant, it, language) }
        Spacer(Modifier.height(10.dp))
        ChoiceRow("Language", CardLanguage.entries, language, { it.label }) { onEdit(variant, condition, it) }
        Spacer(Modifier.height(10.dp))
        DexButton(text = "Done", onClick = onDone, style = DexButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())
    }
}

/** A label over a sideways-scrolling row of chips, one of which is selected. */
@Composable
private fun <T> ChoiceRow(
    label: String,
    options: List<T>,
    selected: T,
    text: (T) -> String,
    onSelect: (T) -> Unit,
) {
    val listState = rememberLazyListState()
    // Bring the selection into view, e.g. Polish at the far end of the languages.
    LaunchedEffect(Unit) {
        val index = options.indexOf(selected)
        if (index > 0) listState.scrollToItem((index - 1).coerceAtLeast(0))
    }
    Text(label.uppercase(), style = DexText.ScreenSmall.copy(fontSize = 11.sp), color = ScreenTextDim)
    Spacer(Modifier.height(4.dp))
    LazyRow(state = listState, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(options) { option ->
            DexChoiceChip(
                text = text(option),
                selected = option == selected,
                onClick = { onSelect(option) },
            )
        }
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
    ChoiceRow("Condition", CardCondition.entries, condition, { it.label }, onConditionChange)
    Spacer(Modifier.height(10.dp))
    ChoiceRow("Language", CardLanguage.entries, language, { it.label }, onLanguageChange)
    Spacer(Modifier.height(14.dp))
    // The condition and language are remembered from the last add, so spell out what a
    // button will add rather than leaving it to the chips above.
    val copyDescription = "${condition.label} · ${language.label}"
    if (variants.size == 1) {
        DexButton(
            text = "Add · $copyDescription",
            onClick = { onAdd(variants.single()) },
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }
    Text(
        "Each button adds a $copyDescription copy",
        style = DexText.ScreenSmall,
        color = ScreenTextDim,
        modifier = Modifier.padding(bottom = 8.dp),
    )
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
                text = "+ ${variant.label}",
                onClick = { onAdd(variant) },
                style = if (variant == usual) DexButtonStyle.Primary else DexButtonStyle.Secondary,
            )
        }
    }
}
