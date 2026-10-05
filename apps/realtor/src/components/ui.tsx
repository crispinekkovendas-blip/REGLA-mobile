import type { ReactNode } from 'react';
import {
  ActivityIndicator, Pressable, RefreshControl, ScrollView, StyleSheet, Text, View,
  type StyleProp, type ViewStyle,
} from 'react-native';
import { colors, radius, shadow, space, tones, type, type Tone } from '../theme';
import { initials } from '../utils/labels';

// ─── Layout ───────────────────────────────────────────────────────────

export function Screen({
  children, refreshing = false, onRefresh, contentStyle, header,
}: {
  children: ReactNode;
  refreshing?: boolean;
  onRefresh?: () => void;
  contentStyle?: StyleProp<ViewStyle>;
  header?: ReactNode;
}) {
  return (
    <ScrollView
      style={styles.screen}
      contentContainerStyle={[styles.screenContent, contentStyle]}
      keyboardShouldPersistTaps="handled"
      refreshControl={onRefresh ? (
        <RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={colors.coral} colors={[colors.coral]} />
      ) : undefined}
    >
      {header}
      {children}
    </ScrollView>
  );
}

export function Card({ children, style, onPress, testID }: {
  children: ReactNode; style?: StyleProp<ViewStyle>; onPress?: () => void; testID?: string;
}) {
  if (onPress) {
    return (
      <Pressable
        testID={testID}
        onPress={onPress}
        accessibilityRole="button"
        style={({ pressed }) => [styles.card, pressed && styles.pressed, style]}
      >
        {children}
      </Pressable>
    );
  }
  return <View testID={testID} style={[styles.card, style]}>{children}</View>;
}

export function SectionHeader({ title, right, style }: { title: string; right?: ReactNode; style?: StyleProp<ViewStyle> }) {
  return (
    <View style={[styles.sectionHeader, style]}>
      <Text style={type.over}>{title}</Text>
      {right}
    </View>
  );
}

export function Divider({ style }: { style?: StyleProp<ViewStyle> }) {
  return <View style={[styles.divider, style]} />;
}

export function Row({ children, style, gap = space.sm }: { children: ReactNode; style?: StyleProp<ViewStyle>; gap?: number }) {
  return <View style={[{ flexDirection: 'row', alignItems: 'center', gap }, style]}>{children}</View>;
}

// ─── Status & chips ───────────────────────────────────────────────────

export function Pill({ label, tone = 'neutral', dot = true, testID }: { label: string; tone?: Tone; dot?: boolean; testID?: string }) {
  const t = tones[tone];
  return (
    <View testID={testID} style={[styles.pill, { backgroundColor: t.bg }]}>
      {dot && tone !== 'navy' ? <View style={[styles.pillDot, { backgroundColor: t.dot }]} /> : null}
      <Text style={[styles.pillText, { color: t.fg }]} numberOfLines={1}>{label}</Text>
    </View>
  );
}

export function Chip({ label, count, active, onPress, testID }: {
  label: string; count?: number; active?: boolean; onPress?: () => void; testID?: string;
}) {
  return (
    <Pressable
      testID={testID}
      onPress={onPress}
      accessibilityRole="button"
      accessibilityState={{ selected: !!active }}
      style={({ pressed }) => [styles.chip, active && styles.chipActive, pressed && styles.pressed]}
    >
      <Text style={[styles.chipText, active && styles.chipTextActive]}>{label}</Text>
      {count !== undefined ? (
        <View style={[styles.chipCount, active && styles.chipCountActive]}>
          <Text style={[styles.chipCountText, active && styles.chipCountTextActive]}>{count}</Text>
        </View>
      ) : null}
    </Pressable>
  );
}

export function ChipRow({ children, style }: { children: ReactNode; style?: StyleProp<ViewStyle> }) {
  return (
    <ScrollView
      horizontal
      showsHorizontalScrollIndicator={false}
      style={[styles.chipRowWrap, style]}
      contentContainerStyle={styles.chipRow}
    >
      {children}
    </ScrollView>
  );
}

