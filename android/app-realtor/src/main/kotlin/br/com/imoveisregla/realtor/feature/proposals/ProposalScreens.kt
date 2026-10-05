package br.com.imoveisregla.realtor.feature.proposals

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
import br.com.imoveisregla.core.designsystem.DemoBanner
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
        if (!container.isLive) DemoBanner()
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
            items(PROPOSAL_FILTERS, key = { it?.name ?: "ALL" }) { f ->
                FilterChip(
                    selected = state.filter == f,
                    onClick = { vm.setFilter(f) },
                    label = { Text("${filterLabel(f)} (${counts[f] ?: 0})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Regla.Navy,
                        selectedLabelColor = Color.White,
                        containerColor = Regla.Surface,
                    ),
                    modifier = Modifier.testTag("filter-${f?.name ?: "ALL"}"),
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
                                    "Nenhuma proposta com status \"${filterLabel(state.filter)}\".",
                                    action = { ReglaButton("Ver todas", { vm.setFilter(null) }, kind = ButtonKind.Secondary) },
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
    val diff = listing?.let { priceDiffPercent(app.offeredPrice, it.price) }
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(Regla.RadiusCard),
        colors = CardDefaults.cardColors(containerColor = Regla.Surface),
        modifier = Modifier.fillMaxWidth().testTag("proposal-${app.id}"),
    ) {
        Column(Modifier.padding(Regla.Gutter), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                    formatPrice(app.offeredPrice, currency),
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
                    val aff = affordability(app.offeredPrice, app.profile?.monthlyIncome)
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
    var dialog by remember { mutableStateOf<ApplicationStatus?>(null) }

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
                ReviewActionBar(
                    status = app.status,
                    enabled = state.canAct,
                    loading = state.submitting,
                    onUnderReview = { vm.review(ApplicationStatus.UNDER_REVIEW, null) },
                    onRequestDocs = { dialog = ApplicationStatus.DOCS_REQUESTED },
                    onApprove = { dialog = ApplicationStatus.APPROVED },
                    onReject = { dialog = ApplicationStatus.REJECTED },
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

    val target = dialog
    if (target != null) when (target) {
        ApplicationStatus.APPROVED -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Aprovar proposta?") },
            text = { Text("O cliente será avisado da aprovação e o lead será marcado como fechado.") },
            confirmButton = {
                TextButton(onClick = {
                    dialog = null
                    vm.review(ApplicationStatus.APPROVED, null)
                }) { Text("Aprovar", color = Regla.Coral, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Voltar") } },
        )
        ApplicationStatus.DOCS_REQUESTED, ApplicationStatus.REJECTED -> {
            val missing = missingRequiredDocs(state.documents)
            NoteDialog(
                title = if (target == ApplicationStatus.REJECTED) "Recusar proposta" else "Pedir documentos",
                confirmLabel = if (target == ApplicationStatus.REJECTED) "Recusar" else "Enviar pedido",
                initial = if (target == ApplicationStatus.DOCS_REQUESTED && missing.isNotEmpty()) {
                    "Por favor, envie: " + missing.joinToString(", ") { it.label } + "."
                } else "",
                hint = if (target == ApplicationStatus.REJECTED) "Motivo da recusa (o cliente verá esta mensagem)"
                else "Quais documentos o cliente precisa enviar?",
                onDismiss = { dialog = null },
                onConfirm = { note ->
                    dialog = null
                    vm.review(target, note)
                },
            )
        }
        else -> Unit
    }
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
private fun ReviewActionBar(
    status: ApplicationStatus,
    enabled: Boolean,
    loading: Boolean,
    onUnderReview: () -> Unit,
    onRequestDocs: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    Surface(color = Regla.Surface, shadowElevation = 8.dp) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = Regla.Gutter, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReglaButton(
                    "Em análise", onUnderReview, Modifier.weight(1f), kind = ButtonKind.Secondary,
                    enabled = enabled && status != ApplicationStatus.UNDER_REVIEW,
                )
                ReglaButton(
                    "Pedir documentos", onRequestDocs, Modifier.weight(1f), kind = ButtonKind.Secondary,
                    enabled = enabled,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReglaButton("Recusar", onReject, Modifier.weight(1f), kind = ButtonKind.Secondary, enabled = enabled)
                ReglaButton("Aprovar", onApprove, Modifier.weight(1f), kind = ButtonKind.Accent, enabled = enabled, loading = loading)
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
            InfoRow("Valor ofertado", formatPrice(app.offeredPrice, currency))
            if (listing != null && listing.price > 0) {
                InfoRow("Valor anunciado", formatPrice(listing.price, currency))
                InfoRow("Diferença", formatDiffPercent(priceDiffPercent(app.offeredPrice, listing.price)))
            }
            InfoRow("Garantia", app.guaranteeType?.label ?: "—")
            InfoRow("Mudança", app.moveInDate?.let { formatDateBr(it) } ?: "—")
            if (!app.message.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text("Mensagem do cliente", style = MaterialTheme.typography.labelMedium, color = Regla.Muted)
                Text("“${app.message}”", style = MaterialTheme.typography.bodyMedium, color = Regla.Ink)
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
                val aff = affordability(app.offeredPrice, income)
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
                rentIncomePercent(app.offeredPrice, income)?.let { pct ->
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
        SectionCard {
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
