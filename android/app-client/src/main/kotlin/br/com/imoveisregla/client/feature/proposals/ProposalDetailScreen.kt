package br.com.imoveisregla.client.feature.proposals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import br.com.imoveisregla.client.LocalAppContainer
import br.com.imoveisregla.client.feature.apply.MaskedDigitsField
import br.com.imoveisregla.client.feature.apply.formatIsoDateBR
import br.com.imoveisregla.client.feature.profile.DocumentChecklist
import br.com.imoveisregla.core.designsystem.ApplicationStatusPill
import br.com.imoveisregla.core.designsystem.ApplicationStatusTimeline
import br.com.imoveisregla.core.designsystem.ButtonKind
import br.com.imoveisregla.core.designsystem.ErrorState
import br.com.imoveisregla.core.designsystem.LoadingState
import br.com.imoveisregla.core.designsystem.Regla
import br.com.imoveisregla.core.designsystem.ReglaButton
import br.com.imoveisregla.core.designsystem.SectionTitle
import br.com.imoveisregla.core.model.Application
import br.com.imoveisregla.core.model.ApplicationStatus
import br.com.imoveisregla.core.model.Currency
import br.com.imoveisregla.core.model.Offer
import br.com.imoveisregla.core.model.Party
import br.com.imoveisregla.core.model.formatPrice
import br.com.imoveisregla.core.model.label
import br.com.imoveisregla.core.model.listingRef
import kotlinx.coroutines.launch

