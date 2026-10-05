package br.com.imoveisregla.realtor.nav

import kotlinx.serialization.Serializable

// Type-safe Navigation Compose routes. FIXED CONTRACT — features navigate via the
// lambdas their screen receives; only ui/RealtorApp.kt maps lambdas to these routes.

// Bottom tabs
@Serializable data object DashboardRoute
@Serializable data object LeadsRoute
@Serializable data object ProposalsRoute
@Serializable data object AgendaRoute
@Serializable data object ListingsRoute

// Stack screens
@Serializable data class LeadDetailRoute(val id: Long)
@Serializable data class ApplicationDetailRoute(val id: Long)
@Serializable data class ListingDetailRoute(val id: Long)
