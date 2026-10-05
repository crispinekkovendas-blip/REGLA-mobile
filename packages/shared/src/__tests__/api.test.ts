import { describe, expect, it, vi } from 'vitest';
import type { SupabaseClient } from '@supabase/supabase-js';
import {
  bookVisit, documentPath, fetchApplicationsForReview, fetchDashboardStats, fetchFavoriteIds, fetchListing,
  fetchListings, fetchMyProfile, isRealtor, sendInquiry, submitApplication, toggleFavorite, uploadClientDocument,
  withdrawApplication,
} from '../api';
import { CLIENT_DOCUMENTS_BUCKET } from '../supabase';

// ─── Hand-rolled chainable Supabase mock ──────────────────────────────
// Every builder method records [name, ...args] and returns the builder; the
// builder is a thenable that resolves to the queued response for that table.

type Call = [string, ...unknown[]];
interface Res { data?: unknown; error?: { message: string } | null; count?: number | null }

function mockSupabase(responses: Record<string, Res | Res[]> = {}) {
  const queries: { table: string; calls: Call[] }[] = [];
  const queues: Record<string, Res[]> = Object.fromEntries(
    Object.entries(responses).map(([k, v]) => [k, Array.isArray(v) ? [...v] : [v]]),
  );
  const next = (table: string): Res => {
    const q = queues[table];
    if (!q || q.length === 0) return { data: null, error: null };
    return q.length > 1 ? q.shift()! : q[0];
  };

  const methods = ['select', 'insert', 'upsert', 'update', 'delete', 'eq', 'gte', 'lte', 'lt', 'in', 'or', 'order', 'limit', 'single', 'maybeSingle'];
  const builder = (table: string) => {
    const entry = { table, calls: [] as Call[] };
    queries.push(entry);
    const b: Record<string, unknown> = {};
    for (const m of methods) b[m] = (...args: unknown[]) => { entry.calls.push([m, ...args]); return b; };
    b.then = (ok: (v: unknown) => unknown, ko?: (e: unknown) => unknown) => {
      const r = next(table);
      return Promise.resolve({ data: r.data ?? null, error: r.error ?? null, count: r.count ?? null }).then(ok, ko);
    };
    return b;
  };

  const storageCalls: Call[] = [];
  const storageRes: { upload: Res; signed: Res } = { upload: { data: { path: 'x' }, error: null }, signed: { data: { signedUrl: 'https://signed' }, error: null } };
  const sb = {
    from: vi.fn((table: string) => builder(table)),
    rpc: vi.fn(async (fn: string) => next(`rpc:${fn}`)),
    storage: {
      from: vi.fn((bucket: string) => ({
        upload: async (...args: unknown[]) => { storageCalls.push(['upload', bucket, ...args]); return storageRes.upload; },
        createSignedUrl: async (...args: unknown[]) => { storageCalls.push(['createSignedUrl', bucket, ...args]); return storageRes.signed; },
      })),
    },
  };
  return { sb: sb as unknown as SupabaseClient, queries, storageCalls, storageRes };
}

const callsOf = (q: { calls: Call[] }, name: string) => q.calls.filter((c) => c[0] === name).map((c) => c.slice(1));

const photo = (id: number, position: number) => ({ id, listing_id: 1, storage_path: `p${id}.jpg`, alt_text: null, position });

// ─── Listings ─────────────────────────────────────────────────────────

describe('fetchListings', () => {
  it('always restricts to live listings and orders featured first', async () => {
    const { sb, queries } = mockSupabase({ listings: { data: [] } });
    await fetchListings(sb);
    const [q] = queries;
    expect(q.table).toBe('listings');
    expect(callsOf(q, 'eq')).toEqual([['status', 'live']]);
    expect(callsOf(q, 'order')).toEqual([['featured', { ascending: false }], ['created_at', { ascending: false }]]);
    expect(callsOf(q, 'limit')).toEqual([[100]]);
    expect(String(callsOf(q, 'select')[0][0])).toContain('listing_photos(');
  });

  it('applies every filter', async () => {
    const { sb, queries } = mockSupabase({ listings: { data: [] } });
    await fetchListings(sb, { city: 'São Paulo', type: 'house', minBeds: 2, maxPrice: 5000000, query: '  vila (madalena), sp% ' });
    const [q] = queries;
    expect(callsOf(q, 'eq')).toEqual([['status', 'live'], ['city', 'São Paulo'], ['type', 'house']]);
    expect(callsOf(q, 'gte')).toEqual([['beds', 2]]);
    expect(callsOf(q, 'lte')).toEqual([['price', 5000000]]);
    const [[or]] = callsOf(q, 'or') as [[string]];
    // PostgREST-breaking characters are neutralised
    expect(or).not.toMatch(/[()]/);
    expect(or.split(',')).toHaveLength(3);
    expect(or).toMatch(/^title\.ilike\.%vila  madalena   sp %,neighborhood\.ilike\.%.+%,city\.ilike\.%.+%$/);
  });

  it('skips empty filters and blank queries', async () => {
    const { sb, queries } = mockSupabase({ listings: { data: [] } });
    await fetchListings(sb, { query: '   ', minBeds: 0 });
    expect(callsOf(queries[0], 'or')).toEqual([]);
    expect(callsOf(queries[0], 'gte')).toEqual([]);
  });

  it('sorts photos by position', async () => {
    const { sb } = mockSupabase({ listings: { data: [{ id: 1, listing_photos: [photo(3, 2), photo(1, 0), photo(2, 1)] }] } });
    const [l] = await fetchListings(sb);
    expect(l.listing_photos.map((p) => p.id)).toEqual([1, 2, 3]);
  });

  it('tolerates a listing without photos', async () => {
    const { sb } = mockSupabase({ listings: { data: { id: 7, listing_photos: null } } });
    const l = await fetchListing(sb, 7);
    expect(l.listing_photos).toEqual([]);
  });
});

