import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { Image } from 'expo-image';
import { Stack, useLocalSearchParams } from 'expo-router';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  fetchListing, formatArea, formatPrice, listingRef, photoUrl, setListingStatus,
  type ListingStatus, type ListingWithPhotos,
} from '@regla/shared';
import { SUPABASE_URL, getSupabase } from '../../lib/supabase';
import { qk } from '../../lib/queryClient';
import { Card, ErrorState, Field, InlineError, Loading, Pill, Row, Screen, Segmented } from '../../components/ui';
import { LISTING_STATUSES, LISTING_STATUS_LABEL, LISTING_STATUS_TONE, LISTING_TYPE_LABEL } from '../../utils/labels';
import { colors, radius, space, type } from '../../theme';

export default function ListingDetailScreen() {
  const { id: raw } = useLocalSearchParams<{ id: string }>();
  const id = Number(raw);
  const qc = useQueryClient();
  const q = useQuery({ queryKey: qk.listing(id), queryFn: () => fetchListing(getSupabase(), id), enabled: Number.isFinite(id) });

  const m = useMutation({
    mutationFn: (status: ListingStatus) => setListingStatus(getSupabase(), id, status),
    onMutate: async (status) => {
      await qc.cancelQueries({ queryKey: qk.listing(id) });
      const prev = qc.getQueryData<ListingWithPhotos>(qk.listing(id));
      if (prev) qc.setQueryData(qk.listing(id), { ...prev, status });
      return { prev };
    },
    onError: (_e, _s, ctx) => { if (ctx?.prev) qc.setQueryData(qk.listing(id), ctx.prev); },
    onSettled: () => {
      qc.invalidateQueries({ queryKey: qk.listing(id) });
      qc.invalidateQueries({ queryKey: qk.listings });
      qc.invalidateQueries({ queryKey: qk.stats });
    },
  });

  if (q.isLoading) return <Screen><Loading /></Screen>;
  if (q.error || !q.data) return <Screen><ErrorState error={q.error ?? 'Imóvel não encontrado'} onRetry={() => q.refetch()} /></Screen>;
  const l = q.data;

  return (
    <Screen refreshing={q.isRefetching} onRefresh={() => q.refetch()}>
      <Stack.Screen options={{ title: listingRef(l.id) }} />

      {l.listing_photos.length && SUPABASE_URL ? (
        <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.gallery} contentContainerStyle={{ gap: space.sm, paddingHorizontal: space.lg }}>
          {l.listing_photos.map((p) => (
            <Image key={p.id} source={{ uri: photoUrl(SUPABASE_URL, p.storage_path) }} style={styles.photo} contentFit="cover" accessibilityLabel={p.alt_text ?? l.title} />
          ))}
        </ScrollView>
      ) : null}

      <Card>
        <Row style={{ justifyContent: 'space-between' }}>
          <Text style={type.mono}>{listingRef(l.id)} · {LISTING_TYPE_LABEL[l.type]}</Text>
          <Pill label={LISTING_STATUS_LABEL[l.status]} tone={LISTING_STATUS_TONE[l.status]} testID="listing-status-pill" />
        </Row>
        <Text style={[type.title, { marginTop: space.sm }]}>{l.title}</Text>
        <Text style={type.small}>{l.neighborhood}, {l.city}</Text>
        <Text style={styles.price}>{formatPrice(l.price, l.currency)}</Text>
        <View style={styles.fields}>
          <Field label="Quartos" value={String(l.beds)} mono />
          <Field label="Banheiros" value={String(l.baths)} mono />
          <Field label="Área" value={formatArea(l.area_m2)} mono />
          <Field label="Destaque" value={l.featured ? 'Sim' : 'Não'} />
        </View>
        {l.tags.length ? (
          <View style={styles.tags}>
            {l.tags.map((t) => <Text key={t} style={styles.tag}>{t}</Text>)}
          </View>
        ) : null}
      </Card>

      <Card>
        <Text style={[type.over, { marginBottom: space.sm }]}>Status do anúncio</Text>
        <Segmented testIDPrefix="listing-status" options={LISTING_STATUSES} value={l.status} labels={LISTING_STATUS_LABEL} disabled={m.isPending} onChange={(s) => m.mutate(s)} />
        <Text style={[type.small, { marginTop: space.sm }]}>
          {l.status === 'live' ? 'Visível no site e no app do cliente.' : 'Oculto para clientes.'}
        </Text>
        <InlineError message={m.error ? (m.error as Error).message : null} />
      </Card>

      {l.summary || l.description ? (
        <Card>
          <Text style={[type.over, { marginBottom: space.sm }]}>Descrição</Text>
          {l.summary ? <Text style={[type.body, { fontWeight: '600', marginBottom: space.sm }]}>{l.summary}</Text> : null}
          {l.description ? <Text style={type.body}>{l.description}</Text> : null}
        </Card>
      ) : null}
    </Screen>
  );
}

const styles = StyleSheet.create({
  gallery: { flexGrow: 0, marginHorizontal: -space.lg },
  photo: { width: 260, height: 170, borderRadius: radius.lg, backgroundColor: colors.line },
  price: { fontSize: 24, fontWeight: '800', color: colors.navy, marginTop: space.sm, ...type.num },
  fields: { flexDirection: 'row', flexWrap: 'wrap', columnGap: space.md, marginTop: space.sm },
  tags: { flexDirection: 'row', flexWrap: 'wrap', gap: 6, marginTop: space.sm },
  tag: { fontSize: 12, color: colors.muted, backgroundColor: colors.navySoft, paddingHorizontal: 8, paddingVertical: 3, borderRadius: radius.sm, overflow: 'hidden' },
});
