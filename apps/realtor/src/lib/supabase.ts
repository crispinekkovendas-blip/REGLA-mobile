import 'react-native-url-polyfill/auto';
import AsyncStorage from '@react-native-async-storage/async-storage';
import type { SupabaseClient } from '@supabase/supabase-js';
import { createReglaClient, type StorageAdapter } from '@regla/shared';

// Must be read as literal `process.env.EXPO_PUBLIC_*` so Expo inlines them at build time.
export const SUPABASE_URL = process.env.EXPO_PUBLIC_SUPABASE_URL ?? '';
const SUPABASE_ANON_KEY = process.env.EXPO_PUBLIC_SUPABASE_ANON_KEY ?? '';

export const isConfigured = Boolean(SUPABASE_URL && SUPABASE_ANON_KEY);

const storage: StorageAdapter = {
  getItem: (k) => AsyncStorage.getItem(k),
  setItem: (k, v) => AsyncStorage.setItem(k, v),
  removeItem: (k) => AsyncStorage.removeItem(k),
};

/** null when the .env is missing — the root layout then shows the "configure .env" screen. */
export const supabase: SupabaseClient | null = isConfigured
  ? createReglaClient(SUPABASE_URL, SUPABASE_ANON_KEY, storage)
  : null;

/** Non-null accessor for screens that only mount once the app is configured. */
export function getSupabase(): SupabaseClient {
  if (!supabase) throw new Error('Supabase não configurado (.env ausente).');
  return supabase;
}