describe('unwrap', () => {
  it('throws the Supabase error message', async () => {
    const { sb } = mockSupabase({ listings: { error: { message: 'JWT expired' } } });
    await expect(fetchListings(sb)).rejects.toThrow('JWT expired');
  });

  it('propagates errors from writes too', async () => {
    const { sb } = mockSupabase({ applications: { error: { message: 'new row violates row-level security policy' } } });
    await expect(withdrawApplication(sb, 1)).rejects.toThrow(/row-level security/);
  });
});

// ─── Profile, applications, favorites ─────────────────────────────────

describe('fetchMyProfile', () => {
  it('returns null when no profile yet', async () => {
    const { sb, queries } = mockSupabase({ client_profiles: { data: null } });
    expect(await fetchMyProfile(sb, 'u1')).toBeNull();
    expect(callsOf(queries[0], 'eq')).toEqual([['user_id', 'u1']]);
    expect(callsOf(queries[0], 'maybeSingle')).toHaveLength(1);
  });
});

describe('submitApplication', () => {
  it('inserts with the caller user_id and returns the row', async () => {
    const row = { id: 10, status: 'submitted', inquiry_id: 99 };
    const { sb, queries } = mockSupabase({ applications: { data: row } });
    const input = { listing_id: 1, intent: 'rent' as const, offered_price: 8500, guarantee_type: null, move_in_date: null, message: null };
    expect(await submitApplication(sb, 'u1', input)).toEqual(row);
    const [q] = queries;
    expect(q.table).toBe('applications');
    expect(callsOf(q, 'insert')).toEqual([[{ ...input, user_id: 'u1' }]]);
    expect(callsOf(q, 'single')).toHaveLength(1);
  });

  it('lets user_id win over a spoofed input field', async () => {
    const { sb, queries } = mockSupabase({ applications: { data: {} } });
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    await submitApplication(sb, 'u1', { listing_id: 1, intent: 'buy', offered_price: 1, guarantee_type: null, move_in_date: null, message: null, user_id: 'evil' } as any);
    expect((callsOf(queries[0], 'insert')[0][0] as { user_id: string }).user_id).toBe('u1');
  });
});

describe('withdrawApplication / review', () => {
  it('only sets status=withdrawn', async () => {
    const { sb, queries } = mockSupabase({ applications: { data: [{ id: 3 }] } });
    await withdrawApplication(sb, 3);
    expect(callsOf(queries[0], 'update')).toEqual([[{ status: 'withdrawn' }]]);
    expect(callsOf(queries[0], 'eq')).toEqual([['id', 3]]);
  });

  it('embeds client_profiles for realtor review and filters by status', async () => {
    const { sb, queries } = mockSupabase({ applications: { data: [] } });
    await fetchApplicationsForReview(sb, 'submitted');
    expect(String(callsOf(queries[0], 'select')[0][0])).toContain('client_profiles(*)');
    expect(callsOf(queries[0], 'eq')).toEqual([['status', 'submitted']]);
  });
});

describe('favorites', () => {
  it('maps rows to ids', async () => {
    const { sb } = mockSupabase({ favorites: { data: [{ listing_id: 4 }, { listing_id: 9 }] } });
    expect(await fetchFavoriteIds(sb)).toEqual([4, 9]);
  });

  it('upserts on and deletes off', async () => {
    const { sb, queries } = mockSupabase({ favorites: { data: [] } });
    await toggleFavorite(sb, 'u1', 4, true);
    await toggleFavorite(sb, 'u1', 4, false);
    expect(callsOf(queries[0], 'upsert')).toEqual([[{ user_id: 'u1', listing_id: 4 }]]);
    expect(callsOf(queries[1], 'delete')).toHaveLength(1);
    expect(callsOf(queries[1], 'eq')).toEqual([['user_id', 'u1'], ['listing_id', 4]]);
  });
});

// ─── Inquiries & visits ───────────────────────────────────────────────