/** Wrapping group of selectable options (stage / priority / status selectors). */
export function Segmented<T extends string>({ options, value, onChange, labels, disabled, testIDPrefix }: {
  options: readonly T[];
  value: T | null;
  onChange: (v: T) => void;
  labels: Record<T, string>;
  disabled?: boolean;
  testIDPrefix?: string;
}) {
  return (
    <View style={styles.segWrap}>
      {options.map((o) => {
        const on = o === value;
        return (
          <Pressable
            key={o}
            testID={testIDPrefix ? `${testIDPrefix}-${o}` : undefined}
            disabled={disabled || on}
            onPress={() => onChange(o)}
            accessibilityRole="button"
            accessibilityState={{ selected: on, disabled: !!disabled }}
            style={({ pressed }) => [styles.seg, on && styles.segOn, pressed && styles.pressed, disabled && !on && { opacity: 0.5 }]}
          >
            <Text style={[styles.segText, on && styles.segTextOn]}>{labels[o]}</Text>
          </Pressable>
        );
      })}
    </View>
  );
}

// ─── Buttons ──────────────────────────────────────────────────────────

type ButtonVariant = 'primary' | 'coral' | 'secondary' | 'ghost' | 'danger' | 'ok';

export function Button({
  label, onPress, variant = 'primary', disabled, loading, small, icon, style, testID, flex,
}: {
  label: string;
  onPress?: () => void;
  variant?: ButtonVariant;
  disabled?: boolean;
  loading?: boolean;
  small?: boolean;
  icon?: ReactNode;
  style?: StyleProp<ViewStyle>;
  testID?: string;
  flex?: boolean;
}) {
  const v = BUTTON_VARIANTS[variant];
  const off = disabled || loading;
  return (
    <Pressable
      testID={testID}
      onPress={onPress}
      disabled={off}
      accessibilityRole="button"
      accessibilityLabel={label}
      accessibilityState={{ disabled: !!off, busy: !!loading }}
      style={({ pressed }) => [
        styles.btn,
        small && styles.btnSmall,
        { backgroundColor: v.bg, borderColor: v.border },
        flex && { flex: 1 },
        pressed && styles.pressed,
        off && { opacity: 0.5 },
        style,
      ]}
    >
      {loading ? <ActivityIndicator size="small" color={v.fg} /> : icon}
      <Text style={[styles.btnText, small && styles.btnTextSmall, { color: v.fg }]} numberOfLines={1}>{label}</Text>
    </Pressable>
  );
}

const BUTTON_VARIANTS: Record<ButtonVariant, { bg: string; fg: string; border: string }> = {
  primary: { bg: colors.navy, fg: colors.white, border: colors.navy },
  coral: { bg: colors.coral, fg: colors.white, border: colors.coral },
  secondary: { bg: colors.white, fg: colors.navy, border: colors.line },
  ghost: { bg: 'transparent', fg: colors.navy, border: 'transparent' },
  danger: { bg: colors.white, fg: colors.danger, border: '#F5C2C2' },
  ok: { bg: '#0F9D63', fg: colors.white, border: '#0F9D63' },
};

// ─── Data display ─────────────────────────────────────────────────────

export function Field({ label, value, mono, testID }: { label: string; value: ReactNode; mono?: boolean; testID?: string }) {
  return (
    <View style={styles.field} testID={testID}>
      <Text style={styles.fieldLabel}>{label}</Text>
      {typeof value === 'string' || typeof value === 'number'
        ? <Text style={[styles.fieldValue, mono && type.num]} selectable>{value}</Text>
        : value}
    </View>
  );
}

export function Avatar({ name, size = 36 }: { name: string; size?: number }) {
  const ini = initials(name);
  return (
    <View style={[styles.avatar, { width: size, height: size, borderRadius: size / 2 }]}>
      <Text style={[styles.avatarText, { fontSize: size * 0.38 }]}>{ini}</Text>
    </View>
  );
}

export function EmptyState({ title, hint }: { title: string; hint?: string }) {
  return (
    <View style={styles.empty}>
      <View style={styles.emptyMark} />
      <Text style={styles.emptyTitle}>{title}</Text>
      {hint ? <Text style={styles.emptyHint}>{hint}</Text> : null}
    </View>
  );
}

export function ErrorState({ error, onRetry }: { error: unknown; onRetry?: () => void }) {
  const msg = error instanceof Error ? error.message : String(error ?? 'Erro desconhecido');
  return (
    <View style={styles.errorBox}>
      <Text style={styles.errorTitle}>Não foi possível carregar</Text>
      <Text style={styles.errorMsg}>{msg}</Text>
      {onRetry ? <Button small variant="secondary" label="Tentar novamente" onPress={onRetry} style={{ alignSelf: 'flex-start', marginTop: space.sm }} /> : null}
    </View>
  );
}

