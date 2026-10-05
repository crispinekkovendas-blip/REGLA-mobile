package br.com.imoveisregla.realtor.feature.listings

import androidx.compose.ui.graphics.Color
import br.com.imoveisregla.core.designsystem.Regla
import br.com.imoveisregla.core.model.Listing
import br.com.imoveisregla.core.model.ListingStatus
import br.com.imoveisregla.core.model.formatPrice
import br.com.imoveisregla.core.model.listingRef

const val SITE_URL = "https://imoveisregla.com.br"

/** Number of listings per status (missing statuses map to 0). */
fun statusCounts(listings: List<Listing>): Map<ListingStatus, Int> {
    val grouped = listings.groupingBy { it.status }.eachCount()
    return ListingStatus.entries.associateWith { grouped[it] ?: 0 }
}

/** Status filter (null = Todos) + search over title, neighborhood, city and ref code. */
fun filterListings(listings: List<Listing>, status: ListingStatus?, query: String): List<Listing> {
    val q = query.trim()
    return listings.filter { l ->
        (status == null || l.status == status) &&
            (q.isEmpty() || listOf(l.title, l.neighborhood, l.city, listingRef(l.id)).any { it.contains(q, ignoreCase = true) })
    }
}

/** "R$ 4.800/mês" for rentals, "R$ 3.950.000" for sales. */
fun listingPrice(l: Listing): String = formatPrice(l.price, l.currency) + if (l.isRental) "/mês" else ""

fun publicListingUrl(slug: String): String = "$SITE_URL/imovel/$slug"

fun shareText(l: Listing): String = "${l.title}\n${publicListingUrl(l.slug)}"

/** Taking a listing off the market asks for confirmation first. */
fun needsConfirmation(status: ListingStatus): Boolean = status == ListingStatus.SOLD || status == ListingStatus.WITHDRAWN

/** Order used by the filter chips. */
val STATUS_ORDER: List<ListingStatus> = listOf(ListingStatus.LIVE, ListingStatus.DRAFT, ListingStatus.SOLD, ListingStatus.WITHDRAWN)

internal fun statusColors(s: ListingStatus): Pair<Color, Color> = when (s) {
    ListingStatus.LIVE -> Regla.Ok to Regla.OkSoft
    ListingStatus.DRAFT -> Regla.Muted to Regla.NavySoft
    ListingStatus.SOLD -> Regla.Info to Regla.InfoSoft
    ListingStatus.WITHDRAWN -> Regla.Danger to Regla.DangerSoft
}
