import { useMemo, useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { fetchAgenda, updateShowingStatus, type ShowingStatus } from '@regla/shared';
import { getSupabase } from '../../lib/supabase';
import { qk } from '../../lib/queryClient';
import { ShowingItem } from '../../components/rows';
import { ErrorState, InlineError, Loading, Screen } from '../../components/ui';
import { addDays, dayKey, dayLabel, groupByDay, shortDate, startOfDay } from '../../utils/agenda';
import { colors, radius, space, type } from '../../theme';

export default function AgendaScreen() {
  const qc = useQueryClient();
  const today = useMemo(() => startOfDay(new Date()), []);
  const fromKey = dayKey(today);
  const [hideDone, setHideDone] = useState(false);

  const q = useQuery({
    queryKey: qk.agenda(fromKey),
    queryFn: () => fetchAgenda(getSupabase(), today.toISOString(), addDays(today, 7).toISOString()),
  });

  const m = useMutation({
    mutationFn: (v: { id: number; status: ShowingStatus }) => updateShowingStatus(getSupabase(), v.id, v.status),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: qk.agendaAll });
      qc.invalidateQueries({ queryKey: qk.stats });
    },
  });

  const groups = useMemo(() => {
    const items = (q.data ?? []).filter((s) => !hideDone || s.status === 'scheduled' || s.status === 'confirmed');
    return groupByDay(items, today, 7);
  }, [q.data, today, hideDone]);
  const total = groups.reduce((n, g) => n + g.items.length, 0);

  return (
    <Screen refreshing={q.isRefetching} onRefresh={() => q.refetch()}>
      <View style={styles.week}>
        {groups.map((g) => (
          <View key={g.key} style={[styles.weekDay, g.key === fromKey && styles.weekToday]}>
            <Text style={[styles.weekLabel, g.key === fromKey && { color: colors.white }]}>
              {['D', 'S', 'T', 'Q', 'Q', 'S', 'S'][g.date.getDay()]}
            </Text>
            <Text style={[styles.weekNum, g.key === fromKey && { color: colors.white }]}>{g.date.getDate()}</Text>
            <View style={[styles.weekDot, { opacity: g.items.length ? 1 : 0 }]} />
          </View>
        ))}
      </View>

      <View style={styles.toolbar}>
        <Text style={type.small}>{total} {total === 1 ? 'visita' : 'visitas'} nos próximos 7 dias</Text>
        <Text style={styles.toggle} onPress={() => setHideDone((v) => !v)} accessibilityRole="button">
          {hideDone ? 'Mostrar todas' : 'Só pendentes'}
        </Text>
      </View>
      <InlineError message={m.error ? (m.error as Error).message : null} />

      {q.isLoading ? <Loading /> : q.error ? <ErrorState error={q.error} onRetry={() => q.refetch()} /> : groups.map((g) => (
        <View key={g.key} style={{ gap: space.sm }} testID={`day-${g.key}`}>
          <View style={styles.dayHead}>
            <Text style={styles.dayTitle}>{dayLabel(g.date)}</Text>
            <Text style={type.small}>{shortDate(g.date)}</Text>
            <View style={styles.dayLine} />
            <Text style={styles.dayCount}>{g.items.length}</Text>
          </View>
          {g.items.length ? g.items.map((s) => (
            <ShowingItem
              key={s.id}
              showing={s}
              busy={m.isPending && m.variables?.id === s.id}
              onStatus={(status) => m.mutate({ id: s.id, status })}
            />
          )) : <Text style={styles.free}>Sem visitas</Text>}
        </View>
      ))}
    </Screen>
  );
}

const styles = StyleSheet.create({
  week: { flexDirection: 'row', backgroundColor: colors.navy, borderRadius: radius.lg, padding: 6, gap: 4 },
  weekDay: { flex: 1, alignItems: 'center', paddingVertical: 6, borderRadius: radius.md },
  weekToday: { backgroundColor: colors.coral },
  weekLabel: { fontSize: 10, fontWeight: '700', color: colors.onNavyMuted },
  weekNum: { fontSize: 16, fontWeight: '800', color: colors.white, ...type.num },
  weekDot: { width: 4, height: 4, borderRadius: 2, backgroundColor: colors.white, marginTop: 3 },
  toolbar: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  toggle: { color: colors.coral, fontWeight: '700', fontSize: 13 },
  dayHead: { flexDirection: 'row', alignItems: 'center', gap: space.sm, marginTop: space.sm },
  dayTitle: { ...type.h2, textTransform: 'capitalize' },
  dayLine: { flex: 1, height: StyleSheet.hairlineWidth, backgroundColor: colors.line },
  dayCount: { fontSize: 12, fontWeight: '800', color: colors.muted, ...type.num },
  free: { fontSize: 13, color: colors.faint, paddingLeft: 2 },
});
