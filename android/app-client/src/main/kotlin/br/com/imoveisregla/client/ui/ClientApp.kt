package br.com.imoveisregla.client.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import br.com.imoveisregla.client.LocalAppContainer
import br.com.imoveisregla.client.feature.apply.ApplyScreen
import br.com.imoveisregla.client.feature.auth.LoginScreen
import br.com.imoveisregla.client.feature.auth.SignupScreen
import br.com.imoveisregla.client.feature.favorites.FavoritesScreen
import br.com.imoveisregla.client.feature.listing.ListingDetailScreen
import br.com.imoveisregla.client.feature.profile.DocumentsScreen
import br.com.imoveisregla.client.feature.profile.EditProfileScreen
import br.com.imoveisregla.client.feature.profile.ProfileScreen
import br.com.imoveisregla.client.feature.proposals.ProposalsScreen
import br.com.imoveisregla.client.feature.search.SearchScreen
import br.com.imoveisregla.client.feature.visit.BookVisitScreen
import br.com.imoveisregla.client.feature.visits.VisitsScreen
import br.com.imoveisregla.client.nav.ApplyRoute
import br.com.imoveisregla.client.nav.BookVisitRoute
import br.com.imoveisregla.client.nav.DocumentsRoute
import br.com.imoveisregla.client.nav.EditProfileRoute
import br.com.imoveisregla.client.nav.FavoritesRoute
import br.com.imoveisregla.client.nav.ListingDetailRoute
import br.com.imoveisregla.client.nav.LoginRoute
import br.com.imoveisregla.client.nav.ProfileRoute
import br.com.imoveisregla.client.nav.ProposalsRoute
import br.com.imoveisregla.client.nav.SearchRoute
import br.com.imoveisregla.client.nav.SignupRoute
import br.com.imoveisregla.client.nav.VisitsRoute
import br.com.imoveisregla.core.designsystem.DemoBanner
import br.com.imoveisregla.core.designsystem.Regla

/** Selects a bottom tab: single top, saving/restoring each tab's back stack. */
internal fun NavHostController.navigateToTab(tab: ClientTab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

internal fun NavDestination?.isTab(tab: ClientTab): Boolean =
    this?.hierarchy?.any { it.hasRoute(tab.route::class) } == true

/** App shell: bottom navigation + NavHost. Owned by the auth/shell feature. */
@Composable
fun ClientApp(nav: NavHostController = rememberNavController()) {
    val container = LocalAppContainer.current
    val backStackEntry by nav.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val showBottomBar = destination == null || ClientTab.entries.any { destination.isTab(it) }

    val openListing: (Long) -> Unit = { nav.navigate(ListingDetailRoute(it)) }
    val requireLogin: () -> Unit = { nav.navigate(LoginRoute) { launchSingleTop = true } }

    Scaffold(
        topBar = {
            if (!container.isLive) {
                Box(Modifier.background(Regla.WarnSoft).statusBarsPadding()) { DemoBanner() }
            }
        },
        bottomBar = {
            if (showBottomBar) {
                ClientBottomBar(
                    isSelected = { destination.isTab(it) },
                    onSelect = { nav.navigateToTab(it) },
                )
            }
        },
    ) { padding ->
        NavHost(
            nav,
            startDestination = SearchRoute,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            composable<SearchRoute> { SearchScreen(onOpenListing = openListing) }
            composable<FavoritesRoute> { FavoritesScreen(onOpenListing = openListing, onRequireLogin = requireLogin) }
            composable<VisitsRoute> { VisitsScreen(onOpenListing = openListing, onRequireLogin = requireLogin) }
            composable<ProposalsRoute> {
                ProposalsScreen(
                    onOpenListing = openListing, onRequireLogin = requireLogin,
                    onOpenDocuments = { nav.navigate(DocumentsRoute) },
                )
            }
            composable<ProfileRoute> {
                ProfileScreen(
                    onEditProfile = { nav.navigate(EditProfileRoute) },
                    onDocuments = { nav.navigate(DocumentsRoute) },
                    onLogin = requireLogin,
                )
            }
            composable<ListingDetailRoute> { entry ->
                val r = entry.toRoute<ListingDetailRoute>()
                ListingDetailScreen(
                    listingId = r.id, onBack = { nav.popBackStack() },
                    onBookVisit = { nav.navigate(BookVisitRoute(r.id)) },
                    onApply = { nav.navigate(ApplyRoute(r.id)) },
                    onRequireLogin = requireLogin,
                )
            }
            composable<BookVisitRoute> { entry ->
                val r = entry.toRoute<BookVisitRoute>()
                BookVisitScreen(
                    listingId = r.listingId, onBack = { nav.popBackStack() },
                    onDone = { nav.navigateToTab(ClientTab.Visits) },
                )
            }
            composable<ApplyRoute> { entry ->
                val r = entry.toRoute<ApplyRoute>()
                ApplyScreen(
                    listingId = r.listingId, onBack = { nav.popBackStack() },
                    onDone = { nav.navigateToTab(ClientTab.Proposals) },
                )
            }
            composable<LoginRoute> {
                LoginScreen(onLoggedIn = { nav.popBackStack() }, onSignup = { nav.navigate(SignupRoute) }, onBack = { nav.popBackStack() })
            }
            composable<SignupRoute> {
                SignupScreen(onSignedUp = { nav.popBackStack(LoginRoute, inclusive = true) }, onBack = { nav.popBackStack() })
            }
            composable<EditProfileRoute> { EditProfileScreen(onBack = { nav.popBackStack() }) }
            composable<DocumentsRoute> { DocumentsScreen(onBack = { nav.popBackStack() }) }
        }
    }
}
