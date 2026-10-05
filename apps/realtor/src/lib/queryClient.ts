import { QueryClient } from '@tanstack/react-query';

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: { staleTime: 30_000, retry: 1, refetchOnWindowFocus: false },
    mutations: { retry: 0 },
  },
});

/** Query keys in one place so invalidation after mutations stays consistent. */
export const qk = {
  stats: ['stats'] as const,
  agenda: (fromKey: string) => ['agenda', fromKey] as const,
  agendaAll: ['agenda'] as const,
  applications: ['applications'] as const,
  application: (id: number) => ['application', id] as const,
  documents: (userId: string) => ['documents', userId] as const,
  leads: ['leads'] as const,
  lead: (id: number) => ['lead', id] as const,
  leadPhone: (id: number) => ['lead-phone', id] as const,
  notes: (id: number) => ['notes', id] as const,
  listings: ['listings'] as const,
  listing: (id: number) => ['listing', id] as const,
};
