package br.com.imoveisregla.realtor.feature.agenda

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.imoveisregla.core.designsystem.EmptyState
import br.com.imoveisregla.core.designsystem.ErrorState
import br.com.imoveisregla.core.designsystem.LoadingState
import br.com.imoveisregla.core.designsystem.Regla
import br.com.imoveisregla.core.designsystem.StatusPill
import br.com.imoveisregla.core.model.Showing
import br.com.imoveisregla.core.model.ShowingStatus
import br.com.imoveisregla.core.model.ShowingType
import br.com.imoveisregla.core.model.label
import br.com.imoveisregla.core.model.listingRef
import br.com.imoveisregla.core.model.maskPhoneBR
import br.com.imoveisregla.core.model.waToClient
import br.com.imoveisregla.realtor.LocalAppContainer
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen(onOpenListing: (Long) -> Unit) {
    val container = LocalAppContainer.current
    val vm = viewModel { AgendaViewModel(container.agenda) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(state.message) {
        val m = state.message ?: return@LaunchedEffect
        snackbar.showSnackbar(m)
        vm.consumeMessage()
    }

    Scaffold(containerColor = Regla.NavySoft, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Text(
                "Agenda",
                style = MaterialTheme.typography.headlineMedium,
                color = Regla.Ink,
                modifier = Modifier.padding(start = Regla.Gutter, end = Regla.Gutter, top = 16.dp),
            )
            Text(
                "Próximos 7 dias · ${state.all.count { it.status != ShowingStatus.CANCELLED }} visitas",
                style = MaterialTheme.typography.bodyMedium,
                color = Regla.Muted,
                modifier = Modifier.padding(horizontal = Regla.Gutter),
            )
            Spacer(Modifier.height(12.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = Regla.Gutter),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.days, key = { it.toString() }) { day ->
                    DayChip(
                        day = day,
                        today = state.today,
                        count = activeCount(state.byDay[day]),
                        selected = day == state.selected,
                        onClick = { vm.select(day) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = vm::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    state.loading -> LoadingState()
                    state.error != null && state.byDay.isEmpty() -> ErrorState(state.error!!, onRetry = { vm.load() })
                    state.selectedShowings.isEmpty() -> LazyColumn(Modifier.fillMaxSize()) {
                        item {
                            Box(Modifier.fillParentMaxSize()) {
                                EmptyState(
                                    "Nenhuma visita ${dayLabel(state.selected, state.today).let { if (it == "Hoje" || it == "Amanhã") it.lowercase() else "em $it" }}",
                                    "Visitas agendadas pelos clientes no app ou no site aparecem aqui.",
                                )
                            }
                        }
                    }
                    else -> LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = Regla.Gutter, end = Regla.Gutter, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.selectedShowings, key = { it.id }) { s ->
                            ShowingCard(
                                showing = s,
                                busy = s.id in state.busy,
                                onOpenListing = { onOpenListing(s.listingId) },
                                onAction = { target -> vm.setStatus(s.id, target) },
                                onWhatsApp = { phone ->
                                    val first = s.visitorName?.trim()?.substringBefore(' ').orEmpty()
                                    val title = s.listings?.title ?: listingRef(s.listingId)
                                    val text = "Olá${if (first.isNotBlank()) " $first" else ""}, aqui é da REGLA sobre a sua " +
                                        "visita ao imóvel $title (${dayLabel(state.selected, state.today).lowercase()} às ${showingTime(s)})"
                                    runCatching { uriHandler.openUri(waToClient(phone, text)) }
                                        .onFailure { vm.showMessage("Não foi possível abrir o WhatsApp") }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayChip(day: LocalDate, today: LocalDate, count: Int, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(Regla.RadiusControl)
    val bg = if (selected) Regla.Navy else Regla.Surface
    val fg = if (selected) Color.White else Regla.Ink
    val sub = if (selected) Color.White.copy(alpha = 0.8f) else Regla.Muted
    val top = when (day) {
        today -> "Hoje"
        today.plusDays(1) -> "Amanhã"
        else -> weekdayShort(day)
    }
    Column(
        Modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, if (selected) Regla.Navy else Regla.Line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("day-$day"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(top, style = MaterialTheme.typography.labelMedium, color = sub)
        Text("${day.dayOfMonth} ${monthShort(day)}", style = MaterialTheme.typography.titleMedium, color = fg)
        Text(
            when (count) {
                0 -> "—"
                1 -> "1 visita"
                else -> "$count visitas"
            },
            style = MaterialTheme.typography.labelMedium,
            color = if (count > 0 && !selected) Regla.Coral else sub,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShowingCard(
    showing: Showing,
    busy: Boolean,
    onOpenListing: () -> Unit,
    onAction: (ShowingStatus) -> Unit,
    onWhatsApp: (String) -> Unit,
) {
    Surface(
        color = Regla.Surface,
        shape = RoundedCornerShape(Regla.RadiusCard),
        modifier = Modifier.fillMaxWidth().testTag("showing-${showing.id}"),
    ) {
        Row(Modifier.padding(Regla.Gutter)) {
            // Time column (timeline)
            Column(Modifier.width(64.dp)) {
                Text(showingTime(showing), style = MaterialTheme.typography.titleMedium, color = Regla.Navy)
                Text("${showing.durationMinutes} min", style = MaterialTheme.typography.labelMedium, color = Regla.Muted)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        showing.listings?.title ?: "Imóvel ${listingRef(showing.listingId)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Regla.Ink,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f).clickable(onClick = onOpenListing),
                    )
                    Spacer(Modifier.width(8.dp))
                    val (fg, bg) = showingStatusColors(showing.status)
                    StatusPill(showing.status.label, fg, bg, Modifier.testTag("showing-status-${showing.id}"))
                }
                Text(
                    showingTimeRange(showing) + if (showing.type == ShowingType.OPEN_HOUSE) " · Open house" else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = Regla.Muted,
                )
                showing.visitorName?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = Regla.Ink)
                }
                val contact = listOfNotNull(
                    showing.visitorPhone?.takeIf { it.isNotBlank() }?.let { maskPhoneBR(it).ifBlank { it } },
                    showing.visitorEmail?.takeIf { it.isNotBlank() },
                ).joinToString(" · ")
                if (contact.isNotBlank()) Text(contact, style = MaterialTheme.typography.labelMedium, color = Regla.Muted)
                showing.notes?.takeIf { it.isNotBlank() }?.let {
                    Text("“$it”", style = MaterialTheme.typography.bodyMedium, color = Regla.Muted)
                }
                val actions = agendaActions(showing.status)
                val phone = showing.visitorPhone?.takeIf { it.isNotBlank() }
                if (actions.isNotEmpty() || phone != null) {
                    Spacer(Modifier.height(4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        actions.forEach { target ->
                            val danger = target == ShowingStatus.CANCELLED || target == ShowingStatus.NO_SHOW
                            OutlinedButton(
                                onClick = { onAction(target) },
                                enabled = !busy,
                                shape = RoundedCornerShape(Regla.RadiusControl),
                                modifier = Modifier.testTag("action-${showing.id}-${target.name}"),
                            ) {
                                Text(actionLabel(target), color = if (danger) Regla.Danger else Regla.Navy)
                            }
                        }
                        if (phone != null) {
                            TextButton(onClick = { onWhatsApp(phone) }) { Text("WhatsApp", color = Regla.Ok) }
                        }
                    }
                }
            }
        }
    }
}
