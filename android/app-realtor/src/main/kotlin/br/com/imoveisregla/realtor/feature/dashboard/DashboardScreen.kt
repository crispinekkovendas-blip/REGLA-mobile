package br.com.imoveisregla.realtor.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.imoveisregla.core.designsystem.ErrorState
import br.com.imoveisregla.core.designsystem.LoadingState
import br.com.imoveisregla.core.designsystem.Regla
import br.com.imoveisregla.core.designsystem.pillColors
import br.com.imoveisregla.core.designsystem.SectionTitle
import br.com.imoveisregla.core.designsystem.StatusPill
import br.com.imoveisregla.core.model.Application
import br.com.imoveisregla.core.model.ApplicationStatus
import br.com.imoveisregla.core.model.Currency
import br.com.imoveisregla.core.model.Inquiry
import br.com.imoveisregla.core.model.Showing
import br.com.imoveisregla.core.model.formatPrice
import br.com.imoveisregla.core.model.label
import br.com.imoveisregla.realtor.LocalAppContainer
import java.time.OffsetDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onOpenLead: (Long) -> Unit, onOpenApplication: (Long) -> Unit, onOpenAgenda: () -> Unit) {
    val container = LocalAppContainer.current
    val vm = viewModel { DashboardViewModel(container) }
    val state by vm.state.collectAsStateWithLifecycle()
    val now = OffsetDateTime.now(DashboardViewModel.BRT)

    Column(Modifier.fillMaxSize().background(Regla.NavySoft)) {
        DashboardHeader(
            greeting = greetingFor(now.hour),
            date = dashboardDate(now),
            email = state.email,
            live = vm.isLive,
            onSignOut = vm::signOut,
        )
        when {
            state.loading && state.stats == null -> LoadingState()
            state.stats == null && state.error != null -> ErrorState(state.error!!, onRetry = vm::refresh)
            else -> PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = vm::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(Regla.Gutter),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (state.error != null) {
                        item {
                            Text(
                                state.error!!,
                                color = Regla.Danger,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(Regla.RadiusControl))
                                    .background(Regla.DangerSoft).padding(12.dp),
                            )
                        }
                    }
                    item { StatsGrid(state) }
                    item {
                        DashSection("Próximas visitas", actionLabel = "Ver agenda", onAction = onOpenAgenda) {
                            if (state.upcomingVisits.isEmpty()) {
                                EmptyRow("Nenhuma visita nos próximos 7 dias")
                            } else {
                                state.upcomingVisits.forEachIndexed { i, v ->
                                    if (i > 0) HorizontalDivider(color = Regla.Line)
                                    VisitRow(v, now, onClick = onOpenAgenda)
                                }
                            }
                        }
                    }
                    item {
                        DashSection("Propostas recentes") {
                            if (state.recentProposals.isEmpty()) {
                                EmptyRow("Nenhuma proposta recebida ainda")
                            } else {
                                state.recentProposals.forEachIndexed { i, a ->
                                    if (i > 0) HorizontalDivider(color = Regla.Line)
                                    ProposalRow(a, onClick = { onOpenApplication(a.id) })
                                }
                            }
                        }
                    }
                    item {
                        DashSection("Leads novos") {
                            if (state.newLeads.isEmpty()) {
                                EmptyRow("Caixa de entrada vazia")
                            } else {
                                state.newLeads.forEachIndexed { i, l ->
                                    if (i > 0) HorizontalDivider(color = Regla.Line)
                                    LeadRow(l, onClick = { onOpenLead(l.id) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardHeader(greeting: String, date: String, email: String?, live: Boolean, onSignOut: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Regla.Navy)
            .padding(start = Regla.Gutter, end = 4.dp, top = 16.dp, bottom = 20.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            LiveIndicator(live)
            Spacer(Modifier.height(10.dp))
            Text(greeting, color = Color.White, style = MaterialTheme.typography.headlineMedium)
            Text(date, color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodyMedium)
            if (!email.isNullOrBlank()) {
                Text(
                    email, color = Color.White.copy(alpha = 0.55f), style = MaterialTheme.typography.labelMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = onSignOut) {
            Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = "Sair", tint = Color.White)
        }
    }
}

@Composable
private fun LiveIndicator(live: Boolean) {
    val dot = if (live) Regla.Ok else Regla.Warn
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).background(dot, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(if (live) "Ao vivo" else "Demo", color = Color.White, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun StatsGrid(state: DashboardUiState) {
    val s = state.stats
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("Novos leads", s?.newLeads, Icons.Outlined.PersonAdd, Regla.Coral, Regla.CoralSoft, Modifier.weight(1f))
            StatTile("Propostas pendentes", s?.pendingApplications, Icons.Outlined.Description, Regla.Warn, Regla.WarnSoft, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("Visitas hoje", s?.visitsToday, Icons.Outlined.CalendarMonth, Regla.Info, Regla.InfoSoft, Modifier.weight(1f))
            StatTile("Imóveis ativos", s?.liveListings, Icons.Outlined.HomeWork, Regla.Ok, Regla.OkSoft, Modifier.weight(1f))
        }
    }
}

@Composable
internal fun StatTile(label: String, value: Int?, icon: ImageVector, tint: Color, tintSoft: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(Regla.RadiusCard))
            .background(Regla.Surface)
            .border(1.dp, Regla.Line, RoundedCornerShape(Regla.RadiusCard))
            .padding(14.dp),
    ) {
        Box(Modifier.size(34.dp).background(tintSoft, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(value?.toString() ?: "–", style = MaterialTheme.typography.headlineMedium, color = Regla.Ink)
        Text(label, style = MaterialTheme.typography.labelMedium, color = Regla.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun DashSection(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(title, Modifier.weight(1f))
            if (actionLabel != null && onAction != null) {
                TextButton(onClick = onAction) { Text(actionLabel, color = Regla.Coral) }
            }
        }
        Spacer(Modifier.height(6.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Regla.RadiusCard))
                .background(Regla.Surface)
                .border(1.dp, Regla.Line, RoundedCornerShape(Regla.RadiusCard)),
        ) { content() }
    }
}

@Composable
private fun EmptyRow(text: String) {
    Text(text, color = Regla.Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
}

@Composable
private fun VisitRow(v: Showing, now: OffsetDateTime, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).background(Regla.InfoSoft, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = Regla.Info, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(visitWhen(v.startsAt, now), style = MaterialTheme.typography.titleMedium, color = Regla.Ink)
            Text(
                listOfNotNull(v.visitorName, v.listings?.title).joinToString(" · ").ifBlank { "Visita" },
                style = MaterialTheme.typography.bodyMedium, color = Regla.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(v.status.label, style = MaterialTheme.typography.labelMedium, color = Regla.Muted)
    }
}

private fun ApplicationStatus.colors(): Pair<Color, Color> =
    pillColors().let { it.foreground to it.background }

@Composable
private fun ProposalRow(a: Application, onClick: () -> Unit) {
    val (fg, bg) = a.status.colors()
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                a.profile?.fullName ?: "Cliente", style = MaterialTheme.typography.titleMedium, color = Regla.Ink,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                a.listings?.title ?: "Imóvel #${a.listingId}", style = MaterialTheme.typography.bodyMedium, color = Regla.Muted,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                formatPrice(a.offeredPrice, a.listings?.currency ?: Currency.BRL) + " · " + a.intent.label,
                style = MaterialTheme.typography.bodyMedium, color = Regla.Navy, fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.width(8.dp))
        StatusPill(a.status.label, color = fg, background = bg)
    }
}

@Composable
private fun LeadRow(l: Inquiry, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp), contentAlignment = Alignment.Center) {
            if (!l.read) Box(Modifier.size(8.dp).background(Regla.Coral, CircleShape))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                l.name, style = MaterialTheme.typography.titleMedium, color = Regla.Ink,
                fontWeight = if (l.read) FontWeight.Normal else FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(l.message, style = MaterialTheme.typography.bodyMedium, color = Regla.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                l.listings?.title ?: "Contato geral", style = MaterialTheme.typography.labelMedium, color = Regla.Muted,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
