// Data-access layer shared by the client and realtor apps. Every function takes
// the Supabase client explicitly so it is trivially mockable in tests.
import type { SupabaseClient } from '@supabase/supabase-js';
import type {
  Application, ApplicationStatus, ClientDocument, ClientProfile, DocumentKind, Inquiry,
  InquiryStage, LeadNote, ListingStatus, ListingType, ListingWithPhotos, Showing, ShowingStatus,
} from './types';
import type { ApplicationInput, ClientProfileInput, InquiryInput, VisitInput } from './schemas';
import { CLIENT_DOCUMENTS_BUCKET } from './supabase';

// The client is untyped (no generated Database type), so rows come back loosely
// typed; callers cast to the hand-written row interfaces in ./types.
// eslint-disable-next-line @typescript-eslint/no-explicit-any
function unwrap(res: { data: unknown; error: { message: string } | null }): any {
  if (res.error) throw new Error(res.error.message);
  return res.data;
}

// ─── Listings ─────────────────────────────────────────────────────────

export interface ListingFilters {
  query?: string;
  city?: string;
  type?: ListingType;
  minBeds?: number;
  maxPrice?: number;
}

const LISTING_SELECT = '*, listing_photos(id, listing_id, storage_path, alt_text, position)';

export async function fetchListings(sb: SupabaseClient, f: ListingFilters = {}): Promise<ListingWithPhotos[]> {
  let q = sb.from('listings').select(LISTING_SELECT).eq('status', 'live');
  if (f.city) q = q.eq('city', f.city);
  if (f.type) q = q.eq('type', f.type);
  if (f.minBeds) q = q.gte('beds', f.minBeds);
  if (f.maxPrice) q = q.lte('price', f.maxPrice);
  if (f.query?.trim()) {
    const s = f.query.trim().replace(/[%,()]/g, ' ');
    q = q.or(`title.ilike.%${s}%,neighborhood.ilike.%${s}%,city.ilike.%${s}%`);
  }
  const rows = unwrap(await q.order('featured', { ascending: false }).order('created_at', { ascending: false }).limit(100));
  return (rows as ListingWithPhotos[]).map(sortPhotos);
}

/** Realtor view: every listing regardless of status (requires is_admin). */
export async function fetchAllListings(sb: SupabaseClient): Promise<ListingWithPhotos[]> {
  const rows = unwrap(await sb.from('listings').select(LISTING_SELECT).order('updated_at', { ascending: false }));
  return (rows as ListingWithPhotos[]).map(sortPhotos);
}

export async function fetchListing(sb: SupabaseClient, id: number): Promise<ListingWithPhotos> {
  const row = unwrap(await sb.from('listings').select(LISTING_SELECT).eq('id', id).single());
  return sortPhotos(row as ListingWithPhotos);
}

export async function setListingStatus(sb: SupabaseClient, id: number, status: ListingStatus): Promise<void> {
  unwrap(await sb.from('listings').update({ status }).eq('id', id).select('id'));
}

function sortPhotos(l: ListingWithPhotos): ListingWithPhotos {
  return { ...l, listing_photos: [...(l.listing_photos ?? [])].sort((a, b) => a.position - b.position) };
}

// ─── Favorites ────────────────────────────────────────────────────────

export async function fetchFavoriteIds(sb: SupabaseClient): Promise<number[]> {
  const rows = unwrap(await sb.from('favorites').select('listing_id'));
  return (rows as { listing_id: number }[]).map((r) => r.listing_id);
}

export async function toggleFavorite(sb: SupabaseClient, userId: string, listingId: number, on: boolean): Promise<void> {
  if (on) unwrap(await sb.from('favorites').upsert({ user_id: userId, listing_id: listingId }).select('listing_id'));
  else unwrap(await sb.from('favorites').delete().eq('user_id', userId).eq('listing_id', listingId).select('listing_id'));
}

// ─── Client profile ("meu cadastro") ──────────────────────────────────

export async function fetchMyProfile(sb: SupabaseClient, userId: string): Promise<ClientProfile | null> {
  const res = await sb.from('client_profiles').select('*').eq('user_id', userId).maybeSingle();
  return unwrap(res) as ClientProfile | null;
}

export async function upsertMyProfile(sb: SupabaseClient, userId: string, input: ClientProfileInput): Promise<ClientProfile> {
  const row = unwrap(await sb.from('client_profiles').upsert({ user_id: userId, ...input }).select('*').single());
  return row as ClientProfile;
}

