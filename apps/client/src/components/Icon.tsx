// Dependency-free icon set drawn with Views, so it renders identically on
// iOS, Android and web without loading an icon font.
import { Text, View, type ColorValue, type ViewStyle } from 'react-native';
import { colors } from '@/theme';

export type IconName =
  | 'search' | 'heart' | 'heart-outline' | 'calendar' | 'doc' | 'user' | 'home'
  | 'chevron-left' | 'chevron-right' | 'chevron-down' | 'check' | 'close' | 'plus' | 'chat' | 'pin' | 'clock' | 'upload';

interface Props {
  name: IconName;
  size?: number;
  color?: ColorValue;
  style?: ViewStyle;
}

export function Icon({ name, size = 22, color = colors.text, style }: Props) {
  const s = size;
  const stroke = Math.max(1.6, s / 11);
  const box: ViewStyle = { width: s, height: s, alignItems: 'center', justifyContent: 'center' };

  switch (name) {
    case 'search':
      return (
        <View style={[box, style]}>
          <View style={{ position: 'absolute', left: s * 0.1, top: s * 0.1, width: s * 0.62, height: s * 0.62, borderRadius: s, borderWidth: stroke, borderColor: color }} />
          <View style={{ position: 'absolute', left: s * 0.6, top: s * 0.66, width: s * 0.34, height: stroke * 1.1, borderRadius: stroke, backgroundColor: color, transform: [{ rotate: '45deg' }] }} />
        </View>
      );
    case 'heart':
    case 'heart-outline':
      return (
        <View style={[box, style]}>
          <Text allowFontScaling={false} style={{ color, fontSize: s * 0.95, lineHeight: s * 1.1, textAlign: 'center', includeFontPadding: false }}>
            {name === 'heart' ? '♥' : '♡'}
          </Text>
        </View>
      );
    case 'calendar':
      return (
        <View style={[box, style]}>
          <View style={{ width: s * 0.82, height: s * 0.74, marginTop: s * 0.1, borderRadius: s * 0.16, borderWidth: stroke, borderColor: color, overflow: 'hidden' }}>
            <View style={{ height: s * 0.16, backgroundColor: color }} />
          </View>
          <View style={{ position: 'absolute', top: s * 0.04, left: s * 0.3, width: stroke, height: s * 0.2, borderRadius: stroke, backgroundColor: color }} />
          <View style={{ position: 'absolute', top: s * 0.04, right: s * 0.3, width: stroke, height: s * 0.2, borderRadius: stroke, backgroundColor: color }} />
        </View>
      );
    case 'doc':
      return (
        <View style={[box, style]}>
          <View style={{ width: s * 0.66, height: s * 0.86, borderRadius: s * 0.12, borderWidth: stroke, borderColor: color, paddingTop: s * 0.16, paddingHorizontal: s * 0.1, gap: s * 0.1 }}>
            <View style={{ height: stroke, backgroundColor: color, borderRadius: stroke }} />
            <View style={{ height: stroke, backgroundColor: color, borderRadius: stroke }} />
            <View style={{ height: stroke, width: '60%', backgroundColor: color, borderRadius: stroke }} />
          </View>
        </View>
      );
    case 'user':
      return (
        <View style={[box, style]}>
          <View style={{ width: s * 0.4, height: s * 0.4, borderRadius: s, borderWidth: stroke, borderColor: color, marginBottom: s * 0.04 }} />
          <View style={{ width: s * 0.78, height: s * 0.36, borderTopLeftRadius: s * 0.4, borderTopRightRadius: s * 0.4, borderWidth: stroke, borderBottomWidth: 0, borderColor: color }} />
        </View>
      );
    case 'home':
      return (
        <View style={[box, style]}>
          <View style={{ width: s * 0.56, height: s * 0.56, borderLeftWidth: stroke, borderTopWidth: stroke, borderColor: color, transform: [{ rotate: '45deg' }], position: 'absolute', top: s * 0.16 }} />
          <View style={{ width: s * 0.6, height: s * 0.44, borderWidth: stroke, borderTopWidth: 0, borderColor: color, position: 'absolute', bottom: s * 0.08 }} />
        </View>
      );
    case 'chevron-left':
    case 'chevron-right':
    case 'chevron-down': {
      const rot = name === 'chevron-left' ? '-45deg' : name === 'chevron-right' ? '135deg' : '-135deg';
      const nudge = name === 'chevron-left' ? { marginLeft: s * 0.18 } : name === 'chevron-right' ? { marginRight: s * 0.18 } : { marginTop: -s * 0.2 };
      return (
        <View style={[box, style]}>
          <View style={[{ width: s * 0.42, height: s * 0.42, borderLeftWidth: stroke * 1.1, borderTopWidth: stroke * 1.1, borderColor: color, transform: [{ rotate: rot }] }, nudge]} />
        </View>
      );
    }
    case 'check':
      return (
        <View style={[box, style]}>
          <View style={{ width: s * 0.3, height: s * 0.56, borderRightWidth: stroke * 1.2, borderBottomWidth: stroke * 1.2, borderColor: color, transform: [{ rotate: '45deg' }], marginTop: -s * 0.12 }} />
        </View>
      );
    case 'close':
    case 'plus': {
      const r = name === 'close' ? '45deg' : '0deg';
      return (
        <View style={[box, style, { transform: [{ rotate: r }] }]}>
          <View style={{ position: 'absolute', width: s * 0.72, height: stroke, borderRadius: stroke, backgroundColor: color }} />
          <View style={{ position: 'absolute', width: stroke, height: s * 0.72, borderRadius: stroke, backgroundColor: color }} />
        </View>
      );
    }
    case 'chat':
      return (
        <View style={[box, style]}>
          <View style={{ width: s * 0.8, height: s * 0.62, borderRadius: s * 0.3, borderWidth: stroke, borderColor: color, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: s * 0.08 }}>
            {[0, 1, 2].map((i) => (
              <View key={i} style={{ width: stroke * 1.2, height: stroke * 1.2, borderRadius: stroke, backgroundColor: color }} />
            ))}
          </View>
          <View style={{ position: 'absolute', bottom: s * 0.1, left: s * 0.22, width: s * 0.18, height: s * 0.18, borderLeftWidth: stroke, borderBottomWidth: stroke, borderColor: color, transform: [{ rotate: '-20deg' }] }} />
        </View>
      );
    case 'pin':
      return (
        <View style={[box, style]}>
          <View style={{ width: s * 0.6, height: s * 0.6, borderRadius: s, borderBottomRightRadius: 0, borderWidth: stroke, borderColor: color, transform: [{ rotate: '45deg' }], alignItems: 'center', justifyContent: 'center', marginTop: -s * 0.1 }}>
            <View style={{ width: s * 0.16, height: s * 0.16, borderRadius: s, backgroundColor: color }} />
          </View>
        </View>
      );
    case 'clock':
      return (
        <View style={[box, style]}>
          <View style={{ width: s * 0.84, height: s * 0.84, borderRadius: s, borderWidth: stroke, borderColor: color }} />
          <View style={{ position: 'absolute', width: stroke, height: s * 0.28, top: s * 0.24, borderRadius: stroke, backgroundColor: color }} />
          <View style={{ position: 'absolute', height: stroke, width: s * 0.22, left: s * 0.5, top: s * 0.5, borderRadius: stroke, backgroundColor: color }} />
        </View>
      );
    case 'upload':
      return (
        <View style={[box, style]}>
          <View style={{ width: s * 0.36, height: s * 0.36, borderLeftWidth: stroke, borderTopWidth: stroke, borderColor: color, transform: [{ rotate: '45deg' }], position: 'absolute', top: s * 0.12 }} />
          <View style={{ position: 'absolute', top: s * 0.14, width: stroke, height: s * 0.5, borderRadius: stroke, backgroundColor: color }} />
          <View style={{ position: 'absolute', bottom: s * 0.1, width: s * 0.72, height: stroke, borderRadius: stroke, backgroundColor: color }} />
        </View>
      );
    default:
      return <View style={[box, style]} />;
  }
}
