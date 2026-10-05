import 'react-native-url-polyfill/auto';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { AppState, Platform } from 'react-native';
import type { SupabaseClient } from '@supabase/supabase-js';
import { createReglaClient, photoUrl, type StorageAdapter } from '@regla/shared';

const url = (process.env.EXPO_PUBLIC_SUPABASE_URL ?? '').trim();
const anonKey = (process.env.EXPO_PUBLIC_SUPABASE_ANON_KEY ?? '').trim();

/** False when the .env is missing — the app then renders a "configure .env" screen instead of crashing. */
export const isConfigured = /^https?:\/\//.test(url) && anonKey.length > 0;

export const SUPABASE_URL = isConfigured ? url : 'https://not-configured.supabase.co';

const storage: StorageAdapter = {
  getItem: (k) => AsyncStorage.getItem(k),
  setItem: (k, v) => AsyncStorage.setItem(k, v),
  removeItem: (k) => AsyncStorage.removeItem(k),
};

// When not configured we still build a client against a placeholder host so
// imports never throw; the UI gate prevents any request from being made.
export const supabase: SupabaseClient = createReglaClient(SUPABASE_URL, isConfigured ? anonKey : 'not-configured', storage);

export const listingPhotoUrl = (storagePath: string): string => photoUrl(SUPABASE_URL, storagePath);

// Keep the session fresh only while the app is in the foreground (Supabase RN guidance).
if (isConfigured && Platform.OS !== 'web') {
  AppState.addEventListener('change', (state) => {
    if (state === 'active') supabase.auth.startAutoRefresh();
    else supabase.auth.stopAutoRefresh();
  });
}
