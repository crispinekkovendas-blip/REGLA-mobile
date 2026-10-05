import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { QueryClientProvider } from '@tanstack/react-query';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { queryClient } from '../lib/queryClient';
import { AuthProvider, useAuth } from '../lib/auth';
import { isConfigured } from '../lib/supabase';
import { ConfigureEnvScreen, RestrictedScreen, SplashGate } from '../components/GateScreens';
import { colors } from '../theme';

export default function RootLayout() {
  return (
    <SafeAreaProvider>
      <StatusBar style="light" />
      {isConfigured ? (
        <QueryClientProvider client={queryClient}>
          <AuthProvider>
            <Gate />
          </AuthProvider>
        </QueryClientProvider>
      ) : (
        <ConfigureEnvScreen />
      )}
    </SafeAreaProvider>
  );
}

function Gate() {
  const { ready, session, realtor, email, signOut } = useAuth();
  if (!ready) return <SplashGate />;
  if (session && (realtor === 'checking' || realtor === 'unknown')) return <SplashGate label="Verificando acesso…" />;
  if (session && realtor === 'no') {
    return <RestrictedScreen email={email} onSignOut={() => { queryClient.clear(); signOut(); }} />;
  }
  const authed = Boolean(session && realtor === 'yes');

  return (
    <Stack
      screenOptions={{
        headerStyle: { backgroundColor: colors.navy },
        headerTintColor: colors.white,
        headerTitleStyle: { fontWeight: '700' },
        headerShadowVisible: false,
        headerBackTitle: 'Voltar',
        contentStyle: { backgroundColor: colors.navySoft },
      }}
    >
      <Stack.Protected guard={authed}>
        <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
        <Stack.Screen name="lead/[id]" options={{ title: 'Lead' }} />
        <Stack.Screen name="application/[id]" options={{ title: 'Proposta' }} />
        <Stack.Screen name="listing/[id]" options={{ title: 'Imóvel' }} />
      </Stack.Protected>
      <Stack.Protected guard={!authed}>
        <Stack.Screen name="login" options={{ headerShown: false }} />
      </Stack.Protected>
    </Stack>
  );
}
