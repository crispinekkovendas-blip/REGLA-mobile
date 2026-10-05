package br.com.imoveisregla.realtor.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import br.com.imoveisregla.realtor.feature.agenda.AgendaScreen
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
@Composable
fun RealtorApp() {
    val nav = rememberNavController()
    val openLead: (Long) -> Unit = { nav.navigate(LeadDetailRoute(it)) }
    val openApplication: (Long) -> Unit = { nav.navigate(ApplicationDetailRoute(it)) }
    val openListing: (Long) -> Unit = { nav.navigate(ListingDetailRoute(it)) }

    Scaffold { padding ->
        NavHost(nav, startDestination = DashboardRoute, modifier = Modifier.padding(padding)) {
            composable<DashboardRoute> {
                DashboardScreen(onOpenLead = openLead, onOpenApplication = openApplication, onOpenAgenda = { nav.navigate(AgendaRoute) })
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
