package br.com.imoveisregla.client.feature.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.imoveisregla.client.LocalAppContainer
import br.com.imoveisregla.client.feature.search.BrowseListingCard
import br.com.imoveisregla.core.designsystem.EmptyState
import br.com.imoveisregla.core.designsystem.ErrorState
import br.com.imoveisregla.core.designsystem.LoadingState
import br.com.imoveisregla.core.designsystem.Regla
import br.com.imoveisregla.core.designsystem.ReglaButton

@Composable
fun FavoritesScreen(onOpenListing: (Long) -> Unit, onRequireLogin: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: FavoritesViewModel = viewModel { FavoritesViewModel(container) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LifecycleResumeEffect(Unit) {
        vm.load()
        onPauseOrDispose { }
    }
    val message = (state as? FavoritesUiState.Loaded)?.message
    LaunchedEffect(message) {
        if (message != null) {
            vm.messageShown()
            snackbar.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Regla.NavySoft,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(
                "Favoritos",
                style = MaterialTheme.typography.headlineLarge,
                color = Regla.Navy,
                modifier = Modifier.padding(start = Regla.Gutter, end = Regla.Gutter, top = 20.dp),
            )
            Spacer(Modifier.height(12.dp))
            when (val s = state) {
                FavoritesUiState.Loading -> LoadingState()
                FavoritesUiState.SignedOut -> EmptyState(
                    title = "Salve os imóveis que você amou",
                    message = "Entre na sua conta para guardar favoritos e acessá-los em qualquer lugar.",
                    action = { ReglaButton("Entrar", onRequireLogin) },
                )
                is FavoritesUiState.Error -> ErrorState(message = s.message, onRetry = vm::load)
                is FavoritesUiState.Loaded -> if (s.listings.isEmpty()) {
                    EmptyState(
                        title = "Nenhum favorito ainda",
                        message = "Toque no coração de um imóvel para salvá-lo aqui.",
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = Regla.Gutter, end = Regla.Gutter, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item(key = "count") {
                            Text(
                                if (s.listings.size == 1) "1 imóvel salvo" else "${s.listings.size} imóveis salvos",
                                style = MaterialTheme.typography.labelLarge,
                                color = Regla.Muted,
                            )
                        }
                        items(s.listings, key = { it.id }) { listing ->
                            BrowseListingCard(
                                listing = listing,
                                photoUrl = listing.photos.minByOrNull { it.position }
                                    ?.let { container.listings.photoUrl(it.storagePath) },
                                isFavorite = true,
                                onClick = { onOpenListing(listing.id) },
                                onToggleFavorite = { vm.remove(listing.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}