describe('sendInquiry / bookVisit', () => {
  it('inserts an inquiry without selecting it back (SELECT is admin-only)', async () => {
    const { sb, queries } = mockSupabase();
    await sendInquiry(sb, { name: 'Ana', email: 'a@b.co', message: 'Oi, quero visitar', property_id: 1, intent: 'rent' });
    expect(callsOf(queries[0], 'insert')).toHaveLength(1);
    expect(callsOf(queries[0], 'select')).toEqual([]);
  });

  it('books a private scheduled visit owned by the user (matches 0010 policy)', async () => {
    const { sb, queries } = mockSupabase();
    const input = { listing_id: 1, starts_at: '2026-10-10T14:00:00-03:00', visitor_name: 'Ana', visitor_email: 'a@b.co', visitor_phone: null, notes: null };
    await bookVisit(sb, 'u1', input);
    expect(queries[0].table).toBe('showings');
    expect(callsOf(queries[0], 'insert')).toEqual([[{ ...input, type: 'private', status: 'scheduled', created_by: 'u1' }]]);
  });

  it('throws on insert errors', async () => {
    const { sb } = mockSupabase({ showings: { error: { message: 'denied' } } });
    await expect(bookVisit(sb, 'u1', { listing_id: 1, starts_at: 'x', visitor_name: 'A', visitor_email: 'a@b.co', visitor_phone: null, notes: null })).rejects.toThrow('denied');
  });
});

// ─── Documents ────────────────────────────────────────────────────────

describe('documentPath', () => {
  it('prefixes with the user id (storage RLS folder) and kind', () => {
    expect(documentPath('u1', 'cpf', 'cpf.pdf', 1700000000000)).toBe('u1/cpf/1700000000000-cpf.pdf');
  });

  it('strips accents and replaces spaces / unsafe characters', () => {
    expect(documentPath('u1', 'comprovante_renda', 'Holerite Março São João.pdf', 1)).toBe(
      'u1/comprovante_renda/1-Holerite_Marco_Sao_Joao.pdf',
    );
    expect(documentPath('u1', 'outro', '../../etc/pässwd (1).png', 2)).toBe('u1/outro/2-.._.._etc_passwd__1_.png');
  });

  it('never lets the filename escape the user folder', () => {
    const p = documentPath('u1', 'outro', 'a/b\\c.pdf', 3);
    expect(p.split('/')).toHaveLength(3);
    expect(p.startsWith('u1/')).toBe(true);
  });
});

describe('uploadClientDocument', () => {
  const file = { uri: 'file:///x', name: 'RG frente.jpg', mimeType: 'image/jpeg', size: 2048, body: new ArrayBuffer(8) };

  it('uploads to the private bucket then records the row', async () => {
    const { sb, queries, storageCalls } = mockSupabase({ client_documents: { data: { id: 1 } } });
    await uploadClientDocument(sb, 'u1', 'rg_cnh', file, 10);
    const [, bucket, path, , opts] = storageCalls[0];
    expect(bucket).toBe(CLIENT_DOCUMENTS_BUCKET);
    expect(path).toMatch(/^u1\/rg_cnh\/\d+-RG_frente\.jpg$/);
    expect(opts).toEqual({ contentType: 'image/jpeg', upsert: false });
    const [[row]] = callsOf(queries[0], 'insert') as [[Record<string, unknown>]];
    expect(row).toEqual({
      user_id: 'u1', application_id: 10, kind: 'rg_cnh', filename: 'RG frente.jpg',
      storage_path: path, mime_type: 'image/jpeg', size_bytes: 2048,
    });
  });

  it('does not write the row when the upload fails', async () => {
    const { sb, queries, storageRes } = mockSupabase();
    storageRes.upload = { data: null, error: { message: 'Payload too large' } };
    await expect(uploadClientDocument(sb, 'u1', 'cpf', file)).rejects.toThrow('Payload too large');
    expect(queries).toHaveLength(0);
  });
});

// ─── Realtor ──────────────────────────────────────────────────────────

describe('isRealtor', () => {
  it('is true only when is_admin() returns true', async () => {
    expect(await isRealtor(mockSupabase({ 'rpc:is_admin': { data: true } }).sb)).toBe(true);
    expect(await isRealtor(mockSupabase({ 'rpc:is_admin': { data: false } }).sb)).toBe(false);
    expect(await isRealtor(mockSupabase({ 'rpc:is_admin': { error: { message: 'x' } } }).sb)).toBe(false);
  });
});

describe('fetchDashboardStats', () => {
  it('counts each metric with head-only queries', async () => {
    const { sb, queries } = mockSupabase({
      inquiries: { count: 3 }, applications: { count: 2 }, showings: { count: 1 }, listings: { count: 39 },
    });
    const stats = await fetchDashboardStats(sb, new Date('2026-10-04T15:00:00'));
    expect(stats).toEqual({ newLeads: 3, pendingApplications: 2, visitsToday: 1, liveListings: 39 });
    const apps = queries.find((q) => q.table === 'applications')!;
    expect(callsOf(apps, 'in')).toEqual([['status', ['submitted', 'under_review']]]);
    expect(callsOf(apps, 'select')).toEqual([['id', { count: 'exact', head: true }]]);
  });
});
