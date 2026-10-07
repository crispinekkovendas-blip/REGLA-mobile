package br.com.imoveisregla.client.feature.proposals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PriorityHigh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.imoveisregla.client.LocalAppContainer
import br.com.imoveisregla.core.designsystem.ButtonKind
import br.com.imoveisregla.core.designsystem.EmptyState
import br.com.imoveisregla.core.designsystem.ErrorState
import br.com.imoveisregla.core.designsystem.LoadingState
import br.com.imoveisregla.core.designsystem.Regla
import br.com.imoveisregla.core.designsystem.ReglaButton
import br.com.imoveisregla.core.designsystem.StatusPill
import br.com.imoveisregla.core.model.Application
import br.com.imoveisregla.core.model.ApplicationStatus
import br.com.imoveisregla.core.model.Currency
import br.com.imoveisregla.core.model.Party
import br.com.imoveisregla.core.designsystem.pillColors
import br.com.imoveisregla.core.model.formatPrice
import br.com.imoveisregla.core.model.label
import br.com.imoveisregla.core.model.listingRef

/** Pill colors (foreground, background) for each proposal status (shared design-system palette). */
fun applicationStatusColors(status: ApplicationStatus): Pair<Color, Color> =
    status.pillColors().let { it.foreground to it.background }

const val YOUR_TURN_LABEL = "Sua vez"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProposalsScreen(onOpenListing: (Long) -> Unit, onRequireLogin: () -> Unit, onOpenProposal: (Long) -> Unit) {
    val container = LocalAppContainer.current
    val vm: ProposalsViewModel = viewModel { ProposalsViewModel(container.auth, container.applications) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        val msg = state.message
        if (msg != null) {
            snackbar.showSnackbar(msg)
            vm.messageShown()
        }
    }

    Scaffold(
        containerColor = Regla.NavySoft,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                !state.signedIn -> EmptyState(
                    title = "Suas propostas",
                    message = "Entre na sua conta para enviar e acompanhar propostas de aluguel e compra.",
                    action = { ReglaButton("Entrar", onRequireLogin) },
                )
                state.loading -> LoadingState()
                state.error != null -> ErrorState(state.error ?: "", onRetry = vm::retry)
                else -> PullToRefreshBox(
                    isRefreshing = state.refreshing,
                    onRefresh = vm::refresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = Regla.Gutter, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item {
                            Text("Propostas", style = MaterialTheme.typography.headlineMedium, color = Regla.Ink)
                        }
                        if (state.items.isEmpty()) {
                            item {
                                Box(Modifier.fillParentMaxSize()) {
                                    EmptyState(
                                        title = "Nenhuma proposta ainda",
                                        message = "Encontrou um imóvel? Toque em \"Fazer proposta\" no anúncio e acompanhe tudo por aqui.",
                                    )
                                }
                            }
                        } else {
                            items(state.items, key = { it.id }) { app ->
                                ProposalCard(
                                    app = app,
                                    withdrawing = state.withdrawingId == app.id,
                                    onOpenListing = { onOpenListing(app.listingId) },
                                    onOpen = { onOpenProposal(app.id) },
                                    onWithdraw = { vm.askWithdraw(app) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val pending = state.confirmWithdraw
    if (pending != null) {
        AlertDialog(
            onDismissRequest = vm::dismissWithdraw,
            title = { Text("Cancelar proposta?") },
            text = {
                Text(
                    "A proposta de ${formatPrice(pending.currentPrice, pending.listings?.currency ?: Currency.BRL)} " +
                        "para \"${proposalTitle(pending)}\" será cancelada. Essa ação não pode ser desfeita.",
                )
            },
            confirmButton = {
                TextButton(onClick = vm::confirmWithdraw) { Text("Cancelar proposta", color = Regla.Danger) }
            },
            dismissButton = { TextButton(onClick = vm::dismissWithdraw) { Text("Manter") } },
        )
    }
}

private fun proposalTitle(app: Application): String = app.listings?.title ?: "Imóvel ${listingRef(app.listingId)}"

@Composable
private fun ProposalCard(
    app: Application,
    withdrawing: Boolean,
    onOpenListing: () -> Unit,
    onOpen: () -> Unit,
    onWithdraw: () -> Unit,
) {
    val yourTurn = app.isTurnOf(Party.CLIENT)
    val currency = app.listings?.currency ?: Currency.BRL
    val (fg, bg) = applicationStatusColors(app.status)
    Card(
        shape = RoundedCornerShape(Regla.RadiusCard),
        colors = CardDefaults.cardColors(containerColor = Regla.Surface),
        border = BorderStroke(if (yourTurn) 2.dp else 1.dp, if (yourTurn) Regla.Coral else Regla.Line),
        modifier = Modifier.fillMaxWidth().testTag("proposal-${app.id}").clickable(onClick = onOpen),
    ) {
        Column(Modifier.padding(16.dp)) {
            if (yourTurn) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(YOUR_TURN_LABEL, color = Color.White, background = Regla.Coral)
                    Spacer(Modifier.width(8.dp))
                    Text(clientHeadline(app), style = MaterialTheme.typography.labelLarge, color = Regla.CoralInk)
                }
                Spacer(Modifier.height(10.dp))
            }
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f).clickable(onClick = onOpenListing)) {
                    Text(
                        proposalTitle(app), style = MaterialTheme.typography.titleMedium, color = Regla.Ink,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        listingRef(app.listingId) + (app.listings?.neighborhood?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
                        style = MaterialTheme.typography.labelMedium, color = Regla.Muted,
                    )
                }
                Spacer(Modifier.width(8.dp))
                StatusPill(app.status.label, color = fg, background = bg)
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    val priceLabel = when {
                        app.agreedPrice != null -> "Valor acordado"
                        app.latestOffer?.author == Party.REALTOR -> "Contraproposta"
                        else -> "Sua oferta"
                    }
                    Text("$priceLabel · ${app.intent.label}", style = MaterialTheme.typography.labelMedium, color = Regla.Muted)
                    Text(formatPrice(app.currentPrice, currency), style = MaterialTheme.typography.titleLarge, color = Regla.Navy)
                }
                val created = formatProposalDate(app.createdAt)
                if (created.isNotEmpty()) {
                    Text("Enviada em $created", style = MaterialTheme.typography.labelMedium, color = Regla.Muted)
                }
            }
            Spacer(Modifier.height(14.dp))
            StatusTimeline(proposalTimeline(app.status))

            val note = app.reviewerNote
            if (!note.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Regla.RadiusControl))
                        .background(Regla.NavySoft)
                        .padding(12.dp),
                ) {
                    Text("Mensagem do corretor", style = MaterialTheme.typography.labelMedium, color = Regla.Muted)
                    Spacer(Modifier.height(2.dp))
                    Text(note, style = MaterialTheme.typography.bodyMedium, color = Regla.Ink)
                }
            }

            if (yourTurn) {
                Spacer(Modifier.height(12.dp))
                ReglaButton(
                    text = if (app.status.isDocumentsPhase) "Enviar documentos" else "Responder",
                    onClick = onOpen,
                    kind = ButtonKind.Accent,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (app.status.isOpen) {
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onWithdraw, enabled = !withdrawing, modifier = Modifier.align(Alignment.End)) {
                    Text(if (withdrawing) "Cancelando…" else "Cancelar proposta", color = Regla.Danger)
                }
            }
        }
    }
}

