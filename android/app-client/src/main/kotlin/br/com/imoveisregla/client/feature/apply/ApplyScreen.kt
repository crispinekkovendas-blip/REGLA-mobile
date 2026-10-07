package br.com.imoveisregla.client.feature.apply

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import br.com.imoveisregla.core.designsystem.SectionTitle
import br.com.imoveisregla.core.model.ApplicationIntent
import br.com.imoveisregla.core.model.Currency
import br.com.imoveisregla.core.model.GuaranteeType
import br.com.imoveisregla.core.model.Listing
import br.com.imoveisregla.core.model.affordability
import br.com.imoveisregla.core.model.formatPrice
import br.com.imoveisregla.core.model.label
import br.com.imoveisregla.core.model.listingRef
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplyScreen(listingId: Long, onBack: () -> Unit, onDone: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: ApplyViewModel = viewModel(key = "apply-$listingId") { ApplyViewModel(listingId, container) }
    val state by vm.state.collectAsStateWithLifecycle()

    BackHandler(enabled = state.submitted == null && state.step != WizardStep.PROFILE) { vm.back() }

    Scaffold(
        containerColor = Regla.NavySoft,
        topBar = {
            TopAppBar(
                title = { Text(if (state.submitted != null) "Proposta" else "Fazer proposta") },
                navigationIcon = {
                    IconButton(onClick = { if (state.submitted != null) onDone() else if (!vm.back()) onBack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Regla.Surface),
            )
        },
        bottomBar = {
            if (!state.loading && state.loadError == null && !state.signedOut && state.submitted == null) {
                WizardBottomBar(
                    step = state.step,
                    busy = state.busy,
                    onBack = { if (!vm.back()) onBack() },
                    onNext = vm::next,
                )
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            val listing = state.listing
            when {
                state.loading -> LoadingState()
                state.signedOut -> EmptyState(
                    title = "Entre para fazer uma proposta",
                    message = "Você precisa estar logado para enviar uma proposta.",
                    action = { ReglaButton("Voltar", onBack, kind = ButtonKind.Secondary) },
                )
                state.loadError != null -> ErrorState(state.loadError ?: "", onRetry = vm::load)
                state.submitted != null -> SubmittedContent(onDone)
                listing != null -> WizardContent(state, listing, vm)
            }
        }
    }

    val error = state.error
    if (error != null) {
        AlertDialog(
            onDismissRequest = vm::clearError,
            title = { Text("Não foi possível continuar") },
            text = { Text(error) },
            confirmButton = { TextButton(onClick = vm::clearError) { Text("OK") } },
        )
    }
}

@Composable
private fun WizardBottomBar(step: WizardStep, busy: Boolean, onBack: () -> Unit, onNext: () -> Unit) {
    Surface(color = Regla.Surface, shadowElevation = 8.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Regla.Gutter, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (step != WizardStep.PROFILE) {
                ReglaButton("Voltar", onBack, kind = ButtonKind.Secondary, enabled = !busy, modifier = Modifier.weight(1f))
            }
            ReglaButton(
                text = if (step == WizardStep.REVIEW) "Enviar proposta" else "Continuar",
                onClick = onNext,
                kind = if (step == WizardStep.REVIEW) ButtonKind.Accent else ButtonKind.Primary,
                loading = busy,
                modifier = Modifier.weight(if (step == WizardStep.PROFILE) 1f else 1.6f),
            )
        }
    }
}

@Composable
private fun WizardContent(state: ApplyUiState, listing: Listing, vm: ApplyViewModel) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Regla.Gutter, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        WizardStepIndicator(state.step)
        ListingHeader(listing)
        val open = state.existingOpen
        if (open != null) {
            NoticeBox(
                title = "Você já tem uma proposta em aberto",
                text = "Sua proposta de ${formatPrice(open.offeredPrice, listing.currency)} para este imóvel está " +
                    "\"${open.status.label}\". Enviar outra pode atrasar a análise.",
            )
        }
        when (state.step) {
            WizardStep.PROFILE -> WizardProfileStep(state.profile, state.profileErrors, vm::updateProfile)
            WizardStep.OFFER -> OfferStep(state, listing, vm)
            WizardStep.REVIEW -> ReviewStep(state, listing, vm)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ListingHeader(listing: Listing) {
    WizardCard {
        Text(listing.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
        Spacer(Modifier.height(2.dp))
        Text(
            "${listingRef(listing.id)} · ${listing.neighborhood}, ${listing.city}",
            style = MaterialTheme.typography.labelMedium, color = Regla.Muted,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            formatPrice(listing.price, listing.currency) + if (listing.isRental) " /mês" else "",
            style = MaterialTheme.typography.titleLarge, color = Regla.Navy,
        )
    }
}

// ─── step 2 ───────────────────────────────────────────────────────────

private val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
private val BR_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** "2026-10-20" → "20/10/2026"; returns the input when it isn't a date. */
fun formatIsoDateBR(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return runCatching { LocalDate.parse(iso.take(10), ISO_DATE).format(BR_DATE) }.getOrDefault(iso)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OfferStep(state: ApplyUiState, listing: Listing, vm: ApplyViewModel) {
    val offer = state.offer
    val errors = state.offerErrors
    var showDatePicker by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Sua proposta")
        FieldLabel("Você quer")
        ChoiceChips(
            options = ApplicationIntent.entries,
            selected = offer.intent,
            label = { it.label },
            onSelect = { v -> vm.updateOffer { it.copy(intent = v) } },
        )

        MaskedDigitsField(
            digits = offer.priceDigits,
            onDigitsChange = { v -> vm.updateOffer { it.copy(priceDigits = v) } },
            label = if (offer.intent == ApplicationIntent.RENT) "Valor do aluguel proposto" else "Valor proposto",
            format = { formatPrice(it.toLong(), listing.currency) },
            maxDigits = 12,
            error = errors["offeredPrice"],
            supporting = priceDifferenceLabel(offer.offeredPrice, listing.price),
            stripLeadingZeros = true,
        )

        if (offer.intent == ApplicationIntent.RENT) {
            val aff = affordability(offer.offeredPrice, state.monthlyIncome)
            Row(verticalAlignment = Alignment.CenterVertically) {
                WizardAffordabilityBadge(aff)
                Spacer(Modifier.width(8.dp))
                val income = state.monthlyIncome
                if (income != null && income > 0 && offer.offeredPrice > 0) {
                    val pct = (offer.offeredPrice * 100.0 / income).toInt()
                    Text("$pct% da renda", style = MaterialTheme.typography.labelMedium, color = Regla.Muted)
                }
            }

            FieldLabel("Garantia")
            ChoiceChips(
                options = GuaranteeType.entries,
                selected = offer.guaranteeType,
                label = { it.label },
                onSelect = { v -> vm.updateOffer { it.copy(guaranteeType = v) } },
            )
            FieldError(errors["guaranteeType"])
        }

        FieldLabel(if (offer.intent == ApplicationIntent.RENT) "Data de mudança" else "Data desejada para a posse")
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(Regla.RadiusControl),
            color = Regla.Surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (errors["moveInDate"] != null) Regla.Danger else Regla.Line),
            modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = Regla.Muted)
                Spacer(Modifier.width(10.dp))
                Text(
                    if (offer.moveInDate.isBlank()) "Escolher data (opcional)" else formatIsoDateBR(offer.moveInDate),
                    color = if (offer.moveInDate.isBlank()) Regla.Muted else Regla.Ink,
                    modifier = Modifier.weight(1f),
                )
                if (offer.moveInDate.isNotBlank()) {
                    TextButton(onClick = { vm.updateOffer { it.copy(moveInDate = "") } }) { Text("Limpar") }
                }
            }
        }
        FieldError(errors["moveInDate"])

        OutlinedTextField(
            value = offer.message,
            onValueChange = { v -> vm.updateOffer { it.copy(message = v) } },
            label = { Text("Mensagem para o proprietário (opcional)") },
            isError = errors["message"] != null,
            supportingText = { Text(errors["message"] ?: "${offer.message.length}/$MAX_MESSAGE_LENGTH") },
            minLines = 3,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(Regla.RadiusControl),
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (showDatePicker) {
        val todayMillis = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val initial = runCatching {
            LocalDate.parse(offer.moveInDate).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }.getOrNull()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initial,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= todayMillis
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = pickerState.selectedDateMillis
                    if (millis != null) {
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().format(ISO_DATE)
                        vm.updateOffer { it.copy(moveInDate = date) }
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

// ─── step 3 ───────────────────────────────────────────────────────────

@Composable
private fun ReviewRow(label: String, value: String, valueColor: Color = Regla.Ink) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = Regla.Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            value, color = valueColor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End, modifier = Modifier.weight(1.3f),
        )
    }
}

@Composable
private fun ReviewStep(state: ApplyUiState, listing: Listing, vm: ApplyViewModel) {
    val offer = state.offer
    val isRent = offer.intent == ApplicationIntent.RENT
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Revise sua proposta")

        WizardCard {
            Text("Sua oferta", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            ReviewRow("Tipo", offer.intent.label)
            ReviewRow("Valor anunciado", formatPrice(listing.price, listing.currency))
            ReviewRow("Valor proposto", formatPrice(offer.offeredPrice, listing.currency), Regla.Navy)
            priceDifferenceLabel(offer.offeredPrice, listing.price)?.let {
                Text(it, style = MaterialTheme.typography.labelMedium, color = Regla.Muted)
            }
            if (isRent) ReviewRow("Garantia", offer.guaranteeType?.label ?: "—")
            ReviewRow(if (isRent) "Mudança" else "Posse", formatIsoDateBR(offer.moveInDate).ifBlank { "A combinar" })
            if (offer.message.isNotBlank()) {
                HorizontalDivider(Modifier.padding(vertical = 6.dp), color = Regla.Line)
                Text("“${offer.message.trim()}”", style = MaterialTheme.typography.bodyMedium, color = Regla.Muted)
            }
        }

        WizardCard {
            Text("Seu cadastro", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            ReviewRow("Nome", state.profile.fullName)
            ReviewRow("Vínculo", state.profile.employmentType?.label ?: "—")
            val income = state.monthlyIncome
            ReviewRow("Renda mensal", if (income != null) formatPrice(income, Currency.BRL) else "—")
            ReviewRow("Moradores", "${state.profile.residents}" + if (state.profile.hasPets) " · com pets" else "")
            if (isRent) {
                Spacer(Modifier.height(4.dp))
                WizardAffordabilityBadge(affordability(offer.offeredPrice, income))
            }
        }

        NoticeBox(
            title = "Próximos passos",
            text = "O proprietário pode aceitar, recusar ou fazer uma contraproposta. " +
                "Os documentos só serão pedidos depois que vocês chegarem a um acordo.",
            color = Regla.Navy,
            background = Regla.Surface,
        )

        Row(
            Modifier
                .fillMaxWidth()
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(Regla.RadiusControl))
                .clickable { vm.setDeclared(!state.declared) }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = state.declared,
                onCheckedChange = vm::setDeclared,
                colors = CheckboxDefaults.colors(checkedColor = Regla.Navy),
            )
            Text("Declaro que as informações são verdadeiras", style = MaterialTheme.typography.bodyMedium)
        }
        if (state.declarationError) FieldError("Confirme a declaração para enviar")
    }
}

// ─── success ──────────────────────────────────────────────────────────

@Composable
private fun SubmittedContent(onDone: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(84.dp).clip(CircleShape).background(Regla.OkSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Check, contentDescription = null, tint = Regla.Ok, modifier = Modifier.size(48.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("Proposta enviada!", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "O proprietário vai analisar sua oferta. Você será avisado para aceitar uma contraproposta ou enviar os documentos.",
            color = Regla.Muted, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        ReglaButton("Ver minhas propostas", onDone, modifier = Modifier.fillMaxWidth())
    }
}
