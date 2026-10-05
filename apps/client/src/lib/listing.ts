import { formatPrice, listingRef, type Listing, type ListingType } from '@regla/shared';
import { placeholderTones } from '@/theme';

export const LISTING_TYPE_LABEL: Record<ListingType, string> = {
  apartment: 'Apartamento',
  house: 'Casa',
  commercial: 'Comercial',
  land: 'Terreno',
};

/**
 * REGLA lists both rentals (Oliveira portfolio) and sales. The schema has no
 * explicit flag, so infer from tags, falling back to the price magnitude.
 */
export function isRental(l: Pick<Listing, 'tags' | 'price'>): boolean {
  const tags = (l.tags ?? []).map((t) => t.toLowerCase());
  if (tags.some((t) => /alug|rent|locac|locaç/.test(t))) return true;
  if (tags.some((t) => /venda|sale|compra/.test(t))) return false;
  return l.price > 0 && l.price < 50_000;
}

export function priceLabel(l: Pick<Listing, 'price' | 'currency' | 'tags'>): string {
  return `${formatPrice(l.price, l.currency)}${isRental(l) ? '/mês' : ''}`;
}

export function specsLabel(l: Pick<Listing, 'beds' | 'baths' | 'area_m2'>): string[] {
  const parts: string[] = [];
  if (l.beds) parts.push(`${l.beds} ${l.beds === 1 ? 'quarto' : 'quartos'}`);
  if (l.baths) parts.push(`${l.baths} ${l.baths === 1 ? 'banheiro' : 'banheiros'}`);
  if (l.area_m2) parts.push(`${l.area_m2} m²`);
  return parts;
}

export const locationLabel = (l: Pick<Listing, 'neighborhood' | 'city'>): string =>
  [l.neighborhood, l.city].filter(Boolean).join(' · ');

export const placeholderTone = (id: number): string => placeholderTones[Math.abs(id) % placeholderTones.length];

export { listingRef };
