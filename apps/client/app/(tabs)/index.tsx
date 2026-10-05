import { useEffect, useMemo, useState } from 'react';
import { FlatList, RefreshControl, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { router } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { formatPrice, type ListingFilters, type ListingType } from '@regla/shared';
import { colors, radius, shadow, space, type } from '@/theme';
import { useListings, errorText } from '@/lib/queries';
import { useFavorites } from '@/lib/useFavorites';
import { useAuth } from '@/lib/auth';
import { LISTING_TYPE_LABEL } from '@/lib/listing';
import { ListingCard } from '@/components/ListingCard';
import { FilterSheet, type Option } from '@/components/FilterSheet';
import { Icon } from '@/components/Icon';
import { Chip, EmptyState, ErrorBox, Loading } from '@/components/ui';

type SheetKey = 'type' | 'beds' | 'price' | 'city' | null;

const TYPE_OPTIONS: Option<ListingType>[] = (Object.keys(LISTING_TYPE_LABEL) as ListingType[]).map((t) => ({ value: t, label: LISTING_TYPE_LABEL[t] }));
const BED_OPTIONS: Option<number>[] = [1, 2, 3, 4].map((n) => ({ value: n, label: `${n}+ quarto${n > 1 ? 's' : ''}` }));
const PRICE_OPTIONS: Option<number>[] = [2_000, 3_000, 5_000, 8_000, 15_000, 500_000, 1_000_000, 2_000_000, 5_000_000].map((v) => ({
  value: v,
  label: `Até ${formatPrice(v, 'BRL')}${v < 50_000 ? '/mês' : ''}`,
}));

function useDebounced<T>(value: T, ms = 350): T {
  const [v, setV] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setV(value), ms);
    return () => clearTimeout(t);
  }, [value, ms]);
  return v;
}

