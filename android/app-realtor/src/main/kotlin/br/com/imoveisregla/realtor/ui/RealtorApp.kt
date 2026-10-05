package br.com.imoveisregla.realtor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import br.com.imoveisregla.core.designsystem.ButtonKind
import br.com.imoveisregla.core.designsystem.DemoBanner
import br.com.imoveisregla.core.designsystem.ErrorState
import br.com.imoveisregla.core.designsystem.LoadingState
import br.com.imoveisregla.core.designsystem.Regla
import br.com.imoveisregla.core.designsystem.ReglaButton
import br.com.imoveisregla.realtor.LocalAppContainer
import br.com.imoveisregla.realtor.feature.agenda.AgendaScreen
import br.com.imoveisregla.realtor.feature.auth.LoginScreen
import br.com.imoveisregla.realtor.feature.dashboard.DashboardScreen
import br.com.imoveisregla.realtor.feature.leads.LeadDetailScreen
import br.com.imoveisregla.realtor.feature.leads.LeadsScreen
import br.com.imoveisregla.realtor.feature.listings.ListingDetailScreen
import br.com.imoveisregla.realtor.feature.listings.ListingsScreen
import br.com.imoveisregla.realtor.feature.proposals.ApplicationDetailScreen
import br.com.imoveisregla.realtor.feature.proposals.ProposalsScreen
import br.com.imoveisregla.realtor.nav.AgendaRoute
import br.com.imoveisregla.realtor.nav.ApplicationDetailRoute
import br.com.imoveisregla.realtor.nav.DashboardRoute
import br.com.imoveisregla.realtor.nav.LeadDetailRoute
import br.com.imoveisregla.realtor.nav.LeadsRoute
import br.com.imoveisregla.realtor.nav.ListingDetailRoute
import br.com.imoveisregla.realtor.nav.ListingsRoute
import br.com.imoveisregla.realtor.nav.ProposalsRoute

/** App shell: auth gate + bottom navigation + NavHost. Owned by the shell/dashboard feature. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RealtorApp() {
    val container = LocalAppContainer.current
    val gate = viewModel { AuthGateViewModel(container.auth) }
    val state by gate.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().background(Regla.NavySoft)) {
        val showBanner = !container.isLive && state !is GateState.Ready
        if (showBanner) DemoBanner(Modifier.statusBarsPadding())
        Box(
            Modifier.weight(1f).fillMaxWidth()
                .then(if (showBanner) Modifier.consumeWindowInsets(WindowInsets.statusBars) else Modifier),
        ) {
            when (val s = state) {
                GateState.Loading -> LoadingState()
                GateState.LoginRequired -> LoginScreen(onLoggedIn = {})
                is GateState.Restricted -> RestrictedScreen(email = s.email, onSignOut = gate::signOut)
                is GateState.Failed -> Box(Modifier.safeDrawingPadding()) {
                    ErrorState(message = s.message, onRetry = gate::retry)
                }
                is GateState.Ready -> RealtorMain(showDemoBanner = !container.isLive)
            }
        }
    }
}

private class Tab(val label: String, val icon: ImageVector, val route: Any, val matches: (NavDestination) -> Boolean)

private val Tabs = listOf(
    Tab("Início", Icons.Outlined.Home, DashboardRoute, { it.hasRoute<DashboardRoute>() }),
    Tab("Leads", Icons.Outlined.People, LeadsRoute, { it.hasRoute<LeadsRoute>() }),
    Tab("Propostas", Icons.Outlined.Description, ProposalsRoute, { it.hasRoute<ProposalsRoute>() }),
    Tab("Agenda", Icons.Outlined.CalendarMonth, AgendaRoute, { it.hasRoute<AgendaRoute>() }),
    Tab("Imóveis", Icons.Outlined.HomeWork, ListingsRoute, { it.hasRoute<ListingsRoute>() }),
)

private fun NavDestination?.isTab(tab: Tab): Boolean =
    this?.hierarchy?.any { tab.matches(it) } == true

private fun NavHostController.navigateToTab(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun RealtorMain(showDemoBanner: Boolean) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val destination = backStack?.destination
    // Before the first destination is attached, assume the start tab (Início).
    val onTab = destination == null || Tabs.any { destination.isTab(it) }

    val openLead: (Long) -> Unit = { nav.navigate(LeadDetailRoute(it)) }
    val openApplication: (Long) -> Unit = { nav.navigate(ApplicationDetailRoute(it)) }
    val openListing: (Long) -> Unit = { nav.navigate(ListingDetailRoute(it)) }

    Scaffold(
        containerColor = Regla.NavySoft,
        topBar = { if (showDemoBanner) DemoBanner(Modifier.statusBarsPadding()) },
        bottomBar = {
            if (onTab) {
                NavigationBar(containerColor = Regla.Navy, contentColor = Color.White) {
                    Tabs.forEach { tab ->
                        val selected = if (destination == null) tab.route == DashboardRoute else destination.isTab(tab)
                        NavigationBarItem(
                            selected = selected,
                            onClick = { if (!selected) nav.navigateToTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Regla.Coral,
                                selectedTextColor = Color.White,
                                indicatorColor = Color.White.copy(alpha = 0.12f),
                                unselectedIconColor = Color.White.copy(alpha = 0.65f),
                                unselectedTextColor = Color.White.copy(alpha = 0.65f),
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = DashboardRoute, modifier = Modifier.padding(padding)) {
            composable<DashboardRoute> {
                DashboardScreen(onOpenLead = openLead, onOpenApplication = openApplication, onOpenAgenda = { nav.navigateToTab(AgendaRoute) })
            }
            composable<LeadsRoute> { LeadsScreen(onOpenLead = openLead) }
            composable<ProposalsRoute> { ProposalsScreen(onOpenApplication = openApplication) }
            composable<AgendaRoute> { AgendaScreen(onOpenListing = openListing) }
            composable<ListingsRoute> { ListingsScreen(onOpenListing = openListing) }
            composable<LeadDetailRoute> { e ->
                LeadDetailScreen(leadId = e.toRoute<LeadDetailRoute>().id, onBack = { nav.popBackStack() }, onOpenListing = openListing)
            }
            composable<ApplicationDetailRoute> { e ->
                ApplicationDetailScreen(
                    applicationId = e.toRoute<ApplicationDetailRoute>().id, onBack = { nav.popBackStack() }, onOpenListing = openListing,
                )
            }
            composable<ListingDetailRoute> { e ->
                ListingDetailScreen(listingId = e.toRoute<ListingDetailRoute>().id, onBack = { nav.popBackStack() })
            }
        }
    }
}

@Composable
private fun RestrictedScreen(email: String, onSignOut: () -> Unit) {
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(72.dp).background(Regla.CoralSoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = Regla.Coral, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Acesso restrito a corretores REGLA",
            style = MaterialTheme.typography.titleLarge,
            color = Regla.Ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "A conta $email não tem permissão de corretor. Se você é cliente, use o app REGLA Imóveis.",
            color = Regla.Muted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        ReglaButton("Sair", onSignOut, kind = ButtonKind.Secondary, modifier = Modifier.fillMaxWidth())
    }
}
