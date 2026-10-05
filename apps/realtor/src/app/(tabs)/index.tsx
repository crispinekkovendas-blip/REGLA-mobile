import { useCallback, type ReactNode } from 'react';
import { Pressable, RefreshControl, ScrollView, StyleSheet, Text, View } from 'react-native';
import { router } from 'expo-router';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { fetchAgenda, fetchApplicationsForReview, fetchDashboardStats } from '@regla/shared';
import { getSupabase } from '../../lib/supabase';
import { qk } from '../../lib/queryClient';
import { useAuth } from '../../lib/auth';
import { useRealtimeInbox } from '../../lib/realtime';
import { StatGrid, type StatKey } from '../../components/StatTile';
import { ApplicationRow, ShowingItem } from '../../components/rows';
import { EmptyState, ErrorState, Loading, SectionHeader } from '../../components/ui';
import { addDays, dayKey, startOfDay } from '../../utils/agenda';
import { colors, space } from '../../theme';

const STAT_ROUTES: Record<StatKey, string> = {
  newLeads: '/leads',
  pendingApplications: '/propostas',
  visitsToday: '/agenda',
  liveListings: '/imoveis',
};

function greeting(d = new Date()) {
  const h = d.getHours();
  return h < 12 ? 'Bom dia' : h < 18 ? 'Boa tarde' : 'Boa noite';
}

export default function DashboardScreen() {
  const { email, signOut } = useAuth();
  const qc = useQueryClient();
  const live = useRealtimeInbox(true);
  const today = startOfDay(new Date());
  const fromKey = dayKey(today);

  const stats = useQuery({ queryKey: qk.stats, queryFn: () => fetchDashboardStats(getSupabase()) });
  const agenda = useQuery({
    queryKey: qk.agenda(fromKey),
    queryFn: () => fetchAgenda(getSupabase(), today.toISOString(), addDays(today, 7).toISOString()),
  });
  const apps = useQuery({ queryKey: qk.applications, queryFn: () => fetchApplicationsForReview(getSupabase()) });

  const refreshing = stats.isRefetching || agenda.isRefetching || apps.isRefetching;
  const onRefresh = useCallback(() => {
    qc.invalidateQueries({ queryKey: qk.stats });
    qc.invalidateQueries({ queryKey: qk.agendaAll });
    qc.invalidateQueries({ queryKey: qk.applications });
  }, [qc]);

  const now = Date.now();
  const upcoming = (agenda.data ?? [])
    .filter((s) => (s.status === 'scheduled' || s.status === 'confirmed')
      && Date.parse(s.starts_at) + s.duration_minutes * 60_000 >= now)
    .slice(0, 4);
  const recent = (apps.data ?? []).slice(0, 4);
  const firstName = email?.split('@')[0]?.split(/[._-]/)[0] ?? '';

  return (
      <View style={{ flex: 1, backgroundColor: colors.navySoft }}>
        <ScrollHost refreshing={refreshing} onRefresh={onRefresh}>
          <View style={styles.hero}>
            <View style={styles.heroTop}>
              <View style={{ flex: 1 }}>
                <Text style={styles.hello}>{greeting()}{firstName ? `, ${capitalize(firstName)}` : ''}</Text>
                <Text style={styles.heroSub}>Resumo do dia</Text>
              </View>
              <View style={styles.liveWrap} testID="realtime-indicator">
                <View style={[styles.liveDot, { backgroundColor: live ? colors.ok : colors.onNavyMuted }]} />
                <Text style={styles.liveText}>{live ? 'Ao vivo' : 'Offline'}</Text>
              </View>
              <Pressable onPress={() => { qc.clear(); signOut(); }} accessibilityRole="button" style={styles.signOut}>
                <Text style={styles.signOutText}>Sair</Text>
              </Pressable>
            </View>
            {stats.error ? <ErrorState error={stats.error} onRetry={() => stats.refetch()} /> : (
              <StatGrid stats={stats.data} onPress={(k) => router.push(STAT_ROUTES[k] as never)} />
            )}
          </View>

          <View style={styles.body}>
            <SectionHeader
              title="Próximas visitas"
              right={<Text style={styles.link} onPress={() => router.push('/agenda')}>Ver agenda</Text>}
            />
            {agenda.isLoading ? <Loading /> : agenda.error ? <ErrorState error={agenda.error} onRetry={() => agenda.refetch()} /> : upcoming.length ? (
              upcoming.map((s) => <ShowingItem key={s.id} showing={s} compact showDay />)
            ) : <EmptyState title="Nenhuma visita nos próximos 7 dias" />}

            <SectionHeader
              title="Propostas recentes"
              right={<Text style={styles.link} onPress={() => router.push('/propostas')}>Ver todas</Text>}
              style={{ marginTop: space.lg }}
            />
            {apps.isLoading ? <Loading /> : apps.error ? <ErrorState error={apps.error} onRetry={() => apps.refetch()} /> : recent.length ? (
              recent.map((a) => (
                <ApplicationRow key={a.id} app={a} onPress={() => router.push({ pathname: '/application/[id]', params: { id: String(a.id) } })} />
              ))
            ) : <EmptyState title="Nenhuma proposta ainda" hint="As propostas enviadas pelo app do cliente aparecem aqui." />}
          </View>
        </ScrollHost>
      </View>
  );
}

function capitalize(s: string) {
  return s ? s[0]!.toUpperCase() + s.slice(1) : s;
}

// Local scroll host: the hero is edge-to-edge navy so we can't use <Screen>'s padding.
function ScrollHost({ children, refreshing, onRefresh }: { children: ReactNode; refreshing: boolean; onRefresh: () => void }) {
  return (
    <ScrollView
      contentContainerStyle={{ paddingBottom: space.xxl * 2 }}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={colors.white} colors={[colors.coral]} />}
    >
      {children}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  hero: { backgroundColor: colors.navy, paddingHorizontal: space.lg, paddingBottom: space.xl, paddingTop: space.sm, gap: space.md },
  heroTop: { flexDirection: 'row', alignItems: 'center', gap: space.sm },
  hello: { color: colors.white, fontSize: 22, fontWeight: '800', letterSpacing: -0.4 },
  heroSub: { color: colors.onNavyMuted, fontSize: 13 },
  liveWrap: { flexDirection: 'row', alignItems: 'center', gap: 5, paddingHorizontal: 8, paddingVertical: 4, borderRadius: 999, backgroundColor: colors.navyMid },
  liveDot: { width: 7, height: 7, borderRadius: 4 },
  liveText: { color: colors.white, fontSize: 11, fontWeight: '700' },
  signOut: { paddingHorizontal: 10, paddingVertical: 6, borderRadius: 6, borderWidth: 1, borderColor: colors.navyMid },
  signOutText: { color: colors.onNavyMuted, fontSize: 12, fontWeight: '700' },
  body: { padding: space.lg, gap: space.md },
  link: { color: colors.coral, fontWeight: '700', fontSize: 13 },
});
