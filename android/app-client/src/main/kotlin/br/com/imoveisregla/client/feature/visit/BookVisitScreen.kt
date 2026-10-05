package br.com.imoveisregla.client.feature.visit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.imoveisregla.client.LocalAppContainer
import br.com.imoveisregla.core.designsystem.ButtonKind
import br.com.imoveisregla.core.designsystem.EmptyState
import br.com.imoveisregla.core.designsystem.ErrorState
import br.com.imoveisregla.core.designsystem.ListingImage
import br.com.imoveisregla.core.designsystem.LoadingState
import br.com.imoveisregla.core.designsystem.Regla
import br.com.imoveisregla.core.designsystem.ReglaButton
import br.com.imoveisregla.core.designsystem.ReglaTextField
import br.com.imoveisregla.core.designsystem.SectionTitle
import br.com.imoveisregla.core.model.Listing
import br.com.imoveisregla.core.model.listingRef
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookVisitScreen(listingId: Long, onBack: () -> Unit, onDone: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: BookVisitViewModel = viewModel(key = "book-visit-$listingId") { BookVisitViewModel(listingId, container) }
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agendar visita") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Regla.Surface),
            )
        },
        containerColor = Regla.NavySoft,
    ) { padding ->
        val m = Modifier.padding(padding)
        when {
            !state.signedIn -> EmptyState(
                title = "Entre para agendar",
                message = "Faça login na sua conta para agendar uma visita a este imóvel.",
                modifier = m,
                action = { ReglaButton("Voltar ao imóvel", onBack) },
            )
            state.loading -> LoadingState(m)
            state.loadError != null -> ErrorState(state.loadError!!, onRetry = vm::retry, modifier = m)
            state.done -> VisitBooked(state, onDone, m)
            else -> BookVisitForm(state, vm, m)
        }
    }
}

@Composable
private fun VisitBooked(state: BookVisitUiState, onDone: () -> Unit, modifier: Modifier) {
    Column(
        modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Regla.Ok, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text("Visita solicitada!", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "O corretor vai confirmar pelo WhatsApp.",
            color = Regla.Muted, textAlign = TextAlign.Center,
        )
        state.selectedSlot?.let {
            Spacer(Modifier.height(12.dp))
            Text(visitWhenLabel(it), style = MaterialTheme.typography.titleMedium, color = Regla.Navy)
        }
        Spacer(Modifier.height(24.dp))
        ReglaButton("Ver minhas visitas", onDone, modifier = Modifier.fillMaxWidth())
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BookVisitForm(state: BookVisitUiState, vm: BookVisitViewModel, modifier: Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Regla.Gutter),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.listing?.let { ListingSummary(it, state.photoUrl) }

        Spacer(Modifier.height(4.dp))
        SectionTitle("Escolha o dia")
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).testTag("visit-days"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.days.forEach { d -> DayChip(d, selected = d == state.selectedDay, onClick = { vm.selectDay(d) }) }
        }

        SectionTitle("Escolha o horário")
        if (state.slots.none { it.enabled }) {
            Text("Nenhum horário disponível neste dia. Escolha outra data.", color = Regla.Muted)
        }
        FlowRow(
            Modifier.fillMaxWidth().testTag("visit-slots"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.slots.forEach { s ->
                SlotChip(s, selected = s.isoStart == state.selectedSlot, onClick = { vm.selectSlot(s) })
            }
        }
        state.errors["startsAt"]?.let { Text(it, color = Regla.Danger, style = MaterialTheme.typography.bodyMedium) }

        Spacer(Modifier.height(4.dp))
        SectionTitle("Seus dados")
        ReglaTextField(state.name, vm::onName, "Nome", error = state.errors["visitorName"])
        ReglaTextField(state.email, vm::onEmail, "E-mail", error = state.errors["visitorEmail"], keyboardType = KeyboardType.Email)
        ReglaTextField(
            state.phone, vm::onPhone, "Telefone / WhatsApp",
            error = state.errors["visitorPhone"], keyboardType = KeyboardType.Phone,
        )
        ReglaTextField(
            state.notes, vm::onNotes, "Observações (opcional)",
            singleLine = false, minLines = 3,
        )

        state.submitError?.let { Text(it, color = Regla.Danger, style = MaterialTheme.typography.bodyMedium) }

        ReglaButton(
            text = "Confirmar visita",
            onClick = vm::confirm,
            kind = ButtonKind.Accent,
            loading = state.submitting,
            modifier = Modifier.fillMaxWidth().testTag("visit-confirm"),
        )
        Text(
            "A visita é gratuita. Você recebe a confirmação do corretor pelo WhatsApp.",
            color = Regla.Muted, style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ListingSummary(listing: Listing, photoUrl: String?) {
    Surface(
        shape = RoundedCornerShape(Regla.RadiusCard),
        color = Regla.Surface,
        border = BorderStroke(1.dp, Regla.Line),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ListingImage(
                url = photoUrl, contentDescription = listing.title,
                modifier = Modifier.size(76.dp).clip(RoundedCornerShape(Regla.RadiusControl)),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    listing.title, style = MaterialTheme.typography.titleMedium,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOf(listing.neighborhood, listing.city).filter { it.isNotBlank() }.joinToString(" · "),
                    color = Regla.Muted, style = MaterialTheme.typography.bodyMedium,
                )
                Text("Ref. ${listingRef(listing.id)}", color = Regla.Muted, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun DayChip(day: LocalDate, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) Regla.Navy else Regla.Surface
    val fg = if (selected) Color.White else Regla.Ink
    val sub = if (selected) Color.White.copy(alpha = 0.8f) else Regla.Muted
    Column(
        Modifier
            .clip(RoundedCornerShape(Regla.RadiusControl))
            .background(bg)
            .border(1.dp, if (selected) Regla.Navy else Regla.Line, RoundedCornerShape(Regla.RadiusControl))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("day-$day"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(weekdayShort(day), color = sub, style = MaterialTheme.typography.labelMedium)
        Text("${day.dayOfMonth}", color = fg, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(monthShort(day), color = sub, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SlotChip(slot: Slot, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    val bg = when {
        selected -> Regla.Coral
        !slot.enabled -> Regla.NavySoft
        else -> Regla.Surface
    }
    val fg = when {
        selected -> Color.White
        !slot.enabled -> Regla.Muted.copy(alpha = 0.5f)
        else -> Regla.Ink
    }
    Text(
        slot.label,
        color = fg,
        style = MaterialTheme.typography.labelLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .width(76.dp)
            .clip(shape)
            .background(bg)
            .border(1.dp, if (selected) Regla.Coral else Regla.Line, shape)
            .clickable(enabled = slot.enabled, onClick = onClick)
            .padding(vertical = 10.dp)
            .testTag("slot-${slot.label}"),
    )
}
