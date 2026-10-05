package br.com.imoveisregla.client.feature.visits

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.imoveisregla.client.LocalAppContainer
import br.com.imoveisregla.client.feature.visit.visitWhenLabel
import br.com.imoveisregla.core.designsystem.EmptyState
import br.com.imoveisregla.core.designsystem.ErrorState
import br.com.imoveisregla.core.designsystem.LoadingState
import br.com.imoveisregla.core.designsystem.Regla
import br.com.imoveisregla.core.designsystem.ReglaButton
import br.com.imoveisregla.core.designsystem.SectionTitle
import br.com.imoveisregla.core.designsystem.StatusPill
import br.com.imoveisregla.core.model.Showing
import br.com.imoveisregla.core.model.ShowingStatus
import br.com.imoveisregla.core.model.label
import br.com.imoveisregla.core.model.listingRef

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitsScreen(onOpenListing: (Long) -> Unit, onRequireLogin: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: VisitsViewModel = viewModel { VisitsViewModel(container) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmCancel by remember { mutableStateOf<Showing?>(null) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            vm.messageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Minhas visitas") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Regla.Surface),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = Regla.NavySoft,
    ) { padding ->
        val m = Modifier.padding(padding)
        when {
            !state.signedIn -> EmptyState(
                title = "Suas visitas ficam aqui",
                message = "Entre na sua conta para agendar e acompanhar visitas aos imóveis.",
                modifier = m,
                action = { ReglaButton("Entrar", onRequireLogin) },
            )
            state.loading -> LoadingState(m)
            state.error != null && state.upcoming.isEmpty() && state.past.isEmpty() ->
                ErrorState(state.error!!, onRetry = vm::retry, modifier = m)
            else -> PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = vm::refresh,
                modifier = m.fillMaxSize(),
            ) {
                val now = vm.now()
                LazyColumn(
                    Modifier.fillMaxSize().testTag("visits-list"),
                    contentPadding = PaddingValues(Regla.Gutter),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (state.upcoming.isEmpty() && state.past.isEmpty()) {
                        item {
                            EmptyState(
                                title = "Nenhuma visita agendada",
                                message = "Encontre um imóvel e toque em \"Agendar visita\" para conhecê-lo pessoalmente.",
                                modifier = Modifier.fillParentMaxSize(),
                            )
                        }
                    }
                    if (state.upcoming.isNotEmpty()) {
                        item(key = "h-up") { SectionTitle("Próximas") }
                        items(state.upcoming, key = { "u-${it.id}" }) { s ->
                            VisitCard(
                                s,
                                cancellable = canCancel(s, now),
                                cancelling = state.cancellingId == s.id,
                                onOpenListing = onOpenListing,
                                onCancel = { confirmCancel = s },
                            )
                        }
                    }
                    if (state.past.isNotEmpty()) {
                        item(key = "h-past") {
                            SectionTitle("Anteriores", Modifier.padding(top = if (state.upcoming.isEmpty()) 0.dp else 8.dp))
                        }
                        items(state.past, key = { "p-${it.id}" }) { s ->
                            VisitCard(
                                s, cancellable = false, cancelling = false,
                                onOpenListing = onOpenListing, onCancel = {},
                            )
                        }
                    }
                }
            }
        }
    }

    confirmCancel?.let { s ->
        AlertDialog(
            onDismissRequest = { confirmCancel = null },
            title = { Text("Cancelar visita?") },
            text = {
                Text("A visita de ${visitWhenLabel(s.startsAt)} em \"${titleOf(s)}\" será cancelada e o corretor será avisado.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmCancel = null
                    vm.cancel(s.id)
                }) { Text("Sim, cancelar", color = Regla.Danger) }
            },
            dismissButton = { TextButton(onClick = { confirmCancel = null }) { Text("Manter") } },
        )
    }
}

private fun titleOf(s: Showing): String = s.listings?.title ?: "Imóvel ${listingRef(s.listingId)}"

private fun statusColors(status: ShowingStatus): Pair<Color, Color> = when (status) {
    ShowingStatus.SCHEDULED -> Regla.Warn to Regla.WarnSoft
    ShowingStatus.CONFIRMED -> Regla.Ok to Regla.OkSoft
    ShowingStatus.ATTENDED -> Regla.Info to Regla.InfoSoft
    ShowingStatus.NO_SHOW -> Regla.Danger to Regla.DangerSoft
    ShowingStatus.CANCELLED -> Regla.Muted to Regla.Line
}

@Composable
private fun VisitCard(
    s: Showing,
    cancellable: Boolean,
    cancelling: Boolean,
    onOpenListing: (Long) -> Unit,
    onCancel: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(Regla.RadiusCard),
        color = Regla.Surface,
        border = BorderStroke(1.dp, Regla.Line),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    visitWhenLabel(s.startsAt),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (s.status == ShowingStatus.CANCELLED) Regla.Muted else Regla.Navy,
                    modifier = Modifier.weight(1f),
                )
                val (fg, bg) = statusColors(s.status)
                StatusPill(s.status.label, fg, bg)
            }
            Spacer(Modifier.height(6.dp))
            Column(Modifier.fillMaxWidth().clickable { onOpenListing(s.listingId) }.padding(vertical = 4.dp)) {
                Text(
                    titleOf(s),
                    style = MaterialTheme.typography.titleMedium,
                    color = Regla.Ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val place = listOfNotNull(
                    s.listings?.neighborhood?.takeIf { it.isNotBlank() },
                    s.listings?.city?.takeIf { it.isNotBlank() },
                ).joinToString(" · ")
                Text(
                    if (place.isBlank()) "Ref. ${listingRef(s.listingId)}" else "$place · Ref. ${listingRef(s.listingId)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Regla.Muted,
                )
            }
            if (cancellable) {
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    if (cancelling) {
                        CircularProgressIndicator(Modifier.height(18.dp), strokeWidth = 2.dp, color = Regla.Danger)
                    } else {
                        TextButton(onClick = onCancel) { Text("Cancelar visita", color = Regla.Danger) }
                    }
                }
            }
        }
    }
}