const val COUNTER_DIALOG_TAG = "counter-dialog"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProposalDetailScreen(applicationId: Long, onBack: () -> Unit, onOpenListing: (Long) -> Unit) {
    val container = LocalAppContainer.current
    val vm: ProposalDetailViewModel =
        viewModel(key = "proposal-$applicationId") { ProposalDetailViewModel(applicationId, container) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

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
        topBar = {
            TopAppBar(
                title = { Text("Proposta") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Regla.Surface),
            )
        },
        bottomBar = {
            val app = state.app
            if (app != null) ActionBar(state, vm)
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            val app = state.app
            when {
                state.loading -> LoadingState()
                app == null -> ErrorState(state.error ?: "Proposta não encontrada", onRetry = vm::load)
                else -> Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Regla.Gutter, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    HeaderCard(app, onOpenListing = { onOpenListing(app.listingId) })
                    NegotiationSection(app)
                    if (app.status.isDocumentsPhase || app.status == ApplicationStatus.APPROVED) {
                        DocumentsSection(
                            state = state,
                            app = app,
                            onUpload = vm::upload,
                            onDelete = vm::deleteDocument,
                            onOpen = { doc ->
                                scope.launch { vm.documentUrl(doc)?.let { runCatching { uriHandler.openUri(it) } } }
                            },
                        )
                    }
                    if (app.status.isOpen) {
                        TextButton(
                            onClick = { vm.ask(ConfirmKind.DECLINE) },
                            enabled = !state.busy,
                            modifier = Modifier.align(Alignment.End),
                        ) { Text("Cancelar proposta", color = Regla.Danger) }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    ConfirmDialogs(state, vm)
    if (state.counterOpen) CounterDialog(state, vm)
}

@Composable
private fun HeaderCard(app: Application, onOpenListing: () -> Unit) {
    val currency = app.listings?.currency ?: Currency.BRL
    DetailCard {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).clickable(onClick = onOpenListing)) {
                Text(app.listings?.title ?: "Imóvel ${listingRef(app.listingId)}", style = MaterialTheme.typography.titleMedium)
                Text(
                    listingRef(app.listingId) + (app.listings?.neighborhood?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.labelMedium, color = Regla.Muted,
                )
            }
            ApplicationStatusPill(app.status)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            clientHeadline(app),
            style = MaterialTheme.typography.titleMedium,
            color = if (app.isTurnOf(Party.CLIENT)) Regla.CoralInk else Regla.Ink,
            modifier = Modifier.testTag("proposal-headline"),
        )
        Spacer(Modifier.height(12.dp))
        ApplicationStatusTimeline(app.status)
        Spacer(Modifier.height(12.dp))
        Row {
            Column(Modifier.weight(1f)) {
                Text(
                    if (app.agreedPrice != null) "Valor acordado" else "Oferta atual",
                    style = MaterialTheme.typography.labelMedium, color = Regla.Muted,
                )
                Text(formatPrice(app.currentPrice, currency), style = MaterialTheme.typography.titleLarge, color = Regla.Navy)
            }
            val asking = app.listings?.price
            if (asking != null && asking > 0) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("Anunciado", style = MaterialTheme.typography.labelMedium, color = Regla.Muted)
                    Text(formatPrice(asking, currency), style = MaterialTheme.typography.titleMedium, color = Regla.Muted)
                }
            }
        }
        val note = app.reviewerNote
        if (!note.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(Regla.RadiusControl)).background(Regla.NavySoft).padding(12.dp),
            ) {
                Text("Mensagem do corretor", style = MaterialTheme.typography.labelMedium, color = Regla.Muted)
                Text(note, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun NegotiationSection(app: Application) {
    val currency = app.listings?.currency ?: Currency.BRL
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Negociação")
        if (app.offers.isEmpty()) {
            Text("Sua oferta de ${formatPrice(app.offeredPrice, currency)}", color = Regla.Muted)
        }
        app.offers.forEachIndexed { i, offer -> OfferBubble(offer, currency, first = i == 0) }
        if (app.status.isNegotiation && app.awaiting == Party.REALTOR) {
            Text(
                "Aguardando o proprietário responder…",
                style = MaterialTheme.typography.labelMedium, color = Regla.Muted,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun OfferBubble(offer: Offer, currency: Currency, first: Boolean) {
    val mine = offer.author == Party.CLIENT
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (mine) 16.dp else 4.dp, bottomEnd = if (mine) 4.dp else 16.dp,
            ),
            color = if (mine) Regla.Navy else Regla.Surface,
            border = if (mine) null else BorderStroke(1.dp, Regla.Line),
            modifier = Modifier.widthIn(max = 300.dp).testTag("offer-${offer.id}"),
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                val who = when {
                    mine && first -> "Sua proposta"
                    mine -> "Sua contraproposta"
                    else -> "Contraproposta do proprietário"
                }
                val fg = if (mine) Color.White else Regla.Ink
                Text(who, style = MaterialTheme.typography.labelMedium, color = if (mine) Color.White.copy(alpha = 0.75f) else Regla.Muted)
                Text(formatPrice(offer.price, currency), style = MaterialTheme.typography.titleLarge, color = fg)
                val terms = listOfNotNull(
                    offer.guaranteeType?.label,
                    offer.moveInDate?.let { "mudança ${formatIsoDateBR(it)}" },
                ).joinToString(" · ")
                if (terms.isNotBlank()) Text(terms, style = MaterialTheme.typography.labelMedium, color = fg.copy(alpha = 0.8f))
                if (!offer.message.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(offer.message.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = fg)
                }
                val date = formatProposalDate(offer.createdAt)
                if (date.isNotBlank()) {
                    Text(date, style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = 0.6f), modifier = Modifier.align(Alignment.End))
                }
            }
        }
    }
}

@Composable
private fun DocumentsSection(
    state: ProposalDetailUiState,
    app: Application,
    onUpload: (br.com.imoveisregla.core.model.DocumentKind, br.com.imoveisregla.core.model.UploadFile) -> Unit,
    onDelete: (Long) -> Unit,
    onOpen: (br.com.imoveisregla.core.model.ClientDocument) -> Unit,
) {
    val editable = state.action == ClientAction.SEND_DOCUMENTS
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Documentos")
        Text(
            when (app.status) {
                ApplicationStatus.ACCEPTED -> "Envie os documentos abaixo para a análise de crédito. Depois toque em \"Concluí o envio\"."
                ApplicationStatus.DOCS_REQUESTED -> "Ajuste os documentos conforme a mensagem do corretor e envie novamente."
                ApplicationStatus.DOCS_REVIEW -> "Recebemos seus documentos. O corretor está analisando."
                else -> "Documentos enviados para esta proposta."
            },
            style = MaterialTheme.typography.bodyMedium, color = Regla.Muted,
        )
        DocumentChecklist(
            documents = state.documents,
            onUpload = { kind, file -> if (editable) onUpload(kind, file) },
            onDelete = if (editable) onDelete else null,
            uploadingKind = state.uploadingKind,
            onOpen = onOpen,
        )
    }
}