export default function SearchScreen() {
  const insets = useSafeAreaInsets();
  const { user } = useAuth();
  const [text, setText] = useState('');
  const query = useDebounced(text);
  const [type_, setType] = useState<ListingType | undefined>();
  const [minBeds, setMinBeds] = useState<number | undefined>();
  const [maxPrice, setMaxPrice] = useState<number | undefined>();
  const [city, setCity] = useState<string | undefined>();
  const [sheet, setSheet] = useState<SheetKey>(null);

  const filters: ListingFilters = useMemo(
    () => ({ query: query || undefined, type: type_, minBeds, maxPrice, city }),
    [query, type_, minBeds, maxPrice, city],
  );
  const listings = useListings(filters);
  const all = useListings({});
  const { isFavorite, toggle } = useFavorites();

  const cityOptions: Option<string>[] = useMemo(() => {
    const set = new Set((all.data ?? []).map((l) => l.city).filter(Boolean));
    return [...set].sort((a, b) => a.localeCompare(b, 'pt-BR')).map((c) => ({ value: c, label: c }));
  }, [all.data]);

  const activeCount = [type_, minBeds, maxPrice, city].filter((v) => v !== undefined).length;
  const firstName = (user?.user_metadata?.full_name as string | undefined)?.split(' ')[0];

  const header = (
    <View>
      <View style={[styles.hero, { paddingTop: insets.top + space.lg }]}>
        <View style={styles.heroClip} pointerEvents="none">
          <View style={styles.heroDecor} />
        </View>
        <Text style={styles.brand}>REGLA IMÓVEIS</Text>
        <Text style={styles.heroTitle}>{firstName ? `Olá, ${firstName}.\nVamos achar seu lugar?` : 'Encontre o imóvel\nque é a sua cara.'}</Text>
        <View style={styles.searchBox}>
          <Icon name="search" size={20} color={colors.muted} />
          <TextInput
            value={text}
            onChangeText={setText}
            placeholder="Bairro, cidade ou nome do imóvel"
            placeholderTextColor={colors.faint}
            style={styles.searchInput}
            returnKeyType="search"
            accessibilityLabel="Buscar imóveis"
            testID="search-input"
          />
          {text ? (
            <Text onPress={() => setText('')} style={styles.clear} accessibilityRole="button">
              Limpar
            </Text>
          ) : null}
        </View>
      </View>
      <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.chips}>
        <Chip label={type_ ? LISTING_TYPE_LABEL[type_] : 'Tipo'} active={!!type_} icon="chevron-down" onPress={() => setSheet('type')} testID="chip-type" />
        <Chip label={minBeds ? `${minBeds}+ quartos` : 'Quartos'} active={!!minBeds} icon="chevron-down" onPress={() => setSheet('beds')} testID="chip-beds" />
        <Chip
          label={maxPrice ? `Até ${formatPrice(maxPrice, 'BRL')}` : 'Preço máx.'}
          active={!!maxPrice}
          icon="chevron-down"
          onPress={() => setSheet('price')}
          testID="chip-price"
        />
        <Chip label={city ?? 'Cidade'} active={!!city} icon="chevron-down" onPress={() => setSheet('city')} testID="chip-city" />
        {activeCount ? (
          <Chip
            label="Limpar filtros"
            onPress={() => {
              setType(undefined);
              setMinBeds(undefined);
              setMaxPrice(undefined);
              setCity(undefined);
            }}
          />
        ) : null}
      </ScrollView>
      <View style={styles.countRow}>
        <Text style={type.small}>
          {listings.isLoading ? 'Buscando…' : `${listings.data?.length ?? 0} imóve${listings.data?.length === 1 ? 'l' : 'is'} disponíve${listings.data?.length === 1 ? 'l' : 'is'}`}
        </Text>
      </View>
      {listings.error ? <View style={{ paddingHorizontal: space.lg }}><ErrorBox message={errorText(listings.error)} onRetry={() => listings.refetch()} /></View> : null}
    </View>
  );

  return (
    <View style={{ flex: 1, backgroundColor: colors.bg }}>
      <FlatList
        data={listings.data ?? []}
        keyExtractor={(l) => String(l.id)}
        ListHeaderComponent={header}
        contentContainerStyle={{ paddingBottom: space.xxxl }}
        renderItem={({ item }) => (
          <View style={styles.cardWrap}>
            <ListingCard
              listing={item}
              isFavorite={isFavorite(item.id)}
              onToggleFavorite={(l) => toggle(l.id)}
              onPress={(l) => router.push(`/listing/${l.id}`)}
            />
          </View>
        )}
        ListEmptyComponent={
          listings.isLoading ? (
            <Loading label="Buscando imóveis…" />
          ) : listings.error ? null : (
            <EmptyState icon="search" title="Nenhum imóvel encontrado" text="Tente outro bairro ou remova alguns filtros." />
          )
        }
        refreshControl={<RefreshControl refreshing={listings.isRefetching} onRefresh={() => listings.refetch()} tintColor={colors.navy} />}
        keyboardShouldPersistTaps="handled"
      />

      <FilterSheet visible={sheet === 'type'} title="Tipo de imóvel" options={TYPE_OPTIONS} selected={type_} onSelect={setType} onClose={() => setSheet(null)} />
      <FilterSheet visible={sheet === 'beds'} title="Quartos" options={BED_OPTIONS} selected={minBeds} onSelect={setMinBeds} onClose={() => setSheet(null)} />
      <FilterSheet visible={sheet === 'price'} title="Preço máximo" options={PRICE_OPTIONS} selected={maxPrice} onSelect={setMaxPrice} onClose={() => setSheet(null)} />
      <FilterSheet visible={sheet === 'city'} title="Cidade" options={cityOptions} selected={city} onSelect={setCity} onClose={() => setSheet(null)} />
    </View>
  );
}

const styles = StyleSheet.create({
  hero: {
    backgroundColor: colors.navy,
    paddingHorizontal: space.xl,
    paddingBottom: space.xxxl + space.md,
    borderBottomLeftRadius: 28,
    borderBottomRightRadius: 28,
    zIndex: 2,
  },
  heroClip: { ...StyleSheet.absoluteFill, overflow: 'hidden', borderBottomLeftRadius: 28, borderBottomRightRadius: 28 },
  heroDecor: {
    position: 'absolute',
    right: -70,
    top: -40,
    width: 220,
    height: 220,
    borderRadius: 110,
    borderWidth: 34,
    borderColor: 'rgba(255,107,53,0.16)',
  },
  brand: { color: colors.coral, fontWeight: '900', letterSpacing: 3, fontSize: 12, marginBottom: space.sm },
  heroTitle: { ...type.display, color: colors.white },
  searchBox: {
    position: 'absolute',
    left: space.xl,
    right: space.xl,
    bottom: -26,
    height: 54,
    borderRadius: radius.lg,
    backgroundColor: colors.white,
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: space.lg,
    gap: space.sm,
    ...shadow,
  },
  searchInput: { flex: 1, fontSize: 16, color: colors.text, height: '100%' },
  clear: { color: colors.coral, fontWeight: '700', fontSize: 13 },
  chips: { gap: space.sm, paddingHorizontal: space.lg, paddingTop: space.xxxl + 4, paddingBottom: space.sm },
  countRow: { paddingHorizontal: space.xl, paddingVertical: space.sm },
  cardWrap: { paddingHorizontal: space.lg },
});
