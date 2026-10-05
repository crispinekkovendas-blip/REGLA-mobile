import { Platform, type TextStyle } from 'react-native';

export const colors = {
  navy: '#0B1F3A',
  navyDeep: '#071629',
  navyMid: '#163257',
  navySoft: '#F1F4F9',
  coral: '#FF6B35',
  coralSoft: '#FFF0EA',
  text: '#1A2233',
  muted: '#5C6478',
  faint: '#8B93A7',
  line: '#E5E8EF',
  white: '#FFFFFF',
  ok: '#3DDC97',
  warn: '#FFB547',
  danger: '#DC2626',
  info: '#5AB7FF',
  onNavyMuted: '#9FB0C8',
} as const;

/** Tone → {fg, bg} pairs used by pills, badges and status dots. */
export type Tone = 'ok' | 'warn' | 'danger' | 'info' | 'neutral' | 'coral' | 'navy';

export const tones: Record<Tone, { fg: string; bg: string; dot: string }> = {
  ok: { fg: '#0F7A4F', bg: '#E3FAF0', dot: colors.ok },
  warn: { fg: '#8A5A00', bg: '#FFF4DF', dot: colors.warn },
  danger: { fg: '#A51C1C', bg: '#FDE8E8', dot: colors.danger },
  info: { fg: '#0B5C94', bg: '#E5F3FF', dot: colors.info },
  neutral: { fg: colors.muted, bg: '#EDF0F5', dot: colors.faint },
  coral: { fg: '#C2410C', bg: colors.coralSoft, dot: colors.coral },
  navy: { fg: colors.white, bg: colors.navy, dot: colors.navy },
};

export const space = { xs: 4, sm: 8, md: 12, lg: 16, xl: 24, xxl: 32 } as const;
export const radius = { sm: 6, md: 10, lg: 14, pill: 999 } as const;

const num: TextStyle = { fontVariant: ['tabular-nums'] };

export const type = {
  display: { fontSize: 28, fontWeight: '800', letterSpacing: -0.6, color: colors.text, ...num } as TextStyle,
  title: { fontSize: 18, fontWeight: '700', letterSpacing: -0.2, color: colors.text } as TextStyle,
  h2: { fontSize: 15, fontWeight: '700', color: colors.text } as TextStyle,
  body: { fontSize: 14, color: colors.text, lineHeight: 20 } as TextStyle,
  small: { fontSize: 12, color: colors.muted } as TextStyle,
  /** Overline: tiny uppercase label used for section headers & field labels. */
  over: { fontSize: 11, fontWeight: '700', letterSpacing: 0.8, textTransform: 'uppercase', color: colors.muted } as TextStyle,
  mono: {
    fontSize: 12,
    fontFamily: Platform.select({ ios: 'Menlo', android: 'monospace', default: 'monospace' }),
    color: colors.muted,
  } as TextStyle,
  num,
};

export const shadow = Platform.select({
  web: { boxShadow: '0 1px 2px rgba(11,31,58,0.06)' } as object,
  default: {
    shadowColor: colors.navy,
    shadowOpacity: 0.06,
    shadowRadius: 4,
    shadowOffset: { width: 0, height: 1 },
    elevation: 1,
  },
});