@Composable
private fun ActionBar(state: ProposalDetailUiState, vm: ProposalDetailViewModel) {
    val action = state.action
    if (action != ClientAction.RESPOND_OFFER && action != ClientAction.SEND_DOCUMENTS) return
    Surface(color = Regla.Surface, shadowElevation = 8.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Regla.Gutter, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when (action) {
                ClientAction.RESPOND_OFFER -> {
                    ReglaButton("Contrapropor", vm::openCounter, kind = ButtonKind.Secondary, enabled = !state.busy, modifier = Modifier.weight(1f))
                    ReglaButton("Aceitar", { vm.ask(ConfirmKind.ACCEPT) }, kind = ButtonKind.Accent, loading = state.busy, modifier = Modifier.weight(1f))
                }
                else -> {
                    val missing = state.missingDocuments.size
                    ReglaButton(
                        text = if (missing == 0) "Concluí o envio" else "Faltam $missing documento" + if (missing > 1) "s" else "",
                        onClick = vm::askSendDocuments,
                        kind = ButtonKind.Accent,
                        enabled = missing == 0,
                        loading = state.busy,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfirmDialogs(state: ProposalDetailUiState, vm: ProposalDetailViewModel) {
    val kind = state.confirm ?: return
    val app = state.app ?: return
    val currency = app.listings?.currency ?: Currency.BRL
    val (title, text, label) = when (kind) {
        ConfirmKind.ACCEPT -> Triple(
            "Aceitar contraproposta?",
            "Você concorda com ${formatPrice(app.currentPrice, currency)}. Em seguida pediremos seus documentos para a análise.",
            "Aceitar",
        )
        ConfirmKind.DECLINE -> Triple(
            "Cancelar proposta?",
            "A negociação será encerrada. Essa ação não pode ser desfeita.",
            "Cancelar proposta",
        )
        ConfirmKind.SEND_DOCS -> Triple(
            "Enviar documentos para análise?",
            "O corretor será avisado e vai analisar seus documentos.",
            "Enviar",
        )
    }
    AlertDialog(
        onDismissRequest = vm::dismissConfirm,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = vm::confirm) {
                Text(label, color = if (kind == ConfirmKind.DECLINE) Regla.Danger else Regla.Navy, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = vm::dismissConfirm) { Text("Voltar") } },
    )
}

@Composable
private fun CounterDialog(state: ProposalDetailUiState, vm: ProposalDetailViewModel) {
    val currency = state.app?.listings?.currency ?: Currency.BRL
    AlertDialog(
        onDismissRequest = vm::closeCounter,
        modifier = Modifier.testTag(COUNTER_DIALOG_TAG),
        title = { Text("Fazer contraproposta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MaskedDigitsField(
                    digits = state.counterDigits,
                    onDigitsChange = { vm.updateCounter(digits = it) },
                    label = "Novo valor",
                    format = { formatPrice(it.toLong(), currency) },
                    maxDigits = 12,
                    error = state.counterError,
                    stripLeadingZeros = true,
                )
                OutlinedTextField(
                    value = state.counterMessage,
                    onValueChange = { vm.updateCounter(message = it) },
                    label = { Text("Mensagem (opcional)") },
                    minLines = 2,
                    shape = RoundedCornerShape(Regla.RadiusControl),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = vm::sendCounter) { Text("Enviar", fontWeight = FontWeight.SemiBold) } },
        dismissButton = { TextButton(onClick = vm::closeCounter) { Text("Cancelar") } },
    )
}

@Composable
private fun DetailCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(Regla.RadiusCard),
        color = Regla.Surface,
        border = BorderStroke(1.dp, Regla.Line),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}
