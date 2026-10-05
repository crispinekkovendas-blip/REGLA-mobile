import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  bookVisit, fetchDocuments, fetchFavoriteIds, fetchListing, fetchListings, fetchMyApplications,
  fetchMyProfile, fetchMyVisits, submitApplication, toggleFavorite, upsertMyProfile,
  uploadClientDocument, withdrawApplication,
  type ApplicationInput, type ClientProfileInput, type DocumentKind, type ListingFilters,
  type UploadableFile, type VisitInput,
} from '@regla/shared';
import { supabase } from './supabase';
import { useAuth } from './auth';

export const qk = {
  listings: (f: ListingFilters) => ['listings', f] as const,
  listing: (id: number) => ['listing', id] as const,
  favorites: (uid?: string) => ['favorites', uid] as const,
  profile: (uid?: string) => ['profile', uid] as const,
  applications: (uid?: string) => ['applications', uid] as const,
  visits: (uid?: string) => ['visits', uid] as const,
  documents: (uid?: string) => ['documents', uid] as const,
};

export function useListings(filters: ListingFilters) {
  return useQuery({ queryKey: qk.listings(filters), queryFn: () => fetchListings(supabase, filters) });
}

export function useListing(id: number) {
  return useQuery({ queryKey: qk.listing(id), queryFn: () => fetchListing(supabase, id), enabled: Number.isFinite(id) && id > 0 });
}

export function useFavoriteIds() {
  const { user } = useAuth();
  return useQuery({
    queryKey: qk.favorites(user?.id),
    queryFn: () => fetchFavoriteIds(supabase),
    enabled: !!user,
  });
}

export function useToggleFavorite() {
  const { user } = useAuth();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ listingId, on }: { listingId: number; on: boolean }) => {
      if (!user) throw new Error('Faça login para favoritar.');
      return toggleFavorite(supabase, user.id, listingId, on);
    },
    onMutate: async ({ listingId, on }) => {
      const key = qk.favorites(user?.id);
      await qc.cancelQueries({ queryKey: key });
      const prev = qc.getQueryData<number[]>(key);
      qc.setQueryData<number[]>(key, (ids = []) => (on ? [...new Set([...ids, listingId])] : ids.filter((i) => i !== listingId)));
      return { prev };
    },
    onError: (_e, _v, ctx) => {
      if (ctx?.prev) qc.setQueryData(qk.favorites(user?.id), ctx.prev);
    },
    onSettled: () => qc.invalidateQueries({ queryKey: qk.favorites(user?.id) }),
  });
}

export function useProfile() {
  const { user } = useAuth();
  return useQuery({
    queryKey: qk.profile(user?.id),
    queryFn: () => fetchMyProfile(supabase, user!.id),
    enabled: !!user,
  });
}

export function useSaveProfile() {
  const { user } = useAuth();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: ClientProfileInput) => {
      if (!user) throw new Error('Faça login para salvar seu cadastro.');
      return upsertMyProfile(supabase, user.id, input);
    },
    onSuccess: (row) => qc.setQueryData(qk.profile(user?.id), row),
  });
}

export function useApplications() {
  const { user } = useAuth();
  return useQuery({
    queryKey: qk.applications(user?.id),
    queryFn: () => fetchMyApplications(supabase, user!.id),
    enabled: !!user,
  });
}

export function useSubmitApplication() {
  const { user } = useAuth();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: ApplicationInput) => {
      if (!user) throw new Error('Faça login para enviar a proposta.');
      return submitApplication(supabase, user.id, input);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: qk.applications(user?.id) }),
  });
}

export function useWithdrawApplication() {
  const { user } = useAuth();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => withdrawApplication(supabase, id),
    onSuccess: () => qc.invalidateQueries({ queryKey: qk.applications(user?.id) }),
  });
}

export function useVisits() {
  const { user } = useAuth();
  return useQuery({
    queryKey: qk.visits(user?.id),
    queryFn: () => fetchMyVisits(supabase, user!.id),
    enabled: !!user,
  });
}

export function useBookVisit() {
  const { user } = useAuth();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: VisitInput) => {
      if (!user) throw new Error('Faça login para agendar.');
      return bookVisit(supabase, user.id, input);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: qk.visits(user?.id) }),
  });
}

export function useDocuments() {
  const { user } = useAuth();
  return useQuery({
    queryKey: qk.documents(user?.id),
    queryFn: () => fetchDocuments(supabase, user!.id),
    enabled: !!user,
  });
}

export function useUploadDocument() {
  const { user } = useAuth();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ kind, file, applicationId = null }: { kind: DocumentKind; file: UploadableFile; applicationId?: number | null }) => {
      if (!user) throw new Error('Faça login para enviar documentos.');
      return uploadClientDocument(supabase, user.id, kind, file, applicationId);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: qk.documents(user?.id) }),
  });
}

export const errorText = (e: unknown): string => {
  const m = e instanceof Error ? e.message : String(e ?? '');
  if (/network|fetch/i.test(m)) return 'Sem conexão com o servidor. Tente novamente.';
  if (/row-level security|permission denied/i.test(m)) return 'Você não tem permissão para esta ação.';
  if (/duplicate key/i.test(m)) return 'Este registro já existe.';
  return m || 'Algo deu errado.';
};
