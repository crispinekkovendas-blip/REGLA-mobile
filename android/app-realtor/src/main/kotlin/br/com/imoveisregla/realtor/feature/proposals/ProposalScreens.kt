package br.com.imoveisregla.realtor.feature.proposals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import br.com.imoveisregla.core.designsystem.ButtonKind
import br.com.imoveisregla.core.designsystem.EmptyState
import br.com.imoveisregla.core.designsystem.ErrorState
import br.com.imoveisregla.core.designsystem.LoadingState
import br.com.imoveisregla.core.designsystem.Regla
import br.com.imoveisregla.core.designsystem.ReglaButton
import br.com.imoveisregla.core.designsystem.ReglaTextField
import br.com.imoveisregla.core.designsystem.SectionTitle
import br.com.imoveisregla.core.designsystem.StatusPill
import br.com.imoveisregla.core.model.Application
import br.com.imoveisregla.core.model.ApplicationIntent
import br.com.imoveisregla.core.model.ApplicationStatus
import br.com.imoveisregla.core.model.ClientDocument
import br.com.imoveisregla.core.model.Currency
import br.com.imoveisregla.core.model.Offer
import br.com.imoveisregla.core.model.Party
import br.com.imoveisregla.core.model.affordability
import br.com.imoveisregla.core.model.formatPrice
import br.com.imoveisregla.core.model.label
import br.com.imoveisregla.core.model.listingRef
import br.com.imoveisregla.core.model.maskCpf
import br.com.imoveisregla.core.model.maskPhoneBR
import br.com.imoveisregla.core.model.waToClient
import br.com.imoveisregla.realtor.LocalAppContainer
import kotlinx.coroutines.launch

