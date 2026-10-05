import { createClient, type SupabaseClient } from '@supabase/supabase-js';

export interface StorageAdapter {
  getItem(key: string): Promise<string | null>;
  setItem(key: string, value: string): Promise<void>;
  removeItem(key: string): Promise<void>;
}

/**
 * Build the Supabase client used by both apps. Each app passes its own
 * persistent storage (AsyncStorage on native) so sessions survive restarts.
 * URL + anon key come from EXPO_PUBLIC_SUPABASE_URL / EXPO_PUBLIC_SUPABASE_ANON_KEY.
 */
export function createReglaClient(url: string, anonKey: string, storage?: StorageAdapter): SupabaseClient {
  return createClient(url, anonKey, {
    auth: {
      storage,
      autoRefreshToken: true,
      persistSession: Boolean(storage),
      detectSessionInUrl: false,
    },
  });
}

export const LISTING_PHOTOS_BUCKET = 'listing-photos';
export const CLIENT_DOCUMENTS_BUCKET = 'client-documents';

export const photoUrl = (supabaseUrl: string, storagePath: string): string =>
  `${supabaseUrl.replace(/\/$/, '')}/storage/v1/object/public/${LISTING_PHOTOS_BUCKET}/${storagePath}`;
