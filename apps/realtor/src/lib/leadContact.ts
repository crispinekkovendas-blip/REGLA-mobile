import type { SupabaseClient } from '@supabase/supabase-js';

/**
 * `inquiries` has no phone column, so look one up for the "Ligar"/WhatsApp
 * buttons: first a showing booked for this lead, then the client profile with
 * the same e-mail. Any RLS/network error just yields null (buttons disable).
 */
export async function findLeadPhone(sb: SupabaseClient, inquiryId: number, email: string): Promise<string | null> {
  try {
    const s = await sb.from('showings').select('visitor_phone')
      .eq('inquiry_id', inquiryId).not('visitor_phone', 'is', null).limit(1);
    const fromShowing = (s.data as { visitor_phone: string | null }[] | null)?.[0]?.visitor_phone;
    if (fromShowing) return fromShowing;
  } catch { /* ignore */ }
  try {
    const p = await sb.from('client_profiles').select('phone').ilike('email', email.trim()).limit(1);
    const fromProfile = (p.data as { phone: string | null }[] | null)?.[0]?.phone;
    if (fromProfile) return fromProfile;
  } catch { /* ignore */ }
  return null;
}
