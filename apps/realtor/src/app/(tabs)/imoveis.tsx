import { useMemo, useState } from 'react';
import { StyleSheet, Text, TextInput } from 'react-native';
import { router } from 'expo-router';
import { useQuery } from '@tanstack/react-query';
import { fetchAllListings, listingRef, type ListingStatus } from '@regla/shared';
import { getSupabase } from '../../lib/supabase';
import { qk } from '../../lib/queryClient';
import { ListingRow } from '../../components/rows';
import { Chip, ChipRow, EmptyState, ErrorState, Loading, Screen } from '../../components/ui';
import { LISTING_STATUSES, LISTING_STATUS_LABEL } from '../../utils/labels';
import { countBy } from '../../utils/pipeline';
import { colors, radius, space, type } from '../../theme';

export default function ListingsScreen() {
  const [filter, setFilter] = useState<ListingStatus | 'all'>('all');
  const [search, setSearch] = useState('');
  const q = useQuery({ queryKey: qk.listings, queryFn: () => fetchAllListings(getSupabase()) });
  const all = q.data ?? [];
  const counts = useMemo(() => countBy(all, (l) => l.status, LISTING_STATUSES), [all]);
  const s = search.trim().toLowerCase();
  const visible = all.filter((l) => (filter === 'all' || l.status === filter)
    && (!s || l.title.toLowerCase().includes(s) || l.neighborhood.toLowerCase().includes(s)
      || listingRef(l.id).toLowerCase().includes(s)));

  return (
    <Screen refreshing={q.isRefetching} onRefresh={() => q.refetch()}>
      <ChipRow>
        <Chip label="Todos" count={all.length} active={filter === 'all'} onPress={() => setFilter('all')} />
        {LISTING_STATUSES.map((st) => (
          <Chip key={st} label={LISTING_STATUS_LABEL[st]} count={counts[st]} active={filter === st} onPress={() => setFilter(st)} />
        ))}
      </ChipRow>
      <TextInput
        value={search}
        onChangeText={setSearch}
        placeholder="Buscar por título, bairro ou RG-0000"
        placeholderTextColor={colors.faint}
        style={styles.search}
        autoCapitalize="none"
      />
      {q.isLoading ? <Loading /> : q.error ? <ErrorState error={q.error} onRetry={() => q.refetch()} /> : visible.length ? (
        <>
          <Text style={type.small}>{visible.length} {visible.length === 1 ? 'imóvel' : 'imóveis'}</Text>
          {visible.map((l) => (
            <ListingRow key={l.id} listing={l} onPress={() => router.push({ pathname: '/listing/[id]', params: { id: String(l.id) } })} />
          ))}
        </>
      ) : <EmptyState title="Nenhum imóvel encontrado" />}
    </Screen>
  );
}

const styles = StyleSheet.create({
  search: {
    height: 40, borderRadius: radius.md, borderWidth: 1, borderColor: colors.line, backgroundColor: colors.white,
    paddingHorizontal: space.md, fontSize: 14, color: colors.text,
  },
});
