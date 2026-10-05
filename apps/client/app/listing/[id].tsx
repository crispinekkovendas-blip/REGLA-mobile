import { useState } from 'react';
import {
  Linking, Pressable, ScrollView, StyleSheet, Text, useWindowDimensions, View,
  type NativeScrollEvent, type NativeSyntheticEvent,
} from 'react-native';
import { router, Stack, useLocalSearchParams } from 'expo-router';
import { Image } from 'expo-image';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { formatArea, waInterest, type ListingWithPhotos } from '@regla/shared';
import { colors, radius, shadow, space, type } from '@/theme';
import { listingPhotoUrl } from '@/lib/supabase';
import { useAuth } from '@/lib/auth';
import { errorText, useListing } from '@/lib/queries';
import { useFavorites } from '@/lib/useFavorites';
import { LISTING_TYPE_LABEL, isRental, listingRef, locationLabel, priceLabel } from '@/lib/listing';
import { ListingImage } from '@/components/ListingCard';
import { Icon } from '@/components/Icon';
import { Button, EmptyState, ErrorBox, Loading } from '@/components/ui';

const MAX_W = 720;

function Carousel({ listing }: { listing: ListingWithPhotos }) {
  const { width: winW } = useWindowDimensions();
  const width = Math.min(winW, MAX_W);
  const height = Math.round(width * 0.8);
  const [index, setIndex] = useState(0);
  const photos = listing.listing_photos ?? [];

  if (!photos.length) return <ListingImage listing={listing} height={height} rounded={false} />;

  const onScroll = (e: NativeSyntheticEvent<NativeScrollEvent>) => {
    const i = Math.round(e.nativeEvent.contentOffset.x / width);
    if (i !== index) setIndex(i);
  };

  return (
    <View style={{ width, height }}>
      <ScrollView horizontal pagingEnabled showsHorizontalScrollIndicator={false} onScroll={onScroll} scrollEventThrottle={32} testID="photo-carousel">
        {photos.map((p) => (
          <Image
            key={p.id}
            source={{ uri: listingPhotoUrl(p.storage_path) }}
            style={{ width, height, backgroundColor: colors.navyMid }}
            contentFit="cover"
            transition={150}
            accessibilityLabel={p.alt_text ?? listing.title}
          />
        ))}
      </ScrollView>
      <View style={styles.counter}>
        <Text style={styles.counterText}>
          {index + 1}/{photos.length}
        </Text>
      </View>
      {photos.length > 1 ? (
        <View style={styles.dots}>
          {photos.slice(0, 10).map((p, i) => (
            <View key={p.id} style={[styles.dot, i === index && styles.dotActive]} />
          ))}
        </View>
      ) : null}
    </View>
  );
}

function Spec({ value, label }: { value: string; label: string }) {
  return (
    <View style={styles.spec}>
      <Text style={styles.specValue}>{value}</Text>
      <Text style={styles.specLabel}>{label}</Text>
    </View>
  );
}

