package br.com.imoveisregla.client.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
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

/** App shell: bottom navigation + NavHost. Owned by the auth/shell feature. */
@Composable
fun ClientApp() {
    val nav = rememberNavController()
    val openListing: (Long) -> Unit = { nav.navigate(ListingDetailRoute(it)) }
    val requireLogin: () -> Unit = { nav.navigate(LoginRoute) }

    Scaffold { padding ->
        NavHost(nav, startDestination = SearchRoute, modifier = Modifier.padding(padding)) {
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
                BookVisitScreen(listingId = r.listingId, onBack = { nav.popBackStack() }, onDone = { nav.navigate(VisitsRoute) })
            }
            composable<ApplyRoute> { entry ->
                val r = entry.toRoute<ApplyRoute>()
                ApplyScreen(listingId = r.listingId, onBack = { nav.popBackStack() }, onDone = { nav.navigate(ProposalsRoute) })
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
