package br.com.imoveisregla.realtor.feature.leads

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.imoveisregla.core.designsystem.EmptyState
import br.com.imoveisregla.core.designsystem.ErrorState
import br.com.imoveisregla.core.designsystem.LoadingState
import br.com.imoveisregla.core.designsystem.Regla
import br.com.imoveisregla.core.designsystem.SectionTitle
import br.com.imoveisregla.core.model.Inquiry
import br.com.imoveisregla.core.model.InquiryStage
import br.com.imoveisregla.core.model.LeadNote
import br.com.imoveisregla.core.model.Priority
import br.com.imoveisregla.core.model.label
import br.com.imoveisregla.core.model.listingRef
import br.com.imoveisregla.core.model.waToClient
import br.com.imoveisregla.realtor.LocalAppContainer
import java.time.OffsetDateTime

// ───────────────────────────── Leads list ─────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeadsScreen(onOpenLead: (Long) -> Unit) {
    val container = LocalAppContainer.current
    val vm = viewModel { LeadsViewModel(container.leads) }
    val state by vm.state.collectAsStateWithLifecycle()
    val now = remember(state.all) { OffsetDateTime.now() }

    Column(Modifier.fillMaxSize()) {
        Text(
            "Leads",
            style = MaterialTheme.typography.headlineMedium,
            color = Regla.Ink,
            modifier = Modifier.padding(start = Regla.Gutter, end = Regla.Gutter, top = 16.dp, bottom = 8.dp),
        )
        StageChips(
            selected = state.stage,
            counts = state.counts,
            total = state.all.size,
            onSelect = vm::setStage,
        )
        OutlinedTextField(
            value = state.query,
            onValueChange = vm::setQuery,
            placeholder = { Text("Buscar por nome, e-mail ou mensagem") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(Regla.RadiusControl),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier.fillMaxWidth().padding(horizontal = Regla.Gutter, vertical = 8.dp),
        )
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = vm::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            val error = state.error
            when {
                state.loading -> LoadingState()
                error != null && state.all.isEmpty() -> ErrorState(error, onRetry = { vm.load() })
                else -> {
                    val visible = state.visible
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = Regla.Gutter, end = Regla.Gutter, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (visible.isEmpty()) {
                            item {
                                val (title, msg) = emptyCopy(state.stage, state.query)
                                EmptyState(title, msg, modifier = Modifier.fillParentMaxSize())
                            }
                        } else {
                            items(visible, key = { it.id }) { lead ->
                                LeadRow(lead, now, onClick = { onOpenLead(lead.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun emptyCopy(stage: InquiryStage?, query: String): Pair<String, String> = when {
    query.isNotBlank() -> "Nenhum resultado" to "Nenhum lead corresponde a \"${query.trim()}\"."
    stage == null -> "Nenhum lead ainda" to "Quando alguém entrar em contato pelo site ou pelo app, o lead aparece aqui."
    else -> "Nenhum lead em \"${stage.label}\"" to "Os leads desta etapa aparecerão aqui."
}

@Composable
private fun StageChips(
    selected: InquiryStage?,
    counts: Map<InquiryStage, Int>,
    total: Int,
    onSelect: (InquiryStage?) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = Regla.Gutter),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CountChip("Todos", total, selected == null) { onSelect(null) }
        InquiryStage.entries.forEach { stage ->
            CountChip(stage.label, counts[stage] ?: 0, selected == stage) { onSelect(stage) }
        }
    }
}

@Composable
internal fun CountChip(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text("$label · $count") },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Regla.Navy,
            selectedLabelColor = Color.White,
        ),
    )
}

@Composable
private fun LeadRow(lead: Inquiry, now: OffsetDateTime, onClick: () -> Unit) {
    val listing = lead.listings
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(Regla.RadiusCard),
        colors = CardDefaults.cardColors(containerColor = Regla.Surface),
        border = BorderStroke(1.dp, Regla.Line),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UnreadDot(!lead.read)
                Spacer(Modifier.width(8.dp))
                Text(
                    lead.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (lead.read) FontWeight.SemiBold else FontWeight.Bold,
                    color = Regla.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    relativeTime(lead.lastActivityAt.ifBlank { lead.createdAt }, now),
                    style = MaterialTheme.typography.labelMedium,
                    color = Regla.Muted,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                lead.message,
                style = MaterialTheme.typography.bodyMedium,
                color = Regla.Muted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (listing != null) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Home, contentDescription = null, tint = Regla.Muted, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        listing.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = Regla.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StagePill(lead.stage)
                PriorityBadge(lead.priority)
            }
        }
    }
}

// ───────────────────────────── Lead detail ─────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeadDetailScreen(leadId: Long, onBack: () -> Unit, onOpenListing: (Long) -> Unit) {
    val container = LocalAppContainer.current
    val vm = viewModel(key = "lead-$leadId") { LeadDetailViewModel(leadId, container.leads, container.agenda) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(state.message) {
        val msg = state.message
        if (msg != null) {
            snackbar.showSnackbar(msg)
            vm.messageShown()
        }
    }

    fun open(uri: String) {
        try {
            uriHandler.openUri(uri)
        } catch (e: Exception) {
            vm.showMessage("Nenhum app disponível para abrir este link")
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Lead") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Regla.Surface),
            )
        },
    ) { padding ->
        val lead = state.lead
        val error = state.error
        Column(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.loading && lead == null -> LoadingState()
                lead == null -> ErrorState(error ?: "Lead não encontrado", onRetry = { vm.load() })
                else -> LeadDetailContent(
                    lead = lead,
                    state = state,
                    onEmail = { open("mailto:${lead.email}") },
                    onWhatsApp = { phone ->
                        val first = lead.name.trim().substringBefore(' ')
                        open(waToClient(phone, "Olá, $first! Aqui é da REGLA Imóveis, sobre o seu contato."))
                    },
                    onOpenListing = onOpenListing,
                    onStage = vm::setStage,
                    onPriority = vm::setPriority,
                    onDraft = vm::setNoteDraft,
                    onSendNote = vm::addNote,
                )
            }
        }
    }
}