export function Loading({ label = 'Carregando…' }: { label?: string }) {
  return (
    <View style={styles.loading}>
      <ActivityIndicator color={colors.coral} />
      <Text style={type.small}>{label}</Text>
    </View>
  );
}

export function InlineError({ message }: { message: string | null | undefined }) {
  if (!message) return null;
  return <Text style={styles.inlineError}>{message}</Text>;
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.navySoft },
  screenContent: { padding: space.lg, paddingBottom: space.xxl * 2, gap: space.md },
  card: {
    backgroundColor: colors.white,
    borderRadius: radius.lg,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: colors.line,
    padding: space.lg,
    ...shadow,
  },
  pressed: { opacity: 0.75 },
  sectionHeader: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginTop: space.sm },
  divider: { height: StyleSheet.hairlineWidth, backgroundColor: colors.line, marginVertical: space.md },
  pill: {
    flexDirection: 'row', alignItems: 'center', gap: 5, alignSelf: 'flex-start',
    paddingHorizontal: 8, paddingVertical: 3, borderRadius: radius.pill,
  },
  pillDot: { width: 6, height: 6, borderRadius: 3 },
  pillText: { fontSize: 11, fontWeight: '700', letterSpacing: 0.2 },
  chipRowWrap: { flexGrow: 0, marginHorizontal: -space.lg },
  chipRow: { paddingHorizontal: space.lg, gap: space.sm },
  chip: {
    flexDirection: 'row', alignItems: 'center', gap: 6, paddingLeft: 12, paddingRight: 6, height: 32,
    borderRadius: radius.pill, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.line,
  },
  chipActive: { backgroundColor: colors.navy, borderColor: colors.navy },
  chipText: { fontSize: 13, fontWeight: '600', color: colors.text },
  chipTextActive: { color: colors.white },
  chipCount: { minWidth: 22, height: 20, paddingHorizontal: 6, borderRadius: 10, backgroundColor: colors.navySoft, alignItems: 'center', justifyContent: 'center' },
  chipCountActive: { backgroundColor: colors.coral },
  chipCountText: { fontSize: 11, fontWeight: '800', color: colors.muted, ...type.num },
  chipCountTextActive: { color: colors.white },
  segWrap: { flexDirection: 'row', flexWrap: 'wrap', gap: 6 },
  seg: { paddingHorizontal: 12, paddingVertical: 7, borderRadius: radius.sm, borderWidth: 1, borderColor: colors.line, backgroundColor: colors.white },
  segOn: { backgroundColor: colors.navy, borderColor: colors.navy },
  segText: { fontSize: 13, fontWeight: '600', color: colors.text },
  segTextOn: { color: colors.white },
  btn: {
    flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 8,
    height: 46, paddingHorizontal: space.lg, borderRadius: radius.md, borderWidth: 1,
  },
  btnSmall: { height: 34, paddingHorizontal: space.md, borderRadius: radius.sm },
  btnText: { fontSize: 15, fontWeight: '700' },
  btnTextSmall: { fontSize: 13 },
  field: { paddingVertical: 6, gap: 2, minWidth: '45%', flexGrow: 1, flexBasis: '45%' },
  fieldLabel: { ...type.over, fontSize: 10 },
  fieldValue: { fontSize: 14, fontWeight: '600', color: colors.text },
  avatar: { backgroundColor: colors.navy, alignItems: 'center', justifyContent: 'center' },
  avatarText: { color: colors.white, fontWeight: '800' },
  empty: { alignItems: 'center', paddingVertical: space.xxl, gap: 6 },
  emptyMark: { width: 28, height: 4, borderRadius: 2, backgroundColor: colors.coral, marginBottom: 6 },
  emptyTitle: { ...type.h2, color: colors.muted },
  emptyHint: { ...type.small, textAlign: 'center', maxWidth: 260 },
  errorBox: { backgroundColor: tones.danger.bg, borderRadius: radius.md, padding: space.lg, borderLeftWidth: 3, borderLeftColor: colors.danger },
  errorTitle: { fontWeight: '700', color: tones.danger.fg, marginBottom: 2 },
  errorMsg: { fontSize: 13, color: tones.danger.fg },
  loading: { alignItems: 'center', paddingVertical: space.xxl, gap: space.sm },
  inlineError: { color: colors.danger, fontSize: 13, fontWeight: '600' },
});
