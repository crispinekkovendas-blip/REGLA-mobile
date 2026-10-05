import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { colors, radius, space, type } from '@/theme';

/** Shown instead of the app when EXPO_PUBLIC_SUPABASE_* env vars are missing. */
export function ConfigureScreen() {
  return (
    <ScrollView contentContainerStyle={styles.wrap} style={{ backgroundColor: colors.navy }}>
      <Text style={styles.brand}>REGLA</Text>
      <Text style={styles.title}>Quase lá!</Text>
      <Text style={styles.text}>
        O app ainda não está conectado ao servidor. Crie o arquivo <Text style={styles.mono}>apps/client/.env</Text> com as chaves do
        Supabase e reinicie o Expo.
      </Text>
      <View style={styles.code}>
        <Text style={styles.mono}>EXPO_PUBLIC_SUPABASE_URL=https://xxxx.supabase.co</Text>
        <Text style={styles.mono}>EXPO_PUBLIC_SUPABASE_ANON_KEY=eyJhbGciOi…</Text>
      </View>
      <Text style={styles.small}>Use o modelo em .env.example. Depois rode: npx expo start -c</Text>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  wrap: { flexGrow: 1, justifyContent: 'center', padding: space.xxl, gap: space.lg },
  brand: { color: colors.coral, fontWeight: '900', letterSpacing: 4, fontSize: 14 },
  title: { ...type.display, color: colors.white },
  text: { ...type.body, color: 'rgba(255,255,255,0.85)' },
  code: { backgroundColor: colors.navyDeep, borderRadius: radius.md, padding: space.lg, gap: 6 },
  mono: { fontFamily: 'monospace', color: '#FFD8C7', fontSize: 13 },
  small: { ...type.small, color: 'rgba(255,255,255,0.6)' },
});
