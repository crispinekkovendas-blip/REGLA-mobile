import { useCallback, useMemo } from 'react';
import { router } from 'expo-router';
import { useAuth } from './auth';
import { useFavoriteIds, useToggleFavorite } from './queries';

/** Favorite ids + a toggle that sends anonymous users to login first. */
export function useFavorites() {
  const { user } = useAuth();
  const { data } = useFavoriteIds();
  const mutation = useToggleFavorite();
  const ids = useMemo(() => new Set(data ?? []), [data]);

  const toggle = useCallback(
    (listingId: number) => {
      if (!user) {
        router.push('/auth/login');
        return;
      }
      mutation.mutate({ listingId, on: !ids.has(listingId) });
    },
    [user, ids, mutation],
  );

  return { ids, toggle, isFavorite: (id: number) => ids.has(id) };
}
