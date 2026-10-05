import { useState } from 'react';
import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from '@/lib/auth';
import { isConfigured } from '@/lib/supabase';
import { ConfigureScreen } from '@/components/ConfigureScreen';
import { colors } from '@/theme';

export default function RootLayout() {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: { staleTime: 60_000, retry: 1, refetchOnWindowFocus: false },
          mutations: { retry: 0 },
        },
      }),
  );

  if (!isConfigured) {
    return (
      <SafeAreaProvider>
        <StatusBar style="light" />
        <ConfigureScreen />
      </SafeAreaProvider>
    );
  }

  return (
    <SafeAreaProvider>
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <StatusBar style="dark" />
          <Stack
            screenOptions={{
              headerTintColor: colors.navy,
              headerTitleStyle: { fontWeight: '700', color: colors.text },
              headerShadowVisible: false,
              headerStyle: { backgroundColor: colors.bg },
              contentStyle: { backgroundColor: colors.bg },
              headerBackButtonDisplayMode: 'minimal',
            }}
          >
            <Stack.Screen name="(tabs)" options={{ headerShown: false, title: 'Início' }} />
            <Stack.Screen name="listing/[id]" options={{ headerTransparent: true, title: '', headerStyle: { backgroundColor: 'transparent' } }} />
            <Stack.Screen name="visit/[listingId]" options={{ title: 'Agendar visita' }} />
            <Stack.Screen name="apply/[listingId]" options={{ title: 'Fazer proposta', gestureEnabled: false }} />
            <Stack.Screen name="auth/login" options={{ title: 'Entrar', presentation: 'modal' }} />
            <Stack.Screen name="auth/signup" options={{ title: 'Criar conta', presentation: 'modal' }} />
            <Stack.Screen name="auth/callback" options={{ title: '', headerShown: false }} />
            <Stack.Screen name="profile/edit" options={{ title: 'Meu cadastro' }} />
            <Stack.Screen name="profile/documents" options={{ title: 'Meus documentos' }} />
          </Stack>
        </AuthProvider>
      </QueryClientProvider>
    </SafeAreaProvider>
  );
}
