import { memo } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { Image } from 'expo-image';
import type { ListingWithPhotos } from '@regla/shared';
import { colors, radius, shadow, space, type } from '@/theme';
import { listingPhotoUrl } from '@/lib/supabase';
import { LISTING_TYPE_LABEL, isRental, listingRef, locationLabel, placeholderTone, priceLabel, specsLabel } from '@/lib/listing';
import { Icon } from './Icon';

interface Props {
  listing: ListingWithPhotos;
  isFavorite?: boolean;
  onToggleFavorite?: (listing: ListingWithPhotos) => void;
  onPress?: (listing: ListingWithPhotos) => void;
  compact?: boolean;
}

export function ListingImage({ listing, height, rounded = true }: { listing: ListingWithPhotos; height: number; rounded?: boolean }) {
  const photo = listing.listing_photos?.[0];
  const r = rounded ? { borderTopLeftRadius: radius.lg, borderTopRightRadius: radius.lg } : null;
  if (photo) {
    return (
      <Image
        source={{ uri: listingPhotoUrl(photo.storage_path) }}
        style={[{ width: '100%', height, backgroundColor: placeholderTone(listing.id) }, r]}
        contentFit="cover"
        transition={200}
        accessibilityLabel={photo.alt_text ?? listing.title}
      />
    );
  }
  return (
    <View testID="listing-placeholder" style={[styles.placeholder, { height, backgroundColor: placeholderTone(listing.id) }, r]}>
      <View style={styles.placeholderRing} />
      <Icon name="home" size={44} color="rgba(255,255,255,0.9)" />
      <Text style={styles.placeholderText}>{LISTING_TYPE_LABEL[listing.type] ?? 'Imóvel'}</Text>
    </View>
  );
}

function ListingCardBase({ listing, isFavorite, onToggleFavorite, onPress, compact }: Props) {
  const specs = specsLabel(listing);
  return (
    <Pressable
      testID={`listing-card-${listing.id}`}
      accessibilityRole="button"
      accessibilityLabel={`${listing.title}, ${priceLabel(listing)}`}
      onPress={() => onPress?.(listing)}
      style={({ pressed }) => [styles.card, pressed && { transform: [{ scale: 0.992 }], opacity: 0.96 }]}
    >
      <View>
        <ListingImage listing={listing} height={compact ? 150 : 210} />
        <View style={styles.topRow}>
          <View style={styles.refPill}>
            <Text style={styles.refText}>{listingRef(listing.id)}</Text>
          </View>
          {listing.featured ? (
            <View style={[styles.refPill, { backgroundColor: colors.coral }]}>
              <Text style={[styles.refText, { color: colors.white }]}>Destaque</Text>
            </View>
          ) : null}
        </View>
        {onToggleFavorite ? (
          <Pressable
            testID={`fav-${listing.id}`}
            accessibilityRole="button"
            accessibilityLabel={isFavorite ? 'Remover dos favoritos' : 'Adicionar aos favoritos'}
            hitSlop={10}
            onPress={() => onToggleFavorite(listing)}
            style={({ pressed }) => [styles.heart, pressed && { transform: [{ scale: 0.9 }] }]}
          >
            <Icon name={isFavorite ? 'heart' : 'heart-outline'} size={22} color={isFavorite ? colors.coral : colors.navy} />
          </Pressable>
        ) : null}
        <View style={styles.dealPill}>
          <Text style={styles.dealText}>{isRental(listing) ? 'Aluguel' : 'Venda'}</Text>
        </View>
      </View>
      <View style={styles.body}>
        <Text style={type.price} testID="listing-price">
          {priceLabel(listing)}
        </Text>
        <Text style={styles.title} numberOfLines={1}>
          {listing.title}
        </Text>
        <View style={styles.locRow}>
          <Icon name="pin" size={13} color={colors.muted} />
          <Text style={styles.loc} numberOfLines={1}>
            {locationLabel(listing)}
          </Text>
        </View>
        {specs.length ? (
          <View style={styles.specs}>
            {specs.map((s) => (
              <View key={s} style={styles.spec}>
                <Text style={styles.specText}>{s}</Text>
              </View>
            ))}
          </View>
        ) : null}
      </View>
    </Pressable>
  );
}

export const ListingCard = memo(ListingCardBase);

const styles = StyleSheet.create({
  card: { backgroundColor: colors.white, borderRadius: radius.lg, marginBottom: space.lg, ...shadow },
  placeholder: { width: '100%', alignItems: 'center', justifyContent: 'center', overflow: 'hidden', gap: 6 },
  placeholderRing: {
    position: 'absolute',
    width: 260,
    height: 260,
    borderRadius: 130,
    borderWidth: 40,
    borderColor: 'rgba(255,255,255,0.05)',
    right: -90,
    top: -110,
  },
  placeholderText: { color: 'rgba(255,255,255,0.75)', fontSize: 12, fontWeight: '700', letterSpacing: 1.2, textTransform: 'uppercase' },
  topRow: { position: 'absolute', top: space.md, left: space.md, flexDirection: 'row', gap: 6 },
  refPill: { backgroundColor: 'rgba(255,255,255,0.94)', paddingHorizontal: 9, paddingVertical: 4, borderRadius: radius.pill },
  refText: { fontSize: 11, fontWeight: '800', color: colors.navy, letterSpacing: 0.5 },
  heart: {
    position: 'absolute',
    top: space.md,
    right: space.md,
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: colors.white,
    alignItems: 'center',
    justifyContent: 'center',
    ...shadow,
  },
  dealPill: {
    position: 'absolute',
    bottom: -12,
    right: space.lg,
    backgroundColor: colors.navy,
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: radius.pill,
    borderWidth: 2,
    borderColor: colors.white,
  },
  dealText: { color: colors.white, fontSize: 12, fontWeight: '700' },
  body: { padding: space.lg, paddingTop: space.lg, gap: 4 },
  title: { fontSize: 16, fontWeight: '600', color: colors.text },
  locRow: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  loc: { fontSize: 14, color: colors.muted, flex: 1 },
  specs: { flexDirection: 'row', flexWrap: 'wrap', gap: 6, marginTop: space.sm },
  spec: { backgroundColor: colors.navySoft, paddingHorizontal: 10, paddingVertical: 5, borderRadius: radius.sm },
  specText: { fontSize: 13, color: colors.text, fontWeight: '600' },
});
