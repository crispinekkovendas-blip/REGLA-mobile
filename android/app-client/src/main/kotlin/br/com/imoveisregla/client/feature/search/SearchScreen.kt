package br.com.imoveisregla.client.feature.search

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.imoveisregla.client.LocalAppContainer
import br.com.imoveisregla.core.designsystem.ButtonKind
import br.com.imoveisregla.core.designsystem.DemoBanner
import br.com.imoveisregla.core.designsystem.EmptyState
import br.com.imoveisregla.core.designsystem.ErrorState
import br.com.imoveisregla.core.designsystem.LoadingState
import br.com.imoveisregla.core.designsystem.Regla
import br.com.imoveisregla.core.designsystem.ReglaButton
import br.com.imoveisregla.core.model.ListingType
import br.com.imoveisregla.core.model.formatPrice
import br.com.imoveisregla.core.model.Currency
import br.com.imoveisregla.core.model.label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(onOpenListing: (Long) -> Unit) {
    val container = LocalAppContainer.current
    val vm: SearchViewModel = viewModel { SearchViewModel(container) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        val msg = state.message ?: return@LaunchedEffect
        vm.messageShown()
        snackbar.showSnackbar(msg)
    }
    LifecycleResumeEffect(Unit) {
        vm.refreshFavorites()
        onPauseOrDispose { }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Regla.NavySoft,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (!container.isLive) DemoBanner()
            SearchHeader(
                state = state,
                onQuery = vm::setQuery,
                onType = vm::setType,
                onBeds = vm::setMinBeds,
                onMaxPrice = vm::setMaxPrice,
                onCity = vm::setCity,
            )
            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = vm::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    state.loading && state.listings.isEmpty() -> LoadingState()
                    state.error != null && state.listings.isEmpty() ->
                        ErrorState(message = state.error!!, onRetry = vm::retry)
                    state.listings.isEmpty() -> EmptyState(
                        title = "Nenhum imóvel encontrado",
                        message = "Tente outra busca ou remova alguns filtros.",
                        action = { ReglaButton("Limpar filtros", vm::clearFilters, kind = ButtonKind.Secondary) },
                    )
                    else -> LazyColumn(
                        contentPadding = PaddingValues(start = Regla.Gutter, end = Regla.Gutter, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize().testTag("search_results"),
                    ) {
                        item(key = "count") {
                            Text(
                                resultsLabel(state.listings.size),
                                style = MaterialTheme.typography.labelLarge,
                                color = Regla.Muted,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        items(state.listings, key = { it.id }) { listing ->
                            BrowseListingCard(
                                listing = listing,
                                photoUrl = listing.photos.minByOrNull { it.position }
                                    ?.let { container.listings.photoUrl(it.storagePath) },
                                isFavorite = listing.id in state.favoriteIds,
                                onClick = { onOpenListing(listing.id) },
                                onToggleFavorite = { vm.toggleFavorite(listing.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun resultsLabel(n: Int): String = when (n) {
    0 -> "Nenhum imóvel encontrado"
    1 -> "1 imóvel encontrado"
    else -> "$n imóveis encontrados"
}

internal fun priceOptionLabel(max: Long?): String =
    if (max == null) "Sem limite" else "Até ${formatPrice(max, Currency.BRL)}"

internal fun bedsOptionLabel(min: Int?): String = if (min == null) "Qualquer" else "$min+ quartos"

@Composable
private fun SearchHeader(
    state: SearchUiState,
    onQuery: (String) -> Unit,
    onType: (ListingType?) -> Unit,
    onBeds: (Int?) -> Unit,
    onMaxPrice: (Long?) -> Unit,
    onCity: (String?) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 12.dp)) {
        Text(
            "Encontre seu próximo lar",
            style = MaterialTheme.typography.headlineLarge,
            color = Regla.Navy,
            modifier = Modifier.padding(horizontal = Regla.Gutter),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Aluguel e venda com a curadoria REGLA",
            style = MaterialTheme.typography.bodyMedium,
            color = Regla.Muted,
            modifier = Modifier.padding(horizontal = Regla.Gutter),
        )
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = state.query,
            onValueChange = onQuery,
            placeholder = { Text("Bairro, cidade ou título") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = Regla.Muted) },
            trailingIcon = if (state.query.isNotEmpty()) {
                { IconButton(onClick = { onQuery("") }) { Icon(Icons.Filled.Close, contentDescription = "Limpar busca") } }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(Regla.RadiusControl),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Regla.Surface,
                unfocusedContainerColor = Regla.Surface,
                unfocusedBorderColor = Regla.Line,
                focusedBorderColor = Regla.Navy,
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = Regla.Gutter).testTag("search_field"),
        )
        Spacer(Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = Regla.Gutter),
        ) {
            val f = state.filters
            DropdownChip(
                label = f.type?.label ?: "Tipo",
                selected = f.type != null,
                options = listOf<ListingType?>(null) + ListingType.entries,
                optionLabel = { it?.label ?: "Todos os tipos" },
                onSelect = onType,
            )
            DropdownChip(
                label = f.minBeds?.let { "$it+ quartos" } ?: "Quartos",
                selected = f.minBeds != null,
                options = BED_OPTIONS,
                optionLabel = ::bedsOptionLabel,
                onSelect = onBeds,
            )
            DropdownChip(
                label = f.maxPrice?.let { priceOptionLabel(it) } ?: "Preço máx",
                selected = f.maxPrice != null,
                options = PRICE_OPTIONS,
                optionLabel = ::priceOptionLabel,
                onSelect = onMaxPrice,
            )
            DropdownChip(
                label = f.city ?: "Cidade",
                selected = f.city != null,
                options = listOf<String?>(null) + state.cities,
                optionLabel = { it ?: "Todas as cidades" },
                onSelect = onCity,
            )
        }
    }
}

@Composable
private fun <T> DropdownChip(
    label: String,
    selected: Boolean,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selected,
            onClick = { open = true },
            label = { Text(label, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            colors = FilterChipDefaults.filterChipColors(
                containerColor = Regla.Surface,
                selectedContainerColor = Regla.Navy,
                selectedLabelColor = Regla.Surface,
                selectedTrailingIconColor = Regla.Surface,
            ),
            shape = RoundedCornerShape(50),
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        open = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}
