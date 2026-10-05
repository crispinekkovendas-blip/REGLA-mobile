package br.com.imoveisregla.client.feature.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.imoveisregla.core.designsystem.ListingImage
import br.com.imoveisregla.core.designsystem.Regla
import br.com.imoveisregla.core.model.Listing
import br.com.imoveisregla.core.model.formatArea
import br.com.imoveisregla.core.model.label
import br.com.imoveisregla.core.model.formatPrice
import br.com.imoveisregla.core.model.listingRef

/** "R$ 4.800/mês" for rentals, "R$ 3.950.000" for sales. */
fun Listing.displayPrice(): String = formatPrice(price, currency) + if (isRental) "/mês" else ""

/** "2 quartos · 2 banheiros · 72 m²" (zero counts are omitted). */
fun Listing.specsLine(): String = buildList {
    if (beds > 0) add(if (beds == 1) "1 quarto" else "$beds quartos")
    if (baths > 0) add(if (baths == 1) "1 banheiro" else "$baths banheiros")
    if (areaM2 > 0) add(formatArea(areaM2))
}.joinToString(" · ")

/** Heart button on a translucent white circle, used over listing photos. */
@Composable
fun FavoriteOverlayButton(isFavorite: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.92f)),
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = if (isFavorite) "Remover dos favoritos" else "Salvar nos favoritos",
            tint = if (isFavorite) Regla.Coral else Regla.Ink,
        )
    }
}

@Composable
fun BrowseListingCard(
    listing: Listing,
    photoUrl: String?,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(Regla.RadiusCard),
        colors = CardDefaults.cardColors(containerColor = Regla.Surface),
        border = BorderStroke(1.dp, Regla.Line),
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 10f)) {
            ListingImage(url = photoUrl, contentDescription = listing.title, modifier = Modifier.fillMaxSize())
            FavoriteOverlayButton(
                isFavorite = isFavorite,
                onClick = onToggleFavorite,
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
            )
            Text(
                listing.type.label,
                style = MaterialTheme.typography.labelMedium,
                color = Regla.Ink,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.92f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    listing.displayPrice(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Regla.Navy,
                    modifier = Modifier.weight(1f),
                )
                Text(listingRef(listing.id), style = MaterialTheme.typography.labelMedium, color = Regla.Muted)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                listing.title,
                style = MaterialTheme.typography.titleMedium,
                color = Regla.Ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                listOf(listing.neighborhood, listing.city).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = Regla.Muted,
            )
            val specs = listing.specsLine()
            if (specs.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(3.dp).height(14.dp).background(Regla.Coral, RoundedCornerShape(2.dp)))
                    Text(specs, style = MaterialTheme.typography.bodyMedium, color = Regla.Ink)
                }
            }
        }
    }
}