// ─── Proposals list ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProposalsScreen(onOpenApplication: (Long) -> Unit) {
    val container = LocalAppContainer.current
    val vm = viewModel { ProposalsViewModel(container.applications) }
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().background(Regla.NavySoft)) {
        Text(
            "Propostas",
            style = MaterialTheme.typography.headlineMedium,
            color = Regla.Ink,
            modifier = Modifier.padding(start = Regla.Gutter, end = Regla.Gutter, top = 16.dp, bottom = 8.dp),
        )
        val counts = state.counts
        LazyRow(
            contentPadding = PaddingValues(horizontal = Regla.Gutter),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(ProposalFilter.entries, key = { it.name }) { f ->
                FilterChip(
                    selected = state.filter == f,
                    onClick = { vm.setFilter(f) },
                    label = { Text("${f.label} (${counts[f] ?: 0})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Regla.Navy,
                        selectedLabelColor = Color.White,
                        containerColor = Regla.Surface,
                    ),
                    modifier = Modifier.testTag("filter-${f.name}"),
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
                state.error != null && state.all.isEmpty() -> ErrorState(state.error!!, onRetry = { vm.load() })
                state.visible.isEmpty() -> LazyColumn(Modifier.fillMaxSize()) {
                    item {
                        Box(Modifier.fillParentMaxSize()) {
                            if (state.all.isEmpty()) {
                                EmptyState("Nenhuma proposta ainda", "As propostas enviadas pelos clientes no app aparecem aqui.")
                            } else {
                                EmptyState(
                                    "Nada por aqui",
                                    "Nenhuma proposta em \"${state.filter.label}\".",
                                    action = { ReglaButton("Ver todas", { vm.setFilter(ProposalFilter.ALL) }, kind = ButtonKind.Secondary) },
                                )
                            }
                        }
                    }
                }
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = Regla.Gutter, end = Regla.Gutter, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.visible, key = { it.id }) { app ->
                        ProposalCard(app, onClick = { onOpenApplication(app.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ProposalCard(app: Application, onClick: () -> Unit) {
    val listing = app.listings
    val currency = listing?.currency ?: Currency.BRL
    val price = app.currentPrice
    val diff = listing?.let { priceDiffPercent(price, it.price) }
    val yourTurn = app.isTurnOf(Party.REALTOR)
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(Regla.RadiusCard),
        colors = CardDefaults.cardColors(containerColor = Regla.Surface),
        border = if (yourTurn) BorderStroke(2.dp, Regla.Coral) else null,
        modifier = Modifier.fillMaxWidth().testTag("proposal-${app.id}"),
    ) {
        Column(Modifier.padding(Regla.Gutter), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (yourTurn) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusPill("Sua vez", Color.White, Regla.Coral)
                    Spacer(Modifier.width(8.dp))
                    Text(realtorHeadline(app), style = MaterialTheme.typography.labelMedium, color = Regla.CoralInk, maxLines = 1)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    app.profile?.fullName ?: "Cliente sem cadastro",
                    style = MaterialTheme.typography.titleMedium,
                    color = Regla.Ink,
                    modifier = Modifier.weight(1f),
                )
                ApplicationStatusPill(app.status, Modifier.testTag("status-${app.id}"))
            }
            Text(
                listing?.title ?: "Imóvel ${listingRef(app.listingId)}",
                style = MaterialTheme.typography.bodyMedium,
                color = Regla.Muted,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatPrice(price, currency),
                    style = MaterialTheme.typography.titleMedium,
                    color = Regla.Navy,
                )
                if (listing != null && listing.price > 0) {
                    Text(
                        "  vs ${formatPrice(listing.price, currency)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Regla.Muted,
                    )
                    Spacer(Modifier.width(8.dp))
                    DiffText(diff)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(app.intent.label, style = MaterialTheme.typography.labelMedium, color = Regla.Navy)
                if (app.intent == ApplicationIntent.RENT) {
                    Spacer(Modifier.width(12.dp))
                    val aff = affordability(price, app.profile?.monthlyIncome)
                    Dot(affordabilityColor(aff))
                    Spacer(Modifier.width(6.dp))
                    Text(affordabilityRealtorLabel(aff), style = MaterialTheme.typography.labelMedium, color = Regla.Muted)
                }
                Spacer(Modifier.weight(1f))
                Text(relativeTime(app.createdAt), style = MaterialTheme.typography.labelMedium, color = Regla.Muted)
            }
        }
    }
}

@Composable
internal fun ApplicationStatusPill(status: ApplicationStatus, modifier: Modifier = Modifier) {
    val (fg, bg) = statusColors(status)
    StatusPill(status.label, fg, bg, modifier)
}

@Composable
private fun DiffText(diff: Double?) {
    if (diff == null) return
    val color = when {
        diff >= 0 -> Regla.Ok
        diff >= -10 -> Regla.Warn
        else -> Regla.Danger
    }
    Text(formatDiffPercent(diff), style = MaterialTheme.typography.labelLarge, color = color)
}

@Composable
private fun Dot(color: Color, size: Int = 10) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(color))
}

// ─── Application detail ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationDetailScreen(applicationId: Long, onBack: () -> Unit, onOpenListing: (Long) -> Unit) {
    val container = LocalAppContainer.current
    val vm = viewModel(key = "application-$applicationId") {
        ApplicationDetailViewModel(container.applications, container.documents, applicationId)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    var dialog by remember { mutableStateOf<DetailDialog?>(null) }

    LaunchedEffect(state.message) {
        val m = state.message ?: return@LaunchedEffect
        snackbar.showSnackbar(m)
        vm.consumeMessage()
    }

    val app = state.application
    Scaffold(
        containerColor = Regla.NavySoft,
        topBar = {
            TopAppBar(
                title = { Text("Proposta #$applicationId") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Regla.Surface),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (app != null && app.status.isOpen) {
                RealtorActionBar(
                    app = app,
                    action = state.action,
                    enabled = state.canAct,
                    loading = state.submitting,
                    onUnderReview = { vm.review(ApplicationStatus.UNDER_REVIEW, null) },
                    onOpen = { dialog = it },
                )
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.loading -> LoadingState()
                app == null -> ErrorState(state.error ?: "Proposta não encontrada", onRetry = { vm.load() })
                else -> ApplicationDetailContent(
                    app = app,
                    documents = state.documents,
                    documentsError = state.documentsError,
                    onOpenListing = onOpenListing,
                    onWhatsApp = { url ->
                        runCatching { uriHandler.openUri(url) }.onFailure { vm.showMessage("Não foi possível abrir o WhatsApp") }
                    },
                    onOpenDocument = { doc ->
                        scope.launch {
                            try {
                                uriHandler.openUri(vm.documentUrl(doc))
                            } catch (e: Exception) {
                                vm.showMessage("Não foi possível abrir o documento")
                            }
                        }
                    },
                )
            }
        }
    }

    val current = dialog
    if (current != null && app != null) {
        val close = { dialog = null }
        when (current) {
            DetailDialog.ACCEPT -> AlertDialog(
                onDismissRequest = close,
                title = { Text("Aceitar proposta?") },
                text = {
                    Text(
                        "O cliente será avisado de que a proposta de ${formatPrice(app.currentPrice, app.listings?.currency ?: Currency.BRL)} " +
                            "foi aceita e vai enviar os documentos para a análise.",
                    )
                },
                confirmButton = {
                    TextButton(onClick = { close(); vm.accept() }) {
                        Text("Aceitar", color = Regla.Coral, fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = { TextButton(onClick = close) { Text("Voltar") } },
            )
            DetailDialog.COUNTER -> CounterDialog(
                current = app.currentPrice,
                currency = app.listings?.currency ?: Currency.BRL,
                onDismiss = close,
                onConfirm = { price, note -> if (vm.counter(price, note) == null) close() },
            )
            DetailDialog.APPROVE -> {
                val missing = missingRequiredDocs(state.documents)
                AlertDialog(
                    onDismissRequest = close,
                    title = { Text("Aprovar proposta?") },
                    text = {
                        Text(
                            (if (missing.isNotEmpty()) "Atenção: faltam ${missing.joinToString(", ") { it.label }}. " else "") +
                                "O cliente será avisado da aprovação e o lead será marcado como fechado.",
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { close(); vm.review(ApplicationStatus.APPROVED, null) }) {
                            Text("Aprovar", color = Regla.Coral, fontWeight = FontWeight.SemiBold)
                        }
                    },
                    dismissButton = { TextButton(onClick = close) { Text("Voltar") } },
                )
            }
            DetailDialog.REQUEST_DOCS -> {
                val missing = missingRequiredDocs(state.documents)
                NoteDialog(
                    title = "Pedir correção",
                    confirmLabel = "Enviar pedido",
                    initial = if (missing.isNotEmpty()) "Por favor, envie: " + missing.joinToString(", ") { it.label } + "." else "",
                    hint = "O que o cliente precisa corrigir ou enviar?",
                    onDismiss = close,
                    onConfirm = { note -> close(); vm.review(ApplicationStatus.DOCS_REQUESTED, note) },
                )
            }
            DetailDialog.DECLINE -> NoteDialog(
                title = "Recusar proposta",
                confirmLabel = "Recusar",
                initial = "",
                hint = "Motivo da recusa (o cliente verá esta mensagem)",
                onDismiss = close,
                onConfirm = { note -> close(); vm.decline(note) },
            )
        }
    }
}

enum class DetailDialog { ACCEPT, COUNTER, DECLINE, APPROVE, REQUEST_DOCS }

@Composable
private fun CounterDialog(
    current: Long,
    currency: Currency,
    onDismiss: () -> Unit,
    onConfirm: (Long?, String?) -> Unit,
) {
    var digits by remember { mutableStateOf(current.toString()) }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("counter-dialog"),
        title = { Text("Contraproposta do proprietário") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Oferta atual: ${formatPrice(current, currency)}", style = MaterialTheme.typography.bodyMedium, color = Regla.Muted)
                ReglaTextField(
                    value = digits.toLongOrNull()?.let { formatPrice(it, currency) } ?: "",
                    onValueChange = { v -> digits = v.filter(Char::isDigit).trimStart('0').take(12) },
                    label = "Novo valor",
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                    modifier = Modifier.testTag("counter-price"),
                )
                ReglaTextField(
                    value = note,
                    onValueChange = { note = it.take(500) },
                    label = "Mensagem ao cliente (opcional)",
                    singleLine = false,
                    minLines = 2,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(digits.toLongOrNull(), note) }) { Text("Enviar", fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun NoteDialog(
    title: String,
    confirmLabel: String,
    initial: String,
    hint: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var note by remember { mutableStateOf(initial) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(hint, style = MaterialTheme.typography.bodyMedium, color = Regla.Muted)
                Spacer(Modifier.height(8.dp))
                ReglaTextField(
                    value = note,
                    onValueChange = { note = it; error = null },
                    label = "Observação",
                    error = error,
                    singleLine = false,
                    minLines = 3,
                    modifier = Modifier.testTag("review-note"),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (note.isBlank()) error = "Escreva uma observação para o cliente" else onConfirm(note.trim())
            }) { Text(confirmLabel, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun RealtorActionBar(
    app: Application,
    action: RealtorAction,
    enabled: Boolean,
    loading: Boolean,
    onUnderReview: () -> Unit,
    onOpen: (DetailDialog) -> Unit,
) {
    Surface(color = Regla.Surface, shadowElevation = 8.dp) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = Regla.Gutter, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (action) {
                RealtorAction.RESPOND_OFFER -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReglaButton("Contrapropor", { onOpen(DetailDialog.COUNTER) }, Modifier.weight(1f), kind = ButtonKind.Secondary, enabled = enabled)
                        ReglaButton("Aceitar", { onOpen(DetailDialog.ACCEPT) }, Modifier.weight(1f), kind = ButtonKind.Accent, enabled = enabled, loading = loading)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (app.status == ApplicationStatus.SUBMITTED) {
                            ReglaButton("Em análise", onUnderReview, Modifier.weight(1f), kind = ButtonKind.Secondary, enabled = enabled)
                        }
                        ReglaButton("Recusar", { onOpen(DetailDialog.DECLINE) }, Modifier.weight(1f), kind = ButtonKind.Secondary, enabled = enabled)
                    }
                }
                RealtorAction.REVIEW_DOCS -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReglaButton("Pedir correção", { onOpen(DetailDialog.REQUEST_DOCS) }, Modifier.weight(1f), kind = ButtonKind.Secondary, enabled = enabled)
                        ReglaButton("Aprovar", { onOpen(DetailDialog.APPROVE) }, Modifier.weight(1f), kind = ButtonKind.Accent, enabled = enabled, loading = loading)
                    }
                    ReglaButton("Recusar", { onOpen(DetailDialog.DECLINE) }, Modifier.fillMaxWidth(), kind = ButtonKind.Secondary, enabled = enabled)
                }
                RealtorAction.WAIT_CLIENT -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        realtorHeadline(app),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Regla.Muted,
                        modifier = Modifier.weight(1f).testTag("waiting-client"),
                    )
                    TextButton(onClick = { onOpen(DetailDialog.DECLINE) }, enabled = enabled) {
                        Text("Recusar", color = Regla.Danger)
                    }
                }
                RealtorAction.CLOSED -> Unit
            }
        }
    }
}

@Composable
private fun ApplicationDetailContent(
    app: Application,
    documents: List<ClientDocument>,
    documentsError: String?,
    onOpenListing: (Long) -> Unit,
    onWhatsApp: (String) -> Unit,
    onOpenDocument: (ClientDocument) -> Unit,
) {
    val listing = app.listings
    val currency = listing?.currency ?: Currency.BRL
    val profile = app.profile
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Regla.Gutter),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Status header
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ApplicationStatusPill(app.status, Modifier.testTag("detail-status"))
                Spacer(Modifier.weight(1f))
                Text(
                    "Enviada ${relativeTime(app.createdAt)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Regla.Muted,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                realtorHeadline(app),
                style = MaterialTheme.typography.titleMedium,
                color = if (app.isTurnOf(Party.REALTOR)) Regla.CoralInk else Regla.Ink,
                modifier = Modifier.testTag("detail-headline"),
            )
            Spacer(Modifier.height(12.dp))
            Timeline(timelineSteps(app))
            if (!app.reviewerNote.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Text("Observação: ${app.reviewerNote}", style = MaterialTheme.typography.bodyMedium, color = Regla.Ink)
            }
            if (app.updatedAt.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Atualizada em ${formatDateTimeBr(app.updatedAt)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Regla.Muted,
                )
            }
        }

        // Proposta
        SectionCard {
            SectionTitle("Proposta")
            Spacer(Modifier.height(8.dp))
            InfoRow("Intenção", app.intent.label)
            InfoRow("Oferta inicial", formatPrice(app.offeredPrice, currency))
            InfoRow(if (app.agreedPrice != null) "Valor acordado" else "Oferta atual", formatPrice(app.currentPrice, currency))
            if (listing != null && listing.price > 0) {
                InfoRow("Valor anunciado", formatPrice(listing.price, currency))
                InfoRow("Diferença", formatDiffPercent(priceDiffPercent(app.currentPrice, listing.price)))
            }
            InfoRow("Garantia", app.guaranteeType?.label ?: "—")
            InfoRow("Mudança", app.moveInDate?.let { formatDateBr(it) } ?: "—")
            if (!app.message.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text("Mensagem do cliente", style = MaterialTheme.typography.labelMedium, color = Regla.Muted)
                Text("“${app.message}”", style = MaterialTheme.typography.bodyMedium, color = Regla.Ink)
            }
        }

        // Negociação
        if (app.offers.isNotEmpty()) {
            SectionCard {
                SectionTitle("Negociação")
                Spacer(Modifier.height(8.dp))
                app.offers.forEachIndexed { i, offer ->
                    OfferRow(offer, currency, first = i == 0)
                    if (i < app.offers.lastIndex) HorizontalDivider(color = Regla.Line)
                }
            }
        }

        // Cliente
        SectionCard {
            SectionTitle("Cliente")
            Spacer(Modifier.height(8.dp))
            if (profile == null) {
                Text("O cliente ainda não completou o cadastro.", color = Regla.Muted)
            } else {
                InfoRow("Nome", profile.fullName)
                InfoRow("CPF", profile.cpf?.takeIf { it.isNotBlank() }?.let { maskCpf(it) } ?: "Não informado")
                InfoRow("E-mail", profile.email)
                InfoRow("Telefone", maskPhoneBR(profile.phone).ifBlank { profile.phone })
                InfoRow("Nascimento", profile.birthDate?.let { formatDateBr(it) } ?: "—")
                InfoRow("Profissão", profile.occupation?.takeIf { it.isNotBlank() } ?: "—")
                InfoRow("Vínculo", profile.employmentType?.label ?: "—")
                InfoRow("Renda mensal", profile.monthlyIncome?.let { formatPrice(it, Currency.BRL) } ?: "Não informada")
                InfoRow("Moradores", profile.residents.toString())
                InfoRow("Pets", if (profile.hasPets) "Sim" else "Não")
                if (profile.phone.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    val title = listing?.title ?: listingRef(app.listingId)
                    val text = "Olá ${firstName(profile.fullName)}, aqui é da REGLA sobre sua proposta para $title"
                    ReglaButton(
                        "Conversar no WhatsApp",
                        { onWhatsApp(waToClient(profile.phone, text)) },
                        Modifier.fillMaxWidth(),
                        kind = ButtonKind.Secondary,
                    )
                }
            }
        }

        // Análise de renda
        SectionCard {
            SectionTitle("Análise de renda")
            Spacer(Modifier.height(8.dp))
            val income = profile?.monthlyIncome
            if (app.intent == ApplicationIntent.RENT) {
                val aff = affordability(app.currentPrice, income)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Dot(affordabilityColor(aff), 12)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        affordabilityRealtorLabel(aff),
                        style = MaterialTheme.typography.titleMedium,
                        color = affordabilityColor(aff),
                        modifier = Modifier.testTag("affordability"),
                    )
                }
                rentIncomePercent(app.currentPrice, income)?.let { pct ->
                    Spacer(Modifier.height(4.dp))
                    Text("aluguel = $pct% da renda", style = MaterialTheme.typography.bodyMedium, color = Regla.Ink)
                }
                Text(
                    "Referência: até 30% da renda (até 40% é apertado).",
                    style = MaterialTheme.typography.labelMedium,
                    color = Regla.Muted,
                )
            } else {
                Text(
                    if (income != null) "Renda mensal declarada: ${formatPrice(income, Currency.BRL)}."
                    else "Renda não informada.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Para compra, avalie financiamento e crédito separadamente.",
                    style = MaterialTheme.typography.labelMedium,
                    color = Regla.Muted,
                )
            }
        }

        // Documentos
        if (!app.status.isDocumentsPhase && app.status != ApplicationStatus.APPROVED) {
            SectionCard {
                SectionTitle("Documentos")
                Spacer(Modifier.height(6.dp))
                Text(
                    "Os documentos são pedidos ao cliente só depois que a proposta é aceita.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Regla.Muted,
                )
            }
        } else SectionCard {
            SectionTitle("Documentos")
            Spacer(Modifier.height(8.dp))
            val missing = missingRequiredDocs(documents).toSet()
            REQUIRED_DOCS.forEach { kind ->
                Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    val ok = kind !in missing
                    Icon(
                        if (ok) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = if (ok) Regla.Ok else Regla.Danger,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(kind.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(
                        if (ok) "Enviado" else "Pendente",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (ok) Regla.Ok else Regla.Danger,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Regla.Line)
            when {
                documentsError != null -> Text(documentsError, color = Regla.Danger, modifier = Modifier.padding(top = 8.dp))
                documents.isEmpty() -> Text(
                    "Nenhum documento enviado.",
                    color = Regla.Muted,
                    modifier = Modifier.padding(top = 8.dp),
                )
                else -> documents.forEach { doc ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onOpenDocument(doc) }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Description, contentDescription = null, tint = Regla.Navy)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(doc.kind.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${doc.filename} · ${formatBytes(doc.sizeBytes)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Regla.Muted,
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Abrir", tint = Regla.Muted)
                    }
                }
            }
        }

        // Imóvel
        Card(
            onClick = { onOpenListing(app.listingId) },
            shape = RoundedCornerShape(Regla.RadiusCard),
            colors = CardDefaults.cardColors(containerColor = Regla.Surface),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(Modifier.padding(Regla.Gutter), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(listingRef(app.listingId), style = MaterialTheme.typography.labelMedium, color = Regla.Muted)
                    Text(listing?.title ?: "Ver imóvel", style = MaterialTheme.typography.titleMedium, color = Regla.Ink)
                    if (listing != null) {
                        val place = listOf(listing.neighborhood, listing.city).filter { it.isNotBlank() }.joinToString(", ")
                        if (place.isNotBlank()) Text(place, style = MaterialTheme.typography.bodyMedium, color = Regla.Muted)
                        if (listing.price > 0) {
                            Text(formatPrice(listing.price, currency), style = MaterialTheme.typography.bodyMedium, color = Regla.Navy)
                        }
                    }
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Abrir imóvel", tint = Regla.Muted)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun OfferRow(offer: Offer, currency: Currency, first: Boolean) {
    val fromClient = offer.author == Party.CLIENT
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("offer-${offer.id}"), verticalAlignment = Alignment.Top) {
        Dot(if (fromClient) Regla.Navy else Regla.Coral, 10)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                when {
                    fromClient && first -> "Proposta do cliente"
                    fromClient -> "Contraproposta do cliente"
                    else -> "Sua contraproposta"
                },
                style = MaterialTheme.typography.labelMedium,
                color = Regla.Muted,
            )
            Text(formatPrice(offer.price, currency), style = MaterialTheme.typography.titleMedium, color = Regla.Ink)
            if (!offer.message.isNullOrBlank()) {
                Text("“${offer.message}”", style = MaterialTheme.typography.bodyMedium, color = Regla.Ink)
            }
        }
        Text(relativeTime(offer.createdAt), style = MaterialTheme.typography.labelMedium, color = Regla.Muted)
    }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = Regla.Surface,
        shape = RoundedCornerShape(Regla.RadiusCard),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Regla.Gutter), content = content)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Regla.Muted, modifier = Modifier.weight(0.42f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Regla.Ink, modifier = Modifier.weight(0.58f))
    }
}

@Composable
private fun Timeline(steps: List<TimelineStep>) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        steps.forEachIndexed { i, step ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.weight(1f).height(2.dp)
                            .background(if (i == 0) Color.Transparent else if (step.done) Regla.Navy else Regla.Line),
                    )
                    Dot(if (step.done) step.tone else Regla.Line, if (step.current) 14 else 10)
                    Box(
                        Modifier.weight(1f).height(2.dp).background(
                            when {
                                i == steps.lastIndex -> Color.Transparent
                                steps[i + 1].done -> Regla.Navy
                                else -> Regla.Line
                            },
                        ),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    step.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (step.current) Regla.Ink else Regla.Muted,
                    fontWeight = if (step.current) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 2,
                )
            }
        }
    }
}