@Composable
private fun LeadDetailContent(
    lead: Inquiry,
    state: LeadDetailUiState,
    onEmail: () -> Unit,
    onWhatsApp: (String) -> Unit,
    onOpenListing: (Long) -> Unit,
    onStage: (InquiryStage) -> Unit,
    onPriority: (Priority) -> Unit,
    onDraft: (String) -> Unit,
    onSendNote: () -> Unit,
) {
    val now = remember(state.notes) { OffsetDateTime.now() }
    val phone = state.phone
    val propertyId = lead.propertyId
    val listing = lead.listings

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Regla.Gutter),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InitialsAvatar(lead.name)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(lead.name, style = MaterialTheme.typography.titleLarge, color = Regla.Ink)
                    Text(lead.email, style = MaterialTheme.typography.bodyMedium, color = Regla.Muted)
                    val since = relativeTime(lead.createdAt, now)
                    if (since.isNotEmpty()) {
                        Text("Contato $since", style = MaterialTheme.typography.labelMedium, color = Regla.Muted)
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onEmail, modifier = Modifier.weight(1f).height(48.dp)) {
                    Icon(Icons.Outlined.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("E-mail")
                }
                OutlinedButton(
                    onClick = { if (phone != null) onWhatsApp(phone) },
                    enabled = phone != null,
                    modifier = Modifier.weight(1f).height(48.dp),
                ) {
                    Icon(Icons.Outlined.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("WhatsApp")
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (phone != null) "Telefone: $phone (da visita agendada)"
                else "Sem telefone: este lead ainda não agendou visita com telefone",
                style = MaterialTheme.typography.labelMedium,
                color = Regla.Muted,
            )
        }
        item {
            InfoCard {
                SectionTitle("Mensagem")
                Spacer(Modifier.height(6.dp))
                Text(lead.message, style = MaterialTheme.typography.bodyLarge, color = Regla.Ink)
                val intent = lead.intent
                val region = lead.region
                val extra = listOfNotNull(
                    intent?.let { if (it == "rent") "Quer alugar" else if (it == "buy") "Quer comprar" else it },
                    region?.takeIf { it.isNotBlank() },
                ).joinToString(" · ")
                if (extra.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(extra, style = MaterialTheme.typography.labelMedium, color = Regla.Muted)
                }
            }
        }
        if (propertyId != null) {
            item {
                InfoCard(onClick = { onOpenListing(propertyId) }) {
                    Text("Imóvel de interesse", style = MaterialTheme.typography.labelMedium, color = Regla.Muted)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        listing?.title ?: "Imóvel ${listingRef(propertyId)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Regla.Ink,
                    )
                    if (listing != null) {
                        Text(
                            listOf(listingRef(propertyId), listing.neighborhood, refPrice(listing))
                                .filter { it.isNotBlank() }.joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Regla.Muted,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Ver imóvel ›", style = MaterialTheme.typography.labelLarge, color = Regla.Coral)
                }
            }
        }
        item {
            SectionTitle("Etapa")
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                InquiryStage.entries.forEach { s ->
                    SelectChip(s.label, selected = lead.stage == s, enabled = !state.saving) { onStage(s) }
                }
            }
        }
        item {
            SectionTitle("Prioridade")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Priority.entries.reversed().forEach { p ->
                    SelectChip(p.label, selected = lead.priority == p, enabled = !state.saving) { onPriority(p) }
                }
            }
        }
        item {
            SectionTitle("Notas")
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = state.noteDraft,
                    onValueChange = onDraft,
                    placeholder = { Text("Adicionar nota") },
                    shape = RoundedCornerShape(Regla.RadiusControl),
                    maxLines = 4,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                if (state.sendingNote) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = Regla.Coral)
                } else {
                    IconButton(onClick = onSendNote, enabled = state.noteDraft.isNotBlank()) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar nota", tint = Regla.Coral)
                    }
                }
            }
        }
        if (state.notes.isEmpty()) {
            item { Text("Nenhuma nota ainda.", color = Regla.Muted, style = MaterialTheme.typography.bodyMedium) }
        } else {
            items(state.notes, key = { it.id }) { note -> NoteItem(note, now) }
        }
    }
}

@Composable
private fun NoteItem(note: LeadNote, now: OffsetDateTime) {
    Column(Modifier.fillMaxWidth()) {
        Text(note.body, style = MaterialTheme.typography.bodyMedium, color = Regla.Ink)
        Spacer(Modifier.height(2.dp))
        Text(relativeTime(note.createdAt, now), style = MaterialTheme.typography.labelMedium, color = Regla.Muted)
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = Regla.Line)
    }
}

@Composable
internal fun SelectChip(label: String, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Regla.Navy,
            selectedLabelColor = Color.White,
        ),
    )
}

@Composable
private fun InfoCard(onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(Regla.RadiusCard),
        colors = CardDefaults.cardColors(containerColor = Regla.Surface),
        border = BorderStroke(1.dp, Regla.Line),
        modifier = Modifier.fillMaxWidth().let { if (onClick != null) it.clickable(onClick = onClick) else it },
    ) {
        Column(Modifier.padding(14.dp)) { content() }
    }
}
