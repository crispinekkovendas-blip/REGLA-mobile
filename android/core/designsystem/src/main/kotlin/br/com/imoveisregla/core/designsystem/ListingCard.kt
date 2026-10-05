package br.com.imoveisregla.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Bathtub
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.SquareFoot
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.imoveisregla.core.model.Listing
import br.com.imoveisregla.core.model.formatArea
import br.com.imoveisregla.core.model.formatPrice
import br.com.imoveisregla.core.model.label
import br.com.imoveisregla.core.model.listingRef

/** "R$ 4.500/mês" for rentals, "R$ 1.850.000" for sales. */
fun listingPriceText(listing: Listing): String =
    formatPrice(listing.price, listing.currency) + if (listing.isRental) "/mês" else ""

fun bedsText(n: Int) = if (n == 1) "1 quarto" else "$n quartos"
fun bathsText(n: Int) = if (n == 1) "1 banheiro" else "$n banheiros"

/**
 * Browse card: 16:10 photo, prominent price, neighborhood · city, specs row, ref code and favorite heart.
 * Pass `onToggleFavorite = null` to hide the heart (e.g. realtor app).
 */
@Composable
fun ListingCard(
    listing: Listing,
    photoUrl: String?,
    isFavorite: Boolean,
    onToggleFavorite: (() -> Unit)?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Regla.RadiusCard + 4.dp),
        color = Regla.Surface,
        border = BorderStroke(1.dp, Regla.Line),
        shadowElevation = 1.dp,
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f),
            ) {
                ListingImage(photoUrl, listing.title, Modifier.matchParentSize())
                // Top-left: type + rental tag
                Row(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    OverlayTag(listing.type.label, Regla.Navy.copy(alpha = 0.85f), Color.White)
                    if (listing.isRental) OverlayTag("Aluguel", Regla.Coral, Color.White)
                    else OverlayTag("Venda", Color.White.copy(alpha = 0.92f), Regla.Navy)
                }
                if (onToggleFavorite != null) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.94f)),
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remover dos favoritos" else "Adicionar aos favoritos",
                            tint = if (isFavorite) Regla.Coral else Regla.Navy,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        formatPrice(listing.price, listing.currency),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Regla.Navy,
                    )
                    if (listing.isRental) {
                        Text(
                            "/mês",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Regla.Muted,
                            modifier = Modifier.padding(start = 2.dp, bottom = 3.dp),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        listingRef(listing.id),
                        style = MaterialTheme.typography.labelSmall,
                        color = Regla.Subtle,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    listing.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Regla.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Regla.Coral, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(
                        listOf(listing.neighborhood, listing.city).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Regla.Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Regla.Line),
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (listing.beds > 0) Spec(Icons.Outlined.Bed, bedsText(listing.beds))
                    if (listing.baths > 0) Spec(Icons.Outlined.Bathtub, bathsText(listing.baths))
                    Spec(Icons.Outlined.SquareFoot, formatArea(listing.areaM2))
                }
            }
        }
    }
}

@Composable
private fun OverlayTag(text: String, bg: Color, fg: Color) {
    Text(
        text,
        color = fg,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun Spec(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Regla.Navy, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(5.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = Regla.Ink, fontWeight = FontWeight.Medium)
    }
}

