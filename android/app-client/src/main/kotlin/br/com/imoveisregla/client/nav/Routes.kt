package br.com.imoveisregla.client.nav

import kotlinx.serialization.Serializable

// Type-safe Navigation Compose routes. FIXED CONTRACT — features navigate via the
// lambdas their screen receives; only ui/ClientApp.kt maps lambdas to these routes.

// Bottom tabs
@Serializable data object SearchRoute
@Serializable data object FavoritesRoute
@Serializable data object VisitsRoute
@Serializable data object ProposalsRoute
@Serializable data object ProfileRoute

// Stack screens
@Serializable data class ListingDetailRoute(val id: Long)
@Serializable data class BookVisitRoute(val listingId: Long)
@Serializable data class ApplyRoute(val listingId: Long)
@Serializable data object LoginRoute
@Serializable data object SignupRoute
@Serializable data object EditProfileRoute
@Serializable data object DocumentsRoute
