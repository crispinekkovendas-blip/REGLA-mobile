package br.com.imoveisregla.realtor.feature.listings

import android.content.Intent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.imoveisregla.core.designsystem.EmptyState
import br.com.imoveisregla.core.designsystem.ErrorState
import br.com.imoveisregla.core.designsystem.ListingImage
import br.com.imoveisregla.core.designsystem.LoadingState
import br.com.imoveisregla.core.designsystem.Regla
import br.com.imoveisregla.core.designsystem.SectionTitle
import br.com.imoveisregla.core.designsystem.StatusPill
import br.com.imoveisregla.core.model.Listing
import br.com.imoveisregla.core.model.ListingStatus
import br.com.imoveisregla.core.model.formatArea
import br.com.imoveisregla.core.model.label
import br.com.imoveisregla.core.model.listingRef
import br.com.imoveisregla.realtor.LocalAppContainer
import br.com.imoveisregla.realtor.feature.leads.CountChip
import br.com.imoveisregla.realtor.feature.leads.SelectChip

@Composable
internal fun ListingStatusPill(status: ListingStatus, modifier: Modifier = Modifier) {
    val (fg, bg) = statusColors(status)
    StatusPill(status.label, fg, bg, modifier)
}

// ───────────────────────────── Listings list ─────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingsScreen(onOpenListing: (Long) -> Unit) {
    val container = LocalAppContainer.current
    val vm = viewModel { ListingsViewModel(container.listings) }
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        Text(
            "Imóveis",
            style = MaterialTheme.typography.headlineMedium,
            color = Regla.Ink,
            modifier = Modifier.padding(start = Regla.Gutter, end = Regla.Gutter, top = 16.dp, bottom = 8.dp),
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = Regla.Gutter),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CountChip("Todos", state.all.size, state.status == null) { vm.setStatusFilter(null) }
            STATUS_ORDER.forEach { s ->
                CountChip(s.label, state.counts[s] ?: 0, state.status == s) { vm.setStatusFilter(s) }
            }
        }
        OutlinedTextField(
            value = state.query,
            onValueChange = vm::setQuery,
            placeholder = { Text("Buscar por título, bairro ou ref.") },
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
                                val status = state.status
                                EmptyState(
                                    title = if (state.query.isNotBlank()) "Nenhum resultado"
                                    else if (status != null) "Nenhum imóvel \"${status.label}\""
                                    else "Nenhum imóvel cadastrado",
                                    message = if (state.query.isNotBlank()) "Tente outra busca."
                                    else "Os imóveis aparecerão aqui.",
                                    modifier = Modifier.fillParentMaxSize(),
                                )
                            }
                        } else {
                            items(visible, key = { it.id }) { l ->
                                ListingRow(l, container.listings::photoUrl, onClick = { onOpenListing(l.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ListingRow(l: Listing, photoUrl: (String) -> String, onClick: () -> Unit) {
    val cover = l.photos.minByOrNull { it.position }
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(Regla.RadiusCard),
        colors = CardDefaults.cardColors(containerColor = Regla.Surface),
        border = BorderStroke(1.dp, Regla.Line),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            ListingImage(
                url = cover?.let { photoUrl(it.storagePath) },
                contentDescription = l.title,
                modifier = Modifier.size(84.dp).clip(RoundedCornerShape(Regla.RadiusControl)),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    l.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Regla.Ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${listingRef(l.id)} · ${l.neighborhood}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Regla.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        listingPrice(l),
                        style = MaterialTheme.typography.labelLarge,
                        color = Regla.Navy,
                        modifier = Modifier.weight(1f),
                    )
                    ListingStatusPill(l.status)
                }
            }
        }
    }
}

// ───────────────────────────── Listing detail ─────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingDetailScreen(listingId: Long, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val vm = viewModel(key = "listing-$listingId") { ListingDetailViewModel(listingId, container.listings) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    var pending by remember { mutableStateOf<ListingStatus?>(null) }

    LaunchedEffect(state.message) {
        val msg = state.message
        if (msg != null) {
            snackbar.showSnackbar(msg)
            vm.messageShown()
        }
    }

    fun share(l: Listing) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, l.title)
            putExtra(Intent.EXTRA_TEXT, shareText(l))
        }
        try {
            context.startActivity(Intent.createChooser(send, "Compartilhar imóvel"))
        } catch (e: Exception) {
            vm.showMessage("Nenhum app disponível para compartilhar")
        }
    }

    fun openSite(l: Listing) {
        try {
            uriHandler.openUri(publicListingUrl(l.slug))
        } catch (e: Exception) {
            vm.showMessage("Nenhum navegador disponível")
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Imóvel") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    val l = state.listing
                    if (l != null) {
                        IconButton(onClick = { share(l) }) {
                            Icon(Icons.Outlined.Share, contentDescription = "Compartilhar")
                        }
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Regla.Surface),
            )
        },
    ) { padding ->
        val listing = state.listing
        Column(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.loading && listing == null -> LoadingState()
                listing == null -> ErrorState(state.error ?: "Imóvel não encontrado", onRetry = { vm.load() })
                else -> ListingDetailContent(
                    l = listing,
                    saving = state.saving,
                    photoUrl = vm::photoUrl,
                    onStatus = { s -> if (needsConfirmation(s)) { pending = s } else { vm.setStatus(s) } },
                    onShare = { share(listing) },
                    onOpenSite = { openSite(listing) },
                )
            }
        }
    }

    val confirm = pending
    if (confirm != null) {
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Marcar como ${confirm.label.lowercase()}?") },
            text = {
                Text(
                    if (confirm == ListingStatus.SOLD) "O imóvel sairá do site e será registrado como vendido."
                    else "O imóvel deixará de aparecer no site e no app para clientes.",
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.setStatus(confirm); pending = null }) { Text("Confirmar") }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun ListingDetailContent(
    l: Listing,
    saving: Boolean,
    photoUrl: (String) -> String,
    onStatus: (ListingStatus) -> Unit,
    onShare: () -> Unit,
    onOpenSite: () -> Unit,
) {
    val photos = l.photos.sortedBy { it.position }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            if (photos.isEmpty()) {
                ListingImage(null, l.title, Modifier.fillMaxWidth().height(240.dp))
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(photos, key = { it.id }) { p ->
                        ListingImage(
                            url = photoUrl(p.storagePath),
                            contentDescription = p.altText ?: l.title,
                            modifier = Modifier.fillParentMaxWidth(if (photos.size > 1) 0.92f else 1f).height(240.dp),
                        )
                    }
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = Regla.Gutter)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${listingRef(l.id)} · ${l.type.label}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Regla.Muted,
                        modifier = Modifier.weight(1f),
                    )
                    ListingStatusPill(l.status)
                }
                Spacer(Modifier.height(6.dp))
                Text(l.title, style = MaterialTheme.typography.headlineMedium, color = Regla.Ink)
                Text("${l.neighborhood}, ${l.city}", style = MaterialTheme.typography.bodyMedium, color = Regla.Muted)
                Spacer(Modifier.height(8.dp))
                Text(listingPrice(l), style = MaterialTheme.typography.titleLarge, color = Regla.Navy)
                Spacer(Modifier.height(6.dp))
                Text(specsLine(l), style = MaterialTheme.typography.bodyMedium, color = Regla.Ink)
                if (l.tags.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        l.tags.forEach { StatusPill(it, Regla.Navy, Regla.NavySoft) }
                    }
                }
            }
        }
        item {
            Row(Modifier.padding(horizontal = Regla.Gutter), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f).height(48.dp)) {
                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Compartilhar")
                }
                OutlinedButton(onClick = onOpenSite, modifier = Modifier.weight(1f).height(48.dp)) {
                    Icon(Icons.Outlined.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Ver no site")
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = Regla.Gutter)) {
                SectionTitle("Status do anúncio")
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    STATUS_ORDER.forEach { s ->
                        SelectChip(s.label, selected = l.status == s, enabled = !saving) { onStatus(s) }
                    }
                }
            }
        }
        val text = l.description.ifBlank { l.summary }
        if (text.isNotBlank()) {
            item {
                Column(Modifier.padding(horizontal = Regla.Gutter)) {
                    SectionTitle("Descrição")
                    Spacer(Modifier.height(6.dp))
                    Text(text, style = MaterialTheme.typography.bodyLarge, color = Regla.Ink)
                }
            }
        }
    }
}

internal fun specsLine(l: Listing): String = buildList {
    if (l.beds > 0) add(if (l.beds == 1) "1 quarto" else "${l.beds} quartos")
    if (l.baths > 0) add(if (l.baths == 1) "1 banheiro" else "${l.baths} banheiros")
    add(formatArea(l.areaM2))
}.joinToString(" · ")
