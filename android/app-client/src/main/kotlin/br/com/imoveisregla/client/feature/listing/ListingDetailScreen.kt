package br.com.imoveisregla.client.feature.listing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.LocationOn
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.imoveisregla.client.LocalAppContainer
import br.com.imoveisregla.client.feature.search.FavoriteOverlayButton
import br.com.imoveisregla.client.feature.search.displayPrice
import br.com.imoveisregla.core.data.SessionState
import br.com.imoveisregla.core.designsystem.ButtonKind
import br.com.imoveisregla.core.designsystem.EmptyState
import br.com.imoveisregla.core.designsystem.ErrorState
import br.com.imoveisregla.core.designsystem.ListingImage
import br.com.imoveisregla.core.designsystem.LoadingState
import br.com.imoveisregla.core.designsystem.Regla
import br.com.imoveisregla.core.designsystem.ReglaButton
import br.com.imoveisregla.core.designsystem.SectionTitle
import br.com.imoveisregla.core.designsystem.StatusPill
import br.com.imoveisregla.core.model.Listing
import br.com.imoveisregla.core.model.formatArea
import br.com.imoveisregla.core.model.label
import br.com.imoveisregla.core.model.listingRef
import br.com.imoveisregla.core.model.waInterest

@Composable
fun ListingDetailScreen(listingId: Long, onBack: () -> Unit, onBookVisit: () -> Unit, onApply: () -> Unit, onRequireLogin: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: ListingDetailViewModel = viewModel(key = "listing-$listingId") { ListingDetailViewModel(container, listingId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val session by container.auth.session.collectAsStateWithLifecycle()
    val signedOut = session is SessionState.SignedOut
    val snackbar = remember { SnackbarHostState() }

    LifecycleResumeEffect(Unit) {
        vm.refreshFavorite()
        onPauseOrDispose { }
    }
    val message = (state as? ListingDetailUiState.Loaded)?.message
    LaunchedEffect(message) {
        if (message != null) {
            vm.messageShown()
            snackbar.showSnackbar(message)
        }
    }

    when (val s = state) {
        ListingDetailUiState.Loading -> LoadingState()
        ListingDetailUiState.NotFound -> EmptyState(
            title = "Imóvel não encontrado",
            message = "Este anúncio pode ter sido removido ou não está mais disponível.",
            action = { ReglaButton("Voltar", onBack, kind = ButtonKind.Secondary) },
        )
        is ListingDetailUiState.Error -> ErrorState(message = s.message, onRetry = vm::load)
        is ListingDetailUiState.Loaded -> Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = Regla.Surface,
            bottomBar = {
                DetailBottomBar(
                    listing = s.listing,
                    onBookVisit = if (signedOut) onRequireLogin else onBookVisit,
                    onApply = if (signedOut) onRequireLogin else onApply,
                )
            },
        ) { padding ->
            DetailContent(
                listing = s.listing,
                photoUrls = s.photoUrls,
                isFavorite = s.isFavorite,
                onBack = onBack,
                onToggleFavorite = { if (signedOut) onRequireLogin() else vm.toggleFavorite() },
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailContent(
    listing: Listing,
    photoUrls: List<String>,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PhotoCarousel(photoUrls, listing.title, isFavorite, onBack, onToggleFavorite)

        Column(Modifier.padding(horizontal = Regla.Gutter, vertical = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusPill(
                    text = if (listing.isRental) "Aluguel" else "Venda",
                    color = Regla.Coral,
                    background = Regla.CoralSoft,
                )
                Spacer(Modifier.weight(1f))
                Text("Ref. ${listingRef(listing.id)}", style = MaterialTheme.typography.labelMedium, color = Regla.Muted)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                listing.displayPrice(),
                style = MaterialTheme.typography.headlineMedium,
                color = Regla.Navy,
            )
            Spacer(Modifier.height(6.dp))
            Text(listing.title, style = MaterialTheme.typography.titleLarge, color = Regla.Ink)
            Spacer(Modifier.height(4.dp))
            Text(
                listOf(listing.neighborhood, listing.city).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = Regla.Muted,
            )

            Spacer(Modifier.height(20.dp))
            SpecsGrid(listing)

            if (listing.tags.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listing.tags.forEach { StatusPill(it, color = Regla.Navy, background = Regla.NavySoft) }
                }
            }

            val description = listing.description.ifBlank { listing.summary }
            if (description.isNotBlank()) {
                Spacer(Modifier.height(24.dp))
                SectionTitle("Descrição")
                Spacer(Modifier.height(8.dp))
                Text(description, style = MaterialTheme.typography.bodyLarge, color = Regla.Ink)
            }

            Spacer(Modifier.height(24.dp))
            SectionTitle("Sobre a região")
            Spacer(Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Regla.RadiusCard))
                    .background(Regla.NavySoft)
                    .padding(16.dp),
            ) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(Regla.Surface),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Regla.Coral)
                }
                Spacer(Modifier.size(12.dp))
                Column {
                    Text(listing.neighborhood.ifBlank { listing.city }, style = MaterialTheme.typography.titleMedium, color = Regla.Ink)
                    Text(
                        listOf(listing.city, listing.country).filter { it.isNotBlank() }.joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Regla.Muted,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            TextButton(
                onClick = { uriHandler.openUri(waInterest(listing.title, listingRef(listing.id))) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Falar no WhatsApp", color = Regla.Ok, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PhotoCarousel(
    photoUrls: List<String>,
    title: String,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val pageCount = photoUrls.size.coerceAtLeast(1)
    val pager = rememberPagerState { pageCount }
    Box(Modifier.fillMaxWidth().aspectRatio(4f / 3f)) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            ListingImage(
                url = photoUrls.getOrNull(page),
                contentDescription = "$title — foto ${page + 1}",
                modifier = Modifier.fillMaxSize(),
            )
        }
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.92f)),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Regla.Ink)
        }
        FavoriteOverlayButton(
            isFavorite = isFavorite,
            onClick = onToggleFavorite,
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
        )
        if (pageCount > 1) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                repeat(pageCount) { i ->
                    val active = i == pager.currentPage
                    Box(
                        Modifier
                            .size(if (active) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (active) Color.White else Color.White.copy(alpha = 0.55f)),
                    )
                }
            }
            Text(
                "${pager.currentPage + 1}/$pageCount",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}

@Composable
private fun SpecsGrid(listing: Listing) {
    val specs = listOf(
        "Tipo" to listing.type.label,
        "Quartos" to listing.beds.toString(),
        "Banheiros" to listing.baths.toString(),
        "Área" to formatArea(listing.areaM2),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        specs.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (label, value) ->
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(Regla.RadiusControl))
                            .background(Regla.NavySoft)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        Text(label, style = MaterialTheme.typography.labelMedium, color = Regla.Muted)
                        Spacer(Modifier.height(2.dp))
                        Text(value, style = MaterialTheme.typography.titleMedium, color = Regla.Ink)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailBottomBar(listing: Listing, onBookVisit: () -> Unit, onApply: () -> Unit) {
    Surface(color = Regla.Surface, shadowElevation = 8.dp) {
        Column {
            HorizontalDivider(color = Regla.Line)
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = Regla.Gutter, vertical = 12.dp),
            ) {
                ReglaButton(
                    text = "Agendar visita",
                    onClick = onBookVisit,
                    kind = ButtonKind.Secondary,
                    modifier = Modifier.weight(1f),
                )
                ReglaButton(
                    text = "Fazer proposta",
                    onClick = onApply,
                    kind = ButtonKind.Accent,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
