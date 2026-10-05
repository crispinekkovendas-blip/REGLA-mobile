import { forwardRef, type ReactNode } from 'react';
import {
  ActivityIndicator, Pressable, StyleSheet, Text, TextInput, View,
  type StyleProp, type TextInputProps, type ViewStyle,
} from 'react-native';
import { colors, radius, shadow, space, type } from '@/theme';
import { Icon, type IconName } from './Icon';

// ─── Button ───────────────────────────────────────────────────────────

type Variant = 'primary' | 'accent' | 'outline' | 'ghost' | 'danger';

interface ButtonProps {
  title: string;
  onPress?: () => void;
  variant?: Variant;
  loading?: boolean;
  disabled?: boolean;
  icon?: IconName;
  style?: StyleProp<ViewStyle>;
  small?: boolean;
  testID?: string;
}

export function Button({ title, onPress, variant = 'primary', loading, disabled, icon, style, small, testID }: ButtonProps) {
  const palette = {
    primary: { bg: colors.navy, fg: colors.white, border: colors.navy },
    accent: { bg: colors.coral, fg: colors.white, border: colors.coral },
    outline: { bg: colors.white, fg: colors.navy, border: colors.line },
    ghost: { bg: 'transparent', fg: colors.navy, border: 'transparent' },
    danger: { bg: colors.white, fg: colors.danger, border: colors.dangerSoft },
  }[variant];
  const inactive = disabled || loading;
  return (
    <Pressable
      testID={testID}
      accessibilityRole="button"
      accessibilityState={{ disabled: !!inactive, busy: !!loading }}
      onPress={inactive ? undefined : onPress}
      style={({ pressed }) => [
        styles.btn,
        small && styles.btnSmall,
        { backgroundColor: palette.bg, borderColor: palette.border, opacity: inactive ? 0.55 : pressed ? 0.85 : 1 },
        pressed && !inactive && { transform: [{ scale: 0.985 }] },
        style,
      ]}
    >
      {loading ? (
        <ActivityIndicator color={palette.fg} />
      ) : (
        <View style={styles.btnInner}>
          {icon ? <Icon name={icon} size={small ? 16 : 18} color={palette.fg} /> : null}
          <Text style={[styles.btnText, small && { fontSize: 14 }, { color: palette.fg }]}>{title}</Text>
        </View>
      )}
    </Pressable>
  );
}

// ─── Field ────────────────────────────────────────────────────────────

interface FieldProps extends TextInputProps {
  label: string;
  error?: string;
  hint?: string;
  prefix?: string;
}

export const Field = forwardRef<TextInput, FieldProps>(function Field({ label, error, hint, prefix, style, ...rest }, ref) {
  return (
    <View style={styles.field}>
      <Text style={styles.fieldLabel}>{label}</Text>
      <View style={[styles.inputWrap, !!error && styles.inputError]}>
        {prefix ? <Text style={styles.prefix}>{prefix}</Text> : null}
        <TextInput
          ref={ref}
          accessibilityLabel={label}
          placeholderTextColor={colors.faint}
          style={[styles.input, rest.multiline && { minHeight: 96, textAlignVertical: 'top', paddingTop: 12 }, style]}
          {...rest}
        />
      </View>
      {error ? (
        <Text style={styles.errorText} accessibilityRole="alert">
          {error}
        </Text>
      ) : hint ? (
        <Text style={styles.hint}>{hint}</Text>
      ) : null}
    </View>
  );
});

// ─── Chip ─────────────────────────────────────────────────────────────

export function Chip({ label, active, onPress, icon, testID }: { label: string; active?: boolean; onPress?: () => void; icon?: IconName; testID?: string }) {
  return (
    <Pressable
      testID={testID}
      accessibilityRole="button"
      accessibilityState={{ selected: !!active }}
      onPress={onPress}
      style={({ pressed }) => [styles.chip, active && styles.chipActive, pressed && { opacity: 0.8 }]}
    >
      <Text style={[styles.chipText, active && styles.chipTextActive]} numberOfLines={1}>
        {label}
      </Text>
      {icon ? <Icon name={icon} size={12} color={active ? colors.white : colors.muted} /> : null}
    </Pressable>
  );
}

// ─── Card / Section ───────────────────────────────────────────────────

export function Card({ children, style }: { children: ReactNode; style?: StyleProp<ViewStyle> }) {
  return <View style={[styles.card, style]}>{children}</View>;
}

export function SectionTitle({ title, action }: { title: string; action?: ReactNode }) {
  return (
    <View style={styles.sectionTitle}>
      <Text style={type.h2}>{title}</Text>
      {action}
    </View>
  );
}

// ─── Badge ────────────────────────────────────────────────────────────