@Composable
private fun StatusTimeline(steps: List<TimelineStep>) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        steps.forEachIndexed { i, step ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TimelineConnector(visible = i > 0, active = step.state != TimelineState.PENDING, modifier = Modifier.weight(1f))
                    TimelineDot(step.state)
                    val nextActive = steps.getOrNull(i + 1)?.let { it.state != TimelineState.PENDING } ?: false
                    TimelineConnector(visible = i < steps.lastIndex, active = nextActive, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    step.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (step.state == TimelineState.PENDING) Regla.Muted else Regla.Ink,
                    fontWeight = if (step.state == TimelineState.CURRENT || step.state == TimelineState.WARNING) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun TimelineConnector(visible: Boolean, active: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(2.dp)
            .background(if (!visible) Color.Transparent else if (active) Regla.Ok else Regla.Line),
    )
}

@Composable
private fun TimelineDot(state: TimelineState) {
    val (bg, icon) = when (state) {
        TimelineState.DONE -> Regla.Ok to Icons.Outlined.Check
        TimelineState.CURRENT -> Regla.Navy to null
        TimelineState.WARNING -> Regla.Warn to Icons.Outlined.PriorityHigh
        TimelineState.FAILED -> Regla.Danger to Icons.Outlined.Close
        TimelineState.PENDING -> Regla.Line to null
    }
    Box(Modifier.size(20.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
        } else if (state == TimelineState.CURRENT) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(Color.White))
        }
    }
}