// ─── Applications ("propostas") ───────────────────────────────────────

export type ApplicationWithListing = Application & { listings: Pick<ListingWithPhotos, 'id' | 'title' | 'neighborhood' | 'city' | 'price' | 'currency'> | null };
export type ApplicationForReview = ApplicationWithListing & { client_profiles: ClientProfile | null };

const APP_SELECT = '*, listings(id, title, neighborhood, city, price, currency)';

export async function submitApplication(sb: SupabaseClient, userId: string, input: ApplicationInput): Promise<Application> {
  const row = unwrap(await sb.from('applications').insert({ ...input, user_id: userId }).select('*').single());
  return row as Application;
}

export async function fetchMyApplications(sb: SupabaseClient, userId: string): Promise<ApplicationWithListing[]> {
  return unwrap(await sb.from('applications').select(APP_SELECT).eq('user_id', userId).order('created_at', { ascending: false })) as ApplicationWithListing[];
}

export async function withdrawApplication(sb: SupabaseClient, id: number): Promise<void> {
  unwrap(await sb.from('applications').update({ status: 'withdrawn' }).eq('id', id).select('id'));
}

/** Realtor: all applications with the applicant profile joined (requires is_admin). */
export async function fetchApplicationsForReview(sb: SupabaseClient, status?: ApplicationStatus): Promise<ApplicationForReview[]> {
  let q = sb.from('applications').select(`${APP_SELECT}, client_profiles(*)`);
  if (status) q = q.eq('status', status);
  return unwrap(await q.order('created_at', { ascending: false })) as ApplicationForReview[];
}

export async function fetchApplicationForReview(sb: SupabaseClient, id: number): Promise<ApplicationForReview> {
  return unwrap(await sb.from('applications').select(`${APP_SELECT}, client_profiles(*)`).eq('id', id).single()) as ApplicationForReview;
}

export async function reviewApplication(
  sb: SupabaseClient, id: number, status: ApplicationStatus, reviewerNote: string | null, reviewerId: string,
): Promise<void> {
  unwrap(await sb.from('applications').update({ status, reviewer_note: reviewerNote, reviewed_by: reviewerId }).eq('id', id).select('id'));
}

// ─── Client documents ─────────────────────────────────────────────────

export interface UploadableFile {
  uri: string;
  name: string;
  mimeType: string;
  size: number;
  /** Raw bytes; apps read these from `uri` (fetch(uri).arrayBuffer()). */
  body: ArrayBuffer | Blob;
}

export function documentPath(userId: string, kind: DocumentKind, filename: string, now = Date.now()): string {
  const safe = filename.normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/[^a-zA-Z0-9._-]/g, '_');
  return `${userId}/${kind}/${now}-${safe}`;
}

export async function uploadClientDocument(
  sb: SupabaseClient, userId: string, kind: DocumentKind, file: UploadableFile, applicationId: number | null = null,
): Promise<ClientDocument> {
  const path = documentPath(userId, kind, file.name);
  const up = await sb.storage.from(CLIENT_DOCUMENTS_BUCKET).upload(path, file.body, { contentType: file.mimeType, upsert: false });
  if (up.error) throw new Error(up.error.message);
  const row = unwrap(
    await sb.from('client_documents').insert({
      user_id: userId, application_id: applicationId, kind, filename: file.name,
      storage_path: path, mime_type: file.mimeType, size_bytes: file.size,
    }).select('*').single(),
  );
  return row as ClientDocument;
}

export async function fetchDocuments(sb: SupabaseClient, userId: string): Promise<ClientDocument[]> {
  return unwrap(await sb.from('client_documents').select('*').eq('user_id', userId).order('created_at', { ascending: false })) as ClientDocument[];
}

export async function signedDocumentUrl(sb: SupabaseClient, storagePath: string, seconds = 300): Promise<string> {
  const res = await sb.storage.from(CLIENT_DOCUMENTS_BUCKET).createSignedUrl(storagePath, seconds);
  if (res.error) throw new Error(res.error.message);
  return res.data.signedUrl;
}

// ─── Inquiries & visits (client side) ─────────────────────────────────

export async function sendInquiry(sb: SupabaseClient, input: InquiryInput): Promise<void> {
  // anon/authed INSERT is allowed; SELECT is admin-only, so don't .select() back.
  const res = await sb.from('inquiries').insert(input);
  if (res.error) throw new Error(res.error.message);
}