export type Tone = 'neutral' | 'success' | 'warning' | 'danger' | 'info' | 'accent';

const TONES: Record<Tone, { bg: string; fg: string }> = {
  neutral: { bg: colors.navySoft, fg: colors.muted },
  success: { bg: colors.successSoft, fg: colors.success },
  warning: { bg: colors.warningSoft, fg: colors.warning },
  danger: { bg: colors.dangerSoft, fg: colors.danger },
  info: { bg: colors.infoSoft, fg: colors.info },
  accent: { bg: colors.coralSoft, fg: colors.coralDark },
};

export function Badge({ label, tone = 'neutral', testID }: { label: string; tone?: Tone; testID?: string }) {
  const t = TONES[tone];
  return (
    <View testID={testID} style={[styles.badge, { backgroundColor: t.bg }]}>
      <Text style={[styles.badgeText, { color: t.fg }]}>{label}</Text>
    </View>
  );
}

// ─── States ───────────────────────────────────────────────────────────

export function EmptyState({ icon = 'home', title, text, action }: { icon?: IconName; title: string; text?: string; action?: ReactNode }) {
  return (
    <View style={styles.empty}>
      <View style={styles.emptyIcon}>
        <Icon name={icon} size={30} color={colors.navy} />
      </View>
      <Text style={[type.h2, { textAlign: 'center' }]}>{title}</Text>
      {text ? <Text style={[type.body, { color: colors.muted, textAlign: 'center' }]}>{text}</Text> : null}
      {action ? <View style={{ marginTop: space.md, alignSelf: 'stretch' }}>{action}</View> : null}
    </View>
  );
}

export function Loading({ label = 'Carregando…' }: { label?: string }) {
  return (
    <View style={styles.empty}>
      <ActivityIndicator color={colors.navy} size="large" />
      <Text style={type.small}>{label}</Text>
    </View>
  );
}

export function ErrorBox({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <View style={styles.errorBox}>
      <Text style={{ color: colors.danger, fontWeight: '600', flex: 1 }}>{message}</Text>
      {onRetry ? <Button small variant="outline" title="Tentar de novo" onPress={onRetry} /> : null}
    </View>
  );
}

export function Divider() {
  return <View style={{ height: StyleSheet.hairlineWidth, backgroundColor: colors.line, marginVertical: space.md }} />;
}

const styles = StyleSheet.create({
  btn: {
    minHeight: 52,
    borderRadius: radius.md,
    borderWidth: 1.5,
    paddingHorizontal: space.xl,
    alignItems: 'center',
    justifyContent: 'center',
  },
  btnSmall: { minHeight: 38, paddingHorizontal: space.md, borderRadius: radius.sm },
  btnInner: { flexDirection: 'row', alignItems: 'center', gap: space.sm },
  btnText: { fontSize: 16, fontWeight: '700', letterSpacing: 0.1 },
  field: { gap: 6, marginBottom: space.lg },
  fieldLabel: { fontSize: 13, fontWeight: '700', color: colors.text },
  inputWrap: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.white,
    borderWidth: 1.5,
    borderColor: colors.line,
    borderRadius: radius.md,
    paddingHorizontal: space.md,
  },
  inputError: { borderColor: colors.danger, backgroundColor: '#FFFBFB' },
  prefix: { fontSize: 16, color: colors.muted, fontWeight: '600', marginRight: 6 },
  input: { flex: 1, minHeight: 50, fontSize: 16, color: colors.text, paddingVertical: 10 },
  errorText: { color: colors.danger, fontSize: 13, fontWeight: '600' },
  hint: { color: colors.muted, fontSize: 12 },
  chip: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    paddingHorizontal: 14,
    height: 36,
    borderRadius: radius.pill,
    backgroundColor: colors.white,
    borderWidth: 1.5,
    borderColor: colors.line,
  },
  chipActive: { backgroundColor: colors.navy, borderColor: colors.navy },
  chipText: { fontSize: 14, fontWeight: '600', color: colors.text },
  chipTextActive: { color: colors.white },
  card: { backgroundColor: colors.white, borderRadius: radius.lg, padding: space.lg, ...shadow },
  sectionTitle: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: space.md },
  badge: { alignSelf: 'flex-start', paddingHorizontal: 10, paddingVertical: 4, borderRadius: radius.pill },
  badgeText: { fontSize: 12, fontWeight: '700' },
  empty: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: space.xxl, gap: space.sm, minHeight: 280 },
  emptyIcon: {
    width: 68,
    height: 68,
    borderRadius: 34,
    backgroundColor: colors.white,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: space.sm,
    ...shadow,
  },
  errorBox: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: space.md,
    backgroundColor: colors.dangerSoft,
    borderRadius: radius.md,
    padding: space.md,
    marginVertical: space.sm,
  },
});
