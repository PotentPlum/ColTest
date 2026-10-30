package com.potentplum.dextracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.potentplum.dextracker.ui.theme.Bezel
import com.potentplum.dextracker.ui.theme.DexText
import com.potentplum.dextracker.ui.theme.DexYellow
import com.potentplum.dextracker.ui.theme.DexYellowDark
import com.potentplum.dextracker.ui.theme.Ink
import com.potentplum.dextracker.ui.theme.Screen
import com.potentplum.dextracker.ui.theme.ScreenDeep
import com.potentplum.dextracker.ui.theme.ScreenGlow
import com.potentplum.dextracker.ui.theme.ScreenLine
import com.potentplum.dextracker.ui.theme.ScreenText
import com.potentplum.dextracker.ui.theme.ScreenTextDim

/**
 * Search box drawn as a small screen. [onSearch] runs when the keyboard's search key is pressed,
 * and [onFocusChange] reports when the keyboard opens for it, so screens can make room.
 *
 * [query] must be updated in the same frame as [onQueryChange] (plain Compose state, not a
 * StateFlow): a text field that's handed back a stale value drops or repeats keystrokes.
 */
@Composable
fun DexSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onSearch: () -> Unit = {},
    onFocusChange: (Boolean) -> Unit = {},
) {
    val focus = LocalFocusManager.current
    DexScreen(modifier.fillMaxWidth(), contentPadding = 0.dp, showGrid = false, shape = RoundedCornerShape(12.dp)) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = DexText.Screen,
            cursorBrush = SolidColor(ScreenGlow),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    focus.clearFocus()
                    onSearch()
                },
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { onFocusChange(it.isFocused) }
                .semantics { this.contentDescription = contentDescription },
            // The whole box is the text field, so a tap anywhere in it starts typing.
            decorationBox = { textField ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .padding(start = 10.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = ScreenTextDim)
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(placeholder, style = DexText.Screen, color = ScreenTextDim, maxLines = 1)
                        }
                        textField()
                    }
                    if (query.isNotEmpty()) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Clear search",
                            tint = ScreenTextDim,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { onQueryChange("") }
                                .padding(10.dp),
                        )
                    }
                }
            },
        )
    }
}

/** Small dark-screen button used for filter controls. */
@Composable
fun ScreenButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(Bezel)
            .padding(3.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Screen, ScreenDeep)))
            .border(1.dp, ScreenLine, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** A row of mutually exclusive options set into one bezel; the selected one lights up yellow. */
@Composable
fun <T> DexSegmentedControl(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val outer = RoundedCornerShape(12.dp)
    val inner = RoundedCornerShape(9.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(outer)
            .background(Bezel)
            .padding(3.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(inner)
                    .background(
                        if (isSelected) Brush.verticalGradient(listOf(DexYellow, DexYellow, DexYellowDark))
                        else Brush.verticalGradient(listOf(Screen, ScreenDeep)),
                    )
                    .semantics { this.selected = isSelected }
                    .clickable(role = Role.Tab) { onSelect(option) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(option).uppercase(),
                    style = if (isSelected) DexText.Label.copy(fontSize = 12.sp) else DexText.ScreenSmall.copy(fontSize = 12.sp),
                    color = if (isSelected) Ink else ScreenText,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}

/** Compact selectable chip for picking one value, e.g. a card's condition or language. */
@Composable
fun DexChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier
            .heightIn(min = 36.dp)
            .clip(shape)
            .background(if (selected) DexYellow else ScreenDeep)
            .border(1.dp, if (selected) DexYellowDark else ScreenLine, shape)
            .semantics {
                this.selected = selected
                if (contentDescription != null) this.contentDescription = contentDescription
            }
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = if (selected) DexText.Label.copy(fontSize = 13.sp) else DexText.ScreenSmall.copy(fontSize = 13.sp),
            color = if (selected) Ink else ScreenText,
        )
    }
}