export async function bookVisit(sb: SupabaseClient, userId: string, input: VisitInput): Promise<void> {
  const res = await sb.from('showings').insert({ ...input, type: 'private', status: 'scheduled', created_by: userId });
  if (res.error) throw new Error(res.error.message);
}

export type ShowingWithListing = Showing & { listings: { id: number; title: string; neighborhood: string; city: string } | null };

export async function fetchMyVisits(sb: SupabaseClient, userId: string): Promise<ShowingWithListing[]> {
  return unwrap(await sb.from('showings').select('*, listings(id, title, neighborhood, city)').eq('created_by', userId).order('starts_at', { ascending: true })) as ShowingWithListing[];
}

// ─── Realtor: leads, notes, agenda ────────────────────────────────────

export type InquiryWithListing = Inquiry & { listings: { id: number; title: string } | null };

export async function fetchLeads(sb: SupabaseClient, stage?: InquiryStage): Promise<InquiryWithListing[]> {
  let q = sb.from('inquiries').select('*, listings(id, title)');
  if (stage) q = q.eq('stage', stage);
  return unwrap(await q.order('last_activity_at', { ascending: false }).limit(200)) as InquiryWithListing[];
}

export async function fetchLead(sb: SupabaseClient, id: number): Promise<InquiryWithListing> {
  return unwrap(await sb.from('inquiries').select('*, listings(id, title)').eq('id', id).single()) as InquiryWithListing;
}

export async function updateLead(sb: SupabaseClient, id: number, patch: Partial<Pick<Inquiry, 'stage' | 'priority' | 'read'>>): Promise<void> {
  unwrap(await sb.from('inquiries').update(patch).eq('id', id).select('id'));
}

export async function fetchNotes(sb: SupabaseClient, inquiryId: number): Promise<LeadNote[]> {
  return unwrap(await sb.from('lead_notes').select('*').eq('inquiry_id', inquiryId).order('created_at', { ascending: false })) as LeadNote[];
}

export async function addNote(sb: SupabaseClient, inquiryId: number, body: string, authorId: string): Promise<LeadNote> {
  return unwrap(await sb.from('lead_notes').insert({ inquiry_id: inquiryId, body, author_id: authorId }).select('*').single()) as LeadNote;
}

export async function fetchAgenda(sb: SupabaseClient, fromIso: string, toIso: string): Promise<ShowingWithListing[]> {
  return unwrap(
    await sb.from('showings').select('*, listings(id, title, neighborhood, city)')
      .gte('starts_at', fromIso).lt('starts_at', toIso).order('starts_at', { ascending: true }),
  ) as ShowingWithListing[];
}

export async function updateShowingStatus(sb: SupabaseClient, id: number, status: ShowingStatus): Promise<void> {
  unwrap(await sb.from('showings').update({ status }).eq('id', id).select('id'));
}

export async function isRealtor(sb: SupabaseClient): Promise<boolean> {
  const res = await sb.rpc('is_admin');
  if (res.error) return false;
  return res.data === true;
}

export interface DashboardStats {
  newLeads: number;
  pendingApplications: number;
  visitsToday: number;
  liveListings: number;
}

export async function fetchDashboardStats(sb: SupabaseClient, now = new Date()): Promise<DashboardStats> {
  const start = new Date(now); start.setHours(0, 0, 0, 0);
  const end = new Date(start); end.setDate(end.getDate() + 1);
  const count = async (p: PromiseLike<{ count: number | null; error: { message: string } | null }>) => {
    const r = await p;
    if (r.error) throw new Error(r.error.message);
    return r.count ?? 0;
  };
  const [newLeads, pendingApplications, visitsToday, liveListings] = await Promise.all([
    count(sb.from('inquiries').select('id', { count: 'exact', head: true }).eq('stage', 'inbox')),
    count(sb.from('applications').select('id', { count: 'exact', head: true }).in('status', ['submitted', 'under_review'])),
    count(sb.from('showings').select('id', { count: 'exact', head: true }).gte('starts_at', start.toISOString()).lt('starts_at', end.toISOString())),
    count(sb.from('listings').select('id', { count: 'exact', head: true }).eq('status', 'live')),
  ]);
  return { newLeads, pendingApplications, visitsToday, liveListings };
}
