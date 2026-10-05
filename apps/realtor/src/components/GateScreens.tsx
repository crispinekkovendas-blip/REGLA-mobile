import type { ReactNode } from 'react';
import { ActivityIndicator, StyleSheet, Text, View } from 'react-native';
import { colors, radius, space } from '../theme';
import { Button } from './ui';

function NavyShell({ children }: { children: ReactNode }) {
  return (
    <View style={styles.shell}>
      <View style={styles.inner}>
        <Brand />
        {children}
      </View>
    </View>
  );
}

export function Brand({ light = true }: { light?: boolean }) {
  return (
    <View style={styles.brand}>
      <View style={styles.mark}><Text style={styles.markText}>R</Text></View>
      <View>
        <Text style={[styles.brandName, !light && { color: colors.navy }]}>REGLA</Text>
        <Text style={styles.brandSub}>CORRETOR</Text>
      </View>
    </View>
  );
}

export function ConfigureEnvScreen() {
  return (
    <NavyShell>
      <Text style={styles.title}>Configure o .env</Text>
      <Text style={styles.body}>
        O app não encontrou as credenciais do Supabase. Crie o arquivo <Text style={styles.code}>apps/realtor/.env</Text> a
        partir de <Text style={styles.code}>.env.example</Text> e reinicie o bundler:
      </Text>
      <View style={styles.codeBox}>
        <Text style={styles.codeLine}>EXPO_PUBLIC_SUPABASE_URL=https://….supabase.co</Text>
        <Text style={styles.codeLine}>EXPO_PUBLIC_SUPABASE_ANON_KEY=…</Text>
      </View>
      <Text style={styles.hint}>npx expo start --clear</Text>
    </NavyShell>
  );
}

export function RestrictedScreen({ email, onSignOut }: { email: string | null; onSignOut: () => void }) {
  return (
    <NavyShell>
      <Text style={styles.title}>Acesso restrito a corretores REGLA</Text>
      <Text style={styles.body}>
        A conta {email ? <Text style={styles.strong}>{email}</Text> : 'atual'} não tem permissão de corretor.
        Se você é cliente, use o app REGLA. Se é da equipe, peça ao administrador para liberar seu acesso.
      </Text>
      <Button label="Sair" variant="coral" onPress={onSignOut} style={{ marginTop: space.lg }} testID="restricted-signout" />
    </NavyShell>
  );
}

export function SplashGate({ label = 'Carregando…' }: { label?: string }) {
  return (
    <NavyShell>
      <View style={{ flexDirection: 'row', alignItems: 'center', gap: space.sm }}>
        <ActivityIndicator color={colors.coral} />
        <Text style={styles.body}>{label}</Text>
      </View>
    </NavyShell>
  );
}

const styles = StyleSheet.create({
  shell: { flex: 1, backgroundColor: colors.navy, justifyContent: 'center', padding: space.xl },
  inner: { width: '100%', maxWidth: 440, alignSelf: 'center', gap: space.md },
  brand: { flexDirection: 'row', alignItems: 'center', gap: space.md, marginBottom: space.xl },
  mark: { width: 40, height: 40, borderRadius: radius.md, backgroundColor: colors.coral, alignItems: 'center', justifyContent: 'center' },
  markText: { color: colors.white, fontSize: 22, fontWeight: '900' },
  brandName: { color: colors.white, fontSize: 20, fontWeight: '900', letterSpacing: 3 },
  brandSub: { color: colors.coral, fontSize: 10, fontWeight: '800', letterSpacing: 3 },
  title: { color: colors.white, fontSize: 24, fontWeight: '800', letterSpacing: -0.4 },
  body: { color: colors.onNavyMuted, fontSize: 15, lineHeight: 22 },
  strong: { color: colors.white, fontWeight: '700' },
  code: { color: colors.white, fontWeight: '700' },
  codeBox: { backgroundColor: colors.navyDeep, borderRadius: radius.md, padding: space.md, gap: 4 },
  codeLine: { color: '#CFE0F5', fontSize: 12, fontFamily: 'monospace' },
  hint: { color: colors.onNavyMuted, fontSize: 12 },
});
