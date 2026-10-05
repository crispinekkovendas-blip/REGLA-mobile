import { Pressable, StyleSheet, Text, View } from 'react-native';
import { colors, radius, space, type } from '../theme';

export function StatTile({ label, value, accent = colors.coral, hint, onPress, testID }: {
  label: string;
  value: number | null | undefined;
  accent?: string;
  hint?: string;
  onPress?: () => void;
  testID?: string;
}) {
  return (
    <Pressable
      testID={testID}
      onPress={onPress}
      disabled={!onPress}
      accessibilityRole={onPress ? 'button' : undefined}
      accessibilityLabel={`${label}: ${value ?? '—'}`}
      style={({ pressed }) => [styles.tile, pressed && { opacity: 0.8 }]}
    >
      <View style={[styles.bar, { backgroundColor: accent }]} />
      <Text style={styles.value} testID={testID ? `${testID}-value` : undefined}>
        {value === null || value === undefined ? '—' : String(value)}
      </Text>
      <Text style={styles.label} numberOfLines={2}>{label}</Text>
      {hint ? <Text style={styles.hint} numberOfLines={1}>{hint}</Text> : null}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  tile: {
    flexBasis: '47%',
    flexGrow: 1,
    backgroundColor: colors.navyMid,
    borderRadius: radius.lg,
    padding: space.md,
    paddingTop: space.md + 2,
    minHeight: 96,
    overflow: 'hidden',
  },
  bar: { position: 'absolute', left: 0, top: 0, bottom: 0, width: 3 },
  value: { ...type.display, color: colors.white, fontSize: 30 },
  label: { fontSize: 12, fontWeight: '600', color: colors.onNavyMuted, marginTop: 2 },
  hint: { fontSize: 11, color: colors.onNavyMuted, opacity: 0.7, marginTop: 2 },
});

export type StatKey = 'newLeads' | 'pendingApplications' | 'visitsToday' | 'liveListings';

export const STAT_TILES: { key: StatKey; label: string; accent: string }[] = [
  { key: 'newLeads', label: 'Novos leads', accent: colors.coral },
  { key: 'pendingApplications', label: 'Propostas pendentes', accent: colors.warn },
  { key: 'visitsToday', label: 'Visitas hoje', accent: colors.info },
  { key: 'liveListings', label: 'Imóveis ativos', accent: colors.ok },
];

/** 2×2 grid of dashboard stat tiles. `stats` undefined → placeholders. */
export function StatGrid({ stats, onPress }: {
  stats: Record<StatKey, number> | undefined;
  onPress?: (key: StatKey) => void;
}) {
  return (
    <View style={gridStyles.grid}>
      {STAT_TILES.map((t) => (
        <StatTile
          key={t.key}
          testID={`stat-${t.key}`}
          label={t.label}
          accent={t.accent}
          value={stats?.[t.key]}
          onPress={onPress ? () => onPress(t.key) : undefined}
        />
      ))}
    </View>
  );
}

const gridStyles = StyleSheet.create({
  grid: { flexDirection: 'row', flexWrap: 'wrap', gap: space.sm },
});
