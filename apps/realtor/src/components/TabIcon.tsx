import { StyleSheet, View, type ColorValue } from 'react-native';

export type TabIconName = 'home' | 'leads' | 'proposals' | 'agenda' | 'listings';

/** Tiny geometric tab glyphs drawn with Views — no icon-font dependency. */
export function TabIcon({ name, color, size = 22 }: { name: TabIconName; color: ColorValue; size?: number }) {
  const s = size;
  const b = 2;
  switch (name) {
    case 'home':
      return (
        <View style={{ width: s, height: s, flexDirection: 'row', flexWrap: 'wrap', gap: 3, alignContent: 'center', justifyContent: 'center' }}>
          {[0, 1, 2, 3].map((i) => (
            <View key={i} style={{ width: (s - 5) / 2, height: (s - 5) / 2, borderRadius: 2, borderWidth: b, borderColor: color, backgroundColor: i === 0 ? color : 'transparent' }} />
          ))}
        </View>
      );
    case 'leads':
      return (
        <View style={{ width: s, height: s, alignItems: 'center', justifyContent: 'center', gap: 3 }}>
          {[1, 0.7, 0.4].map((w) => (
            <View key={w} style={{ width: s * w, height: 4, borderRadius: 2, backgroundColor: color }} />
          ))}
        </View>
      );
    case 'proposals':
      return (
        <View style={{ width: s, height: s, alignItems: 'center', justifyContent: 'center' }}>
          <View style={{ width: s * 0.72, height: s * 0.92, borderRadius: 3, borderWidth: b, borderColor: color, padding: 3, gap: 2.5, justifyContent: 'center' }}>
            {[1, 1, 0.6].map((w, i) => (
              <View key={i} style={{ width: `${w * 100}%`, height: 2, borderRadius: 1, backgroundColor: color }} />
            ))}
          </View>
        </View>
      );
    case 'agenda':
      return (
        <View style={{ width: s, height: s, alignItems: 'center', justifyContent: 'center' }}>
          <View style={{ width: s * 0.9, height: s * 0.82, borderRadius: 3, borderWidth: b, borderColor: color, overflow: 'hidden' }}>
            <View style={{ height: 5, backgroundColor: color }} />
            <View style={styles.dots}>
              {[0, 1, 2, 3].map((i) => (
                <View key={i} style={{ width: 3, height: 3, borderRadius: 1.5, backgroundColor: color }} />
              ))}
            </View>
          </View>
        </View>
      );
    case 'listings':
      return (
        <View style={{ width: s, height: s, alignItems: 'center', justifyContent: 'flex-end' }}>
          <View style={{
            width: 0, height: 0, borderLeftWidth: s * 0.48, borderRightWidth: s * 0.48, borderBottomWidth: s * 0.38,
            borderLeftColor: 'transparent', borderRightColor: 'transparent', borderBottomColor: color,
          }} />
          <View style={{ width: s * 0.7, height: s * 0.48, borderWidth: b, borderTopWidth: 0, borderColor: color, alignItems: 'center', justifyContent: 'flex-end' }}>
            <View style={{ width: 5, height: s * 0.26, backgroundColor: color }} />
          </View>
        </View>
      );
  }
}

const styles = StyleSheet.create({
  dots: { flex: 1, flexDirection: 'row', flexWrap: 'wrap', gap: 2, padding: 2, alignContent: 'center', justifyContent: 'center' },
});
