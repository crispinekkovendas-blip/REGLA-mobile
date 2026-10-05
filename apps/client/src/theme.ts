import { Platform, type TextStyle, type ViewStyle } from 'react-native';

export const colors = {
  navy: '#0B1F3A',
  navyDeep: '#071629',
  navyMid: '#1B3359',
  navySoft: '#F1F4F9',
  coral: '#FF6B35',
  coralSoft: '#FFF0EA',
  coralDark: '#E2551F',
  text: '#1A2233',
  muted: '#5C6478',
  faint: '#9AA1B2',
  line: '#E5E8EF',
  white: '#FFFFFF',
  bg: '#F1F4F9',
  success: '#12805C',
  successSoft: '#E4F5EE',
  warning: '#B26A00',
  warningSoft: '#FFF4E0',
  danger: '#C62E3A',
  dangerSoft: '#FDECEE',
  info: '#2457C5',
  infoSoft: '#E8EFFD',
} as const;

export const radius = { sm: 10, md: 14, lg: 16, xl: 22, pill: 999 } as const;

export const space = { xs: 4, sm: 8, md: 12, lg: 16, xl: 20, xxl: 28, xxxl: 40 } as const;

export const type = {
  display: { fontSize: 30, lineHeight: 36, fontWeight: '800', letterSpacing: -0.8, color: colors.text },
  h1: { fontSize: 24, lineHeight: 30, fontWeight: '800', letterSpacing: -0.5, color: colors.text },
  h2: { fontSize: 19, lineHeight: 25, fontWeight: '700', letterSpacing: -0.3, color: colors.text },
  h3: { fontSize: 16, lineHeight: 22, fontWeight: '700', color: colors.text },
  body: { fontSize: 15, lineHeight: 22, color: colors.text },
  small: { fontSize: 13, lineHeight: 18, color: colors.muted },
  label: { fontSize: 12, lineHeight: 16, fontWeight: '700', letterSpacing: 0.6, textTransform: 'uppercase', color: colors.muted },
  price: { fontSize: 22, lineHeight: 28, fontWeight: '800', letterSpacing: -0.4, color: colors.navy },
} satisfies Record<string, TextStyle>;

export const shadow: ViewStyle = Platform.select<ViewStyle>({
  web: { boxShadow: '0 6px 24px rgba(11,31,58,0.08)' } as ViewStyle,
  android: { elevation: 3 },
  default: {
    shadowColor: '#0B1F3A',
    shadowOpacity: 0.08,
    shadowRadius: 16,
    shadowOffset: { width: 0, height: 6 },
  },
});

/** Placeholder tones for listings without photos — picked deterministically per listing. */
export const placeholderTones = ['#1B3359', '#2C4A73', '#3D5A80', '#22395E', '#41557A', '#16304F'];
