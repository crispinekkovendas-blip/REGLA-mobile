import type { ReactNode } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { colors, space, type } from '@/theme';

/** Large, left-aligned title used at the top of each tab. */
export function ScreenTitle({ title, subtitle, right }: { title: string; subtitle?: string; right?: ReactNode }) {
  const insets = useSafeAreaInsets();
  return (
    <View style={[styles.wrap, { paddingTop: insets.top + space.xl }]}>
      <View style={{ flex: 1 }}>
        <Text style={type.display}>{title}</Text>
        {subtitle ? <Text style={[type.body, { color: colors.muted, marginTop: 4 }]}>{subtitle}</Text> : null}
      </View>
      {right}
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { flexDirection: 'row', alignItems: 'flex-end', paddingHorizontal: space.xl, paddingBottom: space.lg, backgroundColor: colors.bg },
});
