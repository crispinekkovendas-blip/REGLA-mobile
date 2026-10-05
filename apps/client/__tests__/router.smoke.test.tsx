import path from 'path';
import { renderRouter, screen, waitFor } from 'expo-router/testing-library';

// Fake Supabase: every query builder chain resolves to the listing fixture.
const LISTING = {
  id: 3, slug: 'casa', title: 'Casa com quintal na Vila Madalena', city: 'São Paulo', country: 'BR',
  neighborhood: 'Vila Madalena', type: 'house', price: 1_250_000, currency: 'BRL', beds: 3, baths: 2,
  area_m2: 140, area_ft2: null, tags: ['venda'], palette: '', shape: '', summary: 'Linda casa',
  description: 'Casa reformada com quintal.', status: 'live', featured: true, agent_id: null,
  created_at: '2026-10-01T00:00:00Z', updated_at: '2026-10-01T00:00:00Z', listing_photos: [],
};

jest.mock('@/lib/supabase', () => {
  const builder = (single: boolean): unknown =>
    new Proxy(
      {},
      {
        get(_t, prop) {
          if (prop === 'then') {
            const res = { data: single ? LISTING : [LISTING], error: null };
            return (ok: (v: unknown) => unknown) => Promise.resolve(res).then(ok);
          }
          if (prop === 'single' || prop === 'maybeSingle') return () => builder(true);
          return () => builder(single);
        },
      },
    );
  return {
    isConfigured: true,
    SUPABASE_URL: 'https://test.supabase.co',
    listingPhotoUrl: (p: string) => `https://test.supabase.co/${p}`,
    supabase: {
      from: () => builder(false),
      auth: {
        getSession: () => Promise.resolve({ data: { session: null } }),
        onAuthStateChange: () => ({ data: { subscription: { unsubscribe() {} } } }),
      },
    },
  };
});

jest.setTimeout(30_000);

const APP_DIR = path.join(__dirname, '..', 'app');

describe('app routes (smoke)', () => {
  it('renders the Buscar tab with listings from the API', async () => {
    await renderRouter(APP_DIR, { initialUrl: '/' });
    expect(await screen.findByText('Casa com quintal na Vila Madalena')).toBeTruthy();
    expect(screen.getByText('RG-0003')).toBeTruthy();
    expect(screen.getByText('Buscar')).toBeTruthy();
  });

  it('renders the listing detail with CTAs', async () => {
    await renderRouter(APP_DIR, { initialUrl: '/listing/3' });
    expect(await screen.findByText('Fazer proposta')).toBeTruthy();
    expect(screen.getByText('Agendar visita')).toBeTruthy();
    expect(screen.getByText('Falar no WhatsApp')).toBeTruthy();
    expect(screen.getByText('Ref. RG-0003')).toBeTruthy();
  });

  it('asks anonymous users to log in on protected tabs', async () => {
    await renderRouter(APP_DIR, { initialUrl: '/propostas' });
    await waitFor(() => expect(screen.getByText('Suas propostas')).toBeTruthy());
    expect(screen.getByText('Criar conta grátis')).toBeTruthy();
  });
});
