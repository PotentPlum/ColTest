package com.potentplum.dextracker.ui.value

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.potentplum.dextracker.domain.CardVariant
import com.potentplum.dextracker.domain.Valuation
import com.potentplum.dextracker.domain.ValuedCopy
import com.potentplum.dextracker.ui.components.DexInset
import com.potentplum.dextracker.ui.components.DexScaffold
import com.potentplum.dextracker.ui.components.DexScreen
import com.potentplum.dextracker.ui.components.DexTabBar
import com.potentplum.dextracker.ui.components.ScreenMessage
import com.potentplum.dextracker.ui.components.TcgCardImage
import com.potentplum.dextracker.ui.components.TopTab
import com.potentplum.dextracker.ui.components.euro
import com.potentplum.dextracker.ui.theme.DexText
import com.potentplum.dextracker.ui.theme.ScreenGlow
import com.potentplum.dextracker.ui.theme.ScreenLine
import com.potentplum.dextracker.ui.theme.ScreenText
import com.potentplum.dextracker.ui.theme.ScreenTextDim
import java.text.DateFormat
import java.util.Date

@Composable
fun ValueScreen(
    viewModel: ValueViewModel,
    onSelectTab: (TopTab) -> Unit,
    onOpenPokemon: (Int) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ValueContent(state, onSelectTab, onOpenPokemon, onRefresh = viewModel::refreshPrices)
}

@Composable
fun ValueContent(
    state: ValueUiState,
    onSelectTab: (TopTab) -> Unit,
    onOpenPokemon: (Int) -> Unit,
    onRefresh: () -> Unit,
) {
    DexScaffold(
        title = "Collection value",
        bottomBar = { DexTabBar(TopTab.VALUE, onSelectTab) },
    ) {
        TotalScreen(state, onRefresh)
        Spacer(Modifier.height(10.dp))
        DexInset(
            Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            DexScreen(Modifier.fillMaxSize(), contentPadding = 0.dp) {
                val notable = state.valuation.notable
                when {
                    state.loading -> Unit
                    state.valuation.totalCopies == 0 -> ScreenMessage(
                        title = "No cards yet",
                        body = "Add cards to your Pokémon and their value shows up here.",
                        modifier = Modifier.align(Alignment.Center),
                    )
                    notable.isEmpty() -> ScreenMessage(
                        title = "Nothing over ${euro(Valuation.NOTABLE_OVER_EUR)}",
                        body = "Cards worth more than ${euro(Valuation.NOTABLE_OVER_EUR)} each are listed here.",
                        modifier = Modifier.align(Alignment.Center),
                    )
                    else -> NotableList(notable, onOpenPokemon)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun TotalScreen(state: ValueUiState, onRefresh: () -> Unit) {
    val valuation = state.valuation
    DexScreen(Modifier.fillMaxWidth(), showGrid = false, contentPadding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) {},
            ) {
                Text("TOTAL WORTH", style = DexText.ScreenSmall, color = ScreenTextDim)
                Text(euro(valuation.total), style = DexText.ScreenLarge.copy(fontSize = 24.sp, lineHeight = 30.sp), color = ScreenGlow)
                Text(
                    buildString {
                        append(copies(valuation.totalCopies))
                        if (valuation.pricedCopies < valuation.totalCopies) append(" · ${valuation.pricedCopies} priced")
                    },
                    style = DexText.ScreenSmall,
                    color = ScreenTextDim,
                )
                Text(statusLine(state), style = DexText.ScreenSmall.copy(fontSize = 10.sp), color = ScreenTextDim)
            }
            if (valuation.totalCopies > 0) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .clickable(
                            enabled = state.online && !state.refreshing,
                            onClickLabel = "Refresh prices",
                            role = Role.Button,
                            onClick = onRefresh,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.refreshing) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = ScreenGlow, strokeWidth = 2.dp)
                    } else {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = "Refresh prices",
                            tint = if (state.online) ScreenGlow else ScreenLine,
                        )
                    }
                }
            }
        }
    }
}

private fun statusLine(state: ValueUiState): String {
    val source = "Cardmarket trend prices"
    val updated = state.pricesUpdatedAt?.let { "updated ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it))}" }
    return when {
        state.refreshing -> "Updating prices…"
        !state.online -> listOfNotNull("Offline", updated).joinToString(" · ")
        state.refreshFailed -> "Couldn't update prices · " + (updated ?: "try again later")
        else -> listOfNotNull(source, updated).joinToString(" · ")
    }
}

@Composable
private fun NotableList(entries: List<ValuedCopy>, onOpenPokemon: (Int) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(vertical = 6.dp), modifier = Modifier.fillMaxSize()) {
        item {
            Text(
                "WORTH OVER ${euro(Valuation.NOTABLE_OVER_EUR).uppercase()}",
                style = DexText.ScreenSmall,
                color = ScreenTextDim,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
        items(entries, key = { "${it.cardId}|${it.variant}|${it.condition}|${it.language}" }) { entry ->
            NotableRow(entry, onClick = { onOpenPokemon(entry.dex) })
            HorizontalDivider(color = ScreenLine.copy(alpha = 0.6f), modifier = Modifier.padding(horizontal = 14.dp))
        }
    }
}

@Composable
private fun NotableRow(entry: ValuedCopy, onClick: () -> Unit) {
    val printing = "${entry.setName} #${entry.number}"
    val copy = listOfNotNull(
        entry.variant.takeIf { it != CardVariant.UNSPECIFIED }?.label,
        entry.condition.code,
        entry.language.code,
    ).joinToString(" · ")
    val details = "$printing, $copy"
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Open ${entry.name}", onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "${entry.name}, $details, ${euro(entry.unitValue ?: 0.0)} each" +
                    if (entry.quantity > 1) ", ${entry.quantity} copies" else ""
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TcgCardImage(
            url = entry.imageSmall,
            modifier = Modifier
                .width(40.dp)
                .aspectRatio(0.716f),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.name, style = DexText.Screen, color = ScreenText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(printing, style = DexText.ScreenSmall, color = ScreenTextDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(copy, style = DexText.ScreenSmall, color = ScreenTextDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center) {
            Text(euro(entry.unitValue ?: 0.0), style = DexText.Screen, color = ScreenText)
            if (entry.quantity > 1) {
                Text("×${entry.quantity}", style = DexText.ScreenSmall, color = ScreenTextDim)
            }
        }
    }
}

private fun copies(count: Int) = if (count == 1) "1 card" else "$count cards"
