package br.com.imoveisregla.client.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.imoveisregla.client.nav.FavoritesRoute
import br.com.imoveisregla.client.nav.ProfileRoute
import br.com.imoveisregla.client.nav.ProposalsRoute
import br.com.imoveisregla.client.nav.SearchRoute
import br.com.imoveisregla.client.nav.VisitsRoute
import br.com.imoveisregla.core.designsystem.Regla

/** The five bottom tabs of the client app. */
enum class ClientTab(
    val route: Any,
    val label: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector,
    val testTag: String,
) {
    Search(SearchRoute, "Buscar", Icons.Filled.Search, Icons.Outlined.Search, "tab_buscar"),
    Favorites(FavoritesRoute, "Favoritos", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder, "tab_favoritos"),
    Visits(VisitsRoute, "Visitas", Icons.Filled.Event, Icons.Outlined.Event, "tab_visitas"),
    Proposals(ProposalsRoute, "Propostas", Icons.Filled.Description, Icons.Outlined.Description, "tab_propostas"),
    Profile(ProfileRoute, "Perfil", Icons.Filled.Person, Icons.Outlined.Person, "tab_perfil"),
}

const val BOTTOM_BAR_TAG = "client_bottom_bar"

@Composable
internal fun ClientBottomBar(
    isSelected: (ClientTab) -> Boolean,
    onSelect: (ClientTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.testTag(BOTTOM_BAR_TAG)) {
        HorizontalDivider(color = Regla.Line)
        NavigationBar(containerColor = Regla.Surface, tonalElevation = 0.dp) {
            ClientTab.entries.forEach { tab ->
                val selected = isSelected(tab)
                NavigationBarItem(
                    selected = selected,
                    onClick = { if (!selected) onSelect(tab) },
                    icon = { Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null) },
                    label = {
                        Text(
                            tab.label,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = 1,
                        )
                    },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Regla.Coral,
                        selectedTextColor = Regla.Navy,
                        indicatorColor = Regla.CoralSoft,
                        unselectedIconColor = Regla.Muted,
                        unselectedTextColor = Regla.Muted,
                    ),
                    modifier = Modifier.testTag(tab.testTag),
                )
            }
        }
    }
}

