import { useMemo } from 'react';
import { Pressable, RefreshControl, ScrollView, StyleSheet, Text, View } from 'react-native';
import { router } from 'expo-router';
import { SHOWING_STATUS_LABEL, listingRef, type ShowingStatus, type ShowingWithListing } from '@regla/shared';
import { colors, radius, shadow, space, type } from '@/theme';
import { useAuth } from '@/lib/auth';
import { errorText, useVisits } from '@/lib/queries';
import { LoginPrompt } from '@/components/LoginPrompt';
import { ScreenTitle } from '@/components/ScreenTitle';
import { Badge, Button, EmptyState, ErrorBox, Loading, type Tone } from '@/components/ui';
import { Icon } from '@/components/Icon';

const TONE: Record<ShowingStatus, Tone> = {
  scheduled: 'info',
  confirmed: 'success',
  attended: 'neutral',
  no_show: 'danger',
  cancelled: 'danger',
};

const MONTHS = ['JAN', 'FEV', 'MAR', 'ABR', 'MAI', 'JUN', 'JUL', 'AGO', 'SET', 'OUT', 'NOV', 'DEZ'];
const WEEKDAYS = ['Domingo', 'Segunda', 'Terça', 'Quarta', 'Quinta', 'Sexta', 'Sábado'];

function VisitRow({ v }: { v: ShowingWithListing }) {
  const d = new Date(v.starts_at);
  const time = `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
  return (
    <Pressable
      style={({ pressed }) => [styles.row, pressed && { opacity: 0.9 }]}
      onPress={() => v.listing_id && router.push(`/listing/${v.listing_id}`)}
      accessibilityRole="button"
    >
      <View style={styles.date}>
        <Text style={styles.dateMonth}>{MONTHS[d.getMonth()]}</Text>
        <Text style={styles.dateDay}>{String(d.getDate()).padStart(2, '0')}</Text>
      </View>
      <View style={{ flex: 1, gap: 3 }}>
        <Text style={type.h3} numberOfLines={1}>
          {v.listings?.title ?? `Imóvel ${listingRef(v.listing_id)}`}
        </Text>
        <View style={{ flexDirection: 'row', alignItems: 'center', gap: 4 }}>
          <Icon name="clock" size={13} color={colors.muted} />
          <Text style={type.small}>
            {WEEKDAYS[d.getDay()]} · {time} · {v.duration_minutes} min
          </Text>
        </View>
        {v.listings ? <Text style={type.small} numberOfLines={1}>{[v.listings.neighborhood, v.listings.city].filter(Boolean).join(' · ')}</Text> : null}
        <View style={{ marginTop: 4 }}>
          <Badge label={SHOWING_STATUS_LABEL[v.status]} tone={TONE[v.status]} />
        </View>
      </View>
      <Icon name="chevron-right" size={16} color={colors.faint} />
    </Pressable>
  );
}

export default function VisitsScreen() {
  const { user } = useAuth();
  const visits = useVisits();

  const { upcoming, past } = useMemo(() => {
    const now = Date.now();
    const list = visits.data ?? [];
    return {
      upcoming: list.filter((v) => new Date(v.starts_at).getTime() >= now && v.status !== 'cancelled'),
      past: list.filter((v) => new Date(v.starts_at).getTime() < now || v.status === 'cancelled').reverse(),
    };
  }, [visits.data]);

  if (!user) return <LoginPrompt icon="calendar" title="Suas visitas" text="Entre para agendar visitas e acompanhar a confirmação do corretor." />;

  return (
    <ScrollView
      style={{ flex: 1, backgroundColor: colors.bg }}
      contentContainerStyle={{ paddingBottom: space.xxxl }}
      refreshControl={<RefreshControl refreshing={visits.isRefetching} onRefresh={() => visits.refetch()} tintColor={colors.navy} />}
    >
      <ScreenTitle title="Visitas" subtitle="Seus horários agendados" />
      <View style={{ paddingHorizontal: space.lg }}>
        {visits.error ? <ErrorBox message={errorText(visits.error)} onRetry={() => visits.refetch()} /> : null}
        {visits.isLoading ? (
          <Loading />
        ) : !upcoming.length && !past.length ? (
          <EmptyState
            icon="calendar"
            title="Nenhuma visita agendada"
            text="Encontrou um imóvel interessante? Agende uma visita em poucos toques."
            action={<Button title="Buscar imóveis" variant="accent" onPress={() => router.navigate('/')} />}
          />
        ) : (
          <>
            <Text style={styles.group}>Próximas</Text>
            {upcoming.length ? upcoming.map((v) => <VisitRow key={v.id} v={v} />) : <Text style={[type.small, { marginBottom: space.lg }]}>Nenhuma visita futura.</Text>}
            {past.length ? (
              <>
                <Text style={styles.group}>Histórico</Text>
                {past.map((v) => <VisitRow key={v.id} v={v} />)}
              </>
            ) : null}
          </>
        )}
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  group: { ...type.label, marginTop: space.sm, marginBottom: space.md },
  row: { flexDirection: 'row', alignItems: 'center', gap: space.lg, backgroundColor: colors.white, borderRadius: radius.lg, padding: space.lg, marginBottom: space.md, ...shadow },
  date: { width: 58, height: 64, borderRadius: radius.md, backgroundColor: colors.navy, alignItems: 'center', justifyContent: 'center' },
  dateMonth: { color: colors.coral, fontSize: 11, fontWeight: '800', letterSpacing: 1 },
  dateDay: { color: colors.white, fontSize: 24, fontWeight: '800', lineHeight: 28 },
});