export default function ListingScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const listingId = Number(id);
  const insets = useSafeAreaInsets();
  const { user } = useAuth();
  const { data: listing, isLoading, error, refetch } = useListing(listingId);
  const { isFavorite, toggle } = useFavorites();
  const [expanded, setExpanded] = useState(false);

  const requireAuth = (path: `/visit/${number}` | `/apply/${number}`) => {
    if (!user) router.push('/auth/login');
    else router.push(path);
  };

  if (isLoading) {
    return (
      <View style={{ flex: 1, backgroundColor: colors.bg }}>
        <Stack.Screen options={{ headerTintColor: colors.navy }} />
        <Loading label="Carregando imóvel…" />
      </View>
    );
  }
  if (error || !listing) {
    return (
      <View style={{ flex: 1, padding: space.xl, paddingTop: insets.top + 64, backgroundColor: colors.bg }}>
        <Stack.Screen options={{ headerTintColor: colors.navy }} />
        {error ? <ErrorBox message={errorText(error)} onRetry={() => refetch()} /> : null}
        <EmptyState title="Imóvel não encontrado" text="Ele pode ter sido alugado ou vendido." action={<Button title="Voltar à busca" onPress={() => router.navigate('/')} />} />
      </View>
    );
  }

  const fav = isFavorite(listing.id);
  const rental = isRental(listing);
  const ref = listingRef(listing.id);
  const desc = listing.description || listing.summary;

  return (
    <View style={{ flex: 1, backgroundColor: colors.bg }}>
      <Stack.Screen
        options={{
          headerTintColor: colors.white,
          headerRight: () => (
            <Pressable
              testID="detail-fav"
              accessibilityRole="button"
              accessibilityLabel={fav ? 'Remover dos favoritos' : 'Favoritar'}
              onPress={() => toggle(listing.id)}
              style={styles.headerFav}
            >
              <Icon name={fav ? 'heart' : 'heart-outline'} size={20} color={fav ? colors.coral : colors.navy} />
            </Pressable>
          ),
        }}
      />
      <ScrollView contentContainerStyle={{ paddingBottom: 120 + insets.bottom }}>
        <View style={{ alignItems: 'center', backgroundColor: colors.navy }}>
          <Carousel listing={listing} />
        </View>
        <View style={styles.sheet}>
          <View style={styles.tagsRow}>
            <View style={styles.dealPill}>
              <Text style={styles.dealText}>{rental ? 'Para alugar' : 'À venda'}</Text>
            </View>
            <Text style={styles.ref} testID="detail-ref">
              Ref. {ref}
            </Text>
          </View>
          <Text style={[type.display, { color: colors.navy }]} testID="detail-price">
            {priceLabel(listing)}
          </Text>
          <Text style={[type.h2, { marginTop: space.xs }]}>{listing.title}</Text>
          <View style={styles.locRow}>
            <Icon name="pin" size={15} color={colors.muted} />
            <Text style={[type.body, { color: colors.muted }]}>{locationLabel(listing)}</Text>
          </View>

          <View style={styles.specs}>
            <Spec value={String(listing.beds)} label={listing.beds === 1 ? 'quarto' : 'quartos'} />
            <Spec value={String(listing.baths)} label={listing.baths === 1 ? 'banheiro' : 'banheiros'} />
            <Spec value={formatArea(listing.area_m2)} label="área" />
            <Spec value={LISTING_TYPE_LABEL[listing.type] ?? '—'} label="tipo" />
          </View>

          {listing.tags?.length ? (
            <View style={styles.tags}>
              {listing.tags.map((t) => (
                <View key={t} style={styles.tag}>
                  <Text style={styles.tagText}>{t}</Text>
                </View>
              ))}
            </View>
          ) : null}

          {desc ? (
            <View style={{ marginTop: space.xl }}>
              <Text style={type.h2}>Sobre o imóvel</Text>
              <Text style={[type.body, { marginTop: space.sm, color: colors.text }]} numberOfLines={expanded ? undefined : 6}>
                {desc}
              </Text>
              {desc.length > 280 ? (
                <Text style={styles.more} onPress={() => setExpanded((v) => !v)}>
                  {expanded ? 'Mostrar menos' : 'Ler descrição completa'}
                </Text>
              ) : null}
            </View>
          ) : null}

          <View style={styles.howCard}>
            <Text style={type.h3}>Como funciona na REGLA</Text>
            {[
              ['1', 'Agende uma visita no horário que preferir'],
              ['2', 'Faça sua proposta pelo app, sem papelada'],
              ['3', 'Envie seus documentos e acompanhe a análise'],
            ].map(([n, t]) => (
              <View key={n} style={styles.howRow}>
                <View style={styles.howNum}>
                  <Text style={styles.howNumText}>{n}</Text>
                </View>
                <Text style={[type.body, { flex: 1 }]}>{t}</Text>
              </View>
            ))}
          </View>

          <Button
            variant="outline"
            icon="chat"
            title="Falar no WhatsApp"
            style={{ marginTop: space.xl }}
            onPress={() => Linking.openURL(waInterest(listing.title, ref))}
            testID="btn-whatsapp"
          />
          <Button
            variant="ghost"
            icon={fav ? 'heart' : 'heart-outline'}
            title={fav ? 'Salvo nos favoritos' : 'Favoritar'}
            style={{ marginTop: space.sm }}
            onPress={() => toggle(listing.id)}
            testID="btn-favorite"
          />
        </View>
      </ScrollView>

      <View style={[styles.bar, { paddingBottom: insets.bottom + space.md }]}>
        <Button variant="outline" icon="calendar" title="Agendar visita" style={{ flex: 1 }} onPress={() => requireAuth(`/visit/${listing.id}`)} testID="btn-visit" />
        <Button variant="accent" title="Fazer proposta" style={{ flex: 1 }} onPress={() => requireAuth(`/apply/${listing.id}`)} testID="btn-apply" />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  headerFav: { width: 38, height: 38, borderRadius: 19, backgroundColor: colors.white, alignItems: 'center', justifyContent: 'center', ...shadow },
  counter: { position: 'absolute', right: space.lg, bottom: space.xxl + 8, backgroundColor: 'rgba(7,22,41,0.65)', paddingHorizontal: 10, paddingVertical: 4, borderRadius: radius.pill },
  counterText: { color: colors.white, fontSize: 12, fontWeight: '700' },
  dots: { position: 'absolute', bottom: space.xxl + 12, left: 0, right: 0, flexDirection: 'row', justifyContent: 'center', gap: 6 },
  dot: { width: 6, height: 6, borderRadius: 3, backgroundColor: 'rgba(255,255,255,0.5)' },
  dotActive: { backgroundColor: colors.white, width: 18 },
  sheet: {
    marginTop: -space.xl,
    backgroundColor: colors.bg,
    borderTopLeftRadius: radius.xl,
    borderTopRightRadius: radius.xl,
    padding: space.xl,
    width: '100%',
    maxWidth: MAX_W,
    alignSelf: 'center',
  },
  tagsRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: space.sm },
  dealPill: { backgroundColor: colors.coralSoft, paddingHorizontal: 12, paddingVertical: 5, borderRadius: radius.pill },
  dealText: { color: colors.coralDark, fontWeight: '800', fontSize: 12 },
  ref: { fontSize: 12, fontWeight: '800', color: colors.muted, letterSpacing: 0.8 },
  locRow: { flexDirection: 'row', alignItems: 'center', gap: 4, marginTop: 4 },
  specs: { flexDirection: 'row', gap: space.sm, marginTop: space.xl },
  spec: { flex: 1, backgroundColor: colors.white, borderRadius: radius.md, paddingVertical: space.md, paddingHorizontal: space.sm, alignItems: 'center', ...shadow },
  specValue: { fontSize: 16, fontWeight: '800', color: colors.navy, textAlign: 'center' },
  specLabel: { fontSize: 12, color: colors.muted, marginTop: 2 },
  tags: { flexDirection: 'row', flexWrap: 'wrap', gap: space.sm, marginTop: space.lg },
  tag: { borderWidth: 1.5, borderColor: colors.line, borderRadius: radius.pill, paddingHorizontal: 12, paddingVertical: 5, backgroundColor: colors.white },
  tagText: { fontSize: 13, color: colors.text, fontWeight: '600' },
  more: { color: colors.coral, fontWeight: '700', marginTop: space.sm },
  howCard: { marginTop: space.xl, backgroundColor: colors.white, borderRadius: radius.lg, padding: space.lg, gap: space.md, ...shadow },
  howRow: { flexDirection: 'row', alignItems: 'center', gap: space.md },
  howNum: { width: 28, height: 28, borderRadius: 14, backgroundColor: colors.navy, alignItems: 'center', justifyContent: 'center' },
  howNumText: { color: colors.white, fontWeight: '800', fontSize: 13 },
  bar: {
    position: 'absolute',
    left: 0,
    right: 0,
    bottom: 0,
    flexDirection: 'row',
    gap: space.md,
    paddingHorizontal: space.lg,
    paddingTop: space.md,
    backgroundColor: colors.white,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: colors.line,
  },
});
