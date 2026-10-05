import { useMemo, useState } from 'react';
import { StyleSheet, Text, TextInput, View } from 'react-native';
import { router } from 'expo-router';
import { useQuery } from '@tanstack/react-query';
import { STAGE_LABEL, fetchLeads, type InquiryStage } from '@regla/shared';
import { getSupabase } from '../../lib/supabase';
import { qk } from '../../lib/queryClient';
import { LeadRow } from '../../components/rows';
import { Chip, ChipRow, EmptyState, ErrorState, Loading, Screen } from '../../components/ui';
import { STAGES, countByStage, sortLeads } from '../../utils/pipeline';
import { colors, radius, space, type } from '../../theme';

type Filter = InquiryStage | 'all' | 'unread';

export default function LeadsScreen() {
  const [filter, setFilter] = useState<Filter>('all');
  const [search, setSearch] = useState('');
  const q = useQuery({ queryKey: qk.leads, queryFn: () => fetchLeads(getSupabase()) });

  const leads = q.data ?? [];
  const counts = useMemo(() => countByStage(leads), [leads]);
  const visible = useMemo(() => {
    const s = search.trim().toLowerCase();
    const list = leads.filter((l) =>
      (filter === 'all' || (filter === 'unread' ? !l.read : l.stage === filter))
      && (!s || l.name.toLowerCase().includes(s) || l.email.toLowerCase().includes(s)
        || (l.listings?.title.toLowerCase().includes(s) ?? false)));
    return sortLeads(list);
  }, [leads, filter, search]);

  return (
    <Screen refreshing={q.isRefetching} onRefresh={() => q.refetch()}>
      <View style={styles.funnel}>
        {STAGES.map((st, i) => {
          const n = counts[st];
          const max = Math.max(1, ...STAGES.map((x) => counts[x]));
          return (
            <View key={st} style={styles.funnelCol}>
              <View style={styles.funnelTrack}>
                <View style={[styles.funnelBar, { height: `${Math.max(6, (n / max) * 100)}%`, backgroundColor: i >= 4 ? (st === 'closed_won' ? colors.ok : colors.faint) : colors.coral, opacity: filter === st || filter === 'all' ? 1 : 0.35 }]} />
              </View>
              <Text style={styles.funnelNum}>{n}</Text>
            </View>
          );
        })}
      </View>

      <ChipRow>
        <Chip label="Todos" count={counts.all} active={filter === 'all'} onPress={() => setFilter('all')} testID="stage-all" />
        <Chip label="Não lidos" count={counts.unread} active={filter === 'unread'} onPress={() => setFilter('unread')} testID="stage-unread" />
        {STAGES.map((st) => (
          <Chip key={st} label={STAGE_LABEL[st]} count={counts[st]} active={filter === st} onPress={() => setFilter(st)} testID={`stage-${st}`} />
        ))}
      </ChipRow>

      <TextInput
        value={search}
        onChangeText={setSearch}
        placeholder="Buscar por nome, e-mail ou imóvel"
        placeholderTextColor={colors.faint}
        style={styles.search}
        autoCapitalize="none"
      />

      {q.isLoading ? <Loading /> : q.error ? <ErrorState error={q.error} onRetry={() => q.refetch()} /> : visible.length ? (
        <>
          <Text style={type.small}>{visible.length} {visible.length === 1 ? 'lead' : 'leads'}</Text>
          {visible.map((l) => (
            <LeadRow key={l.id} lead={l} onPress={() => router.push({ pathname: '/lead/[id]', params: { id: String(l.id) } })} />
          ))}
        </>
      ) : <EmptyState title="Nenhum lead aqui" hint={filter === 'all' ? 'Contatos do site e do app aparecem nesta lista.' : 'Tente outro estágio do funil.'} />}
    </Screen>
  );
}

const styles = StyleSheet.create({
  funnel: {
    flexDirection: 'row', gap: 6, height: 64, backgroundColor: colors.navy, borderRadius: radius.lg,
    paddingHorizontal: space.md, paddingTop: space.sm, paddingBottom: 4,
  },
  funnelCol: { flex: 1, alignItems: 'center' },
  funnelTrack: { flex: 1, width: '100%', justifyContent: 'flex-end' },
  funnelBar: { width: '100%', borderRadius: 3 },
  funnelNum: { color: colors.white, fontSize: 11, fontWeight: '800', marginTop: 2, ...type.num },
  search: {
    height: 40, borderRadius: radius.md, borderWidth: 1, borderColor: colors.line, backgroundColor: colors.white,
    paddingHorizontal: space.md, fontSize: 14, color: colors.text,
  },
});
